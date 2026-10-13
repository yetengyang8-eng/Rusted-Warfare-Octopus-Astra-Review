package io.rwagent.client;

import java.util.*;

/** The gap between two local jobs is not a home-construction vacancy. */
public final class R16BuilderRegionHarness {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void capital()throws Exception{
        G6CapitalPolicyHarness.Host h=G6CapitalPolicyHarness.funded();
        G53ContinuingOperationsHarness.set(h.policy,"hardCap",0);
        for(Map<String,Object> u:h.own)if("builder".equals(u.get("type")))u.put("x",2000.);
        h.observe(421000);Object worker=G6CapitalPolicyHarness.worker(h,3);
        check(((ProspectCommitment)G53ContinuingOperationsHarness.get(worker,"prospect")).tile()<0,"fixture has no active prospect or current-visible resource");
        long generation=h.fake.gate.ownerGeneration(3L);h.policy.act(0,1);
        check(h.fake.orders.stream().noneMatch(p->p.startsWith("/command/move?unitId=3")),"completed-region worker is not recalled across map for capital");
        check(h.fake.gate.ownerGeneration(3L)==generation,"region retention does not invent a transfer");h.policy.close();
        G6CapitalPolicyHarness.Host busy=G6CapitalPolicyHarness.funded();G53ContinuingOperationsHarness.set(busy.policy,"hardCap",0);for(Map<String,Object> u:busy.own)if("builder".equals(u.get("type")))u.put("orderType","repair");
        busy.observe(421000);busy.policy.act(0,1);check(busy.purchases("fabricatorT1")==0,"native repair is not overwritten merely because bookkeeping job is IDLE");busy.policy.close();
        G6CapitalPolicyHarness.Host home=G6CapitalPolicyHarness.funded();G53ContinuingOperationsHarness.set(home.policy,"hardCap",0);home.policy.act(0,1);check(home.purchases("fabricatorT1")>0,"available home constructor still builds capital");home.policy.close();
    }
    static void recovery()throws Exception{
        R9BuilderCoverageHarness.Recovery f=new R9BuilderCoverageHarness.Recovery();f.start();f.arrive();f.recovery=1;
        Object worker=((Map<?,?>)G53ContinuingOperationsHarness.get(f.director,"workers")).get(5924L);
        G53ContinuingOperationsHarness.set(worker,"job","IDLE");((ProspectCommitment)G53ContinuingOperationsHarness.get(worker,"prospect")).clear();
        f.resources.clear();f.orders.clear();f.observe(136000,102);f.director.act(0,1);
        check(f.eventCount("strategy_recovery_builder_rally")==0,"remote worker after local completion is not drafted for routine home repair");f.director.close();
    }
    public static void main(String[] args)throws Exception{capital();recovery();System.out.println("R16_BUILDER_REGION_OK checks="+checks+" fixtureOnly=true");}
}
