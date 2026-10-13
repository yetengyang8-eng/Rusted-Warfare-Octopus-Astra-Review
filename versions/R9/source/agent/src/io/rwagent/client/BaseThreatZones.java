package io.rwagent.client;

import java.util.*;

/** Last legally witnessed base geometry can veto travel; it never authorizes an attack. */
public final class BaseThreatZones {
    public static final int SMALL_FORCE_MAXIMUM=11;
    private String session,player;private long frame=-1,time=-1;
    private final Map<Long,Map<String,Object>> zones=new LinkedHashMap<Long,Map<String,Object>>();
    public void reset(){zones.clear();session=player=null;frame=time=-1;}
    public void observe(String session,String player,long frame,long now,Map<String,Object> packet,boolean current){
        if(!Objects.equals(this.session,session)||!Objects.equals(this.player,player)||frame<this.frame||now<this.time){reset();this.session=session;this.player=player;}
        if(!current||frame<=this.frame)return;this.frame=frame;time=now;
        Object rows=packet.get("visibleEnemies");if(!(rows instanceof List))return;
        for(Object raw:(List<?>)rows){if(!(raw instanceof Map))continue;@SuppressWarnings("unchecked") Map<String,Object> row=(Map<String,Object>)raw;long id=TacticalOpportunity.id(row);
            if(id<0||!Objects.equals(GameClock.number(packet.get("gameTimeMs")),GameClock.number(row.get("lastSeenGameTimeMs"))))continue;
            if(Boolean.TRUE.equals(row.get("dead"))||row.get("hp") instanceof Number&&((Number)row.get("hp")).doubleValue()==0){zones.remove(id);continue;}
            if(!"commandCenter".equals(row.get("type"))||!Boolean.TRUE.equals(row.get("canAttack")))continue;
            CombatProfiles.Profile profile=CombatProfiles.get("commandCenter",CombatProfiles.trusted(packet));Map<String,Object> zone=new LinkedHashMap<String,Object>();
            zone.put("enemyId",id);zone.put("type",row.get("type"));zone.put("x",row.get("x"));zone.put("y",row.get("y"));zone.put("range",profile==null?TacticalOpportunity.STANDOFF_DISTANCE:profile.range);zone.put("rangeEvidence",profile==null?"UNKNOWN_CONSERVATIVE_POLICY_MARGIN":"VERIFIED_STATIC_MAX_RANGE");zone.put("witnessFrame",frame);zone.put("witnessGameTimeMs",now);zone.put("sessionId",session);zone.put("playerKey",player);zone.put("authority","TRAVEL_VETO_ONLY_NOT_CURRENT_ENEMY_OR_DEATH_OR_ATTACK_PERMISSION");zones.put(id,zone);
        }
    }
    public Long blocks(double x,double y,double tx,double ty,Set<Long> currentlyAdmitted){double dx=tx-x,dy=ty-y,len=dx*dx+dy*dy;
        for(Map.Entry<Long,Map<String,Object>> entry:zones.entrySet()){if(currentlyAdmitted!=null&&currentlyAdmitted.contains(entry.getKey()))continue;Map<String,Object> z=entry.getValue();double zx=TacticalOpportunity.number(z,"x"),zy=TacticalOpportunity.number(z,"y"),radius=TacticalOpportunity.number(z,"range")+60;
            // Memory must not imprison a force already inside the remembered circle.
            // A straight segment continuously increasing distance may leave it; native path safety stays unknown.
            if(Math.hypot(x-zx,y-zy)<=radius&&Math.hypot(tx-zx,ty-zy)>radius&&(x-zx)*dx+(y-zy)*dy>=0)continue;
            double t=len>0?Math.max(0,Math.min(1,((zx-x)*dx+(zy-y)*dy)/len)):0;if(Math.hypot(zx-x-t*dx,zy-y-t*dy)<=radius)return entry.getKey();}
        return null;
    }
    /** This remembered geometry protects small ordinary ground detachments only.
     * Count must be refreshed from owned, healthy, ready members, never desired roster strength. */
    public Long blocks(double x,double y,double tx,double ty,int healthyReadyOrdinaryGround){
        return healthyReadyOrdinaryGround>SMALL_FORCE_MAXIMUM?null:blocks(x,y,tx,ty,Collections.<Long>emptySet());
    }
    public List<Map<String,Object>> view(){List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();for(Map<String,Object> zone:zones.values())result.add(Collections.unmodifiableMap(new LinkedHashMap<String,Object>(zone)));return Collections.unmodifiableList(result);}
}
