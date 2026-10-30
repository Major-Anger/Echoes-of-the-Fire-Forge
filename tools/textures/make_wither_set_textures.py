"""Textures originales du set du Wither (plan, section 13 ; aperçu en attente de validation d'Anger).

Usage : python3 make_wither_set_textures.py <dossier de sortie>

L'armure que portaient les Attiseurs quand la maladie les a pris : de l'acier noirci, garni de laiton, des lunettes
rondes au heaume (comme celles du Cendreux). Elle change avec le porteur, en cinq états :
  0 neuve ; 1 des fêlures, la cendre dedans ; 2 la braise dans les fêlures ; 3 le laiton terni, l'acier qui s'écaille
  sur de l'os ; 4 presque tout os noirci, la braise entre les côtes, les lunettes fendues.
Sorties : wither_layer_1_<n>.png et wither_layer_2_<n>.png (64 x 32, modèles d'armure ; à partir du palier 2, leur
couche de braises lumineuse _glow), les icônes des quatre pièces et du Gabarit du Wither (16 x 16).
"""
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

STEEL_DD = (22, 20, 20)
STEEL_D = (34, 31, 31)
STEEL = (48, 44, 43)
STEEL_L = (68, 62, 60)
EDGE = (96, 88, 83)
BRASS_D = (122, 90, 36)
BRASS = (165, 130, 47)
BRASS_L = (207, 166, 74)
TARNISH = (76, 60, 34)       # laiton terni
TARNISH_D = (52, 42, 28)
ASH = (140, 133, 127)
ASH_L = (170, 163, 156)
BONE_D = (40, 36, 35)
BONE = (60, 55, 52)
BONE_L = (84, 77, 73)
CHAR_D = (17, 15, 15)
EMB_D = (150, 52, 22)
EMB = (232, 112, 38)
EMB_L = (255, 190, 92)
GLASS = (70, 44, 28)
GLASS_L = (150, 96, 50)
CLEAR = (0, 0, 0, 0)


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


HEAD = faces(0, 0, 8, 8, 8)
BODY = faces(16, 16, 8, 12, 4)
ARM = faces(40, 16, 4, 12, 4)
LEG = faces(0, 16, 4, 12, 4)
SIDES = ("right", "front", "left", "back")


class Sheet:
    """Une feuille 64 x 32, et ce que chaque pixel « est » (plaque, laiton, verre…) pour les états suivants."""

    def __init__(self):
        self.img = Image.new("RGBA", (64, 32), CLEAR)
        self.kind = {}

    def put(self, x, y, color, kind="plate"):
        self.img.putpixel((x, y), color + (255,) if len(color) == 3 else color)
        self.kind[(x, y)] = kind

    def fill(self, rect, pick, kind="plate"):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                c = pick(x, y, w, h)
                if c is not None:
                    self.put(x0 + x, y0 + y, c, kind)


def plate(x, y, w, h):
    """Acier noirci : un bord clair en haut, un joint sombre en bas, un grain discret."""
    if y == 0:
        return STEEL_L
    if y == h - 1:
        return STEEL_DD
    return STEEL_D if (x * 7 + y * 5) % 9 == 0 else STEEL


