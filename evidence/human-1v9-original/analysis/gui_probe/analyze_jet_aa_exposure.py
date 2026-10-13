import gzip,csv,collections,math,statistics,pathlib
P=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
T=collections.defaultdict(lambda:collections.defaultdict(list))
for r in csv.DictReader(gzip.open(P/'units_valid_to_real_match_end.csv.gz','rt',encoding='utf-8',newline='')):
 ms=int(r['gameTimeMs'])
 if ms<165*60000:continue
 slot=int(r['slot'])
 if slot<0:continue
 r['id']=int(r['id']);r['x']=float(r['x']);r['y']=float(r['y']);r['slot']=slot
 T[ms][slot].append(r)
times=sorted(T)
def ds(u,v):return math.hypot(u['x']-v['x'],u['y']-v['y'])
def closest(u,tar):return min((ds(u,t) for t in tar),default=math.inf)
def binning(d):
 if d<350:return '<350'
 if d<650:return '350-649'
 if d<1200:return '650-1199'
 if d<2400:return '1200-2399'
 if math.isfinite(d):return '>=2400'
 return 'no AA building'
cat=collections.Counter();death=collections.Counter()
combat=collections.Counter();combatdeath=collections.Counter()
examples=[]
for t1,t2 in zip(times,times[1:]):
 a,b=T[t1],T[t2]
 jets=[x for x in a.get(5,[]) if x['type']=='amphibiousJet']
 if not jets:continue
 ids2={x['id'] for row in b.values() for x in row}
 enemy=[x for s,row in a.items() if s!=5 for x in row]
 aa=[x for x in enemy if x['building']=='true' and 'antiair' in x['type'].lower()]
 for j in jets:
  d=closest(j,aa); d2=closest(j,enemy);bucket=binning(d)
  cat[bucket]+=1
  if j['id'] not in ids2:
   death[bucket]+=1
  if d2<=650:
   combat[bucket]+=1
   if j['id'] not in ids2:combatdeath[bucket]+=1
aa_deaths=[];near_counts=collections.Counter()
for t1,t2 in zip(times,times[1:]):
 a,b=T[t1],T[t2]; jet=[x for x in a.get(5,[]) if x['type']=='amphibiousJet']
 heavy=[x for x in a.get(5,[]) if x['type']=='heavyTank']
 enemy=[x for s,row in a.items() if s!=5 for x in row]
 aa=[x for x in enemy if x['building']=='true' and 'antiair' in x['type'].lower()]
 ids2={x['id'] for row in b.values() for x in row}
 for x in aa:
  if x['id'] in ids2:continue
  dj=closest(x,jet);dh=closest(x,heavy)
  rec={'minute':round(t1/60000,2),'slot':x['slot'],'type':x['type'],'pos':(round(x['x']),round(x['y'])),'jet':round(dj) if math.isfinite(dj) else None,'heavy':round(dh) if math.isfinite(dh) else None}
  aa_deaths.append(rec)
  near_counts['aa_disappeared']+=1
  if dj<=650:near_counts['jet_near650']+=1
  if dh<=650:near_counts['heavy_near650']+=1
  if dj<=650 and dh<=650:near_counts['both_near650']+=1
with (P/'tactical_spatial'/'aa_jet_exposure.txt').open('w',encoding='utf-8') as f:
 def w(*args):print(*args,file=f)
 w('Observations are consecutive 30second snapshots, repeat exposures of same jet NOT independent. Distance to building containing "antiAir" in its type is a LOWER-BOUND proxy of AA threat. Unknown AA mobile not included. Missing unit IDs no longer present anywhere on next snapshot.')
 for k in ['<350','350-649','650-1199','1200-2399','>=2400','no AA building']:
  n=cat[k];v=death[k];m=combat[k];q=combatdeath[k]
  w(f'nearest-AA {k:>13} exposures={n:5} lostNextSample={v:4} fraction={(v/n*100 if n else 0):5.1f}% nearestEnemy<=650={m:5} lostWithEnemyNear={q}')
 w('AA disappearance context',near_counts)
 w('AA disappearance examples first 60')
 for x in aa_deaths[:60]:w(x)
print('SNAPSHOT_EXPOSURES',sum(cat.values()),'LOSSES',sum(death.values()),'AA disappearance',len(aa_deaths),'file',P/'tactical_spatial'/'aa_jet_exposure.txt')
