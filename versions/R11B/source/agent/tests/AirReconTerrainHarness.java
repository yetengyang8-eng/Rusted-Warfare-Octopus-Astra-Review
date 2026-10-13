import com.corrodinggames.rts.game.units.am;
import com.corrodinggames.rts.game.units.ao;
import com.corrodinggames.rts.game.units.ar;
import java.lang.reflect.Array;
import java.util.*;

/** Native engine/fog fixture: AIR movement rule allows exploration, hidden terrain remains unobserved. */
public final class AirReconTerrainHarness {
    static void require(boolean ok,String why){BridgeHarness.require(ok,why);}
    static Map<?,?> get(String path)throws Exception{return EconomyHarness.obj(BridgeHarness.check("GET",path,200,"sessionId"));}
    public static void main(String[] args)throws Exception{
        try{
            OpeningHarness.test();com.corrodinggames.rts.game.i e=EconomyHarness.engine;
            e.cf.b.clear();for(byte[] col:e.bs.N)Arrays.fill(col,(byte)10);
            e.bs.N[47][89]=0;
            // The only native grid in this fixture is LAND. AIR policy must not inspect or require it.
            Object grids=Array.newInstance(Class.forName("com.corrodinggames.rts.gameFramework.k.i"),1);
            Array.set(grids,0,EconomyHarness.costs);EconomyHarness.set(e.bU,"v",grids);
            am flyer=ar.valueOf("gunShip").a(true);flyer.eh=193;flyer.bX=e.bs;flyer.eo=950;flyer.ep=1790;flyer.cm=1;flyer.cu=260;
            require(flyer.h()==ao.d,"fixture aircraft uses real native AIR movement");am.bE.add(flyer);
            Map<?,?> before=get("/scout/observe");int seen=((Number)before.get("exploredTiles")).intValue();
            Map<?,?> plan=get("/scout/plan?role=recon&unitId=193");
            require("planned".equals(plan.get("status"))&&"AIR".equals(plan.get("movementType")),"AIR receives lawful frontier with no AIR path grid");
            require(Boolean.TRUE.equals(plan.get("pathKnown")),"universal AIR movement is a known rule");
            boolean unseen=false;for(Object item:(List<?>)plan.get("routeTiles")){
                int tile=((Number)item).intValue();if(e.bs.N[tile/110][tile%110]>=5)unseen=true;
            }
            require(unseen,"AIR route genuinely leaves the tiny legally visible region");
            require(Boolean.FALSE.equals(plan.get("pathVisible")),"unseen AIR corridor is not misreported as visible");
            require(((Number)plan.get("potentialNeverSeenTiles")).intValue()>0,"destination carries lawful unknown-map information need");
            am enemy=ar.valueOf("gunShip").a(true);enemy.eh=194;enemy.bX=new com.corrodinggames.rts.game.e(1,false);
            enemy.bX.r=1;e.bs.r=0;enemy.eo=1010;enemy.ep=1790;enemy.cm=1;am.bE.add(enemy);e.bs.N[50][89]=0;
            Map<?,?> danger=get("/scout/observe");
            List<?> threats=(List<?>)danger.get("rememberedThreats");require(!threats.isEmpty(),"only current legal sighting supplies new hazard");
            Map<?,?> threat=(Map<?,?>)threats.get(0);double caution=((Number)threat.get("range")).doubleValue()+140;
            Map<?,?> detour=get("/scout/plan?role=recon&unitId=193");
            require("planned".equals(detour.get("status")),"new threat surrounding anchor selects an escape route instead of exhausting all exploration");
            double previous=-1;for(Object item:(List<?>)detour.get("routeTiles")){
                int tile=((Number)item).intValue();double d=Math.hypot((tile/110+.5)*20-enemy.eo,(tile%110+.5)*20-enemy.ep);
                if(previous>=0&&d<caution)require(d>previous,"every route edge inside known caution circle moves outward");previous=d;
            }
            am.bE.remove(enemy);get("/scout/observe"); // legally visible empty site clears this fixture threat
            plan=get("/scout/plan?role=recon&unitId=193");
            // Poison hidden map cells. Replanning must not read them or change AIR route choice.
            java.lang.reflect.Field field=e.bL.u.getClass().getDeclaredField("q");field.setAccessible(true);
            short[] cells=(short[])field.get(e.bL.u);short[] original=cells.clone();
            for(int i=0;i<cells.length;i++)if(i!=47*110+89&&i!=50*110+89)cells[i]=32767;
            Map<?,?> poisoned=get("/scout/plan?role=recon&unitId=193");
            require(plan.get("targetTile").equals(poisoned.get("targetTile")),"invalid hidden terrain does not affect AIR plan");
            Map<?,?> after=get("/scout/observe");
            require(((Number)after.get("exploredTiles")).intValue()==seen+1,"planning never turns unseen tiles into observed terrain or vision beyond the explicit enemy sighting");
            System.arraycopy(original,0,cells,0,cells.length);
            // Leave an unseen northern band beyond the local radius, with legally witnessed old
            // fog nearby. AIR endgame asks for coverage, not an endless convenient nearby recheck.
            e.by=100000;for(int c=0;c<110;c++)for(int r=0;r<110;r++)e.bs.N[c][r]=(byte)(r>=10?0:10);
            get("/scout/observe");for(byte[] col:e.bs.N)Arrays.fill(col,(byte)10);e.bs.N[47][89]=0;e.by=230000;
            Map<?,?> legacy=get("/scout/plan?role=recon&unitId=193");
            require(((Number)legacy.get("pathLength")).intValue()<=260&&!legacy.containsKey("routeComplete"),"existing unscoped Recon keeps its bounded menu contract");
            Map<?,?> full=get("/scout/plan?role=recon&unitId=193&routeScope=air-search");
            require("planned".equals(full.get("status"))&&Boolean.TRUE.equals(full.get("routeComplete")),"explicit AIR consumer receives a complete lawful information route");
            require(full.get("frontierTile").equals(full.get("targetTile"))&&((Number)full.get("pathLength")).intValue()>260,"AIR route reaches its selected frontier rather than inheriting short Scout truncation");
            require(((Number)full.get("potentialNeverSeenTiles")).intValue()>0&&((Number)full.get("fallbackLevel")).intValue()==4,"nearby old fog cannot conceal reachable never-seen coverage beyond local radius");
            require(((Number)full.get("gameTimeMs")).longValue()==230000&&full.get("frame") instanceof Number,"route exports its actual source clock for later own-anchor validation");
            BridgeHarness.check("GET","/scout/plan?role=army&unitId=193&routeScope=air-search",400,"error");
            EconomyHarness.set(Class.forName("com.corrodinggames.rts.game.units.e.n"),"b",BridgeHarness.construct("com.corrodinggames.rts.gameFramework.m.e"));
            ar.valueOf("tank").h();am land=ar.valueOf("tank").a(true);land.eh=195;land.bX=e.bs;land.eo=950;land.ep=1790;land.cm=1;am.bE.add(land);
            BridgeHarness.check("GET","/scout/plan?role=recon&unitId=195&routeScope=air-search",409,"error");
            for(int c=0;c<110;c++)for(int r=0;r<10;r++)cells[c*110+r]=32767;
            Map<?,?> fullPoisoned=get("/scout/plan?role=recon&unitId=193&routeScope=air-search");
            require(full.get("targetTile").equals(fullPoisoned.get("targetTile"))&&full.get("routeTiles").equals(fullPoisoned.get("routeTiles")),"complete AIR coverage planning does not inspect poisoned hidden terrain");
            require(e.cf.b.isEmpty(),"read-only AIR planning issues no native move or attack");
            System.out.println("AIR_RECON_TERRAIN_OK checks="+BridgeHarness.checks+" nativeFixture=true");System.exit(0);
        }catch(Throwable failure){failure.printStackTrace();System.exit(1);}
    }
}
