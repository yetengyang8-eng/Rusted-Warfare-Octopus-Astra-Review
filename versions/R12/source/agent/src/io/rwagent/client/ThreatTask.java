package io.rwagent.client;

import java.util.*;

/** Commander attention only. CLEAR means visibility disappeared, never a proven kill. */
public final class ThreatTask {
    public enum Status {CURRENT,UNKNOWN,CLEAR}
    public final long id;
    public final Set<Long> enemyIds;
    public final double x,y;
    public final long sourceFrame,sourceGameTimeMs;
    public final Status status;
    public final String reason;
    /** Current legal roles establish a mobile response task; unknown/static roles stay strategic. */
    public final boolean smallResponseRoleKnown;
    /** Durability allocation budget only; never a weapon, range or FightEstimate claim. */
    public final double demandStrength,localCoveredStrength,assignedStrength,deficitStrength;
    public final Set<Long> assignedGeneralIds,localResponderIds;
    public final List<Map<String,Object>> allocationDecisions;
    ThreatTask(long id,Collection<Long> enemies,double x,double y,long frame,long time,Status status,String reason){
        this(id,enemies,x,y,frame,time,status,reason,false,0,0,0,Collections.<Long>emptySet(),Collections.<Long>emptySet(),Collections.<Map<String,Object>>emptyList());
    }
    ThreatTask(long id,Collection<Long> enemies,double x,double y,long frame,long time,Status status,String reason,double demand){
        this(id,enemies,x,y,frame,time,status,reason,false,demand,0,0,Collections.<Long>emptySet(),Collections.<Long>emptySet(),Collections.<Map<String,Object>>emptyList());
    }
    ThreatTask(long id,Collection<Long> enemies,double x,double y,long frame,long time,Status status,String reason,double demand,boolean mobileRoleKnown){
        this(id,enemies,x,y,frame,time,status,reason,mobileRoleKnown,demand,0,0,Collections.<Long>emptySet(),Collections.<Long>emptySet(),Collections.<Map<String,Object>>emptyList());
    }
    private ThreatTask(long id,Collection<Long> enemies,double x,double y,long frame,long time,Status status,String reason,
            boolean mobileRoleKnown,double demand,double local,double assigned,Collection<Long> generals,Collection<Long> responders,List<Map<String,Object>> decisions){
        this.id=id;enemyIds=Collections.unmodifiableSet(new LinkedHashSet<Long>(enemies));this.x=x;this.y=y;
        sourceFrame=frame;sourceGameTimeMs=time;this.status=status;this.reason=reason;
        smallResponseRoleKnown=mobileRoleKnown;
        demandStrength=demand;localCoveredStrength=local;assignedStrength=assigned;deficitStrength=Math.max(0,demand-local-assigned);
        assignedGeneralIds=Collections.unmodifiableSet(new LinkedHashSet<Long>(generals));localResponderIds=Collections.unmodifiableSet(new LinkedHashSet<Long>(responders));
        List<Map<String,Object>> copy=new ArrayList<Map<String,Object>>();for(Map<String,Object> decision:decisions)copy.add(Collections.unmodifiableMap(new LinkedHashMap<String,Object>(decision)));
        allocationDecisions=Collections.unmodifiableList(copy);
    }
    ThreatTask withStatus(Status value,String why){return new ThreatTask(id,enemyIds,x,y,sourceFrame,sourceGameTimeMs,value,why,demandStrength,smallResponseRoleKnown);}
    ThreatTask withBudget(double local,double assigned,Collection<Long> generals,Collection<Long> responders,List<Map<String,Object>> decisions){
        return new ThreatTask(id,enemyIds,x,y,sourceFrame,sourceGameTimeMs,status,reason,smallResponseRoleKnown,demandStrength,local,assigned,generals,responders,decisions);
    }
    public Long primaryEnemyId(){return status==Status.CURRENT&&!enemyIds.isEmpty()?enemyIds.iterator().next():null;}
    public Map<String,Object> metadata(){Map<String,Object> result=new LinkedHashMap<String,Object>();
        result.put("taskId",id);result.put("enemyIds",new ArrayList<Long>(enemyIds));result.put("status",status.name());result.put("reason",reason);
        result.put("smallResponseRoleKnown",smallResponseRoleKnown);
        result.put("x",status==Status.CURRENT?x:null);result.put("y",status==Status.CURRENT?y:null);
        result.put("strengthEstimate","CURRENT_HP_ALLOCATION_PROXY_NOT_FIGHT_ESTIMATE");result.put("demandKnown",status==Status.CURRENT);
        result.put("demand",status==Status.CURRENT?demandStrength:null);result.put("localCovered",status==Status.CURRENT?localCoveredStrength:null);
        result.put("assigned",status==Status.CURRENT?assignedStrength:null);result.put("deficit",status==Status.CURRENT?deficitStrength:null);
        result.put("assignedGeneralIds",new ArrayList<Long>(assignedGeneralIds));result.put("localResponderIds",new ArrayList<Long>(localResponderIds));
        result.put("allocationDecisions",allocationDecisions);
        result.put("sourceFrame",sourceFrame);result.put("sourceGameTimeMs",sourceGameTimeMs);result.put("killClaim",false);return result;}
}
