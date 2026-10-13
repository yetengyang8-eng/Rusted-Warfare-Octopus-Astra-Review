package io.rwagent.client;

/** A builder's paid travel/own-position commitment, never an assertion of hidden safety.
 * The host must still obtain current occupancy, threat, native path and build/price evidence. */
public final class ProspectCommitment {
    public static final double CLUSTER_RADIUS=600,ARRIVAL_RADIUS=75;
    private long tile=-1,acceptedFrame=-1,startedAt=-1,arrivedAt=-1;
    private double x,y,moveX,moveY,travel;
    private int emptyObservations;

    /** Call only for an actual accepted move receipt; acceptance is not arrival. */
    public void accepted(long tile,double x,double y,double fromX,double fromY,long frame,long now){
        if(tile<0||frame<0||now<0||!finite(x,y,fromX,fromY))throw new IllegalArgumentException("Known prospect and actual move receipt required");
        // Completing another nearby approach spends the same local opportunity. Keep the original
        // arrived cluster anchor, so a chain of 600-unit moves cannot gradually drift across map.
        if(local(x,y)){moveX=x;moveY=y;acceptedFrame=frame;startedAt=now;emptyObservations=0;return;}
        this.tile=tile;this.x=x;this.y=y;acceptedFrame=frame;startedAt=now;
        moveX=x;moveY=y;
        travel=Math.hypot(fromX-x,fromY-y);arrivedAt=-1;emptyObservations=0;
    }
    /** Only a later own native position can establish arrival. */
    public boolean observeArrival(double ownX,double ownY,long frame,long now){
        if(tile<0||!finite(ownX,ownY)||frame<=acceptedFrame||now<startedAt)return false;
        if(Math.hypot(ownX-moveX,ownY-moveY)>ARRIVAL_RADIUS)return false;
        if(arrivedAt<0)arrivedAt=now;return true;
    }
    public boolean arrived(){return tile>=0&&arrivedAt>=0;}
    public long tile(){return tile;}
    public double x(){return x;}
    public double y(){return y;}
    public double paidTravel(){return travel;}
    /** Local preference is geometric. It does not mean the candidate is safe, empty or reachable. */
    public boolean local(double targetX,double targetY){return arrived()&&finite(targetX,targetY)&&Math.hypot(targetX-x,targetY-y)<=CLUSTER_RADIUS;}
    /** A native/current local backlog keeps the commitment. Two fresh empty assessments release it.
     * Unknown observations must not be passed as complete. Immediate danger is a host release. */
    public boolean assessLocalBacklog(boolean complete,boolean hasLawfulCandidate){
        if(!arrived()||!complete)return arrived();
        if(hasLawfulCandidate)emptyObservations=0;else emptyObservations++;
        if(emptyObservations>=2)clear();return arrived();
    }
    public void clear(){tile=-1;acceptedFrame=startedAt=arrivedAt=-1;emptyObservations=0;travel=0;}
    private static boolean finite(double... values){for(double value:values)if(!Double.isFinite(value))return false;return true;}
}
