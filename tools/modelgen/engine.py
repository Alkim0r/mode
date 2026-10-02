"""Движок генерации моделей Regnum.

Одна спецификация -> Java-класс с геометрией (LayerDefinition) + текстура + текстура свечения.
Развёртка UV — стандартная «коробочная» Minecraft:
  верх [u+d, v]..[u+d+w, v+d], низ [u+d+w, v]..[u+d+2w, v+d],
  -x [u, v+d], перед(-z) [u+d, v+d], +x [u+d+w, v+d], зад [u+2d+w, v+d] (высота h).
"""
import math
import random
from PIL import Image

T = (0, 0, 0, 0)


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


# ---------------------------------------------------------------- материалы

class Mat:
    """Материал: базовый цвет, второй цвет, узор, шум, светится ли, рваный ли край."""

    def __init__(self, base, second=None, pattern="noise", noise=0.08, glow=False, tatter=0, alpha=255, edge=None, grad=0.14):
        self.base = hexc(base, alpha)
        self.second = hexc(second, alpha) if second else shade(self.base, 0.75)
        self.pattern = pattern
        self.noise = noise
        self.glow = glow
        self.tatter = tatter  # сколько нижних рядов «рвать» на боковых гранях
        # обводка граней (объём, «нарисованный» вид) и вертикальный градиент сверху-вниз
        self.edge = edge if edge is not None else (1.0 if glow or pattern in ("glow",) else 0.84)
        self.grad = 0.0 if glow else grad

    def but(self, **kw):
        """Копия материала с изменёнными полями."""
        import copy
        m = copy.copy(self)
        for k, v in kw.items():
            if k in ("base", "second"):
                v = hexc(v)
            setattr(m, k, v)
        return m


def paint_face(img, glow_img, x0, y0, w, h, mat, rng, face):
    for j in range(h):
        for i in range(w):
            c = pixel(mat, i, j, w, h, rng, face)
            if c is None:
                continue
            if face not in ("up", "down") and mat.grad and h > 1:
                c = shade(c, 1 + mat.grad * (0.5 - j / (h - 1)))
            if mat.edge < 1.0 and w > 2 and h > 2 and (i in (0, w - 1) or j in (0, h - 1)):
                c = shade(c, mat.edge)
            img.putpixel((x0 + i, y0 + j), c)
            if mat.glow and glow_img is not None:
                glow_img.putpixel((x0 + i, y0 + j), c)
    # рваный низ
    if mat.tatter and face in ("north", "south", "east", "west"):
        for i in range(w):
            cut = rng.randint(0, mat.tatter)
            for k in range(cut):
                yy = y0 + h - 1 - k
                if yy >= y0:
                    img.putpixel((x0 + i, yy), T)