def base_layer_1():
    s = Sheet()
    # ---- le heaume : acier, bandeau de laiton, deux lunettes rondes, une fente de visière
    for f in ("top", "right", "left", "back"):
        s.fill(HEAD[f], plate)
    s.fill(HEAD["bottom"], lambda x, y, w, h: CLEAR, "none")
    fx, fy, _, _ = HEAD["front"]
    s.fill(HEAD["front"], plate)
    for f in SIDES:
        x0, y0, w, h = HEAD[f]
        for x in range(w):
            s.put(x0 + x, y0 + 2, BRASS_L if x % 3 == 0 else BRASS, "brass")
    for cx in (1, 5):                                   # les lunettes, sur les yeux
        for dx, dy in [(0, 3), (1, 3), (0, 4), (1, 4)]:
            s.put(fx + cx + dx, fy + dy, GLASS if (dx + dy) % 2 else GLASS_L, "glass")
        for dx, dy in [(-1, 3), (-1, 4), (2, 3), (2, 4), (0, 2), (1, 2), (0, 5), (1, 5)]:
            s.put(fx + cx + dx, fy + dy, BRASS_L if dy == 2 else BRASS, "brass")
    for x in range(1, 7):                               # la visière, en bas
        s.put(fx + x, fy + 6, CHAR_D, "slit")
    tx, ty, _, _ = HEAD["top"]
    for y in range(8):                                  # une crête de laiton sur le crâne
        s.put(tx + 3, ty + y, BRASS_D, "brass")
        s.put(tx + 4, ty + y, BRASS, "brass")
    # ---- le plastron : des côtes d'acier, un sternum de laiton riveté
    for f in ("top", "bottom", "right", "left", "back"):
        s.fill(BODY[f], plate)
    bx, by, bw, bh = BODY["front"]
    for y in range(bh):
        for x in range(bw):
            if x in (3, 4):
                c, k = (BRASS_L if y % 3 == 0 else BRASS), "brass"
            elif y % 2 == 0 and y < 9:
                c, k = (EDGE if x in (1, 6) else STEEL_L), "rib"
            else:
                c, k = (STEEL_DD if y < 9 else STEEL), "gap" if y < 9 else "plate"
            s.put(bx + x, by + y, c, k)
    for x in range(bw):
        s.put(bx + x, by + bh - 1, BRASS_D, "brass")
    kx, ky, kw, kh = BODY["back"]
    for y in range(kh):                                 # l'échine, au dos
        s.put(kx + 3, ky + y, EDGE if y % 2 == 0 else STEEL_L, "rib")
        s.put(kx + 4, ky + y, STEEL_L if y % 2 == 0 else STEEL, "rib")
    # ---- les bras : épaulières, coudières de laiton
    for f in ("top", "right", "front", "left", "back"):
        s.fill(ARM[f], plate)
    s.fill(ARM["bottom"], lambda x, y, w, h: STEEL_DD)
    for f in SIDES:
        x0, y0, w, h = ARM[f]
        for x in range(w):
            s.put(x0 + x, y0 + 0, EDGE, "rib")
            s.put(x0 + x, y0 + 1, STEEL_L, "rib")
            s.put(x0 + x, y0 + 6, BRASS if x % 2 else BRASS_L, "brass")
        for y in range(9, 12):                          # gantelets
            for x in range(w):
                s.put(x0 + x, y0 + y, STEEL_D if (x + y) % 2 else STEEL, "plate")
    # ---- les bottes (bas des jambes) : semelle épaisse, embout de laiton
    for f in SIDES:
        x0, y0, w, h = LEG[f]
        for y in range(h):
            for x in range(w):
                if y < 6:
                    continue
                c, k = (STEEL_L if y == 6 else (STEEL_DD if y == 11 else plate(x, y - 6, w, 6))), ("rib" if y == 6 else "plate")
                if y == 7 and x % 3 == 1:
                    c, k = BRASS, "brass"                       # rivets de la tige
                if f == "front" and y == 10 and x in (1, 2):
                    c, k = BRASS_L, "brass"                     # l'embout"
                s.put(x0 + x, y0 + y, c, k)
    s.fill(LEG["bottom"], lambda x, y, w, h: STEEL_DD)
    return s


def base_layer_2():
    s = Sheet()
    # ---- la ceinture (bas du corps) : cuir d'acier, boucle de laiton
    for f in SIDES:
        x0, y0, w, h = BODY[f]
        for y in range(h - 4, h):
            for x in range(w):
                s.put(x0 + x, y0 + y, BRASS_D if y == h - 4 else plate(x, y - (h - 4), w, 4), "brass" if y == h - 4 else "plate")
    bx, by, bw, bh = BODY["front"]
    for x, y in [(3, bh - 3), (4, bh - 3), (3, bh - 2), (4, bh - 2)]:
        s.put(bx + x, by + y, BRASS_L if y == bh - 3 else BRASS, "brass")
    # ---- les cuissards et genouillères
    for f in SIDES:
        x0, y0, w, h = LEG[f]
        for y in range(0, 9):
            for x in range(w):
                c, k = plate(x, y % 4, w, 4), "plate"
                if y in (5, 6) and f == "front":
                    c, k = (EDGE if y == 5 else STEEL_L), "rib"   # la genouillère
                if y == 0:
                    c, k = BRASS_D, "brass"
                s.put(x0 + x, y0 + y, c, k)
    s.fill(LEG["top"], plate)
    return s


