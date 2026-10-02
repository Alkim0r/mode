"""NBT с сохранением типов тегов (для переписывания чужих структур без потерь).
Значение тега: (type, value); compound — dict имя -> (type, value); list — (9, (elem_type, [values]))."""
import struct, gzip, io


def read(b):
    if b[:2] == b'\x1f\x8b':
        b = gzip.decompress(b)
    s = io.BytesIO(b)

    def u(f, n):
        return struct.unpack(f, s.read(n))[0]

    def rs():
        return s.read(u('>H', 2)).decode('utf8', 'surrogatepass')

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
            return (et, [pay(et) for _ in range(n)])
        if t == 10:
            d = {}
            while True:
                tt = u('>b', 1)
                if tt == 0: break
                k = rs()
                d[k] = (tt, pay(tt))
            return d
        if t == 11:
            n = u('>i', 4)
            return list(struct.unpack('>%di' % n, s.read(4 * n)))
        if t == 12:
            n = u('>i', 4)
            return list(struct.unpack('>%dq' % n, s.read(8 * n)))
        raise ValueError('tag %d' % t)

    t = u('>b', 1)
    name = rs()
    return (t, pay(t)), name


def write(root, name=''):
    out = bytearray()

    def ws(x):
        b = x.encode('utf8', 'surrogatepass')
        out.extend(struct.pack('>H', len(b)))
        out.extend(b)

    def pay(t, v):
        if t == 1: out.extend(struct.pack('>b', v))
        elif t == 2: out.extend(struct.pack('>h', v))
        elif t == 3: out.extend(struct.pack('>i', v))
        elif t == 4: out.extend(struct.pack('>q', v))
        elif t == 5: out.extend(struct.pack('>f', v))
        elif t == 6: out.extend(struct.pack('>d', v))
        elif t == 7:
            out.extend(struct.pack('>i', len(v)))
            out.extend(v)
        elif t == 8: ws(v)
        elif t == 9:
            et, items = v
            out.extend(struct.pack('>b', et))
            out.extend(struct.pack('>i', len(items)))
            for it in items:
                pay(et, it)
        elif t == 10:
            for k, (tt, vv) in v.items():
                out.extend(struct.pack('>b', tt))
                ws(k)
                pay(tt, vv)
            out.extend(b'\x00')
        elif t == 11:
            out.extend(struct.pack('>i', len(v)))
            out.extend(struct.pack('>%di' % len(v), *v))
        elif t == 12:
            out.extend(struct.pack('>i', len(v)))
            out.extend(struct.pack('>%dq' % len(v), *v))

    t, v = root
    out.extend(struct.pack('>b', t))
    ws(name)
    pay(t, v)
    return gzip.compress(bytes(out), 9)


def walk_strings(tag, fn):
    """Рекурсивно применяет fn к каждой строке; возвращает новый тег."""
    t, v = tag
    if t == 8:
        return (8, fn(v))
    if t == 9:
        et, items = v
        if et == 8:
            return (9, (et, [fn(x) for x in items]))
        if et in (9, 10):
            return (9, (et, [walk_strings((et, x), fn)[1] for x in items]))
        return tag
    if t == 10:
        return (10, {k: walk_strings(x, fn) for k, x in v.items()})
    return tag


def strings(tag, acc=None):
    if acc is None:
        acc = []
    walk_strings(tag, lambda s: (acc.append(s), s)[1])
    return acc
