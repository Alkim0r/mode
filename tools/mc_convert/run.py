"""Запуск: python3 run.py <minecolonies.jar> <out_dir> [culture...]
Кладёт data/regnum/structure/<culture>/<категория>/<имя>.nbt и index.json."""
import os, re, sys, json, zipfile
import convert

# культура -> [(стиль MineColonies, [категории])]
CULTURES = {
    'north': [('nordic', ['walls', 'military', 'fundamentals', 'craftsmanship', 'education', 'mystic', 'decorations'])],
    'empire': [('ancientathens', ['walls', 'military', 'fundamentals', 'craftsmanship', 'education', 'mystic', 'decorations'])],
    'west': [('fortress', ['walls', 'military']), ('medievaloak', ['fundamentals', 'craftsmanship', 'education', 'mystic', 'decorations'])],
    'steppe': [('pagoda', ['walls', 'military', 'fundamentals', 'craftsmanship', 'education', 'mystic', 'decorations'])],
    'sultanate': [('sandstone', ['walls', 'military', 'fundamentals', 'craftsmanship', 'education', 'mystic', 'decorations'])],
    'clans': [('caledonia', ['walls', 'military', 'fundamentals', 'craftsmanship', 'education', 'mystic', 'decorations'])],
}
LEVELS_KEEP = ('1', '3', '5', '')  # из пяти уровней оставляем 1/3/5 (остальные — почти копии)


def keep(name):
    m = re.search(r'(\d+)$', name)
    return (m is None) or (m.group(1) in LEVELS_KEEP)


def main():
    jar, out = sys.argv[1], sys.argv[2]
    only = sys.argv[3:] or list(CULTURES)
    z = zipfile.ZipFile(jar)
    names = [n for n in z.namelist() if n.endswith('.blueprint')]
    index = {}
    total = 0
    for cul in only:
        for style, cats in CULTURES[cul]:
            pref = 'blueprints/minecolonies/%s/' % style
            for n in names:
                if not n.startswith(pref):
                    continue
                rel = n[len(pref):-len('.blueprint')]
                parts = rel.split('/')
                cat = parts[0]
                if cat not in cats:
                    continue
                base = parts[-1]
                if not keep(base):
                    continue
                # плоское имя: подпапки внутри категории склеиваем через "_"
                flat = '_'.join(parts[1:]).lower()
                flat = re.sub(r'[^a-z0-9_]', '_', flat)
                try:
                    r = convert.convert_one(z, n)
                except Exception as e:
                    print('FAIL', n, e)
                    continue
                if r is None:
                    continue
                data, size, cnt = r
                if cnt < 20:
                    continue
                d = os.path.join(out, 'data', 'regnum', 'structure', cul, cat)
                os.makedirs(d, exist_ok=True)
                with open(os.path.join(d, flat + '.nbt'), 'wb') as f:
                    f.write(data)
                index['%s/%s/%s' % (cul, cat, flat)] = {'size': list(size), 'blocks': cnt, 'src': n}
                total += len(data)
        print(cul, 'ok', len([k for k in index if k.startswith(cul + '/')]))
    with open(os.path.join(out, 'structure_index.json'), 'w') as f:
        json.dump(index, f, indent=0)
    print('files', len(index), 'bytes', total)


if __name__ == '__main__':
    main()
