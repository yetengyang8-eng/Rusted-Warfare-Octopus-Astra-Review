package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Actual StrategyDirector worker allocation with synthetic lawful menus/receipts/own frames.
 * This demonstrates causal controller behavior, not human Spain improvement or native arrival. */
public final class G51ProspectIntegrationHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<String,Object> unit(long id,String type,double x,double y){return map("id",id,"type",type,"x",x,"y",y,"hp",1000,"maxHp",1000,
        "buildProgress",1,"productionQueue",0,"mobile",!"commandCenter".equals(type)&&!"landFactory".equals(type),"canAttack","heavyTank".equals(type),"dead",false,"orderType",null);}
    static Map<String,Object> resource(long tile,double x,double y){return map("tile",tile,"x",x,"y",y,"currentlyVisible",true,"lastSeenGameTimeMs",120000L);}
    static class Fixture implements StrategyDirector.Host {
        final CommandArbiter gate=new CommandArbiter();final StrategyDirector director=new StrategyDirector(this,gate);
        final List<String> orders=new ArrayList<String>();final List<Map<String,Object>> events=new ArrayList<Map<String,Object>>();
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),force=new ArrayList<Map<String,Object>>(),resources=new ArrayList<Map<String,Object>>(),visible=new ArrayList<Map<String,Object>>(),threats=new ArrayList<Map<String,Object>>();
        final Map<String,Object> state,enemies,scout,builder;Map<String,Object> plan;long now,frame;
        boolean denyApproach,unknownVisibility;double credits=10000;
        Fixture()throws Exception{
            director.enable(map("strategyContractVersion",1),128);
            own.add(unit(1,"commandCenter",7370,1950));own.add(unit(2,"builder",7370,1900));own.add(unit(90,"landFactory",7330,1850));
            builder=unit(5924,"builder",6330,3710);own.add(builder);
            for(long id=10;id<16;id++){Map<String,Object> actor=unit(id,"heavyTank",7200,1700);force.add(actor);own.add(actor);}
            state=map("sessionId","session","playerId",0,"player",map("credits",credits),"map",map("tilesWide",400,"tilesHigh",370),"ownUnits",own);
            enemies=map("sessionId","session","playerId",0,"visibleEnemies",visible,"enemyIntel",Collections.emptyList());
            scout=map("sessionId","session","playerId",0,"resources",resources,"rememberedThreats",threats);
            resources.add(resource(42,7410,410));observe(120000,100);
            for(Map<String,Object> actor:force)check(gate.claim("general:1",((Number)actor.get("id")).longValue()),"ordinary force is lawfully owned by a General");
        }
        public boolean assessOwnedOrdinaryStrategy(){return true;}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/expansion/plan"))return plan;
            if(path.startsWith("/scout/visible?tiles=")){
                if(unknownVisibility)return null;
                List<Map<String,Object>> tiles=new ArrayList<Map<String,Object>>();
                for(String tile:path.split("tiles=")[1].split("&")[0].split(","))tiles.add(map("tile",Long.parseLong(tile),"visible",true));
                return map("sessionId","session","frame",frame,"gameTimeMs",now,"tiles",tiles);
            }
            if(path.startsWith("/scout/resource-approach")){
                long tile=Long.parseLong(path.split("tile=")[1].split("&")[0]);
                for(Map<String,Object> resource:resources)if(((Number)resource.get("tile")).longValue()==tile)
                    return map("status","planned","sessionId","session","frame",frame,"gameTimeMs",now,"unitId",5924L,"tile",tile,"pathKnown",!denyApproach,"x",resource.get("x"),"y",resource.get("y"));
            }
            if(path.startsWith("/combat/engagement")){
                List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();for(Map<String,Object> actor:force)
                    actors.add(map("unitId",actor.get("id"),"status","APPROACH_PATH_KNOWN","compatibility","COMPATIBLE","approachX",7300,"approachY",410));
                long target=Long.parseLong(path.split("targetId=")[1].split("&")[0]);
                return map("sessionId","session","frame",frame,"gameTimeMs",now,"targetId",target,"targetVisible",true,"targetObservedAtGameTimeMs",now,"actors",actors);
            }
            return null;
        }
        public Map<String,Object> orderStrategy(String owner,String path){
            long id=Long.parseLong(path.split("unitId=")[1].split("&")[0]);
            check(gate.admit(gate.stamp(),owner,Collections.singletonList(id))==null,"real arbiter validates worker owner and per-observation admission");
            orders.add(path);return map("status","queued","frame",frame,"gameTimeMs",now,"unitIds",Collections.singletonList(id),"requestId","r"+orders.size());
        }
        public void emitStrategy(String event,Map<String,Object> data){events.add(map("event",event,"data",new LinkedHashMap<String,Object>(data)));}
        public void spendStrategy(String category,long cost,String product,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
        void observe(long time,long nextFrame)throws Exception{
            now=time;frame=nextFrame;state.put("frame",frame);state.put("gameTimeMs",now);((Map<String,Object>)state.get("player")).put("credits",credits);
            enemies.put("frame",frame);enemies.put("gameTimeMs",now);scout.put("frame",frame);scout.put("gameTimeMs",now);
            for(Map<String,Object> resource:resources)resource.put("lastSeenGameTimeMs",now);
            for(Map<String,Object> enemy:visible)enemy.put("lastSeenGameTimeMs",now);
            for(Map<String,Object> threat:threats)threat.put("lastSeenGameTimeMs",now);
            List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:own)ids.add(((Number)actor.get("id")).longValue());
            gate.observe(new CommandArbiter.Stamp("session","team:0",frame,now),ids);
            director.observe(state,enemies,scout,force,0,900000,100,40,0,false);
        }
        long eventCount(String type){return events.stream().filter(e->type.equals(e.get("event"))).count();}
        void start()throws Exception{
            director.act(0,1);
            check(orders.size()==1&&orders.get(0).startsWith("/command/move?unitId=5924")&&orders.get(0).contains("x=7410.0"),"additional worker makes real accepted north prospect move");
            check(eventCount("strategy_prospect_observed")==0,"accepted receipt alone creates no arrival");
        }
        void arrive()throws Exception{
            builder.put("x",7410.);builder.put("y",410.);builder.put("orderType","move");builder.put("orderX",7410.);builder.put("orderY",410.);
            observe(128000,101);
            check(eventCount("strategy_prospect_observed")==1,"later own state produces actual prospect arrival");
        }
        void localPlan(double cost){plan=map("status","planned","sessionId","session","frame",frame,"gameTimeMs",now,"unitId",5924L,
            "extractorCost",cost,"extractorX",7470.,"extractorY",610.,"affordable",true);}
    }
    static void arrivedLocalBuild()throws Exception{
        Fixture f=new Fixture();f.start();f.arrive();f.localPlan(700);
        check(f.director.act(0,1),"arrived worker converts current affordable local plan into construction");
        check(f.orders.size()==2&&f.orders.get(1).startsWith("/command/build-extractor?unitId=5924")&&f.orders.get(1).contains("x=7470.0"),"native local mine beats cross-map reallocation");
        check(f.gate.reserved(5924)&&f.eventCount("strategy_construction_ordered")==1,"construction retains unique strategy lease");f.director.close();
        Fixture budget=new Fixture();budget.start();budget.arrive();budget.credits=300;budget.observe(136000,102);budget.localPlan(700);budget.director.act(0,1);
        check(budget.orders.size()==1,"cluster commitment cannot bypass native price/free-budget admission");budget.director.close();
    }
    static void localBeforeGlobal()throws Exception{
        Fixture f=new Fixture();f.start();f.arrive();f.resources.add(resource(43,7470,610));f.resources.add(resource(99,6330,3710));
        f.director.act(0,1);
        check(f.orders.size()==2&&f.orders.get(1).contains("x=7470.0"),"plan unavailable: existing arrived north cluster keeps next reachable local prospect");
        check(f.orders.stream().noneMatch(p->p.contains("x=6330.0")),"south resource does not displace local commitment");
        f.builder.put("x",7470.);f.builder.put("y",610.);f.observe(136000,102);f.localPlan(700);f.director.act(0,1);
        check(f.eventCount("strategy_prospect_observed")==2,"second local own arrival exits PROSPECT using latest receipt endpoint");
        check(f.orders.size()==3&&f.orders.get(2).startsWith("/command/build-extractor"),"local continuation arrival converts into mine without losing cluster anchor");f.director.close();
        Fixture occupied=new Fixture();occupied.start();occupied.arrive();occupied.resources.add(resource(43,7470,610));occupied.resources.add(resource(99,6330,3710));
        occupied.own.add(unit(400,"extractorT1",7470,610));occupied.own.add(unit(401,"extractorT1",7410,410));occupied.director.act(0,1);
        check(occupied.orders.size()==1,"currently occupied local mine cannot be selected; first local assessment holds global redirect");occupied.director.close();
    }
    static void exhaustedAndDanger()throws Exception{
        Fixture f=new Fixture();f.start();f.arrive();f.resources.add(resource(99,6330,3710));f.own.add(unit(400,"extractorT1",7410,410));f.director.act(0,1);
        check(f.orders.size()==1,"one complete empty local assessment preserves commitment");
        f.director.act(0,1);check(f.orders.size()==1,"repeat act in same observation cannot spend second empty assessment");
        f.observe(144000,102);f.director.act(0,1);
        check(f.orders.size()==2&&f.orders.get(1).contains("x=6330.0"),"second fresh empty assessment releases commitment for global prospect");f.director.close();
        Fixture hostile=new Fixture();hostile.start();hostile.arrive();hostile.localPlan(700);
        Map<String,Object> enemy=map("id",600L,"type","heavyTank","x",7420.,"y",420.,"hp",1000,"maxHp",1000,"canAttack",true,"building",false,"range",160.,"lastSeenGameTimeMs",136000L);
        hostile.visible.add(enemy);hostile.threats.add(enemy);hostile.observe(136000,102);hostile.director.act(0,1);
        check(hostile.orders.stream().noneMatch(p->p.startsWith("/command/build-extractor")),"current legal hostile proximity prevents local commitment mine build");
        check(hostile.events.stream().anyMatch(e->"strategy_prospect_cluster_released".equals(e.get("event"))),"current visible threat immediately releases local commitment");hostile.director.close();
    }
    static void unknownCoverage()throws Exception{
        Fixture f=new Fixture();f.start();f.arrive();f.resources.add(resource(99,6330,3710));f.own.add(unit(400,"extractorT1",7410,410));f.unknownVisibility=true;
        f.director.act(0,1);f.observe(144000,102);f.director.act(0,1);f.observe(160000,103);f.director.act(0,1);
        check(f.orders.size()==1,"missing native local visibility cannot count as repeated empty assessments");
        f.unknownVisibility=false;f.observe(176000,104);f.director.act(0,1);
        check(f.orders.size()==1,"first recovered complete native local visibility starts empty window");
        f.observe(192000,105);f.director.act(0,1);
        check(f.orders.size()==2&&f.orders.get(1).contains("x=6330.0"),"only two complete recovered observations permit southern replan");f.director.close();
    }
    public static void main(String[] args)throws Exception{arrivedLocalBuild();localBeforeGlobal();exhaustedAndDanger();unknownCoverage();System.out.println("G51ProspectIntegrationHarness checks="+checks+" PASS");}
}
