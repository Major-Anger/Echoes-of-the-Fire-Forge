"""Les données du maître de forge (plan, section 14 ; étape 14a) : le modèle et les textures (tirés des fichiers
Blockbench d'Anger), la barre de boss, l'œuf d'apparition, les tags, les sons, les textes (fr_fr / en_us).
Les sons d'Anger sont convertis d'abord par tools/sounds/make_forgemaster_sounds.py (fichiers dans sounds/forge_master/).

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

# les sons : ceux d'Anger (sounds/forge_master/), et pour sa voix ceux du wither squelette, plus graves
OWN = {
    "ground_hit": ["ground_hit1", "ground_hit2", "ground_hit3", "ground_hit4"],
    "hit": ["hit1", "hit2", "hit3", "hit4"],
    "swing": ["swing1", "swing2", "swing3"],
    "steam": ["steam1", "steam2"],
    "step": ["step1", "step2", "step3", "step4", "step5", "step6"],
    "death": ["death1", "death2"],
}
VOICE = {
    "ambient": ["mob/wither_skeleton/idle1", "mob/wither_skeleton/idle2", "mob/wither_skeleton/idle3"],
    "hurt": ["mob/wither_skeleton/hurt1", "mob/wither_skeleton/hurt2", "mob/wither_skeleton/hurt3", "mob/wither_skeleton/hurt4"],
}
FAR = {"ground_hit": 32, "death": 48, "steam": 24}               # les grands bruits portent plus loin que 16 blocs
for d in os.listdir(os.path.join(A, "sounds", "forge_master")):
    if d.endswith(".ogg") and d[:-4] not in sum(OWN.values(), []):
        sys.exit("son sans entrée : " + d)
sounds_path = os.path.join(A, "sounds.json")
sounds = read(sounds_path) if os.path.exists(sounds_path) else {}
for key, files in OWN.items():
    entry = [{"name": NS + ":forge_master/" + f} for f in files]
    for e in entry:
        if key in FAR:
            e["attenuation_distance"] = FAR[key]
    sounds["entity.forge_master." + key] = {"sounds": entry, "subtitle": "subtitles." + NS + ".forge_master." + key}
for key, files in VOICE.items():
    sounds["entity.forge_master." + key] = {"sounds": [{"name": "minecraft:" + f, "pitch": 0.6} for f in files],
                                            "subtitle": "subtitles." + NS + ".forge_master." + key}
write(sounds_path, sounds)

FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


t("entity.echoes_fireforge.forge_master", "Le Maître de forge", "The Forge Master")
t("item.echoes_fireforge.forge_master_spawn_egg", "Œuf d'apparition du Maître de forge", "Forge Master Spawn Egg")
t("subtitles.echoes_fireforge.forge_master.ground_hit", "Le marteau du maître frappe le sol", "Forge Master's hammer hits the ground")
t("subtitles.echoes_fireforge.forge_master.hit", "Le marteau du maître frappe", "Forge Master's hammer strikes")
t("subtitles.echoes_fireforge.forge_master.swing", "Le marteau du maître fend l'air", "Forge Master's hammer swings")
t("subtitles.echoes_fireforge.forge_master.steam", "La vapeur siffle", "Steam hisses")
t("subtitles.echoes_fireforge.forge_master.step", "Pas lourds", "Heavy footsteps")
t("subtitles.echoes_fireforge.forge_master.death", "Le maître de forge s'effondre", "Forge Master collapses")
t("subtitles.echoes_fireforge.forge_master.ambient", "Le maître de forge râle", "Forge Master rattles")
t("subtitles.echoes_fireforge.forge_master.hurt", "Le maître de forge est blessé", "Forge Master hurts")
for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    current = read(path)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
