package io.rwagent.client;

import java.net.URLEncoder;
import java.util.*;
import static io.rwagent.client.BattleClient.*;

/** Session-local capability demand, investment allocation and leased specialist tasks.
 * It consumes legal observations only. Geometry is evidence about an approach, never a kill promise.
 */
final class StrategyDirector {
    interface Host {
        Map<String,Object> readStrategy(String path,String event)throws Exception;
        Map<String,Object> orderStrategy(String owner,String path)throws Exception;
        default Map<String,Object> orderStrategy(String owner,String path,Map<String,Object> context)throws Exception{return orderStrategy(owner,path);}
        default WorldState worldStrategy(){return null;}
        default boolean parallelStrategy(){return false;}
        /** G5's supplied ordinary roster already includes owned General members. Assessment is
         * read-only and must not confuse command ownership with eligibility for demand evidence. */
        default boolean assessOwnedOrdinaryStrategy(){return false;}
        /** G5 uses its own continuing General commitments, independent of hidden enemy state. */
        default boolean economicArmyPolicyStrategy(){return false;}
        /** Explicit G6 entry, leaving older fixtures and R6 behavior available for comparison. */
        default boolean endgameStrategy(){return false;}
        default String landRouteDemandStrategy(){return "UNKNOWN";}
        default boolean airTargetNeedStrategy(long enemy){return false;}
        default int militaryMissionDemandStrategy(){return 0;}
        /** Desired total recovery sites from the host's actual wounded-unit pool. */
        default int recoveryCapacityNeed(){return 0;}
        default boolean borrowBaselineBuilderForRecovery(){return false;}
        /** Own uncommitted facility jobs, after the host subtracts its BuildJob/paid ghost. */
        default int constructionDemandStrategy(){return 0;}
        /** Actual current own attached General positions, never their destination or enemy state.
         * Each entry supplies generalId, x, y and readyMembers. Nearby resource opportunities
         * prepare an economic front; the General count alone creates no constructor demand. */
        default List<Map<String,Object>> constructionMilitaryFrontsStrategy(){return Collections.emptyList();}
        /** Baseline builder's accepted travel/build site; coordinates are world coordinates. */
        default boolean constructionResourceCommittedStrategy(double x,double y){return false;}
        /** Conservative currently known home-side coverage; an empty contact list is insufficient. */
        default boolean mineSafeZoneStrategy(Map<String,Object> mine){return false;}
        default double effectiveStrategyCredits(double nativeCredits){return nativeCredits;}
        /** Identity of the actual validated native menu/plan packet; absent metadata is UNKNOWN.
         * A later unrelated GET and the state observation are never this quote's price source. */
        default Map<String,Object> costSourceStrategy(Map<String,Object> nativeResponse){return Collections.emptyMap();}
        void emitStrategy(String event,Map<String,Object> data)throws Exception;
        void spendStrategy(String category,long cost,String type,long actor)throws Exception;
        void strategicAttack(Map<String,Object> receipt);
        /** Optional G1 sidecar. Its data must never be read by strategy policy. */
        default void traceStrategy(String event,Map<String,Object> data){}
    }
    private final Host host;
    private final CommandArbiter arbiter;
    private boolean enabled;
    private long now,started,remaining,lastCapacity=-100000,lastAllocation=-100000,taskSequence=1000000;
    private int hardCap=40,armyTarget=40,activeTarget=24,reserveTarget=8,builderTarget=1;
    private int militaryCapacityFloor,militaryCapacityIncreases;
    private int economicMilitaryGrowthGoal=-1;
    private double income,consumption,protectedFunds,homeX,homeY;
    private boolean homeEmergency;
    private Map<String,Object> state,enemies,scout;
    private final Map<Long,Need> needs=new LinkedHashMap<Long,Need>();
    private final Map<Long,Assessment> assessments=new LinkedHashMap<Long,Assessment>();
    private final Map<Long,Worker> workers=new LinkedHashMap<Long,Worker>();
    private final Map<Long,CapabilityTask> capabilityTasks=new LinkedHashMap<Long,CapabilityTask>();
    private final Map<Long,Purchase> purchases=new LinkedHashMap<Long,Purchase>();
    private final Set<Long> matchedConstructionProducts=new HashSet<Long>();
    private final Set<Long> matchedPurchasedWorkers=new HashSet<Long>();
    private final MineInvestmentPolicy mineInvestment=new MineInvestmentPolicy();
    private final AirGeneralController airGeneral;
    private final LongTermCapital longTermCapital=new LongTermCapital();
    private boolean endgameReady;
    private int endgameAirTarget;
    private int endgameProviderTarget;
    private String lastEndgameProviderDemand="";
    private double endgameJetQuote=-1;
    private Worker airFundingWorker;
    private Map<String,Object> airFundingQuote;
    private double airFundingCost;
    private long airFundingProducer=-1;
    private String airFundingAction;
    private Map<String,Object> militaryInvestment=Collections.emptyMap();
    private String lastMilitaryInvestment="";
    private int capitalUpgrades;
    private int investments,upgrades,completedJobs,resolvedNeeds;
    private CapabilityFunding capabilityFunding;
    private long capabilityFundingRetryAt;
    private int capabilityReservesStarted,capabilityReservesReleased;
    private static final class CapabilityFunding {
        final long need,producer,started,deadline;
        final String action;
        final double cost;
        CapabilityFunding(long need,long producer,String action,double cost,long now){
            this.need=need;this.producer=producer;this.action=action;this.cost=cost;started=now;deadline=now+90000;
        }
    }
    private static final class Assessment {
        long checked=-100000;String signature="";double x,y;
        final Set<Long> blocked=new HashSet<Long>();
        final Set<String> usefulProducts=new HashSet<String>();
        final Set<String> checkedProducts=new HashSet<String>(),unknownProducts=new HashSet<String>();
        boolean complete;
    }
    private static final class Need {
        final long id;String type,reason;double x,y;long seen,awaitVisibleAfter=-1;int failures;boolean building;
        Need(long id){this.id=id;}
    }
    private static final class Worker {
        final long unit,task;final String owner;
        String job="IDLE",product;long target=-1,jobAt,lastOrder=-100000,lastProgress,retryAt,recoveryRetryAt,lastAssessment=-100000,investigateAt,investigateSeen;
        double x,y,buildX,buildY,best=Double.MAX_VALUE,hp=Double.MAX_VALUE;
        Set<Long> before=new HashSet<Long>();
        Set<Long> prospects=new HashSet<Long>();
        final ProspectCommitment prospect=new ProspectCommitment();
        Map<String,Object> supportQuote;
        double supportReserve;
        long supportDeadline;
        String modeAction;
        final CapabilityUnitMode mode;
        boolean paidConstruction,endgameConstruction,endgameProvider;
        long staticProspectTile=-1;
        Map<String,Object> mineQuote;
        double mineReserve;
        Worker(long unit,long task){this.unit=unit;this.task=task;owner="strategy:"+task;mode=new CapabilityUnitMode(unit);}
    }
    private static final class Purchase {
        final String product;final long at,selectedNeed;final boolean endgameProvider;long need;boolean active,queueFinished,fulfilled;
        final Set<Long> before=new HashSet<Long>();
        Purchase(String product,long at){this(product,at,-1);}
        Purchase(String product,long at,long need){this(product,at,need,false);}
        Purchase(String product,long at,long need,boolean endgameProvider){this.product=product;this.at=at;this.need=need;selectedNeed=need;this.endgameProvider=endgameProvider;}
    }
    StrategyDirector(Host host,CommandArbiter arbiter){this.host=host;this.arbiter=arbiter;airGeneral=new AirGeneralController(host,arbiter);}
    boolean hasCrossDomainNeed(long enemy){return needs.containsKey(enemy);}
    void enable(Map<String,Object> health,int hardCap){
        enabled=number(health,"strategyContractVersion",0)>=1
                &&!"false".equalsIgnoreCase(System.getProperty("rwagent.globalStrategy","true"));
        this.hardCap=hardCap;armyTarget=Math.min(40,hardCap);
    }
    boolean enabled(){return enabled;}
    int armyTarget(){return enabled?armyTarget:hardCap;}
    int militaryGrowthGoal(){return host.economicArmyPolicyStrategy()?Math.max(0,economicMilitaryGrowthGoal):hardCap;}
    int activeTarget(){return activeTarget;}
    int builderTarget(){return builderTarget;}
    /** Only an arrived worker with a current legal native site protects construction funds. */
    double frontierConstructionReserve(){double total=0;for(Worker w:workers.values())total+=w.mineReserve;return total;}
    double capabilityReserve(){double value=(capabilityFunding==null?0:capabilityFunding.cost)+airFundingCost;
        for(Worker worker:workers.values())value+=worker.supportReserve;return value;}
    boolean preferSpatialInvestment(){return Boolean.TRUE.equals(militaryInvestment.get("preferSpatialInvestment"));}
    boolean wouldBreachCapabilityReserve(double credits,double cost,double otherReserved){
        return capabilityReserve()>0&&credits-cost<capabilityReserve()+Math.max(0,otherReserved);
    }
    boolean pending(long actor){return purchases.containsKey(actor);}
    private double ordinaryNativePrice=-1;
    private double builderNativePrice=-1;
    private Map<String,Object> constructionMap;
    private String constructionSession;
    private long constructionMapRetry=-100000;
    private int constructionDomains;
    private int actionableConstructionDomains,constructionFacilityJobs,constructionIdleWorkers,constructionUnmetJobs;
    private int militaryConstructionDomains,homeCapitalConstructionJobs;
    private List<Map<String,Object>> constructionMilitaryFronts=Collections.emptyList();
    private final Set<Long> constructionReachable=new HashSet<Long>();
    private final Set<Long> constructionUnreachable=new HashSet<Long>();
    private int unobservedCombatSlots;
    void noteUnobservedCombatSlots(int slots){unobservedCombatSlots=Math.max(0,slots);}
    private long lastIncomeSurplusHold=-100000;
    void noteOrdinaryNativePrice(double price){if(Double.isFinite(price)&&price>0)ordinaryNativePrice=price;}
    int committedArmedForPolicy(){return state==null?0:committedArmed();}
    boolean safetyCapacityAvailable(){return !enabled||state==null||committedArmed()+unobservedCombatSlots<hardCap;}
    private int committedArmed(){
        int count=0;
        for(Map<String,Object> u:units(state))if(alive(u)){
            if(Boolean.TRUE.equals(u.get("mobile"))&&Boolean.TRUE.equals(u.get("canAttack")))count++;
            // Queue contents are not exported per item. Counting every factory slot as armed is a
            // conservative bound, including upgrades/builders rather than undercounting paid units.
            if("landFactory".equals(u.get("type")))count+=(int)Math.max(0,number(u,"productionQueue",0));
        }
        for(Purchase purchase:purchases.values())if(!purchase.active&&"combatEngineer".equals(purchase.product))count++;
        for(Worker worker:workers.values())if(worker.paidConstruction
                &&("heavyTank".equals(worker.product)||"amphibiousJet".equals(worker.product)))count++;
        return count;
    }
    void observe(Map<String,Object> state,Map<String,Object> enemies,Map<String,Object> scout,
                 List<Map<String,Object>> main,long start,long remainingMs,double income,double consumption,
                 double reserved,boolean buildBacklog)throws Exception{
        Map<String,Map<String,Object>> before=traceStates();
        try{observePolicy(state,enemies,scout,main,start,remainingMs,income,consumption,reserved,buildBacklog);}
        finally{traceStateChanges(before,"OBSERVE");}
    }
    private void observePolicy(Map<String,Object> state,Map<String,Object> enemies,Map<String,Object> scout,
                 List<Map<String,Object>> main,long start,long remainingMs,double income,double consumption,
                 double reserved,boolean buildBacklog)throws Exception{
        if(!enabled)return;
        this.state=state;this.enemies=enemies;this.scout=scout;now=(long)n(state,"gameTimeMs");started=start;
        constructionMilitaryFronts=host.economicArmyPolicyStrategy()?host.constructionMilitaryFrontsStrategy():Collections.emptyList();
        mineInvestment.observe(state,enemies,scout);
        remaining=remainingMs;this.income=income;this.consumption=consumption;protectedFunds=Math.max(0,reserved-frontierConstructionReserve());
        for(Map<String,Object> unit:units(state))if("commandCenter".equals(unit.get("type"))){homeX=n(unit,"x");homeY=n(unit,"y");break;}
        homeEmergency=false;
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies"))if(Boolean.TRUE.equals(enemy.get("canAttack"))
                &&distance(enemy,homeX,homeY)<500)homeEmergency=true;
        claimWorkers();updatePurchases();refreshAssessments(main);clearNeeds();updateWorkers();
        refreshFrontierFunding();
        if(host.endgameStrategy())observeEndgame();
        refreshMilitaryInvestment();
        validateCapabilityFunding();
        if(now-lastCapacity>=30000){
            lastCapacity=now;
            int backlog=resourceBacklog(),armed=army(state).size();
            Map<String,Object> map=obj(state.get("map"));
            double area=number(map,"tilesWide",110)*number(map,"tilesHigh",110);
            int threats=0;for(Map<String,Object> enemy:items(enemies,"visibleEnemies"))if(Boolean.TRUE.equals(enemy.get("canAttack")))threats++;
            Capacity value=capacity(area,income,threats,needs.size(),backlog,buildBacklog,armed,
                    now-started,hardCap,System.getProperty("rwagent.mobileUnitHardCap")!=null);
            Map<String,Object> policyEvidence=Collections.emptyMap();
            if(host.economicArmyPolicyStrategy()){
                refreshConstructionMap();
                boolean useful="KNOWN".equals(ordinaryDemand("heavyTank"))||"KNOWN".equals(ordinaryDemand("tank"))
                    ||"KNOWN".equals(ordinaryDemand("c_tank"));
                GlobalStrategyPolicy.Goal goal=GlobalStrategyPolicy.goal(value.total,value.active,hardCap,
                    System.getProperty("rwagent.mobileUnitHardCap")!=null,now-started,income,consumption,
                    ordinaryNativePrice,host.militaryMissionDemandStrategy(),useful,homeEmergency);
                economicMilitaryGrowthGoal=goal.total;
                // A past expansion cannot become independent demand above today's economic goal.
                // Existing native units and paid-before-visible commitments remain protected below.
                militaryCapacityFloor=Math.min(militaryCapacityFloor,economicMilitaryGrowthGoal);
                value=new Capacity(goal.active,goal.total,value.builders);policyEvidence=goal.evidence;
                value=new Capacity(value.active,value.total,constructionBuilderTarget());
            }
            if(host.recoveryCapacityNeed()>visibleRecoveryConstruction()+pendingRecoveryConstruction()
                    &&armed>=6&&count("builder")>0)value=new Capacity(value.active,value.total,Math.max(2,value.builders));
            // Grow at most eight slots per evaluation. Never cancel already paid units when income falls.
            int nextTarget=Math.min(Math.max(value.total,militaryCapacityFloor),armyTarget+8);
            if(host.economicArmyPolicyStrategy())nextTarget=Math.max(nextTarget,committedArmed()+unobservedCombatSlots);
            armyTarget=Math.min(hardCap,nextTarget);activeTarget=Math.min(value.active,armyTarget);
            reserveTarget=Math.max(0,armyTarget-activeTarget);builderTarget=value.builders;
            Map<String,Object> capacityData=map("armyTarget",armyTarget,"activeArmyTarget",activeTarget,"reserveTarget",reserveTarget,
                "hardSafetyCap",hardCap,"builderTarget",builderTarget,"mapTiles",area,"incomeEstimate",income,
                "productionConsumption",consumption,"knownCapabilityNeeds",needs.size(),"resourceBacklog",backlog,
                "buildBacklog",buildBacklog,"fixedCapOverride",System.getProperty("rwagent.mobileUnitHardCap")!=null);
            capacityData.putAll(policyEvidence);emit("strategy_capacity",capacityData);
        }
    }
    /** Only current-visible, native-compatible ordinary actors with a known approach create
     * ordinary route demand. Capability needs and hidden contacts alone cannot authorize tanks.
     */
    String ordinaryDemand(String product){
        boolean visible=false,unknown=false;
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies")){
            visible=true;Assessment a=assessments.get(id(enemy));
            if(a==null||!a.complete||now-a.checked>15000||Math.hypot(n(enemy,"x")-a.x,n(enemy,"y")-a.y)>20){unknown=true;continue;}
            if(a.usefulProducts.contains(product))return "KNOWN";
            if(!a.checkedProducts.contains(product)||a.unknownProducts.contains(product))unknown=true;
        }
        return !visible?"NONE":unknown?"UNKNOWN":"ROUTE_UNAVAILABLE";
    }
    boolean increaseMilitaryCapacity(int committed,int slots,Map<String,Object> evidence)throws Exception{
        if(!enabled||homeEmergency||committed>=hardCap||committed<armyTarget||armyTarget>=hardCap||slots<=0)return false;
        int growthBound=hardCap;
        if(host.economicArmyPolicyStrategy()){
            if(economicMilitaryGrowthGoal<0)return false;
            growthBound=Math.min(growthBound,economicMilitaryGrowthGoal);
            if(armyTarget>=growthBound)return false;
        }
        int old=armyTarget;armyTarget=Math.min(growthBound,armyTarget+Math.min(8,slots));
        militaryCapacityFloor=armyTarget;militaryCapacityIncreases++;
        activeTarget=Math.min(armyTarget-8,host.economicArmyPolicyStrategy()?activeTarget:activeTarget+(armyTarget-old));
        reserveTarget=Math.max(0,armyTarget-activeTarget);
        Map<String,Object> data=new LinkedHashMap<String,Object>(evidence);
        data.put("oldTarget",old);data.put("newTarget",armyTarget);data.put("slotsAdded",armyTarget-old);
        if(host.economicArmyPolicyStrategy())data.put("economicMilitaryGrowthGoal",economicMilitaryGrowthGoal);
        data.put("reason","ARMY_CAPACITY_LIMIT");data.put("decision","BOUNDED_MILITARY_CAPACITY_INCREASE");
        emit("military_capacity_increased",data);return true;
    }
    /** Bounded policy targets. Area/observed threats/tasks create demand; income supports capacity. */
    static final class Capacity {
        final int active,total,builders;
        Capacity(int active,int total,int builders){this.active=active;this.total=total;this.builders=builders;}
    }
    static Capacity capacity(double area,double income,int threats,int needs,int backlog,boolean buildBacklog,
                             int army,long age,int hard,boolean fixed){
        double scale=Math.max(1,Math.min(3,Math.sqrt(area/12100.0)));
        int demand=24+(int)Math.ceil((scale-1)*24)+Math.min(24,threats*2)+(needs>0?8:0);
        int supported=32+(int)Math.max(0,(income-45)*.65);
        int total=fixed?hard:Math.min(hard,age<90000?40:Math.max(32,Math.min(96,Math.min(demand+12,supported))));
        int builders=age>=90000&&army>=12&&income>=70&&backlog>=2&&(buildBacklog||area>=20000)?2:1;
        return new Capacity(Math.min(demand,Math.max(1,total-8)),total,builders);
    }
    private void refreshAssessments(List<Map<String,Object>> force)throws Exception{
        if(force.isEmpty())return;
        List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> actor:force)if(!arbiter.reserved(id(actor))
                ||host.assessOwnedOrdinaryStrategy()&&ready(actor))actors.add(actor);
        if(actors.isEmpty())return;
        String ids=ids(actors);int queried=0;
        List<Map<String,Object>> contacts=new ArrayList<Map<String,Object>>(items(enemies,"visibleEnemies"));
        Collections.sort(contacts,(a,b)->Boolean.compare(Boolean.TRUE.equals(b.get("building")),Boolean.TRUE.equals(a.get("building"))));
        for(Map<String,Object> enemy:contacts){
            long eid=id(enemy);Assessment assessment=assessments.get(eid);
            if(assessment==null){assessment=new Assessment();assessments.put(eid,assessment);}
            if(now-assessment.checked<10000&&ids.equals(assessment.signature)
                    &&Math.hypot(n(enemy,"x")-assessment.x,n(enemy,"y")-assessment.y)<20)continue;
            if(queried++>=4)break;
            Map<String,Object> answer=null;List<Map<String,Object>> combined=new ArrayList<Map<String,Object>>();boolean complete=true;
            for(int offset=0;offset<actors.size();offset+=48){
                Map<String,Object> part=host.readStrategy("/combat/engagement?unitIds="+ids(actors.subList(offset,Math.min(actors.size(),offset+48)))+"&targetId="+eid,"engagement_observation");
                if(part==null||!Boolean.TRUE.equals(part.get("targetVisible"))){complete=false;break;}
                if(answer!=null&&Math.hypot(number(part,"targetX",0)-number(answer,"targetX",0),number(part,"targetY",0)-number(answer,"targetY",0))>20){complete=false;break;}
                answer=new LinkedHashMap<String,Object>(part);combined.addAll(items(part,"actors"));
            }
            assessment.checked=now;assessment.signature=ids;
            if(!complete||answer==null){
                assessment.complete=false;assessment.usefulProducts.clear();
                emit("engagement_assessment_deferred",map("targetId",eid,"reason","CONTACT_LOST_OR_MOVED_BETWEEN_FORMATION_BATCHES"));
                continue;
            }
            answer.put("actors",combined);
            host.emitStrategy("engagement_assessment",answer);
            if(Math.hypot(number(answer,"targetX",0)-assessment.x,number(answer,"targetY",0)-assessment.y)>20)
                assessment.blocked.clear();
            assessment.x=number(answer,"targetX",0);assessment.y=number(answer,"targetY",0);
            int blocked=0,incompatible=0,total=0;boolean possible=false;assessment.complete=true;
            assessment.usefulProducts.clear();assessment.checkedProducts.clear();assessment.unknownProducts.clear();
            for(Map<String,Object> actor:items(answer,"actors")){
                long uid=(long)n(actor,"unitId");total++;
                Map<String,Object> own=find(state,uid);
                if(own!=null){
                    String product=String.valueOf(own.get("type"));assessment.checkedProducts.add(product);
                    if(!"INCOMPATIBLE".equals(actor.get("compatibility"))&&!"BLOCKED_TERRAIN".equals(actor.get("status"))
                            &&!("COMPATIBLE".equals(actor.get("compatibility"))&&"APPROACH_PATH_KNOWN".equals(actor.get("status"))))
                        assessment.unknownProducts.add(product);
                }
                if("BLOCKED_TERRAIN".equals(actor.get("status"))){assessment.blocked.add(uid);blocked++;}
                else if("APPROACH_PATH_KNOWN".equals(actor.get("status")))assessment.blocked.remove(uid);
                // UNKNOWN cannot release a prior terrain rejection for the same last-known objective.
                if("INCOMPATIBLE".equals(actor.get("compatibility")))incompatible++;
                if("APPROACH_PATH_KNOWN".equals(actor.get("status"))&&"COMPATIBLE".equals(actor.get("compatibility"))){
                    possible=true;
                    if(own!=null&&("heavyTank".equals(own.get("type"))||"c_tank".equals(own.get("type"))||"tank".equals(own.get("type"))))
                        assessment.usefulProducts.add(String.valueOf(own.get("type")));
                }
            }
            if(total>0&&(blocked==total||incompatible==total)){
                Need need=needs.get(eid);boolean fresh=need==null;
                if(fresh){need=new Need(eid);needs.put(eid,need);}
                need.type=String.valueOf(enemy.get("type"));need.x=assessment.x;need.y=assessment.y;
                need.building=Boolean.TRUE.equals(enemy.get("building"));
                need.seen=(long)number(answer,"targetObservedAtGameTimeMs",now);
                need.reason=incompatible==total?"WEAPON_DOMAIN_GAP":"MOVEMENT_APPROACH_GAP";
                if(fresh)emit("capability_need_created",map("targetId",eid,"targetType",need.type,"reason",need.reason,
                    "x",need.x,"y",need.y,"observedAtGameTimeMs",need.seen,"evidence",answer));
            }else if(possible&&needs.containsKey(eid)&&!assigned(eid)){
                needs.remove(eid);capabilityTasks.remove(eid);emit("capability_need_released",map("targetId",eid,"reason","CURRENT_FORCE_HAS_NEW_APPROACH_EVIDENCE"));
            }
        }
        // Bound historical geometry without forgetting an unresolved need.
        if(assessments.size()>256){Iterator<Long> it=assessments.keySet().iterator();while(it.hasNext()&&assessments.size()>256){Long id=it.next();if(!needs.containsKey(id))it.remove();}}
    }
    List<Map<String,Object>> eligible(Map<String,Object> enemy,List<Map<String,Object>> force){
        if(!enabled)return force;
        Assessment a=assessments.get(id(enemy));if(a==null||a.blocked.isEmpty())return force;
        List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> actor:force)if(!a.blocked.contains(id(actor)))result.add(actor);
        return result;
    }
    boolean rejected(long enemyId){Assessment a=assessments.get(enemyId);return enabled&&a!=null&&!a.blocked.isEmpty()&&needs.containsKey(enemyId);}
    private void clearNeeds()throws Exception{
        for(Map<String,Object> intel:items(enemies,"enemyIntel"))if("CLEARED".equals(intel.get("status"))){
            long id=id(intel);CapabilityTask task=capabilityTasks.remove(id);if(task!=null)task.legalSiteCleared();if(needs.remove(id)!=null){resolvedNeeds++;emit("capability_need_resolved",map("targetId",id,"reason","LEGAL_SITE_CLEARED","evidence",intel));}
            assessments.remove(id);
        }
    }
    private boolean assigned(long target){
        for(Worker w:workers.values())if(w.target==target)return true;
        for(Purchase p:purchases.values())if(p.need==target)return true;
        return false;
    }
    private boolean available(Need need){return need!=null&&need.failures<3&&need.seen>need.awaitVisibleAfter;}
    private Need eligibleNeed(){for(Need need:needs.values())if(available(need)&&!assigned(need.id))return need;return null;}
    private void bind(Worker worker,Need need,String reason,long producer)throws Exception{
        worker.prospect.clear();
        detachCapability(worker);worker.target=need.id;
        CapabilityTask collective=capabilityTasks.get(need.id);
        if(collective==null){collective=new CapabilityTask(need.id,need.id,need.reason);capabilityTasks.put(need.id,collective);}
        collective.attach(worker.unit,worker.mode);
        emit("strategy_worker_committed",map("taskId",worker.task,"unitId",worker.unit,"needId",need.id,
            "reason",reason,"producerId",producer,"assignmentSemantics","OBSERVED_AVAILABLE_ROLE_NOT_FACTORY_PROVENANCE"));
    }
    private void detachCapability(Worker worker){CapabilityTask task=capabilityTasks.get(worker.target);if(task!=null)task.detach(worker.unit);}
    private double effectiveCredits(){return host.effectiveStrategyCredits(n(obj(state.get("player")),"credits"));}

    private void refreshMilitaryInvestment()throws Exception{
        if(!host.endgameStrategy())return;
        militaryInvestment=MilitaryInvestmentPolicy.assess(state,enemies,activeTarget,endgameSpatialDuty());
        refreshEndgameProviderDemand();
        String signature=militaryInvestment.get("reason")+":"+rearAirProviders()+":"+airGeneral.size();
        if(!signature.equals(lastMilitaryInvestment)){lastMilitaryInvestment=signature;emit("strategic_marginal_investment",militaryInvestment);}
        airFundingWorker=null;airFundingQuote=null;airFundingCost=0;
        airFundingProducer=-1;airFundingAction=null;
        if(!preferSpatialInvestment()||homeEmergency)return;
        // Protect one actual next air manufacture, not an inventory ratio or all providers' wish lists.
        // If an arrived mine needs these same funds it takes precedence over this new soft pipeline.
        if(frontierConstructionReserve()>0&&effectiveCredits()<protectedFunds+Math.max(0,endgameJetQuote))return;
        if(rearAirProviders()+pendingEndgameProviders()<endgameProviderTarget){
            Map<String,Object> menu=host.readStrategy("/combat/production","endgame_provider_funding_quote");
            for(Map<String,Object> f:items(menu,"factories"))if(number(f,"queue",-1)==0&&!pending(id(f)))
                for(Map<String,Object> a:items(f,"actions"))if("combatEngineer".equals(a.get("type"))&&a.get("actionId") instanceof String
                        &&Double.isFinite(number(a,"cost",Double.NaN))&&n(a,"cost")>0
                        &&(frontierConstructionReserve()==0||effectiveCredits()>=protectedFunds+n(a,"cost"))){
                    airFundingProducer=id(f);airFundingAction=String.valueOf(a.get("actionId"));airFundingCost=n(a,"cost");
                    emit("endgame_provider_funding_committed",costContext(menu,map("producerId",airFundingProducer,"nativeCost",airFundingCost,
                        "reason","ACTUAL_AIR_SERVICE_NEEDS_MORE_MANUFACTURERS","acceptedSpending",false)));return;
                }
        }
        for(Worker w:workers.values()){
            Map<String,Object> a=find(state,w.unit);
            if(a==null||!ready(a)||!w.endgameProvider||!"combatEngineer".equals(a.get("type"))||!"IDLE".equals(w.job)
                    ||w.paidConstruction||w.target>=0||distance(a,homeX,homeY)>650||nearThreat(n(a,"x"),n(a,"y"),400))continue;
            Map<String,Object> q=host.readStrategy("/economy/construction-plan?unitId="+w.unit+"&type=amphibiousJet","endgame_air_funding_quote");
            if(q==null||!Objects.equals(state.get("sessionId"),q.get("sessionId"))||number(q,"unitId",-1)!=w.unit
                    ||!"amphibiousJet".equals(q.get("type"))||!(q.get("actionId") instanceof String)
                    ||!Double.isFinite(number(q,"cost",Double.NaN))||n(q,"cost")<=0
                    ||!Double.isFinite(number(q,"x",Double.NaN))||!Double.isFinite(number(q,"y",Double.NaN)))continue;
            if(frontierConstructionReserve()>0&&effectiveCredits()<protectedFunds+n(q,"cost"))continue;
            airFundingWorker=w;airFundingQuote=new LinkedHashMap<String,Object>(q);airFundingQuote.putAll(host.costSourceStrategy(q));airFundingCost=n(q,"cost");endgameJetQuote=airFundingCost;
            emit("endgame_air_funding_committed",costContext(q,map("providerId",w.unit,"nativeCost",airFundingCost,"priorityReason",militaryInvestment,
                    "acceptedSpending",false,"fixedAirRatio",false,"otherManufacturersUnreserved",true)));break;
        }
    }

    private boolean arrivedMinePosition(Worker w,Map<String,Object> own,Map<String,Object> plan){
        if(own==null||plan==null)return false;
        // Arrival establishes a local working cluster, not contact with a mineral's center.
        // The same current native local plan that the allocator can actually build must be
        // funded before production. Do not demand an extra MOVE to a static rally when a
        // visible, reachable native build candidate is already available in that cluster.
        return w.prospect.arrived()&&w.prospect.local(n(plan,"extractorX"),n(plan,"extractorY"))
            &&distance(own,w.prospect.x(),w.prospect.y())<=ProspectCommitment.CLUSTER_RADIUS;
    }
    private boolean currentMineSite(Worker w,Map<String,Object> plan){
        Map<String,Object> own=find(state,w.unit);
        return own!=null&&ready(own)&&"builder".equals(own.get("type"))&&"IDLE".equals(w.job)&&!w.paidConstruction
            &&arbiter.owns(w.owner,w.unit)&&plan!=null&&"planned".equals(plan.get("status"))
            &&Objects.equals(state.get("sessionId"),plan.get("sessionId"))&&number(plan,"builderId",-1)==w.unit
            &&Double.isFinite(number(plan,"extractorCost",Double.NaN))&&n(plan,"extractorCost")>0
            &&Double.isFinite(number(plan,"extractorX",Double.NaN))&&Double.isFinite(number(plan,"extractorY",Double.NaN))
            &&arrivedMinePosition(w,own,plan)
            &&!nearThreat(n(plan,"extractorX"),n(plan,"extractorY"),350)
            &&!resourceCommitted(n(plan,"extractorX"),n(plan,"extractorY"),w);
    }
    private void releaseMineFunding(Worker w,String reason)throws Exception{
        if(w.mineReserve<=0)return;
        double cost=w.mineReserve;w.mineReserve=0;w.mineQuote=null;
        emit("strategy_frontier_funding_released",map("unitId",w.unit,"cost",cost,"reason",reason,"creditsRefunded",false));
    }
    private void refreshFrontierFunding()throws Exception{
        if(!host.economicArmyPolicyStrategy())return;
        for(Worker w:workers.values()){
            Map<String,Object> own=find(state,w.unit);
            if(own==null||!ready(own)||!"builder".equals(own.get("type"))||!"IDLE".equals(w.job)||w.paidConstruction||!arbiter.owns(w.owner,w.unit)){
                releaseMineFunding(w,"CURRENT_WORKER_OR_JOB_UNAVAILABLE");continue;
            }
            if(w.mineQuote!=null&&(!arrivedMinePosition(w,own,w.mineQuote)
                    ||nearThreat(n(w.mineQuote,"extractorX"),n(w.mineQuote,"extractorY"),350))){releaseMineFunding(w,"ARRIVED_SITE_OR_CURRENT_RISK_CHANGED");}
            // Travel is a soft intention. After an own-position arrival, the current native
            // local footprint is actionable work; static future resources remain unreserved.
            if(w.mineReserve<=0&&!w.prospect.arrived())continue;
            Map<String,Object> quote=host.readStrategy("/expansion/plan?unitId="+w.unit,"strategy_frontier_funding_quote");
            if(currentMineSite(w,quote)){
                boolean changed=w.mineQuote==null||w.mineReserve!=n(quote,"extractorCost")
                    ||Math.hypot(n(w.mineQuote,"extractorX")-n(quote,"extractorX"),n(w.mineQuote,"extractorY")-n(quote,"extractorY"))>1;
                w.mineQuote=new LinkedHashMap<String,Object>(quote);w.mineQuote.putAll(host.costSourceStrategy(quote));w.mineReserve=n(quote,"extractorCost");
                if(changed)emit("strategy_frontier_funding_committed",costContext(quote,map("unitId",w.unit,"owner",w.owner,"nativeCost",w.mineReserve,
                    "x",n(quote,"extractorX"),"y",n(quote,"extractorY"),"reason","CURRENT_NATIVE_LOCAL_SITE_AND_LATER_OWN_CLUSTER_ARRIVAL",
                    "arrivalSourceFrame",state.get("frame"),"travelReserve",false,"acceptedSpending",false)));
            }else if(quote!=null)releaseMineFunding(w,"CURRENT_NATIVE_SITE_CHANGED");
            // An unavailable endpoint is UNKNOWN; it is not evidence that a valid commitment vanished.
        }
        protectedFunds+=frontierConstructionReserve();
        if(frontierConstructionReserve()>0&&capabilityFunding!=null&&effectiveCredits()<protectedFunds+capabilityFunding.cost)
            releaseCapabilityFunding("ARRIVED_NATIVE_MINE_HAS_CONSTRUCTION_PRIORITY");
    }
    private boolean actFrontierFunding()throws Exception{
        boolean acted=false;
        for(Worker w:workers.values())if(w.mineReserve>0&&currentMineSite(w,w.mineQuote)){
            double free=effectiveCredits()-Math.max(0,protectedFunds-w.mineReserve)-capabilityReserve();
            if(free<w.mineReserve){emit("strategy_frontier_funding_waiting",map("unitId",w.unit,"nativeCost",w.mineReserve,"freeCredits",free,"gameTimeMs",now));continue;}
            Map<String,Object> q=w.mineQuote;double cost=w.mineReserve;
            if(startBuild(w,q,"/command/build-extractor?unitId="+w.unit+"&x="+n(q,"extractorX")+"&y="+n(q,"extractorY"),
                    "extractorT1",n(q,"extractorX"),n(q,"extractorY"),(long)cost,"MINE")){
                releaseMineFunding(w,"NATIVE_CONSTRUCTION_ACCEPTED_SPENDING_WITNESS_PENDING");protectedFunds=Math.max(0,protectedFunds-cost);acted=true;
                if(!host.parallelStrategy())break;
            }
        }
        return acted;
    }
    private boolean joinableResponse(Need need){
        if(!host.parallelStrategy())return false;
        CapabilityTask task=capabilityTasks.get(need.id);if(task==null)return false;
        for(Long unit:task.members().keySet()){Map<String,Object> actor=find(state,unit);if(actor!=null&&"amphibiousJet".equals(actor.get("type")))return true;}
        return false;
    }
    private void claimWorkers()throws Exception{
        long baselineBuilder=Long.MAX_VALUE;
        for(Map<String,Object> unit:units(state))if(ready(unit)&&"builder".equals(unit.get("type")))baselineBuilder=Math.min(baselineBuilder,id(unit));
        for(Map<String,Object> unit:units(state)){
            long uid=id(unit);String type=String.valueOf(unit.get("type"));
            if(!ready(unit)||!("combatEngineer".equals(type)||"amphibiousJet".equals(type)||"builder".equals(type)&&uid!=baselineBuilder))continue;
            if(workers.containsKey(uid)||arbiter.reserved(uid))continue;
            Worker w=new Worker(uid,++taskSequence);
            if(arbiter.claim(w.owner,uid)){
                workers.put(uid,w);emit("task_ownership_acquired",map("taskId",w.task,"owner",w.owner,"unitId",uid,"role","STRATEGIC_SPECIALIST"));
            }
        }
        Worker baseline=workers.get(baselineBuilder);
        if(baseline!=null&&"IDLE".equals(baseline.job)&&baseline.supportReserve==0&&!economicFrontCommitted(baseline)){release(baseline,"BASELINE_BUILDER_HANDOVER");workers.remove(baselineBuilder);}
        for(Map.Entry<Long,Purchase> entry:purchases.entrySet()){
            Purchase purchase=entry.getValue();Need need=needs.get(purchase.need);
            if(!"combatEngineer".equals(purchase.product)||need==null&&!purchase.endgameProvider||purchase.fulfilled)continue;
            for(Worker worker:workers.values())if(worker.target<0&&"IDLE".equals(worker.job)&&!worker.endgameProvider&&!worker.paidConstruction&&worker.supportReserve==0){
                Map<String,Object> actor=find(state,worker.unit);
                if(actor!=null&&ready(actor)&&"combatEngineer".equals(actor.get("type"))
                        &&!purchase.before.contains(worker.unit)&&!matchedPurchasedWorkers.contains(worker.unit)){
                    tracePurchaseMatch(entry.getKey(),purchase,worker);
                    matchedPurchasedWorkers.add(worker.unit);
                    purchase.need=-1;purchase.fulfilled=true;
                    if(purchase.endgameProvider)retainRearAirProvider(worker,"ENDGAME_PROVIDER_PURCHASE_FULFILMENT");
                    else bind(worker,need,"PURCHASE_NEED_FULFILMENT",entry.getKey());break;
                }
            }
        }
    }
    /** Existing own force supports a second spatial role; this is not proof of enemy defeat. */
    private void observeEndgame()throws Exception{
        longTermCapital.observe(String.valueOf(state.get("sessionId")),now,n(obj(state.get("player")),"credits"));
        int healthyGround=0;
        for(Map<String,Object> u:army(state))if(ready(u)&&!"amphibiousJet".equals(u.get("type"))&&!"combatEngineer".equals(u.get("type"))
                &&!"scout".equals(u.get("type"))&&n(u,"maxHp")>0&&n(u,"hp")/n(u,"maxHp")>=.75)healthyGround++;
        endgameReady=healthyGround>=activeTarget&&activeTarget>=LocalArmyDirector.FORM_MINIMUM&&!homeEmergency
                &&ordinaryNativePrice>0;
        if(endgameJetQuote>0)endgameAirTarget=constructionCount("amphibiousJet")+longTermCapital.airStrength(income,consumption,endgameJetQuote);
        if(endgameReady){Iterator<Worker> it=workers.values().iterator();while(it.hasNext()){
            Worker w=it.next();Map<String,Object> a=find(state,w.unit);
            if(a==null||!"amphibiousJet".equals(a.get("type"))||!"IDLE".equals(w.job)||w.paidConstruction||w.supportReserve>0)continue;
            boolean currentNeed=false;for(Map<String,Object> e:items(enemies,"visibleEnemies"))if(id(e)==w.target)currentNeed=true;
            if(w.target>=0&&currentNeed)continue; // An active current Capability commitment is not stolen.
            if(airGeneral.accept(w.unit,w.owner,w.mode)){
                detachCapability(w);emit("strategy_specialist_handover",map("unitId",w.unit,"taskId",w.task,"oldOwner",w.owner,
                    "newOwner",AirGeneralController.OWNER,"reason","IDLE_SPECIALIST_TO_LONG_TERM_SPATIAL_DUTY","oldNeedId",w.target));it.remove();
            }
        }}
        airGeneral.observe(state,enemies,scout,homeX,homeY);
        refreshEndgameProviderDemand();
    }
    private double groundProductionRunway(){return ordinaryNativePrice>0?ordinaryNativePrice*(preferSpatialInvestment()?1:count("landFactory")):Double.POSITIVE_INFINITY;}
    private int rearAirProviders(){int count=0;for(Worker w:workers.values()){
        Map<String,Object> a=find(state,w.unit);if(a!=null&&alive(a)&&"combatEngineer".equals(a.get("type"))&&w.endgameProvider)count++;
    }return count;}
    private int pendingEndgameProviders(){int count=0;for(Purchase p:purchases.values())if(p.endgameProvider&&!p.fulfilled&&"combatEngineer".equals(p.product))count++;return count;}
    /** A legal unfinished information duty is not a claim that the mainland is cleared. */
    private boolean endgameSpatialDuty(){
        return endgameReady&&(airGeneral.size()>0||!needs.isEmpty()
                ||n(scout,"mapMemoryNeverSeenTiles")+n(scout,"mapMemoryDeepFogTiles")+n(scout,"mapMemoryStaleFogTiles")>0);
    }
    private void refreshEndgameProviderDemand()throws Exception{
        int ready=rearAirProviders(),pending=pendingEndgameProviders(),funded=0;
        for(Worker w:workers.values())if(w.endgameProvider){Map<String,Object> a=find(state,w.unit);
            if(a!=null&&ready(a)&&"combatEngineer".equals(a.get("type"))&&longTermCapital.airFundedRecently(w.unit,now))funded++;
        }
        int headroom=(int)Math.min(Integer.MAX_VALUE,Math.max(0L,(long)hardCap-committedArmed()-unobservedCombatSlots));
        endgameProviderTarget=longTermCapital.airProviderTarget(ready,pending,funded,endgameSpatialDuty(),homeEmergency,headroom);
        double service=longTermCapital.observedAirProviderSpendPerSecond();
        if(preferSpatialInvestment()&&!homeEmergency&&pending==0&&funded>=ready&&service>0&&Double.isFinite(income)&&income>0){
            double groundReplacementFlow=count("landFactory")>0?consumption/count("landFactory"):consumption;
            int flowProviders=(int)Math.min((long)ready+headroom,Math.ceil(Math.max(0,income-groundReplacementFlow)/service));
            endgameProviderTarget=Math.max(endgameProviderTarget,flowProviders);
        }
        String signature=ready+":"+pending+":"+funded+":"+endgameProviderTarget+":"+endgameSpatialDuty();
        if(!signature.equals(lastEndgameProviderDemand)){
            lastEndgameProviderDemand=signature;
            emit("endgame_air_provider_demand",map("readyProviders",ready,"pendingProviders",pending,"recentNativeFundedProviders",funded,
                    "targetProviders",endgameProviderTarget,"spatialDuty",endgameSpatialDuty(),"cashHistory",longTermCapital.snapshot(),
                    "groundRole","CURRENT_FORMATIONS_AND_LAWFUL_LAND_FRONTIERS_CONTINUE","mainlandCleared","UNKNOWN",
                    "reason","OBSERVED_IDLE_CAPITAL_AND_NATIVE_AIR_SERVICE","militaryHeadroom",headroom));
        }
    }
    private void retainRearAirProvider(Worker w,String reason)throws Exception{
        if(w.endgameProvider)return;
        w.endgameProvider=true;emit("strategy_rear_air_provider_assigned",map("unitId",w.unit,"taskId",w.task,"owner",w.owner,
            "reason",reason,"role","ENDGAME_AIR_PROVIDER","needId",w.target,"assignmentSemantics","OBSERVED_AVAILABLE_ROLE_NOT_FACTORY_PROVENANCE"));
    }
    private double endgameFree(){return effectiveCredits()-protectedFunds-capabilityReserve()-groundProductionRunway();}
    private double capitalFree(){return endgameFree()-(endgameJetQuote>0?endgameJetQuote:0);}
    private boolean actualEndgamePlan(Map<String,Object> plan,long actor,String product){
        return plan!=null&&arbiter.stamp().session.equals(plan.get("sessionId"))&&number(plan,"unitId",-1)==actor
            &&product.equals(plan.get("type"))&&plan.get("actionId") instanceof String&&Boolean.TRUE.equals(plan.get("affordable"))
            &&Double.isFinite(number(plan,"cost",Double.NaN))&&n(plan,"cost")>0&&Double.isFinite(number(plan,"x",Double.NaN))&&Double.isFinite(number(plan,"y",Double.NaN))
            &&!nearThreat(n(plan,"x"),n(plan,"y"),400);
    }
    private boolean actEndgame(long factoryTarget)throws Exception{
        if(!endgameReady)return false;
        boolean acted=false;
        // Independent rear providers manufacture aircraft; no new land-factory product fiction.
        for(Worker w:new ArrayList<Worker>(workers.values())){
            Map<String,Object> a=find(state,w.unit);
            if(a==null||!"combatEngineer".equals(a.get("type"))||!"IDLE".equals(w.job)||w.target>=0||w.paidConstruction||w.supportReserve>0||now<w.retryAt)continue;
            retainRearAirProvider(w,"AVAILABLE_IDLE_ENDGAME_MANUFACTURER");
            if(distance(a,homeX,homeY)>650){returnHome(w,0);if(workerAction(w))acted=true;continue;}
            if(nearThreat(n(a,"x"),n(a,"y"),400)||!safetyCapacityAvailable())continue;
            Map<String,Object> plan=airFundingWorker==w&&airFundingQuote!=null?airFundingQuote:
                    host.readStrategy("/economy/construction-plan?unitId="+w.unit+"&type=amphibiousJet","endgame_air_production_plan");
            if(actualEndgamePlan(plan,w.unit,"amphibiousJet")){
                endgameJetQuote=n(plan,"cost");
                // A funded production flow is additional capacity, not a total inventory cap.
                endgameAirTarget=constructionCount("amphibiousJet")+longTermCapital.airStrength(income,consumption,endgameJetQuote);
            }
            if(!actualEndgamePlan(plan,w.unit,"amphibiousJet")||n(plan,"cost")>endgameFree()+(airFundingWorker==w?airFundingCost:0))continue;
            if(startBuild(w,plan,"/command/construct?unitId="+w.unit+"&actionId="+encode(plan.get("actionId"))+"&x="+n(plan,"x")+"&y="+n(plan,"y"),
                    "amphibiousJet",n(plan,"x"),n(plan,"y"),(long)n(plan,"cost"),"BUILD")){
                w.endgameConstruction=true;acted=true;emit("endgame_air_investment",map("providerId",w.unit,"airTarget",endgameAirTarget,
                    "airOwnedAndCommitted",constructionCount("amphibiousJet"),"nativeCost",n(plan,"cost"),"groundProductionRunway",groundProductionRunway(),
                    "income",income,"landConsumption",consumption,"groundAdvantage","UNKNOWN_OWN_FORCE_SUPPORTED_SPATIAL_ROLE"));
                longTermCapital.airStarted(w.unit,now,n(plan,"cost"));
                if(airFundingWorker==w){airFundingCost=0;airFundingWorker=null;airFundingQuote=null;}
            }
        }
        refreshEndgameProviderDemand();
        // Long-lived capital is secondary to air throughput, real facility gaps and restoration.
        if(!longTermCapital.growing()||airGeneral.size()==0||rearAirProviders()==0
                ||pendingEndgameProviders()>0||rearAirProviders()<endgameProviderTarget
                ||constructionCount("landFactory")<factoryTarget||host.recoveryCapacityNeed()>visibleRecoveryConstruction()+pendingRecoveryConstruction())return acted;
        Map<String,Object> menu=host.readStrategy("/economy/investments","endgame_capital_menu");
        for(Map<String,Object> c:items(menu,"units"))if(("fabricatorT2".equals(c.get("product"))||"fabricatorT3".equals(c.get("product")))
                &&number(c,"queue",-1)==0&&!pending(id(c))&&Boolean.TRUE.equals(c.get("affordable"))&&n(c,"cost")>0&&n(c,"cost")<=capitalFree()){
            if(traceOrder(CommandArbiter.DEFAULT_OWNER,"/command/invest?unitId="+id(c)+"&actionId="+encode(c.get("actionId")),costContext(menu,
                map("product",c.get("product"),"cost",n(c,"cost"),"producerSlots",1,"investmentSlotAvailable",1,"reason","LONG_TERM_IDLE_CAPITAL_AFTER_WAR_CAPACITY")))!=null){
                purchases.put(id(c),purchase(String.valueOf(c.get("product")),-1));host.spendStrategy("CAPITAL_UPGRADE",(long)n(c,"cost"),String.valueOf(c.get("product")),id(c));acted=true;
            }
        }
        List<Worker> builders=new ArrayList<Worker>();for(Worker w:workers.values())if(homeConstructionAvailable(w)){Map<String,Object> a=find(state,w.unit);if(a!=null&&"builder".equals(a.get("type"))&&distance(a,homeX,homeY)<650)builders.add(w);}
        if(builders.isEmpty())for(Map<String,Object> a:units(state))if(ready(a)&&"builder".equals(a.get("type"))&&!arbiter.reserved(id(a))&&distance(a,homeX,homeY)<650){
            String order=String.valueOf(a.get("orderType"));if(!"none".equalsIgnoreCase(order)&&!"null".equals(order)&&!"idle".equalsIgnoreCase(order))continue;
            Worker w=new Worker(id(a),++taskSequence);if(arbiter.claim(w.owner,w.unit)){workers.put(w.unit,w);builders.add(w);break;}
        }
        if(builders.isEmpty()){
            List<Worker> idle=new ArrayList<Worker>();for(Worker w:workers.values())if(homeConstructionAvailable(w)){Map<String,Object> a=find(state,w.unit);if(a!=null&&"builder".equals(a.get("type")))idle.add(w);}
            Collections.sort(idle,(a,b)->Double.compare(distance(find(state,a.unit),homeX,homeY),distance(find(state,b.unit),homeX,homeY)));
            for(Worker w:idle){
                Map<String,Object> local=host.readStrategy("/expansion/plan?unitId="+w.unit,"endgame_local_mine_priority");
                if(local!=null&&n(local,"extractorCost")>0)continue;
                returnHome(w,0);if(workerAction(w))acted=true;emit("endgame_capital_builder_rally",map("unitId",w.unit,"reason","ONE_IDLE_CONSTRUCTOR_TO_HOME_CAPITAL_SITE","siteAuthority","NEEDS_LATER_CURRENT_NATIVE_PLAN"));break;
            }
        }
        for(Worker w:builders){
            // A known current local mine remains more valuable immediate infrastructure.
            Map<String,Object> mine=host.readStrategy("/expansion/plan?unitId="+w.unit,"endgame_local_mine_priority");
            if(mine!=null&&n(mine,"extractorCost")>0&&n(mine,"extractorCost")<=capitalFree())continue;
            Map<String,Object> plan=host.readStrategy("/economy/construction-plan?unitId="+w.unit+"&type=fabricatorT1","endgame_capital_construction_plan");
            if(!actualEndgamePlan(plan,w.unit,"fabricatorT1")||Math.hypot(n(plan,"x")-homeX,n(plan,"y")-homeY)>650||n(plan,"cost")>capitalFree())continue;
            if(startBuild(w,plan,"/command/construct?unitId="+w.unit+"&actionId="+encode(plan.get("actionId"))+"&x="+n(plan,"x")+"&y="+n(plan,"y"),
                    "fabricatorT1",n(plan,"x"),n(plan,"y"),(long)n(plan,"cost"),"BUILD")){
                w.endgameConstruction=true;acted=true;emit("endgame_capital_investment",map("providerId",w.unit,"nativeCost",n(plan,"cost"),"cashHistory",longTermCapital.snapshot(),"airGeneralSize",airGeneral.size(),"groundProductionRunway",groundProductionRunway()));
            }
        }
        return acted;
    }
    private void updatePurchases()throws Exception{
        Iterator<Map.Entry<Long,Purchase>> it=purchases.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Long,Purchase> entry=it.next();Purchase p=entry.getValue();Map<String,Object> unit=find(state,entry.getKey());
            if(p.need>=0&&!needs.containsKey(p.need))p.need=-1;
            if(unit==null||!alive(unit)){Need need=needs.get(p.need);if(need!=null)need.failures++;
                emit("strategy_purchase_lost",map("actorId",entry.getKey(),"product",p.product,"needId",p.need));it.remove();continue;}
            if(number(unit,"productionQueue",0)>0)p.active=true;
            boolean mineUpgrade=MineInvestmentPolicy.isUpgradeProduct(p.product)||"fabricatorT2".equals(p.product)||"fabricatorT3".equals(p.product);
            if(mineUpgrade&&p.product.equals(unit.get("type"))){
                boolean capital=p.product.startsWith("fabricator");if(capital)capitalUpgrades++;else upgrades++;
                emit(capital?"capital_upgrade_observed":"mine_upgrade_observed",map("unitId",id(unit),"product",p.product,"unit",unit));it.remove();
            }else if(!mineUpgrade&&(p.active||p.fulfilled)&&number(unit,"productionQueue",0)==0){
                if(!p.queueFinished){p.queueFinished=true;emit("strategy_production_queue_finished",map("producerId",id(unit),"product",p.product,
                    "needId",p.selectedNeed,"evidence",p.active?"QUEUE_BECAME_EMPTY":"AVAILABLE_WORKER_ASSIGNED"));}
                if(!"combatEngineer".equals(p.product)||p.fulfilled||p.need<0&&!p.endgameProvider)it.remove();
                else if(now-p.at>180000){Need need=needs.get(p.need);if(need!=null)need.failures++;
                    emit("strategy_purchase_unconfirmed",map("actorId",entry.getKey(),"product",p.product,"needId",p.need,"reason","OBSERVATION_TIMEOUT"));it.remove();}
            }else if(now-p.at>180000){Need need=needs.get(p.need);if(need!=null)need.failures++;
                emit("strategy_purchase_unconfirmed",map("actorId",entry.getKey(),"product",p.product,"needId",p.need,"reason","OBSERVATION_TIMEOUT"));it.remove();}
        }
    }
    private void updateWorkers()throws Exception{
        Iterator<Worker> it=workers.values().iterator();
        while(it.hasNext()){
            Worker w=it.next();Map<String,Object> actor=find(state,w.unit);
            if(actor==null||!alive(actor)){
                if(w.endgameProvider)longTermCapital.airUnconfirmed(w.unit);
                Need need=needs.get(w.target);if(need!=null)need.failures++;
                releaseSupportReserve(w,"PROVIDER_LOST");
                emit("strategy_task_lost",map("taskId",w.task,"unitId",w.unit,"job",w.job));release(w,"ACTOR_LOST");it.remove();continue;
            }
            if(w.target>=0&&!needs.containsKey(w.target)){
                emit("strategy_task_completed",map("taskId",w.task,"targetId",w.target,"reason","NEED_RELEASED_BY_LEGAL_EVIDENCE"));
                releaseSupportReserve(w,"NEED_RELEASED");detachCapability(w);w.target=-1;if(!"BUILD".equals(w.job))w.job="IDLE";
            }
            Need commitment=needs.get(w.target);
            if(commitment!=null&&commitment.failures>=3){
                emit("strategy_worker_commitment_released",map("taskId",w.task,"unitId",w.unit,"needId",w.target,"reason","NEED_ATTEMPT_LIMIT"));
                detachCapability(w);w.target=-1;if(!"RETURN".equals(w.job)&&!"BUILD".equals(w.job))returnHome(w,15000);
            }
            if(w.paidConstruction){
                for(Map<String,Object> unit:units(state))if(!w.before.contains(id(unit))&&!matchedConstructionProducts.contains(id(unit))&&ready(unit)
                        &&w.product.equals(unit.get("type"))&&distance(unit,w.buildX,w.buildY)<120){
                    traceReadyMatch(w,unit);
                    matchedConstructionProducts.add(id(unit));
                    completedJobs++;emit("strategy_construction_observed",map("taskId",w.task,"builderId",w.unit,"product",w.product,"unit",unit));
                    if(w.endgameConstruction&&"amphibiousJet".equals(w.product))longTermCapital.airCompleted(w.unit,now);
                    Worker responder=workers.get(id(unit));Need supported=needs.get(w.target);
                    if("amphibiousJet".equals(w.product)&&!w.endgameConstruction&&responder!=null&&responder.target<0&&supported!=null){
                        detachCapability(w);w.target=-1;emit("strategy_worker_commitment_released",map("taskId",w.task,"unitId",w.unit,
                            "needId",supported.id,"reason","SUPPORT_RESPONDER_HANDOVER"));
                        bind(responder,supported,"OBSERVED_SUPPORT_RESPONDER",-1);
                        emit("strategy_support_transferred",map("taskId",w.task,"unitId",w.unit,"responderId",responder.unit,"needId",supported.id));
                    }
                    w.paidConstruction=false;w.endgameConstruction=false;if(!"RETURN".equals(w.job))w.job="IDLE";w.retryAt=now+5000;break;
                }
                if(w.paidConstruction&&now-w.jobAt>150000){
                    if(w.endgameConstruction&&"amphibiousJet".equals(w.product))longTermCapital.airUnconfirmed(w.unit);
                    emit("strategy_task_blocked",map("taskId",w.task,"job",w.job,"reason","CONSTRUCTION_TIMEOUT"));
                    Need failed=needs.get(w.target);if(failed!=null)failed.failures++;
                    if(w.target>=0)emit("strategy_worker_commitment_released",map("taskId",w.task,"unitId",w.unit,"needId",w.target,"reason","CONSTRUCTION_TIMEOUT"));
                    detachCapability(w);w.target=-1;w.paidConstruction=false;if(!"RETURN".equals(w.job))w.job="IDLE";w.retryAt=now+30000;
                }
            }else if("MODE_APPROACH".equals(w.job)||"MODE_WAIT".equals(w.job)){
                double d=distance(actor,w.x,w.y);if(d+20<w.best){w.best=d;w.lastProgress=now;}
                if(now-w.jobAt>90000||now-w.lastProgress>45000){
                    Need failed=needs.get(w.target);if(failed!=null)failed.failures++;
                    emit("strategy_task_blocked",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,"reason","MODE_TRANSITION_NO_PROGRESS"));
                    returnHome(w,30000);
                }
            }else if("PROSPECT".equals(w.job)){
                double d=distance(actor,w.x,w.y);
                if(d+20<w.best){w.best=d;w.lastProgress=now;}
                if(d<75){
                    if(host.assessOwnedOrdinaryStrategy()){
                        Long frame=GameClock.number(state.get("frame"));
                        if(frame==null||!w.prospect.observeArrival(n(actor,"x"),n(actor,"y"),frame,now))continue;
                        emit("strategy_prospect_cluster_committed",map("taskId",w.task,"unitId",w.unit,"anchorX",w.prospect.x(),"anchorY",w.prospect.y(),
                            "paidTravel",w.prospect.paidTravel(),"sourceFrame",frame,"safety","CURRENT_NATIVE_SITE_STILL_REQUIRED"));
                    }
                    if(w.staticProspectTile>=0&&Math.hypot(n(actor,"x")-w.buildX,n(actor,"y")-w.buildY)>ProspectCommitment.CLUSTER_RADIUS)
                        w.prospect.clear();
                    emit("strategy_prospect_observed",map("taskId",w.task,"unitId",w.unit,"unit",actor));w.job="IDLE";w.retryAt=now;
                }
                else if(now-w.lastProgress>45000||now-w.jobAt>120000){
                    if(w.staticProspectTile>=0){w.prospects.add(w.staticProspectTile);w.staticProspectTile=-1;w.prospect.clear();}
                    emit("strategy_task_blocked",map("taskId",w.task,"job",w.job,"reason","PROSPECT_NO_PROGRESS"));w.job="IDLE";w.retryAt=now+15000;
                }
            }else if("RESPONSE".equals(w.job)||"INVESTIGATE".equals(w.job)){
                if(!needs.containsKey(w.target)){emit("strategy_task_completed",map("taskId",w.task,"targetId",w.target,"reason","NEED_RESOLVED"));w.job="IDLE";w.target=-1;continue;}
                assessResponse(w,actor);
                if("INVESTIGATE".equals(w.job)){
                    if(now-w.investigateAt>=15000){
                        Need need=needs.get(w.target);need.awaitVisibleAfter=Math.max(need.awaitVisibleAfter,w.investigateSeen);
                        emit("strategy_investigation_exhausted",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,
                            "lastSeenGameTimeMs",w.investigateSeen,"reason","LAST_KNOWN_APPROACH_REACHED_WITHOUT_FRESH_CONTACT"));
                        returnHome(w,15000);
                    }
                    continue;
                }
                if(!"RESPONSE".equals(w.job))continue;
                double d=distance(actor,w.x,w.y);
                if(d+20<w.best){w.best=d;w.lastProgress=now;emit("strategy_response_progress",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,"distance",d,"unit",actor));}
                for(Map<String,Object> enemy:items(enemies,"visibleEnemies"))if(id(enemy)==w.target){
                    if(n(enemy,"hp")<w.hp-0.01){
                        if(w.hp<Double.MAX_VALUE)emit("strategy_target_damage_observed",map("taskId",w.task,"targetId",w.target,"hpBefore",w.hp,"hpNow",n(enemy,"hp"),"attribution","TEAM_DAMAGE_NOT_EXCLUSIVE_TO_RESPONDER"));
                        w.hp=n(enemy,"hp");w.lastProgress=now;
                    }
                }
                if(now-w.lastProgress>60000||now-w.jobAt>240000){
                    Need need=needs.get(w.target);need.failures++;
                    emit("strategy_task_blocked",map("taskId",w.task,"targetId",w.target,"reason","NO_OBSERVED_PROGRESS","attempts",need.failures));
                    returnHome(w,45000);
                }
            }else if("RETURN".equals(w.job)&&distance(actor,homeX,homeY)<180&&now>=w.retryAt){
                Need waiting=needs.get(w.target);
                if(waiting!=null&&waiting.seen<=waiting.awaitVisibleAfter){
                    emit("strategy_worker_commitment_released",map("taskId",w.task,"unitId",w.unit,"needId",w.target,
                        "reason","STALE_SITE_INVESTIGATION_RETURNED_HOME"));detachCapability(w);w.target=-1;
                }
                if(n(actor,"hp")>=n(actor,"maxHp")*.3)w.job="IDLE";
            }
            boolean supportNeeded=repairFunding(w)?host.recoveryCapacityNeed()>visibleRecoveryConstruction()+pendingRecoveryConstruction():available(needs.get(w.target))&&safetyCapacityAvailable();
            if(w.supportReserve>0&&(now>=w.supportDeadline||homeEmergency||!supportNeeded))
                releaseSupportReserve(w,now>=w.supportDeadline?"FUNDING_TIMEOUT":homeEmergency?"HOME_EMERGENCY":"NEED_OR_CAPACITY_UNAVAILABLE");
        }
    }
    /** Legacy hosts retain one slot; scheduler hosts admit independent workers in one observation. */
    boolean act(double reserved,long factoryTarget)throws Exception{
        Map<String,Map<String,Object>> before=traceStates();
        try{return actPolicy(reserved,factoryTarget);}
        finally{traceStateChanges(before,"ACT");}
    }
    private boolean actPolicy(double reserved,long factoryTarget)throws Exception{
        if(!enabled||!arbiter.ready(now))return false;
        protectedFunds=reserved;
        boolean acted=actFrontierFunding();
        if(capabilityFunding!=null&&fulfilCapabilityFunding()){acted=true;if(!host.parallelStrategy())return true;}
        if(!arbiter.ready(now))return acted;
        // Recovery demand comes from the host's actual wounded-unit pool. Use existing leased
        // workers, native footprints and paid construction witnesses, without a cash threshold.
        int recoveryNeed=Math.max(0,host.recoveryCapacityNeed()-visibleRecoveryConstruction()-pendingRecoveryConstruction());
        boolean repairFundingSelected=false;for(Worker worker:workers.values())if(repairFunding(worker))repairFundingSelected=true;
        List<Worker> recoveryWorkers=new ArrayList<Worker>(workers.values());
        if(recoveryNeed>0&&host.borrowBaselineBuilderForRecovery())for(Map<String,Object> candidate:units(state)){
            if(ready(candidate)&&"builder".equals(candidate.get("type"))&&!workers.containsKey(id(candidate))&&!arbiter.reserved(id(candidate))){
                recoveryWorkers.add(new Worker(id(candidate),++taskSequence));break;}
        }
        Collections.sort(recoveryWorkers,(a,b)->Double.compare(distance(find(state,a.unit),homeX,homeY),distance(find(state,b.unit),homeX,homeY)));
        if(recoveryNeed>0)for(Worker worker:recoveryWorkers){
            if(recoveryNeed<=0||!arbiter.ready(now))break;
            Map<String,Object> actor=find(state,worker.unit);
            if(actor==null||!"IDLE".equals(worker.job)||worker.target>=0||now<worker.recoveryRetryAt
                    ||!ready(actor)||!("combatEngineer".equals(actor.get("type"))||"builder".equals(actor.get("type"))))continue;
            if(economicFrontCommitted(worker)){
                emit("strategy_home_construction_deferred",map("unitId",worker.unit,"product","repairbay",
                    "reason","ECONOMIC_FRONT_COMMITMENT_RETAINED","x",n(actor,"x"),"y",n(actor,"y")));
                continue;
            }
            if(host.economicArmyPolicyStrategy()&&distance(actor,homeX,homeY)>180){
                if(!workers.containsKey(worker.unit)){
                    if(!arbiter.claim(worker.owner,worker.unit))continue;
                    workers.put(worker.unit,worker);
                }
                returnHome(worker,0);
                emit("strategy_recovery_builder_rally",map("unitId",worker.unit,"owner",worker.owner,"x",homeX,"y",homeY,
                    "reason","HOME_REPAIR_SITE_BEFORE_NATIVE_CONSTRUCTION_PLAN","siteAuthority","NEEDS_CURRENT_NATIVE_PLAN"));
                if(workerAction(worker))acted=true;
                // One needed service project calls one constructor home, not every expansion worker.
                break;
            }
            Map<String,Object> plan=host.readStrategy("/economy/construction-plan?unitId="+worker.unit+"&type=repairbay","strategy_recovery_plan");
            double freeRecovery=effectiveCredits()-protectedFunds-capabilityReserve()+(repairFunding(worker)?worker.supportReserve:0);
            if(plan==null||!"repairbay".equals(plan.get("type"))
                    ||!(plan.get("actionId") instanceof String)||!(number(plan,"cost",-1)>0)
                    ||number(plan,"unitId",-1)!=worker.unit
                    ||!Double.isFinite(number(plan,"cost",Double.NaN))
                    ||recoverySiteRisk(number(plan,"x",Double.NaN),number(plan,"y",Double.NaN))){
                if(repairFunding(worker))releaseSupportReserve(worker,"CURRENT_REPAIR_PLAN_UNAVAILABLE_OR_UNSAFE");continue;
            }
            if(!Boolean.TRUE.equals(plan.get("affordable"))||n(plan,"cost")>freeRecovery){
                // Select one actual native repair project using the existing bounded
                // support funding path. Continuous unit purchases may not consume
                // its chosen funds forever; no whole-army recovery veto is created.
                if(host.economicArmyPolicyStrategy()&&!repairFundingSelected&&worker.supportReserve==0
                        &&income>0&&Double.isFinite(income)&&(n(plan,"cost")-freeRecovery)/income<=60
                        &&(workers.containsKey(worker.unit)||arbiter.claim(worker.owner,worker.unit))){
                    workers.put(worker.unit,worker);worker.supportReserve=n(plan,"cost");worker.supportQuote=new LinkedHashMap<String,Object>(plan);worker.supportDeadline=now+90000;repairFundingSelected=true;
                    emit("strategy_support_reserve_started",map("taskId",worker.task,"unitId",worker.unit,"product","repairbay","cost",worker.supportReserve,"deadlineGameTimeMs",worker.supportDeadline,"evidence",plan));
                }
                continue;
            }
            if(!workers.containsKey(worker.unit)){
                if(!arbiter.claim(worker.owner,worker.unit))continue;
                workers.put(worker.unit,worker);emit("task_ownership_acquired",map("taskId",worker.task,"owner",worker.owner,"unitId",worker.unit,"role","RECOVERY_CONSTRUCTION_BASELINE_LOAN"));
            }
            if(startBuild(worker,plan,"/command/construct?unitId="+worker.unit+"&actionId="+encode(plan.get("actionId"))
                    +"&x="+n(plan,"x")+"&y="+n(plan,"y"),"repairbay",n(plan,"x"),n(plan,"y"),(long)n(plan,"cost"),"BUILD")){
                recoveryNeed--;acted=true;emit("strategy_recovery_capacity_committed",map("unitId",worker.unit,
                    "nativeCost",n(plan,"cost"),"recoveryCapacityNeed",host.recoveryCapacityNeed(),"product","repairbay"));
                if(repairFunding(worker))releaseSupportReserve(worker,"CONSTRUCTION_ACCEPTED");
                if(!host.parallelStrategy())return true;
            }else worker.recoveryRetryAt=now+15000;
        }
        if(!arbiter.ready(now))return acted;
        for(Worker worker:workers.values()){if(!arbiter.ready(now))break;if(workerAction(worker)){acted=true;if(!host.parallelStrategy())return true;}}
        if(host.endgameStrategy()){if(airGeneral.act())acted=true;if(actEndgame(factoryTarget))acted=true;}
        if(!arbiter.ready(now))return acted;
        if(now-lastAllocation<8000)return acted;
        lastAllocation=now;
        double credits=effectiveCredits(),free=credits-protectedFunds-capabilityReserve();
        int force=army(state).size(),engineers=count("combatEngineer"),builders=count("builder");
        int backlog=resourceBacklog();
        Map<String,Object> providerMenu=null;
        if(host.economicArmyPolicyStrategy()){
            providerMenu=host.readStrategy("/combat/production","strategy_production_menu");
            builderNativePrice=-1;
            for(Map<String,Object> factory:items(providerMenu,"factories"))for(Map<String,Object> action:items(factory,"actions")){
                double price=number(action,"cost",Double.NaN);
                if("builder".equals(action.get("type"))&&action.get("actionId") instanceof String&&Double.isFinite(price)&&price>0)
                    builderNativePrice=builderNativePrice<0?price:Math.min(builderNativePrice,price);
            }
            int previous=builderTarget;builderTarget=constructionBuilderTarget();
            if(previous!=builderTarget)emit("strategy_construction_capacity",map("builderTarget",builderTarget,
                "constructionDomains",constructionDomains,"nativeBuilderCost",builderNativePrice,
                "actionableConstructionDomains",actionableConstructionDomains,"actionableFacilityJobs",constructionFacilityJobs,
                "militaryConstructionDomains",militaryConstructionDomains,"homeCapitalConstructionJobs",homeCapitalConstructionJobs,
                "idleConstructionWorkers",constructionIdleWorkers,"unmetConstructionJobs",constructionUnmetJobs,
                "incomeEstimate",income,"productionConsumption",consumption,"planningGameSeconds",30,
                "facilityDemand",host.constructionDemandStrategy(),"capacityModel","CURRENT_OWN_ECONOMIC_AND_GENERAL_FRONTS_AND_NATIVE_JOBS_MINUS_EXISTING_COVERAGE",
                "staticResourcesBoundary","EXPLORATION_DEMAND_ONLY_DYNAMIC_OCCUPANCY_SAFETY_AND_BUILDABILITY_UNKNOWN"));
        }
        Need selectedNeed=eligibleNeed();boolean unserved=selectedNeed!=null;
        // Explicit alternatives: a task capability gap outranks throughput, then safe growth.
        String product=null,reason=null;
        if(capabilityFunding==null&&now>=capabilityFundingRetryAt&&unserved&&engineers+pendingCount("combatEngineer")<Math.min(2,needs.size())&&force>=6&&!homeEmergency&&safetyCapacityAvailable()){product="combatEngineer";reason="UNSERVED_CAPABILITY_NEED";}
        else if(host.endgameStrategy()&&endgameReady&&rearAirProviders()+pendingEndgameProviders()<endgameProviderTarget&&safetyCapacityAvailable()){
            product="combatEngineer";reason="ENDGAME_AIR_PROVIDER";
        }
        else if(builders+pendingCount("builder")<builderTarget&&!homeEmergency){product="builder";reason="PARALLEL_CONSTRUCTION_BACKLOG";}
        Map<String,Object> alternatives=map("militaryDeficit",Math.max(0,armyTarget-force),"unservedCapabilityNeed",unserved,
            "constructionBacklog",backlog,"builders",builders,"builderTarget",builderTarget,"engineers",engineers,
            "freeCredits",free,"protectedFunds",protectedFunds,"remainingGameSeconds",remaining/1000);
        if(product!=null){
            Map<String,Object> menu=providerMenu!=null?providerMenu:host.readStrategy("/combat/production","strategy_production_menu");
            providerAllocation: if(menu!=null)for(Map<String,Object> factory:items(menu,"factories"))if(number(factory,"queue",-1)==0&&!pending(id(factory))){
                for(Map<String,Object> action:items(factory,"actions"))if(product.equals(action.get("type"))
                        &&action.get("actionId") instanceof String&&Double.isFinite(number(action,"cost",Double.NaN))
                        &&number(action,"cost",-1)>0&&Boolean.TRUE.equals(action.get("affordable"))
                        &&n(action,"cost")<=free+("ENDGAME_AIR_PROVIDER".equals(reason)&&id(factory)==airFundingProducer&&Objects.equals(airFundingAction,action.get("actionId"))?airFundingCost:0)){
                    alternatives.put("selected",reason);alternatives.put("action",action);emit("strategy_allocation",alternatives);
                    if("ENDGAME_AIR_PROVIDER".equals(reason)&&free+(id(factory)==airFundingProducer?airFundingCost:0)<n(action,"cost")+groundProductionRunway())continue;
                    long needId="UNSERVED_CAPABILITY_NEED".equals(reason)&&selectedNeed!=null?selectedNeed.id:-1;
                    Map<String,Object> receipt=traceOrder(CommandArbiter.DEFAULT_OWNER,"/command/queue?unitId="+id(factory)+"&actionId="+encode(action.get("actionId")),costContext(menu,map("needId",needId,"purchasePurpose",reason,"commitmentId",purchaseTraceId(id(factory),now),"product",product,"cost",n(action,"cost"),"producerSlots",1,"militarySlots","combatEngineer".equals(product)?1:0)));
                    if(receipt!=null){
                        purchases.put(id(factory),purchase(product,needId,"ENDGAME_AIR_PROVIDER".equals(reason)));
                        if("ENDGAME_AIR_PROVIDER".equals(reason)&&id(factory)==airFundingProducer){airFundingCost=0;airFundingProducer=-1;airFundingAction=null;}
                        if(needId>=0)emit("strategy_purchase_committed",map("needId",needId,"producerId",id(factory),"product",product));
                        investments++;host.spendStrategy("STRATEGIC_CAPABILITY",(long)n(action,"cost"),product,id(factory));acted=true;if(!host.parallelStrategy())return true;break providerAllocation;}
                }
            }
            credits=effectiveCredits();free=credits-protectedFunds-capabilityReserve();
            if("UNSERVED_CAPABILITY_NEED".equals(reason)&&selectedNeed!=null&&startCapabilityFunding(menu,free))free-=capabilityReserve();
        }
        // Local risk and the disclosed payback window replace the blanket cash-surplus veto.
        // A distant crisis does not erase a quiet mine; shared task funds and replacements stay held.
        if(force>=6&&remaining>0){
            if(ordinaryNativePrice<=0){
                Map<String,Object> nativeProduction=host.readStrategy("/combat/production","strategy_price_menu");
                double quote=-1;
                if(nativeProduction!=null)for(Map<String,Object> factory:items(nativeProduction,"factories"))for(Map<String,Object> action:items(factory,"actions"))
                    if("heavyTank".equals(action.get("type"))&&n(action,"cost")>0)quote=n(action,"cost");
                if(quote<0&&nativeProduction!=null)for(Map<String,Object> factory:items(nativeProduction,"factories"))for(Map<String,Object> action:items(factory,"actions"))
                    if(("tank".equals(action.get("type"))||"c_tank".equals(action.get("type")))&&n(action,"cost")>0)quote=n(action,"cost");
                noteOrdinaryNativePrice(quote);
            }
            Map<String,Object> menu=host.readStrategy("/economy/investments","strategy_investment_menu");
            double currentMineCost=host.economicArmyPolicyStrategy()?currentIdleMineCost():-1;
            if(menu!=null)for(Map<String,Object> candidate:items(menu,"units")){
                Map<String,Object> mine=find(state,id(candidate));
                double cost=number(candidate,"cost",-1);
                boolean cheaperMineReady=backlog>0&&idleWorkerNearResource();
                if(host.economicArmyPolicyStrategy())cheaperMineReady=currentMineCost>0&&currentMineCost<cost
                    &&credits-protectedFunds-capabilityReserve()-cost-2*ordinaryNativePrice<currentMineCost;
                MineInvestmentPolicy.Decision growth=mineInvestment.evaluate(mine,candidate,now,remaining/1000.0,
                    credits,protectedFunds+capabilityReserve(),ordinaryNativePrice,force,pending(id(candidate)),cheaperMineReady,
                    host.economicArmyPolicyStrategy(),host.mineSafeZoneStrategy(mine));
                emit("mine_income_investment_evaluated",new LinkedHashMap<String,Object>(growth.evidence));
                if(!growth.selected){
                    if(now-lastIncomeSurplusHold>=10000){
                        lastIncomeSurplusHold=now;Map<String,Object> hold=new LinkedHashMap<String,Object>(growth.evidence);
                        hold.put("mineId",id(candidate));emit("mine_income_investment_deferred",hold);
                    }
                    continue;
                }
                alternatives.putAll(growth.evidence);alternatives.put("selected","extractorT3_reinforced".equals(growth.product)
                    ?"MINE_SURVIVAL_INVESTMENT":"extractorT3_overclocked".equals(growth.product)?"MINE_OVERCLOCK_INCOME_INVESTMENT"
                    :"extractorT3".equals(growth.product)?"MINE_T3_INCOME_INVESTMENT":"MINE_T2_INCOME_INVESTMENT");
                alternatives.put("action",candidate);emit("strategy_allocation",alternatives);
                Map<String,Object> receipt=traceOrder(CommandArbiter.DEFAULT_OWNER,"/command/invest?unitId="+id(candidate)+"&actionId="+encode(candidate.get("actionId")),costContext(menu,map("cost",cost,"product",growth.product,"producerSlots",1,"quotedProducerQueue",candidate.get("queue"),"investmentSlotAvailable",pending(id(candidate))?0:1,"slotEvidenceSource","EXISTING_PURCHASE_COMMITMENT","commitmentId",purchaseTraceId(id(candidate),now))));
                if(receipt!=null){purchases.put(id(candidate),new Purchase(growth.product,now));investments++;host.spendStrategy("MINE_UPGRADE",(long)cost,growth.product,id(candidate));acted=true;if(!host.parallelStrategy())return true;credits=effectiveCredits();free=credits-protectedFunds-capabilityReserve();}
            }
        }
        // Combat engineers remain capability specialists. Only additional builders enter the
        // ordinary economic lane; a specialist may construct a responder for its explicit need.
        workerAllocation: for(Worker worker:workers.values())if("IDLE".equals(worker.job)&&now>=worker.retryAt&&!repairFunding(worker)){
            // The arrived worker already has concrete local construction, handled above.
            // Insufficient cash must not send another prospect MOVE that cancels its own bank.
            if(worker.mineReserve>0)continue;
            credits=effectiveCredits();free=credits-protectedFunds-capabilityReserve();
            Map<String,Object> actor=find(state,worker.unit);if(actor==null)continue;
            if("combatEngineer".equals(actor.get("type"))){
                if(startSupportBuild(worker,actor,free+worker.supportReserve)){acted=true;if(!host.parallelStrategy())return true;}
                continue;
            }
            if(!"builder".equals(actor.get("type")))continue;
            if(host.assessOwnedOrdinaryStrategy()&&worker.prospect.arrived()&&nearThreat(n(actor,"x"),n(actor,"y"),400)){
                worker.prospect.clear();emit("strategy_prospect_cluster_released",map("unitId",worker.unit,"reason","CURRENT_OR_CONSERVATIVE_LOCAL_THREAT"));}
            if(!nearThreat(n(actor,"x"),n(actor,"y"),400)){
                Map<String,Object> plan=host.readStrategy("/expansion/plan?unitId="+worker.unit,"strategy_expansion_plan");
                if(plan!=null&&number(plan,"extractorCost",Double.MAX_VALUE)<=free
                        &&(!host.economicArmyPolicyStrategy()||!resourceCommitted(n(plan,"extractorX"),n(plan,"extractorY"),worker))
                        &&!nearThreat(n(plan,"extractorX"),n(plan,"extractorY"),350)
                        &&(!host.assessOwnedOrdinaryStrategy()||!worker.prospect.arrived()||worker.prospect.local(n(plan,"extractorX"),n(plan,"extractorY")))){
                    if(startBuild(worker,plan,"/command/build-extractor?unitId="+worker.unit+"&x="+n(plan,"extractorX")+"&y="+n(plan,"extractorY"),
                        "extractorT1",n(plan,"extractorX"),n(plan,"extractorY"),(long)n(plan,"extractorCost"),"MINE")){acted=true;if(!host.parallelStrategy())return true;continue workerAllocation;}
                }
                // Already paid travel and a current native site identify real work.
                // A lack of spendable credits changes funding, not the exploration target.
                if(host.assessOwnedOrdinaryStrategy()&&worker.prospect.arrived()&&plan!=null
                        &&Objects.equals(state.get("sessionId"),plan.get("sessionId"))&&number(plan,"builderId",-1)==worker.unit
                        &&Double.isFinite(number(plan,"extractorCost",Double.NaN))&&n(plan,"extractorCost")>free&&n(plan,"extractorCost")>0
                        &&worker.prospect.local(n(plan,"extractorX"),n(plan,"extractorY"))
                        &&distance(actor,n(plan,"extractorX"),n(plan,"extractorY"))<ProspectCommitment.ARRIVAL_RADIUS
                        &&!resourceCommitted(n(plan,"extractorX"),n(plan,"extractorY"),worker)&&!nearThreat(n(plan,"extractorX"),n(plan,"extractorY"),350)){
                    emit("strategy_local_mine_waiting",costContext(plan,map("unitId",worker.unit,"reason","LOCAL_SITE_AWAITING_FUNDS",
                        "x",n(plan,"extractorX"),"y",n(plan,"extractorY"),"nativeCost",n(plan,"extractorCost"),
                        "nativeCredits",n(obj(state.get("player")),"credits"),"effectiveCredits",effectiveCredits(),"freeCredits",free,
                        "protectedFunds",protectedFunds,"capabilityReserve",capabilityReserve(),"paidTravel",worker.prospect.paidTravel())));
                    worker.retryAt=now+15000;continue workerAllocation;
                }
                String build=(host.economicArmyPolicyStrategy()?constructionCount("landFactory"):count("landFactory"))<factoryTarget
                    &&(!host.assessOwnedOrdinaryStrategy()||!worker.prospect.arrived())?"landFactory":null;
                if(build!=null){
                    Map<String,Object> plan2=host.readStrategy("/economy/construction-plan?unitId="+worker.unit+"&type="+build,"strategy_construction_plan");
                    if(plan2!=null&&Boolean.TRUE.equals(plan2.get("affordable"))&&n(plan2,"cost")<=free){
                        if(startBuild(worker,plan2,"/command/construct?unitId="+worker.unit+"&actionId="+encode(plan2.get("actionId"))+"&x="+n(plan2,"x")+"&y="+n(plan2,"y"),
                            build,n(plan2,"x"),n(plan2,"y"),(long)n(plan2,"cost"),"BUILD")){acted=true;if(!host.parallelStrategy())return true;continue workerAllocation;}
                    }
                }
                // Additional construction capacity must actually reach its backlog, not stand at
                // home once all sites within the native 600-unit building search are occupied.
                List<Map<String,Object>> resources=constructionResources();
                if(host.assessOwnedOrdinaryStrategy()&&worker.prospect.arrived()){
                    List<Map<String,Object>> local=new ArrayList<Map<String,Object>>();
                    for(Map<String,Object> resource:resources)if(worker.prospect.local(n(resource,"x"),n(resource,"y"))&&!occupiedResource(resource)
                        &&!nearThreat(n(resource,"x"),n(resource,"y"),350))local.add(resource);
                    boolean complete=localResourcesCurrent(resources,worker.prospect);
                    boolean retained=worker.prospect.assessLocalBacklog(complete,!local.isEmpty()||plan!=null&&worker.prospect.local(n(plan,"extractorX"),n(plan,"extractorY")));
                    if(retained){resources=local;
                        emit("strategy_prospect_cluster_retained",map("unitId",worker.unit,"localCandidates",local.size(),"assessment",complete?"CURRENT_LOCAL_TILES":"UNKNOWN", "paidTravel",worker.prospect.paidTravel()));
                    }else emit("strategy_prospect_cluster_released",map("unitId",worker.unit,"reason","TWO_CURRENT_EMPTY_LOCAL_ASSESSMENTS"));
                }
                Collections.sort(resources,(a,b)->{int retained=Boolean.compare((long)n(b,"tile")==worker.staticProspectTile,(long)n(a,"tile")==worker.staticProspectTile);
                    if(retained!=0)return retained;
                    int prepared=Boolean.compare(preparedConstructionOpportunity(b),preparedConstructionOpportunity(a));
                    return prepared!=0?prepared:Double.compare(distance(actor,n(a,"x"),n(a,"y")),distance(actor,n(b,"x"),n(b,"y")));});
                int queriedResources=0;
                for(Map<String,Object> resource:resources){
                    long tile=(long)n(resource,"tile");
                    if(worker.prospects.contains(tile)||occupiedResource(resource)||nearThreat(n(resource,"x"),n(resource,"y"),350)
                            ||host.economicArmyPolicyStrategy()&&resourceCommitted(n(resource,"x"),n(resource,"y"),worker))continue;
                    if(queriedResources++>=4)break;
                    worker.prospects.add(tile);
                    boolean staticResource=Boolean.TRUE.equals(resource.get("staticConstructionCandidate"));
                    Map<String,Object> approach=host.readStrategy((staticResource?"/static-map/approach":"/scout/resource-approach")+"?unitId="+worker.unit+"&tile="+tile,"strategy_resource_approach");
                    if(staticResource){if(!validConstructionApproach(approach,worker.unit,tile))continue;}
                    else if(approach==null||!Boolean.TRUE.equals(approach.get("pathKnown")))continue;
                    // Follow the native waypoint, retaining the ultimate resource rally as a claim.
                    if(staticResource&&Boolean.TRUE.equals(resource.get("currentlyVisible"))){
                        Map<String,Object> safe=host.readStrategy("/scout/resource-approach?unitId="+worker.unit+"&tile="+tile,"strategy_resource_safe_approach");
                        if(safe!=null&&Objects.equals(state.get("sessionId"),safe.get("sessionId"))
                                &&number(safe,"unitId",-1)==worker.unit&&number(safe,"tile",-1)==tile
                                &&number(safe,"gameTimeMs",-1)>=now
                                &&Boolean.TRUE.equals(safe.get("pathKnown"))&&Boolean.TRUE.equals(safe.get("safeSegmentKnown")))approach=safe;
                        else if(!knownThreatSegmentSafe(actor,n(approach,"waypointX"),n(approach,"waypointY")))continue;
                    }
                    double moveX=Double.isFinite(number(approach,"waypointX",Double.NaN))?n(approach,"waypointX"):n(approach,"x");
                    double moveY=Double.isFinite(number(approach,"waypointY",Double.NaN))?n(approach,"waypointY"):n(approach,"y");
                    if(!knownThreatSegmentSafe(actor,moveX,moveY))continue;
                    Map<String,Object> prospectReceipt=traceOrder(worker.owner,"/command/move?unitId="+worker.unit+"&x="+moveX+"&y="+moveY);
                    if(prospectReceipt!=null){
                        Long receiptFrame=GameClock.number(prospectReceipt.get("frame"));
                        if(host.assessOwnedOrdinaryStrategy()&&receiptFrame!=null)worker.prospect.accepted(tile,moveX,moveY,n(actor,"x"),n(actor,"y"),receiptFrame,now);
                        worker.job="PROSPECT";worker.x=moveX;worker.y=moveY;worker.buildX=n(resource,"x");worker.buildY=n(resource,"y");worker.jobAt=worker.lastProgress=now;worker.best=distance(actor,worker.x,worker.y);
                        if(staticResource)worker.prospects.remove(tile);
                        worker.staticProspectTile=staticResource?tile:-1;
                        emit("strategy_prospect_ordered",map("taskId",worker.task,"unitId",worker.unit,"tile",tile,"evidence",approach));acted=true;if(!host.parallelStrategy())return true;continue workerAllocation;
                    }
                    break;
                }
            }
            worker.retryAt=now+15000;
        }
        return acted;
    }
    private int visibleRecoveryConstruction(){int count=0;for(Map<String,Object> unit:units(state))
        if(alive(unit)&&"repairbay".equals(unit.get("type")))count++;return count;}
    private int pendingRecoveryConstruction(){int count=0;Set<Long> matched=new HashSet<Long>();for(Worker worker:workers.values())
        if(worker.paidConstruction&&"repairbay".equals(worker.product)){
            boolean visible=false;for(Map<String,Object> unit:units(state))if(alive(unit)&&"repairbay".equals(unit.get("type"))
                &&!worker.before.contains(id(unit))&&!matched.contains(id(unit))&&!matchedConstructionProducts.contains(id(unit))
                &&distance(unit,worker.buildX,worker.buildY)<120){matched.add(id(unit));visible=true;break;}
            if(!visible)count++;
        }return count;}
    private boolean knownThreatSegmentSafe(Map<String,Object> actor,double x,double y){
        if(actor==null||!Double.isFinite(x)||!Double.isFinite(y))return false;
        List<Map<String,Object>> danger=new ArrayList<Map<String,Object>>(items(scout,"rememberedThreats"));
        for(Map<String,Object> e:items(enemies,"visibleEnemies"))if(Boolean.TRUE.equals(e.get("canAttack")))danger.add(e);
        double ax=n(actor,"x"),ay=n(actor,"y"),dx=x-ax,dy=y-ay,length=dx*dx+dy*dy;
        for(Map<String,Object> threat:danger){
            double tx=number(threat,"x",Double.NaN),ty=number(threat,"y",Double.NaN),range=number(threat,"range",280)+80;
            if(!Double.isFinite(tx)||!Double.isFinite(ty)||!Double.isFinite(range))return false;
            double start=Math.hypot(ax-tx,ay-ty),end=Math.hypot(x-tx,y-ty);
            if(start<range&&end>start&&(ax-tx)*dx+(ay-ty)*dy>=0)continue;
            double t=length==0?0:Math.max(0,Math.min(1,((tx-ax)*dx+(ty-ay)*dy)/length));
            if(Math.hypot(ax+t*dx-tx,ay+t*dy-ty)<range)return false;
        }return true;
    }
    private boolean recoverySiteRisk(double x,double y){
        if(!Double.isFinite(x)||!Double.isFinite(y)
                ||host.economicArmyPolicyStrategy()&&Math.hypot(x-homeX,y-homeY)>650
                ||enemies==null||!(enemies.get("visibleEnemies") instanceof List)
                ||scout==null||!(scout.get("rememberedThreats") instanceof List))return true;
        for(Object row:(List<?>)enemies.get("visibleEnemies")){
            if(!(row instanceof Map))return true;
            @SuppressWarnings("unchecked") Map<String,Object> enemy=(Map<String,Object>)row;
            if(!(enemy.get("canAttack") instanceof Boolean))return true;
            if(Boolean.TRUE.equals(enemy.get("canAttack"))){
                if(!Double.isFinite(number(enemy,"x",Double.NaN))||!Double.isFinite(number(enemy,"y",Double.NaN))
                        ||distance(enemy,x,y)<400)return true;
            }
        }
        for(Object row:(List<?>)scout.get("rememberedThreats")){
            if(!(row instanceof Map))return true;
            @SuppressWarnings("unchecked") Map<String,Object> threat=(Map<String,Object>)row;
            if(!Double.isFinite(number(threat,"x",Double.NaN))||!Double.isFinite(number(threat,"y",Double.NaN))
                    ||!Double.isFinite(number(threat,"range",0))
                    ||distance(threat,x,y)<Math.max(400,number(threat,"range",0)+100))return true;
        }
        return false;
    }
    /** Current visibility over every remembered local tile can prove a locally empty candidate
     * list. Missing tiles/packets are UNKNOWN, not evidence that the distant cluster is exhausted. */
    private boolean localResourcesCurrent(List<Map<String,Object>> resources,ProspectCommitment commitment)throws Exception{
        if(!commitment.arrived())return false;
        Set<Long> tiles=new LinkedHashSet<Long>();for(Map<String,Object> resource:resources)if(commitment.local(n(resource,"x"),n(resource,"y")))tiles.add((long)n(resource,"tile"));
        if(tiles.isEmpty()||tiles.size()>48)return false;
        StringBuilder query=new StringBuilder();for(Long tile:tiles){if(query.length()>0)query.append(',');query.append(tile);}
        Map<String,Object> current=host.readStrategy("/scout/visible?tiles="+query,"strategy_local_cluster_visibility");
        if(current==null||!Objects.equals(state.get("sessionId"),current.get("sessionId"))||number(current,"gameTimeMs",-1)<now)return false;
        Set<Long> observed=new HashSet<Long>();for(Map<String,Object> tile:items(current,"tiles"))if(Boolean.TRUE.equals(tile.get("visible")))observed.add((long)n(tile,"tile"));
        return observed.containsAll(tiles);
    }
    private void validateCapabilityFunding()throws Exception{
        if(capabilityFunding==null)return;
        Need need=needs.get(capabilityFunding.need);
        Map<String,Object> producer=find(state,capabilityFunding.producer);
        String reason=null;
        if(now>=capabilityFunding.deadline)reason="TIMEOUT";
        else if(!available(need))reason="NEED_UNAVAILABLE";
        else if(assigned(need.id))reason="NEED_ALREADY_SERVED";
        else if(homeEmergency)reason="HOME_EMERGENCY";
        else if(army(state).size()<6)reason="FORCE_BELOW_MINIMUM";
        else if(producer==null||!alive(producer))reason="PRODUCER_LOST";
        else if(!safetyCapacityAvailable())reason="CAPACITY_UNAVAILABLE";
        else if(count("combatEngineer")+pendingCount("combatEngineer")>=Math.min(2,needs.size()))reason="ENGINEER_CAPACITY_SATISFIED";
        if(reason!=null)releaseCapabilityFunding(reason);
    }
    private boolean startCapabilityFunding(Map<String,Object> menu,double free)throws Exception{
        if(capabilityFunding!=null||now<capabilityFundingRetryAt||!(income>0)||!Double.isFinite(income))return false;
        Need selected=eligibleNeed();
        if(selected==null)return false;
        for(Map<String,Object> factory:items(menu,"factories")){
            Map<String,Object> producer=find(state,id(factory));
            if(producer==null||!alive(producer)||pending(id(factory)))continue;
            for(Map<String,Object> action:items(factory,"actions")){
                double cost=number(action,"cost",0);
                if(!"combatEngineer".equals(action.get("type"))||!(action.get("actionId") instanceof String)
                        ||String.valueOf(action.get("actionId")).isEmpty()||!Double.isFinite(cost)||cost<=0||free>=cost)continue;
                double fundingSeconds=(cost-free)/income;
                if(fundingSeconds>60)continue;
                capabilityFunding=new CapabilityFunding(selected.id,id(factory),(String)action.get("actionId"),cost,now);
                capabilityReservesStarted++;
                Map<String,Object> data=capabilityFundingData("BOUNDED_CAPABILITY_FUNDING");
                data.put("freeCredits",free);data.put("protectedFunds",protectedFunds);data.put("incomeEstimate",income);
                data.put("estimatedFundingGameSeconds",fundingSeconds);data.put("estimateSemantics","BUDGET_FEASIBILITY_NOT_OUTCOME_PREDICTION");
                emit("strategy_capability_reserve_started",data);return true;
            }
        }
        return false;
    }
    /** A funded purchase keeps its original hard-reserve deduction, never its own reserve. */
    private boolean fulfilCapabilityFunding()throws Exception{
        CapabilityFunding funding=capabilityFunding;
        Map<String,Object> menu=host.readStrategy("/combat/production","strategy_production_menu");
        if(menu==null){releaseCapabilityFunding("MENU_UNAVAILABLE");return false;}
        Map<String,Object> producer=null,chosen=null;
        for(Map<String,Object> factory:items(menu,"factories"))if(id(factory)==funding.producer){producer=factory;break;}
        if(producer!=null)for(Map<String,Object> action:items(producer,"actions"))
            if("combatEngineer".equals(action.get("type"))&&funding.action.equals(action.get("actionId"))){chosen=action;break;}
        if(chosen==null){releaseCapabilityFunding("ACTION_UNAVAILABLE");return false;}
        if(number(chosen,"cost",Double.NaN)!=funding.cost){releaseCapabilityFunding("PRICE_CHANGED");return false;}
        double free=effectiveCredits()-protectedFunds-(capabilityReserve()-funding.cost);
        if(number(producer,"queue",-1)!=0||pending(funding.producer)||!Boolean.TRUE.equals(chosen.get("affordable"))||free<funding.cost)return false;
        Map<String,Object> data=capabilityFundingData("CAPABILITY_COMMITMENT_READY");
        data.put("selected","UNSERVED_CAPABILITY_NEED");data.put("action",chosen);data.put("freeCredits",free);data.put("protectedFunds",protectedFunds);
        emit("strategy_allocation",data);
        Map<String,Object> receipt=traceOrder(CommandArbiter.DEFAULT_OWNER,"/command/queue?unitId="+funding.producer+"&actionId="+encode(funding.action),costContext(menu,map("needId",funding.need,"commitmentId",purchaseTraceId(funding.producer,now),"product","combatEngineer","cost",funding.cost,"producerSlots",1,"militarySlots",1)));
        if(receipt==null){releaseCapabilityFunding("ORDER_REJECTED");return false;}
        purchases.put(funding.producer,purchase("combatEngineer",funding.need));investments++;
        emit("strategy_purchase_committed",map("needId",funding.need,"producerId",funding.producer,"product","combatEngineer"));
        host.spendStrategy("STRATEGIC_CAPABILITY",(long)funding.cost,"combatEngineer",funding.producer);
        releaseCapabilityFunding("PURCHASE_ACCEPTED");return true;
    }
    private Map<String,Object> capabilityFundingData(String reason){
        CapabilityFunding f=capabilityFunding;
        return map("needId",f.need,"producerId",f.producer,"product","combatEngineer","actionId",f.action,"cost",f.cost,
            "startedAtGameTimeMs",f.started,"deadlineGameTimeMs",f.deadline,"ageGameMs",now-f.started,"reason",reason);
    }
    private void releaseCapabilityFunding(String reason)throws Exception{
        if(capabilityFunding==null)return;
        Map<String,Object> data=capabilityFundingData(reason);capabilityFunding=null;capabilityReservesReleased++;
        if(!"PURCHASE_ACCEPTED".equals(reason))capabilityFundingRetryAt=now+60000;
        emit("strategy_capability_reserve_released",data);
    }
    private boolean workerAction(Worker w)throws Exception{
        Map<String,Object> actor=find(state,w.unit);if(actor==null||!ready(actor))return false;
        if(repairFunding(w))return false;
        if(n(actor,"hp")<n(actor,"maxHp")*.3&&!"RETURN".equals(w.job)){
            Need interrupted=needs.get(w.target);
            if("RESPONSE".equals(w.job)&&interrupted!=null)interrupted.failures++;
            emit("strategy_task_preempted",map("taskId",w.task,"previousJob",w.job,"reason","CRITICAL_HP"));
            returnHome(w,45000);
        }
        if("RETURN".equals(w.job)){
            if("amphibiousJet".equals(actor.get("type"))&&now-w.lastOrder>=10000){
                Map<String,Object> modes=host.readStrategy("/combat/unit-modes?unitId="+w.unit,"strategy_unit_modes");
                if(modes!=null)traceMode(w,modes,null,"UNIT_MODES_NATIVE_FIELDS");
                if(modes!=null&&Boolean.TRUE.equals(modes.get("submergedWeaponAvailable")))for(Map<String,Object> action:items(modes,"actions"))
                    if("FLY".equals(action.get("mode"))&&Boolean.TRUE.equals(action.get("available"))&&Boolean.TRUE.equals(action.get("affordable"))){
                        if(traceOrder(w.owner,"/command/unit-mode?unitId="+w.unit+"&actionId="+encode(action.get("actionId")))!=null){
                            w.mode.flyAccepted();w.lastOrder=now;emit("strategy_responder_mode_ordered",map("taskId",w.task,"unitId",w.unit,"mode","FLY","reason","RETURN_TO_OWN_REAR","evidence",modes));return true;}
                        return false;
                    }
            }
            if(distance(actor,homeX,homeY)<180)return false;
            if("move".equals(actor.get("orderType"))&&actor.get("orderX") instanceof Number&&actor.get("orderY") instanceof Number
                    &&Math.hypot(n(actor,"orderX")-homeX,n(actor,"orderY")-homeY)<1)return false;
            if(now-w.lastOrder<10000)return false;
            if(traceOrder(w.owner,"/command/move?unitId="+w.unit+"&x="+homeX+"&y="+homeY)!=null){w.lastOrder=now;return true;}return false;
        }
        boolean engineer="combatEngineer".equals(actor.get("type")),jet="amphibiousJet".equals(actor.get("type"));
        if(jet&&("MODE_APPROACH".equals(w.job)||"MODE_WAIT".equals(w.job)))return modeResponse(w,actor);
        if(engineer&&"IDLE".equals(w.job)&&w.target<0&&!w.paidConstruction&&w.supportReserve==0){
            if(!w.endgameProvider&&host.endgameStrategy()&&endgameReady&&rearAirProviders()+pendingEndgameProviders()==0)
                retainRearAirProvider(w,"AVAILABLE_IDLE_ENDGAME_MANUFACTURER");
            if(w.endgameProvider)return false;
        }
        // A weapon-domain gap needs a production provider. Its own torpedo is conditional on
        // water position; sending the constructor to satisfy it sacrifices the rear production node.
        if(engineer&&"IDLE".equals(w.job)&&now>=w.retryAt){
            Need support=needs.get(w.target);
            if(support==null)for(Need candidate:needs.values())if(available(candidate)&&!assigned(candidate.id)
                    &&"WEAPON_DOMAIN_GAP".equals(candidate.reason)){support=candidate;break;}
            if(available(support)&&"WEAPON_DOMAIN_GAP".equals(support.reason)){
                if(w.target<0&&readyResponderAvailable(support))return false;
                if(w.target<0)bind(w,support,"REAR_CAPABILITY_PROVIDER",-1);
                if(distance(actor,homeX,homeY)>650||nearThreat(n(actor,"x"),n(actor,"y"),400)){
                    emit("strategy_provider_repositioned",map("taskId",w.task,"unitId",w.unit,"needId",support.id,
                        "reason","RETURN_TO_OWN_REAR_BEFORE_PRODUCTION","x",homeX,"y",homeY));
                    returnHome(w,0);return workerAction(w);
                }
                return startSupportBuild(w,actor,effectiveCredits()-protectedFunds-capabilityReserve()+w.supportReserve);
            }
        }
        if("IDLE".equals(w.job)&&now>=w.retryAt&&(engineer||jet)){
            List<Need> ordered=new ArrayList<Need>();
            if(w.target>=0){Need bound=needs.get(w.target);if(bound!=null)ordered.add(bound);}
            else ordered.addAll(needs.values());
            Collections.sort(ordered,(a,b)->Boolean.compare(b.building,a.building));
            for(Need need:ordered)if(available(need)&&(w.target==need.id||!assigned(need.id)||jet&&joinableResponse(need))){
                if(engineer&&"WEAPON_DOMAIN_GAP".equals(need.reason))continue;
                if(engineer&&threatCount(need.x,need.y,450)>1)continue;
                Map<String,Object> response=host.readStrategy("/combat/engagement?unitIds="+w.unit+"&targetId="+need.id,"response_engagement_observation");
                if(response==null)continue;
                List<Map<String,Object>> options=items(response,"actors");if(options.isEmpty())continue;
                Map<String,Object> choice=options.get(0);
                if(jet)traceMode(w,response,choice,"ENGAGEMENT_NATIVE_FIELDS");
                boolean current=Boolean.TRUE.equals(response.get("targetVisible"));
                if(jet&&"WEAPON_DOMAIN_GAP".equals(need.reason)&&!current)continue;
                if(jet&&current&&"DIVE".equals(choice.get("requiredMode"))
                        &&"APPROACH_PATH_KNOWN".equals(choice.get("modeApproachStatus"))&&choice.get("modeActionId") instanceof String){
                    if(w.target<0)bind(w,need,"AVAILABLE_MODE_CAPABLE_RESPONDER",-1);
                    w.job="MODE_APPROACH";w.mode.approachingWater();w.modeAction=String.valueOf(choice.get("modeActionId"));
                    w.x=n(choice,"modeApproachX");w.y=n(choice,"modeApproachY");w.jobAt=w.lastProgress=now;w.best=distance(actor,w.x,w.y);w.lastOrder=-100000;
                    emit("strategy_responder_mode_planned",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,"mode","DIVE",
                        "x",w.x,"y",w.y,"evidence",response));return modeResponse(w,actor);
                }
                boolean investigate=!current&&(need.building||now-need.seen<180000)
                        &&"APPROACH_PATH_KNOWN".equals(choice.get("lastKnownPositionApproachStatus"));
                if(!investigate&&(!"COMPATIBLE".equals(choice.get("compatibility"))||!"APPROACH_PATH_KNOWN".equals(choice.get("status"))))continue;
                if(w.target<0)bind(w,need,"AVAILABLE_SPECIALIST",-1);
                w.job="RESPONSE";w.x=n(choice,"approachX");w.y=n(choice,"approachY");
                w.jobAt=w.lastProgress=now;w.lastOrder=-100000;w.best=Double.MAX_VALUE;w.hp=Double.MAX_VALUE;
                w.lastAssessment=now;
                emit("strategy_task_assigned",map("taskId",w.task,"unitId",w.unit,"role","CROSS_DOMAIN_RESPONSE","targetId",w.target,
                    "approachX",w.x,"approachY",w.y,"objectiveSemantics",investigate?"LAST_KNOWN_SITE_INVESTIGATION":"CURRENT_CONTACT_APPROACH","evidence",response));break;
            }
        }
        if("RESPONSE".equals(w.job)&&now-w.lastOrder>=12000){
            // Hold one objective. Other visible targets cannot steal this specialist through the main army.
            if("attackMove".equals(actor.get("orderType"))&&number(actor,"orderX",-1)==w.x&&number(actor,"orderY",-1)==w.y)return false;
            if(jet){
                Map<String,Object> fresh=host.readStrategy("/combat/engagement?unitIds="+w.unit+"&targetId="+w.target,"response_engagement_observation");
                if(fresh==null||!arbiter.stamp().session.equals(fresh.get("sessionId"))||number(fresh,"targetId",-1)!=w.target
                        ||!Boolean.TRUE.equals(fresh.get("targetVisible"))||number(fresh,"gameTimeMs",-1)<now
                        ||number(fresh,"targetObservedAtGameTimeMs",-1)!=number(fresh,"gameTimeMs",-2)||items(fresh,"actors").isEmpty())return false;
                Map<String,Object> chosen=items(fresh,"actors").get(0);
                if(number(chosen,"unitId",-1)!=w.unit||!"COMPATIBLE".equals(chosen.get("compatibility"))
                        ||!"APPROACH_PATH_KNOWN".equals(chosen.get("status")))return false;
                traceMode(w,fresh,chosen,"ENGAGEMENT_NATIVE_FIELDS");
                double x=n(chosen,"approachX"),y=n(chosen,"approachY");
                if(x!=w.x||y!=w.y)emit("strategy_response_replanned",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,"x",x,"y",y,"evidence",fresh));
                w.x=x;w.y=y;w.lastAssessment=now;
            }
            emit("tactical_intent",map("reason","STRATEGIC_RESPONSE","targetX",w.x,"targetY",w.y,"targetId",w.target,"taskId",w.task));
            Map<String,Object> receipt=traceOrder(w.owner,"/command/attack-move?unitIds="+w.unit+"&x="+w.x+"&y="+w.y);
            if(receipt!=null){host.strategicAttack(receipt);w.lastOrder=now;emit("strategy_response_ordered",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,"receipt",receipt));return true;}
        }
        return false;
    }
    private boolean modeResponse(Worker worker,Map<String,Object> actor)throws Exception{
        Need need=needs.get(worker.target);if(!available(need))return false;
        Map<String,Object> response=host.readStrategy("/combat/engagement?unitIds="+worker.unit+"&targetId="+worker.target,"response_engagement_observation");
        if(response==null||!Boolean.TRUE.equals(response.get("targetVisible"))||items(response,"actors").isEmpty())return false;
        Map<String,Object> choice=items(response,"actors").get(0);
        traceMode(worker,response,choice,"ENGAGEMENT_NATIVE_FIELDS");
        if("COMPATIBLE".equals(choice.get("compatibility"))&&"APPROACH_PATH_KNOWN".equals(choice.get("status"))){
            worker.job="RESPONSE";worker.x=n(choice,"approachX");worker.y=n(choice,"approachY");worker.lastOrder=-100000;
            worker.jobAt=worker.lastProgress=now;worker.best=Double.MAX_VALUE;worker.hp=Double.MAX_VALUE;
            Map<String,Object> observed=map("taskId",worker.task,"unitId",worker.unit,"targetId",worker.target,"mode","DIVE","evidence",response);
            if(host.parallelStrategy()){observed.put("actualModeProof",false);observed.put("unitMode",worker.mode.snapshot());}
            emit("strategy_responder_mode_observed",observed);
            emit("strategy_task_assigned",map("taskId",worker.task,"unitId",worker.unit,"targetId",worker.target,
                "approachX",worker.x,"approachY",worker.y,"role","CROSS_DOMAIN_RESPONSE","objectiveSemantics","CURRENT_CONTACT_APPROACH","evidence",response));return workerAction(worker);
        }
        if("MODE_WAIT".equals(worker.job))return false;
        if(!"DIVE".equals(choice.get("requiredMode"))||!"APPROACH_PATH_KNOWN".equals(choice.get("modeApproachStatus")))return false;
        if(Boolean.TRUE.equals(choice.get("modeActionReady"))){
            if(traceOrder(worker.owner,"/command/unit-mode?unitId="+worker.unit+"&actionId="+encode(choice.get("modeActionId")))!=null){
                worker.job="MODE_WAIT";worker.mode.diveAccepted();worker.lastOrder=worker.lastProgress=now;
                emit("strategy_responder_mode_ordered",map("taskId",worker.task,"unitId",worker.unit,"targetId",worker.target,"mode","DIVE","evidence",response));return true;}
            return false;
        }
        double x=n(choice,"modeApproachX"),y=n(choice,"modeApproachY");
        if(Math.hypot(x-worker.x,y-worker.y)>60){worker.x=x;worker.y=y;worker.best=distance(actor,x,y);worker.lastProgress=now;}
        if(now-worker.lastOrder<10000)return false;
        if("move".equals(actor.get("orderType"))&&number(actor,"orderX",-1)==worker.x&&number(actor,"orderY",-1)==worker.y)return false;
        if(traceOrder(worker.owner,"/command/move?unitId="+worker.unit+"&x="+worker.x+"&y="+worker.y)!=null){
            worker.lastOrder=now;emit("strategy_responder_water_approach_ordered",map("taskId",worker.task,"unitId",worker.unit,"targetId",worker.target,
                "x",worker.x,"y",worker.y,"evidence",response));return true;}
        return false;
    }
    private void returnHome(Worker w,long cooldown){w.mode.repositioning();w.job="RETURN";w.x=homeX;w.y=homeY;w.lastOrder=-100000;w.retryAt=now+cooldown;}
    private void assessResponse(Worker w,Map<String,Object> actor)throws Exception{
        // Reads are independent of the actuator slot, so an arrival/contact transition cannot
        // starve behind another task's command. Investigation polls retain the normal decision rate.
        if(!"INVESTIGATE".equals(w.job)&&now-w.lastAssessment<10000)return;
        w.lastAssessment=now;
        Map<String,Object> response=host.readStrategy("/combat/engagement?unitIds="+w.unit+"&targetId="+w.target,"response_engagement_observation");
        if(response==null)return;
        Need need=needs.get(w.target);if(need==null)return;
        if(Boolean.FALSE.equals(response.get("targetVisible"))){
            if("RESPONSE".equals(w.job)&&distance(actor,w.x,w.y)<=75){
                w.job="INVESTIGATE";w.investigateAt=now;w.investigateSeen=need.seen;
                emit("strategy_investigation_started",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,
                    "lastSeenGameTimeMs",w.investigateSeen,"deadlineGameTimeMs",now+15000,"distance",distance(actor,w.x,w.y)));
            }
            return;
        }
        if(!Boolean.TRUE.equals(response.get("targetVisible"))||items(response,"actors").isEmpty())return;
        long seen=(long)number(response,"targetObservedAtGameTimeMs",-1);
        if(seen>=0)need.seen=Math.max(need.seen,seen);
        Map<String,Object> choice=items(response,"actors").get(0);
        if("INCOMPATIBLE".equals(choice.get("compatibility"))||"BLOCKED_TERRAIN".equals(choice.get("status"))){
            emit("strategy_task_preempted",map("taskId",w.task,"targetId",w.target,"reason","NEW_VISIBLE_NEGATIVE_EVIDENCE","evidence",response));
            returnHome(w,30000);return;
        }
        if("COMPATIBLE".equals(choice.get("compatibility"))&&"APPROACH_PATH_KNOWN".equals(choice.get("status"))){
            boolean resumed="INVESTIGATE".equals(w.job);
            if(resumed){w.job="RESPONSE";w.lastProgress=now;w.lastOrder=-100000;
                emit("strategy_investigation_contact_restored",map("taskId",w.task,"unitId",w.unit,"targetId",w.target,"observedAtGameTimeMs",seen));}
            if(resumed||Math.hypot(n(choice,"approachX")-w.x,n(choice,"approachY")-w.y)>60){
                w.x=n(choice,"approachX");w.y=n(choice,"approachY");w.best=distance(actor,w.x,w.y);
                emit("strategy_response_replanned",map("taskId",w.task,"targetId",w.target,"x",w.x,"y",w.y,"reason","SAME_TARGET_NEW_VISIBLE_APPROACH","evidence",response));
            }
        }
    }
    private boolean startSupportBuild(Worker worker,Map<String,Object> actor,double free)throws Exception{
        if(worker.endgameProvider&&worker.target<0)return false;
        if(homeEmergency||nearThreat(n(actor,"x"),n(actor,"y"),400)||!safetyCapacityAvailable())return false;
        Need need=needs.get(worker.target);
        if(worker.target<0)for(Need candidate:needs.values())if(available(candidate)&&!assigned(candidate.id)
                &&"MOVEMENT_APPROACH_GAP".equals(candidate.reason)){need=candidate;break;}
        if(!available(need)||!("MOVEMENT_APPROACH_GAP".equals(need.reason)||"WEAPON_DOMAIN_GAP".equals(need.reason)))return false;
        int jets=count("amphibiousJet");for(Worker other:workers.values())if("amphibiousJet".equals(other.product)&&"BUILD".equals(other.job))jets++;
        if(jets>=4)return false;
        Map<String,Object> plan=host.readStrategy("/economy/construction-plan?unitId="+worker.unit+"&type=amphibiousJet","strategy_construction_plan");
        if(plan==null||!(plan.get("actionId") instanceof String)||String.valueOf(plan.get("actionId")).isEmpty()
                ||!Double.isFinite(number(plan,"cost",Double.NaN))||n(plan,"cost")<=0
                ||nearThreat(n(plan,"x"),n(plan,"y"),400)){
            releaseSupportReserve(worker,"PLAN_UNAVAILABLE_OR_UNSAFE");return false;
        }
        double cost=n(plan,"cost");
        if(worker.supportReserve>0&&(cost!=worker.supportReserve||!Objects.equals(plan.get("actionId"),worker.supportQuote.get("actionId")))){
            releaseSupportReserve(worker,"PRICE_OR_ACTION_CHANGED");worker.retryAt=now+15000;return false;
        }
        if(!Boolean.TRUE.equals(plan.get("affordable"))||cost>free){
            if(worker.supportReserve==0&&income>0&&Double.isFinite(income)&&(cost-free)/income<=60){
                if(worker.target<0)bind(worker,need,"REAR_CAPABILITY_PROVIDER",-1);
                worker.supportReserve=cost;worker.supportQuote=new LinkedHashMap<String,Object>(plan);worker.supportDeadline=now+90000;
                emit("strategy_support_reserve_started",map("taskId",worker.task,"unitId",worker.unit,"needId",need.id,
                    "product","amphibiousJet","cost",cost,"deadlineGameTimeMs",worker.supportDeadline,"evidence",plan));
            }
            return false;
        }
        if(startBuild(worker,plan,"/command/construct?unitId="+worker.unit+"&actionId="+encode(plan.get("actionId"))+"&x="+n(plan,"x")+"&y="+n(plan,"y"),
                "amphibiousJet",n(plan,"x"),n(plan,"y"),(long)n(plan,"cost"),"BUILD")){
            if(worker.target<0)bind(worker,need,"CAPABILITY_SUPPORT_CONSTRUCTION",-1);
            releaseSupportReserve(worker,"CONSTRUCTION_ACCEPTED");
            emit("strategy_support_construction",map("taskId",worker.task,"unitId",worker.unit,"needId",need.id,"product","amphibiousJet"));return true;
        }
        releaseSupportReserve(worker,"CONSTRUCTION_REJECTED");worker.retryAt=now+15000;
        return false;
    }
    private boolean readyResponderAvailable(Need need)throws Exception{
        for(Worker candidate:workers.values())if("IDLE".equals(candidate.job)&&candidate.target<0&&now>=candidate.retryAt){
            Map<String,Object> unit=find(state,candidate.unit);if(unit==null||!"amphibiousJet".equals(unit.get("type")))continue;
            Map<String,Object> observation=host.readStrategy("/combat/engagement?unitIds="+candidate.unit+"&targetId="+need.id,"response_engagement_observation");
            if(observation==null||!Boolean.TRUE.equals(observation.get("targetVisible"))||items(observation,"actors").isEmpty())continue;
            Map<String,Object> choice=items(observation,"actors").get(0);
            if("COMPATIBLE".equals(choice.get("compatibility"))&&"APPROACH_PATH_KNOWN".equals(choice.get("status"))
                    ||"DIVE".equals(choice.get("requiredMode"))&&"APPROACH_PATH_KNOWN".equals(choice.get("modeApproachStatus")))return true;
        }return false;
    }
    private Purchase purchase(String product,long need){return purchase(product,need,false);}
    private Purchase purchase(String product,long need,boolean endgameProvider){Purchase purchase=new Purchase(product,now,need,endgameProvider);
        for(Map<String,Object> unit:units(state))purchase.before.add(id(unit));return purchase;}
    private static boolean repairFunding(Worker worker){return worker.supportReserve>0&&worker.supportQuote!=null&&"repairbay".equals(worker.supportQuote.get("type"));}
    private void releaseSupportReserve(Worker worker,String reason)throws Exception{
        if(worker.supportReserve<=0)return;
        emit("strategy_support_reserve_released",map("taskId",worker.task,"unitId",worker.unit,"needId",worker.target,
            "product",repairFunding(worker)?"repairbay":"amphibiousJet","cost",worker.supportReserve,"reason",reason));
        worker.supportReserve=0;worker.supportQuote=null;
    }
    private boolean startBuild(Worker w,Map<String,Object> evidence,String path,String product,double x,double y,long cost,String job)throws Exception{
        if(traceOrder(w.owner,path,costContext(evidence,map("commitmentId",constructionTraceId(w,now),"product",product,"cost",cost,"producerSlots",1,"constructionSlotAvailable",w.paidConstruction?0:1,"slotEvidenceSource","EXISTING_WORKER_CONSTRUCTION_COMMITMENT","militarySlots",("heavyTank".equals(product)||"amphibiousJet".equals(product))?1:0)))==null)return false;
        w.staticProspectTile=-1;w.job=job;w.product=product;w.paidConstruction=true;w.x=w.buildX=x;w.y=w.buildY=y;w.jobAt=now;w.before.clear();for(Map<String,Object> u:units(state))w.before.add(id(u));
        investments++;host.spendStrategy("STRATEGIC_CONSTRUCTION",cost,product,w.unit);
        emit("strategy_construction_ordered",map("taskId",w.task,"unitId",w.unit,"product",product,"x",x,"y",y,"evidence",evidence));return true;
    }
    private int count(String type){int count=0;for(Map<String,Object> u:units(state))if(ready(u)&&type.equals(u.get("type")))count++;return count;}
    private int pendingCount(String type){int count=0;for(Purchase p:purchases.values())if(type.equals(p.product))count++;return count;}
    private boolean nearThreat(double x,double y,double radius){
        for(Map<String,Object> e:items(scout,"rememberedThreats"))if(distance(e,x,y)<Math.max(radius,number(e,"range",0)+100))return true;return false;
    }
    private int threatCount(double x,double y,double radius){int count=0;for(Map<String,Object> e:items(scout,"rememberedThreats"))if(distance(e,x,y)<radius)count++;return count;}
    private void refreshConstructionMap()throws Exception{
        String current=String.valueOf(state.get("sessionId"));
        if(!current.equals(constructionSession)){constructionMap=null;constructionSession=current;constructionMapRetry=-100000;constructionReachable.clear();constructionUnreachable.clear();}
        if(constructionMap==null){
            if(now<constructionMapRetry)return;constructionMapRetry=now+15000;
            Map<String,Object> packet=host.readStrategy("/static-map/observe","strategy_static_construction_map");
            if(packet!=null&&"KNOWN".equals(packet.get("status"))&&Boolean.TRUE.equals(packet.get("staticOnly"))
                    &&Objects.equals(state.get("sessionId"),packet.get("sessionId"))&&packet.get("knowledgeId") instanceof String
                    &&packet.get("resourceTiles") instanceof List)constructionMap=packet;
        }
        if(constructionMap==null)return;
        long actor=-1;for(Map<String,Object> unit:units(state))if(ready(unit)&&"builder".equals(unit.get("type"))){actor=id(unit);break;}
        if(actor<0)return;
        int queried=0;List<Map<String,Object>> anchors=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> resource:constructionResources())if(Boolean.TRUE.equals(resource.get("staticConstructionCandidate"))){
            long tile=(long)n(resource,"tile");if(occupiedResource(resource)||nearThreat(n(resource,"x"),n(resource,"y"),350))continue;
            boolean same=false;for(Map<String,Object> anchor:anchors)if(distance(anchor,n(resource,"x"),n(resource,"y"))<=ProspectCommitment.CLUSTER_RADIUS){same=true;break;}
            if(same)continue;anchors.add(resource);
            if(constructionReachable.contains(tile)||constructionUnreachable.contains(tile))continue;
            if(queried++>=4)break;
            Map<String,Object> plan=host.readStrategy("/static-map/approach?unitId="+actor+"&tile="+tile,"strategy_static_construction_domain");
            if(validConstructionApproach(plan,actor,tile))constructionReachable.add(tile);
            else if(plan!=null&&Boolean.TRUE.equals(plan.get("staticOnly"))&&Objects.equals(plan.get("knowledgeId"),constructionMap.get("knowledgeId"))
                    &&Objects.equals(plan.get("sessionId"),state.get("sessionId"))&&Objects.equals(GameClock.number(plan.get("unitId")),Long.valueOf(actor))
                    &&Objects.equals(GameClock.number(plan.get("targetTile")),Long.valueOf(tile))&&Boolean.FALSE.equals(plan.get("staticPathKnown")))constructionUnreachable.add(tile);
        }
    }
    private List<Map<String,Object>> constructionResources(){
        List<Map<String,Object>> result=new ArrayList<Map<String,Object>>(items(scout,"resources"));
        if(!host.economicArmyPolicyStrategy()||constructionMap==null)return result;
        Set<Long> seen=new HashSet<Long>();for(Map<String,Object> resource:result)seen.add((long)number(resource,"tile",-1));
        for(Map<String,Object> resource:items(constructionMap,"resourceTiles")){
            Long tile=GameClock.number(resource.get("tile"));double x=number(resource,"x",Double.NaN),y=number(resource,"y",Double.NaN);
            if(tile==null||tile<0||tile>Integer.MAX_VALUE||!Double.isFinite(x)||!Double.isFinite(y)||!seen.add(tile))continue;
            Map<String,Object> candidate=new LinkedHashMap<String,Object>(resource);candidate.put("staticConstructionCandidate",true);result.add(candidate);
        }return result;
    }
    private boolean validConstructionApproach(Map<String,Object> plan,long actor,long tile){
        Long time=plan==null?null:GameClock.number(plan.get("gameTimeMs")),frame=plan==null?null:GameClock.number(plan.get("frame"));
        return plan!=null&&constructionMap!=null&&"KNOWN".equals(plan.get("status"))&&Boolean.TRUE.equals(plan.get("staticOnly"))
            &&Boolean.TRUE.equals(plan.get("staticPathKnown"))&&Objects.equals(plan.get("knowledgeId"),constructionMap.get("knowledgeId"))
            &&Objects.equals(plan.get("sessionId"),state.get("sessionId"))&&Objects.equals(GameClock.number(plan.get("unitId")),Long.valueOf(actor))
            &&Objects.equals(GameClock.number(plan.get("targetTile")),Long.valueOf(tile))&&time!=null&&time>=now&&frame!=null&&frame>=arbiter.stamp().frame
            &&Double.isFinite(number(plan,"waypointX",Double.NaN))&&Double.isFinite(number(plan,"waypointY",Double.NaN))
            &&Double.isFinite(number(plan,"x",Double.NaN))&&Double.isFinite(number(plan,"y",Double.NaN));
    }
    private boolean resourceCommitted(double x,double y,Worker except){
        if(host.constructionResourceCommittedStrategy(x,y))return true;
        for(Worker worker:workers.values())if(worker!=except){
            if(worker.mineQuote!=null&&worker.mineReserve>0&&Math.hypot(x-n(worker.mineQuote,"extractorX"),y-n(worker.mineQuote,"extractorY"))<60)return true;
            if(worker.paidConstruction&&"extractorT1".equals(worker.product)&&Math.hypot(x-worker.buildX,y-worker.buildY)<60)return true;
            if(("PROSPECT".equals(worker.job)||worker.staticProspectTile>=0)
                    &&Math.hypot(x-worker.buildX,y-worker.buildY)<=ProspectCommitment.CLUSTER_RADIUS)return true;
            if(worker.prospect.local(x,y))return true;
        }return false;
    }
    /** IDLE describes the current command, not absence of a regional economic responsibility. */
    private boolean economicFrontCommitted(Worker worker){
        if(worker.mineReserve>0)return true;
        if(worker.prospect.tile()>=0||"PROSPECT".equals(worker.job))return true;
        Map<String,Object> actor=find(state,worker.unit);
        if(actor==null||!"builder".equals(actor.get("type"))||distance(actor,homeX,homeY)<=650)return false;
        for(Map<String,Object> resource:items(scout,"resources"))if(Boolean.TRUE.equals(resource.get("currentlyVisible"))
                &&distance(actor,n(resource,"x"),n(resource,"y"))<=ProspectCommitment.CLUSTER_RADIUS
                &&!occupiedResource(resource)&&!constructionSiteRisk(n(resource,"x"),n(resource,"y")))return true;
        return false;
    }
    private boolean homeConstructionAvailable(Worker worker){
        return "IDLE".equals(worker.job)&&worker.target<0&&!worker.paidConstruction&&worker.supportReserve==0
            &&!worker.endgameProvider&&!economicFrontCommitted(worker);
    }
    private boolean militaryConstructionFront(double x,double y){
        for(Map<String,Object> front:constructionMilitaryFronts){
            double fx=number(front,"x",Double.NaN),fy=number(front,"y",Double.NaN);
            if(number(front,"readyMembers",0)>0&&Double.isFinite(fx)&&Double.isFinite(fy)
                    &&Math.hypot(x-fx,y-fy)<=ProspectCommitment.CLUSTER_RADIUS)return true;
        }return false;
    }
    private boolean preparedConstructionOpportunity(Map<String,Object> resource){
        double x=number(resource,"x",Double.NaN),y=number(resource,"y",Double.NaN);
        return Double.isFinite(x)&&Double.isFinite(y)&&!occupiedResource(resource)&&!constructionSiteRisk(x,y)
            &&(Boolean.TRUE.equals(resource.get("currentlyVisible"))&&developedConstructionFront(x,y)
                ||Boolean.TRUE.equals(resource.get("staticConstructionCandidate"))&&constructionReachable.contains((long)n(resource,"tile"))&&militaryConstructionFront(x,y));
    }
    /** Static domains are future exploration opportunities. Workforce demand comes from current
     * developed fronts and native jobs, with idle/paid workers covering work before any purchase. */
    private int constructionBuilderTarget()throws Exception{
        List<Map<String,Object>> anchors=new ArrayList<Map<String,Object>>(),actionable=new ArrayList<Map<String,Object>>(),military=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> resource:constructionResources()){
            double x=number(resource,"x",Double.NaN),y=number(resource,"y",Double.NaN);
            if(!Double.isFinite(x)||!Double.isFinite(y)||occupiedResource(resource)||constructionSiteRisk(x,y)||resourceCommitted(x,y,null))continue;
            if(Boolean.TRUE.equals(resource.get("staticConstructionCandidate"))&&!constructionReachable.contains((long)n(resource,"tile")))continue;
            addConstructionDomain(anchors,x,y);
            // A scout seeing a far mine, or a static native route across the map, does not prepare
            // a construction lane. Current resources near actual own work form the next frontier.
            if(preparedConstructionOpportunity(resource)){
                addConstructionDomain(actionable,x,y);
                if(militaryConstructionFront(x,y))addConstructionDomain(military,x,y);
            }
        }
        constructionDomains=anchors.size();militaryConstructionDomains=military.size();
        int builders=count("builder")+pendingCount("builder"),idle=0,committedExtra=0;
        long baselineBuilder=Long.MAX_VALUE;
        for(Map<String,Object> actor:units(state))if(ready(actor)&&"builder".equals(actor.get("type")))baselineBuilder=Math.min(baselineBuilder,id(actor));
        List<Map<String,Object>> availableWorkers=new ArrayList<Map<String,Object>>();
        // The host's visible facility count already includes unfinished sites. Only invisible
        // paid sites subtract here, so a later footprint witness cannot erase another real job.
        int paidFactories=Math.max(0,constructionCount("landFactory")-count("landFactory"));
        Map<String,Object> planner=null;
        for(Map<String,Object> actor:units(state))if(ready(actor)&&"builder".equals(actor.get("type"))){
            if(planner==null||distance(actor,homeX,homeY)<distance(planner,homeX,homeY))planner=actor;
        }
        for(Worker worker:workers.values()){Map<String,Object> actor=find(state,worker.unit);
            if(actor==null||!"builder".equals(actor.get("type")))continue;
            if(homeConstructionAvailable(worker))availableWorkers.add(actor);
            else if(worker.unit!=baselineBuilder)committedExtra++;
        }
        // A native plan also supplies current local work when the resource packet has no explicit
        // visibility marker. Its price/site are planning evidence, never construction authority.
        if(planner!=null){
            Map<String,Object> plan=host.readStrategy("/expansion/plan?unitId="+id(planner),"strategy_current_construction_demand");
            double x=number(plan,"extractorX",Double.NaN),y=number(plan,"extractorY",Double.NaN),cost=number(plan,"extractorCost",Double.NaN);
            if(plan!=null&&number(plan,"builderId",-1)==id(planner)&&Double.isFinite(x)&&Double.isFinite(y)
                    &&Double.isFinite(cost)&&cost>0&&!constructionSiteRisk(x,y)&&!resourceCommitted(x,y,null))addConstructionDomain(actionable,x,y);
        }
        actionableConstructionDomains=actionable.size();
        constructionFacilityJobs=Math.max(0,host.constructionDemandStrategy()-paidFactories);
        if(constructionFacilityJobs>0){
            Map<String,Object> plan=planner==null?null:host.readStrategy("/economy/construction-plan?unitId="+id(planner)+"&type=landFactory","strategy_current_facility_demand");
            if(plan==null||number(plan,"unitId",-1)!=id(planner)||!"landFactory".equals(plan.get("type"))
                    ||!(plan.get("actionId") instanceof String)||!Double.isFinite(number(plan,"cost",Double.NaN))||number(plan,"cost",-1)<=0
                    ||constructionSiteRisk(number(plan,"x",Double.NaN),number(plan,"y",Double.NaN)))constructionFacilityJobs=0;
        }
        // Capital construction is a current native home job, not a reason to conscript an
        // already committed remote prospector. A paid or returning home worker covers it.
        homeCapitalConstructionJobs=0;
        if(host.endgameStrategy()&&endgameReady&&longTermCapital.growing()&&airGeneral.size()>0&&rearAirProviders()>0
                &&pendingEndgameProviders()==0&&rearAirProviders()>=endgameProviderTarget&&planner!=null){
            boolean covered=false;
            for(Worker worker:workers.values()){
                Map<String,Object> actor=find(state,worker.unit);
                if(actor!=null&&"builder".equals(actor.get("type"))&&("RETURN".equals(worker.job)
                        ||worker.paidConstruction&&worker.endgameConstruction||homeConstructionAvailable(worker)&&distance(actor,homeX,homeY)<650)){covered=true;break;}
            }
            if(!covered){
                Map<String,Object> plan=host.readStrategy("/economy/construction-plan?unitId="+id(planner)+"&type=fabricatorT1","strategy_current_capital_construction_demand");
                if(actualEndgamePlan(plan,id(planner),"fabricatorT1")&&Math.hypot(n(plan,"x")-homeX,n(plan,"y")-homeY)<=650&&n(plan,"cost")<=capitalFree())homeCapitalConstructionJobs=1;
            }
        }
        int homeJobs=constructionFacilityJobs+homeCapitalConstructionJobs
            +Math.max(0,host.recoveryCapacityNeed()-visibleRecoveryConstruction()-pendingRecoveryConstruction());
        int jobs=actionableConstructionDomains+homeJobs;
        // Match actual nearby idle constructors once per spatial job. A home-side idle worker
        // does not claim remote economic coverage; it can still be dispatched there below.
        // The baseline constructor remains in its mature host-owned economic lane.
        List<Map<String,Object>> unmatched=new ArrayList<Map<String,Object>>(availableWorkers);
        for(Map<String,Object> domain:actionable){
            Map<String,Object> closest=null;
            for(Map<String,Object> actor:unmatched)if(distance(actor,n(domain,"x"),n(domain,"y"))<=ProspectCommitment.CLUSTER_RADIUS
                    &&(closest==null||distance(actor,n(domain,"x"),n(domain,"y"))<distance(closest,n(domain,"x"),n(domain,"y"))))closest=actor;
            if(closest!=null){idle++;unmatched.remove(closest);}
        }
        for(Map<String,Object> actor:unmatched)if(homeJobs>0&&distance(actor,homeX,homeY)<=650){idle++;homeJobs--;}
        int unmet=Math.max(0,jobs-idle-pendingCount("builder"));
        constructionIdleWorkers=idle;constructionUnmetJobs=unmet;
        // Ordinary spending is observed throughput, not a permanent claim on future income.
        // Subtracting it here would let successful continuous production suppress all worker
        // growth. Actual purchases still protect current effective credits and every reserve.
        int funded=Double.isFinite(income)&&builderNativePrice>0&&!homeEmergency
            ?(int)Math.min(Integer.MAX_VALUE,Math.floor(Math.max(0,income)*30/builderNativePrice)):0;
        // Delayed or refused travel must not buy a new home worker every evaluation. The total
        // known regional work plus already committed extra constructors bounds procurement;
        // existing workers remain protected even when current demand shrinks.
        int workCapacity=1+committedExtra+jobs;
        return Math.max(1,Math.max(builders,Math.min(workCapacity,builders+Math.min(unmet,funded))));
    }
    private static void addConstructionDomain(List<Map<String,Object>> domains,double x,double y){
        for(Map<String,Object> anchor:domains)if(distance(anchor,x,y)<=ProspectCommitment.CLUSTER_RADIUS)return;
        domains.add(map("x",x,"y",y));
    }
    private boolean developedConstructionFront(double x,double y){
        for(Map<String,Object> actor:units(state))if(ready(actor)){
            String type=String.valueOf(actor.get("type"));
            if(("builder".equals(type)||"commandCenter".equals(type)||"landFactory".equals(type)||type.startsWith("extractor"))
                    &&distance(actor,x,y)<=ProspectCommitment.CLUSTER_RADIUS)return true;
        }return militaryConstructionFront(x,y);
    }
    private boolean constructionSiteRisk(double x,double y){
        if(!Double.isFinite(x)||!Double.isFinite(y)||nearThreat(x,y,350))return true;
        for(Map<String,Object> enemy:items(enemies,"visibleEnemies"))if(Boolean.TRUE.equals(enemy.get("canAttack"))&&distance(enemy,x,y)<400)return true;
        return false;
    }
    private int constructionCount(String product){
        int count=0;Set<Long> matched=new HashSet<Long>();
        for(Map<String,Object> unit:units(state))if(alive(unit)&&product.equals(unit.get("type")))count++;
        for(Worker worker:workers.values())if(worker.paidConstruction&&product.equals(worker.product)){
            boolean visible=false;for(Map<String,Object> unit:units(state))if(alive(unit)&&product.equals(unit.get("type"))
                    &&!worker.before.contains(id(unit))&&!matched.contains(id(unit))&&!matchedConstructionProducts.contains(id(unit))
                    &&distance(unit,worker.buildX,worker.buildY)<120){matched.add(id(unit));visible=true;break;}
            if(!visible)count++;
        }return count;
    }
    private double currentIdleMineCost()throws Exception{
        double cheapest=-1;
        for(Worker worker:workers.values())if("IDLE".equals(worker.job)&&now>=worker.retryAt){
            Map<String,Object> actor=find(state,worker.unit);if(actor==null||!"builder".equals(actor.get("type")))continue;
            Map<String,Object> plan=host.readStrategy("/expansion/plan?unitId="+worker.unit,"strategy_income_alternative_plan");
            double cost=number(plan,"extractorCost",Double.NaN),x=number(plan,"extractorX",Double.NaN),y=number(plan,"extractorY",Double.NaN);
            if(plan==null||number(plan,"builderId",-1)!=worker.unit||!Double.isFinite(cost)||cost<=0
                    ||!Double.isFinite(x)||!Double.isFinite(y)||nearThreat(x,y,350)||resourceCommitted(x,y,worker)
                    ||worker.prospect.arrived()&&!worker.prospect.local(x,y))continue;
            if(cheapest<0||cost<cheapest)cheapest=cost;
        }return cheapest;
    }
    private int resourceBacklog(){
        int count=0;for(Map<String,Object> resource:items(scout,"resources")){
            if(nearThreat(n(resource,"x"),n(resource,"y"),350))continue;
            boolean occupied=false;for(Map<String,Object> unit:units(state))if(alive(unit)&&String.valueOf(unit.get("type")).startsWith("extractor")
                    &&distance(unit,n(resource,"x"),n(resource,"y"))<60){occupied=true;break;}
            if(!occupied)count++;
        }return count;
    }
    private boolean occupiedResource(Map<String,Object> resource){
        for(Map<String,Object> unit:units(state))if(alive(unit)&&String.valueOf(unit.get("type")).startsWith("extractor")
                &&distance(unit,n(resource,"x"),n(resource,"y"))<60)return true;
        for(Map<String,Object> unit:items(enemies,"visibleEnemies"))if(String.valueOf(unit.get("type")).startsWith("extractor")
                &&distance(unit,n(resource,"x"),n(resource,"y"))<60)return true;
        return false;
    }
    private boolean idleWorkerNearResource(){
        for(Worker w:workers.values())if("IDLE".equals(w.job)){
            Map<String,Object> actor=find(state,w.unit);if(actor==null)continue;
            if(!"builder".equals(actor.get("type")))continue;
            for(Map<String,Object> res:items(scout,"resources"))if(Boolean.TRUE.equals(res.get("currentlyVisible"))
                    &&distance(actor,n(res,"x"),n(res,"y"))<600&&!nearThreat(n(res,"x"),n(res,"y"),350)){
                boolean occupied=false;for(Map<String,Object> unit:units(state))if(alive(unit)&&String.valueOf(unit.get("type")).startsWith("extractor")&&distance(unit,n(res,"x"),n(res,"y"))<60)occupied=true;
                if(!occupied)return true;
            }
        }return false;
    }
    private void release(Worker w,String reason)throws Exception{
        detachCapability(w);
        if(arbiter.release(w.owner))emit("task_ownership_released",map("taskId",w.task,"owner",w.owner,"reason",reason));
    }
    void close()throws Exception{releaseCapabilityFunding("CONTROLLER_ENDED");for(Worker w:workers.values()){
        releaseSupportReserve(w,"CONTROLLER_ENDED");release(w,"CONTROLLER_ENDED");}workers.clear();capabilityTasks.clear();airGeneral.close();}
    Map<String,Object> summary(){Map<String,Object> result=map("enabled",enabled,"armyTarget",armyTarget,"activeTarget",activeTarget,"reserveTarget",reserveTarget,
        "hardSafetyCap",hardCap,"builderTarget",builderTarget,"militaryCapacityIncreases",militaryCapacityIncreases,
        "militaryCapacityFloor",militaryCapacityFloor,"capabilityNeedsAtEnd",needs.size(),"needsResolvedByLegalEvidence",resolvedNeeds,
        "investmentOrders",investments,"mineUpgradesObserved",upgrades,"constructionCompletionsObserved",completedJobs,
        "capabilityReservesStarted",capabilityReservesStarted,"capabilityReservesReleased",capabilityReservesReleased,"capabilityReserveAtEnd",capabilityReserve());
        if(host.parallelStrategy()){result.put("capabilityTasksAtEnd",capabilityTasks.size());result.put("capabilityModeScope","PER_UNIT");
            result.put("searchAreaPolicy",host.endgameStrategy()?"LEGAL_AIR_FRONTIER_CURRENT_COVERAGE_SEARCH":"CONTRACT_ONLY_NEEDS_LEGAL_COVERAGE_INPUT");}
        if(host.endgameStrategy()){result.put("g6",true);result.put("airGeneral",airGeneral.snapshot());result.put("endgameReady",endgameReady);
            result.put("airTarget",endgameAirTarget);result.put("airProviderTarget",endgameProviderTarget);result.put("airProviders",rearAirProviders());
            result.put("pendingAirProviders",pendingEndgameProviders());result.put("capital",longTermCapital.snapshot());result.put("capitalUpgradesObserved",capitalUpgrades);}return result;}
    private void emit(String event,Map<String,Object> data)throws Exception{
        data.put("gameTimeMs",now);data.put("sessionId",arbiter.stamp().session);data.put("player",arbiter.stamp().player);host.emitStrategy(event,data);
    }
    // G1 sidecars below never feed policy state, timing, matching, funding or command admission.
    private void trace(String event,Map<String,Object> data){
        try{data.put("detectedAtGameTimeMs",now);data.put("eventOccurredAtGameTimeMs",null);
            data.put("timeSemantics","DETECTED_BY_EXISTING_POLICY_NOT_EXACT_EVENT_TIME");
            host.traceStrategy(event,data);
        }catch(RuntimeException ignored){/* Optional diagnostics cannot alter existing actuation. */}
    }
    private Map<String,Object> costContext(Map<String,Object> nativeResponse,Map<String,Object> extra){
        Map<String,Object> context=new LinkedHashMap<String,Object>(extra);
        Map<String,Object> source=nativeResponse==null?null:host.costSourceStrategy(nativeResponse);
        if((source==null||!(source.get("costSourceObservationId") instanceof String))&&nativeResponse!=null
                &&nativeResponse.get("costSourceObservationId") instanceof String&&nativeResponse.get("costSourceRequestPath") instanceof String)
            source=nativeResponse;
        Object id=source==null?null:source.get("costSourceObservationId"),path=source==null?null:source.get("costSourceRequestPath");
        boolean known=id instanceof String&&!((String)id).isEmpty()&&path instanceof String&&!((String)path).isEmpty()
                &&!"/state".equals(path)&&!((String)path).startsWith("/state?");
        context.put("costSourceObservationId",known?id:null);context.put("costSourceRequestPath",known?path:null);
        context.put("costSourceStatus",known?"VALIDATED_NATIVE_QUOTE_PACKET":"UNKNOWN_PRICE_SOURCE");
        return context;
    }
    private Map<String,Object> traceOrder(String owner,String path)throws Exception{
        return traceOrder(owner,path,Collections.<String,Object>emptyMap());
    }
    private Map<String,Object> traceOrder(String owner,String path,Map<String,Object> extra)throws Exception{
        Map<String,Object> context=map("lane","Strategy","owner",owner,"path",path);
        for(Worker worker:workers.values())if(worker.owner.equals(owner)){
            context.putAll(workerTrace(worker));break;
        }
        context.putAll(extra);
        WorldState world=host.worldStrategy();
        if(world!=null){
            context.put("worldRevision",world.revision);context.put("worldEpoch",world.epoch);
            context.put("worldEpochIsOwnerGeneration",false);context.put("worldSessionId",world.sessionId);
            Object unit=context.get("unitId");
            if(unit instanceof Number){WorldState.UnitFact fact=world.ownUnits().get(((Number)unit).longValue());
                context.put("worldActorAvailability",fact==null?"UNKNOWN":fact.availability);
                if(fact!=null){WorldState.SourceView source=world.views().get(fact.sourceScope);
                    context.put("worldActorSourceObservationId",source==null?null:source.observation.id);}}
            Object need=context.get("needId");
            if(need instanceof Number){WorldState.EnemyFact fact=world.enemies().get(((Number)need).longValue());
                context.put("worldTargetVisibility",fact==null?"UNKNOWN":fact.visibility);
                context.put("worldTargetCurrentKnown",fact!=null&&!fact.current.isEmpty());}
            context.put("worldUnknownSemantics","NO_HIDDEN_COORDINATES_OR_PRODUCTION_LINEAGE");
        }
        trace("command_context",context);
        return host.orderStrategy(owner,path,context);
    }
    private static String constructionTraceId(Worker worker,long at){return "strategy-construction:"+worker.task+":"+at;}
    private static String purchaseTraceId(long producer,long at){return "strategy-purchase:"+producer+":"+at;}
    private Map<String,Object> workerTrace(Worker worker){
        return map("taskId",worker.task,"unitId",worker.unit,"owner",worker.owner,"needId",worker.target,
            "job",worker.job,"capabilityTaskId",worker.target<0?null:worker.target,"endgameProvider",worker.endgameProvider,"unitMode",worker.mode.snapshot(),"paidConstruction",worker.paidConstruction,"product",worker.product,
            "commitmentId",worker.paidConstruction?constructionTraceId(worker,worker.jobAt):null);
    }
    private Map<String,Map<String,Object>> traceStates(){
        Map<String,Map<String,Object>> snapshots=new LinkedHashMap<String,Map<String,Object>>();
        for(Worker worker:workers.values())snapshots.put("worker:"+worker.unit,workerTrace(worker));
        for(Need need:needs.values())snapshots.put("need:"+need.id,map("needId",need.id,"targetType",need.type,
            "reason",need.reason,"failures",need.failures,"awaitVisibleAfterGameTimeMs",need.awaitVisibleAfter));
        for(Map.Entry<Long,Purchase> entry:purchases.entrySet()){
            Purchase purchase=entry.getValue();snapshots.put("purchase:"+entry.getKey(),map("producerId",entry.getKey(),
                "needId",purchase.selectedNeed,"boundNeedId",purchase.need,"product",purchase.product,"endgameProvider",purchase.endgameProvider,
                "active",purchase.active,"queueFinished",purchase.queueFinished,"fulfilled",purchase.fulfilled,
                "commitmentId",purchaseTraceId(entry.getKey(),purchase.at)));
        }
        return snapshots;
    }
    private void traceStateChanges(Map<String,Map<String,Object>> before,String phase){
        Map<String,Map<String,Object>> after=traceStates();Set<String> keys=new LinkedHashSet<String>(before.keySet());keys.addAll(after.keySet());
        for(String key:keys)if(!Objects.equals(before.get(key),after.get(key))){
            Map<String,Object> identity=after.containsKey(key)?after.get(key):before.get(key);
            Map<String,Object> data=new LinkedHashMap<String,Object>(identity);
            data.put("objectKey",key);data.put("phase",phase);data.put("oldState",before.get(key));data.put("newState",after.get(key));
            data.put("transitionSemantics","NET_CHANGE_DURING_EXISTING_POLICY_CALL");trace("state_transition",data);
        }
    }
    Map<String,Object> capabilitySnapshot(){Map<String,Object> result=new LinkedHashMap<String,Object>();
        for(CapabilityTask task:capabilityTasks.values())result.put(String.valueOf(task.taskId),task.snapshot());return result;}
    Map<Long,Map<String,Object>> unitModes(){Map<Long,Map<String,Object>> result=new LinkedHashMap<Long,Map<String,Object>>();
        for(Worker worker:workers.values()){Map<String,Object> actor=find(state,worker.unit);if(actor!=null&&"amphibiousJet".equals(actor.get("type")))result.put(worker.unit,worker.mode.snapshot());}return result;}
    private void traceReadyMatch(Worker worker,Map<String,Object> selected){
        List<Long> candidates=new ArrayList<Long>();
        for(Map<String,Object> unit:units(state))if(!worker.before.contains(id(unit))&&!matchedConstructionProducts.contains(id(unit))&&ready(unit)
                &&worker.product.equals(unit.get("type"))&&distance(unit,worker.buildX,worker.buildY)<120)candidates.add(id(unit));
        Map<String,Object> data=workerTrace(worker);data.put("selectedUnitId",id(selected));data.put("candidateUnitIds",candidates);
        data.put("candidateCount",candidates.size());data.put("matchAmbiguous",candidates.size()>1);data.put("observedUnit",selected);
        data.put("matchSemantics","EXISTING_FIRST_MATCH_NEW_READY_TYPE_WITHIN_120");data.put("producerLineageConfirmed",false);
        data.put("sourceGameTimeMs",state.get("gameTimeMs"));data.put("sourceFrame",state.get("frame"));trace("ready_match_witness",data);
    }
    private void tracePurchaseMatch(long producer,Purchase purchase,Worker selected){
        List<Long> candidates=new ArrayList<Long>();
        for(Worker worker:workers.values())if(worker.target<0&&"IDLE".equals(worker.job)){
            Map<String,Object> actor=find(state,worker.unit);
            if(actor!=null&&ready(actor)&&"combatEngineer".equals(actor.get("type"))
                    &&!purchase.before.contains(worker.unit)&&!matchedPurchasedWorkers.contains(worker.unit))candidates.add(worker.unit);
        }
        trace("purchase_ready_match_witness",map("producerId",producer,"needId",purchase.selectedNeed,
            "commitmentId",purchaseTraceId(producer,purchase.at),"taskId",selected.task,"unitId",selected.unit,"owner",selected.owner,
            "candidateUnitIds",candidates,"candidateCount",candidates.size(),"matchAmbiguous",candidates.size()>1,
            "matchSemantics","EXISTING_FIRST_AVAILABLE_NEW_READY_WORKER","producerLineageConfirmed",false,
            "sourceGameTimeMs",state.get("gameTimeMs"),"sourceFrame",state.get("frame")));
    }
    private void traceMode(Worker worker,Map<String,Object> response,Map<String,Object> choice,String source)throws Exception{
        boolean responseWitnessAccepted=arbiter.stamp().session.equals(response.get("sessionId"))&&worker.mode.witness(response,choice,now);
        // Engagement movement is a desired flag, not native height completion. Only G3 obtains
        // the independent own-mode witness; the legacy toggle retains its exact GET sequence.
        if(host.parallelStrategy()&&choice!=null&&number(choice,"unitId",-1)==worker.unit){
            Map<String,Object> modes=host.readStrategy("/combat/unit-modes?unitId="+worker.unit,"strategy_unit_modes");
            if(modes!=null&&arbiter.stamp().session.equals(modes.get("sessionId"))){
                boolean actualAccepted=worker.mode.witness(modes,null,now);
                Map<String,Object> actual=workerTrace(worker);actual.put("nativeModeWitnessAccepted",actualAccepted);actual.put("sourceKind","UNIT_MODES_NATIVE_FIELDS");
                actual.put("sourceGameTimeMs",modes.get("gameTimeMs"));actual.put("sourceFrame",modes.get("frame"));
                actual.put("nativeSubmergedWeaponAvailable",modes.get("submergedWeaponAvailable"));
                actual.put("actualModeEvidence","NATIVE_OWN_SUBMERGED_THRESHOLD_NOT_EXACT_HEIGHT_OR_TERRAIN_PERMISSION");
                actual.put("legacyDiveObservedIsActualModeProof",false);trace("mode_evidence_witness",actual);
            }
        }
        Map<String,Object> data=workerTrace(worker);data.put("nativeModeWitnessAccepted",responseWitnessAccepted);data.put("sourceKind",source);data.put("sourceGameTimeMs",response.get("gameTimeMs"));
        data.put("sourceFrame",response.get("frame"));data.put("nativeSubmergedWeaponAvailable",response.get("submergedWeaponAvailable"));
        data.put("nativeMovementType",choice==null?null:choice.get("movementType"));
        data.put("nativeWeaponRange",choice==null?null:choice.get("weaponRange"));
        data.put("targetVisible",response.get("targetVisible"));data.put("targetDomain",response.get("targetDomain"));
        data.put("compatibility",choice==null?null:choice.get("compatibility"));data.put("approachStatus",choice==null?null:choice.get("status"));
        data.put("actualModeEvidence",choice==null?"OWN_UNIT_MODES_BOOLEAN_ONLY":"DESIRED_MOVEMENT_ONLY_PHYSICAL_MODE_NEEDS_EVIDENCE");
        data.put("legacyDiveObservedIsActualModeProof",false);trace("mode_evidence_witness",data);
    }
    static Map<String,Object> map(Object... kv){Map<String,Object> m=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)m.put((String)kv[i],kv[i+1]);return m;}
    static double number(Map<String,Object> value,String key,double fallback){return value!=null&&value.get(key) instanceof Number?((Number)value.get(key)).doubleValue():fallback;}
    static boolean ready(Map<String,Object> u){return alive(u)&&number(u,"buildProgress",0)>=1;}
    static List<Map<String,Object>> items(Map<String,Object> m,String key){return m!=null&&m.get(key) instanceof List<?>?list(m,key):Collections.<Map<String,Object>>emptyList();}
    private static String ids(List<Map<String,Object>> units){StringBuilder s=new StringBuilder();for(Map<String,Object> u:units){if(s.length()>0)s.append(',');s.append(id(u));}return s.toString();}
    private static String encode(Object value)throws Exception{return URLEncoder.encode(String.valueOf(value),"UTF-8");}
}
