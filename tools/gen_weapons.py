"""Оружие культур: процедурные пиксельные спрайты 16×16 (рукоять внизу слева, остриё вверху справа),
модели предметов, локализация и рецепты.

Запуск из корня проекта: python3 tools/gen_weapons.py [превью.png]
"""
import json
import math
import os
import sys

from PIL import Image

A = "src/main/resources/assets/regnum"
D = "src/main/resources/data/regnum"
T = (0, 0, 0, 0)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


# ---------------------------------------------------------------- палитры

STEEL = dict(hi='#f0f0f4', mid='#c4c6cf', lo='#8a8c96', edge='#5a5c66')
DAMASK = dict(hi='#e8e4f0', mid='#b0aec0', lo='#7c7a90', edge='#4a4860')
BRONZE = dict(hi='#f6d68a', mid='#d0a050', lo='#9a7030', edge='#6a4818')
DARK = dict(hi='#b8b8c0', mid='#7a7a84', lo='#54545e', edge='#34343c')

GOLD = '#e0b030'
IRON = '#9a9aa4'
BRASS = '#c09040'


class W:
    """Описание оружия."""

    def __init__(self, id, name, culture, role, kind, blade=STEEL, guard=IRON, grip='#5a3418', pommel=None, accent=None,
                 lore=""):
        self.id, self.name, self.culture, self.role, self.kind = id, name, culture, role, kind
        self.blade, self.guard, self.grip = blade, guard, grip
        self.pommel = pommel or guard
        self.accent = accent
        self.lore = lore


WEAPONS = [
    W("north_spear", "Рогатина", "north", "militia", "spear", blade=DARK, guard=IRON, grip='#6a4a2a', lore="Широкое копьё на медведя и на ворога"),
    W("north_sword", "Каролинг", "north", "infantry", "broad", guard=IRON, grip='#7a1a1a', lore="Широкий клинок с долом — меч дружины"),
    W("north_noble", "Княжий меч", "north", "noble", "broad", blade=DAMASK, guard=GOLD, grip='#9a1f1f', accent='#e04040', lore="Узорчатый булат и золотая крестовина"),

    W("empire_spear", "Пилум", "empire", "militia", "pilum", blade=DARK, guard=IRON, grip='#7a5a36', lore="Длинное острие гнётся в щите врага"),
    W("empire_sword", "Гладиус", "empire", "infantry", "short", guard=BRASS, grip='#5a3418', pommel=BRASS, lore="Короткий и быстрый — колоть из-за щита"),
    W("empire_noble", "Спата", "empire", "noble", "straight", guard=GOLD, grip='#5a1d6b', accent='#a050c0', lore="Длинный меч всадников империи"),

    W("west_mace", "Моргенштерн", "west", "militia", "mace", blade=DARK, guard=IRON, grip='#5a3a22', lore="Шипастый шар против кольчуги"),
    W("west_sword", "Рыцарский меч", "west", "infantry", "straight", guard=IRON, grip='#1f3f8a', lore="Прямой обоюдоострый клинок"),
    W("west_noble", "Полуторный меч", "west", "noble", "long", guard=GOLD, grip='#1f3f8a', accent='#e8e8f0', lore="Длинная рукоять — бей хоть одной, хоть двумя"),

    W("steppe_spear", "Пика", "steppe", "militia", "spear", blade=STEEL, guard=BRASS, grip='#7a5a3a', accent='#c03030', lore="Лёгкое копьё всадника с бунчуком"),
    W("steppe_sword", "Сабля", "steppe", "infantry", "sabre", guard=BRASS, grip='#3a2a1a', lore="Изогнутый клинок рубит с оттяжкой — раны кровоточат"),
    W("steppe_noble", "Ханская сабля", "steppe", "noble", "sabre", blade=DAMASK, guard=GOLD, grip='#1f7a7a', accent='#30c0c0', lore="Сабля нойонов, ножны в бирюзе"),

    W("sultanate_spear", "Копьё бедуина", "sultanate", "militia", "spear", blade=STEEL, guard=IRON, grip='#8a6a3a', accent='#2b6cb0', lore="Тонкое длинное острие"),
    W("sultanate_sword", "Скимитар", "sultanate", "infantry", "scimitar", guard=BRASS, grip='#2b6cb0', lore="Широкий изогнутый клинок — глубокие раны"),
    W("sultanate_noble", "Шамшир", "sultanate", "noble", "shamshir", blade=DAMASK, guard=GOLD, grip='#efe6d0', accent='#2b6cb0', lore="Клинок-коготь султанской гвардии"),

    W("clans_axe", "Топор кланов", "clans", "militia", "axe", blade=STEEL, guard=IRON, grip='#6a4a2a', lore="Бородовидный топор — пробивает щиты"),
    W("clans_sword", "Фальката", "clans", "infantry", "falcata", guard=BRASS, grip='#3a5a2a', lore="Клинок с обратным изгибом, тяжёлый удар"),
    W("clans_noble", "Клеймор", "clans", "noble", "claymore", guard=GOLD, grip='#2f6a2a', accent='#c8a040', lore="Огромный двуручник вождей — широкий размах"),
]

