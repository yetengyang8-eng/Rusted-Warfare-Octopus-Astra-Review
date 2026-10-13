import com.corrodinggames.rts.game.units.*;
import io.rwagent.client.*;
import java.nio.file.*;
import java.net.URLEncoder;
import java.util.*;

/** Original native menus, command dispatcher and own-order/mode witnesses.
 * Explicit seeded units, clock, fog and height controls; no natural completion or victory claim. */
public final class G6NativeEndgameHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<String,Object> map(Object... pairs){return G3NativeAcceptanceHarness.map(pairs);}
    static void record(String event,Object data)throws Exception{G3NativeAcceptanceHarness.record(event,data);}
    static double n(Map<String,Object> m,String key){Object v=m.get(key);return v instanceof Number?((Number)v).doubleValue():Double.NaN;}
    static String enc(Object value)throws Exception{return URLEncoder.encode(String.valueOf(value),"UTF-8");}
    static Map<String,Object> order(G3NativeAcceptanceHarness.Client c,String owner,String path,Map<String,Object> quote,String product)throws Exception{
        G3NativeAcceptanceHarness.set(c.battle,"executingLane","STRATEGY");
        Map<String,Object> context=new LinkedHashMap<String,Object>(c.battle.costSourceStrategy(quote));
        context.putAll(map("product",product,"cost",n(quote,"cost"),"producerSlots",1,
            "constructionSlotAvailable",1,"investmentSlotAvailable",1,"slotEvidenceSource","ISOLATED_NO_OTHER_FIXTURE_COMMITMENT",
            "commitmentId","g6-native:"+c.label+":"+c.arbiter.stamp().frame+":"+quote.get("unitId")));
        return c.battle.orderStrategy(owner,path,context);
    }
    static void overlappingAirConstruction()throws Exception{
        G3NativeAcceptanceHarness.base(8,1,100000);float[] p=G3NativeAcceptanceHarness.dry();
        y first=(y)G3NativeAcceptanceHarness.unit("combatEngineer",91,G3NativeAcceptanceHarness.engine.bs,p[0]+200,p[1]+200);
        y second=(y)G3NativeAcceptanceHarness.unit("combatEngineer",92,G3NativeAcceptanceHarness.engine.bs,p[0]+200,p[1]+240);
        G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("g6-overlapping-air-cost",256,false);c.advance(1000,false);
        Map<String,Object> a=c.read("/economy/construction-plan?unitId=91&type=amphibiousJet");
        Map<String,Object> b=c.read("/economy/construction-plan?unitId=92&type=amphibiousJet");
        check("amphibiousJet".equals(a.get("type"))&&"amphibiousJet".equals(b.get("type")),"two original engineer menus expose real jets");
        check(Math.hypot(n(a,"x")-n(b,"x"),n(a,"y")-n(b,"y"))<120,"real two engineer sites overlap conservative association envelopes");
        for(Map<String,Object> q:Arrays.asList(a,b)){
            long actor=((Number)q.get("unitId")).longValue();
            check(order(c,CommandArbiter.DEFAULT_OWNER,"/command/construct?unitId="+actor+"&actionId="+enc(q.get("actionId"))+"&x="+n(q,"x")+"&y="+n(q,"y"),q,"amphibiousJet")!=null,"native jet receipt accepted independently");
        }
        long total=(long)(n(a,"cost")+n(b,"cost"));check(c.scheduler.unsettledCreditTotal()==total,"both receipts remain paid-before-visible obligations");
        G3NativeAcceptanceHarness.process();c.advance(1000,false);
        check(c.scheduler.unsettledCreditTotal()==total,"real build orders alone do not settle spending");
        double cash=G3NativeAcceptanceHarness.engine.bs.o;
        au wa=first.ar();Object ra=first.a(wa,wa.a(),1,wa.g(),wa.h());am sa=(am)G3NativeAcceptanceHarness.value(ra,"a");
        check(sa!=null&&sa.cm<1,"original native helper creates first unfinished jet site");
        c.advance(1000,false);check(c.scheduler.unsettledCreditTotal()==total,"one actual site cannot pay for two overlapping obligations");
        au wb=second.ar();Object rb=second.a(wb,wb.a(),1,wb.g(),wb.h());am sb=(am)G3NativeAcceptanceHarness.value(rb,"a");
        check(sb!=null&&sb.eh!=sa.eh&&sb.cm<1,"second native helper creates independent unfinished jet site");
        check(Math.abs(cash-G3NativeAcceptanceHarness.engine.bs.o-total)<.01,"two original site helpers pay exact native invoices");
        c.advance(1000,false);check(c.scheduler.unsettledCreditCount()==0,"two distinct later own site effects settle balanced group without producer attribution");
        check(c.scheduler.effectiveCredits()==(long)G3NativeAcceptanceHarness.engine.bs.o,"later observed cash no longer double deducts group invoices");
        record("g6_overlapping_native_construction_effects",map("siteA",c.actor(sa.eh),"siteB",c.actor(sb.eh),"ledger",c.ledger(),"nativeCostTotal",total,"individualProducerAttribution",false,"simulationTicks",0));c.close();
    }
    static void fabricatorBuild()throws Exception{
        G3NativeAcceptanceHarness.base(8,1,100000);
        G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("g6-fabricator-build",256,false);
        c.advance(1000,false);
        Map<String,Object> plan=c.read("/economy/construction-plan?unitId=2&type=fabricatorT1");
        record("g6_fabricator_native_quote",plan);
        check(plan!=null&&"fabricatorT1".equals(plan.get("type"))&&n(plan,"cost")>0&&plan.get("actionId") instanceof String,
            "actual native Builder exposes priced legal fabricatorT1 construction");
        Map<String,Object> receipt=order(c,CommandArbiter.DEFAULT_OWNER,"/command/construct?unitId=2&actionId="+enc(plan.get("actionId"))+"&x="+n(plan,"x")+"&y="+n(plan,"y"),plan,"fabricatorT1");
        check(receipt!=null&&"fabricatorT1".equals(receipt.get("type")),"native fab construction command accepted through real scheduler");
        check(G3NativeAcceptanceHarness.commands().stream().anyMatch(x->"fabricatorT1".equals(x.get("nativeProduct"))),"actual engine buffer holds native fab build waypoint");
        G3NativeAcceptanceHarness.process();c.advance(1000,false);
        check("build".equals(c.actor(2).get("orderType")),"later native own order witnesses Builder fab construction");
        y builder=(y)G5NativeOperationsHarness.actor(2);au waypoint=builder.ar();double credits=G3NativeAcceptanceHarness.engine.bs.o;
        Object nativeResult=builder.a(waypoint,waypoint.a(),1,waypoint.g(),waypoint.h());
        am site=(am)G3NativeAcceptanceHarness.value(nativeResult,"a");
        check(site!=null&&"fabricatorT1".equals(site.r().i())&&site.cm<1,"original guarded build helper initializes actual unfinished fab site");
        check(Math.abs(credits-G3NativeAcceptanceHarness.engine.bs.o-n(plan,"cost"))<.01,"original native site helper pays actual quoted fab cost");
        record("fixture_fabricator_site_initialization",map("product",site.r().i(),"unitId",site.eh,"buildProgress",site.cm,"nativeCost",n(plan,"cost"),
            "creditsBefore",credits,"creditsAfter",G3NativeAcceptanceHarness.engine.bs.o,"method","ORIGINAL_Y_A_AU_AS_INT_FLOAT_FLOAT",
            "simulationTicks",0,"naturalConstructionCompletionProven",false));
        c.advance(1000,false);
        check("fabricatorT1".equals(c.actor(site.eh).get("type"))&&n(c.actor(site.eh),"buildProgress")<1,"later real state witnesses unfinished fab site without claiming completion");
        c.close();
    }
    static void fabricatorUpgrade(String from,String to,long id)throws Exception{
        G3NativeAcceptanceHarness.base(8,1,100000);float[] p=G3NativeAcceptanceHarness.dry();
        G3NativeAcceptanceHarness.unit(from,id,G3NativeAcceptanceHarness.engine.bs,p[0]+300,p[1]+250);
        record("fixture_ready_fabricator_seed",map("type",from,"unitId",id,"completionBasis","EXPLICIT_READY_NATIVE_UNIT_SEED_NOT_NATURAL_UPGRADE"));
        G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("g6-upgrade-"+from,256,false);c.advance(1000,false);
        Map<String,Object> menu=c.read("/economy/investments"),quote=null;
        for(Map<String,Object> item:G3NativeAcceptanceHarness.items(menu,"units"))if(n(item,"id")==id&&to.equals(item.get("product")))quote=item;
        check(quote!=null&&n(quote,"cost")>0&&n(quote,"queue")==0,"actual native "+from+" exposes priced "+to+" menu");
        record("g6_fabricator_upgrade_quote",map("from",from,"to",to,"quote",quote,"source",c.battle.costSourceStrategy(menu)));
        Map<String,Object> context=new LinkedHashMap<String,Object>(c.battle.costSourceStrategy(menu));
        context.putAll(map("product",to,"cost",n(quote,"cost"),"producerSlots",1,"investmentSlotAvailable",1,
            "commitmentId","g6-native-upgrade:"+from+":"+c.arbiter.stamp().frame));
        G3NativeAcceptanceHarness.set(c.battle,"executingLane","STRATEGY");
        Map<String,Object> receipt=c.battle.orderStrategy(CommandArbiter.DEFAULT_OWNER,"/command/invest?unitId="+id+"&actionId="+enc(quote.get("actionId")),context);
        check(receipt!=null&&to.equals(receipt.get("type")),"native "+from+" to "+to+" command accepted");
        check(G3NativeAcceptanceHarness.commands().stream().anyMatch(x->quoteAction(x,receipt)),"original native upgrade action is buffered");
        double credits=G3NativeAcceptanceHarness.engine.bs.o;G3NativeAcceptanceHarness.process();
        check(Math.abs(credits-G3NativeAcceptanceHarness.engine.bs.o-n(quote,"cost"))<.01,"actual native upgrade queue pays quoted "+to+" cost");
        c.advance(1000,false);check(n(c.actor(id),"productionQueue")==1,"later native state witnesses actual "+to+" upgrade queue");
        record("g6_fabricator_upgrade_queue_witness",map("actor",c.actor(id),"receipt",receipt,"nativeCost",n(quote,"cost"),"naturalUpgradeCompletionProven",false));c.close();
    }
    static boolean quoteAction(Map<String,Object> command,Map<String,Object> receipt){return Objects.equals(command.get("nativeAction"),receipt.get("actionId"));}
    static void airOwnershipMoveAndMode()throws Exception{
        G3NativeAcceptanceHarness.base(8,1,100000);float[] coast=G3NativeAcceptanceHarness.coast(),dry=G3NativeAcceptanceHarness.dry();
        y jet=(y)G3NativeAcceptanceHarness.unit("amphibiousJet",81,G3NativeAcceptanceHarness.engine.bs,coast[0],coast[1]);
        G3NativeAcceptanceHarness.Client c=new G3NativeAcceptanceHarness.Client("g6-air-formation",256,false);
        Object air=G3NativeAcceptanceHarness.value(c.strategy,"airGeneral");String prior="strategy:air-native-fixture",owner="air-general:1";
        check(c.arbiter.claim(prior,81),"current real own jet admits existing specialist lease");
        Map<Long,Long> previous=c.arbiter.snapshotGenerations(prior,Arrays.asList(81L));CapabilityUnitMode mode=new CapabilityUnitMode(81);
        check(Boolean.TRUE.equals(G3NativeAcceptanceHarness.invoke(air,"accept",new Class<?>[]{long.class,String.class,CapabilityUnitMode.class},81L,prior,mode)),"real Air General accepts independent specialist transfer");
        check(c.arbiter.owns(owner,81)&&!c.arbiter.owns(prior,81),"air formation owns exact actor outside ground General ownership");
        check("STALE_OWNER_GENERATION".equals(c.arbiter.validateGenerations(Arrays.asList(81L),previous)),"prior specialist intent generation becomes stale");
        G3NativeAcceptanceHarness.invoke(air,"observe",new Class<?>[]{Map.class,Map.class,Map.class,double.class,double.class},c.state,c.enemies,c.scout,(double)dry[0],(double)dry[1]);
        Map<String,Object> receipt=c.battle.orderStrategy(owner,"/command/move?unitId=81&x="+(coast[0]+40)+"&y="+coast[1],map("unitId",81L,"reason","G6_NATIVE_ISOLATED_MOVE_PROTOCOL"));
        check(receipt!=null,"independent Air General owner can issue actual native MOVE");
        G3NativeAcceptanceHarness.process();c.advance(1000,false);
        check("move".equals(c.actor(81).get("orderType")),"later native own order witnesses real Air MOVE without claiming arrival");
        jet.eo=dry[0];jet.ep=dry[1];record("fixture_air_join_arrival",map("unitId",81,"x",jet.eo,"y",jet.ep,"basis","EXPLICIT_POST_RECEIPT_POSITION_NO_MOVEMENT_TICKS"));
        c.advance(1000,false);G3NativeAcceptanceHarness.invoke(air,"observe",new Class<?>[]{Map.class,Map.class,Map.class,double.class,double.class},c.state,c.enemies,c.scout,(double)dry[0],(double)dry[1]);
        Map<String,Object> snapshot=G3NativeAcceptanceHarness.packet(G3NativeAcceptanceHarness.invoke(air,"snapshot",new Class<?>[0]));
        check("ATTACHED".equals(G3NativeAcceptanceHarness.items(snapshot,"roster").get(0).get("membership")),"later real own-position snapshot attaches transferred Air member");
        jet.eo=coast[0];jet.ep=coast[1];jet.eq=20;
        com.corrodinggames.rts.game.n opponent=G5NativeOperationsHarness.opponent();am target=G3NativeAcceptanceHarness.unit("amphibiousJet",230,opponent,coast[0],coast[1]);target.eq=-5;
        record("fixture_submerged_target_seed",map("unitId",230,"type","amphibiousJet","height",-5,"fog","CONTROLLED_ALL_VISIBLE","naturalDiscoveryProven",false));
        c.advance(1000,false);G3NativeAcceptanceHarness.invoke(air,"observe",new Class<?>[]{Map.class,Map.class,Map.class,double.class,double.class},c.state,c.enemies,c.scout,(double)dry[0],(double)dry[1]);
        check(Boolean.TRUE.equals(G3NativeAcceptanceHarness.invoke(air,"act",new Class<?>[0])),"real Air controller consumes current native submerged engagement");
        check(mode.phase()==CapabilityUnitMode.Phase.DIVE_REQUESTED&&G3NativeAcceptanceHarness.commands().stream().anyMatch(x->"152".equals(x.get("nativeAction"))),"Air member independently requests actual native Dive action");
        G3NativeAcceptanceHarness.process();check(!jet.ae(),"native Dive dispatch sets desired mode without claiming physical submerged readiness");
        c.advance(1000,false);G3NativeAcceptanceHarness.invoke(air,"observe",new Class<?>[]{Map.class,Map.class,Map.class,double.class,double.class},c.state,c.enemies,c.scout,(double)dry[0],(double)dry[1]);
        G3NativeAcceptanceHarness.invoke(air,"act",new Class<?>[0]);
        check(G3NativeAcceptanceHarness.commands().stream().noneMatch(x->"151".equals(x.get("nativeAction"))),"current submarine task waits for later Dive witness instead of canceling to Fly");
        jet.eq=-5;record("fixture_air_dive_height",map("unitId",81,"height",-5,"basis","EXPLICIT_NATIVE_HEIGHT_NO_DIVE_SIMULATION_TICKS"));
        c.advance(1000,false);G3NativeAcceptanceHarness.invoke(air,"observe",new Class<?>[]{Map.class,Map.class,Map.class,double.class,double.class},c.state,c.enemies,c.scout,(double)dry[0],(double)dry[1]);
        G3NativeAcceptanceHarness.invoke(air,"act",new Class<?>[0]);
        check("SUBMERGED".equals(mode.nativeMode()),"Air member physical phase follows independent original own-mode witness");
        record("g6_air_native_mode_witness",map("mode",mode.snapshot(),"commands",G3NativeAcceptanceHarness.commands(),"ownerGeneration",c.arbiter.ownerGeneration(81),"naturalModeCompletionProven",false));
        G3NativeAcceptanceHarness.process();G3NativeAcceptanceHarness.invoke(air,"close",new Class<?>[0]);check(!c.arbiter.reserved(81),"Air shutdown releases exact actor lease");c.close();
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.g6","true");G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);G5NativeOperationsHarness.bootstrap();
        record("g6_scope",map("g6",true,"simulationTicks",0,"artificialUnitSeeds",true,"artificialClockFogPositionHeight",true,"naturalMatch",false,"desktopTouched",false,"nativeVictoryProven",false));
        overlappingAirConstruction();fabricatorBuild();fabricatorUpgrade("fabricatorT1","fabricatorT2",301);fabricatorUpgrade("fabricatorT2","fabricatorT3",302);airOwnershipMoveAndMode();
        G3NativeAcceptanceHarness.fixture.close();System.out.println("G6_NATIVE_ENDGAME_OK checks="+checks+" simulationTicks=0 fixtureOnly=true nativeVictoryProven=false");System.exit(0);
    }
}