def pixel(m, i, j, w, h, rng, face):
    n = 1 + rng.uniform(-m.noise, m.noise)
    b, s = m.base, m.second
    p = m.pattern
    if p == "noise":
        return shade(b, n)
    if p == "checker":  # кольчуга
        return shade(b if (i + j) % 2 == 0 else s, n)
    if p == "rings":  # кольчуга покрупнее
        return shade(s if (i % 2 == 0 and j % 2 == 0) else b, n)
    if p == "plate":  # латы: блик сверху, тень снизу, заклёпки по углам
        if j == 0:
            return shade(b, 1.3)
        if j == h - 1:
            return shade(b, 0.72)
        if (i in (1, w - 2)) and (j in (1, h - 2)) and w > 3 and h > 3:
            return shade(s, 1.15)
        hl = 1.22 if (w > 3 and i == max(1, w // 3)) else 1.0
        return shade(b, (1 + rng.uniform(-m.noise, m.noise) * 0.5) * hl)
    if p == "quilt":  # стёганка
        return shade(s if (i % 3 == 0 or j % 3 == 0) else b, n)
    if p == "stripes_h":
        return shade(b if (j // 1) % 2 == 0 else s, n)
    if p == "stripes_v":
        return shade(b if (i // 1) % 2 == 0 else s, n)
    if p == "wrap":  # бинты мумии
        return shade(s if (i + 2 * j) % 5 == 0 else b, n)
    if p == "fur":
        return shade(b if rng.random() > 0.3 else s, 1 + rng.uniform(-0.2, 0.2))
    if p == "wood":
        return shade(b if (i % 4) else s, n)
    if p == "bone":
        c = shade(b, n)
        return shade(s, 1.0) if rng.random() < 0.06 else c
    if p == "chitin":
        # A broad subdued highlight and dark plate seam, without metallic stripes/rivets.
        across = i / max(1, w - 1)
        highlight = max(0, 1 - abs(across - 0.35) * 2.2)
        c = mix(b, s, highlight * 0.32)
        seam = 0.70 if j == h - 1 and h > 2 else 1.0
        return shade(c, n * seam)
    if p == "gold":
        t = (i + j) % 4
        return shade(b, 1.25 if t == 0 else 1.0 if t < 3 else 0.85)
    if p == "glow":
        return shade(b, 1 + rng.uniform(-0.15, 0.15))
    if p == "cracks":  # тёмный металл с раскалёнными трещинами
        if rng.random() < 0.07:
            return hexc('#ff8a2a')
        return shade(b, n)
    if p == "wing":  # переливающиеся крылья скарабея
        t = j / max(1, h - 1)
        c = mix(b, s, t)
        if i % 4 == 0 or j % 5 == 0:
            return hexc('#d9a62b')
        return shade(c, 1 + rng.uniform(-0.06, 0.06))
    if p == "moss":
        return shade(b if rng.random() > 0.35 else s, 1 + rng.uniform(-0.15, 0.1))
    if p == "mail":  # кольчуга: ряды колечек со сдвигом, мягкий контраст
        k = (i + (j % 2)) % 2
        if k == 0:
            return shade(b, 1.08 * n)
        return shade(mix(b, s, 0.45), n)
    if p == "tartan":  # клетка: полосы акцента по обеим осям
        si, sj = i % 5 == 1, j % 5 == 1
        if si and sj:
            return shade(s, 0.85 * n)
        if si or sj:
            return shade(mix(b, s, 0.55), n)
        if i % 5 == 3 or j % 5 == 3:
            return shade(b, 0.78 * n)
        return shade(b, n)
    if p == "scale":  # чешуя: полукруглые пластинки со светлым верхом
        cy = j % 3
        cx = (i + (j // 3) % 2) % 3
        if cy == 0:
            return shade(b, 1.2 * n)
        if cx == 2 or cy == 2:
            return shade(s, n)
        return shade(b, n)
    if p == "lamellar":  # ламеллы: узкие вертикальные пластины, шнуровка рядами
        if j % 4 == 3:
            return shade(s, n)
        return shade(b, (1.18 if i % 2 == 0 else 0.95) * n)
    if p == "cloth":  # ткань со складками
        fold = 0.9 if (i % 4 == 0) else 1.0
        return shade(b, fold * n)
    if p == "leather":  # кожа: потёртости и строчка
        if j == 1 or j == h - 2:
            return shade(s, 1.1) if i % 2 == 0 else shade(b, n)
        return shade(b, n * (0.92 if rng.random() < 0.15 else 1.0))
    if p == "segmented":  # лорика сегментата / пластинчатый пояс
        if j % 3 == 0:
            return shade(b, 1.3)
        if j % 3 == 2:
            return shade(b, 0.72)
        return shade(b, n)
    if p == "skin":
        return shade(b, 1 + rng.uniform(-m.noise, m.noise) * 0.6)
    if p == "hair":
        return shade(b if (i + rng.randint(0, 1)) % 2 else s, 1 + rng.uniform(-0.08, 0.08))
    if p == "brigandine":  # ткань с рядами заклёпок
        if i % 3 == 1 and j % 3 == 1:
            return shade(s, 1.4)
        return shade(b, n)
    if p == "trim2":  # ткань с цветной каймой сверху и снизу
        if j == 0 or j == h - 1:
            return shade(s, 1.0)
        return shade(b, n)
    if p == "trim":  # ткань с золотой каймой снизу
        if j >= h - 2:
            return hexc('#d9a62b')
        return shade(b, n)
    return shade(b, n)


# ---------------------------------------------------------------- геометрия

class Cube:
    def __init__(self, origin, size, mat, inflate=0.0, decals=None, mirror=False):
        self.origin = origin
        self.size = size
        self.mat = mat
        self.inflate = inflate
        self.decals = decals or {}
        self.mirror = mirror
        self.uv = None


class Part:
    def __init__(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=None, decor=False):
        self.name = name
        self.parent = parent
        self.pivot = pivot
        self.rot = rot
        self.cubes = cubes or []
        self.decor = decor


class Model:
    def __init__(self, name, java_class, tex_w=128, tex_h=128, base="humanoid", seed=1):
        self.name = name
        self.java_class = java_class
        self.tw, self.th = tex_w, tex_h
        self.base = base
        self.parts = []
        self.seed = seed

    def part(self, *a, **k):
        p = Part(*a, **k)
        self.parts.append(p)
        return p

    # ---------- упаковка UV
    def pack(self):
        cubes = [c for p in self.parts for c in p.cubes]
        order = sorted(cubes, key=lambda c: -(c.size[2] + c.size[1]))
        x = y = 0
        row_h = 0
        for c in order:
            w, h, d = [int(math.ceil(v)) for v in c.size]
            cw, ch = 2 * (d + w), d + h
            if x + cw > self.tw:
                x = 0
                y += row_h
                row_h = 0
            c.uv = (x, y)
            x += cw
            row_h = max(row_h, ch)
            if y + ch > self.th:
                raise ValueError(f"{self.name}: текстура {self.tw}x{self.th} мала")

    # ---------- текстура
    def paint(self, variant_fn=None):
        rng = random.Random(self.seed)
        img = Image.new("RGBA", (self.tw, self.th), T)
        glow = Image.new("RGBA", (self.tw, self.th), T)
        for p in self.parts:
            for c in p.cubes:
                mat = variant_fn(c.mat) if variant_fn else c.mat
                u, v = c.uv
                w, h, d = [int(math.ceil(s)) for s in c.size]
                faces = {
                    "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
                    "west": (u, v + d, d, h), "north": (u + d, v + d, w, h),
                    "east": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
                }
                for fname, (fx, fy, fw, fh) in faces.items():
                    if fw <= 0 or fh <= 0:
                        continue
                    paint_face(img, glow, fx, fy, fw, fh, mat, rng, fname)
                    if fname in c.decals:
                        c.decals[fname](Canvas(img, glow, fx, fy, fw, fh), rng)
        return img, glow

    # ---------- Java
    def java(self, package):
        lines = []
        cls = self.java_class
        lines.append(f"package {package};")
        lines.append("")
        lines.append("import com.alkimor.regnum.Regnum;")
        lines.append("import net.minecraft.client.model.geom.ModelLayerLocation;")
        lines.append("import net.minecraft.client.model.geom.ModelPart;")
        lines.append("import net.minecraft.client.model.geom.PartPose;")
        lines.append("import net.minecraft.client.model.geom.builders.*;")
        lines.append("")
        lines.append("import java.util.ArrayList;")
        lines.append("import java.util.List;")
        lines.append("")
        lines.append(f"/** Сгенерировано tools/modelgen — не редактировать вручную. Модель: {self.name}. */")
        lines.append(f"public final class {cls} {{")
        lines.append(f"    private {cls}() {{}}")
        lines.append("")
        lines.append(f'    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Regnum.id("{self.name}"), "main");')
        lines.append("")
        lines.append("    public static LayerDefinition create() {")
        lines.append("        MeshDefinition mesh = new MeshDefinition();")
        lines.append("        PartDefinition root = mesh.getRoot();")
        names = {}
        seen = set()
        for p in self.parts:
            assert p.name not in seen, f"{self.name}: повтор части {p.name}"
            seen.add(p.name)
        for p in self.parts:
            var = "p_" + p.name
            parent = "root" if p.parent is None else "p_" + p.parent
            cb = "CubeListBuilder.create()"
            for c in p.cubes:
                # Minecraft берёт UV по точным (дробным) размерам — поэтому размер округляем вверх до целого,
                # а разницу убираем через CubeDeformation по осям: геометрия та же, развёртка совпадает с текстурой.
                org, dims, grow = [], [], []
                for o, sz in zip(c.origin, c.size):
                    S = int(math.ceil(sz - 1e-6))
                    g = (sz - S) / 2.0
                    org.append(round(o + g, 4))
                    dims.append(S)
                    grow.append(round(g + c.inflate, 4))
                ox, oy, oz = org
                w, h, d = dims
                cb += f".texOffs({c.uv[0]}, {c.uv[1]})"
                if c.mirror:
                    cb += ".mirror()"
                cb += f".addBox({ox}F, {oy}F, {oz}F, {w}F, {h}F, {d}F, new CubeDeformation({grow[0]}F, {grow[1]}F, {grow[2]}F))"
                if c.mirror:
                    cb += ".mirror(false)"
            px, py, pz = p.pivot
            rx, ry, rz = [math.radians(a) for a in p.rot]
            pose = f"PartPose.offsetAndRotation({px}F, {py}F, {pz}F, {rx:.4f}F, {ry:.4f}F, {rz:.4f}F)"
            lines.append(f'        PartDefinition {var} = {parent}.addOrReplaceChild("{p.name}", {cb}, {pose});')
            names[p.name] = p
        if self.base == "humanoid" and not any(p.name == "hat" and p.parent is None for p in self.parts):
            lines.append('        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);')
        lines.append(f"        return LayerDefinition.create(mesh, {self.tw}, {self.th});")
        lines.append("    }")
        lines.append("")
        lines.append("    /** Декоративные детали (скрываются в облегчённом режиме). */")
        lines.append("    public static List<ModelPart> decor(ModelPart root) {")
        lines.append("        List<ModelPart> list = new ArrayList<>();")
        for p in self.parts:
            if p.decor:
                chain = []
                q = p
                while q is not None:
                    chain.append(q.name)
                    q = names.get(q.parent) if q.parent else None
                chain.reverse()
                expr = "root" + "".join(f'.getChild("{n}")' for n in chain)
                lines.append(f"        list.add({expr});")
        lines.append("        return list;")
        lines.append("    }")
        lines.append("}")
        return "\n".join(lines) + "\n"


class Canvas:
    """Рисование поверх грани: координаты от левого верхнего угла грани."""

    def __init__(self, img, glow, x0, y0, w, h):
        self.img, self.glow, self.x0, self.y0, self.w, self.h = img, glow, x0, y0, w, h

    def px(self, x, y, c, glow=False):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((self.x0 + x, self.y0 + y), c)
            if glow:
                self.glow.putpixel((self.x0 + x, self.y0 + y), c)

    def rect(self, x0, y0, x1, y1, c, glow=False):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                self.px(x, y, c, glow)

    def clear(self, x0, y0, x1, y1):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                if 0 <= x < self.w and 0 <= y < self.h:
                    self.img.putpixel((self.x0 + x, self.y0 + y), T)
