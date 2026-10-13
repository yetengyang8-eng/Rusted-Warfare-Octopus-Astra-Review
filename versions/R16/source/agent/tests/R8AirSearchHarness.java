package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Own-state route lifecycle fixture. No natural travel, coverage or victory claim. */
public final class R8AirSearchHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Host implements StrategyDirector.Host {
        final G6EndgameHarness.Host nativeHost=new G6EndgameHarness.Host();
        final List<Long> route=new ArrayList<Long>();int plans;long planFrame=-1;
        Host(){for(long col=5;col<=50;col++)route.add(col*100+5);for(long row=6;row<=25;row++)route.add(5000+row);}
        public boolean endgameStrategy(){return true;}
        public String landRouteDemandStrategy(){return "NONE";}
        public Map<String,Object> costSourceStrategy(Map<String,Object> p){return nativeHost.costSourceStrategy(p);}
        public Map<String,Object> readStrategy(String path,String event){
            if(!path.startsWith("/scout/plan"))return nativeHost.readStrategy(path,event);
            plans++;check(path.contains("routeScope=air-search"),"only the explicit AIR consumer requests a complete route");
            long last=route.get(route.size()-1);return map("status","planned","sessionId","s","builderId",G6EndgameHarness.Host.param(path,"unitId"),
                "frame",planFrame<0?nativeHost.frame:planFrame,"gameTimeMs",nativeHost.time,"movementType","AIR","pathKnown",true,
                "routeScope","air-search","routeComplete",true,"routeTiles",new ArrayList<Long>(route),
                "targetX",(last/100+.5)*20,"targetY",(last%100+.5)*20,"frontierTile",last,"targetTile",last);
        }
        public Map<String,Object> orderStrategy(String owner,String path){return nativeHost.orderStrategy(owner,path);}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context){return nativeHost.orderStrategy(owner,path,context);}
        public void emitStrategy(String event,Map<String,Object> data){nativeHost.emitStrategy(event,data);}
        public void spendStrategy(String c,long cost,String type,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
        void next(AirGeneralController air,long elapsed)throws Exception{nativeHost.frame++;nativeHost.time+=elapsed;nativeHost.sample();observe(air);}
        void observe(AirGeneralController air)throws Exception{air.observe(nativeHost.state,nativeHost.enemy,nativeHost.vision,100,100);}
        Map<String,Object> actor(long id){for(Map<String,Object> a:nativeHost.own)if(BattleClient.id(a)==id)return a;throw new AssertionError("missing fixture actor");}
        AirGeneralController formation()throws Exception{
            AirGeneralController air=new AirGeneralController(this,nativeHost.arbiter);
            for(long id:new long[]{10,11}){nativeHost.arbiter.claim("strategy:"+id,id);check(air.accept(id,"strategy:"+id,new CapabilityUnitMode(id)),"real idle specialist ownership transfers");}
            next(air,1000);return air;
        }
    }
    static void continuingRoute()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();long generation=h.nativeHost.arbiter.ownerGeneration(10);
        air.act();check(h.plans==1&&h.nativeHost.orders.size()==2,"one lawful plan serves both attached members");
        check(BattleClient.n(h.actor(10),"orderX")==1010&&BattleClient.n(h.actor(10),"orderY")==110,"long straight route is not chopped at the old 260 world limit or cut across its corner");
        h.observe(air);check(BattleClient.n(air.snapshot(),"routeCursor")==0,"same-frame queued MOVE is not arrival");
        h.actor(11).put("x",1010.);h.actor(11).put("y",110.);h.next(air,17000);air.act();
        check(BattleClient.n(air.snapshot(),"routeCursor")==0&&h.plans==1,"another member arriving cannot advance the leader's route or repeatedly replan");
        h.actor(10).put("x",1010.);h.actor(10).put("y",110.);h.next(air,17000);air.act();
        check(BattleClient.n(air.snapshot(),"routeCursor")==45&&h.plans==1,"later own leader arrival advances retained route without another frontier selection");
        check(BattleClient.n(h.actor(10),"orderX")==1010&&BattleClient.n(h.actor(10),"orderY")==510,"next continuous route leg follows the actual corner");
        check(h.nativeHost.arbiter.ownerGeneration(10)==generation&&air.size()==2,"route continuation preserves member leases and generation");
        air.act();check(h.plans==1&&h.nativeHost.orders.size()==4,"current valid native route orders are reused without instruction frequency inflation");
        h.actor(10).put("x",1010.);h.actor(10).put("y",510.);h.next(air,17000);air.act();
        check(h.plans==2,"whole route completion resumes new lawful exploration rather than stopping mission");air.close();
    }
    static void freshAnchorAndMissingLeader()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();h.planFrame=h.nativeHost.frame+1;air.act();
        check(h.plans==1&&h.nativeHost.orders.isEmpty(),"newer native route waits for a current own position sample");
        h.next(air,17000);air.act();check(h.plans==1&&h.nativeHost.orders.size()==2,"later own anchor consumes retained native route without planning again");
        h.nativeHost.own.remove(h.actor(10));h.actor(11).put("x",1010.);h.actor(11).put("y",110.);h.next(air,17000);air.act();
        check(air.size()==1&&h.plans==1&&BattleClient.n(h.actor(11),"orderY")==510,"currently available replacement leader continues owned route after lawful member loss");
        check(!h.nativeHost.arbiter.reserved(10),"missing member is released without a death or arrival claim");air.close();
    }
    static void invalidAndThreatenedRoute()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();h.route.set(2,9000L);air.act();
        check(h.nativeHost.orders.isEmpty(),"malformed nonadjacent route cannot fall through into a blind direct MOVE");air.close();
        h=new Host();air=h.formation();h.nativeHost.vision.put("rememberedThreats",Arrays.asList(map("x",800.,"y",110.,"range",100.)));air.act();
        check(BattleClient.n(h.actor(10),"orderX")<560,"full route stops before newly known attack corridor instead of crossing it");
        check(h.nativeHost.orders.stream().allMatch(p->p.startsWith("/command/move")),"remembered danger does not authorize an attack");air.close();
        h=new Host();air=h.formation();h.planFrame=h.nativeHost.frame+1;
        h.nativeHost.vision.put("rememberedThreats",Arrays.asList(map("x",800.,"y",110.,"range",100.)));
        h.actor(10).put("orderType","attackMove");h.actor(10).put("orderX",1010.);h.actor(10).put("orderY",110.);
        air.act();
        check(h.nativeHost.orders.size()==1&&BattleClient.n(h.actor(10),"orderX")==100,
            "waiting for a fresh route anchor still cancels dangerous old native travel after target loss");air.close();
    }
    static void currentHoldRetainsPendingEscape()throws Exception{
        Host h=new Host();AirGeneralController air=h.formation();h.planFrame=h.nativeHost.frame+1;
        h.nativeHost.vision.put("rememberedThreats",Arrays.asList(map("x",100.,"y",100.,"range",30.)));
        h.actor(10).put("orderType","move");h.actor(10).put("orderX",100.);h.actor(10).put("orderY",100.);
        air.act();check(h.plans==1&&h.nativeHost.orders.isEmpty(),"an observed current-position hold retains the future-anchor escape route");
        air.act();check(h.plans==1,"holding inside a known zone cannot repeatedly erase and replan the pending route");
        h.next(air,17000);air.act();
        check(h.plans==1&&BattleClient.n(h.actor(10),"orderX")==1010,"later own anchor resumes lawful travel away from the remembered range");
        check(h.nativeHost.orders.stream().allMatch(p->p.startsWith("/command/move")),"escape from memory retains MOVE and never authorizes an attack");air.close();
    }
    public static void main(String[] args)throws Exception{continuingRoute();freshAnchorAndMissingLeader();invalidAndThreatenedRoute();currentHoldRetainsPendingEscape();System.out.println("R8_AIR_SEARCH_OK checks="+checks+" fixtureOnly=true");}
}
