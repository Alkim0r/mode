"""Создаёт structure_set для импортированных данжей D&T (regnum:dt/<имя>).
Запуск из корня проекта: python3 tools/gen_dt_sets.py"""
import json, os

ROOT = os.path.join('src', 'main', 'resources', 'data', 'regnum', 'worldgen')
# имя: (spacing, separation, salt, frequency)
SETS = {
    'undead_crypt': (36, 18, 70101, 0.8),
    'creeping_crypt': (40, 20, 70102, 0.7),
    'toxic_lair': (44, 22, 70103, 0.8),
    'lone_citadel': (60, 30, 70104, 0.7),
    'desert_ruins': (40, 20, 70105, 0.8),
    'bunker': (44, 22, 70106, 0.7),
    'stray_fort': (50, 25, 70107, 0.7),
    'witch_villa': (46, 23, 70108, 0.6),
    'illager_camp': (36, 18, 70109, 0.7),
}


def main():
    sd = os.path.join(ROOT, 'structure', 'dt')
    out = os.path.join(ROOT, 'structure_set', 'dt')
    os.makedirs(out, exist_ok=True)
    for name, (sp, se, salt, fr) in SETS.items():
        if not os.path.exists(os.path.join(sd, name + '.json')):
            print('нет структуры', name)
            continue
        doc = {
            'structures': [{'structure': 'regnum:dt/' + name, 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': sp, 'separation': se, 'salt': salt,
                          'frequency': fr},
        }
        with open(os.path.join(out, name + '.json'), 'w') as f:
            json.dump(doc, f, indent=2)
        print('ok', name)


if __name__ == '__main__':
    main()
