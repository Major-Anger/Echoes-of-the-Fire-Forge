"""Textures 16x16 originales de l'Établi d'Attiseur, du bouilleur et des gabarits de mécanisme.

Usage : python3 make_workbench_textures.py <dossier de sortie>
(en attente de validation d'Anger : planche d'aperçu d'abord, règles 4 et 5)
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
    'E': (74, 52, 22),     # gravure dans le laiton
    'c': (150, 82, 50),    # cuivre sombre
    'C': (190, 108, 64),   # cuivre
    'H': (226, 150, 98),   # cuivre clair
    'k': (22, 15, 13),     # fond du foyer
    'h': (120, 40, 18),    # braise sombre
    'F': (225, 110, 36),   # braise
    'f': (255, 186, 82),   # braise vive
    'S': (40, 40, 46),     # acier
    's': (62, 62, 70),
    'A': (98, 98, 108),    # arête d'acier
    'w': (236, 226, 200),  # cadran
    'W': (232, 205, 130),  # reflet laiton
    'n': (84, 52, 31),     # bois sombre (manches)
    'l': (112, 72, 42),
}

# Gabarit rare : acier bleui par la trempe (le métal chauffé puis plongé dans l'eau)
RARE = dict(PAL, B=(52, 44, 92), b=(84, 76, 140), Y=(150, 132, 200), E=(28, 22, 52))


def img_from(rows, pal=PAL, w=16, h=16):
    rows = rows + ["." * w] * (h - len(rows))
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == w, (y, row, len(row))
        for x, ch in enumerate(row):
            if pal[ch]:
                img.putpixel((x, y), pal[ch] + (255,))
    return img


def save(img, name):
    img.save(os.path.join(OUT, name + '.png'))


def grid(w, h, fill='.'):
    return [[fill] * w for _ in range(h)]


def rows_of(g):
    return [''.join(r) for r in g]


def plates(g, x0, y0, w, h, trim=True):
    """Plaques de fer rivetées (comme la forge), garniture de laiton en haut."""
    for y in range(h):
        for x in range(w):
            if trim and y == 0:
                ch = 'Y' if x % 5 == 1 else 'b'
            elif trim and y == 1:
                ch = 'B'
            elif x % 16 in (0, 15):
                ch = 'K'
            elif y == h - 1:
                ch = 'D'
            else:
                ch = 'D' if (x * 7 + y * 3) % 11 == 0 else 'd'
                if trim and y == 2:
                    ch = 'M'
            if y in (3, h - 2) and x % 16 in (2, 13):
                ch = 'R'
            g[y0 + y][x0 + x] = ch


def stamp(g, x0, y0, art):
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch != '.':
                g[y0 + y][x0 + x] = ch


# ====================================================================== l'établi (2 blocs)
# Face avant, 32 px : à gauche deux tiroirs, à droite le tableau à outils (clé et marteau).
front = grid(32, 16)
plates(front, 0, 0, 32, 16)
DRAWER = ["MMMMMMMMMMMM",
          "MDDDDDDDDDDM",
          "MD........DM",
          "MD..bYYb..DM",
          "MD........DM",
          "MMMMMMMMMMMM"]
for dy in (3, 9):
    stamp(front, 2, dy, [r.replace('.', 'd') for r in DRAWER])
BOARD = ["SSSSSSSSSSSS",
         "S.A.A.AAAA.S",
         "S.AAA.AAAA.S",
         "S..A....l..S",
         "S..A....l..S",
         "S..A....l..S",
         "S..A....l..S",
         "S.AAA...l..S",
         "S.A.A......S",
         "SSSSSSSSSSSS"]
stamp(front, 18, 4, [r.replace('.', 's') for r in BOARD])
save(img_from(rows_of(front), w=32), 'workbench_front_full')
fi = img_from(rows_of(front), w=32)
save(fi.crop((0, 0, 16, 16)), 'workbench_front_left')
save(fi.crop((16, 0, 32, 16)), 'workbench_front_right')

# Dessus, 32 px : plan de travail cerclé de laiton, un étau à gauche, des rouages posés à droite.
top = grid(32, 16, 'd')
for y in range(16):
    for x in range(32):
        edge = y in (0, 15) or x in (0, 31)
        top[y][x] = 'b' if edge else ('D' if (x * 5 + y * 7) % 13 == 0 else 'M' if (x + y) % 9 == 0 else 'd')
        if edge and (x + y) % 6 == 0:
            top[y][x] = 'Y'
VISE = ["..AAAA..",
        "..SssS..",
        "..S..S..",
        "..SssS..",
        "AAAAAAAA",
        "...nn...",
        "...nn..."]
stamp(top, 4, 4, VISE)
GEAR = [".Y.Y.",
        "YbbbY",
        ".bEb.",
        "YbbbY",
        ".Y.Y."]
stamp(top, 20, 3, GEAR)
stamp(top, 25, 8, GEAR)
stamp(top, 18, 10, ["LLLLLL", "RLLLLR"])   # une plaque gravée en cours
ti = img_from(rows_of(top), w=32)
save(ti, 'workbench_top_full')
save(ti.crop((0, 0, 16, 16)), 'workbench_top_left')
save(ti.crop((16, 0, 32, 16)), 'workbench_top_right')

# Côté : plaques et petit manomètre.
side = grid(16, 16)
plates(side, 0, 0, 16, 16)
stamp(side, 5, 6, [".bbbb.", "bwwwwb", "bwwEwb", "bwEwwb", "bwwwwb", ".bbbb."])
save(img_from(rows_of(side)), 'workbench_side')

# ====================================================================== le bouilleur (module de l'établi)
# En bas un foyer de fer à grille, en haut un ballon de cuivre avec son manomètre.
bf = grid(16, 16)
for y in range(16):
    for x in range(16):
        if y < 8:
            bf[y][x] = 'H' if x in (3, 4) else 'C' if 2 <= x <= 12 else 'c'
            if x in (0, 15):
                bf[y][x] = 'c'
        else:
            bf[y][x] = 'D' if (x * 7 + y * 3) % 11 == 0 else 'd'
            if x in (0, 15):
                bf[y][x] = 'K'
bf[0] = list('BbbbYbbbbYbbbbbB')
bf[7] = list('SSSSSSSSSSSSSSSS')
stamp(bf, 9, 1, [".bbbb.", "bwwwwb", "bwwEwb", "bwEwwb", ".bbbb."])
stamp(bf, 3, 9, ["SSSSSSSSSS",
                 "SkhkhkhkhS",
                 "SFfFfFfFhS",
                 "SkhkhkhkhS",
                 "SSSSSSSSSS"])
bf[14] = list('KRddddddddddddRK')
bf[15] = list('DDDDDDDDDDDDDDDD')
save(img_from(rows_of(bf)), 'steam_boiler_front')
# éteint : la grille sans braise
save(img_from([r.replace('F', 'h').replace('f', 'h') for r in rows_of(bf)]), 'steam_boiler_front_off')

bs = grid(16, 16)
for y in range(16):
    for x in range(16):
        bs[y][x] = ('H' if x in (4, 5) else 'C' if 2 <= x <= 13 else 'c') if y < 8 else ('D' if (x * 7 + y * 3) % 11 == 0 else 'd')
bs[0] = list('BbbbYbbbbYbbbbbB')
bs[3] = list('SSSSSSSSSSSSSSSS')
bs[7] = list('SSSSSSSSSSSSSSSS')
bs[14] = list('KRddddddddddddRK')
bs[15] = list('DDDDDDDDDDDDDDDD')
save(img_from(rows_of(bs)), 'steam_boiler_side')

bt = grid(16, 16, 'C')
for y in range(16):
    for x in range(16):
        if y in (0, 15) or x in (0, 15):
            bt[y][x] = 'c'
stamp(bt, 5, 5, [".SSSS.", "SAAAAS", "SAYYAS", "SAYYAS", "SAAAAS", ".SSSS."])  # le sifflet
save(img_from(rows_of(bt)), 'steam_boiler_top')

# ====================================================================== les gabarits (items)
def template(art, pal):
    g = grid(16, 16)
    for y in range(1, 15):
        for x in range(2, 14):
            corner = (y in (1, 14)) and (x in (2, 13))
            if corner:
                continue
            rim = y in (1, 14) or x in (2, 13)
            g[y][x] = 'B' if rim else 'b'
            if not rim and (x == 3 or y == 2):
                g[y][x] = 'Y'                      # reflet en haut à gauche
    g[2][7] = g[2][8] = 'K'                        # le trou pour l'accrocher
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch == '#':
                g[4 + y][4 + x] = 'E'
    return img_from(rows_of(g), pal)


ART = {
    'longue_vue': ["......##", ".....###", "....###.", "...###..", "..###...", ".###....", "#.#.....", ".#......", "........", "........"],
    'sonde': [".######.", "#......#", "..####..", ".#....#.", "...##...", "..#..#..", "...##...", "........", "........", "........"],
    'lampe': ["......##", "##..##..", "###.....", "###.####", "###.....", "##..##..", "......##", "........", "........", "........"],
    'sac': ["..####..", ".#....#.", ".######.", ".#.##.#.", ".#....#.", ".#.##.#.", ".######.", "........", "........", "........"],
    'planeur': ["........", "#......#", "##....##", "###..###", "########", ".##..##.", "..#..#..", "........", "........", "........"],
    'jet': ["#..#..#.", ".#.#.#..", "..###...", "###.###.", "..###...", ".#.#.#..", "#..#..#.", "........", "........", "........"],
    'poches': ["########", "#......#", "#.####.#", "#.#..#.#", "#.####.#", "#......#", ".######.", "........", "........", "........"],
    'pistons': ["..####..", "..#..#..", "..####..", "...##...", "...##...", ".######.", ".######.", "........", "........", "........"],
    'ancre': ["...##...", "..#..#..", "...##...", ".######.", "...##...", "#..##..#", ".#.##.#.", "..####..", "........", "........"],
    'patins': ["#.......", "#.......", "#.......", "########", "########", ".#....#.", "###..###", ".#....#.", "........", "........"],
    'ressorts': [".######.", "...##...", ".##.....", "...##...", ".....##.", "...##...", ".######.", "........", "........", "........"],
    'crampons': ["####....", "####....", "#####...", "########", "########", "#.#.#.#.", "#.#.#.#.", "........", "........", "........"],
    'piston': ["...#....", "....#...", ".....#..", "########", ".....#..", "....#...", "...#....", "........", "........", "........"],
    'sillon': ["........", ".....#..", "..#..##.", ".###.###", "########", "########", "........", "........", "........", "........"],
    'frappe_sol': ["...##...", "...##...", "...##...", "#..##..#", ".#.##.#.", "########", "#.#..#.#", "........", "........", "........"],
    'treuil': ["..##....", ".#..#...", "..##....", "....##..", "...#..#.", "....##.#", ".......#", ".....##.", "........", "........"],
    'trident_de_trempe': ["#..#..#.", "#..#..#.", ".######.", "...#....", "...#..#.", "...#.###", "...#.###", "......#.", "........", "........"],
}
RARES = {'sonde', 'planeur', 'pistons', 'ressorts'}
for name, art in ART.items():
    save(template(art, RARE if name in RARES else PAL), 'template_' + name)

# ====================================================================== le Plan de l'Établi
# La même feuille de laiton enroulée que les autres plans, gravée d'un établi.
plan = ["................",
        "................",
        ".BB..........BB.",
        "BYbB........BYbB",
        "BbbWYYYYYYYYBbbB",
        "BYbYbbbbbEbbBYbB",
        "BbbYbbbbEbEbBbbB",
        "BYbYEEEEEEEEBYbB",
        "BbbYEbEbbbbEBbbB",
        "BYbYEEEbbbbEBYbB",
        "BbbYEbbbbbbEBbbB",
        "BYbYEbbbbbbEBYbB",
        "BbbBBBBBBBBBBbbB",
        ".BB..........BB.",
        "................",
        "................"]
save(img_from(plan), 'workbench_plan')
print('ok')
