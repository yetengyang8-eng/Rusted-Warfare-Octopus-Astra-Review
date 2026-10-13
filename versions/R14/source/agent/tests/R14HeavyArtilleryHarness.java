package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.StrategyDirector.map;

/** Current own/contact fixtures, not native movement or firing evidence. */
public final class R14HeavyArtilleryHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void catalog(boolean missing){
        check(TargetCatalog.available()&&TargetCatalog.PACKET_SHA256.equals(TargetCatalog.catalogSha256()),"original accepted packet identity remains independently valid");
        check("LAND".equals(TargetCatalog.movementType("heavyTank")),"existing heavyTank mechanics retained");
        for(String type:Arrays.asList("heavyArtillery","heavy_artillery")){
            if(missing){check(TargetCatalog.movementType(type)==null&&"UNKNOWN".equals(TargetCatalog.evaluate(type,"SURFACE",false,true,true).status),"missing or corrupt supplemental profile remains UNKNOWN");continue;}
            check("LAND".equals(TargetCatalog.movementType(type)),"distinct native heavy artillery joins LAND movement cohort");
            check("COMPATIBLE".equals(TargetCatalog.evaluate(type,"SURFACE",false,true,true).status),"current dry SURFACE domain is supported");
            check("COMPATIBLE".equals(TargetCatalog.evaluate(type,"SURFACE",null,true,true).status),"surface support does not fabricate a water-only gun constraint");
            check("INCOMPATIBLE".equals(TargetCatalog.evaluate(type,"AIR",null,true,true).status),"nearby helicopter never becomes a heavy artillery target");
            check("INCOMPATIBLE".equals(TargetCatalog.evaluate(type,"SUBMERGED",true,true,true).status),"underwater domain remains unsupported");
            check("UNKNOWN".equals(TargetCatalog.evaluate(type,"SURFACE",false,false,true).status),"static mechanics cannot authorize a last-known target");
            check("UNKNOWN".equals(TargetCatalog.evaluate(type,"SURFACE",false,true,false).status),"unverified game identity cannot authorize new type");
            check("UNKNOWN".equals(TargetCatalog.evaluate(type,null,false,true,true).status),"unknown current domain remains UNKNOWN");
            check(TargetCatalog.evaluate(type,"SURFACE",false,true,true).sourceId.contains("HEAVY_ARTILLERY_1_15.json#sha256:"),"decision records independently verified heavy profile provenance");
        }
        check("UNKNOWN".equals(TargetCatalog.evaluate("unknownBuiltInType","SURFACE",false,true,true).status),"supplement does not relax unrelated UNKNOWN types");
        check("INCOMPATIBLE".equals(TargetCatalog.evaluate("artillery","AIR",null,true,true).status),"ordinary artillery retains its original independent profile");
    }
    static void mixedFrontier()throws Exception{
        R11GeneralMotionHarness.Fixture f=new R11GeneralMotionHarness.Fixture();
        f.find(15).put("type","heavyArtillery");f.find(15).put("hp",600.);f.find(15).put("maxHp",600.);
        f.plan(101,800);f.run();
        check(f.first().size()==1&&f.first().get(0).actors.size()==6&&f.first().get(0).actors.contains(15L),"same accepted native route includes attached heavy artillery beside five heavy tanks");
        long generation=f.registry.unit(15).ownerGeneration;f.accept();
        for(long id=10;id<15;id++){f.find(id).put("orderType","attackMove");f.find(id).put("orderX",800.);f.find(id).put("orderY",100.);}
        f.find(15).put("orderType",null);f.sample(5000);f.run();
        check(f.first().size()==1&&f.first().get(0).actors.equals(Collections.singletonList(15L)),"later own null-order artillery resumes existing frontier while matching heavyTank native orders are reused");
        check(f.registry.unit(15).ownerGeneration==generation&&"general:1".equals(f.registry.unit(15).owner),"mixed movement does not transfer or duplicate ownership");
        f.accept();f.find(15).put("orderType","attackMove");f.find(15).put("orderX",800.);f.find(15).put("orderY",100.);f.sample(6000);f.run();
        check(f.first().isEmpty(),"current matching artillery order closes the local retry loop");
    }
    static void detachmentReturn()throws Exception{
        R13ThreatDetachmentHarness.Fixture f=new R13ThreatDetachmentHarness.Fixture(12,0);
        for(long id=1;id<12;id++)f.own(id).put("x",6000.);
        f.own(12).put("type","heavyArtillery");f.own(12).put("x",1700.);f.own(12).put("hp",600.);f.own(12).put("maxHp",600.);
        f.enemy(501,1000,1800,false);f.observe();f.run();
        check(f.responses().size()==1&&f.actors(0).equals(Collections.singletonList(12L)),"nearby attached artillery can retain minimum remote cleanup responsibility without moving main General");
        String owner=f.registry.unit(12).owner;long generation=f.registry.unit(12).ownerGeneration;
        Map<Long,Long> old=f.gate.snapshotGenerations(owner,Collections.singletonList(12L));
        f.accept(f.lane("THREAT_DETACHMENT").get(0));f.contacts.clear();f.observe();f.run();
        check(f.lane("THREAT_DETACHMENT").isEmpty()&&f.lane("THREAT_REACQUIRE").size()==1&&owner.equals(f.registry.unit(12).owner),"lost contact switches artillery to lawful region MOVE while retaining responsibility");
        f.regionVisible=true;f.observe();f.run();f.observe();f.run();
        check(f.responses().isEmpty()&&f.general().owner().equals(f.registry.unit(12).owner)&&f.registry.unit(12).ownerGeneration==generation+1,"searched current region returns heavy artillery to original General generation");
        check("STALE_OWNER_GENERATION".equals(f.gate.validateGenerations(Collections.singletonList(12L),old)),"old detachment token cannot act after return ABA");
        f.invariants();
    }
    public static void main(String[] args)throws Exception{
        boolean missing=args.length>0;catalog(missing);if(!missing){mixedFrontier();detachmentReturn();}
        System.out.println("R14HeavyArtilleryHarness checks="+checks+" PASS profile="+(missing?"MISSING_OR_CORRUPT":"VALID")+" fixtureOnly=true");
    }
}
