package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** State-machine/allocator tests with explicit synthetic packets. Not natural movement or victory. */
public final class G6EndgameHarness {
    static int checks;
    static void check(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
    static Map<String,Object> unit(long id,String type,double x,double y){return map("id",id,"type",type,"x",x,"y",y,"hp",1000.,"maxHp",1000.,"buildProgress",1.,"mobile",!"commandCenter".equals(type)&&!"landFactory".equals(type),"canAttack",!"builder".equals(type),"movementType","amphibiousJet".equals(type)?"AIR":"LAND","productionQueue",0,"orderType","none");}
    static final class Host implements StrategyDirector.Host {
        final CommandArbiter arbiter=new CommandArbiter();final List<String> orders=new ArrayList<String>();
        final List<Map<String,Object>> events=new ArrayList<Map<String,Object>>(),own=new ArrayList<Map<String,Object>>();
        final Map<Long,Boolean> wet=new HashMap<Long,Boolean>(),physical=new HashMap<Long,Boolean>();
        Map<String,Object> state,enemy,vision;long frame=1,time=1000;boolean targetVisible=true,noPlan;List<Long> route;
        Host(){arbiter.configureIndependentActors();own.add(unit(1,"commandCenter",100,100));own.add(unit(10,"amphibiousJet",100,100));own.add(unit(11,"amphibiousJet",120,100));sample();}
        void sample(){state=map("sessionId","s","frame",frame,"gameTimeMs",time,"ownUnits",own,"player",map("credits",100000.),"map",map("tilesWide",100,"tilesHigh",100,"tileWidth",20,"tileHeight",20));
            enemy=map("sessionId","s","gameTimeMs",time,"visibleEnemies",Collections.emptyList());vision=map("mapMemoryNeverSeenTiles",9000,"mapMemoryCurrentVisibleTiles",1000);
            List<Long> ids=new ArrayList<Long>();for(Map<String,Object> u:own)ids.add(((Number)u.get("id")).longValue());arbiter.observe(new CommandArbiter.Stamp("s","p",frame,time),ids);}
        public boolean endgameStrategy(){return true;}
        public String landRouteDemandStrategy(){return "NONE";}
        public Map<String,Object> costSourceStrategy(Map<String,Object> response){return map("costSourceObservationId","s:obs:"+frame,"costSourceRequestPath","/scout/observe");}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/scout/plan")){if(noPlan)return null;long id=param(path,"unitId");Map<String,Object> plan=map("status","planned","sessionId","s","builderId",id,"pathKnown",true,"targetX",800.,"targetY",300.,"targetTile",4015,"frontierTile",8015);if(route!=null)plan.put("routeTiles",route);return plan;}
            if(path.startsWith("/combat/unit-modes?unitIds=")){List<Map<String,Object>> modes=new ArrayList<Map<String,Object>>();
                for(String raw:path.split("unitIds=")[1].split("&")[0].split(","))modes.add(modePacket(Long.parseLong(raw)));
                return map("status","observed","sessionId","s","gameTimeMs",time,"units",modes);}
            if(path.startsWith("/combat/unit-modes"))return modePacket(param(path,"unitId"));
            if(path.startsWith("/combat/engagement")){List<Map<String,Object>> choices=new ArrayList<Map<String,Object>>();String ids=path.split("unitIds=")[1].split("&")[0];for(String s:ids.split(",")){long id=Long.parseLong(s);
                boolean submerged=physical.getOrDefault(id,false);choices.add(map("unitId",id,"movementType",submerged?"WATER":"AIR","compatibility",submerged?"COMPATIBLE":"INCOMPATIBLE","status","APPROACH_PATH_KNOWN","approachX",900.,"approachY",500.,"requiredMode",submerged?null:"DIVE","modeApproachStatus","APPROACH_PATH_KNOWN","modeActionId","152","modeActionReady",wet.getOrDefault(id,false),"modeApproachX",700.,"modeApproachY",400.));}
                return map("sessionId","s","gameTimeMs",time,"targetObservedAtGameTimeMs",targetVisible?time:time-1000,"targetVisible",targetVisible,"targetId",90L,"actors",choices);}
            return null;
        }
        Map<String,Object> modePacket(long id){return map("status","observed","sessionId","s","gameTimeMs",time,"unitId",id,"submergedWeaponAvailable",physical.getOrDefault(id,false),"actions",Arrays.asList(map("mode","FLY","actionId","151","available",true,"affordable",true),map("mode","DIVE","actionId","152","available",true,"affordable",true)));}
        static long param(String p,String field){return Long.parseLong(p.split(field+"=")[1].split("&")[0]);}
        public Map<String,Object> orderStrategy(String owner,String path){return orderStrategy(owner,path,Collections.emptyMap());}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context){
            orders.add(path);if(path.startsWith("/command/move")){long id=param(path,"unitId");for(Map<String,Object> a:own)if(((Number)a.get("id")).longValue()==id){a.put("orderType","move");a.put("orderX",Double.parseDouble(path.split("x=")[1].split("&")[0]));a.put("orderY",Double.parseDouble(path.split("y=")[1].split("&")[0]));}}
            if(path.startsWith("/command/attack-target"))for(String raw:path.split("unitIds=")[1].split("&")[0].split(","))
                for(Map<String,Object> a:own)if(((Number)a.get("id")).longValue()==Long.parseLong(raw)){a.put("orderType","attack");a.put("orderTargetId",param(path,"targetId"));}
            return map("frame",frame,"requestId","r"+orders.size(),"status","queued");
        }
        public void emitStrategy(String event,Map<String,Object> data){events.add(map("event",event,"data",data));}
        public void spendStrategy(String category,long cost,String type,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
    }
    static void air()throws Exception{
        Host h=new Host();AirGeneralController air=new AirGeneralController(h,h.arbiter);
        check(h.arbiter.claim("strategy:10",10)&&h.arbiter.claim("strategy:11",11),"idle specialist leases exist");
        Map<Long,Long> old=h.arbiter.snapshotGenerations("strategy:10",Arrays.asList(10L));CapabilityUnitMode a=new CapabilityUnitMode(10),b=new CapabilityUnitMode(11);
        check(air.accept(10,"strategy:10",a)&&air.accept(11,"strategy:11",b),"separate typed formation accepts actual idle specialist transfer");
        check("STALE_OWNER_GENERATION".equals(h.arbiter.validateGenerations(Arrays.asList(10L),old)),"old specialist intent invalid after transfer");
        air.observe(h.state,h.enemy,h.vision,100,100);check("JOINING".equals(((Map<?,?>)((List<?>)air.snapshot().get("roster")).get(0)).get("membership")),"same frame cannot prove joining arrival");
        h.frame++;h.time+=1000;h.sample();air.observe(h.state,h.enemy,h.vision,100,100);air.act();
        check("ATTACHED".equals(((Map<?,?>)((List<?>)air.snapshot().get("roster")).get(0)).get("membership")),"later own formation anchor witness attaches");
        check(h.orders.size()==2&&h.orders.stream().allMatch(p->p.startsWith("/command/move")),"whole air formation searches using MOVE with no enemy inference");
        air.act();check(h.orders.size()==2,"explicit current valid search orders are reused");
        check(air.snapshot().get("searchNeed") instanceof Map&&"AIR".equals(((Map<?,?>)air.snapshot().get("searchNeed")).get("domain")),"lawful fog coverage creates typed air search need");
        Map<String,Object> submarine=unit(90,"attackSubmarine",900,500);submarine.put("targetDomain","SUBMERGED");h.enemy.put("visibleEnemies",Arrays.asList(submarine));h.wet.put(10L,true);h.wet.put(11L,false);h.orders.clear();
        air.observe(h.state,h.enemy,h.vision,100,100);air.act();
        check(h.orders.stream().anyMatch(p->p.contains("unitId=10&actionId=152")),"wet member independently requests Dive");
        check(h.orders.stream().anyMatch(p->p.startsWith("/command/move?unitId=11")),"dry member independently approaches legal water");
        check(a.phase()==CapabilityUnitMode.Phase.DIVE_REQUESTED&&b.phase()==CapabilityUnitMode.Phase.MOVE_TO_WATER,"no group synchronized mode");
        check(!"SUBMERGED".equals(a.nativeMode()),"accepted Dive not physical proof");
        h.orders.clear();air.act();check(h.orders.stream().noneMatch(p->p.contains("actionId=151")),"current submerged target waits for physical Dive witness without reversing to search Fly");
        h.frame++;h.time+=1000;h.sample();h.physical.put(10L,true);h.enemy.put("visibleEnemies",Arrays.asList(submarine));air.observe(h.state,h.enemy,h.vision,100,100);h.orders.clear();air.act();
        check(h.orders.stream().anyMatch(p->p.startsWith("/command/attack-target?unitIds=10")),"ready compatible member targets the current native entity while late member remains approaching");
        h.frame++;h.time+=1000;h.sample();h.targetVisible=false;air.observe(h.state,h.enemy,h.vision,100,100);h.orders.clear();air.act();
        check(h.orders.stream().noneMatch(p->p.startsWith("/command/attack-target")),"lost contact provides no attack authorization");
        check(h.orders.stream().anyMatch(p->p.startsWith("/command/move?unitId=10")),"fog loss cancels native target attack before submerged reacquisition needs its own Fly");
        h.frame++;h.time+=1000;h.sample();air.observe(h.state,h.enemy,h.vision,100,100);h.orders.clear();air.act();
        check(h.orders.stream().anyMatch(p->p.contains("unitId=10&actionId=151")),"submerged member independently requests Fly after replacing lost target attack");
        check(b.phase()!=CapabilityUnitMode.Phase.FLY_REQUESTED,"member Fly does not change another mode");
        check(h.events.stream().anyMatch(e->"air_general_target_clear".equals(e.get("event"))),"clear logged separately from killed");
        h.own.remove(1);h.frame++;h.time+=1000;h.sample();air.observe(h.state,h.enemy,h.vision,100,100);
        check(!h.arbiter.reserved(10)&&air.size()==1,"unavailable actor lease released without death attribution");
        air.close();check(!h.arbiter.reserved(11),"formation shutdown releases remaining lease generation");
    }
    static void capital(){
        LongTermCapital c=new LongTermCapital();c.observe("s",0,1000000);c.observe("s",120000,1000000);
        check(!c.growing(),"high stock alone does not authorize capital investment");c.observe("s",240000,1000100);
        check(c.growing(),"actual long own credit growth supports capital evaluation");c.observe("new",0,2000000);
        check(!c.growing(),"session reset cannot inherit growing cash");c.observe("new",120000,2000100);c.observe("new",1000,9000000);
        check(!c.growing(),"game time rollback clears history");
        check(c.airStrength(400,200,2000)==12,"real residual income over planning horizon sets air demand without cash threshold");
        check(c.airStrength(400,200,-1)==0,"unknown native jet price does not create priced demand");
        check(c.airStrength(100,120,2000)==0,"land consumption deficit does not authorize air expansion");
    }
    static void hazardEscape()throws Exception{
        Host h=new Host();h.own.remove(2);h.sample();AirGeneralController air=new AirGeneralController(h,h.arbiter);
        h.arbiter.claim("strategy:10",10);air.accept(10,"strategy:10",new CapabilityUnitMode(10));
        h.frame++;h.time+=1000;h.sample();h.route=Arrays.asList(505L,405L,305L,304L,303L);
        h.vision.put("rememberedThreats",Arrays.asList(map("x",120.,"y",120.,"range",50.)));
        air.observe(h.state,h.enemy,h.vision,100,100);air.act();
        check(h.orders.stream().anyMatch(p->p.contains("x=70.0&y=110.0")),"native root tile is not a movement step toward enclosing hazard; consume the first contiguous monotonic outward route leg");
        check(h.orders.stream().noneMatch(p->p.startsWith("/command/attack-move")),"remembered hazard only changes exploration route and never grants attack");air.close();
    }
    static void blockedReplan()throws Exception{
        Host h=new Host();AirGeneralController air=new AirGeneralController(h,h.arbiter);h.arbiter.claim("strategy:10",10);h.arbiter.claim("strategy:11",11);
        air.accept(10,"strategy:10",new CapabilityUnitMode(10));air.accept(11,"strategy:11",new CapabilityUnitMode(11));
        h.frame++;h.time+=1000;h.sample();air.observe(h.state,h.enemy,h.vision,100,100);air.act();
        h.frame++;h.time+=1000;h.sample();h.noPlan=true;h.vision.put("rememberedThreats",Arrays.asList(map("x",600.,"y",300.,"range",150.)));h.orders.clear();
        air.observe(h.state,h.enemy,h.vision,100,100);air.act();
        check(h.orders.size()==2&&h.orders.stream().allMatch(p->p.startsWith("/command/move")),"failed safe replan cancels both dangerous native MOVE orders with current-position hold");
        check(number(h.own.get(1),"orderX",0)==100&&number(h.own.get(2),"orderX",0)==120,"native-style order replacement does not keep old dangerous endpoint");
        h.orders.clear();air.act();check(h.orders.isEmpty()&&air.size()==2,"failed replans preserve mission and leases without repeated high-priority hold flood");air.close();
    }
    public static void main(String[] args)throws Exception{air();capital();hazardEscape();blockedReplan();System.out.println("G6_ENDGAME_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");}
}
