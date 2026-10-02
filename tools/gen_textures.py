"""Генератор оригинальных пиксельных текстур Regnum (16x16, эффекты 18x18).
Запуск: python3 tools/gen_textures.py  (из корня проекта)
"""
import random
from PIL import Image

ROOT = "src/main/resources/assets/regnum/textures"
T = (0, 0, 0, 0)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def new(size=16):
    return Image.new("RGBA", (size, size), T)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def line(img, pts, c):
    for (x, y) in pts:
        px(img, x, y, c)


def rect(img, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            px(img, x, y, c)


def outline(img, c=hexc('#1a1410')):
    """Тёмный контур вокруг непрозрачных пикселей (стиль ванильных предметов)."""
    src = img.copy()
    for x in range(img.width):
        for y in range(img.height):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < img.width and 0 <= ny < img.height and src.getpixel((nx, ny))[3] > 0 \
                            and src.getpixel((nx, ny)) != c:
                        img.putpixel((x, y), c)
                        break
    return img


def noise_fill(img, base, var=0.12, seed=1, region=None):
    rnd = random.Random(seed)
    x0, y0, x1, y1 = region or (0, 0, img.width - 1, img.height - 1)
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            px(img, x, y, shade(base, 1 + rnd.uniform(-var, var)))


def save(img, path):
    img.save(f"{ROOT}/{path}.png")


# ============================================================ ПРЕДМЕТЫ

def item_baton():
    img = new()
    wood, wood_d, gold, gold_l, red = hexc('#6b4423'), hexc('#4a2e17'), hexc('#d9a62b'), hexc('#ffe27a'), hexc('#a8231f')
    for i in range(3, 13):
        px(img, i, 15 - i, wood); px(img, i + 1, 15 - i, wood_d)
    rect(img, 11, 2, 13, 4, gold); px(img, 12, 3, red); px(img, 11, 2, gold_l); px(img, 13, 1, gold); px(img, 14, 2, gold)
    rect(img, 2, 12, 3, 13, gold); px(img, 2, 12, gold_l)
    px(img, 7, 8, gold); px(img, 8, 7, gold)
    return outline(img)


def item_bandage():
    img = new()
    w, w2, w3, red = hexc('#f2efe6'), hexc('#d8d2c2'), hexc('#b9b19c'), hexc('#c0392b')
    for y in range(4, 12):
        for x in range(3, 13):
            if (x - 8) ** 2 / 25 + (y - 8) ** 2 / 16 <= 1:
                px(img, x, y, w if (x + y) % 3 else w2)
    for y in range(5, 11):
        px(img, 8, y, w3)
    rect(img, 10, 7, 14, 9, w2); px(img, 14, 8, w3)
    px(img, 5, 6, red); px(img, 4, 7, red); px(img, 5, 7, red); px(img, 6, 7, red); px(img, 5, 8, red)
    return outline(img)


def item_splint():
    img = new()
    wood, wood_d, cloth = hexc('#9c7a4a'), hexc('#6e5430'), hexc('#ece6d6')
    for y in range(1, 15):
        px(img, 5, y, wood); px(img, 6, y, wood_d); px(img, 9, y, wood); px(img, 10, y, wood_d)
    for y in (3, 4, 8, 9, 12, 13):
        rect(img, 5, y, 10, y, cloth)
    return outline(img)


def potion(liquid, hi, cork=hexc('#8a6a3a')):
    img = new()
    glass, glass_d = hexc('#cfe3ea', 200), hexc('#9fb8c2', 220)
    rect(img, 7, 1, 8, 2, cork)
    rect(img, 6, 3, 9, 4, glass_d)
    for y in range(5, 15):
        for x in range(3, 13):
            if (x - 7.5) ** 2 / 20 + (y - 10) ** 2 / 22 <= 1:
                px(img, x, y, glass)
    for y in range(8, 15):
        for x in range(3, 13):
            if (x - 7.5) ** 2 / 17 + (y - 10) ** 2 / 19 <= 1:
                px(img, x, y, liquid if (x + y) % 4 else hi)
    px(img, 5, 7, hexc('#ffffff')); px(img, 5, 8, hexc('#ffffff'))
    return outline(img)


def item_medkit():
    img = new()
    box, box_d, white, handle = hexc('#c23b2e'), hexc('#8e2219'), hexc('#f5f1e8'), hexc('#3a2a20')
    rect(img, 2, 5, 13, 13, box); rect(img, 2, 13, 13, 13, box_d); rect(img, 13, 5, 13, 13, box_d)
    rect(img, 6, 3, 9, 3, handle); px(img, 6, 4, handle); px(img, 9, 4, handle)
    rect(img, 7, 6, 8, 12, white); rect(img, 5, 8, 10, 10, white)
    return outline(img)


def item_herb():
    img = new()
    g1, g2, g3, stem, fl = hexc('#4f8a2e'), hexc('#6fb23e'), hexc('#2f5c1b'), hexc('#5a7a2a'), hexc('#e8e070')
    for y in range(6, 15):
        px(img, 8, y, stem)
    leaves = [(6, 5), (5, 6), (6, 6), (7, 7), (10, 6), (11, 5), (10, 5), (9, 7), (5, 9), (6, 10), (7, 10), (11, 9), (10, 10), (9, 10),
              (4, 8), (12, 8)]
    for i, (x, y) in enumerate(leaves):
        px(img, x, y, [g1, g2, g3][i % 3])
    px(img, 8, 3, fl); px(img, 7, 4, fl); px(img, 9, 4, fl); px(img, 8, 4, hexc('#f0a030'))
    return outline(img)


def item_journal():
    img = new()
    cover, cover_d, page, strap, gold = hexc('#5c3a21'), hexc('#3e2614'), hexc('#efe4c8'), hexc('#2a1c10'), hexc('#d9a62b')
    rect(img, 3, 2, 12, 14, cover); rect(img, 3, 2, 3, 14, cover_d)
    rect(img, 12, 3, 13, 13, page)
    rect(img, 5, 5, 10, 5, gold); rect(img, 5, 7, 9, 7, gold)
    rect(img, 9, 9, 13, 10, strap); px(img, 11, 9, gold)
    return outline(img)


def item_scroll():
    img = new()
    paper, paper_d, roll, seal = hexc('#f1e3bd'), hexc('#d6c193'), hexc('#b89a5e'), hexc('#a8231f')
    rect(img, 4, 3, 11, 12, paper)
    for y in (5, 7, 9):
        rect(img, 5, y, 10, y, paper_d)
    rect(img, 3, 2, 12, 2, roll); rect(img, 3, 13, 12, 13, roll)
    px(img, 2, 2, roll); px(img, 13, 13, roll)
    rect(img, 9, 10, 10, 11, seal)
    return outline(img)


def item_crypt_heart():
    img = new()
    c1, c2, c3, glow = hexc('#5b2a86'), hexc('#8a4fd0'), hexc('#2e1247'), hexc('#d6b0ff')
    heart = ["..XX..XX..", ".XXXXXXXX.", "XXXXXXXXXX", "XXXXXXXXXX", ".XXXXXXXX.", "..XXXXXX..", "...XXXX...", "....XX...."]
    for j, row in enumerate(heart):
        for i, ch in enumerate(row):
            if ch == 'X':
                x, y = i + 3, j + 4
                px(img, x, y, c2 if (i + j) % 3 else c1)
    for (x, y) in [(5, 6), (6, 5), (5, 5)]:
        px(img, x, y, glow)
    for (x, y) in [(8, 7), (9, 8), (10, 9), (8, 9), (9, 10)]:
        px(img, x, y, c3)
    px(img, 7, 2, glow); px(img, 12, 3, glow); px(img, 3, 3, c2)
    return outline(img)


def item_chitin_plate():
    """Согнутая хитиновая пластина с тёплой кромкой, 16x16."""
    img = new()
    shell = hexc('#76503d')
    shell_mid = hexc('#a36b46')
    shell_light = hexc('#c18452')
    shell_dark = hexc('#37242a')
    rust = hexc('#c96d3a')
    pale = hexc('#e2a16a')
    # Асимметричная пластина: широкое плечо сверху и загнутый кончик снизу.
    rows = ((7, 8), (5, 10), (4, 11), (3, 12), (2, 12), (2, 12),
            (3, 12), (4, 11), (5, 10), (6, 9), (7, 8))
    for y, (left, right) in enumerate(rows, start=2):
        for x in range(left, right + 1):
            if x == left or x == right:
                color = shell_dark
            elif x <= left + 1:
                color = shell_mid
            elif y <= 6 and x <= left + 4:
                color = shell_light
            elif x >= right - 2:
                color = shell_dark if (x + y) % 2 else shell
            else:
                color = shell
            px(img, x, y, color)
    # Изогнутая центральная жилка и несколько сегментных швов.
    for x, y in ((7, 3), (6, 4), (6, 5), (7, 6), (8, 7), (8, 8), (9, 9), (8, 10), (8, 11)):
        px(img, x, y, shell_light)
    for x, y in ((4, 7), (5, 8), (6, 9), (7, 10)):
        px(img, x, y, shell_dark)
    for x, y in ((4, 4), (4, 5), (5, 3), (10, 4), (11, 5), (10, 7), (9, 8), (7, 12)):
        px(img, x, y, rust)
    for x, y in ((5, 4), (10, 5), (9, 7), (6, 6)):
        px(img, x, y, pale)
    # Малые зазубрины делают край похожим на панцирную пластину, а не слиток.
    px(img, 1, 6, shell_dark); px(img, 13, 7, shell_dark)
    px(img, 5, 13, shell_dark); px(img, 9, 13, shell_dark)
    return outline(img)


def item_blade():
    img = new()
    steel, steel_l, steel_d, purple, guard, grip, gem = hexc('#3d3a4a'), hexc('#8e86a8'), hexc('#1f1c29'), hexc('#7b2fbe'), hexc('#c9a227'), hexc('#2b1a12'), hexc('#c46bff')
    for i in range(0, 10):
        x, y = 4 + i, 11 - i
        px(img, x, y, steel); px(img, x + 1, y, steel_l if i % 2 else steel); px(img, x, y - 1, steel_d)
        if i % 3 == 1:
            px(img, x + 1, y - 1, purple)
    px(img, 14, 1, steel_l); px(img, 15, 0, steel_l)
    for (x, y) in [(2, 10), (3, 11), (4, 12), (5, 13), (6, 14)]:
        px(img, x, y, guard)
    px(img, 4, 12, gem)
    for i in range(3):
        px(img, 3 - i, 13 + i, grip)
    px(img, 0, 15, guard)
    return outline(img)


def item_life_root():
    img = new()
    root, root_d, gold, leaf = hexc('#c79a52'), hexc('#8a6430'), hexc('#ffe27a'), hexc('#6fb23e')
    body = [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8), (6, 9), (9, 9),
            (5, 10), (10, 10), (7, 9), (8, 9), (4, 11), (11, 11), (7, 10), (7, 11), (8, 12)]
    for i, (x, y) in enumerate(body):
        px(img, x, y, root if i % 3 else root_d)
    for (x, y) in [(7, 2), (8, 1), (6, 2), (9, 2), (8, 3)]:
        px(img, x, y, leaf)
    for (x, y) in [(7, 5), (8, 7), (3, 4), (12, 6), (11, 2)]:
        px(img, x, y, gold)
    return outline(img)


