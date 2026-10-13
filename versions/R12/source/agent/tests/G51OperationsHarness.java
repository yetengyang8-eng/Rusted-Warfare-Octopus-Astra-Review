package io.rwagent.client;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import static io.rwagent.client.StrategyDirector.*;

/** Current lawful AIR procurement and existing economy integration. Synthetic inputs, no native motion. */
public final class G51OperationsHarness {
    static int checks;
    static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void set(Object target,String field,Object v)throws Exception{Field f=target.getClass().getDeclaredField(field);f.setAccessible(true);f.set(target,v);}
    static Object get(Object target,String field)throws Exception{Field f=target.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(target);}
    static Object call(Object target,String name,Map<String,Object> state)throws Exception{Method m=target.getClass().getDeclaredMethod(name,Map.class);m.setAccessible(true);return m.invoke(target,state);}
    static Map<String,Object> own(long id,String type){return map("id",id,"type",type,"x",100,"y",100,"hp",600,"maxHp",600,"buildProgress",1,"mobile",true,"canAttack",true,"dead",false);}
    static Map<String,Object> packet(long time,long frame,List<Map<String,Object>> enemies){return map("sessionId","g51","playerKey","team:0","gameTimeMs",time,"frame",frame,"visibleEnemies",enemies,
        "catalogGameJarMatched",true,"catalogSha256",TargetCatalog.catalogSha256());}
    static void air(){
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();for(int i=1;i<=6;i++)own.add(own(i,"c_tank"));
        Map<String,Object> state=map("sessionId","g51","playerKey","team:0","frame",10L,"gameTimeMs",10000L,"ownUnits",own);
        Map<String,Object> enemy=map("id",99L,"type","gunShip","x",300,"y",100,"hp",260,"canAttack",true,"targetDomain","AIR","lastSeenGameTimeMs",10000L,"domainObservedAtGameTimeMs",10000L);
        Map<String,Object> contacts=packet(10000,10,Collections.singletonList(enemy));
        Set<Long> ids=new LinkedHashSet<Long>(Arrays.asList(1L,2L,3L,4L,5L,6L));
        AirResponsePolicy.Need need=AirResponsePolicy.assess(state,contacts,ids,0);
        require(need.deficit==2&&need.targets.equals(Collections.singletonList(99L)),"current aircraft creates bounded need over c_tank shortage");
        Map<String,Object> heavy=map("type","heavyTank","actionId","heavy","cost",800,"affordable",true);
        Map<String,Object> tank=map("type","c_tank","actionId","tank","cost",350,"affordable",true);
        Map<String,Object> factory=map("actions",Arrays.asList(tank,heavy));
        require(AirResponsePolicy.action(factory,need,2000,700)==heavy,"legal counter quote chosen while protected funds remain");
        require(AirResponsePolicy.action(factory,need,1400,700)==null,"reserve protects counter purchase");
        require(AirResponsePolicy.action(map("actions",Collections.singletonList(tank)),need,100000,0)==null,"cash does not invent T1 counter route");
        require(AirResponsePolicy.assess(state,contacts,ids,2).deficit==0,"actual outstanding counter commitments count before visibility");
        own.get(0).put("type","heavyTank");own.get(1).put("type","heavyTank");
        require(AirResponsePolicy.assess(state,contacts,ids,0).deficit==0,"ready counter coverage avoids blind counter spam");
        enemy.put("domainObservedAtGameTimeMs",9000L);require(AirResponsePolicy.assess(state,contacts,ids,0).threats==0,"stale physical AIR not used");enemy.put("domainObservedAtGameTimeMs",10000L);
        contacts.put("visibleEnemies",Collections.emptyList());contacts.put("rememberedEnemies",Collections.singletonList(enemy));
        require(AirResponsePolicy.assess(state,contacts,ids,0).desired==0,"LOST_CONTACT not a new procurement target");
        contacts.put("visibleEnemies",Collections.singletonList(enemy));contacts.put("gameTimeMs",9000L);
        require("UNKNOWN".equals(AirResponsePolicy.assess(state,contacts,ids,0).evidence.get("status")),"stale endpoint unknown");
        contacts.put("gameTimeMs",10000L);contacts.put("sessionId","foreign");require(AirResponsePolicy.assess(state,contacts,ids,0).desired==0,"foreign session refused");
        contacts.put("sessionId","g51");contacts.put("playerKey","team:2");require(AirResponsePolicy.assess(state,contacts,ids,0).desired==0,"foreign player refused");
        contacts.put("playerKey","team:0");contacts.put("catalogGameJarMatched",false);require(AirResponsePolicy.assess(state,contacts,ids,0).desired==0,"catalog identity required");
        contacts.put("catalogGameJarMatched",true);state.remove("playerKey");state.put("player",map("teamId",0));
        require("team:0".equals(AirResponsePolicy.assess(state,contacts,ids,0).evidence.get("player")),"native own player.teamId exports evidence identity without synthetic key");
        contacts.put("playerKey","team:2");require("UNKNOWN".equals(AirResponsePolicy.assess(state,contacts,ids,0).evidence.get("status")),"foreign key refused when native own state has no playerKey");
        contacts.put("playerKey","team:0");contacts.put("player",map("teamId",2));
        require("UNKNOWN".equals(AirResponsePolicy.assess(state,contacts,ids,0).evidence.get("status")),"contradictory source native player identity refused");
        contacts.remove("player");contacts.put("visibleEnemies",Collections.singletonList("bad-row"));
        require("UNKNOWN".equals(AirResponsePolicy.assess(state,contacts,ids,0).evidence.get("status")),"malformed air contact list remains unknown rather than failing controller");
        contacts.put("visibleEnemies",Collections.singletonList(enemy));enemy.put("hp",Double.POSITIVE_INFINITY);
        require("UNKNOWN".equals(AirResponsePolicy.assess(state,contacts,ids,0).evidence.get("status")),"nonfinite air HP cannot create legal procurement evidence");
        enemy.put("hp",260);enemy.put("dead",true);
        require("UNKNOWN".equals(AirResponsePolicy.assess(state,contacts,ids,0).evidence.get("status")),"contradictory dead visible air row cannot establish calm or new target");
    }
    static void economy()throws Exception{
        BattleClient client=new BattleClient();set(client,"session","g51");set(client,"time",200000L);set(client,"startTime",0L);set(client,"log",new BufferedWriter(new StringWriter()));
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();for(int i=1;i<=6;i++)own.add(own(i,"c_tank"));
        Map<String,Object> builder=own(50,"builder");builder.put("canAttack",false);own.add(builder);
        Map<String,Object> state=map("sessionId","g51","frame",20L,"gameTimeMs",200000L,"player",map("credits",3000,"teamId",0),"ownUnits",own);
        set(client,"lastEnemies",packet(200000,20,Collections.emptyList()));set(client,"homeX",100.0);set(client,"homeY",100.0);
        set(client,"militaryUrgency","EMERGENCY");
        require(Boolean.FALSE.equals(call(client,"productionRecovery",state)),"distant old global urgency is not production recovery");
        set(client,"lastPreferredUnitCost",350L);set(client,"activeArmyTarget",32);
        call(client,"considerInvestmentIntent",state);require(get(client,"investment")!=null,"six healthy force supports income conversion without waiting for active target32");
        call(client,"maintainInvestment",state);require(get(client,"investment")!=null,"distant pressure does not cancel quiet economic intent");
        Map<String,Object> danger=map("id",900L,"x",200,"y",100,"hp",1000,"canAttack",true,"lastSeenGameTimeMs",200000L);
        set(client,"lastEnemies",packet(200000,20,Collections.singletonList(danger)));
        require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"current homeland threat protects recovery");
        call(client,"maintainInvestment",state);require(get(client,"investment")==null,"current homeland threat cancels unpaid investment");
        set(client,"lastEnemies",packet(199000,19,Collections.emptyList()));require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"unknown current safety cannot fund conversion");
        Map<String,Object> malformed=packet(200000,20,Collections.singletonList(danger));danger.remove("lastSeenGameTimeMs");
        set(client,"lastEnemies",malformed);require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"missing visible-row clock cannot clear recovery");
        danger.put("lastSeenGameTimeMs",199000L);require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"stale row inside current packet remains unknown");
        danger.put("lastSeenGameTimeMs",200000L);danger.put("x",Double.NaN);require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"malformed row geometry cannot establish quiet homeland");
        malformed.put("visibleEnemies",Collections.singletonList("bad-row"));require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"malformed contact collection preserves recovery");
        malformed.put("visibleEnemies",Collections.emptyList());malformed.put("frame",19L);require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"stale frame cannot establish quiet homeland");
        malformed.put("frame",20L);malformed.put("playerKey","team:2");require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"foreign explicit player key cannot fund investment");
        malformed.put("playerKey","team:0");malformed.put("player",map("teamId",2));require(Boolean.TRUE.equals(call(client,"productionRecovery",state)),"foreign source team cannot fund investment");
        malformed.remove("player");require(Boolean.FALSE.equals(call(client,"productionRecovery",state)),"current lawful empty contacts still release distant global veto");
    }
    static void cappedTech()throws Exception{
        System.setProperty("rwagent.allowSmallTanks","true"); // Historical replacement route variant.
        BattleClient client=new BattleClient();set(client,"mobileUnitHardCap",6);
        List<Map<String,Object>> own=new ArrayList<Map<String,Object>>();for(int i=1;i<=6;i++)own.add(own(i,"c_tank"));
        Map<String,Object> state=map("ownUnits",own,"player",map("credits",2500));
        set(client,"lastState",state); // Production admission reads the current own inventory, as the real main loop does.
        Map<String,Object> unitQuote=map("type","c_tank","actionId","native-tank","cost",350,"affordable",true);
        Map<String,Object> upgrade=map("type","upgrade","actionId","native-upgrade","cost",2000,"affordable",true);
        Map<String,Object> factory=map("id",50L,"tier",1,"queue",0,"actions",Arrays.asList(unitQuote,upgrade));
        Method preferred=BattleClient.class.getDeclaredMethod("preferredAction",Map.class,Map.class,int.class);preferred.setAccessible(true);
        Map<String,Object> noUnit=(Map<String,Object>)preferred.invoke(client,factory,state,6);
        require(noUnit==null,"ordinary unit policy blocks production at military cap");
        Method replacement=BattleClient.class.getDeclaredMethod("techReplacementNativeCost",Map.class,Map.class);replacement.setAccessible(true);
        double quoted=(Double)replacement.invoke(client,factory,noUnit);
        require(quoted==350,"fresh native replacement price remains known at army cap");
        require(ProductionCapacity.techInvestment(upgrade,6,6,2500,0,quoted,63,20,false).selected,"capped healthy force may upgrade existing producer without buying military slot");
        require((Integer)get(client,"mobileUnitHardCap")==6,"native tech quote cannot enlarge military cap");
        require(Double.isNaN((Double)replacement.invoke(client,map("actions",Collections.singletonList(upgrade)),null)),"missing native replacement route stays unknown even at cap");
    }
    public static void main(String[] args)throws Exception{air();economy();cappedTech();System.out.println("G51_OPERATIONS_CONTRACT_OK checks="+checks+" fixtureOnly=true");}
}
