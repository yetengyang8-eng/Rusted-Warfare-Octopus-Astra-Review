package io.rwagent.client;
import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Actual Strategy worker allocator, synthetic current native site/receipt/own-position facts. */
public final class R10FrontierFundingHarness {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Funding extends G51ProspectIntegrationHarness.Fixture {
        final List<Map<String,Object>> contexts=new ArrayList<Map<String,Object>>();
        Funding()throws Exception{super();}
        public boolean economicArmyPolicyStrategy(){return true;}
        public Map<String,Object> costSourceStrategy(Map<String,Object> p){
            return p==plan?map("costSourceObservationId","q:"+frame,"costSourceRequestPath","/expansion/plan?unitId=5924"):Collections.emptyMap();
        }
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context){contexts.add(new LinkedHashMap<String,Object>(context));return orderStrategy(owner,path);}
        void quote(double cost){plan=map("status","planned","sessionId","session","builderId",5924L,"extractorCost",cost,"extractorX",7410.,"extractorY",410.);}
    }
    static final class RallyFunding extends G51ProspectIntegrationHarness.Fixture {
        boolean intermediate;
        RallyFunding(boolean intermediate)throws Exception{super();this.intermediate=intermediate;}
        public boolean economicArmyPolicyStrategy(){return true;}
        public Map<String,Object> readStrategy(String path,String event){
            Map<String,Object> value=super.readStrategy(path,event);
            if(value!=null&&path.startsWith("/scout/resource-approach")){
                value.put("x",7530.);value.put("y",410.);value.put("waypointX",intermediate?7300.:7530.);value.put("waypointY",410.);
            }
            return value;
        }
        void quote(){plan=map("status","planned","sessionId","session","builderId",5924L,"extractorCost",700.,"extractorX",7410.,"extractorY",410.);}
        void arriveRally()throws Exception{builder.put("x",intermediate?7300.:7530.);builder.put("y",410.);observe(136000,102);}
    }
    public static void main(String[] args)throws Exception{
        Funding f=new Funding();f.start();check(f.director.frontierConstructionReserve()==0,"long travel has no hard price reservation");
        f.credits=300;f.arrive();f.quote(700);f.observe(136000,102);
        check(f.director.frontierConstructionReserve()==700,"later own arrival and native current site select exact price even when not affordable");
        int previous=f.orders.size();f.director.act(700,1);check(f.orders.size()==previous,"unaffordable arrived site does not fabricate build receipt");
        f.credits=700;f.observe(137000,103);f.director.act(f.director.frontierConstructionReserve(),1);
        check(f.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924")),"own mining reserve is available to its worker at exact native price, not double subtracted");
        check(f.director.frontierConstructionReserve()==0,"accepted construction converts soft funding to existing execution commitment, never double reserves");
        check(f.contexts.stream().anyMatch(c->"/expansion/plan?unitId=5924".equals(c.get("costSourceRequestPath"))&&String.valueOf(c.get("costSourceObservationId")).startsWith("q:")),"saved quote keeps real endpoint/observation provenance through copied packet");f.director.close();
        Funding risk=new Funding();risk.start();risk.arrive();risk.quote(850);risk.credits=100;risk.observe(136000,102);
        check(risk.director.frontierConstructionReserve()==850,"price is native 850, not a fixed per-builder 700");
        risk.builder.put("x",9000.);risk.observe(137000,103);check(risk.director.frontierConstructionReserve()==0,"leaving arrived local working cluster removes unaccepted reserve without refund");risk.director.close();
        Funding unknown=new Funding();unknown.start();unknown.arrive();unknown.quote(700);unknown.observe(136000,102);unknown.plan=null;unknown.observe(137000,103);
        check(unknown.director.frontierConstructionReserve()==700,"missing endpoint does not pretend prior arrived quote is invalid");unknown.director.close();
        RallyFunding rally=new RallyFunding(false);rally.credits=100;rally.director.act(0,1);rally.quote();
        check(rally.director.frontierConstructionReserve()==0,"accepted final rally move is not arrival");rally.arriveRally();
        check(rally.director.frontierConstructionReserve()==700,"later arrival at actual final rally 120 from mine center banks that current native site instead of waiting for impossible center proximity");
        rally.resources.add(G51ProspectIntegrationHarness.resource(43,7410,410));
        int rallyOrders=rally.orders.size();rally.director.act(rally.director.frontierConstructionReserve(),1);
        check(rally.orders.size()==rallyOrders,"funded but not affordable worker does not re-prospect a local candidate while saving its exact mine quote");
        rally.observe(136500,103);check(rally.director.frontierConstructionReserve()==700,"waiting construction keeps worker and native quote bank on the next own observation");
        rally.credits=700;rally.observe(137000,104);rally.director.act(rally.director.frontierConstructionReserve(),1);
        check(rally.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924&x=7410.0")),"funded final-rally worker dispatches exact native mine guard");rally.director.close();
        RallyFunding intermediate=new RallyFunding(true);intermediate.credits=100;intermediate.director.act(0,1);intermediate.quote();intermediate.arriveRally();
        check(intermediate.director.frontierConstructionReserve()==700,"after waypoint arrival, a current native local mine 110 away is actionable construction, not a distant static promise");
        intermediate.plan.put("extractorX",9000.);intermediate.observe(137000,103);
        check(intermediate.director.frontierConstructionReserve()==0,"distant native candidate outside arrived working cluster cannot bank a future expansion");intermediate.director.close();
        System.out.println("R10_FRONTIER_FUNDING_OK checks="+checks+" fixtureOnly=true");
    }
}
