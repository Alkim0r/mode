"""Оригинальные модели боссов Regnum."""
from engine import Model, Mat, Cube, hexc

BONE = Mat('#d9d2bd', '#a79f88', 'bone', 0.06)
DARK_METAL = Mat('#3a3640', '#5a5466', 'plate', 0.06)
ROYAL = Mat('#4a1d6b', '#2e0f45', 'trim', 0.06, tatter=4)
DARK_CLOTH = Mat('#2a2230', '#1a1520', 'noise', 0.08, tatter=2)
GOLD = Mat('#d9a62b', '#a87d18', 'gold', 0.0)
HORN = Mat('#3a3226', '#1e1a14', 'noise', 0.1)
VIOLET_GLOW = Mat('#b46bff', '#7a2fbe', 'glow', 0.1, glow=True)


def skull_face(c, rng):
    dark = hexc('#1a1420')
    c.rect(1, 2, 2, 4, dark)
    c.rect(5, 2, 6, 4, dark)
    c.px(1, 3, hexc('#c46bff'), True)
    c.px(6, 3, hexc('#c46bff'), True)
    c.px(2, 3, hexc('#e6b8ff'), True)
    c.px(5, 3, hexc('#e6b8ff'), True)
    c.rect(3, 5, 4, 5, dark)
    for x in range(1, 7):
        c.px(x, 7, hexc('#f4efe0') if x % 2 else dark)


