package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.GeneralRegistry.*;

/** Same-frame synthetic roster refresh; no native healing, exchange or arrival claim. */
public final class GeneralRosterRefreshHarness {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Map<String,Object> map(Object... pairs){Map<String,Object> result=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;}
    private static double n(Map<String,Object> row,String key){return ((Number)row.get(key)).doubleValue();}
    public static void main(String[] args)throws Exception{
        CommandArbiter gate=new CommandArbiter();GeneralRegistry registry=new GeneralRegistry(gate);List<Long> ids=new ArrayList<Long>();
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),enemiesRows=new ArrayList<Map<String,Object>>();final List<ForceController.Proposal> proposals=new ArrayList<ForceController.Proposal>();
        for(long id=1;id<=12;id++){ids.add(id);own.add(map("id",id,"type","tank","canAttack",true,"x",id<=6?100:1400,"y",100,"hp",1000,"maxHp",1000,"buildProgress",1));}
        Map<String,Object> state=map("sessionId","s","playerKey","team:0","frame",1L,"gameTimeMs",1000L,"ownUnits",own);
        enemiesRows.add(map("id",501L,"type","tank","canAttack",true,"x",100,"y",100,"hp",1000,"lastSeenGameTimeMs",1000L));
        enemiesRows.add(map("id",502L,"type","tank","canAttack",true,"x",1450,"y",100,"hp",500,"lastSeenGameTimeMs",1000L));
        Map<String,Object> enemies=map("sessionId","s","playerKey","team:0","frame",1L,"gameTimeMs",1000L,"visibleEnemies",enemiesRows);
        ForceController.Host host=new ForceController.Host(){public Map<String,Object> read(String path,String event){return Collections.emptyMap();}
            public List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target,Map<String,Object> contacts){return actors;}
            public void collect(ForceController.Proposal proposal){proposals.add(proposal);}};
        gate.observe(new CommandArbiter.Stamp("s","team:0",1,1000),ids);registry.bootstrap(Arrays.<Collection<Long>>asList(ids),ids);
        GeneralId general=registry.generals().get(0).id;registry.updateGeneralCentroid(general,750,100);GeneralCombatDirector combat=new GeneralCombatDirector(registry,host);
        combat.observe(state,enemies,100,100,1000);check(n(combat.view(general),"currentMembers")==12,"first observation counts original twelve members");
        state.put("frame",2L);state.put("gameTimeMs",2000L);enemies.put("frame",2L);enemies.put("gameTimeMs",2000L);for(Map<String,Object> enemy:enemiesRows)enemy.put("lastSeenGameTimeMs",2000L);
        own.get(6).put("hp",900);gate.observe(new CommandArbiter.Stamp("s","team:0",2,2000),ids);for(Long id:ids)registry.updateHealthRole(id,HealthRole.NORMAL);
        registry.updateGeneralCentroid(general,750,100);combat.observe(state,enemies,100,100,2000);Map<String,Object> before=combat.view(general);
        check(n(before,"recentOwnDamage")==100,"bounded second observation detects one real synthetic HP decrease");
        for(long id=1;id<=6;id++)check(registry.beginLocalResponse(id,"local-response:refresh"),"detachment transfers actual owner");
        registry.updateGeneralCentroid(general,1400,100);combat.refreshCurrentRoster(state,enemies,100,100,2000);Map<String,Object> after=combat.view(general);
        check(registry.general(general).x==1400&&registry.general(general).members.size()==6,"caller supplies current remaining-member centroid1400");
        check(n(after,"currentMembers")==6&&n(after,"currentHealthy")==6,"same-frame refresh counts six current attached members");
        check(n(after,"ownHp")==5900&&n(after,"ownMaxHp")==6000,"same-frame aggregate excludes detached LR HP");
        check(n(after,"visiblePressure")==500,"pressure uses new centroid and nearby front threat, excludes home threat");
        check(n(after,"recentOwnDamage")==100&&Objects.equals(before.get("weakVisibleHpDecrease"),after.get("weakVisibleHpDecrease")),"refresh cannot repeat own/enemy damage deltas");
        for(String key:Arrays.asList("observedOwnDeaths","hpDeltaWindow","lastObservationGapMs","unknownWindowOwnHpDecrease","unknownWindowWeakHpDecrease","crisis","retreating","commandRevision","revision","escapeDamageWitness"))
            check(Objects.equals(before.get(key),after.get(key)),"refresh preserves temporal/crisis field "+key);
        Map<Long,Long> generations=new HashMap<Long,Long>();for(UnitView u:registry.units())generations.put(u.unitId,u.ownerGeneration);
        combat.refreshCurrentRoster(state,enemies,100,100,2000);check(combat.view(general).equals(after),"duplicate refresh is idempotent without trend or revision accumulation");
        for(UnitView u:registry.units())check(generations.get(u.unitId)==u.ownerGeneration,"read refresh cannot mutate owner generation");
        CommanderDirector commander=new CommanderDirector(registry);commander.setCombatProvider(combat::view);commander.reconcile(state,enemies,100,100,2000,24);combat.setCommander(commander);
        combat.collect(state,enemies,2000);check(proposals.size()==1&&proposals.get(0).path.contains("x=1450.0"),"current threat selection uses remaining centroid and nearby front target");
        check(proposals.get(0).actors.size()==6&&!proposals.get(0).actors.contains(1L),"detached actors never re-enter General commands");
        Map<String,Object> foreign=new LinkedHashMap<String,Object>(state);foreign.put("sessionId","foreign");combat.refreshCurrentRoster(foreign,enemies,100,100,2000);
        check(n(combat.view(general),"recentOwnDamage")==100&&n(combat.view(general),"currentMembers")==6,"foreign own sample cannot refresh roster facts");
        System.out.println("GeneralRosterRefreshHarness PASS checks="+checks);
    }
}
