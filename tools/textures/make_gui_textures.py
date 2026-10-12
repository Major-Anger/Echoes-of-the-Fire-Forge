"""Interface de la Forge des Attiseurs (256 x 256) : fond, marteau, jauges, soufflet. Dessin original.

Usage : python3 make_gui_textures.py <dossier de sortie>
Les positions doivent correspondre à FireForgeMenu et FireForgeScreen.
"""
import os
import random
import sys

from PIL import Image, ImageDraw

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

SUIE = (31, 27, 28)
FER_SOMBRE = (47, 42, 42)
FER = (61, 55, 53)
FER_CLAIR = (107, 98, 92)
RIVET = (143, 135, 128)
LAITON_SOMBRE = (122, 90, 36)
LAITON = (165, 130, 47)
LAITON_CLAIR = (207, 166, 74)
CASE = (40, 36, 36)
FOND_JAUGE = (22, 18, 18)

W, H = 200, 190
img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
px = img.load()
rnd = random.Random(42)


def rect(x, y, w, h, color):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[xx, yy] = color + (255,)


def inset(x, y, w, h, fill=CASE):
    """Un creux (case, jauge) : ombre en haut à gauche, reflet en bas à droite."""
    rect(x, y, w, h, fill)
    rect(x, y, w, 1, SUIE)
    rect(x, y, 1, h, SUIE)
    rect(x, y + h - 1, w, 1, FER_CLAIR)
    rect(x + w - 1, y, 1, h, FER_CLAIR)


# ---------------------------------------------------------------- panneau : plaques de fer, bordure de laiton
for y in range(H):
    for x in range(W):
        px[x, y] = (FER_SOMBRE if rnd.random() < 0.12 else FER) + (255,)
for x in range(W):
    for y in (0, H - 1):
        px[x, y] = LAITON_SOMBRE + (255,)
    px[x, 1] = LAITON_CLAIR + (255,)
    px[x, H - 2] = LAITON + (255,)
for y in range(H):
    for x in (0, W - 1):
        px[x, y] = LAITON_SOMBRE + (255,)
    px[1, y] = LAITON_CLAIR + (255,)
    px[W - 2, y] = LAITON + (255,)
