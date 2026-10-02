"""Inspect downloaded assets without executing any third-party mod code."""
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TARGETS = ('wroughtnaut', 'frostmaw', 'ignis', 'monstrosity', 'ender_guardian',
           'void_blossom', 'night_lich')
catalog = []
for jar in sorted((ROOT / '_refs').glob('*.jar')):
    entry = dict(jar=jar.name, animation_files=[], geometry_files=[], java_animation_classes=[])
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            relevant = any(target in name.lower() for target in TARGETS)
            if relevant and name.endswith('.json') and ('animation' in name or '/geo/' in name):
                asset = json.loads(archive.read(name))
                if 'animations' in asset:
                    clips = []
                    for clip_name, clip in asset['animations'].items():
                        clips.append(dict(name=clip_name, seconds=clip.get('animation_length'),
                            loop=clip.get('loop', False), bones=len(clip.get('bones', {}))))
                    entry['animation_files'].append(dict(path=name, clips=clips))
                elif 'minecraft:geometry' in asset:
                    entry['geometry_files'].append(dict(path=name, geometries=[
                        dict(bones=len(geo.get('bones', [])), cubes=sum(len(b.get('cubes', []))
                            for b in geo.get('bones', []))) for geo in asset['minecraft:geometry']]))
            if relevant and name.endswith('.class') and '/client/' in name and any(word in name.lower() for word in ('model', 'animation')):
                entry['java_animation_classes'].append(name)
    catalog.append(entry)
(ROOT / '_refs' / 'ANIMATION_CATALOG.json').write_text(
    json.dumps(catalog, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
for mod in catalog:
    print(mod['jar'], 'JSON animations:', len(mod['animation_files']),
          'geometry:', len(mod['geometry_files']), 'Java models/animation classes:', len(mod['java_animation_classes']))
    for file in mod['animation_files']:
        print(' ', file['path'], 'clips:', len(file['clips']),
              'examples:', ', '.join(c['name'] for c in file['clips'][:8]))

raw_log = (ROOT / 'coord' / 'battle-dodge-v2-live.log').read_bytes()
if raw_log.startswith((b'\xff\xfe', b'\xfe\xff')):
    log = raw_log.decode('utf-16')
else:
    try:
        log = raw_log.decode('utf-8-sig')
    except UnicodeDecodeError:
        log = raw_log.decode('cp1251')
events = []
for line in log.splitlines():
    if '[REGNUM-DODGE] STOP' in line:
        event = dict(re.findall(r'(\w+)=([^ ]+)', line))
        events.append(event)
summary = dict(stops=len(events),
    alive_stops_before_impact=sum(e.get('alive') == 'true' and int(e['remaining']) > 0 for e in events),
    alive_safe_after_impact=sum(e.get('alive') == 'true' and e.get('safe') == 'true'
        and int(e['remaining']) <= 0 for e in events),
    unsafe_stops=sum(e.get('safe') == 'false' for e in events),
    deaths_during_dodge=sum(e.get('alive') == 'false' for e in events),
    orders_lost=sum(e.get('orderKept') != 'true' for e in events),
    targets_lost=sum(e.get('targetKept') != 'true' for e in events),
    arena_escapes=log.count('[REGNUM-BATTLE] ARENA ESCAPE'),
    round_results=re.findall(r'\[REGNUM-BATTLE\] END[^\r\n]+', log),
    limit='Live sample only; safe uses center position against the tracked strike, not all overlapping zones.')
(ROOT / 'coord' / 'C10_DODGE_V2_QA.json').write_text(
    json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False))
