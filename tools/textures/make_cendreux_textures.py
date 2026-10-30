"""Textures originales de l'étape 12c (aperçu, en attente de validation d'Anger) : le Cendreux (64x64, avec sa
couche de braises qui luit dans le noir), la Cendre d'Attiseur, le Lit de cendre, et la face avant de la forge
garnie de son lit de cendre (avec le plateau de laiton qui dépasse du foyer).

Usage : python3 make_cendreux_textures.py <dossier de sortie>
Le modèle du Cendreux (les boîtes et leurs coins de texture) est dans CENDREUX_PARTS : la planche d'aperçu le
dessine en 3D, et le code Java le reprendra tel quel.

Le Cendreux est un Attiseur déjà presque wither squelette : des os noircis, la cage thoracique ouverte où couve la
braise, et seulement des lambeaux de peau de cendre (la moitié du visage, une épaule, les cuisses). Ce qui reste de
l'Attiseur : les lunettes relevées, le tablier roussi, les bandes de cuir.
"""
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

# ---------------------------------------------------------------------- couleurs
ASH_W = (170, 163, 156)    # cendre claire
ASH = (136, 129, 123)      # peau de cendre
ASH_D = (108, 102, 97)
ASH_DD = (84, 79, 76)
BONE_L = (84, 77, 73)      # os calciné, reflet
BONE = (60, 55, 52)        # os calciné
BONE_D = (40, 36, 35)
CHAR = (30, 27, 27)        # noir de suie
CHAR_D = (17, 15, 15)      # creux (orbites, entre les côtes)
EMB_D = (150, 52, 22)      # braise sombre
EMB = (232, 112, 38)       # braise
EMB_L = (255, 190, 92)     # braise vive
LEA_D = (64, 40, 25)       # cuir
LEA = (92, 60, 35)
LEA_L = (118, 80, 48)
SINGE = (44, 30, 22)       # cuir roussi
CLOTH_D = (48, 43, 41)     # toile
CLOTH = (64, 58, 55)
BR_D = (122, 90, 36)       # laiton
BR = (165, 130, 47)
BR_L = (207, 166, 74)
GLASS_L = (150, 96, 50)    # verre fumé des lunettes

# ---------------------------------------------------------------------- le modèle
# nom, parent, pivot (x, y, z), boîte (x, y, z) relative au pivot, taille (l, h, p), coin de texture (u, v),
# rotation (degrés, x, y, z), gonflement. Repère : y vers le haut, l'avant vers +z, en pixels de modèle (1/16 bloc).
# Membres d'os de 2 px, bras de 12 px (comme un squelette). Hauteur : 37 px, 2,3 blocs (le joueur : 32 ;
# un wither squelette : 38).
CENDREUX_PARTS = [
    ("body", None, (0, 17, 0), (-4, 0, -2), (8, 12, 4), (16, 16), (14, 0, 0), 0),
    ("head", "body", (0, 12, 0), (-4, 0, -4), (8, 8, 8), (0, 0), (-8, 0, 6), 0),
    ("strap", "head", (0, 0, 0), (-4, 4, -4), (8, 2, 8), (32, 0), (0, 0, 0), 0.4),
    ("lens_r", "head", (0, 0, 0), (-3.5, 4.2, 4.2), (3, 3, 1), (32, 10), (0, 0, 0), 0),
    ("lens_l", "head", (0, 0, 0), (0.5, 4.2, 4.2), (3, 3, 1), (40, 10), (0, 0, 0), 0),
    ("apron", "body", (0, 0, 0), (-3, -5, 2), (6, 8, 1), (40, 16), (-12, 0, 0), 0),
    ("arm_r", "body", (-5, 11, 0), (-1, -12, -1), (2, 12, 2), (0, 16), (-20, 0, -5), 0),
    ("arm_l", "body", (5, 11, 0), (-1, -12, -1), (2, 12, 2), (0, 36), (-10, 0, 4), 0),
    ("leg_r", None, (-2, 17, 0), (-1, -17, -1), (2, 17, 2), (16, 36), (6, 0, 0), 0),
    ("leg_l", None, (2, 17, 0), (-1, -17, -1), (2, 17, 2), (24, 36), (-6, 0, 0), 0),
]


