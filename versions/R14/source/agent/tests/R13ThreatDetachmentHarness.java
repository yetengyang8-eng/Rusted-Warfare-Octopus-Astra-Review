package io.rwagent.client;

import java.util.*;
import static io.rwagent.client.GeneralRegistry.*;
import static io.rwagent.client.StrategyDirector.map;

/** Lawful synthetic own/contact/region samples. No native travel, kill or victory claim. */
public final class R13ThreatDetachmentHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final class Fixture implements ForceController.Host {
        final CommandArbiter gate=new CommandArbiter();final GeneralRegistry registry=new GeneralRegistry(gate);
        final CommanderDirector commander=new CommanderDirector(registry);final ForceController force=new ForceController(registry,this);
        final GeneralCombatDirector combat=new GeneralCombatDirector(registry,this);
        final List<Map<String,Object>> own=new ArrayList<Map<String,Object>>(),contacts=new ArrayList<Map<String,Object>>();
        final List<ForceController.Proposal> proposals=new ArrayList<ForceController.Proposal>();
        final Map<String,Object> state=map("sessionId","s","playerKey","team:0","ownUnits",own,
            "map",map("tilesWide",500,"tilesHigh",500,"tileWidth",20,"tileHeight",20));
        final Map<String,Object> enemies=map("sessionId","s","playerKey","team:0","visibleEnemies",contacts);
        long frame,now;boolean regionVisible,foreignProbe,blocked;
        Fixture(int members,int free){List<Long> group=new ArrayList<Long>(),ordinary=new ArrayList<Long>();
            for(long id=1;id<=members;id++){group.add(id);ordinary.add(id);own.add(actor(id,1200));}
            for(long id=100;id<100+free;id++){ordinary.add(id);own.add(actor(id,1800));}
            own.add(map("id",999L,"type","commandCenter","building",true,"x",100.,"y",100.,"hp",4000.,"maxHp",4000.,"buildProgress",1.));
            observe();registry.bootstrap(Collections.singletonList(group),ordinary);refresh();force.setGeneralCombatDirector(combat);
            combat.setCommander(commander);commander.setCombatProvider(combat::view);commander.setLocalCoverageProvider(force::localResponseCoverageView);
        }
        Map<String,Object> actor(long id,double x){return map("id",id,"type","tank","x",x,"y",100.,"hp",1000.,"maxHp",1000.,"buildProgress",1.);}
        Map<String,Object> own(long id){for(Map<String,Object> row:own)if(((Number)row.get("id")).longValue()==id)return row;return null;}
        void observe(){frame++;now=frame*1000;state.put("frame",frame);state.put("gameTimeMs",now);enemies.put("frame",frame);enemies.put("gameTimeMs",now);
            for(Map<String,Object> enemy:contacts)enemy.put("lastSeenGameTimeMs",now);List<Long> ids=new ArrayList<Long>();for(Map<String,Object> row:own)ids.add(((Number)row.get("id")).longValue());
            gate.observe(new CommandArbiter.Stamp("s","team:0",frame,now),ids);refresh();}
        void refresh(){for(UnitView unit:registry.units())registry.updateHealthRole(unit.unitId,unit.healthRole);
            for(GeneralView general:registry.generals())if(!general.members.isEmpty()){double x=0,y=0;for(Long id:general.members){x+=((Number)own(id).get("x")).doubleValue();y+=((Number)own(id).get("y")).doubleValue();}
                registry.updateGeneralCentroid(general.id,x/general.members.size(),y/general.members.size());}}
        GeneralId general(){return registry.generals().get(0).id;}
        void enemy(long id,double hp,double x,boolean armed){contacts.add(map("id",id,"type",armed?"turret":"extractorT1","building",true,"canAttack",armed,
            "hp",hp,"x",x,"y",100.,"targetDomain","SURFACE","lastSeenGameTimeMs",now));}
        void run()throws Exception{combat.observe(state,enemies,100,100,now);commander.prepareThreatTasks(state,enemies,now);
            force.prepareLocalResponse(state,enemies,now,commander.currentThreatTasks());refresh();combat.refreshCurrentRoster(state,enemies,100,100,now);
            commander.reconcile(state,enemies,100,100,now,24);combat.setPreferredThreats(commander.preferredThreats());proposals.clear();force.collect(state,enemies,map(),100,100,now);}
        List<ForceController.Proposal> lane(String lane){List<ForceController.Proposal> rows=new ArrayList<ForceController.Proposal>();for(ForceController.Proposal proposal:proposals)if(lane.equals(proposal.lane))rows.add(proposal);return rows;}
        @SuppressWarnings("unchecked") List<Map<String,Object>> responses(){return (List<Map<String,Object>>)force.localResponseCoverageView().get("taskResponses");}
        @SuppressWarnings("unchecked") List<Map<String,Object>> tasks(){return (List<Map<String,Object>>)commander.snapshot().get("tasks");}
        @SuppressWarnings("unchecked") List<Long> actors(int index){return (List<Long>)responses().get(index).get("ownedActorIds");}
        void accept(ForceController.Proposal p)throws Exception{p.accepted(map("status","queued","frame",frame,"gameTimeMs",now));
            String order=p.path.startsWith("/command/move?")?"move":"attackMove";double x=Double.parseDouble(p.path.split("&x=")[1].split("&")[0]),y=Double.parseDouble(p.path.split("&y=")[1].split("&")[0]);
            for(Long actor:p.actors){own(actor).put("orderType",order);own(actor).put("orderX",x);own(actor).put("orderY",y);}}
        public Map<String,Object> read(String path,String event){if(path.startsWith("/scout/observe?")){long tile=Long.parseLong(path.split("tile=")[1].split("&")[0]);
            return map("status","observed","sessionId",foreignProbe?"foreign":"s","frame",frame,"region",map("tile",tile,"sampleGameTimeMs",now,
                "currentVisibleTiles",regionVisible?289:0,"recentFogTiles",0,"staleFogTiles",0,"deepFogTiles",regionVisible?0:289,"neverSeenTiles",0));}
            return map("status","planned","sessionId","s","pathKnown",true,"targetTile",4500,"frontierTile",4500,"targetX",9000.,"targetY",100.);}
        public List<Map<String,Object>> eligible(List<Map<String,Object>> actors,Map<String,Object> target,Map<String,Object> enemies){return blocked?Collections.<Map<String,Object>>emptyList():actors;}
        public void collect(ForceController.Proposal proposal){proposals.add(proposal);}
        void invariants(){Set<Long> accounted=new HashSet<Long>();for(GeneralView g:registry.generals()){
            for(Long actor:g.members)check(accounted.add(actor)&&g.id.equals(registry.unit(actor).generalId),"actual roster unique");
            for(Long actor:g.reservations)check(accounted.add(actor)&&g.id.equals(registry.unit(actor).reservedGeneralId),"temporary origin reservation unique");}
            Set<Long> rented=new HashSet<Long>();for(Map<String,Object> row:responses())for(Long actor:(List<Long>)row.get("ownedActorIds"))check(rented.add(actor),"one explicit task per actor");
            for(UnitView unit:registry.units())check(gate.owns(unit.owner,unit.unitId),"registry matches actual arbiter owner");}
    }
    static void smallBuildingPreservesMission()throws Exception{
        Fixture f=new Fixture(12,3);f.run();for(ForceController.Proposal p:f.proposals)if(f.general().owner().equals(p.owner))f.accept(p);
        f.enemy(501,1000,1800,false);f.observe();f.run();
        check(f.lane("THREAT_DETACHMENT").size()==1&&f.actors(0).equals(Collections.singletonList(100L)),"unarmed remote building gets one minimum compatible nearby FREE actor");
        check(f.commander.preferredThreats().isEmpty()&&f.registry.general(f.general()).members.size()==12,"small building does not redirect or detach complete General");
        check(f.registry.general(f.general()).goalX==9000,"accepted main mission survives current remote contact");
        check(f.proposals.stream().noneMatch(p->f.general().owner().equals(p.owner)&&p.path.contains("x=1800.0")),"remaining General never receives remote small-task target");
        check(((Number)f.tasks().get(0).get("localCovered")).doubleValue()==1&&((Number)f.tasks().get(0).get("deficit")).doubleValue()==0,"actual current compatible cleanup ownership is budgeted once");
        f.invariants();
        Fixture armed=new Fixture(12,0);armed.enemy(501,1000,1800,true);armed.observe();armed.run();
        check(armed.actors(0).size()==2&&armed.commander.preferredThreats().isEmpty(),"small legally compatible armed building also borrows minimum instead of whole General");
        Fixture durable=new Fixture(12,0);durable.enemy(501,100000,1800,false);durable.observe();durable.run();
        check(durable.actors(0).size()==1&&durable.commander.preferredThreats().isEmpty()&&durable.registry.general(durable.general()).members.size()==11,"highHP legally harmless building never fabricates frontline firepower or pulls most roster");
    }
    static void lostContactSearchAndReturn()throws Exception{
        Fixture f=new Fixture(12,1);f.enemy(501,1000,1800,true);f.observe();f.run();List<Long> actors=new ArrayList<Long>(f.actors(0));long borrowed=actors.get(1);
        long generation=f.registry.unit(borrowed).ownerGeneration;Map<Long,Long> token=f.gate.snapshotGenerations(f.registry.unit(borrowed).owner,Collections.singletonList(borrowed));
        f.accept(f.lane("THREAT_DETACHMENT").get(0));f.contacts.clear();f.observe();f.run();
        check(f.responses().size()==1&&f.registry.unit(borrowed).ownerGeneration==generation,"loss of contact retains actual detachment responsibility");
        check("UNKNOWN".equals(f.tasks().get(0).get("status"))&&Boolean.FALSE.equals(f.tasks().get(0).get("attackAuthorized")),"lost task is UNKNOWN, never hidden attack authority");
        check(f.lane("THREAT_DETACHMENT").isEmpty()&&f.lane("THREAT_REACQUIRE").size()==2,"lost contact repairs old attack to MOVE toward last-known region");
        for(ForceController.Proposal p:f.lane("THREAT_REACQUIRE"))f.accept(p);
        f.observe();f.run();check(f.lane("THREAT_REACQUIRE").isEmpty()&&f.responses().size()==1,"explicit current native MOVE is reused and elapsed time cannot close unknown region");
        f.foreignProbe=true;f.regionVisible=true;f.observe();f.run();check(f.responses().size()==1,"foreign visibility cannot release search responsibility");
        f.foreignProbe=false;f.observe();f.run();check(f.responses().size()==1,"fully visible probe alone waits for subsequent complete legal contacts");
        f.regionVisible=false;f.observe();f.run();check(f.responses().size()==1,"region refog before later contact sample invalidates earlier visibility witness");
        f.regionVisible=true;f.observe();f.run();check(f.responses().size()==1,"refogged region requires another fresh visibility and following contacts pair");
        f.observe();f.run();check(f.responses().isEmpty()&&f.registry.unit(borrowed).owner.equals(f.general().owner()),"later current empty searched region restores General owner");
        check(f.registry.unit(100).owner.equals(FREE_OWNER)&&f.registry.unit(borrowed).ownerGeneration==generation+1,"FREE origin and borrowed origin return through existing generation transitions");
        check("STALE_OWNER_GENERATION".equals(f.gate.validateGenerations(Collections.singletonList(borrowed),token)),"old detachment receipt cannot survive return ABA");
        f.observe();f.run();check("CLEAR".equals(f.tasks().get(0).get("status"))&&Boolean.FALSE.equals(f.tasks().get(0).get("killClaim")),"searched clear means responsibility ended without kill inference");f.invariants();
    }
    static void freshReacquisitionAndProgressiveGrowth()throws Exception{
        Fixture f=new Fixture(24,0);f.enemy(501,1000,1800,true);f.observe();f.run();long task=((Number)f.tasks().get(0).get("taskId")).longValue();
        check(f.actors(0).size()==2,"small initial demand borrows only two members");
        f.accept(f.lane("THREAT_DETACHMENT").get(0));
        f.contacts.clear();f.observe();f.run();int members=f.registry.general(f.general()).members.size();
        f.observe();f.run();check(f.registry.general(f.general()).members.size()==members,"UNKNOWN cannot recruit speculative reinforcements");
        f.enemy(502,5000,1810,true);f.observe();f.run();
        check(((Number)f.tasks().get(0).get("taskId")).longValue()==task&&f.actors(0).size()==8,"fresh stronger contact in searched region reuses responsibility and adds only adequate remainder");
        check(f.commander.preferredThreats().isEmpty()&&f.registry.general(f.general()).members.size()==16,"reinforcement exceeding old six-member envelope still preserves actual main roster");
        f.run();check(f.actors(0).size()==8,"same-source prepare cannot duplicate growth or ownership");
        check(!f.lane("THREAT_DETACHMENT").isEmpty()&&f.lane("THREAT_REACQUIRE").isEmpty(),"only reacquired current legal target switches MOVE search back to attack");f.invariants();
        f.contacts.get(0).put("canAttack",false);f.observe();f.run();
        check(f.actors(0).size()==1&&f.registry.general(f.general()).members.size()==23,"new legally harmless role releases surplus through original ownership rather than retaining combat-sized cleanup");f.invariants();
        Fixture scale=new Fixture(12,0);scale.enemy(501,8000,1800,true);scale.observe();scale.run();
        check(scale.responses().isEmpty()&&scale.commander.preferredThreats().size()==1,"current frontline demand matching actual entire roster admits independent whole General");
    }
    static void joiningAndMultipleOwners()throws Exception{
        Fixture f=new Fixture(12,2);f.observe();check(f.registry.requestJoin(100,f.general())&&f.registry.beginJoining(100)&&f.registry.joinAccepted(100,f.frame),"setup real JOINING owner and receipt after FREE admission frame");
        f.enemy(501,1000,1800,true);f.observe();f.run();check(f.actors(0).equals(Arrays.asList(101L,100L)),"FREE precedes nearest JOINING borrower");
        f.contacts.clear();f.regionVisible=true;f.observe();f.run();f.observe();f.run();
        check(f.registry.unit(100).owner.equals(f.general().joinOwner())&&f.registry.unit(100).membership==Membership.JOINING&&f.registry.unit(100).joinAcceptedFrame==-1,"region release restores JOINING and requires new receipt");
        check(!f.registry.observeJoinPosition(100,f.gate.stamp(),1200,100,180),"old joining receipt never becomes new arrival");
        Fixture multiple=new Fixture(12,4);multiple.enemy(501,50,1800,false);multiple.enemy(502,50,2600,false);multiple.observe();multiple.run();
        check(multiple.responses().size()==2&&!multiple.responses().get(0).get("owner").equals(multiple.responses().get(1).get("owner")),"independent remote regions retain unique actual owners");multiple.invariants();
        for(Long actor:new ArrayList<Long>(multiple.actors(0)))multiple.registry.enterRecovery(actor);
        multiple.contacts.removeIf(enemy->((Number)enemy.get("id")).longValue()==501);multiple.observe();multiple.run();
        check(multiple.responses().size()==1,"actors leaving for recovery remove emptied controller without reclaim or orphan");multiple.invariants();
    }
    static void missingOwnReturnIsUnknown()throws Exception{
        Fixture f=new Fixture(12,1);f.enemy(501,1000,1800,true);f.observe();f.run();long borrowed=f.actors(0).get(1);
        Map<String,Object> missing=f.own(borrowed);String owner=f.registry.unit(borrowed).owner;long generation=f.registry.unit(borrowed).ownerGeneration;
        f.contacts.clear();f.regionVisible=true;f.observe();f.run();f.own.remove(missing);f.observe();f.run();
        check(f.responses().size()==1&&f.registry.unit(borrowed).owner.equals(owner)&&f.registry.unit(borrowed).ownerGeneration==generation,"unavailable own actor keeps explicit return responsibility instead of orphaning its untransferable lease");
        check(f.registry.unit(100).owner.equals(FREE_OWNER),"currently observed FREE actor returns independently of unavailable teammate");
        f.own.add(missing);f.observe();f.run();check(f.responses().isEmpty()&&f.registry.unit(borrowed).owner.equals(f.general().owner()),"later legal own row completes deferred origin return without death inference");f.invariants();
    }
    static void missingOwnCommitmentCannotReinforce()throws Exception{
        for(boolean armed:new boolean[]{false,true}){
            Fixture f=new Fixture(12,1);f.enemy(501,1000,1800,armed);f.observe();f.run();
            List<Long> committed=new ArrayList<Long>(f.actors(0));long actor=committed.get(committed.size()-1);
            check(committed.size()==(armed?2:1),"setup actual adequate armed or unarmed response");
            Map<String,Object> missing=f.own(actor);UnitView leased=f.registry.unit(actor);String owner=leased.owner;
            long generation=leased.ownerGeneration;GeneralId reservation=leased.reservedGeneralId;int members=f.registry.general(f.general()).members.size();
            f.own.remove(missing);
            for(int sample=0;sample<2;sample++){f.observe();f.run();
                check(f.responses().size()==1&&f.responses().get(0).get("ownedActorIds").equals(committed),"new current unchanged enemy packet cannot replace unknown existing commitment with another borrower");
                check(f.registry.general(f.general()).members.size()==members,"missing own row never creates a false capacity deficit that borrows another General member");
                check(f.registry.unit(actor).owner.equals(owner)&&f.registry.unit(actor).ownerGeneration==generation&&Objects.equals(f.registry.unit(actor).reservedGeneralId,reservation),"missing own actor retains its exact lease generation and original reservation");
                check(f.lane("THREAT_DETACHMENT").stream().noneMatch(p->p.actors.contains(actor)),"missing actor receives no attack proposal until lawful own evidence returns");
            }
            f.own.add(missing);f.observe();f.run();
            check(f.actors(0).equals(committed)&&f.registry.general(f.general()).members.size()==members,"current own row restores full committed coverage without extra borrow");
            check(f.lane("THREAT_DETACHMENT").size()==1&&f.lane("THREAT_DETACHMENT").get(0).actors.equals(committed),"restored current own actors resume the same legal response owner");f.invariants();
            f.contacts.clear();f.regionVisible=true;f.observe();f.run();f.observe();f.run();
            check(f.responses().isEmpty()&&f.registry.unit(actor).owner.equals(armed?f.general().owner():FREE_OWNER),"recovered own evidence still permits normal lawful regional return");f.invariants();
        }
    }
    public static void main(String[] args)throws Exception{smallBuildingPreservesMission();lostContactSearchAndReturn();freshReacquisitionAndProgressiveGrowth();joiningAndMultipleOwners();missingOwnReturnIsUnknown();missingOwnCommitmentCannotReinforce();System.out.println("R13ThreatDetachmentHarness PASS checks="+checks);}
}
