import csv,collections,math,os
p=r'G:\deepseek 工作台\_analysis\human_1v9_20261012\timeseries_1m.csv'
out=r'G:\deepseek 工作台\_analysis\human_1v9_20261012\summary.txt'
rows=[]
reader=csv.reader(open(p,encoding='utf-8-sig',newline=''))
header=next(reader)
for values in reader:
    if len(values)!=len(header): continue
    r=dict(zip(header,values))
    if not r.get("gameTimeMs") or not r.get("effectiveIncome"): continue
    for k in ['gameTimeMs','frame','teamId','effectiveIncome','rawIncome','totalUnits','combatAssets','militaryValue','mobileMilitaryValue','mobileCombat','buildings','extractorT1','extractorT2','extractorT3','extractorT3_reinforced','landFactory','builder','heavyTank','amphibiousJet','commandCenter']:
        r[k]=int(float(r[k] or 0))
    r['credits']=float(r['credits']);r['minute']=r['gameTimeMs']/60000
    rows.append(r)
by=collections.defaultdict(list)
for r in rows:by[r['teamId']].append(r)
for arr in by.values():arr.sort(key=lambda r:r['gameTimeMs'])
def near(slot,m):
    return min(by[slot],key=lambda r:abs(r['minute']-m))
def nmin(slot,key,n):
    hit=next((r for r in by[slot] if r[key]>=n),None)
    return hit and round(hit['minute'],1)
def num(x):return f'{x:,.0f}'
f=open(out,'w',encoding='utf-8')
def w(s=''):print(s,file=f)
w('SOURCE: '+p)
w(f'END {rows[-1]["minute"]:.2f} minutes, {len(rows)} rows')
w('SLOTS')
for s,arr in sorted(by.items()):w(f'{s}: {arr[0]["name"]}')
w()
w('HUMAN ARES SNAPSHOTS vs AI combined/max')
fields=['credits','effectiveIncome','rawIncome','totalUnits','buildings','builder','landFactory','heavyTank','amphibiousJet','mobileCombat','mobileMilitaryValue','militaryValue','extractorT1','extractorT2','extractorT3','extractorT3_reinforced','commandCenter']
for m in [0,5,10,15,20,30,40,50,60,70,80,90,100,120,140,160,180,190,200,210]:
    h=near(5,m);a=[near(s,m) for s in range(10) if s!=5]
    active=[r for r in a if r['totalUnits']>0]
    w(f'\n## {m}m actual={h["minute"]:.1f}; liveAI={len(active)}')
    for k in fields:
        s=sum(x[k] for x in a)
        maxid,maxv=max(((x['teamId'],x[k]) for x in a),key=lambda x:x[1])
        w(f'{k:24} human={num(h[k]):>11} AI_sum={num(s):>12} AI_max={num(maxv):>11} slot={maxid}')
w('\nAI TIME OF COLLAPSE')
for s in range(10):
    if s==5: continue
    arr=by[s];last_positive=max((r['minute'] for r in arr if r['totalUnits']>0),default=None)
    first_dead=next((r['minute'] for r in arr if r['minute']>5 and r['totalUnits']==0),None)
    peakInc=max(arr,key=lambda r:r['effectiveIncome'])
    peakMil=max(arr,key=lambda r:r['mobileMilitaryValue'])
    w(f'slot {s}: firstZero={first_dead},lastPositive={last_positive}; peakIncome={peakInc["effectiveIncome"]} at {peakInc["minute"]:.1f}; peakMobileMil={peakMil["mobileMilitaryValue"]} at {peakMil["minute"]:.1f}')
w('\nHUMAN FIRST REACHED')
for k in ['builder','landFactory','heavyTank','amphibiousJet','extractorT1','extractorT2','extractorT3']:
    w(k+': '+', '.join(f'{n}=>{nmin(5,k,n)}m' for n in [1,2,5,10,20,50,100,150,200,300]))
w('\nHUMAN PEAKS')
for k in ['credits','effectiveIncome','totalUnits','mobileCombat','mobileMilitaryValue','heavyTank','amphibiousJet','landFactory','builder','extractorT1','extractorT2','extractorT3']:
    arr=by[5]; mx=max(arr,key=lambda r:r[k])
    w(f'{k}: {mx[k]} at {mx["minute"]:.1f}')
w('\nHUMAN COMBAT VALUE DELTAS')
for m in range(0,211,10):
    a,b=near(5,m),near(5,m+10)
    w(f'{m:3}-{m+10:3} min: mobileMil {num(a["mobileMilitaryValue"])}->{num(b["mobileMilitaryValue"])} ({b["mobileMilitaryValue"]-a["mobileMilitaryValue"]:+}), heavy {a["heavyTank"]}->{b["heavyTank"]}, inc {a["effectiveIncome"]}->{b["effectiveIncome"]}, credits {round(a["credits"])}->{round(b["credits"])}')
f.close();print(out)
