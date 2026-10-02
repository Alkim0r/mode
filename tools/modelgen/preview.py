"""Офлайн-превью моделей: z-буфер, ортографическая проекция, та же развёртка UV, что и в Minecraft.

python3 tools/modelgen/preview.py out.png [фильтр-имени]
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))


def rot_matrix(rx, ry, rz):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def world_transforms(model, pose=None):
    """pose: {имя_части: (rx, ry, rz)} — дополнительная анимация."""
    pose = pose or {}
    by = {p.name: p for p in model.parts}
    cache = {}

    def get(name):
        if name in cache:
            return cache[name]
        p = by[name]
        r = list(p.rot)
        if name in pose:
            r = [a + b for a, b in zip(r, pose[name])]
        M = np.eye(4)
        M[:3, :3] = rot_matrix(*r)
        M[:3, 3] = p.pivot
        if p.parent:
            M = get(p.parent) @ M
        cache[name] = M
        return M

    for p in model.parts:
        get(p.name)
    return cache


def faces_of(cube):
    """Грань: (угол O, вектор U вдоль u текстуры, вектор V вдоль v, (u0, v0, du, dv) в текселях, нормаль)."""
    x0, y0, z0 = cube.origin
    w, h, d = cube.size
    g = cube.inflate
    X0, Y0, Z0 = x0 - g, y0 - g, z0 - g
    X1, Y1, Z1 = x0 + w + g, y0 + h + g, z0 + d + g
    u, v = cube.uv
    W, H, D = [int(math.ceil(s)) for s in (w, h, d)]
    A = np.array
    out = [
        # north (-z): u по +x, v по +y
        (A([X0, Y0, Z0]), A([X1 - X0, 0, 0]), A([0, Y1 - Y0, 0]), (u + D, v + D, W, H), A([0, 0, -1])),
        # east (+x): u по +z
        (A([X1, Y0, Z0]), A([0, 0, Z1 - Z0]), A([0, Y1 - Y0, 0]), (u + D + W, v + D, D, H), A([1, 0, 0])),
        # south (+z): u по -x
        (A([X1, Y0, Z1]), A([X0 - X1, 0, 0]), A([0, Y1 - Y0, 0]), (u + 2 * D + W, v + D, W, H), A([0, 0, 1])),
        # west (-x): u по -z
        (A([X0, Y0, Z1]), A([0, 0, Z0 - Z1]), A([0, Y1 - Y0, 0]), (u, v + D, D, H), A([-1, 0, 0])),
        # up (min y): u по +x, v по -z (низ текстуры примыкает к лицу)
        (A([X0, Y0, Z1]), A([X1 - X0, 0, 0]), A([0, 0, Z0 - Z1]), (u + D, v, W, D), A([0, -1, 0])),
        # down (max y): v растёт от задней грани к передней
        (A([X0, Y1, Z1]), A([X1 - X0, 0, 0]), A([0, 0, Z0 - Z1]), (u + D + W, v, W, D), A([0, 1, 0])),
    ]
    if cube.mirror:  # как в Minecraft: minX и maxX меняются местами
        F = np.array([-1.0, 1, 1])
        cx2 = X0 + X1
        out = [(np.array([cx2 - O[0], O[1], O[2]]), U * F, V * F, uv, n * F) for O, U, V, uv, n in out]
    return out


def render(model, tex, glow=None, yaw=30, pitch=10, scale=10, size=(300, 420), pose=None, bg=(46, 50, 58)):
    tex = np.asarray(tex.convert("RGBA")).astype(np.float32)
    glow_a = np.asarray(glow.convert("RGBA")).astype(np.float32) if glow is not None else None
    Wd, Ht = size
    img = np.zeros((Ht, Wd, 3), np.float32)
    img[:] = bg
    zbuf = np.full((Ht, Wd), np.inf, np.float32)
    view = rot_matrix(pitch, 0, 0) @ rot_matrix(0, yaw, 0)
    light = np.array([-0.4, -1.0, -0.7])
    light /= np.linalg.norm(light)
    T = world_transforms(model, pose)
    # центр: модель стоит на y=24
    cx, cy = Wd / 2, Ht * 0.5 - 5 * scale
    for p in model.parts:
        M = T[p.name]
        for c in p.cubes:
            for O, U, V, (tu, tv, tw, th), n in faces_of(c):
                if tw <= 0 or th <= 0:
                    continue
                Ow = M[:3, :3] @ O + M[:3, 3]
                Uw, Vw = M[:3, :3] @ U, M[:3, :3] @ V
                nw = M[:3, :3] @ n
                Os, Us, Vs = view @ Ow, view @ Uw, view @ Vw
                ns = view @ nw
                # экран: x вправо, y вниз, z вглубь (камера смотрит на +z)
                P = np.array([[Us[0], Vs[0]], [Us[1], Vs[1]]]) * scale
                det = np.linalg.det(P)
                if abs(det) < 1e-6:
                    continue
                Pinv = np.linalg.inv(P)
                o2 = np.array([cx + Os[0] * scale, cy + Os[1] * scale])
                corners = [o2, o2 + P[:, 0], o2 + P[:, 1], o2 + P[:, 0] + P[:, 1]]
                xs = [q[0] for q in corners]
                ys = [q[1] for q in corners]
                x0, x1 = max(0, int(min(xs))), min(Wd - 1, int(max(xs)) + 1)
                y0, y1 = max(0, int(min(ys))), min(Ht - 1, int(max(ys)) + 1)
                if x0 > x1 or y0 > y1:
                    continue
                gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
                rel = np.stack([gx - o2[0], gy - o2[1]], -1)
                ab = rel @ Pinv.T
                a, b = ab[..., 0], ab[..., 1]
                inside = (a >= 0) & (a < 1) & (b >= 0) & (b < 1)
                if not inside.any():
                    continue
                depth = Os[2] + a * Us[2] + b * Vs[2]
                ti = np.clip((tu + a * tw).astype(int), 0, tex.shape[1] - 1)
                tj = np.clip((tv + b * th).astype(int), 0, tex.shape[0] - 1)
                col = tex[tj, ti]
                ok = inside & (col[..., 3] > 10)
                sub = zbuf[y0:y1 + 1, x0:x1 + 1]
                ok &= depth < sub
                if not ok.any():
                    continue
                facing = ns if ns[2] < 0 else -ns
                lum = 0.72 + 0.4 * max(0.0, float(-facing @ light))
                rgb = col[..., :3] * lum
                if glow_a is not None:
                    gcol = glow_a[tj, ti]
                    gmask = gcol[..., 3] > 10
                    rgb = np.where(gmask[..., None], np.minimum(255, gcol[..., :3] * 1.1), rgb)
                region = img[y0:y1 + 1, x0:x1 + 1]
                region[ok] = rgb[ok]
                sub[ok] = depth[ok]
    return Image.fromarray(img.clip(0, 255).astype(np.uint8))


def sheet(entries, out, cols=6, size=(240, 360), scale=8):
    """entries: [(подпись, model, tex, glow, yaw)]"""
    rows = (len(entries) + cols - 1) // cols
    W, H = size
    S = Image.new("RGB", (cols * W, rows * (H + 18)), (30, 32, 38))
    d = ImageDraw.Draw(S)
    for i, (label, m, t, g, yaw) in enumerate(entries):
        im = render(m, t, g, yaw=yaw, scale=scale, size=size)
        x, y = (i % cols) * W, (i // cols) * (H + 18)
        S.paste(im, (x, y))
        d.text((x + 4, y + H + 2), label, fill=(230, 230, 230))
    S.save(out)


if __name__ == "__main__":
    import bosses
    import units
    out = sys.argv[1] if len(sys.argv) > 1 else "preview.png"
    filt = sys.argv[2] if len(sys.argv) > 2 else ""
    entries = []
    models = [(fn(), None) for fn in bosses.ALL] + units.ALL()
    for m, variants in models:
        if filt and filt not in m.name:
            continue
        m.pack()
        if variants:
            name, fn = next(iter(variants.items()))
            t, g = m.paint(fn)
        else:
            t, g = m.paint()
        g = g if g.getbbox() else None
        big = m.base != "humanoid"
        for yaw in (-30, 150):
            entries.append((m.name + (" (спина)" if yaw > 90 else ""), m, t, g, yaw))
    sheet(entries, out, cols=int(sys.argv[3]) if len(sys.argv) > 3 else 6)
    print(out, len(entries))