# ---------------------------------------------------------------- геометрия

P0 = (1.2, 14.8)  # конец навершия (центр пикселя)
U = (1 / math.sqrt(2), -1 / math.sqrt(2))  # вдоль оружия
N = (1 / math.sqrt(2), 1 / math.sqrt(2))  # поперёк (вниз-вправо)

# s — расстояние вдоль оси от навершия; максимум ≈ 19.5
PROFILES = {
    #        навершие, рукоять, гарда(толщ, полуширина), клинок длина, ширина у гарды, изгиб, сужение к острию
    "broad": dict(pom=1.3, grip=3.2, gt=1.0, gw=3.0, bl=13.0, bw=1.55, curve=0.0, taper=0.35, fuller=True),
    "straight": dict(pom=1.2, grip=3.0, gt=0.9, gw=2.8, bl=13.5, bw=1.25, curve=0.0, taper=0.25, fuller=True),
    "long": dict(pom=1.4, grip=4.2, gt=0.9, gw=3.2, bl=12.6, bw=1.2, curve=0.0, taper=0.25, fuller=True),
    "short": dict(pom=1.6, grip=3.0, gt=1.0, gw=2.0, bl=9.0, bw=1.6, curve=0.0, taper=0.0, fuller=False, leaf=True),
    "sabre": dict(pom=1.0, grip=3.2, gt=0.8, gw=2.2, bl=14.0, bw=1.15, curve=2.0, taper=0.3, fuller=False),
    "scimitar": dict(pom=1.0, grip=3.0, gt=0.8, gw=2.4, bl=13.0, bw=1.3, curve=2.0, taper=-0.6, fuller=False),
    "shamshir": dict(pom=1.0, grip=3.0, gt=0.8, gw=2.0, bl=14.0, bw=0.95, curve=3.2, taper=0.2, fuller=False),
    "falcata": dict(pom=1.6, grip=3.0, gt=0.6, gw=1.0, bl=11.5, bw=1.5, curve=-1.6, taper=-0.45, fuller=False),
    "claymore": dict(pom=1.6, grip=4.6, gt=1.0, gw=4.0, bl=12.5, bw=1.3, curve=0.0, taper=0.2, fuller=True, droop=True),
}


def axis(x, y):
    dx, dy = x - P0[0], y - P0[1]
    return dx * U[0] + dy * U[1], dx * N[0] + dy * N[1]


def sample(w, s, q):
    """Что за материал в точке (s вдоль, q поперёк): None или код."""
    k = w.kind
    if k in PROFILES:
        p = PROFILES[k]
        pom, grip, gt, gw = p["pom"], p["grip"], p["gt"], p["gw"]
        if s < 0:
            return None
        if s < pom:
            return "pommel" if abs(q) <= 0.95 else None
        if s < pom + grip:
            return ("grip_hi" if q < 0 else "grip") if abs(q) <= 0.62 else None
        g0 = pom + grip
        if s < g0 + gt:
            if abs(q) <= gw:
                return "guard"
            return None
        if p.get("droop") and g0 - 1.2 < s < g0 + gt and abs(q) > gw - 1.0 and abs(q) <= gw + 0.3:
            return "guard"
        t = (s - g0 - gt) / p["bl"]
        if t > 1:
            return None
        curve = p["curve"] * t * t
        width = p["bw"] * (1 - p["taper"] * t)
        if p.get("leaf"):
            width = p["bw"] * (1 + 0.25 * math.sin(math.pi * min(1, t * 1.1)))
        if t > 0.82:  # остриё
            width *= max(0.0, (1 - t) / 0.18)
        if k == "scimitar" and t > 0.55:  # расширение к концу
            width *= 1 + 0.6 * (t - 0.55)
        dq = q - curve
        if abs(dq) > width:
            return None
        if p.get("fuller") and abs(dq) < 0.28 and 0.05 < t < 0.75:
            return "fuller"
        if dq < -width * 0.25:
            return "hi"
        if dq > width * 0.2:
            return "lo"
        return "mid"
    if s < 0:
        return None
    if k in ("spear", "pilum"):
        shaft_end = 12.0 if k == "spear" else 10.5
        if 0 <= s < shaft_end:
            if abs(q) <= 0.62:
                return "wood_hi" if q < 0 else "wood"
            return None
        if shaft_end <= s < shaft_end + 0.9:
            return "guard" if abs(q) <= 0.95 else None
        if k == "pilum":
            if s < 17.5:
                return "lo" if abs(q) <= 0.45 else None
            if s < 19.5:
                width = 1.1 * (19.5 - s) / 2.0
                return ("hi" if q < 0 else "mid") if abs(q) <= width else None
            return None
        t = (s - shaft_end - 0.9) / 6.5
        if t > 1:
            return None
        width = 1.7 * math.sin(math.pi * min(1.0, t * 1.05) ** 0.8)
        if abs(q) > width:
            return None
        if abs(q) < 0.25:
            return "fuller"
        return "hi" if q < 0 else "mid"
    if k == "axe":
        if 0 <= s < 16.5:
            if abs(q) <= 0.62:
                return "wood_hi" if q < 0 else "wood"
        if 11.0 <= s <= 16.5:
            edge = 5.4 - 0.36 * (s - 13.8) ** 2
            inner = 0.5 + 0.10 * (s - 13.8) ** 2
            if inner <= q <= edge:
                return "hi" if q > edge - 0.9 else "mid" if q > edge - 2.2 else "lo"
            if -1.5 <= q <= -0.5 and 13.0 <= s <= 14.6:
                return "lo"
        return None
    if k == "mace":
        if 0 <= s < 12.0:
            if abs(q) <= 0.62:
                return "wood_hi" if q < 0 else "wood"
            return None
        cs, r = 14.6, 2.2
        d = math.hypot(s - cs, q)
        if d <= r:
            return "hi" if (s - cs) + (-q) > 0.8 else "mid" if d < r - 0.6 else "lo"
        for a in range(8):
            ang = a * math.pi / 4
            sx, sq = cs + math.cos(ang) * (r + 0.9), math.sin(ang) * (r + 0.9)
            if math.hypot(s - sx, q - sq) <= 0.55:
                return "spike"
        return None
    return None


