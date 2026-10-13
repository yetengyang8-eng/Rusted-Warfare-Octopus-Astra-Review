package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Own-front demand and region-retention allocator contracts. Synthetic packets only. */
public final class R9BuilderCoverageHarness {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Map<String,Object> front(long id,double x){return map("generalId",id,"x",x,"y",100.,"readyMembers",6);}
    static final class Fronts extends R4ConstructionHarness.Fixture {
        List<Map<String,Object>> fronts=new ArrayList<Map<String,Object>>();
        Fronts()throws Exception{super();}
        public List<Map<String,Object>> constructionMilitaryFrontsStrategy(){return fronts==null?Collections.emptyList():fronts;}
    }
    static void spatialDemand()throws Exception{
        Fronts f=new Fronts();f.fronts.add(front(1,1000));f.fronts.add(front(2,2300));f.fronts.add(front(3,3600));
        f.scout.put("resources",Arrays.asList(R7BuilderDemandHarness.resource(1000,1100,true),R7BuilderDemandHarness.resource(1001,2400,true),R7BuilderDemandHarness.resource(1002,3700,true)));
        f.observe(560000);f.strategy.act(1200,1);
        check(f.strategy.builderTarget()==4,"three actual separate General positions prepare current mineral fronts without waiting for an existing mine there");
        check(f.contexts.stream().anyMatch(c->"builder".equals(c.get("product"))),"prepared fronts use existing native worker procurement");f.strategy.close();
        Fronts count=new Fronts();for(long id=1;id<=20;id++)count.fronts.add(front(id,1000));count.resources.clear();count.observe(560000);count.strategy.act(1200,1);
        check(count.strategy.builderTarget()==1,"twenty Generals without economic opportunities add no workforce by count");count.strategy.close();
        Fronts cluster=new Fronts();cluster.fronts.add(front(1,1000));cluster.fronts.add(front(2,1100));
        cluster.scout.put("resources",Arrays.asList(R7BuilderDemandHarness.resource(1000,1000,true),R7BuilderDemandHarness.resource(1001,1150,true)));
        cluster.observe(560000);cluster.strategy.act(1200,1);check(cluster.strategy.builderTarget()==2,"nearby General formations and mines share actual construction coverage");cluster.strategy.close();
        Fronts unknown=new Fronts();unknown.fronts.add(map("generalId",1,"x",1000.,"y",100.,"readyMembers",0));
        unknown.scout.put("resources",Arrays.asList(R7BuilderDemandHarness.resource(1000,1000,true)));unknown.unknownRoute=true;
        unknown.observe(560000);unknown.strategy.act(1200,1);check(unknown.strategy.builderTarget()==1,"an absent ready own formation is not a prepared front");unknown.strategy.close();
        Fronts staticFront=new Fronts();staticFront.fronts.add(front(1,1000));staticFront.observe(560000);staticFront.strategy.act(1200,1);
        check(staticFront.strategy.builderTarget()==2,"known static resource and validated route beside own General create exploration capacity, not a build order");
        check(staticFront.fake.orders.stream().noneMatch(p->p.startsWith("/command/build-extractor")),"static opportunity never grants native construction authority");staticFront.strategy.close();
        Fronts danger=new Fronts();danger.fronts.add(front(1,1000));danger.scout.put("resources",Arrays.asList(R7BuilderDemandHarness.resource(1000,1000,true)));
        danger.enemies.put("visibleEnemies",Arrays.asList(map("id",9000L,"canAttack",true,"x",1000.,"y",100.)));danger.observe(560000);danger.strategy.act(1200,1);
        check(danger.strategy.builderTarget()==1,"actual local military danger does not create safe construction demand");danger.strategy.close();
        Fronts more=new Fronts();more.fronts.add(front(1,1000));more.facilities=4;more.observe(560000);more.strategy.act(1200,1);
        check(more.strategy.builderTarget()>more.fronts.size(),"native home facilities can demand more builders than Generals without an artificial ratio");more.strategy.close();
        Fronts misplaced=new Fronts();misplaced.fronts.add(front(1,1000));misplaced.own.add(StrategyContractHarness.unit(3,"builder",100,100));
        misplaced.observe(560000);misplaced.strategy.act(1200,1);
        check(((Number)G53ContinuingOperationsHarness.get(misplaced.strategy,"constructionIdleWorkers")).intValue()==0
            &&((Number)G53ContinuingOperationsHarness.get(misplaced.strategy,"constructionUnmetJobs")).intValue()==1,"near-home idle constructor does not report coverage of the separate General mineral front");
        check(misplaced.fake.orders.stream().anyMatch(p->p.startsWith("/command/move?unitId=3")&&p.contains("x=1000.0")),"existing misplaced spare is dispatched to actual prepared front before more procurement is necessary");
        check(misplaced.strategy.builderTarget()==2,"spatial gap cannot repeatedly buy more workers than currently known construction lanes");misplaced.strategy.close();
    }
    static final class Recovery extends G51ProspectIntegrationHarness.Fixture {
        int recovery;
        Recovery()throws Exception{super();}
        public int recoveryCapacityNeed(){return recovery;}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.contains("type=repairbay"))return map("unitId",Long.parseLong(path.split("unitId=")[1].split("&")[0]),"type","repairbay","actionId","native-repairbay","cost",1500.,"affordable",true,"x",7370.,"y",1950.);
            return super.readStrategy(path,event);
        }
        public boolean economicArmyPolicyStrategy(){return true;}
    }
    static void repairRetention()throws Exception{
        Recovery f=new Recovery();f.start();f.arrive();f.recovery=1;f.credits=300;f.plan=map("sessionId","session","builderId",5924L,"extractorCost",700.,"extractorX",7410.,"extractorY",410.);
        f.observe(136000,102);long generation=f.gate.ownerGeneration(5924L);f.director.act(0,1);
        check(f.orders.stream().noneMatch(p->p.startsWith("/command/move?unitId=5924")&&p.contains("y=1950.0")),"repair deficiency does not recall arrived remote miner even when its current mine is unaffordable");
        check(f.eventCount("strategy_recovery_builder_rally")==0&&f.eventCount("strategy_home_construction_deferred")==1,"deferral records the retained regional responsibility instead of an accepted rally");
        check(f.gate.ownerGeneration(5924L)==generation,"economic-front retention preserves lease generation");
        f.own.add(G51ProspectIntegrationHarness.unit(5925,"builder",7370,1950));f.credits=10000;f.observe(152000,103);f.director.act(0,1);
        check(f.orders.stream().anyMatch(p->p.startsWith("/command/construct?unitId=5925")),"local idle extra constructor services repair without hauling the remote prospect home");f.director.close();
        R4ConstructionHarness.Fixture demand=new R4ConstructionHarness.Fixture(){public int recoveryCapacityNeed(){return 1;}};
        demand.own.add(StrategyContractHarness.unit(3,"builder",1000,100));demand.observe(560000);
        Object remote=((Map<?,?>)G53ContinuingOperationsHarness.get(demand.strategy,"workers")).get(3L);
        ProspectCommitment prospect=(ProspectCommitment)G53ContinuingOperationsHarness.get(remote,"prospect");prospect.accepted(1000,1000,100,100,100,530000,530000);prospect.observeArrival(1000,100,540000,540000);
        demand.strategy.act(1200,1);
        check(demand.strategy.builderTarget()==3&&demand.contexts.stream().anyMatch(c->"builder".equals(c.get("product"))),"a real unserved home recovery site procures capacity while the only extra builder retains its remote economic commitment");demand.strategy.close();
    }
    static void capitalRetention()throws Exception{
        G6CapitalPolicyHarness.Host h=G6CapitalPolicyHarness.funded();
        for(Map<String,Object> u:h.own)if("builder".equals(u.get("type")))u.put("x",2000.);
        Object worker=G6CapitalPolicyHarness.worker(h,3);ProspectCommitment prospect=(ProspectCommitment)G53ContinuingOperationsHarness.get(worker,"prospect");
        prospect.accepted(1000,2000,100,100,100,420000,420000);prospect.observeArrival(2000,100,421000,421000);
        h.observe(421000);long generation=h.fake.gate.ownerGeneration(3L);h.policy.act(0,1);
        check(h.fake.orders.stream().noneMatch(p->p.startsWith("/command/move?unitId=3")),"capital investment never treats arrived remote regional builder as merely idle");
        check(h.purchases("fabricatorT1")==0&&prospect.arrived(),"absent a home constructor leaves the remote claim intact while procurement can provide home capacity");
        check(h.fake.gate.ownerGeneration(3L)==generation,"capital deferral leaves ownership generation intact");h.policy.close();
    }
    public static void main(String[] args)throws Exception{spatialDemand();repairRetention();capitalRetention();System.out.println("R9_BUILDER_COVERAGE_OK checks="+checks+" fixtureOnly=true");}
}
