"""Конвертер MineColonies .blueprint (1.20.1) -> ванильный structure NBT (1.21.1).
Структура: слой 0 — уровень земли, слой 1+ — здание. structurize-подстановки отбрасываются
(земля остаётся мировой), domum_ornamentum заменяется ванильными блоками по textureData."""
import json
import nbt
from nbt import Int, Str, LIST, COMP

WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']


def wood_of(n):
    n = n.replace('stripped_', '')
    for suf in ('_planks', '_log', '_wood', '_stem', '_hyphae', '_block'):
        if n.endswith(suf):
            w = n[:-len(suf)]
            if w in WOODS:
                return w
    return None


STONE = {
    'stone': ('stone_stairs', 'stone_slab', 'stone_brick_wall'),
    'cobblestone': ('cobblestone_stairs', 'cobblestone_slab', 'cobblestone_wall'),
    'mossy_cobblestone': ('mossy_cobblestone_stairs', 'mossy_cobblestone_slab', 'mossy_cobblestone_wall'),
    'stone_bricks': ('stone_brick_stairs', 'stone_brick_slab', 'stone_brick_wall'),
    'mossy_stone_bricks': ('mossy_stone_brick_stairs', 'mossy_stone_brick_slab', 'mossy_stone_brick_wall'),
    'bricks': ('brick_stairs', 'brick_slab', 'brick_wall'),
    'sandstone': ('sandstone_stairs', 'sandstone_slab', 'sandstone_wall'),
    'smooth_sandstone': ('smooth_sandstone_stairs', 'smooth_sandstone_slab', 'sandstone_wall'),
    'cut_sandstone': ('sandstone_stairs', 'cut_sandstone_slab', 'sandstone_wall'),
    'chiseled_sandstone': ('sandstone_stairs', 'sandstone_slab', 'sandstone_wall'),
    'red_sandstone': ('red_sandstone_stairs', 'red_sandstone_slab', 'red_sandstone_wall'),
    'smooth_red_sandstone': ('smooth_red_sandstone_stairs', 'smooth_red_sandstone_slab', 'red_sandstone_wall'),
    'cut_red_sandstone': ('red_sandstone_stairs', 'cut_red_sandstone_slab', 'red_sandstone_wall'),
    'chiseled_red_sandstone': ('red_sandstone_stairs', 'red_sandstone_slab', 'red_sandstone_wall'),
    'quartz_block': ('quartz_stairs', 'quartz_slab', 'stone_brick_wall'),
    'smooth_quartz': ('smooth_quartz_stairs', 'smooth_quartz_slab', 'stone_brick_wall'),
    'quartz_pillar': ('quartz_stairs', 'quartz_slab', 'stone_brick_wall'),
    'chiseled_quartz_block': ('quartz_stairs', 'quartz_slab', 'stone_brick_wall'),
    'quartz_bricks': ('quartz_stairs', 'quartz_slab', 'stone_brick_wall'),
    'andesite': ('andesite_stairs', 'andesite_slab', 'andesite_wall'),
    'polished_andesite': ('polished_andesite_stairs', 'polished_andesite_slab', 'andesite_wall'),
    'diorite': ('diorite_stairs', 'diorite_slab', 'diorite_wall'),
    'polished_diorite': ('polished_diorite_stairs', 'polished_diorite_slab', 'diorite_wall'),
    'granite': ('granite_stairs', 'granite_slab', 'granite_wall'),
    'polished_granite': ('polished_granite_stairs', 'polished_granite_slab', 'granite_wall'),
    'deepslate': ('cobbled_deepslate_stairs', 'cobbled_deepslate_slab', 'cobbled_deepslate_wall'),
    'cobbled_deepslate': ('cobbled_deepslate_stairs', 'cobbled_deepslate_slab', 'cobbled_deepslate_wall'),
    'polished_deepslate': ('polished_deepslate_stairs', 'polished_deepslate_slab', 'polished_deepslate_wall'),
    'deepslate_bricks': ('deepslate_brick_stairs', 'deepslate_brick_slab', 'deepslate_brick_wall'),
    'deepslate_tiles': ('deepslate_tile_stairs', 'deepslate_tile_slab', 'deepslate_tile_wall'),
    'blackstone': ('blackstone_stairs', 'blackstone_slab', 'blackstone_wall'),
    'polished_blackstone': ('polished_blackstone_stairs', 'polished_blackstone_slab', 'polished_blackstone_wall'),
    'polished_blackstone_bricks': ('polished_blackstone_brick_stairs', 'polished_blackstone_brick_slab', 'polished_blackstone_brick_wall'),
    'nether_bricks': ('nether_brick_stairs', 'nether_brick_slab', 'nether_brick_wall'),
    'red_nether_bricks': ('red_nether_brick_stairs', 'red_nether_brick_slab', 'red_nether_brick_wall'),
    'end_stone_bricks': ('end_stone_brick_stairs', 'end_stone_brick_slab', 'end_stone_brick_wall'),
    'mud_bricks': ('mud_brick_stairs', 'mud_brick_slab', 'mud_brick_wall'),
    'prismarine': ('prismarine_stairs', 'prismarine_slab', 'prismarine_wall'),
    'prismarine_bricks': ('prismarine_brick_stairs', 'prismarine_brick_slab', 'prismarine_wall'),
    'cut_copper': ('cut_copper_stairs', 'cut_copper_slab', 'stone_brick_wall'),
    'smooth_stone': ('stone_stairs', 'smooth_stone_slab', 'stone_brick_wall'),
}
DOM_BLOCK = {
    'beige_bricks': 'smooth_sandstone', 'beige_stone_bricks': 'cut_sandstone', 'sand_bricks': 'sandstone',
    'sand_stone_bricks': 'cut_sandstone', 'cream_bricks': 'end_stone_bricks', 'cream_stone_bricks': 'end_stone_bricks',
    'roan_bricks': 'stone_bricks', 'roan_stone_bricks': 'stone_bricks', 'brown_bricks': 'mud_bricks',
    'brown_stone_bricks': 'mud_bricks', 'paper_extra': 'calcite', 'white_paper_extra': 'calcite',
    'brick_extra': 'bricks', 'red_brick_extra': 'bricks', 'orange_brick_extra': 'bricks',
    'light_blue_brick_extra': 'prismarine_bricks', 'blue_cobblestone_extra': 'deepslate_tiles',
    'purple_cobblestone_extra': 'deepslate_tiles', 'cobblestone_extra': 'cobblestone',
    'wheat_extra': 'oak_planks', 'cactus_extra': 'jungle_planks',
}


