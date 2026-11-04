"""Texture 16x16 du carnet du maître de forge (plan, 14c) : des feuilles de laiton tenues par des anneaux de fer,
une couverture de fer sombre aux coins de laiton, un marteau-pilon en laiton (la tête et son gros boulon). À présenter à Anger avant de la garder.

Usage : python3 tools/textures/make_forgemaster_notebook.py <dossier de sortie>
"""
import os
import sys

from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

PAL = {
    '.': None,
    'K': (31, 27, 28),     # suie
    'D': (47, 42, 42),     # fer sombre
    'd': (61, 55, 53),
    'M': (78, 71, 68),     # fer
    'L': (107, 98, 92),    # fer clair
    'R': (143, 135, 128),  # anneau
    'B': (122, 90, 36),    # laiton sombre
    'b': (165, 130, 47),   # laiton
    'Y': (207, 166, 74),   # laiton clair
    'W': (232, 205, 130),  # reflet
    'E': (66, 48, 30),     # gravure
    'o': (232, 118, 42),   # braise
}

ROWS = [
    "................",
    "...KKKKKKKKKKK..",
    "..RKBDddddddDBK.",
    "..RKddddRRddddKW",
    "..KKddYYYYYYddKY",
    "..RKddYbbbbYddKb",
    "..KKddBBBBBBddKY",
    "..RKddddLLddddKb",
    "..KKddddLLddddKY",
    "..RKddddLLddddKb",
    "..KKddddMMddddKY",
    "..RKDddddddddDKb",
    "..KKBDDDDDDDDBKb",
    "...KKKKKKKKKKKKB",
    "....BBBBBBBBBBB.",
    "................",
]
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y, row in enumerate(ROWS):
    assert len(row) == 16, (y, row, len(row))
    for x, ch in enumerate(row):
        if PAL[ch]:
            img.putpixel((x, y), PAL[ch] + (255,))
img.save(os.path.join(OUT, "forge_master_notebook.png"))
print("ok")
