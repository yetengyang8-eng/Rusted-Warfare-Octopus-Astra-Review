import csv,gzip,collections,math
p='unit_states_30s.csv.gz'
target=[5,10,15,20,25,30,35,40,45,50,55,60,65,70]
frames=collections.defaultdict(list)
for r in csv.DictReader(gzip.open(p,'rt',encoding='utf-8',newline='')):
 m=int(r['gameTimeMs'])/60000
 if m>70.5:break
 near=round(m)
 if near not in target or abs(m-near)>.06:continue
 if r['slot']=='5' and r['type']=='heavyTank':
  frames[near].append((float(r['x']),float(r['y'])))
def groups(positions,dist):
 n=len(positions);par=list(range(n))
 def root(x):
  while par[x]!=x:
   par[x]=par[par[x]];x=par[x]
  return x
 for i in range(n):
  for j in range(i+1,n):
   if math.hypot(positions[i][0]-positions[j][0],positions[i][1]-positions[j][1])<dist:
    par[root(i)]=root(j)
 g=collections.defaultdict(list)
 for i,p in enumerate(positions):g[root(i)].append(p)
 result=sorted(g.values(),key=len,reverse=True)
 return [(len(x),(round(sum(p[0] for p in x)/len(x)),round(sum(p[1] for p in x)/len(x)))) for x in result]
for minute in target:
 g=groups(frames[minute],300)
 print(minute,'N',len(frames[minute]),'groups>=5',[(n,xy) for n,xy in g if n>=5][:12], 'largest-share', round(g[0][0]/len(frames[minute]),2) if g else 0)
