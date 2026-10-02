"""Инструменты классов: текстуры, модели, состояния блоков, язык, рецепты, лут блоков.
Запуск из корня проекта: python3 tools/gen_kit.py [превью.png]
"""
import json
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import gen_weapons as gw  # noqa: E402

A = "src/main/resources/assets/regnum"
D = "src/main/resources/data/regnum"
T = (0, 0, 0, 0)
K = (28, 26, 32, 255)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def new(n=16):
    return Image.new("RGBA", (n, n), T)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def rect(img, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            px(img, x, y, c)


def line(img, x0, y0, x1, y1, c):
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        px(img, round(x0 + (x1 - x0) * i / max(1, n)), round(y0 + (y1 - y0) * i / max(1, n)), c)


def disc(img, cx, cy, r, c, c2=None):
    for x in range(img.width):
        for y in range(img.height):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                px(img, x, y, c2 if (c2 and (x - cx) + (y - cy) < -r * 0.5) else c)


def outline(img, c=K):
    src = img.copy()
    for x in range(img.width):
        for y in range(img.height):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < img.width and 0 <= ny < img.height and src.getpixel((nx, ny))[3] > 0 and src.getpixel((nx, ny)) != c:
                        img.putpixel((x, y), c)
                        break
    return img


# ---------------------------------------------------------------- предметы

def whetstone():
    img = new()
    g, G, d = hexc('#8f9096'), hexc('#c4c6cc'), hexc('#5c5d64')
    for i in range(9):
        x, y = 4 + i, 11 - i
        px(img, x, y, g)
        px(img, x + 1, y, g)
        px(img, x, y - 1, G)
        px(img, x + 1, y + 1, d)
        px(img, x + 2, y, d)
    px(img, 6, 9, hexc('#a8aab0'))
    px(img, 9, 6, hexc('#a8aab0'))
    return outline(img)


def lockpick():
    img = new()
    I, i, n, y = hexc('#f0f0f4'), hexc('#c4c6cf'), hexc('#7a7c86'), hexc('#c09040')
    line(img, 5, 10, 12, 3, i)
    line(img, 6, 10, 13, 3, n)
    px(img, 13, 2, I)
    px(img, 14, 2, i)
    px(img, 14, 3, i)
    # кольцо-рукоять
    for (x, yy) in [(3, 10), (4, 10), (5, 11), (5, 12), (4, 13), (3, 13), (2, 12), (2, 11)]:
        px(img, x, yy, y)
    px(img, 3, 11, shade(y, 1.3))
    # вторая, тонкая
    line(img, 7, 13, 12, 8, n)
    px(img, 12, 7, i)
    return outline(img)


def smoke_bomb():
    img = new()
    d, n, I = hexc('#3e3c44'), hexc('#5c5a66'), hexc('#9a98a6')
    disc(img, 7.5, 9.5, 4.6, d, n)
    px(img, 5, 7, I)
    px(img, 6, 7, n)
    px(img, 5, 8, n)
    rect(img, 7, 4, 8, 5, hexc('#8a6a40'))
    px(img, 9, 3, hexc('#c8a878'))
    px(img, 10, 2, hexc('#ffb040'))
    px(img, 11, 1, hexc('#ffe080'))
    img = outline(img)
    for (x, y) in [(12, 3), (13, 2), (13, 4), (14, 3)]:
        px(img, x, y, hexc('#b0b4c0', 200))
    return img


def grappling_hook():
    img = new()
    i, I, n, o, O = hexc('#c4c6cf'), hexc('#f0f0f4'), hexc('#7a7c86'), hexc('#b08a58'), hexc('#d8b480')
    # три зубца
    line(img, 7, 2, 7, 7, i)
    line(img, 8, 2, 8, 7, n)
    line(img, 3, 3, 6, 6, i)
    line(img, 12, 3, 9, 6, n)
    px(img, 3, 2, I)
    px(img, 12, 2, I)
    px(img, 7, 1, I)
    # моток верёвки
    for r, c in ((4.0, o), (2.6, O)):
        for k in range(48):
            import math
            a = k / 48 * 2 * math.pi
            px(img, round(7.5 + math.cos(a) * r), round(11 + math.sin(a) * r * 0.7), c)
    line(img, 7, 7, 7, 9, o)
    return outline(img)


def herbal_mortar():
    img = new()
    g, G, d, w, W = hexc('#8f9096'), hexc('#b8bac0'), hexc('#5c5d64'), hexc('#8a6238'), hexc('#b08850')
    h, H = hexc('#3e7a2e'), hexc('#7ac060')
    rect(img, 3, 8, 12, 9, g)
    rect(img, 4, 10, 11, 10, g)
    rect(img, 5, 11, 10, 11, d)
    rect(img, 6, 12, 9, 12, d)
    rect(img, 3, 7, 12, 7, G)
    for x in (4, 6, 8, 10):
        px(img, x, 6, h)
        px(img, x + 1, 6, H)
    px(img, 7, 5, H)
    line(img, 9, 6, 12, 2, w)
    line(img, 10, 6, 13, 2, W)
    return outline(img)


def field_smith_kit():
    img = new()
    l, L, y = hexc('#6a4a2a'), hexc('#8a6a40'), hexc('#c09040')
    i, I, w = hexc('#9a9aa4'), hexc('#d0d2da'), hexc('#7a5a36')
    rect(img, 2, 8, 13, 13, l)
    rect(img, 2, 8, 13, 8, L)
    rect(img, 7, 8, 8, 13, shade(l, 0.8))
    px(img, 7, 10, y)
    px(img, 8, 10, y)
    # молоток
    line(img, 9, 7, 12, 2, w)
    rect(img, 10, 1, 14, 3, i)
    rect(img, 10, 1, 14, 1, I)
    # клещи
    line(img, 3, 7, 6, 3, i)
    line(img, 5, 7, 6, 4, shade(i, 0.8))
    return outline(img)


def war_banner():
    img = new()
    w, W, r, R, y = hexc('#6a4a2a'), hexc('#8a6a40'), hexc('#9a1f1f'), hexc('#c03030'), hexc('#e0b030')
    line(img, 3, 1, 3, 15, w)
    px(img, 3, 0, y)
    rect(img, 4, 2, 12, 10, r)
    rect(img, 4, 2, 12, 2, R)
    for x in range(4, 13):
        if x % 2 == 0:
            px(img, x, 11, r)
    # герб — солнце
    rect(img, 7, 5, 9, 7, y)
    for (x, yy) in [(8, 3), (8, 9), (5, 6), (11, 6)]:
        px(img, x, yy, y)
    return outline(img)


def builder_plan():
    img = new()
    p, d, r = hexc('#e6dcc0'), hexc('#b8a880'), hexc('#a8231f')
    rect(img, 3, 3, 12, 12, p)
    rect(img, 3, 3, 12, 3, d); rect(img, 3, 12, 12, 12, d)
    px(img, 2, 4, d); px(img, 2, 11, d); px(img, 13, 4, d); px(img, 13, 11, d)
    # чертёж стены: линия с башнями
    for x in range(4, 12):
        px(img, x, 8, hexc('#3a4a6a'))
    rect(img, 4, 6, 5, 10, hexc('#3a4a6a')); rect(img, 10, 6, 11, 10, hexc('#3a4a6a'))
    px(img, 7, 5, r); px(img, 8, 5, r)
    return outline(img)


def wildfire_flask():
    img = new()
    g, G, d = hexc('#3ee0a0'), hexc('#b8ffe0'), hexc('#1a8a68')
    glass = hexc('#d8e8f0')
    rect(img, 6, 2, 9, 3, hexc('#8a6a40'))
    rect(img, 7, 4, 8, 6, glass)
    for y in range(7, 14):
        w = 2 + min(y - 7, 2)
        for x in range(8 - w, 8 + w):
            px(img, x, y, glass if (x == 8 - w or x == 7 + w) else g)
    rect(img, 6, 9, 9, 11, G)
    px(img, 7, 8, hexc('#ffffff')); px(img, 9, 12, d)
    return outline(img)


def javelin():
    w = gw.W("javelin", "", "north", "militia", "pilum", blade=gw.STEEL, guard=gw.IRON, grip='#8a6a40')
    return gw.render(w)


def throwing_axe():
    w = gw.W("throwing_axe", "", "clans", "militia", "axe", blade=gw.STEEL, guard=gw.IRON, grip='#7a5a36')
    img = gw.render(w)
    # покороче: сдвиг вниз-влево не нужен — топорик и так читается
    return img


# ---------------------------------------------------------------- блоки

def chest_tex(kind):
    rnd = random.Random(hash(kind) & 0xFFFF)
    img = Image.new("RGBA", (16, 16))
    base = hexc('#6a4626')
    for x in range(16):
        for y in range(16):
            plank = (y // 4) % 2
            c = shade(base, (1.0 if plank else 0.9) * (1 + rnd.uniform(-0.06, 0.06)))
            if y % 4 == 3:
                c = shade(base, 0.7)
            img.putpixel((x, y), c)
    band, band_hi, rivet = hexc('#4a4a52'), hexc('#7a7a84'), hexc('#c0c0c8')
    for x in (0, 1, 14, 15):
        for y in range(16):
            img.putpixel((x, y), band if x in (0, 15) else band_hi)
    if kind in ("front", "side"):
        for y in (0, 1, 14, 15):
            for x in range(16):
                img.putpixel((x, y), band if y in (0, 15) else band_hi)
        for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
            img.putpixel((x, y), rivet)
    if kind == "front":
        lock, lock_hi, hole = hexc('#c09040'), hexc('#e8c070'), hexc('#1a1410')
        for x in range(6, 10):
            for y in range(7, 12):
                img.putpixel((x, y), lock)
        for x in range(6, 10):
            img.putpixel((x, 7), lock_hi)
        img.putpixel((7, 4), hexc('#9a9aa4'))
        img.putpixel((8, 4), hexc('#9a9aa4'))
        img.putpixel((6, 5), hexc('#9a9aa4'))
        img.putpixel((9, 5), hexc('#9a9aa4'))
        img.putpixel((6, 6), hexc('#9a9aa4'))
        img.putpixel((9, 6), hexc('#9a9aa4'))
        img.putpixel((7, 9), hole)
        img.putpixel((8, 9), hole)
        img.putpixel((7, 10), hole)
    if kind == "top":
        for x in range(16):
            img.putpixel((x, 7), band)
            img.putpixel((x, 8), band_hi)
    return img


def trap_tex():
    rnd = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), shade(hexc('#6e6c74'), 1 + rnd.uniform(-0.12, 0.12)))
    for x in range(0, 16, 2):
        img.putpixel((x, 0), hexc('#c4c6cf'))
        img.putpixel((x, 15), hexc('#c4c6cf'))
    for y in range(5, 11):
        for x in range(5, 11):
            img.putpixel((x, y), hexc('#8a6a40'))
    return img


