package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Controller fixture reproduces lawful local work with native cash above the
 * quote but a lower protected execution budget. No native arrival or improvement claim. */
public final class R8BuilderWorkHarness {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Fixture extends G51ProspectIntegrationHarness.Fixture {
        double spendable=116;
        Fixture()throws Exception{super();}
        public double effectiveStrategyCredits(double nativeCredits){return Math.min(nativeCredits,spendable);}
        public Map<String,Object> costSourceStrategy(Map<String,Object> quote){return map("costSourceObservationId","current-native-quote:"+frame,"costSourceRequestPath","/expansion/plan?unitId=5924");}
        void atMine()throws Exception{
            start();arrive();credits=1529;observe(136000,102);
            resources.add(G51ProspectIntegrationHarness.resource(43,7450,430));
            plan=map("status","planned","sessionId","session","builderId",5924L,"extractorCost",700.,"extractorX",7410.,"extractorY",410.);
        }
        Map<String,Object> waiting(){for(Map<String,Object> e:events)if("strategy_local_mine_waiting".equals(e.get("event")))return BattleClient.obj(e.get("data"));return null;}
    }
    static void fundingThenBuild()throws Exception{
        Fixture f=new Fixture();f.atMine();long generation=f.gate.ownerGeneration(5924L);
        f.director.act(0,1);
        check(f.orders.size()==1,"native cash above cost does not bypass effective credits or generate another same-cluster MOVE");
        Map<String,Object> waiting=f.waiting();
        check(waiting!=null&&"LOCAL_SITE_AWAITING_FUNDS".equals(waiting.get("reason")),"arrived lawful work has an explicit funding reason");
        check(StrategyDirector.number(waiting,"nativeCredits",0)==1529&&StrategyDirector.number(waiting,"effectiveCredits",0)==116&&StrategyDirector.number(waiting,"nativeCost",0)==700,"waiting separates actual wallet, spendable ledger and real site quote");
        check("/expansion/plan?unitId=5924".equals(waiting.get("costSourceRequestPath")),"waiting records current native quote provenance");
        check(f.gate.reserved(5924L)&&f.gate.ownerGeneration(5924L)==generation&&f.eventCount("strategy_prospect_cluster_released")==0,"unfunded work keeps its paid local opportunity and owner generation");
        f.spendable=700;f.observe(152000,103);f.director.act(50,1);
        check(f.orders.size()==1,"exact quote remains unavailable while another real reserve protects 50 credits");
        f.observe(168000,104);f.director.act(0,1);
        check(f.orders.size()==2&&f.orders.get(1).startsWith("/command/build-extractor?unitId=5924&x=7410.0&y=410.0"),"later actual free budget converts fresh local quote to existing construction path");
        check(f.eventCount("strategy_construction_ordered")==1,"funding wait does not fabricate construction or readiness");f.director.close();
    }
    static void noFalseCommitment()throws Exception{
        Fixture wrong=new Fixture();wrong.atMine();wrong.plan.put("builderId",99L);wrong.director.act(0,1);
        check(wrong.waiting()==null&&wrong.orders.size()==2,"another actor's quote cannot freeze the local exploration lifecycle");wrong.director.close();
        Fixture distant=new Fixture();distant.atMine();distant.plan.put("extractorX",7600.);distant.director.act(0,1);
        check(distant.waiting()==null&&distant.orders.size()==2,"arrival at rally does not claim actual readiness at a farther legal construction site");distant.director.close();
        Fixture missing=new Fixture();missing.atMine();missing.plan=null;missing.director.act(0,1);
        check(missing.waiting()==null&&missing.orders.size()==2,"absent current native site still permits existing local prospect search");missing.director.close();
        Fixture danger=new Fixture();danger.atMine();Map<String,Object> threat=map("id",600L,"type","heavyTank","x",7420.,"y",420.,"hp",1000,"maxHp",1000,"canAttack",true);danger.visible.add(threat);danger.threats.add(threat);danger.observe(144000,103);danger.director.act(0,1);
        check(danger.waiting()==null&&danger.eventCount("strategy_prospect_cluster_released")==1,"current danger releases paid local work instead of preserving a funds wait");danger.director.close();
    }
    public static void main(String[] args)throws Exception{fundingThenBuild();noFalseCommitment();System.out.println("R8BuilderWorkHarness checks="+checks+" PASS fixtureOnly=true nativeCashIsNotFreeBudget=true");}
}
