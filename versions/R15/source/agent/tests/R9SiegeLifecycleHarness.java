package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Accepted objective continuity and genuine withdrawal contracts; synthetic, not battle efficacy. */
public final class R9SiegeLifecycleHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static G5CombatHarness.Fixture fixture(double x)throws Exception{
        G5CombatHarness.Fixture f=R8BaseAdmissionHarness.fixture();
        f.state.put("map",map("width",3000.,"height",3000.));
        for(long id=10;id<16;id++){f.find(id).put("x",x);f.find(id).put("y",1000.);}
        G51TacticalHarness.enemy(f,148,"commandCenter",1000,1000,3700,true,true,"SURFACE");
        G51TacticalHarness.enemy(f,149,"lightGunship",1010,1000,1300,true,false,"SURFACE");
        f.centroids();return f;
    }
    static void accept(G5CombatHarness.Fixture f,long frame)throws Exception{for(ForceController.Proposal p:f.owned("general:1"))p.accepted(map("status","queued","frame",frame,"gameTimeMs",f.now));}
    static void chip(G5CombatHarness.Fixture f)throws Exception{for(long id=10;id<16;id++)f.find(id).put("hp",564.);f.sample(1000);f.collect();}
    static double coordinate(String path,String key){for(String part:path.substring(path.indexOf('?')+1).split("&"))if(part.startsWith(key+"="))return Double.parseDouble(part.substring(key.length()+1));throw new AssertionError(path);}
    static void outside(G5CombatHarness.Fixture f,String reason){
        check(!f.owned("general:1").isEmpty(),reason+": real withdrawal proposal exists");
        for(ForceController.Proposal p:f.owned("general:1")){
            check(p.path.startsWith("/command/move?"),reason+": native MOVE replaces attack");
            check(Math.hypot(coordinate(p.path,"x")-1000,coordinate(p.path,"y")-1000)>280,reason+": destination leaves verified base range");
        }
    }
    static void engagedSiegeRetainsLifecycle()throws Exception{
        G5CombatHarness.Fixture f=fixture(800);f.collect();check("ADVANCE".equals(f.combat.view(f.a).get("tacticalChoice")),"initial current/native admitted siege");
        check(f.owned("general:1").stream().allMatch(p->p.path.startsWith("/command/attack-move?")),"initial siege uses existing protocol");
        accept(f,f.frame);chip(f);Map<String,Object> view=f.combat.view(f.a);
        check(!f.combat.retreating(f.a)&&!Boolean.TRUE.equals(view.get("escapeDamageWitness")),"six percent chip damage has no established crisis withdrawal witness");
        check("ADVANCE".equals(view.get("tacticalChoice")),"post-receipt own entry does not repay historical approach cost and oscillate to hold");
        check("CURRENT_BASE_SIEGE_POST_RECEIPT_OWN_ENTRY_CONTINUATION".equals(view.get("tacticalReason")),"continuity carries explicit lawful own-entry evidence");
        check(f.owned("general:1").stream().noneMatch(p->p.path.startsWith("/command/move?")),"engaged viable siege is never cancelled with a hold move");
        for(long id=10;id<16;id++){f.find(id).put("hp",390.);f.find(id).put("orderType","attackMove");}
        f.sample(2000);f.collect();check(f.combat.retreating(f.a),"actual severe exchange deterioration retains existing withdrawal lifecycle");outside(f,"severe exchange");
    }
    static void receiptAndAbaCannotInventEntry()throws Exception{
        G5CombatHarness.Fixture travel=fixture(500);travel.collect();accept(travel,travel.frame);chip(travel);
        check("STANDOFF".equals(travel.combat.view(travel.a).get("tacticalChoice")),"accepted siege still outside base range receives fresh approach admission");outside(travel,"not yet entered");
        G5CombatHarness.Fixture future=fixture(800);future.collect();accept(future,future.frame+10);chip(future);
        check(!"CURRENT_BASE_SIEGE_POST_RECEIPT_OWN_ENTRY_CONTINUATION".equals(future.combat.view(future.a).get("tacticalReason")),"pre-receipt own frame is not entry evidence");outside(future,"future receipt");
        G5CombatHarness.Fixture aba=fixture(800);aba.collect();accept(aba,aba.frame);
        for(long id=10;id<16;id++)check(aba.gate.transfer("general:1","test:aba",id)&&aba.gate.transfer("test:aba","general:1",id),"real generation ABA fixture");chip(aba);
        check(!"CURRENT_BASE_SIEGE_POST_RECEIPT_OWN_ENTRY_CONTINUATION".equals(aba.combat.view(aba.a).get("tacticalReason")),"old generation receipt cannot preserve siege permission");
        check(!aba.owned("general:1").isEmpty(),"new generation must acquire its own fresh native order receipt");
    }
    static void lostOrDeniedSiegeLeavesFireZone()throws Exception{
        G5CombatHarness.Fixture lost=fixture(800);lost.collect();accept(lost,lost.frame);lost.enemyRows.clear();lost.sample(1000);lost.collect();
        check(((Number)lost.combat.view(lost.a).get("observedOwnDeaths")).intValue()==0,"lost base contact does not infer own deaths");outside(lost,"lost contact no frontier route");
        check(lost.owned("general:1").stream().noneMatch(p->p.path.startsWith("/command/attack-move?")),"base memory cannot authorize ghost attack");
        G5CombatHarness.Fixture denied=fixture(800);denied.collect();accept(denied,denied.frame);denied.blocked=true;denied.sample(1000);denied.collect();
        check("CURRENT_BASE_SIEGE_NATIVE_ELIGIBILITY_LOST".equals(denied.combat.view(denied.a).get("tacticalReason")),"fresh native eligibility is still mandatory during continuity");outside(denied,"native denial");
        G5CombatHarness.Fixture blocked=fixture(800);blocked.enemyRows.remove(1);blocked.enemyRows.get(0).put("hp",16000.);
        G51TacticalHarness.enemy(blocked,150,"extractor",1100,1000,800,false,true,"SURFACE");blocked.collect();
        check(blocked.combat.view(blocked.a).get("blockedBaseZone")!=null,"cheap objective beside unaffordable base retains travel veto");outside(blocked,"blocked current base");
    }
    static void invalidOldHoldIsReplaced()throws Exception{
        G5CombatHarness.Fixture f=fixture(800);f.collect();accept(f,f.frame);f.enemyRows.clear();f.sample(1000);f.collect();
        accept(f,f.frame);for(long id=10;id<16;id++){f.find(id).put("orderType","move");f.find(id).put("orderX",800.);f.find(id).put("orderY",1000.);}
        f.sample(5000);f.collect();outside(f,"wrong current hold order");
        check(f.owned("general:1").stream().noneMatch(p->coordinate(p.path,"x")==800.&&coordinate(p.path,"y")==1000.),"centroid in known range never becomes stable hold point");
    }
    static void commanderAllocationDoesNotRestartEnteredSiege()throws Exception{
        G5CombatHarness.Fixture f=fixture(750);f.collect();accept(f,f.frame);
        CommanderDirector commander=new CommanderDirector(f.registry);f.combat.setCommander(commander);
        check(!commander.allowsTarget(f.a,148),"fixture removes ordinary remote Commander target allocation");
        chip(f);check("ADVANCE".equals(f.combat.view(f.a).get("tacticalChoice")),"already entered admitted current base remains the General local combat lifecycle");
        check("CURRENT_BASE_SIEGE_POST_RECEIPT_OWN_ENTRY_CONTINUATION".equals(f.combat.view(f.a).get("tacticalReason")),"Commander allocation exception has explicit current entry reason");
        check(f.owned("general:1").stream().noneMatch(p->p.path.startsWith("/command/move?")),"allocation churn cannot issue an in-range cancellation hold");
        f.enemyRows.clear();f.sample(2000);f.collect();outside(f,"Commander current contact lost");
    }
    public static void main(String[] args)throws Exception{engagedSiegeRetainsLifecycle();receiptAndAbaCannotInventEntry();lostOrDeniedSiegeLeavesFireZone();invalidOldHoldIsReplaced();commanderAllocationDoesNotRestartEnteredSiege();System.out.println("R9SiegeLifecycleHarness checks="+checks+" PASS");}
}
