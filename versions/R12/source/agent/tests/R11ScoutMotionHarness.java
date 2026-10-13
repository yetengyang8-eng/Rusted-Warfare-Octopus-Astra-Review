package io.rwagent.client;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Lawful synthetic own samples: lookahead, repeated danger redirects, independent pending recheck. */
public final class R11ScoutMotionHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Object get(Object o,String k)throws Exception{return G53ContinuingOperationsHarness.get(o,k);}
    static void set(Object o,String k,Object v)throws Exception{G53ContinuingOperationsHarness.set(o,k,v);}
    static Object call(Object o,String k,Class<?>[] t,Object... a)throws Exception{return G53ContinuingOperationsHarness.call(o,k,t,a);}
    static final class Fixture implements AutoCloseable {
        final G53ContinuingOperationsHarness.Fixture f=new G53ContinuingOperationsHarness.Fixture();
        final Map<String,Object> actor=f.own.get(1);final HttpServer server;
        final List<String> posts=new ArrayList<String>();int direction=1,steps=40,corner=-1;boolean available=true;
        Map<String,Object> state;
        Fixture()throws Exception{
            actor.putAll(map("type","scout","mobile",true,"x",810.,"y",810.));
            for(long id=20;id<28;id++){Map<String,Object> u=G53ContinuingOperationsHarness.Fixture.unit(id,"tank",400,false,true);u.put("mobile",true);f.own.add(u);}
            state=f.sample(1);f.arbiter.release("force:service-test");set(f.battle,"openingScoutId",10L);set(f.battle,"reconTaskCounter",1L);
            call(f.battle,"claimRecon",new Class<?>[]{long.class,long.class},1L,10L);
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",e->{
                Map<String,Object> response=map("status","queued","sessionId","s","frame",f.frame,"requestId","r11-"+posts.size(),"unitId",10L);
                if(e.getRequestURI().getRawQuery()!=null)for(String q:e.getRequestURI().getRawQuery().split("&"))
                    if(q.startsWith("unitId="))response.put("unitId",Long.valueOf(q.substring(7)));
                if("POST".equals(e.getRequestMethod()))posts.add(e.getRequestURI().toString());
                if("/state".equals(e.getRequestURI().getPath()))response=f.state();
                if("/scout/plan".equals(e.getRequestURI().getPath())){
                    int c=(int)(BattleClient.n(actor,"x")/20),r=(int)(BattleClient.n(actor,"y")/20);List<Integer> route=new ArrayList<Integer>();
                    route.add(c*110+r);
                    for(int i=1;i<=steps;i++)route.add(corner>=0&&i>corner?(c+corner)*110+r+i-corner:(c+i*direction)*110+r);
                    int end=route.get(route.size()-1);
                    response=map("status","planned","sessionId","s","frame",f.frame,"gameTimeMs",f.now,"anchorUnitId",10L,"anchorX",actor.get("x"),"anchorY",actor.get("y"),
                        "routeScope","scout-search","routeComplete",true,"movementType","HOVER","routeTiles",route,"targetTile",end,"frontierTile",end,
                        "targetX",(end/110+.5)*20,"targetY",(end%110+.5)*20,"potentialInfoScore",100,"potentialNeverSeenTiles",25,"potentialDeepFogTiles",0,"potentialStaleFogTiles",0);
                    if(!available)response=map("status","no_frontier","sessionId","s");
                }
                byte[] bytes=BattleClient.json(response).getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();
            });server.start();set(f.battle,"port",server.getAddress().getPort());
            tick(1,250);set(f.battle,"lastScout",map("rememberedThreats",Collections.emptyList()));
        }
        void tick(int frame,long now)throws Exception{
            f.frame=frame;f.now=now;RuntimeAcceleration runtime=(RuntimeAcceleration)get(f.battle,"runtime");if(runtime!=null)runtime.beginCycle();
            state=(Map<String,Object>)call(f.battle,"observe",new Class<?>[0]);state.put("ownUnits",f.own);
            state.put("map",map("tilesWide",110,"tilesHigh",110,"tileWidth",20,"tileHeight",20));
            call(f.battle,"beginExecutionObservation",new Class<?>[]{Map.class},state);
        }
        Object plan()throws Exception{
            check(Boolean.TRUE.equals(call(f.battle,"createFrontierPlan",new Class<?>[]{Map.class,Map.class,long.class},state,actor,1L)),"complete lawful fixture route admitted");
            Object task=get(f.battle,"reconTask");set(task,"awaitFreshOwnObservation",false);return task;
        }
        boolean issue()throws Exception{return Boolean.TRUE.equals(call(f.battle,"issueReconOrder",new Class<?>[]{Map.class},state));}
        void witness(Object task)throws Exception{
            actor.put("orderType","move");actor.put("orderX",get(task,"waypointX"));actor.put("orderY",get(task,"waypointY"));
            call(f.battle,"observeReconMotion",new Class<?>[]{Map.class},state);
        }
        void update(Map<String,Object> intel)throws Exception{call(f.battle,"updateRecon",new Class<?>[]{Map.class,Map.class},state,map("enemyIntel",Arrays.asList(intel)));}
        public void close()throws Exception{server.stop(0);f.close();}
    }
    static void lookahead()throws Exception{
        try(Fixture x=new Fixture()){
            Object task=x.plan();long generation=x.f.arbiter.ownerGeneration(10);
            check(x.issue(),"first straight segment issued");check(((Number)get(task,"routeWaypointIndex")).intValue()==6,"first lookahead bounded to six route cells");
            check(((Number)get(task,"routeCursor")).intValue()==0,"receipt does not advance explored route");
            MoveExecution move=(MoveExecution)get(task,"move");
            check(move.observe(new CommandArbiter.Stamp("s","0",move.acceptedFrame,250),10,930,810,null,null,null)==MoveExecution.Witness.NONE,"same accepted frame cannot witness arrival");
            check(move.observe(new CommandArbiter.Stamp("foreign-session","0",move.acceptedFrame+1,500),10,930,810,null,null,null)==MoveExecution.Witness.NONE,"foreign session cannot witness route arrival");
            x.tick(2,500);x.witness(task);check(!x.issue()&&x.posts.size()==1,"active order without displacement cannot extend route");
            x.actor.put("x",870.);x.tick(3,750);x.witness(task);
            check(((Number)get(task,"routeCursor")).intValue()==3,"own center passage proves only three traversed cells");
            check(((Number)get(task,"waypointArrivedAt")).longValue()<0,"center passage is not old endpoint arrival");
            check(x.issue(),"normal lookahead extends before reaching old endpoint and within old four second gate");
            check(((Number)get(task,"routeWaypointIndex")).intValue()==9&&((Number)get(task,"routeCursor")).intValue()==3,"new target extends six cells from proven cursor without consuming remaining route");
            check(((Number)get(task,"waypointArrivedAt")).longValue()<0,"new receipt has no arrival witness");
            x.actor.put("x",910.);x.tick(4,1000);x.witness(task);check(x.issue(),"continued displacement permits a second straight extension");
            check(x.f.arbiter.ownerGeneration(10)==generation&&x.f.arbiter.owns("recon:1",10),"lookahead never changes Scout owner or generation");
            int cursor=((Number)get(task,"routeCursor")).intValue();x.actor.put("y",850.);x.tick(5,1250);x.witness(task);
            check(((Number)get(task,"routeCursor")).intValue()==cursor,"off-route own position cannot skip unexplored cells");
            x.actor.put("y",810.);x.actor.put("x",950.);x.tick(6,1500);x.witness(task);
            check(((Number)get(task,"routeCursor")).intValue()==7,"actual new displacement makes another command eligible before ownership rejection");
            x.f.arbiter.release("recon:1");check(x.f.arbiter.claim("foreign",10),"foreign generation admitted for rejection fixture");
            int before=x.posts.size();check(!x.issue()&&x.posts.size()==before&&x.f.arbiter.owns("foreign",10),"stale recon ownership cannot issue over a new foreign generation");
        }
    }
    static void cornersAndDanger()throws Exception{
        try(Fixture x=new Fixture()){
            x.corner=2;x.steps=10;Object task=x.plan();check(x.issue(),"corner route first straight issued");
            x.actor.put("x",830.);x.tick(2,500);x.witness(task);check(!x.issue(),"lookahead cannot cut the next unexplored corner");
            x.actor.put("x",850.);x.tick(3,750);x.witness(task);check(x.issue(),"real corner arrival enables next straight immediately");
            check(((Number)get(task,"routeCursor")).intValue()==2&&((Number)get(task,"routeWaypointIndex")).intValue()==8,"corner command preserves actual cursor and bounded known turn route");
        }
        try(Fixture x=new Fixture()){
            Object task=x.plan();check(x.issue(),"initial ordinary route issued");long generation=x.f.arbiter.ownerGeneration(10);
            x.direction=-1;x.steps=20;x.tick(2,500);set(x.f.battle,"lastScout",map("rememberedThreats",Arrays.asList(map("x",1010.,"y",810.,"range",100.))));
            x.issue();Object west=get(x.f.battle,"reconTask");
            check(x.posts.size()==2&&x.posts.get(1).contains("x=690.0"),"new current legal danger immediately replaces young order with outward west route without a hold");
            check(((Number)get(west,"routeCursor")).intValue()==0&&((Number)get(west,"waypointArrivedAt")).longValue()<0,"danger replacement receipt claims no travel or arrival");
            x.direction=1;x.tick(3,750);set(x.f.battle,"lastScout",map("rememberedThreats",Arrays.asList(map("x",610.,"y",810.,"range",100.))));
            x.issue();Object east=get(x.f.battle,"reconTask");
            check(x.posts.size()==3&&x.posts.get(2).contains("x=930.0"),"another current legal danger can redirect again immediately");
            check(x.f.arbiter.ownerGeneration(10)==generation&&((Number)get(east,"taskId")).longValue()==1,"multiple redirects preserve task, owner and generation");
            x.available=false;x.tick(4,1000);set(x.f.battle,"lastScout",map("rememberedThreats",Arrays.asList(map("x",1010.,"y",810.,"range",100.))));x.issue();
            Object waiting=get(x.f.battle,"reconTask");check(Boolean.TRUE.equals(get(waiting,"frontierNeedsPlan")),"no safe route retains owned waiting exploration");
            x.available=true;x.direction=1;x.tick(5,1250);set(x.f.battle,"lastScout",map("rememberedThreats",Arrays.asList(map("x",610.,"y",810.,"range",100.))));
            call(x.f.battle,"updateRecon",new Class<?>[]{Map.class,Map.class},x.state,map("enemyIntel",Collections.emptyList()));
            check(get(x.f.battle,"reconTask")!=waiting&&!Boolean.TRUE.equals(get(get(x.f.battle,"reconTask"),"frontierNeedsPlan")),"actual current hazard change resumes safe route immediately before failed-plan timer");
            check(x.posts.get(x.posts.size()-1).contains("x=930.0")&&x.f.arbiter.ownerGeneration(10)==generation,"hazard-triggered retry executes safe move with preserved generation");
        }
    }
    static void pendingRecheckKeepsMoving()throws Exception{
        try(Fixture x=new Fixture()){
            x.steps=6;Object explorer=x.plan();check(x.issue(),"short actual route starts continuing mission");
            x.actor.put("x",930.);x.tick(2,500);x.witness(explorer);
            Map<String,Object> intel=map("id",777L,"status","LOST_CONTACT","lastKnownBuilding",true,"lastKnownSiteVisible",false,"lastKnownType","commandCenter","lastSeenGameTimeMs",250L,"lastKnownX",1800.,"lastKnownY",1800.);
            check(Boolean.TRUE.equals(call(x.f.battle,"interruptFrontierForRecheck",new Class<?>[]{Map.class,List.class,explorer.getClass()},x.state,Arrays.asList(intel),explorer)),"completed legal route can start independent temporary check");
            Object pending=get(x.f.battle,"reconTask");long generation=x.f.arbiter.ownerGeneration(10);
            x.steps=40;x.tick(3,750);x.update(intel);Object continued=get(x.f.battle,"suspendedFrontier");
            check(continued!=null&&((Number)get(continued,"taskId")).longValue()==1,"waiting recheck replans Explorer in its same long mission");
            x.tick(4,1000);x.update(intel);int before=x.posts.size();x.issue();
            check(x.posts.size()>before&&x.posts.get(before).contains("unitId=10&"),"pending check does not freeze Scout movement waiting twenty seconds");
            check(get(x.f.battle,"reconTask")==pending&&!((Map<?,?>)get(x.f.battle,"reconClosedObservations")).containsKey(777L),"unconfirmed old site remains pending without fabricated clearance");
            continued=get(x.f.battle,"suspendedFrontier");x.actor.put("x",990.);x.tick(5,1250);x.witness(continued);
            check(((Number)get(continued,"routeCursor")).intValue()==3,"between-decision own witness advances independent Explorer during check");
            intel.put("status","CLEARED");x.update(intel);
            check(get(x.f.battle,"reconTask")==continued&&get(x.f.battle,"suspendedFrontier")==null,"actual lawful clearance immediately restores moving Explorer");
            check(get(continued,"move")!=null&&!Boolean.TRUE.equals(get(continued,"frontierNeedsPlan")),"clearance preserves live Explorer route and move witness");
            check(x.f.arbiter.ownerGeneration(10)==generation&&x.f.arbiter.owns("recon:1",10),"independent waiting and resolution preserve Scout generation");
        }
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.g3Execution","true");System.setProperty("rwagent.g5","true");System.setProperty("rwagent.runtimeAdaptive","false");
        System.setProperty("rwagent.globalStrategy","false");System.setProperty("rwagent.g1Trace","false");
        lookahead();cornersAndDanger();pendingRecheckKeepsMoving();
        System.out.println("R11_SCOUT_MOTION_OK checks="+checks+" fixtureOnly=true noNaturalImprovementClaim=true");
    }
}
