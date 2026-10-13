package io.rwagent.client;

import java.util.*;

/** Transparent first-volley/TTK exchange estimate. Native eligibility remains mandatory. */
public final class FightEstimate {
    public static final double MAX_EXPECTED_LOSS_FRACTION=.55;
    public final double ownHp,ownDps,enemyDps,targetHp,timeToKillSeconds,approachExposureSeconds,expectedLossFraction;
    public final int knownActors,unknownActors,unknownDefenders;
    public final boolean known,admitted;
    public final String reason;
    private FightEstimate(double hp,double dps,double incoming,double targetHp,double ttk,double approach,double loss,int actors,int unknown,int defenders,boolean known,boolean admitted,String reason){this.ownHp=hp;this.ownDps=dps;this.enemyDps=incoming;this.targetHp=targetHp;this.timeToKillSeconds=ttk;this.approachExposureSeconds=approach;this.expectedLossFraction=loss;this.knownActors=actors;this.unknownActors=unknown;this.unknownDefenders=defenders;this.known=known;this.admitted=admitted;this.reason=reason;}
    public static FightEstimate estimate(List<Map<String,Object>> actors,Map<String,Object> target,List<Map<String,Object>> enemies,Map<String,Object> packet){
        boolean trusted=CombatProfiles.trusted(packet);double hp=0,dps=0,incoming=0,approach=0;int count=0,unknown=0,defenders=0;
        String domain=CombatProfiles.currentDomain(target,packet);if(domain==null&&Boolean.TRUE.equals(target.get("building")))domain="SURFACE";
        for(Map<String,Object> actor:actors){hp+=TacticalOpportunity.number(actor,"hp");CombatProfiles.Profile profile=CombatProfiles.get((String)actor.get("type"),trusted);
            if(profile==null||domain==null){unknown++;continue;}
            if(!profile.supports(domain))continue;count++;dps+=profile.dps();
        }
        for(Map<String,Object> enemy:enemies){if(!Boolean.TRUE.equals(enemy.get("canAttack"))||TacticalOpportunity.distance(enemy,TacticalOpportunity.number(target,"x"),TacticalOpportunity.number(target,"y"))>TacticalOpportunity.DEFENDER_RADIUS)continue;
            CombatProfiles.Profile profile=CombatProfiles.get((String)enemy.get("type"),trusted);if(profile==null){defenders++;continue;}
            // The model may only count a weapon against this current actor domain; missing domain stays UNKNOWN.
            boolean threatens=false,domainUnknown=false;double defenderApproach=0;
            for(Map<String,Object> actor:actors){String actorDomain=CombatProfiles.ownDomain(actor,packet);if(actorDomain==null){domainUnknown=true;threatens=true;continue;}if(!profile.supports(actorDomain))continue;threatens=true;
                CombatProfiles.Profile own=CombatProfiles.get((String)actor.get("type"),trusted);
                if(own!=null&&own.speed>0)defenderApproach=Math.max(defenderApproach,Math.min(Math.max(0,profile.range-own.range),Math.max(0,TacticalOpportunity.distance(actor,TacticalOpportunity.number(enemy,"x"),TacticalOpportunity.number(enemy,"y"))-own.range))/(own.speed*60));
            }
            if(domainUnknown)defenders++;
            if(threatens){incoming+=profile.dps();approach=Math.max(approach,defenderApproach);}
        }
        double targetHp=TacticalOpportunity.number(target,"hp"),ttk=dps>0?targetHp/dps:Double.POSITIVE_INFINITY;
        boolean known=trusted&&unknown==0&&defenders==0&&dps>0&&hp>0;
        double loss=incoming==0?0:incoming*(ttk+approach)/Math.max(1,hp);
        boolean admitted=known&&loss<MAX_EXPECTED_LOSS_FRACTION;
        return new FightEstimate(hp,dps,incoming,targetHp,ttk,approach,loss,count,unknown,defenders,known,admitted,known?(admitted?"KNOWN_STATIC_TTK_EXCHANGE_WITHIN_LOSS_BUDGET":"KNOWN_STATIC_TTK_EXCHANGE_EXCEEDS_LOSS_BUDGET"):"UNKNOWN_PROFILE_OR_CURRENT_DOMAIN_USE_CONSERVATIVE_FALLBACK");
    }
    public Map<String,Object> view(){Map<String,Object> out=new LinkedHashMap<String,Object>();out.put("confidence",known?"STATIC_MECHANICS_MODEL":"UNKNOWN_PARTIAL_PROFILE");out.put("reason",reason);out.put("source",CombatProfiles.SOURCE);out.put("ownHp",ownHp);out.put("ownDps",unknownActors==0?ownDps:null);out.put("enemyDps",unknownDefenders==0?enemyDps:null);out.put("knownOwnDpsSubtotal",ownDps);out.put("knownEnemyDpsSubtotal",enemyDps);out.put("targetHp",targetHp);out.put("timeToKillSeconds",known&&Double.isFinite(timeToKillSeconds)?timeToKillSeconds:null);out.put("approachExposureSeconds",known?approachExposureSeconds:null);out.put("expectedLossFraction",known&&Double.isFinite(expectedLossFraction)?expectedLossFraction:null);out.put("lossBudget",MAX_EXPECTED_LOSS_FRACTION);out.put("knownActors",knownActors);out.put("unknownActors",unknownActors);out.put("unknownDefenders",unknownDefenders);out.put("admitted",admitted);out.put("limits","NOMINAL_FIRE_UNIFORM_HP_NO_FORMATION_FOCUS_FIRE_REPAIR_OR_EXECUTION_CLAIM");return out;}
}
