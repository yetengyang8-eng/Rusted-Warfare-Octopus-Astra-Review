package io.rwagent.client;

import static io.rwagent.client.R11ScoutMotionHarness.*;

/** Turns change destination before the old cell-center arrival, preserving one EXPLORE owner. */
public final class R13ScoutNavigationHarness {
    static int checks;
    static void require(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.g3Execution","true");System.setProperty("rwagent.g5","true");
        System.setProperty("rwagent.runtimeAdaptive","false");System.setProperty("rwagent.globalStrategy","false");System.setProperty("rwagent.g1Trace","false");
        try(Fixture x=new Fixture()){
            x.corner=2;x.steps=10;Object task=x.plan();long generation=x.f.arbiter.ownerGeneration(10);require(x.issue(),"initial east leg queued");
            x.actor.put("x",839.);x.tick(2,500);x.witness(task);require(!x.issue(),"cannot cut across an unknown diagonal before entering corner cell");
            x.actor.put("x",841.);x.tick(3,750);x.witness(task);
            require(x.issue(),"entering known corner cell sends south leg before old waypoint center at850");
            require(x.posts.get(x.posts.size()-1).contains("y=930.0"),"next real segment changes direction without a stop command");
            require(x.f.arbiter.ownerGeneration(10)==generation,"turn retains command owner generation");
            require(((Number)get(task,"waypointArrivedAt")).longValue()<0,"accepted turn command is not an arrival witness");
        }
        try(Fixture x=new Fixture()){
            x.steps=6;Object task=x.plan();require(x.issue(),"first frontier route starts");long generation=x.f.arbiter.ownerGeneration(10);
            x.actor.put("x",921.);x.tick(2,500);x.witness(task);x.steps=20;int before=x.posts.size();x.issue();
            require(x.posts.size()==before+1,"next frontier is planned and dispatched in the same cycle before endpoint930");
            require(x.f.arbiter.ownerGeneration(10)==generation&&((Number)get(get(x.f.battle,"reconTask"),"taskId")).longValue()==1,"frontier renewal preserves long mission and lease");
        }
        System.out.println("R13_SCOUT_NAVIGATION_OK checks="+checks+" fixtureOnly=true noNaturalMotionClaim=true");
    }
}
