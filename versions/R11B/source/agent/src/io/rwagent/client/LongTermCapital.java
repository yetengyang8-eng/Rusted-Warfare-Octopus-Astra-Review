package io.rwagent.client;

import java.util.*;

/** Actual own cash history, not an enemy-strength claim or fixed wealthy-credit threshold. */
final class LongTermCapital {
    private final Deque<double[]> samples=new ArrayDeque<double[]>();
    private String session;
    private long last=-1;
    private double growth;
    private boolean covered;
    private final Map<Long,double[]> airInvoices=new LinkedHashMap<Long,double[]>();
    private final Map<Long,Long> recentAirFunding=new LinkedHashMap<Long,Long>();
    private final Deque<double[]> airService=new ArrayDeque<double[]>();
    static final long HORIZON_MS=120000; // Existing deep-fog/search planning horizon.
    void observe(String session,long time,double credits){
        if(!Objects.equals(this.session,session)||time<last){samples.clear();airInvoices.clear();recentAirFunding.clear();airService.clear();this.session=session;last=-1;}
        if(time==last)return;
        last=time;samples.addLast(new double[]{time,credits});
        while(samples.size()>1){Iterator<double[]> it=samples.iterator();it.next();if(it.next()[0]>time-HORIZON_MS)break;samples.removeFirst();}
        double[] first=samples.peekFirst();covered=first!=null&&time-first[0]>=HORIZON_MS;
        growth=covered?credits-first[1]:0;
    }
    boolean growing(){return covered&&growth>0;}
    /** Accepted, native-priced construction only. This does not settle any funds or slots. */
    void airStarted(long provider,long time,double nativeCost){
        if(time>=0&&Double.isFinite(nativeCost)&&nativeCost>0&&!airInvoices.containsKey(provider)){
            airInvoices.put(provider,new double[]{time,nativeCost});
            recentAirFunding.put(provider,time);
        }
    }
    /** A later own ready-product match is service evidence; an empty queue/timer is not. */
    void airCompleted(long provider,long time){
        double[] invoice=airInvoices.remove(provider);
        if(invoice!=null&&time>invoice[0])airService.addLast(new double[]{time,time-invoice[0],invoice[1]});
        pruneService(time);
    }
    void airUnconfirmed(long provider){airInvoices.remove(provider);recentAirFunding.remove(provider);}
    /** Independent manufacturers need not all be BUILD in one polling frame. A recently accepted
     * real invoice remains work evidence through ordinary retry/ready-product handover. */
    boolean airFundedRecently(long provider,long time){
        Long at=recentAirFunding.get(provider);return at!=null&&time>=at&&time-at<=HORIZON_MS;
    }
    private void pruneService(long time){
        // Keep the most recent completed service even when a build takes longer than the horizon.
        while(airService.size()>1&&airService.peekFirst()[0]<time-HORIZON_MS)airService.removeFirst();
    }
    double observedAirProviderSpendPerSecond(){
        double cost=0,millis=0;for(double[] sample:airService){cost+=sample[2];millis+=sample[1];}
        return millis>0?cost*1000/millis:0;
    }
    /** Expand a currently funded spatial-duty pipeline to consume observed idle capital.
     * All providers must have recent real accepted air work before another provider is demanded.
     * Unknown native build speed is kept unknown until a ready-product witness exists.
     * Pending workers suppress duplicate growth until their ownership/role is fulfilled.
     */
    int airProviderTarget(int readyProviders,int pendingProviders,int fundedProviders,
            boolean spatialDuty,boolean emergency,int additionalMilitarySlots){
        int ready=Math.max(0,readyProviders),pending=Math.max(0,pendingProviders),committed=ready+pending;
        if(!spatialDuty||emergency||additionalMilitarySlots<=0)return committed;
        if(committed==0)return 1; // One native-priced provider probes actual service, as before.
        if(pending>0||fundedProviders<ready||!growing())return committed;
        double rate=observedAirProviderSpendPerSecond();
        if(!Double.isFinite(rate)||rate<=0)return committed;
        double idleFlow=growth/(HORIZON_MS/1000.0);
        int extra=(int)Math.min(additionalMilitarySlots,Math.ceil(idleFlow/rate));
        return committed+Math.max(0,extra);
    }
    int airStrength(double income,double landConsumption,double nativeJetCost){
        if(!Double.isFinite(nativeJetCost)||nativeJetCost<=0)return 0;
        double capacity=Math.max(0,income-landConsumption)*HORIZON_MS/1000.0;
        return (int)Math.min(Integer.MAX_VALUE,Math.floor(capacity/nativeJetCost));
    }
    Map<String,Object> snapshot(){return StrategyDirector.map("covered",covered,"horizonGameMs",HORIZON_MS,"ownCreditsGrowth",covered?growth:null,
        "source","CURRENT_OWN_CREDITS_SAMPLES","growing",growing(),"wealthThreshold",null,"hiddenEnemyAdvantage", "UNKNOWN",
        "idleCapitalPerGameSecond",covered?growth/(HORIZON_MS/1000.0):null,"observedAirServiceSamples",airService.size(),
        "airProviderNativeSpendPerGameSecond",airService.isEmpty()?null:observedAirProviderSpendPerSecond(),
        "airInvoicesAwaitingReadyWitness",airInvoices.size(),"airThroughputSource","ACCEPTED_NATIVE_INVOICE_TO_LATER_OWN_READY_PRODUCT",
        "airProviderModel","CURRENT_FUNDED_SPATIAL_DUTY_PLUS_OBSERVED_IDLE_CAPITAL_OVER_OBSERVED_PROVIDER_SERVICE");}
}
