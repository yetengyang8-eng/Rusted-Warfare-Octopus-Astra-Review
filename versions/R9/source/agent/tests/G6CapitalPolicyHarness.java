package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Actual allocator paths with explicit own-state/quote fixtures; no simulated arrival or victory. */
public final class G6CapitalPolicyHarness {
    static int checks;
    static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    static final class Host implements StrategyDirector.Host {
        final StrategyContractHarness.Fake fake=new StrategyContractHarness.Fake();
        final StrategyDirector policy=new StrategyDirector(this,fake.gate);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),ground=new ArrayList<Map<String,Object>>(),contexts=new ArrayList<Map<String,Object>>();
        final Map<String,Object> state,enemies,vision;
        boolean localMine,wrongQuote;double credits=100000,income=100,consumption=60;long time;
        Host()throws Exception{
            fake.gate.configureIndependentActors();
            own.add(StrategyContractHarness.unit(1,"commandCenter",100,100));own.add(StrategyContractHarness.unit(2,"builder",100,100));
            own.add(StrategyContractHarness.unit(3,"builder",100,100));own.add(StrategyContractHarness.unit(90,"landFactory",200,100));
            own.add(StrategyContractHarness.unit(91,"combatEngineer",200,100));
            own.add(StrategyContractHarness.unit(92,"amphibiousJet",180,100));
            for(int i=100;i<190;i++){Map<String,Object> u=StrategyContractHarness.unit(i,"heavyTank",200,100);own.add(u);ground.add(u);}
            state=map("sessionId","session","player",map("credits",credits),"ownUnits",own,"map",map("tilesWide",200,"tilesHigh",200,"tileWidth",20,"tileHeight",20));
            enemies=map("sessionId","session","visibleEnemies",Collections.emptyList(),"enemyIntel",Collections.emptyList());
            vision=map("resources",Collections.emptyList(),"rememberedThreats",Collections.emptyList(),"mapMemoryNeverSeenTiles",30000,"mapMemoryCurrentVisibleTiles",10000);
            policy.enable(map("strategyContractVersion",1),512);policy.noteOrdinaryNativePrice(800);observe(300000);
        }
        void observe(long now)throws Exception{time=now;state.put("frame",now);state.put("gameTimeMs",now);BattleClient.obj(state.get("player")).put("credits",credits);enemies.put("gameTimeMs",now);fake.stamp(state);policy.observe(state,enemies,vision,ground,0,2400000,income,consumption,0,false);}
        public boolean endgameStrategy(){return true;}
        public boolean parallelStrategy(){return true;}
        public boolean economicArmyPolicyStrategy(){return true;}
        public String landRouteDemandStrategy(){return "NONE";}
        public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/economy/construction-plan")){
                long actor=Long.parseLong(path.split("unitId=")[1].split("&")[0]);String type=path.split("type=")[1].split("&")[0];
                if(!"amphibiousJet".equals(type)&&!"fabricatorT1".equals(type))return null;
                return map("sessionId","session","unitId",wrongQuote?999L:actor,"type",type,"actionId","native-"+type,"cost","fabricatorT1".equals(type)?2200.:2000.,"affordable",true,"x",140.,"y",100.);
            }
            if(path.startsWith("/expansion/plan"))return localMine?map("extractorCost",700.,"extractorX",180.,"extractorY",100.):null;
            if(path.equals("/economy/investments"))return map("sessionId","session","units",Collections.emptyList());
            return fake.readStrategy(path,event);
        }
        public Map<String,Object> costSourceStrategy(Map<String,Object> p){return map("costSourceObservationId","session:quote:"+time,"costSourceRequestPath",p.containsKey("factories")?"/combat/production":p.containsKey("units")?"/economy/investments":"/economy/construction-plan");}
        public Map<String,Object> orderStrategy(String owner,String path)throws Exception{return fake.orderStrategy(owner,path);}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context)throws Exception{contexts.add(new LinkedHashMap<String,Object>(context));return fake.orderStrategy(owner,path);}
        public void emitStrategy(String event,Map<String,Object> data){fake.emitStrategy(event,data);}
        public void spendStrategy(String category,long cost,String product,long actor){}
        public void strategicAttack(Map<String,Object> receipt){}
        long purchases(String product){return contexts.stream().filter(c->product.equals(c.get("product"))).count();}
    }
    static void capital()throws Exception{
        Host h=new Host();
        check(h.fake.gate.owns(AirGeneralController.OWNER,92),"idle specialist transfers to independent air command owner");
        h.policy.act(0,1);check(h.purchases("amphibiousJet")==1,"real current rear-provider invoice creates air construction commitment");
        check(h.purchases("fabricatorT1")==0,"high cash without long growth and funded air does not authorize capital");
        check(h.contexts.stream().filter(c->"amphibiousJet".equals(c.get("product"))).allMatch(c->"/economy/construction-plan".equals(c.get("costSourceRequestPath"))),"air invoice keeps actual quote provenance");
        Map<String,Object> jet=StrategyContractHarness.unit(93,"amphibiousJet",140,100);h.own.add(jet);h.credits+=10000;h.observe(420000);
        check(h.fake.gate.owns(AirGeneralController.OWNER,93),"later ready paid product transfers without stealing old capability target");
        check(h.fake.events("strategy_construction_observed")==1,"later ready native-style packet settles one paid construction");
        h.policy.act(0,1);
        check(h.purchases("combatEngineer")==1&&h.purchases("fabricatorT1")==0,"R9 observed idle flow expands real air throughput before capital, not just one existing provider");
        check(h.contexts.stream().filter(c->"combatEngineer".equals(c.get("product"))).allMatch(c->"ENDGAME_AIR_PROVIDER".equals(c.get("purchasePurpose"))
                &&"/combat/production".equals(c.get("costSourceRequestPath"))),"additional provider uses native invoice and preserved rear purpose");
        h.own.add(StrategyContractHarness.unit(94,"combatEngineer",220,100));h.observe(421000);
        check(Boolean.TRUE.equals(G53ContinuingOperationsHarness.get(worker(h,94),"endgameProvider")),"later own worker fulfills additional manufacturing role");
        // Explicit exhausted military headroom fixture: capital may absorb money when the
        // real new military pipeline cannot expand. This is not a natural army cap claim.
        G53ContinuingOperationsHarness.set(h.policy,"hardCap",0);h.policy.act(0,1);
        check(h.purchases("fabricatorT1")>=1,"long capital with fulfilled providers and no additional military headroom can build a home fabricator");
        check(h.contexts.stream().filter(c->"fabricatorT1".equals(c.get("product"))).allMatch(c->number(c,"cost",0)==2200&&"/economy/construction-plan".equals(c.get("costSourceRequestPath"))),"fabricator uses exact native quote, not a cash threshold or state price");
        G53ContinuingOperationsHarness.set(h.policy,"hardCap",512);h.observe(426000);h.policy.act(0,1);
        check(h.purchases("amphibiousJet")>=2,"owned air inventory does not turn funded additional production capacity into a stock stop");
        check(h.fake.orders.stream().anyMatch(p->p.startsWith("/command/construct?unitId=94&")),"fulfilled additional provider actually submits its own air construction receipt");
        h.policy.close();check(!h.fake.gate.reserved(92)&&!h.fake.gate.reserved(93),"shutdown releases transferred leases");
    }
    static Host funded()throws Exception{
        Host h=new Host();h.policy.act(0,1);h.own.add(StrategyContractHarness.unit(93,"amphibiousJet",140,100));h.credits+=10000;h.observe(420000);return h;
    }
    static void boundaries()throws Exception{
        Host mine=funded();mine.localMine=true;mine.policy.act(0,1);check(mine.purchases("fabricatorT1")==0,"current affordable local expansion outranks fabricator");mine.policy.close();
        Host debt=new Host();debt.credits=2500;debt.observe(301000);debt.policy.act(0,1);check(debt.purchases("amphibiousJet")==0,"air quote cannot consume last current land production runway");debt.policy.close();
        Host wrong=new Host();wrong.wrongQuote=true;wrong.policy.act(0,1);check(wrong.purchases("amphibiousJet")==0,"another actor's quote cannot authorize air construction");wrong.policy.close();
        Host committed=new Host();Map<?,?> workers=(Map<?,?>)G53ContinuingOperationsHarness.get(committed.policy,"workers");
        Object provider=workers.get(91L);G53ContinuingOperationsHarness.set(provider,"target",999L);
        G53ContinuingOperationsHarness.call(committed.policy,"actEndgame",new Class<?>[]{long.class},1L);
        check(committed.purchases("amphibiousJet")==0,"an idle but committed Capability provider is not stolen by endgame manufacturing");committed.policy.close();
        Host slow=new Host();slow.income=61;slow.consumption=60;slow.observe(301000);slow.policy.act(0,1);
        check(slow.purchases("amphibiousJet")==1,"less than one aircraft per planning horizon cannot stop an actually funded native job");slow.policy.close();
        Host noisy=new Host();noisy.income=50;noisy.consumption=150;noisy.observe(301000);noisy.policy.act(0,1);
        check(noisy.purchases("amphibiousJet")==1,"recent spending rate does not revoke a healthy supported spatial role with real free funds");noisy.policy.close();
        Host remote=funded();G53ContinuingOperationsHarness.set(remote.policy,"hardCap",0);
        for(Map<String,Object> a:remote.own)if("builder".equals(a.get("type")))a.put("x",2000.);remote.observe(421000);remote.policy.act(0,1);
        check(remote.purchases("fabricatorT1")==0&&remote.fake.orders.stream().anyMatch(p->p.startsWith("/command/move?unitId=3")),"one idle distant constructor rallies home before requesting a home site");
        remote.policy.close();
    }
    static Object worker(Host h,long id)throws Exception{return ((Map<?,?>)G53ContinuingOperationsHarness.get(h.policy,"workers")).get(id);}
    static void providerPurpose()throws Exception{
        Host h=new Host();h.own.add(StrategyContractHarness.unit(94,"combatEngineer",200,100));h.fake.specialistApproach="UNKNOWN";
        List<Map<String,Object>> contacts=new ArrayList<Map<String,Object>>();for(long id=501;id<=503;id++)contacts.add(map("id",id,"type","tank","x",2000.,"y",2000.,"canAttack",false,"building",false));
        h.enemies.put("visibleEnemies",contacts);h.observe(301000);
        check(h.policy.hasCrossDomainNeed(503),"actual current blocked ground assessment creates another available Capability need");
        G53ContinuingOperationsHarness.set(worker(h,91),"target",501L);G53ContinuingOperationsHarness.set(worker(h,94),"target",502L);
        h.policy.act(0,1);
        check(h.purchases("combatEngineer")==1,"endgame purchases a rear provider while existing engineers retain their different Capability commitments");
        Map<String,Object> invoice=h.contexts.stream().filter(c->"combatEngineer".equals(c.get("product"))).findFirst().get();
        check("ENDGAME_AIR_PROVIDER".equals(invoice.get("purchasePurpose"))&&number(invoice,"needId",999)==-1,"endgame invoice does not inherit the unrelated selected Need");
        Object purchase=((Map<?,?>)G53ContinuingOperationsHarness.get(h.policy,"purchases")).get(90L);
        check(Boolean.TRUE.equals(G53ContinuingOperationsHarness.get(purchase,"endgameProvider"))&&((Number)G53ContinuingOperationsHarness.get(purchase,"selectedNeed")).longValue()==-1,"purchase preserves explicit rear-production purpose without a Need binding");
        check(G53ContinuingOperationsHarness.get(h.policy,"capabilityFunding")==null,"endgame allocation cannot start funding for the unrelated Capability need");
        Map<String,Object> factory=BattleClient.find(h.state,90);factory.put("productionQueue",1);h.observe(302000);h.policy.act(0,1);
        check(h.purchases("combatEngineer")==1,"a pending rear-provider purchase prevents duplicate endgame procurement");
        factory.put("productionQueue",0);h.observe(303000);h.policy.act(0,1);
        check(h.policy.pending(90)&&h.purchases("combatEngineer")==1,"empty queue alone does not forget rear-provider purpose before an own ready worker appears");
        long priorSupportAircraft=h.purchases("amphibiousJet");
        h.own.add(StrategyContractHarness.unit(95,"combatEngineer",200,100));h.credits=2500;h.observe(304000);
        Object rear=worker(h,95);String owner=(String)G53ContinuingOperationsHarness.get(rear,"owner");
        Map<Long,Long> generation=h.fake.gate.snapshotGenerations(owner,Arrays.asList(95L));
        check(Boolean.TRUE.equals(G53ContinuingOperationsHarness.get(rear,"endgameProvider"))&&((Number)G53ContinuingOperationsHarness.get(rear,"target")).longValue()==-1,"ready fulfillment retains rear role and remains unbound to the other Need");
        h.policy.act(0,1);h.observe(305000);h.policy.act(0,1);
        check(h.purchases("amphibiousJet")==priorSupportAircraft&&"IDLE".equals(G53ContinuingOperationsHarness.get(rear,"job"))&&((Number)G53ContinuingOperationsHarness.get(rear,"target")).longValue()==-1,"temporary shortage of funds keeps the rear provider idle instead of automatically binding it to Capability");
        check(h.purchases("combatEngineer")==1&&h.fake.gate.owns(owner,95)&&generation.equals(h.fake.gate.snapshotGenerations(owner,Arrays.asList(95L))),"retained actual role prevents replacement purchases and preserves owner generation");
        check(((Number)G53ContinuingOperationsHarness.get(worker(h,91),"target")).longValue()==501&&((Number)G53ContinuingOperationsHarness.get(worker(h,94),"target")).longValue()==502,"rear allocation does not steal already committed engineers");
        h.credits=100000;h.observe(306000);h.policy.act(0,1);
        check(h.purchases("amphibiousJet")==priorSupportAircraft+1&&Boolean.TRUE.equals(G53ContinuingOperationsHarness.get(rear,"paidConstruction"))&&Boolean.TRUE.equals(G53ContinuingOperationsHarness.get(rear,"endgameConstruction")),"later affordable current native quote closes procurement-to-ready-to-air-manufacture flow");
        check(h.contexts.stream().filter(c->"amphibiousJet".equals(c.get("product"))).allMatch(c->number(c,"cost",0)==2000&&"/economy/construction-plan".equals(c.get("costSourceRequestPath"))),"rear-role manufacture still requires exact actor quote and original cost provenance");
        h.policy.close();
    }
    public static void main(String[] args)throws Exception{capital();boundaries();providerPurpose();System.out.println("G6_CAPITAL_POLICY_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");}
}
