import csv,gzip,pathlib,json,collections
base=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\steam_exact_gui_clone\replay_probe_output')
out=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
out.mkdir(exist_ok=True)
last_frame=687897 # real match summary endFrame
end_game_ms=11169962 # real match summary endGameMs
for name in ('units','players'):
    source=base/f'{name}_30s.csv'
    dest=out/f'{name}_valid_to_real_match_end.csv.gz'
    used=0;excluded=0;ts=set();states=collections.Counter()
    with source.open(encoding='utf-8',newline='') as fi,gzip.open(dest,'wt',encoding='utf-8',newline='',compresslevel=6) as fo:
        reader=csv.DictReader(fi)
        writer=csv.DictWriter(fo,fieldnames=reader.fieldnames)
        writer.writeheader()
        for row in reader:
            t=int(row['gameTimeMs']);fr=int(row['frame'])
            if t>end_game_ms or fr>last_frame:excluded+=1;continue
            writer.writerow(row);used+=1;ts.add(t)
            if name=='units':states[row['type']]+=1
    print(name,'included',used,'excluded_postgame',excluded,'snapshots',len(ts),'first_last_time_ms',min(ts),max(ts),'file',str(dest),'bytes',dest.stat().st_size)
