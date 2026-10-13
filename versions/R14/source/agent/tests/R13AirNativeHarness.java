import com.corrodinggames.rts.game.units.am;
import com.corrodinggames.rts.game.units.ar;
import com.corrodinggames.rts.game.units.y;
import java.util.*;

/** Native 1.15 domain flags, target command and legal fog memory; explicit fixtures, zero ticks. */
public final class R13AirNativeHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Map<?,?> get(String path)throws Exception{return EconomyHarness.obj(BridgeHarness.check("GET",path,200,"sessionId"));}
    static Map<?,?> row(Map<?,?> packet,String field,long id){for(Object raw:(List<?>)packet.get(field)){
        Map<?,?> unit=(Map<?,?>)raw;if(((Number)unit.get("id")).longValue()==id)return unit;}throw new AssertionError("missing "+field+" id="+id);}
    static am add(String type,long id,com.corrodinggames.rts.game.n team,float x,float yy){
        am unit=ar.valueOf(type).a(true);unit.eh=id;unit.bX=team;unit.eo=x;unit.ep=yy;unit.cm=1;am.bE.add(unit);return unit;
    }
    public static void main(String[] args){try{
        OpeningHarness.test();com.corrodinggames.rts.game.i engine=EconomyHarness.engine;engine.cf.b.clear();
        // Original am.cH() uses the native isOverWater D/A grids. This explicit dry fixture
        // supplies them; unavailable terrain must remain UNKNOWN in production.
        EconomyHarness.set(engine.bU,"D",EconomyHarness.costs);EconomyHarness.set(engine.bU,"A",EconomyHarness.costs);
        for(byte[] column:engine.bs.N)Arrays.fill(column,(byte)0);
        com.corrodinggames.rts.game.e enemyTeam=new com.corrodinggames.rts.game.e(1,false);enemyTeam.r=1;engine.bs.r=0;
        am flyer=add("gunShip",193,engine.bs,500,610);flyer.eq=20;
        am aa=add("antiAirTurret",201,enemyTeam,610,610);
        am factory=add("landFactory",202,enemyTeam,630,610);
        am mine=add("extractor",203,enemyTeam,650,610);
        am surface=add("turret",204,enemyTeam,670,610);
        am enemyFlyer=add("gunShip",205,enemyTeam,690,610);enemyFlyer.eq=20;
        check(((y)aa).af()&&!((y)aa).ae(),"actual original antiAirTurret has anti-air and no submerged weapon");
        check(!factory.l()&&!mine.l()&&((y)surface).ag()&&!((y)surface).af(),"actual original factory/mine are unarmed and surface turret cannot fire at AIR");
        Map<?,?> observed=get("/combat/observe");Map<?,?> aaRow=row(observed,"visibleEnemies",201),factoryRow=row(observed,"visibleEnemies",202),mineRow=row(observed,"visibleEnemies",203),surfaceRow=row(observed,"visibleEnemies",204);
        check(Boolean.TRUE.equals(aaRow.get("canAttackAir"))&&Boolean.FALSE.equals(aaRow.get("canAttackSubmerged"))
            &&((Number)aaRow.get("weaponRange")).doubleValue()==((y)aa).m(),"current legal enemy packet exports actual native anti-air domain and range");
        for(Map<?,?> harmless:Arrays.asList(factoryRow,mineRow))check(Boolean.FALSE.equals(harmless.get("canAttack"))
            &&Boolean.FALSE.equals(harmless.get("canAttackAir"))&&Boolean.FALSE.equals(harmless.get("canAttackSubmerged")),"unarmed native structure cannot become domain risk from HP");
        check(Boolean.FALSE.equals(surfaceRow.get("canAttackAir"))&&Boolean.TRUE.equals(surfaceRow.get("canAttackSurface")),"native surface-only gun stays distinct from anti-air");
        Map<?,?> scout=get("/scout/observe");Map<?,?> aaThreat=row(scout,"rememberedThreats",201);
        check(Boolean.TRUE.equals(aaThreat.get("canAttackAir"))&&Boolean.FALSE.equals(aaThreat.get("canAttackSubmerged")),"Scout captures legal weapon-domain fields for later route veto");
        String session=String.valueOf(observed.get("sessionId")),suffix="&sessionId="+session+"&requestId=";
        String attack="/command/attack-target?unitIds=193&targetId=201"+suffix+"r13-aa";
        BridgeHarness.check("POST",attack,200,"\"orderType\":\"attack\"");BridgeHarness.check("POST",attack,200,"queued");
        check(engine.cf.b.size()==1,"same target receipt queues exactly one native command");
        com.corrodinggames.rts.gameFramework.e command=(com.corrodinggames.rts.gameFramework.e)engine.cf.b.get(0);
        check("attack".equals(command.j.d().name())&&command.j.i()==aa,"actual original native waypoint targets AA entity explicitly despite nearby harmless buildings");
        engine.bV=new com.corrodinggames.rts.gameFramework.aa();command.k();
        Map<?,?> own=row(get("/state"),"ownUnits",193);
        check("attack".equals(own.get("orderType"))&&((Number)own.get("orderTargetId")).longValue()==201,"later native command application exports own target ID witness");
        BridgeHarness.check("POST",attack.replace("targetId=201","targetId=205").replace("r13-aa","r13-incompatible"),409,"domain incompatible");
        BridgeHarness.check("POST",attack.replace("193","193,193").replace("r13-aa","r13-duplicate"),400,"duplicate");
        BridgeHarness.check("POST",attack.replace("r13-aa","r13-foreign").replace(session,"foreign"),409,"session changed");
        check(engine.cf.b.size()==1,"incompatible/duplicate/foreign target requests do not add native orders");
        engine.bs.N[30][30]=10;engine.by+=1000;aa.eo=1010;aa.ep=1010;aa.cu=1;engine.bs.N[50][50]=10;
        Map<?,?> hidden=get("/combat/observe"),memory=row(hidden,"rememberedEnemies",201);
        check(memory.get("x").equals(aaRow.get("x"))&&memory.get("hp").equals(aaRow.get("hp"))
            &&memory.get("canAttackAir").equals(aaRow.get("canAttackAir"))&&memory.get("weaponRange").equals(aaRow.get("weaponRange")),"hidden target memory keeps legally captured coordinates/HP/weapon fields and never refreshes hidden native state");
        BridgeHarness.check("POST",attack.replace("r13-aa","r13-hidden"),409,"legally visible hostile");
        check(engine.cf.b.size()==1,"last-known AA ID cannot authorize another explicit attack in fog");
        Map<?,?> remembered=row(get("/scout/observe"),"rememberedThreats",201);
        check(remembered.get("x").equals(aaThreat.get("x"))&&remembered.get("canAttackAir").equals(aaThreat.get("canAttackAir")),"Scout fog memory retains prior legal weapon record without hidden coordinate refresh");
        System.out.println("R13_AIR_NATIVE_OK checks="+checks+" simulationTicks=0 fixtureOnly=true receiptIsDamage=false nativeVictoryProven=false");System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
