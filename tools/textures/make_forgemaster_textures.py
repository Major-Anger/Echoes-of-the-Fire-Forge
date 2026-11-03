"""Le maître de forge (plan, section 14) : son modèle en boîtes (repère du rendu : y vers le haut, l'avant vers +z,
en pixels, 16 par bloc) et sa texture 128 x 128, plus la couche qui luit (braises, yeux). Aperçu avant validation.

Usage : python3 tools/textures/make_forgemaster_textures.py <dossier de sortie>
Les parties : (nom, parent, pivot, décalage, taille, uv, rotation, gonflement[, miroir]) ; uv en « box uv ».
"""
import os
import random
import sys

from PIL import Image

OUT = sys.argv[1]
TEX = 128
rnd = random.Random(14)

# ------------------------------------------------------------------ le modèle (4 blocs de haut)
# (nom, parent, pivot, décalage, taille, matière, rotation, gonflement, miroir)
SPEC = [
    ("body", None, (0, 28, 0), (-1.5, 0, -1.5), (3, 8, 3), "bone", (0, 0, 0), 0),      # la colonne
    ("chest", "body", (0, 0, 0), (-8, 8, -3.5), (16, 14, 7), "ribs", (0, 0, 0), 0),     # la cage thoracique
    ("core", "body", (0, 0, 0), (-4, 10, -2.5), (8, 8, 4), "core", (0, 0, 0), 0),
    ("pelvis", "body", (0, 0, 0), (-6, -3, -3), (12, 5, 6), "bone", (0, 0, 0), 0),
    # au choix (planche) : rien, une ceinture de forge, un tablier court
    ("belt", "body", (0, 0, 0), (-6.5, 0, -3.5), (13, 2, 7), "leather", (0, 0, 0), 0),
    ("buckle", "body", (0, 0, 0), (-1.5, -0.5, 3.5), (3, 3, 1), "bolt", (0, 0, 0), 0),
    ("pouch", "body", (0, 0, 0), (3, -4, 3.5), (3, 4, 2), "leather", (0, 0, 0), 0),
    ("apron", "body", (0, 0, 0), (-5, -11, 3), (10, 12, 1), "apron", (0, 0, 0), 0),
    ("head", "body", (0, 22, 0), (-6, 0, -6), (12, 12, 12), "skull", (0, 0, 0), 0),
    ("goggles", "head", (0, 0, 0), (-6, 7, -6), (12, 3, 12), "goggles", (0, 0, 0), 0.45),
    ("pauldron_r", "body", (0, 0, 0), (-13, 18, -4.5), (7, 6, 9), "brass", (0, 0, 0), 0),
    ("pauldron_l", "body", (0, 0, 0), (6, 18, -4.5), (7, 6, 9), "brass", (0, 0, 0), 0),
    ("arm_r", "body", (-11, 20, 0), (-1.5, -28, -1.5), (3, 28, 3), "bone", (-6, 0, -4), 0),
    ("arm_l", "body", (11, 20, 0), (-1.5, -28, -1.5), (3, 28, 3), "bone", (4, 0, 5), 0, True),
    ("leg_r", None, (-4, 26, 0), (-2, -26, -2), (4, 26, 4), "bone", (0, 0, 0), 0),
    ("leg_l", None, (4, 26, 0), (-2, -26, -2), (4, 26, 4), "bone", (0, 0, 0), 0, True),
    # le marteau-pilon (dessin d'Anger), dans la main droite, la tête au sol
    ("hammer", "arm_r", (0, -27, 0), (-1.5, -8, -1.5), (3, 30, 3), "grip", (6, 0, 4), 0),
    ("counterweight", "hammer", (0, 0, 0), (-2.5, 22, -2.5), (5, 5, 5), "iron", (0, 0, 0), 0),
    ("hammer_head", "hammer", (0, 0, 0), (-9, -19, -6), (18, 12, 12), "iron", (0, 0, 0), 0),
    ("strike_plate", "hammer", (0, 0, 0), (-11, -20, -7), (2, 14, 14), "plate", (0, 0, 0), 0),
    ("nozzle", "hammer", (0, 0, 0), (9, -15.5, -2.5), (5, 5, 5), "copper", (0, 0, 0), 0),
    ("gear", "hammer", (0, 0, 0), (-5, -18, 6), (10, 10, 1), "gear", (0, 0, 0), 0),
    ("bolt", "hammer", (0, 0, 0), (-3, -7, -3), (6, 3, 6), "bolt", (0, 0, 0), 0),
]


