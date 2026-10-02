#!/usr/bin/env python3
"""Generate a standalone, one-piece Nether Keep jigsaw template."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from tools.mc_convert import tnbt

OUT = ROOT / "src/main/resources/data/regnum/structure/dt/nether_keep/main_keep.nbt"
SIZE = (41, 22, 41)
blocks: dict[tuple[int, int, int], str] = {}


def put(x: int, y: int, z: int, block: str) -> None:
    if 0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2]:
        blocks[(x, y, z)] = block


def fill_box(x1: int, y1: int, z1: int, x2: int, y2: int, z2: int, block: str) -> None:
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                put(x, y, z, block)


def perimeter(x1: int, z1: int, x2: int, z2: int, y1: int, y2: int, block: str) -> None:
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            put(x, y, z1, block)
            put(x, y, z2, block)
    for z in range(z1 + 1, z2):
        for y in range(y1, y2 + 1):
            put(x1, y, z, block)
            put(x2, y, z, block)


# Continuous basalt footing and paved courtyard.
fill_box(0, 0, 0, 40, 0, 40, "minecraft:polished_blackstone_bricks")
for x in range(2, 39):
    for z in range(2, 39):
        if (x + z) % 9 == 0:
            put(x, 1, z, "minecraft:cracked_nether_bricks")
        elif x % 7 == 0 or z % 7 == 0:
            put(x, 1, z, "minecraft:polished_blackstone")
        else:
            put(x, 1, z, "minecraft:nether_bricks")

# Curtain wall, with a single front gate opening and a clear, wide approach.
perimeter(1, 1, 39, 39, 1, 7, "minecraft:nether_bricks")
for y in range(1, 5):
    for x in range(18, 23):
        put(x, y, 1, "minecraft:air")
for x in range(1, 40):
    if x % 2 == 0:
        put(x, 8, 1, "minecraft:nether_brick_fence")
        put(x, 8, 39, "minecraft:nether_brick_fence")
for z in range(2, 39):
    if z % 2 == 0:
        put(1, 8, z, "minecraft:nether_brick_fence")
        put(39, 8, z, "minecraft:nether_brick_fence")

# Gatehouse: layered arch, heavy flanking piers, and a readable crest.
fill_box(14, 6, 0, 26, 10, 2, "minecraft:polished_blackstone_bricks")
fill_box(15, 1, 0, 17, 8, 3, "minecraft:nether_bricks")
fill_box(23, 1, 0, 25, 8, 3, "minecraft:nether_bricks")
for y in range(1, 6):
    for x in range(18, 23):
        put(x, y, 1, "minecraft:air")
for x in range(16, 25):
    put(x, 9, 0, "minecraft:red_nether_bricks")
for x in range(17, 24):
    put(x, 10, 1, "minecraft:chiseled_polished_blackstone")
for x in (15, 25):
    for z in (0, 2, 4):
        put(x, 11, z, "minecraft:nether_brick_fence")

# Four squat watchtowers, each with a lit crown and crenellated roofline.
for cx, cz in ((4, 4), (36, 4), (4, 36), (36, 36)):
    for y in range(1, 12):
        for x in range(cx - 3, cx + 4):
            for z in range(cz - 3, cz + 4):
                edge = max(abs(x - cx), abs(z - cz)) == 3
                if edge or (y <= 2 and abs(x - cx) <= 2 and abs(z - cz) <= 2):
                    put(x, y, z, "minecraft:polished_blackstone_bricks" if y <= 2 else "minecraft:nether_bricks")
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            if max(abs(x - cx), abs(z - cz)) == 3 and (x + z) % 2 == 0:
                put(x, 12, z, "minecraft:nether_brick_fence")
    put(cx, 10, cz, "minecraft:soul_lantern")

# The inner hall forms a second defensive line, open towards the main approach.
perimeter(13, 13, 27, 29, 1, 10, "minecraft:nether_bricks")
for y in range(1, 5):
    for x in range(19, 22):
        put(x, y, 13, "minecraft:air")
fill_box(14, 5, 14, 26, 5, 28, "minecraft:polished_blackstone_bricks")
for x in range(14, 27, 4):
    put(x, 6, 13, "minecraft:chiseled_polished_blackstone")
    put(x, 6, 29, "minecraft:chiseled_polished_blackstone")
for z in range(14, 29, 4):
    put(13, 6, z, "minecraft:chiseled_polished_blackstone")
    put(27, 6, z, "minecraft:chiseled_polished_blackstone")
for x in range(13, 28):
    if x % 2 == 1:
        put(x, 11, 13, "minecraft:nether_brick_fence")
        put(x, 11, 29, "minecraft:nether_brick_fence")
for z in range(14, 29):
    if z % 2 == 1:
        put(13, 11, z, "minecraft:nether_brick_fence")
        put(27, 11, z, "minecraft:nether_brick_fence")

# Central beacon tower rises above the keep; open floors leave an interior path.
perimeter(17, 17, 23, 25, 2, 16, "minecraft:polished_blackstone_bricks")
fill_box(18, 7, 18, 22, 7, 24, "minecraft:nether_bricks")
for y in (5, 10, 15):
    for x in range(18, 23):
        for z in range(18, 25):
            put(x, y, z, "minecraft:polished_blackstone_bricks")
for x in range(17, 24):
    for z in range(17, 26):
        if x in (17, 23) or z in (17, 25):
            put(x, 17, z, "minecraft:nether_brick_fence")
# A three-wide entrance and ladder shaft make the tower traversable instead of a sealed prop.
for y in range(2, 5):
    for x in range(19, 22):
        put(x, y, 17, "minecraft:air")
for y in (5, 10, 15):
    put(19, y, 18, "minecraft:air")
for y in range(2, 16):
    put(19, y, 18, "minecraft:ladder[facing=south,waterlogged=false]")
put(20, 16, 21, "minecraft:soul_lantern")

# Corner braziers and a central chest give the courtyard a readable focal point.
for x, z in ((11, 11), (29, 11), (11, 31), (29, 31)):
    put(x, 1, z, "minecraft:polished_blackstone_bricks")
    put(x, 2, z, "minecraft:soul_lantern")
put(20, 2, 24, "minecraft:chest[facing=north,type=single,waterlogged=false]")

# Palette and block records use vanilla structure NBT; chest loot is generated at first opening.
palette: list[tuple[str, dict[str, str] | None]] = []
indices: dict[str, int] = {}
for block in sorted(set(blocks.values())):
    if "[" in block:
        name, raw = block[:-1].split("[", 1)
        props = dict(part.split("=", 1) for part in raw.split(","))
    else:
        name, props = block, None
    indices[block] = len(palette)
    palette.append((name, props))

palette_tags = []
for name, props in palette:
    state = {"Name": (8, name)}
    if props:
        state["Properties"] = (10, {key: (8, value) for key, value in sorted(props.items())})
    palette_tags.append(state)

records = []
for (x, y, z), block in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
    record = {"pos": (9, (3, [x, y, z])), "state": (3, indices[block])}
    if block.startswith("minecraft:chest["):
        record["nbt"] = (10, {
            "id": (8, "minecraft:chest"),
            "LootTable": (8, "regnum:dt/chests/nether_skeleton_tower/skeleton_tower_chest"),
            "LootTableSeed": (4, 0),
        })
    records.append(record)

root = (10, {
    "DataVersion": (3, 3688),
    "size": (9, (3, list(SIZE))),
    "palette": (9, (10, palette_tags)),
    "blocks": (9, (10, records)),
    "entities": (9, (10, [])),
})
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(tnbt.write(root))
print(f"Wrote {OUT.relative_to(ROOT)}: {len(records)} blocks, {len(palette)} palette states, size {SIZE}")
