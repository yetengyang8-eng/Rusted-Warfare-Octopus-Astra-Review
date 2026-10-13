package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Synthetic R3 construction pressure; no game throughput/desktop acceptance claim. */
public final class R4ConstructionHarness {
    static int checks;
    static void require(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Fixture implements StrategyDirector.Host {
        final StrategyContractHarness.Fake fake=new StrategyContractHarness.Fake();
        final StrategyDirector strategy=new StrategyDirector(this,fake.gate);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),force=new ArrayList<Map<String,Object>>(),contexts=new ArrayList<Map<String,Object>>();
        final Map<String,Object> world,enemies,scout;
        double income=117.4,consumption=32;
        List<Map<String,Object>> resources;
        double builderCost=500;int facilities;boolean claimed,unknownRoute,wrongKnowledge;
        int staticReads,approachReads;boolean minePlan,mineUpgrade;
        Fixture()throws Exception{
            own.add(StrategyContractHarness.unit(1,"commandCenter",100,100));own.add(StrategyContractHarness.unit(2,"builder",100,100));
            own.add(StrategyContractHarness.unit(90,"landFactory",120,100));
            for(int i=10;i<18;i++){Map<String,Object> tank=StrategyContractHarness.unit(i,"heavyTank",100,100);own.add(tank);force.add(tank);}
            world=map("sessionId","session","frame",500000L,"gameTimeMs",500000L,"map",map("tilesWide",400,"tilesHigh",370),"player",map("credits",100000),"ownUnits",own);
            enemies=map("visibleEnemies",Collections.emptyList(),"enemyIntel",Collections.emptyList());scout=map("resources",Collections.emptyList(),"rememberedThreats",Collections.emptyList());
            strategy.enable(map("strategyContractVersion",1),512);strategy.noteOrdinaryNativePrice(800);
            resources=new ArrayList<Map<String,Object>>();
            for(int i=0;i<8;i++)resources.add(map("tile",1000+i,"x",1000.+i*1300,"y",100.));
            observe(530000);
        }
        void observe(long time)throws Exception{world.put("gameTimeMs",time);world.put("frame",time);fake.stamp(world);strategy.observe(world,enemies,scout,force,0,2400000,income,consumption,1200,false);}
        void credits(double amount){BattleClient.obj(world.get("player")).put("credits",amount);}
        public boolean economicArmyPolicyStrategy(){return true;}
        public boolean assessOwnedOrdinaryStrategy(){return true;}
        public int constructionDemandStrategy(){return facilities;}
        public boolean constructionResourceCommittedStrategy(double x,double y){return claimed&&x<2300;}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.equals("/static-map/observe")){
                staticReads++;return resources==null?null:map("status","KNOWN","staticOnly",true,"sessionId","session","knowledgeId","static1","resourceTiles",resources);
            }
            if(path.startsWith("/static-map/approach")){
                approachReads++;long actor=Long.parseLong(path.split("unitId=")[1].split("&")[0]),tile=Long.parseLong(path.split("tile=")[1]);
                Map<String,Object> resource=resources.get((int)(tile-1000));
                return map("status","KNOWN","staticOnly",true,"staticPathKnown",!unknownRoute,"sessionId","session",
                    "knowledgeId",wrongKnowledge?"wrong":"static1","unitId",actor,"targetTile",tile,
                    "frame",world.get("gameTimeMs"),"gameTimeMs",world.get("gameTimeMs"),"x",resource.get("x"),"y",resource.get("y"),
                    "waypointX",resource.get("x"),"waypointY",resource.get("y"));
            }
            if(path.equals("/combat/production"))return map("factories",Arrays.asList(map("id",90L,"queue",0,"actions",Arrays.asList(
                map("type","builder","actionId","native_builder","cost",builderCost,"affordable",true)))));
            if(path.equals("/economy/investments"))return map("units",mineUpgrade?Arrays.asList(map("id",91L,"type","extractorT1","product","extractorT2",
                "actionId","native_mine_upgrade","cost",1400.,"queue",0,"affordable",true)):Collections.emptyList());
            if(path.startsWith("/expansion/plan"))return minePlan?map("builderId",Long.parseLong(path.split("unitId=")[1]),"extractorX",1000.,"extractorY",100.,"extractorCost",700.):null;
            if(path.contains("type=landFactory"))return map("unitId",Long.parseLong(path.split("unitId=")[1].split("&")[0]),
                "actionId","native_factory","type","landFactory","cost",900.,"affordable",true,"x",300.,"y",100.);
            return fake.readStrategy(path,event);
        }
        public Map<String,Object> orderStrategy(String owner,String path)throws Exception{return fake.orderStrategy(owner,path);}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context)throws Exception{
            contexts.add(new LinkedHashMap<String,Object>(context));Map<String,Object> receipt=fake.orderStrategy(owner,path);receipt.put("frame",world.get("gameTimeMs"));return receipt;
        }
        public Map<String,Object> costSourceStrategy(Map<String,Object> response){return map("costSourceObservationId","fresh-native-plan","costSourceRequestPath","fixture-native-menu");}
        public void emitStrategy(String event,Map<String,Object> data){fake.emitStrategy(event,data);}
        public void spendStrategy(String category,long cost,String product,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
    }
    static void pressure()throws Exception{
        Fixture f=new Fixture();f.facilities=4;f.strategy.act(1200,1);
        require(f.strategy.builderTarget()>2,"income and current native facility work release worker target above old2");
        require(f.contexts.size()==1&&"builder".equals(f.contexts.get(0).get("product")),"native builder is purchased through existing exact producer commitment");
        require(f.strategy.pending(90),"paid unseen builder remains held on producer");
        Fixture flowing=new Fixture();flowing.facilities=4;flowing.consumption=flowing.income;flowing.observe(560000);flowing.strategy.act(1200,1);
        require(flowing.strategy.builderTarget()>2&&flowing.contexts.stream().anyMatch(c->"builder".equals(c.get("product"))),
            "continuous military consumption matching gross income cannot suppress independently demanded payable worker growth");
        int reads=f.staticReads;f.observe(531000);f.strategy.act(1200,1);
        require(f.contexts.size()==1,"paid-before-visible builder cannot be bought twice from same producer");
        require(f.staticReads==reads,"static full-map packet is cached rather than per-observation rescanned");
        Fixture local=new Fixture();local.minePlan=true;for(int i=0;i<8;i++)local.resources.get(i).put("x",1000.+i*20);local.observe(560000);local.strategy.act(1200,1);
        require(local.strategy.builderTarget()==2,"eight nearby mines share one native600 work domain rather than eight workers");
        Fixture none=new Fixture();none.resources.clear();none.observe(560000);none.strategy.act(1200,1);
        require(none.strategy.builderTarget()==1&&none.contexts.isEmpty(),"large cash and no own construction demand create no worker purchase");
        Fixture expensive=new Fixture();expensive.facilities=4;expensive.builderCost=100000;expensive.strategy.act(1200,1);
        require(expensive.strategy.builderTarget()==1&&expensive.contexts.isEmpty(),"current expensive native quote constrains income-supported worker growth");
        Fixture bad=new Fixture();bad.facilities=4;bad.builderCost=Double.NaN;bad.strategy.act(1200,1);
        require(bad.strategy.builderTarget()==1&&bad.contexts.isEmpty(),"unknown native worker price blocks growth and spending");
        Fixture reserve=new Fixture();reserve.facilities=4;reserve.credits(1699);reserve.strategy.act(1200,1);
        require(reserve.strategy.builderTarget()>2&&reserve.contexts.isEmpty(),"demand and income never bypass exact all-reserved budget");
        Fixture facility=new Fixture();facility.resources.clear();facility.facilities=4;facility.observe(560000);facility.strategy.act(1200,1);
        require(facility.strategy.builderTarget()>2,"own uncommitted factory work supports workers even without any mine backlog");
    }
    static void travel()throws Exception{
        Fixture f=new Fixture();f.own.add(StrategyContractHarness.unit(3,"builder",100,100));f.own.add(StrategyContractHarness.unit(4,"builder",100,100));
        f.claimed=true;f.builderCost=100000;f.observe(560000);f.strategy.act(1200,1);
        require(f.fake.orders.size()==1&&f.fake.orders.get(0).startsWith("/command/move?unitId=3"),"additional worker travels on a current native static path after baseline claim");
        require(f.approachReads<=9,"capacity checks at most four new static domains per30seconds and one actual worker move");
        f.observe(561000);f.strategy.act(1200,1);require(f.fake.orders.size()==1,"allocation interval cannot duplicate active travel");
        f.observe(568000);f.strategy.act(1200,1);
        require(f.fake.orders.size()==2&&!f.fake.orders.get(0).equals(f.fake.orders.get(1)),"second independent worker has a distinct construction domain");
        require(f.fake.orders.stream().noneMatch(p->p.startsWith("/command/build-extractor")),"static resource knowledge and paths cannot authorize construction without current native site");
        Fixture unknown=new Fixture();unknown.builderCost=100000;unknown.unknownRoute=true;unknown.own.add(StrategyContractHarness.unit(3,"builder",100,100));unknown.observe(560000);unknown.strategy.act(1200,1);
        require(unknown.fake.orders.isEmpty(),"unknown native static path cannot move a worker");
        Fixture mismatch=new Fixture();mismatch.builderCost=100000;mismatch.wrongKnowledge=true;mismatch.own.add(StrategyContractHarness.unit(3,"builder",100,100));mismatch.observe(560000);mismatch.strategy.act(1200,1);
        require(mismatch.fake.orders.isEmpty(),"another map's approach cannot use this resource claim");
    }
    static void building()throws Exception{
        Fixture f=new Fixture();f.resources.clear();f.builderCost=100000;f.facilities=1;
        f.own.add(StrategyContractHarness.unit(3,"builder",100,100));f.own.add(StrategyContractHarness.unit(4,"builder",100,100));f.observe(560000);
        f.strategy.act(1200,2);require(f.contexts.size()==1&&"landFactory".equals(f.contexts.get(0).get("product")),"one real unmet factory target permits one current native construction");
        f.observe(568000);f.strategy.act(1200,2);require(f.contexts.size()==1,"paid invisible factory subtracts target before another constructor spends");
        Map<String,Object> site=StrategyContractHarness.unit(700,"landFactory",300,100);site.put("buildProgress",.5);f.own.add(site);f.observe(576000);f.strategy.act(1200,2);
        require(f.contexts.size()==1,"visible unfinished factory and its matching paid worker count exactly once");
    }
    static Fixture investment(double credits,boolean currentPlan)throws Exception{
        Fixture f=new Fixture();f.resources.clear();f.builderCost=100000;f.mineUpgrade=true;f.minePlan=currentPlan;
        f.own.add(StrategyContractHarness.unit(3,"builder",900,100));f.own.add(StrategyContractHarness.unit(91,"extractorT1",800,100));
        BattleClient.obj(f.world.get("player")).put("credits",credits);
        f.scout.put("resources",Arrays.asList(map("tile",1000L,"x",1000.,"y",100.,"currentlyVisible",true)));
        f.observe(560000);f.observe(605000);return f;
    }
    static void investmentFlow()throws Exception{
        Fixture rich=investment(4900,true);rich.strategy.act(1200,1);
        require(rich.fake.orders.stream().anyMatch(p->p.startsWith("/command/invest")),"budget for native mine plus upgrade plus replacements permits upgrade while expansion stays open");
        Fixture scarce=investment(4500,true);scarce.strategy.act(1200,1);
        require(scarce.fake.orders.stream().noneMatch(p->p.startsWith("/command/invest")),"actual affordable cheaper native site retains priority when both investments cannot be protected");
        Fixture refused=investment(4500,false);refused.strategy.act(1200,1);
        require(refused.fake.orders.stream().anyMatch(p->p.startsWith("/command/invest")),"visible empty resource without any current legal site quote cannot veto native mine upgrade forever");
    }
    public static void main(String[] args)throws Exception{pressure();travel();building();investmentFlow();System.out.println("R4_CONSTRUCTION_CONTRACT_OK checks="+checks+" fixtureOnly=true");}
}