def footprint(size):
    w, h, d = (int(round(v)) for v in size)
    return 2 * (w + d), d + h


# un rangement simple des uv, en étagères
uvs, x, y, row = {}, 0, 0, 0
for name, _, _, _, size, *_ in sorted(SPEC, key=lambda s: -footprint(s[4])[1]):
    fw, fh = footprint(size)
    if x + fw > TEX:
        x, y, row = 0, y + row, 0
    uvs[name] = (x, y)
    x, row = x + fw, max(row, fh)
assert y + row <= TEX, "la texture déborde"

OPTIONAL = {"belt", "buckle", "pouch", "apron"}
VARIANTS = {"rien": set(), "ceinture": {"belt", "buckle", "pouch"}, "tablier court": {"apron"}}

ALL_PARTS = []
for s in SPEC:
    name, parent, pivot, off, size, mat, r, inf = s[:8]
    part = (name, parent, pivot, off, size, uvs[name], r, inf)
    if len(s) > 8:
        part += (s[8],)
    ALL_PARTS.append(part)


def parts(variant="ceinture"):
    keep = VARIANTS[variant]
    return [p for p in ALL_PARTS if p[0] not in OPTIONAL or p[0] in keep]


PARTS = parts()

# ------------------------------------------------------------------ la texture
tex = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
px, gx = tex.load(), glow.load()


def jitter(c, n=8):
    k = rnd.randint(-n, n)
    return tuple(max(0, min(255, v + k)) for v in c[:3]) + (255,)