def ribs(c, rng):
    bone = hexc('#d9d2bd')
    for y in (2, 4, 6, 8):
        c.rect(1, y, c.w - 2, y, bone)
    c.rect(c.w // 2 - 1, 1, c.w // 2, c.h - 2, bone)
    c.px(c.w // 2 - 1, 5, hexc('#c46bff'), True)  # сердце-кристалл
    c.px(c.w // 2, 5, hexc('#c46bff'), True)


def crypt_lord():
    m = Model("crypt_lord", "CryptLordModel", 128, 128, seed=11)
    head = m.part("head", pivot=(0, 0, 0), cubes=[Cube((-4, -8, -4), (8, 8, 8), BONE, decals={"north": skull_face})])
    m.part("crown", "head", cubes=[
        Cube((-4.5, -10, -4.5), (9, 2, 9), GOLD),
        Cube((-4.5, -12, -4.5), (1, 2, 1), GOLD), Cube((-0.5, -13, -4.5), (1, 3, 1), GOLD), Cube((3.5, -12, -4.5), (1, 2, 1), GOLD),
        Cube((-4.5, -12, 3.5), (1, 2, 1), GOLD), Cube((3.5, -12, 3.5), (1, 2, 1), GOLD),
        Cube((-1, -9.6, -4.9), (2, 1, 1), VIOLET_GLOW)])
    m.part("horn_l", "head", pivot=(4, -6, 0), rot=(0, 0, -25), decor=True, cubes=[Cube((0, -1, -1), (3, 2, 2), HORN)])
    m.part("horn_l_tip", "horn_l", pivot=(3, 0, 0), rot=(0, 0, -40), decor=True, cubes=[Cube((0, -0.5, -0.5), (3, 1, 1), HORN)])
    m.part("horn_r", "head", pivot=(-4, -6, 0), rot=(0, 0, 25), decor=True, cubes=[Cube((-3, -1, -1), (3, 2, 2), HORN)])
    m.part("horn_r_tip", "horn_r", pivot=(-3, 0, 0), rot=(0, 0, 40), decor=True, cubes=[Cube((-3, -0.5, -0.5), (3, 1, 1), HORN)])

    m.part("body", cubes=[Cube((-4, 0, -2), (8, 12, 4), DARK_METAL, decals={"north": ribs}),
                          Cube((-4.5, 9, -2.5), (9, 6, 5), DARK_CLOTH)])
    m.part("collar", "body", cubes=[Cube((-5, -1, -3), (10, 2, 6), GOLD)])
    m.part("cape", "body", pivot=(0, 0, 2.2), rot=(8, 0, 0), decor=True, cubes=[Cube((-5.5, 0, 0), (11, 22, 1), ROYAL)])

    m.part("right_arm", pivot=(-5, 2, 0), cubes=[Cube((-1.5, -2, -1), (2, 12, 2), BONE)])
    m.part("pauldron_r", "right_arm", cubes=[Cube((-3.5, -3.5, -3), (5, 4, 6), DARK_METAL)])
    m.part("spike_r", "pauldron_r", decor=True, cubes=[Cube((-2.5, -6, -0.5), (1, 3, 1), GOLD)])
    m.part("left_arm", pivot=(5, 2, 0), cubes=[Cube((-0.5, -2, -1), (2, 12, 2), BONE)])
    m.part("pauldron_l", "left_arm", cubes=[Cube((-1.5, -3.5, -3), (5, 4, 6), DARK_METAL)])
    m.part("spike_l", "pauldron_l", decor=True, cubes=[Cube((1.5, -6, -0.5), (1, 3, 1), GOLD)])

    m.part("right_leg", pivot=(-1.9, 12, 0), cubes=[Cube((-1, 0, -1), (2, 12, 2), BONE), Cube((-1.5, 6, -1.5), (3, 6, 3), DARK_METAL)])
    m.part("left_leg", pivot=(1.9, 12, 0), cubes=[Cube((-1, 0, -1), (2, 12, 2), BONE), Cube((-1.5, 6, -1.5), (3, 6, 3), DARK_METAL)])
    return m


# ------------------------------------------------------------------ Матушка Топь
SWAMP_SKIN = Mat('#6f7d55', '#4f5c3a', 'noise', 0.1)
BOG_ROBE = Mat('#3a3a22', '#2a2a18', 'moss', 0.1, tatter=3)
MOSS = Mat('#4f7a2a', '#2f4f1a', 'moss', 0.12, tatter=3)
MOSS_HAIR = Mat('#3a4a28', '#24301a', 'moss', 0.12, tatter=5)
WOOD = Mat('#4a3a24', '#2e2416', 'wood', 0.08)
CLAW = Mat('#2a2418', '#1a1610', 'noise', 0.05)
YELLOW_GLOW = Mat('#e8e050', '#c0b020', 'glow', 0.1, glow=True)


def hag_face(c, rng):
    c.rect(1, 3, 2, 3, hexc('#e8e050'), True)
    c.rect(5, 3, 6, 3, hexc('#e8e050'), True)
    c.rect(1, 2, 2, 2, hexc('#2a2a18'))
    c.rect(5, 2, 6, 2, hexc('#2a2a18'))
    c.rect(2, 7, 5, 7, hexc('#1a1a10'))
    c.px(3, 8, hexc('#d9d2bd'))
    c.px(6, 5, hexc('#3a4a28'))
    c.px(0, 6, hexc('#3a4a28'))


def lantern(c, rng):
    c.rect(0, 0, c.w - 1, c.h - 1, hexc('#e8c050'), True)


def mire_mother():
    m = Model("mire_mother", "MireMotherModel", 128, 128, seed=21)
    m.part("head", pivot=(0, 0, 0), cubes=[Cube((-4, -9, -4), (8, 9, 8), SWAMP_SKIN, decals={"north": hag_face})])
    m.part("nose", "head", pivot=(0, -4, -4), rot=(-15, 0, 0), cubes=[Cube((-1, -1, -3), (2, 3, 3), SWAMP_SKIN)])
    m.part("hair", "head", decor=True, cubes=[Cube((-4.5, -9.5, 0), (9, 14, 5), MOSS_HAIR)])
    m.part("branch_l", "head", pivot=(3, -9, 0), rot=(0, 0, -20), decor=True, cubes=[Cube((-0.5, -7, -0.5), (1, 7, 1), WOOD)])
    m.part("branch_l2", "branch_l", pivot=(0, -6, 0), rot=(0, 0, -45), decor=True, cubes=[Cube((-0.5, -4, -0.5), (1, 4, 1), WOOD)])
    m.part("branch_r", "head", pivot=(-3, -9, 0), rot=(0, 0, 20), decor=True, cubes=[Cube((-0.5, -7, -0.5), (1, 7, 1), WOOD)])
    m.part("branch_r2", "branch_r", pivot=(0, -5, 0), rot=(0, 0, 45), decor=True, cubes=[Cube((-0.5, -4, -0.5), (1, 4, 1), WOOD)])

    m.part("body", pivot=(0, 0, 0))
    m.part("torso", "body", rot=(12, 0, 0), cubes=[Cube((-5, 0, -3), (10, 12, 6), BOG_ROBE)])
    m.part("shawl", "torso", decor=True, cubes=[Cube((-5.5, -0.5, -3.5), (11, 6, 7), MOSS)])
    m.part("skirt", "body", cubes=[Cube((-6, 9, -4), (12, 12, 8), BOG_ROBE)])
    m.part("lantern", "torso", pivot=(4, 9, -3), decor=True, cubes=[Cube((-1, 0, -1.5), (2, 3, 2), YELLOW_GLOW, decals={"north": lantern})])

    m.part("right_arm", pivot=(-6, 2, 0), cubes=[Cube((-2, -2, -1.5), (3, 17, 3), SWAMP_SKIN), Cube((-2.5, -2.5, -2), (4, 6, 4), MOSS)])
    m.part("claws_r", "right_arm", decor=True, cubes=[Cube((-2, 15, -1.5), (1, 3, 1), CLAW), Cube((-0.5, 15, -1.5), (1, 4, 1), CLAW), Cube((0, 15, 0.5), (1, 3, 1), CLAW)])
    m.part("left_arm", pivot=(6, 2, 0), cubes=[Cube((-1, -2, -1.5), (3, 17, 3), SWAMP_SKIN), Cube((-1.5, -2.5, -2), (4, 6, 4), MOSS)])
    m.part("claws_l", "left_arm", decor=True, cubes=[Cube((0, 15, -1.5), (1, 3, 1), CLAW), Cube((1.5, 15, -1.5), (1, 4, 1), CLAW), Cube((-1, 15, 0.5), (1, 3, 1), CLAW)])
    m.part("right_leg", pivot=(-2, 12, 0), cubes=[Cube((-1.5, 0, -1.5), (3, 12, 3), SWAMP_SKIN)])
    m.part("left_leg", pivot=(2, 12, 0), cubes=[Cube((-1.5, 0, -1.5), (3, 12, 3), SWAMP_SKIN)])
    return m


# ------------------------------------------------------------------ Горновой (на основе скелета голема)
IRON_DARK = Mat('#3e3c40', '#5c5a60', 'cracks', 0.07)
IRON = Mat('#6a6870', '#8c8a92', 'plate', 0.06)
RUST = Mat('#7a4a2a', '#5a3218', 'noise', 0.1)
EMBER = Mat('#ff8a2a', '#ffd060', 'glow', 0.15, glow=True)


def visor(c, rng):
    c.rect(1, 3, c.w - 2, 4, hexc('#ff9a2a'), True)
    c.rect(2, 3, c.w - 3, 3, hexc('#ffe27a'), True)
    for x in (0, c.w - 1):
        c.px(x, 1, hexc('#9c9aa2'))
        c.px(x, c.h - 2, hexc('#9c9aa2'))


def furnace(c, rng):
    w, h = c.w, c.h
    x0, x1 = w // 2 - 4, w // 2 + 3
    for x in range(x0, x1 + 1):
        for y in range(3, h - 2):
            if y == 3 and x in (x0, x1):
                continue
            col = hexc('#ffd060') if rng.random() < 0.35 else hexc('#ff7a1a') if rng.random() < 0.7 else hexc('#e0401a')
            c.px(x, y, col, True)
    c.rect(x0 - 1, 2, x1 + 1, 2, hexc('#2a2a2e'))
    for y in range(3, h - 2):
        c.px(x0 - 1, y, hexc('#2a2a2e'))
        c.px(x1 + 1, y, hexc('#2a2a2e'))


def forgemaster():
    m = Model("forgemaster", "ForgemasterModel", 128, 128, base="golem", seed=31)
    m.part("head", pivot=(0, -7, -2), cubes=[Cube((-4, -12, -5.5), (8, 10, 8), IRON_DARK, decals={"north": visor})])
    m.part("crest", "head", decor=True, cubes=[Cube((-1, -14, -6), (2, 3, 10), IRON)])
    m.part("body", pivot=(0, -7, 0), cubes=[Cube((-9, -2, -6), (18, 12, 11), IRON_DARK, decals={"north": furnace}),
                                           Cube((-4.5, 10, -3), (9, 5, 6), RUST, inflate=0.5)])
    m.part("chimney_l", "body", decor=True, cubes=[Cube((-7, -11, 2), (3, 9, 3), IRON), Cube((-7.5, -12, 1.5), (4, 1, 4), EMBER)])
    m.part("chimney_r", "body", decor=True, cubes=[Cube((4, -9, 2), (3, 7, 3), IRON), Cube((3.5, -10, 1.5), (4, 1, 4), EMBER)])
    m.part("right_arm", pivot=(0, -7, 0), cubes=[Cube((-13, -2.5, -3), (4, 28, 6), IRON_DARK)])
    m.part("pauldron_r", "right_arm", decor=True, cubes=[Cube((-14.5, -4.5, -4), (6, 5, 8), IRON)])
    m.part("hammer", "right_arm", cubes=[Cube((-12, 24, -1), (2, 3, 2), RUST), Cube((-16, 26.5, -4), (10, 5, 8), IRON)])
    m.part("left_arm", pivot=(0, -7, 0), cubes=[Cube((9, -2.5, -3), (4, 28, 6), IRON_DARK)])
    m.part("pauldron_l", "left_arm", decor=True, cubes=[Cube((8.5, -4.5, -4), (6, 5, 8), IRON)])
    m.part("ember_fist", "left_arm", cubes=[Cube((9, 25, -2.5), (4, 3, 5), EMBER)])
    m.part("right_leg", pivot=(-4, 11, 0), cubes=[Cube((-3.5, -3, -3), (6, 16, 5), IRON_DARK)])
    m.part("knee_r", "right_leg", decor=True, cubes=[Cube((-4, 3, -4), (7, 4, 1), IRON)])
    m.part("left_leg", pivot=(5, 11, 0), cubes=[Cube((-3.5, -3, -3), (6, 16, 5), IRON_DARK)])
    m.part("knee_l", "left_leg", decor=True, cubes=[Cube((-4, 3, -4), (7, 4, 1), IRON)])
    return m


# ------------------------------------------------------------------ Сехмет-ра
WRAP = Mat('#d8ccaa', '#a89a78', 'wrap', 0.06)
NEMES = Mat('#d9a62b', '#2b4c9a', 'stripes_h', 0.03)
NEMES_V = Mat('#d9a62b', '#2b4c9a', 'stripes_v', 0.03)
COLLAR = Mat('#2b6cb0', '#d9a62b', 'stripes_h', 0.03)
LINEN = Mat('#efe6cc', '#d9a62b', 'trim', 0.04)
WING = Mat('#1f6f7a', '#4a2f8a', 'wing', 0.0)
CYAN_GLOW = Mat('#6af0ff', '#2ab0d0', 'glow', 0.1, glow=True)


def mummy_face(c, rng):
    c.rect(0, 2, 7, 4, hexc('#1a1410'))
    c.rect(1, 3, 2, 3, hexc('#6af0ff'), True)
    c.rect(5, 3, 6, 3, hexc('#6af0ff'), True)
    c.rect(0, 1, 7, 1, hexc('#d9a62b'))


def scarab_queen():
    m = Model("scarab_queen", "ScarabQueenModel", 128, 128, seed=41)
    m.part("head", pivot=(0, 0, 0), cubes=[Cube((-4, -8, -4), (8, 8, 8), WRAP, decals={"north": mummy_face})])
    m.part("nemes", "head", cubes=[Cube((-4.5, -8.6, -4.5), (9, 3, 9), NEMES), Cube((-4.5, -6, 3.5), (9, 11, 1), NEMES)])
    m.part("flap_l", "head", cubes=[Cube((4, -6, -3), (1, 10, 4), NEMES_V)])
    m.part("flap_r", "head", cubes=[Cube((-5, -6, -3), (1, 10, 4), NEMES_V)])
    m.part("uraeus", "head", decor=True, cubes=[Cube((-0.5, -11, -5), (1, 3, 1), GOLD), Cube((-1, -11.5, -5.2), (2, 2, 1), GOLD)])
    m.part("body", cubes=[Cube((-4, 0, -2), (8, 12, 4), WRAP), Cube((-4.5, 10, -2.6), (9, 7, 5), LINEN)])
    m.part("collar", "body", cubes=[Cube((-5, -0.5, -3), (10, 4, 6), COLLAR), Cube((-4.5, 8, -2.5), (9, 2, 5), GOLD)])
    m.part("wing_r", "body", pivot=(-1.5, 1, 2), rot=(0, 25, -20), decor=True, cubes=[Cube((-15, 0, 0), (15, 17, 1), WING)])
    m.part("wing_l", "body", pivot=(1.5, 1, 2), rot=(0, -25, 20), decor=True, cubes=[Cube((0, 0, 0), (15, 17, 1), WING)])
    m.part("right_arm", pivot=(-5, 2, 0), cubes=[Cube((-3, -2, -2), (4, 12, 4), WRAP)])
    m.part("bracer_r", "right_arm", decor=True, cubes=[Cube((-3.5, 6, -2.5), (5, 2, 5), GOLD)])
    m.part("left_arm", pivot=(5, 2, 0), cubes=[Cube((-1, -2, -2), (4, 12, 4), WRAP)])
    m.part("bracer_l", "left_arm", decor=True, cubes=[Cube((-1.5, 6, -2.5), (5, 2, 5), GOLD)])
    m.part("right_leg", pivot=(-1.9, 12, 0), cubes=[Cube((-2, 0, -2), (4, 12, 4), WRAP)])
    m.part("left_leg", pivot=(1.9, 12, 0), cubes=[Cube((-2, 0, -2), (4, 12, 4), WRAP)])
    m.part("eye_gem", "head", cubes=[Cube((-0.5, -9, -4.7), (1, 1, 1), CYAN_GLOW)])
    return m


from mine_creatures import crawler, crawler_queen

ALL = [crypt_lord, mire_mother, forgemaster, scarab_queen, crawler, crawler_queen]
