package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Explicit synthetic /state-shaped packets; no native travel, coverage or victory claim. */
public final class R11AirContinuityHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Host implements StrategyDirector.Host {
        final R8AirSearchHarness.Host planner=new R8AirSearchHarness.Host();
        final G6EndgameHarness.Host nativeHost=planner.nativeHost;
        final Set<Long> ground=new HashSet<Long>();
        final List<Map<String,Object>> dispatches=new ArrayList<Map<String,Object>>();
        boolean rejectSearch;
        Host(){for(Map<String,Object> actor:nativeHost.own)actor.remove("movementType");nativeHost.sample();}
        public String landRouteDemandStrategy(){return "KNOWN";}
        public boolean groundTargetCommittedStrategy(long enemy){return ground.contains(enemy);}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/combat/engagement")){
                List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();
                for(String id:path.split("unitIds=")[1].split("&")[0].split(","))actors.add(map("unitId",Long.parseLong(id),
                        "movementType","AIR","compatibility","COMPATIBLE","status","APPROACH_PATH_KNOWN","approachX",300.,"approachY",100.));
                return map("sessionId","s","gameTimeMs",nativeHost.time,"targetObservedAtGameTimeMs",nativeHost.time,
                        "targetVisible",true,"targetId",G6EndgameHarness.Host.param(path,"targetId"),"actors",actors);
            }
            return planner.readStrategy(path,event);
        }
        public Map<String,Object> orderStrategy(String owner,String path){return orderStrategy(owner,path,Collections.emptyMap());}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context){
            String field=path.contains("unitIds=")?"unitIds":"unitId";String ids=path.split(field+"=")[1].split("&")[0];
            for(String id:ids.split(","))check(nativeHost.arbiter.owns(owner,Long.parseLong(id)),"each command uses its actual current owner");
            dispatches.add(map("owner",owner,"path",path,"context",context));
            if(rejectSearch&&"SEARCH".equals(context.get("reason")))return null;
            if(path.startsWith("/command/attack-move"))for(String id:ids.split(",")){
                Map<String,Object> actor=actor(Long.parseLong(id));actor.put("orderType","attackMove");
                actor.put("orderX",300.);actor.put("orderY",100.);
            }
            return nativeHost.orderStrategy(owner,path,context);
        }
        public void emitStrategy(String event,Map<String,Object> data){nativeHost.emitStrategy(event,data);}
        public void spendStrategy(String category,long cost,String type,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
        Map<String,Object> actor(long id){return planner.actor(id);}
        void tick(){nativeHost.frame++;nativeHost.time+=1000;nativeHost.sample();}
        void observe(AirGeneralController air)throws Exception{air.observe(nativeHost.state,nativeHost.enemy,nativeHost.vision,100,100);}
        void next(AirGeneralController air)throws Exception{tick();observe(air);}
        AirGeneralController formation()throws Exception{
            AirGeneralController air=new AirGeneralController(this,nativeHost.arbiter);
            for(long id:new long[]{10,11}){nativeHost.arbiter.claim("strategy:"+id,id);check(air.accept(id,"strategy:"+id,new CapabilityUnitMode(id)),"idle specialist lease transfers");}
            next(air);air.act();check(nativeHost.own.stream().noneMatch(a->a.containsKey("movementType")),"real own-state movementType remains omitted");return air;
        }
        void contact(AirGeneralController air,Map<String,Object>... enemies)throws Exception{nativeHost.enemy.put("visibleEnemies",Arrays.asList(enemies));observe(air);}
        long events(String event){return nativeHost.events.stream().filter(e->event.equals(e.get("event"))).count();}
    }
    static Map<String,Object> enemy(long id,String domain,boolean water,double x,double y){
        return map("id",id,"type","AIR".equals(domain)?"helicopter":"tank","x",x,"y",y,"hp",1000.,
                "targetDomain",domain,"touchingWater",water,"domainSourceId","native:am.i/Q/cH_after_legal_visibility");
    }
    static void lookaheadDoesNotInventArrival()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();long generation=h.nativeHost.arbiter.ownerGeneration(10);Object frontier=air.snapshot().get("routeFrontierTile");
        h.actor(10).put("x",800.);h.actor(10).put("y",110.);h.next(air);h.dispatches.clear();air.act();
        check(BattleClient.n(h.actor(10),"orderX")==1010&&BattleClient.n(h.actor(10),"orderY")==510,"later current own travel safely redirects before old corner arrival");
        check(BattleClient.n(air.snapshot(),"routeCursor")==0&&BattleClient.n(air.snapshot(),"searchLegsWitnessed")==0,"lookahead receipt advances neither arrival cursor nor witness count");
        check(h.events("air_general_search_lookahead")==1&&h.events("air_general_search_leg_witness")==0,"lookahead has its own non-arrival causal event");
        check(h.planner.plans==1&&frontier.equals(air.snapshot().get("routeFrontierTile")),"lookahead retains the same accepted native route and frontier");
        check(h.nativeHost.arbiter.ownerGeneration(10)==generation,"continuing MOVE never transfers or reclaims the owner generation");
        h.observe(air);air.act();check(h.events("air_general_search_lookahead")==1&&h.events("air_general_search_leg_witness")==0,"same-frame receipt or repeated observation cannot advance again");
        h.actor(10).put("x",1010.);h.actor(10).put("y",510.);h.next(air);
        check(h.events("air_general_search_leg_witness")==1,"later own destination witness establishes actual arrival");
        Map<String,Object> arrival=null;for(Map<String,Object> e:h.nativeHost.events)if("air_general_search_leg_witness".equals(e.get("event")))arrival=BattleClient.obj(e.get("data"));
        check(arrival!=null&&number(arrival,"routeCursor",-1)==65&&!Boolean.TRUE.equals(arrival.get("arrivalIsFullCoverage")),"actual next-leg endpoint advances its route index without full coverage inference");air.close();
    }
    static void rejectedAndUnsafeLookahead()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();h.actor(10).put("x",800.);h.actor(10).put("y",110.);h.next(air);h.rejectSearch=true;air.act();
        check(BattleClient.n(h.actor(10),"orderY")==110&&BattleClient.n(air.snapshot(),"routeCursor")==0,"failed redirect leaves old native travel and unproved cursor intact");
        h.actor(10).put("x",1010.);h.next(air);check(h.events("air_general_search_leg_witness")==0,"old endpoint after a rejected redirect is not arrival at the new leg");
        h.rejectSearch=false;air.act();check(BattleClient.n(h.actor(10),"orderY")==510,"owned retained route retries the unaccepted search redirect");air.close();
        h=new Host();air=h.formation();for(long id:new long[]{10,11}){h.actor(id).put("x",800.);h.actor(id).put("y",110.);}h.tick();
        h.nativeHost.vision.put("rememberedThreats",Arrays.asList(map("x",905.,"y",310.,"range",5.)));h.observe(air);h.dispatches.clear();air.act();
        check(BattleClient.n(h.actor(10),"orderY")<510,"newly known attack corridor vetoes unsafe future waypoint");
        for(Map<String,Object> dispatch:h.dispatches){String path=String.valueOf(dispatch.get("path"));check(!path.startsWith("/command/attack-move"),"remembered danger grants no attack authority");
            if(path.startsWith("/command/move")){double x=Double.parseDouble(path.split("x=")[1].split("&")[0]),y=Double.parseDouble(path.split("y=")[1].split("&")[0]);
                double dx=x-800,dy=y-110,t=Math.max(0,Math.min(1,(105*dx+200*dy)/(dx*dx+dy*dy)));
                check(Math.hypot(905-(800+t*dx),310-(110+t*dy))>=145,"redirected actual segment respects remembered range veto");}}
        check(BattleClient.n(air.snapshot(),"routeCursor")==0,"safe redirection is never a fabricated arrival");air.close();
    }
    static void unknownContactRetainsMission()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();h.contact(air,enemy(90,"SURFACE",false,100,100));air.act();Object frontier=air.snapshot().get("routeFrontierTile");
        for(int scenario=0;scenario<4;scenario++){
            h.tick();Map<String,Object> unknown=scenario==0?null:map("sessionId",scenario==1?"foreign":"s","gameTimeMs",scenario==2?h.nativeHost.time-1:h.nativeHost.time);
            if(unknown!=null&&scenario!=3)unknown.put("visibleEnemies",Collections.emptyList());
            air.observe(h.nativeHost.state,unknown,h.nativeHost.vision,100,100);h.dispatches.clear();air.act();
            check(number(air.snapshot(),"targetId",-1)==90&&frontier.equals(air.snapshot().get("routeFrontierTile")),"missing/foreign/stale/omitted-array contact source remains UNKNOWN while preserving target and original route");
            check(h.events("air_general_target_clear")==0&&h.dispatches.isEmpty()&&h.planner.plans==1,"UNKNOWN emits no legal absence, new attack, danger hold or destructive replan");
        }
        h.next(air);h.dispatches.clear();air.act();
        check(h.events("air_general_target_clear")==1&&air.snapshot().get("targetId")==null,"current lawful absent contact alone releases target duty");
        check(h.dispatches.stream().anyMatch(d->String.valueOf(d.get("path")).startsWith("/command/move"))&&h.planner.plans==1&&frontier.equals(air.snapshot().get("routeFrontierTile")),"fresh legal target loss restores original search order and cursor");air.close();
    }
    static void groundDutyAndSpatialPriority()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();Object frontier=air.snapshot().get("routeFrontierTile");h.ground.add(90L);
        h.contact(air,enemy(90,"SURFACE",false,100,100));h.dispatches.clear();air.act();
        check(air.snapshot().get("targetId")==null&&h.dispatches.isEmpty(),"current ground commitment leaves existing safe AIR search order in place");
        h.contact(air,enemy(90,"SURFACE",false,100,100),enemy(91,"AIR",false,1100,100));air.act();
        check(number(air.snapshot(),"targetId",-1)==91,"current air contact takes priority over covered nearby ground target");
        h.next(air);h.ground.clear();h.contact(air,enemy(90,"SURFACE",false,100,100),enemy(91,"AIR",false,1100,100));air.act();
        check(number(air.snapshot(),"targetId",-1)==91,"current legal air duty persists without converging onto nearby surface target");
        h.next(air);h.contact(air,enemy(90,"SURFACE",false,100,100));air.act();check(number(air.snapshot(),"targetId",-1)==90,"uncovered local legal ground contact remains eligible");
        h.next(air);h.ground.add(90L);h.contact(air,enemy(90,"SURFACE",false,100,100));h.dispatches.clear();air.act();
        check(air.snapshot().get("targetId")==null&&frontier.equals(air.snapshot().get("routeFrontierTile"))&&h.planner.plans==1,"later actual ground commitment releases AIR target duty while retaining the suspended route");
        check(h.dispatches.stream().anyMatch(d->String.valueOf(d.get("path")).startsWith("/command/move"))&&h.dispatches.stream().noneMatch(d->String.valueOf(d.get("path")).startsWith("/command/attack-move")),"ground duty transfer restores original native AIR search travel");
        h.next(air);h.ground.add(92L);h.contact(air,enemy(90,"SURFACE",false,100,100),enemy(92,"SURFACE",true,1200,100));air.act();
        check(number(air.snapshot(),"targetId",-1)==92,"current native water contact keeps AIR spatial duty instead of using ground coverage as a sea veto");
        check(h.nativeHost.own.stream().noneMatch(a->a.containsKey("movementType")),"contact choice never invents omitted own-state movementType");air.close();
    }
    static void independentOwnerLookahead()throws Exception{
        R10AirFormationsHarness.Host h=new R10AirFormationsHarness.Host(15);AirGeneralController air=h.formation();h.next(air);air.act();h.next(air);air.act();h.next(air);air.act();
        List<Map<String,Object>> before=R10AirFormationsHarness.formations(air);Map<String,Object> child=before.get(1);long leader=((Number)child.get("routeLeader")).longValue();
        long generation=h.nativeHost.arbiter.ownerGeneration(leader);double x=number(child,"searchLegX",0),y=number(child,"searchLegY",0);
        h.actor(leader).put("x",x);h.actor(leader).put("y",y-200);h.next(air);h.dispatches.clear();air.act();
        List<Map<String,Object>> after=R10AirFormationsHarness.formations(air);
        check(h.dispatches.stream().anyMatch(d->"air-general:2".equals(d.get("owner"))),"one owner's later travel can redirect its own leg");
        for(int i:new int[]{0,2})check(before.get(i).get("searchLegX").equals(after.get(i).get("searchLegX"))&&before.get(i).get("searchLegY").equals(after.get(i).get("searchLegY")),"another owner's route/order destination is unaffected by lookahead");
        check(number(after.get(1),"routeCursor",-1)==0&&h.nativeHost.arbiter.ownerGeneration(leader)==generation,"independent lookahead preserves cursor witness and generation");air.close();
    }
    public static void main(String[] args)throws Exception{lookaheadDoesNotInventArrival();rejectedAndUnsafeLookahead();unknownContactRetainsMission();groundDutyAndSpatialPriority();independentOwnerLookahead();System.out.println("R11_AIR_CONTINUITY_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");}
}
