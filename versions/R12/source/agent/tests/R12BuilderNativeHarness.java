import com.corrodinggames.rts.game.units.am;
import com.corrodinggames.rts.game.units.ar;
import java.util.*;
import java.net.URLEncoder;

/** Real native legal menus/guards with explicit fixtures; no simulation or natural-arrival claim. */
public final class R12BuilderNativeHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<?,?> get(String path)throws Exception{return EconomyHarness.obj(BridgeHarness.check("GET",path,200,"builderId"));}
    static String site(Map<?,?> m){return m.get("extractorX")+","+m.get("extractorY");}
    public static void main(String[] args)throws Exception{
        try{
            OpeningHarness.test();com.corrodinggames.rts.game.i engine=EconomyHarness.engine;engine.cf.b.clear();
            Object grids=java.lang.reflect.Array.newInstance(Class.forName("com.corrodinggames.rts.gameFramework.k.i"),1);
            java.lang.reflect.Array.set(grids,0,EconomyHarness.costs);EconomyHarness.set(engine.bU,"v",grids);
            for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)0);
            Map<?,?> first=get("/expansion/plan?unitId=4");
            String avoid=URLEncoder.encode(site(first),"UTF-8");
            Map<?,?> second=get("/expansion/plan?unitId=4&excludeSites="+avoid);
            check(!site(first).equals(site(second)),"exclude a claimed actual mine and return another currently visible native legal mine");
            check(first.get("extractorCost").equals(second.get("extractorCost")),"price remains the current native action quote");
            check(site(get("/expansion/plan?unitId=4")).equals(site(first)),"old no-exclusion request keeps its nearest-site behavior");
            check(engine.cf.b.isEmpty(),"planning exclusions neither command nor spend");
            BridgeHarness.check("GET","/expansion/plan?unitId=4&excludeSites=bad",400,"x,y");
            BridgeHarness.check("GET","/economy/plan?unitId=4&excludeSites="+avoid,400,"unitId");
            am builder=ar.valueOf("builder").a(true);builder.eh=191;builder.bX=engine.bs;builder.eo=EconomyHarness.builder.eo+20;builder.ep=EconomyHarness.builder.ep;am.bE.add(builder);
            Map<?,?> other=get("/expansion/plan?unitId=191&excludeSites="+avoid);
            check(!site(first).equals(site(other)),"another actor can quote independent work while the first accepted command is not yet visible");
            String session=String.valueOf(first.get("sessionId"));
            BridgeHarness.check("POST","/command/build-extractor?unitId=4&x="+first.get("extractorX")+"&y="+first.get("extractorY")+"&sessionId="+session+"&requestId=r12-first",200,"queued");
            BridgeHarness.check("POST","/command/build-extractor?unitId=191&x="+other.get("extractorX")+"&y="+other.get("extractorY")+"&sessionId="+session+"&requestId=r12-second",200,"queued");
            check(engine.cf.b.size()==2,"independent constructors enter actual native command queue without duplicate site");
            for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)10);
            BridgeHarness.check("GET","/expansion/plan?unitId=191&excludeSites="+avoid,409,"visibleResourceCandidates=0");
            BridgeHarness.check("POST","/command/build-extractor?unitId=191&x="+other.get("extractorX")+"&y="+other.get("extractorY")+"&sessionId="+session+"&requestId=r12-hidden",409,"visible legal");
            check(engine.cf.b.size()==2,"exclusion cannot authorize hidden construction or bypass native guard");
            System.out.println("R12_BUILDER_NATIVE_OK checks="+checks+" simulationTicks=0 fixtureOnly=true receiptIsReady=false");System.exit(0);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
