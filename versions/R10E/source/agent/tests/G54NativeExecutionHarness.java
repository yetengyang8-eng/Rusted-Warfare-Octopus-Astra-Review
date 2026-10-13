import java.nio.file.*;
import java.util.*;

/** Original-engine fixture: real HTTP and command.k; not natural motion or healing. */
public final class G54NativeExecutionHarness {
    public static void main(String[] args)throws Exception{
        G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);
        G5NativeOperationsHarness.bootstrap();G3NativeAcceptanceHarness.base(64,4,100000);
        G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("g54-independent-native-actors",160,true);
        c.advance(1000,true);float[] p=G3NativeAcceptanceHarness.dry();
        G3NativeAcceptanceHarness.set(c.battle,"executingLane","RECOVERY");
        for(long actor=100;actor<164;actor++){
            String owner="fixture:recovery";
            G3NativeAcceptanceHarness.require(c.arbiter.claim(owner,actor),"own native mobile actor claimed");
            G3NativeAcceptanceHarness.require(c.battle.orderStrategy(owner,"/command/move?unitId="+actor+"&x="+p[0]+"&y="+p[1])!=null,"independent native move accepted");
        }
        c.produce();G3NativeAcceptanceHarness.require(G3NativeAcceptanceHarness.engine.cf.b.size()==68,"64 actual mobile HTTP commands cannot starve four independent native producers");
        Map<String,Object> plan=c.read("/economy/construction-plan?unitId=2&type=repairbay");
        G3NativeAcceptanceHarness.require("planned".equals(plan.get("status")),"native repairbay footprint legally quoted");
        G3NativeAcceptanceHarness.require(c.arbiter.claim("fixture:builder",2),"independent builder ownership claimed");
        Map<String,Object> context=G3NativeAcceptanceHarness.map("nativeCost",plan.get("cost"),"product",plan.get("type"));context.putAll(c.battle.costSourceStrategy(plan));
        String path="/command/construct?unitId=2&actionId="+java.net.URLEncoder.encode(String.valueOf(plan.get("actionId")),"UTF-8")+"&x="+plan.get("x")+"&y="+plan.get("y");
        G3NativeAcceptanceHarness.require(c.battle.orderStrategy("fixture:builder",path,context)!=null,"repair construction independently admitted after wounded moves and production");
        G3NativeAcceptanceHarness.require(G3NativeAcceptanceHarness.engine.cf.b.size()==69,"69 nonconflicting original native commands in one observation");
        G3NativeAcceptanceHarness.record("g54_independent_native_admission",G3NativeAcceptanceHarness.map("ledger",c.ledger(),"moves",64,"producers",4,"repairConstruction",1,"simulationTicks",0,"naturalHealing",false));
        G3NativeAcceptanceHarness.process();c.advance(1000,true);
        for(long actor=100;actor<164;actor++)G3NativeAcceptanceHarness.require("move".equals(c.actor(actor).get("orderType")),"later real native move order witness");
        int queued=0;for(Map<String,Object> own:G3NativeAcceptanceHarness.items(c.state,"ownUnits"))if("landFactory".equals(own.get("type"))&&((Number)own.get("productionQueue")).intValue()>0)queued++;
        G3NativeAcceptanceHarness.require(queued==4,"later four native queue effects observed");
        G3NativeAcceptanceHarness.require("build".equals(c.actor(2).get("orderType")),"later real repairbay build waypoint observed, not completed bay");
        G3NativeAcceptanceHarness.require(c.scheduler.unsettledCreditTotal()==((Number)plan.get("cost")).longValue(),"only unexecuted repair site price remains held; queue effects settled");
        c.close();G3NativeAcceptanceHarness.construction();
        G3NativeAcceptanceHarness.fixture.close();System.out.println("G54_NATIVE_EXECUTION_OK independentCommands=69 receiptIsEffect=false simulationTicks=0");System.exit(0);
    }
}
