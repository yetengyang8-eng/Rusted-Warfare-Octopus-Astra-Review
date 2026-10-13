import zipfile
jar=r'G:\Steam\steamapps\common\Rusted Warfare\rw-camera-follow\AssistFramework_v0.7.4-alpha1.jar'
refs=[]
with zipfile.ZipFile(jar) as z:
 for x in z.namelist():
  if not x.endswith('.class'):continue
  d=z.read(x)
  if b'rw/camerafollow/MatchTelemetry' in d and b'assist' in d:
   refs.append(x)
print('CLASS REFERENCES of MatchTelemetry + assist',len(refs))
for x in refs:print(x)
