package io.rwagent.client;
import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

public final class R10MilitaryInvestmentHarness {
    static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    public static void main(String[] args){
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();for(int i=0;i<24;i++)own.add(G51ProspectIntegrationHarness.unit(i+1,"heavyTank",100,100));
        Map<String,Object> s=map("sessionId","s","gameTimeMs",1000L,"ownUnits",own);
        Map<String,Object> e=map("sessionId","s","gameTimeMs",1000L,"visibleEnemies",new ArrayList<Map<String,Object>>());
        check(Boolean.TRUE.equals(MilitaryInvestmentPolicy.assess(s,e,24,true).get("preferSpatialInvestment")),"one mature ground formation covers current empty legal land sample while air has unfinished spatial duty");
        check(!Boolean.TRUE.equals(MilitaryInvestmentPolicy.assess(s,e,24,false).get("preferSpatialInvestment")),"no space work does not invent air demand");
        own.remove(0);check(!Boolean.TRUE.equals(MilitaryInvestmentPolicy.assess(s,e,24,true).get("preferSpatialInvestment")),"ground replacement readiness retained before marginal shift");
        own.add(G51ProspectIntegrationHarness.unit(99,"heavyTank",100,100));
        ((List<Map<String,Object>>)e.get("visibleEnemies")).add(map("id",900L,"targetDomain","LAND","hp",40000.,"lastSeenGameTimeMs",1000L));
        check(!Boolean.TRUE.equals(MilitaryInvestmentPolicy.assess(s,e,24,true).get("preferSpatialInvestment")),"actual high visible land pressure keeps ground investment");
        e.put("gameTimeMs",500L);Map<String,Object> stale=MilitaryInvestmentPolicy.assess(s,e,24,true);
        check(!Boolean.TRUE.equals(stale.get("preferSpatialInvestment"))&&stale.get("currentVisibleLandPressure")==null,"stale endpoint is UNKNOWN, not zero pressure");
        e.put("sessionId","other");e.put("gameTimeMs",1000L);check(!Boolean.TRUE.equals(MilitaryInvestmentPolicy.assess(s,e,24,true).get("preferSpatialInvestment")),"session mismatch cannot establish advantage");
        System.setProperty("rwagent.battleSafetyWallSeconds","14400");check(BattleBudget.wallLimit()==14400,"single-speed human 7200 entry honors supplied wall budget instead of silent one-hour cutoff");System.clearProperty("rwagent.battleSafetyWallSeconds");
        System.out.println("R10_MILITARY_INVESTMENT_OK checks="+checks+" fixtureOnly=true");
    }
}
