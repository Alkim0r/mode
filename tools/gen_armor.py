"""Броня культур: иконки, надетые слои (64x32), модели, язык, рецепты. Запуск из корня: python3 tools/gen_armor.py"""
import json, os, random
from PIL import Image

A = "src/main/resources/assets/regnum"
D = "src/main/resources/data/regnum"
K = (24, 22, 28, 255)


def hexc(h):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


HELMET = ["................", "................", "................", "....########....", "...##########...", "..############..",
          "..############..", "..############..", "..############..", "..tttttttttttt..", "..##........##..", "..##........##..",
          "..##........##..", "................", "................", "................"]
CHEST = ["................", "..###......###..", ".#####....#####.", ".##############.", ".##############.", "..############..",
         "..#####tt#####..", "..############..", "..############..", "..tttttttttttt..", "..############..", "...##########...",
         "...##########...", "................", "................", "................"]
LEGS = ["................", "................", "..tttttttttttt..", "..############..", "..############..", "..#####..#####..",
        "..#####..#####..", "..#####..#####..", "..#####..#####..", "..#####..#####..", "..####....####..", "..####....####..",
        "..tttt....tttt..", "................", "................", "................"]
BOOTS = ["................"] * 6 + ["..####....####..", "..####....####..", "..####....####..", "..####....####..", ".#####....#####.",
                                  ".######..######.", ".tttttt..tttttt.", "................", "................", "................"]

CULT = {
    "north": dict(base='#8c97a3', trim='#b08a50', acc='#7a1a1a',
                  names=["Шишак дружинника", "Чешуйчатый доспех", "Поножи дружинника", "Сапоги дружинника"]),
    "empire": dict(base='#b8bcc4', trim='#d9a62b', acc='#a8231f',
                   names=["Галеа легионера", "Сегментата", "Поножи легионера", "Калиги"]),
    "west": dict(base='#9aa0aa', trim='#4a4a52', acc='#2a4aa8',
                 names=["Топфхельм", "Латный нагрудник", "Латные поножи", "Латные сапоги"]),
    "steppe": dict(base='#6a5a3a', trim='#c0873a', acc='#2a8a8a',
                   names=["Остроконечный шлем", "Ламеллярный панцирь", "Кожаные поножи", "Сапоги всадника"]),
    "sultanate": dict(base='#c8b078', trim='#d0a030', acc='#2a6a9a',
                      names=["Тюрбан с бармицей", "Кольчужный халат", "Шаровары с наколенниками", "Сапоги гулама"]),
    "clans": dict(base='#5a6a3a', trim='#3a4a8a', acc='#e8e8e8',
                  names=["Кожаный шлем клана", "Кожаная куртка", "Килт воина", "Сапоги охотника"]),
}
DEF = {"north": 1, "empire": 1, "west": 1, "steppe": 1, "sultanate": 1, "clans": 1}
PARTS = ["helmet", "chestplate", "leggings", "boots"]
SHAPES = [HELMET, CHEST, LEGS, BOOTS]


def outline(img):
    src = img.copy()
    for x in range(16):
        for y in range(16):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] > 0 and src.getpixel((nx, ny)) != K:
                        img.putpixel((x, y), K); break
    return img


def icon(cid, part):
    c = CULT[cid]; base, trim, acc = hexc(c['base']), hexc(c['trim']), hexc(c['acc'])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); r = random.Random(sum(map(ord, cid + part)))
    for y, row in enumerate(SHAPES[PARTS.index(part)]):
        for x, ch in enumerate(row):
            if ch == '.': continue
            f = 1.12 - 0.3 * ((x + y) / 30) + r.uniform(-0.05, 0.05)
            img.putpixel((x, y), shade(base if ch == '#' else trim, f))
    px = img.putpixel
    if part == "helmet":
        if cid == "north":
            for p in ((7, 2), (8, 2), (7, 1), (8, 1), (7, 0)): px(p, trim)
        elif cid == "empire":
            for x in range(4, 12): px((x, 2), acc); px((x, 3), acc)
        elif cid == "west":
            for x in range(4, 12): px((x, 7), shade(base, 0.35))
            px((7, 6), shade(base, 0.35)); px((8, 6), shade(base, 0.35))
        elif cid == "steppe":
            for p in ((7, 2), (8, 2), (7, 1), (8, 1), (7, 0), (7, 3)): px(p, base)
            px((7, 0), acc)
        elif cid == "sultanate":
            for y in range(3, 9):
                for x in range(2, 14):
                    if img.getpixel((x, y))[3]: px((x, y), shade(hexc('#e8e0cc'), 1.05 - 0.05 * ((x + y) % 3)))
            for x in range(3, 13): px((x, 9), acc)
        elif cid == "clans":
            px((3, 4), (230, 230, 230, 255)); px((2, 3), (230, 230, 230, 255)); px((12, 4), (230, 230, 230, 255)); px((13, 3), (230, 230, 230, 255))
            for x in range(3, 13): px((x, 6), acc if x % 2 else trim)
    if part == "chestplate" and cid in ("empire", "west", "north"):
        for y in (6, 7): px((7, y), acc); px((8, y), acc)
    if part == "chestplate" and cid == "clans":
        for x in range(3, 13): px((x, 4), trim if x % 2 else base)
    return outline(img)


