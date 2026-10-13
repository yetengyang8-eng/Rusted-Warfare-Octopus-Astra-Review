package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Geometry/ownership fixtures, not a simulated siege outcome. */
public final class R16GroundBaseHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Fixture implements ForceController.Host {
        final CommandArbiter gate=new CommandArbiter();final GeneralRegistry registry=new GeneralRegistry(gate);
        final GeneralCombatDirector combat=new GeneralCombatDirector(registry,this);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),enemies=new ArrayList<Map<String,Object>>();
        final List<ForceController.Proposal> orders=new ArrayList<ForceController.Proposal>();
        final GeneralRegistry.GeneralId general=new GeneralRegistry.GeneralId(1);
        final Map<String,Object> state=map("sessionId","r16","player",map("credits",10000),"map",map("width",8000.,"height",8000.),"ownUnits",own);
        final Map<String,Object> packet=map("sessionId","r16","visibleEnemies",enemies,"catalogGameJarMatched",true,"catalogSha256",TargetCatalog.catalogSha256());
        boolean targetAllowed=true;long frame;
        Fixture(int count,int local){List<Long> ids=new ArrayList<Long>();for(long id=1;id<=count;id++){
            Map<String,Object> actor=ForceControllerHarness.unit(id,"heavyTank",id<=local?500:6500,1000);actor.put("hp",600.);actor.put("maxHp",600.);own.add(actor);ids.add(id);}
            gate.observe(new CommandArbiter.Stamp("r16","team:0",0,0),ids);registry.bootstrap(Arrays.asList(ids),ids);
            enemies.add(map("id",900L,"type","commandCenter","x",1000.,"y",1000.,"hp",4000.,"building",true,"canAttack",true,"targetDomain","SURFACE"));
        }
        void sample()throws Exception{frame++;long now=frame*1000;List<Long> ids=new ArrayList<Long>();double x=0;
            for(Map<String,Object> a:own){ids.add(((Number)a.get("id")).longValue());x+=((Number)a.get("x")).doubleValue();}
            gate.observe(new CommandArbiter.Stamp("r16","team:0",frame,now),ids);state.put("frame",frame);state.put("gameTimeMs",now);packet.put("frame",frame);packet.put("gameTimeMs",now);
            for(Map<String,Object> e:enemies){e.put("lastSeenGameTimeMs",now);e.put("domainObservedAtGameTimeMs",now);}
            registry.updateGeneralCentroid(general,x/own.size(),1000);orders.clear();combat.observe(state,packet,100,100,now);combat.collect(state,packet,now);
        }
        public Map<String,Object> read(String path,String event){return map("status","planned","pathKnown",true,"targetTile",100,"targetX",1000.,"targetY",1000.);}
        public List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target,Map<String,Object> packet){return targetAllowed?actors:Collections.emptyList();}
        public void collect(ForceController.Proposal p){orders.add(p);}
        boolean attacks(){return orders.stream().anyMatch(p->p.path.startsWith("/command/attack-move"));}
        Map<?,?> approach(){return (Map<?,?>)combat.view(general).get("baseApproach");}
    }
    static void separatedRosterCannotFundSiege()throws Exception{
        Fixture f=new Fixture(14,2);f.sample();check(!f.attacks(),"two lead tanks cannot borrow twelve remote tanks to enter a base");
        check(((List<?>)f.approach().get("localActorIds")).size()==2,"decision names the actual local support, not desired or total roster");
        check(f.orders.stream().allMatch(p->p.path.startsWith("/command/move")),"denied advance gathers using MOVE outside fire");
        for(Map<String,Object> a:f.own)a.put("x",500.);f.sample();check(f.attacks(),"later own clustered positions admit existing strong-force siege without a new timer");
        check(f.registry.general(f.general).members.size()==14&&f.registry.unit(1).owner.equals("general:1"),"gathering preserves General membership and owner");
    }
    static void frontierCannotBypassBase()throws Exception{
        Fixture f=new Fixture(14,2);f.targetAllowed=false;f.sample();check(!f.attacks(),"filtered/ineligible target cannot return as an ordinary frontier through its fire zone");
        f.enemies.clear();f.sample();check(!f.attacks(),"remembered base protects the small local approach despite large remote roster");
        for(Map<String,Object> a:f.own)a.put("x",500.);f.sample();check(f.attacks(),"existing large physical formation retains remembered-zone release and frontier mobility");
        check(f.combat.view(f.general).get("tacticalChoice").equals("FRONTIER"),"memory release is travel, not permission to attack a ghost");
        Fixture cheap=new Fixture(14,2);cheap.enemies.add(map("id",901L,"type","extractor","hp",800.,"x",1020.,"y",1000.,"building",true,"canAttack",false,"targetDomain","SURFACE"));cheap.sample();
        check(!cheap.attacks(),"cheap nearby target does not price the base as an eight-hundred-HP objective");
    }
    public static void main(String[] args)throws Exception{separatedRosterCannotFundSiege();frontierCannotBypassBase();System.out.println("R16_GROUND_BASE_OK checks="+checks+" fixtureOnly=true");}
}
