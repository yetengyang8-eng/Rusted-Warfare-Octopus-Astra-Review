package io.rwagent.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Logging-only fixtures and optional streaming replay, never boots an engine. */
public final class CompactBattleLogHarness {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static Map<String,Object> map(Object... pairs){Map<String,Object> m=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    @SuppressWarnings("unchecked") private static Map<String,Object> obj(Object value){return (Map<String,Object>)value;}
    @SuppressWarnings("unchecked") private static Object expand(Object value,Map<String,Object> root){
        if(value instanceof Map){Map<String,Object> m=obj(value);
            if(m.size()==1&&m.containsKey("$rwReportRef")){
                String path=(String)m.get("$rwReportRef");Object target=root;
                for(String key:path.substring(2).split("/"))target=obj(target).get(key.replace("~1","/").replace("~0","~"));
                return expand(target,root);
            }
            Map<String,Object> out=new LinkedHashMap<String,Object>();for(String key:m.keySet())out.put(key,expand(m.get(key),root));return out;
        }
        if(value instanceof List){List<Object> out=new ArrayList<Object>();for(Object item:(List<?>)value)out.add(expand(item,root));return out;}return value;
    }
    private static Map<String,Object> observation(long game,String session){return map("observationId","run:o:"+game,"sourceSessionId",session,
        "sourcePlayerId","team:0","sourceFrame",game,"sourceGameTimeMs",game,"requestPath","/state","discontinuities",Collections.emptyList());}
    public static void main(String[] args)throws Exception{
        CompactBattleLog compact=new CompactBattleLog("compact",30000,15000),full=new CompactBattleLog("full",30000,15000);
        List<Object> units=new ArrayList<Object>();for(int i=0;i<100;i++)units.add(map("id",i,"x",i,"hp",100,"allocation","FREE"));
        Map<String,Object> state=map("gameTimeMs",1000,"sessionId","one","ownUnits",units,"match",map("outcome","ONGOING"));
        Map<String,Object> trace=map("phase","OBSERVATION","observation",observation(1000,"one"),"context",Collections.emptyMap());
        Map<String,Object> first=compact.project(1000,"observation",state,trace);
        check(Boolean.TRUE.equals(obj(first.get("report")).get("keyframe")),"first read keyframe");
        Map<String,Object> second=compact.project(1100,"observation",state,trace);
        check(!obj(second.get("data")).containsKey("ownUnits"),"continuous giant list omitted");
        check(obj(obj(obj(second.get("report")).get("omittedFields")).get("ownUnits")).get("entries").equals(100),"omitted exact count");
        check(state.get("ownUnits")==units&&obj(trace.get("observation")).get("sourceSessionId").equals("one"),"policy inputs unchanged");
        check(obj(second.get("trace")).get("observation").equals(trace.get("observation")),"all read identities preserved");
        check(Boolean.TRUE.equals(obj(compact.project(16000,"observation",state,trace).get("report")).get("keyframe")),"wall keyframe");
        trace=map("phase","OBSERVATION","observation",observation(32000,"one"),"context",Collections.emptyMap());state.put("gameTimeMs",32000);
        check(Boolean.TRUE.equals(obj(compact.project(16100,"observation",state,trace).get("report")).get("keyframe")),"game keyframe");
        trace=map("phase","OBSERVATION","observation",observation(100,"two"),"context",Collections.emptyMap());state.put("gameTimeMs",100);
        check(Boolean.TRUE.equals(obj(compact.project(16200,"observation",state,trace).get("report")).get("keyframe")),"session reset keyframe");
        for(String event:Arrays.asList("g1_command_attempt","g3_intent","g3_execution","command_result","g1_move_witness","g4_force_transition","g5_general_transition","g2_event","error")){
            Map<String,Object> data=map("owner","General:1","inputObservationIds",Arrays.asList("run:o:1","run:o:2"),"actors",units,"receipt",map("status","queued"),"deathProven",false);
            Map<String,Object> causalTrace=map("phase","LATER_WITNESS","command",map("commandId","run:c:1","ownerGenerations",map("7",2)),"context",data,"observation",observation(100,"two"));
            Map<String,Object> row=compact.project(17000,event,data,causalTrace);
            check(row.get("data").equals(data),event+" full causal data");
            check(expand(row.get("trace"),row).equals(causalTrace),event+" exact trace reconstruction");
        }
        Map<String,Object> observedEvent=map("kind","ENEMY_UPDATED","data",map("enemyId",7,"hp",50,"hiddenPositionKnown",false));
        Map<String,Object> observedTrace=map("phase","OBSERVATION","context",observedEvent,"observation",observation(100,"two"));
        compact.project(17100,"g2_event",observedEvent,observedTrace);
        Map<String,Object> observedAgain=compact.project(17200,"g2_event",observedEvent,observedTrace);
        check(observedAgain.get("data").equals(observedEvent)&&!obj(observedAgain.get("report")).containsKey("omittedFields"),"observed G2 events never sampled as raw GET");
        Map<String,Object> numbers=map("count",3,"actors",units);
        Map<String,Object> numberTrace=map("phase","COMMAND_ATTEMPT","context",numbers,"command",map("actors",units));
        StringWriter numericText=new StringWriter();BufferedWriter numericWriter=new BufferedWriter(numericText);
        compact.write(numericWriter,9,"g1_command_attempt",BattleClient.json(numbers),numberTrace);numericWriter.flush();
        Map<String,Object> numericRow=obj(Json.parse(numericText.toString()));
        check(obj(obj(numericRow.get("trace")).get("context")).containsKey("$rwReportRef"),"Integer vs parsed Long context still deduplicated");
        check(BattleClient.json(expand(numericRow.get("trace"),numericRow)).equals(BattleClient.json(numberTrace)),"writer numeric trace reconstruction exact JSON");
        Map<String,Object> force=map("units",units,"generals",Collections.emptyList(),"sourceGameTimeMs",100,"observationId","run:o:100");
        compact.project(18000,"g4_force_state",force,null);
        Map<String,Object> forceSummary=compact.project(18100,"g4_force_state",force,null);
        check(obj(forceSummary.get("data")).get("free").equals(100),"observer free count retained");
        check(obj(forceSummary.get("data")).get("generals").equals(force.get("generals")),"observer generals retained");
        check(full.project(18000,"observation",state,trace).get("data")==state,"full payload unchanged");
        StringWriter text=new StringWriter();BufferedWriter writer=new BufferedWriter(text);full.write(writer,7,"error","{\"reason\":\"中文\"}",null);writer.flush();
        check(text.toString().equals("{\"wallTimeMs\":7,\"event\":\"error\",\"data\":{\"reason\":\"中文\"}}\n"),"full writer exact legacy bytes");
        check(compact.counters().get("droppedEvents").equals(0),"events never dropped");
        System.out.println("COMPACT_BATTLE_LOG_OK checks="+checks+" synthetic logging-only preservation, counts, keyframe and input immutability");
        if(args.length>0)replay(args[0]);
    }
    private static void replay(String path)throws Exception{
        CompactBattleLog compact=new CompactBattleLog("compact",30000,15000);long raw=0,output=0,valid=0,malformed=0,causal=0;
        Map<String,long[]> sizes=new TreeMap<String,long[]>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(path),StandardCharsets.UTF_8),1024*1024)){
            for(String line;(line=in.readLine())!=null;){
                int bytes=line.getBytes(StandardCharsets.UTF_8).length+1;raw+=bytes;Map<String,Object> row;
                try{row=obj(Json.parse(line));}catch(RuntimeException error){malformed++;continue;}
                String kind=(String)row.get("event");Map<String,Object> trace=row.get("trace") instanceof Map?obj(row.get("trace")):null;
                Map<String,Object> projected=compact.project(((Number)row.get("wallTimeMs")).longValue(),kind,row.get("data"),trace);
                if(!obj(projected.get("report")).containsKey("omittedFields")){
                    check(expand(projected.get("data"),projected).equals(row.get("data")),"replay data "+kind);
                    if(trace!=null)check(expand(projected.get("trace"),projected).equals(trace),"replay trace "+kind);
                    causal++;
                }
                long size=BattleClient.json(projected).getBytes(StandardCharsets.UTF_8).length+1;output+=size;valid++;
                long[] totals=sizes.get(kind);if(totals==null){totals=new long[3];sizes.put(kind,totals);}totals[0]++;totals[1]+=bytes;totals[2]+=size;
            }
        }
        System.out.println(BattleClient.json(map("source",path,"rawBytesIncludingInferredFinalNewline",raw,"validRecords",valid,"malformedRecords",malformed,
            "compactProjectedBytes",output,"ratio",(double)output/raw,"recordsWithExactExpandedDataAndTrace",causal,"counters",compact.counters(),"eventSizesCountRawCompact",sizesToMap(sizes),
            "boundary","OFFLINE_REPLAY_SIZE_AND_FORMAT_ONLY_NO_NEW_NATIVE_OR_DESKTOP_ACCEPTANCE")));
    }
    private static Map<String,Object> sizesToMap(Map<String,long[]> data){Map<String,Object> out=new TreeMap<String,Object>();for(String key:data.keySet()){long[] v=data.get(key);out.put(key,Arrays.asList(v[0],v[1],v[2]));}return out;}
}
