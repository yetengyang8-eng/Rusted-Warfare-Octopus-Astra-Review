package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Current quote, negative evidence and en-route opportunity through the real allocator. */
public final class R13BuilderOpportunityHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refusalsAndUnknown()throws Exception{
        R12BuilderWorkHarness.Work f=new R12BuilderWorkHarness.Work();
        f.credits=100;f.quote(5924,5890,1850);f.observe(122000,102);
        check(f.director.frontierConstructionReserve()==700,"actual native quote commits its own price");
        f.quotes.remove(5924L);f.credits=2000;f.observe(123000,103);f.director.act(700,1);
        check(f.director.frontierConstructionReserve()==700,"UNKNOWN does not refund or fabricate invalidation");
        check(f.orders.stream().noneMatch(p->p.startsWith("/command/build-extractor")),"UNKNOWN never replays stale invoice into a command");
        f.quotes.put(5924L,map("status","no-current-work","reason","NATIVE_CURRENT_SITE_REFUSED"));f.observe(124000,104);
        check(f.director.frontierConstructionReserve()==0,"current native refusal releases only unpaid mine intention");
        f.quote(5924,5900,1900);f.observe(125000,105);f.director.act(700,1);
        check(f.orders.stream().filter(p->p.startsWith("/command/build-extractor")).count()==1,"new legal local quote resumes actual work once");f.director.close();
    }
    static void activeTravelOpportunity()throws Exception{
        R12BuilderWorkHarness.Work f=new R12BuilderWorkHarness.Work();
        f.field(5924,"job","PROSPECT");f.field(5924,"x",8000.);f.field(5924,"y",1850.);
        f.field(5924,"lastProgress",122000L);f.field(5924,"jobAt",122000L);
        f.builder.put("orderType","move");f.builder.put("orderX",8000.);f.builder.put("orderY",1850.);
        f.quote(5924,5890,1850);f.observe(123000,103);f.director.act(700,1);
        check(f.eventCount("strategy_prospect_local_work")==1,"current real opportunity interrupts still-moving distant prospect");
        check(f.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"same controller cycle turns soft travel into native construction attempt");
        check(f.gate.owns(String.valueOf(((Map<?,?>)f.events.stream().filter(e->"task_ownership_acquired".equals(e.get("event"))).findFirst().get().get("data")).get("owner")),5924),"construction retains worker ownership");
        f.director.close();
        R12BuilderWorkHarness.Work saving=new R12BuilderWorkHarness.Work();saving.credits=100;
        saving.field(5924,"job","PROSPECT");saving.field(5924,"x",8000.);saving.field(5924,"y",1850.);
        saving.builder.put("orderType","move");saving.builder.put("orderX",8000.);saving.builder.put("orderY",1850.);
        saving.quote(5924,5890,1850);saving.observe(123000,103);saving.director.act(700,1);
        check(saving.orders.stream().anyMatch(p->p.startsWith("/command/move?unitId=5924&x=5800.0")),"selected local invoice cancels old distant travel while actually saving");
        saving.builder.put("orderType",null);saving.credits=700;saving.observe(124000,104);saving.director.act(700,1);
        check(saving.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"saved native price immediately starts work without a new journey");saving.director.close();
        R12BuilderWorkHarness.Work nativeWork=new R12BuilderWorkHarness.Work();nativeWork.field(5924,"job","PROSPECT");
        nativeWork.builder.put("orderType","repair");nativeWork.quote(5924,5890,1850);nativeWork.observe(123000,103);nativeWork.director.act(0,1);
        check(nativeWork.orders.stream().noneMatch(p->p.contains("unitId=5924")),"opportunity does not overwrite actual native repair");nativeWork.director.close();
    }
    static void refusedSiteAfterClusterArrival()throws Exception{
        R12BuilderWorkHarness.Work f=new R12BuilderWorkHarness.Work();
        f.staleRegion(5924,5800,1850);
        f.quotes.put(5924L,map("status","no-current-work","reason","NATIVE_CURRENT_SITE_REFUSED"));
        f.observe(123000,103);f.director.act(0,1);
        check(f.director.frontierConstructionReserve()==0,"arrived region with current refused site has no fabricated construction invoice");
        check(f.orders.stream().noneMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"refused site can continue regional assessment without coordinates, crash or build");
        f.quote(5924,5890,1850);f.observe(124000,104);f.director.act(700,1);
        check(f.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"later current legal quote still starts work after refused-site regional assessment");f.director.close();
    }
    public static void main(String[] args)throws Exception{refusalsAndUnknown();activeTravelOpportunity();refusedSiteAfterClusterArrival();System.out.println("R13_BUILDER_OPPORTUNITY_OK checks="+checks+" fixtureOnly=true");}
}
