package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Focused own-position and command reuse fixtures; not natural Spain behavior evidence. */
public final class G51LogisticsHarness {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void localCommitment(){
        ProspectCommitment p=new ProspectCommitment();
        check(!p.arrived()&&!p.local(7410,410),"no receipt creates no local commitment");
        p.accepted(42,7410,410,6500,2400,200,626000);
        check(!p.arrived()&&p.paidTravel()>2000,"paid travel retained; receipt is not arrival");
        check(!p.observeArrival(7410,410,200,694000),"receipt frame is never arrival");
        check(!p.observeArrival(7410,410,201,625000),"older game time cannot be arrival");
        check(!p.observeArrival(7410,600,201,694000),"remote own position is not arrival");
        check(p.observeArrival(7410,410,201,694000)&&p.arrived(),"later own position establishes cluster commitment");
        check(p.local(7470,610)&&p.local(7300,410),"two nearby resource candidates remain local");
        check(!p.local(6330,3710),"south cross-map site cannot displace arrived north commitment");
        p.accepted(43,7800,410,7410,410,202,700000);
        check(p.arrived()&&p.x()==7410&&p.tile()==42,"local continuation preserves original arrived cluster anchor");
        check(!p.local(8390,410),"successive nearby orders cannot drift commitment across map");
        check(!p.observeArrival(7800,410,202,701000),"local next endpoint still requires post-receipt own frame");
        check(p.observeArrival(7800,410,203,701000)&&p.x()==7410,"later local arrival witnesses latest endpoint while retaining original cluster");
        check(p.assessLocalBacklog(false,false),"unknown local coverage does not release paid travel");
        check(p.assessLocalBacklog(true,false),"one empty local assessment leaves reconciliation time");
        check(p.assessLocalBacklog(true,true),"current lawful local candidate preserves commitment");
        check(p.assessLocalBacklog(true,false)&&!p.assessLocalBacklog(true,false),"two fresh empty assessments allow global replan");
        p.accepted(44,200,200,900,900,300,700000);p.observeArrival(200,200,301,701000);p.clear();
        check(!p.arrived()&&p.tile()<0,"current threat/owner handover can release immediately");
        boolean invalid=false;try{p.accepted(5,Double.NaN,0,0,0,10,10);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"malformed coordinates cannot establish commitment");
    }
    private static ForceControllerHarness.Fixture joining()throws Exception{
        ForceControllerHarness.Fixture f=new ForceControllerHarness.Fixture();f.observe(1000);
        check(f.registry.requestJoin(33,f.a)&&f.registry.beginJoining(33),"actor obtains actual join owner");
        f.collect();ForceController.Proposal p=f.lane("JOINING");check(p!=null,"initial joining move proposed");
        p.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        Map<String,Object> own=f.find(33);own.put("orderType","move");own.put("orderX",f.registry.general(f.a).x);own.put("orderY",f.registry.general(f.a).y);
        return f;
    }
    private static void reuseAndRedirect()throws Exception{
        ForceControllerHarness.Fixture f=joining();
        for(Long actor:f.registry.general(f.a).members)f.find(actor).put("x",((Number)f.find(actor).get("x")).doubleValue()+50);
        f.find(33).put("x",250.);f.observe(10000);f.collect();
        check(f.lane("JOINING")==null,"current matching native move survives small centroid change while progressing");
        check(f.registry.unit(33).membership==GeneralRegistry.Membership.JOINING,"order reuse never invents arrival");
        f.find(33).put("orderType","attackMove");f.observe(11000);f.collect();
        check(f.lane("JOINING")!=null,"overwritten native move requires fresh command despite close centroid");
        ForceControllerHarness.Fixture large=joining();
        for(Long actor:large.registry.general(large.a).members)large.find(actor).put("x",((Number)large.find(actor).get("x")).doubleValue()+200);
        large.observe(10000);large.collect();check(large.lane("JOINING")!=null,"materially moved rendezvous redirects");
        ForceControllerHarness.Fixture stale=joining();stale.observe(23000);stale.collect();
        check(stale.lane("JOINING")==null,"explicit current native rendezvous remains reusable without a progress timer");
        ForceControllerHarness.Fixture rally=joining();rally.observe(2000);
        check(rally.registry.setJoinTargetOverride(rally.a,rally.gate.stamp(),100,100),"current rally override lawful");rally.collect();
        check(rally.lane("JOINING")!=null&&rally.lane("JOINING").path.contains("x=100.0"),"urgent retreat rally bypasses command reuse and cooldown");
        ForceControllerHarness.Fixture aba=joining();long old=aba.registry.unit(33).ownerGeneration;
        check(aba.registry.cancelJoin(33),"old join lifecycle released");aba.observe(2000);
        check(aba.registry.requestJoin(33,aba.a)&&aba.registry.beginJoining(33)&&aba.registry.unit(33).ownerGeneration!=old,"same actor gets new generation");
        aba.collect();check(aba.lane("JOINING")!=null,"old receipt/native order cannot suppress new generation move");
    }
    private static void legacyJoiningShortDefer()throws Exception{
        ForceControllerHarness.Fixture f=joining();Map<String,Object> actor=f.find(33);actor.remove("orderX");actor.remove("orderY");actor.put("x",300.);f.observe(2000);f.collect();
        actor.put("x",280.);f.observe(8000);f.collect();
        actor.put("x",250.);f.observe(10000);f.collect();check(f.lane("JOINING")==null,"legacy move without destination briefly defers after net progress toward accepted rendezvous");
        check(((Number)f.controller.orderReuseView().get("destinationUnknownJoinDeferCount")).longValue()==1&&f.registry.unit(33).membership==GeneralRegistry.Membership.JOINING,"joining defer diagnosis records unknown destination and never claims arrival");
        actor.put("x",220.);f.observe(12000);f.collect();check(f.lane("JOINING")==null,"recent own progress preserves defer before hard twelve-second deadline");
        actor.put("x",190.);f.observe(13000);f.collect();check(f.lane("JOINING")!=null,"continuing join progress cannot extend accepted twelve-second hard deadline");
        ForceControllerHarness.Fixture partial=joining();actor=partial.find(33);actor.remove("orderY");actor.put("x",300.);partial.observe(2000);partial.collect();actor.put("x",250.);partial.observe(10000);partial.collect();
        check(partial.lane("JOINING")!=null,"one missing coordinate cannot invoke joining fallback");
        ForceControllerHarness.Fixture away=joining();actor=away.find(33);actor.remove("orderX");actor.remove("orderY");actor.put("x",300.);away.observe(2000);away.collect();actor.put("x",350.);away.observe(10000);away.collect();
        check(away.lane("JOINING")!=null,"movement away from accepted rendezvous is not useful joining progress");
        ForceControllerHarness.Fixture changed=joining();actor=changed.find(33);actor.remove("orderX");actor.remove("orderY");actor.put("x",300.);changed.observe(2000);changed.collect();changed.enemy(9999,3000,100);actor.put("x",250.);changed.observe(3000);changed.collect();
        check(changed.lane("JOINING")!=null,"new lawful armed contact invalidates join defer/cooldown immediately");
    }
    public static void main(String[] args)throws Exception{localCommitment();reuseAndRedirect();legacyJoiningShortDefer();System.out.println("G51LogisticsHarness checks="+checks+" PASS");}
}
