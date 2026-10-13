package io.rwagent.client;
import java.util.*;
import java.lang.reflect.*;
import static io.rwagent.client.StrategyDirector.map;

/** Multi-worker lawful site facts; no synthetic movement is a natural-match claim. */
public final class R12BuilderWorkHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Work extends G51ProspectIntegrationHarness.Fixture {
        final Map<Long,Map<String,Object>> quotes=new LinkedHashMap<Long,Map<String,Object>>();
        final Map<Long,Map<String,Object>> alternatives=new LinkedHashMap<Long,Map<String,Object>>();
        final List<String> reads=new ArrayList<String>();
        Work()throws Exception{super();gate.configureIndependentActors();resources.clear();credits=1400;
            builder.put("x",5800.);builder.put("y",1850.);own.add(G51ProspectIntegrationHarness.unit(5950,"builder",5900,2050));observe(121000,101);}
        public boolean economicArmyPolicyStrategy(){return true;}
        public boolean parallelStrategy(){return true;}
        public double effectiveStrategyCredits(double nativeCredits){return credits;}
        public void spendStrategy(String category,long cost,String product,long actor){credits-=cost;}
        @Override public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/expansion/plan")){
                long actor=Long.parseLong(path.split("unitId=")[1].split("&")[0]);
                if(reads!=null)reads.add(path);
                return quotes==null?null:path.contains("&excludeSites=")?alternatives.get(actor):quotes.get(actor);
            }return super.readStrategy(path,event);
        }
        void quote(long actor,double x,double y){quotes.put(actor,map("status","planned","sessionId","session","builderId",actor,
            "extractorCost",700.,"extractorX",x,"extractorY",y));}
        Object worker(long id)throws Exception{Field f=StrategyDirector.class.getDeclaredField("workers");f.setAccessible(true);return ((Map<Long,Object>)f.get(director)).get(id);}
        void staleRegion(long id,double x,double y)throws Exception{
            Object w=worker(id);Field p=w.getClass().getDeclaredField("prospect");p.setAccessible(true);ProspectCommitment c=(ProspectCommitment)p.get(w);
            c.accepted(id,x,y,x-100,y,100,120000);c.observeArrival(x,y,101,121000);
        }
        void field(long id,String name,Object value)throws Exception{Object w=worker(id);Field f=w.getClass().getDeclaredField(name);f.setAccessible(true);f.set(w,value);}
    }
    static void twoNearbySites()throws Exception{
        Work f=new Work();f.staleRegion(5924,5800,1850);f.staleRegion(5950,5900,2050);
        f.quote(5924,5570,1710);f.quote(5950,5890,2250);f.observe(122000,102);
        check(f.director.frontierConstructionReserve()==1400,"nearby Builders reserve different actual mines, not each others entire 600-world region");
        f.director.act(1400,1);
        check(f.orders.stream().filter(p->p.startsWith("/command/build-extractor")).count()==2,"two affordable independent native sites both dispatch");
        check(f.credits==0,"real accepted charges prevent overspending");f.director.close();
    }
    static void staleJourneyDoesNotReserveMine()throws Exception{
        Work f=new Work();f.staleRegion(5924,5800,1850);f.field(5924,"staticProspectTile",42L);f.field(5924,"buildX",5570.);f.field(5924,"buildY",1710.);
        f.quote(5950,5570,1710);f.observe(122000,102);
        check(f.director.frontierConstructionReserve()==700,"obsolete IDLE journey cannot block another Builders actual current work");
        f.director.act(700,1);check(f.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5950")),"free current constructor can work old unbuilt target");f.director.close();
    }
    static void currentWorkDoesNotNeedOldRally()throws Exception{
        Work f=new Work();f.staleRegion(5924,5000,1850);f.quote(5924,5890,1850);f.observe(122000,102);
        check(f.director.frontierConstructionReserve()==700,"current legal local work beats an obsolete distant rally preference");
        f.director.act(700,1);check(f.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"worker builds instead of being stranded by old cluster");f.director.close();
    }
    static void inheritedConstructionIsNotCancelled()throws Exception{
        Work f=new Work();f.builder.put("orderType","build");f.quote(5924,5570,1710);f.resources.add(G51ProspectIntegrationHarness.resource(44,6200,1850));
        f.observe(122000,102);f.director.act(0,1);
        check(f.orders.stream().noneMatch(p->p.contains("unitId=5924")),"an inherited actual native build is not replaced by ordinary prospect or duplicate construction");
        f.builder.put("orderType",null);f.observe(123000,103);f.director.act(f.director.frontierConstructionReserve(),1);
        check(f.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"native construction clear restores current mining work without permanent idle latch");f.director.close();
    }
    static void nearestSiteIsClaimedNotWholeRegion()throws Exception{
        Work f=new Work();f.quote(5924,5570,1710);f.quote(5950,5570,1710);
        f.alternatives.put(5950L,map("status","planned","sessionId","session","builderId",5950L,"extractorCost",700.,"extractorX",5890.,"extractorY",2250.));
        f.observe(122000,102);
        check(f.director.frontierConstructionReserve()==1400,"second Builder requotes another actual legal site when the nearest is already reserved");
        check(f.reads.stream().anyMatch(p->p.contains("unitId=5950&excludeSites=")),"alternative legality is obtained from the native endpoint, not fabricated by the controller");
        f.director.act(1400,1);
        check(f.orders.stream().filter(p->p.startsWith("/command/build-extractor")).count()==2,"both distinct quoted sites dispatch without duplicate mine");f.director.close();
    }
    static void baselineSharesExactSiteClaims()throws Exception{
        Work f=new Work();f.quote(5924,5570,1710);f.observe(122000,102);f.quote(2,5570,1710);
        f.alternatives.put(2L,map("status","planned","sessionId","session","builderId",2L,"extractorCost",700.,"extractorX",5890.,"extractorY",2250.));
        Map<String,Object> plan=f.director.baselineMinePlan(2,"test_baseline");
        check(plan!=null&&((Number)plan.get("extractorX")).doubleValue()==5890,"baseline Economy avoids the owned worker's exact funded mine using a real alternate quote");
        f.alternatives.remove(2L);
        check(f.director.baselineMinePlan(2,"test_unknown")==null,"unknown alternative never invents legality or steals an owned site");f.director.close();
    }
    public static void main(String[] args)throws Exception{
        twoNearbySites();staleJourneyDoesNotReserveMine();currentWorkDoesNotNeedOldRally();inheritedConstructionIsNotCancelled();
        nearestSiteIsClaimedNotWholeRegion();baselineSharesExactSiteClaims();
        System.out.println("R12_BUILDER_WORK_OK checks="+checks+" fixtureOnly=true");
    }
}
