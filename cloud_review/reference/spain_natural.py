from pathlib import Path
import subprocess,shutil,time,json,socket,urllib.request,sys,hashlib

root=Path(__file__).parent
source=Path(r'G:\deepseek 工作台\_validation\g42-natural-direct-fix1-20261003\work')
map_source=Path(r'G:\deepseek 工作台\游戏环境\P1F-GPTSol61-Operations-2026-10-01\mods\maps\[10p] 10p 西班牙混战_by_MP97.tmx')
java=Path(r'G:\deepseek 工作台\游戏环境\P1F-GPTSol61-ProductionCapacity-2026-10-02\jvm64\bin\java.exe')
label,jar,duration=sys.argv[1],Path(sys.argv[2]),int(sys.argv[3])
difficulty=int(sys.argv[4]) if len(sys.argv)>4 else 0
player=5
folder=root/label;work=folder/'work';work.mkdir(parents=True,exist_ok=False)
for name in ('assets','libs','res'):shutil.copytree(source/name,work/name)
for path in (source/'game-lib.jar',jar):shutil.copy2(path,work/path.name)
map_name='maps/skirmish/[10p] Spain MP97.tmx'
shutil.copy2(map_source,work/'assets'/map_name)
# Preserve unchanged map bytes and its relative original tileset references.
shutil.copytree(work/'assets/tilesets',work/'assets/assets/tilesets')
wall_limit=max(180,min(3500,duration//3+180))
inputs=dict(jarSha256=hashlib.sha256(jar.read_bytes()).hexdigest(),mapSource=str(map_source),
    mapSha256=hashlib.sha256(map_source.read_bytes()).hexdigest(),loadedMap=map_name,speed=5,difficulty=difficulty,playerId=player,
    durationGameSeconds=duration,wallSafetySeconds=wall_limit,nativePositionMutation=False,fixture=False,desktopTouched=False)
(folder/'input.json').write_text(json.dumps(inputs,ensure_ascii=False,indent=2),encoding='utf-8')
with socket.socket() as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
def get(path):
    with urllib.request.urlopen(f'http://127.0.0.1:{port}{path}',timeout=10) as response:return json.load(response)
logs=[(folder/name).open('wb') for name in ('engine.log','engine.err.log','client.log','client.err.log')]
engine=subprocess.Popen([str(java),'-Djava.awt.headless=true','-Dfile.encoding=UTF-8','-cp','rw-agent-bootstrap.jar;game-lib.jar;libs/*',
    'io.rwagent.headless.HeadlessRunner','--map',map_name,'--port',str(port),'--speed','5','--max-wall-seconds',str(wall_limit+20),'--frames','0','--difficulty',str(difficulty),'--player',str(player)],
    cwd=work,stdout=logs[0],stderr=logs[1],creationflags=0x08000000)
client=None;started=time.monotonic();result={}
try:
    for _ in range(300):
        if engine.poll() is not None:raise RuntimeError('isolated Spain engine ended before ready')
        try:
            state=get('/state')
            if state.get('status')=='running':break
        except Exception:pass
        time.sleep(.1)
    else:raise RuntimeError('no Spain bridge')
    (folder/'initial-state.json').write_text(json.dumps(state),encoding='utf-8')
    assert state['player']['teamId']==player, state.get('player')
    boot=json.loads((work/'native-bootstrap.json').read_text(encoding='utf-8'))
    assert any(c['x']==7370 and c['y']==1950 for c in boot['ownCommandCenters']), boot
    print(label,'SPAIN_READY',state.get('player'),flush=True)
    client=subprocess.Popen([str(java),'-Dfile.encoding=UTF-8','-Dsun.stdout.encoding=UTF-8','-Dsun.stderr.encoding=UTF-8',
        '-Drwagent.g6=true','-Drwagent.reportMode=compact','-Drwagent.reportGzip=true','-Drwagent.battleSafetyGameSeconds=14400',
        f'-Drwagent.battleSafetyWallSeconds={wall_limit}',f'-Drwagent.port={port}','-cp','rw-agent-bootstrap.jar','io.rwagent.client.BattleClient',str(duration)],
        cwd=work,stdout=logs[2],stderr=logs[3],creationflags=0x08000000)
    with (folder/'monitor.jsonl').open('w',encoding='utf-8') as monitor:
        while client.poll() is None:
            if time.monotonic()-started>wall_limit+10:raise TimeoutError('isolated observation wall budget exceeded')
            try:
                state=get('/state');own=state.get('ownUnits',[]);types={}
                for unit in own:
                    if not unit.get('dead') and unit.get('hp',0)>0:
                        key=unit.get('type');types[key]=types.get(key,0)+1
                observed=[{k:u.get(k) for k in ("id","type","x","y","hp","maxHp","buildProgress","orderType","orderX","orderY","orderTargetId")} for u in own if u.get("type") in ("builder","scout","amphibiousJet")]
                record=dict(observedUnits=observed,frame=state.get("frame"),wallSeconds=time.monotonic()-started,gameTimeMs=state.get('gameTimeMs'),player=state.get('player'),types=types)
                monitor.write(json.dumps(record,ensure_ascii=False)+'\n');monitor.flush()
                print(label,state.get('gameTimeMs'),types,flush=True)
            except Exception as error:print('MONITOR_READ_UNKNOWN',type(error).__name__,flush=True)
            time.sleep(1)
    result=dict(clientExit=client.returncode,engineExit=engine.poll(),jarSha256=inputs['jarSha256'],noDesktopTouched=True)
    (folder/'final-state.json').write_text(json.dumps(get('/state')),encoding='utf-8')
except Exception as error:
    result.update(errorType=type(error).__name__,error=str(error));raise
finally:
    (folder/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    if client and client.poll() is None:client.terminate();client.wait(timeout=10)
    (work/'stop.request').write_text('root-owned isolated Spain run stop')
    try:engine.wait(timeout=10)
    except subprocess.TimeoutExpired:engine.terminate();engine.wait(timeout=10)
    for log in logs:log.close()
