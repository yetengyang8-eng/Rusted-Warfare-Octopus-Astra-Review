package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.GeneralRegistry.*;

/** Synthetic own observations. No native elapsed march, death or victory claim. */
public final class G5CommanderHarness {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Map<String,Object> map(Object... values){Map<String,Object> result=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)result.put((String)values[i],values[i+1]);return result;}
    private static Map<String,Object> actor(long id,double x,double y){return map("id",id,"type","tank","hp",1000,"maxHp",1000,"buildProgress",1,"x",x,"y",y,"dead",false);}
    private static final class Fixture {
        final CommandArbiter gate=new CommandArbiter();final GeneralRegistry registry=new GeneralRegistry(gate);final CommanderDirector commander=new CommanderDirector(registry);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),visible=new ArrayList<Map<String,Object>>();
        final Map<String,Object> state=map("sessionId","s","playerKey","team:0","player",map("teamId",0),"ownUnits",own);
        final Map<String,Object> enemies=map("sessionId","s","playerKey","team:0","visibleEnemies",visible);
        final Map<GeneralId,Map<String,Object>> combat=new HashMap<GeneralId,Map<String,Object>>();long frame;
        Fixture(int seed,int free){this(seed,free,false);}
        Fixture(int seed,int free,boolean split){List<Long> first=new ArrayList<Long>(),second=new ArrayList<Long>(),ordinary=new ArrayList<Long>();own.add(map("id",999L,"type","commandCenter","building",true,"x",100,"y",100,"hp",3000,"maxHp",3000,"buildProgress",1));
            for(long id=1;id<=seed+free;id++){own.add(actor(id,id<=seed?800:100,100));ordinary.add(id);if(id<=seed){if(split&&id>seed/2)second.add(id);else first.add(id);}}
            observe();registry.bootstrap(seed>0?split?Arrays.<Collection<Long>>asList(first,second):Arrays.<Collection<Long>>asList(first):Collections.<Collection<Long>>emptyList(),ordinary,24,100,100);refresh();commander.setCombatProvider(id->combat.get(id));}
        void observe(){frame++;state.put("frame",frame);state.put("gameTimeMs",frame*1000);enemies.put("gameTimeMs",frame*1000);enemies.put("frame",frame);
            for(Map<String,Object> enemy:visible)enemy.put("lastSeenGameTimeMs",frame*1000);
            List<Long> ids=new ArrayList<Long>();for(Map<String,Object> actor:own)ids.add(((Number)actor.get("id")).longValue());gate.observe(new CommandArbiter.Stamp("s","team:0",frame,frame*1000),ids);refresh();}
        void refresh(){for(UnitView u:registry.units())registry.updateHealthRole(u.unitId,u.healthRole);for(GeneralView g:registry.generals())if(!g.members.isEmpty())registry.updateGeneralCentroid(g.id,g.id.value==1?800:1200,100);}
        void run(){for(Map<String,Object> view:combat.values()){view.put("ownCurrent",true);view.put("sourceSessionId","s");view.put("sourceFrame",frame);view.put("gameTimeMs",frame*1000);}
            commander.reconcile(state,enemies,100,100,frame*1000,24);}
        GeneralId a(){return registry.generals().get(0).id;}
        Map<String,Object> unit(long id){for(Map<String,Object> row:own)if(((Number)row.get("id")).longValue()==id)return row;return null;}
        void enemy(long id,double x){visible.add(map("id",id,"x",x,"y",100,"hp",1000,"building",false,"lastSeenGameTimeMs",frame*1000,"canAttack",true));}
        void invariant(){Set<Long> seen=new HashSet<Long>();for(GeneralView g:registry.generals())for(Long id:g.reservations){UnitView u=registry.unit(id);
            check(seen.add(id)&&u!=null&&g.id.equals(u.reservedGeneralId)&&u.allocation==Allocation.PENDING_JOIN,"reservation is counted once with a live independent General");}
            for(UnitView u:registry.units()){check(gate.owns(u.owner,u.unitId),"actual arbiter owner matches registry");if(u.reservedGeneralId!=null)check(registry.general(u.reservedGeneralId)!=null,"no dangling reservation");}}
    }
    private static void overflowAndBarrier(){Fixture f=new Fixture(24,38);long generation=f.gate.ownerGeneration(25);f.run();
        check(f.registry.generals().size()==2,"24 members plus 38 FREE creates an additional independent formation");GeneralView b=f.registry.generals().get(1);
        check(b.phase==Phase.FORMING&&b.desiredStrength==24&&b.members.isEmpty()&&b.reservations.isEmpty(),"birth never assigns a one-man roster or bypasses admission frame");
        check(f.gate.ownerGeneration(25)==generation&&f.registry.unit(25).owner.equals(FREE_OWNER),"birth does not mutate free owner generation");
        f.run();check(f.registry.generals().size()==2,"same observation cannot force multiple empty births");f.observe();f.run();
        b=f.registry.general(b.id);check(b.reservations.size()==24&&b.members.isEmpty(),"later frame fills desired24 through pending not attached");
        check(f.registry.generals().size()==3&&f.registry.generals().get(2).desiredStrength==24,"remaining14 can form the next explicit full-target lifecycle without a count cap");
        check(f.registry.generals().get(2).reservations.size()==14,"next independent formation reserves surplus only once");f.run();check(f.registry.generals().size()==3,"pending capacity prevents forced fragmentation");f.invariant();
        Fixture partial=new Fixture(23,8);partial.registry.beginLocalResponse(1,"local-response:capacity");partial.run();check(partial.registry.generals().size()==1,"unsatisfied existing formation blocks overflow");
        Fixture five=new Fixture(24,5);five.observe();five.run();check(five.registry.generals().size()==1,"five free cannot force another formation");
        for(long id=25;id<=29;id++)five.unit(id).put("hp",300);five.own.add(actor(30,100,100));five.observe();five.registry.admitFree(30,HealthRole.NORMAL);five.run();check(five.registry.generals().size()==1,"six NORMAL but only one actually healthy cannot birth");
    }
    private static void retreatPendingAndABA(){Fixture q=new Fixture(6,4);GeneralId a=q.a();q.observe();q.registry.beginLocalResponse(2,"local-response:fixture");q.registry.clearLocalResponse(2);q.registry.updateHealthRole(2,HealthRole.RECOVERING);q.observe();
        q.registry.beginLocalResponse(7,"local-response:free");q.combat.put(a,map("crisis","OVERMATCHED","retreating",true,"rallyKnown",true,"rallyX",220,"rallyY",100,"currentHealthy",5,"visiblePressure",5));
        q.run();UnitView pending=q.registry.unit(7);check(pending.allocation==Allocation.PENDING_JOIN&&pending.temporaryTask==TemporaryTask.LOCAL_RESPONSE&&pending.reservedGeneralId.equals(a),"FREE-origin pending and LocalResponse coexist for retreat refill");
        check(q.registry.general(a).joinTargetX==220&&q.registry.general(a).joinTargetSource.contains("DYNAMIC_UNKNOWN"),"retreat replenishment targets explicit home-side rally without safe claim");
        long old=q.gate.ownerGeneration(7);q.registry.clearLocalResponse(7);check(q.registry.unit(7).membership==Membership.JOINING&&q.gate.ownerGeneration(7)==old+1,"response clear genuinely transfers into independent joining owner");
        Map<Long,Long> token=q.gate.snapshotGenerations(a.joinOwner(),Arrays.asList(7L));q.registry.joinAccepted(7,q.frame);
        check(!q.registry.observeJoinPosition(7,q.gate.stamp(),220,100,180),"receipt frame cannot prove arrival");q.observe();
        check(!q.registry.observeJoinPosition(7,q.gate.stamp(),800,100,180),"stale rally never falls back to pressured centroid");q.run();
        check(!q.registry.observeJoinPosition(7,new CommandArbiter.Stamp("foreign","team:0",q.frame,q.frame*1000),220,100,180),"foreign session cannot attach to rally");
        check(q.registry.observeJoinPosition(7,q.gate.stamp(),220,100,180),"fresh later own position reaches current rally shared with force target");
        check("STALE_OWNER_GENERATION".equals(q.gate.validateGenerations(Arrays.asList(7L),token)),"join arrival invalidates collected join generation");
        q.registry.beginLocalResponse(7,"local-response:again");q.registry.clearLocalResponse(7);q.observe();q.run();check(q.registry.unit(7).reservedGeneralId!=null,"later FREE may reserve after response cycle");
        check("STALE_OWNER_GENERATION".equals(q.gate.validateGenerations(Arrays.asList(7L),token)),"reserve and return ABA cannot revive a prior owner token");
        q.combat.get(a).put("sourceSessionId","foreign");q.commander.reconcile(q.state,q.enemies,100,100,q.frame*1000,24);
        check(!q.registry.general(a).joinTargetKnown,"foreign combat view cannot restamp old geometry into current rally");
        q.combat.put(a,map("retreating",true,"rallyKnown",false));q.run();check(!q.registry.general(a).joinTargetKnown,"unknown retreat rally blocks fabricated target");q.invariant();
    }
    private static void sharedThreatAndUnknown(){Fixture f=new Fixture(12,0,true);f.observe();f.run();GeneralId a=f.a(),b=f.registry.generals().get(1).id;
        f.enemy(501,900);f.enemy(502,950);for(Map<String,Object> enemy:f.visible)enemy.put("hp",5000);f.run();ThreatTask first=f.commander.taskForGeneral(a),second=f.commander.taskForGeneral(b);
        check(first!=null&&second!=null&&first.id==second.id&&first.enemyIds.size()==2,"multiple independent Generals may attend one current visible cluster");
        check(!f.registry.general(a).owner.equals(f.registry.general(b).owner)&&Collections.disjoint(f.registry.general(a).members,f.registry.general(b).members),"shared task does not merge owners or rosters");
        f.combat.put(a,map("retreating",true,"rallyKnown",true,"rallyX",200,"rallyY",100));f.run();
        check(f.commander.preferredThreats().size()==1&&f.registry.general(a).joinTargetX==200&&f.registry.general(b).joinTargetX==1200,"A retreats without attack allocation while B retains independent normal task");
        f.enemies.put("sessionId","foreign");f.run();check(f.commander.taskForGeneral(b).status==ThreatTask.Status.UNKNOWN&&f.commander.preferredThreats().isEmpty(),"foreign sample retains UNKNOWN task and cannot create attack target");
        f.enemies.put("sessionId","s");f.visible.clear();f.run();check(f.commander.taskForGeneral(a)==null&&f.commander.preferredThreats().isEmpty(),"current visibility disappearance clears attention");
        @SuppressWarnings("unchecked") List<Map<String,Object>> tasks=(List<Map<String,Object>>)f.commander.snapshot().get("tasks");check("CLEAR".equals(tasks.get(0).get("status"))&&Boolean.FALSE.equals(tasks.get(0).get("killClaim")),"CLEAR explicitly never claims kill");
        f.enemy(501,900);f.run();ThreatTask reappeared=f.commander.currentThreatTasks().get(0);
        check(reappeared.id!=first.id&&reappeared.status==ThreatTask.Status.CURRENT&&f.commander.taskForGeneral(b)==null,"reappearing small target retains a fresh task identity without redirecting whole General");
        check(f.registry.beginTemporaryResponse(8,"local-response:small-reappearance")&&f.registry.unit(8).responseOriginOwner.equals(b.owner()),"small response saves actual original General owner");
        check(f.registry.clearLocalResponse(8)&&f.registry.unit(8).owner.equals(b.owner())&&f.registry.general(b).members.contains(8L),"response clear restores original owner and General roster before invalidation");f.registry.invalidateGeneral(b);f.run();
        check(!f.commander.preferredThreats().containsKey(b),"invalid General attention is cleaned");for(UnitView u:f.registry.units())check(!b.equals(u.reservedGeneralId),"invalid General releases pending reservations");f.invariant();
    }
    private static void independentUrgency(){Fixture f=new Fixture(12,2,true);GeneralId a=f.a(),b=f.registry.generals().get(1).id;
        f.observe();for(long actor:new long[]{2,8}){f.registry.beginLocalResponse(actor,"local-response:capacity");f.registry.clearLocalResponse(actor);f.registry.updateHealthRole(actor,HealthRole.RECOVERING);}f.observe();
        f.unit(13).put("x",1200);f.unit(14).put("x",1200);
        f.combat.put(a,map("crisis","OVERMATCHED","retreating",true,"rallyKnown",true,"rallyX",200,"rallyY",100,"currentHealthy",5,"visiblePressure",5000));
        f.combat.put(b,map("crisis","NORMAL","retreating",false,"currentHealthy",5,"visiblePressure",0));f.run();
        check(a.equals(f.registry.unit(13).reservedGeneralId)&&b.equals(f.registry.unit(14).reservedGeneralId),"crisis/retreat deficit wins a distant reinforcement before nearer normal General; reservations then balance live capacity");
        check(f.registry.general(a).joinTargetX==200&&f.registry.general(b).joinTargetX==1200,"independent active A rally and B centroid coexist");
        check(Boolean.TRUE.equals(f.registry.joiningTarget(a,f.state).get("known"))&&((Number)f.registry.joiningTarget(a,f.state).get("x")).doubleValue()==200,"ForceController and own-position adapter read the same explicit current rally");
        f.registry.beginJoining(13);f.run();check(a.equals(f.registry.unit(13).reservedGeneralId)&&f.registry.unit(13).membership==Membership.JOINING,"JOINING never reallocated to normal B");
        f.enemy(601,1000);f.run();f.visible.get(0).put("lastSeenGameTimeMs",0);f.run();
        @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)f.commander.snapshot().get("tasks");
        check("UNKNOWN".equals(rows.get(0).get("status"))&&f.commander.taskForGeneral(b)==null,"malformed/stale small visible row retains UNKNOWN task without whole-General attention");
        f.state.put("playerKey","team:1");check(f.registry.createAdditionalForming(f.state,24,100,100)==null,"foreign own player cannot create overflow lifecycle");f.state.put("playerKey","team:0");
        f.registry.invalidateGeneral(a);f.run();check(f.registry.unit(13).reservedGeneralId==null&&f.registry.unit(13).owner.equals(FREE_OWNER),"General invalidation releases actual join owner and reservation");
        check(!f.commander.preferredThreats().containsKey(a),"Commander invalidation removes independent task attention");f.invariant();
    }
    public static void main(String[] args){overflowAndBarrier();retreatPendingAndABA();sharedThreatAndUnknown();independentUrgency();System.out.println("G5CommanderHarness PASS checks="+checks);}
}
