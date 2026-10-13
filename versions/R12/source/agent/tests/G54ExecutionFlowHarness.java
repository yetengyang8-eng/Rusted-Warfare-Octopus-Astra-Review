package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Regression of the human R4F starvation mechanism, not a natural gameplay claim. */
public final class G54ExecutionFlowHarness {
    static int checks,sequence;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static Intent intent(CommandArbiter a,long actor,Intent.Commitment cost){return new Intent("i:"+(++sequence),CommandArbiter.DEFAULT_OWNER,
        a.snapshotGenerations(CommandArbiter.DEFAULT_OWNER,Collections.singletonList(actor)),Collections.singletonList(actor),"COMMAND","FLOW",30,
        a.stamp(),"o:1","/state","/command/move?unitId="+actor,cost);}
    static void independentAdmission(){
        CommandArbiter a=new CommandArbiter();ExecutionScheduler s=new ExecutionScheduler(a,0,0);
        List<Long> own=new ArrayList<Long>();for(long id=1;id<=70;id++)own.add(id);
        a.observe(new CommandArbiter.Stamp("s","team:0",1,0),own);
        Map<Long,Integer> slots=new HashMap<Long,Integer>();slots.put(65L,1);slots.put(66L,1);
        s.beginObservation(a.stamp(),"o:1",2000L,slots,3);
        ExecutionScheduler.Dispatcher nativeAccept=i->map("status","queued");
        for(long id=1;id<=64;id++)check(s.dispatch(intent(a,id,Intent.Commitment.none()),nativeAccept).accepted,"independent recovery actor accepted "+id);
        Intent.Commitment produce=new Intent.Commitment(true,350L,Collections.singletonMap(65L,1),1);
        check(s.dispatch(intent(a,65,produce),nativeAccept).accepted,"64 recovery attempts cannot starve producer");
        check(s.dispatch(intent(a,67,new Intent.Commitment(true,700L,Collections.<Long,Integer>emptyMap(),0)),nativeAccept).accepted,"construction gets independent service");
        check(s.dispatch(intent(a,68,Intent.Commitment.none()),nativeAccept).accepted,"General gets independent service");
        check("ACTOR_CONFLICT_IN_OBSERVATION".equals(s.dispatch(intent(a,65,Intent.Commitment.none()),nativeAccept).cancelReason),"same actor still conflicts");
        check("INSUFFICIENT_EFFECTIVE_CREDITS".equals(s.dispatch(intent(a,66,new Intent.Commitment(true,1000L,Collections.singletonMap(66L,1),1)),nativeAccept).cancelReason),"independent admission does not overspend");
        a.observe(new CommandArbiter.Stamp("s","team:0",2,100),own);s.beginObservation(a.stamp(),"o:2",2000L,slots,3);
        check(s.effectiveCredits()==950L&&s.unsettledCreditCount()==2,"a later clock does not refund paid-before-visible commitments");
        check(a.claim("test:a",69),"claim transfer actor");Intent stale=new Intent("old", "test:a",a.snapshotGenerations("test:a",Collections.singletonList(69L)),Collections.singletonList(69L),"COMMAND","FLOW",30,a.stamp(),"o:2","/state","/command/move?unitId=69",Intent.Commitment.none());
        a.transfer("test:a","test:b",69);a.transfer("test:b","test:a",69);
        check("STALE_OWNER_GENERATION".equals(s.dispatch(stale,nativeAccept).cancelReason),"unbounded independent actors retain ABA rejection");
    }
    static void recoveryOrderReuse()throws Exception{
        RecoveryPoolHarness.Fixture f=new RecoveryPoolHarness.Fixture(6);f.units.get(0).put("hp",20d);f.pool.reconcile(f.state);
        List<ForceController.Proposal> proposals=new ArrayList<ForceController.Proposal>();
        ForceController.Host host=new ForceController.Host(){public Map<String,Object> read(String p,String e){return null;}
            public List<Map<String,Object>> eligible(List<Map<String,Object>> a,Map<String,Object> t,Map<String,Object> e){return a;}
            public void collect(ForceController.Proposal p){proposals.add(p);}};
        f.pool.collect(f.state,100,100,host);check(proposals.size()==1&&proposals.get(0).priority==90,"one actual withdrawal remains critical");
        proposals.get(0).accepted(map("status","queued","frame",f.frame));proposals.clear();
        for(int i=0;i<120;i++){f.next();f.pool.reconcile(f.state);f.pool.collect(f.state,100,100,host);}
        check(proposals.isEmpty(),"no facility and unchanged own evidence produce no repeated home command flood");
        Map<String,Object> bay=RecoveryPoolHarness.unit(100,1000);bay.put("type","repairbay");bay.put("x",500d);bay.put("y",500d);f.units.add(bay);f.next();f.pool.reconcile(f.state);f.pool.collect(f.state,100,100,host);
        check(proposals.size()==1&&proposals.get(0).priority<70&&proposals.get(0).path.contains("x=500.0"),"new actual repair site resumes ordinary recovery movement");
        proposals.get(0).accepted(map("status","queued","frame",f.frame));proposals.clear();
        f.units.get(0).put("orderType","move");f.units.get(0).put("orderX",500d);f.units.get(0).put("orderY",500d);
        for(int i=0;i<120;i++){f.next();f.pool.reconcile(f.state);f.pool.collect(f.state,100,100,host);}
        check(proposals.isEmpty(),"explicit same native repair destination remains reusable beyond cooldown");
        f.units.get(0).put("orderType",null);f.pool.collect(f.state,100,100,host);check(proposals.size()==1,"idle before arrival receives renewed actual bay approach");
        proposals.get(0).rejected(map("status","rejected","nativeHttpStatus",409));proposals.clear();
        for(int i=0;i<30;i++){f.next();f.pool.reconcile(f.state);f.pool.collect(f.state,100,100,host);}
        check(proposals.isEmpty(),"unchanged negative native actor evidence cannot spam 409");
        f.units.get(0).put("x",450d);f.next();f.pool.reconcile(f.state);f.pool.collect(f.state,100,100,host);
        check(f.r.unit(1).healthRole==GeneralRegistry.HealthRole.RECOVERING,"409 and arrival never fabricate death or healing");
        f.units.get(0).put("hp",155d);f.next();f.pool.reconcile(f.state);
        check(f.r.unit(1).healthRole==GeneralRegistry.HealthRole.NORMAL&&GeneralRegistry.FREE_OWNER.equals(f.r.unit(1).owner),"later real own HP restores scheduling");
    }
    static void fundedTech(){
        Map<String,Object> quote=map("type","upgrade","actionId","native-upgrade","cost",1500,"affordable",true);
        check(ProductionCapacity.techInvestment(quote,3,1,5000,700,350,Double.NaN,Double.NaN,true,true).selected,"funded upgrade does not wait for whole-army recovery or rate estimate");
        check(!ProductionCapacity.techInvestment(quote,18,18,2000,700,350,100,50,false).selected,"actual chosen commitments remain protected");
    }
    static void independentQueueBaseline(){
        for(boolean actualQuote:new boolean[]{true,false}){
            CommandArbiter a=new CommandArbiter();ExecutionScheduler s=new ExecutionScheduler(a,0,0);NativeCreditWitness witness=new NativeCreditWitness();
            a.observe(new CommandArbiter.Stamp("s","team:0",10,1000),Collections.singletonList(1L));
            s.beginObservation(a.stamp(),"o:10",2000L,Collections.singletonMap(1L,0),5);
            Map<String,Object> factory=map("id",1L,"type","landFactory","productionQueue",1,"techLevel",1,"buildProgress",1,"dead",false);
            List<Map<String,Object>> own=Collections.singletonList(factory);
            Map<String,Object> state=map("status","running","sessionId","s","frame",10L,"gameTimeMs",1000L,"player",map("teamId",0),"ownUnits",own);
            s.observeProducerSlots(Collections.singletonMap(1L,1));check(s.availableProducerSlots(1L)==1,"new legal own-menu idle queue replaces older state busy evidence before any attempt");
            Intent i=new Intent("queue:1",CommandArbiter.DEFAULT_OWNER,a.snapshotGenerations(CommandArbiter.DEFAULT_OWNER,Collections.singletonList(1L)),Collections.singletonList(1L),"/command/queue","FLOW",30,a.stamp(),"o:10","/state","/command/queue?unitId=1&actionId=nativeTank",Intent.Commitment.spending(350L,Collections.singletonMap(1L,1),1),"quote:11","/combat/production");
            ExecutionScheduler.Result r=s.dispatch(i,intent->map("status","queued","frame",11L));
            Map<String,Object> context=map("product","tank","producerBaselineActorId",1L,"producerBaselineQueue",0L,"producerBaselineTier",1L,
                "producerBaselineObservationId",actualQuote?"quote:11":"unrelated:quote","producerBaselineRequestPath","/combat/production");
            witness.accepted(i,r.receipt,context,state);s.observeProducerSlots(Collections.singletonMap(1L,1));check(s.availableProducerSlots(1L)==0,"a repeated menu never refunds locally attempted producer slot");
            state.put("frame",12L);state.put("gameTimeMs",1200L);
            List<Map<String,Object>> settled=witness.observe(new CommandArbiter.Stamp("s","team:0",12,1200),"o:12",state,s);
            check(actualQuote?settled.size()==1:settled.isEmpty(),"only the exact quote baseline can establish later queue effect");
            if(actualQuote)check(Boolean.FALSE.equals(settled.get(0).get("productReady")),"queue payment effect never claims unit ready");
        }
    }
    static void sequentialConstructionEffects(){
        CommandArbiter a=new CommandArbiter();ExecutionScheduler s=new ExecutionScheduler(a,0,0);NativeCreditWitness w=new NativeCreditWitness();
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();own.add(map("id",1L,"type","builder","buildProgress",1,"dead",false));
        Map<String,Object> state=map("status","running","sessionId","s","frame",10L,"gameTimeMs",1000L,"player",map("teamId",0),"ownUnits",own);
        for(int j=0;j<2;j++){
            long frame=10+j;state.put("frame",frame);state.put("gameTimeMs",1000L+j*100);
            CommandArbiter.Stamp stamp=new CommandArbiter.Stamp("s","team:0",frame,1000+j*100);
            a.observe(stamp,Collections.singletonList(1L));s.beginObservation(stamp,"o:"+frame,5000L,Collections.<Long,Integer>emptyMap(),10);
            double x=100+j*1000;
            Intent i=new Intent("mine:"+j,CommandArbiter.DEFAULT_OWNER,a.snapshotGenerations(CommandArbiter.DEFAULT_OWNER,Collections.singletonList(1L)),Collections.singletonList(1L),"/command/build-extractor","FLOW",30,stamp,"quote:"+j,"/expansion/plan","/command/build-extractor?unitId=1&x="+x+"&y=200",Intent.Commitment.spending(700L,Collections.<Long,Integer>emptyMap(),0),"quote:"+j,"/expansion/plan");
            ExecutionScheduler.Result r=s.dispatch(i,intent->map("status","queued","frame",20L,"type","extractorT1","targetX",x,"targetY",200));
            check(r.accepted,"sequential distinct builder construction accepted");w.accepted(i,r.receipt,map("product","extractor"),state);
        }
        state.put("frame",21L);state.put("gameTimeMs",1500L);
        check(w.observe(new CommandArbiter.Stamp("s","team:0",21,1500),"o:21",state,s).isEmpty()&&s.unsettledCreditTotal()==1400,"native receipt and time cannot settle invisible construction");
        // Both sites can have been completed between polls, and the builder may have left.
        own.clear();for(int j=0;j<2;j++)own.add(map("id",20L+j,"type","extractorT1","buildProgress",1,"dead",false,"x",100+j*1000,"y",200));
        List<Map<String,Object>> effects=w.observe(new CommandArbiter.Stamp("s","team:0",21,1500),"o:21",state,s);
        check(effects.size()==2&&s.unsettledCreditTotal()==0,"two independent own-site effects settle without builder order or assumed generic product type");
        check(Boolean.FALSE.equals(effects.get(0).get("productReady")),"site spending effect does not declare readiness or producer lineage");
    }
    public static void main(String[] args)throws Exception{independentAdmission();recoveryOrderReuse();fundedTech();independentQueueBaseline();sequentialConstructionEffects();System.out.println("G54ExecutionFlowHarness PASS checks="+checks);}
}
