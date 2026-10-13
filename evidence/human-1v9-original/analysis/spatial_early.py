import csv,gzip,collections,math
p='unit_states_30s.csv.gz'
frames=collections.defaultdict(list)
for r in csv.DictReader(gzip.open(p,'rt',encoding='utf-8',newline='')):
    ms=int(r['gameTimeMs']);m=ms/60000
    if m>75:break
    nearest=round(m/5)*5
    if abs(m-nearest)<=.09 and 0<=nearest<=75:
        r['slot']=int(r['slot']);r['x']=float(r['x']);r['y']=float(r['y']);r['building']=r['building']=='true';r['attack']=r['attack']=='true'
        frames[nearest].append(r)
def med(p):
    if not p:return (None,None)
    return round(sum(u['x'] for u in p)/len(p)),round(sum(u['y'] for u in p)/len(p))
def nearest_dist(us, vs):
    if not us or not vs:return None
    # cost O(n*m) but for 1m game approx small
    return round(min(math.hypot(u['x']-v['x'],u['y']-v['y']) for u in us for v in vs))
out=open('spatial_early_summary.txt','w',encoding='utf-8')
def w(*xs):print(*xs,file=out)
w('SAMPLES',[(m,len(r)) for m,r in sorted(frames.items())])
w('STARTING COMMAND CENTERS:')
for u in frames[0]:
    if u['type']=='commandCenter':w('slot',u['slot'],'team',u['team'],'xy',(u['x'],u['y']))
w()
for m, units in sorted(frames.items()):
    if m>70:break
    h=[u for u in units if u['slot']==5]
    hh=[u for u in h if u['type']=='heavyTank']
    ai=[u for u in units if u['slot']!=5]
    attackers=[u for u in ai if u['attack'] and not u['building']]
    hpos=med(hh)
    nearest=nearest_dist(hh,attackers)
    w(f'=== {m} min HUMAN heavy={len(hh)} total={len(h)} center={hpos} enemy_nearest={nearest} AIunits={len(ai)}')
    by=collections.defaultdict(list)
    for u in units:by[u['slot']].append(u)
    for slot, team in sorted(by.items()):
        if slot==5:continue
        cc=[u for u in team if u['type']=='commandCenter']
        attack=[u for u in team if u['attack'] and not u['building']]
        near=nearest_dist(hh,attack)
        w(' slot',slot,'alive',len(team),'mobileAttack',len(attack),'CC',[(round(u['x']),round(u['y'])) for u in cc],'nearestHeavyToEnemy',near)
out.close()
print('SUMMARY_WRITTEN',len(frames))
