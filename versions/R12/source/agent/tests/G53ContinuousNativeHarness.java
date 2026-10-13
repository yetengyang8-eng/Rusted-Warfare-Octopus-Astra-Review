import io.rwagent.client.*;
import java.nio.file.*;
import java.util.*;

/** Original engine fixture, real HTTP/admission/payment. No natural production completion claim. */
public final class G53ContinuousNativeHarness {
    static int checks;
    static void require(boolean v,String reason){checks++;if(!v)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);
        G5NativeOperationsHarness.bootstrap();
        G3NativeAcceptanceHarness.base(48,4,100000);
        G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("g53-continuous-above-target",100,true);
        require(Boolean.TRUE.equals(G3NativeAcceptanceHarness.value(c.battle,"g5Enabled")),"real G5 path enabled");
        c.advance(1000,true);G3NativeAcceptanceHarness.set(c.strategy,"armyTarget",12);c.produce();
        require(G3NativeAcceptanceHarness.engine.cf.b.size()==4,"four original factories work despite48 armed above fixture organization12");
        require(c.scheduler.availableMilitarySlots()==48,"organization target does not reduce52 native-policy slots;4 real commitments deducted");
        require(c.scheduler.effectiveCredits()==96800,"four native800 costs held before visibility");
        Set<Long> producers=new HashSet<Long>();
        for(Map<String,Object> command:G3NativeAcceptanceHarness.commands())for(Object id:(List<?>)command.get("actors"))require(producers.add(((Number)id).longValue()),"no duplicate producer");
        c.advance(1000,true);c.produce();require(G3NativeAcceptanceHarness.engine.cf.b.size()==4,"paid-before-visible producers not reused");
        require(c.scheduler.effectiveCredits()==96800,"later clock cannot release money without native effect");
        G3NativeAcceptanceHarness.process();c.advance(1000,true);
        int queued=0;for(Map<String,Object> own:G3NativeAcceptanceHarness.items(c.state,"ownUnits"))if("landFactory".equals(own.get("type"))&&((Number)own.get("productionQueue")).intValue()>0)queued++;
        require(queued==4,"later original queue witness proves four commands took effect");
        require(G3NativeAcceptanceHarness.engine.bs.o==96800,"original native payment agrees with quotes");
        G3NativeAcceptanceHarness.record("g53_above_organization_target",G3NativeAcceptanceHarness.map("ledger",c.ledger(),"queuedFactories",queued,"armedSeed",48,"organizationTargetAtOrder",12,"simulationTicks",0,"naturalProductCompletion",false));c.close();
        G3NativeAcceptanceHarness.base(48,4,100000);
        System.setProperty("rwagent.allowSmallTanks","true"); // Separate legacy fallback variant, not candidate default.
        G3NativeAcceptanceHarness.Client protectedClient=new G3NativeAcceptanceHarness.Client("g53-explicit-commitment",100,true);
        G3NativeAcceptanceHarness.set(protectedClient.battle,"builderReserve",99500L);protectedClient.advance(1000,true);protectedClient.produce();
        require(G3NativeAcceptanceHarness.engine.cf.b.size()==1,"explicit99500 commitment permits one affordable cheap unit without primary savings stop");
        require(protectedClient.scheduler.effectiveCredits()==99650,"native350 fallback protects aggregate chosen funds");
        G3NativeAcceptanceHarness.process();protectedClient.advance(1000,true);
        require(G3NativeAcceptanceHarness.engine.bs.o==99650,"original native cheap product actually paid350");
        G3NativeAcceptanceHarness.record("g53_continuous_protects_chosen_commitment",G3NativeAcceptanceHarness.map("ledger",protectedClient.ledger(),"chosenProtectedFunds",99500,"policy","CONTINUOUS_NOT_UNCOMMITTED_PRIMARY_BANKING"));protectedClient.close();
        G3NativeAcceptanceHarness.fixture.close();System.out.println("G53_CONTINUOUS_NATIVE_OK checks="+checks+" simulationTicks=0 fixtureOnly=true");System.exit(0);
    }
}
