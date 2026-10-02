"""Validate imported enchantment tags and item-modifier references before starting Minecraft."""
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/data'
errors, checked = [], 0

def path_for(identifier, registry, tag=False):
    namespace, name = identifier.split(':', 1)
    return ROOT / namespace / ('tags/' + registry if tag else registry) / (name + '.json')

imported_tag_paths = list((ROOT / 'regnum/tags/enchantment/dt').rglob('*.json'))
repair_tag = ROOT / 'minecraft/tags/enchantment/exclusive_set/repair.json'
if repair_tag.exists():
    imported_tag_paths.append(repair_tag)
for path in sorted(imported_tag_paths):
    for value in json.loads(path.read_text(encoding='utf-8')).get('values', []):
        required = value.get('required', True) if isinstance(value, dict) else True
        key = value['id'] if isinstance(value, dict) else value
        tag = key.startswith('#')
        key = key.lstrip('#')
        if required and key.startswith('regnum:'):
            checked += 1
            if not path_for(key, 'enchantment', tag).is_file():
                errors.append(f'{path.relative_to(ROOT)}: missing {key}')

def inspect(value, path):
    global checked
    if isinstance(value, dict):
        exclusive = value.get('exclusive_set')
        if isinstance(exclusive, str) and exclusive.startswith('#'):
            key = exclusive[1:]
            if key.startswith('regnum:') or key == 'minecraft:exclusive_set/repair':
                checked += 1
                if not path_for(key, 'enchantment', True).is_file():
                    errors.append(f'{path.relative_to(ROOT)}: missing exclusive enchantment tag {key}')
        if value.get('function') == 'minecraft:reference':
            key = value['name']
            if key.startswith('regnum:'):
                checked += 1
                if not path_for(key, 'item_modifier').is_file():
                    errors.append(f'{path.relative_to(ROOT)}: missing modifier {key}')
        for child in value.values():
            inspect(child, path)
    elif isinstance(value, list):
        for child in value:
            inspect(child, path)

for registry in ('loot_table', 'item_modifier', 'enchantment'):
    for path in sorted((ROOT / 'regnum' / registry).rglob('*.json')):
        inspect(json.loads(path.read_text(encoding='utf-8')), path)
print(f'Imported required references: {checked}, missing: {len(errors)}')
for error in errors:
    print(error)
sys.exit(1 if errors else 0)
