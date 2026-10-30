"""Génère les données du set du Wither (plan, section 13 ; étape 13a) : textures (copiées depuis la sortie de
tools/textures/make_wither_set_textures.py), modèles d'objets, recettes de la table de forgeron, duplication du
gabarit, tags, butin des forteresses, textes (fr_fr / en_us).

Usage : python3 tools/data/make_wither_set_data.py fireforge/src/main/resources <dossier des textures générées>
Le butin du gabarit sur les Cendreux est dans tools/data/make_cendreux_data.py.
"""
import json
import os
import shutil
import sys

RES = sys.argv[1]
TEX = sys.argv[2]
NS = "echoes_fireforge"
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data", NS)
PIECES = [("helmet", "head_armor"), ("chestplate", "chest_armor"), ("leggings", "leg_armor"), ("boots", "foot_armor")]


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def copy(name, dest):
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    shutil.copyfile(os.path.join(TEX, name + ".png"), dest)


# ---------------------------------------------------------------------- textures
ARMOR = os.path.join(A, "textures/models/armor")
for layer in (1, 2):
    copy(f"wither_layer_{layer}_0", os.path.join(ARMOR, f"wither_layer_{layer}.png"))   # celle du matériau : neuve
    for n in range(5):
        copy(f"wither_layer_{layer}_{n}", os.path.join(ARMOR, f"wither_layer_{layer}_{n}.png"))
        if n >= 2:
            copy(f"wither_layer_{layer}_{n}_glow", os.path.join(ARMOR, f"wither_layer_{layer}_{n}_glow.png"))
for piece, _ in PIECES:
    copy("wither_" + piece, os.path.join(A, "textures/item/wither_" + piece + ".png"))
copy("wither_template", os.path.join(A, "textures/item/wither_template.png"))

# ---------------------------------------------------------------------- modèles d'objets
for name in [f"wither_{p}" for p, _ in PIECES] + ["wither_template"]:
    write(os.path.join(A, "models/item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + name}})

# ---------------------------------------------------------------------- recettes : table de forgeron, sur la netherite
for piece, _ in PIECES:
    write(os.path.join(D, "recipe/wither_" + piece + "_smithing.json"), {
        "type": "minecraft:smithing_transform",
        "template": {"item": NS + ":wither_template"},
        "base": {"item": "minecraft:netherite_" + piece},
        "addition": {"item": "minecraft:wither_skeleton_skull"},
        "result": {"id": NS + ":wither_" + piece, "count": 1},
    })
# le gabarit se duplique comme celui de la netherite : sept Cendres d'Attiseur, le gabarit, de la pierre noire
write(os.path.join(D, "recipe/wither_template_copy.json"), {
    "type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["ATA", "ABA", "AAA"],
    "key": {"A": {"item": NS + ":stoker_ash"}, "T": {"item": NS + ":wither_template"}, "B": {"item": "minecraft:blackstone"}},
    "result": {"id": NS + ":wither_template", "count": 2},
})

# ---------------------------------------------------------------------- tags
for piece, tag in PIECES:
    path = os.path.join(RES, "data", "minecraft", "tags", "item", tag + ".json")
    write(path, {"replace": False, "values": [NS + ":wither_" + piece]})
# ne craque jamais à la forge, comme la netherite
path = os.path.join(D, "tags/item/tolerance/fournaise.json")
tag = read(path)
for piece, _ in PIECES:
    if NS + ":wither_" + piece not in tag["values"]:
        tag["values"].append(NS + ":wither_" + piece)
write(path, tag)

# palier 2 (étape 13b) : les animaux d'élevage qui fuient le porteur
write(os.path.join(D, "tags/entity_type/flees_wither.json"), {"values": [
    "minecraft:cow", "minecraft:mooshroom", "minecraft:sheep", "minecraft:pig", "minecraft:chicken", "minecraft:rabbit"]})

# ---------------------------------------------------------------------- butin : rarement dans les coffres des forteresses
write(os.path.join(D, "loot_table/chests/wither_template.json"), {
    "type": "minecraft:chest",
    "pools": [{"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.05}],
               "entries": [{"type": "minecraft:item", "name": NS + ":wither_template"}]}],
    "random_sequence": NS + ":chests/wither_template",
})
write(os.path.join(D, "loot_modifiers/wither_template.json"), {
    "type": "neoforge:add_table",
    "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/nether_bridge"}],
    "table": NS + ":chests/wither_template",
})
path = os.path.join(RES, "data", "neoforge", "loot_modifiers", "global_loot_modifiers.json")
glm = read(path)
if NS + ":wither_template" not in glm["entries"]:
    glm["entries"].append(NS + ":wither_template")
write(path, glm)

# ---------------------------------------------------------------------- textes
FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


t("item.echoes_fireforge.wither_helmet", "Casque du Wither", "Wither Helmet")
t("item.echoes_fireforge.wither_chestplate", "Plastron du Wither", "Wither Chestplate")
t("item.echoes_fireforge.wither_leggings", "Jambières du Wither", "Wither Leggings")
t("item.echoes_fireforge.wither_boots", "Bottes du Wither", "Wither Boots")
t("item.echoes_fireforge.wither_template", "Gabarit du Wither", "Wither Template")
t("item.echoes_fireforge.wither_template.upgrade", "Armure du Wither", "Wither Armor")
t("item.echoes_fireforge.wither_template.applies_to", "Armure en netherite", "Netherite Armor")
t("item.echoes_fireforge.wither_template.ingredients", "Crâne de wither squelette", "Wither Skeleton Skull")
t("item.echoes_fireforge.wither_template.base_slot", "Ajouter une pièce d'armure en netherite", "Add a piece of netherite armor")
t("item.echoes_fireforge.wither_template.additions_slot", "Ajouter un crâne de wither squelette", "Add a wither skeleton skull")
# à chaque palier, une phrase, sans rien expliquer
t("message.echoes_fireforge.wither_stage.1", "Tes mains ne sentent plus la chaleur.", "Your hands no longer feel the heat.")
t("message.echoes_fireforge.wither_stage.2", "Une toux sèche, pleine de cendre.", "A dry cough, full of ash.")
t("message.echoes_fireforge.wither_stage.3", "Ta peau se fend comme le métal.", "Your skin cracks like the metal.")
t("message.echoes_fireforge.wither_stage.4", "Le feu ne te quitte plus.", "The fire no longer leaves you.")
t("commands.echoes_fireforge.witherset.get", "%s : set du Wither à %s %% (palier %s)", "%s: Wither set at %s%% (stage %s)")
t("commands.echoes_fireforge.witherset.set", "%s : set du Wither mis à %s %% (palier %s)", "%s: Wither set put at %s%% (stage %s)")

for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    current = read(path)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
