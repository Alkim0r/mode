"""Минимальный читатель/писатель NBT (без внешних зависимостей)."""
import struct, gzip, io


def read(b):
    if b[:2] == b'\x1f\x8b':
        b = gzip.decompress(b)
    s = io.BytesIO(b)

    def u(f, n):
        return struct.unpack(f, s.read(n))[0]

    def rs():
        return s.read(u('>H', 2)).decode('utf8', 'replace')

    def pay(t):
        if t == 1: return u('>b', 1)
        if t == 2: return u('>h', 2)
        if t == 3: return u('>i', 4)
        if t == 4: return u('>q', 8)
        if t == 5: return u('>f', 4)
        if t == 6: return u('>d', 8)
        if t == 7:
            n = u('>i', 4)
            return s.read(n)
        if t == 8: return rs()
        if t == 9:
            et = u('>b', 1)
            n = u('>i', 4)
            return [pay(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = u('>b', 1)
                if tt == 0: break
                k = rs()
                d[k] = pay(tt)
            return d
        if t == 11:
            n = u('>i', 4)
            return list(struct.unpack('>%di' % n, s.read(4 * n)))
        if t == 12:
            n = u('>i', 4)
            return list(struct.unpack('>%dq' % n, s.read(8 * n)))
        raise ValueError('tag %d' % t)

    t = u('>b', 1)
    rs()
    return pay(t)


class T:
    def __init__(s, t, v):
        s.t = t
        s.v = v


def Int(v): return T(3, v)
def Str(v): return T(8, v)
def LIST(items, et): return T(9, (et, items))
def COMP(d): return T(10, d)


def write(root, name=''):
    out = bytearray()

    def ws(s):
        b = s.encode('utf8')
        out.extend(struct.pack('>H', len(b)))
        out.extend(b)

    def pay(t, v):
        if t == 3: out.extend(struct.pack('>i', v))
        elif t == 8: ws(v)
        elif t == 9:
            et, items = v
            out.extend(struct.pack('>b', et))
            out.extend(struct.pack('>i', len(items)))
            for it in items:
                pay(et, it.v if isinstance(it, T) else it)
        elif t == 10:
            for k, x in v.items():
                out.extend(struct.pack('>b', x.t))
                ws(k)
                pay(x.t, x.v)
            out.extend(b'\x00')

    out.extend(struct.pack('>b', 10))
    ws(name)
    pay(10, root.v)
    return gzip.compress(bytes(out), 9)
