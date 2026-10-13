import gzip,csv,collections,pathlib
B=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
snap=collections.defaultdict(dict)
for x in csv.DictReader(gzip.open(B/'units_valid_to_real_match_end.csv.gz','rt',encoding='utf-8',newline='')):
 t=int(x['gameTimeMs']);slot=int(x['slot'])
 if slot<0:continue
 snap[t][int(x['id'])]=(slot,x['type'])
times=sorted(snap)
def near(m):return min(times,key=lambda t:abs(t/60000-m))
for unit in ['heavyTank','amphibiousJet']:
 print('===',unit)
 for a,b in [(55,60),(60,65),(80,85),(105,110),(120,125),(160,165),(165,170),(170,175),(175,180),(180,185),(185,186)]:
  t1=near(a);t2=near(b)
  d1=snap[t1];d2=snap[t2]
  s1={k for k,(slot,typ) in d1.items() if slot==5 and typ==unit}
  s2={k for k,(slot,typ) in d2.items() if slot==5 and typ==unit}
  missing=s1-s2
  present_other={d2[k] for k in missing if k in d2}
  print(a,b,'stock',len(s1),len(s2),'appeared',len(s2-s1),'missing',len(missing),'missing_alive_anywhere',len([k for k in missing if k in d2]),'other_types',collections.Counter(present_other))