def render(w):
    pal = {
        "hi": hexc(w.blade["hi"]), "mid": hexc(w.blade["mid"]), "lo": hexc(w.blade["lo"]), "fuller": hexc(w.blade["lo"]),
        "guard": hexc(w.guard), "pommel": hexc(w.pommel), "grip": hexc(w.grip), "grip_hi": shade(hexc(w.grip), 1.3),
        "wood": hexc('#6a4a2a'), "wood_hi": hexc('#8a6a40'), "spike": hexc(w.blade["mid"]),
    }
    if w.kind in ("spear", "pilum", "axe", "mace"):
        pal["wood"] = hexc(w.grip)
        pal["wood_hi"] = shade(hexc(w.grip), 1.3)
    img = Image.new("RGBA", (16, 16), T)
    SS = 4
    for y in range(16):
        for x in range(16):
            votes = {}
            for sy in range(SS):
                for sx in range(SS):
                    s, q = axis(x + (sx + 0.5) / SS, y + (sy + 0.5) / SS)
                    m = sample(w, s, q)
                    if m:
                        votes[m] = votes.get(m, 0) + 1
            if not votes:
                continue
            total = sum(votes.values())
            if total < SS * SS * 0.42:
                continue
            m = max(votes, key=lambda k: (votes[k], k == "hi"))
            img.putpixel((x, y), pal[m])
    # обмотка рукояти: каждый второй пиксель темнее
    for y in range(16):
        for x in range(16):
            c = img.getpixel((x, y))
            if c[3] and c[:3] in (pal["grip"][:3], pal["grip_hi"][:3]) and (x + y) % 2 == 0 and w.kind in PROFILES:
                img.putpixel((x, y), shade(c, 0.72))
    # блик на кончике и самоцвет/бунчук
    if w.accent:
        acc = hexc(w.accent)
        if w.kind in PROFILES:
            p = PROFILES[w.kind]
            s = p["pom"] + p["grip"] + p["gt"] / 2
            gx, gy = P0[0] + U[0] * s, P0[1] + U[1] * s
            img.putpixel((int(gx), int(gy)), acc)
        elif w.kind == "spear":
            s = 12.4
            gx, gy = P0[0] + U[0] * s + N[0] * 1.3, P0[1] + U[1] * s + N[1] * 1.3
            for dx, dy in ((0, 0), (1, 1), (0, 1), (-1, 1), (0, 2)):
                xx, yy = int(gx) + dx, int(gy) + dy
                if 0 <= xx < 16 and 0 <= yy < 16 and img.getpixel((xx, yy))[3] == 0:
                    img.putpixel((xx, yy), shade(acc, 1.0 - 0.1 * dy))
    return outline(img, hexc(w.blade["edge"]) if False else hexc('#1c1a20'))


def outline(img, c):
    src = img.copy()
    for x in range(16):
        for y in range(16):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] > 0:
                        img.putpixel((x, y), c)
                        break
    return img


# ---------------------------------------------------------------- вывод

def w_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


