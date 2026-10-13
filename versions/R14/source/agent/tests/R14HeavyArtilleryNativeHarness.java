import com.corrodinggames.rts.game.units.am;
import io.rwagent.bootstrap.RuntimeBridge;
import io.rwagent.client.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Original initialized units, real BC guards/HTTP/owner pipeline; explicit fixtures, zero ticks. */
public final class R14HeavyArtilleryNativeHarness {
    static int checks;static Path out;static com.corrodinggames.rts.game.i engine;static G3NativeAcceptanceHarness.Client client;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Object call(String name,Class<?>[] types,Object... args)throws Exception{return G3NativeAcceptanceHarness.invoke(client.battle,name,types,args);}
    static GeneralRegistry registry()throws Exception{return (GeneralRegistry)G3NativeAcceptanceHarness.value(client.battle,"generals");}
    static void observe()throws Exception{call("observeG4Forces",new Class<?>[]{Map.class,Map.class},client.state,client.enemies);}
    static void advance(long ms)throws Exception{client.advance(ms,false);observe();}
    static void collect()throws Exception{call("collectG4Forces",new Class<?>[]{Map.class,Map.class,Map.class},client.state,client.enemies,client.scout);call("flushG4Forces",new Class<?>[0]);client.log.flush();}
    static List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target)throws Exception{
        return G3NativeAcceptanceHarness.packetList(call("crisisCompatible",new Class<?>[]{List.class,List.class,Map.class},actors,Collections.singletonList(target),client.enemies));}
    static Map<String,Object> enemy(long id){for(Map<String,Object> e:G3NativeAcceptanceHarness.items(client.enemies,"visibleEnemies"))if(((Number)e.get("id")).longValue()==id)return e;throw new AssertionError("not legally visible "+id);}
    static List<Map<String,Object>> rows(String event)throws Exception{
        client.log.flush();List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
        for(String line:Files.readAllLines(out.resolve(client.logName()),StandardCharsets.UTF_8)){Map<String,Object> r=G3NativeAcceptanceHarness.packet(Json.parse(line));if(event.equals(r.get("event")))result.add(r);}return result;}
    static void bootstrap()throws Exception{
        System.setProperty("rwagent.port","47721");System.setProperty("rwagent.g3Execution","true");System.setProperty("rwagent.g4Forces","true");System.setProperty("rwagent.g5","true");
        System.setProperty("rwagent.runtimeAdaptive","false");System.setProperty("rwagent.earlyOperations","true");System.setProperty("rwagent.activeArmyTarget","24");
        System.setProperty("rwagent.globalStrategy","false");System.setProperty("rwagent.g1Trace","true");System.setProperty("rwagent.g2WorldState","true");System.setProperty("rwagent.additionalDiagnostics","true");
        Method initialize=TerrainNativeCostHarness.class.getDeclaredMethod("initialize",String.class);initialize.setAccessible(true);engine=(com.corrodinggames.rts.game.i)initialize.invoke(null,"maps/skirmish/[p2]Big Island (2p).tmx");
        G3NativeAcceptanceHarness.engine=engine;G3NativeAcceptanceHarness.out=out;G3NativeAcceptanceHarness.fixture=Files.newBufferedWriter(out.resolve("fixture.jsonl"),StandardCharsets.UTF_8);
        for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)0);
        Thread pump=new Thread(()->{while(true){Runnable task=(Runnable)engine.k.poll();if(task!=null)task.run();try{Thread.sleep(1);}catch(InterruptedException done){return;}}},"r14-heavy-native-task-pump");pump.setDaemon(true);pump.start();RuntimeBridge.start(engine,47721,true);
    }
    static void flow()throws Exception{
        G3NativeAcceptanceHarness.base(18,0,40000);float[] dry=G3NativeAcceptanceHarness.dry();
        client=new G3NativeAcceptanceHarness.Client("r14-heavy",80,false);observe();
        GeneralRegistry.GeneralView general=registry().generals().get(0);
        am gun=G3NativeAcceptanceHarness.unit("heavyArtillery",500,engine.bs,dry[0]+50,dry[1]+150);
        check(gun.cu==600,"original loaded heavyArtillery is independently HP600, not ordinary artillery");
        advance(1000);check(GeneralRegistry.FREE_OWNER.equals(registry().unit(500).owner),"later actual ready own row admits new native heavy artillery to FREE");
        advance(5000);check(registry().requestJoin(500,general.id),"fixture requests existing formal General reservation without forced attachment");
        collect();check(registry().unit(500).membership==GeneralRegistry.Membership.JOINING&&registry().unit(500).joinAcceptedFrame>=0,"native JOINING command receipt retains reservation until later own arrival");
        G3NativeAcceptanceHarness.process();gun.eo=(float)general.joinTargetX;gun.ep=(float)general.joinTargetY;
        G3NativeAcceptanceHarness.record("fixture_position",G3NativeAcceptanceHarness.map("id",500,"x",gun.eo,"y",gun.ep,"basis","EXPLICIT_POST_RECEIPT_POSITION_NOT_NATURAL_TRAVEL"));
        advance(1000);check(registry().unit(500).membership==GeneralRegistry.Membership.ATTACHED&&general.owner.equals(registry().unit(500).owner),"later native own arrival attaches artillery to General with original generation rules");
        long generation=registry().unit(500).ownerGeneration;
        // Legal native fog field creates a frontier adjoining this observed formation.
        int ownTileX=(int)(gun.eo/engine.bL.n),ownTileY=(int)(gun.ep/engine.bL.o);
        for(int x=0;x<engine.bs.N.length;x++)for(int y=0;y<engine.bs.N[x].length;y++)engine.bs.N[x][y]=(byte)(Math.abs(x-ownTileX)<16&&Math.abs(y-ownTileY)<16?0:10);
        // The earlier all-visible samples recorded explored terrain. Native recon patrol
        // needs its documented 45s stale-fog age; recent refog alone is not a frontier.
        advance(50000);collect();boolean mixed=false;
        for(Map<String,Object> c:G3NativeAcceptanceHarness.commands()){List<?> actors=(List<?>)c.get("actors");if("attackMove".equals(c.get("waypoint"))&&actors.contains(500L)&&actors.contains(100L))mixed=true;}
        check(mixed,"real General native frontier command contains heavy artillery and heavyTank in one LAND group");
        check(registry().unit(500).ownerGeneration==generation,"mixed frontier leaves attached owner generation unchanged");
        G3NativeAcceptanceHarness.process();advance(1000);check("attackMove".equals(client.actor(500).get("orderType")),"applied native frontier command exposes artillery own order witness");
        for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)0);
        com.corrodinggames.rts.game.n opposing=new com.corrodinggames.rts.game.e(1,false);opposing.r=1;
        am surface=G3NativeAcceptanceHarness.unit("tank",900,opposing,gun.eo+90,gun.ep+30);
        am flyer=G3NativeAcceptanceHarness.unit("gunShip",901,opposing,gun.eo+70,gun.ep+10);flyer.eq=20;
        client.advance(1000,false);List<Map<String,Object>> guns=Collections.singletonList(client.actor(500));int before=rows("local_crisis_engagement").size();
        check(eligible(guns,enemy(900)).size()==1,"real BC static guard now admits current SURFACE artillery into original native eligibility query");
        List<Map<String,Object>> guardReads=rows("local_crisis_engagement");check(guardReads.size()==before+1,"current compatible artillery generates an actual native engagement GET");
        Map<String,Object> evidence=G3NativeAcceptanceHarness.packet(guardReads.get(guardReads.size()-1).get("data"));
        Map<String,Object> actor=G3NativeAcceptanceHarness.items(evidence,"actors").get(0);
        check("LAND".equals(actor.get("movementType"))&&"COMPATIBLE".equals(actor.get("compatibility"))&&((Number)actor.get("weaponRange")).doubleValue()==310,"native independent type confirms LAND/SURFACE/range310 without claiming fire or damage");
        check(eligible(guns,enemy(901)).isEmpty()&&rows("local_crisis_engagement").size()==guardReads.size(),"nearby AIR is statically incompatible, never widened into response or query");
        int tx=(int)(surface.eo/engine.bL.n),ty=(int)(surface.ep/engine.bL.o);engine.bs.N[tx][ty]=10;client.advance(1000,false);
        check(G3NativeAcceptanceHarness.items(client.enemies,"visibleEnemies").stream().noneMatch(e->((Number)e.get("id")).longValue()==900),"native fog removes current SURFACE authority");
        Map<String,Object> lastKnown=G3NativeAcceptanceHarness.map("id",900L,"targetDomain","SURFACE","touchingWater",false,"lastSeenGameTimeMs",0L);
        check(eligible(Collections.singletonList(client.actor(500)),lastKnown).isEmpty(),"last-known target cannot trigger heavy artillery native query");
        G3NativeAcceptanceHarness.record("r14_heavy_flow",G3NativeAcceptanceHarness.map("generation",generation,"nativeSurfaceEvidence",evidence,"nearbyAIRTarget",901,"catalogSource",TargetCatalog.evaluate("heavyArtillery","SURFACE",false,true,true).sourceId));client.close();
    }
    public static void main(String[] args){try{
        out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(out);bootstrap();flow();
        Map<String,Object> summary=G3NativeAcceptanceHarness.map("status","PASS","checks",checks,"fixtureOnly",true,"simulationTicks",0,"naturalProductionOrTravelProven",false,"nativeFiringOrVictoryProven",false,"desktopTouched",false);
        Files.write(out.resolve("summary.json"),G3NativeAcceptanceHarness.json(summary).getBytes(StandardCharsets.UTF_8));G3NativeAcceptanceHarness.fixture.close();System.out.println("R14_HEAVY_NATIVE_OK checks="+checks);System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
