import re,csv,collections
inp='sampler2.log';out='human_actions.csv'
step=0;pending=None;events=[]
with open(inp,encoding='utf-16',errors='replace') as f:
 for line in f:
  if 'replay: upcoming in' in line:
   m=re.search(r'upcoming in (\d+)',line)
   if m:step+=int(m.group(1))
  if 'Replay: updateGameFrame: Command:' in line:
   m=re.search(r'Command: (.*?) \((\d+)\) count:(\d+) id:(\d+)',line)
   if m:
    if pending:events.append(pending)
    pending={'estimatedMinute':round(step/3750,4),'sourceFrameCounter':step,'slot':int(m.group(2)),'who':m.group(1),'selectedCount':int(m.group(3)),'commandId':int(m.group(4)),'order':'other'}
  if pending and 'Replay: updateGameFrame: Waypoint:' in line:
    pending['order']=line.split('Waypoint: ',1)[-1].strip()
  if pending and 'Replay: updateGameFrame: SpecialAction:' in line:
    pending['order']='special:'+line.split('SpecialAction: ',1)[-1].strip()
if pending:events.append(pending)
with open(out,'w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,fieldnames=list(events[0].keys()));w.writeheader();w.writerows(events)
print('WRITTEN',len(events),'END',events[-1])
phase=[0,20,40,60,80,100,120,140,160,180,185]
def phaseid(m):
 return max(0,next((i for i in range(len(phase)-1) if phase[i]<=m<phase[i+1]),len(phase)-2))
for side in [5,'AI']:
 print('\n','ARES' if side==5 else 'NINE AI')
 ss=[e for e in events if (e['slot']==side if side==5 else e['slot']!=5)]
 for i in range(len(phase)-1):
  a=[e for e in ss if phaseid(e['estimatedMinute'])==i]
  c=collections.Counter(x['order'] for x in a)
  def incl(*keys):return sum(v for k,v in c.items() if any(k.startswith(s) for s in keys))
  print(f'{phase[i]:3}-{phase[i+1]:3}m orders={len(a):5} move={c["move"]:5} attack={c["attack"]:5} attackMove={c["attackMove"]:5} patrol={c["patrol"]:5} build={c["build"]:5} heavy={c["special:u_heavyTank"]:4} mineUpgrade={incl("special:extractorT2","special:extractorT3"):4} scout={c["special:u_scout"]:4} totalSelected={sum(x["selectedCount"] for x in a)}')
 if side==5:
  c=collections.Counter(e['order'] for e in ss)
  print('TOP',c.most_common(35))
