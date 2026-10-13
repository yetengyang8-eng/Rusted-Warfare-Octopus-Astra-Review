import com.corrodinggames.rts.game.units.am;
import com.corrodinggames.rts.game.units.ar;
import com.corrodinggames.rts.game.units.y;
import io.rwagent.client.R15AirNativeCompanion;
import java.util.*;

/** Real 1.15 objects + HTTP bridge + Air controller. Explicit fog/position/clock fixtures, zero ticks. */
public final class R15AirNativeHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static long value(Map<?,?> packet,String field){return ((Number)packet.get(field)).longValue();}
    static Map<?,?> intel(Map<?,?> packet,long id){for(Object raw:(List<?>)packet.get("enemyIntel")){Map<?,?> row=(Map<?,?>)raw;
        if(value(row,"id")==id)return row;}throw new AssertionError("missing legally captured intel "+id);}
    public static void main(String[] args){try{
        OpeningHarness.test();com.corrodinggames.rts.game.i e=EconomyHarness.engine;e.cf.b.clear();am.bE.clear();e.by=1000;
        // Constructors/menu access use harmless fixture images and native dry grids; no asset tree is loaded.
        EconomyHarness.set(e.bU,"D",EconomyHarness.costs);EconomyHarness.set(e.bU,"A",EconomyHarness.costs);
        EconomyHarness.set(Class.forName("com.corrodinggames.rts.game.units.b.c"),"b",BridgeHarness.construct("com.corrodinggames.rts.gameFramework.m.e"));
        List<y> jets=new ArrayList<y>();for(int i=0;i<6;i++){y jet=R14AirNativeHarness.jet(9001+i,e.bs,100+i*4,100);jet.eq=20;jets.add(jet);}
        com.corrodinggames.rts.game.e other=new com.corrodinggames.rts.game.e(1,false);other.r=1;e.bs.r=0;
        am aa=R13AirNativeHarness.add("antiAirTurret",9100,other,1000,500);
        am factory=R13AirNativeHarness.add("landFactory",9101,other,970,500);
        for(byte[] column:e.bs.N)Arrays.fill(column,(byte)10);e.bs.N[50][25]=0;
        Map<?,?> initial=R13AirNativeHarness.get("/combat/observe");R13AirNativeHarness.get("/scout/observe");
        check("VISIBLE".equals(intel(initial,9100).get("status")),"initial native legal sighting captures original AA");
        e.bs.N[50][25]=10;e.by+=1000;e.bx++;
        try(R15AirNativeCompanion c=new R15AirNativeCompanion(BridgeHarness.base)){
            for(y jet:jets)c.accept(jet.eh);long generation=c.arbiter.ownerGeneration(9001);
            e.by+=1000;e.bx++;c.sample();Map<?,?> hidden=c.read("/combat/observe");
            check(((List<?>)hidden.get("visibleEnemies")).isEmpty()&&"LOST_CONTACT".equals(intel(hidden,9100).get("status")),"fog provides remembered AA location but no current hostile visibility");
            BridgeHarness.check("POST","/command/attack-target?unitIds=9001&targetId=9100&sessionId="+hidden.get("sessionId")+"&requestId=r15-hidden-reject",409,"legally visible hostile");
            c.act();check(Boolean.TRUE.equals(c.snapshot().get("clearanceProbe")),"actual assembled own formation starts remembered-area clearance probe");
            check(value((Map<?,?>)c.snapshot().get("contactArea"),"contactId")==9100,"real bridge memory supplies owned investigation responsibility");
            check(e.cf.b.size()==6,"controller queues one original native investigation MOVE per assembled member");
            for(Object raw:e.cf.b){com.corrodinggames.rts.gameFramework.e command=(com.corrodinggames.rts.gameFramework.e)raw;
                check("move".equals(command.j.d().name())&&command.j.g()==1000&&command.j.h()==500,"actual native waypoint approaches remembered AA area instead of firing at ghost");}
            check(c.commands.stream().noneMatch(row->String.valueOf(row.get("path")).startsWith("/command/attack")),"controller issues no attack-target or attack-move while target remains fogged");
            check(c.arbiter.ownerGeneration(9001)==generation&&c.arbiter.owns("air-general:1",9001),"controlled approach preserves actual member owner generation");
            e.bV=new com.corrodinggames.rts.gameFramework.aa();List<Object> moves=new ArrayList<Object>();for(Object raw:e.cf.b)moves.add(raw);
            for(Object raw:moves)((com.corrodinggames.rts.gameFramework.e)raw).k();e.cf.b.clear();e.by+=1000;e.bx++;c.sample();
            check("move".equals(c.actor(9001).get("orderType"))&&value(c.actor(9001),"orderX")==1000,"later actual own native order witnesses accepted MOVE without claiming travel arrival");
            // Explicit fixture approach and restored native fog are not natural movement/discovery.
            for(int i=0;i<jets.size();i++){jets.get(i).eo=850+i*4;jets.get(i).ep=500;}
            e.bs.N[50][25]=0;e.bs.N[48][25]=0;e.by+=1000;e.bx++;c.sample();c.act();
            check(e.cf.b.size()==1,"current legally visible compatible target creates one formation native attack command");
            com.corrodinggames.rts.gameFramework.e attack=(com.corrodinggames.rts.gameFramework.e)e.cf.b.get(0);
            check("attack".equals(attack.j.d().name())&&attack.j.i()==aa,"current AA native entity wins explicit target priority over closer harmless factory");
            check(attack.j.i()!=factory&&c.arbiter.ownerGeneration(9001)==generation,"current combat neither selects harmless structure first nor changes ownership");
            attack.k();e.cf.b.clear();e.by+=1000;e.bx++;c.sample();
            check("attack".equals(c.actor(9001).get("orderType"))&&value(c.actor(9001),"orderTargetId")==9100,"later real own target-ID witness identifies actual native AA attack order");
        }
        System.out.println("R15_AIR_NATIVE_OK checks="+checks+" simulationTicks=0 fixtureOnly=true explicitFogAndPosition=true naturalDiscoveryProven=false nativeVictoryProven=false");System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
