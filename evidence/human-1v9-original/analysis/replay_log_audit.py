import re,collections
p='sampler2.log'
desync=[];err=collections.Counter();counts=collections.Counter();orders=collections.Counter()
who=None; lastcmd=None; cmdcount=0
first=last=None; replayup=0; m=0
f=open(p,encoding='utf-16',errors='replace')
for line in f:
    if 'desync:' in line.lower():
        err[line.split('desync:',1)[-1].split(' id:')[0].strip()[:100]]+=1
        if len(desync)<15:desync.append((cmdcount,line.strip()[:260]))
    if 'Replay: updateGameFrame: Command:' in line:
        m=re.search(r'Command: (.*?) \((\d+)\) count:(\d+) id:(\d+)',line)
        if m:
            who,slot,count,cid=m.group(1),int(m.group(2)),int(m.group(3)),int(m.group(4))
            counts[(slot,who)]+=1
            if who=='Ares': lastcmd=cid
            cmdcount+=1
    if 'Replay: updateGameFrame: Waypoint:' in line or 'Replay: updateGameFrame: SpecialAction:' in line:
        k=line.split('Replay: updateGameFrame: ')[-1].strip()
        orders[(who,k)]+=1
    if 'replay:updateGameFrame:end of replay block found' in line: last=('end_replay',cmdcount,line[:30])
print('desync_first',desync[:12])
print('DESYNC_COUNTS',err.most_common(12))
print('commands',cmdcount,'ares_last_id',lastcmd,'last',last)
print('COMMANDS BY PLAYER',counts)
print('ARES ORDER TYPES',[(k,v) for k,v in orders.most_common() if k[0]=='Ares'][:50])
print('AI ORDER TYPES',[(k,v) for k,v in orders.most_common() if k[0]!='Ares'][:30])
