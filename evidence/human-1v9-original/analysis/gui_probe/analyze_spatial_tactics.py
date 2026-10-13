import gzip,csv,collections,math,statistics,pathlib,json
B=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
OUT=B/'tactical_spatial'
OUT.mkdir(exist_ok=True)
samples=collections.defaultdict(lambda:collections.defaultdict(list))
with gzip.open(B/'units_valid_to_real_match_end.csv.gz','rt',encoding='utf-8',newline='') as f:
 for u in csv.DictReader(f):
  if int(u['slot'])<0:continue
  u['x']=float(u['x']);u['y']=float(u['y']);u['hp']=float(u['hp']);u['maxhp']=float(u['maxhp']);u['id']=int(u['id'])
  samples[int(u['gameTimeMs'])][int(u['slot'])].append(u)
times=sorted(samples)
def near(minute):return min(times,key=lambda t:abs(t/60000-minute))
def dist(a,b):return math.hypot(a['x']-b['x'],a['y']-b['y'])
def clust(units,rad=285):
 n=len(units); par=list(range(n));rank=[0]*n
 def find(x):
  while par[x]!=x:par[x]=par[par[x]];x=par[x]
  return x
 def union(a,b):
  a,b=find(a),find(b)
  if a==b:return
  if rank[a]<rank[b]:a,b=b,a
  par[b]=a
  if rank[a]==rank[b]:rank[a]+=1
 cells=collections.defaultdict(list)
 for i,u in enumerate(units):
  gx=int(u['x']//rad);gy=int(u['y']//rad)
  for dx in (-1,0,1):
   for dy in (-1,0,1):
    for j in cells.get((gx+dx,gy+dy),()):
     if dist(u,units[j])<=rad:union(i,j)
  cells[(gx,gy)].append(i)
 groups=collections.defaultdict(list)
 for i,u in enumerate(units):groups[find(i)].append(u)
 result=[]
 for g in groups.values():
  cx=sum(v['x'] for v in g)/len(g);cy=sum(v['y'] for v in g)/len(g)
  result.append({'n':len(g),'x':round(cx),'y':round(cy),'units':g,
                 'radius':round(max(math.hypot(v['x']-cx,v['y']-cy) for v in g))})
 return sorted(result,key=lambda g:g['n'],reverse=True)
def bytype(rows):return collections.Counter(u['type'] for u in rows)
def stat(t):
 a=samples[t].get(5,[])
 enemy=[u for s,x in samples[t].items() if s!=5 for u in x]
 c=bytype(a)
 heavy=[u for u in a if u['type']=='heavyTank']
 jets=[u for u in a if u['type']=='amphibiousJet']
 aa=[u for u in enemy if 'antiair' in u['type'].lower() and u['building']=='true']
 other_enemy_build=[u for u in enemy if u['building']=='true' and u not in aa]
 gg=clust(heavy)
 strong=[x for x in gg if x['n']>=8]
 widely=[]
 for x in strong:
  if not widely or min(math.hypot(x['x']-y['x'],x['y']-y['y']) for y in widely)>=800:widely.append(x)
 return dict(t=t,m=round(t/60000,2),own_total=len(a),enemy_total=len(enemy),heavy=len(heavy),jets=len(jets),
    factories=c['landFactory'],aa=len(aa),enemyBuildings=len(other_enemy_build),
    groups=gg,major=strong,fronts=widely,
    enemy_aa=aa,enemy_build=other_enemy_build,heavy_units=heavy,jet_units=jets,enemy_units=enemy)
D={t:stat(t) for t in times}
table=[]
for t in times:
 d=D[t]
 table.append(dict(minute=d['m'],heavy=d['heavy'],jets=d['jets'],factory=d['factories'],enemy=d['enemy_total'],aa_turrets=d['aa'],
    heavy_groups_ge8=len(d['major']),independent_heavy_fronts_ge8=len(d['fronts']),
    groups_top5=';'.join(f'{g["n"]}@({g["x"]},{g["y"]})' for g in d['major'][:5])))
with (OUT/'fronts_every_30s.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,fieldnames=table[0]);w.writeheader();w.writerows(table)
with (OUT/'spatial_summary.txt').open('w',encoding='utf-8') as f:
 def w(*args):print(*args,file=f)
 w('source',B,'snapshots',len(times),'threshold connected heavy distance=285; fronts considered distinct if centroid>=800 apart; major>=8 heavies')
 w('All statistics are per-observation snapshots, not AI General assignments.')
 for min_ in [10,20,30,40,50,60,70,80,90,100,110,120,130,140,150,160,165,170,175,180,185,186]:
  d=D[near(min_)]
  w('== GAME',round(d['m'],2),'heavy',d['heavy'],'jets',d['jets'],'enemy',d['enemy_total'],'AA turrets',d['aa'],
    'major groups',len(d['major']),'independent fronts',len(d['fronts']))
  for g in d['major'][:8]:
   w('   heavy_group',g['n'],'center',g['x'],g['y'],'radius',g['radius'])
  if min_>=160:
   w('   per_enemy summary')
   for slot in sorted(s for s in samples[near(min_)].keys() if s!=5):
    ea=samples[near(min_)][slot];cc=bytype(ea)
    w('    slot',slot,'count',len(ea),'antiAirTurret',sum(v for k,v in cc.items() if 'antiair' in k.lower()),
      'buildings',sum(x['building']=='true' for x in ea),'top',cc.most_common(5))
  if d['jet_units']:
   pairs=[]
   for u in d['jet_units']:
    if d['enemy_aa']:pairs.append(min(dist(u,v) for v in d['enemy_aa']))
   if pairs:w('   min_dist_to_antiAirTurret_by_jet median',round(statistics.median(pairs)),
      'p10',round(sorted(pairs)[int(.1*(len(pairs)-1))]),'near600',sum(x<=600 for x in pairs),'of',len(pairs))
 w('==MULTIFRONT COUNTS')
 for a,b in [(0,30),(30,60),(60,90),(90,120),(120,150),(150,165),(165,186.2)]:
  q=[d for d in D.values() if a<=d['m']<b and d['heavy']>=20]
  w(f'{a}-{b}: n={len(q)} >=2fronts={sum(len(x["fronts"])>=2 for x in q)} >=3fronts={sum(len(x["fronts"])>=3 for x in q)} median_heavy={statistics.median(x["heavy"] for x in q) if q else 0}')
 w('==HEAVY 5 MINUTE FLOW: new unit IDs, absent IDs, stock')
 for minute in range(0,181,5):
  a,b=D[near(minute)],D[near(minute+5)]
  ids1={u['id'] for u in a['heavy_units']};ids2={u['id'] for u in b['heavy_units']}
  w(f'{a["m"]:6.1f}->{b["m"]:6.1f} stock {len(ids1):3}->{len(ids2):3} new={len(ids2-ids1):3} absent={len(ids1-ids2):3} overlap={len(ids1&ids2):3}')
print(OUT,'snapshots',len(times))
