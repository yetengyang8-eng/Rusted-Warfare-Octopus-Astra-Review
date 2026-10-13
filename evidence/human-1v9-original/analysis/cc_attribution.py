import csv,gzip,collections,math
p='unit_states_30s.csv.gz'
snapshots=collections.defaultdict(list)
for r in csv.DictReader(gzip.open(p,'rt',encoding='utf-8',newline='')):
 m=int(r['gameTimeMs'])/60000
 if m>78:break
 k=round(m*2)/2
 if abs(m-k)>.065:continue
 r['x']=float(r['x']);r['y']=float(r['y']);r['slot']=int(r['slot'])
 snapshots[k].append(r)
skeys=sorted(snapshots)
lastcc={}
results=[]
for m in skeys:
 rows=snapshots[m]
 ccs={u['slot']:u for u in rows if u['type']=='commandCenter'}
 if lastcc:
  for slot,base in lastcc.items():
   if slot==5 or slot in ccs or any(x['slot']==slot and x['type']=='commandCenter' for x in rows):continue
   x,y=base['x'],base['y']
   prev=snapshots.get(round((m-.5)*2)/2,[])
   h=[u for u in prev if u['slot']==5 and u['attack']=='true' and u['building']=='false']
   others=[u for u in prev if u['slot'] not in (slot,5,-1) and u['attack']=='true' and u['building']=='false']
   def closest(units):
    a=sorted([(math.hypot(u['x']-x,u['y']-y),u['slot'],u['type'],u['x'],u['y']) for u in units])
    return [(round(z[0]),z[1],z[2]) for z in a[:3]]
   results.append((m,slot,(round(x),round(y)),closest(h),closest(others)))
 lastcc=ccs
print('COMMAND_CENTER_DISAPPEARANCE with nearby troops at previous 30sec')
for a in results:print(a)
