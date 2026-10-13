package io.rwagent.bootstrap;

import java.lang.reflect.Method;

/** Exact known-circle edge geometry used only by the recon planner overload. */
public final class R11ScoutBridgeSafetyHarness {
    static int checks;
    static final Method safe;
    static {try{safe=ScoutBridge.VisibleGrid.class.getDeclaredMethod("safeReconEdge",double.class,double.class,double.class,double.class,ScoutBridge.Threat.class);safe.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static boolean edge(double sx,double sy,double x,double y,ScoutBridge.Threat threat)throws Exception{return (Boolean)safe.invoke(null,sx,sy,x,y,threat);}
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        ScoutBridge.Threat threat=new ScoutBridge.Threat(1,0,0,0,false,"tank",0);
        check(!edge(-10,139.8,10,139.8,threat),"outside endpoints cannot conceal circle crossing between recon route cells");
        check(edge(-10,140.2,10,140.2,threat),"outside safe recon edge remains reachable");
        check(!edge(150,0,130,0,threat),"inward edge entering known caution rejected");
        check(edge(100,0,120,0,threat),"strictly outward escape from current danger allowed");
        check(!edge(100,0,100,20,threat),"tangent inside danger is not an escape");
        check(!edge(100,0,80,0,threat),"inward inside danger rejected");
        check(!edge(140,0,140,20,threat),"boundary tangent rejected consistently with client segment guard");
        check(edge(140,0,160,0,threat),"boundary outward edge allowed consistently with client");
        System.out.println("R11_SCOUT_BRIDGE_SAFETY_OK checks="+checks+" geometryFixtureOnly=true");
    }
}