ROLE_MAT = {"militia": "minecraft:cobblestone", "infantry": "minecraft:iron_ingot", "noble": "minecraft:diamond"}
CULTURE_MARK = {
    "north": "minecraft:leather", "empire": "minecraft:gold_nugget", "west": "minecraft:blue_dye",
    "steppe": "minecraft:string", "sultanate": "minecraft:sand", "clans": "minecraft:moss_block",
}
PATTERNS = {
    "spear": ["  M", " S ", "S  "], "pilum": ["  M", " S ", "S  "],
    "axe": ["MM ", "MS ", " S "], "mace": [" MM", " SM", "S  "],
    "short": [" M ", " M ", " S "],
}


def main():
    os.makedirs(f"{A}/textures/item", exist_ok=True)
    for w in WEAPONS:
        img = render(w)
        img.save(f"{A}/textures/item/{w.id}.png")
        w_json(f"{A}/models/item/{w.id}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"regnum:item/{w.id}"}})
        mat = ROLE_MAT[w.role]
        pat = PATTERNS.get(w.kind, [" M ", " M ", "CS "] if w.role != "noble" else [" M ", "CMC", " S "])
        key = {"M": {"item": mat}, "S": {"item": "minecraft:stick"}}
        if "C" in "".join(pat):
            key["C"] = {"item": CULTURE_MARK[w.culture]}
        w_json(f"{D}/recipe/{w.id}.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
                                          "pattern": pat, "key": key, "result": {"id": f"regnum:{w.id}", "count": 1}})
    # локализация: дописываем в существующие файлы
    for lang in ("ru_ru", "en_us"):
        p = f"{A}/lang/{lang}.json"
        data = json.load(open(p, encoding="utf-8"))
        for w in WEAPONS:
            data[f"item.regnum.{w.id}"] = w.name
        json.dump(data, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    java()
    print("weapons:", len(WEAPONS))


def java():
    """kingdom/CultureWeapons.java — реестр оружия и выбор по культуре/роду войск."""
    TIER = {"militia": "STONE", "infantry": "IRON", "noble": "DIAMOND"}
    RAR = {"militia": "COMMON", "infantry": "UNCOMMON", "noble": "RARE"}
    L = ["package com.alkimor.regnum.kingdom;\n", "import net.minecraft.world.item.Item;", "import net.minecraft.world.item.Items;",
         "import net.minecraft.world.item.Rarity;", "import net.minecraft.world.item.Tiers;",
         "import net.neoforged.neoforge.registries.DeferredItem;\n", "import static com.alkimor.regnum.core.ModRegistries.ITEMS;\n",
         "/** Сгенерировано tools/gen_weapons.py — оружие культур. */", "public final class CultureWeapons {",
         "    private CultureWeapons() {}\n"]
    for w in WEAPONS:
        L.append(f'    public static final DeferredItem<CultureWeaponItem> {w.id.upper()} = ITEMS.register("{w.id}", () -> new CultureWeaponItem('
                 f'Tiers.{TIER[w.role]}, CultureWeaponItem.Kind.{w.kind.upper()}, Culture.{w.culture.upper()}, "{w.lore}", '
                 f'new Item.Properties().rarity(Rarity.{RAR[w.role]})));')
    L += ["", "    /** Оружие солдата по культуре и роду войск (лучники — с луком). */",
          "    public static Item forSoldier(Culture c, SoldierType t) {", "        if (t == SoldierType.ARCHER) return Items.BOW;",
          "        return switch (c) {"]
    for cul in ["north", "empire", "west", "steppe", "sultanate", "clans"]:
        ws = {w.role: w.id.upper() for w in WEAPONS if w.culture == cul}
        L.append(f"            case {cul.upper()} -> t == SoldierType.MILITIA ? {ws['militia']}.get() : "
                 f"t == SoldierType.KNIGHT ? {ws['noble']}.get() : {ws['infantry']}.get();")
    L += ["        };", "    }\n", "    public static void init() {}", "}"]
    with open("src/main/java/com/alkimor/regnum/kingdom/CultureWeapons.java", "w", encoding="utf-8") as f:
        f.write("\n".join(L) + "\n")


def preview(path):
    S = Image.new("RGBA", (6 * 16 * 8 + 70, 3 * 16 * 8 + 40), (40, 44, 52, 255))
    for i, w in enumerate(WEAPONS):
        img = render(w).resize((128, 128), Image.NEAREST)
        cx = {"militia": 0, "infantry": 1, "noble": 2}[w.role]
        cy = ["north", "empire", "west", "steppe", "sultanate", "clans"].index(w.culture)
        S.paste(img, (cy * 140, cx * 140), img)
    S.save(path)


if __name__ == "__main__":
    if len(sys.argv) > 1:
        preview(sys.argv[1])
    else:
        main()
