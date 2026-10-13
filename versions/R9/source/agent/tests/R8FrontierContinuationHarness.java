package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Intermediate native waypoint arrival is not completion of a distant information frontier. */
public final class R8FrontierContinuationHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Fixture extends ForceControllerHarness.Fixture {
        final GeneralCombatDirector combat=new GeneralCombatDirector(registry,this);
        final List<Map<String,Object>> plans=new ArrayList<Map<String,Object>>();
        int firstReads;Map<String,Object> repeated;
        Fixture()throws Exception{super();controller.setGeneralCombatDirector(combat);}
        void sample(long time){observe(time);combat.observe(state,enemies,100,100,now);}
        @Override void collect()throws Exception{combat.observe(state,enemies,100,100,now);super.collect();}
        @Override public Map<String,Object> read(String path,String event){if(!path.contains("unitId=10&"))return map("status","no_frontier");firstReads++;return plans.isEmpty()?(repeated==null?map("status","no_frontier"):repeated):plans.remove(0);}
        void plan(int tile,int outer,double x){plans.add(map("status","planned","sessionId","s","pathKnown",true,"targetTile",tile,"frontierTile",outer,"targetX",x,"targetY",100.));}
        List<ForceController.Proposal> first(){List<ForceController.Proposal> out=new ArrayList<ForceController.Proposal>();for(ForceController.Proposal p:proposals)if(p.owner.equals("general:1"))out.add(p);return out;}
        void accept()throws Exception{for(ForceController.Proposal p:first())p.accepted(map("status","queued","frame",frame,"gameTimeMs",now));}
    }
    static void continueSameOuterFromLaterOwnWaypoint()throws Exception{
        Fixture f=new Fixture();f.plan(101,9000,800);f.collect();check(f.first().size()==1,"initial native path produces a General proposal");f.accept();
        for(long id=10;id<16;id++)f.find(id).put("x",800.);f.sample(5000);f.plan(102,9000,1500);f.collect();
        check(f.first().size()==1&&f.first().get(0).path.contains("x=1500.0"),"arrival at an intermediate waypoint permits a new native step toward the same still-unknown outer frontier");
        check(f.firstReads==2,"continuing an unknown outer frontier needs one new plan after actual waypoint arrival");f.accept();
        for(long id=10;id<16;id++){f.find(id).put("orderType","attackMove");f.find(id).put("orderX",1500.);f.find(id).put("orderY",100.);}
        int reads=f.firstReads;f.sample(20000);f.collect();
        check(f.firstReads==reads&&f.first().isEmpty(),"current accepted native destination is maintained without another planner request or duplicate command");
        f.enemy(801,850,100);f.sample(21000);f.collect();f.accept();
        f.enemyRows.clear();f.sample(40000);f.collect();
        check(f.firstReads==reads&&f.first().stream().anyMatch(p->p.path.contains("x=1500.0")),"temporary combat resumes the accepted outward step without replanning");
    }
    static void repeatedFallbackDoesNotRepeatSameSearch()throws Exception{
        Fixture f=new Fixture();f.repeated=map("status","planned","sessionId","s","pathKnown",true,"targetTile",201,"frontierTile",9000,"targetX",140.,"targetY",100.,"fallbackName","GLOBAL_EXHAUSTIVE");f.collect();
        check(f.firstReads==2,"a native fallback repeating the same rejected candidate ends this search rather than issuing a third identical expensive request");
        check(f.first().isEmpty(),"duplicate candidate detection does not manufacture an order to an already near point");
        f.repeated=map("status","planned","sessionId","s","pathKnown",true,"targetTile",202,"frontierTile",9000,"targetX",1000.,"targetY",100.);f.sample(6000);f.collect();
        check(f.first().size()==1,"a subsequent legal new waypoint on that outer frontier remains available without a timed cooldown gate");
    }
    public static void main(String[] args)throws Exception{continueSameOuterFromLaterOwnWaypoint();repeatedFallbackDoesNotRepeatSameSearch();System.out.println("R8FrontierContinuationHarness checks="+checks+" PASS");}
}
