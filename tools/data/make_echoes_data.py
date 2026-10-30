"""Génère les succès de la Forge et les textes de leurs Échos (étape 12a) : data/echoes_fireforge/advancement/echoes/*.json
et les textes (fr_fr / en_us).

Usage : python3 tools/data/make_echoes_data.py fireforge/src/main/resources
Les succès sont accordés par le code (progress/ForgeEchoes.java), sauf la racine : trouver une pièce de machine.
"""
import json
import os
import sys

RES = sys.argv[1]
NS = "echoes_fireforge"
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data", NS, "advancement", "echoes")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en


# nom, parent, icône, Écho ?, titre fr, titre en, description fr, description en, texte de l'Écho fr, en
ECHOES = [
    ("first_reforge", "root", NS + ":fire_forge", False,
     "Le feu reprend", "The fire takes again",
     "Reforger un premier objet à la Forge des Attiseurs.", "Reforge a first item at the Stokers' Forge.", None, None),
    ("hand", "first_reforge", "minecraft:netherite_sword", True,
     "Main d'Attiseur", "Stoker's Hand",
     "Sortir un Spécial de la forge. Écho : tes mains comptent comme un plan lu de plus.",
     "Bring a Special out of the forge. Echo: your hands count as one more plan read.",
     "Tes mains se souviennent : à la forge, elles comptent comme un plan lu de plus.",
     "Your hands remember: at the forge, they count as one more plan read."),
    ("breath", "first_reforge", NS + ":machine_valve", True,
     "Le souffle juste", "The right breath",
     "Rattraper 10 fêlures au soufflet. Écho : un quart de seconde de plus pour réagir.",
     "Save 10 cracks with the bellows. Echo: a quarter of a second more to react.",
     "Ton souffle est plus sûr : un quart de seconde de plus pour rattraper une fêlure.",
     "Your breath is surer: a quarter of a second more to save a crack."),
    ("notebooks", "root", NS + ":attiseur_plan", True,
     "Carnets complets", "Complete notebooks",
     "Lire tous les plans. Écho : +2 % de chance d'enchantement rare à l'offrande.",
     "Read every plan. Echo: +2% chance of a rare enchantment from the offering.",
     "Les carnets te parlent : +2 % de chance d'enchantement rare à l'offrande.",
     "The notebooks speak to you: +2% chance of a rare enchantment from the offering."),
    ("workbench", "root", NS + ":workbench", False,
     "L'établi", "The workbench",
     "Greffer un premier mécanisme à l'Établi d'Attiseur.", "Fit a first mechanism at the Stoker's Workbench.", None, None),
    ("steam", "workbench", NS + ":steam_boiler", True,
     "Vapeur domptée", "Tamed steam",
     "Recharger 200 charges au bouilleur. Écho : les pièces que tu recharges tiennent 22 charges.",
     "Recharge 200 charges at the boiler. Echo: the pieces you recharge hold 22 charges.",
     "La vapeur t'obéit : les pièces que tu recharges tiennent 22 charges.",
     "Steam obeys you: the pieces you recharge hold 22 charges."),
]


def display(name, icon, frame, toast=True, background=None):
    d = {
        "icon": {"id": icon},
        "title": {"translate": "advancement.%s.%s.title" % (NS, name)},
        "description": {"translate": "advancement.%s.%s.description" % (NS, name)},
        "frame": frame,
        "show_toast": toast,
        "announce_to_chat": toast,
        "hidden": False,
    }
    if background:
        d["background"] = background
    return d


# la racine : une pièce de machine (ou une machine comprimée) en main
write(os.path.join(D, "root.json"), {
    "display": display("root", NS + ":compressed_machine", "task", False, NS + ":textures/block/compressed_machine_side.png"),
    "criteria": {"machine": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [
        NS + ":compressed_machine", NS + ":machine_gear", NS + ":machine_valve", NS + ":engraved_plate"]}]}}},
})
t("advancement.%s.root.title" % NS, "Echoes of the Fire Forge", "Echoes of the Fire Forge")
t("advancement.%s.root.description" % NS, "Les machines des Attiseurs dorment dans la pierre.", "The Stokers' machines sleep in the stone.")

for name, parent, icon, echo, title_fr, title_en, desc_fr, desc_en, echo_fr, echo_en in ECHOES:
    data = {
        "display": display(name, icon, "goal" if echo else "task"),
        # accordé par le code (ForgeEchoes)
        "criteria": {"granted": {"trigger": "minecraft:impossible"}},
        "parent": "%s:echoes/%s" % (NS, parent),
    }
    if echo:
        data["rewards"] = {"experience": 60}
    write(os.path.join(D, name + ".json"), data)
    t("advancement.%s.%s.title" % (NS, name), title_fr, title_en)
    t("advancement.%s.%s.description" % (NS, name), desc_fr, desc_en)
    if echo:
        t("echo.%s.%s" % (NS, name), echo_fr, echo_en)

# ---------------------------------------------------------------------- textes
for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(A, "lang", lang + ".json")
    with open(path, encoding="utf-8") as f:
        current = json.load(f)
    current.update(data)
    write(path, current)
print(len(FR), "clés")
