package io.rwagent.client;

import java.io.BufferedWriter;
import java.util.*;
import static io.rwagent.client.G53ContinuingOperationsHarness.*;
import static io.rwagent.client.StrategyDirector.map;

/** Real BattleClient transport/admission with synthetic time. No natural production claim. */
public final class R9ProductionTimeoutHarness {
    static int checks;
    static void require(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    @SuppressWarnings("unchecked") static Map<Long,BattleClient.Pending> pending(Fixture f)throws Exception{
        return (Map<Long,BattleClient.Pending>)get(f.battle,"pending");
    }
    static BattleClient.Pending waitFor(Fixture f,String type,boolean active)throws Exception{
        BattleClient.Pending p=new BattleClient.Pending();p.type=type;p.active=active;p.started=0;p.tier=1;
        pending(f).put(100L,p);return p;
    }
    static void update(Fixture f,Map<String,Object> state)throws Exception{call(f.battle,"updatePending",new Class<?>[]{Map.class},state);}
    static Map<String,Object> sample(Fixture f,int frame)throws Exception{
        // The shared throughput fixture clears pending to fake instant completion. This
        // harness deliberately preserves it; timeout behavior must face the original job.
        Map<Long,BattleClient.Pending> jobs=new LinkedHashMap<Long,BattleClient.Pending>(pending(f));
        Map<String,Object> state=f.sample(frame);pending(f).putAll(jobs);return state;
    }
    static Map<String,Object> factory(Fixture f){for(Map<String,Object> u:f.own)if(((Number)u.get("id")).longValue()==100)return u;throw new AssertionError();}
    static void busyIsLocalAndOneDiagnostic()throws Exception{
        try(Fixture f=new Fixture()){
            factory(f).put("productionQueue",1);waitFor(f,"heavyTank",false);Map<String,Object> s=sample(f,724);
            update(f,s);require(pending(f).containsKey(100L)&&pending(f).get(100L).active,"busy queue retains producer expectation after timeout");
            require(pending(f).get(100L).unconfirmedReported,"timeout becomes local UNKNOWN");
            update(f,sample(f,725));((BufferedWriter)get(f.battle,"log")).flush();
            String log=f.log.toString();require(log.split("\"event\":\"production_witness_unconfirmed\"",-1).length-1==1,"busy unchanged timeout is not a log flood");
            Object move=call(f.battle,"scheduledPost",new Class<?>[]{String.class,String.class,CommandArbiter.Stamp.class},
                    CommandArbiter.DEFAULT_OWNER,"/command/move?unitId=1&x=400&y=100",f.arbiter.stamp());
            require(move!=null,"unrelated actor transport survives local producer timeout");
            factory(f).put("productionQueue",0);update(f,sample(f,726));
            require(pending(f).isEmpty(),"later nonempty-to-empty witness retires local expectation");
            require(((Number)get(f.battle,"techs")).intValue()==0,"ordinary queue empty invents no upgrade");
        }
    }
    static void unknownAndUpgradeNotSuccess()throws Exception{
        try(Fixture f=new Fixture()){
            factory(f).remove("productionQueue");waitFor(f,"heavyTank",true);update(f,sample(f,724));
            require(pending(f).containsKey(100L),"missing queue is UNKNOWN rather than empty/ready");
            ((BufferedWriter)get(f.battle,"log")).flush();require(f.log.toString().contains("CURRENT_QUEUE_UNKNOWN"),"unknown source is explained");
        }
        try(Fixture f=new Fixture()){
            factory(f).put("techLevel",1);waitFor(f,"upgrade",true);update(f,sample(f,1));
            require(pending(f).isEmpty(),"explicit idle queue with no paid uncertainty can retire bad upgrade expectation");
            require(((Number)get(f.battle,"techs")).intValue()==0,"empty queue without tier witness is not upgrade completion");
            ((BufferedWriter)get(f.battle,"log")).flush();require(f.log.toString().contains("UPGRADE_TIER_UNCONFIRMED"),"upgrade discrepancy is local diagnostic");
            waitFor(f,"upgrade",false);factory(f).put("techLevel",2);update(f,sample(f,2));
            require(((Number)get(f.battle,"techs")).intValue()==1&&pending(f).isEmpty(),"real later tier advance remains completion witness");
        }
    }
    static void unobservedPaymentCannotDisappear()throws Exception{
        try(Fixture f=new Fixture()){
            f.sample(1);f.battle.readStrategy("/combat/production","r9_current_quote");
            set(f.battle,"nextTraceCommandContext",map("product","tank","nativeCost",350));
            Object receipt=call(f.battle,"scheduledPost",new Class<?>[]{String.class,String.class,CommandArbiter.Stamp.class},
                    CommandArbiter.DEFAULT_OWNER,"/command/queue?unitId=100&actionId=produceTank",f.arbiter.stamp());
            require(receipt!=null&&f.scheduler.unsettledCreditTotal()==350,"real accepted receipt holds credits before visible effect");
            waitFor(f,"heavyTank",false);update(f,sample(f,724));
            require(pending(f).containsKey(100L),"unobserved paid queue retains conservative local slot ghost");
            require(f.scheduler.unsettledCreditTotal()==350,"timer cannot refund unresolved payment");
            NativeCreditWitness witness=(NativeCreditWitness)get(f.battle,"creditWitness");
            require(witness.hasUnsettledQueue(100),"producer-specific unresolved effect is queryable");
            factory(f).put("productionQueue",1);update(f,sample(f,725));
            require(f.scheduler.unsettledCreditTotal()==0&&!witness.hasUnsettledQueue(100),"later native queue effect settles actual bill independently");
            factory(f).put("productionQueue",0);update(f,sample(f,726));require(pending(f).isEmpty(),"later queue finish unblocks producer");
        }
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.globalStrategy","false");System.setProperty("rwagent.g1Trace","true");
        busyIsLocalAndOneDiagnostic();unknownAndUpgradeNotSuccess();unobservedPaymentCannotDisappear();
        System.out.println("R9ProductionTimeoutHarness PASS checks="+checks+" evidence=SYNTHETIC_HTTP_NO_NATIVE_ENGINE");
    }
}
