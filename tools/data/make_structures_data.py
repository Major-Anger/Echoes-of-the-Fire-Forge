"""Génère les données des structures de la Forge (plan, section 12b) : les structures et leurs ensembles (où et à quelle
fréquence elles apparaissent), les tags de biomes et de structures, le butin des coffres, la porte scellée (états du
bloc, tags, textes fr_fr / en_us). Les gabarits (.nbt) viennent de tools/structures/make_structures.py.

Usage : python3 tools/data/make_structures_data.py fireforge/src/main/resources
"""
import json
import os
import subprocess
import sys

RES = sys.argv[1]
NS = "echoes_fireforge"
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data", NS)
HERE = os.path.dirname(os.path.abspath(__file__))


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def add_to_tag(path, *values):
    tag = read(path) if os.path.exists(path) else {"values": []}
    for value in values:
        if value not in tag["values"]:
            tag["values"].append(value)
    write(path, tag)


# ---------------------------------------------------------------------- les gabarits
subprocess.run([sys.executable, "-B", os.path.join(HERE, "..", "structures", "make_structures.py"), os.path.join(D, "structure")], check=True)

# ---------------------------------------------------------------------- les lieux (tags de biomes)
WASTES, CRIMSON, SOUL = "minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:soul_sand_valley"
BIOMES = {
    "forge_ruin": [WASTES],                        # les terres désolées
    "forge_ruin_shore": [WASTES, CRIMSON, SOUL],   # les rives de la mer de lave
    "forge_ruin_crimson": [CRIMSON],               # reprise par la forêt carmin
    "forge_ruin_soul": [SOUL],                     # ensevelie dans la vallée des âmes
    "last_hearth": [WASTES],
    "boiler_hall": ["#minecraft:is_nether"],       # creusée dans la roche : partout
    "fortress_annex": ["#minecraft:is_nether"],    # partout où il y a une forteresse
    "ceiling_chimney": ["#minecraft:is_nether"],
}
for name, biomes in BIOMES.items():
    write(os.path.join(D, "tags/worldgen/biome/has_structure", name + ".json"), {"values": biomes})
# l'entrée de la chaufferie ne donne pas dans une forêt : les champignons géants pousseraient devant
write(os.path.join(D, "tags/worldgen/biome/overgrown.json"), {"values": [CRIMSON, "minecraft:warped_forest"]})

# ---------------------------------------------------------------------- les structures
CENDREUX = {"type": NS + ":cendreux", "weight": 1, "minCount": 1, "maxCount": 2}


def structure(name, type_, step="surface_structures", spawns=None, **fields):
    data = {"type": NS + ":" + type_, "biomes": "#" + NS + ":has_structure/" + name, "step": step,
            "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": spawns}} if spawns else {}}
    data.update(fields)
    write(os.path.join(D, "worldgen/structure", name + ".json"), data)


structure("forge_ruin", "floor_ruin", template=NS + ":forge_ruin")
structure("forge_ruin_shore", "floor_ruin", template=NS + ":forge_ruin", shore=True)
structure("forge_ruin_crimson", "floor_ruin", template=NS + ":forge_ruin_crimson")
structure("forge_ruin_soul", "floor_ruin", template=NS + ":forge_ruin_soul",
          foundation={"Name": "minecraft:soul_soil"})
structure("last_hearth", "floor_ruin", spawns=[dict(CENDREUX, maxCount=1)], template=NS + ":last_hearth", kind="hearth")
structure("boiler_hall", "boiler_hall", step="underground_structures", spawns=[CENDREUX])
structure("fortress_annex", "fortress_annex", chance=1.0 / 3.0)
structure("ceiling_chimney", "ceiling_chimney", step="underground_structures")

# ---------------------------------------------------------------------- où et à quelle fréquence (validé par Anger)
SALT = 61450300


def structure_set(name, entries, spacing, separation, salt):
    write(os.path.join(D, "worldgen/structure_set", name + ".json"), {
        "structures": [{"structure": NS + ":" + s, "weight": w} for s, w in entries],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}})


# la forge en ruine : environ une par carré de 28 tronçons ; la rive passe en premier quand la lave est là
structure_set("forge_ruins", [("forge_ruin_shore", 6), ("forge_ruin", 2), ("forge_ruin_crimson", 2), ("forge_ruin_soul", 2)], 28, 8, SALT + 1)
# le dernier foyer : une par carré de 80 tronçons
structure_set("last_hearth", [("last_hearth", 1)], 80, 24, SALT + 2)
# la chaufferie : une par carré de 64 tronçons
structure_set("boiler_hall", [("boiler_hall", 1)], 64, 16, SALT + 3)
# l'annexe : examinée partout, mais elle ne se pose que près d'une vraie forteresse (une sur trois)
structure_set("fortress_annex", [("fortress_annex", 1)], 1, 0, SALT + 4)
# les cheminées du plafond : une par carré de 32 tronçons
structure_set("ceiling_chimney", [("ceiling_chimney", 1)], 32, 8, SALT + 5)

# les Cendreux vivent dans les forteresses, les chaufferies, les derniers foyers
write(os.path.join(D, "tags/worldgen/structure/cendreux_homes.json"), {"values": [
    "minecraft:fortress", NS + ":boiler_hall", NS + ":last_hearth"]})

# ---------------------------------------------------------------------- le butin des coffres


