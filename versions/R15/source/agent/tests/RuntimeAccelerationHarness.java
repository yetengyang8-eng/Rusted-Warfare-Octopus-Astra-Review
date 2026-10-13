package io.rwagent.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real loopback transport, deterministic synthetic native clocks. This measures HTTP overlap,
 * not an atomic world, a natural-game speedup or a change in command permission. */
public final class RuntimeAccelerationHarness {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Map<String,Object> map(Object... values){Map<String,Object> out=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
    @SuppressWarnings("unchecked") private static Map<String,Object> payload(RuntimeAcceleration.Read read){return (Map<String,Object>)Json.parse(read.response.body);}
    private static double number(Map<String,Object> values,String key){return ((Number)values.get(key)).doubleValue();}
    private static final class Fixture implements AutoCloseable {
        final HttpServer server;final ExecutorService handlers=Executors.newFixedThreadPool(4);
        final CountDownLatch parallelArrival=new CountDownLatch(2);
        final AtomicInteger requests=new AtomicInteger(),active=new AtomicInteger(),maxParallel=new AtomicInteger();
        Fixture()throws Exception{server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(handlers);
            server.createContext("/",exchange->{try{reply(exchange);}catch(InterruptedException failure){Thread.currentThread().interrupt();exchange.close();}});server.start();}
        int port(){return server.getAddress().getPort();}
        void reply(HttpExchange exchange)throws IOException,InterruptedException{
            String path=exchange.getRequestURI().getPath();int sequence=requests.incrementAndGet();boolean parallel=path.startsWith("/parallel/");
            int current=active.incrementAndGet();if(parallel){maxParallel.accumulateAndGet(current,Math::max);parallelArrival.countDown();parallelArrival.await(2,TimeUnit.SECONDS);}
            try{if(path.startsWith("/serial/")||parallel)Thread.sleep(250);
                boolean second=path.endsWith("/b");long frame=second?29:17,time=second?2900:1700;
                String body="{\"sessionId\":\"native-session\",\"player\":{\"teamId\":2},\"frame\":"+frame+",\"gameTimeMs\":"+time+",\"requestSequence\":"+sequence+",\"requestPath\":\""+exchange.getRequestURI()+"\"}";
                byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(path.equals("/error")?503:200,bytes.length);
                try(OutputStream out=exchange.getResponseBody()){out.write(bytes);}
            }finally{active.decrementAndGet();exchange.close();}
        }
        public void close(){server.stop(0);handlers.shutdownNow();}
    }
    private static void parallelHttpAndIdentity()throws Exception{
        try(Fixture fixture=new Fixture();RuntimeAcceleration runtime=new RuntimeAcceleration(fixture.port())){
            runtime.beginCycle();runtime.fetch("/warm");long serialStarted=System.nanoTime();runtime.fetch("/serial/a");runtime.fetch("/serial/b");double serialMs=(System.nanoTime()-serialStarted)/1000000.0;
            long beginWall=System.currentTimeMillis(),parallelStarted=System.nanoTime();runtime.prefetch("/parallel/a","/parallel/b");runtime.prefetch("/parallel/a");
            RuntimeAcceleration.Read first=runtime.fetch("/parallel/a"),second=runtime.fetch("/parallel/b");double parallelMs=(System.nanoTime()-parallelStarted)/1000000.0;
            check(fixture.parallelArrival.getCount()==0&&fixture.maxParallel.get()>=2,"independent GETs overlap on actual loopback server workers");
            check(parallelMs<serialMs*.85+50,"two delayed parallel responses have lower wall latency than serial responses");
            check(first.response.status==200&&second.response.status==200,"both actual HTTP responses preserved");
            check(first.requestedWallMs>=beginWall&&second.requestedWallMs>=beginWall,"workers retain their original request start wall time");
            check(first.receivedWallMs>=first.requestedWallMs&&second.receivedWallMs>=second.requestedWallMs,"each response retains its own wall interval");
            check(first.elapsedNanos>0&&second.elapsedNanos>0,"response durations use original monotonic request timing");
            Map<String,Object> a=payload(first),b=payload(second);GameClock clock=new GameClock("acceleration-fixture");
            GameClock.Observation oa=clock.observe("/parallel/a",a,first.requestedWallMs,first.receivedWallMs,1000L);
            GameClock.Observation ob=clock.observe("/parallel/b",b,second.requestedWallMs,second.receivedWallMs,1000L);
            check(oa.sourceFrame==17&&ob.sourceFrame==29&&oa.sourceGameTimeMs==1700&&ob.sourceGameTimeMs==2900,"parallel endpoints retain different original native frames and clocks");
            check(oa.requestedWallTimeMs==first.requestedWallMs&&ob.receivedWallTimeMs==second.receivedWallMs,"GameClock receives transport wall stamps unchanged");
            check("native-session".equals(oa.sourceSessionId)&&"team:2".equals(oa.sourcePlayerId),"native session and player identity are preserved");
            check(!oa.id.equals(ob.id)&&Boolean.FALSE.equals(ob.metadata().get("atomicWithOtherEndpoints")),"independent observation IDs do not imply an atomic parallel snapshot");
            RuntimeAcceleration.Read error=runtime.fetch("/error");check(error.response.status==503&&payload(error).get("sessionId").equals("native-session"),"HTTP error response keeps actual status and native payload");
            Map<String,Object> metrics=runtime.metrics(0,0);check(number(metrics,"observationReads")==6&&number(metrics,"parallelFetches")==2,"fetch reads counted once; duplicate pending prefetch does not create a second HTTP request");
            check(fixture.requests.get()==6,"parallel transport makes exactly the intended GET requests");
            check(number(metrics,"observationLatencyWallMs")>0&&Boolean.FALSE.equals(metrics.get("atomicSnapshot")),"latency is reported without atomic snapshot claim");
            System.out.println("RuntimeAccelerationHarness HTTP serialMs="+serialMs+" parallelMs="+parallelMs);
        }
    }
    private static void cycleCache()throws Exception{
        try(Fixture fixture=new Fixture();RuntimeAcceleration runtime=new RuntimeAcceleration(fixture.port())){
            GameClock clock=new GameClock("cache-fixture");runtime.beginCycle();String path="/combat/production";
            RuntimeAcceleration.Read read=runtime.fetch(path);Map<String,Object> value=payload(read);GameClock.Observation observation=clock.observe(path,value,read.requestedWallMs,read.receivedWallMs,1700L);
            runtime.remember(path,value,observation);int before=fixture.requests.get();RuntimeAcceleration.CachedRead cached=runtime.cached(path);
            check(cached!=null&&cached.payload==value&&cached.observation==observation,"same-cycle menu cache preserves payload and exact original Observation object");
            check(fixture.requests.get()==before&&number(runtime.metrics(0,0),"cacheHits")==1,"cache hit is local and does not invent a new HTTP response");
            runtime.remember("/combat/engagement?unitIds=1&targetId=2",value,observation);
            check(runtime.cached("/combat/engagement?unitIds=1&targetId=3")==null,"engagement queries do not share distinct target identities");
            check(runtime.cached("/combat/engagement?unitIds=1&targetId=2")!=null,"exact engagement query can reuse same-cycle evidence");
            runtime.remember("/state",value,observation);runtime.remember("/combat/observe",value,observation);
            check(runtime.cached("/state")==null&&runtime.cached("/combat/observe")==null,"live own and visible-enemy observations never enter menu cache");
            runtime.beginCycle();check(runtime.cached(path)==null&&runtime.cached("/combat/engagement?unitIds=1&targetId=2")==null,"new cycle invalidates all cached menus and engagements");
            RuntimeAcceleration.Read fresh=runtime.fetch(path);Map<String,Object> next=payload(fresh);GameClock.Observation freshObservation=clock.observe(path,next,fresh.requestedWallMs,fresh.receivedWallMs,1700L);
            check(fixture.requests.get()==before+1&&!freshObservation.id.equals(observation.id),"new-cycle read makes an actual request and distinct original observation even at repeated native stamp");
        }
    }
    private static void adaptiveSleepAndDispatch()throws Exception{
        try(RuntimeAcceleration runtime=new RuntimeAcceleration(1)){
            runtime.beginCycle();check(runtime.sleepMs(10000,System.nanoTime())==100,"unmeasured slow loop sleep respects upper100ms bound");
            check(runtime.sleepMs(100,System.nanoTime()-TimeUnit.SECONDS.toNanos(2))==10,"overdue loop preserves lower10ms bound");
            runtime.sample(0);Thread.sleep(35);runtime.sample(90000);double speed=number(runtime.metrics(0,0),"measuredGameSpeed");
            long acceleratedSleep=runtime.sleepMs(500,System.nanoTime());check(speed>=.1&&speed<=30&&acceleratedSleep>=10&&acceleratedSleep<=100,"measured game speed and adaptive sleep stay within configured bounds");
            check(acceleratedSleep<100,"fast game-time sample shortens the next polling sleep");
            runtime.sample(90000);check(number(runtime.metrics(0,0),"measuredGameSpeed")==speed,"unchanged game time cannot fabricate speed progression");
            Thread.sleep(35);runtime.sample(90001);check(number(runtime.metrics(0,0),"measuredGameSpeed")==.1&&runtime.sleepMs(500,System.nanoTime())==100,"slow game-time sample clamps speed at0.1 and sleep at100ms");
            runtime.dispatch(2000000,true);runtime.dispatch(4000000,false);runtime.dispatch(-1,true);runtime.decision(1000);runtime.decision(1500);runtime.decision(1500);
            Map<String,Object> metrics=runtime.metrics(2,1000);
            check(number(metrics,"commandsThisObservation")==2&&number(metrics,"maxCommandsPerObservation")==2,"only accepted dispatches count as commands in the observation");
            check(number(metrics,"dispatchLatencyWallMs")==2,"all actual dispatch attempts contribute latency; negative duration cannot subtract time");
            check(number(metrics,"effectiveDecisionIntervalGameMs")==500&&number(metrics,"decisions")==3,"decision cadence uses positive source game-time intervals without duplicate-stamp elapsed invention");
            check(number(metrics,"commandsPerGameMinute")==120,"command rate uses game-time denominator");
            runtime.beginCycle();metrics=runtime.metrics(2,1000);check(number(metrics,"commandsThisObservation")==0&&number(metrics,"maxCommandsPerObservation")==2,"new observation resets current accepted count and retains historical maximum");
        }
    }
    public static void main(String[] args)throws Exception{parallelHttpAndIdentity();cycleCache();adaptiveSleepAndDispatch();System.out.println("RuntimeAccelerationHarness PASS checks="+checks);}
}
