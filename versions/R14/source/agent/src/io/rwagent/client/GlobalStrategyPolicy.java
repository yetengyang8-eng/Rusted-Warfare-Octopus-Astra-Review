package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.*;

/** Own mission commitments and income support a bounded inventory of ordinary forces.
 * The inventory horizon is an explicit software policy, never native production speed,
 * unit lifetime, enemy strength, or the engine's unknown unit cap.
 */
final class GlobalStrategyPolicy {
    static final int DEFAULT_SOFTWARE_ARMY_CEILING=512;
    static final double INVENTORY_GAME_SECONDS=600;
    static final class Goal {
        final int total,active;final Map<String,Object> evidence;
        Goal(int total,int active,Map<String,Object> evidence){this.total=total;this.active=active;this.evidence=evidence;}
    }
    static Goal goal(int baseTotal,int baseActive,int ceiling,boolean fixed,long age,double income,
            double consumption,double nativeOrdinaryCost,int ownMissionDemand,boolean currentCompatibleDemand,
            boolean recovery){
        boolean knownBudget=Double.isFinite(income)&&income>0&&Double.isFinite(consumption)
            &&Double.isFinite(nativeOrdinaryCost)&&nativeOrdinaryCost>0;
        boolean useful=ownMissionDemand>0||currentCompatibleDemand;
        boolean supported=!fixed&&age>=90000&&knownBudget&&income>consumption&&useful&&!recovery;
        int economic=knownBudget?(int)Math.min(ceiling,Math.ceil(income*INVENTORY_GAME_SECONDS/nativeOrdinaryCost)):baseTotal;
        int total=fixed?ceiling:Math.min(ceiling,Math.max(baseTotal,supported?Math.max(ownMissionDemand,economic):baseTotal));
        // The inventory is spread across existing independent General formation contracts.
        // Economic inventory growth never turns one later General into a hundreds-unit cohort.
        int active=Math.min(Math.max(1,total-8),baseActive);
        Map<String,Object> evidence=map("policyArmyGoal",total,"policyActiveGoal",active,
            "ordinaryInventoryTarget",total,"formationDesiredStrength",active,
            "economicInventoryGoal",economic,"ownMissionDesiredStrength",Math.max(0,ownMissionDemand),
            "currentCompatibleOrdinaryDemand",currentCompatibleDemand,"incomeEstimate",income,
            "productionConsumptionPerGameSecond",consumption,"ordinaryUnitNativeCost",knownBudget?nativeOrdinaryCost:null,
            "inventoryPolicyGameSeconds",INVENTORY_GAME_SECONDS,"growthSupported",supported,"recovery",recovery,
            "capacitySource",fixed?"EXPLICIT_SOFTWARE_CAP_OVERRIDE":"BOUNDED_SOFTWARE_ECONOMIC_POLICY",
            "softwareSafetyCeiling",ceiling,"nativeUnitCapStatus","UNKNOWN",
            "inventoryModel","INCOME_X_POLICY_HORIZON_OVER_LEGAL_NATIVE_REPLACEMENT_PRICE",
            "inventoryModelStatus","POLICY_TARGET_NOT_NATIVE_THROUGHPUT_OR_ENEMY_ESTIMATE");
        return new Goal(total,active,evidence);
    }
}
