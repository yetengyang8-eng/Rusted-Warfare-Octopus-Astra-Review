import re,collections
log='sampler2.log'
step=0;counts=0;first=None;mark=[]
bucket=collections.defaultdict(lambda:collections.Counter())
who=None
for ln in open(log,encoding='utf-16',errors='replace'):
  if 'replay: upcoming in' in ln:
    m=re.search(r'upcoming in (\d+)',ln)
    if m:step+=int(m.group(1))
  if 'Replay: updateGameFrame: Command:' in ln:
    m=re.search(r'Command: (.*?) \((\d+)\) count:(\d+) id:(\d+)',ln)
    if m:
      who=m.group(1); counts+=1
      bucket[int(step/3750)][who]+=1
  if 'desync:' in ln and first is None:
    first=(ln[:32],counts,step,round(step/3750,2))
  if 'end of replay block found' in ln:mark.append((counts,step,step/3750))
print('FIRST_DESYNC',first)
print('END',mark)
print('ORDER_BUCKETS every 10m')
for a in range(0,211,10):
  h=sum(bucket[m]['Ares'] for m in range(a,a+10))
  ai=sum(sum(v.values())-v['Ares'] for m,v in bucket.items() if a<=m<a+10)
  print(a,h,ai)