def item_elixir():
    return potion(hexc('#7fbf3f'), hexc('#c7e86a'))


def goods_crate(accent, accent2, kind):
    img = new()
    if kind == "crate":
        wood, wood_d = hexc('#a07a48'), hexc('#6e5230')
        rect(img, 2, 4, 13, 14, wood)
        for y in (4, 9, 14):
            rect(img, 2, y, 13, y, wood_d)
        rect(img, 2, 4, 2, 14, wood_d); rect(img, 13, 4, 13, 14, wood_d)
        rect(img, 5, 6, 10, 7, accent); rect(img, 6, 11, 9, 12, accent2)
    elif kind == "sack":
        cloth, cloth_d = accent, accent2
        for y in range(4, 15):
            for x in range(3, 13):
                if (x - 7.5) ** 2 / 25 + (y - 10) ** 2 / 30 <= 1:
                    px(img, x, y, cloth if (x + y) % 4 else cloth_d)
        rect(img, 6, 2, 9, 4, cloth_d); px(img, 7, 1, hexc('#6e5230')); px(img, 8, 1, hexc('#6e5230'))
    elif kind == "barrel":
        wood, wood_d, hoop = hexc('#8a5a2e'), hexc('#5e3b1c'), hexc('#3a3a3e')
        for y in range(2, 15):
            w = 5 if 4 < y < 12 else 4
            rect(img, 8 - w, y, 7 + w, y, wood)
        for x in range(4, 12, 3):
            rect(img, x, 2, x, 14, wood_d)
        for y in (4, 12):
            rect(img, 3, y, 12, y, hoop)
        rect(img, 6, 7, 9, 9, accent)
    elif kind == "bundle":
        fur, fur_d = accent, accent2
        for i in range(3):
            rect(img, 2, 4 + i * 3, 13, 6 + i * 3, fur if i % 2 == 0 else fur_d)
        rect(img, 7, 3, 8, 14, hexc('#6e5230'))
    elif kind == "chest":
        wood, gold = hexc('#6b4423'), hexc('#d9a62b')
        rect(img, 2, 5, 13, 13, wood); rect(img, 2, 5, 13, 7, shade(wood, 1.2))
        rect(img, 2, 8, 13, 8, gold); rect(img, 7, 7, 8, 10, gold)
        px(img, 4, 4, accent); px(img, 10, 3, accent); px(img, 6, 3, accent2); px(img, 9, 4, accent2)
    return outline(img)