def faces(uv, size):
    u, v = uv
    w, h, d = (int(round(t)) for t in size)
    return {"front": (u + d, v + d, w, h), "back": (u + 2 * d + w, v + d, w, h), "right": (u, v + d, d, h),
            "left": (u + d + w, v + d, d, h), "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d)}


BONE, BONE_DARK, EMBER = (54, 50, 48), (26, 23, 22), (255, 120, 30)
BRASS, BRASS_DARK, PATINA = (150, 112, 60), (92, 66, 36), (88, 128, 104)
IRON, IRON_DARK, STEEL = (66, 64, 68), (36, 35, 38), (128, 126, 130)
LEATHER, LEATHER_DARK = (64, 40, 28), (38, 24, 17)


def paint(mat, face, rect):
    fx, fy, w, h = rect
    for i in range(w):
        for j in range(h):
            X, Y = fx + i, fy + j
            edge = i in (0, w - 1) or j in (0, h - 1)
            c = None
            if mat == "bone":
                c = jitter(BONE)
                if rnd.random() < 0.07:
                    c = BONE_DARK + (255,)
                if rnd.random() < 0.015 and face in ("front", "left", "right"):
                    c = (120, 50, 20, 255)
                    gx[X, Y] = EMBER + (255,)
            elif mat == "skull":
                c = jitter(BONE)
                if face == "front":
                    if j in (5, 6) and i in (2, 3, 4, w - 5, w - 4, w - 3):
                        c = (12, 10, 10, 255)                         # les orbites
                        if i in (3, w - 4) and j == 6:
                            c = EMBER + (255,)
                            gx[X, Y] = EMBER + (255,)
                    if j == 10 and i % 2 == 1 and 1 < i < w - 2:
                        c = (14, 12, 12, 255)                         # les dents
                    if j == 8 and i in (w // 2 - 1, w // 2):
                        c = BONE_DARK + (255,)
            elif mat == "ribs":
                c = jitter(BONE)
                if face in ("front", "back") and 1 <= i <= w - 2 and j % 3 == 1 and 2 <= j <= h - 3 and abs(i - w / 2 + 0.5) > 0.6:
                    c = (0, 0, 0, 0)                                  # entre les côtes
                if face in ("front", "back") and abs(i - w / 2 + 0.5) <= 0.6:
                    c = jitter(BONE_DARK, 4)                          # la colonne
            elif mat == "core":
                c = (255, 130 + rnd.randint(-30, 40), 30, 255)
                gx[X, Y] = c
            elif mat == "leather":
                c = jitter(LEATHER, 6)
                if edge:
                    c = LEATHER_DARK + (255,)
            elif mat == "apron":
                c = jitter(LEATHER, 6)
                if rnd.random() < 0.12:
                    c = LEATHER_DARK + (255,)                         # roussi
                if edge and face == "front":
                    c = (82, 54, 36, 255)
                if face == "front" and (i, j) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                    c = BRASS + (255,)
            elif mat in ("brass", "bolt", "copper"):
                base = BRASS if mat != "copper" else (160, 92, 58)
                c = jitter(base, 10)
                if rnd.random() < 0.1:
                    c = PATINA + (255,)
                if edge:
                    c = BRASS_DARK + (255,)
                if mat == "brass" and face in ("front", "left", "right", "top") and (i + j) % 4 == 0 and not edge and rnd.random() < 0.35:
                    c = BRASS_DARK + (255,)                           # rivets
                if mat == "copper" and face in ("left", "right") and 1 <= i <= w - 2 and 1 <= j <= h - 2:
                    c = (20, 16, 14, 255)                             # la bouche de vapeur
            elif mat == "iron":
                c = jitter(IRON, 6)
                if rnd.random() < 0.06:
                    c = (98, 96, 100, 255)                            # rayures
                if j in (1, h - 2) and face != "top" and face != "bottom":
                    c = BRASS + (255,)                                # cerclages de laiton
            elif mat == "plate":
                c = jitter(STEEL, 8)
                d2 = (i - w / 2 + 0.5) ** 2 + (j - h / 2 + 0.5) ** 2
                if d2 < 10:
                    c = jitter((150, 146, 148), 6)                    # usé au centre
                if edge:
                    c = IRON_DARK + (255,)
            elif mat == "grip":
                c = jitter(IRON_DARK, 4)
                if face not in ("top", "bottom") and (j // 2) % 2 == 0 and j < 16:
                    c = jitter(LEATHER, 6)                            # le cuir de la poignée
            elif mat == "gear":
                cx, cy = w / 2 - 0.5, h / 2 - 0.5
                dist = ((i - cx) ** 2 + (j - cy) ** 2) ** 0.5
                if face in ("front", "back"):
                    tooth = (i + j) % 2 == 0 and dist > 3.0
                    if dist > 4.2 or (dist > 3.4 and not tooth):
                        c = (0, 0, 0, 0)
                    else:
                        c = jitter(BRASS, 10)
                        if dist < 1.2:
                            c = BRASS_DARK + (255,)
                else:
                    c = jitter(BRASS_DARK, 6)
            elif mat == "goggles":
                c = (0, 0, 0, 0)
                lens = (1 <= i <= 4) or (w - 5 <= i <= w - 2)
                if face == "front" and lens:
                    rim = i in (1, 4, w - 5, w - 2) or j in (0, 2)
                    c = BRASS + (255,) if rim else (220, 120, 40, 255)
                    if not rim:
                        gx[X, Y] = (255, 150, 60, 255)
                elif j == 1:
                    c = LEATHER + (255,)                              # la sangle
            if c:
                px[X, Y] = c


for s in SPEC:
    for face, rect in faces(uvs[s[0]], s[4]).items():
        paint(s[5], face, rect)

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    tex.save(os.path.join(OUT, "forgemaster.png"))
    glow.save(os.path.join(OUT, "forgemaster_glow.png"))
    print("uv", y + row, "px de haut")
