package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.GeneralRegistry.*;

/** Synthetic current observations and compatibility witnesses; no native, repair or win claim. */
public final class G53FreeResponseHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<String,Object> map(Object... pairs){Map<String,Object> out=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    static final class Fixture implements ForceController.Host {
        final CommandArbiter gate=new CommandArbiter();final GeneralRegistry registry=new GeneralRegistry(gate);
        final CommanderDirector commander=new CommanderDirector(registry);final ForceController force=new ForceController(registry,this);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),visible=new ArrayList<Map<String,Object>>();
        final List<ForceController.Proposal> proposals=new ArrayList<ForceController.Proposal>();
        final Map<String,Object> state=map("sessionId","s","playerKey","team:0","ownUnits",own),enemies=map("sessionId","s","playerKey","team:0","visibleEnemies",visible);
        boolean blocked,foreignResult;long frame;Map<String,Object> combat=map();
        Fixture(int free){List<Long> group=new ArrayList<Long>(),ordinary=new ArrayList<Long>();
            for(long id=1;id<=6;id++){group.add(id);ordinary.add(id);own.add(actor(id,100));}
            for(long id=100;id<100+free;id++){ordinary.add(id);own.add(actor(id,1800));}
            own.add(map("id",999L,"type","commandCenter","building",true,"x",100,"y",100,"hp",4000,"maxHp",4000,"buildProgress",1));
            observe();registry.bootstrap(Collections.singletonList(group),ordinary);refresh();
            commander.setLocalCoverageProvider(force::localResponseCoverageView);
        }
        Map<String,Object> actor(long id,double x){return map("id",id,"type","tank","x",x,"y",100,"hp",1000,"maxHp",1000,"buildProgress",1);}
        void observe(){frame++;state.put("frame",frame);state.put("gameTimeMs",frame*1000);enemies.put("frame",frame);enemies.put("gameTimeMs",frame*1000);
            for(Map<String,Object> e:visible)e.put("lastSeenGameTimeMs",frame*1000);List<Long> ids=new ArrayList<Long>();for(Map<String,Object> u:own)ids.add(((Number)u.get("id")).longValue());
            gate.observe(new CommandArbiter.Stamp("s","team:0",frame,frame*1000),ids);refresh();
            combat.put("ownCurrent",true);combat.put("sourceSessionId","s");combat.put("sourceFrame",frame);combat.put("gameTimeMs",frame*1000);
        }
        void refresh(){for(UnitView u:registry.units())registry.updateHealthRole(u.unitId,u.healthRole);for(GeneralView g:registry.generals())if(!g.members.isEmpty())registry.updateGeneralCentroid(g.id,100,100);}
        void enemy(long id,double hp,double x,String domain){visible.add(map("id",id,"type","tank","x",x,"y",100,"hp",hp,"canAttack",true,"targetDomain",domain,"lastSeenGameTimeMs",frame*1000));}
        void run()throws Exception{commander.prepareThreatTasks(state,enemies,frame*1000);force.prepareLocalResponse(state,enemies,frame*1000,commander.currentThreatTasks());refresh();
            commander.reconcile(state,enemies,100,100,frame*1000,24);proposals.clear();force.collect(state,enemies,map(),100,100,frame*1000);}
        Map<String,Object> task(){@SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)commander.snapshot().get("tasks");return rows.get(rows.size()-1);}
        GeneralId first(){return registry.generals().get(0).id;}
        public Map<String,Object> read(String path,String event){return map("status","UNKNOWN");}
        public List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target,Map<String,Object> all){
            if(blocked)return Collections.emptyList();if(foreignResult)return Collections.singletonList(actor(9999,1800));
            List<Map<String,Object>> out=new ArrayList<Map<String,Object>>();for(Map<String,Object> actor:actors)if(!"AIR".equals(target.get("targetDomain"))||"scout".equals(actor.get("type")))out.add(actor);return out;}
        public void collect(ForceController.Proposal p){proposals.add(p);}
        ForceController.Proposal response(){for(ForceController.Proposal p:proposals)if("LOCAL_RESPONSE".equals(p.lane))return p;return null;}
    }
    static void cheapAndPartial()throws Exception{
        Fixture full=new Fixture(3);full.enemy(501,1000,1800,"SURFACE");full.observe();full.run();
        check(full.response()!=null&&full.response().actors.equals(Arrays.asList(100L,101L)),"distant tiny task rents only two necessary FREE actors");
        check(full.commander.preferredThreats().isEmpty()&&full.registry.general(full.first()).members.size()==6,"native-compatible cheap coverage preserves complete General mission");
        check(full.registry.unit(100).reservedGeneralId==null&&full.registry.unit(101).reservedGeneralId==null,"new LR capacity is not immediately swallowed by join reservations");
        check(full.registry.unit(102).temporaryTask==TemporaryTask.NONE,"surplus FREE is not rented to satisfy a fixed reserve target");
        check(((Number)full.task().get("localCovered")).doubleValue()==2&&((Number)full.task().get("deficit")).doubleValue()==0,"only real current leases enter transparent task budget");
        Fixture partial=new Fixture(1);partial.enemy(501,1000,1800,"SURFACE");partial.observe();partial.run();
        check(partial.response()!=null&&partial.response().actors.equals(Collections.singletonList(100L)),"insufficient FREE still establishes real partial response");
        check(partial.commander.preferredThreats().size()==1&&((Number)partial.task().get("localCovered")).doubleValue()==1,"General covers the remaining visible gap after cheap capacity");
        Fixture tiny=new Fixture(1);tiny.enemy(501,50,1800,"SURFACE");tiny.observe();tiny.run();
        check(tiny.response()!=null&&tiny.commander.preferredThreats().isEmpty(),"one FREE actor answers tiny task without a minimum team size");
    }
    static void lawfulAndCurrent()throws Exception{
        Fixture mixed=new Fixture(2);mixed.enemy(501,50,1800,"SURFACE");mixed.enemy(502,50,1820,"AIR");mixed.observe();mixed.run();
        check(mixed.response()==null&&mixed.registry.unit(100).temporaryTask==TemporaryTask.NONE,"ground decoy cannot bypass all-target native compatibility intersection");
        Fixture blocked=new Fixture(2);blocked.blocked=true;blocked.enemy(501,50,1800,"SURFACE");blocked.observe();blocked.run();check(blocked.response()==null,"native approach failure creates no cheap capacity");
        Fixture foreign=new Fixture(2);foreign.foreignResult=true;foreign.enemy(501,50,1800,"SURFACE");foreign.observe();foreign.run();check(foreign.response()==null,"foreign actors returned by a host never enter leases");
        Fixture stale=new Fixture(2);stale.enemy(501,50,1800,"SURFACE");stale.observe();stale.enemies.put("sessionId","foreign");stale.run();
        check(stale.response()==null&&stale.commander.currentThreatTasks().isEmpty(),"foreign enemy packet remains UNKNOWN rather than a current cheap task");
    }
    static void recoveredAndJoinBoundaries()throws Exception{
        Fixture f=new Fixture(1);long before=f.registry.unit(100).ownerGeneration;check(f.registry.enterRecovery(100),"severe own actor leaves ordinary pool under real recovery owner");
        f.observe();check(f.registry.finishRecovery(100)&&f.registry.unit(100).allocation==Allocation.FREE,"current healed own actor returns FREE before allocation");
        f.enemy(501,50,1800,"SURFACE");f.run();check(f.response()!=null&&f.registry.unit(100).ownerGeneration==before+3,"recovered FREE gets cheap-task opportunity before joining, each owner transfer increments generation");
        f.enemies.put("sessionId","foreign");f.visible.clear();f.observe();f.force.reconcile(f.state,f.enemies,f.frame*1000);
        check(f.registry.unit(100).temporaryTask==TemporaryTask.LOCAL_RESPONSE,"UNKNOWN cannot clear the current lease");
        f.enemies.put("sessionId","s");f.force.reconcile(f.state,f.enemies,f.frame*1000);check(f.registry.unit(100).temporaryTask==TemporaryTask.NONE,"fresh legal visibility clear immediately releases LR without kill claim");
        f.proposals.clear();f.force.collect(f.state,f.enemies,map(),100,100,f.frame*1000);ForceController.Proposal clear=null;
        for(ForceController.Proposal p:f.proposals)if("LOCAL_RESPONSE_CLEAR".equals(p.lane))clear=p;
        check(clear!=null&&clear.path.startsWith("/command/move?")&&clear.path.contains("x=1800.0"),"clear cancels accepted attack at current own position without ghost pursuit");
        clear.accepted(map("status","queued"));f.proposals.clear();f.force.collect(f.state,f.enemies,map(),100,100,f.frame*1000);
        boolean duplicate=false;for(ForceController.Proposal p:f.proposals)if("LOCAL_RESPONSE_CLEAR".equals(p.lane))duplicate=true;check(!duplicate,"actual accepted clear receipt prevents redundant cancellation");
        check(!f.registry.requestJoin(100,f.first()),"same-frame clear preserves FREE breakpoint");f.observe();
        check(f.registry.beginLocalResponse(100,"local-response:pending")&&f.registry.requestJoin(100,f.first()),"public explicit pending plus LR compatibility remains intact");
        check(!f.registry.beginJoining(100)&&f.registry.clearLocalResponse(100)&&f.registry.unit(100).membership==Membership.JOINING,"cleared existing pending lease continues joining");
        long generation=f.registry.unit(100).ownerGeneration;Map<Long,Long> receiptGeneration=f.gate.snapshotGenerations(f.first().joinOwner(),Collections.singletonList(100L));
        check(f.registry.joinAccepted(100,f.frame),"actual receipt creates a joining commitment");check(!f.registry.observeJoinPosition(100,f.gate.stamp(),100,100,180),"same receipt frame never substitutes for actual arrival");
        f.observe();check(f.registry.observeJoinPosition(100,f.gate.stamp(),100,100,180)&&f.registry.unit(100).ownerGeneration==generation+1,"later own position alone attaches under new General owner");
        check("STALE_OWNER_GENERATION".equals(f.gate.validateGenerations(Collections.singletonList(100L),receiptGeneration)),"old LR/join receipts cannot survive owner-generation ABA");
    }
    static void modestGrowthAndOverflow()throws Exception{
        Fixture f=new Fixture(12);f.observe();f.combat.put("visiblePressure",10000);f.commander.setCombatProvider(id->f.combat);
        Map<Long,Long> before=f.gate.snapshotGenerations(FREE_OWNER,f.registry.freeCandidateIds());f.commander.reconcile(f.state,f.enemies,100,100,f.frame*1000,24);
        GeneralView first=f.registry.general(f.first());check(first.desiredStrength==12&&first.members.size()==6&&first.reservations.size()==6,"fresh unmet local need grows healthy full existing General by supplied small batch");
        check(f.registry.generals().size()==2&&f.registry.generals().get(1).phase==Phase.FORMING&&f.registry.generals().get(1).desiredStrength==24,"overflow still births a full intended formation instead of six-unit fragmentation");
        check(f.gate.validateGenerations(new ArrayList<Long>(before.keySet()),before)==null,"organization growth and pending reservations never fabricate arrival or transfer ownership");
        Fixture idle=new Fixture(6);idle.observe();idle.commander.setCombatProvider(id->idle.combat);idle.run();
        check(idle.registry.general(idle.first()).desiredStrength==6&&idle.registry.generals().size()==2,"no current unmet mission need skips growth and permits ordinary overflow birth");
        Fixture stale=new Fixture(6);stale.observe();stale.combat.put("visiblePressure",10000);stale.combat.put("sourceFrame",0);stale.commander.setCombatProvider(id->stale.combat);stale.run();
        check(stale.registry.general(stale.first()).desiredStrength==6,"stale combat demand cannot enlarge a General");
    }
    static ForceControllerHarness.Fixture acceptedJoin()throws Exception{
        ForceControllerHarness.Fixture f=new ForceControllerHarness.Fixture();f.observe(1000);check(f.registry.requestJoin(33,f.a)&&f.registry.beginJoining(33),"ordinary recruit obtains current join owner");f.collect();
        ForceController.Proposal proposal=f.lane("JOINING");check(proposal!=null,"initial real joining command is proposed");proposal.accepted(map("status","queued","frame",f.frame,"gameTimeMs",f.now));
        Map<String,Object> own=f.find(33);own.put("orderType","move");own.put("orderX",f.registry.general(f.a).x);own.put("orderY",f.registry.general(f.a).y);return f;
    }
    static void explicitJoinOrderSurvivesChurn()throws Exception{
        ForceControllerHarness.Fixture f=acceptedJoin();Map<String,Object> own=f.find(33);long generation=f.registry.unit(33).ownerGeneration;
        own.put("x",3000.);f.enemy(6000,4000,50);f.observe(23000);f.collect();check(f.lane("JOINING")==null,"known rendezvous survives new unrelated contact and more than20s without own progress");
        for(Long actor:f.registry.general(f.a).members)f.find(actor).put("x",((Number)f.find(actor).get("x")).doubleValue()+90);
        f.enemyRows.get(0).put("x",5000.);f.observe(60000);f.collect();check(f.lane("JOINING")==null,"exact native accepted destination remains useful at current90unit arrival envelope despite global threat change");
        check(f.registry.unit(33).membership==Membership.JOINING&&f.registry.unit(33).ownerGeneration==generation,"reuse leaves actual membership and owner generation unchanged");
        f.registry.observeJoinPosition(33,f.gate.stamp(),numberAt(own,"x"),numberAt(own,"y"),180);check(f.registry.unit(33).membership==Membership.JOINING,"native move reuse never substitutes for later own arrival");
        for(Long actor:f.registry.general(f.a).members)f.find(actor).put("x",((Number)f.find(actor).get("x")).doubleValue()+1);
        f.observe(60250);f.collect();check(f.lane("JOINING")!=null,"current rendezvous outside useful envelope redirects immediately");
        ForceControllerHarness.Fixture wrong=acceptedJoin();wrong.find(33).put("orderX",9000.);wrong.observe(1250);wrong.collect();check(wrong.lane("JOINING")!=null,"explicit overwritten destination bypasses old cooldown");
        ForceControllerHarness.Fixture attack=acceptedJoin();attack.find(33).put("orderType","attackMove");attack.observe(1250);attack.collect();check(attack.lane("JOINING")!=null,"wrong native order kind must restore join move immediately");
        ForceControllerHarness.Fixture aba=acceptedJoin();check(aba.registry.cancelJoin(33),"old join can release");aba.observe(1250);check(aba.registry.requestJoin(33,aba.a)&&aba.registry.beginJoining(33),"same actor can receive a new join generation");aba.collect();check(aba.lane("JOINING")!=null,"old explicit destination cannot suppress new owner generation");
        ForceControllerHarness.Fixture unknown=acceptedJoin();unknown.find(33).remove("orderX");unknown.find(33).remove("orderY");unknown.observe(23000);unknown.collect();check(unknown.lane("JOINING")!=null,"unknown destination cannot inherit unlimited native reuse");
    }
    static double numberAt(Map<String,Object> map,String key){return ((Number)map.get(key)).doubleValue();}
    public static void main(String[] args)throws Exception{cheapAndPartial();lawfulAndCurrent();recoveredAndJoinBoundaries();modestGrowthAndOverflow();explicitJoinOrderSurvivesChurn();System.out.println("G53FreeResponseHarness PASS checks="+checks);}
}
