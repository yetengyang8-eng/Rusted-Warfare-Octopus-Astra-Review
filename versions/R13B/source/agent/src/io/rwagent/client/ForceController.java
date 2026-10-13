package io.rwagent.client;

import java.util.*;

/** G4 ordinary force proposals. Transport and deterministic batch admission belong to the host.
 * Only actual accepted native receipts advance command cooldowns or joining commitments. */
public final class ForceController {
    /** A lawful static resource objective; this does not assert occupancy or combat safety. */
    public static final class ExpansionSupport {
        public final int targetTile;
        public final String sourceKnowledgeId;
        public ExpansionSupport(int targetTile,String sourceKnowledgeId){
            if(targetTile<0||sourceKnowledgeId==null||sourceKnowledgeId.isEmpty())throw new IllegalArgumentException("Static resource tile and knowledge identity required");
            this.targetTile=targetTile;this.sourceKnowledgeId=sourceKnowledgeId;
        }
    }
    public interface Host {
        Map<String,Object> read(String path,String event)throws Exception;
        List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target,Map<String,Object> enemies)throws Exception;
        void collect(Proposal proposal)throws Exception;
    }
    public interface ReceiptCallback {void accepted(Map<String,Object> receipt)throws Exception;}
    public static final class Proposal {
        public final String owner,path,lane,reason;
        public final List<Long> actors;
        public final int priority;
        private final ReceiptCallback callback;
        private final ReceiptCallback rejection;
        public Proposal(String owner,List<Long> actors,String path,String lane,int priority,String reason,ReceiptCallback callback){
            this(owner,actors,path,lane,priority,reason,callback,null);
        }
        public Proposal(String owner,List<Long> actors,String path,String lane,int priority,String reason,ReceiptCallback callback,ReceiptCallback rejection){
            if(owner==null||owner.isEmpty()||actors==null||actors.isEmpty()||path==null||lane==null||reason==null)throw new IllegalArgumentException("Force proposal identity");
            this.owner=owner;this.actors=Collections.unmodifiableList(new ArrayList<Long>(actors));this.path=path;this.lane=lane;
            this.priority=priority;this.reason=reason;this.callback=callback;this.rejection=rejection;
        }
        /** The execution host calls this only after an accepted real native receipt. */
        public void accepted(Map<String,Object> receipt)throws Exception{if(receipt==null||!("queued".equals(receipt.get("status"))||"accepted".equals(receipt.get("status"))||Boolean.TRUE.equals(receipt.get("accepted"))))throw new IllegalArgumentException("Real accepted receipt required");if(callback!=null)callback.accepted(receipt);}
        /** Negative command evidence; this never declares the actor dead. */
        public void rejected(Map<String,Object> receipt)throws Exception{if(receipt!=null&&"rejected".equals(receipt.get("status"))&&rejection!=null)rejection.accepted(receipt);}
    }
    private final GeneralRegistry registry;
    private final Host host;
    private static final long ORDER_INTERVAL_MS=8000;
    private final Map<Long,Long> generalAccepted=new HashMap<Long,Long>(),joinAccepted=new HashMap<Long,Long>(),responseClearedFrame=new HashMap<Long,Long>();
    private final Map<Long,Long> responseClearedAt=new HashMap<Long,Long>();
    private final Map<Long,Long> cheapClearGeneration=new HashMap<Long,Long>();
    /** A short FREE staging hold after legal visibility clear; formal assignments override it. */
    public boolean localClearStagingHold(long id,long now){GeneralRegistry.UnitView u=registry.unit(id);Long at=responseClearedAt.get(id);
        return at!=null&&now>=at&&now-at<3000&&u!=null&&u.allocation==GeneralRegistry.Allocation.FREE&&u.reservedGeneralId==null&&u.membership==GeneralRegistry.Membership.UNATTACHED;}
    private final Map<Long,JoinReceiptTarget> joinReceiptTargets=new HashMap<Long,JoinReceiptTarget>();
    private long destinationUnknownJoinDeferCount;private Map<String,Object> lastJoinDefer=Collections.emptyMap();
    public Map<String,Object> orderReuseView(){Map<String,Object> out=new LinkedHashMap<String,Object>();out.put("destinationUnknownJoinDeferCount",destinationUnknownJoinDeferCount);out.put("lastJoinDefer",lastJoinDefer);return out;}
    private static final class JoinReceiptTarget {
        final GeneralRegistry.GeneralId general;final long generation;final double x,y;final String source;
        double bestDistance;long lastProgressAt;
        final DestinationUnknownDefer unknownDefer;
        JoinReceiptTarget(GeneralRegistry.UnitView unit,double x,double y,String source,double distance,long now,long receiptFrame,String threat){general=unit.reservedGeneralId;generation=unit.ownerGeneration;this.x=x;this.y=y;this.source=source;bestDistance=distance;lastProgressAt=now;unknownDefer=new DestinationUnknownDefer(x,y,now,receiptFrame,threat);}
    }
    private final Map<Long,Long> screenAccepted=new HashMap<Long,Long>();
    private ExpansionSupport expansionSupport;
    private GeneralCombatDirector generalCombatDirector;
    private RecoveryPool recoveryPool;
    public void setRecoveryPool(RecoveryPool pool){recoveryPool=pool;}
    private Response response;
    /** One small explicit responsibility per remote cluster; ownership still uses the existing
     * temporary-response origin/return path rather than another roster or General. */
    private final Map<Long,Response> detachments=new LinkedHashMap<Long,Response>();
    private CommandArbiter.Stamp taskResponsePreparedStamp;
    private long responseSequence;
    private static final class Response {
        long targetId;final long assetId,createdAt;final String owner;final List<Long> actors;
        Set<Long> taskEnemies;
        ThreatTask task;boolean searching;
        long allocationSourceTime=-1,searchVisibleFrame=-1,searchVisibleTime=-1;
        final Map<Long,Long> generations=new LinkedHashMap<Long,Long>();
        final Set<Long> regionTiles=new LinkedHashSet<Long>();
        CommandArbiter.Stamp coverageStamp;List<Long> coverage=Collections.emptyList();
        long acceptedAt=-100000;
        Response(long target,long asset,long now,long sequence,List<Long> actors){targetId=target;assetId=asset;createdAt=now;
            owner="local-response:"+sequence;this.actors=actors;}
    }
    public ForceController(GeneralRegistry registry,Host host){if(registry==null||host==null)throw new IllegalArgumentException("Registry and host required");this.registry=registry;this.host=host;}
    public void setExpansionSupport(ExpansionSupport support){expansionSupport=support;}
    /** G5 replaces ordinary General decisions only; null retains the validated G4 lane. */
    public void setGeneralCombatDirector(GeneralCombatDirector director){generalCombatDirector=director;}
    public void clearExpansionSupport(){expansionSupport=null;}
    /** Establish the existing low-cost response before Commander budgets whole Generals.
     * This prepares ownership only; the normal collected lane emits the actual Intent. */
    public void prepareLocalResponse(Map<String,Object> state,Map<String,Object> enemies,long now)throws Exception{
        reconcile(state,enemies,now);prepareOnly=true;
        try{collectLocalResponse(state,enemies,now);}finally{prepareOnly=false;}
    }
    /** Current Commander tasks receive nearby FREE capacity before ordinary join allocation.
     * Small tasks may temporarily borrow the minimum compatible remainder, preserving origin. */
    public void prepareLocalResponse(Map<String,Object> state,Map<String,Object> enemies,long now,List<ThreatTask> tasks)throws Exception{
        reconcile(state,enemies,now);
        if(registry.currentOwnState(state))taskResponsePreparedStamp=registry.observation();
        boolean currentCombat=registry.currentOwnState(state)&&combatCurrent(state,enemies,now);
        Long combatSourceTime=currentCombat?GameClock.number(enemies.get("gameTimeMs")):null;
        if(currentCombat)prepareDetachments(state,enemies,now,combatSourceTime,tasks);
        if(currentCombat&&response!=null&&response.taskEnemies!=null&&tasks!=null)for(ThreatTask task:tasks)if(task.status==ThreatTask.Status.CURRENT
                &&Objects.equals(Long.valueOf(task.sourceGameTimeMs),combatSourceTime)&&task.enemyIds.contains(response.targetId)){
            if(!task.smallResponseRoleKnown){clearResponse();break;}
            response.taskEnemies=new LinkedHashSet<Long>(task.enemyIds);}
        if(response==null&&currentCombat)prepareTaskResponse(state,enemies,now,combatSourceTime,tasks);
        prepareOnly=true;try{collectLocalResponse(state,enemies,now);}finally{prepareOnly=false;}
    }
    private void prepareTaskResponse(Map<String,Object> state,Map<String,Object> enemies,long now,long combatSourceTime,List<ThreatTask> tasks)throws Exception{
        if(tasks==null)return;
        List<ThreatTask> ordered=new ArrayList<ThreatTask>(tasks);
        Collections.sort(ordered,(a,b)->{int d=Double.compare(taskAssetDistance(a,state),taskAssetDistance(b,state));return d!=0?d:Long.compare(a.id,b.id);});
        for(ThreatTask task:ordered){
            // Own and combat are independent endpoints. Match the task to the validated combat
            // sample that created its visible facts, never to the earlier own-state clock.
            if(task.status!=ThreatTask.Status.CURRENT||task.sourceGameTimeMs!=combatSourceTime||task.enemyIds.size()>LocalCrisisPolicy.MAX_THREATS||!task.smallResponseRoleKnown
                    ||taskAssetDistance(task,state)>LocalCrisisPolicy.ASSET_RADIUS)continue;
            List<Map<String,Object>> threats=new ArrayList<Map<String,Object>>();for(Long enemy:task.enemyIds){Map<String,Object> row=currentTarget(enemies,enemy);if(row!=null)threats.add(row);}
            if(threats.size()!=task.enemyIds.size()||threats.isEmpty())continue;
            boolean small=smallThreatTask(task);
            List<Map<String,Object>> available=new ArrayList<Map<String,Object>>();
            Set<Long> free=registry.freeCandidateIds();
            for(GeneralRegistry.UnitView u:registry.units()){long actor=u.unitId;Map<String,Object> own=find(state,actor);
                GeneralRegistry.GeneralId origin=u.generalId!=null?u.generalId:u.reservedGeneralId;
                boolean borrow=small&&u.temporaryTask==GeneralRegistry.TemporaryTask.NONE&&u.externalOwner==null
                        &&u.healthRole==GeneralRegistry.HealthRole.NORMAL&&origin!=null
                        &&(generalCombatDirector==null||!generalCombatDirector.retreating(origin));
                if(!free.contains(actor)&&!borrow)continue;
                if(u!=null&&u.healthObservedFrame==registry.observation().frame&&ready(own)&&number(own,"hp")>=number(own,"maxHp")*.5
                        &&distance(own,task.x,task.y)<=LocalCrisisPolicy.RESPONSE_RADIUS
                        &&!Objects.equals(responseClearedFrame.get(actor),GameClock.number(state.get("frame"))))available.add(own);}
            List<Map<String,Object>> compatible=available;
            for(Map<String,Object> threat:threats){List<Map<String,Object>> result=host.eligible(compatible,threat,enemies);
                if(result==null){compatible=Collections.emptyList();break;}List<Long> ids=validatedIds(compatible,result);
                List<Map<String,Object>> filtered=new ArrayList<Map<String,Object>>();for(Map<String,Object> own:compatible)if(ids.contains(id(own)))filtered.add(own);compatible=filtered;}
            if(compatible.isEmpty())continue;
            Collections.sort(compatible,(a,b)->{boolean af=free.contains(id(a)),bf=free.contains(id(b));if(af!=bf)return af?-1:1;
                // Once nearby FREE is exhausted, strongest compatible borrowers minimize the
                // number interrupted. Distance and ID settle ties without another policy floor.
                if(!af){int hp=Double.compare(number(b,"hp"),number(a,"hp"));if(hp!=0)return hp;}
                int d=Double.compare(distance(a,task.x,task.y),distance(b,task.x,task.y));return d!=0?d:Long.compare(id(a),id(b));});
            List<Long> selected=new ArrayList<Long>();double hp=0;
            Map<GeneralRegistry.GeneralId,Integer> left=new HashMap<GeneralRegistry.GeneralId,Integer>();
            for(GeneralRegistry.GeneralView general:registry.generals())left.put(general.id,general.members.size());
            for(Map<String,Object> own:compatible){GeneralRegistry.UnitView unit=registry.unit(id(own));
                if(unit.generalId!=null){Integer remaining=left.get(unit.generalId);if(remaining==null||remaining<=LocalArmyDirector.RELEASE_BELOW)continue;
                    left.put(unit.generalId,remaining-1);}
                selected.add(id(own));hp+=number(own,"hp");if(hp/1000.0>=task.demandStrength||selected.size()>=LocalCrisisPolicy.MAX_RESPONDERS)break;}
            if(selected.isEmpty())continue;
            Response candidate=new Response(id(threats.get(0)),-1,now,++responseSequence,selected);candidate.taskEnemies=new LinkedHashSet<Long>(task.enemyIds);
            boolean acquired=true;for(Long actor:selected)if(!(small?registry.beginTemporaryResponse(actor,candidate.owner):registry.beginLocalResponse(actor,candidate.owner))){acquired=false;break;}
            if(!acquired){for(Long actor:selected){GeneralRegistry.UnitView u=registry.unit(actor);if(u!=null&&candidate.owner.equals(u.owner))registry.clearLocalResponse(actor);}continue;}
            response=candidate;return;
        }
    }
    /** Existing local-response envelope, used by both real leases and Commander allocation. */
    public static boolean smallThreatTask(ThreatTask task){return task!=null&&task.smallResponseRoleKnown&&task.enemyIds.size()<=LocalCrisisPolicy.MAX_THREATS
            &&task.demandStrength>0&&task.demandStrength<=LocalCrisisPolicy.MAX_RESPONDERS;}
    private static double taskAssetDistance(ThreatTask task,Map<String,Object> state){double nearest=Double.MAX_VALUE;
        for(Map<String,Object> own:items(state,"ownUnits"))if(asset(own))nearest=Math.min(nearest,Math.hypot(task.x-number(own,"x"),task.y-number(own,"y")));return nearest;}
    private void prepareDetachments(Map<String,Object> state,Map<String,Object> enemies,long now,long source,List<ThreatTask> tasks)throws Exception{
        if(tasks==null)return;List<ThreatTask> ordered=new ArrayList<ThreatTask>(tasks);
        Collections.sort(ordered,(a,b)->{int d=Double.compare(taskAssetDistance(a,state),taskAssetDistance(b,state));return d!=0?d:Long.compare(a.id,b.id);});
        for(ThreatTask task:ordered){if(task.status!=ThreatTask.Status.CURRENT||task.sourceGameTimeMs!=source)continue;
            Response active=detachments.get(task.id);
            if(active==null&&(taskAssetDistance(task,state)<=LocalCrisisPolicy.ASSET_RADIUS||frontlineDemand(task,state,enemies)))continue;
            List<Map<String,Object>> threats=new ArrayList<Map<String,Object>>();for(Map<String,Object> target:visible(enemies))if(task.enemyIds.contains(id(target)))threats.add(target);
            if(threats.size()!=task.enemyIds.size()||threats.isEmpty())continue;
            if(active!=null){if(active.searching||active.targetId!=id(threats.get(0)))active.acceptedAt=-100000;
                active.task=task;active.taskEnemies=new LinkedHashSet<Long>(task.enemyIds);active.targetId=id(threats.get(0));active.searching=false;
                active.searchVisibleFrame=active.searchVisibleTime=-1;active.regionTiles.clear();rememberRegion(active,state,threats);}
            List<Map<String,Object>> held=active==null?Collections.<Map<String,Object>>emptyList():ownedActors(active,state);
            // A retained lease with no current own row is an unknown existing commitment,
            // not zero capacity. Wait for its own witness before deciding any shortfall.
            boolean missingOwnedRow=false;if(active!=null)for(Long actor:ownedIds(active))if(find(state,actor)==null){missingOwnedRow=true;break;}
            List<Map<String,Object>> coverage=compatible(held,threats,enemies);
            if(active!=null&&task.unarmedContactKnown&&coverage.size()>1){Collections.sort(coverage,(a,b)->{
                boolean af=GeneralRegistry.FREE_OWNER.equals(registry.unit(id(a)).responseOriginOwner),bf=GeneralRegistry.FREE_OWNER.equals(registry.unit(id(b)).responseOriginOwner);
                if(af!=bf)return af?-1:1;int d=Double.compare(distance(a,task.x,task.y),distance(b,task.x,task.y));return d!=0?d:Long.compare(id(a),id(b));});
                long retained=id(coverage.get(0));for(Long actor:new ArrayList<Long>(active.actors))if(actor!=retained)releaseResponseActor(active,actor);
                coverage=new ArrayList<Map<String,Object>>(Collections.singletonList(coverage.get(0)));
            }
            double strength=0;for(Map<String,Object> actor:coverage)strength+=number(actor,"hp")/1000.0;
            // A repeated prepare of the same legal sample cannot grow the response twice.
            if(!missingOwnedRow&&(task.unarmedContactKnown?coverage.isEmpty():strength+1e-9<task.demandStrength)&&(active==null||source>active.allocationSourceTime)){
                List<Map<String,Object>> available=new ArrayList<Map<String,Object>>();Set<Long> free=registry.freeCandidateIds();
                for(GeneralRegistry.UnitView unit:registry.units()){
                    Map<String,Object> own=find(state,unit.unitId);GeneralRegistry.GeneralId origin=unit.generalId!=null?unit.generalId:unit.reservedGeneralId;
                    boolean borrow=unit.temporaryTask==GeneralRegistry.TemporaryTask.NONE&&unit.externalOwner==null&&origin!=null
                            &&(generalCombatDirector==null||!generalCombatDirector.retreating(origin));
                    if((free.contains(unit.unitId)||borrow)&&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&unit.healthObservedFrame==registry.observation().frame
                            &&ready(own)&&number(own,"hp")>=number(own,"maxHp")*.5&&distance(own,task.x,task.y)<=LocalCrisisPolicy.RESPONSE_RADIUS
                            &&!Objects.equals(responseClearedFrame.get(unit.unitId),Long.valueOf(registry.observation().frame)))available.add(own);
                }
                available=compatible(available,threats,enemies);
                Collections.sort(available,(a,b)->{boolean af=free.contains(id(a)),bf=free.contains(id(b));if(af!=bf)return af?-1:1;
                    if(!af){int hp=Double.compare(number(b,"hp"),number(a,"hp"));if(hp!=0)return hp;}
                    int d=Double.compare(distance(a,task.x,task.y),distance(b,task.x,task.y));return d!=0?d:Long.compare(id(a),id(b));});
                if(active==null){active=new Response(id(threats.get(0)),-1,now,++responseSequence,new ArrayList<Long>());
                    active.task=task;active.taskEnemies=new LinkedHashSet<Long>(task.enemyIds);rememberRegion(active,state,threats);}
                Map<GeneralRegistry.GeneralId,Integer> left=new HashMap<GeneralRegistry.GeneralId,Integer>();for(GeneralRegistry.GeneralView general:registry.generals())left.put(general.id,general.members.size());
                for(Map<String,Object> own:available){if(task.unarmedContactKnown?!coverage.isEmpty():strength+1e-9>=task.demandStrength)break;long actor=id(own);GeneralRegistry.UnitView unit=registry.unit(actor);
                    if(unit.generalId!=null){Integer remaining=left.get(unit.generalId);if(remaining==null||remaining<=LocalArmyDirector.RELEASE_BELOW)continue;}
                    if(!registry.beginTemporaryResponse(actor,active.owner))continue;
                    if(unit.generalId!=null)left.put(unit.generalId,left.get(unit.generalId)-1);
                    active.actors.add(actor);active.generations.put(actor,registry.unit(actor).ownerGeneration);coverage.add(own);strength+=number(own,"hp")/1000.0;active.acceptedAt=-100000;
                }
                active.allocationSourceTime=source;
                if(!active.actors.isEmpty())detachments.put(task.id,active);
            }
            if(active!=null&&!active.actors.isEmpty()){active.coverageStamp=registry.observation();List<Long> ids=new ArrayList<Long>();for(Map<String,Object> own:coverage)ids.add(id(own));active.coverage=ids;}
        }
    }
    /** Whole-force demand is relative to a nearby actual roster, never a global fixed count. */
    private boolean frontlineDemand(ThreatTask task,Map<String,Object> state,Map<String,Object> enemies){double armedDemand=0;
        for(Map<String,Object> enemy:visible(enemies))if(task.enemyIds.contains(id(enemy))&&Boolean.TRUE.equals(enemy.get("canAttack")))armedDemand+=number(enemy,"hp")/1000.0*LocalCrisisPolicy.HP_FACTOR;
        for(GeneralRegistry.GeneralView general:registry.generals()){
        if(general.phase!=GeneralRegistry.Phase.ACTIVE||!general.centroidKnown||general.centroidFrame!=registry.observation().frame
                ||Math.hypot(general.x-task.x,general.y-task.y)>LocalCrisisPolicy.RESPONSE_RADIUS
                ||generalCombatDirector!=null&&generalCombatDirector.retreating(general.id))continue;
        double strength=0;for(Long actor:general.members){GeneralRegistry.UnitView unit=registry.unit(actor);Map<String,Object> own=find(state,actor);
            if(unit!=null&&general.owner.equals(unit.owner)&&unit.temporaryTask==GeneralRegistry.TemporaryTask.NONE&&unit.healthRole==GeneralRegistry.HealthRole.NORMAL
                    &&unit.healthObservedFrame==registry.observation().frame&&ready(own)&&number(own,"hp")>=number(own,"maxHp")*.5)strength+=number(own,"hp")/1000.0;}
        if(strength>0&&armedDemand+1e-9>=strength)return true;
    }return false;}
    private List<Map<String,Object>> compatible(List<Map<String,Object>> actors,List<Map<String,Object>> threats,Map<String,Object> enemies)throws Exception{
        List<Map<String,Object>> result=new ArrayList<Map<String,Object>>(actors);for(Map<String,Object> threat:threats){List<Map<String,Object>> eligible=host.eligible(result,threat,enemies);
            if(eligible==null)return new ArrayList<Map<String,Object>>();List<Long> ids=validatedIds(result,eligible);result.removeIf(actor->!ids.contains(id(actor)));if(result.isEmpty())break;}return result;
    }
    private List<Long> ownedIds(Response active){List<Long> ids=new ArrayList<Long>();for(Long actor:active.actors){GeneralRegistry.UnitView unit=registry.unit(actor);
        if(unit!=null&&active.owner.equals(unit.owner)&&unit.temporaryTask==GeneralRegistry.TemporaryTask.LOCAL_RESPONSE
                &&Objects.equals(active.generations.get(actor),Long.valueOf(unit.ownerGeneration)))ids.add(actor);}return ids;}
    private List<Map<String,Object>> ownedActors(Response active,Map<String,Object> state){List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();
        for(Long actor:ownedIds(active)){GeneralRegistry.UnitView unit=registry.unit(actor);Map<String,Object> own=find(state,actor);
            if(unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&unit.healthObservedFrame==registry.observation().frame&&ready(own))actors.add(own);}return actors;}
    private static Map<String,Object> currentResponseTarget(Map<String,Object> enemies,Response active){for(Map<String,Object> target:visible(enemies))
        if(active.taskEnemies.contains(id(target)))return target;return null;}
    @SuppressWarnings("unchecked") private static void rememberRegion(Response active,Map<String,Object> state,List<Map<String,Object>> threats){
        Object raw=state.get("map");if(!(raw instanceof Map))return;Map<String,Object> map=(Map<String,Object>)raw;
        Long wide=GameClock.number(map.get("tilesWide")),high=GameClock.number(map.get("tilesHigh")),tw=GameClock.number(map.get("tileWidth")),th=GameClock.number(map.get("tileHeight"));
        if(wide==null||high==null||tw==null||th==null||wide<=0||high<=0||tw<=0||th<=0)return;
        for(Map<String,Object> enemy:threats){long col=(long)Math.floor(number(enemy,"x")/tw),row=(long)Math.floor(number(enemy,"y")/th);
            if(col>=0&&row>=0&&col<wide&&row<high)active.regionTiles.add(col*high+row);}
    }
    private void collectDetachments(Map<String,Object> state,Map<String,Object> enemies,long now)throws Exception{
        if(!registry.currentOwnState(state))return;boolean current=combatCurrent(state,enemies,now);
        for(final Response active:new ArrayList<Response>(detachments.values())){
            List<Map<String,Object>> actors=ownedActors(active,state);if(actors.isEmpty())continue;
            Map<String,Object> target=current?currentResponseTarget(enemies,active):null;
            if(target!=null){active.searching=false;List<Map<String,Object>> threats=responseThreats(enemies,active,target);List<Map<String,Object>> eligible=compatible(actors,threats,enemies);
                List<Long> ids=validatedIds(actors,eligible);if(ids.isEmpty()||now-active.acceptedAt<LocalCrisisPolicy.RETRY_MS)continue;
                final Map<Long,Long> generations=new LinkedHashMap<Long,Long>(active.generations);
                host.collect(new Proposal(active.owner,ids,attack(ids,number(target,"x"),number(target,"y")),"THREAT_DETACHMENT",70,"CURRENT_LEGAL_THREAT_TASK_MINIMUM_DETACHMENT",
                    receipt->{if(detachments.get(active.task.id)==active&&sameGenerations(active,generations))active.acceptedAt=now;}));continue;
            }
            active.searching=true;
            // A legal fully-visible region witness must be followed by a newer complete contact
            // sample. The earlier empty packet cannot describe enemies newly revealed by this read.
            Long frame=current?GameClock.number(enemies.get("frame")):null,source=current?GameClock.number(enemies.get("gameTimeMs")):null;
            boolean regionEnemy=false;if(current)for(Map<String,Object> enemy:visible(enemies))if(distance(enemy,active.task.x,active.task.y)<=LocalCrisisPolicy.CLUSTER_RADIUS){regionEnemy=true;break;}
            if(current&&!regionEnemy&&active.searchVisibleFrame>=0&&frame!=null&&frame>active.searchVisibleFrame&&source!=null&&source>=active.searchVisibleTime){
                if(observeResponseRegion(active,state,now)){releaseDetachment(active);continue;}
                active.searchVisibleFrame=active.searchVisibleTime=-1;
            }
            if(current&&active.searchVisibleFrame<0)observeResponseRegion(active,state,now);
            for(Map<String,Object> own:actors){final long actor=id(own),generation=active.generations.get(actor);
                boolean correct="move".equals(own.get("orderType"))&&finite(own.get("orderX"))&&finite(own.get("orderY"))
                        &&Math.hypot(number(own,"orderX")-active.task.x,number(own,"orderY")-active.task.y)<1;
                if(correct)continue; // Native MOVE retains search; order kind/target overwrite repairs it.
                host.collect(new Proposal(active.owner,Collections.singletonList(actor),"/command/move?unitId="+actor+"&x="+active.task.x+"&y="+active.task.y,
                    "THREAT_REACQUIRE",70,"LOST_CONTACT_MOVE_TO_LAST_KNOWN_REGION_FOR_LEGAL_VISION",
                    receipt->{GeneralRegistry.UnitView unit=registry.unit(actor);if(detachments.get(active.task.id)==active&&unit!=null&&unit.ownerGeneration==generation
                            &&active.owner.equals(unit.owner))active.acceptedAt=now;}));
            }
        }
    }
    private boolean sameGenerations(Response active,Map<Long,Long> before){if(!active.generations.equals(before))return false;
        for(Map.Entry<Long,Long> entry:before.entrySet()){GeneralRegistry.UnitView unit=registry.unit(entry.getKey());if(unit==null||unit.ownerGeneration!=entry.getValue()||!active.owner.equals(unit.owner))return false;}return true;}
    private boolean observeResponseRegion(Response active,Map<String,Object> state,long now)throws Exception{
        if(active.regionTiles.isEmpty())rememberRegion(active,state,Collections.singletonList(position(active.task.x,active.task.y)));
        if(active.regionTiles.isEmpty())return false;long frame=-1,time=-1;
        for(Long tile:active.regionTiles){Map<String,Object> probe=host.read("/scout/observe?tile="+tile+"&since="+active.task.sourceGameTimeMs,"threat_task_region_visibility");
            if(probe==null||!"observed".equals(probe.get("status"))||!registry.observation().session.equals(probe.get("sessionId"))
                    ||probe.containsKey("playerKey")&&!registry.observation().player.equals(probe.get("playerKey")))return false;
            Long probeFrame=GameClock.number(probe.get("frame"));Object raw=probe.get("region");if(probeFrame==null||probeFrame<registry.observation().frame||!(raw instanceof Map))return false;
            @SuppressWarnings("unchecked") Map<String,Object> region=(Map<String,Object>)raw;Long regionTile=GameClock.number(region.get("tile")),sample=GameClock.number(region.get("sampleGameTimeMs"));
            if(!tile.equals(regionTile)||sample==null||sample<now||number(region,"currentVisibleTiles")<=0
                    ||number(region,"recentFogTiles")!=0||number(region,"staleFogTiles")!=0||number(region,"deepFogTiles")!=0||number(region,"neverSeenTiles")!=0)return false;
            frame=Math.max(frame,probeFrame);time=Math.max(time,sample);
        }active.searchVisibleFrame=frame;active.searchVisibleTime=time;return true;
    }
    private static Map<String,Object> position(double x,double y){Map<String,Object> row=new LinkedHashMap<String,Object>();row.put("x",x);row.put("y",y);return row;}
    private void releaseResponseActor(Response active,long actor){if(!ownedIds(active).contains(actor))return;
        // An absent own row cannot authorize a transfer. Retain its lease/controller until
        // the own adapter supplies a return witness or explicitly releases that registry actor.
        if(!registry.clearLocalResponse(actor))return;GeneralRegistry.UnitView cleared=registry.unit(actor);
        if(cleared!=null){responseClearedFrame.put(actor,registry.observation().frame);responseClearedAt.put(actor,registry.observation().gameTimeMs);
            if(GeneralRegistry.FREE_OWNER.equals(cleared.owner)&&cleared.reservedGeneralId==null)cheapClearGeneration.put(actor,cleared.ownerGeneration);}
        active.actors.remove(Long.valueOf(actor));active.generations.remove(actor);
    }
    private void releaseDetachment(Response active){for(Long actor:ownedIds(active))releaseResponseActor(active,actor);
        if(ownedIds(active).isEmpty())detachments.remove(active.task.id);
    }
    private boolean prepareOnly;
    private CommandArbiter.Stamp coverageStamp;
    private String coverageOwner;
    private List<Long> verifiedCoverage=Collections.emptyList();
    public Map<String,Object> localResponseCoverageView(){
        Map<String,Object> out=new LinkedHashMap<String,Object>();CommandArbiter.Stamp stamp=registry.observation();
        boolean current=coverageStamp!=null&&coverageStamp.samePlayer(stamp)&&coverageStamp.frame==stamp.frame&&coverageStamp.gameTimeMs==stamp.gameTimeMs
            &&response!=null&&response.owner.equals(coverageOwner);
        out.put("ownCurrent",current);out.put("sourceSessionId",stamp.session);out.put("sourcePlayerKey",stamp.player);out.put("sourceFrame",stamp.frame);out.put("gameTimeMs",stamp.gameTimeMs);
        out.put("owner",response==null?null:response.owner);
        out.put("targetEnemyId",response==null?null:response.targetId);
        out.put("responsePolicy",response!=null&&response.taskEnemies!=null?"CURRENT_THREAT_TASK_FREE_FIRST_PARTIAL_ALLOWED":"LEGACY_HOME_RAID");
        List<Map<String,Object>> origins=new ArrayList<Map<String,Object>>();if(response!=null)for(Long id:response.actors){GeneralRegistry.UnitView unit=registry.unit(id);
            if(unit!=null&&response.owner.equals(unit.owner)&&unit.responseOriginOwner!=null)origins.add(unit.metadata());}
        out.put("temporaryResponseOrigins",origins);
        out.put("actorIds",current?new ArrayList<Long>(verifiedCoverage):Collections.emptyList());
        List<Map<String,Object>> detached=new ArrayList<Map<String,Object>>();for(Response active:detachments.values()){
            Map<String,Object> row=new LinkedHashMap<String,Object>();boolean fresh=active.coverageStamp!=null&&active.coverageStamp.samePlayer(stamp)
                    &&active.coverageStamp.frame==stamp.frame&&active.coverageStamp.gameTimeMs==stamp.gameTimeMs;
            row.put("taskId",active.task.id);row.put("owner",active.owner);row.put("responsibilityActive",!ownedIds(active).isEmpty());row.put("searching",active.searching);
            row.put("ownCurrent",fresh);row.put("sourceSessionId",stamp.session);row.put("sourcePlayerKey",stamp.player);row.put("sourceFrame",stamp.frame);row.put("gameTimeMs",stamp.gameTimeMs);
            row.put("combatSourceTime",active.task.sourceGameTimeMs);row.put("actorIds",fresh?new ArrayList<Long>(active.coverage):Collections.emptyList());
            row.put("ownedActorIds",ownedIds(active));row.put("lastKnownX",active.task.x);row.put("lastKnownY",active.task.y);
            row.put("searchVisibleFrame",active.searchVisibleFrame);row.put("searchVisibleTime",active.searchVisibleTime);row.put("attackAuthorized",fresh&&!active.searching);
            detached.add(row);
        }out.put("taskResponses",detached);return out;
    }
    /** Root may call this before its formal replenishment allocator; collect also reconciles idempotently. */
    public void reconcile(Map<String,Object> state,Map<String,Object> enemies,long now){
        if(registry.currentOwnState(state))for(Iterator<Map.Entry<Long,Response>> it=detachments.entrySet().iterator();it.hasNext();){Response active=it.next().getValue();
            active.actors.retainAll(ownedIds(active));active.generations.keySet().retainAll(active.actors);
            if(active.actors.isEmpty())it.remove(); // Recovery/invalidation cannot leave a phantom owner.
            else if(combatCurrent(state,enemies,now)&&currentResponseTarget(enemies,active)==null)active.searching=true;
        }
        if(response==null)return;
        boolean retained=false;for(Long id:response.actors){GeneralRegistry.UnitView unit=registry.unit(id);
            if(unit!=null&&response.owner.equals(unit.owner)&&unit.temporaryTask==GeneralRegistry.TemporaryTask.LOCAL_RESPONSE){retained=true;break;}}
        if(!retained){response=null;return;} // Released/recovering actors cannot keep an orphan controller alive.
        // Only a complete, fresh same-session native visible sample can end this simple task.
        // Missing/stale/foreign coverage is UNKNOWN; elapsed time and geometry are not visibility.
        if(!combatCurrent(state,enemies,now))return;
        if(currentTarget(enemies,response.targetId)==null)clearResponse();
    }
    public void collect(Map<String,Object> state,Map<String,Object> enemies,Map<String,Object> scout,double homeX,double homeY,long now)throws Exception{
        if(state==null||enemies==null||now<0)return;
        reconcile(state,enemies,now);
        if(recoveryPool!=null)recoveryPool.collect(state,homeX,homeY,host);
        collectLocalResponse(state,enemies,now);
        collectDetachments(state,enemies,now);
        collectCheapClear(state);
        collectJoining(state,enemies,now);
        if(generalCombatDirector==null)collectGenerals(state,enemies,now);else generalCombatDirector.collect(state,enemies,now);
        collectFormingScreens(state,now);
        // FREE movement is deliberately absent without a lawful, useful home-resource route.
    }
    private void clearResponse(){if(response==null)return;for(Long actor:response.actors){GeneralRegistry.UnitView unit=registry.unit(actor);
        if(unit!=null&&response.owner.equals(unit.owner)){registry.clearLocalResponse(actor);GeneralRegistry.UnitView cleared=registry.unit(actor);if(cleared!=null){responseClearedFrame.put(actor,registry.observation().frame);responseClearedAt.put(actor,registry.observation().gameTimeMs);
            if(response.taskEnemies!=null&&GeneralRegistry.FREE_OWNER.equals(cleared.owner)&&cleared.reservedGeneralId==null)cheapClearGeneration.put(actor,cleared.ownerGeneration);}}}response=null;}
    /** Releasing a lease cannot leave its accepted native attack travelling to a ghost.
     * Cancel at current own position; formal joins/new owners supersede this small clear action. */
    private void collectCheapClear(Map<String,Object> state)throws Exception{
        if(!registry.currentOwnState(state))return;
        for(Map.Entry<Long,Long> entry:new ArrayList<Map.Entry<Long,Long>>(cheapClearGeneration.entrySet())){
            final long actor=entry.getKey(),generation=entry.getValue();GeneralRegistry.UnitView unit=registry.unit(actor);Map<String,Object> own=find(state,actor);
            if(unit==null||unit.ownerGeneration!=generation||!GeneralRegistry.FREE_OWNER.equals(unit.owner)||unit.reservedGeneralId!=null){cheapClearGeneration.remove(actor);continue;}
            if(!ready(own))continue;
            host.collect(new Proposal(unit.owner,Collections.singletonList(actor),"/command/move?unitId="+actor+"&x="+number(own,"x")+"&y="+number(own,"y"),
                "LOCAL_RESPONSE_CLEAR",70,"CURRENT_VISIBILITY_CLEAR_CANCEL_OLD_ATTACK_NOT_KILL",receipt->{if(Objects.equals(cheapClearGeneration.get(actor),generation))cheapClearGeneration.remove(actor);}));
        }
    }
    private void collectLocalResponse(Map<String,Object> state,Map<String,Object> enemies,long now)throws Exception{
        if(!combatCurrent(state,enemies,now))return;
        if(response==null){
            CommandArbiter.Stamp current=registry.observation();
            if(taskResponsePreparedStamp!=null&&current.samePlayer(taskResponsePreparedStamp)&&current.frame==taskResponsePreparedStamp.frame
                    &&current.gameTimeMs==taskResponsePreparedStamp.gameTimeMs)return;
            Map<String,Object> target=null,asset=null;double nearest=Double.MAX_VALUE;
            for(Map<String,Object> enemy:visible(enemies))if(Boolean.TRUE.equals(enemy.get("canAttack"))&&!Boolean.TRUE.equals(enemy.get("building"))){
                for(Map<String,Object> candidate:items(state,"ownUnits"))if(asset(candidate)){double d=distance(enemy,number(candidate,"x"),number(candidate,"y"));
                    if(d<=LocalCrisisPolicy.ASSET_RADIUS&&(d<nearest||d==nearest&&target!=null&&id(enemy)<id(target))){nearest=d;target=enemy;asset=candidate;}}}
            if(target==null)return;
            List<Map<String,Object>> threats=cluster(enemies,target);if(threats.size()>LocalCrisisPolicy.MAX_THREATS)return;
            double enemyHp=0;for(Map<String,Object> threat:threats){double hp=number(threat,"hp");if(!(hp>0)||!Double.isFinite(hp))return;enemyHp+=hp;}
            List<Map<String,Object>> available=new ArrayList<Map<String,Object>>();
            for(GeneralRegistry.UnitView unit:registry.units())if(unit.membership!=GeneralRegistry.Membership.JOINING
                    &&unit.temporaryTask==GeneralRegistry.TemporaryTask.NONE&&unit.healthRole==GeneralRegistry.HealthRole.NORMAL
                    &&(generalCombatDirector==null||unit.generalId==null||!generalCombatDirector.retreating(unit.generalId))
                    &&unit.externalOwner==null&&!Objects.equals(responseClearedFrame.get(unit.unitId),GameClock.number(state.get("frame")))){Map<String,Object> own=find(state,unit.unitId);if(ready(own)&&number(own,"hp")>=number(own,"maxHp")*.5)available.add(own);}
            if(available.isEmpty())return;
            List<Map<String,Object>> compatible=available;
            for(Map<String,Object> threat:threats){compatible=host.eligible(compatible,threat,enemies);if(compatible==null||compatible.isEmpty())return;}
            final double x=number(asset,"x"),y=number(asset,"y");
            List<Map<String,Object>> candidates=new ArrayList<Map<String,Object>>();
            for(Map<String,Object> actor:compatible)if(contains(available,id(actor))&&distance(actor,x,y)<=LocalCrisisPolicy.RESPONSE_RADIUS)candidates.add(actor);
            Collections.sort(candidates,(a,c)->{int rank=Integer.compare(localRank(id(a)),localRank(id(c)));if(rank!=0)return rank;
                int d=Double.compare(distance(a,x,y),distance(c,x,y));return d!=0?d:Long.compare(id(a),id(c));});
            List<Long> selected=new ArrayList<Long>();Map<GeneralRegistry.GeneralId,Integer> sizes=new HashMap<GeneralRegistry.GeneralId,Integer>();
            for(GeneralRegistry.GeneralView general:registry.generals())sizes.put(general.id,general.members.size());
            double hp=0;
            for(Map<String,Object> actor:candidates){
                if(selected.size()>=LocalCrisisPolicy.MAX_RESPONDERS)break;
                GeneralRegistry.UnitView unit=registry.unit(id(actor));
                if(unit.generalId!=null){Integer left=sizes.get(unit.generalId);GeneralRegistry.GeneralView general=registry.general(unit.generalId);
                    if(left==null||general==null||general.phase==GeneralRegistry.Phase.ACTIVE&&left<=LocalArmyDirector.RELEASE_BELOW)continue;sizes.put(unit.generalId,left-1);}
                selected.add(id(actor));hp+=number(actor,"hp");
                if(hp>=enemyHp*LocalCrisisPolicy.HP_FACTOR)break;
            }
            if(selected.isEmpty()||hp<enemyHp*LocalCrisisPolicy.HP_FACTOR)return;
            Response candidate=new Response(id(target),id(asset),now,++responseSequence,selected);
            for(Long actor:selected)if(!registry.beginLocalResponse(actor,candidate.owner)){
                for(Long acquired:selected){GeneralRegistry.UnitView held=registry.unit(acquired);if(held!=null&&candidate.owner.equals(held.owner))registry.clearLocalResponse(acquired);}return;}
            response=candidate;
        }
        if(prepareOnly){
            coverageStamp=null;coverageOwner=null;verifiedCoverage=Collections.emptyList();
            List<Map<String,Object>> coverage=new ArrayList<Map<String,Object>>();
            for(Long id:response.actors){GeneralRegistry.UnitView unit=registry.unit(id);Map<String,Object> own=find(state,id);
                if(unit!=null&&response.owner.equals(unit.owner)&&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&ready(own))coverage.add(own);}
            Map<String,Object> current=currentTarget(enemies,response.targetId);
            if(current!=null)for(Map<String,Object> threat:responseThreats(enemies,response,current)){List<Map<String,Object>> eligible=host.eligible(coverage,threat,enemies);
                if(eligible==null){coverage.clear();break;}List<Long> ids=validatedIds(coverage,eligible);coverage.removeIf(actor->!ids.contains(id(actor)));if(coverage.isEmpty())break;}
            if(current!=null){List<Long> ids=new ArrayList<Long>();for(Map<String,Object> own:coverage)ids.add(id(own));verifiedCoverage=ids;coverageStamp=registry.observation();coverageOwner=response.owner;}
            return;
        }
        final Response active=response;if(now-active.acceptedAt<LocalCrisisPolicy.RETRY_MS)return;
        Map<String,Object> target=currentTarget(enemies,active.targetId);if(target==null)return;
        List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();for(Long id:active.actors){GeneralRegistry.UnitView unit=registry.unit(id);Map<String,Object> own=find(state,id);
            if(unit!=null&&active.owner.equals(unit.owner)&&unit.temporaryTask==GeneralRegistry.TemporaryTask.LOCAL_RESPONSE&&ready(own))actors.add(own);}
        List<Map<String,Object>> compatible=actors;
        for(Map<String,Object> threat:active.taskEnemies==null?Collections.singletonList(target):responseThreats(enemies,active,target)){List<Map<String,Object>> eligible=host.eligible(compatible,threat,enemies);if(eligible==null||eligible.isEmpty())return;
            List<Long> valid=validatedIds(compatible,eligible);List<Map<String,Object>> filtered=new ArrayList<Map<String,Object>>();for(Map<String,Object> own:compatible)if(valid.contains(id(own)))filtered.add(own);compatible=filtered;}
        List<Long> ids=validatedIds(actors,compatible);if(ids.isEmpty())return;
        host.collect(new Proposal(active.owner,ids,attack(ids,number(target,"x"),number(target,"y")),"LOCAL_RESPONSE",70,
            active.taskEnemies==null?"CURRENT_VISIBLE_HOME_RAID":"CURRENT_THREAT_TASK_FREE_FIRST_TEMPORARY_COMPATIBLE_COVERAGE",
            receipt->{if(response==active)active.acceptedAt=now;}));
    }
    private static List<Map<String,Object>> responseThreats(Map<String,Object> enemies,Response active,Map<String,Object> target){
        if(active.taskEnemies==null)return cluster(enemies,target);List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> row:visible(enemies))if(active.taskEnemies.contains(id(row)))result.add(row);return result;}
    private int localRank(long actor){GeneralRegistry.UnitView unit=registry.unit(actor);return unit!=null&&unit.membership!=GeneralRegistry.Membership.ATTACHED?0:1;}
    private void collectJoining(Map<String,Object> state,Map<String,Object> enemies,long now)throws Exception{
        final Long frame=GameClock.number(state.get("frame"));if(frame==null)return;
        final String threat=DestinationUnknownDefer.threatSignature(enemies,generalCombatDirector==null?combatCurrent(state,enemies,now):generalCombatDirector.combatCurrent(state,enemies,now));
        for(GeneralRegistry.UnitView initial:registry.units()){
            if(initial.temporaryTask!=GeneralRegistry.TemporaryTask.NONE||initial.externalOwner!=null)continue;
            if(initial.allocation==GeneralRegistry.Allocation.PENDING_JOIN&&initial.membership==GeneralRegistry.Membership.UNATTACHED&&initial.healthRole==GeneralRegistry.HealthRole.NORMAL)registry.beginJoining(initial.unitId);
            final GeneralRegistry.UnitView unit=registry.unit(initial.unitId);
            if(unit==null||unit.membership!=GeneralRegistry.Membership.JOINING||unit.reservedGeneralId==null)continue;
            GeneralRegistry.GeneralView general=registry.general(unit.reservedGeneralId);Map<String,Object> own=find(state,unit.unitId);
            Map<String,Object> target=registry.joiningTarget(unit.reservedGeneralId,state);
            if(general==null||!Boolean.TRUE.equals(target.get("known"))||!ready(own))continue;
            final double targetX=number(target,"x"),targetY=number(target,"y");
            final String targetSource=(String)target.get("source");final JoinReceiptTarget prior=joinReceiptTargets.get(unit.unitId);
            final boolean sameCommitment=prior!=null&&prior.generation==unit.ownerGeneration&&unit.reservedGeneralId.equals(prior.general);
            if(sameCommitment)prior.unknownDefer.observe(own,frame,now);
            boolean threatChanged=sameCommitment&&!Objects.equals(prior.unknownDefer.threat,threat);
            boolean urgentRedirect="CURRENT_HOME_SIDE_RALLY_DYNAMIC_UNKNOWN".equals(targetSource)
                    &&(!sameCommitment
                    ||!targetSource.equals(prior.source)||Math.hypot(prior.x-targetX,prior.y-targetY)>=1);
            boolean laterReceipt=sameCommitment&&unit.joinAcceptedFrame>=0&&frame>unit.joinAcceptedFrame;
            boolean targetRedirect=sameCommitment&&(!Objects.equals(targetSource,prior.source)||Math.hypot(prior.x-targetX,prior.y-targetY)>90);
            // Explicit current native destination is stronger than global contact churn or a
            // no-progress timer. Retain it while it still leads into the current arrival envelope.
            if(!urgentRedirect&&laterReceipt&&Objects.equals(targetSource,prior.source)
                    &&"move".equals(own.get("orderType"))&&Math.hypot(number(own,"orderX")-prior.x,number(own,"orderY")-prior.y)<1
                    &&Math.hypot(prior.x-targetX,prior.y-targetY)<=90)continue;
            boolean wrongNativeOrder=laterReceipt&&(own.get("orderType") instanceof String&&!"move".equals(own.get("orderType"))
                    ||finite(own.get("orderX"))&&finite(own.get("orderY"))&&Math.hypot(number(own,"orderX")-prior.x,number(own,"orderY")-prior.y)>=1);
            if(!urgentRedirect&&!targetRedirect&&!wrongNativeOrder&&!threatChanged&&sameCommitment&&now-last(joinAccepted,unit.unitId)<ORDER_INTERVAL_MS)continue;
            if(!urgentRedirect&&!threatChanged&&sameCommitment&&Objects.equals(targetSource,prior.source)&&unit.joinAcceptedFrame>=0&&frame>unit.joinAcceptedFrame
                    &&Math.hypot(prior.x-targetX,prior.y-targetY)<1&&"move".equals(own.get("orderType"))
                    &&prior.unknownDefer.allows(own,frame,now,12000,2500,threat)){
                destinationUnknownJoinDeferCount++;Map<String,Object> witness=new LinkedHashMap<String,Object>();witness.put("evidence",DestinationUnknownDefer.EVIDENCE);witness.put("actorId",unit.unitId);witness.put("generation",unit.ownerGeneration);
                witness.put("sourceFrame",frame);witness.put("gameTimeMs",now);witness.put("semantics","SHORT_SCHEDULING_DEFER_NOT_NATIVE_DESTINATION_OR_ARRIVAL_WITNESS");lastJoinDefer=Collections.unmodifiableMap(witness);continue;}
            List<Long> ids=Collections.singletonList(unit.unitId);
            host.collect(new Proposal(unit.owner,ids,"/command/move?unitId="+unit.unitId+"&x="+targetX+"&y="+targetY,"JOINING",50,
                "CURRENT_HOME_SIDE_RALLY_DYNAMIC_UNKNOWN".equals(target.get("source"))?"MOVE_TO_HOME_SIDE_RALLY_DYNAMIC_UNKNOWN":general.phase==GeneralRegistry.Phase.FORMING?"MOVE_TO_FORMATION_OWN_ANCHOR":"MOVE_TO_CURRENT_GENERAL_CENTROID",
                receipt->{Long actualFrame=GameClock.number(receipt.get("frame"));GeneralRegistry.UnitView current=registry.unit(unit.unitId);
                    if(actualFrame!=null&&actualFrame>=frame&&current!=null&&current.ownerGeneration==unit.ownerGeneration&&current.owner.equals(unit.owner)
                            &&Objects.equals(current.reservedGeneralId,unit.reservedGeneralId)&&registry.joinAccepted(unit.unitId,actualFrame)){
                        Long actualTime=GameClock.number(receipt.get("gameTimeMs"));joinAccepted.put(unit.unitId,actualTime!=null&&actualTime>=now?actualTime:now);
                        long acceptedTime=actualTime!=null&&actualTime>=now?actualTime:now;
                        joinReceiptTargets.put(unit.unitId,new JoinReceiptTarget(unit,targetX,targetY,targetSource,distance(own,targetX,targetY),acceptedTime,actualFrame,threat));}}));
        }
    }
    private void collectGenerals(Map<String,Object> state,Map<String,Object> enemies,long now)throws Exception{
        for(final GeneralRegistry.GeneralView general:registry.generals()){
            if(general.phase!=GeneralRegistry.Phase.ACTIVE)continue;
            if(!general.centroidKnown||!Objects.equals(Long.valueOf(general.centroidFrame),GameClock.number(state.get("frame")))||now-last(generalAccepted,general.id.value)<ORDER_INTERVAL_MS)continue;
            List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();for(Long id:general.members){GeneralRegistry.UnitView unit=registry.unit(id);Map<String,Object> own=find(state,id);
                if(unit!=null&&unit.membership==GeneralRegistry.Membership.ATTACHED&&unit.temporaryTask==GeneralRegistry.TemporaryTask.NONE
                        &&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&unit.externalOwner==null&&general.owner.equals(unit.owner)&&ready(own))actors.add(own);}
            if(actors.isEmpty())continue;
            List<Map<String,Object>> targets=new ArrayList<Map<String,Object>>(combatCurrent(state,enemies,now)?visible(enemies):Collections.<Map<String,Object>>emptyList());Collections.sort(targets,(a,b)->{
                int d=Double.compare(distance(a,general.x,general.y),distance(b,general.x,general.y));return d!=0?d:Long.compare(id(a),id(b));});
            boolean proposed=false;
            for(Map<String,Object> target:targets){List<Map<String,Object>> eligible=host.eligible(actors,target,enemies);if(eligible==null||eligible.isEmpty())continue;
                List<Long> ids=validatedIds(actors,eligible);if(ids.isEmpty())continue;
                host.collect(new Proposal(general.owner,ids,attack(ids,number(target,"x"),number(target,"y")),"GENERAL",30,"CURRENT_VISIBLE_TARGET",
                    receipt->{generalAccepted.put(general.id.value,now);registry.updateGeneralGoal(general.id,number(target,"x"),number(target,"y"),id(target));}));proposed=true;break;}
            if(proposed)continue;
            Map<String,Object> anchor=actors.get(0);for(Map<String,Object> actor:actors)if(distance(actor,general.x,general.y)<distance(anchor,general.x,general.y))anchor=actor;
            Map<String,Object> plan=host.read("/scout/plan?role=army&unitId="+id(anchor)+"&avoid=","g4_general_frontier_plan");
            if(plan==null||!"planned".equals(plan.get("status"))||!Boolean.TRUE.equals(plan.get("pathKnown"))
                    ||GameClock.number(plan.get("targetTile"))==null||!finite(plan.get("targetX"))||!finite(plan.get("targetY")))continue;
            // As in the existing local-army lane, this is anchor path evidence, not a claim that
            // every member can reach it. Homogeneous members keep native final command guards.
            List<Long> ids=new ArrayList<Long>();Object type=anchor.get("type");for(Map<String,Object> actor:actors)if(Objects.equals(type,actor.get("type")))ids.add(id(actor));
            if(ids.isEmpty())continue;
            host.collect(new Proposal(general.owner,ids,attack(ids,number(plan,"targetX"),number(plan,"targetY")),"GENERAL",30,"NATIVE_KNOWN_ANCHOR_FRONTIER",
                receipt->{generalAccepted.put(general.id.value,now);registry.updateGeneralGoal(general.id,number(plan,"targetX"),number(plan,"targetY"),null);}));
        }
    }
    private void collectFormingScreens(Map<String,Object> state,long now)throws Exception{
        final ExpansionSupport support=expansionSupport;if(support==null)return;
        for(final GeneralRegistry.GeneralView general:registry.generals()){
            if(general.phase!=GeneralRegistry.Phase.FORMING)continue;
            if(generalCombatDirector!=null&&generalCombatDirector.retreating(general.id))continue;
            for(final Long actor:general.members){GeneralRegistry.UnitView unit=registry.unit(actor);Map<String,Object> own=find(state,actor);
                if(unit!=null&&unit.membership==GeneralRegistry.Membership.ATTACHED&&unit.temporaryTask==GeneralRegistry.TemporaryTask.NONE
                        &&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&unit.externalOwner==null&&general.owner.equals(unit.owner)
                        &&ready(own)&&number(own,"hp")>=number(own,"maxHp")*.5&&now-last(screenAccepted,actor)>=ORDER_INTERVAL_MS){
                    Map<String,Object> plan=host.read("/static-map/approach?unitId="+actor+"&tile="+support.targetTile,"g41_forming_expansion_approach");
                    if(!staticApproachCurrent(plan,state,now,actor,support))continue;
                    // Each actor is its own path anchor. A static short segment does not prove
                    // hidden occupancy, dynamic safety, final arrival or another member's route.
                    host.collect(new Proposal(general.owner,Collections.singletonList(actor),
                        "/command/move?unitId="+actor+"&x="+number(plan,"waypointX")+"&y="+number(plan,"waypointY"),"FORMING_SCREEN",20,
                        "STATIC_ANCHOR_APPROACH_DYNAMIC_UNKNOWN:knowledge="+support.sourceKnowledgeId+":tile="+support.targetTile+":anchor="+actor,
                        receipt->{screenAccepted.put(actor,now);}));
                }
            }
        }
    }
    private static boolean staticApproachCurrent(Map<String,Object> plan,Map<String,Object> state,long now,long actor,ExpansionSupport support){
        if(plan==null||!"KNOWN".equals(plan.get("status"))||!Boolean.TRUE.equals(plan.get("staticOnly"))||!Boolean.TRUE.equals(plan.get("staticPathKnown")))return false;
        Long unit=GameClock.number(plan.get("unitId")),tile=GameClock.number(plan.get("targetTile")),sourceFrame=GameClock.number(plan.get("frame")),frame=GameClock.number(state.get("frame")),sourceTime=GameClock.number(plan.get("gameTimeMs"));
        Object session=state.get("sessionId");
        return unit!=null&&unit==actor&&tile!=null&&tile==support.targetTile&&support.sourceKnowledgeId.equals(plan.get("knowledgeId"))
                &&session instanceof String&&session.equals(plan.get("sessionId"))&&frame!=null&&sourceFrame!=null&&sourceFrame>=frame&&sourceTime!=null&&sourceTime>=now
                &&finite(plan.get("waypointX"))&&finite(plan.get("waypointY"));
    }
    private static List<Long> validatedIds(List<Map<String,Object>> candidates,List<Map<String,Object>> eligible){List<Long> ids=new ArrayList<Long>();
        for(Map<String,Object> actor:eligible){long id=id(actor);if(contains(candidates,id)&&!ids.contains(id))ids.add(id);}return ids;}
    private static boolean contains(List<Map<String,Object>> actors,long id){for(Map<String,Object> actor:actors)if(id(actor)==id)return true;return false;}
    private static String attack(List<Long> ids,double x,double y){StringBuilder actors=new StringBuilder();for(Long id:ids){if(actors.length()>0)actors.append(',');actors.append(id);}
        return "/command/attack-move?unitIds="+actors+"&x="+x+"&y="+y;}
    private static long last(Map<Long,Long> values,long id){Long value=values.get(id);return value==null?-100000:value;}
    private static List<Map<String,Object>> cluster(Map<String,Object> enemies,Map<String,Object> target){List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> enemy:visible(enemies))if(Boolean.TRUE.equals(enemy.get("canAttack"))&&!Boolean.TRUE.equals(enemy.get("building"))
                &&distance(enemy,number(target,"x"),number(target,"y"))<=LocalCrisisPolicy.CLUSTER_RADIUS)result.add(enemy);return result;}
    private static Map<String,Object> currentTarget(Map<String,Object> enemies,long target){for(Map<String,Object> enemy:visible(enemies))if(id(enemy)==target)return enemy;return null;}
    private boolean combatCurrent(Map<String,Object> state,Map<String,Object> enemies,long now){
        if(state==null||enemies==null||!(enemies.get("visibleEnemies") instanceof List))return false;
        Object session=state.get("sessionId");Long source=GameClock.number(enemies.get("gameTimeMs")),frame=GameClock.number(state.get("frame")),enemyFrame=GameClock.number(enemies.get("frame"));
        if(!(session instanceof String)||!session.equals(enemies.get("sessionId"))||source==null||source<now
                ||frame!=null&&enemyFrame!=null&&enemyFrame<frame
                ||enemies.containsKey("playerKey")&&!Objects.equals(registry.observation().player,enemies.get("playerKey")))return false;
        Object player=enemies.get("player");if(player instanceof Map&&((Map<?,?>)player).containsKey("teamId")){
            Object team=((Map<?,?>)player).get("teamId");if(!(team instanceof Number)||!registry.observation().player.equals("team:"+((Number)team).longValue()))return false;}
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies"))if(GameClock.number(enemy.get("id"))==null||!finite(enemy.get("x"))||!finite(enemy.get("y"))
                ||!finite(enemy.get("hp"))||!source.equals(GameClock.number(enemy.get("lastSeenGameTimeMs"))))return false;
        return true;
    }
    private static List<Map<String,Object>> visible(Map<String,Object> enemies){List<Map<String,Object>> current=new ArrayList<Map<String,Object>>();Long source=GameClock.number(enemies.get("gameTimeMs"));if(source==null)return current;
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies")){Long seen=GameClock.number(enemy.get("lastSeenGameTimeMs"));if(seen!=null&&seen.equals(source)&&readyEnemy(enemy))current.add(enemy);}return current;}
    private static boolean readyEnemy(Map<String,Object> enemy){return GameClock.number(enemy.get("id"))!=null&&finite(enemy.get("x"))&&finite(enemy.get("y"))&&number(enemy,"hp")>0;}
    private static boolean asset(Map<String,Object> own){if(!ready(own))return false;Object type=own.get("type");return "commandCenter".equals(type)||type instanceof String&&((String)type).startsWith("extractor");}
    private static boolean ready(Map<String,Object> own){return own!=null&&finite(own.get("x"))&&finite(own.get("y"))&&number(own,"hp")>0&&!Boolean.TRUE.equals(own.get("dead"))&&number(own,"buildProgress")>=1;}
    private static double distance(Map<String,Object> unit,double x,double y){return Math.hypot(number(unit,"x")-x,number(unit,"y")-y);}
    private static double number(Map<String,Object> map,String key){Object value=map==null?null:map.get(key);return value instanceof Number?((Number)value).doubleValue():Double.NaN;}
    private static boolean finite(Object value){return value instanceof Number&&Double.isFinite(((Number)value).doubleValue());}
    private static long id(Map<String,Object> unit){Long value=GameClock.number(unit.get("id"));return value==null?-1:value;}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> items(Map<String,Object> map,String key){return map!=null&&map.get(key) instanceof List?(List<Map<String,Object>>)map.get(key):Collections.<Map<String,Object>>emptyList();}
    private static Map<String,Object> find(Map<String,Object> state,long id){for(Map<String,Object> own:items(state,"ownUnits"))if(id(own)==id)return own;return null;}
}

