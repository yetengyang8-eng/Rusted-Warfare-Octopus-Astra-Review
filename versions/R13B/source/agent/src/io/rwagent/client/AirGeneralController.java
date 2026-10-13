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
    private final String owner;
    private final long generalId;
    // The public instance remains the StrategyDirector facade. Each child has independent leases,
    // route, mode witnesses and contact duty; it never recursively creates more children.
    private final AirGeneralController registry;
    private final List<AirGeneralController> formations=new ArrayList<AirGeneralController>();
    private final Set<Long> splitAvoided=new LinkedHashSet<Long>();
    private long nextGeneralId=1,splitProbeFrame=-1;
    private boolean joinAnchorKnown;
    private double joinAnchorX,joinAnchorY;
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
    private double routeInformationScore;
    private SearchAreaNeed searchNeed;
    private ContactArea contactArea;
    private int legs,attacks,transfers;
    private static final class Member {
        final long id,joinedFrame;final CapabilityUnitMode mode;
        boolean attached,recovering;
        long modeReceiptFrame=-1;
        long modeReadAt=-1;
        Map<String,Object> modePacket;
        double previousX=Double.NaN,previousY=Double.NaN,travelLookahead;
        long previousTime=-1;
        double destinationX=Double.NaN,destinationY=Double.NaN;
        String orderKind="";
        long orderFrame=-1,orderTargetId=-1,cancelledAttackFrame=-1;
        Member(long id,long frame,CapabilityUnitMode mode){this.id=id;joinedFrame=frame;this.mode=mode;}
    }
    private static final class ContactArea {
        final long contactId;double x,y;long observedFrame,observedAt;
        boolean reacquiring;
        ContactArea(long id,double x,double y,long frame,long time){contactId=id;this.x=x;this.y=y;observedFrame=frame;observedAt=time;}
    }
    AirGeneralController(StrategyDirector.Host host,CommandArbiter arbiter){this(host,arbiter,1,null);}
    private AirGeneralController(StrategyDirector.Host host,CommandArbiter arbiter,long id,AirGeneralController registry){
        this.host=host;this.arbiter=arbiter;generalId=id;owner="air-general:"+id;this.registry=registry;
    }
    boolean accept(long id,String from,CapabilityUnitMode mode)throws Exception{
        if(members.containsKey(id))return true;
        if(mode==null||mode.unitId!=id||!arbiter.transfer(from,owner,id))return false;
        members.put(id,new Member(id,arbiter.stamp().frame,mode));transfers++;
        emit("air_general_member_transition",map("unitId",id,"oldOwner",from,"newOwner",owner,
            "ownerGeneration",arbiter.ownerGeneration(id),"oldMembership","SPECIALIST_POOL_IDLE",
            "newMembership","JOINING","reason","LONG_LIVED_AIR_SEARCH_FORMATION"));
        return true;
    }
    int size(){int count=members.size();for(AirGeneralController f:formations)count+=f.members.size();return count;}
    void observe(Map<String,Object> state,Map<String,Object> contacts,Map<String,Object> vision,double hx,double hy)throws Exception{
        observeFormation(state,contacts,vision,hx,hy);
        Iterator<AirGeneralController> it=formations.iterator();
        while(it.hasNext()){
            AirGeneralController f=it.next();f.observeFormation(state,contacts,vision,hx,hy);
            if(f.members.isEmpty()){it.remove();emit("air_general_formation_retired",map("retiredOwner",f.owner,"reason","NO_CURRENTLY_AVAILABLE_MEMBERS_NOT_DEATH_PROOF"));}
        }
    }
    private void observeFormation(Map<String,Object> state,Map<String,Object> contacts,Map<String,Object> vision,double hx,double hy)throws Exception{
        this.state=state;this.contacts=contacts;this.vision=vision;homeX=hx;homeY=hy;now=(long)n(state,"gameTimeMs");
        if(!arbiter.stamp().session.equals(state.get("sessionId")))return;
        double cx=0,cy=0;int attached=0;
        for(Member m:members.values())if(m.attached&&!m.recovering){Map<String,Object> a=find(state,m.id);if(a!=null){cx+=n(a,"x");cy+=n(a,"y");attached++;}}
        cx=attached>0?cx/attached:joinAnchorKnown?joinAnchorX:homeX;cy=attached>0?cy/attached:joinAnchorKnown?joinAnchorY:homeY;
        Iterator<Member> it=members.values().iterator();
        while(it.hasNext()){
            Member m=it.next();Map<String,Object> a=find(state,m.id);
            if(a==null||!ready(a)||!arbiter.owns(owner,m.id)){
                arbiter.releaseActor(owner,m.id);it.remove();
                emit("air_general_member_transition",map("unitId",m.id,"oldOwner",owner,"newOwner",null,
                    "ownerGeneration",arbiter.ownerGeneration(m.id),"reason","NOT_CURRENTLY_AVAILABLE_NOT_DEATH_PROOF"));continue;
            }
            double fraction=n(a,"maxHp")>0?n(a,"hp")/n(a,"maxHp"):0;
            if(m.previousTime>=0&&now>m.previousTime)m.travelLookahead=Math.hypot(n(a,"x")-m.previousX,n(a,"y")-m.previousY)+80;
            if(now>m.previousTime){m.previousX=n(a,"x");m.previousY=n(a,"y");m.previousTime=now;}
            if(!m.recovering&&fraction<.25){m.recovering=true;invalidate(m);emit("air_general_member_transition",map("unitId",m.id,"oldHealthRole","NORMAL","newHealthRole","RECOVERING","reason","CURRENT_OWN_CRITICAL_HP"));}
            else if(m.recovering&&fraction>=.75){m.recovering=false;invalidate(m);emit("air_general_member_transition",map("unitId",m.id,"oldHealthRole","RECOVERING","newHealthRole","NORMAL","reason","LATER_OWN_HP_RECOVERY"));}
            if(!m.attached&&arbiter.stamp().frame>m.joinedFrame&&distance(a,cx,cy)<LocalArmyDirector.FORM_RADIUS){
                m.attached=true;invalidate(m);emit("air_general_member_transition",map("unitId",m.id,"oldMembership","JOINING","newMembership","ATTACHED",
                    "reason","LATER_OWN_POSITION_WITNESS","sourceFrame",state.get("frame")));
            }
        }
        if(members.isEmpty()){target=-1;contactArea=null;clearSearchRoute();return;}
        if(legKnown&&(!members.containsKey(routeLeader)||members.get(routeLeader).recovering)){
            for(Member m:members.values())if(m.attached&&!m.recovering){routeLeader=m.id;break;}
        }
        if(target>=0&&legalContacts()&&current(target)==null){
            emit("air_general_target_clear",map("targetId",target,"reason","ABSENT_FROM_CURRENT_LEGAL_VISIBILITY","clearedIsKilled",false,
                "areaResponsibilityRetained",contactArea!=null,"resumeRouteCursor",routeCursor));target=-1;
            if(contactArea!=null)contactArea.reacquiring=true;
        }
        if(contactArea!=null&&legalContacts()){
            Map<String,Object> contact=current(contactArea.contactId);
            if(contact!=null){contactArea.x=n(contact,"x");contactArea.y=n(contact,"y");contactArea.observedFrame=arbiter.stamp().frame;
                contactArea.observedAt=now;contactArea.reacquiring=false;}
        }
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
        if(registry!=null)return actFormation();
        splitSearchFormation();
        boolean acted=actFormation();
        for(AirGeneralController f:formations)if(f.actFormation())acted=true;
        return acted;
    }
    private boolean actFormation()throws Exception{
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
        // A missing/stale contact packet is UNKNOWN, not proof of target absence or new danger.
        // Retain the route and native order until travel can be checked against a lawful packet.
        if(!legalContacts()){
            emit("air_general_search_deferred",map("reason","CURRENT_CONTACT_SOURCE_UNKNOWN","targetId",target<0?null:target,"resumeRouteCursor",routeCursor));return acted;
        }
        for(Member m:force)if(find(state,m.id)!=null)readMode(m);
        for(Member m:force){Map<String,Object> a=find(state,m.id);
            if(a==null)continue;
            if("attack".equals(a.get("orderType"))&&current((long)number(a,"orderTargetId",-1))==null){
                double x=n(a,"x"),y=n(a,"y");
                if(contactArea!=null&&"AIR".equals(physicalDomain(m))&&safeSegment(a,contactArea.x,contactArea.y)){x=contactArea.x;y=contactArea.y;}
                if(move(m,a,x,y,"FOG_ATTACK_CANCEL_BEFORE_AREA_REACQUIRE")){m.cancelledAttackFrame=arbiter.stamp().frame;acted=true;}
            }
        }
        if(legalContacts()){
            if(contactArea!=null){Map<String,Object> original=current(contactArea.contactId);
                if(original!=null&&groundCovered(original)){
                    emit("air_general_target_duty_transfer",map("targetId",contactArea.contactId,"reason","CURRENT_GROUND_TARGET_COMMITTED","resumeRouteCursor",routeCursor));
                    if(target==contactArea.contactId)target=-1;contactArea=null;
                }
            }
            Map<String,Object> held=current(target);
            if(held!=null&&groundCovered(held)){
                emit("air_general_target_duty_transfer",map("targetId",target,"reason","CURRENT_GROUND_TARGET_COMMITTED","resumeRouteCursor",routeCursor));
                if(contactArea!=null&&contactArea.contactId==target)contactArea=null;target=-1;
            }
            if(target>=0&&!assignedContact(target)){
                emit("air_general_target_duty_transfer",map("targetId",target,"reason","OTHER_CURRENT_AVAILABLE_AIR_OWNER","resumeRouteCursor",routeCursor));target=-1;
            }
            final double[] center=centroid();List<Map<String,Object>> candidates=new ArrayList<Map<String,Object>>();
            for(Map<String,Object> e:items(contacts,"visibleEnemies")){
                if(groundCovered(e)||!assignedContact(id(e)))continue;
                boolean threat=threatens(force,e);
                boolean spatialDuty=spatialContact(e)||"NONE".equals(host.landRouteDemandStrategy())
                    ||"ROUTE_UNAVAILABLE".equals(host.landRouteDemandStrategy())||distance(e,center[0],center[1])<600;
                if(!spatialDuty&&!threat)continue;
                // An existing area responsibility can be interrupted by immediate compatible
                // danger, but ordinary distant contacts/search never replace it.
                if(contactArea!=null&&id(e)!=contactArea.contactId&&distance(e,contactArea.x,contactArea.y)>250
                        &&!(threat&&distance(e,center[0],center[1])<600))continue;
                candidates.add(e);
            }
            Collections.sort(candidates,new Comparator<Map<String,Object>>(){public int compare(Map<String,Object> a,Map<String,Object> b){
                int ar=contactPriority(force,a),br=contactPriority(force,b);if(ar!=br)return Integer.compare(br,ar);
                if(ar==3){double ad=knownThreatDps(force,a),bd=knownThreatDps(force,b);
                    if(ad>=0&&bd>=0&&ad!=bd)return Double.compare(bd,ad);
                    double as=threatRange(a),bs=threatRange(b);if(as!=bs)return Double.compare(bs,as);}
                if(id(a)==target&&id(b)!=target)return -1;if(id(b)==target&&id(a)!=target)return 1;
                return Double.compare(distance(a,center[0],center[1]),distance(b,center[0],center[1]));
            }});
            for(Map<String,Object> e:candidates)if(engage(force,e)){
                target=id(e);
                if(contactArea==null){contactArea=new ContactArea(target,n(e,"x"),n(e,"y"),arbiter.stamp().frame,now);
                    emit("air_general_area_assigned",map("contactId",target,"x",contactArea.x,"y",contactArea.y,"sourceFrame",state.get("frame"),"attackAuthority","CURRENT_NATIVE_ENGAGEMENT_ONLY"));}
                return true;
            }
        }
        // Search resumes after positive current local visibility completes the area task.
        List<Member> airborne=new ArrayList<Member>();
        for(Member m:force){Map<String,Object> a=find(state,m.id);if(a==null)continue;
            if(m.cancelledAttackFrame==arbiter.stamp().frame)continue;
            if(ensureAir(m,a)){acted=true;continue;}
            if("AIR".equals(m.mode.nativeMode()))airborne.add(m);
        }
        if(airborne.isEmpty())return acted;
        if(contactArea!=null){if(reacquireArea(airborne))return true;if(contactArea!=null)return acted;}
        // Unsafe old combat/route travel is cancelled even while a future native plan anchor
        // is pending. The stored route/frontier remain the same mission through detours.
        for(Member m:airborne){Map<String,Object> a=find(state,m.id);if(unsafeNativeTravel(a)&&(!legKnown||arbiter.stamp().frame<routePlanFrame))
            if(move(m,a,n(a,"x"),n(a,"y"),"DANGEROUS_OLD_LEG_HOLD_BEFORE_REPLAN"))acted=true;}
        lookAhead(airborne);
        if(!legKnown){
            Member leader=members.get(routeLeader);
            if(leader==null||!airborne.contains(leader))leader=airborne.get(0);
            routeLeader=leader.id;
            Map<String,Object> plan=null;
            if(searchRoute.isEmpty()){
                String path=planPath(leader.id,false);
                plan=host.readStrategy(path,"air_general_search_plan");
                if(!validPlan(plan,leader.id)){emit("air_general_search_deferred",map("reason","NO_CURRENT_LEGAL_FRONTIER_PLAN","plan",plan));return acted;}
                if(!separateFrontier(plan,this)){
                    rememberAvoid((long)number(plan,"frontierTile",-1));
                    emit("air_general_search_deferred",map("reason","FRONTIER_RESERVED_BY_OTHER_AIR_OWNER","plan",plan));return acted;
                }
                searchRoute=route(plan);routeCursor=0;routePlanFrame=(long)number(plan,"frame",-1);routeFrontier=(long)number(plan,"frontierTile",-1);
                routeInformationScore=number(plan,"potentialInfoScore",0);
                buildSearchNeed(plan);
            }
            // A route read may occur after the current own sample while the aircraft is still
            // moving. Keep the route and wait for its own position anchor instead of discarding it.
            if(arbiter.stamp().frame<routePlanFrame)return acted;
            RouteLeg leg=searchRoute.isEmpty()?null:safeRouteLeg(find(state,leader.id),searchRoute,routeCursor);
            double[] waypoint=leg==null&&searchRoute.isEmpty()&&plan!=null?safeRouteLeg(find(state,leader.id),plan):leg==null?null:new double[]{leg.x,leg.y};
            if(waypoint==null&&!searchRoute.isEmpty()){leg=routeLeg(find(state,leader.id),searchRoute,routeCursor,false);
                if(leg!=null)waypoint=new double[]{leg.x,leg.y};}
            if(waypoint==null){emit("air_general_search_deferred",map("reason","NO_SAFE_NATIVE_ROUTE_SEGMENT_MISSION_RETAINED","plan",plan));return acted;}
            legRouteEnd=leg==null?-1:leg.end;
            legX=waypoint[0];legY=waypoint[1];legKnown=true;legFrame=arbiter.stamp().frame;legAcceptedFrame=Long.MAX_VALUE;
            emit("air_general_search_leg",map("owner",owner,"unitIds",memberIds(),"x",legX,"y",legY,"frontierTile",routeFrontier,
                "routeCursor",routeCursor,"routeLegEnd",legRouteEnd,"routeTiles",searchRoute.size(),"routeLeader",routeLeader,
                "need",needSnapshot(),"plan",plan,"targetKnowledge","NO_HIDDEN_ENEMY_STATE"));
        }
        for(Member m:airborne){Map<String,Object> a=find(state,m.id);if(a==null)continue;
            double x=legX,y=legY;
            if(!safeSegment(a,x,y)){
                double[] safe=safeDetour(a,x,y);
                if(safe==null){Map<String,Object> detour=host.readStrategy("/scout/plan?unitId="+m.id+"&role=recon&routeScope=air-search","air_general_member_detour");
                    safe=validPlan(detour,m.id)?safeRouteLeg(a,detour):null;}
                if(safe==null){if(unsafeNativeTravel(a))
                    if(move(m,a,n(a,"x"),n(a,"y"),"MEMBER_DANGEROUS_OLD_LEG_HOLD"))acted=true;continue;}
                x=safe[0];y=safe[1];
                emit("air_general_search_detour",map("unitId",m.id,"x",x,"y",y,"missionX",legX,"missionY",legY,
                    "retainedFrontierTile",routeFrontier,"retainedRouteCursor",routeCursor,"detourIsMissionCompletion",false));
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
    private boolean spatialContact(Map<String,Object> enemy){
        return host.airTargetNeedStrategy(id(enemy))||"AIR".equals(enemy.get("targetDomain"))
                ||"SUBMERGED".equals(enemy.get("targetDomain"))||Boolean.TRUE.equals(enemy.get("touchingWater"));
    }
    private boolean groundCovered(Map<String,Object> enemy){return !spatialContact(enemy)&&host.groundTargetCommittedStrategy(id(enemy));}
    private String physicalDomain(Member member){
        if(member.modePacket==null||number(member.mode.snapshot(),"witnessedAtGameTimeMs",-1)<now)return "UNKNOWN";
        return member.mode.nativeMode();
    }
    private boolean threatens(List<Member> force,Map<String,Object> enemy){
        for(Member m:force)if("COMPATIBLE".equals(AirResponsePolicy.threatCompatibility(enemy,physicalDomain(m),contacts)))return true;
        return false;
    }
    private int contactPriority(List<Member> force,Map<String,Object> enemy){
        return threatens(force,enemy)?3:spatialContact(enemy)?2:1;
    }
    private double knownThreatDps(List<Member> force,Map<String,Object> enemy){
        CombatProfiles.Profile profile=CombatProfiles.get(String.valueOf(enemy.get("type")),CombatProfiles.trusted(contacts));
        if(profile==null)return -1;
        for(Member m:force)if(profile.supports(physicalDomain(m)))return profile.dps();return 0;
    }
    private boolean reacquireArea(List<Member> airborne)throws Exception{
        ContactArea area=contactArea;if(area==null)return false;
        // A later own position permits the local fog query. Neither an accepted MOVE nor an
        // absent unit ID proves arrival, local emptiness, destruction, or whole-map clearance.
        boolean arrived=false;for(Member m:airborne){Map<String,Object> a=find(state,m.id);
            if(a!=null&&arbiter.stamp().frame>area.observedFrame&&distance(a,area.x,area.y)<120)arrived=true;}
        if(arrived&&area.reacquiring&&areaVisible(area)){
            Map<String,Object> fresh=host.readStrategy("/combat/observe","air_general_area_contact_recheck");
            if(fresh!=null&&arbiter.stamp().session.equals(fresh.get("sessionId"))&&number(fresh,"gameTimeMs",-1)>=now
                    &&fresh.get("visibleEnemies") instanceof List){
                contacts=fresh;boolean occupied=false;
                for(Map<String,Object> e:items(fresh,"visibleEnemies"))if(id(e)==area.contactId||distance(e,area.x,area.y)<=80){occupied=true;
                    if(id(e)==area.contactId){target=id(e);area.x=n(e,"x");area.y=n(e,"y");area.observedFrame=arbiter.stamp().frame;
                        area.observedAt=now;area.reacquiring=false;}break;}
                if(!occupied){emit("air_general_area_clear",map("contactId",area.contactId,"x",area.x,"y",area.y,
                    "reason","LATER_OWN_POSITION_AND_CURRENT_LOCAL_VISIBLE_EMPTY_CONTACT_PACKET","sourceFrame",state.get("frame"),
                    "localAreaIsWholeMapClear",false,"clearedIsKilled",false,"resumeRouteCursor",routeCursor));contactArea=null;target=-1;return false;}
            }
        }
        for(Member m:airborne){Map<String,Object> a=find(state,m.id);if(a==null)continue;
            double x=area.x,y=area.y;
            if(!safeSegment(a,x,y)){double[] detour=safeDetour(a,x,y);
                if(detour==null){if(unsafeNativeTravel(a)||"attack".equals(a.get("orderType")))move(m,a,n(a,"x"),n(a,"y"),"AREA_REACQUIRE_DANGER_HOLD");continue;}
                x=detour[0];y=detour[1];}
            move(m,a,x,y,"AREA_REACQUIRE_LAST_KNOWN_POSITION_ONLY");
        }
        emit("air_general_area_reacquire",map("contactId",area.contactId,"x",area.x,"y",area.y,"lastObservedAtGameTimeMs",area.observedAt,
            "currentTargetVisible",current(area.contactId)!=null,"lastKnownAreaIsAttackPermission",false,"retainedFrontierTile",routeFrontier));
        return true;
    }
    private boolean areaVisible(ContactArea area)throws Exception{
        Map<String,Object> map=obj(state.get("map"));long h=(long)number(map,"tilesHigh",0),w=(long)number(map,"tilesWide",0);
        double tw=number(map,"tileWidth",20),th=number(map,"tileHeight",20);if(h<=0||w<=0||tw<=0||th<=0)return false;
        long cx=(long)(area.x/tw),cy=(long)(area.y/th);if(cx<0||cy<0||cx>=w||cy>=h)return false;
        Set<Long> wanted=new LinkedHashSet<Long>();for(long x=Math.max(0,cx-2);x<=Math.min(w-1,cx+2);x++)
            for(long y=Math.max(0,cy-2);y<=Math.min(h-1,cy+2);y++)wanted.add(x*h+y);
        Map<String,Object> packet=host.readStrategy("/scout/visible?tiles="+csv(wanted),"air_general_area_visibility");
        if(packet==null||!arbiter.stamp().session.equals(packet.get("sessionId"))||number(packet,"gameTimeMs",-1)<now)return false;
        Set<Long> visible=new HashSet<Long>();for(Map<String,Object> tile:items(packet,"tiles"))
            if(Boolean.TRUE.equals(tile.get("visible")))visible.add((long)number(tile,"tile",-1));
        return visible.containsAll(wanted);
    }
    private void lookAhead(List<Member> airborne)throws Exception{
        Member leader=members.get(routeLeader);Map<String,Object> actor=find(state,routeLeader);
        // Reuse only this owner's already accepted native route. A later own sample and the
        // leader's current order establish travel; neither the receipt nor proximity is arrival.
        if(!legKnown||searchRoute.isEmpty()||legRouteEnd<0||legRouteEnd>=searchRoute.size()-1
                ||leader==null||!airborne.contains(leader)||arbiter.stamp().frame<=legAcceptedFrame
                ||arbiter.stamp().frame<=leader.orderFrame||!reusing(leader,actor,"move",legX,legY)
                ||distance(actor,legX,legY)>Math.max(LocalArmyDirector.FORM_RADIUS,leader.travelLookahead))return;
        RouteLeg next=safeRouteLeg(actor,searchRoute,legRouteEnd);
        if(next==null||next.end<=legRouteEnd)return;
        // Short native turns can all fit in the next observed travel interval. Steer to the
        // farthest safe endpoint through those turns without claiming the skipped tile coverage.
        while(next.end<searchRoute.size()-1&&distance(actor,next.x,next.y)<=Math.max(LocalArmyDirector.FORM_RADIUS,leader.travelLookahead)){
            RouteLeg farther=safeRouteLeg(actor,searchRoute,next.end);if(farther==null||farther.end<=next.end)break;next=farther;
        }
        double previousX=legX,previousY=legY;int previousEnd=legRouteEnd;
        legRouteEnd=next.end;legX=next.x;legY=next.y;legFrame=arbiter.stamp().frame;legAcceptedFrame=Long.MAX_VALUE;
        emit("air_general_search_lookahead",map("unitId",routeLeader,"sourceFrame",state.get("frame"),
                "previousX",previousX,"previousY",previousY,"previousRouteLegEnd",previousEnd,
                "x",legX,"y",legY,"routeLegEnd",legRouteEnd,"routeCursor",routeCursor,
                "frontierTile",routeFrontier,"actualArrivalProof",false,"reason","CURRENT_OWN_TRAVEL_SAFE_NEXT_ROUTE_SEGMENT"));
        emit("air_general_search_leg",map("unitIds",memberIds(),"x",legX,"y",legY,"frontierTile",routeFrontier,
                "routeCursor",routeCursor,"routeLegEnd",legRouteEnd,"routeTiles",searchRoute.size(),"routeLeader",routeLeader,
                "need",needSnapshot(),"plan",null,"targetKnowledge","NO_HIDDEN_ENEMY_STATE","lookahead",true));
    }
    private boolean engage(List<Member> force,Map<String,Object> enemy)throws Exception{
        boolean acted=false,compatible=false,modeCommitment=false;
        for(int start=0;start<force.size();start+=48){
            List<Member> batch=force.subList(start,Math.min(force.size(),start+48));List<Long> ids=new ArrayList<Long>();for(Member m:batch)ids.add(m.id);
            Map<String,Object> response=host.readStrategy("/combat/engagement?unitIds="+csv(ids)+"&targetId="+id(enemy),"air_general_engagement");
            if(!validCurrentTarget(response,id(enemy)))continue;
            List<Long> readyIds=new ArrayList<Long>();
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
                    compatible=true;
                    if(m.cancelledAttackFrame!=arbiter.stamp().frame&&!reusingTarget(m,a,id(enemy)))readyIds.add(id);
                }else if("SUBMERGED".equals(m.mode.nativeMode())&&!"SUBMERGED".equals(enemy.get("targetDomain"))){modeCommitment=true;if(ensureAir(m,a))acted=true;}
            }
            if(!readyIds.isEmpty()){
                Map<String,Object> receipt=order("/command/attack-target?unitIds="+csv(readyIds)+"&targetId="+id(enemy),map("reason","CURRENT_LEGAL_AIR_GENERAL_TARGET","targetId",id(enemy),"targetSource",response));
                if(receipt!=null){for(Long id:readyIds){Member m=members.get(id);remember(m,"attack",Double.NaN,Double.NaN,receipt);m.orderTargetId=id(enemy);}
                    attacks++;host.strategicAttack(receipt);acted=true;emit("air_general_attack",map("targetId",id(enemy),"unitIds",new ArrayList<Long>(readyIds),"receipt",receipt,"receiptIsKill",false));}
            }
        }
        // Legal combat temporarily replaces native orders, but never discards the search mission.
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
        if(m.modeReadAt==now)return m.modePacket;
        m.modeReadAt=now;m.modePacket=null;
        Map<String,Object> p=host.readStrategy("/combat/unit-modes?unitId="+m.id,"air_general_unit_mode");
        if(p==null||!arbiter.stamp().session.equals(p.get("sessionId"))||number(p,"unitId",-1)!=m.id||number(p,"gameTimeMs",-1)<now)return null;
        m.mode.witness(p,null,now);m.modePacket=p;return p;
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
    private boolean reusingTarget(Member m,Map<String,Object> actor,long targetId){
        if("attack".equals(actor.get("orderType"))&&number(actor,"orderTargetId",-1)==targetId)return true;
        return arbiter.stamp().frame<=m.orderFrame&&"attack".equals(m.orderKind)&&m.orderTargetId==targetId;
    }
    private void remember(Member m,String kind,double x,double y,Map<String,Object> r){m.orderKind=kind;m.destinationX=x;m.destinationY=y;m.orderTargetId=-1;m.orderFrame=(long)number(r,"frame",arbiter.stamp().frame);}
    private static void invalidate(Member m){m.orderKind="";m.orderFrame=-1;}
    private Map<String,Object> order(String path,Map<String,Object> extra)throws Exception{
        Map<String,Object> context=map("airGeneralId",generalId,"owner",owner,"lane","AIR_FORCE","executionOrdering","AIR_RECOVERY_MODE_TARGET_SEARCH_CURRENT_ACTOR_CONFLICTS","modeScope","PER_UNIT");context.putAll(extra);
        return host.orderStrategy(owner,path,context);
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
        // Only actual compatible/UNKNOWN weapons can veto travel. A harmless factory's HP
        // and a surface-only gun do not become anti-air firepower. Memory never authorizes fire.
        Member member=members.get(id(actor));String domain=member==null?"UNKNOWN":physicalDomain(member);
        for(Map<String,Object> e:travelThreats()){
            if("INCOMPATIBLE".equals(AirResponsePolicy.threatCompatibility(e,domain,contacts)))continue;
            double range=threatRange(e)+140;double ax=n(actor,"x"),ay=n(actor,"y"),dx=x-ax,dy=y-ay;
            double t=dx*dx+dy*dy>0?Math.max(0,Math.min(1,((n(e,"x")-ax)*dx+(n(e,"y")-ay)*dy)/(dx*dx+dy*dy))):0;
            double initial=distance(e,ax,ay);
            if(initial<range){if(t>0||distance(e,x,y)<=initial)return false;}
            else if(distance(e,ax+t*dx,ay+t*dy)<range)return false;
        }return true;
    }
    private List<Map<String,Object>> travelThreats(){
        List<Map<String,Object>> threats=new ArrayList<Map<String,Object>>(items(vision,"rememberedThreats"));
        Set<Long> seen=new HashSet<Long>();for(Map<String,Object> e:threats){long observedId=(long)number(e,"id",-1);if(observedId>=0)seen.add(observedId);}
        if(legalContacts())for(Map<String,Object> e:items(contacts,"visibleEnemies"))if(!seen.contains(id(e))
                &&(number(e,"weaponRange",0)>0||CombatProfiles.get(String.valueOf(e.get("type")),CombatProfiles.trusted(contacts))!=null))threats.add(e);
        return threats;
    }
    private double threatRange(Map<String,Object> enemy){
        double range=number(enemy,"range",number(enemy,"weaponRange",Double.NaN));
        if(Double.isFinite(range)&&range>=0)return range;
        CombatProfiles.Profile profile=CombatProfiles.get(String.valueOf(enemy.get("type")),CombatProfiles.trusted(contacts));
        return profile==null?0:profile.range;
    }
    private double[] safeDetour(Map<String,Object> actor,double goalX,double goalY){
        Map<String,Object> map=obj(state.get("map"));double w=number(map,"tilesWide",0)*number(map,"tileWidth",20),h=number(map,"tilesHigh",0)*number(map,"tileHeight",20);
        double ax=n(actor,"x"),ay=n(actor,"y"),dx=goalX-ax,dy=goalY-ay,length=Math.hypot(dx,dy);if(length<1)return null;
        Member m=members.get(id(actor));String domain=m==null?"UNKNOWN":physicalDomain(m);
        double[] best=null;double cost=Double.MAX_VALUE;
        for(Map<String,Object> e:travelThreats()){
            if("INCOMPATIBLE".equals(AirResponsePolicy.threatCompatibility(e,domain,contacts)))continue;
            double radius=threatRange(e)+140,ex=n(e,"x"),ey=n(e,"y"),initial=Math.hypot(ax-ex,ay-ey);
            List<double[]> choices=new ArrayList<double[]>();
            if(initial<radius&&initial>0)choices.add(new double[]{ex+(ax-ex)/initial*(radius+80),ey+(ay-ey)/initial*(radius+80)});
            for(int side:new int[]{-1,1})choices.add(new double[]{ex+side*(-dy/length)*(radius*2+80),ey+side*(dx/length)*(radius*2+80)});
            for(double[] point:choices){if(point[0]<0||point[1]<0||point[0]>=w||point[1]>=h||!safeSegment(actor,point[0],point[1]))continue;
                Map<String,Object> anchor=new LinkedHashMap<String,Object>(actor);anchor.put("x",point[0]);anchor.put("y",point[1]);
                boolean reconnects=safeSegment(anchor,goalX,goalY);
                if(!reconnects&&initial>=radius)continue;
                double d=Math.hypot(point[0]-ax,point[1]-ay)+Math.hypot(goalX-point[0],goalY-point[1]);
                if(d<cost){cost=d;best=point;}
            }
        }return best;
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
        return routeLeg(actor,route,cursor,true);
    }
    private RouteLeg routeLeg(Map<String,Object> actor,List<Long> route,int cursor,boolean checkSafety){
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
            if(checkSafety&&!safeSegment(actor,x,y))break;best=new RouteLeg(i,x,y);
        }
        return best;
    }
    private void clearSearchRoute(){legKnown=false;searchRoute=Collections.emptyList();routeCursor=0;legRouteEnd=-1;routePlanFrame=-1;routeFrontier=-1;routeInformationScore=0;}
    private void buildSearchNeed(Map<String,Object> plan){
        Map<String,Object> map=obj(state.get("map"));double w=number(map,"tilesWide",0)*number(map,"tileWidth",20),h=number(map,"tilesHigh",0)*number(map,"tileHeight",20);
        String obs=String.valueOf(host.costSourceStrategy(vision).get("costSourceObservationId"));
        double total=number(vision,"mapMemoryNeverSeenTiles",0)+number(vision,"mapMemoryDeepFogTiles",0)+number(vision,"mapMemoryStaleFogTiles",0)+number(vision,"mapMemoryRecentFogTiles",0)+number(vision,"mapMemoryCurrentVisibleTiles",0);
        double coverage=total>0?number(vision,"mapMemoryCurrentVisibleTiles",0)/total:0;
        boolean noLand="NONE".equals(host.landRouteDemandStrategy())||"ROUTE_UNAVAILABLE".equals(host.landRouteDemandStrategy());
        if(w>0&&h>0&&!"null".equals(obs))searchNeed=SearchAreaNeed.lawful(true,noLand,total>0,"air-frontier:"+generalId+":"+routeFrontier,SearchAreaNeed.Domain.AIR,0,0,w,h,coverage,null,1,now,obs);
    }
    private Map<String,Object> needSnapshot(){return searchNeed==null?map("status","LEGAL_FRONTIER_EXPLORATION","coverageAuthority","NATIVE_PLAYER_FOG_HISTORY","enemyPresence","UNKNOWN"):
        map("areaId",searchNeed.areaId,"domain",searchNeed.domain.name(),"coverage",searchNeed.coverage,
            "coverageScope","GLOBAL_PLAYER_FOG_HISTORY","localCoverage","UNKNOWN","assignedFrontierTile",routeFrontier,
            "sourceObservationId",searchNeed.coverageObservationId,"underwaterDiscoveryRequirement",searchNeed.underwaterDiscoveryRequirement());}
    private Map<String,Object> nearestRepair(Map<String,Object> actor){Map<String,Object> best=null;double d=Double.MAX_VALUE;for(Map<String,Object> u:units(state))if(ready(u)&&"repairbay".equals(u.get("type"))){double v=distance(actor,n(u,"x"),n(u,"y"));if(v<d){d=v;best=u;}}return best;}
    private void rememberAvoid(){Map<String,Object> map=obj(state.get("map"));long height=(long)number(map,"tilesHigh",0);double tw=number(map,"tileWidth",20),th=number(map,"tileHeight",20);if(height>0)rememberAvoid((long)(legX/tw)*height+(long)(legY/th));}
    private void rememberAvoid(long tile){if(tile<0)return;avoided.add(tile);while(avoided.size()>48)avoided.remove(avoided.iterator().next());}
    private double[] centroid(){double x=0,y=0;int count=0;for(Member m:members.values())if(m.attached&&!m.recovering){Map<String,Object> a=find(state,m.id);if(a!=null){x+=n(a,"x");y+=n(a,"y");count++;}}return count>0?new double[]{x/count,y/count}:joinAnchorKnown?new double[]{joinAnchorX,joinAnchorY}:new double[]{homeX,homeY};}
    private List<Long> memberIds(){return new ArrayList<Long>(members.keySet());}
    private static String csv(Collection<Long> ids){StringBuilder s=new StringBuilder();for(Long id:ids){if(s.length()>0)s.append(',');s.append(id);}return s.toString();}
    private static String encode(Object value)throws Exception{return URLEncoder.encode(String.valueOf(value),"UTF-8");}
    private void emit(String kind,Map<String,Object> data)throws Exception{data.put("gameTimeMs",now);data.put("owner",owner);data.put("airGeneralId",generalId);host.emitStrategy(kind,data);}
    private Map<String,Object> formationSnapshot(){List<Map<String,Object>> roster=new ArrayList<Map<String,Object>>();for(Member m:members.values())roster.add(map("unitId",m.id,"owner",owner,"membership",m.attached?"ATTACHED":"JOINING","ownerGeneration",arbiter.ownerGeneration(m.id),"healthRole",m.recovering?"RECOVERING":"NORMAL","mode",m.mode.snapshot()));
        return map("airGeneralId",generalId,"owner",owner,"roster",roster,"targetId",target<0?null:target,"mission","CONTINUING_AIR_MAP_SEARCH","searchLegKnown",legKnown,"searchLegX",legKnown?legX:null,"searchLegY",legKnown?legY:null,
            "routeCursor",routeCursor,"routeLegEnd",legRouteEnd,"routeTiles",searchRoute.size(),"routeLeader",routeLeader,"routeFrontierTile",routeFrontier,
            "routePotentialInfoScore",routeInformationScore,"routeInfoAuthority","NATIVE_PLAYER_FOG_HISTORY",
            "searchNeed",needSnapshot(),"contactArea",contactArea==null?null:map("contactId",contactArea.contactId,"x",contactArea.x,"y",contactArea.y,
                "phase",contactArea.reacquiring?"REACQUIRE_LAST_KNOWN_AREA":"CURRENT_CONTACT_CLEAR_AREA","lastObservedAtGameTimeMs",contactArea.observedAt,
                "currentVisibilityRequiredForAttack",true,"ordinarySearchCanReplace",false),
            "searchLegsWitnessed",legs,"attackReceipts",attacks,"specialistTransfers",transfers,"groundGeneral",false);}
    Map<String,Object> snapshot(){
        Map<String,Object> result=formationSnapshot();
        List<Map<String,Object>> all=new ArrayList<Map<String,Object>>();all.add(formationSnapshot());
        List<Map<String,Object>> roster=new ArrayList<Map<String,Object>>(items(result,"roster"));
        int witnessed=legs,receipts=attacks;
        for(AirGeneralController f:formations){Map<String,Object> s=f.formationSnapshot();all.add(s);roster.addAll(items(s,"roster"));witnessed+=f.legs;receipts+=f.attacks;}
        result.put("formations",all);result.put("formationCount",all.size());result.put("roster",roster);
        result.put("searchLegsWitnessed",witnessed);result.put("attackReceipts",receipts);return result;
    }
    void close(){for(AirGeneralController f:formations)f.close();formations.clear();for(Member m:members.values())arbiter.releaseActor(owner,m.id);members.clear();}

    private List<AirGeneralController> allFormations(){
        AirGeneralController root=registry==null?this:registry;
        List<AirGeneralController> result=new ArrayList<AirGeneralController>();result.add(root);result.addAll(root.formations);return result;
    }
    /** Retain an existing lawful engagement until contact is lost. A new contact goes to its
     * nearest available formation; globally visible contacts cannot attract every owner. */
    private boolean assignedContact(long enemyId){
        List<AirGeneralController> all=allFormations();if(all.size()==1)return true;
        for(AirGeneralController f:all)if(f.contactArea!=null&&f.contactArea.contactId==enemyId&&f.hasAvailableMembers())return f==this;
        for(AirGeneralController f:all)if(f.target==enemyId&&f.current(enemyId)!=null&&f.hasAvailableMembers())return f==this;
        Map<String,Object> enemy=current(enemyId);if(enemy==null)return false;
        AirGeneralController nearest=null;double best=Double.MAX_VALUE;
        for(AirGeneralController f:all){
            if(!f.hasAvailableMembers())continue;
            double[] center=f.centroid();double d=distance(enemy,center[0],center[1]);
            if(f.contactArea!=null&&distance(enemy,f.contactArea.x,f.contactArea.y)>250&&d>=600)continue;
            if(d<best){best=d;nearest=f;}
        }return nearest==this;
    }
    private boolean hasAvailableMembers(){for(Member m:members.values())if(m.attached&&!m.recovering)return true;return false;}
    private String planPath(long leader,boolean split){
        LinkedHashSet<Long> avoid=new LinkedHashSet<Long>();
        if(split)avoid.addAll(splitAvoided);else avoid.addAll(avoided);
        // Reservations come from native lawful frontier plans, never hidden dynamic enemy state.
        for(AirGeneralController f:allFormations())if((split||f!=this)&&f.routeFrontier>=0)avoid.add(f.routeFrontier);
        if(split){
            Map<String,Object> map=obj(state.get("map"));long h=(long)number(map,"tilesHigh",0),w=(long)number(map,"tilesWide",0);
            for(AirGeneralController f:allFormations())if(h>0&&f.routeFrontier>=0){
                long x=f.routeFrontier/h,y=f.routeFrontier%h;
                // Native avoid excludes +/-3 tiles. Adjacent seven-tile cells reserve the
                // current search neighborhood within the native API's existing 48-tile bound.
                for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
                    long tx=x+dx*7,ty=y+dy*7;if(tx>=0&&tx<w&&ty>=0&&ty<h&&avoid.size()<48)avoid.add(tx*h+ty);
                }
            }
        }
        while(avoid.size()>48)avoid.remove(avoid.iterator().next());
        return "/scout/plan?unitId="+leader+"&role=recon&routeScope=air-search"+(avoid.isEmpty()?"":"&avoid="+csv(avoid));
    }
    private boolean separateFrontier(Map<String,Object> plan,AirGeneralController except){
        Map<String,Object> map=obj(state.get("map"));long h=(long)number(map,"tilesHigh",0);
        double tw=number(map,"tileWidth",20),th=number(map,"tileHeight",20);
        if(h<=0)return false;
        long candidate=(long)number(plan,"frontierTile",-1);if(candidate<0)return allFormations().size()==1;
        for(AirGeneralController f:allFormations())if(f!=except&&f.routeFrontier>=0){
            double separation=Math.hypot((candidate/h-f.routeFrontier/h)*tw,(candidate%h-f.routeFrontier%h)*th);
            if(separation<LocalArmyDirector.FORM_RADIUS)return false;
        }return true;
    }
    private void splitSearchFormation()throws Exception{
        if(state==null||splitProbeFrame==arbiter.stamp().frame)return;
        for(AirGeneralController donor:allFormations()){
            // Idle AIR specialists may explore while Ground still has lawful targets. The
            // existing typed whole-map SearchAreaNeed trigger remains stricter and optional.
            if(donor.target>=0||donor.contactArea!=null||donor.routeFrontier<0||donor.routeInformationScore<=0)continue;
            List<Member> ready=new ArrayList<Member>();
            for(Member m:donor.members.values()){
                Map<String,Object> actor=find(state,m.id);
                // /state omits movementType. Only the existing physical native-mode witness
                // and AIR_READY phase establish mode; requested navigation remains separate.
                if(actor!=null&&ready(actor)&&m.attached&&!m.recovering&&m.mode.phase()==CapabilityUnitMode.Phase.AIR_READY
                        &&"AIR".equals(m.mode.nativeMode()))ready.add(m);
            }
            if(ready.size()<LocalArmyDirector.FORM_MINIMUM+LocalArmyDirector.RELEASE_BELOW)continue;
            List<Member> selected=ready.subList(ready.size()-LocalArmyDirector.FORM_MINIMUM,ready.size());
            Member leader=selected.get(0);splitProbeFrame=arbiter.stamp().frame;
            Map<String,Object> plan=host.readStrategy(planPath(leader.id,true),"air_general_split_search_plan");
            if(!donor.validPlan(plan,leader.id)||!Boolean.TRUE.equals(plan.get("routeComplete"))
                    ||!"air-search".equals(plan.get("routeScope"))||number(plan,"potentialInfoScore",0)<=0
                    ||donor.route(plan).isEmpty()||number(plan,"frontierTile",-1)!=donor.route(plan).get(donor.route(plan).size()-1)
                    ||!separateFrontier(plan,null)){
                long candidate=(long)number(plan,"frontierTile",-1);if(candidate>=0){splitAvoided.add(candidate);while(splitAvoided.size()>48)splitAvoided.remove(splitAvoided.iterator().next());}
                emit("air_general_split_deferred",map("reason","NO_SEPARATE_CURRENT_LEGAL_FRONTIER","donorOwner",donor.owner,"plan",plan));return;
            }
            AirGeneralController child=new AirGeneralController(host,arbiter,++nextGeneralId,this);
            double x=0,y=0;for(Member m:selected){Map<String,Object> a=find(state,m.id);x+=n(a,"x");y+=n(a,"y");}
            child.joinAnchorKnown=true;child.joinAnchorX=x/selected.size();child.joinAnchorY=y/selected.size();
            for(Member m:new ArrayList<Member>(selected))if(arbiter.transfer(donor.owner,child.owner,m.id)){
                donor.members.remove(m.id);child.members.put(m.id,new Member(m.id,arbiter.stamp().frame,m.mode));
                child.emit("air_general_member_transition",map("unitId",m.id,"oldOwner",donor.owner,"newOwner",child.owner,
                    "ownerGeneration",arbiter.ownerGeneration(m.id),"oldMembership","ATTACHED","newMembership","JOINING",
                    "reason","SEPARATE_LAWFUL_SEARCH_FRONTIER","actualArrivalProof",false));
            }
            child.observeFormation(state,contacts,vision,homeX,homeY);
            child.searchRoute=child.route(plan);child.routeLeader=leader.id;child.routePlanFrame=(long)number(plan,"frame",-1);
            child.routeInformationScore=number(plan,"potentialInfoScore",0);
            child.routeFrontier=(long)number(plan,"frontierTile",-1);child.buildSearchNeed(plan);formations.add(child);
            emit("air_general_formation_created",map("newOwner",child.owner,"donorOwner",donor.owner,"unitIds",child.memberIds(),
                "frontierTile",child.routeFrontier,"plan",plan,"reason","READY_SPECIALISTS_AND_SEPARATE_LEGAL_SEARCH_WORK"));return;
        }
    }
}
