"""Textures 16x16 originales des modules de la Forge des Attiseurs (validées par Anger).

Usage : python3 make_module_textures.py <dossier de sortie>
"""
import os
import sys

from PIL import Image

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

PAL = {
    '.': None,
    'K': (31, 27, 28), 'D': (47, 42, 42), 'd': (61, 55, 53), 'M': (78, 71, 68), 'L': (107, 98, 92), 'R': (143, 135, 128),
    'B': (122, 90, 36), 'b': (165, 130, 47), 'Y': (207, 166, 74),
    'C': (122, 63, 37), 'c': (162, 88, 47), 'o': (196, 118, 66), 'O': (222, 150, 96),   # cuivre
    'g': (226, 216, 190), 'G': (180, 168, 140),                                          # cadran
    'k': (22, 15, 13), 'h': (150, 48, 16), 'F': (230, 112, 32), 'f': (255, 190, 80),    # lave
    'w': (235, 245, 240),                                                                # reflet de verre
    'T': (14, 34, 40), 'U': (24, 58, 66), 'V': (52, 140, 148), 'v': (95, 214, 214), 'x': (190, 250, 245),  # âme
}


def save(rows, name):
    assert len(rows) == 16, (name, len(rows))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row, len(row))
        for x, ch in enumerate(row):
            if PAL[ch]:
                img.putpixel((x, y), PAL[ch] + (255,))
    img.save(os.path.join(OUT, name + '.png'))


def copper_panel():
    """Tôle de cuivre rivetée, éclairée comme un cylindre (reflet vers la gauche du centre)."""
    shade = "CccooOocccccccCC"
    g = []
    for y in range(16):
        row = list(shade)
        if y % 4 == 2:
            row[1] = 'L'
            row[14] = 'L'                     # rivets le long des joints
        if y == 15:
            row = ['C' if ch != 'O' else 'c' for ch in row]
        g.append(row)
    return g


# ---------------------------------------------------------------- chaudière
panel = copper_panel()
save([''.join(r) for r in panel], 'boiler_side')

# Face avant du bloc du bas : un manomètre (centré dans les 10 px visibles du réservoir)
front = [r[:] for r in copper_panel()]
dial = [
    "...BBB...",
    ".BBgggBB.",
    ".BgGggGB.",
    "BggKggggB",
    "BgggKgggB",
    "BgggggggB",
    ".BgGgGgB.",
    ".BBgggBB.",
    "...BBB...",
]
for dy, line in enumerate(dial):
    for dx, ch in enumerate(line):
        if ch != '.':
            front[3 + dy][1 + dx] = ch
save([''.join(r) for r in front], 'boiler_front')

save([
    "CCCCCCCCCCCCCCCC",
    "CccccccccccccccC",
    "CccccccccccccccC",
    "CcoooooooooooocC",
    "CcoOOOOOOOOOOocC",
    "CcoOccccccccOocC",
    "CcoOccDDDDccOocC",
    "CcoOccDKKDccOocC",
    "CcoOccDKKDccOocC",
    "CcoOccDDDDccOocC",
    "CcoOccccccccOocC",
    "CcoOOOOOOOOOOocC",
    "CcoooooooooooocC",
    "CccccccccccccccC",
    "CccccccccccccccC",
    "CCCCCCCCCCCCCCCC",
], 'boiler_top')

save([
    "LLLLLLLLLLLLLLLL",
    "MRMMMMMMMRMMMMMM",
    "MMMMMMMMMMMMMMMM",
    "dddddddddddddddd",
    "LLLLLLLLLLLLLLLL",
    "MMMMMRMMMMMMMRMM",
    "MMMMMMMMMMMMMMMM",
    "dddddddddddddddd",
    "LLLLLLLLLLLLLLLL",
    "MRMMMMMMMRMMMMMM",
    "MMMMMMMMMMMMMMMM",
    "dddddddddddddddd",
    "LLLLLLLLLLLLLLLL",
    "MMMMMRMMMMMMMRMM",
    "MMMMMMMMMMMMMMMM",
    "dddddddddddddddd",
], 'iron_band')

