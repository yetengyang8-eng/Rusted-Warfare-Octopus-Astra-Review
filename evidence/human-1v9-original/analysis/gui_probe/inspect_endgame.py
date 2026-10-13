import gzip,csv,collections,math,pathlib
base=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
T=[130,140,150,160,165,170,175,180,182,184,185,186]
frames=collections.defaultdict(lambda:collections.defaultdict(list))
for r in csv.DictReader(gzip.open(base/'units_valid_to_real_match_end.csv.gz','rt',encoding='utf-8')):
    t=int(r['gameTimeMs'])
    if abs(t/60000-round(t/60000)) < 0.1 and round(t/60000) in T:frames[t][r['slot']].append(r)
players=collections.defaultdict(dict)
for r in csv.DictReader(gzip.open(base/'players_valid_to_real_match_end.csv.gz','rt',encoding='utf-8')):
    t=int(r['gameTimeMs'])
    if t in frames:players[t][r['slot']]=r
for m in T:
 t=min(frames,key=lambda t:abs(t-m*60000))
 a=frames[t].get('5',[])
 c=collections.Counter(r['type'] for r in a)
 p=players.get(t,{}).get('5',{})
 enemies=[(s,len(us),collections.Counter(x['type'] for x in us).most_common(3)) for s,us in frames[t].items() if s not in ('5','-1','-2') and us]
 print('minute',m,'actual',round(t/60000,2),'Ares',len(a),'income',p.get('income'),'credits',p.get('credits'),'heavy',c['heavyTank'],'amph',c['amphibiousJet'],'factories',c['landFactory'],'extractors',sum(v for k,v in c.items() if k.startswith('extractor')),'builders',c['builder'],'enemies',enemies)
