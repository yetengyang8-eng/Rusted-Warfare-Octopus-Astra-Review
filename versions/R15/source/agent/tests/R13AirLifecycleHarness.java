package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Synthetic own-state/visibility lifecycle evidence; no natural coverage or victory claim. */
public final class R13AirLifecycleHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Host implements StrategyDirector.Host {
        final R11AirContinuityHarness.Host base=new R11AirContinuityHarness.Host();
        boolean visible,foreignVisibility,staleEngagement,unknownContactRecheck;
        public String landRouteDemandStrategy(){return "KNOWN";}
        public boolean groundTargetCommittedStrategy(long id){return base.ground.contains(id);}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/scout/visible")){
                List<Map<String,Object>> tiles=new ArrayList<Map<String,Object>>();
                for(String raw:path.split("tiles=")[1].split(","))tiles.add(map("tile",Long.parseLong(raw),"visible",visible));
                return map("sessionId",foreignVisibility?"foreign":"s","gameTimeMs",base.nativeHost.time,"frame",base.nativeHost.frame,"tiles",tiles);
            }
            if(path.equals("/combat/observe"))return unknownContactRecheck?null:base.nativeHost.enemy;
            Map<String,Object> result=base.readStrategy(path,event);
            if(path.startsWith("/combat/engagement")&&staleEngagement){result=new LinkedHashMap<String,Object>(result);result.put("targetObservedAtGameTimeMs",base.nativeHost.time-1);}
            return result;
        }
        public Map<String,Object> orderStrategy(String owner,String path){return orderStrategy(owner,path,Collections.<String,Object>emptyMap());}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context){return base.orderStrategy(owner,path,context);}
        public void emitStrategy(String event,Map<String,Object> data){base.emitStrategy(event,data);}
        public void spendStrategy(String category,long cost,String type,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
        public Map<String,Object> costSourceStrategy(Map<String,Object> packet){return base.nativeHost.costSourceStrategy(packet);}
        AirGeneralController formation()throws Exception{
            AirGeneralController air=new AirGeneralController(this,base.nativeHost.arbiter);
            for(long id:new long[]{10,11}){base.nativeHost.arbiter.claim("strategy:"+id,id);check(air.accept(id,"strategy:"+id,new CapabilityUnitMode(id)),"idle specialist lease transfers");}
            next(air);air.act();return air;
        }
        Map<String,Object> actor(long id){return base.actor(id);}
        void next(AirGeneralController air)throws Exception{base.next(air);}
        void contacts(AirGeneralController air,Map<String,Object>... contacts)throws Exception{base.contact(air,contacts);}
        void resetOrders(){base.dispatches.clear();}
        long attacks(){return base.dispatches.stream().filter(d->String.valueOf(d.get("path")).startsWith("/command/attack-target")).count();}
    }
    static Map<String,Object> enemy(long id,String type,double x,double y,boolean armed,boolean air,boolean sub){
        return map("id",id,"type",type,"x",x,"y",y,"hp",1000.,"targetDomain","SURFACE","touchingWater",false,
            "canAttack",armed,"canAttackAir",air,"canAttackSubmerged",sub,"canAttackSurface",armed,"canAttackLand",armed);
    }
    static Map<String,Object> area(AirGeneralController air){return BattleClient.obj(air.snapshot().get("contactArea"));}
    static void weaponDomainAndPriority()throws Exception{
        Map<String,Object> unarmed=enemy(90,"landFactory",130,100,false,false,false);unarmed.put("hp",90000000.);
        Map<String,Object> surface=enemy(91,"c_turret_t1",180,100,true,false,false);
        Map<String,Object> aa=enemy(92,"antiAirTurret",300,100,true,true,false);
        Map<String,Object> torpedo=enemy(93,"submarine",300,100,true,false,true);
        check("INCOMPATIBLE".equals(AirResponsePolicy.threatCompatibility(unarmed,"AIR",null)),"unarmed building HP never creates anti-air risk");
        check("INCOMPATIBLE".equals(AirResponsePolicy.threatCompatibility(surface,"AIR",null)),"actual surface gun cannot threaten current AIR");
        check("COMPATIBLE".equals(AirResponsePolicy.threatCompatibility(aa,"AIR",null)),"actual native anti-air weapon threatens AIR");
        check("INCOMPATIBLE".equals(AirResponsePolicy.threatCompatibility(aa,"SUBMERGED",null)),"anti-air weapon does not threaten current submerged actor");
        check("COMPATIBLE".equals(AirResponsePolicy.threatCompatibility(torpedo,"SUBMERGED",null))
            &&"INCOMPATIBLE".equals(AirResponsePolicy.threatCompatibility(torpedo,"AIR",null)),"submerged threat follows physical actor domain");
        check("UNKNOWN".equals(AirResponsePolicy.threatCompatibility(aa,"UNKNOWN",null)),"missing physical mode is UNKNOWN even with a known weapon");
        Host h=new Host();AirGeneralController air=h.formation();h.contacts(air,unarmed,surface,aa);h.resetOrders();air.act();
        check(number(air.snapshot(),"targetId",-1)==92&&h.attacks()==1,"current compatible real anti-air target outranks nearby harmless building and surface gun");
        check(number(area(air),"contactId",-1)==92,"actual compatible engagement creates a persistent local responsibility");air.close();
    }
    static void fogResponsibilityAndPositiveClear()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();Object frontier=air.snapshot().get("routeFrontierTile");
        long generation=h.base.nativeHost.arbiter.ownerGeneration(10);
        h.contacts(air,enemy(90,"heavyTank",480,100,true,true,false));air.act();
        h.next(air);h.resetOrders();air.act();
        check(air.snapshot().get("targetId")==null&&"REACQUIRE_LAST_KNOWN_AREA".equals(area(air).get("phase")),"legal fog loss releases attack ID while retaining owned area task");
        check(h.attacks()==0&&h.base.dispatches.stream().allMatch(d->String.valueOf(d.get("path")).startsWith("/command/move")),"last-known responsibility issues only legal MOVE reacquisition");
        check(BattleClient.n(h.actor(10),"orderX")==480&&h.base.planner.plans==1&&frontier.equals(air.snapshot().get("routeFrontierTile")),"ordinary search does not steal area or reselect suspended frontier");
        h.actor(10).put("x",480.);h.actor(10).put("y",100.);h.next(air);h.resetOrders();air.act();
        check(area(air)!=null&&h.attacks()==0,"later own position without current local visible evidence does not clear the task");
        h.visible=true;h.foreignVisibility=true;h.next(air);air.act();check(area(air)!=null,"foreign visibility cannot clear local responsibility");
        h.foreignVisibility=false;h.unknownContactRecheck=true;h.next(air);air.act();check(area(air)!=null,"visible local tiles with unknown new contact packet do not imply empty area");
        h.unknownContactRecheck=false;h.next(air);h.resetOrders();air.act();
        check(area(air)==null&&frontier.equals(air.snapshot().get("routeFrontierTile"))&&h.base.planner.plans==1,"positive later own position plus fresh visible empty local area resumes the retained native route");
        check(h.attacks()==0&&h.base.nativeHost.arbiter.ownerGeneration(10)==generation,"completion preserves ownership/generation and grants no memory attack");
        check(h.base.events("air_general_area_clear")==1,"local area completion has explicit observable evidence");
        Map<String,Object> cleared=null;for(Map<String,Object> event:h.base.nativeHost.events)if("air_general_area_clear".equals(event.get("event")))cleared=BattleClient.obj(event.get("data"));
        check(cleared!=null&&Boolean.FALSE.equals(cleared.get("clearedIsKilled"))&&Boolean.FALSE.equals(cleared.get("localAreaIsWholeMapClear")),"area completion claims neither kill nor whole-map clearance");air.close();
    }
    static void reacquiredContactAndUnknownPermission()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();h.contacts(air,enemy(90,"heavyTank",480,100,true,true,false));air.act();
        h.next(air);air.act();h.next(air);h.contacts(air,enemy(91,"landFactory",1100,100,false,false,false));h.resetOrders();air.act();
        check(number(area(air),"contactId",-1)==90&&h.attacks()==0&&BattleClient.n(h.actor(10),"orderX")==480,"unrelated visible contact does not replace fog area responsibility");
        h.next(air);h.contacts(air,enemy(90,"heavyTank",510,100,true,true,false));h.resetOrders();air.act();
        check(number(air.snapshot(),"targetId",-1)==90&&number(area(air),"x",-1)==510&&h.attacks()==1,"current legal reacquisition updates the same responsibility and separately authorizes native attack");
        h.next(air);h.staleEngagement=true;h.contacts(air,enemy(90,"heavyTank",520,100,true,true,false));h.resetOrders();air.act();
        check(h.attacks()==0&&area(air)!=null,"stale engagement sample never authorizes attack despite retained task");air.close();
    }
    static void detourKeepsMissionAndHarmlessCorridor()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();Object frontier=air.snapshot().get("routeFrontierTile");
        for(long id:new long[]{10,11}){h.actor(id).put("x",200.);h.actor(id).put("y",110.);}h.next(air);
        Map<String,Object> danger=enemy(90,"antiAirTurret",600,110,true,true,false);danger.put("range",100.);
        h.base.nativeHost.vision.put("rememberedThreats",Arrays.asList(danger));h.base.observe(air);h.resetOrders();air.act();
        check(BattleClient.n(h.actor(10),"orderY")!=110&&BattleClient.n(air.snapshot(),"searchLegY")==110,"actual unsafe segment is redirected through a safe detour while mission endpoint is retained");
        check(frontier.equals(air.snapshot().get("routeFrontierTile"))&&number(air.snapshot(),"routeCursor",-1)==0&&h.base.planner.plans==1,"detour preserves frontier/cursor/plan rather than restarting unrelated exploration");
        double x=BattleClient.n(h.actor(10),"orderX"),y=BattleClient.n(h.actor(10),"orderY");
        for(long id:new long[]{10,11}){h.actor(id).put("x",x);h.actor(id).put("y",y);}h.next(air);
        h.base.nativeHost.vision.put("rememberedThreats",Arrays.asList(danger));h.base.observe(air);h.resetOrders();air.act();
        check(BattleClient.n(h.actor(10),"orderX")==1010&&BattleClient.n(h.actor(10),"orderY")==110,"later detour position reconnects to original route endpoint");
        check(number(air.snapshot(),"searchLegsWitnessed",-1)==0&&h.attacks()==0,"detour arrival is neither mission arrival nor attack permission");air.close();
        h=new Host();air=h.formation();h.next(air);Map<String,Object> harmless=enemy(99,"landFactory",600,110,false,false,false);harmless.put("range",800.);harmless.put("hp",90000000.);
        h.base.nativeHost.vision.put("rememberedThreats",Arrays.asList(harmless));h.base.observe(air);air.act();
        check(BattleClient.n(h.actor(10),"orderX")==1010&&BattleClient.n(h.actor(10),"orderY")==110&&h.base.events("air_general_search_detour")==0,"known harmless building creates no corridor threat from range or HP mass");air.close();
    }
    static void multipleTurnLookahead()throws Exception{
        Host h=new Host();h.base.planner.route.clear();
        for(long x=5;x<=15;x++)h.base.planner.route.add(x*100+5);
        for(long y=6;y<=15;y++)h.base.planner.route.add(1500+y);
        for(long x=16;x<=35;x++)h.base.planner.route.add(x*100+15);
        AirGeneralController air=h.formation();h.actor(10).put("x",200.);h.actor(10).put("y",110.);h.next(air);air.act();
        check(BattleClient.n(h.actor(10),"orderX")==710&&BattleClient.n(h.actor(10),"orderY")==310,"continuous segment lookahead flows through consecutive short turns before old waypoint pause");
        check(number(air.snapshot(),"routeCursor",-1)==0&&number(air.snapshot(),"searchLegsWitnessed",-1)==0,"multi-turn redirect receipt does not fabricate arrival or coverage");
        check(h.base.planner.plans==1&&h.base.events("air_general_search_lookahead")==1,"continuous turns reuse the owned legal native route");air.close();
    }
    public static void main(String[] args)throws Exception{weaponDomainAndPriority();fogResponsibilityAndPositiveClear();reacquiredContactAndUnknownPermission();detourKeepsMissionAndHarmlessCorridor();multipleTurnLookahead();System.out.println("R13_AIR_LIFECYCLE_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");}
}
