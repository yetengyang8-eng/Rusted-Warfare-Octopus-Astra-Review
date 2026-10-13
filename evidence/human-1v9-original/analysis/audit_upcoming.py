import re,collections
p='sampler2.log'
ct=0;s=0;vals=collections.Counter();last=None;full=0
for line in open(p,encoding='utf-16',errors='replace'):
 if 'replay: upcoming in' in line:
  m=re.search(r'upcoming in (\d+) command:(\w+)',line)
  if m: n=int(m.group(1));s+=n;ct+=1;vals[m.group(2)]+=1;last=n
print('n_upcoming',ct,'sum_steps',s,'seconds_at_62.5fps',s/62.5,'minutes',s/62.5/60,'types',vals)
