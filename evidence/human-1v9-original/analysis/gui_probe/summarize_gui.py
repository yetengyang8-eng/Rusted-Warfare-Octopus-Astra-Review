import csv, collections, math, pathlib
base=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\steam_exact_gui_clone\replay_probe_output')
out=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe\spain_gui_summary.txt')
players={}
for p in csv.DictReader((base/'players_30s.csv').open(encoding='utf-8',newline='')):
    try:t=int(p['gameTimeMs']);slot=int(p['slot'])
    except:continue
    players.setdefault(t,{})[slot]=p
units=collections.defaultdict(lambda:collections.defaultdict(list))
ct=0
for u in csv.DictReader((base/'units_30s.csv').open(encoding='utf-8',newline='')):
    try:t=int(u['gameTimeMs']);slot=int(u['slot'])
    except:continue
    ct+=1
    if round(t/60000) in list(range(0,191,10))+[185,186,187]:
        units[t][slot].append(u)
ts=sorted(players)
def near(minutes):return min(ts,key=lambda t:abs(t-minutes*60000))
def value(u,key):return sum(x['type']==key for x in u)
def fmtp(p):return 'credits='+p['credits']+' income='+p['income']+' defeated='+p['defeated']
with out.open('w',encoding='utf-8') as f:
    def w(*a):print(*a,file=f)
    w('GUI faithful replay sample summary');w('source',base);w('player sample times',len(ts),'last game minute',max(ts)/60000,'units rows read',ct)
    w('FIRST LAST',ts[0],ts[-1])
    for m in range(0,191,10):
        t=near(m)
        ps=players.get(t,{})
        loc=units.get(t,{})
        ah=loc.get(5,[])
        ais=sum(len(u) for s,u in loc.items() if 0<=s<=9 and s!=5)
        w(f'GAME {m}m actual={t/60000:.3f}, frame={ps.get(5,{}).get("frame",None)}, Ares units={len(ah)} AI units={ais}')
        if 5 in ps:w(' Ares',fmtp(ps[5]))
        w(' Ares types',collections.Counter(u['type'] for u in ah).most_common(15))
        other={s:(len(loc.get(s,[])), int(ps.get(s,{}).get('income') or 0)) for s in range(10) if s!=5}
        w(' Others',other)
    w('--- PLAYER SAMPLE END ---')
    for s,p in sorted(players[ts[-1]].items()):
        w(s,fmtp(p))
print(out,'bytes',out.stat().st_size,'sample_times',len(ts),'latest_minutes',ts[-1]/60000)
