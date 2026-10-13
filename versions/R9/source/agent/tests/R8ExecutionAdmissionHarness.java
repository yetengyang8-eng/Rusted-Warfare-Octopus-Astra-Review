package io.rwagent.client;

import java.io.BufferedWriter;
import java.lang.reflect.Constructor;
import java.util.*;
import static io.rwagent.client.G53ContinuingOperationsHarness.*;
import static io.rwagent.client.StrategyDirector.map;

/** Actual BattleClient/HTTP path with synthetic clocks, not natural native-game evidence. */
public final class R8ExecutionAdmissionHarness {
    static int checks;
    static void require(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static Object post(Fixture f,String owner,String path,CommandArbiter.Stamp stamp)throws Exception{
        return call(f.battle,"scheduledPost",new Class<?>[]{String.class,String.class,CommandArbiter.Stamp.class},owner,path,stamp);
    }
    static void missingMenuActorDoesNotTerminate()throws Exception{
        try(Fixture f=new Fixture()){
            f.sample(1);long credits=f.scheduler.effectiveCredits();int posts=f.posts.size();
            set(f.battle,"nextTraceCommandContext",map("nativeCost",1500,"investmentSlotAvailable",1,
                    "costSourceObservationId","newer-menu","costSourceRequestPath","/economy/investments"));
            require(post(f,CommandArbiter.DEFAULT_OWNER,"/command/invest?unitId=999&actionId=upgrade",f.arbiter.stamp())==null,
                    "a newer menu actor missing from legal own state is a local rejection, not an exception");
            require(f.posts.size()==posts&&f.scheduler.effectiveCredits()==credits&&f.scheduler.unsettledCreditCount()==0,
                    "rejected actor consumes no HTTP request, money or unsettled hold");
            require(f.scheduler.availableProducerSlots(999)==null,"pre-snapshot rejection cannot create a producer slot");
            require(((Map<?,?>)get(f.battle,"nextTraceCommandContext")).isEmpty(),"rejection drains quote context");
            require(post(f,CommandArbiter.DEFAULT_OWNER,"/command/move?unitIds=1,999&x=400&y=100",f.arbiter.stamp())==null,
                    "mixed group is rejected atomically, never silently filtered");
            require(f.posts.size()==posts,"no partial group transport");
            ForceController.Proposal missing=new ForceController.Proposal("force:service-test",Collections.singletonList(999L),
                    "/command/move?unitId=999&x=400&y=100","CONTINUING_FORCE",40,"FIXTURE_MISSING_ACTOR",null);
            call(f.battle,"collectForceProposal",new Class<?>[]{ForceController.Proposal.class},missing);
            require(((List<?>)get(f.battle,"forceProposals")).isEmpty(),"force collection also tolerates actor absence");
            require(post(f,CommandArbiter.DEFAULT_OWNER,"/command/move?unitId=1&x=400&y=100",f.arbiter.stamp())!=null,
                    "unrelated legal builder remains executable in the same observation");
            f.sample(2);require(post(f,CommandArbiter.DEFAULT_OWNER,"/command/move?unitId=100&x=400&y=100",
                    new CommandArbiter.Stamp("s","team:0",1,250))==null,"stale stamp locally rejected before snapshot");
            ((BufferedWriter)get(f.battle,"log")).flush();String log=f.log.toString();
            require(log.contains("BEFORE_GENERATION_SNAPSHOT")&&log.contains("ACTOR_NOT_OWN")&&log.contains("actorAbsenceProvesDeath"),
                    "negative admission has actor/reason/provenance evidence without inventing death");
            require(!f.arbiter.claim("invented",999),"rejection does not inject or claim unknown actor");
        }
    }
    static void collectedGenerationStillRejected()throws Exception{
        try(Fixture f=new Fixture()){
            f.sample(1);f.force(40);int posts=f.posts.size();
            require(f.arbiter.transfer("force:service-test","force:other",10),"actual transfer increments generation");
            require(f.arbiter.transfer("force:other","force:service-test",10),"ABA transfer back keeps newer generation");
            call(f.battle,"dispatchG4Forces",new Class<?>[]{int.class},Integer.MIN_VALUE);
            require(f.posts.size()==posts,"pre-collected old generation cannot dispatch after ABA");
            ((BufferedWriter)get(f.battle,"log")).flush();require(f.log.toString().contains("STALE_OWNER_GENERATION"),"runtime retains scheduler stale-generation evidence");
        }
    }
    static void ordinaryConstructionPaidBeforeVisible()throws Exception{
        for(String kind:new String[]{"landFactory","extractor"})try(Fixture f=new Fixture()){
            Map<String,Object> state=f.sample(1);
            Class<?> type=Class.forName("io.rwagent.client.BattleClient$BuildJob");
            Constructor<?> c=type.getDeclaredConstructor(String.class,double.class,double.class,long.class,long.class,String.class);c.setAccessible(true);
            Object job=c.newInstance(kind,100d,100d,700L,0L,"R8_SAME_SITE_FIXTURE");
            set(job,"costSource",map("costSourceObservationId","quote:build","costSourceRequestPath","/economy/plan?unitId=1"));
            set(f.battle,"buildJob",job);
            @SuppressWarnings("unchecked") Set<Long> known=(Set<Long>)get(f.battle,"jobKnownUnits");known.add(100L);
            int posts=f.posts.size();
            call(f.battle,"advanceBuildJob",new Class<?>[]{Map.class},state);
            require(Boolean.TRUE.equals(get(job,"orderAccepted")),"ordinary non-capacity construction accepts one receipt: "+kind);
            require(f.posts.size()==posts+1&&f.scheduler.unsettledCreditTotal()==700,"one real transport and one pending bill: "+kind);
            for(int frame=2;frame<=5;frame++){
                state=f.sample(frame);call(f.battle,"advanceBuildJob",new Class<?>[]{Map.class},state);
            }
            require(f.posts.size()==posts+1&&f.scheduler.unsettledCreditTotal()==700,"later polls without site cannot repeat or refund construction: "+kind);
            require(get(f.battle,"buildJob")==job,"receipt alone retains the job rather than declaring completion");
            Map<String,Object> built=Fixture.unit(975,kind.equals("extractor")?"extractorT1":"landFactory",100,true,false);
            f.own.add(built);state=f.sample(6);call(f.battle,"advanceBuildJob",new Class<?>[]{Map.class},state);
            require(get(f.battle,"buildJob")==null,"later actual own-site ready witness completes the same job");
            require(f.posts.size()==posts+1,"ready site does not submit another build");
        }
    }
    public static void main(String[] args)throws Exception{
        missingMenuActorDoesNotTerminate();collectedGenerationStillRejected();ordinaryConstructionPaidBeforeVisible();
        System.out.println("R8ExecutionAdmissionHarness PASS checks="+checks);
    }
}
