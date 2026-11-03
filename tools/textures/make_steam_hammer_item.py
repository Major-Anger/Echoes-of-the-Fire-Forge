"""Le marteau-pilon du maître de forge, en objet pour le joueur (plan, section 14) : le même dessin que celui du boss,
en plus petit. Écrit la texture 64 x 64, le modèle d'objet de Minecraft (.json) et le fichier Blockbench (.bbmodel,
« java_block ») qu'Anger peut retoucher. Aperçu avant validation.

Usage : python3 tools/textures/make_steam_hammer_item.py <dossier de sortie>
"""
import base64
import json
import os
import random
import sys
import uuid

from PIL import Image

OUT = sys.argv[1]
TEX = 64
rnd = random.Random(141)

# (nom, de, taille, matière) en pixels d'objet (16 = un bloc) ; la tête en haut, comme on le tient
SPEC = [
    ("handle", (7, -4, 7), (2, 22, 2), "grip"),
    ("counterweight", (6.5, -7, 6.5), (3, 3, 3), "iron"),
    ("bolt", (6, 18, 6), (4, 2, 4), "brass"),
    ("head", (2, 20, 4), (12, 8, 8), "iron"),
    ("strike_plate", (0, 19, 3), (2, 10, 10), "plate"),
    ("nozzle", (14, 22.5, 6.5), (3, 3, 3), "copper"),
    ("gear", (4.5, 20.5, 12), (7, 7, 1), "gear"),
]

# rangement des uv (box uv) dans la texture
uvs, x, y, row = {}, 0, 0, 0
for name, _, size, _ in sorted(SPEC, key=lambda s: -(s[2][2] + s[2][1])):
    w, h, d = size
    fw, fh = 2 * (w + d), d + h
    if x + fw > TEX:
        x, y, row = 0, y + row, 0
    uvs[name] = (x, y)
    x, row = x + fw, max(row, fh)
assert y + row <= TEX

BRASS, BRASS_DARK, PATINA = (150, 112, 60), (92, 66, 36), (88, 128, 104)
IRON, IRON_DARK, STEEL, LEATHER = (66, 64, 68), (36, 35, 38), (128, 126, 130), (64, 40, 28)
tex = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
px = tex.load()


def jitter(c, n=8):
    k = rnd.randint(-n, n)
    return tuple(max(0, min(255, v + k)) for v in c[:3]) + (255,)


def rects(name, size):
    u, v = uvs[name]
    w, h, d = size
    # face du modèle -> (u, v, largeur, hauteur) ; sud = l'avant (+z)
    return {"south": (u + d, v + d, w, h), "north": (u + 2 * d + w, v + d, w, h), "west": (u, v + d, d, h),
            "east": (u + d + w, v + d, d, h), "up": (u + d, v, w, d), "down": (u + d + w, v, w, d)}


for name, _, size, mat in SPEC:
    for face, (fx, fy, w, h) in rects(name, size).items():
        for i in range(w):
            for j in range(h):
                edge = i in (0, w - 1) or j in (0, h - 1)
                c = None
                if mat == "iron":
                    c = jitter(IRON, 6)
                    if rnd.random() < 0.06:
                        c = (98, 96, 100, 255)
                    if face not in ("up", "down") and j in (1, h - 2):
                        c = BRASS + (255,)
                elif mat == "plate":
                    c = jitter(STEEL, 8)
                    if edge:
                        c = IRON_DARK + (255,)
                elif mat in ("brass", "copper"):
                    c = jitter(BRASS if mat == "brass" else (160, 92, 58), 10)
                    if rnd.random() < 0.1:
                        c = PATINA + (255,)
                    if edge:
                        c = BRASS_DARK + (255,)
                    if mat == "copper" and face in ("east", "west") and not edge:
                        c = (20, 16, 14, 255)
                elif mat == "grip":
                    c = jitter(IRON_DARK, 4)
                    if face not in ("up", "down") and (j // 2) % 2 == 0 and j < 12:
                        c = jitter(LEATHER, 6)
                elif mat == "gear":
                    cx, cy = w / 2 - 0.5, h / 2 - 0.5
                    dist = ((i - cx) ** 2 + (j - cy) ** 2) ** 0.5
                    if face in ("north", "south"):
                        tooth = (i + j) % 2 == 0 and dist > 2.2
                        c = None if dist > 3.3 or (dist > 2.6 and not tooth) else jitter(BRASS, 10)
                        if c and dist < 1.0:
                            c = BRASS_DARK + (255,)
                    else:
                        c = jitter(BRASS_DARK, 6)
                if c:
                    px[fx + i, fy + j] = c

# ------------------------------------------------------------------ le modèle d'objet de Minecraft
k = 16 / TEX
elements = []
for name, frm, size, _ in SPEC:
    to = [frm[i] + size[i] for i in range(3)]
    faces = {}
    for face, (fx, fy, w, h) in rects(name, size).items():
        faces[face] = {"uv": [fx * k, fy * k, (fx + w) * k, (fy + h) * k], "texture": "#0"}
    elements.append({"name": name, "from": list(frm), "to": to, "faces": faces})
display = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 3, 1], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 3, 1], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, 90, 15], "translation": [1, 2, 0], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [0, -90, -15], "translation": [1, 2, 0], "scale": [0.7, 0.7, 0.7]},
    "gui": {"rotation": [20, -135, 35], "translation": [1, -1, 0], "scale": [0.55, 0.55, 0.55]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 90, 35], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
}
model = {"texture_size": [TEX, TEX], "textures": {"0": "echoes_fireforge:item/steam_hammer", "particle": "echoes_fireforge:item/steam_hammer"},
         "elements": elements, "display": display}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    tex.save(os.path.join(OUT, "steam_hammer.png"))
    with open(os.path.join(OUT, "steam_hammer.json"), "w", encoding="utf-8") as f:
        json.dump(model, f, indent=1)
    # le fichier Blockbench (modèle « java_block », texture incluse)
    src = "data:image/png;base64," + base64.b64encode(open(os.path.join(OUT, "steam_hammer.png"), "rb").read()).decode()
    bb_elements = []
    for e in elements:
        bb_elements.append({"name": e["name"], "from": e["from"], "to": e["to"], "uuid": str(uuid.uuid4()), "autouv": 0, "color": 0,
                            "rescale": False, "origin": [8, 8, 8],
                            "faces": {f: {"uv": [v * TEX / 16 for v in d["uv"]], "texture": 0} for f, d in e["faces"].items()}})
    bb = {"meta": {"format_version": "4.5", "model_format": "java_block", "box_uv": False}, "name": "marteau_pilon",
          "resolution": {"width": TEX, "height": TEX}, "elements": bb_elements, "outliner": [e["uuid"] for e in bb_elements],
          "display": display,
          "textures": [{"path": "", "name": "steam_hammer.png", "folder": "item", "namespace": "echoes_fireforge", "id": "0",
                        "particle": True, "render_mode": "default", "visible": True, "mode": "bitmap", "saved": False,
                        "uuid": str(uuid.uuid4()), "width": TEX, "height": TEX, "uv_width": TEX, "uv_height": TEX, "source": src}]}
    with open(os.path.join(OUT, "marteau_pilon.bbmodel"), "w", encoding="utf-8") as f:
        json.dump(bb, f, indent=1)
    print("uv", y + row, "px")
