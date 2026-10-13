"""Textures 16x16 originales de la Forge des Attiseurs (validées par Anger).

Usage : python3 make_forge_textures.py <dossier de sortie>
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
    'k': (22, 15, 13),     # fond du foyer
    'h': (120, 40, 18),    # braise sombre
    'F': (225, 110, 36),   # braise
    'f': (255, 186, 82),   # braise vive
    'n': (84, 52, 31),     # cuir sombre
    'l': (112, 72, 42),    # cuir
    'm': (139, 94, 56),    # cuir clair
    'S': (40, 40, 46),     # acier de l'enclume
    's': (62, 62, 70),
    'A': (98, 98, 108),    # arête de l'enclume
}


# Le feu des âmes (avec le Silt) : la même braise, en flamme froide.
SOUL = dict(PAL, h=(18, 70, 84), F=(54, 196, 206), f=(170, 244, 240))


def save(rows, name, pal=PAL):
    rows = rows + ["." * 16] * (16 - len(rows))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row, len(row))
        for x, ch in enumerate(row):
            if pal[ch]:
                img.putpixel((x, y), pal[ch] + (255,))
    img.save(os.path.join(OUT, name + '.png'))


def plates(width, height):
    """Plaques de fer rivetées, garniture de laiton en haut."""
    g = []
    for y in range(height):
        row = []
        for x in range(width):
            if y == 0:
                ch = 'Y' if x % 5 == 1 else 'b'
            elif y == 1:
                ch = 'B'
            elif x % 16 in (0, 15):
                ch = 'K'                                   # joint entre deux plaques
            elif y == height - 1:
                ch = 'D'
            else:
                ch = 'D' if (x * 7 + y * 3) % 11 == 0 else 'd'
                if y == 2:
                    ch = 'M'                                # reflet sous la garniture
            if y in (3, height - 2) and x % 16 in (2, 13):
                ch = 'R'                                    # rivets
            row.append(ch)
        g.append(row)
    return g


# ---------------------------------------------------------------- face avant (32 px de large : les deux blocs)
# Les 12 premières lignes servent (la base fait 12 px de haut).
H = 12
front = plates(32, H)
x0, x1, y0 = 9, 22, 3                                       # le foyer, centré sur les deux blocs
for y in range(y0, H):
    for x in range(x0, x1 + 1):
        if x in (x0, x1):
            front[y][x] = 'B'                               # montants de laiton
        elif y == y0:
            front[y][x] = 'Y' if x not in (x0 + 1, x1 - 1) else 'b'
        else:
            inner = y - y0
            if inner <= 2:
                front[y][x] = 'k'
            elif inner == 3:
                front[y][x] = 'h' if x % 3 else 'k'
            elif inner in (4, 5):
                front[y][x] = 'F' if (x + y) % 3 else 'f'
            elif inner == 6:
                front[y][x] = 'f' if x % 4 == 1 else 'F'
            else:
                front[y][x] = 'h' if x % 2 else 'K'           # grille du foyer
front_rows = [''.join(r) for r in front]
save([r[:16] for r in front_rows], 'forge_front_left')
save([r[16:] for r in front_rows], 'forge_front_right')
save([r[:16] for r in front_rows], 'forge_front_left_soul', SOUL)
save([r[16:] for r in front_rows], 'forge_front_right_soul', SOUL)
save([''.join(r) for r in plates(16, H)], 'forge_side')

# ---------------------------------------------------------------- dessus : plaque de fer, rebord de laiton, suie
# Une seule plaque sur les deux blocs : pas de rebord au milieu.
TOP = [
    "BbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbB",
    "bRdddDddddddDdddddddDddddddddDRb",
    "bdKKddddddddddDddddddddMdddddddb",
    "bdKKKdddDddddddddDddddddddDddddb",
    "bddKKddddddMddddddddDdddddddddDb",
    "bdddddDdddddddDdddddddddDddddddb",
    "bdddddddddddDdddddMdddddddddDddb",
    "bDdddMdddddddddddddddDdddddddddb",
    "bddddddddDdddddddDddddddddMddddb",
    "bdddDdddddddddDdddddddDddddddddb",
    "bdddddddddMddddddddddddddddDdddb",
    "bdDdddddddddddddddDddddMddddddDb",
    "bdddddddDddddddddddddddddddddddb",
    "bddMdddddddddDdddddddDdddddddddb",
    "bRddddDdddddddddddddddddDdddddRb",
    "BbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbB",
]
save([r[:16] for r in TOP], 'forge_top_left')
save([r[16:] for r in TOP], 'forge_top_right')

# ---------------------------------------------------------------- cheminée : tuyau de fer riveté, bandes de laiton
save([
    "KDdMLLMdDKDdMLMd",
    "DdMLLMdDKDdMLLMd",
    "BbYbbbbbbbbbYbbB",
    "BRbbbbbbRbbbbbRB",
    "DdMLLMdDKDdMLLMd",
    "DdMLLMddKDdMLLMd",
    "DdMLRMdDKDdMRLMd",
    "DdMLLMdDKDdMLLMd",
    "DdMLLMdDKDdMLLMd",
    "DdMLLMdDKDdMLLMd",
    "DdMLRMdDKDdMRLMd",
    "DdMLLMdDKDdMLLMd",
    "BbYbbbbbbbbbYbbB",
    "BRbbbbbbRbbbbbRB",
    "DdMLLMdDKDdMLLMd",
    "DdMLLMdDKDdMLLMd",
], 'chimney')

save([
    "BbbbbbbbbbbbbbbB",
    "bDDDDDDDDDDDDDDb",
    "bDKKKKKKKKKKKKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKkkkkkkkkkkKDb",
    "bDKKKKKKKKKKKKDb",
    "bDDDDDDDDDDDDDDb",
    "BbbbbbbbbbbbbbbB",
], 'chimney_top')

# ---------------------------------------------------------------- enclume : acier sombre, arête claire
save([
    "AAAAAAAAAAAAAAAA",
    "AsssssssssssssSA",
    "sSsSSSsSSSSsSSSs",
    "SSSsSSSSSsSSSSsS",
    "SsSSSSsSSSSSSSSS",
    "SSSSsSSSSsSSsSSS",
    "SSsSSSSSSSSSSSsS",
    "SSSSSSsSSSsSSSSS",
    "SsSSSSSSSSSSsSSS",
    "SSSSsSSSsSSSSSSS",
    "SSSSSSSSSSSSSSsS",
    "SsSSSsSSSSSsSSSS",
    "SSSSSSSSSSSSSSSS",
    "SSsSSSSSsSSSSSSS",
    "SSSSSSSSSSSSSsSS",
    "KKKKKKKKKKKKKKKK",
], 'forge_anvil')

# ---------------------------------------------------------------- soufflet : cuir en accordéon, planches de laiton
save([
    "mmmmmmmmmmmmmmmm",
    "llllllllllllllll",
    "nnnnnnnnnnnnnnnn",
    "mmmmmmmmmmmmmmmm",
    "llllllllllllllll",
    "nnnnnnnnnnnnnnnn",
    "mmmmmmmmmmmmmmmm",
    "llllllllllllllll",
    "nnnnnnnnnnnnnnnn",
    "mmmmmmmmmmmmmmmm",
    "llllllllllllllll",
    "nnnnnnnnnnnnnnnn",
    "mmmmmmmmmmmmmmmm",
    "llllllllllllllll",
    "nnnnnnnnnnnnnnnn",
    "mmmmmmmmmmmmmmmm",
], 'bellows_leather')

save([
    "YbbbbYbbbbbYbbbb",
    "bbBbbbbbBbbbbbBb",
    "bbbbbBbbbbbbbbbb",
    "BbbbbbbbbbBbbbbb",
    "bbbYbbbbbbbbbYbb",
    "bbbbbbbBbbbbbbbb",
    "bBbbbbbbbbbbBbbb",
    "bbbbbbbbbYbbbbbb",
    "bbbbBbbbbbbbbbbB",
    "YbbbbbbbbbbBbbbb",
    "bbbbbbBbbbbbbbbb",
    "bbBbbbbbbbbbbYbb",
    "bbbbbbbbbBbbbbbb",
    "bbbbbYbbbbbbbbbb",
    "bBbbbbbbbbbbBbbb",
    "bbbbbbbbbbbbbbbb",
], 'brass_trim')

print("ok")
