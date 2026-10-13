import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import io.rwagent.client.*;
import com.corrodinggames.rts.game.units.am;

/** Real original-game HTTP/command queue fixture. All placement/fog/clock seed controls are explicit.
 * No simulation ticks, natural aircraft attack, production completion or survival benefit is claimed. */
public final class G51NativeOperationsHarness {
    static int checks;
    static void require(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static void air()throws Exception{
        G3NativeAcceptanceHarness.base(0,2,40000);float[] dry=G3NativeAcceptanceHarness.dry();
        for(int i=0;i<6;i++)G3NativeAcceptanceHarness.unit("tank",100+i,G3NativeAcceptanceHarness.engine.bs,dry[0]+i*12,dry[1]+150);
        am enemy=G3NativeAcceptanceHarness.unit("gunShip",930,G5NativeOperationsHarness.opponent(),dry[0]+130,dry[1]+150);enemy.eq=20;
        G3NativeAcceptanceHarness.record("fixture_air",G3NativeAcceptanceHarness.map("enemyId",930,"ownType","tank","count",6,"aircraftHeightSeed",20,"hpUnmodified",true,"naturalFlight",false));
        G5NativeOperationsHarness.client=new G3NativeAcceptanceHarness.Client("g51-native-air",128,false);
        G3NativeAcceptanceHarness.set(G5NativeOperationsHarness.client.battle,"openingScoutOrdered",true); // Established seeded army, not opening procurement.
        G5NativeOperationsHarness.observe();G3NativeAcceptanceHarness.Client client=G5NativeOperationsHarness.client;
        Map<String,Object> current=G5NativeOperationsHarness.enemy(930);
        require("AIR".equals(current.get("targetDomain")),"original gunShip is currently legally AIR");
        GeneralRegistry.GeneralView general=G5NativeOperationsHarness.registry().generals().get(0);
        Map<String,Object> view=G5NativeOperationsHarness.combat().view(general.id);
        require("ANTI_AIR".equals(view.get("capabilityNeed")),"actual current native tank vs aircraft produces General AIR shortage");
        require(!G5NativeOperationsHarness.combat().retreating(general.id),"native visible uncovered aircraft alone does not cause crisis retreat");
        G5NativeOperationsHarness.collect();G5NativeOperationsHarness.flush();
        require(!"STANDOFF".equals(G5NativeOperationsHarness.combat().view(general.id).get("tacticalChoice")),"one original chip aircraft does not impose a whole-group ground advance veto");
        G3NativeAcceptanceHarness.process();
        // Retain the movement safety check under substantial current native air pressure.
        // These are explicit native unit seeds, not inferred hidden aircraft or a natural attack.
        for(int i=0;i<12;i++)G3NativeAcceptanceHarness.unit("gunShip",931+i,G5NativeOperationsHarness.opponent(),dry[0]+130+i,dry[1]+150).eq=20;
        G3NativeAcceptanceHarness.record("fixture_substantial_air",G3NativeAcceptanceHarness.map("additionalGunships",12,"basis","EXPLICIT_CURRENT_NATIVE_AIR_SEEDS_NOT_NATURAL_COMBAT"));
        client.advance(1000,false);G5NativeOperationsHarness.observe();
        G5NativeOperationsHarness.collect();G5NativeOperationsHarness.flush();int moves=0;
        for(Map<String,Object> command:G3NativeAcceptanceHarness.commands())if(((List<?>)command.get("actors")).contains(100L)){
            require("move".equals(command.get("waypoint")),"actual original command buffer uses move for uncovered-air standoff, not reverse attackMove");moves++;}
        require(moves==1,"native standoff dispatch contains the owned first actor once");G3NativeAcceptanceHarness.process();
        client.advance(1000,false);G5NativeOperationsHarness.observe();require("move".equals(client.actor(100).get("orderType")),"later native waypoint witness confirms standoff move application");
        G5NativeOperationsHarness.actor(100).cu-=1;G3NativeAcceptanceHarness.record("fixture_own_damage",G3NativeAcceptanceHarness.map("actor",100,"hpDecrease",1,"basis","EXPLICIT_NATIVE_CHIP_FIXTURE_NOT_NATURAL_AIR_ATTACK"));
        client.advance(1000,false);G5NativeOperationsHarness.observe();require(!G5NativeOperationsHarness.combat().retreating(general.id),"single native chip hit with visible AIR gap does not withdraw the entire General");
        for(Long id:general.members)G5NativeOperationsHarness.actor(id).cu*=.7f;G3NativeAcceptanceHarness.record("fixture_own_damage",G3NativeAcceptanceHarness.map("actors",general.members,"hpDecreaseFraction",.3,"basis","EXPLICIT_NATIVE_LOSING_EXCHANGE_HP_FIXTURE_NOT_NATURAL_AIR_ATTACK"));
        client.advance(1000,false);G5NativeOperationsHarness.observe();require(G5NativeOperationsHarness.combat().retreating(general.id),"bounded own native HP harm with current AIR gap admits independent protective retreat");
        client.advance(2500,false);G5NativeOperationsHarness.observe();client.produce();
        List<Map<String,Object>> selected=G5NativeOperationsHarness.rows("g51_air_response_selected");
        require(selected.size()==2,"two native producer quotes fund bounded two counter choices");
        require(G3NativeAcceptanceHarness.engine.cf.b.size()==2,"two actual original native queues accepted");
        for(Map<String,Object> row:selected){Map<String,Object> data=G3NativeAcceptanceHarness.packet(row.get("data"));
            require(AirResponsePolicy.counter(String.valueOf(data.get("product"))),"original native menu selects catalog-compatible mobile counter, without hardcoded product");
            require("/combat/production".equals(data.get("costSourceRequestPath"))&&data.get("costSourceObservationId") instanceof String,"counter saves actual menu quote provenance");
        }
        for(Map<String,Object> row:G5NativeOperationsHarness.rows("g3_execution")){Map<String,Object> data=G3NativeAcceptanceHarness.packet(row.get("data"));
            if(Boolean.TRUE.equals(data.get("nativeAccepted"))&&"ORDINARY_PRODUCTION".equals(data.get("lane"))){Map<String,Object> commitment=G3NativeAcceptanceHarness.packet(data.get("commitment"));
                require(((Number)commitment.get("militarySlots")).intValue()==1,"every selected counter reserves one military slot including newly enabled native types");}}
        double before=G3NativeAcceptanceHarness.engine.bs.o;G3NativeAcceptanceHarness.process();
        require(G3NativeAcceptanceHarness.engine.bs.o<before,"original command.k witnesses later payment effect");
        client.advance(1000,false);int queues=0;for(Map<String,Object> unit:G3NativeAcceptanceHarness.items(client.state,"ownUnits"))
            if("landFactory".equals(unit.get("type"))&&((Number)unit.get("productionQueue")).intValue()>0)queues++;
        require(queues==2,"later native queue witness for both counter orders, not ready products");
        client.close();
    }
    public static void main(String[] args){try{
        if(args.length!=1)throw new IllegalArgumentException("OUTPUT_DIRECTORY");G5NativeOperationsHarness.out=Paths.get(args[0]).toAbsolutePath();Files.createDirectories(G5NativeOperationsHarness.out);
        G5NativeOperationsHarness.bootstrap();G5NativeOperationsHarness.nativeLargeCompatibility();G5NativeOperationsHarness.nativeAdditionalFormation();G5NativeOperationsHarness.nativeIndependentRetreat();air();
        Map<String,Object> summary=G3NativeAcceptanceHarness.map("status","PASS","checks",checks+G5NativeOperationsHarness.checks,"airChecks",checks,
            "evidence","E2_NATIVE_FIXTURE_REAL_HTTP","simulationTicks",0,"desktopTouched",false,"naturalSurvivalBenefit","NEEDS_EVIDENCE");
        Files.write(G5NativeOperationsHarness.out.resolve("g51-native-summary.json"),G3NativeAcceptanceHarness.json(summary).getBytes(StandardCharsets.UTF_8));
        G3NativeAcceptanceHarness.record("summary",summary);G3NativeAcceptanceHarness.fixture.close();System.exit(0);
    }catch(Throwable error){error.printStackTrace();System.exit(1);}}
}
