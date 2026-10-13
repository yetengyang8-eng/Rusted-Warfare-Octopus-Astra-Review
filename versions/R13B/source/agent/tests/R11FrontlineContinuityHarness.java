package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Lawful synthetic positions prove controller transitions, not natural travel or victory. */
public final class R11FrontlineContinuityHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Builder extends G51ProspectIntegrationHarness.Fixture {
        Builder()throws Exception{super();credits=100;}
        public boolean economicArmyPolicyStrategy(){return true;}
        @Override public Map<String,Object> readStrategy(String path,String event){
            if(path.startsWith("/scout/resource-approach")){
                long tile=Long.parseLong(path.split("tile=")[1].split("&")[0]);
                Map<String,Object> r=resources.stream().filter(v->((Number)v.get("tile")).longValue()==tile).findFirst().orElse(null);
                if(r==null)return null;
                double x=((Number)builder.get("x")).doubleValue(),y=((Number)builder.get("y")).doubleValue();
                double tx=((Number)r.get("x")).doubleValue(),ty=((Number)r.get("y")).doubleValue(),d=Math.hypot(tx-x,ty-y);
                double step=Math.min(300,d),wx=d==0?tx:x+(tx-x)*step/d,wy=d==0?ty:y+(ty-y)*step/d;
                return map("status","planned","sessionId","session","frame",frame,"gameTimeMs",now,"unitId",5924L,"tile",tile,
                    "pathKnown",true,"safeSegmentKnown",true,"x",tx,"y",ty,"waypointX",wx,"waypointY",wy);
            }return super.readStrategy(path,event);
        }
        void moveToLastReceipt(long time,long frame)throws Exception{
            String p=orders.get(orders.size()-1);builder.put("x",Double.parseDouble(p.split("&x=")[1].split("&")[0]));
            builder.put("y",Double.parseDouble(p.split("&y=")[1].split("&")[0]));observe(time,frame);
        }
    }
    static void simultaneousMineFunds()throws Exception{
        Builder b=new Builder(){
            public boolean parallelStrategy(){return true;}
            public double effectiveStrategyCredits(double nativeCredits){return credits;}
            public void spendStrategy(String category,long cost,String type,long actor){credits-=cost;}
            public Map<String,Object> readStrategy(String path,String event){
                if(path.startsWith("/expansion/plan?unitId=5950"))return map("status","planned","sessionId","session","builderId",5950L,
                    "extractorCost",700.,"extractorX",12000.,"extractorY",410.);
                return super.readStrategy(path,event);
            }
        };
        b.credits=850;b.builder.put("x",7410.);b.builder.put("y",410.);
        b.own.add(G51ProspectIntegrationHarness.unit(5950,"builder",12000,410));b.observe(135000,101);
        java.lang.reflect.Field workers=StrategyDirector.class.getDeclaredField("workers");workers.setAccessible(true);
        Map<Long,Object> all=(Map<Long,Object>)workers.get(b.director);
        for(long id:new long[]{5924,5950}){
            Object worker=all.get(id);java.lang.reflect.Field p=worker.getClass().getDeclaredField("prospect");p.setAccessible(true);
            ProspectCommitment prospect=(ProspectCommitment)p.get(worker);double x=id==5924?7410:12000;
            prospect.accepted(id==5924?42:43,x,410,x-300,410,100,120000);prospect.observeArrival(x,410,101,135000);
        }
        b.plan=map("status","planned","sessionId","session","builderId",5924L,"extractorCost",850.,"extractorX",7410.,"extractorY",410.);
        b.observe(136000,102);check(b.director.frontierConstructionReserve()==1550,"both real local quotes are protected from ordinary production");
        b.director.act(1550,1);
        check(b.orders.stream().filter(p->p.startsWith("/command/build-extractor")).count()==1,"one affordable mine starts without waiting to fund every mine together");
        check(b.credits==0&&b.director.frontierConstructionReserve()==700,"accepted credit charge prevents the second mine from overspending and preserves its unpaid quote");b.director.close();
    }
    static void builderRetainsMineral()throws Exception{
        Builder b=new Builder();b.director.act(0,1);String first=b.orders.get(b.orders.size()-1);
        check(first.startsWith("/command/move?unitId=5924"),"builder begins native resource journey");
        b.resources.add(G51ProspectIntegrationHarness.resource(43,6300,3450));
        b.moveToLastReceipt(135000,101);
        check(b.orders.size()==2,"waypoint arrival continues one resource journey instead of idling for a new allocation");
        Map<String,Object> continued=b.events.stream().filter(e->"strategy_resource_journey_continued".equals(e.get("event"))).findFirst().orElse(null);
        check(continued!=null&&String.valueOf(continued).contains("tile=42"),"a tempting nearer mineral does not replace the committed journey destination");
        check(b.director.frontierConstructionReserve()==0,"far static travel does not reserve a speculative mine price");
        String second=b.orders.get(b.orders.size()-1);check(!second.equals(first),"same goal receives the next legal route leg");
        for(int i=0;i<15&&b.director.frontierConstructionReserve()==0;i++){
            String p=b.orders.get(b.orders.size()-1);double x=Double.parseDouble(p.split("&x=")[1].split("&")[0]),y=Double.parseDouble(p.split("&y=")[1].split("&")[0]);
            if(Math.hypot(x-7410,y-410)<75)b.plan=map("status","planned","sessionId","session","builderId",5924L,
                "extractorCost",850.,"extractorX",7410.,"extractorY",410.);
            b.moveToLastReceipt(150000+i*15000,102+i);
        }
        check(b.director.frontierConstructionReserve()==850,"later actual goal position and current native quote turn travel into exact construction funding");
        int before=b.orders.size();b.director.act(850,1);check(b.orders.size()==before,"insufficient funds cannot fabricate construction");
        b.credits=1000;b.observe(400000,200);b.director.act(b.director.frontierConstructionReserve(),1);
        check(b.orders.stream().anyMatch(p->p.startsWith("/command/build-extractor?unitId=5924&x=7410.0")),"funded native goal dispatches its actual mine guard after continuous travel");
        b.director.close();
    }
    static void stalledRouteCannotHoldForever()throws Exception{
        Builder b=new Builder(){double oldX=Double.NaN,oldY;
            public Map<String,Object> readStrategy(String path,String event){Map<String,Object> r=super.readStrategy(path,event);
                if(path.startsWith("/scout/resource-approach")&&r!=null){if(Double.isNaN(oldX)){oldX=((Number)r.get("waypointX")).doubleValue();oldY=((Number)r.get("waypointY")).doubleValue();}
                    r.put("waypointX",oldX);r.put("waypointY",oldY);}return r;}};
        b.director.act(0,1);b.moveToLastReceipt(135000,101);
        b.observe(190000,102);
        check(b.events.stream().anyMatch(e->"strategy_task_blocked".equals(e.get("event"))&&String.valueOf(e).contains("PROSPECT_ROUTE_NO_PROGRESS")),
            "same idle corner cannot bypass the existing movement progress timeout");
        check(b.director.frontierConstructionReserve()==0,"failed travel has no fabricated mine funding");b.director.close();
    }
    static void acceptedShortLegsAreNotProgress()throws Exception{
        Builder b=new Builder(){int calls;
            public Map<String,Object> readStrategy(String path,String event){Map<String,Object> r=super.readStrategy(path,event);
                if(path.startsWith("/scout/resource-approach")&&r!=null){r.put("waypointX",((Number)builder.get("x")).doubleValue()+(++calls%2==0?30:-30));
                    r.put("waypointY",builder.get("y"));}return r;}};
        b.director.act(0,1);
        for(int i=1;i<=7;i++)b.observe(120000+i*10000,100+i);
        check(b.orders.size()>1,"fixture has distinct accepted short legs without changing own position");
        check(b.events.stream().anyMatch(e->"strategy_task_blocked".equals(e.get("event"))&&String.valueOf(e).contains("PROSPECT_ROUTE_NO_PROGRESS")),
            "accepted alternating short legs cannot reset actual own no-progress time");b.director.close();
    }
    static void cheapBuildingClassification()throws Exception{
        G53FreeResponseHarness.Fixture f=R10TemporaryResponseHarness.nearby(1);
        f.own.add(map("id",997L,"type","extractorT1","building",true,"x",1000.,"y",100.,"hp",1000.,"maxHp",1000.,"buildProgress",1.));
        f.enemy(800,1800,1000,"SURFACE");Map<String,Object> enemy=f.visible.get(0);
        enemy.put("type","extractorT1");enemy.put("building",true);enemy.put("canAttack",false);f.observe();f.run();
        check(f.commander.preferredThreats().isEmpty(),"isolated unarmed building does not divert a whole General");
        check(f.response()!=null&&!f.response().actors.isEmpty()&&f.response().actors.size()<6,"HOME_LOCAL cheap force handles current lawful building near defended extractor");
        f.visible.clear();f.observe();f.force.reconcile(f.state,f.enemies,f.frame*1000);
        R10TemporaryResponseHarness.invariant(f);
        enemy.put("type","commandCenter");enemy.put("canAttack",true);f.visible.add(enemy);f.observe();
        f.commander.prepareThreatTasks(f.state,f.enemies,f.frame*1000);
        check(!ForceController.smallThreatTask(f.commander.currentThreatTasks().get(0)),"armed base retains strategic response authority");
    }
    public static void main(String[] args)throws Exception{
        builderRetainsMineral();simultaneousMineFunds();stalledRouteCannotHoldForever();acceptedShortLegsAreNotProgress();cheapBuildingClassification();
        System.out.println("R11FrontlineContinuityHarness checks="+checks+" PASS fixtureOnly=true");
    }
}
