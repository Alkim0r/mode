"""Модели войск по культурам и разбойников.

Своя анатомия (не ванильный «человечек»): голова 7×7×7, широкая грудь и узкая талия,
плечо/предплечье/кисть и бедро/голень/сапог — отдельными блоками. У каждой культуры
свой силуэт: шлемы, доспехи, щиты, плащи, подолы, цвета, причёски.
Для каждой модели — 3 варианта текстуры (тон кожи и волосы).
"""
from engine import Model, Mat, Cube, hexc, shade

# (кожа, волосы)
SKINS = [('#c8946e', '#3a2414'), ('#e3b892', '#8a5a2a'), ('#9a6444', '#16100c')]
SKIN_KEY = '#c8946e'
HAIR_KEY = '#3a2414'


# ------------------------------------------------------------------ лица и узоры

def face(beard=None, paint=None, mask=None, scar=False, brows='#2a1a10'):
    """Лицо 7×7. beard: None|full|mustache|goatee|long."""
    def fn(c, rng):
        white, pupil = hexc('#f2ece0'), hexc('#2a2a3a')
        mouth = hexc('#7a3a2e')
        hair = hexc(brows)
        c.rect(1, 2, 2, 2, hair)
        c.rect(4, 2, 5, 2, hair)
        c.px(1, 3, white)
        c.px(2, 3, pupil)
        c.px(4, 3, pupil)
        c.px(5, 3, white)
        sk = c.img.getpixel((c.x0 + 3, c.y0 + 4))
        c.px(3, 4, shade(sk, 0.82))
        c.px(3, 3, shade(sk, 0.92))
        c.rect(2, 5, 4, 5, mouth)
        if paint:  # синяя краска кланов
            p = hexc(paint)
            c.rect(0, 1, 6, 1, p)
            c.px(0, 3, p)
            c.px(6, 3, p)
            c.px(1, 4, p)
            c.px(5, 4, p)
        b = hexc(HAIR_KEY)
        if beard in ("full", "long"):
            c.rect(0, 4, 0, 6, b)
            c.rect(6, 4, 6, 6, b)
            c.rect(1, 5, 5, 6, b)
            c.rect(2, 5, 4, 5, mouth)
            c.px(1, 4, b)
            c.px(5, 4, b)
        elif beard == "mustache":
            c.rect(1, 4, 2, 5, b)
            c.rect(4, 4, 5, 5, b)
        elif beard == "goatee":
            c.rect(2, 6, 4, 6, b)
            c.px(2, 4, b)
            c.px(4, 4, b)
        if mask:
            m = hexc(mask)
            c.rect(0, 4, 6, 6, m)
            c.rect(0, 4, 6, 4, shade(m, 1.15))
        if scar:
            c.px(1, 1, hexc('#9a4a3a'))
            c.px(2, 4, hexc('#9a4a3a'))
            c.px(1, 5, hexc('#9a4a3a'))
    return fn


def emblem(kind, accent):
    def fn(c, rng):
        a = hexc(accent)
        cx, cy = c.w // 2, c.h // 2
        if kind == "cross":
            c.rect(cx - 1, 1, cx, c.h - 2, a)
            c.rect(1, cy - 2, c.w - 2, cy - 1, a)
        elif kind == "eagle":
            c.rect(cx - 1, cy - 2, cx, cy + 2, a)
            c.rect(1, cy - 1, c.w - 2, cy - 1, a)
            c.px(1, cy - 2, a)
            c.px(c.w - 2, cy - 2, a)
            c.rect(cx - 1, cy - 3, cx, cy - 3, a)
            c.px(cx - 2, cy + 3, a)
            c.px(cx + 1, cy + 3, a)
        elif kind == "sun":
            c.rect(cx - 1, cy - 1, cx, cy, a)
            for dx, dy in ((-3, 0), (2, 0), (0, -3), (0, 2), (-2, -2), (1, -2), (-2, 1), (1, 1),
                           (-3, -1), (2, -1), (-1, -3), (-1, 2)):
                c.px(cx + dx, cy + dy, a)
        elif kind == "crescent":
            c.rect(cx - 2, cy - 2, cx - 2, cy + 1, a)
            c.px(cx - 1, cy - 3, a)
            c.px(cx - 1, cy + 2, a)
            c.px(cx, cy - 3, a)
            c.px(cx, cy + 2, a)
            c.px(cx + 1, cy - 1, a)
        elif kind == "tree":
            c.rect(cx - 1, 1, cx, c.h - 2, a)
            c.rect(cx - 3, cy - 2, cx + 2, cy - 2, a)
            c.rect(cx - 2, cy, cx + 1, cy, a)
            c.rect(cx - 3, cy + 2, cx + 2, cy + 2, a)
        elif kind == "lion":
            c.rect(cx - 2, cy - 2, cx + 1, cy + 1, a)
            c.rect(cx - 3, cy - 3, cx - 2, cy - 3, a)
            c.px(cx + 2, cy + 2, a)
            c.px(cx - 3, cy + 2, a)
            c.px(cx + 2, cy - 2, a)
    return fn


def rim(c, r):
    for x in range(c.w):
        c.px(x, 0, r)
        c.px(x, c.h - 1, r)
    for y in range(c.h):
        c.px(0, y, r)
        c.px(c.w - 1, y, r)


def shield_face(kind, accent, rim_col):
    em = emblem(kind, accent)

    def fn(c, rng):
        rim(c, hexc(rim_col))
        em(c, rng)
    return fn


def boss_face(rim_col):
    def fn(c, rng):
        rim(c, hexc(rim_col))
        cx, cy = c.w // 2, c.h // 2
        c.rect(cx - 1, cy - 1, cx, cy, hexc('#c8c0a8'))
        c.px(cx - 1, cy - 1, hexc('#f0ead8'))
        for y in range(2, c.h - 2, 2):
            c.px(1, y, shade(hexc(rim_col), 1.3))
    return fn


