package io.rwagent.client;

import java.util.*;
import java.lang.reflect.*;
import static io.rwagent.client.StrategyDirector.*;

/** Economic inventory, native mine branches and paid recovery construction. Synthetic inputs. */
public final class GlobalStrategyPolicyHarness {
    static int checks;
    static void require(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    static GlobalStrategyPolicy.Goal goal(double income,double cost,int mission,boolean visible,boolean recovery){
        return GlobalStrategyPolicy.goal(96,88,512,false,500000,income,40,cost,mission,visible,recovery);
    }
    static void goals(){
        GlobalStrategyPolicy.Goal rich=goal(306,800,144,false,false);
        require(rich.total>128&&rich.total<=512,"own continuing missions and replacement-funded income support inventory beyond old128");
        require(rich.active==88,"economic inventory expansion leaves each General formation contract unchanged");
        require("UNKNOWN".equals(rich.evidence.get("nativeUnitCapStatus")),"software ceiling cannot claim native cap evidence");
        require(goal(306,800,0,false,false).total==96,"income without own mission or compatible current demand never expands inventory");
        require(goal(306,800,0,true,false).total>128,"current compatible ordinary route independently supports inventory demand");
        require(goal(306,Double.NaN,144,false,false).total==96,"unknown native replacement price cannot justify economic scale");
        require(goal(20,800,144,false,false).total==96,"consumption beyond current income cannot justify scale");
        require(goal(306,800,144,false,true).total==96,"local recovery protects scale growth");
        require(GlobalStrategyPolicy.goal(40,32,40,true,500000,306,40,800,144,true,false).total==40,"explicit40 experiment retains exact override");
        require(GlobalStrategyPolicy.goal(40,32,128,true,500000,306,40,800,144,true,false).total==128,"explicit128 experiment retains exact override");
        require(goal(100000,350,144,true,false).total==512,"rich economy retains bounded software ceiling");
    }
    static final class Fixture implements StrategyDirector.Host {
        final StrategyContractHarness.Fake fake=new StrategyContractHarness.Fake();
        final StrategyDirector strategy=new StrategyDirector(this,fake.gate);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),force=new ArrayList<Map<String,Object>>();
        final List<Map<String,Object>> contexts=new ArrayList<Map<String,Object>>();
        final Map<String,Object> world,enemies,scout;
        int mission=144,recovery;double creditsOverride=-1,income=306;boolean safeZone,plan=true,wrongActor,malformedCost,mineMenu;
        Fixture()throws Exception{
            own.add(StrategyContractHarness.unit(1,"commandCenter",100,100));
            own.add(StrategyContractHarness.unit(2,"builder",100,100));
            own.add(StrategyContractHarness.unit(3,"builder",100,100));
            own.add(StrategyContractHarness.unit(90,"landFactory",120,100));
            for(int i=10;i<18;i++){Map<String,Object> tank=StrategyContractHarness.unit(i,"heavyTank",100,100);own.add(tank);force.add(tank);}
            world=map("sessionId","session","frame",200L,"gameTimeMs",500000L,"map",map("tilesWide",400,"tilesHigh",370),
                "player",map("credits",100000),"ownUnits",own);
            enemies=map("visibleEnemies",Collections.emptyList(),"enemyIntel",Collections.emptyList());
            scout=map("resources",Collections.emptyList(),"rememberedThreats",Collections.emptyList());
            strategy.enable(map("strategyContractVersion",1),512);strategy.noteOrdinaryNativePrice(800);
            observe(500000);
        }
        void observe(long time)throws Exception{world.put("gameTimeMs",time);world.put("frame",time/1000);fake.stamp(world);
            strategy.observe(world,enemies,scout,force,0,2400000,income,40,1200,false);}
        void credits(double amount){BattleClient.obj(world.get("player")).put("credits",amount);}
        public boolean economicArmyPolicyStrategy(){return true;}
        public int militaryMissionDemandStrategy(){return mission;}
        public int recoveryCapacityNeed(){return recovery;}
        public boolean mineSafeZoneStrategy(Map<String,Object> mine){return safeZone;}
        public double effectiveStrategyCredits(double nativeCredits){return creditsOverride>=0?creditsOverride:nativeCredits;}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.contains("type=repairbay"))return plan?map("unitId",wrongActor?1000:Long.parseLong(path.split("unitId=")[1].split("&")[0]),
                "type","repairbay","actionId","current-native-repair","cost",malformedCost?Double.NaN:1500.,"affordable",true,"x",140.,"y",100.):null;
            if(path.equals("/economy/investments"))return mineMenu?map("units",Arrays.asList(
                map("id",91L,"type","extractorT3","product","extractorT3_reinforced","actionId","live-reinforce","cost",3700.,"queue",0,"affordable",true),
                map("id",91L,"type","extractorT3","product","extractorT3_overclocked","actionId","live-overclock","cost",8700.,"queue",0,"affordable",true))):map("units",Collections.emptyList());
            return fake.readStrategy(path,event);
        }
        public Map<String,Object> costSourceStrategy(Map<String,Object> response){return map("costSourceObservationId","fresh-native-plan","costSourceRequestPath",response.containsKey("units")?"/economy/investments":"/economy/construction-plan");}
        public Map<String,Object> orderStrategy(String owner,String path)throws Exception{return fake.orderStrategy(owner,path);}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context)throws Exception{contexts.add(new LinkedHashMap<String,Object>(context));return fake.orderStrategy(owner,path);}
        public void emitStrategy(String event,Map<String,Object> data){fake.emitStrategy(event,data);}
        public void spendStrategy(String category,long cost,String product,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
    }
    static void actualCapacity()throws Exception{
        Fixture f=new Fixture();set(f.strategy,"armyTarget",128);set(f.strategy,"lastCapacity",0L);
        f.observe(530000);require(f.strategy.armyTarget()==136,"real policy observation releases eight army slots above old128");
        f.observe(560000);require(f.strategy.armyTarget()==144,"next policy evaluation grows at most eight more");
        require(f.strategy.safetyCapacityAvailable(),"new software ceiling leaves paid-unit safety accounting available");
        set(f.strategy,"hardCap",10);f.strategy.noteUnobservedCombatSlots(2);
        require(!f.strategy.safetyCapacityAvailable(),"existing eight armed plus two paid-before-visible commitments still fill safety budget");
    }
    static void economicGrowthBoundary()throws Exception{
        Fixture f=new Fixture();f.mission=128;f.income=100;set(f.strategy,"armyTarget",128);set(f.strategy,"lastCapacity",0L);
        f.observe(530000);
        require(f.strategy.armyTarget()==128,"lower-income own mission goal remains128");
        require(!f.strategy.increaseMilitaryCapacity(128,8,map()),"150s expansion cannot create a floor above economic goal128");
        f.mission=144;f.observe(560000);
        require(f.strategy.armyTarget()==136&&f.strategy.increaseMilitaryCapacity(136,8,map()),"higher actual mission goal144 permits only remaining bounded growth");
        require(f.strategy.armyTarget()==144&&!f.strategy.increaseMilitaryCapacity(144,8,map()),"goal144 cannot slowly escape toward software512");
        f.income=300;f.observe(590000);
        require(f.strategy.armyTarget()==152&&f.strategy.increaseMilitaryCapacity(152,8,map()),"new income-supported goal releases new bounded growth");
        f.income=100;f.mission=128;f.strategy.noteOrdinaryNativePrice(350);f.observe(620000);
        require(f.strategy.armyTarget()==168&&f.strategy.increaseMilitaryCapacity(168,8,map()),"updated native replacement price can support a newly higher goal");
        f.strategy.noteOrdinaryNativePrice(800);f.mission=128;f.income=100;
        while(f.force.size()<160){long id=1000+f.force.size();Map<String,Object> tank=StrategyContractHarness.unit(id,"heavyTank",100,100);f.force.add(tank);f.own.add(tank);}
        f.strategy.noteUnobservedCombatSlots(2);f.observe(650000);
        require(f.strategy.armyTarget()==162,"falling economic goal preserves160 held armed plus two paid-before-visible slots");
        require(!f.strategy.increaseMilitaryCapacity(162,8,map()),"retained above-goal inventory is not permission for further expansion");
        require(f.strategy.committedArmedForPolicy()==160,"policy change never cancels existing armed inventory");
        f.strategy.noteUnobservedCombatSlots(352);
        require(!f.strategy.safetyCapacityAvailable(),"paid holds remain charged after lower economic goal");
    }
    static void recoveryConstruction()throws Exception{
        Fixture f=new Fixture();f.recovery=1;f.observe(530000);f.credits(2699);
        require(!f.strategy.act(1200,1)&&f.fake.orders.isEmpty(),"recovery native construction protects every reserved credit");
        f.credits(2700);f.observe(531000);require(f.strategy.act(1200,1),"exact protected native cost permits existing extra builder recovery construction");
        require(f.contexts.size()==1&&"repairbay".equals(f.contexts.get(0).get("product")),"recovery owns one actual construction commitment");
        require(number(f.contexts.get(0),"cost",0)==1500&&"VALIDATED_NATIVE_QUOTE_PACKET".equals(f.contexts.get(0).get("costSourceStatus")),"recovery preserves actual quote cost and packet provenance");
        f.own.add(StrategyContractHarness.unit(4,"builder",100,100));f.observe(532000);f.strategy.act(1200,1);
        require(f.contexts.size()==1,"paid-before-visible recovery site blocks duplicate other-builder construction");
        Map<String,Object> bay=StrategyContractHarness.unit(700,"repairbay",140,100);bay.put("buildProgress",.5);f.own.add(bay);
        f.observe(540000);f.strategy.act(1200,1);require(f.contexts.size()==1,"visible unfinished recovery site and its paid commitment count only once");
        bay.put("buildProgress",1);f.observe(550000);f.strategy.act(1200,1);
        require(f.contexts.size()==1&&f.fake.events("strategy_construction_observed")==1,"ready native witness completes exactly one recovery construction");
        f.recovery=2;f.credits(100000);f.observe(560000);require(f.strategy.act(1200,1)&&f.contexts.size()==2,"additional recovery demand uses one more native site beyond actual ready capacity");
        Fixture unsettled=new Fixture();unsettled.recovery=1;unsettled.creditsOverride=1000;unsettled.observe(530000);
        require(!unsettled.strategy.act(1200,1)&&unsettled.fake.orders.isEmpty(),"unsettled effective credits guard recovery despite huge native balance");
        Fixture wrong=new Fixture();wrong.recovery=1;wrong.wrongActor=true;wrong.observe(530000);wrong.strategy.act(1200,1);
        require(wrong.fake.orders.isEmpty(),"native recovery plan for another constructor cannot authorize spending");
        Fixture bad=new Fixture();bad.recovery=1;bad.malformedCost=true;bad.observe(530000);bad.strategy.act(1200,1);
        require(bad.fake.orders.isEmpty(),"unknown native recovery price stays unspent");
        Fixture danger=new Fixture();danger.recovery=1;danger.enemies.put("visibleEnemies",Arrays.asList(map("id",999L,"type","heavyTank","hp",1000,"canAttack",true,"x",Double.NaN,"y",100)));
        danger.observe(530000);danger.strategy.act(1200,1);
        require(danger.contexts.stream().noneMatch(c->"repairbay".equals(c.get("product"))),"unknown current threat geometry cannot authorize a recovery site");
        Fixture overlap=new Fixture();overlap.recovery=2;overlap.own.add(StrategyContractHarness.unit(4,"builder",100,100));overlap.observe(530000);
        require(overlap.strategy.act(1200,1),"first recovery commitment of two admitted");
        overlap.observe(531000);require(overlap.strategy.act(1200,1),"second independent constructor commits remaining recovery demand");
        Map<String,Object> oneVisible=StrategyContractHarness.unit(700,"repairbay",140,100);oneVisible.put("buildProgress",.5);overlap.own.add(oneVisible);
        overlap.own.add(StrategyContractHarness.unit(5,"builder",100,100));overlap.observe(532000);overlap.strategy.act(1200,1);
        require(overlap.contexts.stream().filter(c->"repairbay".equals(c.get("product"))).count()==2,
            "one visible site cannot satisfy two paid constructors and permit a third duplicate");
    }
    static void mineBranches()throws Exception{
        MineInvestmentPolicy p=new MineInvestmentPolicy();Map<String,Object> mine=MineInvestmentHarness.mine("extractorT3");
        Map<String,Object> reinforce=MineInvestmentHarness.quote("extractorT3","extractorT3_reinforced",3700),
            overclock=MineInvestmentHarness.quote("extractorT3","extractorT3_overclocked",8700);
        for(long time=0;time<=180000;time+=5000)
            MineInvestmentHarness.observe(p,mine,time,MineInvestmentHarness.emptyEnemies(),MineInvestmentHarness.emptyScout());
        MineInvestmentPolicy.Decision safe=p.evaluate(mine,overclock,180000,2400,100000,1200,800,39,false,false,true,true);
        require(safe.selected,"full native chain admits actual overclock quote only in known stable zone");
        require(Math.abs(number(safe.evidence,"paybackEstimateGameSeconds",0)-8700/(12.07*10/8))<.001,"overclock payback uses changed native price instead of8000constant");
        require(!p.evaluate(mine,overclock,180000,2400,100000,1200,800,39,false,false,true,false).selected,"quiet visibility alone cannot prove safe overclock zone");
        require(!p.evaluate(mine,reinforce,180000,2400,100000,1200,800,39,false,false,true,true).selected,"safe stable native mine prefers income branch over pointless defense spend");
        MineInvestmentPolicy.Decision conservative=p.evaluate(mine,reinforce,180000,30,100000,1200,800,39,false,false,true,false);
        require(conservative.selected&&number(conservative.evidence,"incomeGainEstimate",-1)==0
            &&conservative.evidence.get("paybackEstimateGameSeconds")==null,"unconfirmed safety may reinforce without inventing income or economic payback");
        require(!p.evaluate(mine,reinforce,180000,2400,100000,1200,800,39,true,false,true,false).selected,"paid reinforcement never duplicates while native queue is unobserved");
        Map<String,Object> threat=MineInvestmentHarness.m("x",1010.,"y",1010.,"canAttack",true);
        MineInvestmentHarness.observe(p,mine,181000,MineInvestmentHarness.m("visibleEnemies",Arrays.asList(threat)),MineInvestmentHarness.emptyScout());
        require(!p.evaluate(mine,overclock,181000,2400,100000,1200,800,39,false,false,true,true).selected,"current visible raid overrides supplied known zone for fragile upgrade");
        require(p.evaluate(mine,reinforce,181000,2400,100000,1200,800,39,false,false,true,false).selected,"legal reinforcement remains available during visible raid with protected budget");
        MineInvestmentPolicy gap=new MineInvestmentPolicy();
        MineInvestmentHarness.observe(gap,mine,0,MineInvestmentHarness.emptyEnemies(),MineInvestmentHarness.emptyScout());
        MineInvestmentHarness.observe(gap,mine,180000,MineInvestmentHarness.emptyEnemies(),MineInvestmentHarness.emptyScout());
        require(!gap.evaluate(mine,overclock,180000,2400,100000,1200,800,39,false,false,true,true).selected,"large observation gap cannot fabricate a continuous overclock quiet window");
        Fixture f=new Fixture();f.mineMenu=true;Map<String,Object> observedMine=StrategyContractHarness.unit(91,"extractorT3",200,100);f.own.add(observedMine);f.observe(530000);
        require(f.strategy.act(1200,1)&&f.strategy.pending(91),"native T3 branch enters mature exact-producer purchase ledger");
        require("extractorT3_reinforced".equals(f.contexts.get(0).get("product"))&&number(f.contexts.get(0),"cost",0)==3700,"actual investment uses quoted branch and native cost");
        f.observe(540000);f.strategy.act(1200,1);require(f.contexts.size()==1,"empty queue before visibility cannot release accepted T3 conversion");
        observedMine.put("type","extractorT3_reinforced");f.observe(550000);
        require(!f.strategy.pending(91)&&f.fake.events("mine_upgrade_observed")==1,"actual observed T3 conversion releases its commitment exactly once");
    }
    static void repairFunding()throws Exception{
        Fixture f=new Fixture();f.recovery=1;f.credits(1400);f.observe(530000);f.strategy.act(1200,1);
        require(f.fake.orders.isEmpty()&&f.strategy.capabilityReserve()==1500,"one real selected repair site can fund without an immediate construction receipt");
        f.observe(531000);f.strategy.act(1200,1);require(f.strategy.capabilityReserve()==1500,"same selected repair funding persists across ordinary observations without duplicate reservations");
        f.credits(3000);f.observe(532000);require(f.strategy.act(1200,1)&&f.contexts.stream().anyMatch(c->"repairbay".equals(c.get("product"))),"selected repair spends its own funds when actually affordable instead of double protecting them");
        require(f.strategy.capabilityReserve()==0,"accepted repair transfers funding to native construction commitment");
        Fixture clear=new Fixture();clear.recovery=1;clear.credits(1400);clear.observe(530000);clear.strategy.act(1200,1);clear.recovery=0;clear.observe(531000);
        require(clear.strategy.capabilityReserve()==0,"actual recovery need clear releases selected funding");
        Fixture lostPlan=new Fixture();lostPlan.recovery=1;lostPlan.credits(1400);lostPlan.observe(530000);lostPlan.strategy.act(1200,1);lostPlan.plan=false;lostPlan.observe(531000);lostPlan.strategy.act(1200,1);
        require(lostPlan.strategy.capabilityReserve()==0,"current unavailable repair plan releases its funding rather than blocking production forever");
    }
    public static void main(String[] args)throws Exception{goals();actualCapacity();economicGrowthBoundary();recoveryConstruction();mineBranches();repairFunding();System.out.println("GLOBAL_STRATEGY_ECONOMIC_CONTRACT_OK checks="+checks+" fixtureOnly=true");}
}
