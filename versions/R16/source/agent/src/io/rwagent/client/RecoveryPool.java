package io.rwagent.client;

import java.util.*;

/** Bounded ordinary wounded pool. Only later current own HP can release its real owner.
 * Repair bays are current own observations; approach receipts never claim actual healing. */
public final class RecoveryPool {
    public static final double ENTER=.25, RELEASE=.75;
    private final GeneralRegistry registry;
    private final Map<Long,Long> entered=new HashMap<Long,Long>(),accepted=new HashMap<Long,Long>(),generations=new HashMap<Long,Long>();
    private final Map<Long,RecoveryOrder> orders=new HashMap<Long,RecoveryOrder>();
    private final Map<Long,String> rejectedOwnEvidence=new HashMap<Long,String>();
    private static final class RecoveryOrder {
        final long generation,frame;final double x,y;final boolean bay;
        RecoveryOrder(long generation,long frame,double x,double y,boolean bay){this.generation=generation;this.frame=frame;this.x=x;this.y=y;this.bay=bay;}
    }
    private int count,targetBays;private double missingHp;
    public RecoveryPool(GeneralRegistry registry){this.registry=registry;}
    public boolean reconcile(Map<String,Object> state){
        if(!registry.currentOwnState(state))return false;long frame=registry.observation().frame;boolean changed=false;
        for(GeneralRegistry.UnitView unit:registry.units()){
            Map<String,Object> own=find(state,unit.unitId);if(!ready(own)||unit.externalOwner!=null)continue;
            double fraction=n(own,"hp")/n(own,"maxHp");if(!Double.isFinite(fraction))continue;
            if(fraction<ENTER&&!"force:recovery".equals(unit.owner)){
                if(registry.enterRecovery(unit.unitId)){entered.put(unit.unitId,frame);accepted.remove(unit.unitId);generations.remove(unit.unitId);orders.remove(unit.unitId);rejectedOwnEvidence.remove(unit.unitId);changed=true;}
            }else if("force:recovery".equals(unit.owner)&&fraction>=RELEASE&&frame>entered.getOrDefault(unit.unitId,frame)){
                if(registry.finishRecovery(unit.unitId)){entered.remove(unit.unitId);accepted.remove(unit.unitId);generations.remove(unit.unitId);orders.remove(unit.unitId);rejectedOwnEvidence.remove(unit.unitId);changed=true;}
            }
        }
        count=0;missingHp=0;Set<Long> live=new HashSet<Long>();
        for(GeneralRegistry.UnitView unit:registry.units()){live.add(unit.unitId);Map<String,Object> own=find(state,unit.unitId);
            if("force:recovery".equals(unit.owner)&&ready(own)){count++;missingHp+=Math.max(0,n(own,"maxHp")-n(own,"hp"));}}
        entered.keySet().retainAll(live);accepted.keySet().retainAll(live);generations.keySet().retainAll(live);
        orders.keySet().retainAll(live);rejectedOwnEvidence.keySet().retainAll(live);
        // A planning service window, not a promise of native repair throughput or a hard 8-unit rule.
        targetBays=count<4&&missingHp<1200?0:Math.max((count+7)/8,(int)Math.ceil(missingHp/1440.0));return changed;
    }
    public int targetBays(){return targetBays;}
    public Map<String,Object> snapshot(){Map<String,Object> out=new LinkedHashMap<String,Object>();out.put("recoveringCount",count);out.put("missingOwnHp",missingHp);
        out.put("repairBayTarget",targetBays);out.put("entryHealthRatio",ENTER);out.put("releaseHealthRatio",RELEASE);
        out.put("capacityStatus","POLICY_TARGET_NATIVE_SERVICE_RATE_NEEDS_EVIDENCE");out.put("repairExecution","CURRENT_HP_WITNESS_REQUIRED");return out;}
    public void collect(Map<String,Object> state,double homeX,double homeY,ForceController.Host host)throws Exception{
        if(!registry.currentOwnState(state))return;final long now=registry.observation().gameTimeMs,frame=registry.observation().frame;
        for(final GeneralRegistry.UnitView unit:registry.units()){
            if(!"force:recovery".equals(unit.owner))continue;Map<String,Object> own=find(state,unit.unitId);if(!ready(own))continue;
            double x=homeX,y=homeY,best=Double.POSITIVE_INFINITY;boolean bay=false;
            for(Map<String,Object> candidate:items(state))if("repairbay".equals(candidate.get("type"))&&ready(candidate)){
                double distance=Math.hypot(n(own,"x")-n(candidate,"x"),n(own,"y")-n(candidate,"y"));
                if(distance<best){best=distance;x=n(candidate,"x");y=n(candidate,"y");bay=true;}}
            if(!Double.isFinite(x)||!Double.isFinite(y))continue;
            final String ownEvidence=evidence(unit,own)+":"+bay+":"+x+":"+y;
            if(ownEvidence.equals(rejectedOwnEvidence.get(unit.unitId)))continue;
            RecoveryOrder previous=orders.get(unit.unitId);
            boolean same=previous!=null&&previous.generation==unit.ownerGeneration&&previous.bay==bay&&Math.hypot(previous.x-x,previous.y-y)<1;
            Object nativeOrder=own.get("orderType");
            boolean attacking="attackMove".equals(nativeOrder)||"attack".equals(nativeOrder);
            if(same&&frame>previous.frame&&"move".equals(nativeOrder)&&Math.hypot(n(own,"orderX")-x,n(own,"orderY")-y)<1)continue;
            // A missing repair facility cannot turn an ordinary waiting pool into an
            // endless critical command source. One withdrawal is enough; a real bay
            // or a conflicting attack order changes the destination/task evidence.
            if(same&&!bay&&!attacking)continue;
            if(Math.hypot(n(own,"x")-x,n(own,"y")-y)<=80&&Objects.equals(generations.get(unit.unitId),unit.ownerGeneration)
                    &&!"attackMove".equals(own.get("orderType"))&&!"attack".equals(own.get("orderType")))continue;
            if(Objects.equals(generations.get(unit.unitId),unit.ownerGeneration)&&now-accepted.getOrDefault(unit.unitId,-100000L)<8000)continue;
            final double destinationX=x,destinationY=y;final boolean actualBay=bay;final String reason=bay?"CURRENT_OWN_REPAIR_BAY_APPROACH_NOT_HEALED":"HOME_RECOVERY_WAIT_NATIVE_REPAIR_CAPACITY_MISSING";
            boolean withdrawal=previous==null||previous.generation!=unit.ownerGeneration||attacking;
            host.collect(new ForceController.Proposal(unit.owner,Collections.singletonList(unit.unitId),"/command/move?unitId="+unit.unitId+"&x="+x+"&y="+y,withdrawal?"CRITICAL_RETREAT":"RECOVERY",withdrawal?90:35,reason,
                receipt->{GeneralRegistry.UnitView current=registry.unit(unit.unitId);Long receiptFrame=GameClock.number(receipt.get("frame"));
                    if(current!=null&&current.ownerGeneration==unit.ownerGeneration&&unit.owner.equals(current.owner)&&receiptFrame!=null&&receiptFrame>=frame){accepted.put(unit.unitId,now);generations.put(unit.unitId,unit.ownerGeneration);
                        orders.put(unit.unitId,new RecoveryOrder(unit.ownerGeneration,receiptFrame,destinationX,destinationY,actualBay));rejectedOwnEvidence.remove(unit.unitId);}},
                receipt->{if(Integer.valueOf(409).equals(receipt.get("nativeHttpStatus")))rejectedOwnEvidence.put(unit.unitId,ownEvidence);}));
        }
    }
    private static String evidence(GeneralRegistry.UnitView unit,Map<String,Object> own){return unit.ownerGeneration+":"+own.get("hp")+":"+own.get("x")+":"+own.get("y")+":"+own.get("orderType")+":"+own.get("orderX")+":"+own.get("orderY");}
    private static boolean ready(Map<String,Object> u){return u!=null&&n(u,"hp")>0&&n(u,"maxHp")>0&&!Boolean.TRUE.equals(u.get("dead"))&&n(u,"buildProgress")>=1&&Double.isFinite(n(u,"x"))&&Double.isFinite(n(u,"y"));}
    private static double n(Map<String,Object> u,String k){Object v=u.get(k);return v instanceof Number?((Number)v).doubleValue():Double.NaN;}
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> items(Map<String,Object> state){return state.get("ownUnits") instanceof List?(List<Map<String,Object>>)state.get("ownUnits"):Collections.emptyList();}
    private static Map<String,Object> find(Map<String,Object> state,long id){for(Map<String,Object> u:items(state))if(Objects.equals(GameClock.number(u.get("id")),Long.valueOf(id)))return u;return null;}
}