def worn(cid, layer):
    c = CULT[cid]; base, trim, acc = hexc(c['base']), hexc(c['trim']), hexc(c['acc'])
    img = Image.new("RGBA", (64, 32)); r = random.Random(7 + layer)
    for x in range(64):
        for y in range(32):
            f = 1.0 + r.uniform(-0.04, 0.04); col = base
            if cid == "north":
                ox = (y // 3 % 2) * 2; ix, iy = (x + ox) % 4, y % 3
                f *= 1.12 if iy == 0 else 0.82 if iy == 2 else 1.0
                if ix == 0: f *= 0.86
            elif cid == "empire":
                f *= 1.12 if y % 4 == 0 else 0.78 if y % 4 == 3 else 1.0
                if y % 8 == 3 and x % 5 == 0: col = trim
            elif cid == "west":
                if x % 8 == 0 or y % 8 == 0: f *= 0.8
                if x % 8 == 4 and y % 8 == 4: col = trim
                if (x // 8 + y // 8) % 2: f *= 1.06
            elif cid == "steppe":
                ix, iy = x % 3, y % 5
                if ix == 2 or iy == 4: f *= 0.72
                if iy == 0: col = trim
            elif cid == "sultanate":
                f *= 1.1 if (x + y) % 2 == 0 else 0.85
                if y % 11 == 0: col = acc
            else:
                f *= 1.08 if (x + y) % 3 == 0 else 0.92
                if y % 9 == 4: col = trim
                if y % 9 == 5: col = acc
            if layer == 2 and y % 16 == 15: f *= 0.7
            img.putpixel((x, y), shade(col, f))
    return img


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, ensure_ascii=False, indent=2)


TRIM = {"north": "minecraft:leather", "empire": "minecraft:gold_ingot", "west": "minecraft:chain",
        "steppe": "minecraft:rabbit_hide", "sultanate": "minecraft:copper_ingot", "clans": "minecraft:white_wool"}
PATTERNS = {"helmet": ["MMM", "MTM"], "chestplate": ["MTM", "MMM", "MMM"], "leggings": ["MMM", "MTM", "M M"], "boots": ["MTM", "M M"]}


def main():
    os.makedirs(f"{A}/textures/item", exist_ok=True)
    os.makedirs(f"{A}/textures/models/armor", exist_ok=True)
    lang = {}
    for cid, c in CULT.items():
        for part, title in zip(PARTS, c["names"]):
            icon(cid, part).save(f"{A}/textures/item/{cid}_{part}.png")
            wj(f"{A}/models/item/{cid}_{part}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"regnum:item/{cid}_{part}"}})
            lang[f"item.regnum.{cid}_{part}"] = title
            wj(f"{D}/recipe/{cid}_{part}.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": PATTERNS[part],
                                                 "key": {"M": {"item": "minecraft:iron_ingot"}, "T": {"item": TRIM[cid]}},
                                                 "result": {"id": f"regnum:{cid}_{part}", "count": 1}})
        worn(cid, 1).save(f"{A}/textures/models/armor/{cid}_layer_1.png")
        worn(cid, 2).save(f"{A}/textures/models/armor/{cid}_layer_2.png")
    for l in ("ru_ru", "en_us"):
        p = f"{A}/lang/{l}.json"; data = json.load(open(p, encoding="utf-8")); data.update(lang)
        json.dump(data, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("armor ok")


if __name__ == "__main__":
    main()