def faces(u, v, w, h, d):
    """Les six faces d'une boîte dans la texture (dépliage habituel des modèles d'entité)."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


rnd = random.Random(12)
tex = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def put(img, x, y, c):
    img.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def fill(rect, pick):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = pick(x, y, w, h)
            if c is not None:
                put(tex, x0 + x, y0 + y, c)


def ember(x, y, c=EMB):
    put(tex, x, y, c)
    put(glow, x, y, c)


def bone(x, y, w, h):
    r = rnd.random()
    return BONE_L if r < 0.12 else (BONE if r < 0.75 else BONE_D)


def skin(x, y, w, h):
    r = rnd.random()
    return ASH_W if r < 0.06 else (ASH if r < 0.7 else (ASH_D if r < 0.94 else ASH_DD))


def part_rect(name):
    for n, _p, _pv, _o, size, uv, _r, _i in CENDREUX_PARTS:
        if n == name:
            return faces(uv[0], uv[1], *size)
    raise KeyError(name)


# ---------------------------------------------------------------------- tête : moitié crâne, moitié peau de cendre
H = part_rect("head")
# la moitié droite du personnage (à gauche, vue de face) est déjà un crâne noir ; l'autre garde sa peau, fendue
for f in ("top", "bottom", "back"):
    fill(H[f], lambda x, y, w, h: bone(x, y, w, h) if x < 4 + (y % 3 == 0) else skin(x, y, w, h))
fill(H["right"], bone)
fill(H["left"], skin)
fx, fy, _, _ = H["front"]
fill(H["front"], lambda x, y, w, h: bone(x, y, w, h) if x < 4 + (1 if y in (2, 5) else 0) else skin(x, y, w, h))
# le bord de la peau, déchiré
for y in range(8):
    put(tex, fx + 4 + (1 if y in (2, 5) else 0), fy + y, ASH_DD)
# côté crâne : une orbite vide où brûle une braise, la cavité du nez, les dents
for x, y in [(1, 3), (2, 3), (1, 4), (2, 4), (1, 5)]:
    put(tex, fx + x, fy + y, CHAR_D)
ember(fx + 2, fy + 4, EMB_L)
put(tex, fx + 3, fy + 5, CHAR_D)
for x in range(0, 4):
    put(tex, fx + x, fy + 6, BONE_L if x % 2 else CHAR_D)
    put(tex, fx + x, fy + 7, CHAR_D if x % 2 else BONE_D)
# côté peau : une fente de braise pour œil, la bouche qui tousse
put(tex, fx + 5, fy + 3, ASH_DD)
put(tex, fx + 6, fy + 3, ASH_DD)
ember(fx + 5, fy + 4, EMB)
ember(fx + 6, fy + 4, EMB_D)
for x in (5, 6):
    put(tex, fx + x, fy + 6, CHAR)
put(tex, fx + 7, fy + 6, ASH_DD)
# fêlures sur le crâne, la braise affleure
tx, ty, _, _ = H["top"]
for x, y in [(5, 2), (6, 3), (6, 4), (5, 6)]:
    put(tex, tx + x, ty + y, CHAR)
ember(tx + 6, ty + 3, EMB_D)
bx, by, _, _ = H["back"]
for x, y in [(5, 3), (6, 4), (6, 5)]:
    put(tex, bx + x, by + y, CHAR)

# ---------------------------------------------------------------------- sangle et lunettes relevées sur le front
S = part_rect("strap")
for f in ("right", "front", "left", "back"):
    fill(S[f], lambda x, y, w, h: LEA_L if y == 0 and x % 3 == 0 else (LEA if y == 0 else LEA_D))
sbx, sby, _, _ = S["back"]
for x in (3, 4):
    for y in (0, 1):
        put(tex, sbx + x, sby + y, BR_L if y == 0 else BR)      # la boucle, derrière la tête
for name in ("lens_r", "lens_l"):
    L = part_rect(name)
    for f in ("top", "bottom", "right", "left", "back"):
        fill(L[f], lambda x, y, w, h: BR if (x + y) % 2 else BR_D)
    lx, ly, _, _ = L["front"]
    for y in range(3):
        for x in range(3):
            put(tex, lx + x, ly + y, BR_L if (x, y) == (0, 0) else BR)
    put(tex, lx + 1, ly + 1, GLASS_L)

# ---------------------------------------------------------------------- corps : la cage ouverte, la braise dedans
B = part_rect("body")
cx, cy, cw, ch = B["front"]
for y in range(12):
    for x in range(8):
        if y < 8:
            # côtes noires sur un fond de braise : les lignes paires sont des côtes, les impaires des creux
            if x in (3, 4):
                c = BONE_L if y % 2 == 0 else BONE              # le sternum
            elif y % 2 == 0:
                c = BONE if x not in (0, 7) else BONE_D
            else:
                c = None
            if c is None:
                # la braise couve au fond, plus vive au cœur (sous le sternum), éteinte sur les bords
                if x in (0, 7) or y == 7:
                    put(tex, cx + x, cy + y, CHAR_D)
                elif x in (1, 6) or y == 1:
                    ember(cx + x, cy + y, EMB_D)
                else:
                    ember(cx + x, cy + y, EMB_L if y in (3, 5) else EMB)
            else:
                put(tex, cx + x, cy + y, c)
        else:
            put(tex, cx + x, cy + y, BONE_D if (x + y) % 3 else CHAR)   # le ventre creux, la colonne
# une plaque de peau de cendre tient encore sur l'épaule gauche (à droite, vue de face)
for x, y in [(6, 0), (7, 0), (7, 1), (6, 1), (7, 2)]:
    put(tex, cx + x, cy + y, ASH)
# les bretelles du tablier
for y in range(0, 10):
    put(tex, cx + 1, cy + y, LEA_D if y % 4 else LEA)
    put(tex, cx + 6, cy + y, LEA_D if y % 4 else LEA)
for f in ("right", "left"):
    x0, y0, w, h = B[f]
    for y in range(h):
        for x in range(w):
            if y in (3, 5) and 0 < x < w - 1:
                ember(x0 + x, y0 + y, EMB_D)
            else:
                put(tex, x0 + x, y0 + y, bone(x, y, w, h))
# dos : la colonne, les omoplates, les bretelles croisées
kx, ky, _, _ = B["back"]
fill(B["back"], lambda x, y, w, h: BONE_D if (x + 2 * y) % 5 else CHAR)
for y in range(12):
    put(tex, kx + 3, ky + y, BONE_L if y % 2 == 0 else BONE)
    put(tex, kx + 4, ky + y, BONE if y % 2 == 0 else BONE_D)
for x, y in [(1, 1), (2, 1), (1, 2), (5, 1), (6, 1), (6, 2)]:
    put(tex, kx + x, ky + y, BONE)
for i in range(8):
    put(tex, kx + i, ky + 1 + i, LEA_D)
    put(tex, kx + 7 - i, ky + 1 + i, LEA_D)
fill(B["top"], bone)
fill(B["bottom"], lambda x, y, w, h: CHAR)
# la ceinture de cuir et sa boucle
for f in ("front", "right", "left", "back"):
    x0, y0, w, h = B[f]
    for x in range(w):
        put(tex, x0 + x, y0 + h - 2, LEA)
        put(tex, x0 + x, y0 + h - 1, LEA_D)
put(tex, cx + 3, cy + 10, BR_L)
put(tex, cx + 4, cy + 10, BR)

# ---------------------------------------------------------------------- tablier de forgeron, roussi et en lambeaux
A = part_rect("apron")
fill(A["front"], lambda x, y, w, h: LEA_L if (x * 5 + y * 3) % 7 == 0 else (LEA if (x + y) % 3 else LEA_D))
for f in ("top", "bottom", "right", "left", "back"):
    fill(A[f], lambda x, y, w, h: LEA_D)
ax, ay, aw, ah = A["front"]
put(tex, ax, ay, BR_L)
put(tex, ax + 5, ay, BR_L)
for x, y in [(1, 3), (2, 3), (4, 5), (3, 5), (1, 6)]:
    put(tex, ax + x, ay + y, SINGE)
for x, y in [(0, 7), (2, 7), (3, 7), (5, 7), (5, 6)]:
    put(tex, ax + x, ay + y, (0, 0, 0, 0))                          # déchiré, brûlé
for x, y in [(0, 7), (2, 7), (3, 7), (5, 7), (5, 6)]:
    bx_, by_, _, _ = A["back"]
    put(tex, bx_ + (5 - x), by_ + y, (0, 0, 0, 0))

# ---------------------------------------------------------------------- bras : des os, un brassard, un lambeau de peau
for name in ("arm_r", "arm_l"):
    R = part_rect(name)
    for f in ("right", "front", "left", "back"):
        fill(R[f], bone)
        x0, y0, w, h = R[f]
        for y in (4, 5, 6):                                          # brassard de cuir
            for x in range(w):
                put(tex, x0 + x, y0 + y, LEA if y != 5 else LEA_D)
        for y in range(h - 3, h):                                    # la main : phalanges
            put(tex, x0 + (y % 2), y0 + y, CHAR_D)
    fill(R["top"], bone)
    fill(R["bottom"], lambda x, y, w, h: CHAR_D)
# le bras gauche garde un lambeau de peau de cendre à l'épaule, avec une veine de braise
for f in ("front", "left", "back"):
    x0, y0, w, h = part_rect("arm_l")[f]
    for y in range(0, 4):
        for x in range(w):
            put(tex, x0 + x, y0 + y, skin(x, y, w, h))
lx, ly, _, _ = part_rect("arm_l")["front"]
ember(lx + 1, ly + 2, EMB)
ember(lx + 1, ly + 3, EMB_D)

# ---------------------------------------------------------------------- jambes : des os, des restes de toile
for name in ("leg_r", "leg_l"):
    G = part_rect(name)
    for f in ("right", "front", "left", "back"):
        x0, y0, w, h = G[f]
        for y in range(h):
            for x in range(w):
                if y < 6:
                    c = CLOTH if (x + y) % 3 else CLOTH_D            # le haut du pantalon, en loques
                    if y == 5 and (x + len(f)) % 2:
                        c = BONE                                     # la toile s'effiloche, l'os paraît
                elif y in (9, 10):
                    c = LEA_D if (y + x) % 2 else LEA                # bande de cuir au genou
                else:
                    c = bone(x, y, w, h)
                put(tex, x0 + x, y0 + y, c)
    fill(G["top"], lambda x, y, w, h: CLOTH_D)
    fill(G["bottom"], lambda x, y, w, h: CHAR_D)

tex.save(os.path.join(OUT, "cendreux.png"))
glow.save(os.path.join(OUT, "cendreux_glow.png"))




# ---------------------------------------------------------------------- objets 16x16
PAL = {
    '.': None, 'W': ASH_W, 'a': ASH, 'g': ASH_D, 'G': ASH_DD, 'k': CHAR_D, 'c': CHAR,
    'h': EMB_D, 'F': EMB, 'f': EMB_L, 'B': BR_D, 'b': BR, 'Y': BR_L, 's': (110, 104, 100), 'K': (31, 27, 28),
    'D': (47, 42, 42), 'd': (61, 55, 53),
}


def save(rows, name, pal=PAL):
    rows = rows + ["." * len(rows[0])] * (16 - len(rows))
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if pal[ch]:
                img.putpixel((x, y), pal[ch] + (255,))
    img.save(os.path.join(OUT, name + ".png"))


# la Cendre d'Attiseur : une pincée de cendre pâle, encore tiède (des braises dedans), un filet de fumée
save([
    "................",
    "........s.......",
    ".......s........",
    "........s.......",
    ".......s........",
    "................",
    "......WWa.......",
    ".....WaaFag.....",
    "....WaaagaWg....",
    "...WaaFaaaaag...",
    "..WaagaaahaWgg..",
    "..aaaaWagaaaagG.",
    ".gaFaaaaaaFagaG.",
    ".GgagagaaagaggG.",
    "..GGgGGgGgGGGG..",
    "................",
], "stoker_ash")

# le Lit de cendre : un plateau de laiton garni de cendre, des braises qui couvent ; une grille à barreaux devant
save([
    "................",
    "................",
    "................",
    "................",
    "......YbbbY.....",
    "....YbWaaWabY...",
    "..YbaWaFaaaWabY.",
    ".bWaaagaaWaFaag.",
    "YbaaFaaWaaagaabY",
    "BbbbaaagaFaabbbB",
    "BYbbbbbbbbbbbbYB",
    "BbBkBkBkBkBkBkbB",
    "BbBkBkBkBkBkBkbB",
    ".BBBBBBBBBBBBBB.",
    "..BBBBBBBBBBBB..",
    "................",
], "ash_bed")

# ---------------------------------------------------------------------- la forge garnie : le foyer et son plateau
# Même face avant que la forge (make_forge_textures.py), mais le bas du foyer est un lit de cendre pâle où couvent
# les braises, au lieu de la grille nue.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
FORGE = {
    'K': (31, 27, 28), 'D': (47, 42, 42), 'd': (61, 55, 53), 'M': (78, 71, 68), 'R': (143, 135, 128),
    'B': BR_D, 'b': BR, 'Y': BR_L, 'k': (22, 15, 13), 'h': (120, 40, 18), 'F': (225, 110, 36), 'f': (255, 186, 82),
    'W': ASH_W, 'a': ASH, 'g': ASH_D,
}
SOUL = dict(FORGE, h=(18, 70, 84), F=(54, 196, 206), f=(170, 244, 240))


def plates(width, height):
    g = []
    for y in range(height):
        row = []
        for x in range(width):
            if y == 0:
                ch = 'Y' if x % 5 == 1 else 'b'
            elif y == 1:
                ch = 'B'
            elif x % 16 in (0, 15):
                ch = 'K'
            elif y == height - 1:
                ch = 'D'
            else:
                ch = 'D' if (x * 7 + y * 3) % 11 == 0 else 'd'
                if y == 2:
                    ch = 'M'
            if y in (3, height - 2) and x % 16 in (2, 13):
                ch = 'R'
            row.append(ch)
        g.append(row)
    return g


Hf = 12
front = plates(32, Hf)
x0, x1, y0 = 9, 22, 3
for y in range(y0, Hf):
    for x in range(x0, x1 + 1):
        if x in (x0, x1):
            front[y][x] = 'B'
        elif y == y0:
            front[y][x] = 'Y' if x not in (x0 + 1, x1 - 1) else 'b'
        else:
            inner = y - y0
            if inner <= 2:
                front[y][x] = 'k'
            elif inner == 3:
                front[y][x] = 'h' if x % 3 else 'k'
            elif inner == 4:
                front[y][x] = 'F' if (x + y) % 3 else 'f'
            elif inner == 5:
                front[y][x] = 'f' if x % 4 == 1 else 'F'
            elif inner == 6:
                front[y][x] = 'F' if x % 3 == 0 else ('W' if x % 3 == 1 else 'a')   # les braises couvent dans la cendre
            elif inner == 7:
                front[y][x] = 'h' if x % 5 == 2 else ('W' if x % 2 else 'a')
            else:
                front[y][x] = 'a' if x % 3 else 'g'
rows = [''.join(r) for r in front]


def save_pal(rows16, name, pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows16):
        for x, ch in enumerate(row):
            img.putpixel((x, y), pal[ch] + (255,))
    img.save(os.path.join(OUT, name + ".png"))


save_pal([r[:16] for r in rows], "forge_front_left_ash", FORGE)
save_pal([r[16:] for r in rows], "forge_front_right_ash", FORGE)
save_pal([r[:16] for r in rows], "forge_front_left_ash_soul", SOUL)
save_pal([r[16:] for r in rows], "forge_front_right_ash_soul", SOUL)

# le dessus du plateau : cendre tassée, quelques braises (h, F) ; elles restent sombres feu éteint
tray = []
for y in range(16):
    row = ""
    for x in range(16):
        r = (x * 13 + y * 7) % 17
        row += 'h' if r == 3 else ('F' if r == 11 and y % 2 else ('W' if r in (1, 8) else ('g' if r in (5, 14) else 'a')))
    tray.append(row)
save_pal(tray, "ash_tray", FORGE)
print("ok")
