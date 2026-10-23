"""Génère les données de l'Établi d'Attiseur et de son bouilleur (étapes 8a, 8b) : modèles, états, recettes, butin, textes (fr_fr / en_us).

Usage : python3 tools/data/make_workbench_data.py fireforge/src/main/resources <dossier des textures générées>
Les textures viennent de tools/textures/make_workbench_textures.py et make_workbench_gui.py.
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


# ---------------------------------------------------------------------- les mécanismes (ordre de Mechanism.java)
# clé, rare, nom fr, nom en, description fr, description en
MECHANISMS = [
    ("longue_vue", False, "Longue-vue", "Spyglass", "Touche C tenue : zoom.", "Hold C: zoom."),
    ("sonde", True, "Sonde de prospection", "Prospecting Probe", "Touche C : les minerais proches brillent un instant.", "Press C: nearby ores glow for a moment."),
    ("lampe", False, "Lampe à vapeur", "Steam Lamp", "Touche C : une lumière qui suit le regard.", "Press C: a light that follows your gaze."),
    ("sac", False, "Sac dorsal", "Backpack", "Touche B : 9 cases de plus.", "Press B: 9 extra slots."),
    ("planeur", True, "Planeur à vapeur", "Steam Glider", "Touche B tenue en l'air : chute ralentie et petites poussées. Vapeur.", "Hold B in the air: slower fall and small boosts. Steam."),
    ("jet", False, "Jet de vapeur", "Steam Burst", "Touche B : une bouffée repousse les créatures proches. Vapeur.", "Press B: a puff pushes nearby creatures away. Steam."),
    ("poches", False, "Poches", "Pockets", "Touche N : 6 cases de plus.", "Press N: 6 extra slots."),
    ("pistons", True, "Pistons de genoux", "Knee Pistons", "Touche N : un élan vers l'avant. Vapeur.", "Press N: a dash forward. Steam."),
    ("ancre", False, "Ancre", "Anchor", "Accroupi : aucun recul.", "While sneaking: no knockback."),
    ("patins", False, "Patins", "Skates", "Plus rapide sur la pierre, le basalte et la pierre noire.", "Faster on stone, basalt and blackstone."),
    ("ressorts", True, "Ressorts", "Springs", "Sauts plus hauts, chutes amorties.", "Higher jumps, softened falls."),
    ("crampons", False, "Crampons", "Crampons", "Monte un bloc sans sauter ; le sable des âmes ne ralentit plus.", "Step up one block; soul sand no longer slows you."),
    ("piston", False, "Piston", "Piston", "Clic droit : une estocade qui transperce en ligne.", "Right-click: a thrust that pierces in a line."),
    ("sillon", False, "Sillon", "Furrow", "Clic droit : une onde court au sol en ligne droite.", "Right-click: a wave runs along the ground in a straight line."),
    ("frappe_sol", False, "Frappe-sol", "Ground Slam", "Clic droit : une onde de choc autour de soi.", "Right-click: a shockwave all around you."),
    ("treuil", False, "Treuil", "Winch", "Clic droit : l'arme part au bout d'une chaîne et revient.", "Right-click: the weapon flies out on a chain and comes back."),
    ("trident_de_trempe", False, "Trident de trempe", "Quenching Trident", "Ne se lance plus. Clic droit : surchauffe ; dans l'eau, de la vapeur.", "Can no longer be thrown. Right-click: overheat; in water, steam."),
]

FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


t("block.echoes_fireforge.workbench", "Établi d'Attiseur", "Stoker's Workbench")
t("container.echoes_fireforge.workbench", "Établi d'Attiseur", "Stoker's Workbench")
t("item.echoes_fireforge.workbench_plan", "Plan de l'Établi", "Workbench Plan")
t("tooltip.echoes_fireforge.template", "Se greffe à l'Établi d'Attiseur", "Fitted at the Stoker's Workbench")
t("tooltip.echoes_fireforge.template.target", "Pour : %s", "For: %s")
t("tooltip.echoes_fireforge.mechanism", "Mécanisme : %s", "Mechanism: %s")
t("mechanism.echoes_fireforge.target.head", "casque", "helmet")
t("mechanism.echoes_fireforge.target.chest", "plastron", "chestplate")
t("mechanism.echoes_fireforge.target.legs", "jambières", "leggings")
t("mechanism.echoes_fireforge.target.feet", "bottes", "boots")
t("mechanism.echoes_fireforge.target.sword", "épée", "sword")
t("mechanism.echoes_fireforge.target.axe", "hache", "axe")
t("mechanism.echoes_fireforge.target.axe_sword", "épée ou hache", "sword or axe")
t("mechanism.echoes_fireforge.target.trident", "trident", "trident")
t("gui.echoes_fireforge.workbench.key", "Greffer le mécanisme", "Fit the mechanism")
for key, fr, en in [
    ("no_piece", "Pose une arme ou une armure", "Place a weapon or armor"),
    ("no_template", "Pose un gabarit", "Place a template"),
    ("no_gear", "Il faut un engrenage", "A gear is needed"),
    ("wrong_piece", "Ce gabarit ne va pas ici", "This template doesn't fit"),
    ("enchanted_trident", "Pas avec Loyauté ou Impulsion", "Not with Loyalty or Riptide"),
    ("already", "Ce mécanisme y est déjà", "This mechanism is already fitted"),
    ("output_full", "Reprends la pièce greffée", "Take the fitted piece"),
    ("ready", "Prêt : un tour de clé", "Ready: turn the wrench"),
]:
    t("gui.echoes_fireforge.workbench.status." + key, fr, en)
t("block.echoes_fireforge.steam_boiler", "Bouilleur", "Steam Boiler")
t("gui.echoes_fireforge.steam_boiler", "Bouilleur", "Boiler")
t("gui.echoes_fireforge.steam_boiler.water", "Eau : %s / %s mB", "Water: %s / %s mB")
t("gui.echoes_fireforge.steam_boiler.water.hint", "Clic avec un seau d'eau pour le verser", "Click with a water bucket to pour it")
t("gui.echoes_fireforge.steam_boiler.fuel", "Combustible en réserve : %s charges", "Fuel in reserve: %s charges")
t("tooltip.echoes_fireforge.steam", "Vapeur : %s / %s", "Steam: %s / %s")
t("message.echoes_fireforge.steam_boiler.unlinked", "Le bouilleur se pose au bout d'un établi", "The boiler goes at one end of a workbench")
# étape 8e : la Lampe à vapeur
t("block.echoes_fireforge.lamp_light", "Lumière de lampe", "Lamp Light")
t("tooltip.echoes_fireforge.lamp.on", "Allumée", "Lit")
t("tooltip.echoes_fireforge.lamp.off", "Éteinte", "Off")
# étape 8c : les touches et les rangements
t("key.categories.echoes_fireforge", "Echoes of the Fire Forge", "Echoes of the Fire Forge")
t("key.echoes_fireforge.helmet", "Mécanisme du casque", "Helmet mechanism")
t("key.echoes_fireforge.chest", "Mécanisme du plastron", "Chestplate mechanism")
t("key.echoes_fireforge.legs", "Mécanisme des jambières", "Leggings mechanism")
t("container.echoes_fireforge.backpack", "Sac dorsal", "Backpack")
t("container.echoes_fireforge.pockets", "Poches", "Pockets")
t("message.echoes_fireforge.no_steam", "Plus de vapeur : recharge au bouilleur", "Out of steam: recharge at the boiler")
for key, rare, fr, en, dfr, den in MECHANISMS:
    t("mechanism.echoes_fireforge." + key, fr, en)
    t("mechanism.echoes_fireforge." + key + ".desc", dfr, den)
    t("item.echoes_fireforge.template_" + key, "Gabarit : " + fr, en + " Template")

# ---------------------------------------------------------------------- textures
for name in ["front_left", "front_right", "top_left", "top_right", "side"]:
    os.makedirs(os.path.join(A, "textures/block/workbench"), exist_ok=True)
    shutil.copy(os.path.join(TEX, "workbench_" + name + ".png"), os.path.join(A, "textures/block/workbench", name + ".png"))
for key, *_ in MECHANISMS:
    shutil.copy(os.path.join(TEX, "template_" + key + ".png"), os.path.join(A, "textures/item", "template_" + key + ".png"))
shutil.copy(os.path.join(TEX, "workbench_plan.png"), os.path.join(A, "textures/item/workbench_plan.png"))
shutil.copy(os.path.join(TEX, "lamp_light.png"), os.path.join(A, "textures/block/lamp_light.png"))
os.makedirs(os.path.join(A, "textures/entity"), exist_ok=True)
shutil.copy(os.path.join(TEX, "lamp_beam.png"), os.path.join(A, "textures/entity/lamp_beam.png"))
shutil.copy(os.path.join(TEX, "workbench.png"), os.path.join(A, "textures/gui/workbench.png"))

os.makedirs(os.path.join(A, "textures/block/steam_boiler"), exist_ok=True)
for name in ["front", "front_off", "side", "top"]:
    shutil.copy(os.path.join(TEX, "steam_boiler_" + name + ".png"), os.path.join(A, "textures/block/steam_boiler", name + ".png"))

# ---------------------------------------------------------------------- modèles et états
SB_PARTICLE = NS + ":block/steam_boiler/top"
B = NS + ":block/workbench/"
for part in ("left", "right"):
    write(os.path.join(A, "models/block/workbench_" + part + ".json"), {
        "parent": "minecraft:block/cube",
        "textures": {"particle": B + "side", "north": B + "side", "south": B + "front_" + part, "east": B + "side",
                     "west": B + "side", "up": B + "top_" + part, "down": B + "side"}})
ROT = {"south": 0, "west": 90, "north": 180, "east": 270}
variants = {}
for facing, y in ROT.items():
    for part in ("left", "right"):
        v = {"model": NS + ":block/workbench_" + part}
        if y:
            v["y"] = y
        variants[f"facing={facing},part={part}"] = v
write(os.path.join(A, "blockstates/workbench.json"), {"variants": variants})


def face(tex, u0, v0, u1, v1):
    return {"texture": tex, "uv": [u0, v0, u1, v1]}


def half(x0, part):
    return {"from": [x0, 0, 0], "to": [x0 + 16, 16, 16], "faces": {
        "north": face("#side", 0, 0, 16, 16), "south": face("#front_" + part, 0, 0, 16, 16),
        "east": face("#side", 0, 0, 16, 16), "west": face("#side", 0, 0, 16, 16),
        "up": face("#top_" + part, 0, 0, 16, 16), "down": face("#side", 0, 0, 16, 16)}}


with open(os.path.join(A, "models/item/fire_forge.json"), encoding="utf-8") as f:
    display = json.load(f)["display"]
write(os.path.join(A, "models/item/workbench.json"), {
    "textures": {"particle": B + "side", "side": B + "side", "front_left": B + "front_left", "front_right": B + "front_right",
                 "top_left": B + "top_left", "top_right": B + "top_right"},
    "elements": [half(-8, "left"), half(8, "right")],
    "display": display})
for key, *_ in MECHANISMS:
    write(os.path.join(A, "models/item/template_" + key + ".json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/template_" + key}})
write(os.path.join(A, "models/item/workbench_plan.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/workbench_plan"}})

# la lumière de la lampe
write(os.path.join(A, "blockstates/lamp_light.json"), {"variants": {"": {"model": NS + ":block/lamp_light"}}})
# un petit cube transparent, en découpe : invisible, mais Iris voit ses sommets dans la passe d'ombre
write(os.path.join(A, "models/block/lamp_light.json"), {
    "render_type": "minecraft:cutout", "ambientocclusion": False,
    "textures": {"particle": SB_PARTICLE, "clear": NS + ":block/lamp_light"},
    "elements": [{"from": [7.5, 7.5, 7.5], "to": [8.5, 8.5, 8.5], "shade": False,
                  "faces": {d: {"texture": "#clear", "uv": [0, 0, 1, 1]} for d in ("north", "south", "east", "west", "up", "down")}}]})
SB = NS + ":block/steam_boiler/"
for lit in (False, True):
    write(os.path.join(A, "models/block/steam_boiler" + ("_lit" if lit else "") + ".json"), {
        "parent": "minecraft:block/orientable",
        "textures": {"front": SB + ("front" if lit else "front_off"), "side": SB + "side", "top": SB + "top", "particle": SB + "side"}})
write(os.path.join(A, "blockstates/steam_boiler.json"), {"variants": {
    f"facing={facing},lit={str(lit).lower()}": dict({"model": NS + ":block/steam_boiler" + ("_lit" if lit else "")}, **({"y": y} if y else {}))
    for facing, y in ROT.items() for lit in (False, True)}})
write(os.path.join(A, "models/item/steam_boiler.json"), {"parent": NS + ":block/steam_boiler"})

# ---------------------------------------------------------------------- recettes
write(os.path.join(D, "recipe/workbench.json"), {
    "type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["GPG", "CTC", "I I"],
    "key": {"G": {"item": NS + ":machine_gear"}, "P": {"item": NS + ":workbench_plan"}, "C": {"item": "minecraft:copper_ingot"},
            "T": {"item": "minecraft:crafting_table"}, "I": {"item": "minecraft:iron_ingot"}},
    "result": {"id": NS + ":workbench", "count": 1}})
for key, rare, *_ in MECHANISMS:
    ingredients = [{"item": NS + ":template_" + key}] + [{"item": NS + ":machine_gear"}] * 3 + [{"item": "minecraft:copper_block"}]
    if rare:
        ingredients.append({"item": NS + ":engraved_plate"})
    write(os.path.join(D, "recipe/template_" + key + "_copy.json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc", "group": "echoes_fireforge_template_copy",
        "ingredients": ingredients, "result": {"id": NS + ":template_" + key, "count": 2}})

write(os.path.join(D, "recipe/steam_boiler.json"), {
    "type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["CVC", "CBC", "IFI"],
    "key": {"C": {"item": "minecraft:copper_ingot"}, "V": {"item": NS + ":machine_valve"}, "B": {"item": "minecraft:bucket"},
            "I": {"item": "minecraft:iron_ingot"}, "F": {"item": "minecraft:furnace"}},
    "result": {"id": NS + ":steam_boiler", "count": 1}})

# ---------------------------------------------------------------------- butin
write(os.path.join(D, "loot_table/blocks/workbench.json"), {
    "type": "minecraft:block", "pools": [{"rolls": 1, "conditions": [
        {"condition": "minecraft:survives_explosion"},
        {"condition": "minecraft:block_state_property", "block": NS + ":workbench", "properties": {"part": "left"}}],
        "entries": [{"type": "minecraft:item", "name": NS + ":workbench"}]}],
    "random_sequence": NS + ":blocks/workbench"})

write(os.path.join(D, "loot_table/blocks/steam_boiler.json"), {
    "type": "minecraft:block", "pools": [{"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"}],
                                          "entries": [{"type": "minecraft:item", "name": NS + ":steam_boiler"}]}],
    "random_sequence": NS + ":blocks/steam_boiler"})

# machine comprimée : la répartition des plans (Forge 35, Établi 25, Attiseur I à V 40) et 2 % de gabarit
path = os.path.join(D, "loot_table/blocks/compressed_machine.json")
with open(path, encoding="utf-8") as f:
    loot = json.load(f)
plan_pool = next(p for p in loot["pools"] if any(e.get("name") == NS + ":forge_plan" for e in p["entries"]))
entries = [{"type": "minecraft:item", "name": NS + ":forge_plan", "weight": 35},
           {"type": "minecraft:item", "name": NS + ":workbench_plan", "weight": 25}]
entries += [e for e in plan_pool["entries"] if e["name"] == NS + ":attiseur_plan"]
for e in entries[2:]:
    e["weight"] = 8
plan_pool["entries"] = entries
loot["pools"] = [p for p in loot["pools"] if not any(e.get("name", "").startswith(NS + ":template_") for e in p["entries"])]
template_conditions = [c for c in plan_pool["conditions"] if c["condition"] != "minecraft:random_chance"]
loot["pools"].append({"rolls": 1, "conditions": template_conditions + [{"condition": "minecraft:random_chance", "chance": 0.02}],
                      "entries": [{"type": "minecraft:item", "name": NS + ":template_" + key, "weight": 1 if rare else 3}
                                  for key, rare, *_ in MECHANISMS]})
write(path, loot)

# coffres de bastion et de forteresse : commun 8 %, rare 2 % par coffre (en tout)
common = [k for k, r, *_ in MECHANISMS if not r]
rares = [k for k, r, *_ in MECHANISMS if r]
write(os.path.join(D, "loot_table/chests/mechanism_templates.json"), {
    "type": "minecraft:chest", "pools": [{"rolls": 1, "entries":
        [{"type": "minecraft:empty", "weight": 900}]
        + [{"type": "minecraft:item", "name": NS + ":template_" + k, "weight": 6} for k in common]
        + [{"type": "minecraft:item", "name": NS + ":template_" + k, "weight": 5} for k in rares]}],
    "random_sequence": NS + ":chests/mechanism_templates"})
CHESTS = ["bastion_treasure", "bastion_other", "bastion_bridge", "bastion_hoglin_stable", "nether_bridge"]
write(os.path.join(D, "loot_modifiers/mechanism_templates.json"), {
    "type": "neoforge:add_table",
    "conditions": [{"condition": "minecraft:any_of", "terms": [
        {"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/" + c} for c in CHESTS]}],
    "table": NS + ":chests/mechanism_templates"})
write(os.path.join(RES, "data/neoforge/loot_modifiers/global_loot_modifiers.json"),
      {"replace": False, "entries": [NS + ":mechanism_templates"]})

# tags : pioche, et la liste des gabarits
path = os.path.join(RES, "data/minecraft/tags/block/mineable/pickaxe.json")
with open(path, encoding="utf-8") as f:
    tag = json.load(f)
for block in ("workbench", "steam_boiler"):
    if NS + ":" + block not in tag["values"]:
        tag["values"].append(NS + ":" + block)
write(path, tag)
write(os.path.join(D, "tags/item/mechanism_templates.json"), {"values": [NS + ":template_" + k for k, *_ in MECHANISMS]})

# étape 8c : où roulent les Patins (pierre, basalte, pierre noire, briques, métal), et ce que cherche la Sonde
write(os.path.join(D, "tags/block/skate_surfaces.json"), {"values": [
    "#minecraft:base_stone_overworld", "#minecraft:base_stone_nether", "#minecraft:stone_bricks",
    "minecraft:cobblestone", "minecraft:smooth_stone", "minecraft:polished_andesite", "minecraft:polished_diorite",
    "minecraft:polished_granite", "minecraft:cobbled_deepslate", "minecraft:polished_deepslate", "minecraft:deepslate_bricks",
    "minecraft:cracked_deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:cracked_deepslate_tiles",
    "minecraft:polished_basalt", "minecraft:smooth_basalt", "minecraft:polished_blackstone", "minecraft:polished_blackstone_bricks",
    "minecraft:cracked_polished_blackstone_bricks", "minecraft:chiseled_polished_blackstone", "minecraft:gilded_blackstone",
    "minecraft:nether_bricks", "minecraft:red_nether_bricks", "minecraft:cracked_nether_bricks", "minecraft:bricks",
    "minecraft:iron_block", "minecraft:copper_block", "minecraft:cut_copper", "minecraft:waxed_copper_block", "minecraft:waxed_cut_copper",
    "minecraft:obsidian"]})
write(os.path.join(D, "tags/block/probe_targets.json"), {"values": [
    "#c:ores", "minecraft:ancient_debris", NS + ":compressed_machine"]})

# ---------------------------------------------------------------------- textes
for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    with open(path, encoding="utf-8") as f:
        current = json.load(f)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
