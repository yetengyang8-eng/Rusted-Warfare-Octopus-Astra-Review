package io.rwagent.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static io.rwagent.client.BattleClient.*;
import static io.rwagent.client.StrategyDirector.*;

/** Package-access adapter to the real Air controller. Every observation/order uses native HTTP. */
public final class R15AirNativeCompanion implements AutoCloseable {
    public final CommandArbiter arbiter=new CommandArbiter();
    public final List<Map<String,Object>> commands=new ArrayList<Map<String,Object>>();
    private final String base;
    private final AirGeneralController air;
    private Map<String,Object> state;
    private int requests;
    private final StrategyDirector.Host host=new StrategyDirector.Host(){
        public String landRouteDemandStrategy(){return "NONE";}
        public Map<String,Object> readStrategy(String path,String event)throws Exception{return read(path);}
        public Map<String,Object> orderStrategy(String owner,String path)throws Exception{return orderStrategy(owner,path,Collections.<String,Object>emptyMap());}
        public Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context)throws Exception{
            String field=path.contains("unitIds=")?"unitIds":"unitId";
            for(String id:path.split(field+"=")[1].split("&")[0].split(","))
                if(!arbiter.owns(owner,Long.parseLong(id)))throw new AssertionError("native fixture attempted cross-owner command");
            Map<String,Object> result=request("POST",path+"&sessionId="+arbiter.stamp().session+"&requestId=r15-native-"+(++requests));
            commands.add(map("owner",owner,"path",path,"context",context,"receipt",result));return result;
        }
        public void emitStrategy(String event,Map<String,Object> data){System.out.println("AIR_EVENT "+event+" "+data);}
        public void spendStrategy(String category,long cost,String type,long actor){throw new AssertionError("clearance fixture must not spend");}
        public void strategicAttack(Map<String,Object> receipt){}
    };
    public R15AirNativeCompanion(String base)throws Exception{this.base=base;arbiter.configureIndependentActors();air=new AirGeneralController(host,arbiter);sample();}
    public Map<String,Object> read(String path)throws Exception{return request("GET",path);}
    @SuppressWarnings("unchecked") private Map<String,Object> request(String method,String path)throws Exception{
        HttpURLConnection connection=(HttpURLConnection)new URL(base+path).openConnection();connection.setRequestMethod(method);connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
        if("POST".equals(method)){connection.setDoOutput(true);connection.setFixedLengthStreamingMode(0);connection.getOutputStream().close();}
        int status=connection.getResponseCode();InputStream input=status>=400?connection.getErrorStream():connection.getInputStream();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);input.close();connection.disconnect();
        String text=new String(bytes.toByteArray(),StandardCharsets.UTF_8);if(status!=200)throw new AssertionError(method+" "+path+" native status="+status+" "+text);
        return (Map<String,Object>)Json.parse(text);
    }
    public void sample()throws Exception{
        state=read("/state");Map<String,Object> combat=read("/combat/observe"),scout=read("/scout/observe");List<Long> own=new ArrayList<Long>();
        for(Map<String,Object> actor:items(state,"ownUnits"))own.add(id(actor));
        arbiter.observe(new CommandArbiter.Stamp(String.valueOf(state.get("sessionId")),String.valueOf(obj(state.get("player")).get("id")),(long)n(state,"frame"),(long)n(state,"gameTimeMs")),own);
        air.observe(state,combat,scout,100,100);
    }
    public void accept(long id)throws Exception{String prior="strategy:native:"+id;
        if(!arbiter.claim(prior,id)||!air.accept(id,prior,new CapabilityUnitMode(id)))throw new AssertionError("actual idle specialist lease did not transfer");}
    public void act()throws Exception{air.act();}
    public Map<String,Object> snapshot(){return air.snapshot();}
    public Map<String,Object> actor(long id){return find(state,id);}
    public void close()throws Exception{air.close();}
}
