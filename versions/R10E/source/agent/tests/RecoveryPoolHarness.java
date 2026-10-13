package io.rwagent.client;
import java.util.*;
public final class RecoveryPoolHarness {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Map<String,Object> map(Object... a){Map<String,Object> m=new LinkedHashMap<String,Object>();for(int i=0;i<a.length;i+=2)m.put((String)a[i],a[i+1]);return m;}
    static Map<String,Object> unit(long id,double hp){return map("id",id,"type","tank","hp",hp,"maxHp",200d,"buildProgress",1d,"x",1000d,"y",1000d,"mobile",true,"canAttack",true);}
    static class Fixture {
        final CommandArbiter a=new CommandArbiter();final GeneralRegistry r=new GeneralRegistry(a);final RecoveryPool pool=new RecoveryPool(r);
        long frame=1;List<Map<String,Object>> units=new ArrayList<Map<String,Object>>();Map<String,Object> state;
        Fixture(int count){List<Long> ids=new ArrayList<Long>();for(long i=1;i<=count;i++){ids.add(i);units.add(unit(i,200));}a.observe(new CommandArbiter.Stamp("s","team:0",frame,100),ids);
            r.bootstrap(Collections.singletonList(ids),ids);sample();}
        void sample(){state=map("sessionId","s","frame",frame,"gameTimeMs",frame*100,"player",map("teamId",0),"ownUnits",units);}
        void next(){frame++;List<Long> ids=new ArrayList<Long>();for(Map<String,Object> u:units)ids.add(((Number)u.get("id")).longValue());a.observe(new CommandArbiter.Stamp("s","team:0",frame,frame*100),ids);sample();}
    }
    static void lifecycle()throws Exception{
        Fixture f=new Fixture(6);GeneralRegistry.GeneralId g=f.r.generals().get(0).id;
        Map<Long,Long> old=Collections.singletonMap(1L,f.a.ownerGeneration(1));f.units.get(0).put("hp",40d);f.pool.reconcile(f.state);
        GeneralRegistry.UnitView wounded=f.r.unit(1);check("force:recovery".equals(wounded.owner),"real recovery command owner");
        check(wounded.healthRole==GeneralRegistry.HealthRole.RECOVERING&&wounded.membership==GeneralRegistry.Membership.UNATTACHED&&wounded.generalId==null,"orthogonal detached recovery");
        check(!f.r.general(g).members.contains(1L),"General releases wounded membership");check("STALE_OWNER_GENERATION".equals(f.a.validateGenerations(Arrays.asList(1L),old)),"old General intent invalidated");
        check(!f.r.requestJoin(1,g),"recovering not allocated");check(f.pool.targetBays()==0,"small early wound does not demand new bay");
        long generation=wounded.ownerGeneration;f.units.get(0).put("hp",155d);f.pool.reconcile(f.state);check("force:recovery".equals(f.r.unit(1).owner),"same-frame HP edit cannot release recovery");
        f.next();f.pool.reconcile(f.state);check(GeneralRegistry.FREE_OWNER.equals(f.r.unit(1).owner)&&f.r.unit(1).healthRole==GeneralRegistry.HealthRole.NORMAL,"later >=75 percent current HP returns FREE");
        check(f.a.ownerGeneration(1)==generation+1,"recovery release generation advances");check(!f.r.requestJoin(1,g),"recovered release-frame FREE barrier retained");
        f.next();check(f.r.requestJoin(1,g)&&f.r.beginJoining(1),"recovered next-frame normal joining reentry");
        f.units.get(0).put("hp",20d);f.pool.reconcile(f.state);check(f.r.general(g).reservations.isEmpty()&&f.r.unit(1).reservedGeneralId==null,"new severe wound clears JOINING reservation");
        f.units.get(0).put("hp",100d);f.next();f.pool.reconcile(f.state);check("force:recovery".equals(f.r.unit(1).owner),"50 percent does not prematurely heal role");
        Map<String,Object> stale=new LinkedHashMap<String,Object>(f.state);stale.put("sessionId","other");f.units.get(0).put("hp",190d);check(!f.pool.reconcile(stale)&&"force:recovery".equals(f.r.unit(1).owner),"foreign packet cannot restore health");
    }
    static void capacityAndCommands()throws Exception{
        Fixture f=new Fixture(40);for(Map<String,Object> u:f.units)u.put("hp",20d);f.pool.reconcile(f.state);check(f.pool.targetBays()>=5,"large wound pool requests scaled service capacity");
        final List<ForceController.Proposal> proposals=new ArrayList<ForceController.Proposal>();ForceController.Host host=new ForceController.Host(){
            public Map<String,Object> read(String p,String e){return null;}public List<Map<String,Object>> eligible(List<Map<String,Object>> a,Map<String,Object> t,Map<String,Object> e){return a;}
            public void collect(ForceController.Proposal p){proposals.add(p);}};
        Map<String,Object> bay=unit(100,1000);bay.put("type","repairbay");bay.put("x",500d);bay.put("y",500d);f.units.add(bay);f.sample();f.pool.collect(f.state,100,100,host);
        check(proposals.size()==40,"independent wounded intents collected");for(ForceController.Proposal p:proposals){check(p.path.startsWith("/command/move?")&&p.path.contains("x=500.0")&&p.priority==90,"move toward actual repair bay, no reverse attack move");p.accepted(map("status","queued","frame",1));}
        proposals.clear();f.pool.collect(f.state,100,100,host);check(proposals.isEmpty(),"accepted moves reuse bounded cooldown");check("force:recovery".equals(f.r.unit(1).owner),"accepted arrival does not claim healing");
        f.next();f.units.get(0).put("x",500d);f.units.get(0).put("y",500d);f.pool.reconcile(f.state);check(f.r.unit(1).healthRole==GeneralRegistry.HealthRole.RECOVERING,"arrival alone not restored HP");
        Fixture near=new Fixture(6);near.units.get(0).put("hp",20d);near.units.get(0).put("x",100d);near.units.get(0).put("y",100d);near.units.get(0).put("orderType","attackMove");near.pool.reconcile(near.state);
        proposals.clear();near.pool.collect(near.state,100,100,host);check(proposals.size()==1&&proposals.get(0).path.startsWith("/command/move?"),"new recovery owner cancels old attack-move even already at recovery point");
    }
    public static void main(String[] args)throws Exception{lifecycle();capacityAndCommands();System.out.println("RecoveryPoolHarness PASS checks="+checks);}
}
