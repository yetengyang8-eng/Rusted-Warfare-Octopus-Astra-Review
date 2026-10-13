import io.rwagent.client.BattleClient;
import java.util.*;
import java.io.*;

/** Native current refusal reaches the Agent as negative quote evidence, not a transport UNKNOWN. */
public final class R13MineQuoteNativeHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){try{
        OpeningHarness.test();com.corrodinggames.rts.game.i engine=EconomyHarness.engine;engine.cf.b.clear();
        Object grids=java.lang.reflect.Array.newInstance(Class.forName("com.corrodinggames.rts.gameFramework.k.i"),1);
        java.lang.reflect.Array.set(grids,0,EconomyHarness.costs);EconomyHarness.set(engine.bU,"v",grids);
        System.setProperty("rwagent.port","47656");System.setProperty("rwagent.g1Trace","false");
        BattleClient client=new BattleClient();EconomyHarness.set(client,"log",new BufferedWriter(new StringWriter()));
        EconomyHarness.set(client,"session",CombatHarness.get("/state").get("sessionId"));
        for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)0);
        Map<String,Object> quote=client.readMinePlanStrategy("/expansion/plan?unitId=4","test_quote");
        check("planned".equals(quote.get("status"))&&quote.get("extractorCost") instanceof Number,"current native quote retains native cost");
        for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)10);
        Map<String,Object> refused=client.readMinePlanStrategy("/expansion/plan?unitId=4","test_refusal");
        check("no-current-work".equals(refused.get("status"))&&!refused.containsKey("extractorCost"),"same native endpoint current409 revokes an unpaid quote without making a fake price");
        check(client.readStrategy("/expansion/plan?unitId=4","test_legacy")==null,"other optional readers retain existing UNKNOWN/null semantics");
        check(engine.cf.b.isEmpty(),"quote/refusal analysis has no command or native spending effects");
        System.out.println("R13_MINE_QUOTE_NATIVE_OK checks="+checks+" simulationTicks=0 fixtureOnly=true");System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
