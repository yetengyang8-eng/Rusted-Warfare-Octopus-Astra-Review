package io.rwagent.client;

import java.util.*;

/** G5 independent General combat decisions; only the host admits proposals and native receipts. */
public final class GeneralCombatDirector {
    public static final double PRESSURE_RADIUS=600,OVERMATCH_RATIO=1.8,LOSING_DAMAGE_FRACTION=.25,ESCAPE_DAMAGE_FRACTION=.08,RECOVERY_RATIO=.55;
    public static final long DAMAGE_WINDOW_MS=5000,REGROUP_HOLD_MS=4000,NORMAL_RETRY_MS=3000,RETREAT_RETRY_MS=1500;
    private final GeneralRegistry registry;private final ForceController.Host host;
    private final Map<GeneralRegistry.GeneralId,CombatLedger> ledgers=new LinkedHashMap<GeneralRegistry.GeneralId,CombatLedger>();
    private final List<Map<String,Object>> changes=new ArrayList<Map<String,Object>>();
    private final Map<GeneralRegistry.GeneralId,LinkedHashMap<Integer,Long>> blockedFrontiers=new HashMap<GeneralRegistry.GeneralId,LinkedHashMap<Integer,Long>>();
    private final Map<GeneralRegistry.GeneralId,OutwardMission> missions=new HashMap<GeneralRegistry.GeneralId,OutwardMission>();
    private static final class OutwardMission {
        final CombatLedger ledger;double directionX,directionY,x,y;boolean directionKnown,active;
        int tile,frontierTile;long acceptedFrame;String anchorType,event="INITIAL";
        OutwardMission(CombatLedger ledger){this.ledger=ledger;}
        Map<String,Object> view(){return fields("active",active,"directionKnown",directionKnown,"directionX",directionKnown?directionX:null,
                "directionY",directionKnown?directionY:null,"targetX",active?x:null,"targetY",active?y:null,"targetTile",active?tile:null,
                "frontierTile",active?frontierTile:null,"acceptedFrame",active?acceptedFrame:null,"lastEvent",event,
                "authority","ACCEPTED_NATIVE_FRONTIER_TRAVEL_ONLY_NO_HIDDEN_TARGET_OR_SEARCH_AREA");}
    }
    private Map<GeneralRegistry.GeneralId,Long> preferred=Collections.emptyMap();
    private CommanderDirector commander;private final BaseThreatZones baseZones=new BaseThreatZones();
    public void setCommander(CommanderDirector commander){this.commander=commander;}
    public boolean knownBaseZoneBlocks(double x,double y){return baseZones.blocks(x,y,x,y,Collections.<Long>emptySet())!=null;}
    public void reset(){ledgers.clear();baseZones.reset();changes.clear();preferred=Collections.emptyMap();blockedFrontiers.clear();missions.clear();}
    public GeneralCombatDirector(GeneralRegistry registry,ForceController.Host host){if(registry==null||host==null)throw new IllegalArgumentException("Registry and host required");this.registry=registry;this.host=host;}
    public void setPreferredThreats(Map<GeneralRegistry.GeneralId,Long> targets){preferred=targets==null?Collections.<GeneralRegistry.GeneralId,Long>emptyMap():new HashMap<GeneralRegistry.GeneralId,Long>(targets);}
    public Map<String,Object> view(GeneralRegistry.GeneralId id){CombatLedger ledger=ledgers.get(id);if(ledger==null)return Collections.<String,Object>emptyMap();Map<String,Object> out=new LinkedHashMap<String,Object>(ledger.view());OutwardMission mission=missions.get(id);out.put("outwardMission",mission==null?Collections.emptyMap():mission.view());return Collections.unmodifiableMap(out);}
    public boolean retreating(GeneralRegistry.GeneralId id){CombatLedger ledger=ledgers.get(id);return ledger!=null&&ledger.retreating;}
    public List<Map<String,Object>> drainChanges(){List<Map<String,Object>> result=new ArrayList<Map<String,Object>>(changes);changes.clear();return result;}
    /** Observe before Commander allocation. Duplicate frame calls cannot create extra trend samples. */
    public void observe(Map<String,Object> state,Map<String,Object> enemies,double homeX,double homeY,long now){
        Long frame=GameClock.number(state==null?null:state.get("frame")),ownTime=GameClock.number(state==null?null:state.get("gameTimeMs"));
        if(frame==null||ownTime==null||ownTime!=now||!(state.get("sessionId") instanceof String)||!(state.get("ownUnits") instanceof List)||!registry.currentOwnState(state))return;
        baseZones.observe((String)state.get("sessionId"),registry.observation().player,frame,now,enemies,combatCurrent(state,enemies,now));
        Set<GeneralRegistry.GeneralId> alive=new HashSet<GeneralRegistry.GeneralId>();
        for(GeneralRegistry.GeneralView general:registry.generals()){
            alive.add(general.id);CombatLedger ledger=ledgers.get(general.id);
            if(ledger==null||!Objects.equals(ledger.session,state.get("sessionId"))||!Objects.equals(ledger.player,registry.observation().player)){ledger=new CombatLedger(general.id);ledger.session=(String)state.get("sessionId");ledger.player=registry.observation().player;ledgers.put(general.id,ledger);}
            if(frame<=ledger.frame||now<ledger.time)continue;
            Map<String,Object> before=ledger.view();long oldRevision=ledger.revision;
            ledger.lastObservationGapMs=ledger.time<0?-1:now-ledger.time;
            ledger.damageWindowKnown=ledger.lastObservationGapMs>=0&&ledger.lastObservationGapMs<=DAMAGE_WINDOW_MS;
            ledger.frame=frame;ledger.time=now;ledger.ownCurrent=true;ledger.currentVisibleKnown=combatCurrent(state,enemies,now);
            ledger.currentThreatSignature=DestinationUnknownDefer.threatSignature(enemies,ledger.currentVisibleKnown);
            ledger.homeX=homeX;ledger.homeY=homeY;
            ledger.baseThreatZones=baseZones.view();ledger.blockedBaseZone=null;
            ledger.ownHp=ledger.ownMaxHp=0;ledger.currentMembers=ledger.currentHealthy=0;
            Map<Long,CombatLedger.OwnSample> current=new LinkedHashMap<Long,CombatLedger.OwnSample>();double damage=0,enemyDelta=0;int deaths=0;
            // Include explicitly observed dead samples for loss evidence; absence is never a death.
            for(Map.Entry<Long,CombatLedger.OwnSample> previous:ledger.own.entrySet()){
                Map<String,Object> unit=find(state,previous.getKey());GeneralRegistry.UnitView owner=registry.unit(previous.getKey());
                if(unit!=null&&(Boolean.TRUE.equals(unit.get("dead"))||number(unit,"hp")==0)
                        &&(owner==null||owner.ownerGeneration==previous.getValue().generation)){deaths++;damage+=previous.getValue().hp;}
            }
            for(Long id:general.members){GeneralRegistry.UnitView owner=registry.unit(id);Map<String,Object> unit=find(state,id);
                if(!owned(general,owner)||!ready(unit))continue;
                double hp=number(unit,"hp"),max=number(unit,"maxHp");boolean maxKnown=Double.isFinite(max)&&max>0;if(!maxKnown)max=hp;
                ledger.ownHp+=hp;ledger.ownMaxHp+=max;ledger.currentMembers++;
                if(owner.healthRole==GeneralRegistry.HealthRole.NORMAL&&maxKnown&&hp>=max*.5)ledger.currentHealthy++;
                CombatLedger.OwnSample previous=ledger.own.get(id);if(previous!=null&&previous.generation==owner.ownerGeneration)damage+=Math.max(0,previous.hp-hp);
                else if(previous!=null){ledger.acceptedAt.remove(id);ledger.acceptedRevision.remove(id);ledger.retreatReceiptFrame.remove(id);ledger.retreatReceiptGeneration.remove(id);ledger.event("OWNER_GENERATION_CHANGED",now);}
                if(!ledger.progressX.containsKey(id)||previous==null||previous.generation!=owner.ownerGeneration||Math.hypot(number(unit,"x")-ledger.progressX.get(id),number(unit,"y")-ledger.progressY.get(id))>=24){
                    ledger.progressX.put(id,number(unit,"x"));ledger.progressY.put(id,number(unit,"y"));ledger.progressAt.put(id,now);}
                DestinationUnknownDefer pending=ledger.unknownDestinationDefer.get(id);if(pending!=null&&Objects.equals(ledger.acceptedGeneration.get(id),owner.ownerGeneration))pending.observe(unit,frame,now);
                current.put(id,new CombatLedger.OwnSample(owner.ownerGeneration,hp));
            }
            ledger.own.clear();ledger.own.putAll(current);ledger.visiblePressure=0;
            if(ledger.currentVisibleKnown){Map<Long,Double> currentHp=new HashMap<Long,Double>();
                for(Map<String,Object> enemy:visible(enemies)){
                    boolean local=general.centroidKnown&&general.centroidFrame==frame&&distance(enemy,general.x,general.y)<=PRESSURE_RADIUS;
                    if(local||Objects.equals(ledger.lastTarget,id(enemy))){Double previous=ledger.visibleHp.get(id(enemy));if(previous!=null)enemyDelta+=Math.max(0,previous-number(enemy,"hp"));currentHp.put(id(enemy),number(enemy,"hp"));}
                    if(Boolean.TRUE.equals(enemy.get("canAttack"))&&general.centroidKnown&&general.centroidFrame==frame
                            &&distance(enemy,general.x,general.y)<=PRESSURE_RADIUS)ledger.visiblePressure+=number(enemy,"hp");
                }
                ledger.visibleHp.clear();ledger.visibleHp.putAll(currentHp);
            }else ledger.visibleHp.clear(); // A later reappearance cannot be called continuous damage.
            ledger.pressureRatio=ledger.ownHp>0?ledger.visiblePressure/ledger.ownHp:ledger.visiblePressure>0?Double.POSITIVE_INFINITY:0;
            ledger.unknownWindowOwnHpDecrease=ledger.damageWindowKnown?0:damage;ledger.unknownWindowWeakHpDecrease=ledger.damageWindowKnown?0:enemyDelta;
            if(damage>0||enemyDelta>0||deaths>0){if(ledger.damageWindowKnown)ledger.recent.add(new CombatLedger.Damage(now,damage,enemyDelta,deaths));ledger.event(!ledger.damageWindowKnown?"HP_DELTA_DETECTED_WINDOW_UNKNOWN":deaths>0?"EXPLICIT_OWN_DEATH_OBSERVED":damage>0?"CURRENT_OWN_HP_DECREASE":"WEAK_VISIBLE_HP_DECREASE",now);}
            while(!ledger.recent.isEmpty()&&now-ledger.recent.peek().time>DAMAGE_WINDOW_MS)ledger.recent.remove();
            ledger.recentOwnDamage=ledger.weakVisibleHpDecrease=0;int recentLosses=0;
            for(CombatLedger.Damage trend:ledger.recent){ledger.recentOwnDamage+=trend.own;ledger.weakVisibleHpDecrease+=trend.enemy;recentLosses+=trend.losses;}
            ledger.observedOwnDeaths+=deaths;
            List<Map<String,Object>> fighting=new ArrayList<Map<String,Object>>();for(Long actorId:general.members){GeneralRegistry.UnitView owner=registry.unit(actorId);Map<String,Object> actor=find(state,actorId);
                if(owned(general,owner)&&owner.healthRole==GeneralRegistry.HealthRole.NORMAL&&ready(actor))fighting.add(actor);}
            TacticalOpportunity.Exposure exposure=TacticalOpportunity.assess(fighting,items(state,"ownUnits"),ledger.currentVisibleKnown&&general.centroidKnown&&general.centroidFrame==frame?visible(enemies):Collections.<Map<String,Object>>emptyList(),
                    enemies==null?Collections.<String,Object>emptyMap():enemies,general.x,general.y,homeX,homeY);
            boolean previousAirNeed=ledger.airCapabilityNeed;ledger.currentAirThreatIds.clear();ledger.currentAirThreatIds.addAll(exposure.airIds);
            ledger.localExposureRatio=exposure.ratio;ledger.visibleStaticHp=exposure.staticHp;ledger.visibleAirHp=exposure.airHp;
            ledger.compatibleAirHp=exposure.compatibleAirHp;ledger.unknownAirHp=exposure.unknownAirHp;ledger.airCoverageKnown=exposure.airCoverageKnown;
            ledger.airCapabilityNeed=ledger.currentVisibleKnown&&exposure.airDeficit();ledger.retreatDistance=exposure.retreatDistance;ledger.supportHp=exposure.supportHp;
            if(previousAirNeed!=ledger.airCapabilityNeed)ledger.event(ledger.airCapabilityNeed?"CURRENT_VISIBLE_ANTI_AIR_CAPABILITY_NEEDED":"CURRENT_AIR_NEED_RELEASED_OR_UNKNOWN",now);
            FightEstimate worst=null;boolean allLocalKnown=true;
            if(ledger.currentVisibleKnown)for(Map<String,Object> target:visible(enemies))if(Boolean.TRUE.equals(target.get("canAttack"))&&distance(target,general.x,general.y)<=PRESSURE_RADIUS){FightEstimate fight=FightEstimate.estimate(fighting,target,visible(enemies),enemies);if(!fight.known)allLocalKnown=false;else if(worst==null||fight.expectedLossFraction>worst.expectedLossFraction)worst=fight;}
            ledger.pressureEvidence="UNKNOWN_PROFILE_HP_FALLBACK";ledger.localFightEstimate=worst==null?Collections.<String,Object>emptyMap():worst.view();
            if(allLocalKnown&&worst!=null){ledger.pressureRatio=worst.expectedLossFraction/FightEstimate.MAX_EXPECTED_LOSS_FRACTION*OVERMATCH_RATIO;ledger.pressureEvidence="KNOWN_STATIC_EXCHANGE_LOSS_SCALED_TO_PRESSURE_POLICY";}
            boolean overmatch=ledger.currentVisibleKnown&&ledger.visiblePressure>0&&ledger.pressureRatio>=OVERMATCH_RATIO;
            boolean defended=ledger.currentVisibleKnown&&exposure.staticHp>0&&(allLocalKnown&&worst!=null?!worst.admitted:exposure.ratio>=TacticalOpportunity.MAX_APPROACH_EXPOSURE);
            boolean losing=ledger.currentMembers>0&&(ledger.recentOwnDamage>=Math.max(1,ledger.ownMaxHp)*LOSING_DAMAGE_FRACTION
                    ||recentLosses>=2);
            // Visible HP/coverage proxies admit objectives; they are not evidence that this General is being hit.
            // A meaningful bounded own delta supplies engagement evidence without attributing it to a particular enemy.
            // A first chip hit retains objective caution/counter demand, without sending the whole General home.
            ledger.escapeDamageWitness=ledger.damageWindowKnown&&(ledger.recentOwnDamage>=Math.max(1,ledger.ownMaxHp)*ESCAPE_DAMAGE_FRACTION||recentLosses>0);
            boolean engagedDanger=ledger.escapeDamageWitness&&ledger.currentVisibleKnown&&(overmatch||defended||exposure.airBlocksAdvance());
            boolean calm=ledger.currentVisibleKnown&&ledger.damageWindowKnown&&!engagedDanger&&ledger.pressureRatio<=RECOVERY_RATIO&&ledger.recentOwnDamage<=Math.max(1,ledger.ownMaxHp)*.02;
            if(!ledger.retreating&&(losing||engagedDanger)){
                ledger.retreating=true;ledger.trigger=losing?"CURRENT_OWN_LOSS_EXCHANGE":ledger.airCapabilityNeed?"OWN_DAMAGE_WITH_CURRENT_AIR_COVERAGE_DEFICIT":defended?"OWN_DAMAGE_WITH_CURRENT_STATIC_EXPOSURE":"OWN_DAMAGE_WITH_CURRENT_HP_OVERMATCH";
                ledger.commandRevision++;
                ledger.change(losing?CombatLedger.Crisis.LOSING_EXCHANGE:CombatLedger.Crisis.OVERMATCHED,now);
                establishRally(ledger,general,state,enemies,homeX,homeY);ledger.regroupSince=-1;
            }else if(ledger.retreating){
                if(ledger.crisis==CombatLedger.Crisis.REGROUPING){
                    if(!calm){ledger.regroupSince=-1;ledger.change(CombatLedger.Crisis.RETREATING,now);}
                    else if(ledger.currentHealthy>0
                            &&ledger.ownHp>=ledger.ownMaxHp*.6&&now-ledger.regroupSince>=REGROUP_HOLD_MS){
                        ledger.retreating=false;ledger.commandRevision++;ledger.trigger="RECOVERY_CURRENT_OWN_RALLY_WITNESSED";ledger.change(CombatLedger.Crisis.NORMAL,now);
                    }
                }else if(calm&&rallyWitness(ledger,general,state,frame)){
                    ledger.regroupSince=now;ledger.change(CombatLedger.Crisis.REGROUPING,now);
                }else ledger.change(CombatLedger.Crisis.RETREATING,now);
            }else if(ledger.currentVisibleKnown)ledger.change(ledger.pressureRatio>=.75?CombatLedger.Crisis.PRESSURED:CombatLedger.Crisis.NORMAL,now);
            if(ledger.revision!=oldRevision)changes.add(fields("reason","GENERAL_COMBAT_OBSERVED","generalId",general.id.value,"before",before,"after",ledger.view(),"sourceFrame",frame,"gameTimeMs",now));
        }
        ledgers.keySet().retainAll(alive);
        missions.keySet().retainAll(alive);blockedFrontiers.keySet().retainAll(alive);
    }
    /** Refresh facts after same-frame ownership/roster transitions. The caller first refreshes
     * the registry centroid from actual remaining members. This is not another trend sample:
     * damage/death baselines, windows, crisis, rally and command revisions remain untouched. */
    public void refreshCurrentRoster(Map<String,Object> state,Map<String,Object> enemies,double homeX,double homeY,long now){
        if(state==null||!registry.currentOwnState(state)||!Objects.equals(Long.valueOf(now),GameClock.number(state.get("gameTimeMs"))))return;
        long frame=registry.observation().frame;boolean visibleCurrent=combatCurrent(state,enemies,now);
        List<Map<String,Object>> hazards=visibleCurrent?visible(enemies):Collections.<Map<String,Object>>emptyList();
        Map<String,Object> packet=enemies==null?Collections.<String,Object>emptyMap():enemies;
        for(GeneralRegistry.GeneralView general:registry.generals()){
            CombatLedger ledger=ledgers.get(general.id);if(ledger==null||ledger.frame!=frame||ledger.time!=now
                    ||!Objects.equals(ledger.session,state.get("sessionId"))||!Objects.equals(ledger.player,registry.observation().player))continue;
            Map<String,Object> before=ledger.view();ledger.ownHp=ledger.ownMaxHp=0;ledger.currentMembers=ledger.currentHealthy=0;
            List<Map<String,Object>> fighting=new ArrayList<Map<String,Object>>();
            for(Long id:general.members){GeneralRegistry.UnitView owner=registry.unit(id);Map<String,Object> actor=find(state,id);
                if(!owned(general,owner)||!ready(actor))continue;double hp=number(actor,"hp"),max=number(actor,"maxHp");boolean known=Double.isFinite(max)&&max>0;
                ledger.ownHp+=hp;ledger.ownMaxHp+=known?max:hp;ledger.currentMembers++;
                if(owner.healthRole==GeneralRegistry.HealthRole.NORMAL){fighting.add(actor);if(known&&hp>=max*.5)ledger.currentHealthy++;}
            }
            ledger.ownCurrent=true;ledger.currentVisibleKnown=visibleCurrent;ledger.currentThreatSignature=DestinationUnknownDefer.threatSignature(enemies,visibleCurrent);
            ledger.homeX=homeX;ledger.homeY=homeY;ledger.visiblePressure=0;
            boolean centroidCurrent=general.centroidKnown&&general.centroidFrame==frame;
            List<Map<String,Object>> localHazards=centroidCurrent?hazards:Collections.<Map<String,Object>>emptyList();
            for(Map<String,Object> enemy:localHazards)if(Boolean.TRUE.equals(enemy.get("canAttack"))&&distance(enemy,general.x,general.y)<=PRESSURE_RADIUS)ledger.visiblePressure+=number(enemy,"hp");
            ledger.pressureRatio=ledger.ownHp>0?ledger.visiblePressure/ledger.ownHp:ledger.visiblePressure>0?Double.POSITIVE_INFINITY:0;
            TacticalOpportunity.Exposure exposure=TacticalOpportunity.assess(fighting,items(state,"ownUnits"),localHazards,packet,general.x,general.y,homeX,homeY);
            ledger.currentAirThreatIds.clear();ledger.currentAirThreatIds.addAll(exposure.airIds);ledger.localExposureRatio=exposure.ratio;
            ledger.visibleStaticHp=exposure.staticHp;ledger.visibleAirHp=exposure.airHp;ledger.compatibleAirHp=exposure.compatibleAirHp;ledger.unknownAirHp=exposure.unknownAirHp;
            ledger.airCoverageKnown=exposure.airCoverageKnown;ledger.airCapabilityNeed=visibleCurrent&&exposure.airDeficit();ledger.retreatDistance=exposure.retreatDistance;ledger.supportHp=exposure.supportHp;
            FightEstimate worst=null;boolean allLocalKnown=true;
            for(Map<String,Object> target:localHazards)if(Boolean.TRUE.equals(target.get("canAttack"))&&distance(target,general.x,general.y)<=PRESSURE_RADIUS){
                FightEstimate fight=FightEstimate.estimate(fighting,target,hazards,packet);if(!fight.known)allLocalKnown=false;else if(worst==null||fight.expectedLossFraction>worst.expectedLossFraction)worst=fight;}
            ledger.pressureEvidence="UNKNOWN_PROFILE_HP_FALLBACK";ledger.localFightEstimate=worst==null?Collections.<String,Object>emptyMap():worst.view();
            if(allLocalKnown&&worst!=null){ledger.pressureRatio=worst.expectedLossFraction/FightEstimate.MAX_EXPECTED_LOSS_FRACTION*OVERMATCH_RATIO;ledger.pressureEvidence="KNOWN_STATIC_EXCHANGE_LOSS_SCALED_TO_PRESSURE_POLICY";}
            Map<String,Object> after=ledger.view();if(!before.equals(after))changes.add(fields("reason","GENERAL_CURRENT_ROSTER_REFRESH_NO_TREND_SAMPLE","generalId",general.id.value,
                    "before",before,"after",after,"sourceFrame",frame,"gameTimeMs",now,"trendSampleAdded",false));
        }
    }
    private void establishRally(CombatLedger ledger,GeneralRegistry.GeneralView general,Map<String,Object> state,Map<String,Object> enemies,double homeX,double homeY){
        if(!Double.isFinite(homeX)||!Double.isFinite(homeY)||!general.centroidKnown)return;
        double x=homeX,y=homeY;
        if(Math.hypot(general.x-homeX,general.y-homeY)<140&&ledger.currentVisibleKnown){
            double dx=0,dy=0;for(Map<String,Object> enemy:visible(enemies))if(Boolean.TRUE.equals(enemy.get("canAttack"))&&distance(enemy,general.x,general.y)<=PRESSURE_RADIUS){dx+=general.x-number(enemy,"x");dy+=general.y-number(enemy,"y");}
            double length=Math.hypot(dx,dy);if(length>0){x=Math.max(0,general.x+dx/length*360);y=Math.max(0,general.y+dy/length*360);}
        }
        Object rawMap=state.get("map");Map<?,?> map=rawMap instanceof Map?(Map<?,?>)rawMap:Collections.emptyMap();
        double width=numeric(map.get("width")),height=numeric(map.get("height"));
        ledger.rallyBoundsKnown=Double.isFinite(width)&&Double.isFinite(height)&&width>0&&height>0;
        // Native coordinates use float and strict <width/height. A world-unit margin avoids float rounding onto the boundary.
        if(ledger.rallyBoundsKnown){x=Math.max(0,Math.min(x,Math.max(0,width-1)));y=Math.max(0,Math.min(y,Math.max(0,height-1)));}
        else{x=Math.max(0,x);y=Math.max(0,y);}
        double[] clear=outsideBaseHold(state,ledger,x,y,general.x,general.y);
        ledger.rallyX=clear[0];ledger.rallyY=clear[1];ledger.rallyKnown=true;
    }
    private boolean rallyWitness(CombatLedger ledger,GeneralRegistry.GeneralView general,Map<String,Object> state,long frame){
        if(!ledger.rallyKnown||ledger.currentHealthy==0)return false;int witnesses=0;
        for(Long id:general.members){GeneralRegistry.UnitView owner=registry.unit(id);Map<String,Object> actor=find(state,id);Long receipt=ledger.retreatReceiptFrame.get(id);
            if(owned(general,owner)&&healthy(actor,owner)&&receipt!=null&&Objects.equals(ledger.retreatReceiptGeneration.get(id),owner.ownerGeneration)&&frame>receipt&&distance(actor,ledger.rallyX,ledger.rallyY)<=140)witnesses++;
        }
        return witnesses>=Math.max(1,(int)Math.ceil(ledger.currentHealthy*.75));
    }
    public void collect(Map<String,Object> state,Map<String,Object> enemies,long now)throws Exception{
        Long frame=GameClock.number(state==null?null:state.get("frame"));if(frame==null||!registry.currentOwnState(state))return;
        for(final GeneralRegistry.GeneralView general:registry.generals()){
            final CombatLedger ledger=ledgers.get(general.id);if(ledger==null||ledger.frame!=frame||ledger.time!=now||!Objects.equals(ledger.session,state.get("sessionId")))continue;
            if(!general.centroidKnown||general.centroidFrame!=frame)continue;
            List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();
            for(Long id:general.members){GeneralRegistry.UnitView owner=registry.unit(id);Map<String,Object> actor=find(state,id);
                if(owned(general,owner)&&owner.healthRole==GeneralRegistry.HealthRole.NORMAL&&ready(actor))actors.add(actor);}
            if(ledger.retreating){
                if(!ledger.rallyKnown||ledger.crisis==CombatLedger.Crisis.REGROUPING)continue;
                for(Map<String,Object> actor:actors){final long id=id(actor);if(!due(ledger,id,now,RETREAT_RETRY_MS))continue;
                    Long retreatFrame=ledger.retreatReceiptFrame.get(id),retreatGeneration=ledger.retreatReceiptGeneration.get(id);
                    Long acceptedRevision=ledger.acceptedRevision.get(id);DestinationUnknownDefer destination=ledger.unknownDestinationDefer.get(id);
                    // Individual current own arrival can hold while other members have not met the General regroup quorum.
                    // Exact current native destination or current idle arrival is useful evidence even when
                    // distant visible enemies change. Generation, real receipt frame and rally identity still apply.
                    boolean currentRetreatReceipt=retreatFrame!=null&&frame>retreatFrame&&retreatGeneration!=null&&retreatGeneration==registry.unit(id).ownerGeneration
                            &&Objects.equals(ledger.acceptedGeneration.get(id),registry.unit(id).ownerGeneration)&&acceptedRevision!=null&&acceptedRevision==ledger.commandRevision
                            &&destination!=null&&Math.hypot(destination.x-ledger.rallyX,destination.y-ledger.rallyY)<1;
                    Object currentOrder=actor.get("orderType");
                    if(currentRetreatReceipt&&(currentOrder==null||"idle".equals(currentOrder))&&distance(actor,ledger.rallyX,ledger.rallyY)<=140){ledger.lastOrderReuseEvidence="CURRENT_OWN_IDLE_AT_CURRENT_RALLY_POST_RECEIPT_OWNER_GENERATION";continue;}
                    if(currentRetreatReceipt&&"move".equals(currentOrder)&&distanceToOrder(actor,ledger.rallyX,ledger.rallyY)<1){ledger.lastOrderReuseEvidence="CURRENT_NATIVE_SAME_RALLY_POST_RECEIPT_OWNER_GENERATION";continue;}
                    if("move".equals(actor.get("orderType"))&&unknownDestinationDefer(ledger,actor,ledger.rallyX,ledger.rallyY,4000))continue;
                    final long generation=registry.unit(id).ownerGeneration,revision=ledger.commandRevision;
                    host.collect(new ForceController.Proposal(general.owner,Collections.singletonList(id),"/command/move?unitId="+id+"&x="+ledger.rallyX+"&y="+ledger.rallyY,"GENERAL_RETREAT",85,
                        ledger.trigger+":RALLY_GEOMETRY_SAFETY_UNKNOWN",receipt->{Long receiptFrame=GameClock.number(receipt.get("frame"));if(receiptFrame==null||receiptFrame<frame||!stillOwned(general,id,generation))return;
                            accepted(ledger,id,receipt,now,revision,ledger.rallyX,ledger.rallyY);ledger.retreatReceiptFrame.put(id,receiptFrame);ledger.retreatReceiptGeneration.put(id,generation);recordAccepted(ledger,general,receipt,"RETREAT_ACCEPTED_NOT_ARRIVED");}));
                }continue;
            }
            if(general.phase!=GeneralRegistry.Phase.ACTIVE||!combatCurrent(state,enemies,now))continue; // UNKNOWN keeps existing orders only.
            List<Map<String,Object>> hazards=new ArrayList<Map<String,Object>>(visible(enemies));List<Map<String,Object>> targets=new ArrayList<Map<String,Object>>();ledger.filteredUnassignedTargets=0;
            // Entry admission belongs to the accepted objective lifecycle. A later current own
            // position inside its fire range witnesses entry, not a kill or a native shot.
            // Ownership generations and current visibility/eligibility still apply every cycle.
            final Long engagedBase=engagedBase(ledger,actors,hazards,enemies,frame);
            // Commander allocates new remote work. An already entered current legal base is
            // this General's local combat lifecycle, until real withdrawal or contact/ownership
            // loss ends it; ordinary allocation churn cannot cancel it into an in-range hold.
            for(Map<String,Object> target:hazards){if(Objects.equals(engagedBase,id(target))||targetEligible(general,target))targets.add(target);else ledger.filteredUnassignedTargets++;}final Long priority=preferred.get(general.id);
            final Map<Long,Double> scores=new HashMap<Long,Double>();
            for(Map<String,Object> target:targets){TacticalOpportunity.Exposure risk=TacticalOpportunity.assess(actors,items(state,"ownUnits"),hazards,enemies,number(target,"x"),number(target,"y"),ledger.homeX,ledger.homeY);
                scores.put(id(target),distance(target,general.x,general.y)+risk.ratio*1000-TacticalOpportunity.value(target)
                        -(Objects.equals(priority,id(target))?120:0)-(Objects.equals(ledger.lastTarget,id(target))?100:0));}
            Collections.sort(targets,(a,b)->{if(engagedBase!=null){boolean ae=Objects.equals(engagedBase,id(a)),be=Objects.equals(engagedBase,id(b));if(ae!=be)return ae?-1:1;}int d=Double.compare(scores.get(id(a)),scores.get(id(b)));return d!=0?d:Long.compare(id(a),id(b));});
            boolean proposed=false;Map<String,Object> refused=null;
            for(Map<String,Object> target:targets){
                TacticalOpportunity.Exposure forceRisk=TacticalOpportunity.assess(actors,items(state,"ownUnits"),hazards,enemies,number(target,"x"),number(target,"y"),ledger.homeX,ledger.homeY);
                // Incompatibility cannot erase a visible air hazard and turn it into a frontier behind that hazard.
                if(forceRisk.airBlocksAdvance()){if(refused==null){refused=target;ledger.targetExposureRatio=forceRisk.ratio;ledger.tacticalReason="OBJECTIVE_CURRENT_AIR_COVERAGE_DEFICIT";}continue;}
                List<Map<String,Object>> eligible=host.eligible(actors,target,enemies);
                List<Long> ids=new ArrayList<Long>();if(eligible!=null)for(Map<String,Object> actor:eligible)if(contains(actors,id(actor))&&!ids.contains(id(actor)))ids.add(id(actor));
                if(ids.isEmpty()){if(Objects.equals(engagedBase,id(target))&&refused==null){refused=target;ledger.tacticalReason="CURRENT_BASE_SIEGE_NATIVE_ELIGIBILITY_LOST";}continue;}
                TacticalOpportunity.Exposure risk=TacticalOpportunity.assess(eligible,items(state,"ownUnits"),hazards,enemies,number(target,"x"),number(target,"y"),ledger.homeX,ledger.homeY);
                FightEstimate fight=FightEstimate.estimate(eligible,target,hazards,enemies);ledger.fightEstimate=fight.view();
                // The crisis observer already decides withdrawal from actual bounded own harm.
                // Recharging past damage as an approach cost while already engaged cancels a
                // viable siege and then restores it as that same window expires.
                boolean continuingSiege=Objects.equals(engagedBase,id(target));
                double damageCost=continuingSiege?0:ledger.recentOwnDamage/Math.max(1,ledger.ownMaxHp);
                if((fight.known?fight.expectedLossFraction+damageCost>=FightEstimate.MAX_EXPECTED_LOSS_FRACTION:risk.ratio+damageCost*2>=TacticalOpportunity.MAX_APPROACH_EXPOSURE)||risk.airBlocksAdvance()){
                    if(refused==null){refused=target;ledger.targetExposureRatio=fight.known?fight.expectedLossFraction:risk.ratio;ledger.tacticalReason=risk.airBlocksAdvance()?"OBJECTIVE_CURRENT_AIR_COVERAGE_DEFICIT":fight.reason;}continue;}
                Long blocked=baseZones.blocks(general.x,general.y,number(target,"x"),number(target,"y"),healthyGround(actors));
                // A remembered travel exclusion cannot veto a current, separately affordable siege.
                // Evaluate the blocking BASE, not a cheap target standing beside it. Lost contacts
                // retain the ordinary memory exclusion and never grant attack permission.
                if(blocked!=null)for(Map<String,Object> base:hazards)if(id(base)==blocked&&"commandCenter".equals(base.get("type"))){
                    List<Map<String,Object>> baseEligible=id(base)==id(target)?eligible:host.eligible(actors,base,enemies);
                    FightEstimate siege=baseEligible==null?null:FightEstimate.estimate(baseEligible,base,hazards,enemies);
                    TacticalOpportunity.Exposure baseRisk=baseEligible==null?null:TacticalOpportunity.assess(baseEligible,items(state,"ownUnits"),hazards,enemies,number(base,"x"),number(base,"y"),ledger.homeX,ledger.homeY);
                    // A current native-eligible siege uses the same known exchange or
                    // UNKNOWN-defender exposure admission as the ordinary objective.
                    // Memory is a travel veto; it cannot impose a second known-only
                    // fight gate. Unverified mechanics and lost contacts retain it.
                    boolean affordable=siege!=null&&!baseEligible.isEmpty()&&!baseRisk.airBlocksAdvance()
                            &&(siege.known?siege.expectedLossFraction+damageCost<FightEstimate.MAX_EXPECTED_LOSS_FRACTION
                            :CombatProfiles.trusted(enemies)&&siege.unknownActors==0&&siege.knownActors>0
                                &&baseRisk.ratio+damageCost*2<TacticalOpportunity.MAX_APPROACH_EXPOSURE);
                    if(affordable){
                        blocked=null;ledger.tacticalReason="CURRENT_BLOCKING_BASE_EXCHANGE_ADMITTED";
                    }
                    break;
                }
                if(blocked!=null){ledger.blockedBaseZone=blocked;if(refused==null){refused=target;ledger.tacticalReason="LEGAL_BASE_ZONE_TRAVEL_VETO";}continue;}
                if(!Objects.equals(ledger.lastTarget,id(target))){ledger.lastTarget=id(target);ledger.commandRevision++;ledger.event("CURRENT_VISIBLE_TASK_CHANGED",now);}
                setTacticalChoice(ledger,"ADVANCE",continuingSiege?"CURRENT_BASE_SIEGE_POST_RECEIPT_OWN_ENTRY_CONTINUATION":fight.known?fight.reason:"ADMITTED_CURRENT_NATIVE_ELIGIBLE_UNKNOWN_PROFILE_FALLBACK",fight.known?fight.expectedLossFraction:risk.ratio,now);
                ids.removeIf(id->!due(ledger,id,now,NORMAL_RETRY_MS)||matchingAdvance(ledger,find(state,id),number(target,"x"),number(target,"y"),id(target)));
                if(ids.isEmpty()){proposed=true;break;}
                attackChunks(general,ledger,ids,number(target,"x"),number(target,"y"),id(target),"CURRENT_VISIBLE_TARGET",frame,now);proposed=true;break;
            }
            if(proposed||actors.isEmpty())continue;
            if(commanderHold(general,ledger,actors,state,frame,now))continue;
            if(commander!=null&&ledger.lastTarget!=null){Map<String,Object> previous=null;for(Map<String,Object> target:hazards)if(Objects.equals(ledger.lastTarget,id(target)))previous=target;
                if(previous!=null&&!targetEligible(general,previous)){setTacticalChoice(ledger,"STANDOFF","COMMANDER_UNASSIGNED_REMOTE_TARGET_ORDER_CANCELLED",0,now);ledger.lastTarget=null;ledger.commandRevision++;List<Long> stop=new ArrayList<Long>();for(Map<String,Object> actor:actors)stop.add(id(actor));reposition(general,ledger,stop,general.x,general.y,null,state,frame,now);continue;}}
            if(refused!=null){double dx=general.x-number(refused,"x"),dy=general.y-number(refused,"y"),length=Math.hypot(dx,dy);
                // Keep useful native exploration if its geometry excludes the current visible hazards.
                // The exclusion radius is a policy margin, not a claimed weapon range or dynamic path safety proof.
                if(length>=TacticalOpportunity.STANDOFF_DISTANCE&&frontier(general,ledger,actors,state,enemies,hazards,frame,now,true))continue;
                setTacticalChoice(ledger,"STANDOFF",ledger.tacticalReason,ledger.targetExposureRatio,now);
                // A rejected defended objective must not fall through to an army frontier behind its defenses.
                // Being near the desired hold point does not cancel a previously accepted unsafe advance.
                // Install the replacement even inside the arrival tolerance, then require a later receipt/order witness to reuse it.
                if(length<1){dx=ledger.homeX-general.x;dy=ledger.homeY-general.y;length=Math.hypot(dx,dy);if(length<1){dx=1;dy=0;length=1;}}
                double rawX=number(refused,"x")+dx/length*TacticalOpportunity.STANDOFF_DISTANCE,
                        rawY=number(refused,"y")+dy/length*TacticalOpportunity.STANDOFF_DISTANCE;
                // Already outside the cautious margin: cancel the denied advance at the current own centroid.
                if(length>=TacticalOpportunity.STANDOFF_DISTANCE){rawX=general.x;rawY=general.y;}
                final double[] point=boundedPoint(state,rawX,rawY);double x=point[0],y=point[1];
                    if(!Objects.equals(ledger.lastTarget,id(refused))||!"STANDOFF".equals(ledger.commandMode)){ledger.lastTarget=id(refused);ledger.commandRevision++;}
                    List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:actors)if(due(ledger,id(actor),now,NORMAL_RETRY_MS)&&!matchingAdvance(ledger,actor,x,y,id(refused)))ids.add(id(actor));
                    reposition(general,ledger,ids,x,y,id(refused),state,frame,now);
                continue;}
            if(!frontier(general,ledger,actors,state,enemies,hazards,frame,now,false)&&baseClearance(ledger,general.x,general.y)<0){
                setTacticalChoice(ledger,"STANDOFF","BASE_OBJECTIVE_UNAVAILABLE_LEAVE_KNOWN_FIRE_ZONE",0,now);ledger.lastTarget=null;
                List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:actors)ids.add(id(actor));reposition(general,ledger,ids,general.x,general.y,null,state,frame,now);
            }
        }
    }
    /** Existing accepted ADVANCE plus a post-receipt current owned member in base range.
     * A receipt alone, ghost target, foreign roster, or old generation cannot start continuity. */
    private Long engagedBase(CombatLedger ledger,List<Map<String,Object>> actors,List<Map<String,Object>> targets,Map<String,Object> enemies,long frame){
        if(ledger.lastTarget==null||!"ADVANCE".equals(ledger.commandMode)||!"ADVANCE".equals(ledger.tacticalChoice)||!CombatProfiles.trusted(enemies))return null;
        for(Map<String,Object> target:targets)if(Objects.equals(ledger.lastTarget,id(target))&&"commandCenter".equals(target.get("type"))&&Boolean.TRUE.equals(target.get("canAttack"))){
            CombatProfiles.Profile profile=CombatProfiles.get("commandCenter",true);
            for(Map<String,Object> actor:actors){long actorId=id(actor);GeneralRegistry.UnitView owner=registry.unit(actorId);Long acceptedFrame=ledger.acceptedFrame.get(actorId);
                if(acceptedFrame!=null&&frame>acceptedFrame&&owner!=null&&Objects.equals(ledger.acceptedGeneration.get(actorId),owner.ownerGeneration)
                        &&Objects.equals(ledger.acceptedRevision.get(actorId),ledger.commandRevision)
                        &&distance(actor,number(target,"x"),number(target,"y"))<=profile.range)return id(target);
            }
        }return null;
    }
    private boolean frontier(final GeneralRegistry.GeneralView general,final CombatLedger ledger,List<Map<String,Object>> actors,Map<String,Object> state,Map<String,Object> enemies,List<Map<String,Object>> targets,long frame,long now,boolean cautious)throws Exception{
        if(actors.isEmpty())return false;
        OutwardMission found=missions.get(general.id);
        if(found==null||found.ledger!=ledger){found=new OutwardMission(ledger);missions.put(general.id,found);blockedFrontiers.remove(general.id);}
        final OutwardMission mission=found;
        LinkedHashMap<Integer,Long> avoid=blockedFrontiers.get(general.id);
        if(avoid==null){avoid=new LinkedHashMap<Integer,Long>();blockedFrontiers.put(general.id,avoid);}
        final int ground=healthyGround(actors);
        if(ground>BaseThreatZones.SMALL_FORCE_MAXIMUM)avoid.entrySet().removeIf(entry->entry.getValue()>=0);
        Map<String,Object> anchor=actors.get(0);String anchorType=(String)anchor.get("type");
        if(mission.active){
            int arrived=0,compatible=0;
            for(Map<String,Object> actor:actors)if(sameMovement(actor,mission.anchorType)){
                compatible++;if(distance(actor,mission.x,mission.y)<=140)arrived++;
            }
            boolean arrival=frame>mission.acceptedFrame&&arrived>=Math.max(1,(int)Math.ceil(compatible*.75));
            if(arrival||compatible==0){
                // Own arrival covers the accepted waypoint only. The planner's
                // information frontier may still be far beyond this route step.
                // Completing that outer tile here strands later legal steps.
                if(arrival)avoidFrontier(avoid,mission.tile,-1L);mission.active=false;
                mission.event=arrival?"CURRENT_OWN_LATER_FRONTIER_ARRIVAL":"ANCHOR_TYPE_NO_LONGER_ATTACHED";
                ledger.event(mission.event,now);
            }else{
                Long blocked=baseZones.blocks(general.x,general.y,mission.x,mission.y,ground);
                if(blocked==null&&frontierPermitted(actors,state,enemies,targets,general,ledger,mission.x,mission.y,cautious)){
                    if(ledger.lastTarget!=null){ledger.lastTarget=null;ledger.commandRevision++;}
                    String reason="RESUME_ACCEPTED_NATIVE_OUTWARD_FRONTIER_DYNAMIC_SAFETY_UNKNOWN";
                    setTacticalChoice(ledger,"FRONTIER",reason,0,now);
                    List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:actors)if(sameMovement(actor,mission.anchorType)&&due(ledger,id(actor),now,NORMAL_RETRY_MS)&&!matchingAdvance(ledger,actor,mission.x,mission.y,null))ids.add(id(actor));
                    attackChunks(general,ledger,ids,mission.x,mission.y,null,reason,frame,now);return true;
                }
                if(blocked==null)return false; // Current tactical hazard pauses the mission; it does not erase direction.
                ledger.blockedBaseZone=blocked;avoidFrontier(avoid,mission.tile,blocked);avoidFrontier(avoid,mission.frontierTile,blocked);mission.active=false;mission.event="SMALL_FORCE_BASE_ZONE_TRAVEL_VETO";
            }
        }
        Long blocked=null;Map<String,Object> detour=null;Set<String> requestedCandidates=new HashSet<String>();
        for(int attempt=0;attempt<3;attempt++){
            StringBuilder csv=new StringBuilder();for(Integer tile:avoid.keySet()){if(csv.length()>0)csv.append(',');csv.append(tile);}
            Map<String,Object> plan=host.read("/scout/plan?role=army&unitId="+id(anchor)+"&avoid="+csv,"g5_general_frontier_plan");
            // Outward is a preference. If bounded alternatives exhaust, retain a legally planned
            // detour instead of making heading another permanent gate on travel.
            if(detour!=null&&(plan==null||!"planned".equals(plan.get("status"))||attempt==2&&(GameClock.number(plan.get("targetTile"))==null
                    ||avoid.containsKey(((Number)plan.get("targetTile")).intValue())||Math.hypot(number(plan,"targetX")-general.x,number(plan,"targetY")-general.y)<=180))){plan=detour;attempt=2;}
            if(plan==null||!"planned".equals(plan.get("status"))||!Boolean.TRUE.equals(plan.get("pathKnown"))||GameClock.number(plan.get("targetTile"))==null||!finite(plan.get("targetX"))||!finite(plan.get("targetY"))
                    ||plan.containsKey("sessionId")&&!Objects.equals(plan.get("sessionId"),ledger.session))break;
            Long candidateOuter=GameClock.number(plan.get("frontierTile"));
            String candidate=plan.get("targetTile")+":"+(candidateOuter==null?plan.get("targetTile"):candidateOuter);
            // Exhaustive native fallback can intentionally ignore soft avoid.
            // Repeating its identical candidate cannot resolve this search.
            // Keep a permitted native detour, otherwise wait for the next actual
            // observation rather than spending a third query on the same result.
            if(!requestedCandidates.add(candidate)){
                if(detour!=null){plan=detour;attempt=2;}
                else{mission.event="NATIVE_FRONTIER_CANDIDATE_REPEATED_WITHIN_COLLECT";break;}
            }
            final int tile=((Number)plan.get("targetTile")).intValue();Long outer=GameClock.number(plan.get("frontierTile"));final int frontierTile=outer==null?tile:outer.intValue();
            final double x=number(plan,"targetX"),y=number(plan,"targetY"),dx=x-general.x,dy=y-general.y;
            // Select only coordinates provided by the existing native planner. Direction is own travel memory,
            // not an inferred enemy base. Completed/near/backward candidates ask the same planner for another edge.
            boolean near=Math.hypot(dx,dy)<=180;
            boolean backward=mission.directionKnown&&dx*mission.directionX+dy*mission.directionY < -140;
            boolean chosenDetour=attempt==2&&plan==detour;
            if(backward&&!near&&!avoid.containsKey(tile)&&!avoid.containsKey(frontierTile)&&baseZones.blocks(general.x,general.y,x,y,ground)==null
                    &&frontierPermitted(actors,state,enemies,targets,general,ledger,x,y,cautious))detour=plan;
            if(near||backward&&attempt<2||!chosenDetour&&(avoid.containsKey(tile)||avoid.containsKey(frontierTile))){
                avoidFrontier(avoid,tile,-1L);mission.event=near?"NATIVE_NEAR_FRONTIER_SKIPPED":backward?"NATIVE_BACKTRACK_FRONTIER_SKIPPED":"NATIVE_COMPLETED_FRONTIER_SKIPPED";continue;
            }
            blocked=baseZones.blocks(general.x,general.y,x,y,ground);
            if(blocked!=null){ledger.blockedBaseZone=blocked;avoidFrontier(avoid,tile,blocked);avoidFrontier(avoid,frontierTile,blocked);continue;}
            if(!frontierPermitted(actors,state,enemies,targets,general,ledger,x,y,cautious)){avoidFrontier(avoid,tile,-1L);avoidFrontier(avoid,frontierTile,-1L);continue;}
            if(ledger.lastTarget!=null){ledger.lastTarget=null;ledger.commandRevision++;}
            String reason=cautious?"NATIVE_KNOWN_FRONTIER_OUTSIDE_VISIBLE_HAZARDS_PATH_SAFETY_UNKNOWN":backward?"NATIVE_OUTWARD_MISSION_TERRAIN_DETOUR":"NATIVE_KNOWN_ANCHOR_FRONTIER";
            setTacticalChoice(ledger,"FRONTIER",reason,0,now);
            if(!ledger.commandPointKnown||!"FRONTIER".equals(ledger.commandMode)||Math.hypot(x-ledger.commandX,y-ledger.commandY)>140)ledger.commandRevision++;
            List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:actors)if(sameMovement(actor,anchorType)&&due(ledger,id(actor),now,NORMAL_RETRY_MS)&&!matchingAdvance(ledger,actor,x,y,null))ids.add(id(actor));
            attackChunks(general,ledger,ids,x,y,null,reason,frame,now,receipt->{
                if(!mission.directionKnown){double ox=x-ledger.homeX,oy=y-ledger.homeY,length=Math.hypot(ox,oy);if(length<1){ox=dx;oy=dy;length=Math.hypot(ox,oy);}if(length>0&&Double.isFinite(length)){mission.directionX=ox/length;mission.directionY=oy/length;mission.directionKnown=true;}}
                mission.x=x;mission.y=y;mission.tile=tile;mission.frontierTile=frontierTile;mission.anchorType=anchorType;mission.active=true;mission.acceptedFrame=GameClock.number(receipt.get("frame"));mission.event="NATIVE_FRONTIER_ACCEPTED_NOT_ARRIVED";
            });return true;
        }
        if(blocked!=null){setTacticalChoice(ledger,"STANDOFF","LEGAL_BASE_ZONE_FRONTIER_TRAVEL_VETO",0,now);if(ledger.lastTarget!=null){ledger.lastTarget=null;ledger.commandRevision++;}List<Long> stop=new ArrayList<Long>();for(Map<String,Object> actor:actors)if(due(ledger,id(actor),now,NORMAL_RETRY_MS)&&!matchingAdvance(ledger,actor,general.x,general.y,null))stop.add(id(actor));reposition(general,ledger,stop,general.x,general.y,null,state,frame,now);return true;}
        return false;
    }
    private boolean frontierPermitted(List<Map<String,Object>> actors,Map<String,Object> state,Map<String,Object> enemies,List<Map<String,Object>> targets,GeneralRegistry.GeneralView general,CombatLedger ledger,double x,double y,boolean cautious){
        if(!cautious)return true;TacticalOpportunity.Exposure risk=TacticalOpportunity.assess(actors,items(state,"ownUnits"),targets,enemies,x,y,ledger.homeX,ledger.homeY);
        return !risk.airBlocksAdvance()&&risk.ratio<TacticalOpportunity.MAX_APPROACH_EXPOSURE&&TacticalOpportunity.excludesVisibleHazards(targets,general.x,general.y,x,y);
    }
    private static void avoidFrontier(LinkedHashMap<Integer,Long> avoid,int tile,long zone){if(tile<0)return;avoid.remove(tile);avoid.put(tile,zone);while(avoid.size()>48)avoid.remove(avoid.keySet().iterator().next());}
    /** Same movement domain keeps ordinary mixed tank rosters moving. An anchor
     * plan is still only anchor evidence; native guards handle each real actor. */
    private static boolean sameMovement(Map<String,Object> actor,String anchorType){String type=(String)actor.get("type");
        if(Objects.equals(type,anchorType))return true;String movement=TargetCatalog.movementType(type);
        return movement!=null&&movement.equals(TargetCatalog.movementType(anchorType));}
    private int healthyGround(List<Map<String,Object>> actors){int count=0;for(Map<String,Object> actor:actors){String movement=TargetCatalog.movementType((String)actor.get("type"));if(healthy(actor,registry.unit(id(actor)))&&("LAND".equals(movement)||"HOVER".equals(movement)))count++;}return count;}
    private boolean targetEligible(GeneralRegistry.GeneralView general,Map<String,Object> target){return commander==null||commander.allowsTarget(general.id,id(target))||distance(target,general.x,general.y)<=220;}
    private boolean commanderHold(GeneralRegistry.GeneralView general,CombatLedger ledger,List<Map<String,Object>> actors,Map<String,Object> state,long frame,long now)throws Exception{if(commander==null)return false;Map<String,Object> hold=commander.holdForGeneral(general.id);if(!Boolean.TRUE.equals(hold.get("active"))||!finite(hold.get("x"))||!finite(hold.get("y")))return false;double[] point=boundedPoint(state,number(hold,"x"),number(hold,"y"));
        setTacticalChoice(ledger,"STANDOFF","COMMANDER_LOST_CONTACT_BOUNDED_HOLD_NO_ATTACK_PERMISSION",0,now);if(ledger.lastTarget!=null){ledger.lastTarget=null;ledger.commandRevision++;}List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:actors)if(due(ledger,id(actor),now,NORMAL_RETRY_MS)&&!matchingAdvance(ledger,actor,point[0],point[1],null))ids.add(id(actor));reposition(general,ledger,ids,point[0],point[1],null,state,frame,now);return true;}
    private void reposition(final GeneralRegistry.GeneralView general,final CombatLedger ledger,List<Long> actors,final double x,final double y,final Long target,Map<String,Object> state,final long frame,final long now)throws Exception{
        // Keep an accepted hold stable across small centroid drift, and do not spend a token to tell
        // a stationary unit to stand where it already is. A revoked advance still needs one cancellation.
        final double[] point=outsideBaseHold(state,ledger,x,y,general.x,general.y);
        final boolean existingHold=ledger.commandPointKnown&&"STANDOFF".equals(ledger.commandMode)&&Math.hypot(point[0]-ledger.commandX,point[1]-ledger.commandY)<=140
                &&baseClearance(ledger,ledger.commandX,ledger.commandY)>=0;
        final double holdX=existingHold?ledger.commandX:point[0],holdY=existingHold?ledger.commandY:point[1];
        if(!existingHold)ledger.commandRevision++;
        final long revision=ledger.commandRevision;
        for(final Long id:actors){final long generation=registry.unit(id).ownerGeneration;
            Map<String,Object> actor=find(state,id);
            if(actor!=null){Object order=actor.get("orderType");
                if("move".equals(order)&&finite(actor.get("orderX"))&&finite(actor.get("orderY"))&&distanceToOrder(actor,holdX,holdY)<=140
                        &&baseClearance(ledger,number(actor,"orderX"),number(actor,"orderY"))>=0)continue;
                if((order==null||"idle".equals(order))&&distance(actor,holdX,holdY)<=140&&baseClearance(ledger,number(actor,"x"),number(actor,"y"))>=0&&(!ledger.acceptedAt.containsKey(id)||"STANDOFF".equals(ledger.commandMode)))continue;
            }
            host.collect(new ForceController.Proposal(general.owner,Collections.singletonList(id),"/command/move?unitId="+id+"&x="+holdX+"&y="+holdY,"GENERAL",30,"TACTICAL_STANDOFF_GEOMETRY_SAFETY_UNKNOWN",
                receipt->{Long receiptFrame=GameClock.number(receipt.get("frame"));if(receiptFrame==null||receiptFrame<frame||!stillOwned(general,id,generation))return;
                    accepted(ledger,id,receipt,now,revision,holdX,holdY);ledger.commandPointKnown=true;ledger.commandX=holdX;ledger.commandY=holdY;ledger.commandMode="STANDOFF";
                    registry.updateGeneralGoal(general.id,holdX,holdY,target);recordAccepted(ledger,general,receipt,"STANDOFF_MOVE_ACCEPTED_NOT_ARRIVED");}));
        }
    }
    /** A denied/lost objective cannot install a hold inside its legally witnessed base
     * weapon circle. Memory supplies travel geometry only; it never grants attack permission.
     * Prefer the requested hold if clear, otherwise the nearest clear bounded candidate.
     * The native MOVE guard still owns reachability and dynamic path safety remains UNKNOWN. */
    private static double[] outsideBaseHold(Map<String,Object> state,CombatLedger ledger,double x,double y,double currentX,double currentY){
        double[] requested=boundedPoint(state,x,y);if(baseClearance(ledger,requested[0],requested[1])>=0)return requested;
        List<double[]> candidates=new ArrayList<double[]>();candidates.add(requested);candidates.add(boundedPoint(state,currentX,currentY));
        candidates.add(boundedPoint(state,ledger.homeX,ledger.homeY));
        for(Map<String,Object> zone:ledger.baseThreatZones){double zx=number(zone,"x"),zy=number(zone,"y"),radius=holdRadius(zone)+1;
            double angle=Math.atan2(y-zy,x-zx);
            for(int step=0;step<16;step++){double a=angle+step*Math.PI/8;candidates.add(boundedPoint(state,zx+Math.cos(a)*radius,zy+Math.sin(a)*radius));}
        }
        Object raw=state.get("map");Map<?,?> map=raw instanceof Map?(Map<?,?>)raw:Collections.emptyMap();
        double width=numeric(map.get("width")),height=numeric(map.get("height"));
        if(width>0&&height>0&&Double.isFinite(width)&&Double.isFinite(height))for(double cx:new double[]{0,width-1})for(double cy:new double[]{0,height-1})candidates.add(new double[]{cx,cy});
        double[] best=requested;double bestClearance=baseClearance(ledger,best[0],best[1]),bestDistance=Double.POSITIVE_INFINITY;
        for(double[] candidate:candidates){double clearance=baseClearance(ledger,candidate[0],candidate[1]),distance=Math.hypot(candidate[0]-requested[0],candidate[1]-requested[1]);
            if(clearance>=0?(bestClearance<0||distance<bestDistance):bestClearance<0&&clearance>bestClearance){best=candidate;bestClearance=clearance;bestDistance=distance;}}
        return best;
    }
    private static double holdRadius(Map<String,Object> zone){return number(zone,"range")+("VERIFIED_STATIC_MAX_RANGE".equals(zone.get("rangeEvidence"))?140:60);}
    private static double baseClearance(CombatLedger ledger,double x,double y){double clearance=Double.POSITIVE_INFINITY;
        // Existing stationary points outside the evidenced weapon/policy circle are valid.
        // New escape candidates add arrival tolerance; that extra margin must not manufacture
        // a fresh threat for a force already safely holding outside the circle.
        for(Map<String,Object> zone:ledger.baseThreatZones)clearance=Math.min(clearance,Math.hypot(x-number(zone,"x"),y-number(zone,"y"))-number(zone,"range"));return clearance;}
    private void attackChunks(final GeneralRegistry.GeneralView general,final CombatLedger ledger,List<Long> actors,final double x,final double y,final Long target,String reason,final long frame,final long now)throws Exception{
        attackChunks(general,ledger,actors,x,y,target,reason,frame,now,null);
    }
    private void attackChunks(final GeneralRegistry.GeneralView general,final CombatLedger ledger,List<Long> actors,final double x,final double y,final Long target,String reason,final long frame,final long now,final java.util.function.Consumer<Map<String,Object>> onAccepted)throws Exception{
        final String mode=target==null?"FRONTIER":"ADVANCE";
        if(!ledger.commandPointKnown||!mode.equals(ledger.commandMode)||Math.hypot(x-ledger.commandX,y-ledger.commandY)>140)ledger.commandRevision++;
        for(int start=0;start<actors.size();start+=48){final List<Long> chunk=new ArrayList<Long>(actors.subList(start,Math.min(start+48,actors.size())));final Map<Long,Long> generations=new HashMap<Long,Long>();
            for(Long id:chunk)generations.put(id,registry.unit(id).ownerGeneration);final long revision=ledger.commandRevision;
            StringBuilder ids=new StringBuilder();for(Long id:chunk){if(ids.length()>0)ids.append(',');ids.append(id);}
            host.collect(new ForceController.Proposal(general.owner,chunk,"/command/attack-move?unitIds="+ids+"&x="+x+"&y="+y,"GENERAL",30,reason,
                receipt->{Long receiptFrame=GameClock.number(receipt.get("frame"));if(receiptFrame==null||receiptFrame<frame)return;for(Long id:chunk)if(!stillOwned(general,id,generations.get(id)))return;
                    for(Long id:chunk)accepted(ledger,id,receipt,now,revision,x,y);ledger.commandPointKnown=true;ledger.commandX=x;ledger.commandY=y;ledger.commandMode=mode;
                    registry.updateGeneralGoal(general.id,x,y,target);if(onAccepted!=null)onAccepted.accept(receipt);recordAccepted(ledger,general,receipt,"ATTACK_ACCEPTED_NOT_EXECUTED");}));
        }
    }
    private void setTacticalChoice(CombatLedger ledger,String choice,String reason,double ratio,long now){boolean modeChanged=!choice.equals(ledger.tacticalChoice),changed=modeChanged||!reason.equals(ledger.tacticalReason);Map<String,Object> before=changed?ledger.view():null;
        if(modeChanged)ledger.commandRevision++;
        ledger.tacticalChoice=choice;ledger.tacticalReason=reason;ledger.targetExposureRatio=ratio;if(changed){ledger.event("TACTICAL_"+choice+":"+reason,now);
            changes.add(fields("reason","GENERAL_TACTICAL_ADMISSION","generalId",ledger.id.value,"before",before,"after",ledger.view(),"sourceFrame",ledger.frame,"gameTimeMs",now));}}
    private boolean matchingAdvance(CombatLedger ledger,Map<String,Object> actor,double x,double y,Long target){if(actor==null||!ledger.acceptedAt.containsKey(id(actor))||!ledger.commandPointKnown||!Objects.equals(ledger.lastTarget,target))return false;
        String mode=ledger.tacticalChoice;Long acceptedRevision=ledger.acceptedRevision.get(id(actor));
        if("STANDOFF".equals(mode)&&baseClearance(ledger,ledger.commandX,ledger.commandY)<0)return false;
        if(acceptedRevision==null||acceptedRevision!=ledger.commandRevision||!mode.equals(ledger.commandMode)||Math.hypot(x-ledger.commandX,y-ledger.commandY)>140)return false;
        GeneralRegistry.UnitView owner=registry.unit(id(actor));Long receiptFrame=ledger.acceptedFrame.get(id(actor));
        if(owner==null||!Objects.equals(ledger.acceptedGeneration.get(id(actor)),owner.ownerGeneration)||receiptFrame==null||ledger.frame<=receiptFrame)return false;
        Object order=actor.get("orderType");return ("STANDOFF".equals(mode)?"move".equals(order):"attackMove".equals(order)||"attack-move".equals(order)||"attack_move".equals(order))
                &&(distanceToOrder(actor,ledger.commandX,ledger.commandY)<=140||Math.hypot(x-ledger.commandX,y-ledger.commandY)<1&&unknownDestinationDefer(ledger,actor,ledger.commandX,ledger.commandY,6000))
                ||order==null&&distance(actor,ledger.commandX,ledger.commandY)<=140;}
    private boolean unknownDestinationDefer(CombatLedger ledger,Map<String,Object> actor,double x,double y,long hardWindow){long id=id(actor);DestinationUnknownDefer pending=ledger.unknownDestinationDefer.get(id);Long revision=ledger.acceptedRevision.get(id);
        if(!currentReceiptProgress(ledger,id)||revision==null||revision!=ledger.commandRevision||pending==null||Math.hypot(pending.x-x,pending.y-y)>1
                ||!pending.allows(actor,ledger.frame,ledger.time,hardWindow,2500,ledger.currentThreatSignature))return false;
        ledger.lastOrderReuseEvidence=DestinationUnknownDefer.EVIDENCE;ledger.destinationUnknownDeferCount++;return true;}
    private boolean currentReceiptProgress(CombatLedger ledger,long id){GeneralRegistry.UnitView owner=registry.unit(id);Long generation=ledger.acceptedGeneration.get(id),receiptFrame=ledger.acceptedFrame.get(id),progressAt=ledger.progressAt.get(id);
        DestinationUnknownDefer pending=ledger.unknownDestinationDefer.get(id);
        return owner!=null&&generation!=null&&generation==owner.ownerGeneration&&receiptFrame!=null&&ledger.frame>receiptFrame&&progressAt!=null&&ledger.time-progressAt<=15000
                &&(pending==null||Objects.equals(pending.threat,ledger.currentThreatSignature));}
    private static double[] boundedPoint(Map<String,Object> state,double x,double y){Object raw=state.get("map");Map<?,?> map=raw instanceof Map?(Map<?,?>)raw:Collections.emptyMap();double width=numeric(map.get("width")),height=numeric(map.get("height"));
        x=Math.max(0,x);y=Math.max(0,y);if(Double.isFinite(width)&&width>0)x=Math.min(x,Math.max(0,width-1));if(Double.isFinite(height)&&height>0)y=Math.min(y,Math.max(0,height-1));return new double[]{x,y};}
    private void recordAccepted(CombatLedger ledger,GeneralRegistry.GeneralView general,Map<String,Object> receipt,String reason){changes.add(fields("reason",reason,"generalId",general.id.value,"owner",general.owner,"receiptFrame",receipt.get("frame"),"receiptGameTimeMs",receipt.get("gameTimeMs"),"after",view(general.id)));}
    private void accepted(CombatLedger ledger,long id,Map<String,Object> receipt,long now,long revision,double x,double y){Long time=GameClock.number(receipt.get("gameTimeMs"));long acceptedTime=time!=null&&time>=now?time:now;ledger.acceptedAt.put(id,acceptedTime);ledger.acceptedRevision.put(id,revision);
        ledger.acceptedGeneration.put(id,registry.unit(id).ownerGeneration);ledger.acceptedFrame.put(id,GameClock.number(receipt.get("frame")));ledger.progressAt.put(id,now);
        ledger.unknownDestinationDefer.put(id,new DestinationUnknownDefer(x,y,acceptedTime,GameClock.number(receipt.get("frame")),ledger.currentThreatSignature));}
    private boolean due(CombatLedger ledger,long id,long now,long interval){Long accepted=ledger.acceptedAt.get(id),revision=ledger.acceptedRevision.get(id),generation=ledger.acceptedGeneration.get(id);GeneralRegistry.UnitView owner=registry.unit(id);
        DestinationUnknownDefer pending=ledger.unknownDestinationDefer.get(id);boolean threatChanged=pending!=null&&!Objects.equals(pending.threat,ledger.currentThreatSignature);
        return threatChanged||owner==null||generation==null||generation!=owner.ownerGeneration||accepted==null||revision==null||revision!=ledger.commandRevision||now-accepted>=interval;}
    private boolean stillOwned(GeneralRegistry.GeneralView general,long id,long generation){GeneralRegistry.UnitView unit=registry.unit(id);return owned(general,unit)&&unit.ownerGeneration==generation;}
    private static boolean owned(GeneralRegistry.GeneralView general,GeneralRegistry.UnitView unit){return unit!=null&&unit.membership==GeneralRegistry.Membership.ATTACHED&&general.id.equals(unit.generalId)&&unit.temporaryTask==GeneralRegistry.TemporaryTask.NONE&&unit.externalOwner==null&&general.owner.equals(unit.owner);}
    private static double distanceToOrder(Map<String,Object> unit,double x,double y){return Math.hypot(number(unit,"orderX")-x,number(unit,"orderY")-y);}
    boolean combatCurrent(Map<String,Object> state,Map<String,Object> enemies,long now){Long source=GameClock.number(enemies==null?null:enemies.get("gameTimeMs"));Long frame=GameClock.number(state==null?null:state.get("frame")),enemyFrame=GameClock.number(enemies==null?null:enemies.get("frame"));
        if(!(state!=null&&enemies!=null&&state.get("sessionId") instanceof String&&state.get("sessionId").equals(enemies.get("sessionId"))&&source!=null&&source>=now&&(enemyFrame==null||frame!=null&&enemyFrame>=frame)&&enemies.get("visibleEnemies") instanceof List))return false;
        String ownPlayer=registry.observation().player;
        if(enemies.containsKey("playerKey")&&!Objects.equals(ownPlayer,enemies.get("playerKey")))return false;
        Object player=enemies.get("player");if(player instanceof Map&&((Map<?,?>)player).containsKey("teamId")){
            Object team=((Map<?,?>)player).get("teamId");if(!(team instanceof Number)||!ownPlayer.equals("team:"+((Number)team).longValue()))return false;
        }
        // A malformed/stale visible row cannot turn a partial packet into evidence of calm.
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies"))if(!Objects.equals(source,GameClock.number(enemy.get("lastSeenGameTimeMs")))||id(enemy)<0||!finite(enemy.get("x"))||!finite(enemy.get("y"))||!(number(enemy,"hp")>0||number(enemy,"hp")==0&&Boolean.TRUE.equals(enemy.get("dead"))))return false;
        return true;}
    static List<Map<String,Object>> visible(Map<String,Object> enemies){List<Map<String,Object>> current=new ArrayList<Map<String,Object>>();Long source=GameClock.number(enemies==null?null:enemies.get("gameTimeMs"));if(source==null)return current;
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies")){Long seen=GameClock.number(enemy.get("lastSeenGameTimeMs"));if(seen!=null&&seen.equals(source)&&id(enemy)>=0&&finite(enemy.get("x"))&&finite(enemy.get("y"))&&number(enemy,"hp")>0&&!Boolean.TRUE.equals(enemy.get("dead")))current.add(enemy);}return current;}
    private static boolean contains(List<Map<String,Object>> actors,long id){for(Map<String,Object> actor:actors)if(id(actor)==id)return true;return false;}
    private static boolean ready(Map<String,Object> unit){return unit!=null&&finite(unit.get("x"))&&finite(unit.get("y"))&&number(unit,"hp")>0&&!Boolean.TRUE.equals(unit.get("dead"))&&number(unit,"buildProgress")>=1;}
    private static boolean healthy(Map<String,Object> unit,GeneralRegistry.UnitView owner){return ready(unit)&&owner!=null&&owner.healthRole==GeneralRegistry.HealthRole.NORMAL&&number(unit,"maxHp")>0&&number(unit,"hp")>=number(unit,"maxHp")*.5;}
    private static double distance(Map<String,Object> unit,double x,double y){return Math.hypot(number(unit,"x")-x,number(unit,"y")-y);}
    private static double number(Map<String,Object> map,String key){Object value=map==null?null:map.get(key);return value instanceof Number?((Number)value).doubleValue():Double.NaN;}
    private static boolean finite(Object value){return value instanceof Number&&Double.isFinite(((Number)value).doubleValue());}
    private static double numeric(Object value){return value instanceof Number?((Number)value).doubleValue():Double.NaN;}
    private static long id(Map<String,Object> unit){Long value=GameClock.number(unit.get("id"));return value==null?-1:value;}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> items(Map<String,Object> map,String key){return map!=null&&map.get(key) instanceof List?(List<Map<String,Object>>)map.get(key):Collections.<Map<String,Object>>emptyList();}
    private static Map<String,Object> find(Map<String,Object> state,long id){for(Map<String,Object> actor:items(state,"ownUnits"))if(id(actor)==id)return actor;return null;}
    private static Map<String,Object> fields(Object... pairs){Map<String,Object> result=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;}
}
