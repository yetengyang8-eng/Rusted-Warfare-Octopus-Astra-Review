import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import io.rwagent.client.*;
import com.corrodinggames.rts.game.units.am;

/** Controlled original-engine HTTP/queue fixture; no natural survival or healing claim. */
public final class G5ClosureNativeHarness {
    static int checks;
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static void investments()throws Exception{
        G3NativeAcceptanceHarness.base(6,2,560000);float[] dry=G3NativeAcceptanceHarness.dry();
        G3NativeAcceptanceHarness.unit("extractorT3",700,G3NativeAcceptanceHarness.engine.bs,dry[0]+400,dry[1]+300);
        G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("closure-native-invest",512,false);
        Map<String,Object> menu=c.read("/economy/investments");List<Map<String,Object>> quotes=G3NativeAcceptanceHarness.items(menu,"units");
        Map<String,Object> reinforce=null,overclock=null;
        for(Map<String,Object> quote:quotes)if(((Number)quote.get("id")).longValue()==700){
            if("extractorT3_reinforced".equals(quote.get("product")))reinforce=quote;
            if("extractorT3_overclocked".equals(quote.get("product")))overclock=quote;}
        check(reinforce!=null&&overclock!=null,"both original typed native T3 conversion actions exposed");
        check(((Number)reinforce.get("cost")).intValue()==3000&&((Number)overclock.get("cost")).intValue()==8000,"actual native quote prices, not community total-price inference");
        c.advance(1000,false);Map<String,Object> context=G3NativeAcceptanceHarness.map("cost",reinforce.get("cost"),"product",reinforce.get("product"),"producerSlots",1,"quotedProducerQueue",0,"investmentSlotAvailable",1);
        context.putAll(c.battle.costSourceStrategy(menu));Map<String,Object> response=c.battle.orderStrategy(CommandArbiter.DEFAULT_OWNER,
            "/command/invest?unitId=700&actionId="+reinforce.get("actionId"),context);
        check(response!=null&&"queued".equals(response.get("status")),"original guarded reinforce command accepted");
        double before=G3NativeAcceptanceHarness.engine.bs.o;G3NativeAcceptanceHarness.process();check(G3NativeAcceptanceHarness.engine.bs.o==before-3000,"later native payment effect exact3000");
        c.advance(1000,false);check(((Number)c.actor(700).get("productionQueue")).intValue()>0,"later native queue witness, not reinforced completion");
        Map<String,Object> plan=c.read("/economy/construction-plan?unitId=2&type=repairbay");
        check("planned".equals(plan.get("status"))&&"repairbay".equals(plan.get("type"))&&((Number)plan.get("cost")).intValue()>0,"normal native builder can quote a visible legal repairbay site");
        G3NativeAcceptanceHarness.record("closure_native_investment",G3NativeAcceptanceHarness.map("nativeMenu",menu,"receipt",response,"laterQueue",c.actor(700),"repairBayPlan",plan,"conversionCompleted",false));
        c.close();
    }
    static void recovery()throws Exception{
        G3NativeAcceptanceHarness.base(6,0,40000);float[] dry=G3NativeAcceptanceHarness.dry();
        G3NativeAcceptanceHarness.unit("repairbay",800,G3NativeAcceptanceHarness.engine.bs,dry[0]+250,dry[1]+250);
        am wounded=G5NativeOperationsHarness.actor(100);wounded.cu=wounded.cv*.2f;
        G3NativeAcceptanceHarness.record("fixture_initial_severe_hp",G3NativeAcceptanceHarness.map("unit",100,"hp",wounded.cu,"maxHp",wounded.cv,"laterHealingInjected",false));
        G5NativeOperationsHarness.client=new G3NativeAcceptanceHarness.Client("closure-native-recovery",512,false);G5NativeOperationsHarness.observe();
        GeneralRegistry registry=G5NativeOperationsHarness.registry();check("force:recovery".equals(registry.unit(100).owner),"original own HP drives recovery owner");
        check(registry.unit(100).generalId==null,"native wounded removed from General");
        G5NativeOperationsHarness.advance(1000);G5NativeOperationsHarness.collect();G5NativeOperationsHarness.flush();boolean found=false;
        for(Map<String,Object> command:G3NativeAcceptanceHarness.commands())if(((List<?>)command.get("actors")).contains(100L)){
            check("move".equals(command.get("waypoint")),"native recovery uses move waypoint");found=true;}
        check(found,"native recovery emits real owned command");G3NativeAcceptanceHarness.process();G5NativeOperationsHarness.advance(1000);
        check("move".equals(G5NativeOperationsHarness.client.actor(100).get("orderType")),"later native recovery movement waypoint witness");
        check(registry.unit(100).healthRole==GeneralRegistry.HealthRole.RECOVERING,"receipt and waypoint do not invent HP restoration");
        G3NativeAcceptanceHarness.record("closure_native_recovery",G3NativeAcceptanceHarness.map("registry",registry.unit(100).metadata(),"laterOwn",G5NativeOperationsHarness.client.actor(100),"naturalHealing","NEEDS_EVIDENCE"));
        G5NativeOperationsHarness.client.close();
    }
    public static void main(String[] args){try{
        G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);G5NativeOperationsHarness.bootstrap();
        investments();recovery();Map<String,Object> result=G3NativeAcceptanceHarness.map("status","PASS","checks",checks,"evidence","E2_NATIVE_HTTP_QUEUE_WITNESSES","simulationTicks",0,"desktopTouched",false,"naturalHealing","NEEDS_EVIDENCE");
        Files.write(G5NativeOperationsHarness.out.resolve("g5-closure-native-summary.json"),G3NativeAcceptanceHarness.json(result).getBytes(StandardCharsets.UTF_8));G3NativeAcceptanceHarness.fixture.close();System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