def mat(v):
    if v is None:
        return 'stone_bricks'
    ns, _, n = v.partition(':')
    if ns == 'domum_ornamentum':
        return DOM_BLOCK.get(n, 'stone_bricks')
    return n


def derive(base, kind):
    w = wood_of(base)
    if w:
        if w in ('crimson', 'warped') and kind == 'log':
            return base
        return {'block': f'{w}_planks', 'stairs': f'{w}_stairs', 'slab': f'{w}_slab', 'fence': f'{w}_fence',
                'fence_gate': f'{w}_fence_gate', 'door': f'{w}_door', 'trapdoor': f'{w}_trapdoor',
                'wall': f'{w}_fence', 'log': f'{w}_log'}[kind]
    if base in STONE:
        s, sl, wl = STONE[base]
        if kind in ('block', 'log'): return base
        if kind == 'stairs': return s
        if kind == 'slab': return sl
        if kind == 'wall': return wl
    if kind in ('block', 'log'):
        return base
    s, sl, wl = STONE['stone_bricks']
    return {'stairs': s, 'slab': sl, 'wall': wl, 'fence': 'spruce_fence', 'fence_gate': 'spruce_fence_gate',
            'door': 'spruce_door', 'trapdoor': 'spruce_trapdoor'}[kind]


KEEP = {
    'stairs': ('facing', 'half', 'shape', 'waterlogged'),
    'slab': ('type', 'waterlogged'),
    'wall': ('east', 'north', 'south', 'west', 'up', 'waterlogged'),
    'fence': ('east', 'north', 'south', 'west', 'waterlogged'),
    'fence_gate': ('facing', 'in_wall', 'open', 'powered'),
    'door': ('facing', 'half', 'hinge', 'open', 'powered'),
    'trapdoor': ('facing', 'half', 'open', 'powered', 'waterlogged'),
}


