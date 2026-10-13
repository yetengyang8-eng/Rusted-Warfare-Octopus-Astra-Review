import com.corrodinggames.rts.game.units.am;
import com.corrodinggames.rts.game.units.ar;
import com.corrodinggames.rts.game.units.y;
import java.util.*;

/** Native 1.15 batch own modes and lawful AIR planning, explicit fog fixtures, zero ticks. */
public final class R14AirNativeHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<?,?> get(String path)throws Exception{return EconomyHarness.obj(BridgeHarness.check("GET",path,200,"sessionId"));}
    static Map<?,?> mode(Map<?,?> packet,long id){for(Object raw:(List<?>)packet.get("units")){Map<?,?> row=(Map<?,?>)raw;
        if(((Number)row.get("unitId")).longValue()==id)return row;}throw new AssertionError("missing own mode identity "+id);}
    static y jet(long id,com.corrodinggames.rts.game.n team,float x,float yy){y unit=(y)ar.valueOf("amphibiousJet").a(true);
        unit.eh=id;unit.bX=team;unit.cm=1;unit.eo=x;unit.ep=yy;am.bE.add(unit);return unit;}
    static void batchModes(com.corrodinggames.rts.game.i e)throws Exception{
        EconomyHarness.set(e.bU,"D",EconomyHarness.costs);EconomyHarness.set(e.bU,"A",EconomyHarness.costs);
        EconomyHarness.set(Class.forName("com.corrodinggames.rts.game.units.b.c"),"b",BridgeHarness.construct("com.corrodinggames.rts.gameFramework.m.e"));
        y first=jet(9001,e.bs,810,1610),second=jet(9002,e.bs,830,1610);
        com.corrodinggames.rts.game.e other=new com.corrodinggames.rts.game.e(1,false);other.r=1;e.bs.r=0;
        y foreign=jet(9003,other,850,1610);foreign.eq=-5;second.eq=-5;
        Map<?,?> packet=get("/combat/unit-modes?unitIds=9001,9002,9003,999999");
        check(((List<?>)packet.get("units")).size()==4,"one batch retains one explicit result for each requested identity");
        check(Boolean.FALSE.equals(mode(packet,9001).get("submergedWeaponAvailable"))&&Boolean.TRUE.equals(mode(packet,9002).get("submergedWeaponAvailable")),"actual two native heights remain independent physical modes in one capture");
        check(packet.get("sessionId").equals(mode(packet,9001).get("sessionId"))&&packet.get("gameTimeMs").equals(mode(packet,9002).get("gameTimeMs")),"legal own batch rows retain actual capture session and game clock");
        for(long id:new long[]{9003,999999}){Map<?,?> row=mode(packet,id);
            check("UNKNOWN".equals(row.get("status"))&&!row.containsKey("actions")&&!row.containsKey("submergedWeaponAvailable")&&!row.containsKey("x")&&!row.containsKey("type"),"foreign or missing identity supplies UNKNOWN without hidden native state");}
        check(get("/combat/unit-modes?unitId=9002").get("submergedWeaponAvailable").equals(mode(packet,9002).get("submergedWeaponAvailable")),"legacy single-own contract remains available for Capability callers");
        BridgeHarness.check("GET","/combat/unit-modes?unitIds=9001,9001",400,"duplicate");
        BridgeHarness.check("GET","/combat/unit-modes?unitIds=9001&unitId=9002",400,"unsupported");
        StringBuilder oversize=new StringBuilder();for(int i=0;i<49;i++){if(i>0)oversize.append(',');oversize.append(10000+i);}
        BridgeHarness.check("GET","/combat/unit-modes?unitIds="+oversize,400,"48");
        first.eq=-5;e.by+=1000;Map<?,?> later=get("/combat/unit-modes?unitIds=9001,9002");
        check(Boolean.TRUE.equals(mode(later,9001).get("submergedWeaponAvailable")),"later actual native physical height produces new own witness rather than neighboring receipt");
        check(e.cf.b.isEmpty(),"mode batch observation queues no native command or mode change");
        am.bE.remove(first);am.bE.remove(second);am.bE.remove(foreign);
    }
    static void hardGlobalReservations(com.corrodinggames.rts.game.i e)throws Exception{
        am flyer=R13AirNativeHarness.add("gunShip",193,e.bs,950,1790);flyer.eq=20;
        e.by=100000;for(int c=0;c<110;c++)for(int r=0;r<110;r++)e.bs.N[c][r]=(byte)(r>=10?0:10);
        get("/scout/observe");for(byte[] col:e.bs.N)Arrays.fill(col,(byte)10);e.bs.N[47][89]=0;e.by=230000;
        String path="/scout/plan?role=recon&unitId=193&routeScope=air-search";
        Map<?,?> initial=get(path);int held=((Number)initial.get("frontierTile")).intValue();
        check(((Number)initial.get("fallbackLevel")).intValue()==4,"controlled old nearby fog reaches GLOBAL_EXHAUSTIVE unseen search fallback");
        Map<?,?> reserved=get(path+"&hardReservations="+Integer.toString(held,36)+"&reservationRadius=400&avoid="+held);
        check("planned".equals(reserved.get("status"))&&((Number)reserved.get("fallbackLevel")).intValue()==4,"hard reservation still allows another reachable global information area");
        int chosen=((Number)reserved.get("frontierTile")).intValue();double separation=Math.hypot((held/110-chosen/110)*20,(held%110-chosen%110)*20);
        check(separation>=400&&((Number)reserved.get("hardReservationCount")).intValue()==1,"global fallback excludes the complete other-owner region instead of ignoring history only");
        List<?> attempts=(List<?>)reserved.get("attempts");Map<?,?> last=(Map<?,?>)attempts.get(attempts.size()-1);
        check(Boolean.FALSE.equals(last.get("softAvoid"))&&Boolean.TRUE.equals(reserved.get("hardReservationsApplyAtEveryFallback")),"native GLOBAL_EXHAUSTIVE drops soft historical preference while preserving hard lease exclusion");
        BridgeHarness.check("GET","/scout/plan?role=recon&unitId=193&hardReservations="+Integer.toString(held,36),400,"air-search");
        check(e.cf.b.isEmpty(),"scoped reserved planning remains read-only");
    }
    static void rememberedAaObservationStand(com.corrodinggames.rts.game.i e)throws Exception{
        am flyer=null;for(Object raw:am.bE){am u=(am)raw;if(u.eh==193)flyer=u;}check(flyer!=null,"current own native AIR planner exists");
        flyer.eo=100;flyer.ep=100;
        com.corrodinggames.rts.game.e other=new com.corrodinggames.rts.game.e(1,false);other.r=1;
        am aa=R13AirNativeHarness.add("antiAirTurret",201,other,1000,500);
        e.bs.N[50][25]=0;get("/scout/observe");e.bs.N[50][25]=10;e.by+=1000;
        Map<?,?> approach=get("/scout/plan?role=recon&unitId=193&routeScope=air-search&observeX=1000&observeY=500");
        check("planned".equals(approach.get("status"))&&"SAFE_AREA_OBSERVATION".equals(approach.get("fallbackName")),"remembered weapon-circle center selects a native legal observation stand and complete route");
        double caution=((y)aa).m()+140,x=((Number)approach.get("targetX")).doubleValue(),yy=((Number)approach.get("targetY")).doubleValue();
        check(Math.hypot(x-1000,yy-500)>=caution&&Math.hypot(x-1000,yy-500)<caution+30,"selected reachable stand approaches area edge without demanding circle-center entry");
        check(Boolean.FALSE.equals(approach.get("observationApproachIsVisibilityProof"))&&Boolean.FALSE.equals(approach.get("pathVisible")),"native stand or route receipt is never a visibility or area-clear witness");
        List<?> route=(List<?>)approach.get("routeTiles");check(route.size()>2&&Boolean.TRUE.equals(approach.get("routeComplete")),"observation approach preserves executable multisegment native route");
        for(Object raw:route){int tile=((Number)raw).intValue();check(Math.hypot((tile/110+.5)*20-1000,(tile%110+.5)*20-500)>=caution,"every native observation route vertex avoids legally remembered AA caution circle");}
        Map<?,?> memory=get("/scout/observe");Map<?,?> threat=R13AirNativeHarness.row(memory,"rememberedThreats",201);
        check(Boolean.TRUE.equals(threat.get("canAttackAir"))&&e.bs.N[50][25]>=5,"planner uses legally captured native AA record while contact remains in fog");
        check(e.cf.b.isEmpty(),"observation area request neither attacks ghost nor creates native movement itself");
    }
    public static void main(String[] args){try{
        OpeningHarness.test();com.corrodinggames.rts.game.i e=EconomyHarness.engine;e.cf.b.clear();
        batchModes(e);hardGlobalReservations(e);rememberedAaObservationStand(e);
        System.out.println("R14_AIR_NATIVE_OK checks="+checks+" simulationTicks=0 fixtureOnly=true nativeVictoryProven=false");System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
