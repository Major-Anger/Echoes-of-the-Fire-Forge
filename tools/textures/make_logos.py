"""Logos de la série Echoes (validés par Anger) : Echoes Core et Echoes of the Fire Forge.
- avec le texte (la liste des mods de NeoForge, `logoFile`) : dessinés en pixels (128 x 64), agrandis 4 fois (512 x 256) ;
- carrés, sans texte (les pages CurseForge et Modrinth) : 64 x 64, agrandis 8 fois (512 x 512).
Police maison (5 x 7), formes originales (règle 4).

Usage : python3 tools/textures/make_logos.py art/logos   (puis copier le logo de la Forge dans fireforge/src/main/resources)
"""
import math
import os
import sys

from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

FONT = {
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "C": [".####", "#....", "#....", "#....", "#....", "#....", ".####"],
    "H": ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "O": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "F": ["#####", "#....", "#....", "####.", "#....", "#....", "#...."],
    "I": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "G": [".####", "#....", "#....", "#..##", "#...#", "#...#", ".###."],
    " ": [".....", ".....", ".....", ".....", ".....", ".....", "....."],
}


def text(img, x, y, s, col, scale=1, shadow=None):
    for ch in s:
        g = FONT[ch]
        for gy, row in enumerate(g):
            for gx, c in enumerate(row):
                if c != "#":
                    continue
                for dx in range(scale):
                    for dy in range(scale):
                        if shadow:
                            put(img, x + gx * scale + dx + 1, y + gy * scale + dy + 1, shadow)
                for dx in range(scale):
                    for dy in range(scale):
                        put(img, x + gx * scale + dx, y + gy * scale + dy, col)
        x += (5 + 1) * scale
    return x


def width(s, scale=1):
    return len(s) * 6 * scale - scale


def put(img, x, y, col):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), col + (255,) if len(col) == 3 else col)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def background(top, bottom, w=128):
    img = Image.new("RGBA", (w, 64))
    for y in range(64):
        c = mix(top, bottom, y / 63)
        for x in range(w):
            img.putpixel((x, y), c + (255,))
    return img


# ------------------------------------------------------------------ Echoes Core : des échos, autour d'une âme
def core(square=False):
    img = background((14, 18, 40), (6, 8, 20), 64 if square else 128)
    ox = 3 if square else 0                                   # carré : l'emblème au milieu
    cx, cy = 28.5 + ox, 31.5
    teal, deep, pale = (0, 175, 190), (16, 31, 101), (190, 245, 250)
    for y in range(64):
        for x in range(64):
            r = math.hypot(x - cx, y - cy)
            for k, rad in enumerate((7, 11.5, 16, 20.5, 25)):
                if abs(r - rad) < 0.75:
                    fade = 1 - k / 5
                    put(img, x, y, mix(deep, teal, fade))
    # l'âme : une petite flamme au centre
    flame = ["...#...", "..###..", "..#W#..", ".##W##.", ".#WWW#.", ".##W##.", "..###.."]
    for fy, row in enumerate(flame):
        for fx, c in enumerate(row):
            if c == "#":
                put(img, 25 + ox + fx, 28 + fy, teal)
            elif c == "W":
                put(img, 25 + ox + fx, 28 + fy, pale)
    if square:
        return img
    x0 = 56
    text(img, x0, 17, "ECHOES", teal, 2, shadow=deep)
    text(img, x0 + (width("ECHOES", 2) - width("CORE", 1)) // 2, 37, "CORE", (170, 180, 200))
    return img


# ------------------------------------------------------------------ Echoes of the Fire Forge : la forge des Attiseurs
def forge(square=False):
    img = background((38, 22, 18), (12, 8, 8), 64 if square else 128)
    brass, dbrass, light = (165, 130, 47), (122, 90, 36), (207, 166, 74)
    iron, diron = (78, 71, 68), (40, 36, 34)
    cx, cy = 31.5, 30.5
    for y in range(64):
        for x in range(64):
            r = math.hypot(x - cx, y - cy)
            a = math.atan2(y - cy, x - cx)
            tooth = ((a + math.pi) / (2 * math.pi) * 12) % 1 < 0.45       # douze dents carrées
            if 17 <= r < 20 or (20 <= r < 23 and tooth):
                put(img, x, y, light if (x + y) % 7 == 0 else brass if r < 21 else dbrass)
            elif r < 17:
                put(img, x, y, mix((60, 26, 14), (22, 12, 10), r / 17))
    # la flamme
    for y in range(14, 40):
        h = (40 - y) / 26
        half = int(7 * math.sin(h * math.pi) * (1 - h * 0.3)) + 1
        for x in range(int(cx) - half, int(cx) + half + 1):
            d = abs(x - cx) / max(1, half)
            col = (255, 236, 170) if d < 0.3 and h < 0.7 else (255, 196, 92) if d < 0.6 else (232, 118, 42)
            if y < 18 and d > 0.5:
                continue
            put(img, x, y, col)
    # l'enclume
    anvil = ["..##############..", "...############...", ".....########.....", "......######......", ".....########.....", "....##########...."]
    for ay, row in enumerate(anvil):
        for ax, c in enumerate(row):
            if c == "#":
                put(img, 23 + ax, 38 + ay, iron if ay < 2 else diron)
    # la vapeur
    for px, py, r in ((44, 10, 3), (49, 6, 2), (40, 6, 2)):
        for y in range(py - r, py + r + 1):
            for x in range(px - r, px + r + 1):
                if math.hypot(x - px, y - py) <= r:
                    put(img, x, y, (200, 196, 190))
    if square:
        return img
    x0 = 64
    text(img, x0 + (width("FIRE", 2) - width("ECHOES", 1)) // 2 + 6, 9, "ECHOES", (0, 175, 190))
    text(img, x0 + 6, 20, "FIRE", light, 2, shadow=(40, 20, 10))
    text(img, x0, 38, "FORGE", light, 2, shadow=(40, 20, 10))
    return img


for name, img in (("echoes_core_logo", core()), ("echoes_fireforge_logo", forge())):
    img.resize((512, 256), Image.NEAREST).save(os.path.join(OUT, name + ".png"))
# carrés, sans texte (pages CurseForge et Modrinth) : 512 x 512
for name, img in (("echoes_core_icon", core(True)), ("echoes_fireforge_icon", forge(True))):
    img.resize((512, 512), Image.NEAREST).save(os.path.join(OUT, name + ".png"))
print("ok")
