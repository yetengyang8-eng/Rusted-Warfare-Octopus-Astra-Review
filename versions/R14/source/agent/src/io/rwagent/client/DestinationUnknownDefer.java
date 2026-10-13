package io.rwagent.client;

import java.util.*;

/** Short scheduling defer from later own progress. Never a native destination or execution witness. */
final class DestinationUnknownDefer {
    static final String EVIDENCE="DESTINATION_UNKNOWN_PROGRESS_SHORT_DEFER";
    final double x,y;final long acceptedAt,receiptFrame;final String threat;
    private double bestDistance=Double.NaN;private long sampleFrame=-1,sampleTime=-1,lastProgressAt=-1;private boolean netProgress;
    DestinationUnknownDefer(double x,double y,long acceptedAt,long receiptFrame,String threat){this.x=x;this.y=y;this.acceptedAt=acceptedAt;this.receiptFrame=receiptFrame;this.threat=threat;}
    boolean allows(Map<String,Object> actor,long frame,long now,long hardWindow,long recentWindow,String currentThreat){
        if(actor.containsKey("orderX")||actor.containsKey("orderY")||frame<=receiptFrame||now<acceptedAt||now-acceptedAt>=hardWindow
                ||"UNKNOWN".equals(currentThreat)||!Objects.equals(threat,currentThreat))return false;
        observe(actor,frame,now);double d=distance(actor,x,y);if(!Double.isFinite(d))return false;
        return netProgress&&lastProgressAt>=0&&now-lastProgressAt<=recentWindow&&d<=bestDistance+8;
    }
    void observe(Map<String,Object> actor,long frame,long now){if(frame<=receiptFrame||frame<=sampleFrame||now<acceptedAt)return;double d=distance(actor,x,y);if(!Double.isFinite(d))return;
        // A long observation gap supplies a new baseline, never a fabricated recent progress interval.
        boolean gap=sampleTime>=0&&(now<sampleTime||now-sampleTime>2500);sampleFrame=frame;sampleTime=now;
        if(!Double.isFinite(bestDistance)||gap){bestDistance=d;netProgress=false;lastProgressAt=-1;}else if(d<=bestDistance-24){bestDistance=d;lastProgressAt=now;netProgress=true;}}
    static String threatSignature(Map<String,Object> enemies,boolean current){if(!current||enemies==null)return "UNKNOWN";Long time=GameClock.number(enemies.get("gameTimeMs"));Object rows=enemies.get("visibleEnemies");
        if(time==null||!(rows instanceof List))return "UNKNOWN";List<String> threats=new ArrayList<String>();
        for(Object raw:(List<?>)rows){if(!(raw instanceof Map))return "UNKNOWN";Map<?,?> row=(Map<?,?>)raw;Long id=GameClock.number(row.get("id"));
            if(id==null||!Objects.equals(time,GameClock.number(row.get("lastSeenGameTimeMs")))||!finite(row.get("x"))||!finite(row.get("y"))||!finite(row.get("hp")))return "UNKNOWN";
            if(Boolean.TRUE.equals(row.get("canAttack")))threats.add(id+":"+row.get("targetDomain")+":"+row.get("building")+":"+Math.floor(number(row,"x")/100)+":"+Math.floor(number(row,"y")/100)+":"+Math.ceil(number(row,"hp")/250));}
        Collections.sort(threats);return threats.toString();}
    private static double distance(Map<String,Object> actor,double x,double y){return Math.hypot(number(actor,"x")-x,number(actor,"y")-y);}
    private static double number(Map<?,?> actor,String key){Object value=actor.get(key);return value instanceof Number?((Number)value).doubleValue():Double.NaN;}
    private static boolean finite(Object value){return value instanceof Number&&Double.isFinite(((Number)value).doubleValue());}
}
