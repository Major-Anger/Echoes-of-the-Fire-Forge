"""Textures 16x16 originales d'Echoes of the Fire Forge (validées par Anger).

Usage : python3 make_textures.py <dossier de sortie>
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
    'R': (143, 135, 128),  # tête de rivet
    'B': (122, 90, 36),    # laiton sombre
    'b': (165, 130, 47),   # laiton
    'Y': (207, 166, 74),   # laiton clair
    'W': (232, 205, 130),  # reflet laiton
    'C': (122, 63, 37),    # cuivre rouillé
    'c': (162, 88, 47),    # cuivre
    'o': (196, 118, 66),   # cuivre clair
    'E': (66, 48, 30),     # gravure (laiton)
    # tons proches des débris antiques (couleurs seulement)
    '0': (51, 24, 16), '1': (65, 30, 21), '2': (74, 44, 35), '3': (93, 52, 44),
    '4': (101, 71, 64), '5': (126, 96, 89), '6': (149, 134, 126),
    'g': (110, 90, 52),    # laiton terni
    'G': (138, 116, 68),   # reflet de laiton terni
    'r': (106, 58, 34),    # rouille
}


def grid(rows, name):
    assert len(rows) == 16, (name, len(rows))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row, len(row))
        for x, ch in enumerate(row):
            col = PAL[ch]
            if col:
                img.putpixel((x, y), col + (255,))
    img.save(os.path.join(OUT, name + '.png'))
    return img


# ---------------------------------------------------------------- bloc : machine comprimée
# Côté : des couches de métal écrasées, un engrenage aplati et la section d'un tuyau.
grid([
    "4533443345543344",
    "3344554433443355",
    "2333344g2333g442",
    "1122112221112211",
    "3gGg3443343rrr33",
    "g222g443334r00r4",
    "4gGg3554432rrr33",
    "3344456644332233",
    "2233323322333223",
    "1111211121111211",
    "5544556544556554",
    "4565445444654454",
    "3344334433443344",
    "1211111211121111",
    "2332r33223323r32",
    "3223322332232233",
], "compressed_machine_side")

# Dessus : une plaque rivetée, l'engrenage vu d'en haut.
grid([
    "1222322222322221",
    "2566652255566652",
    "2511121211111252",
    "2614443344334152",
    "2514222222224162",
    "2613223gG3224152",
    "2614g20002g24162",
    "2514G20002G24152",
    "2514g20002g23162",
    "2613223gg3224152",
    "2613222222224162",
    "2513433443443162",
    "2511112111211152",
    "2566652655666552",
    "1222232222232221",
    "0111111111111110",
], "compressed_machine_top")


# ---------------------------------------------------------------- objet : engrenage
grid([
    "................",
    "......BbbB......",
    "..Bb..bYYb..bB..",
    "..bYbbYYYbbbbB..",
    "...bYYbbbbbbB...",
    ".BbYbbBKKBbbbBB.",
    ".bYYbBK..KBbbbB.",
    "..YbbK....KbbB..",
    "..bbbK....KbbB..",
    ".bbbbBK..KBbbBB.",
    ".BbbbbBKKBbbBBB.",
    "...BbbbbbbbBB...",
    "..BbbbbbbbbBBB..",
    "..BB..BbbB..BB..",
    "......BBBB......",
    "................",
], "machine_gear")

# ---------------------------------------------------------------- objet : soupape
# Un volant de manœuvre sur sa tige, vissé sur un bout de tuyau.
grid([
    "................",
    "......bYYb......",
    "....bY.bb.Bb....",
    "....Y..bb..B....",
    "....YbbWWbbB....",
    "....b..bb..B....",
    "....Bb.bb.BB....",
    "......BBBB......",
    ".......Ld.......",
    ".......Ld.......",
    ".......Ld.......",
    "......dLLd......",
    "..oc..cooc..cC..",
    "..ooccoooocccC..",
    "..CcCCCCCCCCcC..",
    "..CC........CC..",
], "machine_valve")

# ---------------------------------------------------------------- objet : plaque gravée
# Une plaque de laiton rivetée, un coin tordu, des marques gravées (originales, pas les runes du Silt).
grid([
    "................",
    "................",
    "..BBBBBBBBBBB...",
    "..BRbYYbbbbbBB..",
    "..BYbEbbEbEbbB..",
    "..BbEbEbbEbEbB..",
    "..BbEbEbEbEbbB..",
    "..BbEEEbbEbEbB..",
    "..BbbEbbbbEbbB..",
    "..BbEbEbEEEEbB..",
    "..BbbEbbbbEbbB..",
    "..BRbbbbbbbbRB..",
    "..BBBBBBBBBBBB..",
    "................",
    "................",
    "................",
], "engraved_plate")

# ---------------------------------------------------------------- objet : plan
# Le papier n'aurait pas survécu : un plan est une fine feuille de laiton gravée, enroulée.
grid([
    "................",
    "................",
    ".BB..........BB.",
    "BYbB........BYbB",
    "BbbWYYYYYYYYBbbB",
    "BYbYbbEEEbbbBYbB",
    "BbbYbEbbbEbbBbbB",
    "BYbYEEbEbEEEBYbB",
    "BbbYbEbbbEbbBbbB",
    "BYbYbbEEEbbbBYbB",
    "BbbYbbbEbbbbBbbB",
    "BYbYbEbEbEbbBYbB",
    "BbbBBBBBBBBBBbbB",
    ".BB..........BB.",
    "................",
    "................",
], "attiseur_plan")

print("ok")
