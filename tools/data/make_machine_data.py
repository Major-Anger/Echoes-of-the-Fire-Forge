"""Les données de la Grande Machine (plan, section 15) : les blocs techniques (bouche de vapeur, moteur des pistons),
leurs textures (tools/textures/make_machine_textures.py) et modèles, les dégâts de la machine, les textes (fr_fr / en_us).

Usage : python3 tools/data/make_machine_data.py fireforge/src/main/resources   (depuis la racine du dépôt)
Les gabarits et la structure : tools/data/make_structures_data.py.
"""
import json
import os
import subprocess
import sys

RES = sys.argv[1]
NS = "echoes_fireforge"
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data", NS)
TOOLS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


# ---------------------------------------------------------------------- blocs (15b)
subprocess.run([sys.executable, "-B", os.path.join(TOOLS, "textures", "make_machine_textures.py"), os.path.join(A, "textures", "block")],
               check=True, stdout=subprocess.DEVNULL)
write(os.path.join(A, "models/block/steam_vent.json"), {
    "parent": "minecraft:block/cube_bottom_top",
    "textures": {"top": NS + ":block/steam_vent_top", "side": NS + ":block/steam_vent_side", "bottom": NS + ":block/steam_vent_side"}})
write(os.path.join(A, "blockstates/steam_vent.json"), {"variants": {"": {"model": NS + ":block/steam_vent"}}})
# le moteur d'un piston est caché dans son corps : l'aspect d'une machine comprimée
write(os.path.join(A, "blockstates/machine_piston.json"), {"variants": {"": {"model": NS + ":block/compressed_machine"}}})

# ---------------------------------------------------------------------- les dégâts de la machine
write(os.path.join(D, "damage_type/steam.json"), {"exhaustion": 0.1, "message_id": NS + ".steam", "scaling": "never", "effects": "burning"})
write(os.path.join(D, "damage_type/piston.json"), {"exhaustion": 0.1, "message_id": NS + ".piston", "scaling": "never"})

# ---------------------------------------------------------------------- textes
FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


t("block.echoes_fireforge.steam_vent", "Bouche de vapeur", "Steam Vent")
t("block.echoes_fireforge.machine_piston", "Piston géant", "Giant Piston")
t("death.attack.echoes_fireforge.steam", "%1$s a été ébouillanté par la vapeur", "%1$s was scalded by steam")
t("death.attack.echoes_fireforge.piston", "%1$s a été écrasé par un piston géant", "%1$s was crushed by a giant piston")
for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    current = read(path)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
