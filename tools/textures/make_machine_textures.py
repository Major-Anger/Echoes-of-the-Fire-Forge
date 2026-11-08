"""Textures 16x16 de la Grande Machine (plan, section 15) : la bouche de vapeur (15b, validée) ; les vannes du régulateur
et la Braise du cœur (15c). À présenter à Anger sur une planche avant de les garder (règles 4 et 5).

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
    'O': (232, 118, 42),   # braise vive
    'F': (255, 196, 92),   # le cœur de la braise
    'W': (255, 236, 170),  # blanc de chaleur
    'E': (66, 48, 30),     # gravure
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
# ---------------------------------------------------------------- 15c : les vannes du régulateur
# la roue, vue de dessus : une couronne de laiton, quatre rayons, le moyeu ; tournée, les rayons sont en croix de biais
grid([
    "................",
    ".....bbbbbb.....",
    "...bbYYYYYYbb...",
    "..bYYB.bb.BYYb..",
    "..bYB..bb..BYb..",
    ".bYB...bb...BYb.",
    ".bY....bb....Yb.",
    ".bYbbbbRRbbbbYb.",
    ".bYbbbbRRbbbbYb.",
    ".bY....bb....Yb.",
    ".bYB...bb...BYb.",
    "..bYB..bb..BYb..",
    "..bYYB.bb.BYYb..",
    "...bbYYYYYYbb...",
    ".....bbbbbb.....",
    "................",
], "valve_wheel")
grid([
    "................",
    ".....bbbbbb.....",
    "...bbYYYYYYbb...",
    "..bYbB....BbYb..",
    "..bYBbb..bbBYb..",
    ".bYB..bbbb..BYb.",
    ".bY....bb....Yb.",
    ".bY...bRRb...Yb.",
    ".bY...bRRb...Yb.",
    ".bY....bb....Yb.",
    ".bYB..bbbb..BYb.",
    "..bYBbb..bbBYb..",
    "..bYbB....BbYb..",
    "...bbYYYYYYbb...",
    ".....bbbbbb.....",
    "................",
], "valve_wheel_turned")
# le socle, de côté : du fer sombre, une plaque de laiton gravée (− : baisser, + : pousser) ; rien d'autre ne les distingue
for name, mark in (("valve_lower_side", ["......", "......", ".EEEE.", "......", "......"]),
                   ("valve_raise_side", ["......", "..EE..", ".EEEE.", "..EE..", "......"])):
    rows = [
        "BbbbbbbbbbbbbbbB",
        "DDDDDDDDDDDDDDDD",
        "DdDDKDDdDDDKDDdD",
        "dDDdDDKDDdDDDKDD",
        "DDKDDdDDDKDDdDDD",
        "DDDBbbbbbbbbBDDD",
    ]
    for line in mark:
        rows.append("DDDb" + line.replace(".", "Y").replace("E", "E") + "YYbDDD")
    rows += [
        "DDDBbbbbbbbbBDDD",
        "DdDDDDDdDDKDDDdD",
        "KDDdDKDDDdDDDDKD",
        "DDdDDDDKDDDdKDDD",
        "KKKKKKKKKKKKKKKK",
    ]
    grid(rows, name)

# ---------------------------------------------------------------- 15c : la Braise du cœur (objet)
# un éclat de braise vive, pris dans une petite cage de laiton
grid([
    "................",
    "......bbbb......",
    ".....b....b.....",
    "....bBOOOOBb....",
    "...b.OOFFOO.b...",
    "...bOOFWWFOOb...",
    "..bBOFWWWWFOBb..",
    "..b.OFWWWWFO.b..",
    "..bBOFFWWFFOBb..",
    "...bOOFFFFOOb...",
    "...b.oOOOOo.b...",
    "....bBooooBb....",
    ".....b....b.....",
    "......bbbb......",
    "................",
    "................",
], "heart_ember")

print("ok")
