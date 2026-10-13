package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Human failure shapes through real worker allocation; synthetic positions are not native movement. */
public final class R14BuilderFlowHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Journey extends G51ProspectIntegrationHarness.Fixture {
        Journey()throws Exception{super();}
        @Override public Map<String,Object> readStrategy(String path,String event){
            Map<String,Object> result=super.readStrategy(path,event);
            if(path.startsWith("/scout/resource-approach")&&result!=null){result.put("waypointX",6500.);result.put("waypointY",3500.);}
            return result;
        }
    }
    static void cornerIsNotWorkRegion()throws Exception{
        Journey f=new Journey();f.director.act(0,1);
        f.builder.put("x",6500.);f.builder.put("y",3500.);f.observe(128000,101);
        check(f.eventCount("strategy_prospect_cluster_committed")==0,"intermediate own-position witness does not commit a work region");
        check(f.eventCount("strategy_prospect_observed")==0,"corner is not completed resource journey");f.director.close();
    }
    static void mineReplacesOldAnchor()throws Exception{
        R12BuilderWorkHarness.Work f=new R12BuilderWorkHarness.Work();f.staleRegion(5924,5000,1850);
        f.quote(5924,5890,1850);f.observe(122000,102);f.director.act(700,1);
        Object worker=f.worker(5924);java.lang.reflect.Field field=worker.getClass().getDeclaredField("prospect");field.setAccessible(true);
        check(!((ProspectCommitment)field.get(worker)).arrived(),"accepted actual mine discards obsolete travel anchor outside its work area");
        check(f.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"replacement still uses real native quote");f.director.close();
    }
    static void emptyRegionCannotImprisonWorker()throws Exception{
        G51ProspectIntegrationHarness.Fixture f=new G51ProspectIntegrationHarness.Fixture();f.start();f.arrive();
        f.own.add(G51ProspectIntegrationHarness.unit(400,"extractorT1",7410,410));f.resources.add(G51ProspectIntegrationHarness.resource(99,6330,3710));
        f.unknownVisibility=true;f.director.act(0,1);
        check(f.orders.stream().anyMatch(p->p.contains("x=6330.0")),"UNKNOWN regional survey cannot veto another legally planned work journey");
        check(f.events.stream().anyMatch(e->"strategy_prospect_preference_bypassed".equals(e.get("event"))),"soft preference bypass is traced without claiming empty or safe");
        check(f.events.stream().noneMatch(e->"strategy_prospect_cluster_released".equals(e.get("event"))&&String.valueOf(e.get("data")).contains("TWO_CURRENT_EMPTY")),"UNKNOWN never becomes an exhaustion witness");f.director.close();
    }
    static void complementaryAirDoesNotRequireEveryPlannedSoldier()throws Exception{
        G6CapitalPolicyHarness.Host f=new G6CapitalPolicyHarness.Host();
        f.own.removeIf(u->"heavyTank".equals(u.get("type"))&&((Number)u.get("id")).longValue()>=112);
        f.ground.removeIf(u->((Number)u.get("id")).longValue()>=112);
        f.observe(310000);
        check(Boolean.TRUE.equals(f.policy.summary().get("endgameReady")),"formed ready ground force can support complementary air against current low pressure");
        check(((Number)f.policy.summary().get("activeTarget")).intValue()>12,"organization target remains larger; not reduced to force air readiness");
        f.policy.close();
    }
    public static void main(String[]args)throws Exception{cornerIsNotWorkRegion();mineReplacesOldAnchor();emptyRegionCannotImprisonWorker();complementaryAirDoesNotRequireEveryPlannedSoldier();System.out.println("R14_BUILDER_FLOW_OK checks="+checks+" fixtureOnly=true");}
}
