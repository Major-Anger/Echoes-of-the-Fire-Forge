"""Génère les données de l'étape 12c (les Cendreux et le Lit de cendre) : textures (copiées depuis la sortie de
tools/textures/make_cendreux_textures.py), modèles de la forge garnie, états du bloc, modèles d'objets, butin, recette,
tags, apparitions, sons et textes (fr_fr / en_us).

Usage : python3 tools/data/make_cendreux_data.py fireforge/src/main/resources <dossier des textures générées>
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
copy("cendreux", os.path.join(A, "textures/entity/cendreux.png"))
copy("cendreux_glow", os.path.join(A, "textures/entity/cendreux_glow.png"))
for item in ("stoker_ash", "ash_bed"):
    copy(item, os.path.join(A, "textures/item", item + ".png"))
for name in ("forge_front_left_ash", "forge_front_right_ash", "forge_front_left_ash_soul", "forge_front_right_ash_soul", "ash_tray"):
    copy(name, os.path.join(A, "textures/block/forge", name + ".png"))

# ---------------------------------------------------------------------- la forge garnie : le foyer et le plateau
# Le plateau de laiton dépasse de 3 px devant le foyer (x 8 à 24 sur les deux blocs), la cendre tassée dedans, un tas.
BRASS = {f: {"texture": "#brass"} for f in ("north", "south", "east", "west", "up", "down")}


def ash(u0, v0, u1, v1):
    return {"texture": "#ash", "uv": [u0, v0, u1, v1]}


def tray(part):
    if part == "left":
        x0, x1, ix0, ix1, end, heap = 8, 16, 8.5, 16, [8, 1.5, 16, 8.5, 2.5, 18.5], [12, 2.5, 16.3, 16, 3.5, 18]
    else:
        x0, x1, ix0, ix1, end, heap = 0, 8, 0, 7.5, [7.5, 1.5, 16, 8, 2.5, 18.5], [0, 2.5, 16.3, 4, 3.5, 18]
    return [
        {"from": [x0, 0, 16], "to": [x1, 1.5, 19], "faces": BRASS},
        {"from": [x0, 1.5, 18.5], "to": [x1, 2.5, 19], "faces": BRASS},
        {"from": end[:3], "to": end[3:], "faces": BRASS},
        {"from": [ix0, 1.5, 16], "to": [ix1, 2.5, 18.5], "faces": {
            "up": ash(ix0, 0, ix1, 2.5), "south": ash(ix0, 8, ix1, 9), "east": ash(0, 8, 2.5, 9), "west": ash(0, 8, 2.5, 9)}},
        {"from": heap[:3], "to": heap[3:], "faces": {
            "up": ash(4, 4, 8, 5.7), "south": ash(4, 10, 8, 11), "east": ash(0, 10, 1.7, 11), "west": ash(0, 10, 1.7, 11)}},
    ]


for part in ("left", "right"):
    model = read(os.path.join(A, "models/block", "fire_forge_" + part + ".json"))
    model["textures"]["front_" + part] = NS + ":block/forge/forge_front_" + part + "_ash"
    model["textures"]["ash"] = NS + ":block/forge/ash_tray"
    model["textures"].setdefault("brass", NS + ":block/forge/brass_trim")
    model["elements"] = model["elements"] + tray(part)
    write(os.path.join(A, "models/block", "fire_forge_" + part + "_ash.json"), model)
    # le feu des âmes, comme pour la forge nue : le même modèle, le foyer en bleu
    write(os.path.join(A, "models/block", "fire_forge_" + part + "_ash_soul.json"), {
        "parent": NS + ":block/fire_forge_" + part + "_ash",
        "textures": {"front_" + part: NS + ":block/forge/forge_front_" + part + "_ash_soul"}})

# l'état du bloc : « ash » en plus (64 variantes)
path = os.path.join(A, "blockstates/fire_forge.json")
states = read(path)["variants"]
plain = {k: v for k, v in states.items() if "ash=" not in k}
if not plain:
    plain = {k.replace("ash=false,", ""): v for k, v in states.items() if k.startswith("ash=false,")}
variants = {}
for key, value in plain.items():
    variants["ash=false," + key] = value
    garnished = dict(value)
    model = value["model"]
    garnished["model"] = model.replace("_soul", "_ash_soul") if model.endswith("_soul") else model + "_ash"
    variants["ash=true," + key] = garnished
write(path, {"variants": variants})

# ---------------------------------------------------------------------- objets
for item in ("stoker_ash", "ash_bed"):
    write(os.path.join(A, "models/item", item + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/" + item}})
write(os.path.join(A, "models/item/cendreux_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})

# ---------------------------------------------------------------------- butin
write(os.path.join(D, "loot_table/entities/cendreux.json"), {
    "type": "minecraft:entity",
    "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + ":stoker_ash", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
             "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]},
        {"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"},
                                    {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                     "unenchanted_chance": 0.08, "enchanted_chance": {"type": "minecraft:linear", "base": 0.1, "per_level_above_first": 0.02}}],
         "entries": [{"type": "minecraft:item", "name": NS + ":machine_gear"}]},
        # le Gabarit du Wither (plan, section 13) : rarement
        {"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"},
                                    {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                     "unenchanted_chance": 0.025, "enchanted_chance": {"type": "minecraft:linear", "base": 0.035, "per_level_above_first": 0.01}}],
         "entries": [{"type": "minecraft:item", "name": NS + ":wither_template"}]},
    ],
    "random_sequence": NS + ":entities/cendreux",
})
# la forge cassée garde son Lit de cendre
path = os.path.join(D, "loot_table/blocks/fire_forge.json")
loot = read(path)
entry = loot["pools"][0]["entries"][0]
entry["functions"] = [{"function": "minecraft:set_components", "components": {NS + ":ash_bed": {}},
                       "conditions": [{"condition": "minecraft:block_state_property", "block": NS + ":fire_forge", "properties": {"ash": "true"}}]}]
write(path, loot)

# ---------------------------------------------------------------------- recette : 4 cendres aux coins, 4 engrenages, 1 plaque gravée
write(os.path.join(D, "recipe/ash_bed.json"), {
    "type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["CGC", "GPG", "CGC"],
    "key": {"C": {"item": NS + ":stoker_ash"}, "G": {"item": NS + ":machine_gear"}, "P": {"item": NS + ":engraved_plate"}},
    "result": {"id": NS + ":ash_bed", "count": 1},
})

# ---------------------------------------------------------------------- tags : les machines qu'un Cendreux défend
write(os.path.join(D, "tags/block/attiseur_machines.json"), {"values": [
    NS + ":fire_forge", NS + ":workbench", NS + ":steam_boiler", NS + ":compressed_machine",
    NS + ":boiler", NS + ":lava_tank", NS + ":soul_module"]})

# ---------------------------------------------------------------------- apparitions (rares)
write(os.path.join(D, "neoforge/biome_modifier/cendreux.json"), {
    "type": "neoforge:add_spawns", "biomes": ["minecraft:nether_wastes", "minecraft:basalt_deltas"],
    "spawners": {"type": NS + ":cendreux", "weight": 2, "minCount": 1, "maxCount": 2}})
write(os.path.join(D, "neoforge/structure_modifier/cendreux_fortress.json"), {
    "type": "neoforge:add_spawns", "structures": "minecraft:fortress",
    "spawners": {"type": NS + ":cendreux", "weight": 1, "minCount": 1, "maxCount": 1}})

# ---------------------------------------------------------------------- sons (les fichiers du jeu, nos sous-titres)
SOUNDS = {
    "entity.cendreux.cough": ["mob/wither_skeleton/idle1", "mob/wither_skeleton/idle2", "mob/wither_skeleton/idle3"],
    "entity.cendreux.hurt": ["mob/wither_skeleton/hurt1", "mob/wither_skeleton/hurt2", "mob/wither_skeleton/hurt3", "mob/wither_skeleton/hurt4"],
    "entity.cendreux.death": ["mob/wither_skeleton/death1", "mob/wither_skeleton/death2"],
    "entity.cendreux.ember": ["mob/ghast/fireball4"],
}
write(os.path.join(A, "sounds.json"), {
    key: {"sounds": [{"name": "minecraft:" + s, "pitch": 0.8} for s in files], "subtitle": "subtitles." + NS + "." + key.replace("entity.", "")}
    for key, files in SOUNDS.items()})

# ---------------------------------------------------------------------- textes
FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


t("entity.echoes_fireforge.cendreux", "Cendreux", "Ashen")
t("item.echoes_fireforge.cendreux_spawn_egg", "Œuf d'apparition de Cendreux", "Ashen Spawn Egg")
t("item.echoes_fireforge.stoker_ash", "Cendre d'Attiseur", "Stoker's Ash")
t("item.echoes_fireforge.ash_bed", "Lit de cendre", "Ash Bed")
t("tooltip.echoes_fireforge.ash_bed", "Garnit la Forge des Attiseurs (clic droit), pour toujours", "Lines the Stokers' Forge (right-click), for good")
t("tooltip.echoes_fireforge.ash_bed.offering", "Avec une offrande : plus de Fêlé au tirage", "With an offering: no more Cracked")
t("tooltip.echoes_fireforge.ash_bed.scrap", "Avec un fragment de netherite : Trempé au minimum", "With netherite scrap: Tempered at least")
t("tooltip.echoes_fireforge.forge_ash_bed", "Garnie d'un Lit de cendre", "Lined with an Ash Bed")
t("message.echoes_fireforge.ash_bed.installed", "Le lit de cendre garnit le foyer. La chaleur se tient égale.", "The ash bed lines the hearth. The heat holds even.")
t("message.echoes_fireforge.ash_bed.already", "Ce foyer est déjà garni de cendre.", "This hearth is already lined with ash.")
t("subtitles.echoes_fireforge.cendreux.cough", "Un Cendreux tousse", "Ashen coughs")
t("subtitles.echoes_fireforge.cendreux.hurt", "Un Cendreux est blessé", "Ashen hurts")
t("subtitles.echoes_fireforge.cendreux.death", "Un Cendreux s'effondre", "Ashen crumbles")
t("subtitles.echoes_fireforge.cendreux.ember", "Un Cendreux lance une braise", "Ashen throws an ember")

for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    current = read(path)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
