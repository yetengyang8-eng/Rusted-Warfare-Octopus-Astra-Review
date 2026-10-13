"""G5 actual BattleClient HTTP main-loop/read contracts, explicitly SYNTHETIC E2.

Own births, position effects, pressure HP and terminal screens below are fixture
inputs. This is not a natural match, native motion/damage, victory or desktop
acceptance. Existing G41 fixtures are imported unchanged for production/join.
"""
import http.server
import hashlib
import importlib.util
import json
import os
import pathlib
import subprocess
import sys
import tempfile
import threading
import unittest
from unittest import mock
from urllib.parse import parse_qs, urlsplit

JAR = str(pathlib.Path(sys.argv.pop(1)).resolve())
JAR_SHA256 = hashlib.sha256(pathlib.Path(JAR).read_bytes()).hexdigest()
TESTS = pathlib.Path(__file__).resolve().parent
with mock.patch.object(sys, 'argv', ['test_g41_runtime.py', JAR]):
    spec = importlib.util.spec_from_file_location('g5_g41_packet_fixture', TESTS / 'test_g41_runtime.py')
    g41 = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(g41)
JAVA = g41.JAVA
JAVAC = os.environ.get('RW_JAVAC') or str(pathlib.Path(JAVA).with_name('javac.exe' if os.name == 'nt' else 'javac'))


def events(rows, kind):
    return [row['data'] for row in rows if row['event'] == kind]


