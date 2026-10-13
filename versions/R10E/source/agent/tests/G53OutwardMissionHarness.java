package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Focused synthetic travel/ownership contracts; no natural native path or combat effectiveness claim. */
public final class G53OutwardMissionHarness {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static final class Fixture extends ForceControllerHarness.Fixture {
        final GeneralCombatDirector combat=new GeneralCombatDirector(registry,this);
        final List<Map<String,Object>> plans=new ArrayList<Map<String,Object>>();
        final List<String> readPaths=new ArrayList<String>();
        Fixture()throws Exception{super();controller.setGeneralCombatDirector(combat);}
        void sample(long time){observe(time);combat.observe(state,enemies,100,100,now);}
        @Override void collect()throws Exception{combat.observe(state,enemies,100,100,now);super.collect();}
        @Override public Map<String,Object> read(String path,String event){readPaths.add(path);reads++;return plans.isEmpty()?map("status","no_frontier"):plans.remove(0);}
        void plan(int tile,double x,double y){plans.add(map("status","planned","sessionId","s","pathKnown",true,"targetTile",tile,"frontierTile",tile,"targetX",x,"targetY",y));}
        List<ForceController.Proposal> first(){List<ForceController.Proposal> out=new ArrayList<ForceController.Proposal>();for(ForceController.Proposal p:proposals)if("general:1".equals(p.owner))out.add(p);return out;}
        void accept()throws Exception{for(ForceController.Proposal p:first())p.accepted(map("frame",frame,"gameTimeMs",now,"status","queued"));}
        int firstReads(){int count=0;for(String path:readPaths)if(path.contains("unitId=10&"))count++;return count;}
        @SuppressWarnings("unchecked") Map<String,Object> mission(){return (Map<String,Object>)combat.view(a).get("outwardMission");}
    }
    static void progressAndInterruptions()throws Exception{
        Fixture f=new Fixture();f.plan(101,800,100);f.collect();
        check(f.first().size()==1&&!Boolean.TRUE.equals(f.mission().get("active")),"proposal alone neither starts mission nor supplies arrival");f.accept();
        check(Boolean.TRUE.equals(f.mission().get("active")),"native acceptance starts outward travel mission");
        f.sample(4000);int reads=f.firstReads();f.collect();check(f.firstReads()==reads&&f.first().get(0).path.contains("x=800.0"),"before arrival keep accepted destination instead of new near-centroid plan");
        f.enemy(701,200,200);f.sample(5000);f.collect();check("ADVANCE".equals(f.combat.view(f.a).get("tacticalChoice"))&&Boolean.TRUE.equals(f.mission().get("active")),"temporary current-visible fight preserves outward destination");f.accept();
        f.enemyRows.clear();f.sample(25000);reads=f.firstReads();f.collect();check(f.firstReads()==reads&&f.first().get(0).path.contains("x=800.0"),"after long temporary fight resume old frontier, not expired pause or centroid plan");
        for(long id=10;id<16;id++)f.find(id).put("x",800.);f.sample(26000);f.plan(102,800,100);f.plan(103,1500,100);f.collect();
        check(f.first().size()==1&&f.first().get(0).path.contains("x=1500.0"),"later own arrival skips current near point and takes next distant native frontier");
        check(f.readPaths.stream().anyMatch(p->p.contains("avoid=101,102")),"completed and near native tiles feed existing bounded avoid planner");
        f.accept();check(((Number)f.mission().get("targetTile")).intValue()==103,"accepted next frontier becomes persistent mission");
        f.combat.reset();check(f.combat.view(f.a).isEmpty(),"reset clears session travel memory");
    }
    static void validOrderAndStationaryHold()throws Exception{
        Fixture f=new Fixture();f.plan(110,800,100);f.collect();f.accept();
        for(long id=10;id<16;id++){f.find(id).put("orderType","attackMove");f.find(id).put("orderX",800.);f.find(id).put("orderY",100.);}
        f.sample(16000);f.collect();check(f.first().isEmpty(),"explicit same-destination native order is reused without timed progress gate or duplicate command");
        Fixture idle=new Fixture();idle.enemies.put("catalogGameJarMatched",true);idle.enemies.put("catalogSha256",TargetCatalog.catalogSha256());
        idle.enemyRows.add(map("id",901L,"type","commandCenter","hp",4000.,"x",1000.,"y",100.,"canAttack",true,"building",true,"targetDomain","SURFACE","lastSeenGameTimeMs",0L,"domainObservedAtGameTimeMs",0L));
        for(long id=10;id<16;id++){idle.find(id).put("type","tank");idle.find(id).put("hp",210.);idle.find(id).put("maxHp",210.);}
        idle.collect();check("STANDOFF".equals(idle.combat.view(idle.a).get("tacticalChoice"))&&idle.first().isEmpty(),"already stationary outside denied base margin consumes no centroid hold token");
        idle.sample(30000);idle.collect();check(idle.first().isEmpty(),"long stationary standoff does not refresh meaningless centroid move");
    }
    static void smallForceBoundary()throws Exception{
        BaseThreatZones zones=new BaseThreatZones();Map<String,Object> enemy=map("id",501L,"type","commandCenter","hp",4000.,"x",500.,"y",100.,"canAttack",true,"lastSeenGameTimeMs",0L);
        Map<String,Object> packet=map("visibleEnemies",Arrays.asList(enemy),"gameTimeMs",0L);zones.observe("s","team:0",1,0,packet,true);
        check(zones.blocks(100,100,900,100,11)==501L,"11 healthy ready ordinary ground members receive remembered base travel protection");
        check(zones.blocks(100,100,900,100,12)==null,"12 healthy ready ordinary ground members pass simple remembered-zone travel gate");
        packet.put("visibleEnemies",Collections.emptyList());zones.observe("s","team:0",2,1000,packet,true);
        check(zones.blocks(100,100,900,100,11)==501L&&zones.blocks(100,100,900,100,12)==null,"lost legal contact retains small-force travel geometry only");
        Fixture f=new Fixture();f.plan(120,900,100);f.enemyRows.add(enemy);f.collect();
        check(!f.first().isEmpty()&&"STANDOFF".equals(f.combat.view(f.a).get("tacticalChoice")),"R9 small force already inside the conservative remembered circle leaves it instead of installing an unsafe centroid hold");
        for(ForceController.Proposal p:f.first()){
            check(p.path.startsWith("/command/move?"),"denied base transit is a real MOVE withdrawal, never a new ghost attack");
            double x=Double.parseDouble(p.path.split("x=")[1].split("&")[0]),y=Double.parseDouble(p.path.split("y=")[1].split("&")[0]);
            check(Math.hypot(x-500,y-100)>TacticalOpportunity.STANDOFF_DISTANCE,"new hold is outside the unverified conservative base circle");
        }
    }
    static boolean actualRosterCrosses(String twelfthType,double hp,double progress,boolean recovering)throws Exception{
        final CommandArbiter gate=new CommandArbiter();final GeneralRegistry registry=new GeneralRegistry(gate);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();final List<Long> ids=new ArrayList<Long>();final List<ForceController.Proposal> proposals=new ArrayList<ForceController.Proposal>();
        for(long id=10;id<22;id++){ids.add(id);own.add(ForceControllerHarness.unit(id,"heavyTank",100,100));}
        Map<String,Object> last=own.get(11);last.put("type",twelfthType);last.put("hp",hp);last.put("buildProgress",progress);
        gate.observe(new CommandArbiter.Stamp("roster","team:0",1,0),ids);registry.bootstrap(Arrays.asList(ids),ids);GeneralRegistry.GeneralId general=new GeneralRegistry.GeneralId(1);registry.updateGeneralCentroid(general,100,100);
        if(recovering)registry.updateHealthRole(21,GeneralRegistry.HealthRole.RECOVERING);
        ForceController.Host host=new ForceController.Host(){public Map<String,Object> read(String path,String event){return map("status","planned","sessionId","roster","pathKnown",true,"targetTile",170,"frontierTile",170,"targetX",1000.,"targetY",100.);}
            public List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target,Map<String,Object> enemies){return actors;}
            public void collect(ForceController.Proposal proposal){proposals.add(proposal);}};
        GeneralCombatDirector combat=new GeneralCombatDirector(registry,host);Map<String,Object> state=map("sessionId","roster","frame",1L,"gameTimeMs",0L,"ownUnits",own);
        Map<String,Object> enemies=map("sessionId","roster","gameTimeMs",0L,"visibleEnemies",Arrays.asList(map("id",600L,"type","commandCenter","hp",4000.,"x",500.,"y",100.,"canAttack",true,"lastSeenGameTimeMs",0L)));
        combat.observe(state,enemies,100,100,0);gate.observe(new CommandArbiter.Stamp("roster","team:0",2,1000),ids);registry.updateGeneralCentroid(general,100,100);
        state.put("frame",2L);state.put("gameTimeMs",1000L);enemies.put("gameTimeMs",1000L);enemies.put("visibleEnemies",Collections.emptyList());combat.observe(state,enemies,100,100,1000);combat.collect(state,enemies,1000);
        return proposals.stream().anyMatch(p->p.path.startsWith("/command/attack-move")&&p.path.contains("x=1000.0"));
    }
    static void freshCountAndDetour()throws Exception{
        check(actualRosterCrosses("heavyTank",1000,1,false),"12 current healthy ready LAND members pass remembered base travel gate");
        check(!actualRosterCrosses("heavyTank",200,1,false),"wounded twelfth member cannot inflate current healthy ground count");
        check(!actualRosterCrosses("heavyTank",1000,.5,false),"unfinished twelfth member cannot inflate ready ground count");
        check(!actualRosterCrosses("heavyTank",1000,1,true),"recovering twelfth member cannot inflate ordinary combat count");
        check(!actualRosterCrosses("gunShip",1000,1,false),"AIR twelfth member cannot inflate ordinary ground count");
        check(actualRosterCrosses("scout",1000,1,false),"current ordinary HOVER member counts toward ground travel protection boundary");
        Fixture f=new Fixture();f.plan(180,800,100);f.collect();f.accept();for(long id=10;id<16;id++)f.find(id).put("x",800.);f.sample(1000);f.plan(181,200,100);f.collect();
        check(f.first().size()==1&&f.first().get(0).reason.equals("NATIVE_OUTWARD_MISSION_TERRAIN_DETOUR"),"native legal detour survives outward alternative exhaustion instead of becoming a heading deadlock");
        check(f.firstReads()<=3,"bounded detour selection uses existing planner without extra unbounded research or hidden geometry");
    }
    static void realReceiptFrame()throws Exception{
        Fixture f=new Fixture();f.plan(190,800,100);f.collect();long receiptFrame=f.frame+4;
        for(ForceController.Proposal proposal:f.first())proposal.accepted(map("frame",receiptFrame,"gameTimeMs",f.now,"status","queued"));
        check(((Number)f.mission().get("acceptedFrame")).longValue()==receiptFrame,"mission stores actual native receipt frame rather than earlier collection frame");
        for(long id=10;id<16;id++)f.find(id).put("x",800.);f.sample(1000);int reads=f.firstReads();f.collect();
        check(f.firstReads()==reads&&Boolean.TRUE.equals(f.mission().get("active")),"own position after collection but before receipt cannot retire native travel mission");
        while(f.frame<=receiptFrame)f.sample(f.now+1000);f.plan(191,1500,100);f.collect();
        check(f.first().size()==1&&f.first().get(0).path.contains("x=1500.0"),"only strictly later own receipt-frame arrival selects next native frontier");
    }
    static void exactRetreatReuseWithoutGlobalThreatGate()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();f.enemy(2100,180,20000);f.witnessOwnDamage();f.collect();Map<String,Object> rally=f.combat.view(f.a);
        double x=((Number)rally.get("rallyX")).doubleValue(),y=((Number)rally.get("rallyY")).doubleValue();
        for(ForceController.Proposal p:f.owned("general:1"))p.accepted(map("frame",f.frame,"gameTimeMs",f.now,"status","queued"));
        for(long id=10;id<16;id++){f.find(id).put("x",x+300);f.find(id).put("orderType","move");f.find(id).put("orderX",x);f.find(id).put("orderY",y);}
        f.enemy(2101,5000,100);f.sample(19000);f.collect();
        check(f.owned("general:1").isEmpty(),"strictly later same-owner generation exact native rally moves survive 15s progress expiry and new remote enemy");
        f.enemyRows.get(1).put("hp",77);f.enemyRows.get(1).put("x",5100.);f.sample(20000);f.collect();
        check(f.owned("general:1").isEmpty()&&"CURRENT_NATIVE_SAME_RALLY_POST_RECEIPT_OWNER_GENERATION".equals(f.combat.view(f.a).get("lastOrderReuseEvidence")),"remote enemy HP and movement changes do not refresh exact current retreat destinations");
        f.find(10).put("orderX",x+1000);f.sample(22000);f.collect();
        check(f.owned("general:1").size()==1&&f.owned("general:1").get(0).actors.equals(Collections.singletonList(10L)),"wrong current native retreat destination still requires replacement");
        f.owned("general:1").get(0).accepted(map("frame",f.frame,"gameTimeMs",f.now,"status","queued"));
        f.find(10).put("x",x);f.find(10).put("y",y);f.find(10).put("orderType",null);f.enemyRows.get(1).put("hp",55);f.sample(24000);f.collect();
        check(f.owned("general:1").isEmpty(),"current idle at unchanged rally holds without a duplicate move despite remote threat changes");
        f.find(11).put("x",x);f.find(11).put("y",y);f.find(11).put("orderType","attackMove");f.sample(26000);f.collect();
        check(f.owned("general:1").size()==1&&f.owned("general:1").get(0).actors.equals(Collections.singletonList(11L)),"being at rally cannot bless a wrong native attack order as idle retreat arrival");
        f.find(11).put("orderType","move");check(f.gate.transfer("general:1","retreat:test:aba",10)&&f.gate.transfer("retreat:test:aba","general:1",10),"focused fixture performs real same-owner retreat ABA");
        f.sample(28000);f.collect();check(f.owned("general:1").size()==1&&f.owned("general:1").get(0).actors.equals(Collections.singletonList(10L)),"old-generation idle arrival does not suppress replacement after owner ABA");
    }
    static void mixedRosterIdleResume()throws Exception{
        Fixture f=new Fixture();f.find(10).put("type","c_tank");f.find(11).put("type","heavyTank");f.plan(101,800,100);f.collect();
        check(f.first().size()==1&&f.first().get(0).actors.contains(10L)&&f.first().get(0).actors.contains(11L),"ordinary LAND mixed tank roster receives actual frontier command together");f.accept();
        f.find(10).put("dead",true);f.find(10).put("hp",0);f.sample(6000);f.collect();
        check(f.first().stream().anyMatch(p->p.actors.contains(11L)&&p.path.contains("x=800.0")),"surviving heavy members with no current order resume actual accepted direction after light anchor casualty");
        for(long id=11;id<16;id++){f.find(id).put("x",800.0);f.find(id).put("y",100.0);f.find(id).put("hp",400.0);}
        f.sample(12000);f.plan(102,1400,100);f.collect();
        check("CURRENT_OWN_LATER_FRONTIER_ARRIVAL".equals(f.mission().get("lastEvent"))||f.first().stream().anyMatch(p->p.path.contains("x=1400.0")),"current physical arrival is consumed even when surviving normal-role members are below fifty-percent HP");
    }
    public static void main(String[] args)throws Exception{progressAndInterruptions();validOrderAndStationaryHold();smallForceBoundary();freshCountAndDetour();realReceiptFrame();exactRetreatReuseWithoutGlobalThreatGate();mixedRosterIdleResume();System.out.println("G53OutwardMissionHarness checks="+checks+" PASS");}
}
