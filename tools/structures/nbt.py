"""Un petit lecteur et écrivain NBT (le format des gabarits de structure de Minecraft), sans dépendance.

Les valeurs Python portent leur type NBT : Byte, Short, Int, Long, Float, Double, String, List, Compound.
"""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class Tag:
    tag_id = None

    def __init__(self, value):
        self.value = value

    def __repr__(self):
        return f"{type(self).__name__}({self.value!r})"

    def __eq__(self, other):
        return type(self) is type(other) and self.value == other.value


class Byte(Tag): tag_id = BYTE
class Short(Tag): tag_id = SHORT
class Int(Tag): tag_id = INT
class Long(Tag): tag_id = LONG
class Float(Tag): tag_id = FLOAT
class Double(Tag): tag_id = DOUBLE
class String(Tag): tag_id = STRING
class IntArray(Tag): tag_id = INT_ARRAY


class List(Tag):
    tag_id = LIST

    def __init__(self, item_type, items):
        super().__init__(list(items))
        self.item_type = item_type


class Compound(Tag):
    tag_id = COMPOUND

    def __init__(self, value=None):
        super().__init__(dict(value or {}))


def _string(s):
    data = s.encode("utf-8")  # (UTF-8 modifié : identique pour nos textes, sans caractère nul ni hors BMP)
    return struct.pack(">H", len(data)) + data


def _payload(tag):
    t = tag.tag_id
    v = tag.value
    if t == BYTE: return struct.pack(">b", v)
    if t == SHORT: return struct.pack(">h", v)
    if t == INT: return struct.pack(">i", v)
    if t == LONG: return struct.pack(">q", v)
    if t == FLOAT: return struct.pack(">f", v)
    if t == DOUBLE: return struct.pack(">d", v)
    if t == STRING: return _string(v)
    if t == INT_ARRAY: return struct.pack(">i", len(v)) + b"".join(struct.pack(">i", x) for x in v)
    if t == LIST:
        item_type = tag.item_type.tag_id if tag.value else END
        return struct.pack(">bi", item_type, len(v)) + b"".join(_payload(x) for x in v)
    if t == COMPOUND:
        out = b""
        for name, child in v.items():
            out += struct.pack(">b", child.tag_id) + _string(name) + _payload(child)
        return out + struct.pack(">b", END)
    raise ValueError(t)


def write(path, root):
    with gzip.open(path, "wb") as f:
        f.write(struct.pack(">b", COMPOUND) + _string("") + _payload(root))


def read(path):
    with gzip.open(path, "rb") as f:
        data = f.read()
    pos = [0]

    def take(fmt):
        size = struct.calcsize(fmt)
        out = struct.unpack(fmt, data[pos[0]:pos[0] + size])
        pos[0] += size
        return out[0] if len(out) == 1 else out

    def string():
        n = take(">H")
        s = data[pos[0]:pos[0] + n].decode("utf-8", "replace")
        pos[0] += n
        return s

    def payload(t):
        if t == BYTE: return Byte(take(">b"))
        if t == SHORT: return Short(take(">h"))
        if t == INT: return Int(take(">i"))
        if t == LONG: return Long(take(">q"))
        if t == FLOAT: return Float(take(">f"))
        if t == DOUBLE: return Double(take(">d"))
        if t == BYTE_ARRAY:
            n = take(">i"); pos[0] += n; return None
        if t == STRING: return String(string())
        if t == INT_ARRAY:
            n = take(">i"); return IntArray([take(">i") for _ in range(n)])
        if t == LONG_ARRAY:
            n = take(">i"); return [take(">q") for _ in range(n)]
        if t == LIST:
            item, n = take(">bi")
            items = [payload(item) for _ in range(n)]
            lst = List(type(items[0]) if items else Tag, items)
            return lst
        if t == COMPOUND:
            out = {}
            while True:
                child = take(">b")
                if child == END:
                    return Compound(out)
                name = string()
                out[name] = payload(child)
        raise ValueError(t)

    assert take(">b") == COMPOUND
    string()
    return payload(COMPOUND)
