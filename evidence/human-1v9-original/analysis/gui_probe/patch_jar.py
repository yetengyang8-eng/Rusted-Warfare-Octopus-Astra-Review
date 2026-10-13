import zipfile,hashlib,pathlib,os
base=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\gui_probe')
clone=pathlib.Path(r'G:\deepseek 工作台\_analysis\human_1v9_20261012\steam_exact_gui_clone')
source=clone/'generated_lib'/'game-lib.jar'
dest=base/'instrumented_game-lib.jar'
engine=(base/'engine_i_patched.class').read_bytes()
sampler=(base/'classes'/'analysis'/'ReplayProbeSampler.class').read_bytes()
target='com/corrodinggames/rts/game/i.class'
with zipfile.ZipFile(source,'r') as origin,zipfile.ZipFile(dest,'w') as out:
    names={x.filename for x in origin.infolist()}
    assert target in names,target
    assert 'analysis/ReplayProbeSampler.class' not in names
    for obj in origin.infolist():
        content=engine if obj.filename==target else origin.read(obj.filename)
        out.writestr(obj,content)
    out.writestr('analysis/ReplayProbeSampler.class',sampler,compress_type=zipfile.ZIP_DEFLATED)
with zipfile.ZipFile(dest) as f:
    bad=f.testzip()
    assert bad is None,bad
    assert f.read(target)==engine
    assert f.read('analysis/ReplayProbeSampler.class')==sampler
print('PATCHED_JAR',dest,'size',dest.stat().st_size,'sha',hashlib.sha256(dest.read_bytes()).hexdigest())
print('ORIGINAL_JAR',source,'size',source.stat().st_size,'sha',hashlib.sha256(source.read_bytes()).hexdigest())
