package io.rwagent.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/** Output-only projection. It never controls reads, decisions, tokens or native commands.
 * Full is the compatibility default; human launchers explicitly select compact.
 */
final class CompactBattleLog {
    static final String SCHEMA="rw-battle-compact-v1";
    private final boolean compact;
    private final long gameInterval,wallInterval;
    private final Map<String,long[]> keyframes=new HashMap<String,long[]>();
    private final Map<String,Long> projected=new TreeMap<String,Long>();
    private long records,projections,references;
    CompactBattleLog(){this(System.getProperty("rwagent.reportMode","full"),30000,15000);}
    CompactBattleLog(String mode,long gameInterval,long wallInterval){
        if(!"compact".equalsIgnoreCase(mode)&&!"full".equalsIgnoreCase(mode))throw new IllegalArgumentException("rwagent.reportMode must be compact or full");
        this.compact="compact".equalsIgnoreCase(mode);this.gameInterval=gameInterval;this.wallInterval=wallInterval;
    }
    boolean isCompact(){return compact;}
    Map<String,Object> configuration(){return map("schema",SCHEMA,"mode",compact?"compact":"full","keyframeIntervalGameMs",gameInterval,
        "keyframeIntervalWallMs",wallInterval,"projectionScope","CONTINUOUS_OBSERVATIONS_AND_FORCE_SNAPSHOTS_ONLY",
        "causalEvents","ALL_COMMAND_ATTEMPTS_RECEIPTS_WITNESSES_TRANSITIONS_ERRORS_RETAINED",
        "referenceSyntax","$rwReportRef is a same-record JSON pointer; dereference before legacy analysis",
        "snapshotSemantics","PROJECTED_FIELDS_UNKNOWN_BETWEEN_KEYFRAMES; NOT_EXACT_EVENT_OR_DEATH_TIME; NO_HIDDEN_STATE_INFERENCE");}
    Map<String,Object> counters(){return map("schema",SCHEMA,"records",records,"projectedRecords",projections,
        "projectedRecordsByEvent",new TreeMap<String,Long>(projected),"sameRecordReferences",references,"droppedEvents",0);}
    void write(BufferedWriter writer,long wall,String kind,String dataJson,Map<String,Object> trace)throws IOException{
        if(!compact){writer.write("{\"wallTimeMs\":"+wall+",\"event\":"+Json.quote(kind)+",\"data\":"+dataJson+(trace==null?"":",\"trace\":"+BattleClient.json(trace))+"}\n");records++;return;}
        Object value;
        try{value=Json.parse(dataJson);}catch(RuntimeException malformed){
            // Preserve original invalid JSON/error ordering rather than changing controller behavior.
            writer.write("{\"wallTimeMs\":"+wall+",\"event\":"+Json.quote(kind)+",\"data\":"+dataJson+(trace==null?"":",\"trace\":"+BattleClient.json(trace))+"}\n");records++;return;
        }
        writer.write(BattleClient.json(project(wall,kind,value,trace))+"\n");
    }
    Map<String,Object> project(long wall,String kind,Object value,Map<String,Object> trace){
        records++;
        Map<String,Object> row=map("wallTimeMs",wall,"event",kind,"data",value);
        if(!compact){if(trace!=null)row.put("trace",trace);return row;}
        Map<String,Object> data=value instanceof Map?copy(value):null;
        Map<String,Object> note=map("schema",SCHEMA);
        boolean continuous=data!=null&&(isRawObservation(kind,trace)||"g4_force_state".equals(kind)||"g5_runtime_state".equals(kind));
        boolean keyframe=continuous&&keyframe(kind,wall,data,trace);
        if(continuous&&!keyframe){
            Map<String,Object> omitted=new LinkedHashMap<String,Object>();
            if(isRawObservation(kind,trace)){
                for(String key:new ArrayList<String>(data.keySet())){
                    Object field=data.get(key);
                    if(field instanceof List||field instanceof Map&&!Arrays.asList("player","match","map").contains(key))omit(data,key,omitted);
                }
            }else if("g4_force_state".equals(kind)){
                Object units=data.get("units");int free=0;
                if(units instanceof List)for(Object unit:(List<?>)units)if(unit instanceof Map&&"FREE".equals(((Map<?,?>)unit).get("allocation")))free++;
                data.put("free",free);omit(data,"units",omitted);
            }
            if(!omitted.isEmpty()){
                note.put("payload","SUMMARY");note.put("omittedFields",omitted);note.put("omittedFieldsSemantics","COUNTS_ONLY_NO_RECONSTRUCTION_OR_NEGATIVE_EVIDENCE");
                projections++;projected.put(kind,projected.containsKey(kind)?projected.get(kind)+1:1L);
            }
        }
        if(continuous)note.put("keyframe",keyframe);
        row.put("data",data==null?value:data);
        if(trace!=null){
            // Only exact duplicates become references. Unique command context, receipts and legal
            // observation metadata remain present; references never leave this JSONL record.
            Map<String,Object> t=copy(trace);
            Object context=t.get("context");
            if(value instanceof Map&&sameJson(context,value)){t.put("context",ref("#/data"));}
            else if(context instanceof Map&&data!=null)t.put("context",deduplicate(copy(context),data,"#/data"));
            if(t.get("command") instanceof Map&&data!=null)t.put("command",deduplicateCommand(copy(t.get("command")),data));
            Object freshness=data==null?null:data.get("sourceFreshness");
            if(freshness instanceof Map&&sameJson(((Map<?,?>)freshness).get("observation"),t.get("observation"))){
                Map<String,Object> fresh=copy(freshness);fresh.put("observation",ref("#/trace/observation"));data.put("sourceFreshness",fresh);
            }
            row.put("trace",t);
        }
        row.put("report",note);return row;
    }
    private Map<String,Object> deduplicate(Map<String,Object> context,Map<String,Object> data,String base){
        for(String key:new ArrayList<String>(context.keySet()))if(data.containsKey(key)&&sameJson(context.get(key),data.get(key))){
            Object v=context.get(key);
            if((v instanceof Map||v instanceof List||v instanceof String)&&BattleClient.json(v).length()>128)context.put(key,ref(base+"/"+key.replace("~","~0").replace("/","~1")));
        }return context;
    }
    private Map<String,Object> deduplicateCommand(Map<String,Object> command,Map<String,Object> data){
        for(String key:new ArrayList<String>(command.keySet())){
            Object v=command.get(key);if(!(v instanceof Map||v instanceof List||v instanceof String)||BattleClient.json(v).length()<=64)continue;
            String alias="actors".equals(key)?"actorIds":"inputObservationIds".equals(key)?"sourceObservationIds":key;
            for(String candidate:Arrays.asList(key,alias))if(data.containsKey(candidate)&&sameJson(v,data.get(candidate))){command.put(key,ref("#/data/"+candidate));break;}
            if(command.get(key)!=v)continue;
            // Some events embed the complete execution intent rather than flattening its fields.
            for(String nested:Arrays.asList("intent","command"))if(data.get(nested) instanceof Map){Map<String,Object> target=copy(data.get(nested));
                for(String candidate:Arrays.asList(key,alias))if(target.containsKey(candidate)&&sameJson(v,target.get(candidate))){command.put(key,ref("#/data/"+nested+"/"+candidate));break;}
                if(command.get(key)!=v)break;
            }
        }return command;
    }
    private Map<String,Object> ref(String path){references++;return map("$rwReportRef",path);}
    /** Parsing a legacy data string produces Longs while sidecars may contain Integers. */
    private static boolean sameJson(Object left,Object right){
        if(left==right)return true;if(left==null||right==null)return false;
        if(left instanceof Number&&right instanceof Number)return left.toString().equals(right.toString());
        if(left instanceof Map&&right instanceof Map){Map<?,?> a=(Map<?,?>)left,b=(Map<?,?>)right;if(!a.keySet().equals(b.keySet()))return false;for(Object key:a.keySet())if(!sameJson(a.get(key),b.get(key)))return false;return true;}
        if(left instanceof List&&right instanceof List){List<?> a=(List<?>)left,b=(List<?>)right;if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(!sameJson(a.get(i),b.get(i)))return false;return true;}
        return left.equals(right);
    }
    private boolean keyframe(String kind,long wall,Map<String,Object> data,Map<String,Object> trace){
        Map<String,Object> observation=trace!=null&&trace.get("observation") instanceof Map?copy(trace.get("observation")):Collections.<String,Object>emptyMap();
        String scope=kind+"|"+observation.get("requestPath")+"|"+observation.get("sourceSessionId")+"|"+observation.get("sourcePlayerId");
        Long game=GameClock.number(data.containsKey("gameTimeMs")?data.get("gameTimeMs"):data.get("sourceGameTimeMs"));
        if(game==null)game=GameClock.number(observation.get("sourceGameTimeMs"));
        long g=game==null?-1:game;long[] last=keyframes.get(scope);
        boolean emit=last==null||wall<last[1]||wall-last[1]>=wallInterval||g>=0&&last[0]>=0&&(g<last[0]||g-last[0]>=gameInterval)
            ||observation.get("discontinuities") instanceof List&&!((List<?>)observation.get("discontinuities")).isEmpty()&&!((List<?>)observation.get("discontinuities")).equals(Collections.singletonList("REPEATED_SOURCE_STAMP"));
        if(emit){if(keyframes.size()>128)keyframes.clear();keyframes.put(scope,new long[]{g,wall});}return emit;
    }
    private static boolean isRawObservation(String kind,Map<String,Object> trace){return !kind.startsWith("g1_")&&!kind.startsWith("g2_")&&trace!=null&&"OBSERVATION".equals(trace.get("phase"));}
    private static void omit(Map<String,Object> data,String key,Map<String,Object> omitted){
        if(!data.containsKey(key))return;Object v=data.remove(key);
        omitted.put(key,map("status","OMITTED_CONTINUOUS_PAYLOAD","entries",v instanceof Collection?((Collection<?>)v).size():v instanceof Map?((Map<?,?>)v).size():null));
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> copy(Object value){return new LinkedHashMap<String,Object>((Map<String,Object>)value);}
    private static Map<String,Object> map(Object... pairs){Map<String,Object> m=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
}
