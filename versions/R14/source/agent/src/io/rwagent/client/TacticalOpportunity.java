package io.rwagent.client;

import java.util.*;

/** Lawful G5.1 opportunity admission. These are conservative exposure proxies, not DPS or kill estimates. */
final class TacticalOpportunity {
    static final double DEFENDER_RADIUS=600, STATIC_EXPOSURE_WEIGHT=.5, MAX_APPROACH_EXPOSURE=1.15, STANDOFF_DISTANCE=700;
    static final class Exposure {
        double mobileHp,staticHp,airHp,compatibleAirHp,unknownAirHp,retreatDistance,supportHp,ratio;
        boolean airCoverageKnown=true;
        final List<Long> airIds=new ArrayList<Long>(),defenderIds=new ArrayList<Long>();
        boolean airDeficit(){return airHp>0&&airCoverageKnown&&compatibleAirHp<Math.max(airHp*.8,1);}
        /** Coverage is a procurement need. Only substantial current exposure also
         * blocks unrelated ground objectives; a chip aircraft is not a whole-army veto. */
        boolean airBlocksAdvance(){return airDeficit()&&ratio>=MAX_APPROACH_EXPOSURE;}
    }
    static Exposure assess(List<Map<String,Object>> actors,List<Map<String,Object>> own,List<Map<String,Object>> enemies,
                           Map<String,Object> packet,double x,double y,double homeX,double homeY){
        Exposure out=new Exposure();double ownHp=0;
        Set<Long> members=new HashSet<Long>();for(Map<String,Object> actor:actors){ownHp+=number(actor,"hp");members.add(id(actor));}
        boolean trusted=Boolean.TRUE.equals(packet.get("catalogGameJarMatched"))&&TargetCatalog.catalogSha256()!=null
                &&TargetCatalog.catalogSha256().equals(packet.get("catalogSha256"));
        out.airCoverageKnown=trusted;
        Long now=GameClock.number(packet.get("gameTimeMs"));
        for(Map<String,Object> enemy:enemies){if(!Boolean.TRUE.equals(enemy.get("canAttack"))||distance(enemy,x,y)>DEFENDER_RADIUS)continue;
            double hp=number(enemy,"hp");out.defenderIds.add(id(enemy));
            if(Boolean.TRUE.equals(enemy.get("building")))out.staticHp+=hp;else out.mobileHp+=hp;
            if(!"AIR".equals(enemy.get("targetDomain"))||!Objects.equals(now,GameClock.number(enemy.get("domainObservedAtGameTimeMs"))))continue;
            out.airHp+=hp;out.airIds.add(id(enemy));double compatible=0,unknown=0;
            for(Map<String,Object> actor:actors){TargetCatalog.Decision d=TargetCatalog.evaluate((String)actor.get("type"),"AIR",null,true,trusted);
                if("COMPATIBLE".equals(d.status))compatible+=number(actor,"hp");else if("UNKNOWN".equals(d.status))unknown+=number(actor,"hp");}
            // Coverage is the same current force, not summed once for every air contact.
            out.compatibleAirHp=Math.max(out.compatibleAirHp,compatible);out.unknownAirHp=Math.max(out.unknownAirHp,unknown);
            if(unknown>0)out.airCoverageKnown=false;
        }
        for(Map<String,Object> actor:own)if(!members.contains(id(actor))&&Boolean.TRUE.equals(actor.get("canAttack"))
                &&!Boolean.TRUE.equals(actor.get("dead"))&&number(actor,"buildProgress")>=1&&distance(actor,x,y)<=DEFENDER_RADIUS)out.supportHp+=number(actor,"hp");
        out.retreatDistance=Double.isFinite(homeX)&&Double.isFinite(homeY)?Math.hypot(x-homeX,y-homeY):0;
        // Distant rear support cannot be borrowed as fighting strength. Nearby own support discounts exposure modestly.
        double escapeCost=1+Math.min(.35,out.retreatDistance/6000),support=1+Math.min(.25,out.supportHp/Math.max(1,ownHp)*.25);
        out.ratio=(out.mobileHp+out.staticHp*STATIC_EXPOSURE_WEIGHT)/Math.max(1,ownHp)*escapeCost/support;
        return out;
    }
    static double value(Map<String,Object> target){String type=(String)target.get("type");
        if(type!=null&&type.startsWith("extractor"))return 220;if("landFactory".equals(type)||"airFactory".equals(type))return 180;
        if("builder".equals(type))return 160;if("commandCenter".equals(type))return 260;return 0;}
    /** Conservative straight-line policy exclusion only. Native pathKnown still does not establish dynamic safety. */
    static boolean excludesVisibleHazards(List<Map<String,Object>> enemies,double x,double y,double tx,double ty){
        double dx=tx-x,dy=ty-y,length2=dx*dx+dy*dy;
        for(Map<String,Object> enemy:enemies)if(Boolean.TRUE.equals(enemy.get("canAttack"))){
            double fraction=length2>0?Math.max(0,Math.min(1,((number(enemy,"x")-x)*dx+(number(enemy,"y")-y)*dy)/length2)):0;
            if(distance(enemy,x+fraction*dx,y+fraction*dy)<=STANDOFF_DISTANCE)return false;
        }
        return true;
    }
    static double distance(Map<String,Object> row,double x,double y){return Math.hypot(number(row,"x")-x,number(row,"y")-y);}
    static double number(Map<String,Object> row,String key){Object n=row.get(key);return n instanceof Number?((Number)n).doubleValue():0;}
    static long id(Map<String,Object> row){Long n=GameClock.number(row.get("id"));return n==null?-1:n;}
}
