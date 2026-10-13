package io.rwagent.client;

import java.util.*;

/** Current-visible AIR shortage creates a bounded procurement preference, not an attack permission.
 * Prices/products are legal native menu actions. Catalog compatibility is only a domain candidate. */
public final class AirResponsePolicy {
    public static final class Need {
        public final int threats,coverage,desired,deficit;
        public final List<Long> targets;
        public final Map<String,Object> evidence;
        Need(int threats,int coverage,int desired,List<Long> targets,Map<String,Object> evidence){
            this.threats=threats;this.coverage=coverage;this.desired=desired;deficit=Math.max(0,desired-coverage);
            this.targets=Collections.unmodifiableList(targets);this.evidence=evidence;
            evidence.put("visibleAirThreatCount",threats);evidence.put("currentCounterCoverage",coverage);
            evidence.put("desiredCounterCount",desired);evidence.put("counterDeficit",deficit);evidence.put("targetIds",targets);
        }
    }
    public static Need assess(Map<String,Object> state,Map<String,Object> enemies,Collection<Long> ordinary,int committedCounters){
        Long now=num(state,"gameTimeMs"),frame=num(state,"frame"),source=num(enemies,"gameTimeMs");
        Map<String,Object> evidence=map("sessionId",state.get("sessionId"),"player",playerKey(state),"frame",frame,"gameTimeMs",now,
                "need","ANTI_AIR","source","CURRENT_VISIBLE_AIR_NEAR_OWN_FORCE","executionPermission","NEEDS_NATIVE_ENGAGEMENT");
        List<Long> threats=new ArrayList<Long>();int covered=0;
        Long enemyFrame=num(enemies,"frame");
        boolean current=now!=null&&source!=null&&source>=now&&(enemyFrame==null||frame!=null&&enemyFrame>=frame)&&Objects.equals(state.get("sessionId"),enemies==null?null:enemies.get("sessionId"))
            &&enemies.get("visibleEnemies") instanceof List&&Boolean.TRUE.equals(enemies.get("catalogGameJarMatched"))
            &&TargetCatalog.catalogSha256()!=null&&TargetCatalog.catalogSha256().equals(enemies.get("catalogSha256"));
        if(current&&!sourceMatchesPlayer(state,enemies))current=false;
        if(current)for(Object row:(List<?>)enemies.get("visibleEnemies")){
            if(!(row instanceof Map)){current=false;break;}
            @SuppressWarnings("unchecked") Map<String,Object> enemy=(Map<String,Object>)row;
            if(!readyEnemy(enemy)||Boolean.TRUE.equals(enemy.get("dead"))||!Objects.equals(source,num(enemy,"lastSeenGameTimeMs"))){current=false;break;}
        }
        evidence.put("status",current?"CURRENT_ONLY":"UNKNOWN");
        if(!current)return new Need(0,0,0,threats,evidence);
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> unit:items(state,"ownUnits"))if(ordinary.contains(id(unit))&&ready(unit))own.add(unit);
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies")){
            if(!"AIR".equals(enemy.get("targetDomain"))||!Boolean.TRUE.equals(enemy.get("canAttack"))
                    ||!Objects.equals(source,num(enemy,"lastSeenGameTimeMs"))||!Objects.equals(source,num(enemy,"domainObservedAtGameTimeMs"))
                    ||!readyEnemy(enemy))continue;
            for(Map<String,Object> actor:own)if(distance(actor,enemy)<=700){threats.add(id(enemy));break;}
        }
        if(threats.isEmpty())return new Need(0,0,0,threats,evidence);
        // Own ready counters anywhere can be assigned by the existing Commander. No hidden target is used.
        for(Map<String,Object> unit:own)if(counter(String.valueOf(unit.get("type")))&&value(unit,"hp")>=value(unit,"maxHp")*.5)covered++;
        int desired=Math.min(6,Math.max(2,threats.size()));
        return new Need(threats.size(),covered+Math.max(0,committedCounters),desired,threats,evidence);
    }
    public static Map<String,Object> action(Map<String,Object> factory,Need need,double credits,double reserved){
        if(need==null||need.deficit<=0||!"CURRENT_ONLY".equals(need.evidence.get("status"))||!Double.isFinite(credits)||!Double.isFinite(reserved))return null;
        Map<String,Object> best=null;
        for(Map<String,Object> action:items(factory,"actions")){
            String type=String.valueOf(action.get("type"));double cost=value(action,"cost");
            if(!counter(type)||!(action.get("actionId") instanceof String)||!Boolean.TRUE.equals(action.get("affordable"))
                ||!Double.isFinite(cost)||cost<=0||credits-cost<reserved)continue;
            if(best==null||cost<value(best,"cost"))best=action;
        }return best;
    }
    public static boolean counter(String type){
        String movement=TargetCatalog.movementType(type);
        return ("LAND".equals(movement)||"HOVER".equals(movement))
            &&"COMPATIBLE".equals(TargetCatalog.evaluate(type,"AIR",null,true,true).status);
    }
    /** Native state usually supplies player.teamId rather than a synthetic playerKey. */
    static String playerKey(Map<String,Object> state){
        if(state==null)return null;
        Object explicit=state.get("playerKey"),nativePlayer=state.get("player");String nativeKey=null;
        if(nativePlayer instanceof Map&&((Map<?,?>)nativePlayer).containsKey("teamId")){
            Long team=GameClock.number(((Map<?,?>)nativePlayer).get("teamId"));if(team==null)return null;nativeKey="team:"+team;
        }
        if(explicit!=null){if(!(explicit instanceof String)||((String)explicit).isEmpty())return null;
            if(nativeKey!=null&&!nativeKey.equals(explicit))return null;return (String)explicit;}
        return nativeKey;
    }
    static boolean sourceMatchesPlayer(Map<String,Object> state,Map<String,Object> source){
        String own=playerKey(state);if(own==null||source==null)return false;
        if(source.containsKey("playerKey")&&!own.equals(source.get("playerKey")))return false;
        Object player=source.get("player");
        if(player instanceof Map&&((Map<?,?>)player).containsKey("teamId")){
            Long team=GameClock.number(((Map<?,?>)player).get("teamId"));if(team==null||!own.equals("team:"+team))return false;
        }
        return true;
    }
    private static boolean ready(Map<String,Object> u){return readyEnemy(u)&&!Boolean.TRUE.equals(u.get("dead"))&&value(u,"buildProgress")>=1;}
    private static boolean readyEnemy(Map<String,Object> u){return id(u)>=0&&Double.isFinite(value(u,"x"))&&Double.isFinite(value(u,"y"))&&Double.isFinite(value(u,"hp"))&&value(u,"hp")>0;}
    private static double distance(Map<String,Object> a,Map<String,Object> b){return Math.hypot(value(a,"x")-value(b,"x"),value(a,"y")-value(b,"y"));}
    private static long id(Map<String,Object> m){Long n=num(m,"id");return n==null?-1:n;}
    private static Long num(Map<String,Object> m,String k){return m==null?null:GameClock.number(m.get(k));}
    private static double value(Map<String,Object> m,String k){return m!=null&&m.get(k) instanceof Number?((Number)m.get(k)).doubleValue():Double.NaN;}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> items(Map<String,Object> m,String k){return m!=null&&m.get(k) instanceof List?(List<Map<String,Object>>)m.get(k):Collections.<Map<String,Object>>emptyList();}
    private static Map<String,Object> map(Object... v){Map<String,Object> m=new LinkedHashMap<String,Object>();for(int i=0;i<v.length;i+=2)m.put(String.valueOf(v[i]),v[i+1]);return m;}
}
