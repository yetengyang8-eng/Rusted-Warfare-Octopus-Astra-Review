package io.rwagent.client;
import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

public final class R11GeneralMotionHarness {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Fixture extends ForceControllerHarness.Fixture {
        final GeneralCombatDirector combat=new GeneralCombatDirector(registry,this);
        final List<Map<String,Object>> plans=new ArrayList<Map<String,Object>>();boolean patrol;
        final List<String> reads=new ArrayList<String>();
        Fixture()throws Exception{controller.setGeneralCombatDirector(combat);}
        public Map<String,Object> read(String path,String event){reads.add(path);long anchor=Long.parseLong(path.split("unitId=")[1].split("&")[0]);if(anchor<10||anchor>15)return map("status","no_frontier");
            if(patrol&&path.contains("role=army"))return map("status","no_frontier");
            return plans.isEmpty()?map("status","no_frontier"):plans.remove(0);}
        void plan(int tile,double x){plans.add(map("status","planned","sessionId","s","pathKnown",true,"targetTile",tile,"frontierTile",9000,"targetX",x,"targetY",100.));}
        void sample(long time){observe(time);combat.observe(state,enemies,100,100,now);}
        void run()throws Exception{combat.observe(state,enemies,100,100,now);collect();}
        List<ForceController.Proposal> first(){List<ForceController.Proposal> out=new ArrayList<ForceController.Proposal>();for(ForceController.Proposal p:proposals)if(p.owner.equals("general:1"))out.add(p);return out;}
        void accept()throws Exception{for(ForceController.Proposal p:first())p.accepted(map("status","queued","frame",frame,"gameTimeMs",now));}
    }
    static void beforeQuorumArrival()throws Exception{
        Fixture f=new Fixture();f.plan(101,800);f.run();check(f.first().size()==1,"initial lawful mission starts");f.accept();
        for(long id=10;id<16;id++){f.find(id).put("x",id<13?700.:600.);f.find(id).put("orderType","attackMove");f.find(id).put("orderX",800.);f.find(id).put("orderY",100.);}
        f.sample(5000);f.plan(102,1500);f.run();
        check(f.first().size()==1&&f.first().get(0).path.contains("x=1500.0"),"forward continuation can be issued before the whole formation reaches its old waypoint");
        Map<String,Object> mission=(Map<String,Object>)f.combat.view(f.registry.generals().get(0).id).get("outwardMission");
        check("CURRENT_OWN_FRONTIER_LOOKAHEAD_NOT_ARRIVAL".equals(mission.get("lastEvent")),"lookahead is explicitly not actual quorum arrival");
    }
    static void exploredMapPatrol()throws Exception{
        Fixture f=new Fixture();f.patrol=true;f.plan(501,1000);f.run();
        check(f.reads.stream().anyMatch(p->p.contains("role=recon&unitId=")),"exhausted unvisited frontier falls back to lawful deep/stale fog patrol");
        check(f.first().size()==1&&f.first().get(0).actors.size()==6,"the attached General continues as a formation instead of waiting idle");
    }
    static void currentGroundClaim()throws Exception{
        Fixture f=new Fixture();f.enemy(801,500,100);f.run();
        check(!f.combat.currentGroundTargetClaim(f.state,f.enemies,f.now,801),"proposal alone does not reserve enemy away from Air");
        f.accept();check(f.combat.currentGroundTargetClaim(f.state,f.enemies,f.now,801),"actual owned accepted current Ground advance can reserve a lawful contact");
        for(long actor=10;actor<16;actor++)f.find(actor).put("orderType","idle");f.sample(5000);
        check(!f.combat.currentGroundTargetClaim(f.state,f.enemies,f.now,801),"an old receipt without a current native advance cannot keep Air away from an idle Ground target");
        for(long actor=10;actor<16;actor++){f.find(actor).put("x",500.);f.find(actor).put("orderType",null);}f.sample(5500);
        check(!f.combat.currentGroundTargetClaim(f.state,f.enemies,f.now,801),"real native null idle near the old destination cannot keep a Ground attack claim");
        f.enemyRows.clear();f.sample(6000);check(!f.combat.currentGroundTargetClaim(f.state,f.enemies,f.now,801),"lost contact cannot preserve a ghost Ground claim");
    }
    public static void main(String[] args)throws Exception{beforeQuorumArrival();exploredMapPatrol();currentGroundClaim();System.out.println("R11GeneralMotionHarness checks="+checks+" PASS fixtureOnly=true");}
}
