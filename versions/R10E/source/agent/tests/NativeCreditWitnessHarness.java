package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Focused legal own-state evidence for the cross-observation credit ledger. */
public final class NativeCreditWitnessHarness {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Map<String,Object> unit(long id,String type,int queue,int tier,double progress,double x,double y){
        return map("id",id,"type",type,"productionQueue",queue,"techLevel",tier,"buildProgress",progress,"x",x,"y",y,"dead",false);
    }
    private static final class Fixture {
        final CommandArbiter arbiter=new CommandArbiter();
        final ExecutionScheduler scheduler=new ExecutionScheduler(arbiter);
        final NativeCreditWitness witness=new NativeCreditWitness();
        final List<Map<String,Object>> units=new ArrayList<Map<String,Object>>();
        final Map<String,Object> state=map("sessionId","s","status","running","frame",10L,"gameTimeMs",2500L,"player",map("teamId",0,"credits",1600L),"ownUnits",units);
        Fixture(){this(1600L);}
        Fixture(long credits){BattleClient.obj(state.get("player")).put("credits",credits);units.add(unit(1,"landFactory",0,1,1,100,200));units.add(unit(2,"builder",-1,1,1,100,200));
            arbiter.observe(new CommandArbiter.Stamp("s","team:0",0,0),Arrays.asList(1L,2L));
            arbiter.observe(stamp(),Arrays.asList(1L,2L));scheduler.beginObservation(stamp(),"o:10",credits,Collections.<Long,Integer>emptyMap(),10);}
        CommandArbiter.Stamp stamp(){return new CommandArbiter.Stamp("s","team:0",((Number)state.get("frame")).longValue(),((Number)state.get("gameTimeMs")).longValue());}
        void hold(String id,long actor,String kind,String path,String product,boolean unknown){
            Intent intent=new Intent(id,CommandArbiter.DEFAULT_OWNER,arbiter.snapshotGenerations(CommandArbiter.DEFAULT_OWNER,Arrays.asList(actor)),Arrays.asList(actor),kind,"TEST",1,stamp(),"quote:o","/combat/production",path,Intent.Commitment.spending(800L,Collections.<Long,Integer>emptyMap(),0),"quote:o","/combat/production");
            Map<String,Object> receipt=unknown?null:map("status","queued","frame",20L);
            ExecutionScheduler.Result result=scheduler.dispatch(intent,i->receipt);
            check(result.commitmentApplied,"accepted or transport unknown retains credit obligation");witness.accepted(intent,result.receipt,map("product",product),state);
        }
        void queue(boolean unknown){hold("q",1,"/command/queue","/command/queue?unitId=1&actionId=tank","tank",unknown);}
        void construction(String path){hold("c",2,"/command/construct",path,"landFactory",false);}
        List<Map<String,Object>> observe(long frame,long time){state.put("frame",frame);state.put("gameTimeMs",time);return witness.observe(stamp(),"o:"+frame,state,scheduler);}
        void noEffect(String why){check(observe(21,3500).isEmpty(),why);check(scheduler.unsettledCreditTotal()==800,"unproven effect retains exact obligation");}
        void build(){units.get(1).put("orderType","build");units.get(1).put("orderX",100);units.get(1).put("orderY",200);}
        void candidate(long id,double progress){units.add(unit(id,"landFactory",-1,1,progress,100,200));}
        void refresh(){List<Long> ids=new ArrayList<Long>();for(Map<String,Object> u:units)ids.add(BattleClient.id(u));arbiter.observe(stamp(),ids);scheduler.beginObservation(stamp(),"o:refresh",((Number)BattleClient.obj(state.get("player")).get("credits")).longValue(),Collections.<Long,Integer>emptyMap(),10);}
    }
    private static void queuesAndTime(){
        Fixture f=new Fixture();f.queue(false);check(f.scheduler.effectiveCredits()==800,"receipt debits local ledger before native cash");
        f.units.get(0).put("productionQueue",1);check(f.observe(20,3500).isEmpty(),"receipt frame is not a later native effect");
        check(f.observe(21,2000).isEmpty(),"earlier game clock cannot settle");
        List<Map<String,Object>> rows=f.observe(21,3500);check(rows.size()==1,"later empty-to-nonempty queue settles exact intent");
        check("NATIVE_QUEUE_NONEMPTY_NOT_PRODUCT_READY".equals(rows.get(0).get("witnessKind")),"queue witness is not ready product");
        check(Boolean.FALSE.equals(rows.get(0).get("productReady")),"settlement explicitly rejects readiness claim");
        check(f.scheduler.unsettledCreditCount()==0&&f.scheduler.effectiveCredits()==800,"witness removes hold without current-batch refund");
        check(f.observe(22,3500).isEmpty(),"repeated native effect does not settle twice");f.refresh();check(f.scheduler.effectiveCredits()==1600,"next legal begin refreshes settled funds");
        Fixture sameTime=new Fixture();sameTime.queue(false);sameTime.units.get(0).put("productionQueue",1);
        check(sameTime.observe(21,2500).size()==1,"later frame at same game time is legal native evidence");sameTime.refresh();check(sameTime.scheduler.effectiveCredits()==800,"same-time begin cannot refill settled current batch");
        Fixture unknown=new Fixture();unknown.queue(true);unknown.noEffect("transport unknown receipt and elapsed clock do not settle");
        unknown.units.get(0).put("productionQueue",1);check(unknown.observe(22,4000).size()==1,"unknown transport may settle only by later native queue effect");
        Fixture wallet=new Fixture();wallet.queue(false);BattleClient.obj(wallet.state.get("player")).put("credits",0L);wallet.noEffect("wallet drop is not attributable native effect");
        Fixture missingQueue=new Fixture();missingQueue.units.get(0).remove("productionQueue");missingQueue.queue(false);missingQueue.units.get(0).put("productionQueue",1);missingQueue.noEffect("unknown baseline queue is not empty baseline");
        Fixture busy=new Fixture();busy.units.get(0).put("productionQueue",1);busy.queue(false);busy.units.get(0).put("productionQueue",2);busy.noEffect("already nonempty queue increase has no unique lineage");
        Fixture vanished=new Fixture();vanished.queue(false);vanished.units.remove(0);vanished.noEffect("missing actor retains hold");
        Fixture dead=new Fixture();dead.queue(false);dead.units.get(0).put("productionQueue",1);dead.units.get(0).put("dead",true);dead.noEffect("dead actor is not live queue witness");
    }
    private static void identity(){
        for(String field:Arrays.asList("sessionId","player","frame","gameTimeMs","status","networked","replay")){
            Fixture f=new Fixture();f.queue(false);f.units.get(0).put("productionQueue",1);f.state.put("frame",21L);f.state.put("gameTimeMs",3500L);
            CommandArbiter.Stamp legal=f.stamp();
            f.state.put(field,field.equals("sessionId")?"foreign":field.equals("player")?map("teamId",1):field.equals("status")?"ended":field.equals("frame")?999L:field.equals("gameTimeMs")?9999L:true);
            check(f.witness.observe(legal,"o:legal",f.state,f.scheduler).isEmpty(),"foreign or illegal own-state field cannot settle: "+field);
            check(f.scheduler.unsettledCreditCount()==1,"illegal packet retains obligation: "+field);
        }
        Fixture f=new Fixture();f.queue(false);f.units.get(0).put("productionQueue",1);f.state.put("frame",21L);
        check(f.witness.observe(new CommandArbiter.Stamp("s","team:1",21,2500),"o:foreign",f.state,f.scheduler).isEmpty(),"foreign player stamp cannot settle");
        check(f.witness.observe(new CommandArbiter.Stamp("foreign","team:0",21,2500),"o:foreign",f.state,f.scheduler).isEmpty(),"foreign session stamp cannot settle");
        check(f.witness.observe(f.stamp()," ",f.state,f.scheduler).isEmpty(),"missing witness observation identity retains hold");
    }
    private static void investments(){
        Fixture converted=new Fixture();converted.units.get(0).put("type","extractorT3");converted.units.get(0).put("techLevel",3);
        converted.hold("same-tier",1,"/command/invest","/command/invest?unitId=1&actionId=reinforce","extractorT3_reinforced",false);
        converted.units.get(0).put("type","extractorT3_reinforced");check(converted.observe(20,3500).isEmpty(),"same receipt frame conversion cannot settle");
        converted.state.put("sessionId","foreign");check(converted.observe(21,3500).isEmpty(),"foreign same-tier conversion cannot settle");converted.state.put("sessionId","s");
        List<Map<String,Object>> conversion=converted.observe(21,3500);check(conversion.size()==1&&"NATIVE_OWN_CONVERSION_OBSERVED".equals(conversion.get(0).get("witnessKind")),"later same-ID exact native product settles same-tier investment without queue witness");
        check(converted.scheduler.unsettledCreditCount()==0&&converted.observe(22,4000).isEmpty(),"conversion releases hold once, not wallet credit twice");
        Fixture wrong=new Fixture();wrong.units.get(0).put("type","extractorT3");wrong.units.get(0).put("techLevel",3);wrong.hold("same-tier",1,"/command/invest","/command/invest?unitId=1&actionId=reinforce","extractorT3_reinforced",false);
        wrong.units.get(0).put("type","extractorT3_overclocked");wrong.noEffect("unrelated same-tier product does not settle held branch");
        Fixture f=new Fixture();f.hold("i",1,"/command/invest","/command/invest?unitId=1&actionId=tier2","landFactory",false);f.units.get(0).put("techLevel",2);
        check("NATIVE_TIER_ADVANCED".equals(f.observe(21,3500).get(0).get("witnessKind")),"native later tier increase settles investment");
        Fixture unknown=new Fixture();unknown.units.get(0).remove("techLevel");unknown.hold("i",1,"/command/invest","/command/invest?unitId=1&actionId=tier2","landFactory",false);unknown.units.get(0).put("techLevel",2);unknown.noEffect("unknown baseline tier cannot prove increase");
        Fixture missing=new Fixture();missing.units.remove(0);missing.hold("i",1,"/command/invest","/command/invest?unitId=1&actionId=tier2","landFactory",false);missing.units.add(unit(1,"landFactory",1,2,1,100,200));missing.noEffect("actor absent in baseline cannot prove native effect");
        Fixture lower=new Fixture();lower.hold("i",1,"/command/invest","/command/invest?unitId=1&actionId=tier2","landFactory",false);lower.units.get(0).put("techLevel",0);lower.noEffect("tier decrease is not investment settlement");
    }
    private static void constructions(){
        String path="/command/construct?unitId=2&actionId=factory&x=100&y=200";
        Fixture f=new Fixture();f.construction(path);f.build();f.candidate(3,.1);List<Map<String,Object>> rows=f.observe(21,3500);
        check(rows.size()==1&&"NATIVE_CONSTRUCTION_SITE_EFFECT_NOT_PRODUCER_LINEAGE".equals(rows.get(0).get("witnessKind")),"unique new product in site envelope with builder build order settles without lineage claim");
        Fixture progress=new Fixture();progress.candidate(3,.1);progress.construction(path);progress.build();progress.units.get(2).put("buildProgress",.2);check(progress.observe(21,3500).size()==1,"unique baseline candidate progress increase settles construction");
        Fixture unchanged=new Fixture();unchanged.candidate(3,.1);unchanged.construction(path);unchanged.build();unchanged.noEffect("unchanged candidate supplies no effect");
        Fixture noOrder=new Fixture();noOrder.construction(path);noOrder.candidate(3,.1);check(noOrder.observe(21,3500).size()==1,"later unique own site effect settles even after builder becomes idle");
        Fixture originalBridgeShape=new Fixture();originalBridgeShape.construction(path);originalBridgeShape.units.get(1).put("orderType","build");originalBridgeShape.candidate(3,.1);
        check(originalBridgeShape.observe(21,3500).size()==1,"own site effect does not require unavailable native order coordinates");
        Fixture nextJob=new Fixture();nextJob.construction(path);nextJob.units.get(1).put("orderType","repair");nextJob.candidate(3,.1);check(nextJob.observe(21,3500).size()==1,"native repair order does not hide a real construction effect");
        Fixture wrongSite=new Fixture();wrongSite.construction(path);wrongSite.candidate(3,.1);wrongSite.units.get(2).put("x",1000);wrongSite.noEffect("a new building at another site cannot settle construction");
        Fixture ambiguous=new Fixture();ambiguous.construction(path);ambiguous.build();ambiguous.candidate(3,.1);ambiguous.candidate(4,.1);ambiguous.noEffect("multiple new product candidates retain hold");
        Fixture twoBuilders=new Fixture();twoBuilders.construction(path);twoBuilders.hold("c2",1,"/command/construct","/command/construct?unitId=1&actionId=factory&x=100&y=200","landFactory",false);twoBuilders.build();twoBuilders.units.get(0).putAll(map("orderType","build","orderX",100,"orderY",200));twoBuilders.candidate(3,.1);
        check(twoBuilders.observe(21,3500).isEmpty()&&twoBuilders.scheduler.unsettledCreditCount()==2,"one site effect cannot settle overlapping intents from different actors");
        Fixture twoActorHolds=new Fixture();twoActorHolds.queue(false);twoActorHolds.state.put("frame",11L);twoActorHolds.state.put("gameTimeMs",3500L);twoActorHolds.refresh();twoActorHolds.hold("q2",1,"/command/queue","/command/queue?unitId=1&actionId=tank","tank",false);twoActorHolds.units.get(0).put("productionQueue",1);
        check(twoActorHolds.observe(21,4000).isEmpty()&&twoActorHolds.scheduler.unsettledCreditCount()==2,"two unresolved actor queue orders remain ambiguous independent of iteration order");
        for(String bad:Arrays.asList("&x=100","&x=NaN&y=200","&x=100&y=Infinity","&x=garbage&y=200")){
            Fixture badSite=new Fixture();badSite.construction("/command/construct?unitId=2&actionId=factory"+bad);badSite.build();badSite.candidate(3,.1);badSite.noEffect("incomplete or invalid site remains unknown: "+bad);}
        Fixture unknownProgress=new Fixture();unknownProgress.construction(path);unknownProgress.build();unknownProgress.candidate(3,-1);unknownProgress.noEffect("new candidate with missing progress does not fabricate construction effect");
    }
    private static Fixture aircraft(){Fixture f=new Fixture(8000L);f.units.get(0).put("type","combatEngineer");f.units.get(1).put("type","combatEngineer");return f;}
    private static void jetHold(Fixture f,String id,long actor,double x){
        f.hold(id,actor,"/command/construct","/command/construct?unitId="+actor+"&actionId=jet&x="+x+"&y=200","amphibiousJet",false);
    }
    private static void jet(Fixture f,long id,double x,double progress){f.units.add(unit(id,"amphibiousJet",-1,1,progress,x,200));}
    private static void constructionGroups(){
        for(boolean reverse:Arrays.asList(false,true)){
            Fixture f=aircraft();if(reverse){jetHold(f,"j2",2,140);jetHold(f,"j1",1,100);}else{jetHold(f,"j1",1,100);jetHold(f,"j2",2,140);}
            if(reverse){jet(f,12,140,.1);jet(f,11,100,.1);}else{jet(f,11,100,.1);jet(f,12,140,.1);}
            List<Map<String,Object>> rows=f.observe(21,3500);check(rows.size()==2&&f.scheduler.unsettledCreditCount()==0,"two 40-world-separated constructors and two later distinct aircraft settle as a complete group regardless of iteration order");
            for(Map<String,Object> row:rows){check("NATIVE_CONSTRUCTION_GROUP_EFFECT_NOT_INDIVIDUAL_LINEAGE".equals(row.get("witnessKind")),"overlap settlement explicitly claims only a group effect");
                check(Arrays.asList("j1","j2").equals(row.get("constructionGroupIntentIds"))&&Arrays.asList(11L,12L).equals(row.get("constructionEffectUnitIds")),"stable group witness records distinct effects and exact obligations without actor assignment");
                check(Boolean.FALSE.equals(row.get("individualProducerLineageClaimed"))&&Boolean.FALSE.equals(row.get("productReady")),"group effect asserts neither individual lineage nor readiness");}
            check(f.observe(22,4000).isEmpty(),"group settlement cannot release twice");
        }
        Fixture one=aircraft();jetHold(one,"j1",1,100);jetHold(one,"j2",2,140);jet(one,11,100,.1);
        check(one.observe(21,3500).isEmpty()&&one.scheduler.unsettledCreditCount()==2,"two overlapping holds with only one effect retain the entire group");
        jet(one,12,140,.2);check(one.observe(22,4000).size()==2,"a later second legal effect can cover the still-held group");
        Fixture extra=aircraft();jetHold(extra,"j1",1,100);jetHold(extra,"j2",2,140);jet(extra,11,100,.1);jet(extra,12,140,.1);jet(extra,13,120,.1);
        check(extra.observe(21,3500).isEmpty()&&extra.scheduler.unsettledCreditCount()==2,"two holds with three possible effects remain ambiguous");
        Fixture across=aircraft();jetHold(across,"j1",1,100);jet(across,11,100,1);across.state.put("frame",11L);across.state.put("gameTimeMs",2600L);across.refresh();jetHold(across,"j2",2,140);
        List<Map<String,Object>> first=across.observe(21,3500);check(first.size()==1&&"j1".equals(first.get(0).get("intentId"))&&across.scheduler.unsettledCreditCount()==1,"a product already unchanged in the newer hold's baseline cannot geometrically poison the older effect");
        jet(across,12,140,.1);check(across.observe(22,4000).size()==1,"the newer obligation requires its own later effect");
        Fixture matching=aircraft();jetHold(matching,"j1",1,100);jet(matching,11,100,1);matching.state.put("frame",11L);matching.state.put("gameTimeMs",2600L);matching.refresh();jetHold(matching,"j2",2,140);jet(matching,12,140,.1);
        check(matching.observe(21,3500).size()==2,"different before-observation baselines admit a complete matching without inventing individual lineage");
        Fixture future=aircraft();jetHold(future,"j1",1,100);future.state.put("frame",30L);future.refresh();jetHold(future,"j2",2,140);jet(future,11,100,.1);jet(future,12,140,.1);
        check(future.observe(21,3500).isEmpty()&&future.scheduler.unsettledCreditCount()==2,"a competing hold not yet behind the receipt frame cannot create false group uniqueness");
        check(future.observe(31,4000).size()==2,"all members require a legal later frame before the group can settle");
        Fixture absent=aircraft();jetHold(absent,"j1",1,100);Map<String,Object> missing=absent.units.remove(1);jetHold(absent,"j2",2,140);absent.units.add(missing);jet(absent,11,100,.1);jet(absent,12,140,.1);
        check(absent.observe(21,3500).isEmpty()&&absent.scheduler.unsettledCreditCount()==2,"unobserved competing constructor is retained as ambiguity rather than silently ignored");
        Fixture unknown=aircraft();jetHold(unknown,"j1",1,100);jetHold(unknown,"j2",2,140);jet(unknown,11,100,.1);jet(unknown,12,140,-1);
        check(unknown.observe(21,3500).isEmpty(),"unknown product progress cannot enter a valid effect group");unknown.units.get(3).put("buildProgress",.1);
        check(unknown.observe(22,4000).size()==2,"later legal progress can establish the second distinct effect");
        Fixture baselineUnknown=aircraft();jet(baselineUnknown,11,100,-1);jetHold(baselineUnknown,"j1",1,100);jetHold(baselineUnknown,"j2",2,140);baselineUnknown.units.get(2).put("buildProgress",.1);jet(baselineUnknown,12,140,.1);
        check(baselineUnknown.observe(21,3500).isEmpty()&&baselineUnknown.scheduler.unsettledCreditCount()==2,"unknown baseline progress cannot be counted or removed from ambiguity");
        for(CommandArbiter.Stamp foreign:Arrays.asList(new CommandArbiter.Stamp("foreign","team:0",10,2500),new CommandArbiter.Stamp("s","team:1",10,2500))){
            Fixture f=aircraft();jetHold(f,"j1",1,100);
            Intent other=new Intent("foreign",CommandArbiter.DEFAULT_OWNER,Collections.<Long,Long>emptyMap(),Arrays.asList(2L),"/command/construct","TEST",1,foreign,"quote:o","/combat/production","/command/construct?unitId=2&actionId=jet&x=140&y=200",Intent.Commitment.spending(800L,Collections.<Long,Integer>emptyMap(),0),"quote:o","/combat/production");
            f.witness.accepted(other,map("status","queued","frame",20L),map("product","amphibiousJet"),f.state);jet(f,11,100,.1);
            List<Map<String,Object>> rows=f.observe(21,3500);check(rows.size()==1&&"j1".equals(rows.get(0).get("intentId")),"foreign session or player cannot enter the local construction candidate group");
        }
        Fixture unknownProduct=aircraft();jetHold(unknownProduct,"j1",1,100);unknownProduct.hold("unknown",2,"/command/construct","/command/construct?unitId=2&actionId=jet&x=140&y=200",null,false);jet(unknownProduct,11,100,.1);
        check(unknownProduct.observe(21,3500).isEmpty()&&unknownProduct.scheduler.unsettledCreditCount()==2,"a same-player competing hold with unknown product cannot manufacture unique evidence");
        Fixture dead=aircraft();jetHold(dead,"j1",1,100);jetHold(dead,"j2",2,140);jet(dead,11,100,.1);jet(dead,12,140,.1);dead.units.get(3).put("dead",true);
        check(dead.observe(21,3500).isEmpty()&&dead.scheduler.unsettledCreditCount()==2,"dead own product does not count as a second construction effect");
        Fixture hall=aircraft();hall.units.add(unit(3,"combatEngineer",-1,1,1,100,200));hall.refresh();jetHold(hall,"j1",1,0);jetHold(hall,"j2",2,0);jetHold(hall,"j3",3,110);jet(hall,11,0,.1);jet(hall,12,220,.1);jet(hall,13,225,.1);
        check(hall.observe(21,3500).isEmpty()&&hall.scheduler.unsettledCreditCount()==3,"equal group counts without a complete matching do not settle");
    }
    public static void main(String[] args){queuesAndTime();identity();investments();constructions();constructionGroups();System.out.println("NativeCreditWitnessHarness checks="+checks+" PASS");}
}
