"""Interface de l'Établi d'Attiseur (256 x 256) : fond, la clé (4 états), le panneau du bouilleur. Dessin original.

Usage : python3 make_workbench_gui.py <dossier de sortie>
Les positions doivent correspondre à WorkbenchMenu et WorkbenchScreen.
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
FANTOME = (58, 52, 50)   # pictogramme gravé au fond d'une case vide

W, H = 176, 166
img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
px = img.load()
rnd = random.Random(7)


def rect(x, y, w, h, color):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[xx, yy] = color + (255,)


def inset(x, y, w, h, fill=CASE):
    rect(x, y, w, h, fill)
    rect(x, y, w, 1, SUIE)
    rect(x, y, 1, h, SUIE)
    rect(x, y + h - 1, w, 1, FER_CLAIR)
    rect(x + w - 1, y, 1, h, FER_CLAIR)


def ghost(x, y, art):
    for dy, row in enumerate(art):
        for dx, ch in enumerate(row):
            if ch == '#':
                px[x + dx, y + dy] = FANTOME + (255,)


# ---------------------------------------------------------------- panneau : plaques de fer, bordure de laiton (comme la forge)
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
for (x, y) in [(4, 4), (W - 5, 4), (4, H - 5), (W - 5, H - 5)]:
    px[x, y] = RIVET + (255,)
    px[x + 1, y + 1] = SUIE + (255,)
for x in range(3, W - 3):
    px[x, 70] = SUIE + (255,)
    px[x, 71] = FER_CLAIR + (255,)

# ---------------------------------------------------------------- les cases (positions de WorkbenchMenu, moins 1)
for x in (26, 50, 74):
    inset(x, 34, 18, 18)
inset(128, 30, 26, 26)                    # le résultat, plus grand (comme l'objet de la forge)
for row in range(3):
    for col in range(9):
        inset(7 + col * 18, 83 + row * 18, 18, 18)
for col in range(9):
    inset(7 + col * 18, 141, 18, 18)

# fantômes : une épée et un plastron (la pièce), un gabarit, un engrenage
ghost(28, 36, ["...........##...", "..........###...", ".........###....", "........###.....", ".......###......",
               "......###.......", "..#..###........", "...####.........", "...###..........", "..#.##..........",
               ".#....#.........", "#...............", "................", "................"])
ghost(52, 36, ["................", "...##########...", "...#...##...#...", "...#........#...", "...#..####..#...",
               "...#..#..#..#...", "...#..####..#...", "...#........#...", "...#........#...", "...##########...",
               "................"])
ghost(76, 36, ["................", ".....#..#.......", "...#.####.#.....", "....######......", "..###....###....",
               "...##....##.....", "..###....###....", "....######......", "...#.####.#.....", ".....#..#.......",
               "................"])
# une flèche gravée entre la clé et le résultat
for i in range(5):
    px[122 + i, 43] = FANTOME + (255,)
px[125, 42] = px[125, 44] = FANTOME + (255,)


# ---------------------------------------------------------------- la clé (24 x 24) : normale, survolée, éteinte, qui serre
def wrench(shaft, jaw, edge):
    k = Image.new('RGBA', (24, 24), (0, 0, 0, 0))
    d = ImageDraw.Draw(k)
    d.line([(5, 19), (15, 9)], fill=shaft, width=3)
    d.line([(4, 19), (14, 9)], fill=tuple(min(255, c + 30) for c in shaft), width=1)
    d.ellipse([12, 2, 21, 11], fill=jaw, outline=edge)
    d.polygon([(17, 3), (21, 3), (21, 7)], fill=(0, 0, 0, 0))    # l'ouverture de la mâchoire
    d.rectangle([15, 5, 18, 8], fill=(0, 0, 0, 0))
    d.ellipse([3, 17, 7, 21], fill=jaw, outline=edge)              # l'anneau du manche
    return k


normal = wrench((120, 112, 104), LAITON, LAITON_CLAIR)
hover = wrench((160, 150, 140), LAITON_CLAIR, (240, 210, 130))
off = wrench((70, 64, 60), (90, 75, 50), (70, 64, 60))
turn = normal.rotate(-30, resample=Image.NEAREST, center=(16, 7))
img.alpha_composite(normal, (176, 0))
img.alpha_composite(hover, (200, 0))
img.alpha_composite(off, (176, 24))
img.alpha_composite(turn, (200, 24))

# ---------------------------------------------------------------- le panneau du bouilleur (76 x 120, en 176, 48)
CUIVRE_SOMBRE = (150, 82, 50)
CUIVRE = (190, 108, 64)
CUIVRE_CLAIR = (226, 150, 98)
FOND_JAUGE = (22, 18, 18)
PX, PY, PW, PH = 176, 48, 76, 120
for y in range(PH):
    for x in range(PW):
        px[PX + x, PY + y] = (FER_SOMBRE if rnd.random() < 0.12 else FER) + (255,)
for x in range(PW):
    px[PX + x, PY] = CUIVRE_SOMBRE + (255,)
    px[PX + x, PY + 1] = CUIVRE_CLAIR + (255,)
    px[PX + x, PY + PH - 2] = CUIVRE + (255,)
    px[PX + x, PY + PH - 1] = CUIVRE_SOMBRE + (255,)
for y in range(PH):
    px[PX, PY + y] = CUIVRE_SOMBRE + (255,)
    px[PX + 1, PY + y] = CUIVRE_CLAIR + (255,)
    px[PX + PW - 2, PY + y] = CUIVRE + (255,)
    px[PX + PW - 1, PY + y] = CUIVRE_SOMBRE + (255,)
inset(PX + 8, PY + 25, 18, 18)                  # combustible
inset(PX + 30, PY + 31, 38, 6, FOND_JAUGE)      # le combustible déjà brûlé
inset(PX + 8, PY + 48, 12, 64, FOND_JAUGE)      # jauge d'eau
inset(PX + 45, PY + 77, 18, 18)                 # la pièce à recharger
inset(PX + 26, PY + 100, 46, 6, FOND_JAUGE)     # ses charges
# fantômes : une flamme (combustible), un plastron (la pièce)
ghost(PX + 10, PY + 27, ["................", ".......#........", "......##........", "......###.......", ".....####.......",
                         "....######......", "....###.##......", "...###...##.....", "...##.....#.....", "...##.....#.....",
                         "....##...##.....", ".....#####......", "................"])
ghost(PX + 47, PY + 79, ["................", "..###....###....", ".####....####...", ".############...", "..##########....",
                         "...########.....", "...########.....", "...########.....", "...########.....", "...########.....",
                         "....######......", "................"])
# une goutte gravée sous la jauge, et une flèche de la jauge vers la pièce
for (x, y) in [(13, 114), (12, 115), (13, 115), (14, 115), (12, 116), (13, 116), (14, 116), (13, 117)]:
    px[PX + x, PY + y] = (52, 110, 220, 255)
for i in range(18):
    px[PX + 24 + i, PY + 86] = FANTOME + (255,)
px[PX + 40, PY + 85] = px[PX + 40, PY + 87] = FANTOME + (255,)

# remplissages : l'eau (10 x 62, en 0, 168), le combustible (36 x 4, en 12, 168), les charges (vapeur blanche, 44 x 4, en 12, 174)
for y in range(62):
    t = y / 61
    water = tuple(int(c1 + (c2 - c1) * t) for c1, c2 in zip((90, 165, 245), (35, 80, 190)))
    for x in range(10):
        px[x, 168 + y] = (tuple(min(255, c + 25) for c in water) if x == 1 else water) + (255,)
for x in range(36):
    t = x / 35
    ember = tuple(int(c1 + (c2 - c1) * t) for c1, c2 in zip((150, 40, 15), (255, 200, 90)))
    for y in range(4):
        px[12 + x, 168 + y] = (tuple(min(255, c + 25) for c in ember) if y == 0 else ember) + (255,)
for x in range(44):
    for y in range(4):
        px[12 + x, 174 + y] = ((250, 250, 250) if y == 0 else (226, 226, 230)) + (255,)

img.save(os.path.join(OUT, 'workbench.png'))
print('ok')
