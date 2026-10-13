package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Synthetic legal packets and own-position witnesses. No natural clearing/victory claim. */
public final class R10AirFormationsHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Host implements StrategyDirector.Host {
        final G6EndgameHarness.Host nativeHost=new G6EndgameHarness.Host();
        final List<Map<String,Object>> dispatches=new ArrayList<Map<String,Object>>();
        int splits,plans;boolean separate=true,missingPlan,stalePlan,noInformation;
        String landDemand="NONE";
        final Set<Long> unknownPhysical=new HashSet<Long>();
        final Map<Long,CapabilityUnitMode> initialModes=new HashMap<Long,CapabilityUnitMode>();
        Host(int count){
            for(long id=12;id<10+count;id++)nativeHost.own.add(G6EndgameHarness.unit(id,"amphibiousJet",100+(id-10)*5,100));
            // Real /state ownUnits omit movementType. Navigation hints are not physical proof.
            for(Map<String,Object> u:nativeHost.own)u.remove("movementType");nativeHost.sample();
        }
        public boolean endgameStrategy(){return true;}
        public String landRouteDemandStrategy(){return landDemand;}
        public Map<String,Object> costSourceStrategy(Map<String,Object> p){return nativeHost.costSourceStrategy(p);}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/combat/unit-modes")&&unknownPhysical.contains(G6EndgameHarness.Host.param(path,"unitId")))
                return map("sessionId","s","gameTimeMs",nativeHost.time,"unitId",G6EndgameHarness.Host.param(path,"unitId"),"actions",Collections.emptyList());
            if(path.startsWith("/scout/plan")){
                check(path.contains("routeScope=air-search"),"only scoped native lawful AIR plans are used");
                boolean split="air_general_split_search_plan".equals(event);if(split){splits++;check(path.contains("avoid="),"split reserves already assigned lawful search frontiers");}else plans++;
                if(split&&missingPlan)return null;
                long unit=G6EndgameHarness.Host.param(path,"unitId");List<Long> route=new ArrayList<Long>();
                int col=split&&separate?(splits==1?5:75):50,row=split&&separate?(splits==1?65:5):5;
                for(long x=5;x<=col;x++)route.add(x*100+5);for(long y=6;y<=row;y++)route.add(col*100+y);
                if(split&&separate&&splits==1)for(long x=6;x<=25;x++)route.add(x*100+row);
                long last=route.get(route.size()-1);
                return map("status","planned","sessionId",split&&stalePlan?"foreign":"s","builderId",unit,
                    "frame",nativeHost.frame,"gameTimeMs",nativeHost.time,"movementType","AIR","pathKnown",true,
                    "potentialInfoScore",split&&noInformation?0:10,
                    "routeScope","air-search","routeComplete",true,"routeTiles",route,
                    "targetX",(last/100+.5)*20,"targetY",(last%100+.5)*20,"frontierTile",last,"targetTile",last);
            }
            if(path.startsWith("/combat/engagement")){
                List<Map<String,Object>> choices=new ArrayList<Map<String,Object>>();
                for(String id:path.split("unitIds=")[1].split("&")[0].split(","))choices.add(map("unitId",Long.parseLong(id),
                    "movementType","AIR","compatibility","COMPATIBLE","status","APPROACH_PATH_KNOWN","approachX",300.,"approachY",100.));
                return map("sessionId","s","gameTimeMs",nativeHost.time,"targetObservedAtGameTimeMs",nativeHost.time,
                    "targetVisible",true,"targetId",90L,"actors",choices);
            }return nativeHost.readStrategy(path,event);
        }
        public Map<String,Object> orderStrategy(String owner,String path){return orderStrategy(owner,path,Collections.emptyMap());}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context){
            String field=path.contains("unitIds=")?"unitIds":"unitId";
            String ids=path.split(field+"=")[1].split("&")[0];
            for(String id:ids.split(","))check(nativeHost.arbiter.owns(owner,Long.parseLong(id)),"each dispatched actor belongs to this independent owner");
            dispatches.add(map("owner",owner,"path",path,"context",context));
            if(path.startsWith("/command/attack-target"))for(String id:ids.split(",")){
                Map<String,Object> a=actor(Long.parseLong(id));a.put("orderType","attack");
                a.put("orderTargetId",G6EndgameHarness.Host.param(path,"targetId"));
            }
            return nativeHost.orderStrategy(owner,path,context);
        }
        public void emitStrategy(String kind,Map<String,Object> data){nativeHost.emitStrategy(kind,data);}
        public void spendStrategy(String c,long price,String type,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
        Map<String,Object> actor(long id){for(Map<String,Object> u:nativeHost.own)if(BattleClient.id(u)==id)return u;throw new AssertionError("missing actor");}
        AirGeneralController formation()throws Exception{
            AirGeneralController air=new AirGeneralController(this,nativeHost.arbiter);
            for(Map<String,Object> u:nativeHost.own)if("amphibiousJet".equals(u.get("type"))){long id=BattleClient.id(u);
                nativeHost.arbiter.claim("strategy:"+id,id);CapabilityUnitMode mode=initialModes.get(id);
                check(air.accept(id,"strategy:"+id,mode==null?new CapabilityUnitMode(id):mode),"idle specialist leases transfer");}
            next(air);air.act();return air;
        }
        void next(AirGeneralController air)throws Exception{nativeHost.frame++;nativeHost.time+=1000;nativeHost.sample();observe(air);}
        void observe(AirGeneralController air)throws Exception{air.observe(nativeHost.state,nativeHost.enemy,nativeHost.vision,100,100);}
    }
    static List<Map<String,Object>> formations(AirGeneralController air){return items(air.snapshot(),"formations");}
    static void multipleOwnersAndRouteResume()throws Exception{
        Host h=new Host(15);AirGeneralController air=h.formation();long before=h.nativeHost.arbiter.ownerGeneration(19);
        Map<Long,Long> old=h.nativeHost.arbiter.snapshotGenerations(AirGeneralController.OWNER,Arrays.asList(19L));
        h.next(air);air.act();
        check(formations(air).size()==2,"enough current healthy members and a separate native frontier create an independent formation");
        check(h.nativeHost.arbiter.owns("air-general:2",19)&&h.nativeHost.arbiter.ownerGeneration(19)>before,"split advances actual ownership generation");
        check("STALE_OWNER_GENERATION".equals(h.nativeHost.arbiter.validateGenerations(Arrays.asList(19L),old)),"old-owner intent cannot survive split");
        check("JOINING".equals(items(formations(air).get(1),"roster").get(0).get("membership")),"same-frame split receipt is not new formation attachment");
        check(h.nativeHost.orders.size()==15,"same-frame transfer does not issue a second search order for joining actors already at their new anchor");
        h.next(air);air.act();
        check(formations(air).size()==3&&air.size()==15,"third independent search duty preserves all members without a fixed fleet fraction");
        h.next(air);air.act();
        Set<String> owners=new HashSet<String>();for(Map<String,Object> f:formations(air)){
            owners.add(String.valueOf(f.get("owner")));check(Boolean.TRUE.equals(f.get("searchLegKnown")),"each formation owns its own continuing leg");
            check(items(f,"roster").stream().allMatch(m->"ATTACHED".equals(m.get("membership"))),"later own anchor witness attaches each split member");}
        check(owners.size()==3,"three disjoint independent owners are retained");
        check(h.nativeHost.own.stream().allMatch(u->!u.containsKey("movementType")),"split/search preserve missing native state movementType as UNKNOWN without fabricating AIR");
        List<Long> frontiers=new ArrayList<Long>();for(Map<String,Object> f:formations(air))frontiers.add(((Number)f.get("routeFrontierTile")).longValue());
        check(new HashSet<Long>(frontiers).size()==3,"owners retain different native search frontiers");
        Map<String,Object> contact=G6EndgameHarness.unit(90,"tank",100,100);contact.put("targetDomain","LAND");
        h.nativeHost.enemy.put("visibleEnemies",Arrays.asList(contact));h.observe(air);h.dispatches.clear();air.act();
        List<Map<String,Object>> attacks=new ArrayList<Map<String,Object>>();for(Map<String,Object> d:h.dispatches)if(String.valueOf(d.get("path")).startsWith("/command/attack-target"))attacks.add(d);
        check(attacks.size()==1&&AirGeneralController.OWNER.equals(attacks.get(0).get("owner")),"one current contact is assigned to one nearest owner instead of attracting the whole fleet");
        check(formations(air).get(0).get("routeFrontierTile").equals(frontiers.get(0)),"temporary lawful combat retains the suspended route");
        check(formations(air).stream().skip(1).allMatch(f->f.get("targetId")==null),"other owners retain independent search duty");
        h.next(air);h.dispatches.clear();air.act();
        check(h.dispatches.stream().noneMatch(d->String.valueOf(d.get("path")).startsWith("/command/attack-target")),"absent contact never authorizes another attack");
        check(h.dispatches.stream().anyMatch(d->AirGeneralController.OWNER.equals(d.get("owner"))&&String.valueOf(d.get("path")).startsWith("/command/move"))
            &&formations(air).get(0).get("contactArea") instanceof Map,"contact loss replaces attack with owned last-known area reacquisition while retaining the suspended route");
        check(h.plans==1&&frontiers.equals(Arrays.asList(((Number)formations(air).get(0).get("routeFrontierTile")).longValue(),((Number)formations(air).get(1).get("routeFrontierTile")).longValue(),((Number)formations(air).get(2).get("routeFrontierTile")).longValue())),"resume does not select unrelated frontiers");
        Map<String,Object> leader=formations(air).get(1);long id=((Number)leader.get("routeLeader")).longValue();
        h.actor(id).put("x",leader.get("searchLegX"));h.actor(id).put("y",leader.get("searchLegY"));h.next(air);
        check(BattleClient.n(formations(air).get(1),"routeCursor")>0&&BattleClient.n(formations(air).get(0),"routeCursor")==0,"later own arrival advances only that independent owner's cursor");
        h.nativeHost.own.remove(h.actor(19));h.next(air);check(air.size()==14&&!h.nativeHost.arbiter.reserved(19),"unavailable split member releases only its own lease without death inference");
        for(long member=20;member<25;member++)h.nativeHost.own.remove(h.actor(member));h.next(air);
        check(formations(air).size()==2&&air.size()==9,"an unavailable whole child retires its search reservation without merging the remaining owners");
        check("air-general:3".equals(formations(air).get(1).get("owner")),"independent formation IDs remain stable across retirement");
        check(h.nativeHost.events.stream().anyMatch(e->"air_general_formation_retired".equals(e.get("event"))),"retirement is observable and never reported as confirmed kills");
        air.close();for(long member=10;member<25;member++)check(!h.nativeHost.arbiter.reserved(member),"closing facade releases every independent formation");
    }
    static void noInventedSplits()throws Exception{
        for(int scenario=0;scenario<6;scenario++){
            Host h=new Host(scenario==0?8:9);AirGeneralController air=h.formation();
            if(scenario==1)h.separate=false;if(scenario==2)h.missingPlan=true;if(scenario==3)h.stalePlan=true;
            if(scenario==5)h.noInformation=true;
            h.next(air);if(scenario==4){h.actor(18).put("hp",100.);h.observe(air);}
            air.act();check(formations(air).size()==1,"small/recovering roster, duplicate, missing or foreign lawful plan cannot invent parallel search work");
            for(long id=10;id<10+(scenario==0?8:9);id++)check(h.nativeHost.arbiter.owns(AirGeneralController.OWNER,id),"deferred splitting preserves original leases");air.close();
        }
    }
    static void groundDutyDoesNotSuppressIdleAirSearch()throws Exception{
        Host h=new Host(9);h.landDemand="KNOWN";AirGeneralController air=h.formation();
        check(!"AIR".equals(((Map<?,?>)air.snapshot().get("searchNeed")).get("domain")),"whole-map typed SearchAreaNeed does not fabricate no-ground-target evidence");
        h.next(air);air.act();check(formations(air).size()==2,"two native fog frontiers justify independent idle AIR duty while Ground retains its own legal target");air.close();
    }
    static void physicalAndCurrentActorEligibility()throws Exception{
        for(int scenario=0;scenario<5;scenario++){
            Host h=new Host(9);
            if(scenario==0)h.unknownPhysical.add(18L);
            if(scenario==1)h.nativeHost.physical.put(18L,true);
            if(scenario==2){
                CapabilityUnitMode pending=new CapabilityUnitMode(18);
                pending.witness(map("unitId",18L,"gameTimeMs",h.nativeHost.time,"submergedWeaponAvailable",false),null,h.nativeHost.time);
                pending.flyAccepted();h.initialModes.put(18L,pending);h.unknownPhysical.add(18L);
            }
            AirGeneralController air=h.formation();h.next(air);
            if(scenario==3)h.nativeHost.own.remove(h.actor(18));
            if(scenario==4){h.actor(18).put("buildProgress",.5);h.observe(air);}
            h.splits=0;air.act();
            check(formations(air).size()==1&&h.splits==0,"UNKNOWN/submerged/pending Fly or currently unavailable own actor cannot qualify for splitting");
            if(scenario<3){Map<String,Object> mode=null;for(Map<String,Object> m:items(air.snapshot(),"roster"))if(number(m,"unitId",-1)==18)mode=BattleClient.obj(m.get("mode"));
                check(mode!=null&&!"AIR_READY".equals(mode.get("phase")),"only completed native AIR_READY physical evidence qualifies, independent of missing desired movement");
                if(scenario==0)check("UNKNOWN".equals(mode.get("nativeMode")),"missing physical field remains UNKNOWN");
                if(scenario==1)check("SUBMERGED".equals(mode.get("nativeMode")),"true native submerged witness stays separate from a requested Fly");
                if(scenario==2)check("FLY_REQUESTED".equals(mode.get("phase"))&&"AIR".equals(mode.get("nativeMode")),"pending Fly plus last native AIR witness is not a current completed phase");
            }
            check(h.nativeHost.own.stream().allMatch(u->!u.containsKey("movementType")),"eligibility never invents omitted native own-state movement fields");air.close();
        }
    }
    public static void main(String[] args)throws Exception{multipleOwnersAndRouteResume();noInventedSplits();groundDutyDoesNotSuppressIdleAirSearch();physicalAndCurrentActorEligibility();System.out.println("R10_AIR_FORMATIONS_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");}
}
