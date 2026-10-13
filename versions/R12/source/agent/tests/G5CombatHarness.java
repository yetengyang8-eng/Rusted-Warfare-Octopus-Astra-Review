package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Synthetic focused contracts. No native arrival, effectiveness or victory claim. */
public final class G5CombatHarness {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class Fixture extends ForceControllerHarness.Fixture {
        final GeneralCombatDirector combat;
        double frontierX=1000,frontierY=300;
        Fixture()throws Exception{super();combat=new GeneralCombatDirector(registry,this);controller.setGeneralCombatDirector(combat);}
        void sample(long time){long before=now;observe(time);for(Map<String,Object> row:enemyRows)if(Objects.equals(GameClock.number(row.get("domainObservedAtGameTimeMs")),before))row.put("domainObservedAtGameTimeMs",now);combat.observe(state,enemies,100,100,now);}
        void witnessOwnDamage(){combat.observe(state,enemies,100,100,now);for(long id=10;id<16;id++){Map<String,Object> actor=find(id);actor.put("hp",((Number)actor.get("hp")).doubleValue()-((Number)actor.get("maxHp")).doubleValue()*.10);}sample(now+1);}
        @Override public Map<String,Object> read(String path,String event){Map<String,Object> plan=super.read(path,event);plan.put("targetX",frontierX);plan.put("targetY",frontierY);return plan;}
        @Override void collect()throws Exception{combat.observe(state,enemies,100,100,now);super.collect();}
        List<ForceController.Proposal> owned(String owner){List<ForceController.Proposal> result=new ArrayList<ForceController.Proposal>();for(ForceController.Proposal proposal:proposals)if(owner.equals(proposal.owner))result.add(proposal);return result;}
        String crisis(GeneralRegistry.GeneralId id){return (String)combat.view(id).get("crisis");}
    }
    static void independentOvermatchAndReceipts()throws Exception{
        Fixture f=new Fixture();Map<Long,Long> generation=new HashMap<Long,Long>();for(Long id:f.registry.general(f.a).members)generation.put(id,f.registry.unit(id).ownerGeneration);
        f.enemy(900,180,20000);f.enemy(902,2300,1000);f.witnessOwnDamage();f.collect();
        check("OVERMATCHED".equals(f.crisis(f.a))&&f.combat.retreating(f.a),"bounded own damage with overwhelming current-visible HP pressure triggers A retreat");
        check(!f.combat.retreating(f.b)&&f.owned("general:2").size()==1&&"GENERAL".equals(f.owned("general:2").get(0).lane),"B independently continues current-visible offense while A retreats");
        List<ForceController.Proposal> moves=f.owned("general:1");check(moves.size()==6,"A retreats attached actors separately");
        for(ForceController.Proposal p:moves){check(p.actors.size()==1&&p.path.startsWith("/command/move?unitId=")&&p.priority==85,"retreat uses supported one-actor native move below Critical and above LR");
            p.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));}
        check("OVERMATCHED".equals(f.crisis(f.a)),"accepted move receipt cannot claim arrival or recovery");
        for(Long id:f.registry.general(f.a).members)check(f.registry.unit(id).owner.equals("general:1")&&generation.get(id)==f.registry.unit(id).ownerGeneration,"retreat preserves General owner and generation");
        f.proposals.clear();f.collect();check(f.owned("general:1").isEmpty(),"actual receipts begin cooldown; collect alone never does");
        Fixture borrowing=new Fixture();for(long id=30;id<34;id++){borrowing.find(id).put("hp",3000);borrowing.find(id).put("maxHp",3000);}
        borrowing.enemy(901,180,11000);borrowing.witnessOwnDamage();borrowing.collect();
        check(borrowing.combat.retreating(borrowing.a)&&borrowing.registry.general(borrowing.a).members.size()==6&&borrowing.lane("LOCAL_RESPONSE")==null,"LR cannot borrow a retreating General to barely satisfy its weaker-priority coverage envelope");
        Fixture noReceipt=new Fixture();noReceipt.enemy(900,180,20000);noReceipt.witnessOwnDamage();noReceipt.collect();noReceipt.proposals.clear();noReceipt.collect();check(noReceipt.owned("general:1").size()==6,"unaccepted retreat proposals stay eligible on same observation");
    }
    static void ownDamageAndUnknown()throws Exception{
        Fixture f=new Fixture();f.enemy(910,900,1000);f.collect();
        for(long id=10;id<16;id++)f.find(id).put("hp",650);
        f.sample(1000);f.collect();check("LOSING_EXCHANGE".equals(f.crisis(f.a)),"current own HP damage window prevents sustained losing advance");
        check(f.owned("general:1").get(0).lane.equals("GENERAL_RETREAT"),"losing exchange switches normal offense to withdrawal");
        Fixture stale=new Fixture();stale.enemy(920,180,100000);stale.enemies.put("gameTimeMs",-1L);stale.frontierKnown=true;stale.collect();
        check(!stale.combat.retreating(stale.a)&&stale.owned("general:1").isEmpty()&&stale.reads==0,"stale/LastObservation cannot create overmatch or new attack/frontier");
        Fixture staleRow=new Fixture();staleRow.enemy(921,180,100000);staleRow.enemyRows.get(0).put("lastSeenGameTimeMs",-1L);staleRow.collect();
        check(!staleRow.combat.retreating(staleRow.a)&&!Boolean.TRUE.equals(staleRow.combat.view(staleRow.a).get("visibleCurrent")),"stale visible row cannot manufacture a complete calm observation");
        Fixture lost=new Fixture();lost.enemy(930,900,1000);lost.collect();lost.enemyRows.clear();lost.enemies.put("enemyIntel",Arrays.asList(map("id",930,"status","LOST_CONTACT","lastKnownHp",0)));lost.sample(1000);lost.collect();
        check(((Number)lost.combat.view(lost.a).get("weakVisibleHpDecrease")).doubleValue()==0&&((Number)lost.combat.view(lost.a).get("observedOwnDeaths")).intValue()==0,"lost contact is neither visible HP decrease nor death");
        Fixture missing=new Fixture();missing.collect();missing.own.remove(missing.find(10));missing.registry.removeUnit(10);missing.sample(1000);
        check(((Number)missing.combat.view(missing.a).get("observedOwnDeaths")).intValue()==0,"absent own record is not guessed dead");
    }
    static void recoveryRequiresLaterPositionAndHysteresis()throws Exception{
        Fixture f=new Fixture();f.enemy(940,180,20000);f.witnessOwnDamage();f.collect();List<ForceController.Proposal> retreats=f.owned("general:1");
        for(ForceController.Proposal p:retreats)p.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        double x=((Number)f.combat.view(f.a).get("rallyX")).doubleValue(),y=((Number)f.combat.view(f.a).get("rallyY")).doubleValue();
        f.enemyRows.clear();for(long id=10;id<16;id++){f.find(id).put("x",x);f.find(id).put("y",y);}
        f.combat.observe(f.state,f.enemies,100,100,f.now);check(!"REGROUPING".equals(f.crisis(f.a)),"same receipt frame own geometry does not prove later arrival");
        f.sample(1000);check(!"REGROUPING".equals(f.crisis(f.a)),"recent meaningful own harm keeps recovery gated despite later rally arrival");
        f.sample(6000);check("REGROUPING".equals(f.crisis(f.a))&&f.combat.retreating(f.a),"later current own rally positions after damage expiry begin regrouping without declaring safety");
        f.sample(9000);check(f.combat.retreating(f.a),"regroup hold applies hysteresis before releasing advance");
        f.sample(10000);check("NORMAL".equals(f.crisis(f.a))&&!f.combat.retreating(f.a),"stable current low pressure and own-health hold restores independent task eligibility");
        check("UNKNOWN".equals(f.combat.view(f.a).get("rallySafety")),"rally geometry does not assert dynamic safety");
        check("s".equals(f.combat.view(f.a).get("sourceSessionId"))&&((Number)f.combat.view(f.a).get("sourceFrame")).longValue()==f.frame,"Commander receives exact current own-source stamp rather than refreshed stale rally identity");
    }
    static void generationAndChunks()throws Exception{
        Fixture aba=new Fixture();aba.enemy(950,180,20000);aba.witnessOwnDamage();aba.collect();ForceController.Proposal old=aba.owned("general:1").get(0);long actor=old.actors.get(0);
        check(aba.gate.transfer("general:1","test:aba",actor)&&aba.gate.transfer("test:aba","general:1",actor),"fixture performs true same-owner ABA through production arbiter generations");
        old.accepted(map("status","queued","frame",aba.frame,"gameTimeMs",aba.now));
        check(aba.combat.drainChanges().stream().noneMatch(row->"RETREAT_ACCEPTED_NOT_ARRIVED".equals(row.get("reason"))),"delayed old-generation receipt cannot update retreat witness");
        final CommandArbiter gate=new CommandArbiter();final GeneralRegistry registry=new GeneralRegistry(gate);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();final List<Long> ids=new ArrayList<Long>();
        for(long id=100;id<200;id++){own.add(ForceControllerHarness.unit(id,"heavyTank",2000,100));ids.add(id);}
        gate.observe(new CommandArbiter.Stamp("chunks","team:0",1,0),ids);registry.bootstrap(Arrays.asList(ids),ids);GeneralRegistry.GeneralId g=new GeneralRegistry.GeneralId(1);registry.updateGeneralCentroid(g,2000,100);
        final List<ForceController.Proposal> proposals=new ArrayList<ForceController.Proposal>();ForceController.Host host=new ForceController.Host(){
            public Map<String,Object> read(String path,String event){return Collections.emptyMap();}
            public List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target,Map<String,Object> enemies){return actors;}
            public void collect(ForceController.Proposal p){proposals.add(p);}};
        GeneralCombatDirector combat=new GeneralCombatDirector(registry,host);Map<String,Object> state=map("sessionId","chunks","frame",1L,"gameTimeMs",0L,"ownUnits",own);
        Map<String,Object> enemies=map("sessionId","chunks","gameTimeMs",0L,"visibleEnemies",Arrays.asList(map("id",1000,"hp",1000,"x",2300,"y",100,"canAttack",true,"lastSeenGameTimeMs",0L)));
        combat.observe(state,enemies,100,100,0);combat.collect(state,enemies,0);
        check(proposals.size()==3&&proposals.get(0).actors.size()==48&&proposals.get(1).actors.size()==48&&proposals.get(2).actors.size()==4,"large General attacks are protocol-valid chunks capped at48");
        proposals.get(0).accepted(map("status","queued","frame",1L,"gameTimeMs",0L));proposals.clear();combat.collect(state,enemies,0);
        check(proposals.size()==2&&proposals.get(0).actors.size()==48&&proposals.get(1).actors.size()==4,"only actually accepted chunk enters cooldown; unaccepted actors remain due");
    }
    static void boundsGapsAndMeaningfulCommands()throws Exception{
        Fixture bounded=new Fixture();bounded.state.put("map",map("width",300.,"height",300.));bounded.enemy(960,100,20000);bounded.witnessOwnDamage();bounded.collect();
        Map<String,Object> rally=bounded.combat.view(bounded.a);check(Boolean.TRUE.equals(rally.get("rallyBoundsKnown"))&&((Number)rally.get("rallyX")).doubleValue()==299,"retreat away vector clamps below native float map upper bound");
        Fixture unknownMap=new Fixture();unknownMap.enemy(961,100,20000);unknownMap.witnessOwnDamage();unknownMap.collect();
        check("GEOMETRY_BOUNDS_UNKNOWN".equals(unknownMap.combat.view(unknownMap.a).get("rallyGeometry"))&&"UNKNOWN".equals(unknownMap.combat.view(unknownMap.a).get("rallySafety")),"missing map bounds remain explicit unknown geometry and safety");
        Fixture gap=new Fixture();gap.enemy(962,900,1000);gap.collect();for(long id=10;id<16;id++)gap.find(id).put("hp",650);
        gap.sample(10000);gap.collect();Map<String,Object> view=gap.combat.view(gap.a);
        check(!gap.combat.retreating(gap.a)&&((Number)view.get("recentOwnDamage")).doubleValue()==0&&((Number)view.get("unknownWindowOwnHpDecrease")).doubleValue()==2100,"ten-second HP gap is detected difference with UNKNOWN window, never fabricated five-second exchange rate");
        check("UNKNOWN_OBSERVATION_GAP".equals(view.get("hpDeltaWindow"))&&((Number)view.get("lastObservationGapMs")).longValue()==10000,"long gap and observation detection semantics are exposed");
        for(long id=10;id<16;id++)gap.find(id).put("hp",300);gap.sample(11000);gap.collect();
        check(gap.combat.retreating(gap.a)&&((Number)gap.combat.view(gap.a).get("currentHealthy")).intValue()==0,"subsequent bounded damage responds immediately and NORMAL30percentHP never counts healthy");
        Fixture small=new Fixture();small.enemy(963,900,1000);small.collect();
        for(ForceController.Proposal p:small.proposals)p.accepted(map("status","queued","frame",small.frame,"gameTimeMs",small.now));
        long command=((Number)small.combat.view(small.a).get("commandRevision")).longValue();
        for(long id=10;id<16;id++)small.find(id).put("hp",990);small.sample(1000);small.collect();
        check(small.owned("general:1").isEmpty()&&((Number)small.combat.view(small.a).get("commandRevision")).longValue()==command,"small HP observation revision does not reset normal accepted-command cadence");
        Fixture matching=new Fixture();matching.enemy(964,180,20000);matching.witnessOwnDamage();matching.collect();Map<String,Object> move=matching.combat.view(matching.a);
        for(ForceController.Proposal p:matching.owned("general:1"))p.accepted(map("status","queued","frame",matching.frame,"gameTimeMs",matching.now));
        for(long id=10;id<16;id++){matching.find(id).put("orderType","move");matching.find(id).put("orderX",move.get("rallyX"));matching.find(id).put("orderY",move.get("rallyY"));}
        matching.sample(3000);matching.collect();check(matching.owned("general:1").isEmpty(),"current matching retreat move suppresses duplicate commands even after retry interval");
    }
    static void joiningOverrideRedirectAndGeneration()throws Exception{
        Fixture f=new Fixture();f.sample(1000);check(f.registry.requestJoin(33,f.a)&&f.registry.beginJoining(33),"redirect fixture begins formal joining");f.collect();
        ForceController.Proposal original=f.lane("JOINING");original.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        f.find(33).put("orderType","move");f.find(33).put("orderX",f.registry.general(f.a).x);f.find(33).put("orderY",f.registry.general(f.a).y);
        f.sample(2000);check(f.registry.setJoinTargetOverride(f.a,f.gate.stamp(),500,500),"current own-source rally override installed");f.collect();
        ForceController.Proposal redirect=f.lane("JOINING");check(redirect!=null&&redirect.path.contains("x=500.0")&&redirect.reason.equals("MOVE_TO_HOME_SIDE_RALLY_DYNAMIC_UNKNOWN"),"changed home-side target redirects immediately inside ordinary eight-second cooldown");
        check(f.registry.unit(33).joinAcceptedFrame<f.frame,"collected redirect never updates actual join receipt frame");redirect.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        check(f.registry.unit(33).membership==GeneralRegistry.Membership.JOINING&&!f.registry.observeJoinPosition(33,f.gate.stamp(),500,500,180),"redirect receipt still cannot prove arrival in its receipt frame");
        f.sample(2500);f.find(33).put("orderX",500.0);f.find(33).put("orderY",500.0);
        f.registry.setJoinTargetOverride(f.a,f.gate.stamp(),500,500);f.collect();check(f.lane("JOINING")==null,"later own native order reflects unchanged accepted rally target and retains its receipt cooldown");
        f.sample(3000);f.registry.setJoinTargetOverride(f.a,f.gate.stamp(),600,600);f.collect();ForceController.Proposal moved=f.lane("JOINING");
        check(moved!=null&&moved.path.contains("x=600.0"),"a second explicit rally change remains immediately actionable");long oldFrame=f.registry.unit(33).joinAcceptedFrame;
        check(f.gate.transfer("join:1","test:join-aba",33)&&f.gate.transfer("test:join-aba","join:1",33),"joining receipt fixture performs real same-owner ABA");
        moved.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));check(f.registry.unit(33).joinAcceptedFrame==oldFrame,"old-generation redirect receipt cannot update join acceptance or target cooldown");
    }
    static void combatPlayerBoundary()throws Exception{
        Fixture foreignKey=new Fixture();foreignKey.enemy(970,180,20000);foreignKey.enemies.put("playerKey","team:1");foreignKey.collect();
        check(!Boolean.TRUE.equals(foreignKey.combat.view(foreignKey.a).get("visibleCurrent"))&&!foreignKey.combat.retreating(foreignKey.a)&&foreignKey.owned("general:1").isEmpty(),"foreign enemy endpoint playerKey cannot become General pressure or attack knowledge");
        Fixture foreignTeam=new Fixture();foreignTeam.enemy(971,180,20000);foreignTeam.enemies.put("player",map("teamId",1));foreignTeam.collect();
        check(!Boolean.TRUE.equals(foreignTeam.combat.view(foreignTeam.a).get("visibleCurrent"))&&!foreignTeam.combat.retreating(foreignTeam.a)&&foreignTeam.owned("general:1").isEmpty(),"native endpoint foreign player.teamId is rejected against Registry player even when own state omits team");
        Fixture conflict=new Fixture();conflict.enemy(972,180,20000);conflict.enemies.put("playerKey","team:0");conflict.enemies.put("player",map("teamId",1));conflict.collect();
        check(!Boolean.TRUE.equals(conflict.combat.view(conflict.a).get("visibleCurrent")),"matching playerKey does not override a conflicting native team field");
        Fixture matching=new Fixture();matching.enemy(973,180,20000);matching.enemies.put("playerKey","team:0");matching.enemies.put("player",map("teamId",0));matching.witnessOwnDamage();matching.collect();
        check(Boolean.TRUE.equals(matching.combat.view(matching.a).get("visibleCurrent"))&&matching.combat.retreating(matching.a),"matching endpoint owner fields retain current legal native knowledge");
        Fixture legacy=new Fixture();legacy.enemy(974,180,20000);legacy.witnessOwnDamage();legacy.collect();check(Boolean.TRUE.equals(legacy.combat.view(legacy.a).get("visibleCurrent"))&&legacy.combat.retreating(legacy.a),"legacy endpoint without optional player fields retains validated session/frame/time protocol");
    }
    public static void main(String[] args)throws Exception{independentOvermatchAndReceipts();ownDamageAndUnknown();recoveryRequiresLaterPositionAndHysteresis();generationAndChunks();boundsGapsAndMeaningfulCommands();joiningOverrideRedirectAndGeneration();combatPlayerBoundary();System.out.println("G5CombatHarness checks="+checks+" PASS");}
}
