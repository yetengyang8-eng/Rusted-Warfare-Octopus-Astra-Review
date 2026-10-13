package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;
import static io.rwagent.client.BattleClient.obj;

/** Current own batch modes and fog-area MOVE fixtures. No natural coverage or victory claim. */
public final class R14AirFlowHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Host implements StrategyDirector.Host {
        final R13AirLifecycleHarness.Host base=new R13AirLifecycleHarness.Host();
        final List<String> reads=new ArrayList<String>();
        final Set<Long> unknown=new HashSet<Long>();
        boolean foreignBatch,staleBatch,wrongIdentity,blockedArea;
        List<Long> observationRoute=new ArrayList<Long>();
        public String landRouteDemandStrategy(){return "KNOWN";}
        public Map<String,Object> readStrategy(String path,String event){
            reads.add(path);
            if(path.startsWith("/combat/unit-modes?unitIds=")){
                Map<String,Object> packet=base.readStrategy(path,event);
                if(foreignBatch)packet.put("sessionId","foreign");if(staleBatch)packet.put("gameTimeMs",base.base.nativeHost.time-1);
                for(Map<String,Object> row:items(packet,"units"))if(unknown.contains((long)number(row,"unitId",-1))){row.remove("submergedWeaponAvailable");row.put("status","UNKNOWN");}
                if(wrongIdentity)items(packet,"units").get(0).put("unitId",999999L);return packet;
            }
            if(path.startsWith("/scout/plan")&&path.contains("observeX=")){
                if(blockedArea)return map("status","no_frontier","sessionId","s");
                long last=observationRoute.get(observationRoute.size()-1);
                return map("status","planned","sessionId","s","gameTimeMs",base.base.nativeHost.time,"frame",base.base.nativeHost.frame,
                    "builderId",G6EndgameHarness.Host.param(path,"unitId"),"movementType","AIR","pathKnown",true,"routeComplete",true,
                    "routeScope","air-search","routeTiles",observationRoute,"targetX",(last/100+.5)*20,"targetY",(last%100+.5)*20,
                    "targetTile",last,"frontierTile",last,"observationApproachIsVisibilityProof",false);
            }
            return base.readStrategy(path,event);
        }
        public Map<String,Object> orderStrategy(String owner,String path){return base.orderStrategy(owner,path);}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context){return base.orderStrategy(owner,path,context);}
        public void emitStrategy(String event,Map<String,Object> data){base.emitStrategy(event,data);}
        public void spendStrategy(String c,long price,String type,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
        G6EndgameHarness.Host nativeHost(){return base.base.nativeHost;}
        Map<String,Object> actor(long id){return base.actor(id);}
        void next(AirGeneralController air)throws Exception{base.next(air);}
        AirGeneralController formation(int count)throws Exception{
            for(long id=12;id<10+count;id++)nativeHost().own.add(G6EndgameHarness.unit(id,"amphibiousJet",100,100));
            nativeHost().sample();AirGeneralController air=new AirGeneralController(this,nativeHost().arbiter);
            for(long id=10;id<10+count;id++){nativeHost().arbiter.claim("strategy:"+id,id);check(air.accept(id,"strategy:"+id,new CapabilityUnitMode(id)),"idle owner transfers each independent member");}
            next(air);return air;
        }
        int batches(){return (int)reads.stream().filter(p->p.startsWith("/combat/unit-modes?unitIds=")).count();}
        long events(String event){return base.base.events(event);}
        void contacts(AirGeneralController air,Map<String,Object>... enemies)throws Exception{base.contacts(air,enemies);}
    }
    static void boundedBatchAndIndependentModes()throws Exception{
        Host h=new Host();h.unknown.add(11L);h.nativeHost().physical.put(10L,true);
        AirGeneralController air=h.formation(847);air.act();
        check(h.batches()==18,"847 current own members use exactly ceil(847/48) mode requests per cycle");
        check(h.reads.stream().noneMatch(p->p.startsWith("/combat/unit-modes?unitId=")),"Air uses no per-actor HTTP fallback");
        Set<Long> seen=new HashSet<Long>();for(String path:h.reads)if(path.startsWith("/combat/unit-modes?unitIds=")){
            String[] ids=path.split("unitIds=")[1].split(",");check(ids.length<=48,"batch retains native bounded query size");
            for(String raw:ids)check(seen.add(Long.parseLong(raw)),"each actual owner identity is queried once across all formations");
        }
        check(seen.size()==847,"batch inventory retains all members including independent UNKNOWN");
        List<Map<String,Object>> roster=items(air.snapshot(),"roster");Map<String,Object> wet=null,unknown=null,airborne=null;
        for(Map<String,Object> row:roster){long id=(long)number(row,"unitId",-1);if(id==10)wet=row;if(id==11)unknown=row;if(id==12)airborne=row;}
        check(wet!=null&&"SUBMERGED".equals(obj(wet.get("mode")).get("nativeMode")),"one member's actual submerged witness remains independent of accepted Fly");
        check(unknown!=null&&"UNKNOWN".equals(obj(unknown.get("mode")).get("nativeMode")),"unavailable member is UNKNOWN, never copied from batch neighbor");
        check(airborne!=null&&"AIR".equals(obj(airborne.get("mode")).get("nativeMode")),"other own member's physical AIR witness remains current");
        int requests=h.batches();air.act();check(h.batches()==requests,"same observation reuses its own per-member batch witness");
        h.next(air);air.act();check(h.batches()==36,"next own sample refreshes all formations in another 18 bounded requests");air.close();
        for(int scenario=0;scenario<3;scenario++){
            h=new Host();h.foreignBatch=scenario==0;h.staleBatch=scenario==1;h.wrongIdentity=scenario==2;air=h.formation(2);air.act();
            check(h.base.base.dispatches.isEmpty(),"foreign, stale or unrequested batch identity grants no AIR MOVE or mode command");
            check(h.batches()==1,"invalid batch cannot fall back to per-member requests");air.close();
        }
    }
    static void safeAreaRouteAndVisibleEdgeClear()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation(2);air.act();Object frontier=air.snapshot().get("routeFrontierTile");
        h.contacts(air,R13AirLifecycleHarness.enemy(90,"antiAirTurret",1000,500,true,true,false));air.act();
        // A native-style multi-turn route ends at a stand outside the remembered AA circle.
        for(long r=5;r<=55;r++)h.observationRoute.add(500+r);
        for(long c=6;c<=50;c++)h.observationRoute.add(c*100+55);
        for(long r=54;r>=49;r--)h.observationRoute.add(5000+r);
        Map<String,Object> aa=R13AirLifecycleHarness.enemy(90,"antiAirTurret",1000,500,true,true,false);aa.put("range",320.);
        h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);air.act();
        check(h.base.attacks()==1,"fog transition adds no new ghost-target attack");
        h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);h.base.resetOrders();air.act();
        check(h.reads.stream().anyMatch(p->p.contains("observeX=1000.0")&&p.contains("observeY=500.0")),"blocked remembered center requests native legal observation approach");
        check(BattleClient.n(h.actor(10),"orderY")==1110,"native multi-turn investigation starts by going around the remembered circle");
        check(R13AirLifecycleHarness.area(air)!=null&&frontier.equals(air.snapshot().get("routeFrontierTile")),"observation detour retains contact responsibility and suspended ordinary route");
        for(long id:new long[]{10,11}){h.actor(id).put("x",110.);h.actor(id).put("y",1110.);}h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);h.base.resetOrders();air.act();
        check(BattleClient.n(h.actor(10),"orderX")==1010&&BattleClient.n(h.actor(10),"orderY")==1110,"later own position consumes next turn of the retained investigation route");
        for(long id:new long[]{10,11}){h.actor(id).put("x",1010.);h.actor(id).put("y",1110.);}h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);h.base.resetOrders();air.act();
        check(BattleClient.n(h.actor(10),"orderX")==1010&&BattleClient.n(h.actor(10),"orderY")==990,"later corner arrival advances to safe observation edge without returning to center");
        for(long id:new long[]{10,11}){h.actor(id).put("x",1010.);h.actor(id).put("y",990.);}h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);h.base.resetOrders();air.act();
        check(R13AirLifecycleHarness.area(air)!=null&&h.events("air_general_area_blocked")>0,"safe stand without actual visibility retains responsibility with explicit blocked handoff");
        check(Math.hypot(BattleClient.n(h.actor(10),"x")-1000,BattleClient.n(h.actor(10),"y")-500)>460,"investigation never requires entering remembered caution circle");
        h.base.visible=true;h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);h.base.resetOrders();air.act();
        check(R13AirLifecycleHarness.area(air)==null&&h.events("air_general_area_clear")==1,"fresh legally visible empty area clears from safe stand farther than old120 requirement");
        check(h.base.attacks()==0&&frontier.equals(air.snapshot().get("routeFrontierTile")),"positive area clearance grants no attack and resumes original mission");air.close();
        h=new Host();air=h.formation(2);air.act();h.contacts(air,R13AirLifecycleHarness.enemy(90,"antiAirTurret",1000,500,true,true,false));air.act();h.blockedArea=true;
        h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);air.act();h.next(air);h.nativeHost().vision.put("rememberedThreats",Arrays.asList(aa));h.base.base.observe(air);h.base.resetOrders();air.act();
        check(R13AirLifecycleHarness.area(air)!=null&&h.events("air_general_area_blocked")>0,"native no-route response keeps area ownership and emits explicit investigation handoff");
        check(h.base.attacks()==0&&h.nativeHost().arbiter.owns(AirGeneralController.OWNER,10),"blocked fog area grants no attack or blind ownership release");air.close();
    }
    public static void main(String[] args)throws Exception{boundedBatchAndIndependentModes();safeAreaRouteAndVisibleEdgeClear();System.out.println("R14_AIR_FLOW_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");}
}
