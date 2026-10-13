import pathlib,csv,collections,re,hashlib,json
root=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\steam_exact_gui_clone')
out=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
results={}
for kind in ('units','players'):
 p=root/'replay_probe_output'/f'{kind}_30s.csv'
 n=0;empty=0;times=[];slots=collections.Counter();types=collections.Counter()
 with p.open(encoding='utf-8',newline='') as f:
  rd=csv.DictReader(f)
  for row in rd:
   n+=1
   try:
    t=int(row['gameTimeMs']);slots[row['slot']]+=1
   except:empty+=1;continue
   if not times or t!=times[-1]:times.append(t)
   if kind=='units':types[row['type']]+=1
 results[kind]=dict(bytes=p.stat().st_size,rows=n,invalid=empty,timestamps=len(times),first=times[0],last=times[-1],monotone=all(a<b for a,b in zip(times,times[1:])),slots=dict(slots),topTypes=types.most_common(8),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
log=root/'rwpp-log.txt'
checks=0;matching=0;bad=[];ended=0;lastcheck=None;cmdmax=0
for line in log.open(encoding='utf-8',errors='replace'):
 if 'Replay: checksum:' in line:
  checks+=1
  if 'checksums are matching' in line:matching+=1;lastcheck=line.strip()
 if 'replay:updateGameFrame:end of replay block found' in line:ended+=1
 for pat in ('desync:','checksums don\'t match','checksums are not matching','checksum mismatch','[RWPROBE] sample FAILED'):
  if pat in line.lower():bad.append(line.strip()[:300])
 m=re.search(r'Replay: updateGameFrame: Command: .* id:(\d+)',line)
 if m:cmdmax=max(cmdmax,int(m.group(1)))
results['log']=dict(bytes=log.stat().st_size,checksumEvents=checks,matchingEvents=matching,nonmatchingEvents=checks-matching,ended=ended,lastChecksum=lastcheck,errors=bad[:15],maxCommandId=cmdmax)
p=out/'gui_sample_integrity.json'
p.write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
print('INTEGRITY_FILE',p)
for k,v in results.items():print(k, {key:value for key,value in v.items() if key not in ('slots','topTypes','sha256','errors')})