# ---------------------------------------------------------------------- les états : la maladie gagne l'armure
CRACKS = []
rnd = random.Random(13)
for _ in range(26):
    x, y = rnd.randrange(64), rnd.randrange(32)
    path = []
    for _ in range(rnd.randrange(3, 7)):
        path.append((x, y))
        x = max(0, min(63, x + rnd.choice((-1, 0, 1))))
        y = max(0, min(31, y + 1))
    CRACKS.append(path)
NOISE = {(x, y): random.Random(x * 131 + y * 17).random() for x in range(64) for y in range(32)}
# par plaques de 2 x 2 : l'acier s'écaille par morceaux, pas pixel par pixel
FLAKE = {(x, y): random.Random((x // 2) * 977 + (y // 2) * 59).random() for x in range(64) for y in range(32)}


def stage(base, n):
    s = Sheet()
    s.img = base.img.copy()
    s.kind = dict(base.kind)
    if n == 0:
        return s
    px = s.img.load()
    painted = set(k for k, v in base.kind.items() if v != "none")
    cracks = CRACKS[: 8 + n * 6]
    for path in cracks:
        for i, (x, y) in enumerate(path):
            if (x, y) not in painted or base.kind[(x, y)] in ("glass", "slit"):
                continue
            if n == 1:
                px[x, y] = ASH + (255,) if i % 2 else CHAR_D + (255,)          # fêlure, la cendre dedans
            else:
                px[x, y] = (EMB if i % 3 else EMB_L) + (255,) if n >= 2 and i % 2 == 0 else CHAR_D + (255,)
    for (x, y) in painted:
        k, r = base.kind[(x, y)], NOISE[(x, y)]
        if k == "brass":
            if n >= 3:
                px[x, y] = (TARNISH if r < 0.6 else TARNISH_D) + (255,)
            elif n == 2 and r < 0.3:
                px[x, y] = TARNISH + (255,)
        elif k in ("plate", "rib"):
            f = FLAKE[(x, y)]
            if n >= 3 and f > (0.72 if n == 3 else 0.4):
                px[x, y] = (BONE if r < 0.6 else BONE_L) + (255,)              # l'acier s'écaille sur de l'os
            elif n >= 2 and r < 0.02 * n:
                px[x, y] = ASH + (255,)                                       # un peu de croûte de cendre
        elif k == "gap" and n >= 2:
            px[x, y] = (EMB_D if n == 2 else (EMB if r < 0.6 else EMB_L)) + (255,)   # la braise entre les côtes
        elif k == "glass":
            if n >= 3:
                px[x, y] = (CHAR_D if r < 0.5 else GLASS) + (255,)
            if n == 4 and r > 0.55:
                px[x, y] = EMB_L + (255,)                                     # derrière le verre fendu, la braise
    if n == 4:
        for (x, y) in painted:
            if base.kind[(x, y)] == "rib":
                px[x, y] = (BONE_L if NOISE[(x, y)] < 0.5 else BONE) + (255,)  # les côtes sont de l'os
            elif base.kind[(x, y)] == "plate" and FLAKE[(x, y)] <= 0.4:
                px[x, y] = (BONE_D if NOISE[(x, y)] < 0.5 else STEEL_DD) + (255,)  # ce qui reste d'acier, noirci
    return s


def embers(img):
    """La couche lumineuse : seulement les pixels de braise (ils luisent dans le noir, comme ceux du Cendreux)."""
    glow = Image.new("RGBA", img.size, CLEAR)
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3] and c[:3] in (EMB, EMB_L, EMB_D):
                glow.putpixel((x, y), c)
    return glow


L1, L2 = base_layer_1(), base_layer_2()
for n in range(5):
    for layer, base in ((1, L1), (2, L2)):
        img = stage(base, n).img
        img.save(os.path.join(OUT, f"wither_layer_{layer}_{n}.png"))
        if n >= 2:
            embers(img).save(os.path.join(OUT, f"wither_layer_{layer}_{n}_glow.png"))

# ---------------------------------------------------------------------- les icônes (16 x 16)
PAL = {'.': None, 'K': STEEL_DD, 'D': STEEL_D, 'S': STEEL, 'L': STEEL_L, 'E': EDGE, 'B': BRASS_D, 'b': BRASS,
       'Y': BRASS_L, 'g': GLASS, 'G': GLASS_L, 'k': CHAR_D, 'F': EMB, 'f': EMB_L, 'h': EMB_D, 'o': BONE_L}


def icon(rows, name):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row)
        for x, ch in enumerate(row):
            if PAL[ch]:
                img.putpixel((x, y), PAL[ch] + (255,))
    img.save(os.path.join(OUT, name + ".png"))


icon([
    "................",
    "......KKKK......",
    "....KKLbYLKK....",
    "...KSSSbbSSSK...",
    "..KSLSSbbSSLSK..",
    "..KbbYbbbbYbbK..",
    "..KBYgGBBgGYBK..",
    "..KBbGgBBGgbBK..",
    "..KSBBSSSSBBSK..",
    "..KSSkkkkkkSSK..",
    "..KDS......SDK..",
    "..KDK......KDK..",
    "...K........K...",
    "................",
    "................",
    "................",
], "wither_helmet")
icon([
    "................",
    "..KKKK....KKKK..",
    ".KLLLSKKKKSLLLK.",
    ".KSSSSYbbYSSSSK.",
    ".KEESKbYYbKSEEK.",
    ".KSSKELbbLEKSSK.",
    "..KKKKkbbkKKKK..",
    "....KELbbLEK....",
    "....KkkbbkkK....",
    "....KELbbLEK....",
    "....KkkbbkkK....",
    "....KSSbbSSK....",
    "....KBBBBBBK....",
    ".....KKKKKK.....",
    "................",
    "................",
], "wither_chestplate")
icon([
    "................",
    "...KKKKKKKKKK...",
    "...KBBBYYBBBK...",
    "...KSSSbbSSSK...",
    "...KSLSKKSLSK...",
    "...KSSK..KSSK...",
    "...KSSK..KSSK...",
    "...KELK..KLEK...",
    "...KLSK..KSLK...",
    "...KSSK..KSSK...",
    "...KSDK..KDSK...",
    "...KSSK..KSSK...",
    "...KKKK..KKKK...",
    "................",
    "................",
    "................",
], "wither_leggings")
icon([
    "................",
    "................",
    "................",
    "................",
    "...KKKK..KKKK...",
    "...KLSK..KSLK...",
    "...KSSK..KSSK...",
    "...KYYK..KYYK...",
    "...KSSK..KSSK...",
    "..KSSSK..KSSSK..",
    ".KbSSSK..KSSSbK.",
    ".KbbSDK..KDSbbK.",
    ".KKKKKK..KKKKKK.",
    "................",
    "................",
    "................",
], "wither_boots")
# le Gabarit du Wither : la même plaque que les autres gabarits, en acier noirci, deux lunettes rondes gravées
icon([
    "................",
    "..KKKKKKKKKKKK..",
    "..KDDDDkkDDDDK..",
    "..KDSSSSSSSSDK..",
    "..KDSbbSSbbSDK..",
    "..KDbFbSSbFbDK..",
    "..KDSbbSSbbSDK..",
    "..KDSSSSSSSSDK..",
    "..KDSkSkkSkSDK..",
    "..KDSSkSSkSSDK..",
    "..KDSSSkkSSSDK..",
    "..KDSSSSSSSSDK..",
    "..KDDDDDDDDDDK..",
    "..KKKKKKKKKKKK..",
    "................",
    "................",
], "wither_template")
print("ok")
