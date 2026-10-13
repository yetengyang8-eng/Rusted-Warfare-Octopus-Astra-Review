package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Meaningful synthetic alternatives and fog/receipt contracts; no win or native combat effectiveness claim. */
public final class G51TacticalHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<String,Object> enemy(G5CombatHarness.Fixture f,long id,String type,double x,double y,double hp,boolean armed,boolean building,String domain){
        Map<String,Object> row=map("id",id,"type",type,"x",x,"y",y,"hp",hp,"canAttack",armed,"building",building,"lastSeenGameTimeMs",f.now,
                "targetDomain",domain,"domainObservedAtGameTimeMs",f.now);f.enemyRows.add(row);return row;}
    static void catalog(G5CombatHarness.Fixture f){f.enemies.put("catalogGameJarMatched",true);f.enemies.put("catalogSha256",TargetCatalog.catalogSha256());}
    static void defendedObjectiveAndOpportunity()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();catalog(f);for(long actorId=10;actorId<16;actorId++){f.find(actorId).put("type","tank");f.find(actorId).put("hp",210);f.find(actorId).put("maxHp",210);}enemy(f,1000,"commandCenter",1000,100,4000,true,true,"SURFACE");
        enemy(f,1001,"extractor",1300,1000,800,false,true,"SURFACE");f.combat.setPreferredThreats(Collections.singletonMap(f.a,1000L));f.collect();
        ForceController.Proposal selected=f.owned("general:1").get(0);check(selected.path.contains("x=1300.0")&&selected.path.contains("y=1000.0"),"visible defenses override Commander preference and select a reachable admitted economic opportunity");
        check("ADVANCE".equals(f.combat.view(f.a).get("tacticalChoice")),"alternative target remains meaningful advance without random routes");
        G5CombatHarness.Fixture blocked=new G5CombatHarness.Fixture();enemy(blocked,1000,"commandCenter",1000,100,4000,true,true,"SURFACE");blocked.frontierKnown=true;blocked.collect();
        check("STANDOFF".equals(blocked.combat.view(blocked.a).get("tacticalChoice")),"six-unit first wave holds outside weighted visible static fire exposure");
        check(blocked.owned("general:1").isEmpty(),"already stationary outside denied base margin spends no token on a centroid hold");
        for(ForceController.Proposal p:blocked.owned("general:1"))p.accepted(map("status","queued","frame",blocked.frame,"gameTimeMs",blocked.now));
        blocked.enemyRows.get(0).put("hp",1000);blocked.sample(1000);blocked.collect();
        check("STANDOFF".equals(blocked.combat.view(blocked.a).get("tacticalChoice"))&&blocked.owned("general:1").isEmpty(),"small ordinary ground force retains simple base-zone transit protection even when visible base HP weakens");
        G5CombatHarness.Fixture local=new G5CombatHarness.Fixture();enemy(local,1000,"commandCenter",180,100,4000,true,true,"SURFACE");local.collect();
        check(!local.combat.retreating(local.a)&&"STANDOFF".equals(local.combat.view(local.a).get("tacticalChoice")),"local static HP alone admits cautious reposition rather than evidence-free crisis retreat");
        local.witnessOwnDamage();local.collect();check(!local.combat.retreating(local.a),"building HP and modest damage alone no longer force a mature group home");
        for(long actorId=10;actorId<16;actorId++){Map<String,Object> actor=local.find(actorId);actor.put("hp",TacticalOpportunity.number(actor,"hp")-TacticalOpportunity.number(actor,"maxHp")*.3);}
        local.sample(local.now+1);local.collect();check(local.combat.retreating(local.a),"meaningful actual own exchange deterioration still protects the group");
    }
    static void airNeedAndBoundary()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();catalog(f);for(long id=10;id<16;id++)f.find(id).put("type","tank");
        enemy(f,1100,"gunShip",180,100,260,true,false,"AIR");f.collect();Map<String,Object> view=f.combat.view(f.a);
        check("ANTI_AIR".equals(view.get("capabilityNeed"))&&!f.combat.retreating(f.a),"lawful uncovered gunship creates counter need and objective caution without evidence-free crisis retreat");
        check(((List<?>)view.get("currentVisibleAirThreatIds")).equals(Collections.singletonList(1100L))&&((Number)view.get("compatibleAirHp")).doubleValue()==0,"air requirement carries visible current ids and honest zero coverage");
        check(!f.combat.retreating(f.b)&&"NONE".equals(f.combat.view(f.b).get("capabilityNeed")),"remote independent General is not redirected by another General local air deficit");
        f.witnessOwnDamage();f.collect();check(!f.combat.retreating(f.a)&&"ANTI_AIR".equals(f.combat.view(f.a).get("capabilityNeed")),"small current aircraft and modest own harm retain counter need without unconditional whole-army withdrawal");
        f.enemyRows.clear();f.enemies.put("enemyIntel",Arrays.asList(map("id",1100,"status","LOST_CONTACT","targetDomain","AIR","x",180,"y",100)));f.sample(1000);f.collect();
        check("NONE".equals(f.combat.view(f.a).get("capabilityNeed"))&&((List<?>)f.combat.view(f.a).get("currentVisibleAirThreatIds")).isEmpty(),"lost air contact releases current need without guessing hidden air positions or kills");
        G5CombatHarness.Fixture covered=new G5CombatHarness.Fixture();catalog(covered);enemy(covered,1101,"gunShip",180,100,260,true,false,"AIR");covered.collect();
        check(!covered.combat.retreating(covered.a)&&"NONE".equals(covered.combat.view(covered.a).get("capabilityNeed")),"current heavy tank AA compatibility prevents unconditional air panic");
        G5CombatHarness.Fixture unknown=new G5CombatHarness.Fixture();for(long id=10;id<16;id++)unknown.find(id).put("type","tank");enemy(unknown,1102,"gunShip",180,100,260,true,false,"AIR");unknown.collect();
        check("UNKNOWN".equals(unknown.combat.view(unknown.a).get("airCoverage"))&&"NONE".equals(unknown.combat.view(unknown.a).get("capabilityNeed")),"untrusted catalog is unknown rather than manufactured incompatibility");
        G5CombatHarness.Fixture stale=new G5CombatHarness.Fixture();catalog(stale);for(long id=10;id<16;id++)stale.find(id).put("type","tank");enemy(stale,1103,"gunShip",180,100,260,true,false,"AIR").put("domainObservedAtGameTimeMs",-1L);stale.collect();
        check("NONE".equals(stale.combat.view(stale.a).get("capabilityNeed")),"stale dynamic AIR domain cannot create fresh air capability need");
        G5CombatHarness.Fixture ahead=new G5CombatHarness.Fixture();catalog(ahead);for(long id=10;id<16;id++)ahead.find(id).put("type","tank");enemy(ahead,1104,"gunShip",1200,100,260,true,false,"AIR");ahead.blocked=true;ahead.frontierKnown=true;ahead.collect();
        check("FRONTIER".equals(ahead.combat.view(ahead.a).get("tacticalChoice"))&&!ahead.owned("general:1").isEmpty(),"small unattackable aircraft cannot indefinitely veto independent native frontier travel");
    }
    static void supportEscapeAndOrderPersistence()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();for(long id=10;id<16;id++)actors.add(f.find(id));
        List<Map<String,Object>> rows=Arrays.asList(map("id",1200,"type","commandCenter","x",1000.,"y",100.,"hp",3000.,"canAttack",true,"building",true));
        TacticalOpportunity.Exposure near=TacticalOpportunity.assess(actors,f.own,rows,f.enemies,1000,100,900,100),far=TacticalOpportunity.assess(actors,f.own,rows,f.enemies,1000,100,-5000,100);
        check(far.ratio>near.ratio,"long retreat distance increases defended objective exposure admission cost");
        f.own.add(map("id",1290,"hp",6000,"x",1000.,"y",100.,"canAttack",true,"buildProgress",1));
        TacticalOpportunity.Exposure supported=TacticalOpportunity.assess(actors,f.own,rows,f.enemies,1000,100,900,100);
        check(supported.ratio<near.ratio,"nearby current armed support lowers exposure without changing membership or borrowing distant forces");
        G5CombatHarness.Fixture matching=new G5CombatHarness.Fixture();enemy(matching,1201,"extractor",900,100,800,false,true,"SURFACE");matching.collect();
        for(ForceController.Proposal p:matching.owned("general:1"))p.accepted(map("status","queued","frame",matching.frame,"gameTimeMs",matching.now));
        for(long id=10;id<16;id++){matching.find(id).put("orderType","attackMove");matching.find(id).put("orderX",900.);matching.find(id).put("orderY",100.);}
        matching.sample(5000);matching.collect();check(matching.owned("general:1").isEmpty(),"effective matching accepted General attack-move survives ordinary retry interval without refresh");
        matching.sample(16000);matching.collect();check(matching.owned("general:1").isEmpty(),"explicit matching current native order remains reusable without a timed progress gate");
        matching.find(10).put("orderX",1200.);matching.sample(17000);matching.collect();check(matching.owned("general:1").size()==1&&matching.owned("general:1").get(0).actors.equals(Collections.singletonList(10L)),"different native destination still requires a real replacement for that actor");
        G5CombatHarness.Fixture aba=new G5CombatHarness.Fixture();enemy(aba,1202,"extractor",900,100,800,false,true,"SURFACE");aba.collect();
        for(ForceController.Proposal p:aba.owned("general:1"))p.accepted(map("status","queued","frame",aba.frame,"gameTimeMs",aba.now));
        for(long id=10;id<16;id++){aba.find(id).put("orderType","attackMove");aba.find(id).put("orderX",900.);aba.find(id).put("orderY",100.);}
        check(aba.gate.transfer("general:1","test:aba",10)&&aba.gate.transfer("test:aba","general:1",10),"normal command fixture performs same-owner ABA");aba.sample(1000);aba.collect();
        check(aba.owned("general:1").get(0).actors.equals(Collections.singletonList(10L)),"old-generation accepted order cannot be reused or hold a new generation inside cooldown");
    }
    static void nearbyHoldCancelsAdvanceAndRetreatReuse()throws Exception{
        G5CombatHarness.Fixture nearby=new G5CombatHarness.Fixture();enemy(nearby,1300,"extractor",900,100,1000,false,true,"SURFACE");nearby.collect();
        for(ForceController.Proposal p:nearby.owned("general:1"))p.accepted(map("status","queued","frame",nearby.frame,"gameTimeMs",nearby.now));
        for(long id=10;id<16;id++){nearby.find(id).put("orderType","attackMove");nearby.find(id).put("orderX",900.);nearby.find(id).put("orderY",100.);}
        // Synthetic overwhelming fortified objective retains the unsafe-order replacement contract;
        // a single ordinary base is intentionally no longer inflated by HP*2.
        nearby.sample(1000);enemy(nearby,1304,"commandCenter",900,100,16000,true,true,"SURFACE");nearby.collect();
        check("STANDOFF".equals(nearby.combat.view(nearby.a).get("tacticalChoice"))&&!nearby.combat.retreating(nearby.a),"new defended objective at 762world units rejects advance while outside local static trigger");
        check(nearby.owned("general:1").size()==6&&nearby.owned("general:1").get(0).actors.size()==1&&nearby.owned("general:1").get(0).path.contains("x=137.5"),"nearby hold point installs an actual command replacing unsafe accepted base advance inside ordinary cooldown");
        G5CombatHarness.Fixture stalled=new G5CombatHarness.Fixture();stalled.enemy(1301,180,20000);stalled.witnessOwnDamage();stalled.collect();Map<String,Object> retreat=stalled.combat.view(stalled.a);
        for(ForceController.Proposal p:stalled.owned("general:1"))p.accepted(map("status","queued","frame",stalled.frame,"gameTimeMs",stalled.now));
        for(long id=10;id<16;id++){stalled.find(id).put("x",300.);stalled.find(id).put("orderType","move");stalled.find(id).put("orderX",retreat.get("rallyX"));stalled.find(id).put("orderY",retreat.get("rallyY"));}
        stalled.sample(3000);stalled.collect();check(stalled.owned("general:1").isEmpty(),"later matching same-generation retreat order can be retained inside bounded progress window");
        stalled.sample(19000);stalled.collect();check(stalled.owned("general:1").isEmpty(),"explicit current same-generation retreat destination remains reusable without timed own-progress gate");
        G5CombatHarness.Fixture aba=new G5CombatHarness.Fixture();aba.enemy(1302,180,20000);aba.witnessOwnDamage();aba.collect();retreat=aba.combat.view(aba.a);
        for(ForceController.Proposal p:aba.owned("general:1"))p.accepted(map("status","queued","frame",aba.frame,"gameTimeMs",aba.now));
        for(long id=10;id<16;id++){aba.find(id).put("orderType","move");aba.find(id).put("orderX",retreat.get("rallyX"));aba.find(id).put("orderY",retreat.get("rallyY"));}
        check(aba.gate.transfer("general:1","test:retreat-aba",10)&&aba.gate.transfer("test:retreat-aba","general:1",10),"retreat reuse fixture performs real same-owner ABA");aba.sample(1000);aba.collect();
        check(aba.owned("general:1").size()==1&&aba.owned("general:1").get(0).actors.equals(Collections.singletonList(10L)),"old retreat move witness cannot suppress immediate replacement for new actor generation");
        G5CombatHarness.Fixture future=new G5CombatHarness.Fixture();future.enemy(1303,180,20000);future.witnessOwnDamage();future.collect();retreat=future.combat.view(future.a);
        for(ForceController.Proposal p:future.owned("general:1"))p.accepted(map("status","queued","frame",future.frame+4,"gameTimeMs",future.now));
        for(long id=10;id<16;id++){future.find(id).put("orderType","move");future.find(id).put("orderX",retreat.get("rallyX"));future.find(id).put("orderY",retreat.get("rallyY"));}
        future.sample(3000);future.collect();check(future.owned("general:1").size()==6,"own sample before native retreat receipt frame cannot become matching-order reuse witness");
    }
    static void legacyCoordinatesShortDefer()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();enemy(f,1400,"extractor",900,100,800,false,true,"SURFACE");f.collect();
        for(ForceController.Proposal p:f.owned("general:1"))p.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        for(long id=10;id<16;id++)f.find(id).put("orderType","attackMove");
        for(long id=10;id<16;id++)f.find(id).put("x",((Number)f.find(id).get("x")).doubleValue()+30);f.sample(1000);f.collect();
        for(long id=10;id<16;id++)f.find(id).put("x",((Number)f.find(id).get("x")).doubleValue()+30);f.sample(3000);f.collect();
        check(f.owned("general:1").isEmpty()&&DestinationUnknownDefer.EVIDENCE.equals(f.combat.view(f.a).get("lastOrderReuseEvidence")),"legacy orderType-only General can defer refresh using later net own progress without native destination claim");
        for(long id=10;id<16;id++)f.find(id).put("x",((Number)f.find(id).get("x")).doubleValue()+30);f.sample(5000);f.collect();check(f.owned("general:1").isEmpty(),"recent progress permits bounded reuse inside six-second accepted window");
        for(long id=10;id<16;id++)f.find(id).put("x",((Number)f.find(id).get("x")).doubleValue()+30);f.sample(6000);f.collect();check(!f.owned("general:1").isEmpty(),"ongoing movement cannot extend General six-second hard deadline");
        G5CombatHarness.Fixture retreat=new G5CombatHarness.Fixture();retreat.enemy(1401,180,20000);retreat.witnessOwnDamage();retreat.collect();
        for(ForceController.Proposal p:retreat.owned("general:1"))p.accepted(map("status","queued","frame",retreat.frame,"gameTimeMs",retreat.now));
        for(long id=10;id<16;id++){retreat.find(id).put("orderType","move");retreat.find(id).put("x",310.+id-10);}retreat.sample(1000);retreat.collect();
        for(long id=10;id<16;id++)retreat.find(id).put("x",280.+id-10);retreat.sample(2000);retreat.collect();check(retreat.owned("general:1").isEmpty(),"legacy retreat move can briefly defer while moving toward accepted rally");
        for(long id=10;id<16;id++)retreat.find(id).put("x",250.+id-10);retreat.sample(4001);retreat.collect();check(retreat.owned("general:1").size()==6,"retreat four-second hard deadline forces refresh despite ongoing progress");
        DestinationUnknownDefer d=new DestinationUnknownDefer(900,100,0,1,"[]");Map<String,Object> actor=map("x",100.,"y",100.);
        d.observe(actor,2,1000);actor.put("x",140.);check(d.allows(actor,3,3000,6000,2500,"[]"),"post-receipt net progress independently satisfies bounded fallback");
        actor.put("orderX",900.);check(!d.allows(actor,3,3000,6000,2500,"[]"),"partial destination field cannot use unknown-destination fallback");
        actor.put("orderY",Double.NaN);check(!d.allows(actor,3,3000,6000,2500,"[]"),"malformed destination is rejected rather than treated as missing");
        actor.put("orderX",200.);actor.put("orderY",100.);check(!d.allows(actor,3,3000,6000,2500,"[]"),"explicit different destination never enters fallback");
        actor.remove("orderX");actor.remove("orderY");actor.put("x",100.);check(!d.allows(actor,4,3500,6000,2500,"[]"),"movement away from accepted point cannot retain fallback despite recent earlier progress");
        actor.put("x",180.);check(!d.allows(actor,5,4000,6000,2500,"NEW_THREAT"),"changed lawful threat signature cancels fallback immediately");
        check(!d.allows(actor,1,3000,6000,2500,"[]"),"same/pre-receipt own frame is not fallback progress evidence");
        DestinationUnknownDefer gap=new DestinationUnknownDefer(900,100,0,1,"[]");actor.put("x",100.);gap.observe(actor,2,1000);actor.put("x",200.);
        check(!gap.allows(actor,3,5000,6000,2500,"[]"),"long own-observation gap cannot turn detected displacement into recent progress witness");
    }
    static void arrivedRetreatMembersHoldIndividually()throws Exception{
        G5CombatHarness.Fixture f=new G5CombatHarness.Fixture();f.enemy(1500,180,20000);f.witnessOwnDamage();f.collect();Map<String,Object> rally=f.combat.view(f.a);
        for(ForceController.Proposal p:f.owned("general:1"))p.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        for(long id=10;id<12;id++){f.find(id).put("x",rally.get("rallyX"));f.find(id).put("y",rally.get("rallyY"));f.find(id).put("orderType",null);}
        // Leave four members outside the arrival envelope so individual arrival cannot manufacture General quorum.
        for(long id=12;id<16;id++)f.find(id).put("x",500.);
        f.sample(2000);f.collect();check(f.owned("general:1").size()==4&&f.owned("general:1").stream().noneMatch(p->p.actors.contains(10L)||p.actors.contains(11L)),"later current arrived individuals hold without idle move spam while General is still retreating");
        check(f.combat.retreating(f.a)&&!"REGROUPING".equals(f.crisis(f.a)),"individual hold does not claim General quorum or completed retreat");
        f.enemy(1501,150,100);f.sample(2500);f.collect();check(f.owned("general:1").stream().noneMatch(p->p.actors.contains(10L)||p.actors.contains(11L)),"changed visible threats do not invalidate current arrived idle members at unchanged rally");
        G5CombatHarness.Fixture changed=new G5CombatHarness.Fixture();changed.enemy(1502,180,20000);changed.witnessOwnDamage();changed.collect();rally=changed.combat.view(changed.a);
        for(ForceController.Proposal p:changed.owned("general:1"))p.accepted(map("status","queued","frame",changed.frame,"gameTimeMs",changed.now));
        for(long id=10;id<16;id++){changed.find(id).put("x",rally.get("rallyX"));changed.find(id).put("y",rally.get("rallyY"));}changed.enemyRows.clear();changed.sample(1000);changed.sample(6000);changed.sample(10000);
        check(!changed.combat.retreating(changed.a),"fixture completes actual later own-position regroup hold before second crisis");changed.enemy(1503,180,20000);for(long id=10;id<16;id++)changed.find(id).put("hp",800.);changed.observe(11000);changed.combat.observe(changed.state,changed.enemies,700,100,changed.now);changed.controller.collect(changed.state,changed.enemies,changed.scout,700,100,changed.now);
        check(changed.owned("general:1").size()==6&&changed.owned("general:1").get(0).path.contains("x=700.0"),"new rally cannot reuse prior accepted generation position witness at old rally");
    }
    static void cautionEscapeAndFrontier()throws Exception{
        G5CombatHarness.Fixture proxy=new G5CombatHarness.Fixture();proxy.enemy(1600,180,20000);proxy.collect();
        check(!proxy.combat.retreating(proxy.a)&&Boolean.FALSE.equals(proxy.combat.view(proxy.a).get("escapeDamageWitness")),"first sight of high enemy HP alone is objective caution, not actual engaged danger");
        for(ForceController.Proposal p:proxy.owned("general:1"))check(p.path.startsWith("/command/move?unitId=")&&"GENERAL".equals(p.lane),"unengaged cautious reposition is native move, not reverse attack-move or crisis retreat");
        G5CombatHarness.Fixture frontier=new G5CombatHarness.Fixture();enemy(frontier,1601,"commandCenter",1000,100,4000,true,true,"SURFACE");frontier.frontierKnown=true;frontier.frontierX=100;frontier.frontierY=1200;frontier.collect();
        check("FRONTIER".equals(frontier.combat.view(frontier.a).get("tacticalChoice"))&&frontier.owned("general:1").get(0).path.contains("y=1200.0"),"denied base does not freeze a General when a lawful native-known alternative lies outside current visible hazard geometry");
        check(frontier.owned("general:1").get(0).reason.contains("PATH_SAFETY_UNKNOWN"),"straight-line policy exclusion does not claim dynamic native path safety");
        G5CombatHarness.Fixture blocked=new G5CombatHarness.Fixture();enemy(blocked,1602,"commandCenter",1000,100,4000,true,true,"SURFACE");blocked.frontierKnown=true;blocked.frontierX=1800;blocked.frontierY=100;blocked.collect();
        check("STANDOFF".equals(blocked.combat.view(blocked.a).get("tacticalChoice"))&&blocked.owned("general:1").isEmpty(),"frontier beyond refused base cannot bypass current visible hazard exclusion or manufacture an idle centroid order");
        for(ForceController.Proposal p:blocked.owned("general:1")){check(p.path.contains("x=137.5")&&p.path.startsWith("/command/move?"),"already outside caution margin cancels dangerous advance at own centroid without reverse relocation");p.accepted(map("status","queued","frame",blocked.frame,"gameTimeMs",blocked.now));}
        for(long id=10;id<16;id++){blocked.find(id).put("orderType","move");blocked.find(id).put("orderX",137.5);blocked.find(id).put("orderY",100.);}
        blocked.sample(4000);blocked.collect();check(blocked.owned("general:1").isEmpty(),"current matching post-receipt standoff move is reused rather than sprayed each retry");
        for(long id=10;id<16;id++)blocked.find(id).put("orderType","attackMove");blocked.sample(5000);blocked.collect();
        check(blocked.owned("general:1").size()==6,"reverse attack-move is never accepted as a standoff move witness");
        G5CombatHarness.Fixture aba=new G5CombatHarness.Fixture();enemy(aba,1603,"commandCenter",180,180,16000,true,true,"SURFACE");aba.collect();ForceController.Proposal old=aba.owned("general:1").get(0);
        check(aba.gate.transfer("general:1","test:standoff-aba",10)&&aba.gate.transfer("test:standoff-aba","general:1",10),"standoff fixture performs real same-owner ABA");
        old.accepted(map("status","queued","frame",aba.frame,"gameTimeMs",aba.now));
        check(aba.combat.drainChanges().stream().noneMatch(row->"STANDOFF_MOVE_ACCEPTED_NOT_ARRIVED".equals(row.get("reason"))),"delayed old-generation standoff move cannot install new-generation command or arrival evidence");
        G5CombatHarness.Fixture damage=new G5CombatHarness.Fixture();damage.collect();for(long id=10;id<16;id++)damage.find(id).put("hp",650);damage.sample(1000);damage.collect();
        check(damage.combat.retreating(damage.a)&&"CURRENT_OWN_LOSS_EXCHANGE".equals(damage.combat.view(damage.a).get("trigger")),"actual bounded losing own exchange still retreats with enemy attribution unknown");
        G5CombatHarness.Fixture losses=new G5CombatHarness.Fixture();losses.collect();for(long id=10;id<12;id++){losses.find(id).put("dead",true);losses.find(id).put("hp",0);}losses.sample(1000);losses.collect();
        check(losses.combat.retreating(losses.a)&&((Number)losses.combat.view(losses.a).get("observedOwnDeaths")).intValue()==2,"explicit current own deaths remain real loss evidence for escape");
        G5CombatHarness.Fixture recovery=new G5CombatHarness.Fixture();catalog(recovery);for(long id=10;id<16;id++)recovery.find(id).put("type","tank");enemy(recovery,1604,"gunShip",180,100,260,true,false,"AIR");recovery.collect();for(long id=10;id<16;id++)recovery.find(id).put("hp",650);recovery.sample(1);recovery.collect();Map<String,Object> rally=recovery.combat.view(recovery.a);
        for(ForceController.Proposal p:recovery.owned("general:1"))p.accepted(map("status","queued","frame",recovery.frame,"gameTimeMs",recovery.now));
        for(long id=10;id<16;id++){recovery.find(id).put("x",rally.get("rallyX"));recovery.find(id).put("y",rally.get("rallyY"));}
        recovery.sample(1000);check(!"REGROUPING".equals(recovery.crisis(recovery.a)),"remaining damage-with-air danger cannot enter recovery and retrigger on the same damage window");
        recovery.sample(6000);recovery.sample(10000);check(!recovery.combat.retreating(recovery.a)&&"ANTI_AIR".equals(recovery.combat.view(recovery.a).get("capabilityNeed")),"after actual harm expires and later rally hold, persistent harmless visible AIR need does not indefinitely lock crisis retreat");
        G5CombatHarness.Fixture chip=new G5CombatHarness.Fixture();catalog(chip);for(long id=10;id<16;id++)chip.find(id).put("type","tank");enemy(chip,1605,"gunShip",180,100,260,true,false,"AIR");enemy(chip,1606,"extractor",130,900,800,false,true,"SURFACE");chip.collect();chip.find(10).put("hp",999.);chip.sample(1000);chip.collect();
        check(!chip.combat.retreating(chip.a)&&Boolean.FALSE.equals(chip.combat.view(chip.a).get("escapeDamageWitness"))&&"ANTI_AIR".equals(chip.combat.view(chip.a).get("capabilityNeed")),"single chip hit with AIR deficit retains counter need without whole-General retreat");
        check("ADVANCE".equals(chip.combat.view(chip.a).get("tacticalChoice")),"mild AIR chip does not cancel another current native-eligible objective outside that AIR caution footprint");
        for(long id=10;id<16;id++)chip.find(id).put("hp",920.);chip.sample(2000);chip.collect();
        check(!chip.combat.retreating(chip.a)&&"ANTI_AIR".equals(chip.combat.view(chip.a).get("capabilityNeed")),"eight-percent own harm and small AIR deficit do not alone cancel independent ground operations");
        G5CombatHarness.Fixture oneLoss=new G5CombatHarness.Fixture();catalog(oneLoss);for(long id=10;id<16;id++)oneLoss.find(id).put("type","tank");enemy(oneLoss,1607,"gunShip",180,100,260,true,false,"AIR");oneLoss.collect();oneLoss.find(10).put("dead",true);oneLoss.find(10).put("hp",0);oneLoss.sample(1000);oneLoss.collect();
        check(!oneLoss.combat.retreating(oneLoss.a)&&((Number)oneLoss.combat.view(oneLoss.a).get("observedOwnDeaths")).intValue()==1,"one explicit loss remains evidence without letting one small aircraft veto the surviving force");
        G5CombatHarness.Fixture dangerous=new G5CombatHarness.Fixture();catalog(dangerous);for(long id=10;id<16;id++)dangerous.find(id).put("type","tank");enemy(dangerous,1608,"gunShip",180,100,10000,true,false,"AIR");dangerous.collect();dangerous.witnessOwnDamage();dangerous.collect();
        check(dangerous.combat.retreating(dangerous.a),"substantial current air exposure plus actual bounded harm retains protective withdrawal");
    }
    public static void main(String[] args)throws Exception{defendedObjectiveAndOpportunity();airNeedAndBoundary();supportEscapeAndOrderPersistence();nearbyHoldCancelsAdvanceAndRetreatReuse();legacyCoordinatesShortDefer();arrivedRetreatMembersHoldIndividually();cautionEscapeAndFrontier();System.out.println("G51TacticalHarness checks="+checks+" PASS");}
}
