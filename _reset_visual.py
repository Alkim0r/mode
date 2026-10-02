import shutil, os, glob
# свежая копия тестового мира и очистка старых скриншотов (перезапись, без удаления папок)
src = 'run-selftest/selftest'
dst = 'run-visual/saves/regnumtest'
for root, dirs, files in os.walk(src):
    rel = os.path.relpath(root, src)
    os.makedirs(os.path.join(dst, rel), exist_ok=True)
    for f in files:
        if f == 'session.lock':
            continue
        shutil.copyfile(os.path.join(root, f), os.path.join(dst, rel, f))
print("world reset")
