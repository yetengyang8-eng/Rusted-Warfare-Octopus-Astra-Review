package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.BattleClient.*;

/** Conservative linkage of accepted spending to a later legal own-state effect.
 * A receipt, elapsed time, wallet delta and an empty queue never settle a hold.
 * Construction association uses the existing product/site envelope; ambiguity
 * retains the commitment and does not assert producer lineage or completion.
 */
public final class NativeCreditWitness {
    private static final class Hold {
        Intent intent; long frame, actor; String product,originalType; double tier=-1,x,y;
        final Map<Long,Double> before=new LinkedHashMap<Long,Double>();
        List<String> effectGroup;List<Long> effectUnits;
        boolean queueEmpty,site,actorObserved;
    }
    private final Map<String,Hold> holds=new LinkedHashMap<String,Hold>();
    /** Pending observation retirement never refunds a receipt or releases an unobserved paid queue. */
    public boolean hasUnsettledQueue(long actor){
        for(Hold h:holds.values())if(h.actor==actor&&"/command/queue".equals(h.intent.kind))return true;
        return false;
    }
    public void accepted(Intent intent,Map<String,Object> receipt,Map<String,Object> context,Map<String,Object> state){
        if(!intent.commitment.spending||intent.commitment.credits==null||intent.commitment.credits==0||intent.actorIds.size()!=1)return;
        if(holds.containsKey(intent.intentId))return;
        Hold h=new Hold();h.intent=intent;h.actor=intent.actorIds.get(0);h.frame=intent.observation.frame;
        if(receipt!=null&&receipt.get("frame") instanceof Number)h.frame=Math.max(h.frame,((Number)receipt.get("frame")).longValue());
        h.product=context.get("product") instanceof String?(String)context.get("product"):null;
        for(Map<String,Object> u:units(state)){
            h.before.put(id(u),StrategyDirector.number(u,"buildProgress",-1));
            if(id(u)==h.actor&&!Boolean.TRUE.equals(u.get("dead"))){h.actorObserved=true;h.originalType=u.get("type") instanceof String?(String)u.get("type"):null;h.tier=StrategyDirector.number(u,"techLevel",-1);h.queueEmpty=StrategyDirector.number(u,"productionQueue",-1)==0;}
        }
        boolean hasX=false,hasY=false;
        // A legal production menu may be newer than /state. Its explicit own queue
        // supplies only the before-command baseline; a later native effect after
        // the receipt is still required. These endpoints are not an atomic world.
        Long quotedActor=GameClock.number(context.get("producerBaselineActorId"));
        Long quotedQueue=GameClock.number(context.get("producerBaselineQueue"));
        if("/command/queue".equals(intent.kind)&&quotedActor!=null&&quotedActor==h.actor&&quotedQueue!=null&&quotedQueue>=0
                &&"/combat/production".equals(intent.costSourceRequestPath)
                &&intent.costSourceRequestPath.equals(context.get("producerBaselineRequestPath"))
                &&Objects.equals(intent.costSourceObservationId,context.get("producerBaselineObservationId"))){
            h.actorObserved=true;h.queueEmpty=quotedQueue==0;
            double quotedTier=StrategyDirector.number(context,"producerBaselineTier",-1);if(quotedTier>=0)h.tier=quotedTier;
        }
        for(String field:intent.path.substring(intent.path.indexOf('?')+1).split("&")){
            try{
                if(field.startsWith("x=")){h.x=Double.parseDouble(field.substring(2));hasX=true;}
                if(field.startsWith("y=")){h.y=Double.parseDouble(field.substring(2));hasY=true;}
            }catch(NumberFormatException invalid){h.x=h.y=Double.NaN;break;}
        }
        h.site=hasX&&hasY&&Double.isFinite(h.x)&&Double.isFinite(h.y)&&construction(h.intent.kind);
        // The native guard may resolve a building type or adjust the legal footprint.
        // These receipt fields identify the request, not its execution or spending.
        if(h.site&&receipt!=null){
            if(receipt.get("type") instanceof String)h.product=(String)receipt.get("type");
            double nativeX=StrategyDirector.number(receipt,"targetX",Double.NaN),nativeY=StrategyDirector.number(receipt,"targetY",Double.NaN);
            if(Double.isFinite(nativeX)&&Double.isFinite(nativeY)){h.x=nativeX;h.y=nativeY;}
        }
        holds.put(intent.intentId,h);
    }
    public List<Map<String,Object>> observe(CommandArbiter.Stamp stamp,String observationId,Map<String,Object> state,ExecutionScheduler scheduler){
        List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
        if(stamp==null||observationId==null||observationId.trim().isEmpty()||!legalState(stamp,state))return result;
        Map<Hold,String> settled=constructionEffects(stamp,state);
        for(Hold h:holds.values()){
            if(!eligible(h,stamp))continue;
            // Multiple unresolved orders for one actor do not have unique queue lineage.
            int actorHolds=0;for(Hold other:holds.values())if(other.actor==h.actor)actorHolds++;
            if(!h.site&&actorHolds!=1)continue;
            Map<String,Object> actor=null;for(Map<String,Object> u:units(state))if(id(u)==h.actor)actor=u;
            if(!h.site&&(actor==null||Boolean.TRUE.equals(actor.get("dead"))))continue;
            String kind=null;
            if(!h.site&&("/command/queue".equals(h.intent.kind)||"/command/produce-builder".equals(h.intent.kind)||"/command/invest".equals(h.intent.kind))
                    &&h.queueEmpty&&StrategyDirector.number(actor,"productionQueue",-1)>0)kind="NATIVE_QUEUE_NONEMPTY_NOT_PRODUCT_READY";
            if("/command/invest".equals(h.intent.kind)&&h.tier>=0&&StrategyDirector.number(actor,"techLevel",-1)>h.tier)kind="NATIVE_TIER_ADVANCED";
            if("/command/queue".equals(h.intent.kind)&&"upgrade".equals(h.product)&&h.tier>=0&&StrategyDirector.number(actor,"techLevel",-1)>h.tier)kind="NATIVE_TIER_ADVANCED";
            if("/command/invest".equals(h.intent.kind)&&h.originalType!=null&&h.product!=null&&!h.originalType.equals(h.product)
                    &&h.product.equals(actor.get("type"))&&StrategyDirector.number(actor,"buildProgress",-1)>=1)kind="NATIVE_OWN_CONVERSION_OBSERVED";
            if(kind!=null)settled.put(h,kind);
        }
        // Determine all ambiguity before removing anything; iteration order cannot create lineage.
        for(Map.Entry<Hold,String> entry:settled.entrySet()){
            Hold h=entry.getKey();String kind=entry.getValue();
            if(kind!=null&&scheduler.confirmNativeEffect(h.intent.intentId,observationId,kind)){
                Map<String,Object> row=StrategyDirector.map("intentId",h.intent.intentId,"observationId",observationId,"requestPath","/state","witnessKind",kind,
                        "sourceFrame",stamp.frame,"sourceGameTimeMs",stamp.gameTimeMs,"credits",h.intent.commitment.credits,"productReady",false);
                if(h.effectGroup!=null){row.put("constructionGroupIntentIds",h.effectGroup);row.put("constructionEffectUnitIds",h.effectUnits);row.put("individualProducerLineageClaimed",false);}
                result.add(row);holds.remove(h.intent.intentId);
            }
        }
        return result;
    }
    private static boolean eligible(Hold h,CommandArbiter.Stamp stamp){
        return h.actorObserved&&h.intent.observation.samePlayer(stamp)&&stamp.frame>h.frame&&stamp.gameTimeMs>=h.intent.observation.gameTimeMs;
    }
    private static boolean inSite(Hold h,Map<String,Object> unit){
        return Math.hypot(StrategyDirector.number(unit,"x",Double.NaN)-h.x,StrategyDirector.number(unit,"y",Double.NaN)-h.y)<120;
    }
    /** Later site effects can prove an overlapping group's spending without
     * assigning any product to an individual constructor. Each edge uses that
     * hold's own baseline; mere geometric overlap is not a possible effect.
     * Unobservable competing holds and unknown progress remain ambiguity. */
    private Map<Hold,String> constructionEffects(CommandArbiter.Stamp stamp,Map<String,Object> state){
        Map<Hold,Set<Long>> edges=new LinkedHashMap<Hold,Set<Long>>();
        Map<Long,Set<Hold>> reverse=new LinkedHashMap<Long,Set<Hold>>();
        Set<Hold> uncertain=new HashSet<Hold>();Set<Long> blocked=new HashSet<Long>();
        for(Hold h:holds.values())if(h.site&&h.intent.observation.samePlayer(stamp)){
            boolean usable=eligible(h,stamp)&&h.product!=null;
            if(usable)edges.put(h,new LinkedHashSet<Long>());
            for(Map<String,Object> u:units(state))if(!Boolean.TRUE.equals(u.get("dead"))
                    &&(h.product==null||h.product.equals(u.get("type")))&&inSite(h,u)){
                long unitId=id(u);Double old=h.before.get(unitId);double progress=StrategyDirector.number(u,"buildProgress",-1);
                if(!usable){blocked.add(unitId);continue;}
                if(!Double.isFinite(progress)||progress<0||old!=null&&(!Double.isFinite(old)||old<0)){
                    uncertain.add(h);blocked.add(unitId);continue;
                }
                if(old!=null&&progress<=old)continue;
                edges.get(h).add(unitId);
                Set<Hold> adjacent=reverse.get(unitId);if(adjacent==null){adjacent=new LinkedHashSet<Hold>();reverse.put(unitId,adjacent);}adjacent.add(h);
            }
        }
        Map<Hold,String> result=new LinkedHashMap<Hold,String>();Set<Hold> visited=new HashSet<Hold>();
        for(Hold first:edges.keySet())if(!visited.contains(first)&&!edges.get(first).isEmpty()){
            Set<Hold> component=new LinkedHashSet<Hold>();Set<Long> effects=new LinkedHashSet<Long>();
            Deque<Hold> pending=new ArrayDeque<Hold>();pending.add(first);boolean ambiguous=false;
            while(!pending.isEmpty()){
                Hold h=pending.removeFirst();if(!component.add(h))continue;visited.add(h);
                if(uncertain.contains(h))ambiguous=true;
                for(Long unitId:edges.get(h))if(effects.add(unitId)){
                    if(blocked.contains(unitId))ambiguous=true;
                    for(Hold adjacent:reverse.get(unitId))if(!component.contains(adjacent))pending.addLast(adjacent);
                }
            }
            // Too few effects cannot cover every hold; extra effects do not
            // uniquely identify which accepted spending the group explains.
            if(ambiguous||component.size()!=effects.size())continue;
            Map<Long,Hold> matched=new HashMap<Long,Hold>();boolean complete=true;
            for(Hold h:component)if(!match(h,edges,matched,new HashSet<Long>())){complete=false;break;}
            if(complete){
                List<String> group=new ArrayList<String>();for(Hold h:component)group.add(h.intent.intentId);
                List<Long> products=new ArrayList<Long>(effects);Collections.sort(group);Collections.sort(products);
                for(Hold h:component){h.effectGroup=group;h.effectUnits=products;result.put(h,component.size()==1
                    ?"NATIVE_CONSTRUCTION_SITE_EFFECT_NOT_PRODUCER_LINEAGE":"NATIVE_CONSTRUCTION_GROUP_EFFECT_NOT_INDIVIDUAL_LINEAGE");}
            }
        }
        return result;
    }
    private static boolean match(Hold h,Map<Hold,Set<Long>> edges,Map<Long,Hold> matched,Set<Long> visited){
        for(Long unitId:edges.get(h))if(visited.add(unitId)){
            Hold previous=matched.get(unitId);
            if(previous==null||match(previous,edges,matched,visited)){matched.put(unitId,h);return true;}
        }
        return false;
    }
    private static boolean construction(String kind){return "/command/construct".equals(kind)||"/command/build-extractor".equals(kind)||"/command/build-factory".equals(kind);}
    private static boolean legalState(CommandArbiter.Stamp stamp,Map<String,Object> state){
        if(state==null||!stamp.session.equals(state.get("sessionId"))||!"running".equals(state.get("status"))
                ||Boolean.TRUE.equals(state.get("networked"))||Boolean.TRUE.equals(state.get("replay"))
                ||!(state.get("frame") instanceof Number)||!(state.get("gameTimeMs") instanceof Number)||!(state.get("player") instanceof Map))return false;
        if(((Number)state.get("frame")).longValue()!=stamp.frame||((Number)state.get("gameTimeMs")).longValue()!=stamp.gameTimeMs)return false;
        Object team=obj(state.get("player")).get("teamId");String player=team instanceof Number?"team:"+((Number)team).longValue():"legacy-local";
        return stamp.player.equals(player);
    }
}
