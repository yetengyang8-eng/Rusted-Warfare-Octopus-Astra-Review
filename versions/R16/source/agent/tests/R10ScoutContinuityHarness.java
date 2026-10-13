package io.rwagent.client;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Synthetic lawful observations: continuing exploration, temporary recheck, and safe detour ordering. */
public final class R10ScoutContinuityHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Object get(Object o,String k)throws Exception{return G53ContinuingOperationsHarness.get(o,k);}
    static void set(Object o,String k,Object v)throws Exception{G53ContinuingOperationsHarness.set(o,k,v);}
    static Object call(Object o,String k,Class<?>[] types,Object... args)throws Exception{return G53ContinuingOperationsHarness.call(o,k,types,args);}
    static void update(BattleClient b,Map<String,Object> state,Map<String,Object> enemies)throws Exception{
        call(b,"updateRecon",new Class<?>[]{Map.class,Map.class},state,enemies);
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.g3Execution","true");System.setProperty("rwagent.g5","true");
        System.setProperty("rwagent.runtimeAdaptive","false");System.setProperty("rwagent.globalStrategy","false");
        System.setProperty("rwagent.g1Trace","false");
        try(G53ContinuingOperationsHarness.Fixture f=new G53ContinuingOperationsHarness.Fixture()){
            Map<String,Object> actor=f.own.get(1);actor.put("type","scout");actor.put("mobile",true);actor.put("x",110.);actor.put("y",110.);
            for(long id=20;id<28;id++){Map<String,Object> ready=G53ContinuingOperationsHarness.Fixture.unit(id,"tank",400,false,true);ready.put("mobile",true);f.own.add(ready);}
            Map<String,Object> state=f.sample(1);state.put("ownUnits",f.own);state.put("map",map("tilesWide",110,"tilesHigh",110,"tileWidth",20,"tileHeight",20));
            f.arbiter.release("force:service-test");set(f.battle,"openingScoutId",10L);set(f.battle,"reconTaskCounter",1L);
            check(Boolean.TRUE.equals(call(f.battle,"claimRecon",new Class<?>[]{long.class,long.class},1L,10L)),"persistent scout lease established");
            final List<String> posts=new ArrayList<String>(),queries=new ArrayList<String>();final boolean[] detour={false},available={true};
            HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange->{
                String path=exchange.getRequestURI().getPath();Map<String,Object> response=map("status","queued","sessionId","s","frame",f.frame,"requestId","r10-"+posts.size(),"unitId",10L);
                if("POST".equals(exchange.getRequestMethod()))posts.add(exchange.getRequestURI().toString());
                if("/state".equals(path))response=f.state();
                if("/scout/plan".equals(path)){
                    queries.add(exchange.getRequestURI().toString());response=map("status","no_frontier","sessionId","s");
                    if(available[0]){
                        int c=(int)(BattleClient.n(actor,"x")/20),r=(int)(BattleClient.n(actor,"y")/20);List<Integer> route=new ArrayList<Integer>();
                        int steps=detour[0]?6:80;for(int j=0;j<=steps;j++)route.add((c+(detour[0]?-j:j))*110+r);
                        response=map("status","planned","sessionId","s","routeScope","scout-search","routeComplete",true,"movementType",exchange.getRequestURI().toString().contains("unitId=20")?"LAND":"HOVER",
                            "targetX",(c+(detour[0]?-steps:steps)+.5)*20,"targetY",(r+.5)*20,"targetTile",route.get(steps),"frontierTile",route.get(steps),
                            "routeTiles",route,"potentialInfoScore",100,"potentialNeverSeenTiles",25,"potentialDeepFogTiles",0,"potentialStaleFogTiles",0);
                    }
                }
                byte[] bytes=BattleClient.json(response).getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
            });server.start();
            try{
                set(f.battle,"port",server.getAddress().getPort());
                check(Boolean.TRUE.equals(call(f.battle,"createFrontierPlan",new Class<?>[]{Map.class,Map.class,long.class},state,actor,1L)),"full lawful route beyond old 64-tile cutoff admitted");
                check(queries.get(0).contains("routeScope=scout-search"),"Scout explicitly requests complete durable route scope");
                Object task=get(f.battle,"reconTask");set(task,"awaitFreshOwnObservation",false);
                check(Boolean.TRUE.equals(call(f.battle,"issueReconOrder",new Class<?>[]{Map.class},state)),"first bounded straight segment executes");
                check(((Number)get(task,"routeCursor")).intValue()==0,"queued receipt never advances route cursor or claims arrival");
                set(task,"everOrderObserved",true);set(task,"everExecutionObserved",true);set(task,"progressEvents",1);set(task,"firstProgressAt",250L);
                set(f.battle,"lastScout",map("rememberedThreats",Collections.emptyList(),"region",map("tile",get(task,"frontierTile"),"revealedDeepOrNeverSince",1,"revealedStaleSince",0,"latestRelevantRevealGameTimeMs",500L)));
                set(f.battle,"time",1000L);update(f.battle,state,map("enemyIntel",Collections.emptyList()));
                check(get(f.battle,"reconTask")==task&&Boolean.TRUE.equals(get(task,"frontierRevealRecorded")),"partial vision is progress while long route remains active");
                // A temporary recheck begins only between truly completed routes; no ghost contact becomes an attack.
                Map<String,Object> intel=map("id",777L,"status","LOST_CONTACT","lastKnownBuilding",true,"lastKnownSiteVisible",false,"lastKnownType","commandCenter","lastSeenGameTimeMs",500L,"lastKnownX",1800.,"lastKnownY",800.);
                long generation=f.arbiter.ownerGeneration(10);
                check(Boolean.FALSE.equals(call(f.battle,"interruptFrontierForRecheck",new Class<?>[]{Map.class,List.class,task.getClass()},state,Arrays.asList(intel),task)),"eligible intel cannot interrupt an unfinished route or receipt-only travel");
                actor.put("x",1710.);set(task,"unitId",10L);set(task,"executionObserved",true);set(task,"routeCursor",80);
                set(task,"waypointX",1710.);set(task,"waypointY",110.);set(task,"waypointArrivedAt",1000L);
                check(Boolean.TRUE.equals(call(f.battle,"interruptFrontierForRecheck",new Class<?>[]{Map.class,List.class,task.getClass()},state,Arrays.asList(intel),task)),"serviceable old-site recheck can temporarily borrow lane");
                Object recheck=get(f.battle,"reconTask");check(get(f.battle,"suspendedFrontier")==task&&f.arbiter.owns("recon:1",10),"temporary recheck preserves exploration record and actor lease");
                intel.put("status","CLEARED");set(f.battle,"time",2000L);update(f.battle,state,map("enemyIntel",Arrays.asList(intel)));
                check(get(f.battle,"reconTask")==task&&get(f.battle,"suspendedFrontier")==null,"legal old-site resolution resumes original exploration task");
                check(f.arbiter.ownerGeneration(10)==generation&&f.arbiter.owns("recon:1",10),"recheck completion does not release or change scout generation");
                check(((Map<?,?>)get(f.battle,"reconClosedObservations")).containsKey(777L),"closed old legal observation cannot repeatedly interrupt exploration");
                check(((Number)get(recheck,"taskId")).longValue()!=1L,"recheck has distinct identity");
                actor.put("x",110.);set(f.battle,"time",3000L);update(f.battle,state,map("enemyIntel",Arrays.asList(intel)));
                Object resumed=get(f.battle,"reconTask");check(((Number)get(resumed,"taskId")).longValue()==1L,"replanning after recheck retains continuing mission identity");
                // Current lawful danger permits a safe westward detour, without a prior stop at an exposed anchor.
                actor.put("x",230.);detour[0]=true;set(f.battle,"time",5000L);f.frame=2;f.now=5000L;
                call(f.battle,"observe",new Class<?>[0]);state.put("frame",2);state.put("gameTimeMs",5000L);
                call(f.battle,"beginExecutionObservation",new Class<?>[]{Map.class},state);
                set(f.battle,"lastScout",map("rememberedThreats",Arrays.asList(map("x",400.,"y",110.,"range",100.))));
                set(resumed,"unitId",10L);int before=posts.size();
                call(f.battle,"renewFrontier",new Class<?>[]{Map.class,resumed.getClass(),String.class},state,resumed,"NEW_KNOWN_THREAT_CORRIDOR");
                check(posts.size()==before+1&&posts.get(before).contains("x=110.0"),"available lawful escape replaces danger directly with a move, no exposed hold first");
                Object safe=get(f.battle,"reconTask");check(((Number)get(safe,"routeCursor")).intValue()==0,"detour receipt still provides no arrival proof");
                available[0]=false;set(f.battle,"time",9000L);f.frame=3;f.now=9000L;
                call(f.battle,"observe",new Class<?>[0]);state.put("frame",3);state.put("gameTimeMs",9000L);
                call(f.battle,"beginExecutionObservation",new Class<?>[]{Map.class},state);
                call(f.battle,"renewFrontier",new Class<?>[]{Map.class,safe.getClass(),String.class},state,safe,"NEW_KNOWN_THREAT_CORRIDOR");
                check(Boolean.TRUE.equals(get(safe,"frontierNeedsPlan"))&&f.arbiter.owns("recon:1",10),"no safe route holds continuing lease for lawful retry");
                check(f.arbiter.ownerGeneration(10)==generation,"all exploration renewals preserve actor generation");
                call(f.battle,"releaseOwnership",new Class<?>[]{long.class,String.class},1L,"FIXTURE_SWITCH_TO_BORROWED_EXPLORER");set(f.battle,"reconTask",null);
                Map<String,Object> borrowed=f.own.get(3);borrowed.put("x",110.);borrowed.put("y",110.);borrowed.put("hp",400.);actor.put("x",110.);
                available[0]=true;detour[0]=false;set(f.battle,"lastScout",map("rememberedThreats",Collections.emptyList()));
                check(Boolean.TRUE.equals(call(f.battle,"claimRecon",new Class<?>[]{long.class,long.class},5L,20L)),"borrowed explorer has independent lease");
                check(Boolean.TRUE.equals(call(f.battle,"createFrontierPlan",new Class<?>[]{Map.class,Map.class,long.class},state,borrowed,5L)),"borrowed explorer receives durable route");
                Object borrowedTask=get(f.battle,"reconTask");set(borrowedTask,"unitId",20L);set(borrowedTask,"everOrderObserved",true);
                set(borrowedTask,"progressEvents",1);set(borrowedTask,"firstProgressAt",9000L);
                set(f.battle,"lastScout",map("rememberedThreats",Collections.emptyList(),"region",map("tile",get(borrowedTask,"frontierTile"),"revealedDeepOrNeverSince",1,"revealedStaleSince",0,"latestRelevantRevealGameTimeMs",10000L)));
                set(f.battle,"time",11000L);update(f.battle,state,map("enemyIntel",Collections.emptyList()));
                check(get(f.battle,"reconTask")==borrowedTask&&f.arbiter.owns("recon:5",20),"ordinary explorer partial reveal also preserves task and lease instead of yielding repeatedly to recheck");
                set(f.battle,"militaryUrgency","EMERGENCY");update(f.battle,state,map("enemyIntel",Collections.emptyList()));
                check(get(f.battle,"reconTask")!=borrowedTask&&!f.arbiter.owns("recon:5",20),"borrowed explorer still returns to actual defense preemption");
            }finally{server.stop(0);}
        }
        System.out.println("R10_SCOUT_CONTINUITY_OK checks="+checks+" fixtureOnly=true");
    }
}
