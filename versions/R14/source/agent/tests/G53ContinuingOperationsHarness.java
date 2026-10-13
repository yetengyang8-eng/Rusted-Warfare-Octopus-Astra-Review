package io.rwagent.client;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** SYNTHETIC HTTP caller/scheduler service contract, no original engine or natural build claim.
 * Instantaneous producer completion is fixture input, deliberately not a queue-effect witness. */
public final class G53ContinuingOperationsHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object get(Object target,String name)throws Exception{return field(target.getClass(),name).get(target);}
    static void set(Object target,String name,Object value)throws Exception{field(target.getClass(),name).set(target,value);}
    static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);try{return m.invoke(target,args);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception)throw (Exception)t;throw new RuntimeException(t);}}
    static Map<String,Object> map(Object... args){Map<String,Object> out=new LinkedHashMap<String,Object>();for(int i=0;i<args.length;i+=2)out.put((String)args[i],args[i+1]);return out;}
    @SuppressWarnings("unchecked") static Map<String,Object> packet(Object raw){return (Map<String,Object>)raw;}
    static final class Fixture implements AutoCloseable {
        final ServerSocket server;final Thread serverThread;final BattleClient battle;final CommandArbiter arbiter;final ExecutionScheduler scheduler;
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();final List<String> posts=Collections.synchronizedList(new ArrayList<String>());final List<Integer> postFrames=Collections.synchronizedList(new ArrayList<Integer>());
        final StringWriter log=new StringWriter();volatile int frame;volatile long now;boolean production=true;
        Fixture()throws Exception{
            own.add(unit(1,"builder",100,false,false));own.add(unit(10,"tank",100,false,true));own.add(unit(100,"landFactory",100,true,false));
            server=new ServerSocket(0,20,InetAddress.getByName("127.0.0.1"));serverThread=new Thread(()->{while(!server.isClosed())try{handle(server.accept());}catch(Exception failure){if(!server.isClosed())throw new RuntimeException(failure);}},"g53-http-fixture");serverThread.setDaemon(true);serverThread.start();
            System.setProperty("rwagent.port",String.valueOf(server.getLocalPort()));battle=new BattleClient();arbiter=(CommandArbiter)get(battle,"execution");scheduler=(ExecutionScheduler)get(battle,"scheduler");
            set(battle,"session","s");set(battle,"log",new BufferedWriter(log));set(battle,"homeX",100.0);set(battle,"homeY",100.0);
            Class<?> job=Class.forName("io.rwagent.client.BattleClient$BuildJob");Constructor<?> c=job.getDeclaredConstructor(String.class,double.class,double.class,long.class,long.class,String.class);c.setAccessible(true);
            set(battle,"buildJob",c.newInstance("extractor",2500.0,100.0,700L,0L,"SYNTHETIC_CURRENT_OWN_CONSTRUCTION_MOVE"));
        }
        static Map<String,Object> unit(long id,String type,double x,boolean building,boolean attack){return map("id",id,"type",type,"x",x,"y",100.0,"hp",1000.0,"maxHp",1000.0,"dead",false,"buildProgress",1.0,"building",building,"canAttack",attack,"productionQueue",0,"orderType",null);}
        Map<String,Object> state(){return map("status","running","sessionId","s","frame",frame,"gameTimeMs",now,"player",map("teamId",0,"credits",100000.0),"ownUnits",own);}
        void handle(Socket socket)throws IOException{
            try(Socket x=socket){BufferedReader input=new BufferedReader(new InputStreamReader(x.getInputStream(),StandardCharsets.UTF_8));String request=input.readLine();if(request==null)return;
                String[] parts=request.split(" ");String method=parts[0],requestPath=parts[1],path=requestPath.split("\\?",2)[0];int contentLength=0;String header;
                while((header=input.readLine())!=null&&!header.isEmpty())if(header.toLowerCase(Locale.ROOT).startsWith("content-length:"))contentLength=Integer.parseInt(header.substring(header.indexOf(':')+1).trim());
                for(int i=0;i<contentLength;i++)input.read();Map<String,Object> body;
                if("POST".equals(method)){posts.add(requestPath);postFrames.add(frame);body=map("sessionId","s","frame",frame,"gameTimeMs",now,"requestId","fixture:"+posts.size(),"status","queued");}
                else if("/state".equals(path))body=state();
                else if("/combat/production".equals(path))body=map("sessionId","s","frame",frame,"gameTimeMs",now,"player",map("teamId",0),"factories",production?Collections.singletonList(map("id",100L,"type","landFactory","queue",0,"tier",1,"techLevel",1,"actions",Collections.singletonList(map("type","tank","actionId","produceTank","cost",350,"affordable",true)))):Collections.emptyList());
                else body=map("sessionId","s","frame",frame,"gameTimeMs",now,"status","UNKNOWN");
                byte[] bytes=BattleClient.json(body).getBytes(StandardCharsets.UTF_8);OutputStream out=x.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));out.write(bytes);out.flush();
            }catch(Exception failure){throw new IOException(failure);}
        }
        Map<String,Object> sample(int next)throws Exception{frame=next;now=next*250L;RuntimeAcceleration runtime=(RuntimeAcceleration)get(battle,"runtime");if(runtime!=null)runtime.beginCycle();
            Map<String,Object> state=packet(call(battle,"observe",new Class<?>[0]));set(battle,"lastState",state);set(battle,"lastEnemies",map("sessionId","s","gameTimeMs",now,"frame",frame,"visibleEnemies",Collections.emptyList()));
            // Keep production runnable in every round; this does not claim natural queue completion.
            ((Map<?,?>)get(battle,"pending")).clear();call(battle,"beginExecutionObservation",new Class<?>[]{Map.class},state);
            check(arbiter.claim("force:service-test",10L),"fixture force actor has one real owner");return state;
        }
        void force(int priority)throws Exception{call(battle,"collectForceProposal",new Class<?>[]{ForceController.Proposal.class},
            new ForceController.Proposal("force:service-test",Collections.singletonList(10L),"/command/move?unitId=10&x=700&y=100","CONTINUING_FORCE",priority,"SYNTHETIC_CURRENT_OWN_FORCE",null));}
        void service(Map<String,Object> state,boolean heavy)throws Exception{call(battle,"serviceContinuingOperations",new Class<?>[]{Map.class,boolean.class},state,heavy);}
        String last(){return posts.get(posts.size()-1);}
        public void close()throws Exception{server.close();serverThread.join(1000);RuntimeAcceleration runtime=(RuntimeAcceleration)get(battle,"runtime");if(runtime!=null)runtime.close();((BufferedWriter)get(battle,"log")).flush();}
    }
    static void oneTokenHeavyDebt()throws Exception{
        try(Fixture f=new Fixture()){
            check(((Number)get(f.battle,"mobileUnitHardCap")).intValue()==Integer.MAX_VALUE,"default G5 cap does not depend on enabled globalStrategy");
            List<String> sequence=new ArrayList<String>();int constructions=0,production=0,force=0;
            for(int round=1;round<=17;round++){
                Map<String,Object> state=f.sample(round);((List<?>)get(f.battle,"forceProposals")).clear();f.force(40);int before=f.posts.size();boolean heavy=(round-1)%4==0;
                f.service(state,heavy);check(f.posts.size()-before==(heavy?3:2),"each independent construction, production and force actor gets same-observation HTTP service");
                String path=f.last();String lane=path.contains("unitId=1&")?"C":path.contains("unitId=100&")?"P":"F";sequence.add(lane);
                if("C".equals(lane))constructions++;else if("P".equals(lane))production++;else force++;
                if(heavy){check(f.posts.get(before).contains("unitId=1&"),"construction really executes on heavy opportunity");constructions++;}
                production++;
            }
            check(constructions==5&&production==17&&force==17,"continuing controllers all get service without a global quota");
            for(int round=1;round<=17;round++){int count=0;for(Integer at:f.postFrames)if(at==round)count++;check(count==((round-1)%4==0?3:2),"actual HTTP independent actors execute together");}
            System.out.println("G53_CONTINUING_SEQUENCE "+sequence);
        }
    }
    static void criticalFirstAndCommittedMoney()throws Exception{
        try(Fixture f=new Fixture()){
            Map<String,Object> state=f.sample(1);f.force(90);call(f.battle,"dispatchG4Forces",new Class<?>[]{int.class},70);f.service(state,true);
            check(f.posts.size()==3&&f.posts.get(0).contains("unitId=10&"),"critical force wins its actor without starving independent construction or production");
            set(f.battle,"builderReserve",700L);set(f.battle,"investmentReserve",700L);set(f.battle,"buildJob",null);
            Map<String,Object> small=map("player",map("credits",1600.0));
            check(Boolean.FALSE.equals(call(f.battle,"ordinaryFundsAvailable",new Class<?>[]{Map.class,double.class},small,350.0)),"two chosen reserves protect their sum from cheap fallback production");
            small.put("player",map("credits",1750.0));check(Boolean.TRUE.equals(call(f.battle,"ordinaryFundsAvailable",new Class<?>[]{Map.class,double.class},small,350.0)),"exact affordable remainder allows legal continuing production");
            check(f.scheduler.availableTokens()==Integer.MAX_VALUE,"independent actor mode has no shared refill quota");
        }
    }
    static void productiveCapacityEstimate(){
        int half=ProductionCapacity.incomeSupportedProducers(160,80,400000,50,100000);
        int full=ProductionCapacity.incomeSupportedProducers(160,80,400000,100,100000);
        check(half==4&&full==8,"accepted spend and effective busy factory-time determine modeled income-supported producers");
        check(half<=full,"additional policy idle time cannot inflate supported producer count");
        check(ProductionCapacity.incomeSupportedProducers(160,0,400000,50,100000)==0,"unknown accepted conversion rate remains zero");
        check(ProductionCapacity.incomeSupportedProducers(160,80,400000,0,100000)==0,"zero observed productive factory-time cannot justify expansion");
        check(ProductionCapacity.incomeSupportedProducers(Double.NaN,80,400000,50,100000)==0,"unknown income remains an unknown expansion estimate");
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.g5","true");System.setProperty("rwagent.globalStrategy","false");System.setProperty("rwagent.executionBurst","1");System.setProperty("rwagent.commandBudgetIntervalGameMs","250");
        System.setProperty("rwagent.allowSmallTanks","true"); // Explicit legacy cheap-product service variant.
        System.setProperty("rwagent.g1Trace","false");System.setProperty("rwagent.g51","false");System.clearProperty("rwagent.mobileUnitHardCap");
        productiveCapacityEstimate();oneTokenHeavyDebt();criticalFirstAndCommittedMoney();System.out.println("G53ContinuingOperationsHarness PASS checks="+checks+" evidence=SYNTHETIC_HTTP_NO_NATIVE_ENGINE");
    }
}