def visor_slit(c, rng):
    d = hexc('#121216')
    c.rect(1, 3, c.w - 2, 3, d)
    c.rect(c.w // 2 - 1, 4, c.w // 2 - 1, c.h - 2, d)
    for y in range(5, c.h - 1, 2):
        c.px(2, y, d)
        c.px(c.w - 3, y, d)


def face_mask(c, rng):
    """Личина: прорези для глаз, рельеф носа, щель рта."""
    d = hexc('#121216')
    c.rect(1, 3, 2, 3, d)
    c.rect(c.w - 3, 3, c.w - 2, 3, d)
    c.rect(c.w // 2 - 1, 2, c.w // 2 - 1, 5, shade(c.img.getpixel((c.x0 + c.w // 2, c.y0 + 2)), 1.3))
    c.rect(2, 6, c.w - 4, 6, d)


def caftan_front(color):
    """Запах кафтана/халата: диагональ и пуговицы."""
    def fn(c, rng):
        a = hexc(color)
        for y in range(c.h):
            c.px(c.w // 2 - 2 + y // 3, y, a)
        for y in (1, 3, 5):
            c.px(c.w // 2 + 3, y, a)
    return fn


def open_face(top=1):
    return lambda c, r: c.clear(1, top, c.w - 2, c.h - 1)


# ------------------------------------------------------------------ культуры

CULTURES = {
    # Северное княжество — Русь: шишаки, чешуя, каплевидные щиты, меховые шапки, красное с золотом.
    "north": dict(main='#9a1f1f', accent='#d9a62b', cloth='#7a1a1a', metal='#a2a2aa', leather='#6a4a2a', fur='#5a3a22', linen='#d8cdb0'),
    # Аврелийская империя — Рим/Византия: галеа, сегментата, птеруги, скутум, бронза и пурпур.
    "empire": dict(main='#5a1d6b', accent='#d9a62b', cloth='#9a2230', metal='#b98a3a', leather='#7a4a2a', fur='#4a3020', linen='#e0d6bc'),
    # Королевство Вальдмарк — норманны/франки: шапели, топфхельмы, гербовые сюрко, щиты-«утюги».
    "west": dict(main='#1f3f8a', accent='#e8e8f0', cloth='#2a4a9a', metal='#c4c6ce', leather='#5a3a22', fur='#4a3a2a', linen='#d8d0b8'),
    # Степная орда — монголы/кипчаки: остроконечные шлемы с плюмажем, ламелляр, халаты, мех.
    "steppe": dict(main='#1f7a7a', accent='#d9a62b', cloth='#2a6a7a', metal='#8a8478', leather='#7a5a3a', fur='#8a6a4a', linen='#d8cdb0'),
    # Песчаный султанат — арабы/мамлюки: тюрбаны, куфии, кольчужные бармицы, длинные халаты.
    "sultanate": dict(main='#e8dcc0', accent='#2b6cb0', cloth='#e8dcc0', metal='#b0b0b8', leather='#8a6a3a', fur='#6a4a2a', linen='#efe6d0'),
    # Лесные кланы — кельты: без шлемов, косы, синяя краска, клетчатые килты, деревянные щиты.
    "clans": dict(main='#2f6a2a', accent='#c8a040', cloth='#3a5a2a', metal='#8a8a80', leather='#5a4026', fur='#6a5a3a', linen='#cfc4a4'),
}

TYPES = ["militia", "swordsman", "archer", "knight", "shieldman", "spearman",
         "greatsword", "light_cav", "heavy_cav", "horse_archer", "crossbow", "musketeer", "bombardier", "scout"]


def model_class(culture, kind):
    return "Unit" + culture.capitalize() + "".join(part.capitalize() for part in kind.split("_")) + "Model"


def mats(cul):
    return dict(
        skin=Mat(SKIN_KEY, None, 'skin', 0.05, edge=0.94),
        hair=Mat(HAIR_KEY, '#24160c', 'hair', 0.08, edge=0.9),
        main_trim=Mat(cul["main"], None, 'trim', 0.05),
        main_cloth=Mat(cul["main"], None, 'cloth', 0.06),
        cloth=Mat(cul["cloth"], None, 'cloth', 0.07),
        linen=Mat(cul["linen"], None, 'cloth', 0.05),
        mail=Mat(cul["metal"], '#4a4a52', 'mail', 0.05),
        scale=Mat(cul["metal"], '#4a4a52', 'scale', 0.05),
        plate=Mat(cul["metal"], '#e8e8f0', 'plate', 0.06, edge=0.78),
        lamellar=Mat(cul["metal"], cul["main"], 'lamellar', 0.05),
        segmented=Mat(cul["metal"], None, 'segmented', 0.04, edge=0.8),
        leather=Mat(cul["leather"], '#c8a878', 'leather', 0.08),
        quilt=Mat('#c8b48a', '#9a865c', 'quilt', 0.05),
        fur=Mat(cul["fur"], '#2e2216', 'fur', 0.1, edge=0.92),
        gold=Mat('#d9a62b', None, 'gold', 0.0, edge=0.85),
        bronze=Mat('#b98a3a', '#e0b860', 'plate', 0.05, edge=0.8),
        wood=Mat('#7a5a36', '#5a3e22', 'wood', 0.06),
        plaid=Mat(cul["main"], '#a8382a', 'tartan', 0.04),
        white=Mat('#ece6d6', '#c8bea8', 'cloth', 0.04),
        boots=Mat('#3a2a1a', '#6a5038', 'leather', 0.06),
        plume=Mat('#b02020', '#801010', 'fur', 0.08, edge=0.95),
        black=Mat('#1a1614', None, 'noise', 0.05),
    )


# ------------------------------------------------------------------ анатомия

def build_body(m, M, torso, waist, arm_upper, arm_fore, hands, thigh, shin, boots, heavy=False):
    """Грудь шире талии, конечности из сегментов."""
    cw = 9 if heavy else 8
    cd = 5 if heavy else 4.5
    m.part("body", cubes=[
        Cube((-cw / 2, 0, -cd / 2), (cw, 6.5, cd), torso, decals=getattr(torso, "_decals", None)),
        Cube((-3.5, 6.5, -2), (7, 4, 4), waist),
        Cube((-4, 10, -2.25), (8, 2.5, 4.5), waist),
    ])
    m.part("neck", "body", cubes=[Cube((-1.5, -1, -1.5), (3, 1, 3), M["skin"])])
    ax = 5.5 if heavy else 5
    uw = 4 if heavy else 3.5
    m.part("right_arm", pivot=(-ax, 2, 0), cubes=[
        Cube((-uw + 1, -2, -uw / 2), (uw, 6, uw), arm_upper),
        Cube((-2.25, 4, -1.5), (3, 5, 3), arm_fore),
        Cube((-2.5, 8, -1.75), (3.5, 2.5, 3.5), hands),
    ])
    m.part("left_arm", pivot=(ax, 2, 0), cubes=[
        Cube((-1, -2, -uw / 2), (uw, 6, uw), arm_upper, mirror=True),
        Cube((-0.75, 4, -1.5), (3, 5, 3), arm_fore, mirror=True),
        Cube((-1, 8, -1.75), (3.5, 2.5, 3.5), hands, mirror=True),
    ])
    for side, px in (("right_leg", -1.9), ("left_leg", 1.9)):
        mir = side == "left_leg"
        m.part(side, pivot=(px, 12, 0), cubes=[
            Cube((-2, 0, -2), (4, 6, 4), thigh, mirror=mir),
            Cube((-1.75, 6, -1.75), (3.5, 3.5, 3.5), shin, mirror=mir),
            Cube((-2, 9, -2.75), (4, 3, 4.75), boots, mirror=mir),
        ])


def head(m, M, beard=None, paint=None, mask=None, hair=None, scar=False):
    """Голова 7×7×7 + причёска (если не закрыта шлемом)."""
    m.part("head", cubes=[Cube((-3.5, -7, -3.5), (7, 7, 7), M["skin"],
                               decals={"north": face(beard=beard, paint=paint, mask=mask, scar=scar)})])
    if beard == "long":
        m.part("beard", "head", decor=True, cubes=[Cube((-2.5, 0, -3.6), (5, 3, 2), M["hair"])])
    if hair == "short":
        m.part("hair", "head", cubes=[Cube((-3.75, -7.25, -3.75), (7.5, 2.5, 7.5), M["hair"]),
                                      Cube((-3.75, -4.75, 0.5), (7.5, 3, 3.25), M["hair"])])
    elif hair == "bowl":
        m.part("hair", "head", cubes=[Cube((-3.75, -7.25, -3.75), (7.5, 3, 7.5), M["hair"]),
                                      Cube((-3.75, -4.25, -1), (7.5, 2, 4.75), M["hair"])])
    elif hair == "wild":
        m.part("hair", "head", cubes=[Cube((-4, -7.5, -4), (8, 3, 8), M["hair"]),
                                      Cube((-4, -4.5, -1), (8, 5, 5), M["hair"])])
        m.part("braid_l", "head", decor=True, pivot=(3.5, -3, -1.5), rot=(0, 0, -6), cubes=[Cube((0, 0, -0.75), (1.5, 7, 1.5), M["hair"])])
        m.part("braid_r", "head", decor=True, pivot=(-3.5, -3, -1.5), rot=(0, 0, 6), cubes=[Cube((-1.5, 0, -0.75), (1.5, 7, 1.5), M["hair"])])
    elif hair == "braid":
        m.part("hair", "head", cubes=[Cube((-3.75, -7.25, -3.75), (7.5, 2, 7.5), M["hair"])])
        m.part("braid", "head", decor=True, pivot=(0, -3, 3.6), rot=(12, 0, 0), cubes=[Cube((-0.75, 0, 0), (1.5, 9, 1.5), M["hair"])])


def arm_shield(m, shape, mat, face_fn, boss_mat):
    """Щит на левом предплечье (вместо ванильного щита)."""
    if shape == "kite":
        cubes = [Cube((0, -1, -4.5), (1, 9, 9), mat, decals={"east": face_fn}), Cube((0, 8, -3.5), (1, 3, 7), mat),
                 Cube((0, 11, -2), (1, 2, 4), mat), Cube((0, 13, -1), (1, 1, 2), mat)]
        pivot = (2.6, 3, 0)
    elif shape == "heater":
        cubes = [Cube((0, -1, -4), (1, 8, 8), mat, decals={"east": face_fn}), Cube((0, 7, -3), (1, 2, 6), mat),
                 Cube((0, 9, -1.5), (1, 1.5, 3), mat)]
        pivot = (2.6, 3, 0)
    elif shape == "scutum":
        cubes = [Cube((0, -3, -4.5), (1.5, 15, 9), mat, decals={"east": face_fn}), Cube((1.3, 3, -1), (0.8, 2, 2), boss_mat)]
        pivot = (2.6, 1, 0)
    elif shape == "small_round":
        cubes = [Cube((0, -0.5, -3.5), (1, 7, 7), mat, decals={"east": face_fn}), Cube((0, 0.5, -4.5), (1, 5, 1), mat),
                 Cube((0, 0.5, 3.5), (1, 5, 1), mat), Cube((1, 2, -1), (1, 2, 2), boss_mat)]
        pivot = (2.6, 4, 0)
    else:  # round
        cubes = [Cube((0, -1.5, -4.5), (1, 9, 9), mat, decals={"east": face_fn}), Cube((0, -0.5, -5.5), (1, 7, 1), mat),
                 Cube((0, -0.5, 4.5), (1, 7, 1), mat), Cube((0, -2.5, -3.5), (1, 1, 7), mat), Cube((0, 7.5, -3.5), (1, 1, 7), mat),
                 Cube((1, 2, -1), (1.2, 2, 2), boss_mat)]
        pivot = (2.6, 3, 0)
    m.part("shield", "left_arm", pivot=pivot, cubes=cubes)


def cape(m, mat, length=19, pivot_z=2.4, width=10, decor=True, name="cape"):
    m.part(name, "body", pivot=(0, 0.2, pivot_z), rot=(5, 0, 0), decor=decor,
           cubes=[Cube((-width / 2, 0, 0), (width, length, 1), mat)])


def skirt(m, mat, top=10, length=6, split=False, wide=9, name="skirt"):
    if split:
        m.part(name, "body", cubes=[Cube((-wide / 2, top, -2.6), (wide / 2 - 0.2, length, 5.2), mat),
                                    Cube((0.2, top, -2.6), (wide / 2 - 0.2, length, 5.2), mat, mirror=True)])
    else:
        m.part(name, "body", cubes=[Cube((-wide / 2, top, -2.6), (wide, length, 5.2), mat)])


def belt(m, M, mat=None, buckle=True):
    m.part("belt", "body", cubes=[Cube((-4.25, 9.5, -2.5), (8.5, 1.5, 5), mat or M["leather"])] +
           ([Cube((-1, 9.25, -2.75), (2, 2, 0.5), M["gold"])] if buckle else []))


def quiver(m, M, side=-1):
    m.part("quiver", "body", pivot=(side * 1.5, 1, 2.4), rot=(0, 0, side * -28), decor=True,
           cubes=[Cube((-1.5, -1, 0), (3, 10, 2.5), M["leather"]),
                  Cube((-1.25, -3, 0.25), (0.75, 2, 0.75), M["white"]), Cube((0.25, -3.5, 0.75), (0.75, 2.5, 0.75), M["white"]),
                  Cube((-0.5, -2.5, 1.25), (0.75, 1.5, 0.75), M["white"])])


def pauldrons(m, mat, size=(5, 4, 5.5), layered=None):
    w, h, d = size
    extra_r = [Cube((-w + 1.25, -3 + h - 0.5, -d / 2 + 0.25), (w - 0.5, layered[1], d - 0.5), layered[0])] if layered else []
    extra_l = [Cube((-1.25, -3 + h - 0.5, -d / 2 + 0.25), (w - 0.5, layered[1], d - 0.5), layered[0], mirror=True)] if layered else []
    m.part("pauldron_r", "right_arm", decor=True, cubes=[Cube((-w + 1.5, -3, -d / 2), (w, h, d), mat)] + extra_r)
    m.part("pauldron_l", "left_arm", decor=True, cubes=[Cube((-1.5, -3, -d / 2), (w, h, d), mat, mirror=True)] + extra_l)


def plume(m, parent, mat, pivot, size=(1, 6, 1), rot=(-15, 0, 0), name="plume"):
    m.part(name, parent, pivot=pivot, rot=rot, decor=True, cubes=[Cube((-size[0] / 2, -size[1], -size[2] / 2), size, mat)])


# ------------------------------------------------------------------ культуры: сборка

def north(m, M, cul, kind):
    if kind == "militia":
        build_body(m, M, M["quilt"], M["quilt"], M["quilt"], M["linen"], M["skin"], M["linen"],
                   Mat('#cfc4a4', '#6a4a2a', 'stripes_h', 0.05), M["boots"])
        head(m, M, beard="full", hair="short")
        m.part("hat", "head", cubes=[Cube((-4, -7.6, -4), (8, 2.5, 8), M["fur"]), Cube((-3, -9.6, -3), (6, 2.2, 6), M["main_cloth"])])
        skirt(m, M["quilt"], length=5)
        belt(m, M, buckle=False)
        return
    if kind == "archer":
        kaftan = Mat(cul["main"], '#d9a62b', 'trim2', 0.05)
        build_body(m, M, kaftan, M["main_cloth"], M["main_cloth"], M["leather"], M["skin"], M["linen"], M["linen"], M["boots"])
        head(m, M, beard="mustache", hair="short")
        m.part("hat", "head", cubes=[Cube((-4, -7.6, -4), (8, 2.5, 8), M["fur"]), Cube((-3, -10, -3), (6, 2.6, 6), M["main_cloth"])])
        skirt(m, M["main_trim"], length=7, split=True)
        belt(m, M)
        quiver(m, M)
        return
    if kind == "swordsman":
        build_body(m, M, M["scale"], M["scale"], M["main_cloth"], M["main_cloth"], M["leather"], M["main_cloth"], M["leather"], M["boots"])
        head(m, M, beard="full")
        m.part("helm", "head", cubes=[Cube((-3.9, -7.5, -3.9), (7.8, 2.5, 7.8), M["plate"]),
                                      Cube((-3, -9.5, -3), (6, 2, 6), M["plate"]),
                                      Cube((-1.75, -11, -1.75), (3.5, 1.5, 3.5), M["plate"]),
                                      Cube((-0.5, -12.75, -0.5), (1, 1.75, 1), M["gold"]),
                                      Cube((-0.5, -5, -4.4), (1, 2.5, 0.75), M["plate"])])
        m.part("aventail", "head", cubes=[Cube((-3.9, -5, -1), (7.8, 5.5, 4.9), M["mail"]),
                                          Cube((-3.9, -5, -3.9), (0.5, 4, 3), M["mail"]), Cube((3.4, -5, -3.9), (0.5, 4, 3), M["mail"])])
        skirt(m, M["scale"], length=6)
        belt(m, M)
        arm_shield(m, "kite", Mat(cul["main"], None, 'cloth', 0.04), shield_face("sun", cul["accent"], '#6a6a72'), M["plate"])
        return
    # дружинник
    build_body(m, M, M["lamellar"], M["scale"], M["scale"], M["plate"], M["plate"], M["scale"], M["plate"], M["boots"], heavy=True)
    head(m, M, beard="full")
    m.part("helm", "head", cubes=[Cube((-4, -7.6, -4), (8, 8.1, 8), M["plate"], decals={"north": face_mask}),
                                  Cube((-3.2, -9.6, -3.2), (6.4, 2, 6.4), M["plate"]),
                                  Cube((-2, -11.2, -2), (4, 1.6, 4), M["gold"]),
                                  Cube((-0.5, -13, -0.5), (1, 1.8, 1), M["gold"])])
    m.part("aventail", "head", cubes=[Cube((-4.1, -2.5, -4.1), (8.2, 3.5, 8.2), M["mail"])])
    plume(m, "helm", M["plume"], (0, -13, 0), size=(1.5, 5, 1.5), rot=(-25, 0, 0))
    pauldrons(m, M["plate"], size=(5, 4, 6), layered=(M["lamellar"], 2.5))
    skirt(m, M["lamellar"], length=7, split=True, wide=10)
    belt(m, M)
    cape(m, Mat(cul["main"], None, 'trim', 0.05, tatter=1), length=21, width=11, pivot_z=2.7)
    m.part("collar", "body", decor=True, cubes=[Cube((-5, -1.25, -2.9), (10, 2.25, 5.8), M["fur"])])
    arm_shield(m, "kite", Mat(cul["main"], None, 'cloth', 0.04), shield_face("sun", cul["accent"], '#d9a62b'), M["gold"])


def empire(m, M, cul, kind):
    tunic = Mat(cul["cloth"], '#d9a62b', 'trim', 0.05)
    caligae = Mat('#5a3a20', '#3a2412', 'stripes_h', 0.05)
    felt = Mat('#a88a5a', None, 'noise', 0.05)
    if kind == "militia":
        build_body(m, M, tunic, tunic, tunic, M["skin"], M["skin"], M["skin"], M["leather"], caligae)
        head(m, M, hair="short")
        m.part("hat", "head", cubes=[Cube((-3.8, -7.6, -3.8), (7.6, 2, 7.6), felt), Cube((-2.8, -9.3, -2.8), (5.6, 1.7, 5.6), felt),
                                     Cube((-1.5, -10.5, -1.5), (3, 1.2, 3), felt)])
        skirt(m, tunic, length=5)
        belt(m, M)
        return
    if kind == "archer":
        build_body(m, M, Mat('#b98a3a', '#4a3a22', 'scale', 0.04), tunic, tunic, M["leather"], M["skin"], M["skin"], M["leather"], caligae)
        head(m, M, hair="short")
        m.part("helm", "head", cubes=[Cube((-3.9, -7.5, -3.9), (7.8, 2.5, 7.8), M["bronze"]),
                                      Cube((-2.5, -9.3, -2.5), (5, 1.8, 5), M["bronze"]),
                                      Cube((-0.5, -10.8, -0.5), (1, 1.5, 1), M["bronze"])])
        skirt(m, tunic, length=5)
        belt(m, M)
        quiver(m, M)
        return
    if kind == "swordsman":
        seg = M["segmented"].but(base='#a6a8b0')
        build_body(m, M, seg, seg, tunic, M["skin"], M["leather"], tunic, M["bronze"], caligae)
        head(m, M, hair="short")
        steel = M["plate"].but(base='#a6a8b0')
        m.part("helm", "head", cubes=[Cube((-3.9, -7.6, -3.9), (7.8, 3, 7.8), steel),
                                      Cube((-3.9, -4.6, 1), (7.8, 2, 3.2), steel),
                                      Cube((-4.3, -3.6, 2.5), (8.6, 0.8, 2.5), steel),
                                      Cube((-4, -5, -3.6), (0.6, 4, 3), M["bronze"]), Cube((3.4, -5, -3.6), (0.6, 4, 3), M["bronze"]),
                                      Cube((-3.9, -6.2, -4.1), (7.8, 0.6, 0.6), M["bronze"])])
        m.part("crest", "helm", decor=True, cubes=[Cube((-0.6, -10.5, -3.5), (1.2, 3, 7), M["plume"]),
                                                   Cube((-0.4, -8.6, -0.5), (0.8, 1, 1), M["bronze"])])
        pauldrons(m, seg, size=(4.5, 3, 5), layered=(seg, 2))
        m.part("pteruges", "body", cubes=[Cube((-4.5, 10, -2.6), (9, 5, 5.2), Mat(cul["leather"], '#d9a62b', 'stripes_v', 0.05))])
        belt(m, M, M["bronze"])
        arm_shield(m, "scutum", Mat(cul["main"], None, 'cloth', 0.04), shield_face("eagle", cul["accent"], '#d9a62b'), M["bronze"])
        return
    # катафракт
    sc = Mat('#b98a3a', '#5a4022', 'scale', 0.04)
    build_body(m, M, sc, sc, sc, sc, M["bronze"], sc, sc, M["bronze"], heavy=True)
    head(m, M)
    m.part("helm", "head", cubes=[Cube((-4, -7.6, -4), (8, 8.1, 8), M["bronze"], decals={"north": face_mask}),
                                  Cube((-2.8, -9.4, -2.8), (5.6, 1.8, 5.6), M["bronze"]),
                                  Cube((-0.5, -10.6, -0.5), (1, 1.2, 1), M["gold"])])
    plume(m, "helm", Mat('#6a2a8a', None, 'fur', 0.06, edge=0.95), (0, -10.4, 0), size=(1.5, 7, 1.5), rot=(-35, 0, 0))
    pauldrons(m, sc, size=(5, 4.5, 6), layered=(sc, 3))
    skirt(m, sc, length=8, split=True, wide=10)
    belt(m, M, M["gold"])
    cape(m, Mat(cul["main"], None, 'trim', 0.05), length=20, width=11, pivot_z=2.7)
    arm_shield(m, "round", Mat(cul["main"], None, 'cloth', 0.04), shield_face("eagle", cul["accent"], '#d9a62b'), M["gold"])


def west(m, M, cul, kind):
    surcoat = Mat(cul["main"], None, 'cloth', 0.05)
    hose = Mat('#3a3a4a', None, 'cloth', 0.05)
    kettle = [Cube((-3.9, -7.8, -3.9), (7.8, 3, 7.8), M["plate"]), Cube((-5.5, -5.2, -5.5), (11, 0.7, 11), M["plate"])]
    if kind == "militia":
        jack = Mat('#7a8098', '#5a6078', 'quilt', 0.05)
        build_body(m, M, jack, jack, jack, jack, M["leather"], hose, hose, M["boots"])
        head(m, M, beard="goatee", hair="bowl")
        m.part("helm", "head", cubes=kettle)
        skirt(m, jack, length=5)
        belt(m, M)
        return
    if kind == "archer":
        brig = Mat(cul["main"], '#d0d0d8', 'brigandine', 0.05)
        build_body(m, M, brig, brig, M["quilt"], M["quilt"], M["leather"], hose, hose, M["boots"])
        head(m, M, hair="bowl")
        m.part("helm", "head", cubes=kettle)
        skirt(m, M["quilt"], length=5)
        belt(m, M)
        quiver(m, M)
        return
    if kind == "swordsman":
        build_body(m, M, M["mail"], M["mail"], M["mail"], M["mail"], M["leather"], M["mail"], M["mail"], M["boots"])
        head(m, M, beard="goatee")
        m.part("coif", "head", cubes=[Cube((-3.9, -7.4, -3.9), (7.8, 7.9, 7.8), M["mail"], decals={"north": open_face(2)})])
        m.part("helm", "head", cubes=[Cube((-4, -7.8, -4), (8, 3.2, 8), M["plate"]),
                                      Cube((-0.5, -5, -4.6), (1, 3, 0.7), M["plate"])])
        m.part("tabard", "body", cubes=[Cube((-4.25, -0.1, -2.6), (8.5, 15, 0.6), surcoat, decals={"north": emblem("lion", cul["accent"])}),
                                        Cube((-4.25, -0.1, 2.0), (8.5, 15, 0.6), surcoat)])
        belt(m, M)
        arm_shield(m, "heater", Mat(cul["main"], None, 'cloth', 0.04), shield_face("lion", '#e8c040', '#c4c6ce'), M["plate"])
        return
    # рыцарь
    white = Mat('#ecece6', None, 'cloth', 0.04)
    build_body(m, M, M["plate"], M["mail"], M["plate"], M["plate"], M["plate"], M["mail"], M["plate"], M["plate"], heavy=True)
    head(m, M)
    m.part("helm", "head", cubes=[Cube((-4.1, -7.8, -4.1), (8.2, 8.3, 8.2), M["plate"], decals={"north": visor_slit}),
                                  Cube((-3.8, -8.3, -3.8), (7.6, 0.6, 7.6), M["plate"])])
    m.part("crest", "helm", decor=True, cubes=[Cube((-0.5, -11.5, -2.5), (1, 3.2, 5), Mat(cul["main"], None, 'noise', 0.05)),
                                               Cube((-2.5, -10.5, -0.5), (5, 2, 1), Mat('#e8c040', None, 'gold', 0.0))])
    m.part("surcoat", "body", cubes=[Cube((-4.75, -0.1, -2.85), (9.5, 17, 5.7), white,
                                          decals={"north": emblem("cross", cul["main"]), "south": emblem("cross", cul["main"])})])
    pauldrons(m, M["plate"], size=(5, 4, 6), layered=(M["plate"], 2))
    belt(m, M)
    cape(m, Mat(cul["main"], None, 'trim', 0.05), length=20, width=11, pivot_z=2.95)
    arm_shield(m, "heater", Mat(cul["main"], None, 'cloth', 0.04), shield_face("lion", '#e8c040', '#d9a62b'), M["gold"])


def steppe(m, M, cul, kind):
    deel = Mat(cul["cloth"], '#d9a62b', 'trim2', 0.05)
    deel_front = Mat(cul["cloth"], '#d9a62b', 'trim2', 0.05)
    deel_front._decals = {"north": caftan_front('#d9a62b')}
    boots = Mat('#4a2e1a', '#7a5a3a', 'leather', 0.06)
    lam = M["lamellar"]
    if kind in ("militia", "archer"):
        build_body(m, M, deel_front, deel, deel, deel, M["leather"], deel, boots, boots)
        head(m, M, beard="mustache", hair="braid")
        m.part("hat", "head", cubes=[Cube((-4.3, -7.2, -4.3), (8.6, 2.2, 8.6), M["fur"]),
                                     Cube((-3, -10, -3), (6, 2.8, 6), M["main_cloth"]),
                                     Cube((-1.5, -11.6, -1.5), (3, 1.6, 3), M["main_cloth"]),
                                     Cube((-0.5, -12.6, -0.5), (1, 1, 1), M["gold"])])
        skirt(m, deel, length=8, split=True, wide=9.4)
        belt(m, M, Mat('#d9a62b', None, 'cloth', 0.04))
        if kind == "archer":
            quiver(m, M)
            m.part("bow_case", "body", pivot=(4.2, 9, 0), rot=(0, 0, -12), decor=True,
                   cubes=[Cube((0, 0, -1.5), (1.5, 7, 3), M["leather"])])
        return
    if kind == "swordsman":
        build_body(m, M, lam, lam, deel, deel, M["leather"], deel, boots, boots)
        head(m, M, beard="mustache")
        m.part("helm", "head", cubes=[Cube((-4.3, -6.8, -4.3), (8.6, 1.8, 8.6), M["fur"]),
                                      Cube((-3.6, -8.8, -3.6), (7.2, 2, 7.2), M["plate"]),
                                      Cube((-2.6, -10.6, -2.6), (5.2, 1.8, 5.2), M["plate"]),
                                      Cube((-1.4, -12.2, -1.4), (2.8, 1.6, 2.8), M["plate"]),
                                      Cube((-0.5, -14.4, -0.5), (1, 2.2, 1), M["gold"])])
        m.part("aventail", "head", cubes=[Cube((-4, -5, -0.5), (8, 6, 4.5), lam)])
        plume(m, "helm", Mat('#2a2018', None, 'hair', 0.08), (0, -14.2, 0.3), size=(1.2, 5, 1.2), rot=(-45, 0, 0))
        skirt(m, lam, length=7, split=True)
        belt(m, M)
        arm_shield(m, "small_round", Mat(cul["main"], None, 'wood', 0.05), shield_face("crescent", cul["accent"], '#3a2a1a'), M["plate"])
        return
    # нойон
    build_body(m, M, lam, lam, lam, M["plate"], M["leather"], lam, boots, boots, heavy=True)
    head(m, M, beard="mustache")
    m.part("helm", "head", cubes=[Cube((-4.4, -6.8, -4.4), (8.8, 1.8, 8.8), M["fur"]),
                                  Cube((-3.6, -8.9, -3.6), (7.2, 2.2, 7.2), M["gold"]),
                                  Cube((-2.6, -10.8, -2.6), (5.2, 1.9, 5.2), M["plate"]),
                                  Cube((-1.4, -12.4, -1.4), (2.8, 1.6, 2.8), M["gold"]),
                                  Cube((-0.5, -15.4, -0.5), (1, 3, 1), M["gold"]),
                                  Cube((-3.6, -6, -4.3), (7.2, 0.8, 0.8), M["gold"])])
    m.part("aventail", "head", cubes=[Cube((-4.2, -5, -4.2), (8.4, 6, 8.4), lam, decals={"north": lambda c, r: c.clear(1, 0, c.w - 2, 3)})])
    plume(m, "helm", Mat('#e8e0d0', None, 'hair', 0.06), (0, -15, 0.5), size=(1.5, 8, 1.5), rot=(-50, 0, 0))
    pauldrons(m, lam, size=(5.5, 4, 6), layered=(lam, 3))
    skirt(m, lam, length=9, split=True, wide=10)
    belt(m, M, M["gold"])
    m.part("collar", "body", decor=True, cubes=[Cube((-5, -1.25, -2.9), (10, 2.25, 5.8), M["fur"])])
    cape(m, Mat(cul["main"], None, 'trim', 0.05), length=18, width=10, pivot_z=2.7)
    arm_shield(m, "small_round", Mat(cul["main"], None, 'cloth', 0.05), shield_face("crescent", cul["accent"], '#d9a62b'), M["gold"])


def sultanate(m, M, cul, kind):
    robe = Mat('#efe6d0', '#2b6cb0', 'trim2', 0.04)
    stripes = Mat('#efe6d0', '#2b6cb0', 'stripes_v', 0.04)
    sash = Mat('#2b6cb0', None, 'cloth', 0.05)
    sandal = Mat('#8a6a3a', '#5a4022', 'stripes_h', 0.05)
    if kind in ("militia", "archer"):
        build_body(m, M, robe, robe, robe, robe, M["skin"], robe, M["skin"], sandal)
        head(m, M, beard="full", mask='#2b6cb0' if kind == "archer" else None)
        m.part("keffiyeh", "head", cubes=[Cube((-3.9, -7.6, -3.9), (7.8, 3, 7.8), M["white"]),
                                          Cube((-3.9, -4.6, -1), (7.8, 6.5, 4.9), M["white"]),
                                          Cube((-4.1, -6.8, -4.1), (8.2, 0.8, 8.2), M["black"])])
        skirt(m, robe, length=11, wide=9.2)
        m.part("sash", "body", cubes=[Cube((-4.3, 9, -2.5), (8.6, 2, 5), sash), Cube((2, 10.5, -2.6), (1.5, 5, 0.5), sash)])
        if kind == "archer":
            quiver(m, M)
        return
    if kind == "swordsman":
        build_body(m, M, M["mail"], sash, robe, M["mail"], M["leather"], robe, M["leather"], M["boots"])
        head(m, M, beard="full")
        m.part("helm", "head", cubes=[Cube((-4, -7.6, -4), (8, 3.4, 8), M["white"]),
                                      Cube((-2.6, -9.6, -2.6), (5.2, 2.4, 5.2), M["plate"]),
                                      Cube((-0.5, -11.8, -0.5), (1, 2.2, 1), M["plate"])])
        m.part("veil", "head", cubes=[Cube((-3.9, -3.2, -4.1), (7.8, 3.7, 8), M["mail"])])
        skirt(m, stripes, length=10, wide=9.2)
        belt(m, M, sash)
        arm_shield(m, "round", Mat('#2b6cb0', None, 'cloth', 0.04), shield_face("sun", '#d9a62b', '#b0b0b8'), M["plate"])
        return
    # мамлюк
    build_body(m, M, M["lamellar"].but(second='#2b6cb0'), sash, M["mail"], M["plate"], M["plate"], robe, M["plate"], M["boots"], heavy=True)
    head(m, M, beard="full")
    m.part("helm", "head", cubes=[Cube((-4.2, -7.6, -4.2), (8.4, 3.6, 8.4), M["white"]),
                                  Cube((-3, -9.8, -3), (6, 2.6, 6), M["gold"]),
                                  Cube((-1.5, -11.4, -1.5), (3, 1.6, 3), M["plate"]),
                                  Cube((-0.5, -14, -0.5), (1, 2.6, 1), M["gold"]),
                                  Cube((-0.5, -5, -4.8), (1, 3.4, 0.7), M["gold"])])
    m.part("veil", "head", cubes=[Cube((-4, -3.4, -4.2), (8, 3.9, 8.2), M["mail"])])
    pauldrons(m, M["plate"], size=(5, 3.5, 6), layered=(M["mail"], 3))
    skirt(m, stripes, length=10, split=True, wide=10)
    belt(m, M, M["gold"])
    cape(m, stripes, length=20, width=11, pivot_z=2.7)
    arm_shield(m, "round", Mat('#2b6cb0', None, 'cloth', 0.04), shield_face("sun", '#d9a62b', '#d9a62b'), M["gold"])


def clans(m, M, cul, kind):
    tunic = Mat(cul["cloth"], None, 'cloth', 0.06)
    plaid = M["plaid"]
    wraps = Mat('#8a7a5a', '#5a4a32', 'stripes_h', 0.05)
    woad = '#2a4a9a'
    if kind == "militia":
        build_body(m, M, tunic, tunic, M["skin"], M["skin"], M["skin"], wraps, wraps, M["boots"])
        head(m, M, beard="long", paint=woad, hair="wild")
        skirt(m, plaid, length=6)
        belt(m, M, buckle=False)
        m.part("pelt", "body", decor=True, cubes=[Cube((-4.5, -0.75, -2.75), (9, 3, 5.5), M["fur"])])
        return
    if kind == "archer":
        build_body(m, M, M["leather"], tunic, tunic, M["leather"], M["skin"], wraps, wraps, M["boots"])
        head(m, M, beard="mustache", paint=woad)
        green = Mat('#2a4a22', None, 'cloth', 0.07, tatter=2)
        m.part("hood", "head", cubes=[Cube((-3.9, -7.6, -3.9), (7.8, 8, 7.8), green, decals={"north": open_face(1)})])
        m.part("hood_tip", "hood", decor=True, pivot=(0, -6, 3.9), rot=(40, 0, 0), cubes=[Cube((-1.5, 0, 0), (3, 4, 1.5), green)])
        cape(m, green, length=18, width=9.5)
        skirt(m, plaid, length=5)
        belt(m, M, buckle=False)
        quiver(m, M)
        return
    if kind == "swordsman":
        build_body(m, M, M["mail"], M["leather"], M["skin"], M["leather"], M["skin"], wraps, wraps, M["boots"])
        head(m, M, beard="long", paint=woad, hair="wild")
        m.part("torc", "neck", cubes=[Cube((-2, -0.25, -2), (4, 0.75, 4), M["gold"])])
        m.part("sash", "body", cubes=[Cube((-4.3, -0.2, -2.5), (3, 10, 5), plaid)])
        skirt(m, plaid, length=6)
        belt(m, M)
        arm_shield(m, "round", M["wood"], boss_face('#5a3a1a'), M["plate"])
        return
    # вождь: волчья шкура на голове
    build_body(m, M, M["mail"], M["leather"], M["mail"], M["leather"], M["leather"], wraps, wraps, M["boots"], heavy=True)
    head(m, M, beard="long", paint=woad)
    wolf = Mat('#6a6058', '#3a342e', 'fur', 0.1, edge=0.9)
    m.part("wolf_head", "head", cubes=[Cube((-4, -8.2, -4.2), (8, 3.5, 8.4), wolf),
                                       Cube((-2, -7.6, -7.4), (4, 2.6, 3.4), wolf),
                                       Cube((-3.6, -10, -2.4), (1.6, 2, 1.4), wolf), Cube((2, -10, -2.4), (1.6, 2, 1.4), wolf)])
    m.part("wolf_pelt", "body", decor=True, cubes=[Cube((-5.2, -1, -3), (10.4, 3, 6), wolf), Cube((-5, 1.5, 2.4), (10, 18, 1), wolf.but(tatter=3))])
    m.part("torc", "neck", cubes=[Cube((-2, -0.25, -2), (4, 0.75, 4), M["gold"])])
    pauldrons(m, wolf, size=(5, 3.5, 6))
    skirt(m, plaid, length=7, wide=10)
    belt(m, M, M["gold"])
    arm_shield(m, "round", M["wood"].but(base='#5a3e22'), shield_face("tree", '#d9a62b', '#d9a62b'), M["gold"])


BUILDERS = dict(north=north, empire=empire, west=west, steppe=steppe, sultanate=sultanate, clans=clans)


def role_equipment(m, M, cul, cname, kind):
    """Role silhouettes retain each culture's headgear, palette and base armour.

    Role-defining gear is not decor: lite mode must still distinguish troops.
    Hand-held swords, spears and crossbows are supplied by the existing item layer.
    Firearms have their own geometry because the server currently uses placeholder items.
    """
    symbols = dict(north="cross", empire="eagle", west="lion", steppe="sun", sultanate="crescent", clans="tree")
    if kind in ("greatsword", "horse_archer", "crossbow", "musketeer", "bombardier", "scout"):
        m.parts = [part for part in m.parts if part.name != "shield"]
    if kind in ("crossbow", "musketeer", "bombardier"):
        m.parts = [part for part in m.parts if part.name != "quiver"]

    if kind == "shieldman":
        m.parts = [part for part in m.parts if part.name != "shield"]
        arm_shield(m, "scutum", M["main_trim"], shield_face(symbols[cname], cul["accent"], cul["accent"]), M["plate"])
        m.part("shield_brow", "head", cubes=[Cube((-4, -4.2, -4), (8, 1, 0.8), M["plate"])])
        m.part("guard_cuirass", "body", cubes=[Cube((-4.2, 1, -2.8), (8.4, 6.5, 0.65), M["lamellar"])])
    elif kind == "spearman":
        m.part("spear_mantle", "body", cubes=[Cube((-4.5, 0, -2.7), (9, 2, 5.4), M["main_cloth"])])
        m.part("spear_baldric", "body", rot=(0, 0, -25), cubes=[Cube((-0.7, -0.2, -2.9), (1.4, 11, 0.5), M["leather"])])
        m.part("spear_pennon", "body", decor=True, pivot=(3.8, 4, 2.6), cubes=[Cube((0, 0, 0), (2, 9, 0.6), M["main_cloth"])])
    elif kind == "greatsword":
        m.part("heavy_visor", "head", cubes=[Cube((-3.6, -3.7, -3.9), (7.2, 2.3, 0.7), M["plate"])])
        m.part("heavy_gorget", "body", cubes=[Cube((-3.2, -0.7, -3), (6.4, 2, 6), M["plate"])])
        for side in ("left", "right"):
            m.part("heavy_gauntlet_" + side, side + "_arm", cubes=[Cube((-1.2 if side == "left" else -2.7, 6.5, -2), (3.9, 4, 4), M["plate"])])
        m.part("heavy_apron", "body", cubes=[Cube((-3, 10, -2.8), (6, 6, 0.7), M["scale"])])
    elif kind in ("light_cav", "heavy_cav", "horse_archer", "scout"):
        # Long infantry skirts and cloaks otherwise intersect the saddle.
        for part in m.parts:
            if part.name in ("skirt", "cape", "wolf_pelt"):
                for cube in part.cubes:
                    if cube.size[1] > 7:
                        cube.size = (cube.size[0], 7, cube.size[2])
        armour = M["plate"] if kind == "heavy_cav" else M["leather"]
        for side in ("left", "right"):
            m.part("riding_boot_" + side, side + "_leg", cubes=[Cube((-2.05, 6, -2.2), (4.1, 4.3, 4.4), armour)])
            m.part("riding_spur_" + side, side + "_leg", decor=True, cubes=[Cube((-0.5, 10, 1.8), (1, 1, 1.7), M["plate"])])
        m.part("rider_sash", "body", cubes=[Cube((-4.4, 8.7, -2.7), (8.8, 1.2, 5.4), M["main_cloth"])])
        if kind == "heavy_cav":
            m.part("cavalry_breastplate", "body", cubes=[Cube((-4.6, 0.7, -3.1), (9.2, 7, 1), M["plate"])])
            plume(m, "head", M["main_cloth"], (0, -8, 0), size=(1.5, 5, 1.5), name="cavalry_crest")
        elif kind == "horse_archer":
            if not any(part.name == "bow_case" for part in m.parts):
                m.part("bow_case", "body", pivot=(4, 7, 0), rot=(0, 0, -12), cubes=[Cube((0, 0, -1.5), (2, 8, 3), M["leather"])])
        elif kind == "scout":
            m.part("scout_mantle", "body", cubes=[Cube((-4.5, -0.5, -2.7), (9, 2, 5.4), M["leather"])])
            m.part("map_roll", "body", pivot=(4.2, 8, 0), rot=(0, 0, -20), cubes=[Cube((0, 0, -1), (2, 6, 2), M["linen"]),
                        Cube((-0.1, 2.5, -1.1), (2.2, 1, 2.2), M["leather"])])
            m.part("scout_bedroll", "body", cubes=[Cube((-4, 8, 2.8), (8, 3, 3), M["main_cloth"])])
        else:
            m.part("rider_pouch", "body", cubes=[Cube((3.5, 8, -2), (2.5, 4, 4), M["leather"])])
    elif kind == "crossbow":
        m.part("crossbow_vest", "body", cubes=[Cube((-4.1, 0.3, -2.7), (8.2, 8.5, 0.7), M["quilt"])])
        m.part("bolt_case", "body", pivot=(-4.5, 7.5, 0), rot=(0, 0, 12), cubes=[Cube((-1.2, 0, -1.6), (2.4, 7, 3.2), M["leather"]),
                    Cube((-0.7, -1.5, -1), (0.7, 2, 0.7), M["white"]), Cube((0.3, -1.5, 0), (0.7, 2, 0.7), M["white"])])
        m.part("crossbow_brace", "right_arm", cubes=[Cube((-2.7, 3.5, -1.9), (3.8, 3, 3.8), M["leather"])])
    elif kind == "musketeer":
        m.part("powder_baldric", "body", rot=(0, 0, -25), cubes=[Cube((-0.75, -0.3, -2.9), (1.5, 12, 0.5), M["leather"])])
        m.part("powder_charges", "body", cubes=[Cube((-3 + i * 1.4, 2 + i * 1.5, -3.4), (1, 2.3, 1.1), M["wood"]) for i in range(5)])
        m.part("powder_horn", "body", pivot=(4, 8, 0), rot=(0, 0, -18), cubes=[Cube((0, 0, -1), (2, 4, 2), M["bronze"]), Cube((0.5, 4, -0.5), (1, 1.5, 1), M["black"])])
        m.part("musket", "right_arm", cubes=[Cube((-2, -18, -4), (2, 25, 2), M["plate"]),
                    Cube((-2.5, 2, -4.5), (3, 12, 3), M["wood"]), Cube((-2.8, 12, -4.7), (3.6, 2, 3.4), M["wood"]),
                    Cube((-2.25, -16, -4.25), (2.5, 1, 2.5), M["black"]), Cube((-2.25, -5, -4.25), (2.5, 1, 2.5), M["black"])])
    elif kind == "bombardier":
        m.part("blast_cuirass", "body", cubes=[Cube((-4.6, 0, -3.2), (9.2, 8.5, 1.2), M["plate"])])
        m.part("blast_apron", "body", cubes=[Cube((-4.4, 10, -2.9), (8.8, 6, 0.9), M["leather"])])
        m.part("shell_satchel", "body", cubes=[Cube((-4, 3, 2.8), (8, 7, 3), M["leather"]),
                    Cube((-3, 1, 3.2), (2, 3, 2), M["black"]), Cube((1, 1, 3.2), (2, 3, 2), M["black"])])
        m.part("hand_bombard", "right_arm", cubes=[Cube((-3.5, -8, -5), (5, 17, 5), M["bronze"]),
                    Cube((-4, -7, -5.5), (6, 1.5, 6), M["black"]), Cube((-4, 4, -5.5), (6, 1.5, 6), M["black"]),
                    Cube((-2.5, 9, -4), (3, 5, 3), M["wood"]), Cube((-3, -8.1, -4.5), (4, 0.2, 4), M["black"])])


def unit_model(cname, kind):
    cul = CULTURES[cname]
    M = mats(cul)
    base_kind = {"shieldman": "swordsman", "spearman": "swordsman", "greatsword": "knight",
                 "light_cav": "swordsman", "heavy_cav": "knight", "horse_archer": "archer",
                 "crossbow": "archer", "musketeer": "archer", "bombardier": "swordsman", "scout": "archer"}.get(kind, kind)
    m = Model(f"unit_{cname}_{kind}", model_class(cname, kind), 128 if base_kind == kind else 256, 128,
              seed=sum(map(ord, cname + kind)) * 31)
    BUILDERS[cname](m, M, cul, base_kind)
    if base_kind != kind:
        role_equipment(m, M, cul, cname, kind)
    return m


def skin_variants(name):
    out = {}
    for i, (skin, hair) in enumerate(SKINS):
        def fn(mat, skin=skin, hair=hair):
            if mat.base[:3] == hexc(SKIN_KEY)[:3]:
                return mat.but(base=skin)
            if mat.base[:3] == hexc(HAIR_KEY)[:3]:
                return mat.but(base=hair, second=hair)
            return mat
        out[f"{name}_{i}"] = fn
    return out


# ------------------------------------------------------------------ разбойники

def bandit_model(kind):
    m = Model(f"bandit_{kind}", f"Bandit{kind.capitalize()}Model", 128, 128, seed=900 + len(kind))
    M = mats(CULTURES["north"])
    rag = Mat('#5a4a32', '#3e3222', 'moss', 0.1, tatter=2)
    patch = Mat('#6a5232', '#4a3a22', 'quilt', 0.08)
    leather = Mat('#4a3420', '#7a6040', 'leather', 0.08)
    hood = Mat('#2e3a24' if kind == "archer" else '#3a2e22', None, 'cloth', 0.08, tatter=2)
    red = Mat('#8a1a1a', None, 'cloth', 0.06)
    wraps = Mat('#6a5a42', '#4a3a28', 'stripes_h', 0.06)
    if kind == "thug":
        build_body(m, M, patch, rag, M["skin"], M["skin"], leather, wraps, wraps, M["boots"], heavy=True)
        head(m, M, beard="full", mask='#8a1a1a', scar=True)
        m.part("hood", "head", cubes=[Cube((-3.9, -7.6, -3.9), (7.8, 5, 7.8), hood)])
        m.part("spikes", "body", decor=True, cubes=[Cube((-5.2, -0.8, -2.9), (3, 2.5, 5.8), leather), Cube((-5, -1.8, -0.5), (1, 1, 1), M["plate"])])
        skirt(m, rag, length=5)
        belt(m, M, leather, buckle=False)
    elif kind == "archer":
        build_body(m, M, leather, rag, hood, leather, leather, wraps, wraps, M["boots"])
        head(m, M, beard="mustache", mask='#2e3a24')
        m.part("hood", "head", cubes=[Cube((-3.9, -7.6, -3.9), (7.8, 8, 7.8), hood, decals={"north": open_face(1)})])
        m.part("hood_tip", "hood", decor=True, pivot=(0, -6, 3.9), rot=(40, 0, 0), cubes=[Cube((-1.5, 0, 0), (3, 4, 1.5), hood)])
        cape(m, hood, length=14, width=9)
        skirt(m, rag, length=5)
        belt(m, M, leather, buckle=False)
        quiver(m, M)
    else:  # атаман
        brig = Mat('#5a1a1a', '#c0c0c8', 'brigandine', 0.05)
        build_body(m, M, brig, leather, brig, leather, leather, leather, M["boots"], M["boots"], heavy=True)
        head(m, M, beard="full", scar=True)
        m.part("eyepatch", "head", cubes=[Cube((-3.6, -4.4, -3.6), (7.2, 0.6, 7.2), M["black"])])
        hat = Mat('#2a221c', None, 'cloth', 0.05)
        m.part("cap", "head", cubes=[Cube((-3.9, -7.8, -3.9), (7.8, 3, 7.8), hat), Cube((-5.6, -5.3, -5.6), (11.2, 0.6, 11.2), hat)])
        plume(m, "cap", red, (2.5, -7.5, 1), size=(1, 6, 1), rot=(-40, 0, 25))
        m.part("mantle", "body", decor=True, cubes=[Cube((-5.3, -1, -3), (10.6, 4, 6), M["fur"]), Cube((-5, 2, 2.6), (10, 16, 1), M["fur"].but(tatter=3))])
        skirt(m, leather, length=7, split=True)
        belt(m, M, red)
    return m


def ALL():
    out = []
    for c in CULTURES:
        for k in TYPES:
            m = unit_model(c, k)
            out.append((m, skin_variants(m.name)))
    for k in ("thug", "archer", "captain"):
        m = bandit_model(k)
        out.append((m, skin_variants(m.name)))
    return out