def w_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


ITEMS = {
    "whetstone": (whetstone, "Точильный камень", "generated"),
    "lockpick": (lockpick, "Отмычка", "generated"),
    "smoke_bomb": (smoke_bomb, "Дымовая шашка", "generated"),
    "grappling_hook": (grappling_hook, "Кошка-крюк", "handheld"),
    "herbal_mortar": (herbal_mortar, "Ступка травника", "generated"),
    "field_smith_kit": (field_smith_kit, "Походный набор кузнеца", "generated"),
    "war_banner": (war_banner, "Боевое знамя", "handheld"),
    "builder_plan": (builder_plan, "План строителя", "generated"),
    "wildfire_flask": (wildfire_flask, "Дикий огонь", "generated"),
    "javelin": (javelin, "Дротик", "handheld"),
    "throwing_axe": (throwing_axe, "Метательный топорик", "handheld"),
}

LANG = {
    "block.regnum.locked_chest": "Запертый сундук",
    "block.regnum.bear_trap": "Капкан",
    "effect.regnum.sharpened": "Заточка",
    "entity.regnum.thrown_weapon": "Метательное оружие",
    "entity.regnum.smoke_bomb": "Дымовая шашка",
    "entity.regnum.wildfire_flask": "Дикий огонь",
}

RECIPES = {
    "whetstone": ("shapeless", ["minecraft:smooth_stone", "minecraft:flint", "minecraft:flint"], 1),
    "lockpick": ("shapeless", ["minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:stick"], 2),
    "smoke_bomb": ("shapeless", ["minecraft:gunpowder", "#minecraft:coals", "minecraft:paper"], 2),
    "grappling_hook": ("shaped", (["III", " C ", " L "], {"I": "minecraft:iron_ingot", "C": "minecraft:chain", "L": "minecraft:lead"}), 1),
    "bear_trap": ("shaped", (["I I", "CIC"], {"I": "minecraft:iron_ingot", "C": "minecraft:chain"}), 1),
    "builder_plan": ("shapeless", ["minecraft:paper", "minecraft:paper", "minecraft:string", "minecraft:ink_sac"], 1),
    "wildfire_flask": ("shapeless", ["minecraft:glass_bottle", "minecraft:blaze_powder", "minecraft:blaze_powder", "minecraft:gunpowder", "regnum:rot_root"], 2),
    "javelin": ("shaped", (["  F", " S ", "S  "], {"F": "minecraft:flint", "S": "minecraft:stick"}), 2),
    "throwing_axe": ("shaped", (["II", " S"], {"I": "minecraft:iron_ingot", "S": "minecraft:stick"}), 2),
    "herbal_mortar": ("shaped", (["C C", " C "], {"C": "minecraft:cobblestone"}), 1),
    "field_smith_kit": ("shaped", (["IGI", "LLL"], {"I": "minecraft:iron_ingot", "G": "minecraft:grindstone", "L": "minecraft:leather"}), 1),
    "war_banner": ("shapeless", ["#minecraft:banners", "minecraft:gold_ingot", "minecraft:stick"], 1),
}


