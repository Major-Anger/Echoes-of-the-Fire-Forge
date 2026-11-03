"""Le maître de forge (plan, section 14) : tire du fichier Blockbench d'Anger (art/forgemaster/maitre_de_forge.bbmodel)
la géométrie du modèle (client/ForgeMasterGeometry.java, généré) et sa texture ; la couche qui luit vient de
tools/textures/make_forgemaster_textures.py. Le marteau-pilon du joueur : art/forgemaster/marteau_pilon.bbmodel donne le
modèle d'objet et sa texture.

Usage : python3 tools/models/make_forgemaster_model.py   (depuis la racine du dépôt ; à relancer après chaque retouche)

Repères : Blockbench (y vers le haut, l'avant vers -z) ; modèles du jeu (y vers le bas, pieds à y = 24, l'avant vers -z,
x de l'autre côté) : x' = -x, y' = 24 - y, z' = z ; rotations (-rx, -ry, rz), comme l'export de Blockbench.
"""
import base64
import io
import json
import os
import subprocess
import sys
import tempfile

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ART = os.path.join(ROOT, "art", "forgemaster")
RES = os.path.join(ROOT, "fireforge", "src", "main", "resources", "assets", "echoes_fireforge")
JAVA = os.path.join(ROOT, "fireforge", "src", "main", "java", "fr", "anger", "echoes", "fireforge", "client", "ForgeMasterGeometry.java")
SKIP = {"apron"}                      # le tablier court : Anger a choisi la ceinture
SPIN = {"gear"}                       # tourne sur lui-même : son pivot au milieu de sa boîte


def texture(model):
    return Image.open(io.BytesIO(base64.b64decode(model["textures"][0]["source"].split(",", 1)[1]))).convert("RGBA")


def num(v):
    v = round(v, 4)
    return (str(int(v)) if v == int(v) else repr(v)) + "F"


# ------------------------------------------------------------------ le boss
boss = json.load(open(os.path.join(ART, "maitre_de_forge.bbmodel"), encoding="utf-8"))
groups = {g["uuid"]: g for g in boss["groups"]}
cubes = {e["uuid"]: e for e in boss["elements"]}
tex = texture(boss)
W, H = boss["resolution"]["width"], boss["resolution"]["height"]


def mc(p):
    return (-p[0], 24 - p[1], p[2])


lines = []


def emit(node, parent_var, parent_abs):
    g = groups[node["uuid"]]
    name = g["name"]
    if name in SKIP:
        return
    elems = [cubes[c] for c in node["children"] if isinstance(c, str)]
    pivot = mc(g["origin"])
    boxes = []
    for e in elems:
        lo = (-e["to"][0], 24 - e["to"][1], e["from"][2])
        size = [e["to"][i] - e["from"][i] for i in range(3)]
        boxes.append((e, lo, size))
    if name in SPIN and boxes:
        e, lo, size = boxes[0]
        pivot = tuple(lo[i] + size[i] / 2 for i in range(3))
    rot = g.get("rotation", [0, 0, 0])
    r = (-rot[0], -rot[1], rot[2])
    cb = "CubeListBuilder.create()"
    for e, lo, size in boxes:
        u, v = e.get("uv_offset") or [0, 0]
        if not e.get("uv_offset"):
            # sans uv_offset (fichier de Blockbench 5) : le coin vient de la face nord en box uv
            n = e["faces"]["north"]["uv"]
            u, v = int(n[0] - size[2]), int(n[1] - size[2])
        cb += f".texOffs({u}, {v})" + (".mirror()" if e.get("mirror_uv") else "")
        off = [lo[i] - pivot[i] for i in range(3)]
        dims = ", ".join(num(s) for s in size)
        inf = e.get("inflate", 0)
        cb += f".addBox({num(off[0])}, {num(off[1])}, {num(off[2])}, {dims}" + (f", new CubeDeformation({num(inf)}))" if inf else ")")
        if e.get("mirror_uv"):
            cb += ".mirror(false)"
    rel = [pivot[i] - parent_abs[i] for i in range(3)]
    var = name.replace(" ", "_")
    rad = ", ".join(f"{num(a)} * DEG" if a else "0" for a in r)
    pose = f"PartPose.offsetAndRotation({num(rel[0])}, {num(rel[1])}, {num(rel[2])}, {rad})"
    lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{name}\", {cb},\n                {pose});")
    for c in node["children"]:
        if isinstance(c, dict):
            emit(c, var, pivot)


for node in boss["outliner"]:
    emit(node, "root", (0, 0, 0))

java = f'''package fr.anger.echoes.fireforge.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Généré par tools/models/make_forgemaster_model.py depuis le fichier Blockbench d'Anger
 * (art/forgemaster/maitre_de_forge.bbmodel) : ne pas retoucher à la main, relancer le script.
 */
@SuppressWarnings("unused")
final class ForgeMasterGeometry {{

    private static final float DEG = Mth.DEG_TO_RAD;

    private ForgeMasterGeometry() {{}}

    static LayerDefinition createLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(lines)}
        return LayerDefinition.create(mesh, {W}, {H});
    }}
}}
'''
with open(JAVA, "w", encoding="utf-8") as f:
    f.write(java)
os.makedirs(os.path.join(RES, "textures", "entity"), exist_ok=True)
tex.save(os.path.join(RES, "textures", "entity", "forge_master.png"))
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run([sys.executable, "-B", os.path.join(ROOT, "tools", "textures", "make_forgemaster_textures.py"), tmp], check=True,
                   stdout=subprocess.DEVNULL)
    Image.open(os.path.join(tmp, "forgemaster_glow.png")).save(os.path.join(RES, "textures", "entity", "forge_master_glow.png"))

# ------------------------------------------------------------------ le marteau-pilon du joueur (modèle d'objet)
item = json.load(open(os.path.join(ART, "marteau_pilon.bbmodel"), encoding="utf-8"))
k = 16 / item["resolution"]["width"]
elements = []
for e in item["elements"]:
    el = {"name": e["name"], "from": e["from"], "to": e["to"],
          "faces": {f: {"uv": [v * k for v in d["uv"]], "texture": "#0"} for f, d in e["faces"].items()}}
    if e.get("rotation") and any(e["rotation"]):
        axis = "xyz"[[i for i, a in enumerate(e["rotation"]) if a][0]]
        el["rotation"] = {"angle": e["rotation"]["xyz".index(axis)], "axis": axis, "origin": e["origin"]}
    elements.append(el)
os.makedirs(os.path.join(RES, "models", "item"), exist_ok=True)
with open(os.path.join(RES, "models", "item", "steam_hammer.json"), "w", encoding="utf-8") as f:
    json.dump({"texture_size": [item["resolution"]["width"], item["resolution"]["height"]],
               "textures": {"0": "echoes_fireforge:item/steam_hammer", "particle": "echoes_fireforge:item/steam_hammer"},
               "elements": elements, "display": item["display"]}, f, indent=2)
os.makedirs(os.path.join(RES, "textures", "item"), exist_ok=True)
texture(item).save(os.path.join(RES, "textures", "item", "steam_hammer.png"))
print(len(lines), "parties du boss ;", len(elements), "pièces du marteau")
