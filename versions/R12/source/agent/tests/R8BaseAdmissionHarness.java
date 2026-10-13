package io.rwagent.client;

import java.util.*;

/** Focused current-base admission contracts; synthetic orders are not native battle outcomes. */
public final class R8BaseAdmissionHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static G5CombatHarness.Fixture fixture()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();G51TacticalHarness.catalog(f);
        for(long id=10;id<16;id++){Map<String,Object> actor=f.find(id);actor.put("x",800.);actor.put("hp",600.);actor.put("maxHp",600.);}
        f.centroids();return f;
    }
    static void currentUnknownDefenderIsNotSecondMemoryGate()throws Exception{
        G5CombatHarness.Fixture f=fixture();
        G51TacticalHarness.enemy(f,148,"commandCenter",1000,100,3700,true,true,"SURFACE");
        G51TacticalHarness.enemy(f,1229,"lightGunship",1010,100,50,true,false,"SURFACE");
        f.collect();Map<String,Object> view=f.combat.view(f.a);
        check("ADVANCE".equals(view.get("tacticalChoice")),"native-eligible current base with admitted UNKNOWN-defender fallback is not rejected again by its remembered zone");
        check(!f.owned("general:1").isEmpty()&&f.owned("general:1").get(0).path.contains("x=1000.0"),"current base target retains the existing native attack-move protocol");
        check("UNKNOWN_PARTIAL_PROFILE".equals(((Map<?,?>)view.get("fightEstimate")).get("confidence")),"unknown defender profile remains UNKNOWN after admission");
        for(ForceController.Proposal p:f.owned("general:1"))check(p.owner.equals("general:1")&&p.lane.equals("GENERAL"),"admission preserves ordinary owner and lane");
        f.enemyRows.clear();f.frontierKnown=true;f.frontierX=1800;f.frontierY=100;f.sample(1000);f.collect();
        check("STANDOFF".equals(f.combat.view(f.a).get("tacticalChoice")),"lost base keeps remembered-zone travel exclusion and gains no new attack permission");
        for(ForceController.Proposal p:f.owned("general:1"))check(p.path.startsWith("/command/move?"),"lost contacts can only install a hold move");
    }
    static void unsafeUnknownAndNativeDenialRemainClosed()throws Exception{
        G5CombatHarness.Fixture unsafe=fixture();G51TacticalHarness.enemy(unsafe,148,"commandCenter",1000,100,3700,true,true,"SURFACE");
        G51TacticalHarness.enemy(unsafe,1229,"lightGunship",1010,100,20000,true,false,"SURFACE");unsafe.collect();
        check("STANDOFF".equals(unsafe.combat.view(unsafe.a).get("tacticalChoice")),"substantial unknown defender exposure still denies the siege");
        check(unsafe.owned("general:1").stream().noneMatch(p->p.path.startsWith("/command/attack-move")),"denied siege does not manufacture a forced attack");
        G5CombatHarness.Fixture denied=fixture();denied.blocked=true;G51TacticalHarness.enemy(denied,148,"commandCenter",1000,100,3700,true,true,"SURFACE");
        G51TacticalHarness.enemy(denied,1229,"lightGunship",1010,100,50,true,false,"SURFACE");denied.collect();
        check(denied.owned("general:1").stream().noneMatch(p->p.path.startsWith("/command/attack-move")),"missing current native eligibility cannot be replaced by exposure admission");
        G5CombatHarness.Fixture untrusted=fixture();untrusted.enemies.put("catalogGameJarMatched",false);G51TacticalHarness.enemy(untrusted,148,"commandCenter",1000,100,1000,true,true,"SURFACE");untrusted.collect();
        check("STANDOFF".equals(untrusted.combat.view(untrusted.a).get("tacticalChoice")),"unverified base mechanics retain the conservative memory exclusion");
    }
    public static void main(String[] args)throws Exception{currentUnknownDefenderIsNotSecondMemoryGate();unsafeUnknownAndNativeDenialRemainClosed();System.out.println("R8BaseAdmissionHarness checks="+checks+" PASS");}
}
