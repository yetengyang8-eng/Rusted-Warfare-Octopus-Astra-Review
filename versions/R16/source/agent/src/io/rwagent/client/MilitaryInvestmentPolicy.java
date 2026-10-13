package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.BattleClient.*;

/** Marginal investment in spatial duties. This is not a mainland-clear or victory detector. */
final class MilitaryInvestmentPolicy {
    static Map<String,Object> assess(Map<String,Object> state,Map<String,Object> enemies,int formationSize,boolean spatialDuty){
        double readyHp=0,pressure=0;int readyCount=0,contacts=0;
        for(Map<String,Object> u:units(state))if(alive(u)&&n(u,"buildProgress")>=1&&n(u,"maxHp")>0&&n(u,"hp")>=n(u,"maxHp")*.5
                &&("heavyTank".equals(u.get("type"))||"tank".equals(u.get("type"))||"c_tank".equals(u.get("type"))||"heavyArtillery".equals(u.get("type")))){
            readyHp+=n(u,"hp");readyCount++;
        }
        Long ownTime=GameClock.number(state.get("gameTimeMs")),source=enemies==null?null:GameClock.number(enemies.get("gameTimeMs"));
        boolean current=ownTime!=null&&source!=null&&source>=ownTime&&Objects.equals(state.get("sessionId"),enemies.get("sessionId"))
                &&enemies.get("visibleEnemies") instanceof List;
        if(current)for(Map<String,Object> e:list(enemies,"visibleEnemies")){
            if(!Objects.equals(source,GameClock.number(e.get("lastSeenGameTimeMs")))||!alive(e)){current=false;continue;}
            Object domain=e.get("targetDomain");
            if(domain==null||"UNKNOWN".equals(domain)){current=false;continue;}
            if("LAND".equals(domain)||"GROUND".equals(domain)||"SURFACE".equals(domain)&&!Boolean.TRUE.equals(e.get("touchingWater"))
                    ||Boolean.TRUE.equals(e.get("building"))){
                pressure+=n(e,"hp")*LocalCrisisPolicy.HP_FACTOR;contacts++;
            }
        }
        boolean covered=current&&readyCount>=Math.max(LocalArmyDirector.FORM_MINIMUM,formationSize)&&readyHp>=pressure;
        return StrategyDirector.map("preferSpatialInvestment",spatialDuty&&covered,"currentGroundDemandCovered",covered,
                "readyGroundCount",readyCount,"readyGroundHp",readyHp,"currentVisibleLandPressure",current?pressure:null,
                "currentLandContacts",current?contacts:null,"formationReadinessTarget",formationSize,"spatialDuty",spatialDuty,
                "sourceGameTimeMs",source,"sourceRequestPath","/combat/observe","mainlandCleared","UNKNOWN",
                "reason",!current?"CURRENT_LAND_SAMPLE_UNKNOWN":!covered?"CURRENT_GROUND_COVERAGE_NEEDS_REPLACEMENT"
                    :spatialDuty?"CURRENT_GROUND_COVERED_UNFINISHED_AIR_SPACE":"NO_UNFINISHED_SPATIAL_DUTY",
                "groundProxySemantics","COARSE_ALLOCATION_HP_NOT_FIGHT_OR_HIDDEN_ENEMY_ESTIMATE");
    }
}