# ============================================================ БЛОКИ

def planks(img, seed, base=hexc('#8a6238')):
    noise_fill(img, base, 0.08, seed)
    for y in (3, 7, 11, 15):
        rect(img, 0, y, 15, y, shade(base, 0.7))


def bricks(img, seed, base=hexc('#8f8f8f')):
    noise_fill(img, base, 0.07, seed)
    mortar = shade(base, 0.62)
    for y in (3, 7, 11, 15):
        rect(img, 0, y, 15, y, mortar)
    for row in range(4):
        off = 0 if row % 2 == 0 else 4
        for x in range(off, 16, 8):
            for y in range(row * 4, row * 4 + 3):
                px(img, x, y, mortar)


def block_town_hall():
    side = new(); bricks(side, 11, hexc('#bdb6a3'))
    rect(side, 0, 0, 15, 2, hexc('#7a2a20')); rect(side, 0, 2, 15, 2, hexc('#5a1c14'))
    rect(side, 6, 5, 9, 15, hexc('#4a2e17')); rect(side, 7, 6, 8, 15, hexc('#6b4423')); px(side, 8, 11, hexc('#d9a62b'))
    rect(side, 2, 6, 3, 9, hexc('#3d6fa8')); rect(side, 12, 6, 13, 9, hexc('#3d6fa8'))
    rect(side, 7, 3, 8, 4, hexc('#d9a62b'))
    top = new(); noise_fill(top, hexc('#8e2f22'), 0.08, 12)
    for i in range(16):
        px(top, i, i, hexc('#6a2016')); px(top, 15 - i, i, hexc('#6a2016'))
    rect(top, 6, 6, 9, 9, hexc('#d9a62b')); rect(top, 7, 7, 8, 8, hexc('#ffe27a'))
    return side, top