def sub(props, kind):
    return {k: v for k, v in props.items() if k in KEEP.get(kind, ())}


def axis_of(props):
    f = props.get('facing') or props.get('column')
    if f in ('east', 'west'): return 'x'
    if f in ('north', 'south'): return 'z'
    return 'y'


HUT_DROP = ('blockwaypoint', 'blockstash', 'blockpostbox', 'decorationcontroller', 'blockconstructiontape',
            'colony_banner', 'colony_wall_banner', 'blockminecoloniesnamedgrave', 'mediumquarry', 'simplequarry')


def conv(name, props, tex):
    """-> (name, props) | 'HUT' | None(omit)"""
    ns, _, n = name.partition(':')
    if ns == 'minecraft':
        if n == 'grass': return 'minecraft:short_grass', props
        if n == 'air': return None
        return name, props
    if ns == 'structurize':
        return None
    if ns == 'minecolonies':
        if n.startswith('blockhut'): return 'HUT'
        if n in HUT_DROP: return None
        if n in ('blockminecoloniesrack', 'barrel_block'):
            return 'minecraft:barrel', {'facing': props.get('facing', 'north'), 'open': 'false'}
        if n == 'composted_dirt': return 'minecraft:coarse_dirt', {}
        if n == 'gate_iron': return 'minecraft:iron_bars', {}
        if n == 'gate_wood': return 'minecraft:spruce_fence', {}
        return None
    if ns == 'domum_ornamentum':
        t = tex or {}
        main = mat(t.get('minecraft:block/oak_planks'))
        dark = mat(t['minecraft:block/dark_oak_planks']) if 'minecraft:block/dark_oak_planks' in t else None
        clay = mat(t['minecraft:block/clay']) if 'minecraft:block/clay' in t else None
        glow = mat(t['minecraft:block/glowstone']) if 'minecraft:block/glowstone' in t else 'glowstone'
        if n.endswith('_floating_carpet'):
            return 'minecraft:' + n[:-len('_floating_carpet')] + '_carpet', {}
        if n in DOM_BLOCK: return 'minecraft:' + DOM_BLOCK[n], {}
        if n in ('vanilla_stairs_compat', 'dark_brick_stair'):
            return 'minecraft:' + derive(main, 'stairs'), sub(props, 'stairs')
        if n in ('shingle', 'shingle_flat', 'shingle_flat_lower'):
            return 'minecraft:' + derive(clay or main, 'stairs'), sub(props, 'stairs')
        if n == 'shingle_slab':
            return 'minecraft:' + derive(clay or main, 'slab'), {'type': 'bottom'}
        if n == 'vanilla_slab_compat':
            return 'minecraft:' + derive(main, 'slab'), sub(props, 'slab')
        if n == 'vanilla_wall_compat':
            r = derive(main, 'wall')
            return 'minecraft:' + r, sub(props, 'fence' if r.endswith('_fence') else 'wall')
        if n == 'vanilla_fence_compat':
            return 'minecraft:' + derive(main, 'fence'), sub(props, 'fence')
        if n == 'vanilla_fence_gate_compat':
            return 'minecraft:' + derive(main, 'fence_gate'), sub(props, 'fence_gate')
        if n in ('vanilla_doors_compat', 'fancy_door'):
            return 'minecraft:' + derive(main, 'door'), sub(props, 'door')
        if n in ('vanilla_trapdoors_compat', 'fancy_trapdoors', 'panel'):
            return 'minecraft:' + derive(main, 'trapdoor'), sub(props, 'trapdoor')
        if n == 'post':
            r = derive(main, 'log')
            return 'minecraft:' + r, ({'axis': axis_of(props)} if (wood_of(main) or 'pillar' in r or r.endswith('_log')) else {})
        if n in ('squarepillar', 'blockpillar', 'blockypillar'):
            r = derive(main, 'log')
            return 'minecraft:' + r, ({'axis': 'y'} if ('log' in r or 'pillar' in r or r.endswith('_wood')) else {})
        if n in ('framed', 'double_crossed', 'one_crossed_lr', 'one_crossed_rl', 'side_framed',
                 'side_framed_horizontal', 'up_gated', 'down_gated'):
            return 'minecraft:' + derive(dark or main, 'block'), {}
        if n in ('plain', 'horizontal_plain'):
            return 'minecraft:' + derive(main, 'block'), {}
        if n in ('center_light', 'fancy_light', 'four_light', 'framed_light', 'crossed_light'):
            return 'minecraft:' + glow, {}
        if n == 'blockpaperwall':
            return 'minecraft:white_stained_glass_pane', {k: v for k, v in props.items() if k in ('east', 'north', 'south', 'west', 'waterlogged')}
        if n in ('blockbarreldeco_standing', 'blockbarreldeco_onside'):
            return 'minecraft:barrel', {'facing': 'up' if n.endswith('standing') else props.get('facing', 'north'), 'open': 'false'}
        if n == 'architectscutter':
            return 'minecraft:stonecutter', {'facing': props.get('facing', 'north')}
        return 'minecraft:' + derive(main, 'block'), {}
    return None


