package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.GeneralRegistry.*;
import static io.rwagent.client.G53FreeResponseHarness.*;

/** Synthetic legal observations/compatibility and real owner transitions; no native combat claim. */
public final class R10TemporaryResponseHarness {
    static int count;
    static void verify(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    static Fixture nearby(int free){Fixture f=new Fixture(free);for(long id=1;id<=6;id++)f.own.get((int)id-1).put("x",1800.);f.observe();return f;}
    static Map<String,Object> own(Fixture f,long id){for(Map<String,Object> row:f.own)if(((Number)row.get("id")).longValue()==id)return row;return null;}
    static void invariant(Fixture f){Set<Long> counted=new HashSet<Long>();for(GeneralView g:f.registry.generals()){
        for(Long id:g.members){UnitView u=f.registry.unit(id);verify(counted.add(id)&&g.id.equals(u.generalId)&&u.membership==Membership.ATTACHED,"member has one actual roster");}
        for(Long id:g.reservations){UnitView u=f.registry.unit(id);verify(counted.add(id)&&g.id.equals(u.reservedGeneralId)&&u.allocation==Allocation.PENDING_JOIN,"borrow/join preserves one original reservation");}}
        for(UnitView u:f.registry.units())verify(f.gate.owns(u.owner,u.unitId),"registry and arbiter retain exactly one actual owner");}
    static void freeFirstAndMinimum()throws Exception{
        Fixture f=nearby(1);own(f,1).put("hp",2000);own(f,1).put("maxHp",2000);own(f,1).put("x",2000.);
        f.enemy(501,1800,1800,"SURFACE");f.registry.updateGeneralGoal(f.first(),9000,100,null);
        long generation=f.registry.unit(1).ownerGeneration;Map<Long,Long> before=f.gate.snapshotGenerations(f.first().owner(),Collections.singletonList(1L));f.run();
        verify(f.response()!=null&&f.response().actors.equals(Arrays.asList(100L,1L)),"nearby FREE precedes one strongest compatible borrower rather than two weak borrowed actors");
        verify(f.commander.preferredThreats().isEmpty(),"small task never diverts whole General");
        verify(f.registry.general(f.first()).members.size()==5&&f.registry.general(f.first()).reservations.contains(1L),"borrowed actor preserves original slot outside marching centroid");
        verify(f.registry.general(f.first()).goalX==9000&&f.registry.unit(1).responseOriginOwner.equals(f.first().owner()),"General goal and explicit actor origin survive response");
        verify(f.registry.unit(1).ownerGeneration==generation+1&&"STALE_OWNER_GENERATION".equals(f.gate.validateGenerations(Collections.singletonList(1L),before)),"borrow invalidates already-collected General token");
        verify(((Number)f.task().get("localCovered")).doubleValue()==3&&((Number)f.task().get("deficit")).doubleValue()==0,"only current compatible actual leases close task demand");
        invariant(f);f.visible.clear();f.observe();f.force.reconcile(f.state,f.enemies,f.frame*1000);
        verify(f.registry.unit(1).owner.equals(f.first().owner())&&f.registry.unit(1).membership==Membership.ATTACHED,"clear restores original General owner and membership");
        verify(f.registry.unit(1).ownerGeneration==generation+2&&f.registry.general(f.first()).reservations.isEmpty()&&f.registry.general(f.first()).members.size()==6,"restoration closes original slot with a new ABA generation");
        verify(f.registry.general(f.first()).goalX==9000&&f.registry.unit(100).owner.equals(FREE_OWNER),"original General mission and FREE origin both restore");
        f.proposals.clear();f.force.collect(f.state,f.enemies,map(),100,100,f.frame*1000);
        for(ForceController.Proposal p:f.proposals)verify(!p.actors.contains(1L)||!"LOCAL_RESPONSE_CLEAR".equals(p.lane),"FREE cancellation cannot override restored General actor");
        f.commander.prepareThreatTasks(f.state,f.enemies,f.frame*1000);verify(f.commander.currentThreatTasks().isEmpty()&&Boolean.FALSE.equals(f.task().get("killClaim")),"visibility clear is not a kill");invariant(f);
    }
    static void joiningRestoresFreshWitness()throws Exception{
        Fixture f=nearby(2);verify(f.registry.requestJoin(100,f.first())&&f.registry.beginJoining(100)&&f.registry.joinAccepted(100,f.frame),"setup an actual JOINING lease and receipt");
        Map<Long,Long> old=f.gate.snapshotGenerations(f.first().joinOwner(),Collections.singletonList(100L));
        for(long id=1;id<=6;id++)own(f,id).put("x",100.);f.enemy(501,1000,1800,"SURFACE");f.run();
        verify(f.response()!=null&&f.response().actors.equals(Arrays.asList(101L,100L)),"FREE first then only the required nearby JOINING borrower");
        UnitView leased=f.registry.unit(100);verify(leased.membership==Membership.UNATTACHED&&leased.reservedGeneralId.equals(f.first())&&leased.responseOriginMembership==Membership.JOINING,"temporary JOINING preserves reservation and pauses arrival path");
        verify(!f.registry.observeJoinPosition(100,f.gate.stamp(),100,100,180),"response control cannot attach using old join receipt");
        f.proposals.clear();f.force.collect(f.state,f.enemies,map(),100,100,f.frame*1000);for(ForceController.Proposal p:f.proposals)verify(!"JOINING".equals(p.lane)||!p.actors.contains(100L),"borrowed JOINING has no simultaneous move owner");
        f.visible.clear();f.observe();f.force.reconcile(f.state,f.enemies,f.frame*1000);UnitView restored=f.registry.unit(100);
        verify(restored.owner.equals(f.first().joinOwner())&&restored.membership==Membership.JOINING&&restored.joinAcceptedFrame==-1,"clear restores JOINING owner and requires fresh actual receipt");
        verify("STALE_OWNER_GENERATION".equals(f.gate.validateGenerations(Collections.singletonList(100L),old)),"old JOINING token never survives borrow-return ABA");
        verify(!f.registry.observeJoinPosition(100,f.gate.stamp(),100,100,180),"old own position cannot attach without new receipt");
        verify(f.registry.joinAccepted(100,f.frame),"new actual receipt starts restored joining");verify(!f.registry.observeJoinPosition(100,f.gate.stamp(),100,100,180),"new receipt frame is not arrival");
        own(f,100).put("x",100.);f.observe();verify(f.registry.observeJoinPosition(100,f.gate.stamp(),100,100,180),"later current own arrival resumes original General");invariant(f);
    }
    static void unknownCompatibilityAndDeficit()throws Exception{
        Fixture f=nearby(0);f.enemy(501,50,1800,"SURFACE");f.run();verify(f.response()!=null&&f.response().actors.size()==1,"one compatible borrower suffices for tiny task");
        f.enemies.put("playerKey","team:1");f.visible.clear();f.observe();f.force.reconcile(f.state,f.enemies,f.frame*1000);verify(f.registry.unit(1).temporaryTask==TemporaryTask.LOCAL_RESPONSE,"foreign player does not clear origin");
        f.enemies.put("playerKey","team:0");f.enemy(501,50,1800,"SURFACE");f.visible.get(0).put("lastSeenGameTimeMs",0L);f.force.reconcile(f.state,f.enemies,f.frame*1000);verify(f.registry.unit(1).temporaryTask==TemporaryTask.LOCAL_RESPONSE,"stale row is UNKNOWN and cannot release");
        f.visible.clear();f.force.reconcile(f.state,f.enemies,f.frame*1000);verify(f.registry.unit(1).owner.equals(f.first().owner()),"fresh complete disappearance restores owner");
        Fixture blocked=nearby(0);blocked.blocked=true;blocked.enemy(501,50,100,"SURFACE");blocked.run();verify(blocked.response()==null&&blocked.commander.preferredThreats().isEmpty(),"failed compatibility cannot divert General or fall back to legacy detach");
        verify(((Number)blocked.task().get("deficit")).doubleValue()>0&&blocked.registry.general(blocked.first()).members.size()==6,"uncovered small demand stays explicit and original mission remains intact");
        Fixture air=nearby(0);air.enemy(501,50,1800,"AIR");air.run();verify(air.response()==null&&air.registry.general(air.first()).members.size()==6,"known incompatible ground roster cannot fabricate AIR coverage");
        Fixture big=nearby(0);big.enemy(501,5000,1800,"SURFACE");big.run();verify(big.response()==null&&big.commander.preferredThreats().size()==1,"large task keeps existing independent General attention path");
        Fixture residual=nearby(0);residual.enemy(501,4000,1800,"SURFACE");residual.run();verify(residual.response()!=null&&residual.response().actors.size()==3&&residual.registry.general(residual.first()).members.size()==3,"existing main-roster floor bounds small detachment");
        verify(residual.commander.preferredThreats().isEmpty()&&((Number)residual.task().get("deficit")).doubleValue()==3,"partial small detachment reports residual gap without dragging remainder");invariant(residual);
    }
    static void recoveryAndInvalidation()throws Exception{
        Fixture invalid=nearby(0);invalid.enemy(501,50,1800,"SURFACE");invalid.run();verify(invalid.registry.invalidateGeneral(invalid.first()),"original General can invalidate while borrower is reserved");
        invalid.visible.clear();invalid.observe();invalid.force.reconcile(invalid.state,invalid.enemies,invalid.frame*1000);verify(invalid.registry.unit(1).owner.equals(FREE_OWNER)&&invalid.registry.unit(1).reservedGeneralId==null&&invalid.registry.unit(1).responseOriginOwner==null,"clear never resurrects invalid original General");invariant(invalid);
        Fixture injured=nearby(0);injured.enemy(501,50,1800,"SURFACE");injured.run();verify(injured.registry.enterRecovery(1),"severe injury transfers borrower to actual recovery owner");
        injured.visible.clear();injured.observe();injured.force.reconcile(injured.state,injured.enemies,injured.frame*1000);verify(injured.registry.unit(1).owner.equals("force:recovery")&&injured.registry.unit(1).responseOriginOwner==null&&injured.registry.general(injured.first()).reservations.isEmpty(),"late task clear cannot reclaim recovered actor or leave original reservation");invariant(injured);
    }
    static void originalFrontierActuallyResumes()throws Exception{
        G53OutwardMissionHarness.Fixture f=new G53OutwardMissionHarness.Fixture();CommanderDirector commander=new CommanderDirector(f.registry);
        f.combat.setCommander(commander);commander.setCombatProvider(f.combat::view);commander.setLocalCoverageProvider(f.controller::localResponseCoverageView);
        f.plan(101,800,100);f.collect();f.accept();verify(Boolean.TRUE.equals(f.mission().get("active")),"setup accepted outward mission through existing GeneralCombatDirector");
        for(long id=30;id<=33;id++)f.find(id).put("hp",200.);
        f.enemy(501,650,50);f.sample(1000);for(UnitView u:f.registry.units())f.registry.updateHealthRole(u.unitId,u.healthRole);
        commander.prepareThreatTasks(f.state,f.enemies,f.now);f.controller.prepareLocalResponse(f.state,f.enemies,f.now,commander.currentThreatTasks());
        f.centroids();f.combat.refreshCurrentRoster(f.state,f.enemies,100,100,f.now);commander.reconcile(f.state,f.enemies,100,100,f.now,24);f.combat.setPreferredThreats(commander.preferredThreats());
        f.proposals.clear();f.collect();ForceController.Proposal response=null;for(ForceController.Proposal proposal:f.proposals)if("LOCAL_RESPONSE".equals(proposal.lane))response=proposal;
        verify(response!=null&&response.actors.equals(Collections.singletonList(15L)),"existing force path borrows one nearest compatible actual General actor");
        response.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        verify(f.first().stream().anyMatch(p->p.path.contains("x=800.0")&&!p.actors.contains(15L)),"remaining General actually continues original accepted frontier while detached actor responds");
        f.enemyRows.clear();f.sample(1250);f.controller.reconcile(f.state,f.enemies,f.now);for(UnitView u:f.registry.units())f.registry.updateHealthRole(u.unitId,u.healthRole);f.centroids();
        commander.prepareThreatTasks(f.state,f.enemies,f.now);commander.reconcile(f.state,f.enemies,100,100,f.now,24);f.proposals.clear();f.collect();
        verify(f.first().stream().anyMatch(p->p.actors.contains(15L)&&p.path.contains("x=800.0")),"clear immediately emits original frontier continuation for restored actor despite old command cooldown");
        verify(((Number)f.mission().get("targetTile")).intValue()==101,"temporary threat did not reset or substitute General frontier");
    }
    static void strategicRolesStayGeneral()throws Exception{
        Fixture base=nearby(8);base.enemy(501,1000,1800,"SURFACE");base.visible.get(0).put("type","commandCenter");base.visible.get(0).put("building",true);base.run();
        verify(base.response()==null&&base.commander.allowsTarget(base.first(),501)&&base.registry.general(base.first()).members.size()==6,"current armed commandCenter receives full General authorization even with enough FREE and small HP budget");
        verify(Boolean.FALSE.equals(base.task().get("smallResponseRoleKnown")),"base classification does not infer harassment from small demand");
        Fixture staticDefense=nearby(8);staticDefense.enemy(501,1000,1800,"SURFACE");staticDefense.visible.get(0).put("type","modTurret");staticDefense.visible.get(0).put("building",true);staticDefense.run();
        verify(staticDefense.response()==null&&!staticDefense.commander.preferredThreats().isEmpty(),"legally observed armed static defense remains a General task without relying on a type whitelist");
        Fixture uncertain=nearby(0);uncertain.enemy(501,50,1800,"SURFACE");uncertain.visible.get(0).put("type","unknownRole");uncertain.visible.get(0).remove("building");uncertain.run();
        verify(uncertain.response()==null&&!uncertain.commander.preferredThreats().isEmpty()&&Boolean.FALSE.equals(uncertain.task().get("smallResponseRoleKnown")),"unknown static/mobile role cannot masquerade as small harassment");
        Fixture builder=nearby(0);builder.enemy(501,50,1800,"SURFACE");builder.visible.get(0).put("type","builder");builder.visible.get(0).put("canAttack",false);builder.run();
        verify(builder.response()!=null&&builder.response().actors.size()==1&&builder.commander.preferredThreats().isEmpty(),"current legally non-building unarmed Builder remains a minimal mobile response task");
        Fixture blocked=nearby(0);blocked.blocked=true;blocked.enemy(501,50,1800,"SURFACE");blocked.visible.get(0).put("type","builder");blocked.visible.get(0).put("canAttack",false);blocked.run();
        verify(blocked.response()==null&&blocked.commander.preferredThreats().isEmpty()&&((Number)blocked.task().get("deficit")).doubleValue()>0,"Builder task without compatible responders retains explicit deficit rather than dragging whole General");
        Fixture transition=nearby(0);transition.enemy(501,50,1800,"SURFACE");transition.run();long actor=transition.response().actors.get(0);
        transition.visible.get(0).put("type","commandCenter");transition.visible.get(0).put("building",true);transition.observe();transition.run();
        verify(transition.response()==null&&transition.registry.unit(actor).owner.equals(transition.first().owner())&&transition.commander.allowsTarget(transition.first(),501),"new strategic role ends existing small lease, restores origin, and grants full General current task");
    }
    static void remoteBaseActuallyAdvances()throws Exception{
        G5CombatHarness.Fixture f=R8BaseAdmissionHarness.fixture();for(long id=10;id<16;id++)f.find(id).put("x",600.);f.centroids();
        CommanderDirector commander=new CommanderDirector(f.registry);f.combat.setCommander(commander);commander.setCombatProvider(f.combat::view);commander.setLocalCoverageProvider(f.controller::localResponseCoverageView);
        G51TacticalHarness.enemy(f,148,"commandCenter",1000,100,1000,true,true,"SURFACE");for(UnitView u:f.registry.units())f.registry.updateHealthRole(u.unitId,u.healthRole);
        f.combat.observe(f.state,f.enemies,100,100,f.now);commander.prepareThreatTasks(f.state,f.enemies,f.now);f.controller.prepareLocalResponse(f.state,f.enemies,f.now,commander.currentThreatTasks());
        f.centroids();f.combat.refreshCurrentRoster(f.state,f.enemies,100,100,f.now);commander.reconcile(f.state,f.enemies,100,100,f.now,24);f.combat.setPreferredThreats(commander.preferredThreats());f.proposals.clear();f.collect();
        verify(commander.allowsTarget(f.a,148),"remote visible base beyond self-defense radius receives current whole-General authorization");
        verify(f.owned(f.a.owner()).stream().anyMatch(p->p.path.startsWith("/command/attack-move?")&&p.path.contains("x=1000.0")),"existing lawful/native-compatible siege emits real General advance proposal instead of unassigned remote target hold");
    }
    static List<ThreatTask> endpointTasks(Fixture f,long offset){long source=f.frame*1000+offset;
        f.enemies.put("gameTimeMs",source);f.enemies.put("frame",f.frame+1);for(Map<String,Object> row:f.visible)row.put("lastSeenGameTimeMs",source);
        f.commander.prepareThreatTasks(f.state,f.enemies,f.frame*1000);return f.commander.currentThreatTasks();}
    static void independentEndpointFreshness()throws Exception{
        Fixture f=nearby(1);f.enemy(501,1000,1800,"SURFACE");long initial=f.registry.unit(1).ownerGeneration;
        Map<Long,Long> prior=f.gate.snapshotGenerations(f.first().owner(),Collections.singletonList(1L));
        List<ThreatTask> tasks=endpointTasks(f,48);
        verify(tasks.size()==1&&tasks.get(0).sourceGameTimeMs==f.frame*1000+48&&tasks.get(0).sourceFrame==f.frame+1,"real Commander task export retains validated later combat endpoint source instead of requiring atomic own clock/frame");
        f.force.prepareLocalResponse(f.state,f.enemies,f.frame*1000,tasks);f.proposals.clear();f.force.collect(f.state,f.enemies,map(),100,100,f.frame*1000);
        verify(f.response()!=null&&f.response().actors.equals(Arrays.asList(100L,1L)),"prepareThreatTasks to currentThreatTasks to Force preparation borrows actual minimum remainder when combat is48ms later");
        verify(f.registry.unit(1).responseOriginOwner.equals(f.first().owner())&&"STALE_OWNER_GENERATION".equals(f.gate.validateGenerations(Collections.singletonList(1L),prior)),"later combat borrow preserves origin and invalidates prior General generation");
        f.enemies.put("sessionId","foreign");f.visible.clear();f.commander.prepareThreatTasks(f.state,f.enemies,f.frame*1000);f.force.prepareLocalResponse(f.state,f.enemies,f.frame*1000,f.commander.currentThreatTasks());
        verify(f.commander.currentThreatTasks().isEmpty()&&f.registry.unit(1).temporaryTask==TemporaryTask.LOCAL_RESPONSE,"foreign sample yields UNKNOWN task and never clears a real borrowed origin");
        f.enemies.put("sessionId","s");endpointTasks(f,80);f.force.prepareLocalResponse(f.state,f.enemies,f.frame*1000,f.commander.currentThreatTasks());
        verify(f.registry.unit(1).owner.equals(f.first().owner())&&f.registry.unit(1).ownerGeneration==initial+2,"later legal combat disappearance restores original owner with new ABA generation");
        Fixture changed=nearby(0);changed.enemy(501,50,1800,"SURFACE");changed.force.prepareLocalResponse(changed.state,changed.enemies,changed.frame*1000,endpointTasks(changed,40));
        verify(changed.registry.unit(1).temporaryTask==TemporaryTask.LOCAL_RESPONSE,"later source initially borrows mobile responder");
        changed.visible.get(0).put("type","commandCenter");changed.visible.get(0).put("building",true);changed.force.prepareLocalResponse(changed.state,changed.enemies,changed.frame*1000,endpointTasks(changed,70));
        verify(changed.registry.unit(1).owner.equals(changed.first().owner()),"existing response task refresh also matches later combat source and returns origin when role becomes strategic");
        Fixture stale=nearby(0);stale.enemy(501,50,1800,"SURFACE");List<ThreatTask> invalid=endpointTasks(stale,-1);stale.force.prepareLocalResponse(stale.state,stale.enemies,stale.frame*1000,invalid);
        verify(invalid.isEmpty()&&stale.registry.unit(1).temporaryTask==TemporaryTask.NONE,"stale combat source cannot create task or borrow through real pipeline");
        Fixture foreign=nearby(0);foreign.enemy(501,50,1800,"SURFACE");foreign.enemies.put("sessionId","foreign");invalid=endpointTasks(foreign,48);foreign.force.prepareLocalResponse(foreign.state,foreign.enemies,foreign.frame*1000,invalid);
        verify(invalid.isEmpty()&&foreign.registry.unit(1).temporaryTask==TemporaryTask.NONE,"later foreign session cannot gain task freshness or borrow");
        Fixture player=nearby(0);player.enemy(501,50,1800,"SURFACE");player.enemies.put("playerKey","team:1");invalid=endpointTasks(player,48);player.force.prepareLocalResponse(player.state,player.enemies,player.frame*1000,invalid);
        verify(invalid.isEmpty()&&player.registry.unit(1).temporaryTask==TemporaryTask.NONE,"later foreign player cannot create task or borrow");
        Fixture malformed=nearby(0);malformed.enemy(501,50,1800,"SURFACE");endpointTasks(malformed,48);malformed.visible.get(0).put("lastSeenGameTimeMs",malformed.frame*1000);malformed.commander.prepareThreatTasks(malformed.state,malformed.enemies,malformed.frame*1000);
        malformed.force.prepareLocalResponse(malformed.state,malformed.enemies,malformed.frame*1000,malformed.commander.currentThreatTasks());
        verify(malformed.commander.currentThreatTasks().isEmpty()&&malformed.registry.unit(1).temporaryTask==TemporaryTask.NONE,"malformed current row transitions to UNKNOWN and cannot trigger response");
        Fixture mismatch=nearby(0);mismatch.enemy(501,50,1800,"SURFACE");List<ThreatTask> old=endpointTasks(mismatch,40);endpointTasks(mismatch,80);
        mismatch.force.prepareLocalResponse(mismatch.state,mismatch.enemies,mismatch.frame*1000,old);
        verify(mismatch.registry.unit(1).temporaryTask==TemporaryTask.NONE,"task from previous combat endpoint cannot borrow against a different current source");
        List<ThreatTask> current=mismatch.commander.currentThreatTasks();mismatch.force.prepareLocalResponse(mismatch.state,mismatch.enemies,mismatch.frame*1000,Collections.singletonList(current.get(0).withStatus(ThreatTask.Status.UNKNOWN,"FIXTURE_UNKNOWN")));
        verify(mismatch.registry.unit(1).temporaryTask==TemporaryTask.NONE,"UNKNOWN task cannot borrow even when combat source time matches");
        mismatch.observe();verify(mismatch.commander.currentThreatTasks().isEmpty(),"new own observation requires fresh task preparation instead of exporting prior observation authority");
    }
    public static void main(String[] args)throws Exception{freeFirstAndMinimum();joiningRestoresFreshWitness();unknownCompatibilityAndDeficit();recoveryAndInvalidation();originalFrontierActuallyResumes();strategicRolesStayGeneral();remoteBaseActuallyAdvances();independentEndpointFreshness();System.out.println("R10TemporaryResponseHarness PASS checks="+count);}
}
