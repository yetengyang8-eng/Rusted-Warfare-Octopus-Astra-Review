package analysis;
import com.corrodinggames.rts.game.i;
import com.corrodinggames.rts.game.n;
import com.corrodinggames.rts.game.units.am;
import java.io.*;
import java.util.*;
public final class ReplayProbeSampler {
 private static PrintWriter units,players;
 private static long next=0,last=0,count=0;
 private static boolean playing=false,broken=false;
 public static void onFrame(i engine) {
  try {
   if(broken||engine==null||engine.cb==null)return;
   if(!engine.cb.j()){
    if(playing){playing=false;flush();System.err.println("[RWPROBE] replay stopped at "+last);}
    return;
   }
   if(units==null)open();
   long ms=engine.by;
   if(ms<last){next=0;System.err.println("[RWPROBE] replay restarted");}
   last=ms;playing=true;
   if(ms<next)return;
   next=(ms/30000+1)*30000;
   record(engine,ms);
   flush();
   if(ms%300000<31000)System.err.println("[RWPROBE] sampled "+ms+"ms frame="+engine.bx+" units="+count);
  }catch(Throwable t){broken=true;System.err.println("[RWPROBE] sample FAILED "+t);t.printStackTrace();flush();}
 }
 private static void open()throws IOException {
  File dir=new File(System.getProperty("rwprobe.out","replay_probe_output"));
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create "+dir);
  units=new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dir,"units_30s.csv")),"UTF-8"),262144));
  players=new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dir,"players_30s.csv")),"UTF-8"),16384));
  units.println("gameTimeMs,frame,slot,team,type,id,x,y,hp,maxhp,cost,building,attack");
  players.println("gameTimeMs,frame,slot,team,name,credits,income,rawIncome,defeated,wiped");
  Runtime.getRuntime().addShutdownHook(new Thread(()->{flush();units.close();players.close();}));
  System.err.println("[RWPROBE] initialized "+dir.getAbsolutePath());
 }
 private static String type(am u){try{return u.r()==null?"unknown":u.r().i();}catch(Throwable t){return u.getClass().getSimpleName();}}
 private static int cost(am u){try{return u.r()==null?0:Math.max(0,u.r().c());}catch(Throwable t){return 0;}}
 private static void record(i e,long ms){
  am[] all=am.bE.a();int sz=am.bE.size();
  for(int j=0;j<sz;j++) {
   am u=all[j];if(u==null||u.bX==null||u.ej||u.bV)continue;
   int id=0;try{id=(int)u.eh;}catch(Throwable t){}
   units.printf(Locale.ROOT,"%d,%d,%d,%d,%s,%d,%.1f,%.1f,%.1f,%.1f,%d,%s,%s%n",ms,e.bx,u.bX.k,u.bX.r,type(u),id,(double)u.eo,(double)u.ep,(double)u.cu,(double)u.cv,cost(u),u.bI(),u.l());
   count++;
  }
  for(n p:n.d()) if(p!=null)
   players.printf(Locale.ROOT,"%d,%d,%d,%d,%s,%.1f,%d,%d,%s,%s%n",ms,e.bx,p.k,p.r,(p.v==null?"":p.v.replace(',',';')),p.o,p.v(),p.u(),p.F,p.G);
 }
 private static void flush(){if(units!=null)units.flush();if(players!=null)players.flush();}
}
