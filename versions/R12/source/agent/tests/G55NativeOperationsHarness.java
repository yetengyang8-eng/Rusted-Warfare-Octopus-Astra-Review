import java.nio.file.*;
import java.util.*;

/** Original native menus, HTTP and command.k. Seeded fixtures; no completion or natural motion claim. */
public final class G55NativeOperationsHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void tierOne()throws Exception{
        for(Object raw:com.corrodinggames.rts.game.units.am.bE){com.corrodinggames.rts.game.units.am unit=(com.corrodinggames.rts.game.units.am)raw;
            if("landFactory".equals(unit.r().i()))unit.getClass().getMethod("a",int.class).invoke(unit,1);}
    }
    static List<String> queuedProducts(G3NativeAcceptanceHarness.Client c)throws Exception{
        List<String> products=new ArrayList<String>();
        // Receipts are recorded in the actual BattleClient log; native queue state is checked separately.
        c.log.flush();for(String line:Files.readAllLines(G3NativeAcceptanceHarness.out.resolve(c.label+"-battle.jsonl"),java.nio.charset.StandardCharsets.UTF_8)){
            Map<String,Object> row=G3NativeAcceptanceHarness.packet(io.rwagent.client.Json.parse(line));
            if(!"g3_execution".equals(row.get("event")))continue;
            Map<String,Object> d=G3NativeAcceptanceHarness.packet(row.get("data"));
            if(Boolean.TRUE.equals(d.get("nativeAccepted"))&&"/command/queue".equals(d.get("kind")))products.add(String.valueOf(G3NativeAcceptanceHarness.packet(d.get("receipt")).get("type")));
        }return products;
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.g5","true");System.clearProperty("rwagent.allowSmallTanks");
        G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);G5NativeOperationsHarness.bootstrap();
        G3NativeAcceptanceHarness.base(0,1,10000);tierOne();
        G3NativeAcceptanceHarness.Client first=new G3NativeAcceptanceHarness.Client("g55-opening-scout",96,false);first.advance(1000,false);first.produce();
        check(queuedProducts(first).equals(Arrays.asList("scout")),"actual T1 quote queues exactly opening scout before upgrade");
        first.produce();check(G3NativeAcceptanceHarness.engine.cf.b.size()==1,"same-observation pending scout never duplicates");
        G3NativeAcceptanceHarness.process();first.advance(1000,false);
        check(((Number)first.actor(50).get("productionQueue")).intValue()==1,"real native later queue witness, not ready scout");first.close();
        G3NativeAcceptanceHarness.base(0,1,10000);tierOne();float[] p=G3NativeAcceptanceHarness.dry();G3NativeAcceptanceHarness.unit("scout",100,G3NativeAcceptanceHarness.engine.bs,p[0],p[1]+100);
        G3NativeAcceptanceHarness.Client tech=new G3NativeAcceptanceHarness.Client("g55-scout-to-tech",96,false);tech.advance(1000,false);tech.produce();
        check(queuedProducts(tech).equals(Arrays.asList("upgrade")),"legally observed scout prevents duplicate opening procurement; zero ordinary army can upgrade");
        G3NativeAcceptanceHarness.process();tech.advance(1000,false);check(((Number)tech.actor(50).get("productionQueue")).intValue()==1,"native T1 upgrade genuinely enters queue; tier completion not claimed");tech.close();
        G3NativeAcceptanceHarness.base(8,3,10000);
        G3NativeAcceptanceHarness.Client heavy=new G3NativeAcceptanceHarness.Client("g55-heavy-continuous",96,false);heavy.advance(1000,false);heavy.produce();
        check(queuedProducts(heavy).equals(Arrays.asList("heavyTank","heavyTank","heavyTank")),"three independent actual T2 factories buy heavy, no small fallback");
        G3NativeAcceptanceHarness.process();heavy.advance(1000,false);int queues=0;for(Map<String,Object> actor:G3NativeAcceptanceHarness.items(heavy.state,"ownUnits"))if("landFactory".equals(actor.get("type"))&&((Number)actor.get("productionQueue")).intValue()>0)queues++;
        check(queues==3,"native queue witness proves all three producers execute independently");heavy.close();
        G3NativeAcceptanceHarness.fixture.close();System.out.println("G55_NATIVE_OPERATIONS_OK checks="+checks+" simulationTicks=0 fixtureOnly=true");System.exit(0);
    }
}
