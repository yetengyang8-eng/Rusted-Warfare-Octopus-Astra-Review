import com.corrodinggames.rts.game.units.*;
import io.rwagent.bootstrap.RuntimeBridge;
import io.rwagent.client.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** E2 native fixture with real RuntimeBridge HTTP and BC adapters. Unit placement,
 * fog, clocks and later arrival positions are explicit fixture controls, not a natural match. */
public final class G5NativeOperationsHarness {
    static int checks;static Path out;static com.corrodinggames.rts.game.i engine;
    static final int PORT=47679;static G3NativeAcceptanceHarness.Client client;
    static void require(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static Map<String,Object> map(Object... pairs){return G3NativeAcceptanceHarness.map(pairs);}
    static void record(String event,Object data)throws Exception{G3NativeAcceptanceHarness.record(event,data);}
    static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{return G3NativeAcceptanceHarness.invoke(target,name,types,args);}
    static Object value(Object target,String name)throws Exception{return G3NativeAcceptanceHarness.value(target,name);}
    static void set(Object target,String name,Object data)throws Exception{G3NativeAcceptanceHarness.set(target,name,data);}
    static GeneralRegistry registry()throws Exception{return (GeneralRegistry)value(client.battle,"generals");}
    static GeneralCombatDirector combat()throws Exception{return (GeneralCombatDirector)value(client.battle,"generalCombat");}
    static am actor(long id){for(Object item:am.bE){am unit=(am)item;if(unit.eh==id)return unit;}throw new AssertionError("missing native actor "+id);}
    static List<Map<String,Object>> units(Map<String,Object> packet,String key){return G3NativeAcceptanceHarness.items(packet,key);}
    static Map<String,Object> enemy(long id){for(Map<String,Object> enemy:units(client.enemies,"visibleEnemies"))if(((Number)enemy.get("id")).longValue()==id)return enemy;throw new AssertionError("enemy not legally visible "+id);}
    static void observe()throws Exception{call(client.battle,"observeG4Forces",new Class<?>[]{Map.class,Map.class},client.state,client.enemies);}
    static void advance(long delta)throws Exception{client.advance(delta,false);observe();}
    static void collect()throws Exception{call(client.battle,"collectG4Forces",new Class<?>[]{Map.class,Map.class,Map.class},client.state,client.enemies,client.scout);}
    static void flush()throws Exception{call(client.battle,"flushG4Forces",new Class<?>[0]);call(client.battle,"emitG5Transitions",new Class<?>[0]);client.log.flush();}
    static List<Map<String,Object>> rows(String event)throws Exception{
        client.log.flush();List<Map<String,Object>> rows=new ArrayList<Map<String,Object>>();
        for(String line:Files.readAllLines(out.resolve(client.logName()),StandardCharsets.UTF_8)){
            Map<String,Object> row=G3NativeAcceptanceHarness.packet(Json.parse(line));if(event.equals(row.get("event")))rows.add(row);
        }return rows;
    }
    static void position(long id,double x,double y,String reason)throws Exception{am unit=actor(id);double oldX=unit.eo,oldY=unit.ep;unit.eo=(float)x;unit.ep=(float)y;
        record("fixture_native_position",map("actor",id,"beforeX",oldX,"beforeY",oldY,"x",x,"y",y,"reason",reason,"basis","EXPLICIT_FIXTURE_NATIVE_FIELDS_NO_SIMULATION_TICKS"));}
    static com.corrodinggames.rts.game.n opponent(){com.corrodinggames.rts.game.n team=new com.corrodinggames.rts.game.e(1,false);team.r=1;return team;}
    static void bootstrap()throws Exception{
        System.setProperty("rwagent.port",String.valueOf(PORT));System.setProperty("rwagent.g3Execution","true");System.setProperty("rwagent.g4Forces","true");System.setProperty("rwagent.g5","true");
        System.setProperty("rwagent.runtimeAdaptive","false");System.setProperty("rwagent.earlyOperations","true");System.setProperty("rwagent.executionBurst","16");System.setProperty("rwagent.activeArmyTarget","24");
        System.setProperty("rwagent.globalStrategy","false");System.setProperty("rwagent.g1Trace","true");System.setProperty("rwagent.g2WorldState","true");System.setProperty("rwagent.additionalDiagnostics","true");
        Method initialize=TerrainNativeCostHarness.class.getDeclaredMethod("initialize",String.class);initialize.setAccessible(true);engine=(com.corrodinggames.rts.game.i)initialize.invoke(null,"maps/skirmish/[p2]Big Island (2p).tmx");
        G3NativeAcceptanceHarness.engine=engine;G3NativeAcceptanceHarness.out=out;G3NativeAcceptanceHarness.fixture=Files.newBufferedWriter(out.resolve("g5-native-fixture.jsonl"),StandardCharsets.UTF_8);
        for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)0);
        record("fixture_native_fog",map("reason","CONTROLLED_ALL_TILES_VISIBLE_TO_EXERCISE_LEGAL_CURRENT_ENDPOINT","value",0,"naturalReconProven",false));
        Thread pump=new Thread(()->{while(true){Runnable task=(Runnable)engine.k.poll();if(task!=null)task.run();try{Thread.sleep(1);}catch(InterruptedException stop){return;}}},"g5-native-isolated-task-pump");pump.setDaemon(true);pump.start();RuntimeBridge.start(engine,PORT,true);
        record("scope",map("evidence","E2_NATIVE_FIXTURE_WITH_REAL_HTTP","naturalMatch","NO_NATURAL_MATCH","desktopTouched",false,"simulationTicks",0,"g5",true,"runtimeAdaptive",false,"reasonAdaptiveDisabled","EAGER_RAW_TRACE_FLUSH_FOR_FIXTURE_ASSERTIONS","port",PORT));
    }
    static void nativeLargeCompatibility()throws Exception{
        G3NativeAcceptanceHarness.base(66,0,40000);float[] patch=G3NativeAcceptanceHarness.dry();
        for(int i=0;i<66;i++)position(100+i,patch[0]-60+(i%11)*12,patch[1]+30+(i/11)*12,"66_ACTORS_INSIDE_ORIGINAL_DRY_PATCH_FOR_NATIVE_APPROACH_GUARD");
        G3NativeAcceptanceHarness.unit("tank",900,opponent(),patch[0]+120,patch[1]+100);
        record("fixture_native_enemy_seed",map("id",900,"type","tank","x",actor(900).eo,"y",actor(900).ep,"hp",actor(900).cu,"reason","VISIBLE_COMPATIBLE_ORIGINAL_TARGET_WITHOUT_HP_MANIPULATION"));
        client=new G3NativeAcceptanceHarness.Client("g5-native-66",160,false);require(Boolean.TRUE.equals(value(client.battle,"g5Enabled")),"real BC candidate runs with G5 enabled");
        List<Map<String,Object>> candidates=new ArrayList<Map<String,Object>>();for(Map<String,Object> unit:units(client.state,"ownUnits"))if("heavyTank".equals(unit.get("type")))candidates.add(unit);
        require(candidates.size()==66,"66 actual native ready heavyTank observations");Map<String,Object> target=enemy(900);
        List<Map<String,Object>> eligible=G3NativeAcceptanceHarness.packetList(call(client.battle,"crisisCompatible",new Class<?>[]{List.class,List.class,Map.class},candidates,Collections.singletonList(target),client.enemies));
        require(eligible.size()==66,"real BC crisisCompatible passes all66 through original native legal guards without exiting");
        List<Map<String,Object>> reads=rows("local_crisis_engagement");require(reads.size()==2,"66 actual actors require two real native engagement GETs");Set<Long> seen=new HashSet<Long>();List<Object> evidence=new ArrayList<Object>();
        for(Map<String,Object> row:reads){Map<String,Object> data=G3NativeAcceptanceHarness.packet(row.get("data")),trace=G3NativeAcceptanceHarness.packet(row.get("trace"));Map<String,Object> observation=G3NativeAcceptanceHarness.packet(trace.get("observation"));
            String path=String.valueOf(observation.get("requestPath"));require(path.startsWith("/combat/engagement?unitIds=")&&path.endsWith("&targetId=900"),"raw trace binds actual native engagement request path");
            String csv=path.substring(path.indexOf("unitIds=")+8,path.indexOf("&targetId="));int count=csv.split(",").length;require(count<=48&&count>0,"each actual native query stays inside max48 protocol bound");
            require("observed".equals(data.get("status"))&&Boolean.TRUE.equals(data.get("targetVisible")),"actual native response is observed/current-visible");
            require(units(data,"actors").size()==count,"native actor result count matches real GET chunk");
            for(Map<String,Object> actor:units(data,"actors")){require("COMPATIBLE".equals(actor.get("compatibility"))&&"APPROACH_PATH_KNOWN".equals(actor.get("status")),"original native guard proves approach-only compatibility");require(seen.add(((Number)actor.get("unitId")).longValue()),"native chunks have no duplicate actor");}
            evidence.add(map("path",path,"observationId",observation.get("observationId"),"actorCount",count,"response",data));
        }
        require(seen.size()==66,"two native GET chunks cover exactly66 actors");record("g5_native_66_guard",map("requested",66,"eligible",eligible.size(),"chunks",evidence,"fakeHTTP",false,"approachIsSafetyOrKillProof",false));client.close();
    }
    static void nativeAdditionalFormation()throws Exception{
        G3NativeAcceptanceHarness.base(24,0,40000);client=new G3NativeAcceptanceHarness.Client("g5-native-birth",160,false);observe();
        require(registry().generals().size()==1,"24 original entry troops form one independent General");GeneralRegistry.GeneralView initial=registry().generals().get(0);
        require(initial.phase==GeneralRegistry.Phase.ACTIVE&&initial.members.size()==24&&initial.desiredStrength==24,"initial General is filled at its observed desired24");
        float[] patch=G3NativeAcceptanceHarness.dry();for(int i=0;i<38;i++)G3NativeAcceptanceHarness.unit("heavyTank",500+i,engine.bs,patch[0]+450+(i%6)*12,patch[1]+(i/6)*12);
        record("fixture_ready_products",map("idsStart",500,"count",38,"type","heavyTank","reason","EXPLICIT_READY_ORIGINAL_NATIVE_SEEDS_NOT_NATURAL_PRODUCTION","hpUnmodified",true));
        advance(1000);GeneralRegistry.GeneralView additional=null;for(GeneralRegistry.GeneralView general:registry().generals())if(!general.id.equals(initial.id))additional=general;
        require(additional!=null&&additional.phase==GeneralRegistry.Phase.FORMING&&additional.desiredStrength==24,"G5 current healthy FREE38 can create additional desired24 FORMING");
        require(!additional.owner.equals(initial.owner)&&additional.members.isEmpty(),"new General has independent owner without immediate fake attachment");
        int free=0;for(GeneralRegistry.UnitView unit:registry().units())if(unit.allocation==GeneralRegistry.Allocation.FREE&&unit.membership==GeneralRegistry.Membership.UNATTACHED)free++;
        require(free==38,"new ready actors remain FREE at their admission observation");record("g5_native_additional_forming",map("initial",initial.metadata(),"additional",additional.metadata(),"currentFree",free));
        advance(5000);collect();int before=engine.cf.b.size();require(before==0,"new join collection does not transport before root flush");flush();
        int first=engine.cf.b.size();require(first>0,"actual JOINING moves have native accepted receipts");
        for(Map<String,Object> command:G3NativeAcceptanceHarness.commands())require("move".equals(command.get("waypoint"))&&((List<?>)command.get("actors")).size()==1,"original native joining move protocol is single actor");
        G3NativeAcceptanceHarness.process();advance(5000);collect();flush();G3NativeAcceptanceHarness.process();
        Set<Long> joining=new LinkedHashSet<Long>();for(GeneralRegistry.UnitView unit:registry().units())if(additional.id.equals(unit.reservedGeneralId)&&unit.membership==GeneralRegistry.Membership.JOINING){require(unit.joinAcceptedFrame>=0,"each reserved join actor now has actual accepted native receipt");joining.add(unit.unitId);}
        require(joining.size()==24,"existing actual native joining pipeline fills additional24 reservations");
        GeneralRegistry.GeneralView rally=registry().general(additional.id);for(Long id:joining)position(id,rally.joinTargetX,rally.joinTargetY,"POST_RECEIPT_POSITION_FIXTURE_TO_TEST_LATER_OWN_WITNESS_NOT_NATURAL_MOVEMENT");
        advance(1000);GeneralRegistry.GeneralView attached=registry().general(additional.id);
        require(attached.phase==GeneralRegistry.Phase.ACTIVE&&attached.members.size()==24,"later native own-position witnesses activate the additional General");
        for(Long id:attached.members)require(client.arbiter.owns(attached.owner,id),"additional General owns actual attached actor generation");
        require(registry().general(initial.id).members.size()==24,"old General identity and attached roster remain independent");
        record("g5_native_additional_active",map("general",attached.metadata(),"acceptedJoinActors",joining,"basis","EXPLICIT_LATER_NATIVE_POSITION_WITNESS_NOT_NATURAL_ROUTE_DURATION"));client.close();
    }
    static float[] distantDry(float x,float y){for(int tx=8;tx<engine.bL.C-8;tx++)for(int ty=8;ty<engine.bL.D-8;ty++){
        float px=tx*engine.bL.n+10,py=ty*engine.bL.o+10;if(Math.hypot(px-x,py-y)<1050)continue;boolean dry=true;
        for(int dx=-5;dx<=5&&dry;dx++)for(int dy=-5;dy<=5;dy++)if(engine.bU.a(ao.b).d[(tx+dx)*engine.bL.D+ty+dy]<0){dry=false;break;}
        if(dry)return new float[]{px,py};}throw new AssertionError("second isolated dry patch");}
    static void nativeIndependentRetreat()throws Exception{
        G3NativeAcceptanceHarness.base(12,0,40000);float[] first=G3NativeAcceptanceHarness.dry(),second=distantDry(first[0],first[1]);
        for(int i=0;i<6;i++)position(100+i,first[0]+i*12,first[1]+150,"INDEPENDENT_GENERAL_A_NATIVE_SEED");
        for(int i=0;i<6;i++)position(106+i,second[0]+i*12,second[1],"INDEPENDENT_GENERAL_B_NATIVE_SEED");
        // A visible strategic building remains an independent economic opportunity. A remote
        // mobile builder now requires Commander allocation and cannot bypass the G5 budget.
        com.corrodinggames.rts.game.n enemyTeam=opponent();G3NativeAcceptanceHarness.unit("experimentalTank",920,enemyTeam,first[0]+130,first[1]+150);G3NativeAcceptanceHarness.unit("extractor",921,enemyTeam,second[0]+130,second[1]);G3NativeAcceptanceHarness.unit("experimentalTank",922,enemyTeam,first[0]+160,first[1]+150);
        record("fixture_visible_targets",map("ids",Arrays.asList(920,921,922),"reason","LOCAL_PRESSURE_A_AND_UNDEFENDED_ECONOMIC_TARGET_B_G51_REJECTS_DEFENDED_CC","hpUnmodified",true,"AEnemyHp",actor(920).cu+actor(922).cu,"BEnemyHp",actor(921).cu));
        client=new G3NativeAcceptanceHarness.Client("g5-native-independent",160,false);observe();require(registry().generals().size()==2,"two native own clusters seed two actual independent Generals");
        GeneralRegistry.GeneralView a=null,b=null;for(GeneralRegistry.GeneralView general:registry().generals()){if(general.members.contains(100L))a=general;if(general.members.contains(106L))b=general;}
        require(a!=null&&b!=null&&!a.owner.equals(b.owner),"separate A/B real owner identities");Map<Long,Long> generations=new LinkedHashMap<Long,Long>();for(Long id:a.members)generations.put(id,registry().unit(id).ownerGeneration);
        record("g5_native_independent_pressure",map("A",combat().view(a.id),"B",combat().view(b.id)));
        require(!combat().retreating(a.id)&&!combat().retreating(b.id),"visible native pressure without actual own harm is objective caution only");
        for(Long id:a.members)actor(id).cu-=60;record("fixture_own_damage",map("actors",a.members,"hpDecreasePerActor",60,"generalMaxHpFraction",.10,"basis","EXPLICIT_NATIVE_HP_FIXTURE_NOT_NATURAL_DAMAGE"));advance(1000);
        require(combat().retreating(a.id)&&!combat().retreating(b.id),"bounded native own HP decline with local current pressure admits only General A retreat");advance(5000);collect();flush();
        Set<Long> originalB=new LinkedHashSet<Long>(Arrays.asList(106L,107L,108L,109L,110L,111L));
        int retreats=0,attacks=0;Set<Long> attackingB=new LinkedHashSet<Long>();for(Map<String,Object> command:G3NativeAcceptanceHarness.commands()){
            List<?> ids=(List<?>)command.get("actors");if("move".equals(command.get("waypoint"))){require(ids.size()==1&&a.members.contains(((Number)ids.get(0)).longValue()),"actual native retreat is one owned A actor");retreats++;}
            if("attackMove".equals(command.get("waypoint"))){require(!ids.isEmpty()&&originalB.containsAll(ids),"B native small response and continuing General never borrow A actors");for(Object raw:ids)require(attackingB.add(((Number)raw).longValue()),"each B actor has one native command owner");attacks++;}
        }
        require(retreats==6&&attacks==2&&attackingB.size()==6,"A retreat coexists with B local detachment and continuing General in real root batch");
        int borrowed=0;for(Long id:originalB){GeneralRegistry.UnitView u=registry().unit(id);if(u.temporaryTask==GeneralRegistry.TemporaryTask.LOCAL_RESPONSE){require(u.owner.startsWith("local-response:"),"small building responders have a real temporary owner");borrowed++;}else require(b.owner.equals(u.owner),"unborrowed B members retain General ownership");}
        require(borrowed>0&&borrowed<originalB.size(),"isolated economic building borrows only part of B");
        int receiptRows=0;for(Map<String,Object> row:rows("g3_execution")){Map<String,Object> data=G3NativeAcceptanceHarness.packet(row.get("data"));if(Boolean.TRUE.equals(data.get("nativeAccepted"))&&Boolean.TRUE.equals(data.get("nativeAttempted"))){
            Map<String,Object> receipt=G3NativeAcceptanceHarness.packet(data.get("receipt"));require("queued".equals(receipt.get("status"))&&receipt.get("requestId") instanceof String&&receipt.get("frame") instanceof Number,"native accepted trace binds actual queued request identity and native frame");
            require(Boolean.FALSE.equals(data.get("receiptIsExecution"))&&data.get("executionWitness")==null,"native receipt provenance does not claim execution or arrival");receiptRows++;}}
        require(receiptRows==8,"raw BC scheduler records eight actual accepted native receipts including the separate local detachment");
        record("g5_native_independent_orders",map("commands",G3NativeAcceptanceHarness.commands(),"A",combat().view(a.id),"B",combat().view(b.id),"acceptedNativeReceipts",receiptRows));
        G3NativeAcceptanceHarness.process();advance(1000);require("move".equals(client.actor(100).get("orderType"))&&"attackMove".equals(client.actor(106).get("orderType")),"later actual native state exposes applied move and attack orders");
        require(combat().retreating(a.id),"processed move receipts and unchanged native positions do not claim arrival or recovery");
        for(Long id:a.members)require(a.owner.equals(registry().unit(id).owner)&&generations.get(id).longValue()==registry().unit(id).ownerGeneration,"actual native retreat leaves owner generation intact");
        record("g5_native_later_order_witness",map("AActor",client.actor(100),"BActor",client.actor(106),"basis","ORIGINAL_COMMAND_K_APPLIED_NO_NATURAL_MOTION","AStillRetreating",combat().retreating(a.id)));client.close();
    }
    public static void main(String[] args){try{
        if(args.length!=1)throw new IllegalArgumentException("OUTPUT_DIRECTORY");out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(out);bootstrap();nativeLargeCompatibility();nativeAdditionalFormation();nativeIndependentRetreat();
        Map<String,Object> summary=map("status","PASS","checks",checks,"evidence","E2_NATIVE_FIXTURE_WITH_REAL_HTTP","naturalMatch","NO_NATURAL_MATCH","simulationTicks",0,"actualNativeLargeGuard",66,"nativeGroupLimit",48,"actualBCG5",true,"desktopTouched",false,"naturalProductionOrRouteProven",false,"output",out.toString());
        Files.write(out.resolve("g5-native-summary.json"),G3NativeAcceptanceHarness.json(summary).getBytes(StandardCharsets.UTF_8));record("summary",summary);G3NativeAcceptanceHarness.fixture.close();System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();try{if(out!=null)Files.write(out.resolve("g5-native-summary.json"),G3NativeAcceptanceHarness.json(map("status","FAIL","checks",checks,"failure",failure.toString(),"evidence","E2_NATIVE_FIXTURE_WITH_REAL_HTTP","naturalMatch","NO_NATURAL_MATCH")).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}System.exit(1);}}
}
