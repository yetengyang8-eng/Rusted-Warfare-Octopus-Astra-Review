import csv,json,collections,datetime,os
from pathlib import Path
root=Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012')
base=Path(r'G:\Steam\steamapps\common\Rusted Warfare\rw-camera-follow\matches\6a17f993-aa63-44ca-a7f0-296736891d81')
summary=json.loads((base/'summary.json').read_text(encoding='utf-8'))
out=root/'original_game_metrics_per_minute.csv'
stats=collections.defaultdict(lambda:collections.Counter())
totals=collections.Counter()
with (base/'session.rwmetrics').open(encoding='utf-8') as f:
 for line in f:
  row=json.loads(line)
  m=min(int(row['gameEndMs']/60000),186)
  c=stats[m]
  inp=row['input']
  for x in ('key','left','right','middle','wheel'):c[x]+=inp.get(x,0)
  c['activeMs']+=row.get('activeMs',0)
  c['wallMs']+=row['wallEndMs']-row['wallStartMs']
  for pl in row['players']:
   pid=pl['id']
   c[f'cmd_{pid}']+=pl['commands']
   c[f'assist_{pid}']+=pl['assist']
  c['buckets']+=1
columns=['minute','gameclock','buckets','wallMs','activeMs','key','left','right','middle','wheel']+[f'cmd_{i}' for i in range(10)]+[f'assist_{i}' for i in range(10)]
with out.open('w',encoding='utf-8-sig',newline='') as f:
 writer=csv.DictWriter(f,fieldnames=columns);writer.writeheader()
 for m,c in sorted(stats.items()):
  d={k:c[k] for k in columns if k not in ('minute','gameclock')}
  writer.writerow(dict(minute=m,gameclock=f'{m//60:02}:{m%60:02}',**d))
print('CSV',out,'minutes',len(stats))
print('original game',summary['endGameMs']/60000,'minutes; input',summary['inputTotal'],'APM',summary['inputApm'])
print('recorded player commands',next(p for p in summary['players'] if p['id']==5))
print('PLAYER EXCLUSION',summary['exclusionNs'])
print('DEFEATED players:')
for p in summary['players']:
 if p['type']=='AI':
  t=p.get('participationEndGameMs')
  print('slot',p['id'],'endReason',p['endReason'],'minute',round(t/60000,2) if t else 'unknown')
print('PER 15min')
for st in range(0,181,15):
 sel=[stats[m] for m in range(st,min(st+15,187)) if m in stats]
 tot=lambda k:sum(c[k] for c in sel)
 print(f'{st:3}-{st+15:3}m commands={tot("cmd_5"):5} assist={tot("assist_5"):5} local={tot("cmd_5")-tot("assist_5"):5} input={sum(tot(k) for k in ["key","left","right","middle"]):5} wheel={tot("wheel"):5} AI={sum(tot("cmd_"+str(i)) for i in range(10) if i!=5):5}')