for (x, y) in [(4, 4), (W - 5, 4), (4, H - 5), (W - 5, H - 5), (W // 2, 4), (W // 2, H - 5)]:
    px[x, y] = RIVET + (255,)
    px[x + 1, y + 1] = SUIE + (255,)
# joint entre le haut (la forge) et l'inventaire
for x in range(3, W - 3):
    px[x, 94] = SUIE + (255,)
    px[x, 95] = FER_CLAIR + (255,)

# ---------------------------------------------------------------- creux : jauges, cases, panneau d'état
inset(8, 18, 10, 52, FOND_JAUGE)      # jauge d'eau
inset(46, 18, 10, 52, FOND_JAUGE)     # jauge de chaleur
for (x, y) in [(22, 18), (22, 52), (60, 35), (100, 57)]:
    inset(x, y, 18, 18)                # eau, vides, combustible, offrande
inset(96, 22, 26, 26)                  # l'objet (sur l'enclume)
inset(125, 45, 58, 7, FOND_JAUGE)      # progression
inset(124, 56, 68, 34, FOND_JAUGE)     # état
for row in range(3):
    for col in range(9):
        inset(19 + col * 18, 108 + row * 18, 18, 18)
for col in range(9):
    inset(19 + col * 18, 166, 18, 18)

# petits pictogrammes gravés dans le métal, à côté des jauges
for (x, y) in [(11, 72), (12, 73), (10, 73), (11, 74), (12, 74), (10, 74), (11, 75)]:   # goutte
    px[x, y] = (52, 110, 220, 255)
for (x, y) in [(51, 72), (50, 73), (51, 73), (52, 73), (49, 74), (50, 74), (51, 74), (52, 74), (53, 74), (50, 75), (51, 75), (52, 75)]:  # flamme
    px[x, y] = (230, 120, 40, 255)


# ---------------------------------------------------------------- marteau (24 x 24) : normal, survolé, éteint, frappe
def hammer(handle, head, band, edge):
    h = Image.new('RGBA', (24, 24), (0, 0, 0, 0))
    d = ImageDraw.Draw(h)
    d.line([(4, 20), (14, 10)], fill=handle, width=3)
    d.line([(3, 20), (13, 10)], fill=tuple(min(255, c + 30) for c in handle), width=1)
    cx, cy, a, b = 16, 8, (0.7071, 0.7071), (0.7071, -0.7071)
    poly = [(cx + a[0] * sa * 7 + b[0] * sb * 3, cy + a[1] * sa * 7 + b[1] * sb * 3) for sa, sb in [(-1, -1), (1, -1), (1, 1), (-1, 1)]]
    d.polygon(poly, fill=head, outline=edge)
    d.line([(13, 11), (15, 9)], fill=band, width=2)
    return h


normal = hammer((120, 80, 45), (78, 71, 68), LAITON, FER_CLAIR)
hover = hammer((150, 100, 55), (107, 98, 92), LAITON_CLAIR, LAITON_CLAIR)
off = hammer((70, 55, 45), (50, 46, 45), (90, 75, 50), (70, 64, 60))
strike = normal.rotate(-35, resample=Image.NEAREST, center=(6, 18))
img.alpha_composite(normal, (200, 0))
img.alpha_composite(hover, (224, 0))
img.alpha_composite(off, (200, 24))
img.alpha_composite(strike, (224, 24))

# ---------------------------------------------------------------- remplissages des jauges (8 x 50) et de la progression
for y in range(50):
    t = y / 49
    water = tuple(int(c1 + (c2 - c1) * t) for c1, c2 in zip((90, 165, 245), (35, 80, 190)))
    heat = tuple(int(c1 + (c2 - c1) * t) for c1, c2 in zip((255, 205, 90), (150, 40, 15)))
    for x in range(8):
        px[200 + x, 48 + y] = (tuple(min(255, c + 25) for c in water) if x == 1 else water) + (255,)
        px[210 + x, 48 + y] = (tuple(min(255, c + 20) for c in heat) if x == 1 else heat) + (255,)
for x in range(56):
    t = x / 55
    ember = tuple(int(c1 + (c2 - c1) * t) for c1, c2 in zip((150, 40, 15), (255, 200, 90)))
    for y in range(5):
        px[200 + x, 100 + y] = (tuple(min(255, c + 25) for c in ember) if y == 1 else ember) + (255,)


# ---------------------------------------------------------------- soufflet (26 x 22) : normal, survolé, écrasé, alerte (y = 192)
# Vu de côté : deux planches en coin, du cuir en accordéon, une buse de laiton tournée vers le feu (à gauche).
BOIS = [(86, 54, 30), (122, 80, 46), (150, 102, 60)]
CUIR = [(62, 34, 24), (92, 54, 36), (124, 78, 52)]
ALERTE = (255, 120, 40)


def bellows(press=0.0, hover=False, alert=False):
    b = Image.new('RGBA', (26, 22), (0, 0, 0, 0))
    bp = b.load()

    def put(x, y, c):
        if 0 <= x < 26 and 0 <= y < 22:
            bp[x, y] = c + (255,)

    cy, hinge, opening = 10, 8, 7 - 5 * press

    def top(x):
        return int(round(cy - 1 - max(0, x - hinge) / (22 - hinge) * opening))

    def bottom(x):
        return int(round(cy + 1 + max(0, x - hinge) / (22 - hinge) * opening))

    for x in range(hinge, 23):
        a, z = top(x), bottom(x)
        put(x, a - 1, BOIS[2]); put(x, a, BOIS[1]); put(x, z, BOIS[1]); put(x, z + 1, BOIS[0])
        for y in range(a + 1, z):
            band = int((y - a) / (z - a) * 5)   # plis parallèles aux planches : des chevrons vers la charnière
            put(x, y, CUIR[2] if band % 2 == 0 else CUIR[0] if x % 5 == 0 else CUIR[1])
    for x in (23, 24, 25):                     # poignées
        put(x, top(22) - 1, BOIS[2]); put(x, bottom(22) + 1, BOIS[0])
    for x in (12, 17, 21):                     # clous de laiton
        put(x, top(x) - 1, LAITON_CLAIR); put(x, bottom(x) + 1, LAITON)
    for y in range(cy - 2, cy + 3):            # charnière
        put(hinge - 1, y, LAITON)
    for x in range(1, hinge - 1):              # buse
        put(x, cy - 1, LAITON_CLAIR); put(x, cy, LAITON); put(x, cy + 1, LAITON_SOMBRE)
    src = b.copy().load()                      # contour (rouge-orange quand ça craque)
    for y in range(22):
        for x in range(26):
            if src[x, y][3] == 0 and any(0 <= x + dx < 26 and 0 <= y + dy < 22 and src[x + dx, y + dy][3] > 0
                                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                bp[x, y] = (ALERTE if alert else SUIE) + (255,)
    if hover:
        r, g, bl, a = b.split()
        b = Image.merge('RGBA', (r.point(lambda v: min(255, int(v * 1.2))), g.point(lambda v: min(255, int(v * 1.2))),
                                 bl.point(lambda v: min(255, int(v * 1.1))), a))
    return b


for i, sprite in enumerate([bellows(), bellows(hover=True), bellows(press=1.0), bellows(alert=True)]):
    img.alpha_composite(sprite, (i * 26, 192))

img.save(os.path.join(OUT, 'fire_forge.png'))
print('ok')
