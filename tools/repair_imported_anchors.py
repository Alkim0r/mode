"""Attach the entity-only anchor processor to existing imported D&T pools; keep block processors."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/data/regnum/worldgen'
PROCESSOR = {'processor_type': 'regnum:imported_anchor'}
LIST_ID = 'regnum:dt/imported_anchors'

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def wire(value):
    changes = 0
    if isinstance(value, dict):
        if value.get('processors') == 'minecraft:empty' and str(value.get('location', '')).startswith('regnum:dt/'):
            value['processors'] = LIST_ID
            changes += 1
        for child in value.values():
            changes += wire(child)
    elif isinstance(value, list):
        for child in value:
            changes += wire(child)
    return changes

if __name__ == '__main__':
    lists = 0
    for path in (ROOT / 'processor_list/dt').rglob('*.json'):
        value = json.loads(path.read_text(encoding='utf-8'))
        processors = value if isinstance(value, list) else value['processors']
        if not any(p.get('processor_type') == PROCESSOR['processor_type'] for p in processors):
            processors.append(PROCESSOR.copy())
            write(path, value)
            lists += 1
    write(ROOT / 'processor_list/dt/imported_anchors.json', {'processors': [PROCESSOR]})
    pools = 0
    for path in (ROOT / 'template_pool/dt').rglob('*.json'):
        value = json.loads(path.read_text(encoding='utf-8'))
        if wire(value):
            write(path, value)
            pools += 1
    print(f'Anchor processor lists changed={lists}, pool files changed={pools}')