def item(name, weight, low=1, high=None, functions=()):
    entry = {"type": "minecraft:item", "name": name if ":" in name else NS + ":" + name, "weight": weight}
    fns = list(functions)
    if high is not None and high != low or low != 1:
        fns.insert(0, {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high or low}})
    if fns:
        entry["functions"] = fns
    return entry


def plans(weight):
    """Les cinq Plans d'Attiseur (I à V), chacun avec le même poids."""
    return [item("attiseur_plan", weight, functions=[{"function": "minecraft:set_components",
                                                      "components": {NS + ":plan_number": n}}]) for n in range(1, 6)]


def empty(weight):
    return {"type": "minecraft:empty", "weight": weight}


def templates(weight):
    """Un gabarit de mécanisme pris au hasard (table chests/any_mechanism_template)."""
    return {"type": "minecraft:loot_table", "value": NS + ":chests/any_mechanism_template", "weight": weight}


def pool(rolls, entries):
    return {"rolls": rolls if isinstance(rolls, (int, float)) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
            "entries": entries}


def chest(name, *pools):
    write(os.path.join(D, "loot_table/chests", name + ".json"), {
        "type": "minecraft:chest", "pools": list(pools), "random_sequence": NS + ":chests/" + name})


# tous les gabarits de mécanismes, à parts égales (pour les coffres des structures)
mechanisms = [e["name"] for e in read(os.path.join(D, "loot_table/chests/mechanism_templates.json"))["pools"][0]["entries"]
              if e["type"] == "minecraft:item"]
chest("any_mechanism_template", pool(1, [item(m, 1) for m in mechanisms]))

# la forge en ruine : pièces de machine, Cendre d'Attiseur, charbon ; parfois un plan ou un gabarit
chest("forge_ruin",
      pool((3, 5), [item("minecraft:coal", 10, 2, 6), item("minecraft:charcoal", 4, 1, 4), item("machine_gear", 6, 1, 2),
                    item("machine_valve", 6, 1, 2), item("engraved_plate", 3), item("stoker_ash", 6, 1, 3),
                    item("minecraft:iron_nugget", 6, 3, 9), item("minecraft:gold_nugget", 5, 2, 6)]),
      pool(1, [empty(70), *plans(3), item("forge_plan", 6), templates(8)]))
# la forge d'une forteresse : mieux garnie (lingots), des plans, des gabarits, parfois le Gabarit du Wither
chest("fortress_annex",
      pool((3, 5), [item("minecraft:coal", 8, 2, 6), item("minecraft:iron_ingot", 6, 1, 3), item("minecraft:gold_ingot", 5, 1, 3),
                    item("machine_gear", 6, 1, 2), item("machine_valve", 6, 1, 2), item("engraved_plate", 4),
                    item("stoker_ash", 6, 1, 4)]),
      pool(1, [empty(60), *plans(3), templates(12), item("wither_template", 4)]))
# la chaufferie : le Plan de l'Établi plus souvent qu'ailleurs, des gabarits, parfois le Gabarit du Wither
chest("boiler_hall",
      pool((4, 7), [item("minecraft:coal", 10, 3, 8), item("machine_gear", 8, 1, 3), item("machine_valve", 8, 1, 3),
                    item("engraved_plate", 5, 1, 2), item("stoker_ash", 7, 2, 5), item("minecraft:iron_ingot", 5, 1, 4),
                    item("minecraft:copper_ingot", 5, 2, 6), item("minecraft:gold_ingot", 3, 1, 2)]),
      pool(1, [empty(75), item("workbench_plan", 25)]),
      pool(1, [empty(70), templates(30)]),
      pool(1, [empty(84), item("wither_template", 6), *plans(2)]))
# la salle scellée : le meilleur coffre
chest("boiler_hall_sealed",
      pool((4, 6), [item("machine_gear", 8, 2, 4), item("machine_valve", 8, 2, 4), item("engraved_plate", 6, 1, 3),
                    item("stoker_ash", 6, 3, 6), item("minecraft:gold_ingot", 6, 2, 5), item("minecraft:iron_ingot", 6, 2, 6),
                    item("minecraft:diamond", 3, 1, 2), item("minecraft:netherite_scrap", 2)]),
      pool(1, [empty(50), item("workbench_plan", 50)]),
      pool(2, [empty(40), templates(60)]),
      pool(1, [empty(60), *plans(8)]),
      pool(1, [empty(85), item("wither_template", 15)]))
# le dernier foyer : un coffre modeste
chest("last_hearth",
      pool((2, 4), [item("stoker_ash", 8, 2, 5), item("minecraft:coal", 6, 1, 3), item("minecraft:bone", 4, 1, 3),
                    item("minecraft:gold_nugget", 4, 1, 4), item("machine_gear", 3)]),
      pool(1, [empty(80), *plans(4)]))

# ---------------------------------------------------------------------- la porte scellée
write(os.path.join(A, "blockstates/sealed_machine.json"), {"variants": {
    "lock=false": {"model": NS + ":block/compressed_machine"}, "lock=true": {"model": NS + ":block/compressed_machine"}}})
add_to_tag(os.path.join(RES, "data/minecraft/tags/block/mineable/pickaxe.json"), NS + ":sealed_machine")
add_to_tag(os.path.join(RES, "data/minecraft/tags/block/needs_diamond_tool.json"), NS + ":sealed_machine")

FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


t("block.echoes_fireforge.sealed_machine", "Porte scellée", "Sealed Door")

for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    current = read(path)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
