"""Les données de la Grande Machine (plan, section 15) : les blocs techniques (bouche de vapeur, moteur des pistons),
leurs textures (tools/textures/make_machine_textures.py) et modèles, les dégâts de la machine, les textes (fr_fr / en_us).

Usage : python3 tools/data/make_machine_data.py fireforge/src/main/resources   (depuis la racine du dépôt)
Les gabarits et la structure : tools/data/make_structures_data.py.
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile

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
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run([sys.executable, "-B", os.path.join(TOOLS, "textures", "make_machine_textures.py"), tmp], check=True, stdout=subprocess.DEVNULL)
    for name in os.listdir(tmp):
        kind = "item" if name == "heart_ember.png" else "block"
        os.makedirs(os.path.join(A, "textures", kind), exist_ok=True)
        shutil.copyfile(os.path.join(tmp, name), os.path.join(A, "textures", kind, name))
write(os.path.join(A, "models/block/steam_vent.json"), {
    "parent": "minecraft:block/cube_bottom_top",
    "textures": {"top": NS + ":block/steam_vent_top", "side": NS + ":block/steam_vent_side", "bottom": NS + ":block/steam_vent_side"}})
write(os.path.join(A, "blockstates/steam_vent.json"), {"variants": {"": {"model": NS + ":block/steam_vent"}}})
# le moteur d'un piston est caché dans son corps : l'aspect d'une machine comprimée
write(os.path.join(A, "blockstates/machine_piston.json"), {"variants": {"": {"model": NS + ":block/compressed_machine"}}})

# ---------------------------------------------------------------------- 15c : les vannes, la bouche du cœur, la Braise
# une vanne : un socle (10 x 12 x 10 pixels) et sa roue (16 x 2 x 16) ; la plaque gravée sur le socle, la roue dessus
side_faces = {f: {"uv": [3, 4, 13, 16], "texture": "#side"} for f in ("north", "south", "east", "west")}
side_faces["down"] = {"uv": [3, 3, 13, 13], "texture": "#side"}
wheel_side = {"uv": [0, 7, 16, 9], "texture": "#wheel"}
write(os.path.join(A, "models/block/regulator_valve.json"), {
    "parent": "minecraft:block/block",
    "textures": {"particle": "#side"},
    "elements": [
        {"from": [3, 0, 3], "to": [13, 12, 13], "faces": side_faces},
        {"from": [0, 12, 0], "to": [16, 14, 16], "faces": {
            "up": {"uv": [0, 0, 16, 16], "texture": "#wheel"}, "down": {"uv": [0, 0, 16, 16], "texture": "#wheel"},
            "north": wheel_side, "south": wheel_side, "east": wheel_side, "west": wheel_side}},
    ]})
variants = {}
for kind in ("lower", "raise"):
    for turned in (False, True):
        name = "regulator_valve_%s%s" % (kind, "_turned" if turned else "")
        write(os.path.join(A, "models/block", name + ".json"), {
            "parent": NS + ":block/regulator_valve",
            "textures": {"side": NS + ":block/valve_%s_side" % kind, "wheel": NS + ":block/valve_wheel" + ("_turned" if turned else "")}})
        variants["kind=%s,turned=%s" % (kind, str(turned).lower())] = {"model": NS + ":block/" + name}
write(os.path.join(A, "blockstates/regulator_valve.json"), {"variants": variants})
# la bouche du cœur : une grille de laiton où rougeoie la braise (les textures de la bouche de vapeur)
write(os.path.join(A, "blockstates/heart_mouth.json"), {"variants": {"": {"model": NS + ":block/steam_vent"}}})
write(os.path.join(A, "models/item/heart_ember.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/heart_ember"}})
# à la Forge : la chaleur de la lave (Fournaise)
path = os.path.join(D, "tags/item/heat/fournaise.json")
tag = read(path)
if NS + ":heart_ember" not in tag["values"]:
    tag["values"].append(NS + ":heart_ember")
write(path, tag)

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
t("block.echoes_fireforge.regulator_valve", "Vanne du régulateur", "Regulator Valve")
t("block.echoes_fireforge.heart_mouth", "Bouche du cœur", "Heart's Mouth")
t("item.echoes_fireforge.heart_ember", "Braise du cœur", "Heart Ember")
t("message.echoes_fireforge.valve.confirm", "Ce choix vaut pour tout le monde, et pour toujours. Tourne encore la vanne pour confirmer.",
  "This choice is for everyone, and forever. Turn the valve again to confirm.")
t("message.echoes_fireforge.valve.locked", "Le régulateur ne bouge plus.", "The regulator no longer moves.")
t("message.echoes_fireforge.machine.lowered", "La Grande Machine ralentit, puis se tait.", "The Great Machine slows down, then falls silent.")
t("message.echoes_fireforge.machine.raised", "La Grande Machine gronde.", "The Great Machine roars.")
t("message.echoes_fireforge.machine.drained", "Sous la machine, la lave se retire.", "Under the machine, the lava recedes.")
t("message.echoes_fireforge.heart.nothing", "Rien ne sort de la bouche du cœur.", "Nothing comes out of the heart's mouth.")
t("message.echoes_fireforge.heart.cold", "Le cœur est froid.", "The heart is cold.")
t("message.echoes_fireforge.heart.tomorrow", "Le cœur n'a plus rien à donner aujourd'hui.", "The heart has nothing more to give today.")
t("message.echoes_fireforge.heart.ember", "Le cœur te donne une braise.", "The heart gives you an ember.")
t("death.attack.echoes_fireforge.steam", "%1$s a été ébouillanté par la vapeur", "%1$s was scalded by steam")
t("death.attack.echoes_fireforge.piston", "%1$s a été écrasé par un piston géant", "%1$s was crushed by a giant piston")
for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    current = read(path)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