# ---------------------------------------------------------------- réservoir de lave : fer riveté, hublot (centré dans 12 px)
save([
    "KRdddddddddRdKdd",
    "KdddddddddddDKdd",
    "KdddBBBBddddDKdd",
    "KddBbFFbBdddDKdd",
    "KdBbfFwFbBddDKdd",
    "KdBFfFFFFBddDKdd",
    "KdBFFFFhFBdddKdd",
    "KdBhFFFFhBdddKdd",
    "KddBhhFhBddddKdd",
    "KdddBBBBdddddKdd",
    "KdddddddddddDKdd",
    "KRdddddddddRdKdd",
    "KKKKKKKKKKKKKKKK",
    "KRdddddddddRdKdd",
    "KddddDdddddddKdd",
    "DDDDDDDDDDDDDDDD",
], 'lava_tank_side')

save([
    "DDDDDDDDDDDDDDDD",
    "DRddddddddddddRD",
    "DddddddddddddddD",
    "DdddMMMMMMMMdddD",
    "DddMDDDDDDDDMddD",
    "DddMDbbbbbbDMddD",
    "DddMDbYYYYbDMddD",
    "DddMDbbbbbbDMddD",
    "DddMDDDDDDDDMddD",
    "DdddMMMMMMMMdddD",
    "DddddddddddddddD",
    "DddddddddddddddD",
    "DddddddddddddddD",
    "DddddddddddddddD",
    "DRddddddddddddRD",
    "DDDDDDDDDDDDDDDD",
], 'lava_tank_top')

# ---------------------------------------------------------------- module des âmes : socle sombre veiné d'âme, cristal
save([
    "bbYbbbbbbYbbbbbb",
    "TUTTVTTUTTTUTTVT",
    "TTVvVTTTUTVTTTUT",
    "UTTVTTUTTVvVTUTT",
    "TTUTTTTTTTVTTTTU",
    "TUTTTVTUTTTTTUTT",
    "TTTTVvVTTUTTTVTT",
    "TUTTTVTTTTTUVvVT",
    "TTTUTTTUTTTTTVTT",
    "UTTTTTTTTVTTTTTU",
    "TTVTTUTTVvVTUTTT",
    "TVvVTTTTTVTTTTTT",
    "TTVTTTUTTTTTTUTT",
    "TTTTUTTTTTUTTTTU",
    "TUTTTTTVTTTTTTTT",
    "TTTTTTTTTTTTTTTT",
], 'soul_base_side')

save([
    "BbbbbbbbbbbbbbbB",
    "bTTUTTTTTTTTUTTb",
    "bTUTTVTTTTTTTUTb",
    "bTTTVvVTTTTVTTTb",
    "bUTTTVTTUTVvVTTb",
    "bTTTTTUUUUTVTTTb",
    "bTTTTUVVVVUTTTUb",
    "bTUTTUVvvVUTTTTb",
    "bTTTTUVvvVUTTUTb",
    "bTTVTUVVVVUTTTTb",
    "bTVvVTUUUUTTVTTb",
    "bTTVTTTTTTTVvVTb",
    "bTTTTUTTTTTTVTTb",
    "bTUTTTTTTUTTTTUb",
    "bTTTTTTUTTTTTTTb",
    "BbbbbbbbbbbbbbbB",
], 'soul_base_top')

save([
    "UUUVvvxxxxvvVUUU",
    "UUUVVvvxxvvVVUUU",
    "UUUVVvvxxvvVVUUU",
    "UUUVVvvxxvvVVUUU",
    "UUUVVvvxxvvVVUUU",
    "UUUVVvxxvvvVVUUU",
    "UUUVVvxxvvvVVUUU",
    "UUUVVvxxvvvVVUUU",
    "UUUVVvxxvvvVVUUU",
    "UUUVVvxxvvvVVUUU",
    "UUUVvvvxxvVVVUUU",
    "UUUVVvvxxvvVVUUU",
    "UUUVVvvxxvvVVUUU",
    "UUUVVvvxxvvVVUUU",
    "UUUVVvvxxvvVVUUU",
    "TUUUVVvvvvVVUUUT",
], 'soul_crystal')

print("ok")
