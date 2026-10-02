import zipfile, os, sys
z = zipfile.ZipFile('_sync.zip'); n = 0
for info in z.infolist():
    if info.is_dir(): continue
    os.makedirs(os.path.dirname(info.filename) or '.', exist_ok=True)
    data = z.read(info)
    try:
        if open(info.filename, 'rb').read() == data: continue
    except FileNotFoundError:
        pass
    open(info.filename, 'wb').write(data); n += 1
print("updated", n)
for flag in sys.argv[1:]:
    open(flag, 'w').write('1')
