import com.corrodinggames.rts.game.units.am;
import com.corrodinggames.rts.game.units.ar;
import java.lang.reflect.Array;
import java.util.*;

/** Native fog/cost fixtures for durable Scout routes and lawful Builder detour waypoints. */
public final class R10ScoutBridgeHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<?,?> get(String path)throws Exception{return EconomyHarness.obj(BridgeHarness.check("GET",path,200,"sessionId"));}
    static double n(Map<?,?> m,String key){return ((Number)m.get(key)).doubleValue();}
    static double segmentDistance(double sx,double sy,double x,double y,double tx,double ty){
        double dx=x-sx,dy=y-sy,length2=dx*dx+dy*dy;
        double t=length2==0?0:Math.max(0,Math.min(1,((tx-sx)*dx+(ty-sy)*dy)/length2));
        return Math.hypot(sx+t*dx-tx,sy+t*dy-ty);
    }
    public static void main(String[] args)throws Exception{
        try{
            OpeningHarness.test();com.corrodinggames.rts.game.i engine=EconomyHarness.engine;
            Object costs=Array.newInstance(Class.forName("com.corrodinggames.rts.gameFramework.k.i"),1);Array.set(costs,0,EconomyHarness.costs);
            EconomyHarness.set(engine.bU,"v",costs);engine.cf.b.clear();
            for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)10);
            for(int c=35;c<=60;c++)for(int r=75;r<=103;r++)engine.bs.N[c][r]=0;
            get("/scout/observe");Map<?,?> approach=get("/scout/resource-approach?unitId=4&tile="+(55*110+94));
            check(Boolean.TRUE.equals(approach.get("pathKnown"))&&Boolean.TRUE.equals(approach.get("safeSegmentKnown")),"Builder response separates lawful route and safe current straight segment");
            int startCol=(int)(EconomyHarness.builder.eo/20),startRow=(int)(EconomyHarness.builder.ep/20),waypoint=(int)n(approach,"waypointTile");
            check((waypoint/110==startCol||waypoint%110==startRow)
                    &&Math.abs(waypoint/110-startCol)+Math.abs(waypoint%110-startRow)<=6,"Builder waypoint is bounded first straight segment of actual safe BFS route");
            check(Math.hypot(n(approach,"x")-1110,n(approach,"y")-1890)<=60,"legacy x/y still identifies resource final approach");
            check(n(approach,"waypointX")==(waypoint/110+.5)*20&&n(approach,"waypointY")==(waypoint%110+.5)*20,"waypoint coordinate identifies returned legal route tile");
            // A full lawful visible corridor reaches the north edge; hidden cost poisoning cannot shorten it.
            for(int c=35;c<=60;c++)for(int r=5;r<=103;r++)engine.bs.N[c][r]=0;
            EconomyHarness.set(Class.forName("com.corrodinggames.rts.game.units.e.n"),"b",BridgeHarness.construct("com.corrodinggames.rts.gameFramework.m.e"));
            ar tankType=ar.valueOf("tank");tankType.h();am scout=tankType.a(true);scout.eh=191;scout.bX=engine.bs;scout.eo=950;scout.ep=1790;scout.cm=1;am.bE.add(scout);
            Map<?,?> full=get("/scout/plan?role=recon&routeScope=scout-search&unitId=191");
            check("planned".equals(full.get("status"))&&Boolean.TRUE.equals(full.get("routeComplete")),"ground Scout receives complete durable legal route");
            check(full.get("targetTile").equals(full.get("frontierTile")),"durable Scout target is selected frontier rather than 260-world truncation");
            List<?> route=(List<?>)full.get("routeTiles");check(((Number)route.get(route.size()-1)).longValue()==((Number)full.get("frontierTile")).longValue(),"complete route terminates at selected information frontier");
            for(Object raw:route){int tile=((Number)raw).intValue();check(engine.bs.N[tile/110][tile%110]<5,"Scout full route preserves legal terrain boundary");}
            byte[] poisoned=new byte[12100];for(int c=0;c<110;c++)for(int r=0;r<110;r++)if(engine.bs.N[c][r]>=5)poisoned[c*110+r]=-1;
            EconomyHarness.set(EconomyHarness.costs,"d",poisoned);Map<?,?> same=get("/scout/plan?role=recon&routeScope=scout-search&unitId=191");
            check(full.get("targetTile").equals(same.get("targetTile"))&&route.equals(same.get("routeTiles")),"poisoned hidden terrain cannot alter complete Scout route");
            BridgeHarness.check("GET","/scout/plan?role=army&routeScope=scout-search&unitId=191",400,"requires role recon");
            for(int c=35;c<=80;c++)for(int r=60;r<=103;r++)engine.bs.N[c][r]=0;
            EconomyHarness.set(EconomyHarness.costs,"d",new byte[12100]);EconomyHarness.builder.eo=710;EconomyHarness.builder.ep=1490;
            am enemy=tankType.a(true);enemy.eh=192;enemy.bX=new com.corrodinggames.rts.game.e(1,false);enemy.bX.r=1;engine.bs.r=0;enemy.eo=1000;enemy.ep=1650;am.bE.add(enemy);
            Map<?,?> observed=get("/scout/observe");List<?> threats=(List<?>)observed.get("visibleThreats");
            check(threats.size()==1,"detour fixture uses one legally visible native armed threat");
            double radius=n((Map<?,?>)threats.get(0),"range")+140;
            Map<?,?> detour=get("/scout/resource-approach?unitId=4&tile="+(55*110+94));
            check(segmentDistance(710,1490,n(detour,"x"),n(detour,"y"),1000,1650)<radius,"legacy final-resource straight line crosses lawful danger");
            check(segmentDistance(710,1490,n(detour,"waypointX"),n(detour,"waypointY"),1000,1650)>radius,"new Builder waypoint follows safe detour rather than hazardous final-resource chord");
            check(engine.cf.b.isEmpty(),"read-only Scout and Builder planning creates no native commands");
            System.out.println("R10_SCOUT_BRIDGE_OK checks="+checks+" nativeFixture=true");System.exit(0);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
