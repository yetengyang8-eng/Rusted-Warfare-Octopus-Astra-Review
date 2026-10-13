import zipfile
a=r'G:\Steam\steamapps\common\Rusted Warfare\game-lib.jar'
b=r'G:\Steam\steamapps\common\Rusted Warfare\generated_lib\game-lib.jar'
with zipfile.ZipFile(a) as f1,zipfile.ZipFile(b) as f2:
 x={n.filename:(n.CRC,n.file_size) for n in f1.infolist()}
 y={n.filename:(n.CRC,n.file_size) for n in f2.infolist()}
 print('original',len(x),'generated',len(y))
 print('added',len(y.keys()-x.keys()),[k for k in y if k not in x][:40])
 print('removed',len(x.keys()-y.keys()),[k for k in x if k not in y][:40])
 diff=[k for k in x.keys()&y.keys() if x[k]!=y[k]]
 print('modified',len(diff),sorted(diff)[:100])