def ing(v):
    return {"tag": v[1:]} if v.startswith("#") else {"item": v}


def main():
    os.makedirs(f"{A}/textures/item", exist_ok=True)
    os.makedirs(f"{A}/textures/block", exist_ok=True)
    for name, (fn, title, parent) in ITEMS.items():
        fn().save(f"{A}/textures/item/{name}.png")
        w_json(f"{A}/models/item/{name}.json", {"parent": f"minecraft:item/{parent}", "textures": {"layer0": f"regnum:item/{name}"}})
        LANG[f"item.regnum.{name}"] = title
    # запертый сундук
    for k in ("front", "side", "top"):
        chest_tex(k).save(f"{A}/textures/block/locked_chest_{k}.png")
    w_json(f"{A}/models/block/locked_chest.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": "regnum:block/locked_chest_front", "side": "regnum:block/locked_chest_side", "top": "regnum:block/locked_chest_top"}})
    w_json(f"{A}/blockstates/locked_chest.json", {"variants": {
        "facing=north": {"model": "regnum:block/locked_chest"}, "facing=east": {"model": "regnum:block/locked_chest", "y": 90},
        "facing=south": {"model": "regnum:block/locked_chest", "y": 180}, "facing=west": {"model": "regnum:block/locked_chest", "y": 270}}})
    w_json(f"{A}/models/item/locked_chest.json", {"parent": "regnum:block/locked_chest"})
    # капкан
    trap_tex().save(f"{A}/textures/block/bear_trap.png")
    t = "#t"
    faces = {f: {"texture": t} for f in ("north", "south", "east", "west", "up", "down")}

    def el(a, b):
        return {"from": a, "to": b, "faces": faces}
    armed = [el([3, 0, 3], [13, 0.5, 13]), el([2, 0, 2], [3, 1, 14]), el([13, 0, 2], [14, 1, 14]),
             el([3, 0, 2], [13, 1, 3]), el([3, 0, 13], [13, 1, 14]), el([6, 0.5, 6], [10, 1, 10])]
    for x in range(3, 13, 2):
        armed.append(el([x, 1, 2], [x + 1, 2, 3]))
        armed.append(el([x, 1, 13], [x + 1, 2, 14]))
    sprung = [el([3, 0, 3], [13, 0.5, 13]), el([3, 0, 7], [13, 4, 8]), el([3, 0, 8], [13, 4, 9]), el([2, 0, 7], [3, 2, 9]), el([13, 0, 7], [14, 2, 9])]
    for x in range(3, 13, 2):
        sprung.append(el([x, 4, 7.5], [x + 1, 5, 8.5]))
    w_json(f"{A}/models/block/bear_trap.json", {"parent": "minecraft:block/block", "textures": {"t": "regnum:block/bear_trap", "particle": "regnum:block/bear_trap"},
                                                "elements": armed})
    w_json(f"{A}/models/block/bear_trap_sprung.json", {"parent": "minecraft:block/block", "textures": {"t": "regnum:block/bear_trap", "particle": "regnum:block/bear_trap"},
                                                       "elements": sprung})
    variants = {}
    for armed_v in ("true", "false"):
        for master in ("true", "false"):
            variants[f"armed={armed_v},master={master}"] = {"model": "regnum:block/bear_trap" + ("" if armed_v == "true" else "_sprung")}
    w_json(f"{A}/blockstates/bear_trap.json", {"variants": variants})
    w_json(f"{A}/models/item/bear_trap.json", {"parent": "regnum:block/bear_trap"})
    w_json(f"{D}/loot_table/blocks/bear_trap.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "regnum:bear_trap"}],
                                                                                          "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # рецепты
    for name, (kind, data, count) in RECIPES.items():
        if kind == "shapeless":
            w_json(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
                                              "ingredients": [ing(v) for v in data], "result": {"id": f"regnum:{name}", "count": count}})
        else:
            pat, key = data
            w_json(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pat,
                                              "key": {k: ing(v) for k, v in key.items()}, "result": {"id": f"regnum:{name}", "count": count}})
    # язык
    for lang in ("ru_ru", "en_us"):
        p = f"{A}/lang/{lang}.json"
        data = json.load(open(p, encoding="utf-8"))
        data.update(LANG)
        json.dump(data, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("kit ok")


def preview(path):
    imgs = [fn() for fn, _, _ in ITEMS.values()] + [chest_tex("front"), chest_tex("side"), trap_tex()]
    S = Image.new("RGBA", (len(imgs) * 136, 136), (40, 44, 52, 255))
    for i, im in enumerate(imgs):
        S.paste(im.resize((128, 128), Image.NEAREST), (i * 136, 4), im.resize((128, 128), Image.NEAREST))
    S.save(path)


if __name__ == "__main__":
    if len(sys.argv) > 1:
        preview(sys.argv[1])
    else:
        main()