class G5RuntimeTests(unittest.TestCase):
    maxDiff = 16000

    def save(self, label, raw, details, process=b''):
        details.update(candidateJar=JAR, candidateJarSha256=JAR_SHA256)
        root = os.environ.get('RW_G5_RUNTIME_EVIDENCE_DIR')
        if root:
            dest = pathlib.Path(root) / label
            dest.mkdir(parents=True, exist_ok=True)
            (dest / 'battle.jsonl').write_bytes(raw)
            (dest / 'fixture.json').write_text(json.dumps(details, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
            (dest / 'process.txt').write_bytes(process)

    def test_g5_enabled_production_free_join_later_arrival_activates_first_general(self):
        original_run = subprocess.run
        def enable(command, *args, **kwargs):
            command = list(command)
            command[command.index('-cp'):command.index('-cp')] = ['-Drwagent.g5=true', '-Drwagent.runtimeAdaptive=true']
            return original_run(command, *args, **kwargs)
        evidence = os.environ.get('RW_G5_RUNTIME_EVIDENCE_DIR')
        env = {'RW_G41_RUNTIME_EVIDENCE_DIR': str(pathlib.Path(evidence) / 'g41-reused')} if evidence else {}
        with mock.patch.object(subprocess, 'run', enable), mock.patch.dict(os.environ, env):
            rows, details = g41.G41RuntimeTests().run_case('g5_first_formation', ticks=36)
        config = events(rows, 'battle_config')[0]
        self.assertTrue(config['g5'])
        self.assertTrue(config['runtimeAdaptive'])
        transitions = events(rows, 'g4_force_transition')
        birth = next(t for t in transitions if t['reason'] == 'GENERAL_FORMATION_CREATED')
        self.assertEqual(birth['after']['desiredStrength'], 24)
        self.assertEqual(birth['after']['members'], [])
        self.assertEqual(birth['after']['phase'], 'FORMING')
        arrivals = [t for t in transitions if t['reason'] == 'JOIN_ARRIVAL_OWN_POSITION_WITNESSED']
        self.assertGreaterEqual(len(arrivals), 6)
        for t in arrivals:
            self.assertEqual(t['before']['membership'], 'JOINING')
            self.assertEqual(t['after']['membership'], 'ATTACHED')
            self.assertGreater(t['sourceFrame'], t['before']['joinAcceptedFrame'])
            self.assertGreater(t['after']['ownerGeneration'], t['before']['ownerGeneration'])
        active = next(t for t in transitions if t['reason'] == 'GENERAL_FORMATION_ACTIVE')
        self.assertEqual(active['after']['healthyAttachedStrength'], 6)
        frontier = [r for r in details['reads'] if r['path'] == '/scout/plan' and r['query'].get('role') == ['army']]
        self.assertTrue(frontier, 'G5 ACTIVE General still consumes lawful native frontier plan')
        self.assertTrue(all(r['frame'] >= active['sourceFrame'] for r in frontier))
        self.assertTrue(events(rows, 'g5_runtime_state'))
        self.assertTrue([o for o in details['orders'] if o['path'] == '/command/queue' and o['accepted']])
        production = [x for x in events(rows, 'g3_execution') if x['nativeAccepted'] and x['kind'] == '/command/queue']
        self.assertTrue(all(x['costSourceRequestPath'] == '/combat/production' and x['costSourceObservationId'] for x in production))

    def run_main(self, scenario):
        frame = 0
        reads, orders, effects = [], [], []
        positions, motions = {}, []
        def now():
            return frame * 1000
        def unit(uid, kind, x=100, y=100):
            building = kind in ('commandCenter', 'landFactory')
            return dict(id=uid, type=kind, x=x, y=y, hp=1000, maxHp=1000, dead=False,
                buildProgress=1, mobile=not building, canAttack=kind == 'heavyTank', building=building,
                techLevel=1, productionQueue=0 if kind == 'landFactory' else -1, orderType=None)
        def own():
            actors = [unit(1, 'commandCenter'), unit(2, 'builder', 120, 100)]
            actors += [unit(20 + i, 'heavyTank', 1000 + i * 3, 1000) for i in range(24)]
            if scenario == 'pressure':
                actors += [unit(100 + i, 'heavyTank', 5000 + i * 3, 1000) for i in range(24)]
            elif frame >= 3:
                actors += [unit(200 + i, 'heavyTank', 120 + i * 3, 100) for i in range(6)]
            for actor in actors:
                actor.update(positions.get(actor['id'], {}))
                # Explicit synthetic own-harm witness, separate from enemy HP pressure.
                # G5.1R must not flee merely because a large visible HP proxy appeared.
                if scenario == 'pressure' and frame >= 4 and 20 <= actor['id'] < 44:
                    actor['hp'] = 800
            return actors
        def contacts():
            if scenario != 'pressure':
                return []
            def enemy(uid, x, hp):
                # This legacy crisis fixture deliberately uses an unknown high-HP
                # attacker after frame 3. A tank with invented HP100000 is no
                # longer a valid native-profile firepower oracle for FightEstimate.
                kind = 'unknownHighHpPressure' if uid == 700 and frame >= 3 else 'tank'
                return dict(id=uid, type=kind, x=x, y=1000, hp=hp, maxHp=hp, dead=False,
                    building=False, canAttack=True, targetDomain='SURFACE', touchingWater=False,
                    lastSeenGameTimeMs=now(), domainObservedAtGameTimeMs=now())
            return [enemy(700, 1100, 100000 if frame >= 3 else 1000), enemy(800, 5100, 1000)]

        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass
            def reply(self, body, status=200):
                payload = json.dumps(body).encode()
                self.send_response(status)
                self.send_header('Content-Length', str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)
            def do_GET(self):
                nonlocal frame
                parsed = urlsplit(self.path)
                query = parse_qs(parsed.query)
                reads.append(dict(path=parsed.path, query=query, frame=frame, gameTimeMs=now()))
                if parsed.path == '/health':
                    return self.reply(dict(status='ok', version='0.07-alpha1', strategyContractVersion=1))
                if parsed.path == '/state':
                    frame += 1
                    for motion in list(motions):
                        if frame >= motion['applyFrame']:
                            for uid in motion['unitIds']:
                                positions[uid] = dict(x=motion['x'], y=motion['y'], orderType=None)
                            effects.append(dict(kind='SYNTHETIC_LATER_POSITION_AFTER_ACTUAL_RECEIPT', frame=frame, **motion))
                            motions.remove(motion)
                    terminal = frame >= 32
                    return self.reply(dict(status='running', sessionId='s', frame=frame, gameTimeMs=now(),
                        networked=False, replay=False, player=dict(teamId=0, credits=0),
                        map=dict(width=10000, height=10000, tilesWide=500, tilesHigh=500, tileWidth=20, tileHeight=20),
                        match=dict(outcome='DEFEAT' if terminal else 'ONGOING', nativeDefeat=terminal,
                                   nativeVictory=False, source='SYNTHETIC_NATIVE_RESULT_SCREEN_PACKET'), ownUnits=own()))
                if parsed.path == '/combat/observe':
                    return self.reply(dict(status='observed', sessionId='s', frame=frame, gameTimeMs=now(),
                        catalogSha256=g41.CATALOG_SHA, catalogGameJarMatched=True,
                        visibleEnemies=contacts(), rememberedEnemies=contacts(), enemyIntel=[], rememberedBuildings=[]))
                if parsed.path == '/combat/engagement':
                    ids = [int(x) for x in query['unitIds'][0].split(',')]
                    if len(ids) > 48:
                        return self.reply(dict(status='rejected', reason='FIXTURE_NATIVE_MAX_48'), 400)
                    target = next(e for e in contacts() if e['id'] == int(query['targetId'][0]))
                    return self.reply(dict(status='observed', sessionId='s', frame=frame, gameTimeMs=now(),
                        targetId=target['id'], targetVisible=True, targetX=target['x'], targetY=target['y'], targetObservedAtGameTimeMs=now(),
                        actors=[dict(unitId=uid, compatibility='COMPATIBLE', status='APPROACH_PATH_KNOWN',
                          lastKnownPositionApproachStatus='APPROACH_PATH_KNOWN', approachX=target['x'], approachY=target['y']) for uid in ids]))
                if parsed.path == '/combat/production':
                    return self.reply(dict(status='observed', sessionId='s', frame=frame, gameTimeMs=now(), factories=[]))
                if parsed.path == '/scout/observe':
                    return self.reply(dict(status='observed', sessionId='s', frame=frame, gameTimeMs=now(),
                        newlyObservedTiles=0, resources=[], visibleThreats=[], rememberedThreats=[]))
                if parsed.path in ('/economy/investments', '/combat/capabilities'):
                    return self.reply(dict(status='observed', sessionId='s', frame=frame, gameTimeMs=now(), units=[], capabilities=[]))
                return self.reply(dict(status='no_frontier', sessionId='s', frame=frame, gameTimeMs=now()))
            def do_POST(self):
                parsed = urlsplit(self.path)
                query = parse_qs(parsed.query)
                ids = [int(x) for x in query.get('unitIds', query.get('unitId', ['0']))[0].split(',')]
                record = dict(path=parsed.path, unitIds=ids, frame=frame, gameTimeMs=now(),
                    x=float(query.get('x', ['0'])[0]), y=float(query.get('y', ['0'])[0]), requestId=query['requestId'][0])
                orders.append(record)
                if len(ids) > 48:
                    record['rejected'] = True
                    return self.reply(dict(status='rejected', sessionId='s', reason='FIXTURE_NATIVE_MAX_48'), 400)
                if parsed.path not in ('/command/move', '/command/attack-move'):
                    record['rejected'] = True
                    return self.reply(dict(status='rejected', sessionId='s', reason='FIXTURE_NO_PRODUCTION_OR_CONSTRUCTION'), 409)
                for uid in ids:
                    positions.setdefault(uid, {}).update(orderType='move' if parsed.path == '/command/move' else 'attackMove',
                                                         orderX=record['x'], orderY=record['y'])
                if parsed.path == '/command/move':
                    motions.append(dict(applyFrame=frame + 2, unitIds=ids, x=record['x'], y=record['y'], receiptFrame=frame))
                return self.reply(dict(status='queued', sessionId='s', frame=frame, requestId=query['requestId'][0],
                    unitId=ids[0], unitIds=ids, targetX=record['x'], targetY=record['y'], orderType='move' if parsed.path == '/command/move' else 'attackMove'))

        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        server.daemon_threads = True
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with tempfile.TemporaryDirectory(prefix='rw-g5-main-') as cwd:
                command = [JAVA, '-Dfile.encoding=UTF-8', '-Drwagent.g5=true', '-Drwagent.runtimeAdaptive=true',
                    '-Drwagent.g3Execution=true', '-Drwagent.g4Forces=true', '-Drwagent.earlyOperations=true',
                    '-Drwagent.globalStrategy=false', '-Drwagent.reconEnabled=false', '-Drwagent.reachabilitySample=false',
                    '-Drwagent.g1Trace=true', '-Drwagent.port=' + str(server.server_port), '-cp', JAR, 'io.rwagent.client.BattleClient', '180']
                result = subprocess.run(command, cwd=cwd, capture_output=True, timeout=30)
                reports = list(pathlib.Path(cwd).glob('rw-agent-reports/battle-*.jsonl'))
                raw = reports[0].read_bytes() if reports else b''
                rows = [json.loads(line) for line in raw.splitlines()]
                details = dict(evidence='E2_G5_HTTP_SYNTHETIC_MAIN_LOOP_NO_NATIVE_MATCH', scenario=scenario,
                               reads=reads, orders=orders, effects=effects, command=command, exitCode=result.returncode)
                if scenario == 'pressure':
                    details['ownHarmFixture'] = dict(firstFrame=4, actorIds=list(range(20, 44)), hpBefore=1000, hpAfter=800,
                                                   semantics='EXPLICIT_SYNTHETIC_DELTA_NOT_NATURAL_DAMAGE_OR_KILL')
                self.save(scenario, raw, details, result.stdout + result.stderr)
                self.assertEqual(result.returncode, 0, (result.stdout + result.stderr).decode(errors='replace'))
                self.assertTrue(rows)
                self.assertFalse([o for o in orders if o.get('rejected')])
                return rows, details
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)

    def test_full_24_plus_six_current_free_creates_independent_desired_24_general(self):
        rows, details = self.run_main('overflow')
        transitions = events(rows, 'g4_force_transition')
        birth = next(t for t in transitions if t['reason'] == 'GENERAL_ADDITIONAL_FORMATION_CREATED')
        self.assertEqual(birth['after']['desiredStrength'], 24)
        self.assertEqual(birth['after']['phase'], 'FORMING')
        self.assertEqual(birth['after']['members'], [])
        born = birth['after']['generalId']
        states = events(rows, 'g5_runtime_state')
        admissions = [t for t in transitions[:transitions.index(birth)] if t['reason'] == 'ORDINARY_ADMITTED_FREE'
                      and t.get('unitId') in range(200, 206)]
        self.assertEqual({t['unitId'] for t in admissions}, set(range(200, 206)))
        self.assertTrue(all(t['sourceFrame'] <= birth['sourceFrame'] for t in admissions))
        self.assertTrue(any(s['generalCount'] == 2 and s['freeCount'] >= 6 for s in states),
                        'birth leaves six current FREE actors; later frame performs formal admission')
        self.assertTrue(any(s['generalCount'] == 2 for s in states))
        generals = [g for s in states for g in s['generals'] if g['generalId'] == born]
        self.assertTrue(any(g['phase'] == 'ACTIVE' and len(g['members']) == 6 for g in generals))
        self.assertTrue(all(g['desiredStrength'] == 24 for g in generals))
        self.assertFalse(any(s['generalCount'] > 2 for s in states), 'six FREE must not fragment into extra tiny Generals')
        self.assertTrue([o for o in details['orders'] if o['path'] == '/command/move' and set(o['unitIds']) <= set(range(200, 206))])

    def test_visible_pressure_with_own_damage_retreat_a_leaves_b_independent_and_reports_runtime(self):
        rows, details = self.run_main('pressure')
        states = events(rows, 'g5_runtime_state')
        before_damage = [s for s in states if s['sourceFrame'] < 4]
        self.assertTrue(before_damage)
        self.assertTrue(all(not g.get('retreating') for s in before_damage for g in s['generals']),
                        'visible high HP without own harm cannot trigger whole-General escape')
        retreated = [s for s in states if any(g['generalId'] == 1 and g.get('retreating') for g in s['generals'])]
        self.assertTrue(retreated)
        self.assertTrue(all(not next(g for g in s['generals'] if g['generalId'] == 2).get('retreating') for s in retreated))
        executions = [e for e in events(rows, 'g3_execution') if e['nativeAccepted']]
        a = [e for e in executions if e['owner'] == 'general:1' and e['kind'] == '/command/move']
        b = [e for e in executions if e['owner'] == 'general:2' and e['kind'] == '/command/attack-move']
        self.assertTrue(a)
        self.assertTrue(b)
        self.assertTrue(all(set(e['actorIds']) <= set(range(20, 44)) for e in a))
        self.assertTrue(all(set(e['actorIds']) <= set(range(100, 124)) for e in b))
        # An explicit still-valid native attackMove is retained; independence no longer
        # means issuing another command just because A entered crisis. Verify the later
        # input witness against the actual accepted B goal, not fixture positions alone.
        later_own = [s for s in events(rows, 'observation') if s['frame'] >= 4]
        self.assertTrue(later_own)
        for accepted in b:
            goal = accepted['receipt']
            self.assertTrue(any(
                s['frame'] > goal['frame'] and all(
                    any(u['id'] == uid and u.get('orderType') == 'attackMove'
                        and abs(u.get('orderX', float('inf')) - goal['targetX']) < 1
                        and abs(u.get('orderY', float('inf')) - goal['targetY']) < 1
                        for u in s['ownUnits'])
                    for uid in accepted['actorIds']) for s in later_own),
                'B retains its actual accepted order in a later own observation during A crisis')
        self.assertFalse([e for e in executions if e['owner'] == 'general:2'
                          and e['kind'] == '/command/move' and e['receipt']['frame'] >= 4],
                         'A crisis cannot redirect independent B into retreat')
        runtime = states[-1]['runtime']
        for field in ('effectiveDecisionIntervalGameMs', 'observationLatencyWallMs', 'dispatchLatencyWallMs',
                      'commandsPerGameMinute', 'maxCommandsPerObservation', 'cacheHits', 'parallelFetches'):
            self.assertIn(field, runtime)
        self.assertGreater(runtime['parallelFetches'], 0)
        self.assertGreaterEqual(runtime['cacheHits'], 0)  # A repeat-read scenario below proves an actual hit.
        self.assertFalse(runtime['atomicSnapshot'])
        self.assertTrue(details['effects'], 'retreat effect remains explicitly synthetic and later than receipt')
        observed = [t for t in events(rows, 'g5_general_transition') if t['generalId'] == 1 and t['reason'] == 'GENERAL_COMBAT_OBSERVED']
        phases = [t['after']['crisis'] for t in observed]
        for phase in ('OVERMATCHED', 'RETREATING', 'REGROUPING', 'NORMAL'):
            self.assertIn(phase, phases)
        recovery = next(t for t in observed if t['after']['trigger'] == 'RECOVERY_CURRENT_OWN_RALLY_WITNESSED')
        regroup = next(t for t in observed if t['after']['crisis'] == 'REGROUPING')
        self.assertGreaterEqual(recovery['gameTimeMs'] - regroup['gameTimeMs'], 4000)

    def test_real_bc_70_actor_engagement_chunks_and_cached_source_identity(self):
        """Actual BC private read/compatibility paths, not a mocked chunk implementation."""
        requests = []
        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass
            def do_GET(self):
                parsed = urlsplit(self.path)
                query = parse_qs(parsed.query)
                ids = [int(x) for x in query.get('unitIds', [''])[0].split(',') if x]
                requests.append(dict(path=parsed.path, unitIds=ids))
                body = dict(status='observed', sessionId='s', frame=17, gameTimeMs=1000)
                code = 200
                if parsed.path == '/combat/production':
                    body['factories'] = []
                elif parsed.path == '/combat/engagement':
                    if len(ids) > 48:
                        code = 400
                        body.update(status='rejected', reason='FIXTURE_NATIVE_MAX_48')
                    else:
                        body.update(targetId=700, targetVisible=True, targetX=500, targetY=500,
                            targetObservedAtGameTimeMs=1000,
                            actors=[dict(unitId=uid, compatibility='COMPATIBLE', status='APPROACH_PATH_KNOWN') for uid in ids])
                raw = json.dumps(body).encode()
                self.send_response(code)
                self.send_header('Content-Length', str(len(raw)))
                self.end_headers()
                self.wfile.write(raw)
        source = r'''
package io.rwagent.client;
import java.io.*;import java.lang.reflect.*;import java.util.*;
public final class G5HttpReadProbe {
  static Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
  static void set(Object owner,String name,Object value)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
  static Object invoke(Object owner,String name,Class<?>[] types,Object...args)throws Exception{Method m=owner.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(owner,args);}
  static Map<String,Object> map(Object...items){Map<String,Object> out=new LinkedHashMap<String,Object>();for(int i=0;i<items.length;i+=2)out.put((String)items[i],items[i+1]);return out;}
  public static void main(String[] args)throws Exception{
    BattleClient client=new BattleClient();set(client,"log",new BufferedWriter(new StringWriter()));set(client,"session","s");set(client,"time",1000L);
    try {
      Object first=invoke(client,"optionalGet",new Class[]{String.class,String.class},"/combat/production","probe_production");
      GameClock.Observation original=(GameClock.Observation)field(client,"worldReadObservation");
      set(client,"time",9000L);
      Object cached=invoke(client,"optionalGet",new Class[]{String.class,String.class},"/combat/production","probe_production_cached");
      GameClock.Observation repeated=(GameClock.Observation)field(client,"worldReadObservation");
      Map<String,Object> target=map("id",700L,"type","tank","x",500,"y",500,"hp",1000,"dead",false,"targetDomain","SURFACE","touchingWater",false,"lastSeenGameTimeMs",1000L,"domainObservedAtGameTimeMs",1000L);
      Map<String,Object> enemies=map("sessionId","s","gameTimeMs",1000L,"visibleEnemies",Collections.singletonList(target),"catalogGameJarMatched",true,"catalogSha256","263cdaaa3e923e8307c37f059e50bee529b8ecd7c3e215937ce2adf615a0e236");
      List<Map<String,Object>> actors=new ArrayList<Map<String,Object>>();for(long id=1;id<=70;id++)actors.add(map("id",id,"type","heavyTank","hp",1000,"dead",false,"x",100,"y",100));
      List<?> eligible=(List<?>)invoke(client,"crisisCompatible",new Class[]{List.class,List.class,Map.class},actors,Collections.singletonList(target),enemies);
      RuntimeAcceleration runtime=(RuntimeAcceleration)field(client,"runtime");
      System.out.println(BattleClient.json(map("evidence","E2_SYNTHETIC_REAL_BC_READ_PATH_REFLECTION_HTTP_NOT_NATIVE_MATCH","eligible",eligible.size(),"payloadIdentityPreserved",first==cached,"observationIdentityPreserved",original==repeated,"originalObservation",original.metadata(),"cachedObservation",repeated.metadata(),"runtime",runtime.metrics(0,1000))));
    } finally {((AutoCloseable)field(client,"runtime")).close();}
  }
}
'''
        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with tempfile.TemporaryDirectory(prefix='rw-g5-read-') as cwd:
                source_path = pathlib.Path(cwd) / 'G5HttpReadProbe.java'
                source_path.write_text(source, encoding='utf-8')
                compile_result = subprocess.run([JAVAC, '--release', '13', '-encoding', 'UTF-8', '-cp', JAR, '-d', cwd, str(source_path)], capture_output=True, timeout=25)
                self.assertEqual(compile_result.returncode, 0, compile_result.stderr.decode(errors='replace'))
                command = [JAVA, '-Dfile.encoding=UTF-8', '-Drwagent.g5=true', '-Drwagent.runtimeAdaptive=true',
                    '-Drwagent.globalStrategy=false', '-Drwagent.port=' + str(server.server_port), '-cp', os.pathsep.join((cwd, JAR)), 'io.rwagent.client.G5HttpReadProbe']
                result = subprocess.run(command, cwd=cwd, capture_output=True, timeout=15)
                self.assertEqual(result.returncode, 0, (result.stdout + result.stderr).decode(errors='replace'))
                details = json.loads(result.stdout.decode().splitlines()[-1])
                details['requests'] = requests
                details['command'] = command
                self.save('chunk_cache_read_probe', b'', details, result.stdout + result.stderr)
                self.assertEqual(details['eligible'], 70)
                engagement = [r for r in requests if r['path'] == '/combat/engagement']
                self.assertEqual([len(r['unitIds']) for r in engagement], [48, 22])
                self.assertEqual([uid for r in engagement for uid in r['unitIds']], list(range(1, 71)))
                self.assertEqual(sum(r['path'] == '/combat/production' for r in requests), 1)
                self.assertTrue(details['payloadIdentityPreserved'])
                self.assertTrue(details['observationIdentityPreserved'])
                self.assertEqual(details['originalObservation'], details['cachedObservation'])
                self.assertEqual(details['cachedObservation']['sourceGameTimeMs'], 1000)
                self.assertEqual(details['cachedObservation']['sourceFrame'], 17)
                self.assertEqual(details['cachedObservation']['sourceSessionId'], 's')
                self.assertEqual(details['runtime']['cacheHits'], 1)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)


if __name__ == '__main__':
    unittest.main(verbosity=2)
