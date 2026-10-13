import gzip,csv,collections,math,statistics,pathlib
P=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
folder=P/'tactical_spatial'
# sample by timestamp, only last phase
frames=collections.defaultdict(lambda:collections.defaultdict(list))
for r in csv.DictReader(gzip.open(P/'units_valid_to_real_match_end.csv.gz','rt',encoding='utf-8')):
 t=int(r['gameTimeMs'])
 if t<155*60000:continue
 r['id']=int(r['id']);r['x']=float(r['x']);r['y']=float(r['y']);r['hp']=float(r['hp']);r['maxhp']=float(r['maxhp'])
 frames[t][int(r['slot'])].append(r)
times=sorted(frames)
def dist(u,v):return math.hypot(u['x']-v['x'],u['y']-v['y'])
def nearest(units,targets):
 if not units or not targets:return (None,None,None)
 dd=[min(dist(u,t) for t in targets) for u in units];dd.sort()
 return (round(statistics.median(dd)),round(dd[min(len(dd)-1,int(.1*len(dd)))]),sum(x<650 for x in dd))
def center(units):
 if not units:return None
 return (int(sum(x['x'] for x in units)/len(units)),int(sum(x['y'] for x in units)/len(units)))
def det(t):
 sides=frames[t]; own=sides.get(5,[]);foes=[x for k,row in sides.items() if k not in (-1,-2,5) for x in row]
 jet=[x for x in own if x['type']=='amphibiousJet']
 heavy=[x for x in own if x['type']=='heavyTank']
 antiair=[x for x in foes if x['building']=='true' and ('antiair' in x['type'].lower())]
 buildings=[x for x in foes if x['building']=='true']
 other=[x for x in buildings if 'antiair' not in x['type'].lower()]
 c=collections.Counter(x['type'] for x in own)
 aa_0=[x for x in sides.get(0,[]) if x['building']=='true' and 'antiair' in x['type'].lower()]
 aa_4=[x for x in sides.get(4,[]) if x['building']=='true' and 'antiair' in x['type'].lower()]
 jet_ids={x['id'] for x in jet}
 return dict(minute=round(t/60000,2),jets=len(jet),heavy=len(heavy),factories=c['landFactory'],builders=c['builder'],extractors=sum(v for k,v in c.items() if k.startswith('extractor')),aa=len(antiair),aa_slot0=len(aa_0),aa_slot4=len(aa_4),enemy_build=len(buildings),enemy_other_build=len(other),enemy_all=len(foes),
   jet_center=center(jet),jet_hp_pct=round(statistics.median(100*x['hp']/x['maxhp'] for x in jet),1) if jet and all(x['maxhp']>0 for x in jet) else None,
   jet_aa=nearest(jet,antiair),jet_other_build=nearest(jet,other),jet_heavy=nearest(jet,heavy),jet_enemy=nearest(jet,foes),
   jet_ids=jet_ids,heavy_ids={x['id'] for x in heavy},
   e0=len(sides.get(0,[])),e4=len(sides.get(4,[])),e8=len(sides.get(8,[])))
d={t:det(t) for t in times}
csv_fields=['minute','jets','heavy','factories','builders','extractors','aa','aa_slot0','aa_slot4','enemy_build','enemy_other_build','enemy_all','jet_center','jet_hp_pct','jet_aa','jet_other_build','jet_heavy','jet_enemy','e0','e4','e8']
with (folder/'late_phase_every_30s.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,fieldnames=csv_fields);w.writeheader()
 for row in d.values():w.writerow({k:row[k] for k in csv_fields})
out=folder/'late_phase_detailed.txt'
with out.open('w',encoding='utf-8') as f:
 def w(*args):print(*args,file=f)
 w('Nearest tuple (median distance, 10th percentile, number of jets within 650 world units); AA count includes only building whose type has antiAir substring, is a lower bound on actual air threats.')
 for min_ in [155,160,165,166,167,168,169,170,171,172,173,174,175,177,180,182,184,185,186]:
  t=min(d,key=lambda t:abs(t/60000-min_));x=d[t]
  w('== minute',x['minute'],'jet',x['jets'],'heavy',x['heavy'],'own factories',x['factories'],'extractors',x['extractors'],'enemy AA',x['aa'],'AA_slot0',x['aa_slot0'],'AA_slot4',x['aa_slot4'],'enemy_build',x['enemy_build'],'enemy_total',x['enemy_all'])
  w('   JET CENTER',x['jet_center'],'medianHP%',x['jet_hp_pct'],'dist AA',x['jet_aa'],'dist otherBuilding',x['jet_other_build'],'dist enemyAny',x['jet_enemy'],'dist heavy',x['jet_heavy'])
 w('==ID FLOW Jets every 5 mins')
 for a in [160,165,170,175,180]:
  t=min(d,key=lambda t:abs(t/60000-a));u=min(d,key=lambda t:abs(t/60000-(a+5)))
  new=d[u]['jet_ids']-d[t]['jet_ids'];gone=d[t]['jet_ids']-d[u]['jet_ids']
  w(d[t]['minute'],'to',d[u]['minute'],'JETS',d[t]['jets'],'->',d[u]['jets'],'new',len(new),'absent',len(gone))
 print('COMPLETE',file=f)
print('CREATED',out,'samples',len(d),'first jets',next((x['minute'] for x in d.values() if x['jets']>0),None))
