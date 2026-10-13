package io.rwagent.client;

import java.util.*;

/** Static mechanics calibrated against the frozen 1.15 engine, never hidden dynamic state. */
public final class CombatProfiles {
    public static final String SOURCE="knowledge/COMBAT_PROFILES_1_15.md#frozen-8a550a37";
    public static final class Profile {
        public final double maxHp,damage,reloadTicks,range,speed;
        public final boolean surface,air;
        Profile(double hp,double damage,double reload,double range,double speed,boolean surface,boolean air){this.maxHp=hp;this.damage=damage;this.reloadTicks=reload;this.range=range;this.speed=speed;this.surface=surface;this.air=air;}
        public double dps(){return damage*60/reloadTicks;}
        boolean supports(String domain){return "SURFACE".equals(domain)?surface:"AIR".equals(domain)&&air;}
    }
    private static final Map<String,Profile> PROFILES=new HashMap<String,Profile>();
    static {
        Profile tank=new Profile(210,25,75,130,1.1,true,false);PROFILES.put("tank",tank);PROFILES.put("c_tank",tank);
        PROFILES.put("scout",new Profile(350,17,50,110,1,true,true));
        PROFILES.put("heavyTank",new Profile(600,50,70,160,.8,true,true));
        PROFILES.put("commandCenter",new Profile(4000,70,70,280,0,true,true));
    }
    public static boolean trusted(Map<String,Object> packet){return packet!=null&&Boolean.TRUE.equals(packet.get("catalogGameJarMatched"))&&TargetCatalog.catalogSha256()!=null&&TargetCatalog.catalogSha256().equals(packet.get("catalogSha256"));}
    public static Profile get(String type,boolean trusted){return trusted?PROFILES.get(type):null;}
    static String currentDomain(Map<String,Object> row,Map<String,Object> packet){Long now=GameClock.number(packet.get("gameTimeMs"));return now!=null&&Objects.equals(now,GameClock.number(row.get("domainObservedAtGameTimeMs")))&&row.get("targetDomain") instanceof String?(String)row.get("targetDomain"):null;}
    static String ownDomain(Map<String,Object> row,Map<String,Object> packet){String current=currentDomain(row,packet);if(current!=null)return current;return get((String)row.get("type"),trusted(packet))!=null?"SURFACE":null;}
}
