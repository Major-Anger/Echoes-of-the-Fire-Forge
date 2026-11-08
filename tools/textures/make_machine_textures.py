"""Textures 16x16 de la Grande Machine (plan, section 15) : la bouche de vapeur (dessus, côté). À présenter à Anger
sur une planche avant de les garder (règles 4 et 5).

Usage : python3 tools/textures/make_machine_textures.py <dossier de sortie>
"""
import os
import sys

from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

PAL = {
    '.': None,
    'K': (31, 27, 28),     # suie
    'k': (22, 19, 20),     # le noir des fentes
    'D': (47, 42, 42),     # fer sombre
    'd': (61, 55, 53),
    'M': (78, 71, 68),     # fer
    'L': (107, 98, 92),    # fer clair
    'R': (143, 135, 128),  # tête de rivet
    'B': (122, 90, 36),    # laiton sombre
    'b': (165, 130, 47),   # laiton
    'Y': (207, 166, 74),   # laiton clair
    'o': (150, 74, 34),    # la braise, au fond des fentes
}


def grid(rows, name):
    assert len(rows) == 16, (name, len(rows))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row, len(row))
        for x, ch in enumerate(row):
            if PAL[ch]:
                img.putpixel((x, y), PAL[ch] + (255,))
    img.save(os.path.join(OUT, name + '.png'))
    return img


# la bouche de vapeur, vue de dessus : un cadre de laiton riveté, une grille de fer, et tout au fond, la braise
grid([
    "BbbbbbbbbbbbbbbB",
    "bRYbbbbbbbbbbYRb",
    "bYDDDDDDDDDDDDYb",
    "bbDMMLMMMMLMMDbb",
    "bbDkkkkkkkkkkDbb",
    "bbDMMMMLMMMMMDbb",
    "bbDkkokkkkokkDbb",
    "bbDMMMMMMMLMMDbb",
    "bbDkkkkkokkkkDbb",
    "bbDMLMMMMMMMMDbb",
    "bbDkokkkkkkokDbb",
    "bbDMMMMMLMMMMDbb",
    "bbDkkkkkkkkkkDbb",
    "bYDDDDDDDDDDDDYb",
    "bRYbbbbbbbbbbYRb",
    "BbbbbbbbbbbbbbbB",
], "steam_vent_top")

# la bouche de vapeur, de côté : du fer noirci, une bande de laiton en haut
grid([
    "BbbbbbbbbbbbbbbB",
    "bYYYYYYYYYYYYYYb",
    "BBBBBBBBBBBBBBBB",
    "DdDDKDDdDDDKDDdD",
    "dDDdDDKDDdDDDKDD",
    "DDKDDdDDDKDDdDDD",
    "DdDDDDDdDDKDDDdD",
    "KDDdDKDDDdDDDDKD",
    "DDdDDDDKDDDdKDDD",
    "DKDDdDDDDKDDDDdD",
    "DDDKDDdDKDDdDKDD",
    "dDDDDKDDDDDDDDDK",
    "DDdDDDDdKDDKDdDD",
    "DKDDKDDDDDdDDDDD",
    "DDDdDDKDDDDDKDdD",
    "KKKKKKKKKKKKKKKK",
], "steam_vent_side")
print("ok")
