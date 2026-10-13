import json,collections, pathlib
p=pathlib.Path(r'G:\Steam\steamapps\common\Rusted Warfare\rw-camera-follow\matches\6a17f993-aa63-44ca-a7f0-296736891d81\session.rwmetrics')
kinds=collections.Counter(); fields=collections.defaultdict(set); examples={}
count=0; invalid=0; gameend=0
with p.open(encoding='utf-8') as f:
 for line in f:
  count+=1
  try:r=json.loads(line)
  except:invalid+=1;continue
  kind=r.get('kind','<unknown>');kinds[kind]+=1;fields[kind].update(r.keys());examples.setdefault(kind,r)
  if r.get('gameEndMs',0)>gameend:gameend=r['gameEndMs']
print('lines',count,'invalid',invalid,'kinds',dict(kinds),'endGameMs',gameend)
for kind,items in fields.items():
 print('KIND',kind,'FIELDS',sorted(items))
 print('EXAMPLE',json.dumps(examples[kind],ensure_ascii=False)[:1600])
