import csv
p='timeseries_1m.csv';out='trusted_early_0_70.csv'
raw=csv.reader(open(p,encoding='utf-8',newline=''))
header=next(raw)
with open(out,'w',encoding='utf-8-sig',newline='') as f:
  w=csv.writer(f);w.writerow(header);n=0
  for row in raw:
    if len(row)!=len(header):continue
    if not row[0]:continue
    if float(row[0])<=70*60000:
      w.writerow(row);n+=1
print('rows',n,'file',out)
