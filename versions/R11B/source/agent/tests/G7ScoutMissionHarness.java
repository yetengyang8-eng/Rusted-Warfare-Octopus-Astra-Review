package io.rwagent.client;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Synthetic lawful own-state/HTTP lifecycle checks, not natural Scout travel or victory proof. */
public final class G7ScoutMissionHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Object get(Object o,String k)throws Exception{return G53ContinuingOperationsHarness.get(o,k);}
    static void set(Object o,String k,Object v)throws Exception{G53ContinuingOperationsHarness.set(o,k,v);}
    static Object call(Object o,String k,Class<?>[] t,Object... a)throws Exception{return G53ContinuingOperationsHarness.call(o,k,t,a);}
    static void update(BattleClient b,Map<String,Object> state)throws Exception{
        call(b,"updateRecon",new Class<?>[]{Map.class,Map.class},state,map("enemyIntel",Collections.emptyList()));
    }
    static void lifecycle()throws Exception{
        System.setProperty("rwagent.g3Execution","true");System.setProperty("rwagent.g5","true");
        System.setProperty("rwagent.runtimeAdaptive","false");System.setProperty("rwagent.globalStrategy","false");
        System.setProperty("rwagent.g1Trace","false");
        try(G53ContinuingOperationsHarness.Fixture f=new G53ContinuingOperationsHarness.Fixture()){
            Map<String,Object> actor=f.own.get(1);actor.put("type","scout");actor.put("mobile",true);actor.put("x",110.0);actor.put("y",110.0);
            Map<String,Object> state=f.sample(1);state.put("ownUnits",f.own);state.put("map",map("tilesWide",110,"tilesHigh",110,"tileWidth",20,"tileHeight",20));
            f.arbiter.release("force:service-test");
            state.put("ownUnits",Arrays.asList(f.own.get(0),f.own.get(2)));
            call(f.battle,"observeG4Forces",new Class<?>[]{Map.class,Map.class},state,map("visibleEnemies",Collections.emptyList()));
            state.put("ownUnits",f.own);
            call(f.battle,"observeG4Forces",new Class<?>[]{Map.class,Map.class},state,map("visibleEnemies",Collections.emptyList()));
            check(((Number)get(f.battle,"openingScoutId")).longValue()==10L,"runtime identifies Scout from its first actual ready own observation");
            check(f.arbiter.owns("recon:0",10),"runtime admission reserves independent opening Scout before Commander");
            check(Boolean.TRUE.equals(call(f.battle,"claimRecon",new Class<?>[]{long.class,long.class},1L,10L)),"independent mission owns the actual own Scout");
            long generation=f.arbiter.ownerGeneration(10);
            final boolean[] available={true};final int[] planned={0};
            HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange->{
                Map<String,Object> response=map("status","queued","sessionId","s","frame",f.frame,"requestId","fixture-move","unitId",10L);
                if(exchange.getRequestURI().getPath().equals("/state"))response=f.state();
                else if(exchange.getRequestURI().getPath().equals("/scout/plan")){
                    planned[0]++;
                    if(!available[0])response=map("status","no_frontier","sessionId","s");
                    else {
                        int col=(int)(BattleClient.n(actor,"x")/20),row=(int)(BattleClient.n(actor,"y")/20);
                        List<Integer> route=new ArrayList<Integer>();for(int c=col;c<=col+6;c++)route.add(c*110+row);
                        response=map("status","planned","sessionId","s","frame",f.frame,"pathKnown",true,
                            "targetX",(col+6.5)*20,"targetY",(row+.5)*20,"targetTile",route.get(6),"frontierTile",route.get(6),
                            "routeTiles",route,"movementType","HOVER","potentialInfoScore",100,"potentialNeverSeenTiles",25,
                            "potentialDeepFogTiles",0,"potentialStaleFogTiles",0);
                    }
                }
                byte[] bytes=BattleClient.json(response).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
            });server.start();
            try{
                set(f.battle,"port",server.getAddress().getPort());
                check(Boolean.TRUE.equals(call(f.battle,"createFrontierPlan",new Class<?>[]{Map.class,Map.class,long.class},state,actor,1L)),"lawful plan creates initial route");
                Object task=get(f.battle,"reconTask");set(task,"awaitFreshOwnObservation",false);
                set(f.battle,"militaryUrgency","EMERGENCY");
                check(Boolean.TRUE.equals(call(f.battle,"issueReconOrder",new Class<?>[]{Map.class},state)),"broad military emergency leaves dedicated safe exploration active");
                set(task,"everOrderObserved",true);set(task,"everExecutionObserved",true);set(task,"executionObserved",true);
                set(task,"progressEvents",1);set(task,"firstProgressAt",250L);actor.put("x",150.0);
                set(f.battle,"lastScout",map("rememberedThreats",Collections.emptyList(),"region",map("tile",get(task,"frontierTile"),
                    "revealedDeepOrNeverSince",1,"revealedStaleSince",0,"latestRelevantRevealGameTimeMs",500L)));
                set(f.battle,"time",1000L);update(f.battle,state);
                check(get(f.battle,"reconTask")==task,"one newly seen tile records progress without ending the mission or route");
                check(Boolean.TRUE.equals(get(task,"frontierRevealRecorded")),"partial vision is retained as legal information evidence");
                check(f.arbiter.ownerGeneration(10)==generation&&f.arbiter.owns("recon:1",10),"partial reveal preserves exact actor lease and generation");
                actor.put("x",230.0);set(task,"routeCursor",6);set(task,"waypointArrivedAt",1000L);set(f.battle,"time",2000L);
                update(f.battle,state);Object renewed=get(f.battle,"reconTask");
                check(renewed!=task&&((Number)get(renewed,"taskId")).longValue()==1,"observed leg arrival renews route in same continuing mission");
                check(f.arbiter.ownerGeneration(10)==generation&&f.arbiter.owns("recon:1",10),"route renewal never releases/reclaims or changes generation");
                check(((Number)get(f.battle,"reconRecallUnitId")).longValue()<0,"route completion does not recall Scout home");
                set(f.battle,"lastScout",map("rememberedThreats",Arrays.asList(map("x",400.0,"y",110.0,"range",100.0))));
                check(Boolean.FALSE.equals(call(f.battle,"safeFrontierSegment",new Class<?>[]{Map.class,double.class,double.class},actor,350.0,110.0)),"known danger rejects inward corridor");
                check(Boolean.TRUE.equals(call(f.battle,"safeFrontierSegment",new Class<?>[]{Map.class,double.class,double.class},actor,130.0,110.0)),"Scout already inside caution margin can move strictly away");
                check(Boolean.FALSE.equals(call(f.battle,"safeFrontierSegment",new Class<?>[]{Map.class,double.class,double.class},actor,230.0,230.0)),"tangent motion inside danger does not pretend to be escape");
                available[0]=false;Class<?> taskType=renewed.getClass();
                call(f.battle,"renewFrontier",new Class<?>[]{Map.class,taskType,String.class},state,renewed,"NEW_KNOWN_THREAT_CORRIDOR");
                check(get(f.battle,"reconTask")==renewed&&Boolean.TRUE.equals(get(renewed,"frontierNeedsPlan")),"temporarily unavailable detour remains a continuing owned mission");
                check(f.arbiter.ownerGeneration(10)==generation&&f.arbiter.owns("recon:1",10),"unavailable route does not release or return Scout to army");
                int before=planned[0];update(f.battle,state);check(planned[0]==before,"failed planning uses existing delay instead of rapid retry frequency");
                available[0]=true;set(f.battle,"time",20000L);update(f.battle,state);
                check(get(f.battle,"reconTask")!=renewed&&f.arbiter.ownerGeneration(10)==generation,"later legal route resumes using the retained actor lease");
                actor.put("hp",95.0);actor.put("maxHp",350.0);actor.put("x",500.0);set(f.battle,"time",23000L);
                f.frame=2;f.now=23000L;state.put("frame",f.frame);state.put("gameTimeMs",f.now);
                if(get(f.battle,"runtime")!=null)((RuntimeAcceleration)get(f.battle,"runtime")).beginCycle();call(f.battle,"observe",new Class<?>[0]);
                call(f.battle,"beginExecutionObservation",new Class<?>[]{Map.class},state);
                update(f.battle,state);
                check(get(f.battle,"reconTask")==null,"severely injured dedicated Scout still leaves exploration to recover");
                check(!((Set<?>)get(f.battle,"expendableScouts")).contains(10L),"dedicated Scout never enters the damaged tank recycling pool");
                check(Boolean.TRUE.equals(call(f.battle,"issueReconRecall",new Class<?>[]{Map.class},state)),"injured Scout receives normal lawful return order");
                check("SCOUT_DAMAGED".equals(get(f.battle,"reconRecallReason")),"recall records actual damage cause rather than inventing defense preemption");
                actor.put("x",110.0);call(f.battle,"issueReconRecall",new Class<?>[]{Map.class},state);
                Map<String,Object> lost=map("enemyIntel",Arrays.asList(map("id",777L,"status","LOST_CONTACT","lastKnownBuilding",true,
                    "lastKnownSiteVisible",false,"lastKnownType","commandCenter","lastSeenGameTimeMs",22000L,"lastKnownX",800.0,"lastKnownY",800.0)));
                set(f.battle,"time",45000L);call(f.battle,"updateRecon",new Class<?>[]{Map.class,Map.class},state,lost);
                Object recheck=get(f.battle,"reconTask");
                check(recheck!=null&&"RECHECK_INTEL".equals(get(recheck,"kind")),"recheck remains available while Scout has not recovered");
                check(((Number)get(recheck,"unitId")).longValue()<0,"damaged Scout is not reassigned through expendable fallback");
                f.own.add(G53ContinuingOperationsHarness.Fixture.unit(20,"heavyTank",700,false,true));
                set(f.battle,"reconRecallUnitId",20L);actor.put("hp",350.0);set(f.battle,"time",65000L);
                set(f.battle,"lastScout",map("rememberedThreats",Collections.emptyList()));
                f.frame=3;f.now=65000L;state.put("frame",f.frame);state.put("gameTimeMs",f.now);
                if(get(f.battle,"runtime")!=null)((RuntimeAcceleration)get(f.battle,"runtime")).beginCycle();call(f.battle,"observe",new Class<?>[0]);
                call(f.battle,"beginExecutionObservation",new Class<?>[]{Map.class},state);
                call(f.battle,"observeG4Forces",new Class<?>[]{Map.class,Map.class},state,map("visibleEnemies",Collections.emptyList()));
                check(f.arbiter.owns("recon:0",10),"actual recovered own observation restores dedicated Scout reserve");
                call(f.battle,"updateRecon",new Class<?>[]{Map.class,Map.class},state,lost);
                Object resumed=get(f.battle,"reconTask");
                check(resumed!=null&&"FRONTIER_SWEEP".equals(get(resumed,"kind"))&&((Number)get(resumed,"preferredUnitId")).longValue()==10,
                    "recovered Scout resumes frontier despite waiting combat recheck and another actor recall");
                check(!((Map<?,?>)get(f.battle,"reconClosedObservations")).containsKey(777L),"yielding to permanent exploration does not fabricate old-site resolution");
                check(((Number)get(f.battle,"reconRecallUnitId")).longValue()==20,"independent borrowed actor recall is preserved");
                update(f.battle,state);
                check(Boolean.TRUE.equals(call(f.battle,"issueReconOrder",new Class<?>[]{Map.class},state)),"restored frontier actually issues a legal Scout move");
                check(f.arbiter.owns("recon:"+get(resumed,"taskId"),10),"restored frontier owns the identified Scout through a real generation transfer");
            }finally{server.stop(0);}
        }
    }
    public static void main(String[] args)throws Exception{lifecycle();System.out.println("G7_SCOUT_MISSION_OK checks="+checks+" fixtureOnly=true");}
}

