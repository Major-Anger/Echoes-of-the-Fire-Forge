"""Les données du maître de forge (plan, section 14 ; étape 14a) : le modèle et les textures (tirés des fichiers
Blockbench d'Anger), la barre de boss, l'œuf d'apparition, les tags, les textes (fr_fr / en_us).

Usage : python3 tools/data/make_forgemaster_data.py fireforge/src/main/resources   (depuis la racine du dépôt)
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


subprocess.run([sys.executable, "-B", os.path.join(TOOLS, "models", "make_forgemaster_model.py")], check=True)
subprocess.run([sys.executable, "-B", os.path.join(TOOLS, "textures", "make_forgemaster_bar.py"), RES], check=True)

write(os.path.join(A, "models/item/forge_master_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})
# un mort-vivant (Châtiment, soins inversés, Wither sans effet), comme les wither squelettes
path = os.path.join(RES, "data", "minecraft", "tags", "entity_type", "undead.json")
tag = read(path)
if NS + ":forge_master" not in tag["values"]:
    tag["values"].append(NS + ":forge_master")
write(path, tag)

FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


t("entity.echoes_fireforge.forge_master", "Le Maître de forge", "The Forge Master")
t("item.echoes_fireforge.forge_master_spawn_egg", "Œuf d'apparition du Maître de forge", "Forge Master Spawn Egg")
for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    current = read(path)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
