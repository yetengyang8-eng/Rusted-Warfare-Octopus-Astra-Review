package io.rwagent.client;

import java.util.*;
import java.net.URLEncoder;
import static io.rwagent.client.BattleClient.*;
import static io.rwagent.client.StrategyDirector.*;

/** A typed long-lived air/amphibious formation. Ordinary ground membership is untouched.
 * It receives only idle specialist leases; each native mode and arrival has its own witness.
 * Search is information collection, never permission to fire at an old contact. */
final class AirGeneralController {
    static final String OWNER="air-general:1";
    private final StrategyDirector.Host host;
    private final CommandArbiter arbiter;
    private final Map<Long,Member> members=new LinkedHashMap<Long,Member>();
    private final Set<Long> avoided=new LinkedHashSet<Long>();
    private Map<String,Object> state,contacts,vision;
    private long now,target=-1,legFrame=-1,legAcceptedFrame=-1;
    private double homeX,homeY,legX,legY;
    private boolean legKnown;
    private List<Long> searchRoute=Collections.emptyList();
    private int routeCursor,legRouteEnd=-1;
    private long routeLeader=-1,routeFrontier=-1,routePlanFrame=-1;
    private SearchAreaNeed searchNeed;
    private int legs,attacks,transfers;
    private static final class Member {
        final long id,joinedFrame;final CapabilityUnitMode mode;
        boolean attached,recovering;
        long modeReceiptFrame=-1;
        double destinationX=Double.NaN,destinationY=Double.NaN;
        String orderKind="";
        long orderFrame=-1;
        Member(long id,long frame,CapabilityUnitMode mode){this.id=id;joinedFrame=frame;this.mode=mode;}
    }
    AirGeneralController(StrategyDirector.Host host,CommandArbiter arbiter){this.host=host;this.arbiter=arbiter;}
    boolean accept(long id,String from,CapabilityUnitMode mode)throws Exception{
        if(members.containsKey(id))return true;
        if(mode==null||mode.unitId!=id||!arbiter.transfer(from,OWNER,id))return false;
        members.put(id,new Member(id,arbiter.stamp().frame,mode));transfers++;
        emit("air_general_member_transition",map("unitId",id,"oldOwner",from,"newOwner",OWNER,
            "ownerGeneration",arbiter.ownerGeneration(id),"oldMembership","SPECIALIST_POOL_IDLE",
            "newMembership","JOINING","reason","LONG_LIVED_AIR_SEARCH_FORMATION"));
        return true;
    }
    int size(){return members.size();}
    void observe(Map<String,Object> state,Map<String,Object> contacts,Map<String,Object> vision,double hx,double hy)throws Exception{
        this.state=state;this.contacts=contacts;this.vision=vision;homeX=hx;homeY=hy;now=(long)n(state,"gameTimeMs");
        if(!arbiter.stamp().session.equals(state.get("sessionId")))return;
        double cx=0,cy=0;int attached=0;
        for(Member m:members.values())if(m.attached&&!m.recovering){Map<String,Object> a=find(state,m.id);if(a!=null){cx+=n(a,"x");cy+=n(a,"y");attached++;}}
        cx=attached>0?cx/attached:homeX;cy=attached>0?cy/attached:homeY;
        Iterator<Member> it=members.values().iterator();
        while(it.hasNext()){
            Member m=it.next();Map<String,Object> a=find(state,m.id);
            if(a==null||!ready(a)||!arbiter.owns(OWNER,m.id)){
                arbiter.releaseActor(OWNER,m.id);it.remove();
                emit("air_general_member_transition",map("unitId",m.id,"oldOwner",OWNER,"newOwner",null,
                    "ownerGeneration",arbiter.ownerGeneration(m.id),"reason","NOT_CURRENTLY_AVAILABLE_NOT_DEATH_PROOF"));continue;
            }
            double fraction=n(a,"maxHp")>0?n(a,"hp")/n(a,"maxHp"):0;
            if(!m.recovering&&fraction<.25){m.recovering=true;invalidate(m);emit("air_general_member_transition",map("unitId",m.id,"oldHealthRole","NORMAL","newHealthRole","RECOVERING","reason","CURRENT_OWN_CRITICAL_HP"));}
            else if(m.recovering&&fraction>=.75){m.recovering=false;invalidate(m);emit("air_general_member_transition",map("unitId",m.id,"oldHealthRole","RECOVERING","newHealthRole","NORMAL","reason","LATER_OWN_HP_RECOVERY"));}
            if(!m.attached&&arbiter.stamp().frame>m.joinedFrame&&distance(a,cx,cy)<LocalArmyDirector.FORM_RADIUS){
                m.attached=true;invalidate(m);emit("air_general_member_transition",map("unitId",m.id,"oldMembership","JOINING","newMembership","ATTACHED",
                    "reason","LATER_OWN_POSITION_WITNESS","sourceFrame",state.get("frame")));
            }
        }
        if(legKnown&&(!members.containsKey(routeLeader)||members.get(routeLeader).recovering)){
            for(Member m:members.values())if(m.attached&&!m.recovering){routeLeader=m.id;break;}
        }
        if(target>=0&&current(target)==null){emit("air_general_target_clear",map("targetId",target,"reason","ABSENT_FROM_CURRENT_LEGAL_VISIBILITY","clearedIsKilled",false));target=-1;clearSearchRoute();}
        if(legKnown&&arbiter.stamp().frame>legAcceptedFrame){
            for(Member m:members.values())if(m.attached&&!m.recovering){Map<String,Object> a=find(state,m.id);
                if(a!=null&&m.id==routeLeader&&"move".equals(m.orderKind)
                        &&arbiter.stamp().frame>m.orderFrame&&Math.hypot(m.destinationX-legX,m.destinationY-legY)<1
                        &&distance(a,legX,legY)<80){
                    legKnown=false;routeCursor=Math.max(routeCursor,legRouteEnd);legs++;
                    emit("air_general_search_leg_witness",map("unitId",m.id,"x",legX,"y",legY,"sourceFrame",state.get("frame"),
                        "routeCursor",routeCursor,"routeTiles",searchRoute.size(),"arrivalIsFullCoverage",false));
                    if(searchRoute.isEmpty()||routeCursor>=searchRoute.size()-1){rememberAvoid(routeFrontier);clearSearchRoute();}
                    break;}}
        }
    }
    boolean act()throws Exception{
        if(state==null||members.isEmpty())return false;
        boolean acted=false;
        // Recovery/JOINING have first claim on their own actors; other members keep working.
        for(Member m:members.values()){
            Map<String,Object> a=find(state,m.id);if(a==null)continue;
            if(m.recovering||!m.attached){
                if(ensureAir(m,a))continue;
                double x=homeX,y=homeY;
                if(m.recovering){Map<String,Object> repair=nearestRepair(a);if(repair!=null){x=n(repair,"x");y=n(repair,"y");}}
                else {double[] anchor=centroid();x=anchor[0];y=anchor[1];}
                if(distance(a,x,y)>80&&move(m,a,x,y,m.recovering?"RECOVERY":"JOINING"))acted=true;
            }
        }
        List<Member> force=new ArrayList<Member>();for(Member m:members.values())if(m.attached&&!m.recovering)force.add(m);
        if(force.isEmpty())return acted;
        if(legalContacts()){
            if(current(target)==null){double[] center=centroid();double best=Double.MAX_VALUE;
                for(Map<String,Object> e:items(contacts,"visibleEnemies")){
                    boolean spatialDuty=host.airTargetNeedStrategy(id(e))||"AIR".equals(e.get("targetDomain"))||"SUBMERGED".equals(e.get("targetDomain"))
                        ||"NONE".equals(host.landRouteDemandStrategy())||"ROUTE_UNAVAILABLE".equals(host.landRouteDemandStrategy())||distance(e,center[0],center[1])<600;
                    if(!spatialDuty)continue;
                    double d=distance(e,center[0],center[1]);if(d<best){best=d;target=id(e);}}}
            Map<String,Object> e=current(target);
            if(e!=null){if(engage(force,e))return true;}
        }
        // No remembered target can suspend the long-lived exploration mission.
        List<Member> airborne=new ArrayList<Member>();
        for(Member m:force){Map<String,Object> a=find(state,m.id);if(a==null)continue;
            if(ensureAir(m,a)){acted=true;continue;}
            if("AIR".equals(m.mode.nativeMode()))airborne.add(m);
        }
        if(airborne.isEmpty())return acted;
        boolean unsafeNativeTravel=false;
        for(Member m:airborne){Map<String,Object> a=find(state,m.id);
            if(unsafeNativeTravel(a))unsafeNativeTravel=true;
        }
        if(unsafeNativeTravel||legKnown&&!safeSegment(find(state,airborne.get(0).id),legX,legY)){
            // Losing a policy leg does not cancel the native waypoint. Stop dangerous old travel
            // once even if the replacement planner has no route; retain ownership and mission.
            for(Member m:airborne){Map<String,Object> a=find(state,m.id);if(unsafeNativeTravel(a))
                if(move(m,a,n(a,"x"),n(a,"y"),"DANGEROUS_OLD_LEG_HOLD_BEFORE_REPLAN"))acted=true;}
            if(legKnown)rememberAvoid();else rememberAvoid(routeFrontier);
            clearSearchRoute();emit("air_general_search_reroute",map("reason","CURRENT_VISIBLE_ATTACK_CORRIDOR","targetX",legX,"targetY",legY));}
        if(!legKnown){
            Member leader=members.get(routeLeader);
            if(leader==null||!airborne.contains(leader))leader=airborne.get(0);
            routeLeader=leader.id;
            Map<String,Object> plan=null;
            if(searchRoute.isEmpty()){
                String path="/scout/plan?unitId="+leader.id+"&role=recon&routeScope=air-search";
                if(!avoided.isEmpty()){StringBuilder q=new StringBuilder();for(Long tile:avoided){if(q.length()>0)q.append(',');q.append(tile);}path+="&avoid="+q;}
                plan=host.readStrategy(path,"air_general_search_plan");
                if(!validPlan(plan,leader.id)){emit("air_general_search_deferred",map("reason","NO_CURRENT_LEGAL_FRONTIER_PLAN","plan",plan));return acted;}
                searchRoute=route(plan);routeCursor=0;routePlanFrame=(long)number(plan,"frame",-1);routeFrontier=(long)number(plan,"frontierTile",-1);
                buildSearchNeed(plan);
            }
            // A route read may occur after the current own sample while the aircraft is still
            // moving. Keep the route and wait for its own position anchor instead of discarding it.
            if(arbiter.stamp().frame<routePlanFrame)return acted;
            RouteLeg leg=searchRoute.isEmpty()?null:safeRouteLeg(find(state,leader.id),searchRoute,routeCursor);
            double[] waypoint=leg==null&&searchRoute.isEmpty()&&plan!=null?safeRouteLeg(find(state,leader.id),plan):leg==null?null:new double[]{leg.x,leg.y};
            if(waypoint==null){rememberAvoid(routeFrontier);clearSearchRoute();emit("air_general_search_deferred",map("reason","NO_SAFE_NATIVE_ROUTE_SEGMENT","plan",plan));return acted;}
            legRouteEnd=leg==null?-1:leg.end;
            legX=waypoint[0];legY=waypoint[1];legKnown=true;legFrame=arbiter.stamp().frame;legAcceptedFrame=Long.MAX_VALUE;
            emit("air_general_search_leg",map("owner",OWNER,"unitIds",memberIds(),"x",legX,"y",legY,"frontierTile",routeFrontier,
                "routeCursor",routeCursor,"routeLegEnd",legRouteEnd,"routeTiles",searchRoute.size(),"routeLeader",routeLeader,
                "need",needSnapshot(),"plan",plan,"targetKnowledge","NO_HIDDEN_ENEMY_STATE"));
        }
        for(Member m:airborne){Map<String,Object> a=find(state,m.id);if(a==null)continue;
            double x=legX,y=legY;
            if(!safeSegment(a,x,y)){
                Map<String,Object> detour=host.readStrategy("/scout/plan?unitId="+m.id+"&role=recon&routeScope=air-search","air_general_member_detour");
                double[] safe=validPlan(detour,m.id)?safeRouteLeg(a,detour):null;
                if(safe==null){if("move".equals(a.get("orderType"))&&!safeSegment(a,number(a,"orderX",n(a,"x")),number(a,"orderY",n(a,"y"))))
                    if(move(m,a,n(a,"x"),n(a,"y"),"MEMBER_DANGEROUS_OLD_LEG_HOLD"))acted=true;continue;}
                x=safe[0];y=safe[1];
            }
            if(move(m,a,x,y,"SEARCH"))acted=true;
            if(Math.hypot(x-legX,y-legY)<1&&reusing(m,a,"move",x,y)){
                if(!"move".equals(m.orderKind)||Math.hypot(m.destinationX-x,m.destinationY-y)>=1){
                    m.orderKind="move";m.destinationX=x;m.destinationY=y;m.orderFrame=arbiter.stamp().frame;
                }
                legAcceptedFrame=Math.min(legAcceptedFrame,m.orderFrame);
            }}
        return acted;
    }
    private boolean engage(List<Member> force,Map<String,Object> enemy)throws Exception{
        boolean acted=false,compatible=false,modeCommitment=false;
        for(int start=0;start<force.size();start+=48){
            List<Member> batch=force.subList(start,Math.min(force.size(),start+48));List<Long> ids=new ArrayList<Long>();for(Member m:batch)ids.add(m.id);
            Map<String,Object> response=host.readStrategy("/combat/engagement?unitIds="+csv(ids)+"&targetId="+id(enemy),"air_general_engagement");
            if(!validCurrentTarget(response,id(enemy)))continue;
            Map<String,List<Long>> readyGroups=new LinkedHashMap<String,List<Long>>();Map<String,double[]> destinations=new HashMap<String,double[]>();
            for(Map<String,Object> choice:items(response,"actors")){
                long id=(long)n(choice,"unitId");Member m=members.get(id);Map<String,Object> a=find(state,id);if(m==null||a==null||!ids.contains(id))continue;
                m.mode.witness(response,choice,now); // desired movement is separate, not physical proof
                if(m.mode.phase()==CapabilityUnitMode.Phase.DIVE_REQUESTED||m.mode.phase()==CapabilityUnitMode.Phase.FLY_REQUESTED)readMode(m);
                if("DIVE".equals(choice.get("requiredMode"))&&"APPROACH_PATH_KNOWN".equals(choice.get("modeApproachStatus"))){
                    modeCommitment=true;
                    Map<String,Object> modes=readMode(m);if(modes==null)continue;
                    if(Boolean.TRUE.equals(choice.get("modeActionReady"))&&m.mode.phase()!=CapabilityUnitMode.Phase.DIVE_REQUESTED){
                        if(mode(m,modes,"DIVE")){acted=true;continue;}
                    }
                    if(m.mode.phase()!=CapabilityUnitMode.Phase.DIVE_REQUESTED&&move(m,a,n(choice,"modeApproachX"),n(choice,"modeApproachY"),"MOVE_TO_WATER")){m.mode.approachingWater();acted=true;}
                    continue;
                }
                if("COMPATIBLE".equals(choice.get("compatibility"))&&"APPROACH_PATH_KNOWN".equals(choice.get("status"))){
                    compatible=true;double x=n(choice,"approachX"),y=n(choice,"approachY");
                    if(!reusing(m,a,"attackMove",x,y)){String key=x+":"+y;if(!readyGroups.containsKey(key))readyGroups.put(key,new ArrayList<Long>());readyGroups.get(key).add(id);destinations.put(key,new double[]{x,y});}
                }else if("SUBMERGED".equals(m.mode.nativeMode())&&!"SUBMERGED".equals(enemy.get("targetDomain"))){modeCommitment=true;if(ensureAir(m,a))acted=true;}
            }
            for(Map.Entry<String,List<Long>> group:readyGroups.entrySet()){
                List<Long> readyIds=group.getValue();double x=destinations.get(group.getKey())[0],y=destinations.get(group.getKey())[1];
                Map<String,Object> receipt=order("/command/attack-move?unitIds="+csv(readyIds)+"&x="+x+"&y="+y,map("reason","CURRENT_LEGAL_AIR_GENERAL_TARGET","targetId",id(enemy),"targetSource",response));
                if(receipt!=null){for(Long id:readyIds)remember(members.get(id),"attackMove",x,y,receipt);attacks++;host.strategicAttack(receipt);acted=true;emit("air_general_attack",map("targetId",id(enemy),"unitIds",new ArrayList<Long>(readyIds),"receipt",receipt,"receiptIsKill",false));}
            }
        }
        if(compatible)clearSearchRoute();
        return compatible||modeCommitment||acted;
    }
    private boolean ensureAir(Member m,Map<String,Object> actor)throws Exception{
        if(m.mode.phase()==CapabilityUnitMode.Phase.UNKNOWN||m.mode.phase()==CapabilityUnitMode.Phase.FLY_REQUESTED||m.mode.phase()==CapabilityUnitMode.Phase.DIVE_REQUESTED){
            Map<String,Object> p=readMode(m);if(p==null)return true;
        }
        if(("SUBMERGED".equals(m.mode.nativeMode())||"WATER".equals(actor.get("movementType"))||m.mode.phase()==CapabilityUnitMode.Phase.DIVE_REQUESTED)
                &&m.mode.phase()!=CapabilityUnitMode.Phase.FLY_REQUESTED){Map<String,Object> p=readMode(m);if(p!=null)mode(m,p,"FLY");return true;}
        return m.mode.phase()==CapabilityUnitMode.Phase.FLY_REQUESTED||!"AIR".equals(m.mode.nativeMode());
    }
    private Map<String,Object> readMode(Member m)throws Exception{
        Map<String,Object> p=host.readStrategy("/combat/unit-modes?unitId="+m.id,"air_general_unit_mode");
        if(p==null||!arbiter.stamp().session.equals(p.get("sessionId"))||number(p,"unitId",-1)!=m.id||number(p,"gameTimeMs",-1)<now)return null;
        m.mode.witness(p,null,now);return p;
    }
    private boolean mode(Member m,Map<String,Object> packet,String desired)throws Exception{
        for(Map<String,Object> action:items(packet,"actions"))if(desired.equals(action.get("mode"))&&action.get("actionId") instanceof String&&Boolean.TRUE.equals(action.get("available"))&&Boolean.TRUE.equals(action.get("affordable"))){
            Map<String,Object> r=order("/command/unit-mode?unitId="+m.id+"&actionId="+encode(action.get("actionId")),map("reason","PER_MEMBER_"+desired,"unitId",m.id,"modeEvidence",m.mode.snapshot()));
            if(r!=null){if("DIVE".equals(desired))m.mode.diveAccepted();else m.mode.flyAccepted();m.modeReceiptFrame=arbiter.stamp().frame;invalidate(m);emit("air_general_mode_requested",map("unitId",m.id,"desired",desired,"receipt",r,"mode",m.mode.snapshot(),"actualModeProof",false));return true;}
        }return false;
    }
    private boolean move(Member m,Map<String,Object> actor,double x,double y,String reason)throws Exception{
        if(reusing(m,actor,"move",x,y))return false;
        Map<String,Object> receipt=order("/command/move?unitId="+m.id+"&x="+x+"&y="+y,map("reason",reason,"unitId",m.id,"searchArea",needSnapshot()));
        if(receipt==null)return false;remember(m,"move",x,y,receipt);return true;
    }
    private boolean reusing(Member m,Map<String,Object> actor,String kind,double x,double y){
        if(kind.equals(actor.get("orderType"))&&Math.hypot(number(actor,"orderX",Double.NaN)-x,number(actor,"orderY",Double.NaN)-y)<1)return true;
        // Only a short accepted-before-own-order witness gap. Explicit wrong order never matches.
        return arbiter.stamp().frame<=m.orderFrame&&kind.equals(m.orderKind)&&Math.hypot(m.destinationX-x,m.destinationY-y)<1;
    }
    private void remember(Member m,String kind,double x,double y,Map<String,Object> r){m.orderKind=kind;m.destinationX=x;m.destinationY=y;m.orderFrame=(long)number(r,"frame",arbiter.stamp().frame);}
    private static void invalidate(Member m){m.orderKind="";m.orderFrame=-1;}
    private Map<String,Object> order(String path,Map<String,Object> extra)throws Exception{
        Map<String,Object> context=map("airGeneralId",1,"owner",OWNER,"lane","AIR_FORCE","executionOrdering","AIR_RECOVERY_MODE_TARGET_SEARCH_CURRENT_ACTOR_CONFLICTS","modeScope","PER_UNIT");context.putAll(extra);
        return host.orderStrategy(OWNER,path,context);
    }
    private boolean legalContacts(){return contacts!=null&&contacts.get("visibleEnemies") instanceof List&&arbiter.stamp().session.equals(contacts.get("sessionId"))&&number(contacts,"gameTimeMs",-1)>=now;}
    private Map<String,Object> current(long id){if(!legalContacts())return null;for(Map<String,Object> e:items(contacts,"visibleEnemies"))if(id(e)==id)return e;return null;}
    private boolean validCurrentTarget(Map<String,Object> p,long id){return p!=null&&arbiter.stamp().session.equals(p.get("sessionId"))&&number(p,"targetId",-1)==id&&Boolean.TRUE.equals(p.get("targetVisible"))&&number(p,"gameTimeMs",-1)>=now&&number(p,"targetObservedAtGameTimeMs",-2)==number(p,"gameTimeMs",-1);}
    private boolean validPlan(Map<String,Object> p,long id){
        if(p==null||!"planned".equals(p.get("status"))||!Boolean.TRUE.equals(p.get("pathKnown"))
                ||!arbiter.stamp().session.equals(p.get("sessionId"))||number(p,"builderId",-1)!=id
                ||!Double.isFinite(number(p,"targetX",Double.NaN))||!Double.isFinite(number(p,"targetY",Double.NaN))
                ||p.containsKey("movementType")&&!"AIR".equals(p.get("movementType"))
                ||p.containsKey("gameTimeMs")&&number(p,"gameTimeMs",-1)<now)return false;
        if(!p.containsKey("routeTiles"))return true; // legacy host fixtures can provide a direct AIR segment
        List<Long> route=route(p);if(route.size()<2)return false;
        Map<String,Object> map=obj(state.get("map"));long height=(long)number(map,"tilesHigh",0),width=(long)number(map,"tilesWide",0);
        if(height<=0||width<=0)return false;
        for(int i=0;i<route.size();i++){
            long tile=route.get(i);if(tile<0||tile>=width*height)return false;
            if(i>0){long previous=route.get(i-1);
                if(Math.abs(tile/height-previous/height)+Math.abs(tile%height-previous%height)!=1)return false;
            }
        }return true;
    }
    private boolean safeSegment(Map<String,Object> actor,double x,double y){if(actor==null||!legalContacts())return false;
        // Native Scout threats export observed range. Current enemy packets do not contain range.
        // Remembered threats can only veto travel; they never become attack targets here.
        for(Map<String,Object> e:items(vision,"rememberedThreats")){
            double range=number(e,"range",0)+140;double ax=n(actor,"x"),ay=n(actor,"y"),dx=x-ax,dy=y-ay;
            double t=dx*dx+dy*dy>0?Math.max(0,Math.min(1,((n(e,"x")-ax)*dx+(n(e,"y")-ay)*dy)/(dx*dx+dy*dy))):0;
            double initial=distance(e,ax,ay);
            if(initial<range){if(t>0||distance(e,x,y)<=initial)return false;}
            else if(distance(e,ax+t*dx,ay+t*dy)<range)return false;
        }return true;
    }
    private boolean unsafeNativeTravel(Map<String,Object> actor){
        if(actor==null||!("move".equals(actor.get("orderType"))||"attackMove".equals(actor.get("orderType"))))return false;
        double x=number(actor,"orderX",n(actor,"x")),y=number(actor,"orderY",n(actor,"y"));
        // An observed current-position hold is not travel and must not keep destroying a pending escape route.
        return distance(actor,x,y)>1&&!safeSegment(actor,x,y);
    }
    private double[] safeRouteLeg(Map<String,Object> actor,Map<String,Object> plan){
        List<Long> route=route(plan);
        if(!route.isEmpty()){RouteLeg leg=safeRouteLeg(actor,route,0);return leg==null?null:new double[]{leg.x,leg.y};}
        return safeSegment(actor,n(plan,"targetX"),n(plan,"targetY"))?new double[]{n(plan,"targetX"),n(plan,"targetY")}:null;
    }
    private static final class RouteLeg {
        final int end;final double x,y;
        RouteLeg(int end,double x,double y){this.end=end;this.x=x;this.y=y;}
    }
    private List<Long> route(Map<String,Object> plan){
        if(!(plan.get("routeTiles") instanceof List))return Collections.emptyList();
        List<Long> result=new ArrayList<Long>();for(Object raw:(List<?>)plan.get("routeTiles")){
            if(!(raw instanceof Number))return Collections.emptyList();result.add(((Number)raw).longValue());
        }return result;
    }
    private RouteLeg safeRouteLeg(Map<String,Object> actor,List<Long> route,int cursor){
        Map<String,Object> map=obj(state.get("map"));long height=(long)number(map,"tilesHigh",0);double tw=number(map,"tileWidth",20),th=number(map,"tileHeight",20);
        long width=(long)number(map,"tilesWide",0);if(actor==null||height<=0||width<=0||cursor<0||cursor>=route.size()-1)return null;
        RouteLeg best=null;
        long origin=(long)(n(actor,"x")/tw)*height+(long)(n(actor,"y")/th);
        long direction=route.get(cursor+1)-route.get(cursor);
        for(int i=cursor+1;i<route.size();i++){
            long previous=route.get(i-1),tile=route.get(i);
            if(tile<0||tile>=width*height||Math.abs(tile/height-previous/height)+Math.abs(tile%height-previous%height)!=1)return null;
            if(tile-previous!=direction)break;
            double x=(tile/height+.5)*tw,y=(tile%height+.5)*th;
            if(tile==origin||distance(actor,x,y)<1)continue;
            if(!safeSegment(actor,x,y))break;best=new RouteLeg(i,x,y);
        }
        return best;
    }
    private void clearSearchRoute(){legKnown=false;searchRoute=Collections.emptyList();routeCursor=0;legRouteEnd=-1;routePlanFrame=-1;routeFrontier=-1;}
    private void buildSearchNeed(Map<String,Object> plan){
        Map<String,Object> map=obj(state.get("map"));double w=number(map,"tilesWide",0)*number(map,"tileWidth",20),h=number(map,"tilesHigh",0)*number(map,"tileHeight",20);
        String obs=String.valueOf(host.costSourceStrategy(vision).get("costSourceObservationId"));
        double total=number(vision,"mapMemoryNeverSeenTiles",0)+number(vision,"mapMemoryDeepFogTiles",0)+number(vision,"mapMemoryStaleFogTiles",0)+number(vision,"mapMemoryRecentFogTiles",0)+number(vision,"mapMemoryCurrentVisibleTiles",0);
        double coverage=total>0?number(vision,"mapMemoryCurrentVisibleTiles",0)/total:0;
        boolean noLand="NONE".equals(host.landRouteDemandStrategy())||"ROUTE_UNAVAILABLE".equals(host.landRouteDemandStrategy());
        if(w>0&&h>0&&!"null".equals(obs))searchNeed=SearchAreaNeed.lawful(true,noLand,total>0,"air-whole-map",SearchAreaNeed.Domain.AIR,0,0,w,h,coverage,null,1,now,obs);
    }
    private Map<String,Object> needSnapshot(){return searchNeed==null?map("status","LEGAL_FRONTIER_EXPLORATION","coverageAuthority","NATIVE_PLAYER_FOG_HISTORY","enemyPresence","UNKNOWN"):
        map("areaId",searchNeed.areaId,"domain",searchNeed.domain.name(),"coverage",searchNeed.coverage,"sourceObservationId",searchNeed.coverageObservationId,"underwaterDiscoveryRequirement",searchNeed.underwaterDiscoveryRequirement());}
    private Map<String,Object> nearestRepair(Map<String,Object> actor){Map<String,Object> best=null;double d=Double.MAX_VALUE;for(Map<String,Object> u:units(state))if(ready(u)&&"repairbay".equals(u.get("type"))){double v=distance(actor,n(u,"x"),n(u,"y"));if(v<d){d=v;best=u;}}return best;}
    private void rememberAvoid(){Map<String,Object> map=obj(state.get("map"));long height=(long)number(map,"tilesHigh",0);double tw=number(map,"tileWidth",20),th=number(map,"tileHeight",20);if(height>0)rememberAvoid((long)(legX/tw)*height+(long)(legY/th));}
    private void rememberAvoid(long tile){if(tile<0)return;avoided.add(tile);while(avoided.size()>48)avoided.remove(avoided.iterator().next());}
    private double[] centroid(){double x=0,y=0;int count=0;for(Member m:members.values())if(m.attached&&!m.recovering){Map<String,Object> a=find(state,m.id);if(a!=null){x+=n(a,"x");y+=n(a,"y");count++;}}return count>0?new double[]{x/count,y/count}:new double[]{homeX,homeY};}
    private List<Long> memberIds(){return new ArrayList<Long>(members.keySet());}
    private static String csv(Collection<Long> ids){StringBuilder s=new StringBuilder();for(Long id:ids){if(s.length()>0)s.append(',');s.append(id);}return s.toString();}
    private static String encode(Object value)throws Exception{return URLEncoder.encode(String.valueOf(value),"UTF-8");}
    private void emit(String kind,Map<String,Object> data)throws Exception{data.put("gameTimeMs",now);data.put("owner",OWNER);host.emitStrategy(kind,data);}
    Map<String,Object> snapshot(){List<Map<String,Object>> roster=new ArrayList<Map<String,Object>>();for(Member m:members.values())roster.add(map("unitId",m.id,"membership",m.attached?"ATTACHED":"JOINING","ownerGeneration",arbiter.ownerGeneration(m.id),"healthRole",m.recovering?"RECOVERING":"NORMAL","mode",m.mode.snapshot()));
        return map("airGeneralId",1,"owner",OWNER,"roster",roster,"targetId",target<0?null:target,"mission","CONTINUING_AIR_MAP_SEARCH","searchLegKnown",legKnown,"searchLegX",legKnown?legX:null,"searchLegY",legKnown?legY:null,
            "routeCursor",routeCursor,"routeLegEnd",legRouteEnd,"routeTiles",searchRoute.size(),"routeLeader",routeLeader,"routeFrontierTile",routeFrontier,
            "searchNeed",needSnapshot(),"searchLegsWitnessed",legs,"attackReceipts",attacks,"specialistTransfers",transfers,"groundGeneral",false);}
    void close(){for(Member m:members.values())arbiter.releaseActor(OWNER,m.id);members.clear();}
}
