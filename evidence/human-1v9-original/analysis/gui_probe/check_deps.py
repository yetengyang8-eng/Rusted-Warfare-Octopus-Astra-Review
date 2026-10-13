import importlib.util
for m in ['numpy','scipy','matplotlib','pandas']:
 print(m,bool(importlib.util.find_spec(m)))
