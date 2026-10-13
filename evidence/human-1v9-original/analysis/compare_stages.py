import csv
files=['timeseries_1m.csv','steam_330k/timeseries_generated.csv','steam_330k/timeseries_1m.csv']
for p in files:
 print('\nFILE',p)
 rows=[]
 with open(p,encoding='utf-8-sig',errors='replace',newline='') as f:
  for r in csv.DictReader(f):
   if not r.get('gameTimeMs') or not r.get('teamId'):continue
   if r['teamId'] !='5':continue
   ms=int(r['gameTimeMs'])
   if ms%300000<25000 or 300000-(ms%300000)<25000:
    rows.append(r)
 for r in rows[:20]:
  print(round(int(r['gameTimeMs'])/60000,2),r['credits'],r['effectiveIncome'],r['heavyTank'],r['landFactory'],r['builder'])
