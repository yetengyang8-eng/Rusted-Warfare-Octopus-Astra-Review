import com.corrodinggames.rts.game.units.am;
import io.rwagent.client.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Explicit native HP/position/fog/clock fixture with real HTTP receipts and native command.k.
 * No simulation ticks, natural retreat movement, combat benefit or desktop acceptance claim. */
public final class G53NativeOrderReuseHarness {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static Map<String,Object> map(Object... pairs){return G5NativeOperationsHarness.map(pairs);}
    static void record(String event,Object data)throws Exception{G5NativeOperationsHarness.record(event,data);}
    static GeneralRegistry registry()throws Exception{return G5NativeOperationsHarness.registry();}
    static GeneralCombatDirector combat()throws Exception{return G5NativeOperationsHarness.combat();}
    static G3NativeAcceptanceHarness.Client client(){return G5NativeOperationsHarness.client;}
    static int bufferedMoves(Collection<Long> actors)throws Exception{
        int count=0;for(Map<String,Object> command:G3NativeAcceptanceHarness.commands())if("move".equals(command.get("waypoint")))
            for(Object id:(List<?>)command.get("actors"))if(actors.contains(((Number)id).longValue()))count++;
        return count;
    }
    static void observeAdvance(long delta)throws Exception{G5NativeOperationsHarness.advance(delta);}
    static void collectFlush()throws Exception{G5NativeOperationsHarness.collect();G5NativeOperationsHarness.flush();}
    static void checkNativeDestination(long actor,double x,double y)throws Exception{
        Map<String,Object> row=client().actor(actor);check("move".equals(row.get("orderType")),"later real /state exposes applied native move");
        check(row.get("orderX") instanceof Number&&row.get("orderY") instanceof Number,"later real /state provides actual native destination coordinates");
        check(Math.hypot(((Number)row.get("orderX")).doubleValue()-x,((Number)row.get("orderY")).doubleValue()-y)<1,"current native destination equals accepted owned move goal");
    }
    static void retreat()throws Exception{
        G3NativeAcceptanceHarness.base(6,0,40000);float[] dry=G3NativeAcceptanceHarness.dry();
        com.corrodinggames.rts.game.n opponent=G5NativeOperationsHarness.opponent();
        G3NativeAcceptanceHarness.unit("experimentalTank",900,opponent,dry[0]+130,dry[1]+150);
        G3NativeAcceptanceHarness.unit("experimentalTank",901,opponent,dry[0]+160,dry[1]+150);
        G5NativeOperationsHarness.client=new G3NativeAcceptanceHarness.Client("g53-native-retreat-reuse",160,false);G5NativeOperationsHarness.observe();
        check(registry().generals().size()==1,"six real ready original troops bootstrap one General");GeneralRegistry.GeneralView general=registry().generals().get(0);
        Map<Long,Long> generations=new LinkedHashMap<Long,Long>();for(Long actor:general.members){generations.put(actor,registry().unit(actor).ownerGeneration);G5NativeOperationsHarness.actor(actor).cu-=60;}
        record("fixture_owned_hp_delta",map("actors",general.members,"hpDecreasePerActor",60,"basis","EXPLICIT_NATIVE_HP_FIXTURE_NOT_NATURAL_DAMAGE"));
        observeAdvance(1000);check(combat().retreating(general.id),"actual later native own HP decline and legal visible pressure activate retreat");
        observeAdvance(5000);collectFlush();check(bufferedMoves(general.members)==6,"real HTTP/native queue receives one retreat move per actual owned actor");
        Map<String,Object> view=combat().view(general.id);double x=((Number)view.get("rallyX")).doubleValue(),y=((Number)view.get("rallyY")).doubleValue();
        int accepted=0;long maximumReceiptFrame=-1;
        for(Map<String,Object> row:G5NativeOperationsHarness.rows("g3_execution")){
            Map<String,Object> data=G3NativeAcceptanceHarness.packet(row.get("data"));if(!"GENERAL_RETREAT".equals(data.get("lane"))||!Boolean.TRUE.equals(data.get("nativeAccepted")))continue;
            Map<String,Object> receipt=G3NativeAcceptanceHarness.packet(data.get("receipt"));check("queued".equals(receipt.get("status"))&&receipt.get("requestId") instanceof String&&receipt.get("frame") instanceof Number,"real retreat receipt carries native request ID and frame");
            maximumReceiptFrame=Math.max(maximumReceiptFrame,((Number)receipt.get("frame")).longValue());accepted++;
        }
        check(accepted==6,"six actual HTTP retreat receipts are independently present in raw battle trace");
        G3NativeAcceptanceHarness.process();observeAdvance(20000);check(((Number)client().state.get("frame")).longValue()>maximumReceiptFrame,"current native observation strictly follows actual receipt frame");
        for(Long actor:general.members){checkNativeDestination(actor,x,y);check(registry().unit(actor).ownerGeneration==generations.get(actor)&&general.owner.equals(registry().unit(actor).owner),"current native retreat still has original actual owner generation");}
        float[] far=G5NativeOperationsHarness.distantDry(dry[0],dry[1]);am remote=G3NativeAcceptanceHarness.unit("tank",910,opponent,far[0],far[1]);
        record("fixture_remote_visible_enemy",map("id",910,"x",remote.eo,"y",remote.ep,"hp",remote.cu,"basis","EXPLICIT_ORIGINAL_NATIVE_SEED_AND_LEGAL_HTTP_OBSERVATION"));
        observeAdvance(1000);check(G5NativeOperationsHarness.enemy(910)!=null,"remote added target is legally current in real combat endpoint");collectFlush();
        check(combat().retreating(general.id)&&bufferedMoves(general.members)==0,"same native retreat goals do not refresh after over15s and new global visible enemy");
        remote.cu-=10;remote.eo+=20;record("fixture_remote_hp_position_delta",map("id",910,"hp",remote.cu,"x",remote.eo,"basis","EXPLICIT_NATIVE_FIELDS_NOT_NATURAL_DAMAGE_OR_MOTION"));
        observeAdvance(2000);collectFlush();check(bufferedMoves(general.members)==0,"global visible enemy HP and position changes add no duplicate same-rally move");
        record("g53_native_retreat_reuse",map("general",general.metadata(),"combat",combat().view(general.id),"nativeOwnWitness",client().actor(100),"nativeCurrentEnemy",G5NativeOperationsHarness.enemy(910),"actualHTTPReceipts",accepted,"maximumReceiptFrame",maximumReceiptFrame,"currentFrame",client().state.get("frame"),"duplicateRetreatMoves",bufferedMoves(general.members),"simulationTicks",0));client().close();
    }
    static void joining()throws Exception{
        G3NativeAcceptanceHarness.base(6,0,40000);G5NativeOperationsHarness.client=new G3NativeAcceptanceHarness.Client("g53-native-joining-reuse",160,false);G5NativeOperationsHarness.observe();
        GeneralRegistry.GeneralView general=registry().generals().get(0);float[] far=G5NativeOperationsHarness.distantDry((float)general.x,(float)general.y);
        G3NativeAcceptanceHarness.unit("heavyTank",500,G5NativeOperationsHarness.engine.bs,far[0],far[1]);observeAdvance(1000);observeAdvance(1000);
        check(registry().requestJoin(500,general.id)&&registry().beginJoining(500),"real current native FREE actor enters explicit formal JOINING owner");
        long generation=registry().unit(500).ownerGeneration;observeAdvance(5000);collectFlush();check(bufferedMoves(Collections.singletonList(500L))==1,"actual joining actor receives one native HTTP move");
        GeneralRegistry.GeneralView target=registry().general(general.id);double x=target.joinTargetX,y=target.joinTargetY;long receiptFrame=registry().unit(500).joinAcceptedFrame;
        check(receiptFrame>=0,"JOINING registry records actual accepted native receipt frame");G3NativeAcceptanceHarness.process();observeAdvance(20000);
        check(((Number)client().state.get("frame")).longValue()>receiptFrame,"JOINING same-goal witness strictly follows actual native receipt frame");checkNativeDestination(500,x,y);
        check(registry().unit(500).membership==GeneralRegistry.Membership.JOINING&&registry().unit(500).ownerGeneration==generation,"no simulation movement or receipt invents JOINING arrival or owner transfer");collectFlush();
        check(bufferedMoves(Collections.singletonList(500L))==0,"real current same-destination joining move is reused after ordinary cooldown");
        record("g53_native_joining_reuse",map("unit",registry().unit(500).metadata(),"nativeOwnWitness",client().actor(500),"actualReceiptFrame",receiptFrame,"currentFrame",client().state.get("frame"),"duplicateJoiningMoves",bufferedMoves(Collections.singletonList(500L)),"simulationTicks",0));client().close();
    }
    public static void main(String[] args){try{
        if(args.length!=1)throw new IllegalArgumentException("OUTPUT_DIRECTORY");G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);G5NativeOperationsHarness.bootstrap();retreat();joining();
        Map<String,Object> result=map("status","PASS","checks",checks,"evidence","E2_NATIVE_FIXTURE_REAL_HTTP_RECEIPTS_AND_NATIVE_COMMAND_K","simulationTicks",0,"desktopTouched",false,"naturalMovementOrCombatBenefitProven",false,"retreatReuse","PASS","joiningReuse","PASS");
        Files.write(G5NativeOperationsHarness.out.resolve("g53-native-order-reuse-summary.json"),G3NativeAcceptanceHarness.json(result).getBytes(StandardCharsets.UTF_8));record("summary",result);G3NativeAcceptanceHarness.fixture.close();System.out.println("G53NativeOrderReuseHarness checks="+checks+" PASS simulationTicks=0");System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();try{Files.write(G5NativeOperationsHarness.out.resolve("g53-native-order-reuse-summary.json"),G3NativeAcceptanceHarness.json(map("status","FAIL","checks",checks,"failure",failure.toString(),"simulationTicks",0,"naturalMovementOrCombatBenefitProven",false)).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}System.exit(1);}}
}