def convert_one(z, path):
    d = nbt.read(z.read(path))
    sx, sy, sz = d['size_x'], d['size_y'], d['size_z']
    raw = []
    for v in d['blocks']:
        v &= 0xffffffff
        raw += [(v >> 16) & 0xffff, v & 0xffff]
    pal = d['palette']
    texmap = {}
    for te in d['tile_entities']:
        if isinstance(te, dict) and 'textureData' in te and 'x' in te:
            texmap[(te['x'], te['y'], te['z'])] = te['textureData']
    cache = {}
    blocks = {}
    huts = []
    for y in range(sy):
        for z_ in range(sz):
            for x in range(sx):
                i = raw[(y * sz + z_) * sx + x]
                q = pal[i]
                if q['Name'] == 'minecraft:air':
                    continue  # воздух не пишем: объём перед постановкой чистит Prefab
                tex = texmap.get((x, y, z_))
                key = (i, json.dumps(tex, sort_keys=True))
                if key not in cache:
                    cache[key] = conv(q['Name'], q.get('Properties', {}), tex)
                r = cache[key]
                if r is None:
                    continue
                if r == 'HUT':
                    huts.append((x, y, z_))
                    continue
                blocks[(x, y, z_)] = r
    for (x, y, z_) in huts:
        b = blocks.get((x, y - 1, z_))
        ok = b and not any(s in b[0] for s in ('stairs', 'slab', 'door', 'trapdoor', 'fence'))
        blocks[(x, y, z_)] = b if ok else ('minecraft:stone_bricks', {})
    solid = [k for k, v in blocks.items() if v[0] != 'minecraft:air']
    if not solid:
        return None
    maxy = max(k[1] for k in solid)
    blocks = {k: v for k, v in blocks.items() if k[1] <= maxy}
    palette, pidx, out = [], {}, []
    for (x, y, z_), (n, p) in sorted(blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        k = (n, json.dumps(p, sort_keys=True))
        if k not in pidx:
            pidx[k] = len(palette)
            e = {'Name': Str(n)}
            if p:
                e['Properties'] = COMP({a: Str(str(b)) for a, b in p.items()})
            palette.append(COMP(e))
        out.append(COMP({'pos': LIST([Int(x), Int(y), Int(z_)], 3), 'state': Int(pidx[k])}))
    root = COMP({'DataVersion': Int(3955), 'size': LIST([Int(sx), Int(maxy + 1), Int(sz)], 3),
                 'palette': LIST(palette, 10), 'blocks': LIST(out, 10), 'entities': LIST([], 10)})
    return nbt.write(root), (sx, maxy + 1, sz), len(solid)
