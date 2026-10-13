package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Actual policy/ownership paths with synthetic lawful quotes and own snapshots; not natural RTS proof. */
public final class G55OperationsHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Object call(Object o,String name,Class<?>[] types,Object... args)throws Exception{return G53ContinuingOperationsHarness.call(o,name,types,args);}
    static Map<String,Object> action(String type,int cost){return map("type",type,"actionId","native-"+type,"cost",cost,"affordable",true);}
    static void production()throws Exception{
        System.setProperty("rwagent.g5","true");System.clearProperty("rwagent.allowSmallTanks");
        BattleClient b=new BattleClient();Map<String,Object> state=map("ownUnits",Collections.emptyList(),"player",map("credits",10000.));
        G53ContinuingOperationsHarness.set(b,"lastState",state);
        Map<String,Object> tank=action("c_tank",350),heavy=action("heavyTank",800),scout=action("scout",600),upgrade=action("upgrade",2000);
        Map<String,Object> t1=map("id",50L,"queue",0,"tier",1,"actions",Arrays.asList(tank,scout,upgrade));
        Map<String,Object> t2=map("id",50L,"queue",0,"tier",2,"actions",Arrays.asList(tank,heavy));
        check(call(b,"preferredAction",new Class<?>[]{Map.class,Map.class,int.class},t1,state,0)==null,"T1 has no forbidden ordinary fallback, not a fake heavy route");
        check(call(b,"preferredAction",new Class<?>[]{Map.class,Map.class,int.class},t2,state,0)==heavy,"current T2 menu chooses heavy even when small tank exists");
        check(call(b,"bestAffordableUnit",new Class<?>[]{Map.class},t1)==null,"diagnostic fallback cannot nominate a forbidden tank");
        check(((List<?>)call(b,"ordinaryRoutes",new Class<?>[]{Map.class},t1)).isEmpty(),"capacity diagnosis excludes forbidden routes");
        double price=(Double)call(b,"techReplacementNativeCost",new Class<?>[]{Map.class,Map.class},t1,null);
        check(price==600&&ProductionCapacity.techInvestment(upgrade,0,0,2600,0,price,0,0,true,true).selected,"zero-army opening can fund real upgrade with current permitted scout quote");
        t1.put("actions",Arrays.asList(tank,upgrade));price=(Double)call(b,"techReplacementNativeCost",new Class<?>[]{Map.class,Map.class},t1,null);
        check(price==0&&ProductionCapacity.techInvestment(upgrade,0,0,2000,0,price,0,0,true,true).selected,"no permitted replacement funds only current native upgrade without invented T2 price");
        System.setProperty("rwagent.allowSmallTanks","true");BattleClient opt=new BattleClient();
        G53ContinuingOperationsHarness.set(opt,"lastState",state);
        check(call(opt,"preferredAction",new Class<?>[]{Map.class,Map.class,int.class},t1,state,0)==tank,"explicit future experiment can enable small tanks");System.clearProperty("rwagent.allowSmallTanks");
    }
    static void scoutOwnership()throws Exception{
        System.setProperty("rwagent.globalStrategy","false");
        try(G53ContinuingOperationsHarness.Fixture f=new G53ContinuingOperationsHarness.Fixture()){
            f.own.get(1).put("type","scout");f.own.get(1).put("mobile",true);Map<String,Object> state=f.sample(1);
            f.arbiter.release("force:service-test");GeneralRegistry registry=(GeneralRegistry)G53ContinuingOperationsHarness.get(f.battle,"generals");
            check(registry.admitFree(10,GeneralRegistry.HealthRole.NORMAL),"opening scout enters existing FREE admission");
            G53ContinuingOperationsHarness.set(f.battle,"g4Bootstrapped",true);
            call(f.battle,"reserveOpeningScout",new Class<?>[]{Map.class},state);
            check("recon:0".equals(registry.unit(10).owner)&&registry.freeCandidateIds().isEmpty(),"opening scout reserved before Commander reinforcement without counting as General member");
            long generation=f.arbiter.ownerGeneration(10);
            check(Boolean.TRUE.equals(call(f.battle,"claimRecon",new Class<?>[]{long.class,long.class},1L,10L)),"normal Recon task takes the dedicated scout through registry transfers");
            check("recon:1".equals(registry.unit(10).owner)&&f.arbiter.ownerGeneration(10)>generation,"real owner generation changes, no shared General proxy");
            registry.releaseExternal(10,"recon:1");call(f.battle,"reserveOpeningScout",new Class<?>[]{Map.class},state);
            check("recon:0".equals(registry.unit(10).owner),"completed frontier preserves scout for further legal Recon instead of reinforcement");
        }
    }
    static void selectedTechFunding()throws Exception{
        try(G53ContinuingOperationsHarness.Fixture f=new G53ContinuingOperationsHarness.Fixture()){
            f.sample(1);G53ContinuingOperationsHarness.set(f.battle,"openingScoutOrdered",true);
            Map<String,Object> quote=action("upgrade",2000),factory=map("id",100L,"queue",0,"tier",1,"actions",Arrays.asList(action("c_tank",350),quote));
            call(f.battle,"maintainNativeTechFunding",new Class<?>[]{List.class,Map.class},Arrays.asList(factory),map("factories",Arrays.asList(factory)));
            check(((Number)G53ContinuingOperationsHarness.get(f.battle,"investmentReserve")).longValue()==2000,"one actual T1 invoice protects tech from unrelated continuing worker/unit purchases");
            check(Boolean.FALSE.equals(call(f.battle,"ordinaryFundsAvailable",new Class<?>[]{Map.class,double.class},map("player",map("credits",2500.)),800.)),"selected tech funds are counted once across independent ordinary purchases");
            factory.put("actions",Arrays.asList(action("heavyTank",800)));
            call(f.battle,"maintainNativeTechFunding",new Class<?>[]{List.class,Map.class},Arrays.asList(factory),map("factories",Arrays.asList(factory)));
            check(((Number)G53ContinuingOperationsHarness.get(f.battle,"investmentReserve")).longValue()==0,"upgrade quote withdrawal releases selected funds without claiming tier completion");
        }
    }
    static void repairRally()throws Exception{
        System.setProperty("rwagent.globalStrategy","true");
        GlobalStrategyPolicyHarness.Fixture f=new GlobalStrategyPolicyHarness.Fixture();f.recovery=1;
        for(Map<String,Object> u:f.own)if("builder".equals(u.get("type")))u.put("x",2500.);
        f.observe(530000);f.strategy.act(1200,1);
        check(f.fake.orders.stream().noneMatch(p->p.startsWith("/command/move?")&&p.contains("x=100.0")),"R16: routine repair does not recall a deployed regional constructor");
        check(f.contexts.stream().noneMatch(c->"repairbay".equals(c.get("product"))),"remote worker does not build a remote repair quote");
        f.own.add(StrategyContractHarness.unit(999,"builder",100,100));
        f.observe(531000);f.strategy.act(1200,1);
        check(f.contexts.stream().anyMatch(c->"repairbay".equals(c.get("product"))),"new home-side constructor fixture services repair while remote worker retains its region");
    }
    static void currentSiege()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();G51TacticalHarness.catalog(f);
        for(long id=10;id<16;id++){Map<String,Object> actor=f.find(id);actor.put("hp",600);actor.put("maxHp",600);}
        G51TacticalHarness.enemy(f,5000,"commandCenter",1000,100,4000,true,true,"SURFACE");f.collect();
        check("ADVANCE".equals(f.combat.view(f.a).get("tacticalChoice"))&&!f.owned("general:1").isEmpty(),"six actual healthy heavies can attack current affordable base despite small-travel memory");
        f.enemyRows.clear();f.frontierKnown=true;f.frontierX=1800;f.frontierY=100;f.sample(1000);f.collect();
        check(!"ADVANCE".equals(f.combat.view(f.a).get("tacticalChoice")),"lost base never grants a siege target from memory");
        Map<String,Object> base=map("id",7L,"building",true,"canAttack",true,"hp",4000,"x",100.,"y",100.);
        List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();for(long id=10;id<16;id++)actors.add(f.find(id));
        TacticalOpportunity.Exposure risk=TacticalOpportunity.assess(actors,f.own,Arrays.asList(base),f.enemies,100,100,100,100);
        check(risk.ratio<1,"building HP no longer counts as twice a mobile army's effective firepower");
    }
    public static void main(String[] args)throws Exception{production();scoutOwnership();selectedTechFunding();repairRally();currentSiege();System.out.println("G55_OPERATIONS_OK checks="+checks+" fixtureOnly=true");}
}
