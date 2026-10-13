from pathlib import Path
import re,collections
p=Path(r"G:\Steam\steamapps\common\Rusted Warfare\rwpp-log.txt")
checkpoint=51664
patcmd=re.compile(r'Replay: updateGameFrame: Command: (.*?) \((\d+)\) count:(\d+) id:(\d+)')
patcheck=re.compile(r'Replay: checksum: (.*?) frameNumber:(\d+)')
suspects=collections.Counter()
firsts={}
checks=[]
commandnear=[]
command_count=0
after_cmd=[]
last_cmd=None
for line in p.open("r",encoding="utf-8",errors="replace"):
  m=patcmd.search(line)
  if m:
    cid=int(m.group(4));last_cmd=(cid,m.group(1),line.strip())
    command_count+=1
    if checkpoint-3<=cid<=checkpoint+8:commandnear.append(line.strip())
    if cid>checkpoint and len(after_cmd)<8:after_cmd.append(line.strip())
  m=patcheck.search(line)
  if m:
    checks.append((int(m.group(2)),m.group(1).strip(),line.strip()))
  lower=line.lower()
  for key in ('desync:', 'checksum mismatch','checksums are not matching','out of sync','error: sync','replay: checksum failed'):
    if key in lower:
      suspects[key]+=1
      if key not in firsts:firsts[key]=line.strip()[:400]
print("LOG_BYTES",p.stat().st_size)
print("TOTAL_COMMAND_LOG_LINES",command_count)
print("LAST_COMMAND",last_cmd)
print("NEAR_51664",len(commandnear))
for x in commandnear:print(x)
print("CHECKSUM_TOTAL",len(checks))
print("LATEST_5_CHECKSUMS")
for a in checks[-5:]:print(a)
print("FIRST_MATCHING_AFTER_51664_APPROX",next((a for a in checks if a[0]>291000),None))
print("SUSPECT_COUNTS",dict(suspects))
print("FIRST_SUSPECT_LINES",firsts)
print("ESTIMATED_CHECKSUM_GAME_TIME_SEC",round(checks[-1][0]/62.5,2) if checks else "NONE")
