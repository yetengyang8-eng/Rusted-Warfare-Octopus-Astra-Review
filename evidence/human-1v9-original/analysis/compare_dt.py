import csv
for p in ["timeseries_1m.csv","test_dt096/samples.csv"]:
    print("FILE",p)
    with open(p,encoding="utf-8",errors="replace",newline="") as f:
        for r in csv.DictReader(f):
            if r.get("teamId")=="5" and r.get("gameTimeMs") in ("600000","1200000","1800000","2400000","3000000","4200000"):
                print(r["gameTimeMs"],r["heavyTank"],r["credits"],r["effectiveIncome"],r["landFactory"])
