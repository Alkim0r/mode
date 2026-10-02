"""Импорт данжей Dungeons and Taverns (nova_structures) в нашу область: regnum:dt/...
python3 dt_import.py <dt.jar> <out_root> name1 name2 ...
Копирует структуру, пулы, процессоры, лут и схемы (замыкание по ссылкам), переписывая
«nova_structures:» -> «regnum:dt/». Теги копируются целиком по запросу (--tags)."""
import sys, os, re, json, zipfile
import tnbt

SRC = 'nova_structures'
OLD = SRC + ':'
NEW = 'regnum:dt/'
TAG_TYPES = ['worldgen/biome', 'worldgen/structure', 'block', 'item', 'entity_type', 'enchantment', 'damage_type', 'instrument',
             'loot_table', 'function', 'villager_trade', 'fluid', 'game_event', 'point_of_interest_type']
CANDS = [('worldgen/template_pool', '.json'), ('worldgen/processor_list', '.json'), ('loot_table', '.json'),
         ('structure', '.nbt'), ('worldgen/structure', '.json'),
         ('enchantment', '.json'), ('item_modifier', '.json')]
ENCHANTMENT_RU = {
    'antidote': 'Противоядие', 'boss_behaviour': 'Свойства стража',
    'conductivity_curse': 'Проклятие проводимости', 'ghasted': 'Дыхание гаста',
    'gravity': 'Тяжесть', 'illagers_bane': 'Бич разбойников', 'multishot': 'Тройной выстрел',
    'outreach': 'Дальнее взаимодействие', 'photosynthesis': 'Фотосинтез',
    'piercing': 'Сквозной удар', 'power': 'Мощь', 'shulker_boss': 'Свойства стража шалкеров',
    'shulker_miniboss': 'Свойства малого стража', 'traveler': 'Путешественник',
    'wax_wings': 'Восковые крылья', 'wither_coated': 'Иссушающий клинок',
}


def rw(s):
    return s.replace(OLD, NEW)


def main():
    jar, out = sys.argv[1], sys.argv[2]
    names = [a for a in sys.argv[3:] if not a.startswith('--')]
    z = zipfile.ZipFile(jar)
    have = set(z.namelist())
    done = set()
    queue = []
    support_only = '--support' in sys.argv
    candidates = [(d, ext) for d, ext in CANDS if d in ('enchantment', 'item_modifier')] if support_only else CANDS

    def add_ref(s):
        if not isinstance(s, str):
            return
        s = s.lstrip('#')
        if not s.startswith(OLD):
            return
        p = s[len(OLD):]
        for d, ext in candidates:
            f = 'data/%s/%s/%s%s' % (SRC, d, p, ext)
            if f in have and f not in done:
                done.add(f)
                queue.append((f, d, p, ext))

    for n in names:
        add_ref(OLD + n)
    if support_only:
        # Restore definitions referenced by already imported resources; never rewrite structures.
        from pathlib import Path
        for path in sorted((Path(out) / 'data' / 'regnum').rglob('*.json')):
            for match in re.findall(r'"#?regnum:dt/([^"\s]+)"', path.read_text(encoding='utf-8')):
                add_ref(OLD + match)
    stats = {'json': 0, 'nbt': 0}
    while queue:
        f, d, p, ext = queue.pop()
        raw = z.read(f)
        dst = os.path.join(out, 'data', 'regnum', d, 'dt', p + ext)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        if ext == '.json':
            txt = raw.decode('utf8')
            for m in re.findall(r'"#?%s([^"]+)"' % re.escape(OLD), txt):
                add_ref(OLD + m)
            if d == 'enchantment':
                data = json.loads(rw(txt))
                # These translation keys are not part of Regnum; retain a Russian literal title.
                if p not in ENCHANTMENT_RU:
                    raise ValueError('Missing Russian enchantment name: ' + p)
                data['description'] = {'text': ENCHANTMENT_RU[p]}
                txt = json.dumps(data, ensure_ascii=False, indent=2) + '\n'
            open(dst, 'w', encoding='utf8').write(rw(txt))
            stats['json'] += 1
        else:
            tag, name = tnbt.read(raw)
            for s in tnbt.strings(tag):
                if OLD in s:
                    add_ref(s[s.index(OLD):].split('"')[0].split(' ')[0])
            tag2 = tnbt.walk_strings(tag, rw)
            open(dst, 'wb').write(tnbt.write(tag2, name))
            stats['nbt'] += 1
    print('files', stats)
    if support_only:
        # D&T supplies this Minecraft-namespace tag itself; it is not vanilla
        # 1.21.1. Photosynthesis must stay exclusive with Mending after import.
        support_tag = 'data/minecraft/tags/enchantment/exclusive_set/repair.json'
        if support_tag not in have:
            raise ValueError('Source archive lacks Photosynthesis repair exclusivity tag')
        from pathlib import Path
        target = Path(out) / support_tag
        imported = json.loads(rw(z.read(support_tag).decode('utf-8')))
        if target.exists():
            existing = json.loads(target.read_text(encoding='utf-8'))
            for value in imported['values']:
                if value not in existing.setdefault('values', []):
                    existing['values'].append(value)
            imported = existing
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(imported, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print('repair exclusivity tag restored')
    if '--tags' in sys.argv:
        n = 0
        for f in have:
            pre = 'data/%s/tags/' % SRC
            if f.startswith(pre) and f.endswith('.json'):
                rel = f[len(pre):]
                typ = None
                for t in TAG_TYPES:
                    if rel.startswith(t + '/'):
                        typ = t
                        break
                if typ is None:
                    continue
                name = rel[len(typ) + 1:]
                dst = os.path.join(out, 'data', 'regnum', 'tags', *typ.split('/'), 'dt', *name.split('/'))
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                open(dst, 'w', encoding='utf8').write(rw(z.read(f).decode('utf8')))
                n += 1
        print('tags', n)


if __name__ == '__main__':
    main()