def block_barracks():
    side = new(); planks(side, 21, hexc('#7a5230'))
    rect(side, 0, 0, 0, 15, hexc('#4a2e17')); rect(side, 15, 0, 15, 15, hexc('#4a2e17'))
    # скрещённые мечи
    for i in range(3, 13):
        px(side, i, i, hexc('#c8c8d0')); px(side, 15 - i, i, hexc('#c8c8d0'))
    rect(side, 3, 11, 4, 12, hexc('#d9a62b')); rect(side, 11, 11, 12, 12, hexc('#d9a62b'))
    rect(side, 6, 5, 9, 9, hexc('#a8231f')); rect(side, 7, 6, 8, 8, hexc('#d9a62b'))
    top = new(); planks(top, 22, hexc('#6b4423'))
    rect(top, 3, 3, 12, 12, hexc('#a8231f')); rect(top, 5, 5, 10, 10, hexc('#7d1814'))
    return side, top


def block_market():
    side = new(); planks(side, 31, hexc('#9a7444'))
    for x in range(16):
        c = hexc('#e8e0cc') if (x // 2) % 2 == 0 else hexc('#2e8a4a')
        rect(side, x, 0, x, 4, c)
    rect(side, 0, 5, 15, 5, hexc('#4a2e17'))
    rect(side, 3, 9, 5, 11, hexc('#2fbf6a')); rect(side, 10, 9, 12, 11, hexc('#d9a62b'))
    px(side, 4, 10, hexc('#8af0b0')); px(side, 11, 10, hexc('#ffe27a'))
    rect(side, 1, 12, 14, 12, hexc('#5a3a1e'))
    top = new()
    for x in range(16):
        for y in range(16):
            px(top, x, y, hexc('#e8e0cc') if ((x // 4) + (y // 4)) % 2 == 0 else hexc('#2e8a4a'))
    return side, top


def block_library():
    side = new(); planks(side, 51, hexc('#5a3a1e'))
    cols = ['#a83a2a', '#2f5a9a', '#d9a62b', '#3a7a3a', '#7a2a7a', '#c8c0a8']
    for row, y0 in enumerate((2, 8)):
        rect(side, 0, y0 + 4, 15, y0 + 5, hexc('#3a2410'))
        x = 1
        i = 0
        while x < 15:
            w = 1 + (i * 7 + row) % 2
            rect(side, x, y0, min(14, x + w - 1), y0 + 3, hexc(cols[(i + row * 3) % len(cols)]))
            x += w + 1
            i += 1
    top = new(); planks(top, 52, hexc('#6b4423'))
    rect(top, 3, 4, 12, 11, hexc('#e8e0cc')); rect(top, 7, 4, 8, 11, hexc('#9a8a68'))
    for y in (6, 8, 10):
        rect(top, 4, y, 6, y, hexc('#6a5a3a')); rect(top, 9, y, 11, y, hexc('#6a5a3a'))
    return side, top


def block_university():
    side = new(); bricks(side, 61, hexc('#b8b2a2'))
    rect(side, 0, 0, 15, 2, hexc('#5a2a7a')); rect(side, 0, 3, 15, 3, hexc('#d9a62b'))
    for x in (2, 12):
        rect(side, x, 4, x + 1, 15, hexc('#e8e4d8')); rect(side, x, 4, x + 1, 4, hexc('#d9a62b'))
    rect(side, 5, 6, 10, 13, hexc('#2a1a3a')); rect(side, 6, 7, 9, 12, hexc('#4a2e6a'))
    px(side, 7, 9, hexc('#ffe27a')); px(side, 8, 9, hexc('#ffe27a'))
    top = new()
    for x in range(16):
        for y in range(16):
            px(top, x, y, hexc('#5a2a7a') if ((x // 4) + (y // 4)) % 2 == 0 else hexc('#7a4a9a'))
    rect(top, 6, 6, 9, 9, hexc('#d9a62b')); rect(top, 7, 7, 8, 8, hexc('#fff0a0'))
    return side, top


def block_infirmary():
    side = new(); noise_fill(side, hexc('#ebe7da'), 0.04, 71)
    rect(side, 0, 0, 15, 1, hexc('#b8b4a6')); rect(side, 0, 14, 15, 15, hexc('#b8b4a6'))
    rect(side, 6, 3, 9, 12, hexc('#b8231f')); rect(side, 3, 6, 12, 9, hexc('#b8231f'))
    rect(side, 7, 4, 8, 11, hexc('#e04a3a')); rect(side, 4, 7, 11, 8, hexc('#e04a3a'))
    top = new(); noise_fill(top, hexc('#d8d4c6'), 0.05, 72)
    rect(top, 0, 0, 15, 0, hexc('#b8231f')); rect(top, 0, 15, 15, 15, hexc('#b8231f'))
    rect(top, 0, 0, 0, 15, hexc('#b8231f')); rect(top, 15, 0, 15, 15, hexc('#b8231f'))
    rect(top, 7, 4, 8, 11, hexc('#b8231f')); rect(top, 4, 7, 11, 8, hexc('#b8231f'))
    return side, top


def block_smithy():
    side = new(); bricks(side, 81, hexc('#4a4846'))
    rect(side, 0, 0, 15, 2, hexc('#2e2c2a'))
    rect(side, 3, 8, 12, 9, hexc('#9a9aa4')); rect(side, 5, 10, 10, 11, hexc('#7a7a84')); rect(side, 6, 12, 9, 13, hexc('#5a5a64'))
    rect(side, 2, 6, 5, 7, hexc('#9a9aa4'))
    rect(side, 11, 4, 12, 7, hexc('#7a5a3a')); rect(side, 10, 3, 13, 4, hexc('#8a8a94'))
    rect(side, 0, 14, 15, 15, hexc('#ff7a1a')); rect(side, 0, 15, 15, 15, hexc('#ffd070'))
    top = new(); noise_fill(top, hexc('#3a3836'), 0.08, 82)
    rect(top, 4, 4, 11, 11, hexc('#5a1a10')); rect(top, 5, 5, 10, 10, hexc('#ff6a10')); rect(top, 7, 7, 8, 8, hexc('#ffe27a'))
    return side, top


def block_warehouse():
    side = new(); planks(side, 91, hexc('#8a6a3a'))
    for x in (0, 7, 15):
        rect(side, x, 0, min(15, x + 1), 15, hexc('#4a2e17'))
    rect(side, 0, 7, 15, 8, hexc('#4a2e17'))
    rect(side, 3, 3, 5, 5, hexc('#c8a050')); rect(side, 10, 10, 12, 12, hexc('#c8a050'))
    top = new(); planks(top, 92, hexc('#7a5a30'))
    rect(top, 2, 2, 13, 13, hexc('#9a7a48')); rect(top, 2, 7, 13, 8, hexc('#4a2e17')); rect(top, 7, 2, 8, 13, hexc('#4a2e17'))
    return side, top


def block_stable():
    side = new(); planks(side, 101, hexc('#6a4a2a'))
    rect(side, 0, 0, 15, 2, hexc('#8a6a3a'))
    rect(side, 4, 5, 11, 6, hexc('#c8c8d0')); rect(side, 4, 7, 5, 11, hexc('#c8c8d0')); rect(side, 10, 7, 11, 11, hexc('#c8c8d0'))
    rect(side, 6, 11, 9, 11, hexc('#c8c8d0'))
    for xx in (4, 10):
        px(side, xx, 8, hexc('#4a4a52'))
    top = new(); noise_fill(top, hexc('#d8b84a'), 0.12, 102)
    for i in range(0, 16, 3):
        rect(top, i, (i * 5) % 14, min(15, i + 2), (i * 5) % 14, hexc('#b8962a'))
    return side, top


def block_watchtower():
    side = new(); bricks(side, 41, hexc('#8a8a86'))
    rect(side, 0, 0, 15, 3, hexc('#6b6b67'))
    for x in (1, 5, 9, 13):
        rect(side, x, 0, x + 1, 1, T if False else hexc('#4d4d4a'))
    rect(side, 7, 6, 8, 10, hexc('#1c1c1c')); px(side, 7, 6, hexc('#f0b030'))
    top = new(); noise_fill(top, hexc('#7a7a76'), 0.08, 42)
    rect(top, 0, 0, 15, 1, hexc('#555551')); rect(top, 0, 14, 15, 15, hexc('#555551'))
    rect(top, 0, 0, 1, 15, hexc('#555551')); rect(top, 14, 0, 15, 15, hexc('#555551'))
    rect(top, 7, 7, 8, 8, hexc('#f0b030'))
    return side, top


def block_training():
    side = new(); planks(side, 91, hexc('#8a6a40'))
    # соломенное чучело-мишень
    rect(side, 7, 3, 8, 13, hexc('#5a3a1e'))
    rect(side, 4, 6, 11, 7, hexc('#5a3a1e'))
    for (x, y) in [(6, 2), (7, 2), (8, 2), (9, 2), (6, 3), (9, 3), (6, 4), (7, 4), (8, 4), (9, 4)]:
        px(side, x, y, hexc('#e0c060'))
    rect(side, 6, 8, 9, 11, hexc('#e0c060')); px(side, 7, 9, hexc('#a8231f')); px(side, 8, 10, hexc('#a8231f'))
    top = new(); noise_fill(top, hexc('#b89a62'), 0.08, 92)
    for i in range(16):
        px(top, i, 7, hexc('#8a6a40')); px(top, i, 8, hexc('#8a6a40'))
    rect(top, 5, 5, 10, 10, hexc('#e0c060')); rect(top, 7, 7, 8, 8, hexc('#a8231f'))
    return side, top


def block_builder():
    side = new(); planks(side, 93, hexc('#9a7a4a'))
    # стропила и инструменты: молоток и пила на стене
    rect(side, 0, 0, 15, 1, hexc('#5a3a1e')); rect(side, 0, 14, 15, 15, hexc('#5a3a1e'))
    rect(side, 3, 5, 3, 12, hexc('#6a4a2a')); rect(side, 1, 5, 5, 6, hexc('#9a9aa2'))
    for i in range(8):
        px(side, 8 + i, 5 + i // 2, hexc('#b8b8c0')); px(side, 8 + i, 6 + i // 2, hexc('#7a7a82'))
    rect(side, 7, 4, 9, 6, hexc('#6a4a2a'))
    top = new(); noise_fill(top, hexc('#a68656'), 0.08, 94)
    for i in range(16):
        px(top, i, 4, hexc('#7a5a30')); px(top, i, 11, hexc('#7a5a30'))
    rect(top, 3, 6, 12, 9, hexc('#d8d0b8')); rect(top, 4, 7, 11, 7, hexc('#4a6a9a'))
    return side, top


def block_forge():
    side = new(); bricks(side, 51, hexc('#5e4a40'))
    rect(side, 4, 7, 11, 13, hexc('#1a1210'))
    for x in range(5, 11):
        px(side, x, 12, hexc('#ff7a1a')); px(side, x, 11, hexc('#ffb53a') if x % 2 else hexc('#ff7a1a'))
    px(side, 7, 10, hexc('#ffe27a')); px(side, 8, 9, hexc('#ffb53a'))
    rect(side, 3, 6, 12, 6, hexc('#3a2a24'))
    top = new(); noise_fill(top, hexc('#3a3a3e'), 0.08, 52)
    rect(top, 2, 5, 13, 10, hexc('#55555a')); rect(top, 4, 4, 11, 4, hexc('#55555a')); rect(top, 12, 6, 14, 8, hexc('#55555a'))
    rect(top, 3, 6, 12, 6, hexc('#77777e'))
    return side, top


def block_altar():
    side = new(); noise_fill(side, hexc('#2a2630'), 0.1, 61)
    rect(side, 0, 0, 15, 2, hexc('#3d3647')); rect(side, 0, 13, 15, 15, hexc('#3d3647'))
    for (x, y) in [(4, 6), (5, 7), (6, 8), (9, 8), (10, 7), (11, 6), (7, 9), (8, 9), (7, 5), (8, 5)]:
        px(side, x, y, hexc('#7b2fbe'))
    top = new(); noise_fill(top, hexc('#2a2630'), 0.1, 62)
    rune = hexc('#9b4fff')
    for i in range(3, 13):
        px(top, i, 3, rune); px(top, i, 12, rune); px(top, 3, i, rune); px(top, 12, i, rune)
    for i in range(5, 11):
        px(top, i, i, rune); px(top, 15 - i, i, rune)
    rect(top, 7, 7, 8, 8, hexc('#e0c0ff'))
    top_off = top.copy()
    for x in range(16):
        for y in range(16):
            c = top_off.getpixel((x, y))
            if c[:3] in ((0x9b, 0x4f, 0xff), (0xe0, 0xc0, 0xff)):
                top_off.putpixel((x, y), hexc('#3d3647'))
    return side, top, top_off


def altar_variant(base, rim, rune, glow, seed):
    side = new(); noise_fill(side, hexc(base), 0.1, seed)
    rect(side, 0, 0, 15, 2, hexc(rim)); rect(side, 0, 13, 15, 15, hexc(rim))
    for (x, y) in [(4, 6), (5, 7), (6, 8), (9, 8), (10, 7), (11, 6), (7, 9), (8, 9), (7, 5), (8, 5)]:
        px(side, x, y, hexc(rune))
    top = new(); noise_fill(top, hexc(base), 0.1, seed + 1)
    for i in range(3, 13):
        px(top, i, 3, hexc(rune)); px(top, i, 12, hexc(rune)); px(top, 3, i, hexc(rune)); px(top, 12, i, hexc(rune))
    for i in range(5, 11):
        px(top, i, i, hexc(rune)); px(top, 15 - i, i, hexc(rune))
    rect(top, 7, 7, 8, 8, hexc(glow))
    used = new(); noise_fill(used, hexc(base), 0.1, seed + 1)
    for i in range(3, 13):
        px(used, i, 3, hexc(rim)); px(used, i, 12, hexc(rim)); px(used, 3, i, hexc(rim)); px(used, 12, i, hexc(rim))
    return side, top, used


def block_ember_core():
    img = new(); noise_fill(img, hexc('#3a1a10'), 0.15, 71)
    rnd = random.Random(72)
    for _ in range(40):
        x, y = rnd.randrange(16), rnd.randrange(16)
        px(img, x, y, rnd.choice([hexc('#ff7a1a'), hexc('#ffb53a'), hexc('#ffe27a'), hexc('#e0401a')]))
    rect(img, 6, 6, 9, 9, hexc('#ffb53a')); rect(img, 7, 7, 8, 8, hexc('#fff0a0'))
    return img


def item_gem(c1, c2, hi, shape="round"):
    img = new()
    for y in range(3, 14):
        for x in range(3, 13):
            if shape == "round" and (x - 7.5) ** 2 + (y - 8) ** 2 <= 22:
                px(img, x, y, hexc(c1) if (x + y) % 3 else hexc(c2))
            elif shape == "ingot" and 5 <= y <= 11 and abs(x - 7.5) <= 5 - (y - 5) * 0.2:
                px(img, x, y, hexc(c1) if y > 6 else hexc(hi))
    px(img, 6, 5, hexc(hi)); px(img, 5, 6, hexc(hi))
    return outline(img)


def item_root_rot():
    img = new()
    c, d, g = hexc('#5a4a2a'), hexc('#3a2e18'), hexc('#7fbf3f')
    pts = [(8, 2), (8, 3), (7, 4), (8, 4), (7, 5), (8, 6), (7, 7), (6, 8), (8, 8), (5, 9), (9, 9), (6, 10), (10, 10), (4, 11), (11, 11), (7, 11), (7, 12), (3, 12), (12, 12)]
    for i, (x, y) in enumerate(pts):
        px(img, x, y, c if i % 2 else d)
    for (x, y) in [(6, 4), (9, 6), (5, 10), (10, 9), (8, 12)]:
        px(img, x, y, g)
    return outline(img)


def item_staff():
    img = new()
    wood, wood_d, g, g2 = hexc('#4a3a1e'), hexc('#2e2410'), hexc('#7fbf3f'), hexc('#c7e86a')
    for i in range(2, 14):
        px(img, i, 15 - i, wood); px(img, i + 1, 15 - i, wood_d)
    for (x, y) in [(12, 1), (13, 1), (12, 2), (13, 2), (11, 2), (14, 2), (12, 3), (13, 3)]:
        px(img, x, y, g)
    px(img, 12, 2, g2)
    return outline(img)


def item_star_blade():
    img = new()
    s1, s2, s3, guard, grip = hexc('#4a6aa8'), hexc('#b8d0ff'), hexc('#24365c'), hexc('#ff9a2a'), hexc('#3a2010')
    for i in range(0, 10):
        x, y = 4 + i, 11 - i
        px(img, x, y, s1); px(img, x + 1, y, s2 if i % 2 else s1); px(img, x, y - 1, s3)
    px(img, 14, 1, s2); px(img, 15, 0, hexc('#ffffff'))
    for (x, y) in [(2, 10), (3, 11), (4, 12), (5, 13), (6, 14)]:
        px(img, x, y, guard)
    for i in range(3):
        px(img, 3 - i, 13 + i, grip)
    return outline(img)


def item_amulet():
    img = new()
    chain, gold, amber, hi = hexc('#c9a227'), hexc('#d9a62b'), hexc('#ff9a1a'), hexc('#ffe27a')
    for (x, y) in [(4, 2), (5, 3), (6, 4), (11, 2), (10, 3), (9, 4)]:
        px(img, x, y, chain)
    for y in range(5, 13):
        for x in range(4, 12):
            d = (x - 7.5) ** 2 + (y - 8.5) ** 2
            if d <= 13:
                px(img, x, y, gold if d > 7 else amber)
    px(img, 6, 7, hi); px(img, 7, 7, hi)
    return outline(img)


# ============================================================ ЭФФЕКТЫ (18x18)

def effect_bleeding():
    img = new(18)
    red, red_d, hi = hexc('#c0141c'), hexc('#7a0a10'), hexc('#ff6a6a')
    for y in range(3, 16):
        for x in range(4, 14):
            w = (y - 3) * 0.45 if y < 10 else 4.5 - (y - 10) * 0.6
            if abs(x - 8.5) <= max(0.5, w):
                px(img, x, y, red if x < 10 else red_d)
    px(img, 7, 9, hi); px(img, 7, 10, hi)
    return outline(img)


def effect_fracture():
    img = new(18)
    bone, bone_d = hexc('#efe8d6'), hexc('#bdb39a')
    for i in range(3, 15):
        px(img, i, i, bone); px(img, i + 1, i, bone_d)
    for (x, y) in [(2, 3), (3, 2), (2, 2), (15, 14), (14, 15), (15, 15)]:
        px(img, x, y, bone)
    for (x, y) in [(8, 9), (9, 8), (10, 7), (8, 10)]:
        px(img, x, y, T)
    px(img, 9, 9, hexc('#c0141c'))
    return outline(img)


def effect_infection():
    img = new(18)
    g, g_d, g_l = hexc('#6f9a1e'), hexc('#3e5a0e'), hexc('#b6e04a')
    for y in range(18):
        for x in range(18):
            if (x - 8.5) ** 2 + (y - 8.5) ** 2 <= 30:
                px(img, x, y, g if (x * 3 + y) % 5 else g_d)
    for (x, y) in [(6, 6), (11, 7), (7, 11), (10, 11)]:
        px(img, x, y, g_l); px(img, x + 1, y, g_l)
    for (x, y) in [(8, 2), (8, 1), (15, 8), (16, 8), (8, 15), (8, 16), (2, 8), (1, 8)]:
        px(img, x, y, g_d)
    return outline(img)


if __name__ == "__main__":
    import os
    for d in ("item", "block", "mob_effect"):
        os.makedirs(f"{ROOT}/{d}", exist_ok=True)
    items = {
        "commander_baton": item_baton(), "bandage": item_bandage(), "splint": item_splint(),
        "herbal_decoction": potion(hexc('#3f8a4a'), hexc('#7fd08a')), "medkit": item_medkit(),
        "healing_herb": item_herb(), "survivor_journal": item_journal(), "mastery_scroll": item_scroll(),
        "crypt_heart": item_crypt_heart(), "chitin_plate": item_chitin_plate(),
        "morgrath_blade": item_blade(), "trick_elixir": item_elixir(),
        "life_root": item_life_root(),
        "rot_root": item_root_rot(),
        "star_iron": item_gem('#4a6aa8', '#2e4a80', '#b8d0ff', "ingot"),
        "sun_amber": item_gem('#ff9a1a', '#e07a10', '#ffe27a'),
        "antidote": potion(hexc('#5a3a8a'), hexc('#9a7ad0')),
        "mire_staff": item_staff(),
        "star_blade": item_star_blade(),
        "sun_amulet": item_amulet(),
        "salt": goods_crate(hexc('#f4f4f0'), hexc('#d8d8d0'), "crate"),
        "spices": goods_crate(hexc('#c8622a'), hexc('#9a4318'), "sack"),
        "silk": goods_crate(hexc('#c43b8f'), hexc('#8f2466'), "bundle"),
        "furs": goods_crate(hexc('#7a5636'), hexc('#5a3d24'), "bundle"),
        "wine": goods_crate(hexc('#7a1030'), hexc('#5a0820'), "barrel"),
        "iron_goods": goods_crate(hexc('#c8c8d0'), hexc('#8a8a94'), "crate"),
        "herb_bundle": goods_crate(hexc('#5e9a3a'), hexc('#3e6a22'), "sack"),
        "amber": goods_crate(hexc('#ffb02e'), hexc('#e08a10'), "chest"),
    }
    for k, v in items.items():
        save(v, f"item/{k}")
    for name, fn in (("town_hall", block_town_hall), ("barracks", block_barracks), ("market", block_market),
                     ("watchtower", block_watchtower), ("master_forge", block_forge), ("training_ground", block_training), ("builder_hut", block_builder),
                     ("library", block_library), ("university", block_university), ("infirmary", block_infirmary),
                     ("smithy", block_smithy), ("warehouse", block_warehouse), ("stable", block_stable)):
        side, top = fn()
        save(side, f"block/{name}_side")
        save(top, f"block/{name}_top")
    side, top, top_off = block_altar()
    save(side, "block/crypt_altar_side"); save(top, "block/crypt_altar_top"); save(top_off, "block/crypt_altar_top_used")
    for name, args in (("mire_altar", ('#24301c', '#3a4a2a', '#7fbf3f', '#d0ff9a', 81)),
                       ("forge_altar", ('#2a2220', '#4a3a30', '#ff7a1a', '#ffe27a', 82)),
                       ("sun_altar", ('#8a7448', '#a89060', '#2b6cb0', '#9ad0ff', 83))):
        a_side, a_top, a_used = altar_variant(*args)
        save(a_side, f"block/{name}_side"); save(a_top, f"block/{name}_top"); save(a_used, f"block/{name}_top_used")
    save(block_ember_core(), "block/ember_core")
    save(effect_bleeding(), "mob_effect/bleeding")
    save(effect_fracture(), "mob_effect/fracture")
    save(effect_infection(), "mob_effect/infection")
    print("ok")
