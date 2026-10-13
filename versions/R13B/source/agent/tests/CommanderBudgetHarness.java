package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.GeneralRegistry.*;

/** Synthetic legal snapshots; HP allocation proxies are not native combat acceptance. */
public final class CommanderBudgetHarness {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static Map<String,Object> map(Object... pairs){Map<String,Object> out=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    private static final class Fixture {
        final CommandArbiter gate=new CommandArbiter();final GeneralRegistry registry=new GeneralRegistry(gate);final CommanderDirector commander=new CommanderDirector(registry);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),visible=new ArrayList<Map<String,Object>>();
        final Map<String,Object> state=map("sessionId","s","playerKey","team:0","ownUnits",own),enemies=map("sessionId","s","playerKey","team:0","visibleEnemies",visible);
        final Map<GeneralId,Map<String,Object>> combat=new HashMap<GeneralId,Map<String,Object>>();Map<String,Object> coverage=map();long frame;
        Fixture(int generals,int members,int free){List<Collection<Long>> groups=new ArrayList<Collection<Long>>();List<Long> ordinary=new ArrayList<Long>();
            for(int g=0;g<generals;g++){List<Long> group=new ArrayList<Long>();for(int n=0;n<members;n++){long id=g*members+n+1;group.add(id);ordinary.add(id);own.add(actor(id,800+g*100));}groups.add(group);}
            for(int n=0;n<free;n++){long id=generals*members+n+1;ordinary.add(id);own.add(actor(id,100));}
            own.add(map("id",999L,"type","commandCenter","building",true,"x",100,"y",100,"hp",3000,"maxHp",3000,"buildProgress",1));
            observe();registry.bootstrap(groups,ordinary,24,100,100);refresh();commander.setCombatProvider(id->combat.get(id));commander.setLocalCoverageProvider(()->coverage);
        }
        Map<String,Object> actor(long id,double x){return map("id",id,"type","tank","hp",1000,"maxHp",1000,"x",x,"y",100,"buildProgress",1);}
        void refresh(){for(UnitView u:registry.units())registry.updateHealthRole(u.unitId,u.healthRole);for(GeneralView g:registry.generals())if(!g.members.isEmpty())registry.updateGeneralCentroid(g.id,800+(g.id.value-1)*100,100);}
        void observe(){frame++;state.put("frame",frame);state.put("gameTimeMs",frame*1000);enemies.put("frame",frame);enemies.put("gameTimeMs",frame*1000);
            for(Map<String,Object> enemy:visible)enemy.put("lastSeenGameTimeMs",frame*1000);List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:own)ids.add(((Number)actor.get("id")).longValue());
            gate.observe(new CommandArbiter.Stamp("s","team:0",frame,frame*1000),ids);refresh();}
        void enemy(long id,double hp,double x){visible.add(map("id",id,"type","tank","building",false,"canAttack",true,"hp",hp,"x",x,"y",100,"lastSeenGameTimeMs",frame*1000));}
        void run(){for(Map<String,Object> v:combat.values()){v.put("ownCurrent",true);v.put("sourceSessionId","s");v.put("sourceFrame",frame);v.put("gameTimeMs",frame*1000);}
            commander.reconcile(state,enemies,100,100,frame*1000,24);}
        GeneralId general(int index){return registry.generals().get(index).id;}
        int assigned(){return commander.preferredThreats().size();}
        ThreatTask task(){for(GeneralView g:registry.generals()){ThreatTask task=commander.taskForGeneral(g.id);if(task!=null)return task;}throw new AssertionError("No assigned task");}
        Map<String,Object> taskRow(){@SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)commander.snapshot().get("tasks");return rows.get(rows.size()-1);}
        Map<Long,Long> generations(){Map<Long,Long> result=new LinkedHashMap<Long,Long>();for(UnitView unit:registry.units())result.put(unit.unitId,unit.ownerGeneration);return result;}
        void coverage(long target,String owner,List<Long> ids){coverage=map("ownCurrent",true,"sourceSessionId","s","sourcePlayerKey","team:0","sourceFrame",frame,"gameTimeMs",frame*1000,"targetEnemyId",target,"owner",owner,"actorIds",ids);}
    }
    private static void minimalAndStable(){Fixture small=new Fixture(6,6,0);small.enemy(501,1000,4000);Map<Long,Long> unchanged=small.generations();small.run();
        check(small.assigned()==0&&((Number)small.taskRow().get("deficit")).doubleValue()==1.5,"distant small task without real local capacity retains explicit deficit and original General missions");
        check(small.generations().equals(unchanged)&&!small.commander.currentThreatTasks().isEmpty(),"uncovered small task exists without allocating or transferring any General");
        // R13 keeps sub-roster remote demand on the detachment path. Whole-General budget
        // fixtures must represent current armed frontline demand at the actual roster scale
        // and within current response range; a far-away cluster keeps regional responsibility.
        Fixture subRoster=new Fixture(6,12,0);subRoster.enemy(501,5000,1800);subRoster.run();
        check(subRoster.assigned()==0&&((Number)subRoster.taskRow().get("deficit")).doubleValue()==7.5,"armed remote demand below current roster strength preserves every General mission");
        Fixture f=new Fixture(6,12,0);f.enemy(501,8000,1800);Map<Long,Long> before=f.generations();f.run();
        check(f.assigned()==1,"frontline-scale armed task draws only one sufficient independent General");ThreatTask task=f.task();check(task.demandStrength==12&&task.assignedStrength==12&&task.deficitStrength==0,"current armed frontline demand is covered by one actual roster");
        GeneralId chosen=f.commander.preferredThreats().keySet().iterator().next();check(chosen.equals(f.general(5)),"distance selects nearest uncommitted General");
        check(f.commander.generalAssignedEnemyIds(chosen).contains(501L)&&f.commander.allowsTarget(chosen,501),"assigned current enemy receives exact hard eligibility");
        check(!f.commander.allowsTarget(f.general(0),501)&&!f.commander.allowsTarget(chosen,999),"unassigned and ghost targets have no Commander permission");
        f.observe();check(!f.commander.allowsTarget(chosen,501),"new own observation cannot reuse an unreconciled old task permit");
        f.registry.updateGeneralCentroid(f.general(4),1310,100);f.run();check(f.commander.preferredThreats().containsKey(chosen),"small distance jitter preserves current-task assignment");
        check(f.generations().equals(before),"attention cannot change independent owner generations");
        for(GeneralView g:f.registry.generals())check(g.members.size()==12&&g.owner.equals(g.id.owner()),"independent rosters and owners remain intact");
        check(f.taskRow().containsKey("allocationDecisions"),"snapshot records selected and unselected General reasons");
        boolean budgetTrace=false;for(Map<String,Object> row:f.commander.drainChanges())if("COMMANDER_THREAT_BUDGET".equals(row.get("reason")))budgetTrace=true;
        check(budgetTrace,"material allocation transition has causal budget trace");
    }
    private static void growthAndEligibility(){Fixture f=new Fixture(6,6,0);f.enemy(501,1000,1800);f.run();check(f.assigned()==0&&((Number)f.taskRow().get("deficit")).doubleValue()==1.5,"small task never starts a whole-General diversion");
        f.visible.get(0).put("hp",10000);f.observe();f.run();check(f.assigned()==3&&f.task().deficitStrength==0,"visible growth to demand15 recruits only three capacity6 Generals");
        f.combat.put(f.general(5),map("retreating",true,"crisis","RETREATING"));f.run();check(!f.commander.preferredThreats().containsKey(f.general(5))&&f.assigned()==3,"retreating General is replaced by eligible capacity");
        f.visible.get(0).put("hp",30000);f.observe();f.run();check(f.assigned()==5&&f.task().deficitStrength==15,"over-budget visible threat reports remaining deficit without stealing retreat General");
        Fixture forming=new Fixture(1,3,0);forming.enemy(501,1000,800);forming.run();check(forming.assigned()==0,"FORMING General never receives remote combat task");
        check(((Number)forming.taskRow().get("deficit")).doubleValue()==1.5,"FORMING-ineligible capacity remains a visible unmet demand");
    }
    private static void realLocalCoverage(){Fixture f=new Fixture(6,6,2);f.enemy(501,1000,200);f.run();check(f.assigned()==0&&((Number)f.taskRow().get("localCovered")).doubleValue()==0&&((Number)f.taskRow().get("deficit")).doubleValue()==1.5,"uncommitted FREE cannot manufacture coverage or pull whole General for a small task");
        String owner="local-response:budget";check(f.registry.beginLocalResponse(37,owner)&&f.registry.beginLocalResponse(38,owner),"existing low-cost FREE path obtains actual LR leases");
        f.coverage(501,owner,Arrays.asList(37L,38L));f.run();check(f.assigned()==0,"actual native-compatible LR coverage keeps all six Generals on original work");
        check(((Number)f.taskRow().get("localCovered")).doubleValue()==2&&((Number)f.taskRow().get("deficit")).doubleValue()==0,"LR current health contributes transparent budget");
        f.coverage.put("sourceFrame",0);f.run();check(f.assigned()==0&&((Number)f.taskRow().get("deficit")).doubleValue()==1.5,"stale LR provenance receives no coverage credit and leaves explicit small deficit");
        f.coverage(502,owner,Arrays.asList(37L,38L));f.run();check(((Number)f.taskRow().get("localCovered")).doubleValue()==0,"LR for another target cannot cover this task");
        f.coverage(501,"foreign-owner",Arrays.asList(37L,38L));f.run();check(((Number)f.taskRow().get("localCovered")).doubleValue()==0,"owner mismatch cannot count LR actors");
    }
    private static void interruption(){Fixture f=new Fixture(2,12,0);f.enemy(501,8000,800);f.registry.updateGeneralGoal(f.general(0),5000,100,9999L);f.run();
        check(f.commander.preferredThreats().containsKey(f.general(1)),"nearby General with distant original objective loses to cheap available response");
        f.registry.updateGeneralGoal(f.general(0),800,100,501L);f.run();check(f.assigned()==1,"accepted existing task target has no interruption penalty");
    }
    private static void separateTasks(){Fixture f=new Fixture(6,12,0);f.enemy(501,8000,800);f.enemy(502,8000,2000);f.run();
        check(f.assigned()==2,"two separated large visible clusters need only two sufficient independent Generals");Set<Long> taskIds=new HashSet<Long>();
        for(GeneralId id:f.commander.preferredThreats().keySet()){taskIds.add(f.commander.taskForGeneral(id).id);check(f.commander.generalAssignedEnemyIds(id).size()==1,"each selected General receives only its cluster enemy IDs");}
        check(taskIds.size()==2,"one General allocation cannot be counted against two tasks");
    }
    private static void contactBoundary(){Fixture f=new Fixture(6,12,0);f.enemy(501,8000,1600);f.run();GeneralId chosen=f.commander.preferredThreats().keySet().iterator().next();long task=f.task().id;
        f.enemies.put("sessionId","foreign");f.run();check(f.commander.taskForGeneral(chosen).status==ThreatTask.Status.UNKNOWN,"foreign observation retains unknown attention");
        check(f.commander.generalAssignedEnemyIds(chosen).isEmpty(),"unknown attention never authorizes a ghost attack");
        f.enemies.put("sessionId","s");f.visible.clear();f.observe();f.run();check(f.assigned()==0&&f.commander.taskForGeneral(chosen)==null,"legal visibility disappearance immediately clears attack allocation");
        Map<String,Object> hold=f.commander.holdForGeneral(chosen);check(!hold.isEmpty()&&Boolean.FALSE.equals(hold.get("attackAuthorized")),"short loss retains non-attacking attention window");
        check(((Number)hold.get("x")).doubleValue()==f.registry.general(chosen).x,"hold is at own current centroid, never hidden enemy position");
        long expires=((Number)hold.get("expiresAt")).longValue();f.run();check(((Number)f.commander.holdForGeneral(chosen).get("expiresAt")).longValue()==expires,"same cleared contact cannot refresh the hold forever");
        check("CLEAR".equals(f.taskRow().get("status"))&&Boolean.FALSE.equals(f.taskRow().get("killClaim")),"lost contact is CLEAR and never a kill");
        for(int i=0;i<3;i++){f.observe();f.run();}check(f.commander.holdForGeneral(chosen).isEmpty(),"hold expires after bounded game-time window");
        f.enemy(501,8000,1600);f.run();check(f.task().id!=task&&f.assigned()==1,"legal reappearance creates fresh visible task with bounded budget");
        f.registry.invalidateGeneral(chosen);f.run();check(!f.commander.preferredThreats().containsKey(chosen),"invalidated independent owner loses attention");
    }
    static void compatibleCapacity(){Fixture f=new Fixture(2,12,0);f.enemy(501,7950,1600);Map<String,Object> enemy=f.visible.get(0);
        f.enemies.put("catalogGameJarMatched",true);f.enemies.put("catalogSha256",TargetCatalog.catalogSha256());
        enemy.put("targetDomain","AIR");enemy.put("domainObservedAtGameTimeMs",f.frame*1000);f.run();
        check(f.assigned()==0,"known ground-only tank capacity cannot cover visible AIR task");
        f.enemy(502,50,1600);Map<String,Object> ground=f.visible.get(1);ground.put("targetDomain","SURFACE");ground.put("domainObservedAtGameTimeMs",f.frame*1000);f.run();
        check(f.assigned()==0,"ground decoy in mixed cluster cannot turn ground-only HP into anti-air coverage");
        for(Map<String,Object> unit:f.own)if("tank".equals(unit.get("type")))unit.put("type","scout");f.run();
        check(f.assigned()==1,"compatible current anti-air roster closes AIR response budget without all Generals");
    }
    public static void main(String[] args){minimalAndStable();growthAndEligibility();realLocalCoverage();interruption();separateTasks();contactBoundary();compatibleCapacity();System.out.println("CommanderBudgetHarness PASS checks="+checks);}
}
