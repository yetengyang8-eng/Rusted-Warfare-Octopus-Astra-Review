package io.rwagent.client;
import java.util.*;
import static io.rwagent.client.StrategyDirector.map;
import static io.rwagent.client.G53ContinuingOperationsHarness.*;

public final class R13MarginalProductionHarness {
    static int checks;
    static void require(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("rwagent.g6","true");System.setProperty("rwagent.g5","true");System.setProperty("rwagent.globalStrategy","false");
        System.setProperty("rwagent.allowSmallTanks","true");System.setProperty("rwagent.g51","false");System.setProperty("rwagent.g1Trace","false");
        try(Fixture f=new Fixture()){
            Map<String,Object> state=f.sample(1);StrategyDirector strategy=(StrategyDirector)get(f.battle,"strategy");
            set(strategy,"militaryInvestment",map("preferSpatialInvestment",true));
            call(f.battle,"produce",new Class<?>[]{Map.class},state);
            require(f.posts.isEmpty(),"covered current land duty actually pauses next ordinary queue, despite affordable cash/menu");
            require(!strategy.ordinaryGroundReplacementNeeded(),"organisation target is not the pause authority");
            state=f.sample(2);set(strategy,"militaryInvestment",map("preferSpatialInvestment",false));
            call(f.battle,"produce",new Class<?>[]{Map.class},state);
            require(f.posts.stream().anyMatch(p->p.startsWith("/command/queue?unitId=100")),"new lawful replacement demand resumes the same factory without a batch approval");
            require(f.scheduler.availableTokens()==Integer.MAX_VALUE,"marginal investment does not reintroduce global command quota");
        }
        System.out.println("R13_MARGINAL_PRODUCTION_OK checks="+checks+" fixtureOnly=true");
    }
}
