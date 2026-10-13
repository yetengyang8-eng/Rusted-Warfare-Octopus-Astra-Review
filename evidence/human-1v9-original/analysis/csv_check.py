import csv,collections
p='timeseries_1m.csv'
rows=list(csv.reader(open(p,encoding='utf-8',newline='')))
print('headers',len(rows[0]),rows[0])
c=collections.Counter(map(len,rows))
print('row length counts',c,'last',rows[-1][:6])
for i,row in enumerate(rows[1:],2):
 if len(row)!=len(rows[0]):
  print('BAD',i,len(row),row[:8],row[-6:])
  if i>200:break
