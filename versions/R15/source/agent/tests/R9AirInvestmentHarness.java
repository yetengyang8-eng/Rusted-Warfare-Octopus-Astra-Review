package io.rwagent.client;

/** Local policy evidence. These packets are explicit fixtures, not native game acceptance. */
public final class R9AirInvestmentHarness {
    private static int checks;
    private static void check(boolean pass,String why){checks++;if(!pass)throw new AssertionError(why);}
    public static void main(String[] args){
        LongTermCapital c=new LongTermCapital();c.observe("s",0,1000000);c.observe("s",120000,1060000);
        check(c.airProviderTarget(0,0,0,true,false,100)==1,"bootstrap actual throughput with one provider");
        check(c.airProviderTarget(1,0,1,true,false,100)==1,"growing cash does not invent native aircraft build speed");
        c.airStarted(91,120000,2000);c.airCompleted(91,220000);
        check(c.airFundedRecently(91,225000),"completed service remains real funded work through retry and handover");
        check(!c.airFundedRecently(92,225000),"unused new provider has no funded work evidence");
        check(!c.airFundedRecently(91,240001),"stale funding beyond current horizon cannot claim present productive duty");
        check(c.observedAirProviderSpendPerSecond()==20,"exact native invoice over later ready witness establishes service");
        check(c.airProviderTarget(1,0,1,true,false,100)==26,"500 per second idle flow needs 25 further service providers");
        check(c.airProviderTarget(1,1,1,true,false,100)==2,"paid provider pending fulfillment prevents duplicate growth");
        check(c.airProviderTarget(3,0,2,true,false,100)==3,"unfunded available provider work takes precedence over provider growth");
        check(c.airProviderTarget(1,0,1,false,false,100)==1,"cash does not create nonexistent spatial duty");
        check(c.airProviderTarget(1,0,1,true,true,100)==1,"home emergency keeps replacement priority");
        check(c.airProviderTarget(1,0,1,true,false,2)==3,"current military headroom bounds only new commitments");
        c.airStarted(92,220000,2000);c.airUnconfirmed(92);c.airCompleted(92,320000);
        check(!c.airFundedRecently(92,225000),"unknown or timeout invoice drops recent funded work proof");
        check(c.observedAirProviderSpendPerSecond()==20,"unconfirmed production cannot pretend completed service");
        c.observe("s",240000,1060000);check(c.airProviderTarget(1,0,1,true,false,100)==1,"flat current cash stops new idle-capital capacity demand");
        c.observe("other",0,1060000);c.observe("other",120000,1120000);
        check(!c.airFundedRecently(91,120000),"session reset removes previous provider funding");
        check(c.airProviderTarget(1,0,1,true,false,100)==1,"session reset drops old native service evidence");
        c.airStarted(91,120000,2000);c.airStarted(91,120001,1);c.airCompleted(91,220000);
        check(c.observedAirProviderSpendPerSecond()==20,"duplicate invoice cannot alter existing paid service baseline");
        System.out.println("R9_AIR_INVESTMENT_OK checks="+checks+" fixtureOnly=true nativeVictoryProven=false");
    }
}
