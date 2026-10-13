#!/usr/bin/env python3
"""Descriptive, offline R12 vs R16 monitor comparison. No game access needed.

Metrics are unit spatial snapshots, not native General-group assignments,
and cannot establish why a General chose to fight/retreat from a base.
"""
from pathlib import Path
import argparse
import json
import math
import statistics

def observe(path):
    rows = []
    with path.open(encoding="utf-8", errors="replace") as f:
        for line in f:
            try: r = json.loads(line)
            except ValueError: continue
            t = r.get("gameTimeMs")
            if not isinstance(t, (int, float)): continue
            ground = r.get("groundUnits", r.get("observedUnits", []))
            pts = []
            for u in ground:
                if u.get("type") != "heavyTank": continue
                x, y = u.get("x"), u.get("y")
                if isinstance(x, (int,float)) and isinstance(y, (int,float)):
                    pts.append((float(x), float(y)))
            if len(pts) > 1:
                cx = statistics.mean(x for x,y in pts)
                cy = statistics.mean(y for x,y in pts)
                radial = sorted(math.dist((x,y),(cx,cy)) for x,y in pts)
                spread = statistics.median(radial)
                # Local proximity is a descriptive metric only; not an attack-eligibility gate.
                nearest = statistics.median(min(math.dist(a,b) for j,b in enumerate(pts) if j!=i)
                                             for i,a in enumerate(pts))
            else: spread = nearest = None
            rows.append((float(t)/60000.0, len(pts), spread, nearest, r.get("frame")))
    return rows

def at(rows, minute):
    return min(rows, key=lambda x:abs(x[0]-minute)) if rows else None

def fmt(v):
    if v is None:return "NA"
    return "%.1f" % v if isinstance(v,float) else str(v)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--r12", type=Path, required=True, help="R12 monitor.jsonl from the cloud native run")
    p.add_argument("--r16", type=Path, required=True, help="R16 monitor.jsonl from the cloud native run")
    p.add_argument("--step-minutes", type=int, default=5)
    args=p.parse_args()
    a,b=observe(args.r12),observe(args.r16)
    if not a or not b: raise RuntimeError("No valid gameTimeMs snapshots. Check raw runner output.")
    print("minute,version,actual_minute,frame,heavy_tanks,median_distance_to_all_heavy_centroid,median_nearest_heavy")
    end=int(min(a[-1][0],b[-1][0]))
    for minute in range(0,end+1,args.step_minutes):
        for version,rows in (("R12",a),("R16",b)):
            row=at(rows,minute)
            actual,n,spread,nearest,frame=row
            print(",".join(map(str,(minute,version,fmt(actual),fmt(frame),fmt(n),fmt(spread),fmt(nearest)))))
    print("\nCAUTION: Distances quantify spatial dispersion only, and cannot prove coordinated behavior.")
    print("Use native General/transition traces and actual battle outcomes before diagnosing the cause.")

if __name__ == "__main__":main()
