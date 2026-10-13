"""Expand compact same-record references and optional gzip into a NEW JSONL file.
Omitted snapshot fields stay omitted; this cannot reconstruct unlogged observations.
"""
import argparse
import gzip
import json
from pathlib import Path

def expand(row):
    def visit(value, depth=0):
        if depth > 32:
            raise ValueError('Report reference cycle/depth exceeded')
        if isinstance(value, dict):
            if set(value) == {'$rwReportRef'}:
                pointer=value['$rwReportRef']
                if not isinstance(pointer,str) or not pointer.startswith('#/'):
                    raise ValueError('Only same-record JSON pointers are supported')
                target=row
                for part in pointer[2:].split('/'):
                    part=part.replace('~1','/').replace('~0','~')
                    target=target[int(part)] if isinstance(target,list) else target[part]
                return visit(target,depth+1)
            return {key:visit(item,depth+1) for key,item in value.items()}
        if isinstance(value,list):
            return [visit(item,depth+1) for item in value]
        return value
    return visit(row)

def convert(source,target):
    source,target=Path(source),Path(target)
    opener=gzip.open if '.jsonl.gz' in source.name else open
    count=0
    with target.open('x',encoding='utf-8',newline='\n') as out:
        with opener(source,'rt',encoding='utf-8') as stream:
            for line in stream:
                out.write(json.dumps(expand(json.loads(line)),ensure_ascii=False,separators=(',',':'))+'\n')
                count+=1
    return count

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source');p.add_argument('target')
    a=p.parse_args()
    print('Expanded records:',convert(a.source,a.target))
