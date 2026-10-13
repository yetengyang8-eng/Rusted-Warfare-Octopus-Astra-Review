package io.rwagent.headless;

import io.rwagent.client.Json;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Focused option checks; optional native mode runs the real runner for three frames.
 * Native mode never creates/moves units, changes funds/fog, or issues game commands.
 */
public final class R15HeadlessPlayerHarness {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void reject(String[] args){
        boolean rejected=false;
        try{HeadlessRunner.playerSlot(HeadlessRunner.parseOptions(args));}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"reject invalid/duplicate player option "+Arrays.toString(args));
    }
    static void options(){
        check(HeadlessRunner.playerSlot(HeadlessRunner.parseOptions(new String[0]))==0,"default slot stays zero");
        check(HeadlessRunner.playerSlot(HeadlessRunner.parseOptions(new String[]{"--player","5"}))==5,"explicit original slot five");
        check(HeadlessRunner.playerSlot(HeadlessRunner.parseOptions(new String[]{"--player","0"}))==0,"explicit slot zero");
        check(HeadlessRunner.playerSlot(HeadlessRunner.parseOptions(new String[]{"--player","7"}))==7,"last native quick-match slot");
        check("3".equals(HeadlessRunner.parseOptions(new String[]{"--difficulty","3","--player","5"}).get("difficulty")),"player selection preserves difficulty option");
        reject(new String[]{"--player","-1"});reject(new String[]{"--player","8"});reject(new String[]{"--player","1.0"});
        reject(new String[]{"--player","UNKNOWN"});reject(new String[]{"--player"});
        reject(new String[]{"--player","5","--player","0"});reject(new String[]{"--players","5"});
    }
    @SuppressWarnings("unchecked") static Map<String,Object> read(Path path)throws Exception{
        return (Map<String,Object>)Json.parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8));
    }
    static int number(Object value){return ((Number)value).intValue();}
    @SuppressWarnings("unchecked") static void nativeSmoke(String[] args)throws Exception{
        check(args.length==6,"native MAP PLAYER|default OUT EXPECTED_X|any EXPECTED_Y|any");
        String map=args[1];int player="default".equals(args[2])?0:Integer.parseInt(args[2]);
        List<String> runner=new ArrayList<String>(Arrays.asList("--map",map,"--port",player==0?"47738":"47739","--frames","3","--speed","0","--max-wall-seconds","10","--difficulty","3"));
        if(!"default".equals(args[2]))runner.addAll(Arrays.asList("--player",args[2]));
        HeadlessRunner.run(runner.toArray(new String[runner.size()]));
        Map<String,Object> initial=read(Paths.get("native-bootstrap.json")),runtime=read(Paths.get("runtime.json"));
        check(number(initial.get("playerId"))==player,"bootstrap owns selected native slot");
        check(number(initial.get("simulationTicks"))==0&&number(initial.get("frame"))==0&&number(initial.get("gameTimeMs"))==0,"spawn evidence precedes all simulation frames");
        check(HeadlessRunner.HASH.equals(initial.get("gameJarSha256")),"bootstrap fingerprint matches original supported engine");
        List<Map<String,Object>> teams=(List<Map<String,Object>>)initial.get("teams");int humans=0,ai=0;
        for(Map<String,Object> team:teams) {
            int slot=number(team.get("id"));
            if(slot>=8&&!Boolean.TRUE.equals(team.get("present"))){check(Boolean.FALSE.equals(team.get("present")),"extra native slot "+slot+" is absent");continue;}
            check(Boolean.TRUE.equals(team.get("present")),"native team registered at slot "+slot);
            boolean selected=slot==player;
            check((selected?"com.corrodinggames.rts.game.e":"com.corrodinggames.rts.game.a.a").equals(team.get("class")),"native human/AI class at slot "+slot);
            check(Boolean.valueOf(!selected).equals(team.get("nativeAI")),"only selected human disables native AI at slot "+slot);
            check(number(team.get("initialUnitCount"))>0,"map naturally spawned units at registered slot "+slot);
            if(selected)humans++;else ai++;
        }
        check(humans==1&&ai>=7,"one native human, seven original quick-match AI, plus any map-created native AI");
        List<Map<String,Object>> centers=(List<Map<String,Object>>)initial.get("ownCommandCenters");
        check(centers.size()==1,"exactly one own original map commandCenter");
        Map<String,Object> center=centers.get(0);
        if(!"any".equals(args[4]))check(((Number)center.get("x")).doubleValue()==Double.parseDouble(args[4]),"original commandCenter spawn X matches reference");
        if(!"any".equals(args[5]))check(((Number)center.get("y")).doubleValue()==Double.parseDouble(args[5]),"original commandCenter spawn Y matches reference");
        check(number(runtime.get("playerId"))==player,"runtime retains selected slot");
        check("FRAME_LIMIT".equals(runtime.get("status"))&&number(runtime.get("ticks"))==3&&number(runtime.get("frame"))==3,"real runner advanced exactly three frames");
        check(Boolean.TRUE.equals(runtime.get("fogEnabled"))&&Boolean.TRUE.equals(runtime.get("lineOfSightFog")),"native LOS configuration retained");
        Path out=Paths.get(args[3]);Files.createDirectories(out);
        Files.copy(Paths.get("native-bootstrap.json"),out.resolve("native-bootstrap.json"),StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get("runtime.json"),out.resolve("runtime.json"),StandardCopyOption.REPLACE_EXISTING);
        String summary="{\"status\":\"PASS\",\"checks\":"+checks+",\"playerId\":"+player+",\"simulationFrames\":3,\"initialSpawnX\":"+center.get("x")+",\"initialSpawnY\":"+center.get("y")+",\"nativeHumanCount\":"+humans+",\"nativeAICount\":"+ai+",\"syntheticUnitFixture\":false,\"desktopTouched\":false,\"naturalBattleOrVictoryProven\":false}\n";
        Files.write(out.resolve("summary.json"),summary.getBytes(StandardCharsets.UTF_8));
    }
    public static void main(String[] args){try{
        options();if(args.length>0){check("native".equals(args[0]),"optional mode must be native");nativeSmoke(args);}
        System.out.println("R15_HEADLESS_PLAYER_OK checks="+checks);System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
