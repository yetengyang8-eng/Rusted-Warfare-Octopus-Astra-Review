package io.rwagent.client;

import java.util.*;

/** Allocation/task coordinator. Independent Generals retain every member, command and owner. */
public final class CommanderDirector {
    public interface CombatProvider {Map<String,Object> view(GeneralRegistry.GeneralId id);}
    /** Actual native-compatible LocalResponse lease, with current own observation provenance. */
    public interface LocalCoverageProvider {Map<String,Object> view();}
    public static final long ATTENTION_HOLD_MS=3000;
    private final GeneralRegistry registry;
    private CombatProvider combat;
    private Map<String,Object> currentEnemies=Collections.emptyMap();
    private LocalCoverageProvider localCoverage;
    private final Map<Long,ThreatTask> tasks=new LinkedHashMap<Long,ThreatTask>();
    private final Map<GeneralRegistry.GeneralId,Long> assignments=new LinkedHashMap<GeneralRegistry.GeneralId,Long>();
    private final Map<GeneralRegistry.GeneralId,Map<String,Object>> holds=new LinkedHashMap<GeneralRegistry.GeneralId,Map<String,Object>>();
    private final Map<Long,String> budgetSignatures=new HashMap<Long,String>();
    private CommandArbiter.Stamp assignmentStamp;
    private CommandArbiter.Stamp threatTaskObservation;
    private long threatTaskSourceTime=-1;
    private final List<Map<String,Object>> changes=new ArrayList<Map<String,Object>>();
    private long nextTask,lastBirthFrame=-1;
    public CommanderDirector(GeneralRegistry registry){if(registry==null)throw new IllegalArgumentException("Registry required");this.registry=registry;}
    public void setCombatProvider(CombatProvider provider){combat=provider;}
    public void setLocalCoverageProvider(LocalCoverageProvider provider){localCoverage=provider;}
    /** Prepare legal task facts before the native-compatible cheap-response allocator.
     * No reinforcement reservation or General attention is changed by this read stage. */
    public void prepareThreatTasks(Map<String,Object> state,Map<String,Object> enemies,long now){
        if(!registry.currentOwnState(state)||now!=registry.observation().gameTimeMs)return;
        currentEnemies=enemies==null?Collections.<String,Object>emptyMap():enemies;
        cleanup();reconcileTasks(state,enemies,now);
    }
    public List<ThreatTask> currentThreatTasks(){List<ThreatTask> out=new ArrayList<ThreatTask>();
        CommandArbiter.Stamp own=registry.observation();
        if(threatTaskObservation==null||!own.samePlayer(threatTaskObservation)||own.frame!=threatTaskObservation.frame
                ||own.gameTimeMs!=threatTaskObservation.gameTimeMs)return Collections.unmodifiableList(out);
        for(ThreatTask task:tasks.values())if(task.status==ThreatTask.Status.CURRENT&&task.sourceGameTimeMs==threatTaskSourceTime)out.add(task);
        return Collections.unmodifiableList(out);
    }
    public void reconcile(Map<String,Object> state,Map<String,Object> enemies,double homeX,double homeY,long now,int desiredStrength){
        if(!registry.currentOwnState(state)||now!=registry.observation().gameTimeMs)return;
        currentEnemies=enemies==null?Collections.<String,Object>emptyMap():enemies;
        cleanup();reconcileTasks(state,enemies,now);refreshJoinTargets();allocate(state);
        growCurrentGeneral(state,desiredStrength);allocate(state);
        // One explicit birth per observation; its full desired strength prevents six-unit fragmentation.
        if(lastBirthFrame!=registry.observation().frame&&legalHome(state,homeX,homeY)){
            GeneralRegistry.GeneralId born=registry.createAdditionalForming(state,Math.max(LocalArmyDirector.FORM_MINIMUM,desiredStrength),homeX,homeY);
            if(born!=null){lastBirthFrame=registry.observation().frame;record("COMMANDER_OVERFLOW_FORMATION",fields("generalId",born.value));allocate(state);}
        }
        assignTasks(state,homeX,homeY,now);
    }
    private void cleanup(){Set<GeneralRegistry.GeneralId> live=new HashSet<GeneralRegistry.GeneralId>();for(GeneralRegistry.GeneralView g:registry.generals())live.add(g.id);
        holds.keySet().retainAll(live);
        Iterator<Map.Entry<GeneralRegistry.GeneralId,Long>> it=assignments.entrySet().iterator();while(it.hasNext()){
            Map.Entry<GeneralRegistry.GeneralId,Long> entry=it.next();if(!live.contains(entry.getKey())){record("COMMANDER_GENERAL_INVALIDATED",fields("generalId",entry.getKey().value,"taskId",entry.getValue()));it.remove();}}
    }
    private void refreshJoinTargets(){for(GeneralRegistry.GeneralView general:registry.generals()){
        Map<String,Object> view=rawCombatView(general.id);boolean retreat=flag(view,"retreating")||"OVERMATCHED".equals(view.get("crisis"))||"RETREAT".equals(view.get("crisis"));
        if(view.isEmpty()||!combatCurrent(view)){if(retreat)registry.requireJoinTargetOverride(general.id);continue;}
        if(retreat){registry.requireJoinTargetOverride(general.id);
            if(flag(view,"rallyKnown")&&finite(view.get("rallyX"))&&finite(view.get("rallyY")))registry.setJoinTargetOverride(general.id,registry.observation(),number(view,"rallyX"),number(view,"rallyY"));
        }else registry.clearJoinTargetOverride(general.id);
    }}
    private void allocate(Map<String,Object> state){
        Map<String,Object> local=localCoverage==null?Collections.<String,Object>emptyMap():localCoverage.view();
        Object cheapOwner=local!=null&&"CURRENT_THREAT_TASK_FREE_FIRST_PARTIAL_ALLOWED".equals(local.get("responsePolicy"))?local.get("owner"):null;
        List<GeneralRegistry.UnitView> candidates=new ArrayList<GeneralRegistry.UnitView>();for(GeneralRegistry.UnitView u:registry.units()){
            Map<String,Object> own=find(state,u.unitId);
            if(u.allocation==GeneralRegistry.Allocation.FREE&&u.membership==GeneralRegistry.Membership.UNATTACHED&&u.reservedGeneralId==null
                    &&(u.temporaryTask==GeneralRegistry.TemporaryTask.NONE||!Objects.equals(u.owner,cheapOwner))
                    &&u.externalOwner==null&&!u.localResponseDetachedFromGeneral&&u.healthRole==GeneralRegistry.HealthRole.NORMAL
                    &&u.healthObservedFrame==registry.observation().frame&&u.freeSinceFrame<registry.observation().frame&&healthy(own))candidates.add(u);
        }
        Collections.sort(candidates,(a,b)->Long.compare(a.unitId,b.unitId));
        for(GeneralRegistry.UnitView unit:candidates){GeneralRegistry.GeneralView best=null;double bestScore=-Double.MAX_VALUE;
            Map<String,Object> own=find(state,unit.unitId);
            for(GeneralRegistry.GeneralView g:registry.generals()){
                int vacancies=g.desiredStrength-g.members.size()-g.reservations.size();if(vacancies<=0||!g.joinTargetKnown)continue;
                Map<String,Object> v=combatView(g.id);int currentHealthy=v.get("currentHealthy") instanceof Number?Math.max(0,((Number)v.get("currentHealthy")).intValue()):g.healthyAttachedStrength;
                int deficit=Math.max(0,g.desiredStrength-currentHealthy-g.reservations.size());
                boolean retreat=flag(v,"retreating")||"OVERMATCHED".equals(v.get("crisis"));
                boolean crisis=flag(v,"crisis")||v.get("crisis") instanceof String&&!"NONE".equals(v.get("crisis"))&&!"NORMAL".equals(v.get("crisis"));
                double pressure=finite(v.get("visiblePressure"))?Math.max(0,number(v,"visiblePressure")):0;
                double urgency=finite(v.get("urgency"))?Math.max(0,number(v,"urgency")):pressure;
                double distance=Math.hypot(number(own,"x")-g.joinTargetX,number(own,"y")-g.joinTargetY);
                double score=(crisis?200000:0)+(retreat?100000:0)+1000.0*deficit+100*Math.min(100,urgency)-distance;
                if(best==null||score>bestScore||score==bestScore&&g.id.value<best.id.value){best=g;bestScore=score;}
            }
            if(best!=null&&registry.requestJoin(unit.unitId,best.id))record("COMMANDER_REINFORCEMENT_RESERVED",fields("unitId",unit.unitId,"generalId",best.id.value,"score",bestScore,"joinTargetSource",best.joinTargetSource));
        }
    }
    /** A supplied small batch can grow one busy, full healthy General before overflow birth.
     * This is an organization limit only; it cannot gate production or reserve a FREE floor. */
    private void growCurrentGeneral(Map<String,Object> state,int desiredStrength){
        int supplied=0;for(GeneralRegistry.UnitView u:registry.units())if(registry.freeCandidateIds().contains(u.unitId)
                &&u.freeSinceFrame<registry.observation().frame&&u.healthObservedFrame==registry.observation().frame&&healthy(find(state,u.unitId)))supplied++;
        if(supplied==0)return;GeneralRegistry.GeneralView best=null;double bestNeed=0;
        for(GeneralRegistry.GeneralView g:registry.generals()){
            Map<String,Object> v=rawCombatView(g.id);ThreatTask task=taskForGeneral(g.id);
            if(g.phase!=GeneralRegistry.Phase.ACTIVE||!combatCurrent(v)||eligibility(g,state)!=null||!g.joinTargetKnown
                    ||g.members.size()!=g.desiredStrength||!g.reservations.isEmpty()||g.healthyAttachedStrength<g.desiredStrength)continue;
            boolean healthyRoster=true;for(Long actor:g.members)if(!healthy(find(state,actor))){healthyRoster=false;break;}if(!healthyRoster)continue;
            double pressure=finite(v.get("visiblePressure"))?number(v,"visiblePressure"):0;
            double need=task!=null&&task.status==ThreatTask.Status.CURRENT?task.demandStrength:pressure/1000.0;
            if(!(need>generalStrength(g,state)))continue;
            int limit=Math.max(LocalArmyDirector.FORM_MINIMUM,desiredStrength)+LocalArmyDirector.FORM_MINIMUM;
            if(g.desiredStrength>=limit)continue;
            if(best==null||need>bestNeed||need==bestNeed&&g.id.value<best.id.value){best=g;bestNeed=need;}
        }
        if(best!=null){int limit=Math.max(LocalArmyDirector.FORM_MINIMUM,desiredStrength)+LocalArmyDirector.FORM_MINIMUM;
            int added=Math.min(Math.min(LocalArmyDirector.FORM_MINIMUM,supplied),limit-best.desiredStrength);
            if(registry.growDesiredStrength(state,best.id,best.desiredStrength+added))record("COMMANDER_CURRENT_GENERAL_GROWTH",fields("generalId",best.id.value,
                    "suppliedFree",supplied,"added",added,"visibleNeed",bestNeed,"organizationLimit",limit,"productionCap",false));}
    }
    private void reconcileTasks(Map<String,Object> state,Map<String,Object> enemies,long now){
        if(!enemyCurrent(state,enemies,now)){threatTaskObservation=null;threatTaskSourceTime=-1;for(Map.Entry<Long,ThreatTask> entry:tasks.entrySet())if(entry.getValue().status!=ThreatTask.Status.CLEAR)
            update(entry.getValue().withStatus(ThreatTask.Status.UNKNOWN,"VISIBLE_SAMPLE_UNKNOWN"));return;}
        // Bind task freshness to the validated combat endpoint while retaining this own
        // observation's allocation authority. The two endpoint frames/clocks need not match.
        threatTaskObservation=registry.observation();threatTaskSourceTime=GameClock.number(enemies.get("gameTimeMs"));
        List<Map<String,Object>> rows=new ArrayList<Map<String,Object>>();Long source=GameClock.number(enemies.get("gameTimeMs"));
        for(Map<String,Object> row:items(enemies,"visibleEnemies"))if(GameClock.number(row.get("id"))!=null&&finite(row.get("x"))&&finite(row.get("y"))
                &&number(row,"hp")>0&&!flag(row,"dead")&&Objects.equals(source,GameClock.number(row.get("lastSeenGameTimeMs"))))rows.add(row);
        Collections.sort(rows,(a,b)->Long.compare(id(a),id(b)));
        Set<Long> usedTasks=new HashSet<Long>(),seen=new HashSet<Long>();
        for(Map<String,Object> seed:rows){if(!seen.add(id(seed)))continue;List<Map<String,Object>> cluster=new ArrayList<Map<String,Object>>();cluster.add(seed);
            for(int i=0;i<cluster.size();i++)for(Map<String,Object> row:rows)if(!seen.contains(id(row))&&distance(cluster.get(i),row)<=LocalCrisisPolicy.CLUSTER_RADIUS){seen.add(id(row));cluster.add(row);}
            Set<Long> ids=new LinkedHashSet<Long>();double x=0,y=0,hp=0;for(Map<String,Object> row:cluster){ids.add(id(row));x+=number(row,"x");y+=number(row,"y");hp+=number(row,"hp");}
            ThreatTask old=null;int overlap=0;for(ThreatTask candidate:tasks.values())if(candidate.status!=ThreatTask.Status.CLEAR&&!usedTasks.contains(candidate.id)){
                int common=0;for(Long enemy:ids)if(candidate.enemyIds.contains(enemy))common++;if(common>overlap){old=candidate;overlap=common;}}
            // Fresh contacts inside an actively searched last-known region belong to that
            // responsibility even when the original enemy ID is no longer visible.
            if(old==null)for(ThreatTask candidate:tasks.values())if(candidate.status==ThreatTask.Status.UNKNOWN&&!usedTasks.contains(candidate.id)
                    &&responseResponsibility(candidate.id)&&Math.hypot(candidate.x-x/cluster.size(),candidate.y-y/cluster.size())<=LocalCrisisPolicy.CLUSTER_RADIUS){old=candidate;break;}
            Long enemyFrame=GameClock.number(enemies.get("frame"));long taskId=old==null?++nextTask:old.id;usedTasks.add(taskId);
            boolean unarmed=true;for(Map<String,Object> row:cluster)if(!Boolean.FALSE.equals(row.get("canAttack"))){unarmed=false;break;}
            update(new ThreatTask(taskId,ids,x/cluster.size(),y/cluster.size(),enemyFrame==null?-1:enemyFrame,source,ThreatTask.Status.CURRENT,"CURRENT_LEGAL_VISIBLE_CLUSTER",hp/1000.0*LocalCrisisPolicy.HP_FACTOR,mobileResponseRolesKnown(cluster,enemies)).withUnarmedContacts(unarmed));
        }
        for(Map.Entry<Long,ThreatTask> entry:tasks.entrySet())if(!usedTasks.contains(entry.getKey())&&entry.getValue().status!=ThreatTask.Status.CLEAR)
            update(entry.getValue().withStatus(responseResponsibility(entry.getKey())?ThreatTask.Status.UNKNOWN:ThreatTask.Status.CLEAR,
                    responseResponsibility(entry.getKey())?"LOST_CONTACT_RESPONSE_RECHECKS_LAST_KNOWN_REGION":"CURRENT_VISIBILITY_DISAPPEARED_NOT_KILL"));
    }
    private boolean responseResponsibility(long task){Map<String,Object> coverage=localCoverage==null?null:localCoverage.view();
        if(coverage==null)return false;for(Map<String,Object> row:items(coverage,"taskResponses"))if(Objects.equals(Long.valueOf(task),GameClock.number(row.get("taskId")))
                &&Boolean.TRUE.equals(row.get("responsibilityActive")))return true;return false;}
    private static boolean mobileResponseRolesKnown(List<Map<String,Object>> rows,Map<String,Object> packet){
        for(Map<String,Object> row:rows){String type=row.get("type") instanceof String?(String)row.get("type"):null;
            // An isolated extractor/factory is cheap local work, not a reason to interrupt
            // a distant whole General. Unknown or armed buildings retain strategic handling.
            if("commandCenter".equals(type)||flag(row,"building")){
                if(Boolean.FALSE.equals(row.get("canAttack")))continue;
                return false;
            }
            if(Boolean.FALSE.equals(row.get("building")))continue;
            CombatProfiles.Profile profile=CombatProfiles.get(type,CombatProfiles.trusted(packet));
            if(profile==null||!(profile.speed>0))return false;
        }return !rows.isEmpty();
    }
    private void assignTasks(Map<String,Object> state,double homeX,double homeY,long now){
        Map<GeneralRegistry.GeneralId,Long> next=new LinkedHashMap<GeneralRegistry.GeneralId,Long>();
        List<ThreatTask> current=new ArrayList<ThreatTask>();
        for(ThreatTask task:tasks.values())if(task.status==ThreatTask.Status.CURRENT)current.add(task);
        // Home raids precede distant contacts. Within a tier, the greatest visible deficit goes first.
        Collections.sort(current,(a,b)->{boolean ah=homeDistance(a,homeX,homeY)<=LocalCrisisPolicy.ASSET_RADIUS,bh=homeDistance(b,homeX,homeY)<=LocalCrisisPolicy.ASSET_RADIUS;
            if(ah!=bh)return ah?-1:1;int demand=Double.compare(b.demandStrength,a.demandStrength);return demand!=0?demand:Long.compare(a.id,b.id);});
        Map<String,Object> coverage=localCoverage==null?Collections.<String,Object>emptyMap():localCoverage.view();
        if(coverage==null)coverage=Collections.emptyMap();Set<Long> usedLocal=new HashSet<Long>();
        for(ThreatTask task:current){
            Set<Long> responders=new LinkedHashSet<Long>(),generalIds=new LinkedHashSet<Long>();double local=0,assigned=0;
            for(Map<String,Object> detached:items(coverage,"taskResponses"))if(Objects.equals(Long.valueOf(task.id),GameClock.number(detached.get("taskId")))
                    &&coverageCurrent(detached)&&Objects.equals(Long.valueOf(task.sourceGameTimeMs),GameClock.number(detached.get("combatSourceTime")))){
                Object ids=detached.get("actorIds");if(ids instanceof Collection)for(Object raw:(Collection<?>)ids){Long actor=GameClock.number(raw);
                    GeneralRegistry.UnitView unit=actor==null?null:registry.unit(actor);Map<String,Object> own=actor==null?null:find(state,actor);
                    if(unit!=null&&unit.temporaryTask==GeneralRegistry.TemporaryTask.LOCAL_RESPONSE&&Objects.equals(unit.owner,detached.get("owner"))
                            &&unit.externalOwner==null&&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&unit.healthObservedFrame==registry.observation().frame
                            &&healthy(own)&&usedLocal.add(actor)){responders.add(actor);local+=number(own,"hp")/1000.0;}
                }
            }
            if(coverageCurrent(coverage)&&task.enemyIds.contains(GameClock.number(coverage.get("targetEnemyId")))){
                Object ids=coverage.get("actorIds");if(ids instanceof Collection)for(Object raw:(Collection<?>)ids){Long actor=GameClock.number(raw);
                    GeneralRegistry.UnitView unit=actor==null?null:registry.unit(actor);Map<String,Object> own=actor==null?null:find(state,actor);
                    if(unit!=null&&unit.temporaryTask==GeneralRegistry.TemporaryTask.LOCAL_RESPONSE&&unit.externalOwner==null
                            &&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&unit.healthObservedFrame==registry.observation().frame
                            &&Objects.equals(unit.owner,coverage.get("owner"))&&healthy(own)&&!usedLocal.contains(actor)){
                        responders.add(actor);local+=number(own,"hp")/1000.0;usedLocal.add(actor);
                    }
                }
            }
            List<Map<String,Object>> decisions=new ArrayList<Map<String,Object>>();
            while(local+assigned+1e-9<task.demandStrength){
                GeneralRegistry.GeneralView best=null;double bestStrength=0,bestCost=Double.MAX_VALUE;
                double deficit=task.demandStrength-local-assigned;
                for(GeneralRegistry.GeneralView g:registry.generals()){
                    if(next.containsKey(g.id)||eligibility(g,state)!=null||!frontlineDemand(g,state,task))continue;
                    double strength=taskStrength(g,state,task),cost=assignmentCost(g,task);if(strength<=0)continue;
                    boolean enough=strength+1e-9>=deficit,bestEnough=bestStrength+1e-9>=deficit;
                    // Prefer one General that closes the gap. Otherwise choose the largest available
                    // roster first, which bounds the required count before distance/cost tie-breaking.
                    if(best==null||enough&&!bestEnough||enough==bestEnough&&(enough?(cost<bestCost||cost==bestCost&&g.id.value<best.id.value)
                            :strength>bestStrength||strength==bestStrength&&(cost<bestCost||cost==bestCost&&g.id.value<best.id.value))){best=g;bestStrength=strength;bestCost=cost;}
                }
                if(best==null)break;
                next.put(best.id,task.id);generalIds.add(best.id.value);assigned+=bestStrength;
            }
            for(GeneralRegistry.GeneralView g:registry.generals()){
                String why=eligibility(g,state);boolean selected=Objects.equals(next.get(g.id),task.id);
                if(selected)why=Objects.equals(assignments.get(g.id),task.id)?"RETAINED_CURRENT_TASK_WITHIN_BUDGET":"SELECTED_TO_CLOSE_VISIBLE_DEFICIT";
                else if(why==null)why=!frontlineDemand(g,state,task)?local+1e-9>=task.demandStrength?"REAL_SMALL_RESPONSE_COVERS_DEMAND":"DETACHMENT_DEMAND_RETAINS_GENERAL_MISSION"
                        :taskStrength(g,state,task)<=0?"CURRENT_COMPATIBLE_RESPONSE_CAPACITY_UNKNOWN_OR_ZERO":next.containsKey(g.id)?"ASSIGNED_OTHER_HIGHER_PRIORITY_TASK":local>=task.demandStrength?"REAL_LOCAL_RESPONSE_COVERS_DEMAND"
                        :local+assigned+1e-9>=task.demandStrength?"DEMAND_ALREADY_COVERED_AVOID_INTERRUPTION":"NO_ADDITIONAL_CURRENT_CAPACITY";
                decisions.add(fields("generalId",g.id.value,"selected",selected,"reason",why,"strength",taskStrength(g,state,task),
                        "distance",g.centroidKnown&&g.centroidFrame==registry.observation().frame?Math.hypot(g.x-task.x,g.y-task.y):null,
                        "interruptionCost",interruptionCost(g,task),"cost",g.centroidKnown&&g.centroidFrame==registry.observation().frame?assignmentCost(g,task):null));
            }
            ThreatTask budget=task.withBudget(local,assigned,generalIds,responders,decisions);tasks.put(task.id,budget);
            // Full decisions remain in snapshots; only material budget/choice changes emit a transition.
            String signature=budgetSignature(budget);if(!Objects.equals(budgetSignatures.put(task.id,signature),signature))
                record("COMMANDER_THREAT_BUDGET",fields("task",budget.metadata(),"freeCoverageReason","FREE_UNCOMMITTED_NOT_CREDITED"));
        }
        for(GeneralRegistry.GeneralView g:registry.generals()){
            ThreatTask previous=taskForGeneral(g.id);
            if(previous!=null&&previous.status==ThreatTask.Status.UNKNOWN&&eligibility(g,state)==null)next.put(g.id,previous.id);
            if(previous!=null&&previous.status==ThreatTask.Status.CLEAR&&g.centroidKnown&&g.centroidFrame==registry.observation().frame){
                Map<String,Object> hold=fields("active",true,"taskId",previous.id,"x",g.x,"y",g.y,"createdAt",now,"expiresAt",now+ATTENTION_HOLD_MS,
                        "sourceSessionId",registry.observation().session,"sourcePlayerKey",registry.observation().player,
                        "sourceFrame",registry.observation().frame,"reason","LOST_CONTACT_LOCAL_ATTENTION_NOT_ATTACK","attackAuthorized",false);
                holds.put(g.id,hold);record("COMMANDER_ATTENTION_HOLD",fields("generalId",g.id.value,"hold",hold));
            }
            Long after=next.get(g.id),before=assignments.get(g.id);
            if(after!=null&&tasks.get(after).status==ThreatTask.Status.CURRENT)holds.remove(g.id);
            if(!Objects.equals(before,after))record("COMMANDER_THREAT_ATTENTION",fields("generalId",g.id.value,"beforeTaskId",before,"taskId",after,
                    "reason",after==null?previous!=null&&previous.status==ThreatTask.Status.CLEAR?"VISIBLE_CLEAR_NOT_KILL":"BUDGET_OR_GENERAL_INELIGIBLE":"VISIBLE_DEFICIT_BUDGET"));
        }
        assignments.clear();assignments.putAll(next);assignmentStamp=registry.observation();
        Iterator<Map.Entry<GeneralRegistry.GeneralId,Map<String,Object>>> held=holds.entrySet().iterator();while(held.hasNext()){
            Map.Entry<GeneralRegistry.GeneralId,Map<String,Object>> entry=held.next();Long expires=GameClock.number(entry.getValue().get("expiresAt"));
            if(expires==null||now>=expires)held.remove();
        }
    }
    private String eligibility(GeneralRegistry.GeneralView g,Map<String,Object> state){
        if(g.phase!=GeneralRegistry.Phase.ACTIVE)return "FORMING_NOT_COMBAT_READY";
        Map<String,Object> view=rawCombatView(g.id);
        if(flag(view,"retreating")||"RETREAT".equals(view.get("crisis"))||"RETREATING".equals(view.get("crisis"))||"REGROUPING".equals(view.get("crisis"))
                ||"OVERMATCHED".equals(view.get("crisis"))||"LOSING_EXCHANGE".equals(view.get("crisis")))return "RETREAT_OR_REGROUP_NOT_DIVERTIBLE";
        if(!view.isEmpty()&&!combatCurrent(view))return "COMBAT_SAMPLE_UNKNOWN";
        if(!g.centroidKnown||g.centroidFrame!=registry.observation().frame)return "CURRENT_CENTROID_UNKNOWN";
        return generalStrength(g,state)>0?null:"NO_CURRENT_HEALTHY_ATTACHED_CAPACITY";
    }
    private double generalStrength(GeneralRegistry.GeneralView general,Map<String,Object> state){double result=0;
        for(Long id:general.members){GeneralRegistry.UnitView unit=registry.unit(id);Map<String,Object> own=find(state,id);
            if(unit!=null&&general.id.equals(unit.generalId)&&unit.membership==GeneralRegistry.Membership.ATTACHED
                    &&unit.temporaryTask==GeneralRegistry.TemporaryTask.NONE&&unit.externalOwner==null&&general.owner.equals(unit.owner)
                    &&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&unit.healthObservedFrame==registry.observation().frame&&healthy(own))result+=number(own,"hp")/1000.0;
        }return result;
    }
    /** Compare with this General's actual available roster, including its borrowed members.
     * A cheap or remote contact cannot become a whole-force mission merely by being a building. */
    private boolean frontlineDemand(GeneralRegistry.GeneralView general,Map<String,Object> state,ThreatTask task){
        double strength=generalStrength(general,state);for(Long actor:general.reservations){GeneralRegistry.UnitView unit=registry.unit(actor);Map<String,Object> own=find(state,actor);
            if(unit!=null&&general.owner.equals(unit.responseOriginOwner)&&unit.healthRole==GeneralRegistry.HealthRole.NORMAL&&healthy(own))strength+=number(own,"hp")/1000.0;}
        double armedDemand=0;for(Map<String,Object> enemy:items(currentEnemies,"visibleEnemies"))if(task.enemyIds.contains(id(enemy))&&flag(enemy,"canAttack"))armedDemand+=number(enemy,"hp")/1000.0*LocalCrisisPolicy.HP_FACTOR;
        return strength>0&&armedDemand+1e-9>=strength&&general.centroidKnown&&general.centroidFrame==registry.observation().frame
                &&Math.hypot(general.x-task.x,general.y-task.y)<=LocalCrisisPolicy.RESPONSE_RADIUS;
    }
    /** Allocation HP is coarse, but a known incompatible weapon cannot cover an AIR need. */
    private double taskStrength(GeneralRegistry.GeneralView general,Map<String,Object> state,ThreatTask task){
        if(!CombatProfiles.trusted(currentEnemies))return generalStrength(general,state);
        double result=0;for(Long id:general.members){GeneralRegistry.UnitView unit=registry.unit(id);Map<String,Object> own=find(state,id);
            if(unit==null||!general.owner.equals(unit.owner)||unit.healthRole!=GeneralRegistry.HealthRole.NORMAL||!healthy(own))continue;
            boolean compatible=true,seen=false;for(Map<String,Object> enemy:items(currentEnemies,"visibleEnemies"))if(task.enemyIds.contains(id(enemy))){seen=true;
                String domain=CombatProfiles.currentDomain(enemy,currentEnemies);Boolean water=enemy.get("touchingWater") instanceof Boolean?(Boolean)enemy.get("touchingWater"):null;
                if(!"COMPATIBLE".equals(TargetCatalog.evaluate((String)own.get("type"),domain,water,domain!=null,true).status)){compatible=false;break;}}
            if(seen&&compatible)result+=number(own,"hp")/1000.0;
        }return result;
    }
    private double interruptionCost(GeneralRegistry.GeneralView g,ThreatTask task){
        if(!g.goalKnown||g.targetEnemyId!=null&&task.enemyIds.contains(g.targetEnemyId))return 0;
        return 600+Math.min(1800,Math.hypot(g.goalX-task.x,g.goalY-task.y));
    }
    private double assignmentCost(GeneralRegistry.GeneralView g,ThreatTask task){return Math.hypot(g.x-task.x,g.y-task.y)+interruptionCost(g,task)
            -(Objects.equals(assignments.get(g.id),task.id)?350:0);}
    private boolean coverageCurrent(Map<String,Object> view){CommandArbiter.Stamp stamp=registry.observation();return flag(view,"ownCurrent")
            &&stamp.session.equals(view.get("sourceSessionId"))&&stamp.player.equals(view.get("sourcePlayerKey"))
            &&Objects.equals(Long.valueOf(stamp.frame),GameClock.number(view.get("sourceFrame")))&&Objects.equals(Long.valueOf(stamp.gameTimeMs),GameClock.number(view.get("gameTimeMs")));}
    private static double homeDistance(ThreatTask task,double x,double y){return Double.isFinite(x)&&Double.isFinite(y)?Math.hypot(task.x-x,task.y-y):Double.MAX_VALUE;}
    private static String budgetSignature(ThreatTask task){StringBuilder result=new StringBuilder();result.append(Math.round(task.demandStrength*100)).append('/').append(Math.round(task.localCoveredStrength*100))
            .append('/').append(Math.round(task.assignedStrength*100)).append('/').append(task.assignedGeneralIds).append('/').append(task.localResponderIds);
        for(Map<String,Object> decision:task.allocationDecisions)result.append('/').append(decision.get("generalId")).append(':').append(decision.get("reason"));return result.toString();}
    /** Only current task membership authorizes remote mobile threat pursuit. Strategic objectives
     * and immediate self-defence retain their independent combat admission in the caller. */
    public Set<Long> generalAssignedEnemyIds(GeneralRegistry.GeneralId general){ThreatTask task=taskForGeneral(general);
        CommandArbiter.Stamp own=registry.observation();return assignmentStamp!=null&&own.samePlayer(assignmentStamp)&&own.frame==assignmentStamp.frame&&own.gameTimeMs==assignmentStamp.gameTimeMs
                &&task!=null&&task.status==ThreatTask.Status.CURRENT?task.enemyIds:Collections.<Long>emptySet();}
    public boolean allowsTarget(GeneralRegistry.GeneralId general,long enemy){return generalAssignedEnemyIds(general).contains(enemy);}
    /** A bounded current-location hold, never a last-known-enemy attack or search instruction. */
    public Map<String,Object> holdForGeneral(GeneralRegistry.GeneralId general){Map<String,Object> hold=holds.get(general);Long expires=hold==null?null:GameClock.number(hold.get("expiresAt"));
        return expires!=null&&registry.observation().session.equals(hold.get("sourceSessionId"))&&registry.observation().player.equals(hold.get("sourcePlayerKey"))
                &&registry.observation().gameTimeMs<expires?Collections.unmodifiableMap(hold):Collections.<String,Object>emptyMap();}
    public ThreatTask taskForGeneral(GeneralRegistry.GeneralId general){Long id=assignments.get(general);return id==null?null:tasks.get(id);}
    public Map<GeneralRegistry.GeneralId,Long> preferredThreats(){Map<GeneralRegistry.GeneralId,Long> result=new LinkedHashMap<GeneralRegistry.GeneralId,Long>();
        for(GeneralRegistry.GeneralId id:assignments.keySet()){ThreatTask task=taskForGeneral(id);if(!generalAssignedEnemyIds(id).isEmpty()&&task.primaryEnemyId()!=null)result.put(id,task.primaryEnemyId());}return Collections.unmodifiableMap(result);}
    public List<Map<String,Object>> drainChanges(){List<Map<String,Object>> out=new ArrayList<Map<String,Object>>(changes);changes.clear();return out;}
    public Map<String,Object> snapshot(){List<Map<String,Object>> rows=new ArrayList<Map<String,Object>>();for(ThreatTask task:tasks.values())rows.add(task.metadata());
        List<Map<String,Object>> attention=new ArrayList<Map<String,Object>>();for(Map.Entry<GeneralRegistry.GeneralId,Long> entry:assignments.entrySet())attention.add(fields("generalId",entry.getKey().value,"taskId",entry.getValue()));
        List<Map<String,Object>> holdRows=new ArrayList<Map<String,Object>>();for(GeneralRegistry.GeneralId id:holds.keySet()){Map<String,Object> hold=holdForGeneral(id);
            if(!hold.isEmpty()){Map<String,Object> row=new LinkedHashMap<String,Object>(hold);row.put("generalId",id.value);holdRows.add(row);}}
        return fields("tasks",rows,"generalAttention",attention,"attentionHolds",holdRows,"superGeneral",false);}
    private void update(ThreatTask task){ThreatTask old=tasks.put(task.id,task);if(old==null||old.status!=task.status||!old.enemyIds.equals(task.enemyIds))
        record("COMMANDER_THREAT_TASK",fields("before",old==null?null:old.metadata(),"after",task.metadata()));}
    private Map<String,Object> rawCombatView(GeneralRegistry.GeneralId id){Map<String,Object> view=combat==null?null:combat.view(id);return view==null?Collections.<String,Object>emptyMap():view;}
    private Map<String,Object> combatView(GeneralRegistry.GeneralId id){Map<String,Object> view=rawCombatView(id);return combatCurrent(view)?view:Collections.<String,Object>emptyMap();}
    private boolean combatCurrent(Map<String,Object> view){CommandArbiter.Stamp own=registry.observation();return flag(view,"ownCurrent")
            &&own.session.equals(view.get("sourceSessionId"))&&Objects.equals(Long.valueOf(own.frame),GameClock.number(view.get("sourceFrame")))
            &&Objects.equals(Long.valueOf(own.gameTimeMs),GameClock.number(view.get("gameTimeMs")));}
    private void record(String reason,Map<String,Object> details){Map<String,Object> row=fields("reason",reason,"sourceFrame",registry.observation().frame,"gameTimeMs",registry.observation().gameTimeMs);row.putAll(details);changes.add(row);}
    private static boolean legalHome(Map<String,Object> state,double x,double y){if(!Double.isFinite(x)||!Double.isFinite(y))return false;
        for(Map<String,Object> unit:items(state,"ownUnits"))if(ready(unit)&&("commandCenter".equals(unit.get("type"))||flag(unit,"building"))&&Math.hypot(number(unit,"x")-x,number(unit,"y")-y)<=1)return true;return false;}
    private boolean enemyCurrent(Map<String,Object> state,Map<String,Object> enemies,long now){if(enemies==null||!(enemies.get("visibleEnemies") instanceof List))return false;
        Long source=GameClock.number(enemies.get("gameTimeMs")),frame=GameClock.number(enemies.get("frame"));
        if(!(state.get("sessionId") instanceof String)||!state.get("sessionId").equals(enemies.get("sessionId"))||source==null||source<now
                ||frame!=null&&frame<GameClock.number(state.get("frame"))
                ||enemies.get("playerKey") instanceof String&&!Objects.equals(registry.observation().player,enemies.get("playerKey")))return false;
        Object player=enemies.get("player"),ownPlayer=state.get("player");Object team=player instanceof Map?((Map<?,?>)player).get("teamId"):null;
        Object ownTeam=ownPlayer instanceof Map?((Map<?,?>)ownPlayer).get("teamId"):null;
        if(team instanceof Number&&!registry.observation().player.equals("team:"+((Number)team).longValue()))return false;
        for(Map<String,Object> row:items(enemies,"visibleEnemies"))if(GameClock.number(row.get("id"))==null||!finite(row.get("x"))||!finite(row.get("y"))
                ||!finite(row.get("hp"))||!Objects.equals(source,GameClock.number(row.get("lastSeenGameTimeMs"))))return false;return true;}
    private static boolean ready(Map<String,Object> unit){return unit!=null&&finite(unit.get("x"))&&finite(unit.get("y"))&&number(unit,"hp")>0&&!flag(unit,"dead")&&number(unit,"buildProgress")>=1;}
    private static boolean healthy(Map<String,Object> unit){return ready(unit)&&number(unit,"maxHp")>0&&number(unit,"hp")>=number(unit,"maxHp")*.5;}
    private static boolean flag(Map<String,Object> map,String key){return Boolean.TRUE.equals(map.get(key));}
    private static boolean finite(Object value){return value instanceof Number&&Double.isFinite(((Number)value).doubleValue());}
    private static double number(Map<String,Object> map,String key){Object value=map.get(key);return value instanceof Number?((Number)value).doubleValue():Double.NaN;}
    private static long id(Map<String,Object> row){return ((Number)row.get("id")).longValue();}
    private static double distance(Map<String,Object> a,Map<String,Object> b){return Math.hypot(number(a,"x")-number(b,"x"),number(a,"y")-number(b,"y"));}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> items(Map<String,Object> map,String key){return map.get(key) instanceof List?(List<Map<String,Object>>)map.get(key):Collections.<Map<String,Object>>emptyList();}
    private static Map<String,Object> find(Map<String,Object> state,long id){for(Map<String,Object> unit:items(state,"ownUnits"))if(id(unit)==id)return unit;return null;}
    private static Map<String,Object> fields(Object... pairs){Map<String,Object> result=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;}
}
