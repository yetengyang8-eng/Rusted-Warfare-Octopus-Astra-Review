package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Synthetic current-work/coverage contracts; natural opening pace still needs Spain evidence. */
public final class R7BuilderDemandHarness {
    static int checks;
    static void require(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Map<String,Object> resource(long tile,double x,boolean current){return map("tile",tile,"x",x,"y",100.,"currentlyVisible",current);}
    static void futurePotential()throws Exception{
        R4ConstructionHarness.Fixture f=new R4ConstructionHarness.Fixture();
        f.observe(560000);f.strategy.act(1200,1);
        require(f.strategy.builderTarget()==1&&f.contexts.isEmpty(),"reachable future static domains and rich cash create no present worker demand");
        f.scout.put("resources",Arrays.asList(resource(1000,1000,true),resource(1001,2300,true)));
        f.observe(590000);f.strategy.act(1200,1);
        require(f.strategy.builderTarget()==1,"current scout sightings far from developed own work remain future expansion");
        f.scout.put("resources",Arrays.asList(resource(3000,300,false)));
        f.observe(620000);f.strategy.act(1200,1);
        require(f.strategy.builderTarget()==1,"old local mine memories do not silently become executable construction jobs");
        f.scout.put("resources",Arrays.asList(resource(3000,300,true),resource(3001,350,true)));
        f.observe(650000);f.strategy.act(1200,1);
        require(f.strategy.builderTarget()==2&&f.strategy.pending(90),"current nearby mine cluster supports one parallel lane through existing native purchase");
        int receipts=f.contexts.size();f.observe(658000);f.strategy.act(1200,1);
        require(f.strategy.builderTarget()==2&&f.contexts.size()==receipts,"paid invisible worker covers the current lane without a second purchase");
    }
    static void developedFronts()throws Exception{
        R4ConstructionHarness.Fixture f=new R4ConstructionHarness.Fixture();
        f.own.add(StrategyContractHarness.unit(91,"extractorT1",1000,100));
        f.own.add(StrategyContractHarness.unit(92,"extractorT1",2300,100));
        f.own.add(StrategyContractHarness.unit(93,"extractorT1",3600,100));
        f.scout.put("resources",Arrays.asList(resource(3000,1300,true),resource(3001,2600,true),resource(3002,3900,true)));
        f.observe(560000);f.strategy.act(1200,1);
        require(f.strategy.builderTarget()==4,"three current independent developed fronts naturally support more than early two workers");
        R4ConstructionHarness.Fixture covered=new R4ConstructionHarness.Fixture();
        covered.own.add(StrategyContractHarness.unit(3,"builder",100,100));
        covered.scout.put("resources",Arrays.asList(resource(3000,300,true)));
        covered.observe(560000);covered.strategy.act(1200,1);
        require(covered.strategy.builderTarget()==2&&covered.contexts.stream().noneMatch(c->"builder".equals(c.get("product"))),"existing idle extra worker covers current cluster before procurement");
        R4ConstructionHarness.Fixture committed=new R4ConstructionHarness.Fixture();committed.claimed=true;
        committed.scout.put("resources",Arrays.asList(resource(3000,300,true)));
        committed.observe(560000);committed.strategy.act(1200,1);
        require(committed.strategy.builderTarget()==1,"host's actual committed expansion is excluded from uncovered work");
    }
    static void nativeWorkAndSafety()throws Exception{
        R4ConstructionHarness.Fixture nativeMine=new R4ConstructionHarness.Fixture();nativeMine.minePlan=true;
        nativeMine.strategy.act(1200,1);
        require(nativeMine.strategy.builderTarget()==2,"current native mine plan establishes work without a static visibility assumption");
        R4ConstructionHarness.Fixture unknown=new R4ConstructionHarness.Fixture(){
            @Override public Map<String,Object> readStrategy(String path,String event){
                if(path.contains("type=landFactory"))return null;return super.readStrategy(path,event);
            }
        };
        unknown.facilities=9;unknown.strategy.act(1200,1);
        require(unknown.strategy.builderTarget()==1,"unpriced or unavailable native facility site cannot expand workforce from desired target alone");
        R4ConstructionHarness.Fixture danger=new R4ConstructionHarness.Fixture();danger.facilities=4;
        danger.enemies.put("visibleEnemies",Arrays.asList(map("id",9000L,"canAttack",true,"x",300.,"y",100.)));
        danger.observe(560000);danger.strategy.act(1200,1);
        require(danger.strategy.builderTarget()==1&&danger.contexts.isEmpty(),"actual home danger retains military funding and blocks construction growth");
        R4ConstructionHarness.Fixture threatenedSite=new R4ConstructionHarness.Fixture();threatenedSite.minePlan=true;
        threatenedSite.enemies.put("visibleEnemies",Arrays.asList(map("id",9000L,"canAttack",true,"x",1000.,"y",100.)));
        threatenedSite.observe(560000);threatenedSite.strategy.act(1200,1);
        require(threatenedSite.strategy.builderTarget()==1,"current threat at the proposed mine cannot buy extra workers even before it appears in threat memory");
    }
    public static void main(String[] args)throws Exception{
        futurePotential();developedFronts();nativeWorkAndSafety();
        System.out.println("R7_BUILDER_DEMAND_OK checks="+checks+" fixtureOnly=true");
    }
}
