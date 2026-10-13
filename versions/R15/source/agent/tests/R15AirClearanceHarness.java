package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;
import static io.rwagent.client.BattleClient.*;

/** Synthetic lifecycle packets: native movement, visibility and victory need separate evidence. */
public final class R15AirClearanceHarness {
    static int checks;
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static final class Host implements StrategyDirector.Host {
        final R13AirLifecycleHarness.Host base=new R13AirLifecycleHarness.Host();
        final List<Map<String,Object>> commands=new ArrayList<Map<String,Object>>();
        final List<Map<String,Object>> intel=new ArrayList<Map<String,Object>>();
        String land="NONE";
        public String landRouteDemandStrategy(){return land;}
        public Map<String,Object> costSourceStrategy(Map<String,Object> p){return base.costSourceStrategy(p);}
        public Map<String,Object> readStrategy(String p,String e){
            if(p.contains("observeX="))return map("status","no_frontier","sessionId","s");
            return base.readStrategy(p,e);
        }
        public Map<String,Object> orderStrategy(String owner,String p){return orderStrategy(owner,p,Collections.<String,Object>emptyMap());}
        public Map<String,Object> orderStrategy(String owner,String p,Map<String,Object> context){
            String field=p.contains("unitIds=")?"unitIds":"unitId";
            for(String id:p.split(field+"=")[1].split("&")[0].split(","))check(nativeHost().arbiter.owns(owner,Long.parseLong(id)),"each command keeps actual independent owner");
            commands.add(map("owner",owner,"path",p,"context",context));return base.orderStrategy(owner,p,context);
        }
        public void emitStrategy(String e,Map<String,Object> d){base.emitStrategy(e,d);}
        public void spendStrategy(String c,long price,String t,long actor){}
        public void strategicAttack(Map<String,Object> r){}
        G6EndgameHarness.Host nativeHost(){return base.base.nativeHost;}
        Map<String,Object> actor(long id){return base.actor(id);}
        AirGeneralController create(int count)throws Exception{
            for(long id=12;id<10+count;id++)nativeHost().own.add(G6EndgameHarness.unit(id,"amphibiousJet",100,100));
            nativeHost().sample();AirGeneralController air=new AirGeneralController(this,nativeHost().arbiter);
            for(long id=10;id<10+count;id++){nativeHost().arbiter.claim("strategy:"+id,id);air.accept(id,"strategy:"+id,new CapabilityUnitMode(id));}
            next(air);return air;
        }
        void next(AirGeneralController air)throws Exception{base.next(air);nativeHost().enemy.put("enemyIntel",intel);base.base.observe(air);}
        void known(long id,double x,double y){intel.add(map("id",id,"status","LOST_CONTACT","lastKnownBuilding",true,"lastKnownType","c_antiAirTurretT2","lastKnownX",x,"lastKnownY",y,"lastSeenGameTimeMs",1000L));}
        void danger(AirGeneralController air,long id,double x,double y)throws Exception{
            Map<String,Object> e=R13AirLifecycleHarness.enemy(id,"c_antiAirTurretT2",x,y,true,true,false);e.put("range",320.);e.put("building",true);
            nativeHost().vision.put("rememberedThreats",Arrays.asList(e));base.base.observe(air);
        }
        boolean reason(String reason){for(Map<String,Object> c:commands)if(reason.equals(obj(c.get("context")).get("reason")))return true;return false;}
        long attacks(){return commands.stream().filter(c->String.valueOf(c.get("path")).startsWith("/command/attack-target")).count();}
    }
    static void assembledForceApproachesWithoutGhostAttack()throws Exception{
        Host h=new Host();h.known(90,1000,500);AirGeneralController a=h.create(12);h.danger(a,90,1000,500);
        long gen=h.nativeHost().arbiter.ownerGeneration(10);a.act();
        check(h.reason("CLEARANCE_APPROACH_CURRENT_VISIBILITY"),"formed force can enter remembered circle with MOVE to regain lawful sight");
        check(h.attacks()==0,"remembered building is never an attack permission");
        check(n(h.actor(10),"orderX")==1000&&n(h.actor(10),"orderY")==500,"investigation endpoint is real last-known site, not unreachable safe stand");
        check(Boolean.TRUE.equals(a.snapshot().get("clearanceProbe")),"bounded own-risk probe is explicit");
        check(items(a.snapshot(),"formations").size()==1,"known clearance work is allocated before idle search splitting");
        int orders=h.commands.size();a.act();check(h.commands.size()==orders,"unchanged real MOVE reused without refresh churn");
        h.next(a);h.base.contacts(a,R13AirLifecycleHarness.enemy(90,"c_antiAirTurretT2",1000,500,true,true,false));a.act();
        check(h.attacks()==1,"fresh native engagement can resume direct attack after current visibility returns");
        check(gen==h.nativeHost().arbiter.ownerGeneration(10),"clearance does not replace its owner or generation");a.close();
    }
    static void smallOrScatteredForceCannotPretendAssembled()throws Exception{
        for(int count:new int[]{2,6}){
            Host h=new Host();h.known(90,1000,500);AirGeneralController a=h.create(count);
            if(count==6){for(long id=10;id<16;id++){h.actor(id).put("x",(id-10)*500.);h.actor(id).put("y",100.);}h.next(a);}
            h.danger(a,90,1000,500);a.act();
            check(!h.reason("CLEARANCE_APPROACH_CURRENT_VISIBILITY"),"roster or scattered members do not prove an assembled probe force");
            check(h.attacks()==0,"insufficient force cannot attack fog");
            if(count==6){check(h.reason("CLEARANCE_ASSEMBLE_CURRENT_OWN_FORCE"),"scattered attached members receive a real assembly command");
                for(long id=10;id<16;id++){h.actor(id).put("x",100.);h.actor(id).put("y",100.);}
                h.next(a);h.danger(a,90,1000,500);h.commands.clear();a.act();
                check(h.reason("CLEARANCE_APPROACH_CURRENT_VISIBILITY"),"later actual assembled positions close assembly into investigation");}
            a.close();
        }
    }
    static void clearPointContinuesNextKnownSite()throws Exception{
        Host h=new Host();h.known(90,1000,500);h.known(91,1600,1500);AirGeneralController a=h.create(12);h.danger(a,90,1000,500);a.act();
        Object frontier=a.snapshot().get("routeFrontierTile");h.base.visible=true;
        h.intel.get(0).put("status","CLEARED");h.next(a);h.commands.clear();a.act();
        check(number(obj(a.snapshot().get("contactArea")),"contactId",-1)==91,"positive local clear continues other lawful remembered building work");
        check(Boolean.TRUE.equals(a.snapshot().get("clearanceMission"))&&items(a.snapshot(),"formations").size()==1,"local clear does not release coherent clearance force to search/split");
        check(!h.reason("SEARCH")&&h.attacks()==0,"no old search or ghost attack overrides next-site investigation");
        // Empty visibility alone is local evidence; remove the final known task only after its own check.
        h.intel.get(1).put("status","CLEARED");h.next(a);a.act();
        check(a.snapshot().get("contactArea")==null&&!Boolean.TRUE.equals(a.snapshot().get("clearanceMission")),"exhausted known work returns to ordinary search without claiming world clear");a.close();
    }
    static void observedLossWithdrawsAndDoesNotReprobeOnTimer()throws Exception{
        Host h=new Host();h.known(90,1000,500);AirGeneralController a=h.create(6);h.danger(a,90,1000,500);a.act();
        for(long id=10;id<16;id++){h.actor(id).put("x",700.);h.actor(id).put("y",500.);h.actor(id).put("hp",400.);}
        h.next(a);h.danger(a,90,1000,500);h.nativeHost().enemy.put("sessionId","unknown");h.base.base.observe(a);h.commands.clear();a.act();
        check(h.reason("CLEARANCE_WITHDRAW_TO_SAFE_POINT"),"actual own HP budget loss withdraws instead of standing under fire");
        check(n(h.actor(10),"orderX")==100&&n(h.actor(10),"orderY")==100,"withdrawal uses remembered own safe point");
        check(Boolean.TRUE.equals(a.snapshot().get("clearanceRetreat"))&&h.attacks()==0,"retreat never reinterprets absence as kill or authorizes hidden attack");
        for(long id=10;id<16;id++){h.actor(id).put("x",100.);h.actor(id).put("y",100.);}
        h.next(a);h.commands.clear();a.act();h.next(a);a.act();
        check(h.commands.isEmpty(),"same depleted force at safety cannot spam hold/retry without changed own evidence");
        for(long id=10;id<16;id++)h.actor(id).put("hp",1000.);
        h.next(a);a.act();h.next(a);h.commands.clear();a.act();
        check(h.reason("CLEARANCE_APPROACH_CURRENT_VISIBILITY"),"same roster fully repaired can retry without requiring an impossible HP increase beyond max");a.close();
    }
    static void knownAreasKeepIndependentOwners()throws Exception{
        R10AirFormationsHarness.Host h=new R10AirFormationsHarness.Host(18);AirGeneralController a=h.formation();
        for(int i=0;i<3;i++){h.next(a);a.act();}
        List<Map<String,Object>> intel=new ArrayList<Map<String,Object>>();
        for(int i=0;i<3;i++)intel.add(map("id",90L+i,"status","LOST_CONTACT","lastKnownBuilding",true,"lastKnownX",i==2?100.:1000.,"lastKnownY",i==0?100.:1000.,"lastSeenGameTimeMs",1000L));
        h.nativeHost.enemy.put("enemyIntel",intel);h.observe(a);a.act();Set<Long> held=new HashSet<Long>();
        for(Map<String,Object> f:items(a.snapshot(),"formations")){
            Map<String,Object> area=obj(f.get("contactArea"));check(area!=null&&!area.isEmpty(),"each existing capable formation receives remaining known area work");
            held.add((long)number(area,"contactId",-1));}
        check(held.size()==3,"separated known regions do not collapse independent owners onto one nearest island");a.close();
    }
    static void staleSourceCannotCreateMission()throws Exception{
        Host h=new Host();h.known(90,1000,500);AirGeneralController a=h.create(12);h.nativeHost().enemy.put("sessionId","foreign");h.base.base.observe(a);a.act();
        check(a.snapshot().get("contactArea")==null&&h.commands.isEmpty(),"foreign contact source cannot assign remembered mission or controlled approach");a.close();
    }
    public static void main(String[] args)throws Exception{
        assembledForceApproachesWithoutGhostAttack();smallOrScatteredForceCannotPretendAssembled();clearPointContinuesNextKnownSite();observedLossWithdrawsAndDoesNotReprobeOnTimer();knownAreasKeepIndependentOwners();staleSourceCannotCreateMission();
        System.out.println("R15_AIR_CLEARANCE_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");
    }
}
