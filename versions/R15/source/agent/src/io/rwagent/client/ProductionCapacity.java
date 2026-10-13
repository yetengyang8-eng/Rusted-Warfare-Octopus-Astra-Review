package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Small diagnosis shared by the existing military-target and factory-target controllers.
 * Persistent income/demand evidence and utilisation are supplied by BattleClient's windows.
 * UNKNOWN idle time is not evidence of an operational or producer bottleneck.
 */
final class ProductionCapacity {
    /** Same-source endpoint estimates, never continuous native execution evidence. The maximum
     * bridged interval is the existing window's 20% unobserved-time allowance. Thus normal slow
     * loops can supply a window, while a single long absence cannot manufacture a warm one. */
    static final class Sample {
        final long time,frame;final String source;final boolean valid;
        final boolean surplus,demand,safe;final Map<Long,Boolean> factories;
        Sample(Map<String,Object> state,long detected,boolean surplus,boolean demand,boolean safe,Map<Long,Boolean> factories){
            Long t=GameClock.number(state.get("gameTimeMs")),f=GameClock.number(state.get("frame"));
            String session=GameClock.string(state.get("sessionId"));Object player=state.get("player");
            Object who=player instanceof Map?((Map<?,?>)player).get("teamId"):null;
            source=session==null?null:session+":"+(who==null?"local-player":String.valueOf(who));
            time=t==null?detected:t;frame=f==null?-1:f;
            valid=t!=null&&t==detected&&f!=null&&f>=0&&session!=null&&player instanceof Map;
            this.surplus=surplus;this.demand=demand;this.safe=safe;
            this.factories=factories==null?Collections.<Long,Boolean>emptyMap():new LinkedHashMap<Long,Boolean>(factories);
        }
    }
    static final class Window {
        final long windowMs,maxIntervalMs;final List<Sample> samples=new ArrayList<Sample>();
        Window(long windowMs){this.windowMs=windowMs;maxIntervalMs=windowMs/5;}
        boolean add(Sample sample){
            boolean reset=false;
            if(!samples.isEmpty()){
                Sample previous=samples.get(samples.size()-1);
                if(previous.source!=null&&sample.source!=null&&!previous.source.equals(sample.source)
                        ||sample.time<previous.time||previous.frame>=0&&sample.frame>=0&&sample.frame<previous.frame){samples.clear();reset=true;}
                else if(sample.time==previous.time){
                    // A repeated state cannot refresh evidence. An invalid read breaks continuity.
                    if(!sample.valid)samples.set(samples.size()-1,sample);
                    return reset;
                }
            }
            samples.add(sample);long cutoff=sample.time-windowMs;
            // Keep the predecessor straddling the cutoff, independent of the loop cadence.
            while(samples.size()>1&&samples.get(1).time<=cutoff)samples.remove(0);
            return reset;
        }
        void clear(){samples.clear();}
        double[] metrics(long now){
            double covered=0,positive=0,useful=0,safe=0,total=0,busy=0,excluded=0,maxGap=0;
            for(int i=1;i<samples.size();i++){
                Sample a=samples.get(i-1),b=samples.get(i);long gap=b.time-a.time;
                long dt=Math.min(now,b.time)-Math.max(now-windowMs,a.time);if(dt<=0)continue;
                maxGap=Math.max(maxGap,gap);
                if(!a.valid||!b.valid||!Objects.equals(a.source,b.source)||gap<=0||gap>maxIntervalMs||b.frame<=a.frame){excluded+=dt;continue;}
                covered+=dt;
                if(a.surplus&&b.surplus)positive+=dt;if(a.demand&&b.demand)useful+=dt;if(a.safe&&b.safe)safe+=dt;
                // Changing ready counts or replacing a factory is not a productive interval for
                // the new identity. Include endpoint union as denominator, credit busy intersection.
                Set<Long> ids=new HashSet<Long>(a.factories.keySet());ids.addAll(b.factories.keySet());
                total+=ids.size()*dt;
                for(Long id:ids)if(Boolean.TRUE.equals(a.factories.get(id))&&Boolean.TRUE.equals(b.factories.get(id)))busy+=dt;
            }
            return new double[]{covered,positive,useful,safe,total,busy,excluded,maxGap};
        }
    }
    /** Observed productive factory-time estimates conversion capacity, not native build duration.
     * Organization quotas and idle-time policy stops cannot lower this estimate and justify blind builds. */
    static int incomeSupportedProducers(double income,double consumption,double factoryMs,double utilization,long windowMs){
        double busyMs=factoryMs*utilization/100.0;
        if(!Double.isFinite(income)||income<=0||!Double.isFinite(consumption)||consumption<=0
                ||!Double.isFinite(busyMs)||busyMs<=0||windowMs<=0)return 0;
        double observedSpend=consumption*windowMs/1000.0;
        double perProducerPerSecond=observedSpend*1000.0/busyMs;
        return (int)Math.min(Integer.MAX_VALUE,Math.ceil(income/perProducerPerSecond));
    }
    /** Tech conversion uses the current own force and the price of its legal replacement route.
     * A productive army can fund a native upgrade before an arbitrary ten-unit savings fence.
     * The quote authorizes only the upgrade; it never proves a future product or throughput. */
    static final class TechInvestment {
        final boolean selected,bank;final String reason;
        final Map<String,Object> evidence;
        TechInvestment(boolean selected,boolean bank,String reason,Map<String,Object> evidence){
            this.selected=selected;this.bank=bank;this.reason=reason;this.evidence=evidence;
            evidence.put("selected",selected);evidence.put("bank",bank);evidence.put("reason",reason);
        }
    }
    static TechInvestment techInvestment(Map<String,Object> upgrade,int armed,int healthy,double credits,
            double reserved,double replacementCost,double income,double consumption,boolean recovery){
        return techInvestment(upgrade,armed,healthy,credits,reserved,replacementCost,income,consumption,recovery,false);
    }
    static TechInvestment techInvestment(Map<String,Object> upgrade,int armed,int healthy,double credits,
            double reserved,double replacementCost,double income,double consumption,boolean recovery,boolean independentOperations){
        double cost=number(upgrade,"cost",Double.NaN);
        double required=cost+reserved+replacementCost;
        double surplus=income-consumption;
        double fundingSeconds=Double.isFinite(required)&&surplus>0?Math.max(0,required-credits)/surplus:Double.NaN;
        Map<String,Object> evidence=map("nativeUpgradeCost",Double.isFinite(cost)?cost:null,
            "ordinaryReplacementNativeCost",Double.isFinite(replacementCost)?replacementCost:null,
            "armed",armed,"healthyArmed",healthy,"credits",credits,"allReserved",reserved,
            "incomeEstimate",income,"productionConsumptionPerGameSecond",consumption,
            "protectedRequiredCredits",Double.isFinite(required)?required:null,
            "fundingEstimateGameSeconds",Double.isFinite(fundingSeconds)?fundingSeconds:null,
            "quoteSource","LEGAL_NATIVE_MENU","futureProductStatus","UNKNOWN",
            "fundingModel","ESTIMATED_INCOME_MINUS_ACCEPTED_PRODUCTION_CONSUMPTION",
            "fundingEstimateStatus","MODEL_ESTIMATE_NOT_NATIVE_THROUGHPUT");
        String reason=null;
        if(upgrade==null||!"upgrade".equals(upgrade.get("type"))||!(upgrade.get("actionId") instanceof String)
                ||!Double.isFinite(cost)||cost<=0)reason="NATIVE_UPGRADE_UNKNOWN";
        else if(!Double.isFinite(credits)||!Double.isFinite(reserved)||reserved<0
                ||!Double.isFinite(replacementCost)||replacementCost<0||!independentOperations&&replacementCost==0)reason="CONVERSION_BUDGET_UNKNOWN";
        else if(!independentOperations&&(!Double.isFinite(income)||income<=0||!Double.isFinite(consumption)))reason="CONVERSION_BUDGET_UNKNOWN";
        else if(!independentOperations&&recovery)reason="RECOVERY_PROTECTED";
        else if(!independentOperations&&(armed<6||healthy<6||healthy<armed*.6))reason="HEALTHY_FORCE_RECOVERY";
        if(reason!=null)return new TechInvestment(false,false,reason,evidence);
        if(Boolean.TRUE.equals(upgrade.get("affordable"))&&credits>=required)
            return new TechInvestment(true,false,independentOperations?"FUNDED_NATIVE_TECH_CONVERSION_WITH_SELECTED_COMMITMENTS_PROTECTED":"OWN_FORCE_SUPPORTED_NATIVE_TECH_CONVERSION",evidence);
        // Do not stand idle saving from cash alone. A bounded, income-funded wait is permitted
        // after the useful force exists; protected funds and one replacement are kept intact.
        boolean bank=armed>=6&&healthy>=6&&surplus>0&&Double.isFinite(fundingSeconds)&&fundingSeconds<=30;
        return new TechInvestment(false,bank,bank?"BOUNDED_INCOME_FUNDED_TECH_WAIT":"PRODUCTION_BEFORE_UNFUNDED_TECH",evidence);
    }
    enum Bottleneck { MONEY_LIMITED, RESERVE_PROTECTED, NO_USEFUL_DEMAND, ARMY_CAPACITY_LIMIT,
        HARD_SAFETY_CAP, PRODUCER_THROUGHPUT_LIMIT, PRODUCER_TECH_LIMIT, ROUTE_UNAVAILABLE,
        CAPACITY_EXPANSION_COMMITTED, UNKNOWN }
    static Bottleneck classify(int committed,int target,int hard,String demand,int routes,
            boolean techBlocked,boolean budgetKnown,double credits,double cost,double reserved,
            boolean recovery,boolean expansion,boolean sustained,double utilization,int saturation){
        if(committed>=hard)return Bottleneck.HARD_SAFETY_CAP;
        if("NONE".equals(demand))return Bottleneck.NO_USEFUL_DEMAND;
        if("ROUTE_UNAVAILABLE".equals(demand))return Bottleneck.ROUTE_UNAVAILABLE;
        if(!"KNOWN".equals(demand))return Bottleneck.UNKNOWN;
        if(routes==0)return techBlocked?Bottleneck.PRODUCER_TECH_LIMIT:Bottleneck.ROUTE_UNAVAILABLE;
        if(!budgetKnown)return Bottleneck.UNKNOWN;
        if(credits<cost)return Bottleneck.MONEY_LIMITED;
        if(recovery||credits-cost<reserved)return Bottleneck.RESERVE_PROTECTED;
        if(expansion)return Bottleneck.CAPACITY_EXPANSION_COMMITTED;
        if(committed>=target)return Bottleneck.ARMY_CAPACITY_LIMIT;
        if(sustained&&utilization>=saturation)return Bottleneck.PRODUCER_THROUGHPUT_LIMIT;
        return Bottleneck.UNKNOWN;
    }
}
