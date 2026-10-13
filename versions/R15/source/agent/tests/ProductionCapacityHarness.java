package io.rwagent.client;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Policy/real delayed-build execution contracts. Reflection supplies private state, not a game. */
public final class ProductionCapacityHarness {
    static int checks;
    static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
    static void set(Object value,String name,Object data)throws Exception{field(value.getClass(),name).set(value,data);}
    static Object get(Object value,String name)throws Exception{return field(value.getClass(),name).get(value);}
    static ProductionCapacity.Bottleneck classify(int army,int target,int hard,String demand,double credits,double reserve,boolean recovery,boolean committed,boolean sustained,double busy){
        return ProductionCapacity.classify(army,target,hard,demand,1,false,true,credits,800,reserve,recovery,committed,sustained,busy,80);
    }
    static void diagnoses(){
        require(classify(96,96,128,"KNOWN",100000,0,false,false,true,0)==ProductionCapacity.Bottleneck.ARMY_CAPACITY_LIMIT,"strategy cap with idle producer is army limit");
        require(classify(82,104,128,"KNOWN",100000,0,false,false,true,100)==ProductionCapacity.Bottleneck.PRODUCER_THROUGHPUT_LIMIT,"free military slots and sustained busy producer are throughput limit");
        require(classify(128,128,128,"KNOWN",100000,0,false,false,true,100)==ProductionCapacity.Bottleneck.HARD_SAFETY_CAP,"busy hard cap cannot expand");
        require(classify(20,40,128,"KNOWN",799,0,false,false,true,100)==ProductionCapacity.Bottleneck.MONEY_LIMITED,"native quote not covered");
        require(classify(40,40,128,"KNOWN",1000,500,false,false,true,0)==ProductionCapacity.Bottleneck.RESERVE_PROTECTED,"reserve cannot fund military increase");
        require(classify(40,40,128,"KNOWN",100000,0,true,false,true,0)==ProductionCapacity.Bottleneck.RESERVE_PROTECTED,"recovery cannot fund military increase");
        require(classify(20,40,128,"NONE",100000,0,false,false,true,100)==ProductionCapacity.Bottleneck.NO_USEFUL_DEMAND,"cash without demand cannot expand");
        require(classify(20,40,128,"UNKNOWN",100000,0,false,false,true,100)==ProductionCapacity.Bottleneck.UNKNOWN,"unknown remains unknown");
        require(classify(20,40,128,"KNOWN",100000,0,false,true,true,100)==ProductionCapacity.Bottleneck.CAPACITY_EXPANSION_COMMITTED,"one commitment at a time");
        require(classify(20,40,128,"KNOWN",100000,0,false,false,false,100)==ProductionCapacity.Bottleneck.UNKNOWN,"one busy observation is insufficient");
    }
    static ProductionCapacity.Sample sample(long time,long frame,String session,boolean useful,boolean safe,Long factory,Boolean busy){
        Map<Long,Boolean> factories=new LinkedHashMap<Long,Boolean>();if(factory!=null)factories.put(factory,busy);
        return new ProductionCapacity.Sample(map("gameTimeMs",time,"frame",frame,"sessionId",session,"player",map("teamId",0)),time,true,useful,safe,factories);
    }
    static void sparseIntervals()throws Exception{
        ProductionCapacity.Window window=new ProductionCapacity.Window(150000);
        for(int i=0;i<=12;i++)window.add(sample(i*13000L,i,"s",true,true,5L,true));
        double[] estimate=window.metrics(156000);
        require(estimate[0]==150000&&estimate[1]==150000&&estimate[2]==150000&&estimate[3]==150000,
            "thirteen-second legitimate observations retain entire clipped window");
        require(estimate[4]==150000&&estimate[5]==150000,"same busy producer endpoints supply conservative factory time");
        require(window.samples.get(0).time==0,"cutoff predecessor retained despite sparse cadence");
        window.add(sample(169000,13,"s",false,false,5L,false));estimate=window.metrics(169000);
        require(estimate[2]==137000&&estimate[3]==137000&&estimate[5]==137000,
            "observed route/budget/queue transition cannot be credited from previous positive endpoint");
        window=new ProductionCapacity.Window(150000);window.add(sample(0,0,"s",true,true,5L,true));
        window.add(sample(13000,1,"s",true,true,6L,true));estimate=window.metrics(13000);
        require(estimate[4]==26000&&estimate[5]==0,"busy count equality cannot bridge different factory identities");
        window.add(sample(143000,2,"s",true,true,6L,true));estimate=window.metrics(143000);
        require(estimate[0]==13000&&estimate[6]==130000,"long absence is excluded rather than interpolated");
        window=new ProductionCapacity.Window(150000);window.add(sample(0,0,"s",true,true,5L,true));
        window.add(sample(150000,1,"s",true,true,5L,true));
        require(window.metrics(150000)[0]==0,"two isolated endpoint reads cannot warm an entire window");
        require(window.add(sample(160000,2,"new",true,true,5L,true)),"session switch starts new capacity history");
        require(window.metrics(160000)[0]==0,"no prior-session integration");
        window.add(sample(173000,3,"new",true,true,5L,true));
        require(window.add(sample(170000,4,"new",true,true,5L,true)),"game-time rollback resets history");
        require(window.add(sample(183000,1,"new",true,true,5L,true)),"frame rollback resets history");
        Map<String,Object> state=map("gameTimeMs",196000L,"frame",2L,"sessionId","new","player",map("teamId",1));
        require(window.add(new ProductionCapacity.Sample(state,196000,true,true,true,null)),"player switch resets history");
        window=new ProductionCapacity.Window(150000);window.add(sample(0,0,"s",true,true,5L,true));
        window.add(new ProductionCapacity.Sample(map("frame",1,"sessionId","s","player",map("teamId",0)),13000,true,true,true,null));
        window.add(sample(26000,2,"s",true,true,5L,true));
        require(window.metrics(26000)[0]==0,"missing native source time breaks both adjoining intervals");
        window=new ProductionCapacity.Window(150000);window.add(sample(0,0,"s",true,true,5L,true));
        window.add(sample(13000,0,"s",true,true,5L,true));
        require(window.metrics(13000)[0]==0,"nonadvancing source frame supplies no interval");
        BattleClient client=new BattleClient();set(client,"time",13000L);
        Method current=BattleClient.class.getDeclaredMethod("capacityMenuCurrent",Map.class,Map.class);current.setAccessible(true);
        state=map("gameTimeMs",13000L,"frame",2L,"sessionId","s","player",map("teamId",0));
        require((Boolean)current.invoke(client,state,map("sessionId","s")),"legacy native menu has explicit unknown time but current legal read");
        require(!(Boolean)current.invoke(client,state,map("sessionId","s","gameTimeMs",10000)),"stale native menu cannot establish capacity demand");
        require(!(Boolean)current.invoke(client,state,map("sessionId","s","frame",1)),"stale native menu frame cannot establish capacity demand");
        require(!(Boolean)current.invoke(client,state,map("sessionId","other")),"wrong-session menu cannot establish capacity demand");
        require(!(Boolean)current.invoke(client,state,map("sessionId","s","playerKey","team:1")),"wrong-player menu cannot establish capacity demand");
        require(!(Boolean)current.invoke(client,state,map("sessionId","s","gameTimeMs",200000)),"future menu beyond interval budget remains unproven");
    }
    static void incomeFundedTech(){
        Map<String,Object> upgrade=map("type","upgrade","actionId","native-upgrade","cost",2000,"affordable",true);
        require(ProductionCapacity.techInvestment(upgrade,6,6,2500,0,350,63,20,false).selected,
            "six healthy ordinary actors can invest via own force and native budget before ten-unit fence");
        require(!ProductionCapacity.techInvestment(upgrade,12,5,100000,0,350,63,20,false).selected,
            "large cash does not replace healthy force evidence");
        require(!ProductionCapacity.techInvestment(upgrade,6,6,2500,300,350,63,20,false).selected,
            "upgrade protects active investment and one native replacement");
        require(!ProductionCapacity.techInvestment(upgrade,6,6,100000,0,350,63,20,true).selected,
            "recovery takes priority over paid tech");
        upgrade.put("affordable",false);
        ProductionCapacity.TechInvestment bounded=ProductionCapacity.techInvestment(upgrade,6,6,2000,0,350,63,20,false);
        require(!bounded.selected&&bounded.bank,"positive observed income can support bounded native-tech savings");
        require(!ProductionCapacity.techInvestment(upgrade,6,6,2000,0,350,20,63,false).bank,
            "no banking while consumption outruns income");
        require(!ProductionCapacity.techInvestment(upgrade,6,6,100,0,350,40,39,false).bank,
            "long unfunded tech wait must keep production moving");
        require(!ProductionCapacity.techInvestment(upgrade,6,6,100000,0,Double.NaN,63,20,false).selected,
            "unknown native replacement quote cannot release funds");
        require(!ProductionCapacity.techInvestment(null,6,6,100000,0,350,63,20,false).selected,
            "cash cannot invent native upgrade action");
    }
    static void militaryFloor()throws Exception{
        BattleClient host=new BattleClient();set(host,"log",new BufferedWriter(new StringWriter()));
        CommandArbiter execution=(CommandArbiter)get(host,"execution");
        execution.observe(new CommandArbiter.Stamp("capacity-fixture","0",1,200000),Collections.<Long>emptyList());
        StrategyDirector strategy=(StrategyDirector)get(host,"strategy");strategy.enable(map("strategyContractVersion",1),128);
        set(strategy,"armyTarget",96);set(strategy,"activeTarget",86);
        require(strategy.increaseMilitaryCapacity(96,8,map("gameTimeMs",200000)),"96 policy target may grow while hard cap128 has space");
        require(strategy.armyTarget()==104&&(Integer)get(strategy,"militaryCapacityFloor")==104,"capacity floor is retained beyond old96 formula");
        require(!strategy.increaseMilitaryCapacity(104,0,map()),"no zero-slot increase");
        set(strategy,"armyTarget",128);require(!strategy.increaseMilitaryCapacity(128,8,map()),"hard safety cap cannot grow");
    }
    static void delayedReserve(String reserve)throws Exception{
        BattleClient client=new BattleClient();StringWriter output=new StringWriter();set(client,"log",new BufferedWriter(output));
        Map<String,Object> builder=map("id",4L,"type","builder","hp",1000,"dead",false,"x",100,"y",100,"buildProgress",1);
        Map<String,Object> state=map("sessionId","s","gameTimeMs",10000,"ownUnits",Arrays.asList(builder),"player",map("credits",1000));
        Class<?> jobClass=Class.forName("io.rwagent.client.BattleClient$BuildJob");
        Constructor<?> constructor=jobClass.getDeclaredConstructor(String.class,double.class,double.class,long.class,long.class,String.class);constructor.setAccessible(true);
        Object job=constructor.newInstance("landFactory",100d,100d,700L,8000L,"native plan");set(client,"buildJob",job);set(client,"time",10000L);
        if(!"capability".equals(reserve))set(client,reserve+"Reserve",500L);
        else{
            StrategyDirector strategy=(StrategyDirector)get(client,"strategy");
            Class<?> funding=Class.forName("io.rwagent.client.StrategyDirector$CapabilityFunding");
            Constructor<?> fc=funding.getDeclaredConstructor(long.class,long.class,String.class,double.class,long.class);fc.setAccessible(true);
            set(strategy,"capabilityFunding",fc.newInstance(74L,5L,"engineer",500d,8000L));
        }
        Method advance=BattleClient.class.getDeclaredMethod("advanceBuildJob",Map.class);advance.setAccessible(true);advance.invoke(client,state);
        ((BufferedWriter)get(client,"log")).flush();
        require(output.toString().contains("RESERVE_PROTECTED"),reserve+" reserve is rechecked after plan and before delayed native order");
        require(!(Boolean)get(job,"orderAccepted"),reserve+" hold cannot accept factory order");
    }
    static void paidGap()throws Exception{
        BattleClient client=new BattleClient();BattleClient.Pending pending=new BattleClient.Pending();pending.type="heavyTank";pending.started=1000;pending.active=true;
        Map<Long,BattleClient.Pending> jobs=(Map<Long,BattleClient.Pending>)get(client,"pending");jobs.put(5L,pending);
        Map<String,Object> factory=map("id",5L,"type","landFactory","hp",1000,"dead",false,"productionQueue",0);
        Map<String,Object> state=map("ownUnits",Arrays.asList(factory));
        Method count=BattleClient.class.getDeclaredMethod("ordinaryUnobservedSlots",Map.class);count.setAccessible(true);
        require((Integer)count.invoke(client,state)==1,"ordinary paid empty-queue gap holds a safety slot");
        pending.type="heavyArtillery";require((Integer)count.invoke(client,state)==0,"artillery stays exclusively on its existing commitment ledger");
        pending.type="upgrade";require((Integer)count.invoke(client,state)==0,"upgrade is not an extra ordinary paid slot");
    }
    static void delayedHardCap()throws Exception{
        BattleClient client=new BattleClient();StringWriter output=new StringWriter();set(client,"log",new BufferedWriter(output));
        StrategyDirector strategy=(StrategyDirector)get(client,"strategy");strategy.enable(map("strategyContractVersion",1),128);
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();
        own.add(map("id",4L,"type","builder","hp",1000,"dead",false,"x",100,"y",100,"buildProgress",1));
        for(long i=10;i<138;i++)own.add(map("id",i,"type","heavyTank","hp",1000,"dead",false,"mobile",true,"canAttack",true));
        Map<String,Object> state=map("sessionId","s","ownUnits",own,"player",map("credits",100000));set(strategy,"state",state);set(client,"mobileUnitHardCap",128);
        Class<?> jobClass=Class.forName("io.rwagent.client.BattleClient$BuildJob");Constructor<?> constructor=jobClass.getDeclaredConstructor(String.class,double.class,double.class,long.class,long.class,String.class);constructor.setAccessible(true);
        Object job=constructor.newInstance("landFactory",100d,100d,700L,8000L,"native plan");set(job,"capacityExpansion",true);set(client,"buildJob",job);set(client,"time",10000L);
        Method advance=BattleClient.class.getDeclaredMethod("advanceBuildJob",Map.class);advance.setAccessible(true);advance.invoke(client,state);((BufferedWriter)get(client,"log")).flush();
        require(output.toString().contains("HARD_SAFETY_CAP"),"unpaid expansion is held when hard cap becomes full after planning");
        require(!(Boolean)get(job,"orderAccepted"),"cash cannot execute delayed expansion at hard cap");
        while(own.size()>2)own.remove(own.size()-1);
        set(client,"capacityEvidence",map("demand","UNKNOWN"));advance.invoke(client,state);((BufferedWriter)get(client,"log")).flush();
        require(output.toString().contains("CURRENT_LEGAL_DEMAND_UNPROVEN"),"planned unpaid factory waits when fresh demand becomes unknown");
        set(client,"capacityEvidence",map("demand","KNOWN","recovery",true));advance.invoke(client,state);((BufferedWriter)get(client,"log")).flush();
        require(output.toString().contains("RECOVERY_PROTECTED"),"planned unpaid factory waits during dangerous recovery");
    }
    public static void main(String[] args)throws Exception{
        // This suite proves the accepted legacy military-floor contract. Economic inventory
        // goals, same-tier mine conversion and paid-slot protection have dedicated enabled tests.
        System.setProperty("rwagent.g5","false");
        diagnoses();sparseIntervals();incomeFundedTech();militaryFloor();delayedReserve("builder");delayedReserve("investment");delayedReserve("capability");paidGap();delayedHardCap();
        System.out.println("PRODUCTION_CAPACITY_CONTRACT_OK checks="+checks+" fixtureOnly=true");
    }
}
