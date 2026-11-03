"""Ajoute les musiques d'Anger (style vanilla, pour la diversité) aux listes de musiques du jeu, depuis le Core.

Les morceaux s'ajoutent aux musiques de Minecraft sans en retirer : le jeu les tire au sort parmi les siens, avec ses
silences et le curseur « Musique ». Décision d'Anger : partout dans l'Overworld (la liste générale et celles des biomes),
en survie, en créatif et au menu. Pas de disques. Ces musiques n'ont pas de rôle dans le lore.

Laissées de côté : la Deep Dark (le lieu des Veilleurs garde son ambiance), le dessous de l'eau, le Nether, l'End,
le générique.

Usage : python3 tools/music/make_music_data.py core/src/main/resources <dossier des .ogg> [nom=poids ...]
  ex. : python3 tools/music/make_music_data.py core/src/main/resources ~/musiques aube=2
Chaque fichier <nom>.ogg devient echoes_core:music/<nom> (poids 1 par défaut).
Chaque morceau a aussi son propre son, echoes_core:music.<nom> (assets/echoes_core/sounds.json), pour l'écouter avec
/playsound (ex. : /playsound echoes_core:music.cold_stone_descent music @s).
Volume : les morceaux d'Anger sont mixés plus fort que ceux du jeu (environ -13 LUFS contre -20) ; on les baisse dans
le jeu (VOLUME) plutôt que de toucher aux fichiers.
"""
import json
import os
import shutil
import sys

RES, SRC = sys.argv[1], sys.argv[2]
WEIGHTS = dict((a.split("=")[0], int(a.split("=")[1])) for a in sys.argv[3:])
NS = "echoes_core"
VOLUME = 0.4

EVENTS = ["music.game", "music.creative", "music.menu"] + ["music.overworld." + b for b in (
    "badlands", "bamboo_jungle", "cherry_grove", "desert", "dripstone_caves", "flower_forest", "forest",
    "frozen_peaks", "grove", "jagged_peaks", "jungle", "lush_caves", "meadow", "old_growth_taiga", "snowy_slopes",
    "sparse_jungle", "stony_peaks", "swamp")]

tracks = sorted(f[:-4] for f in os.listdir(SRC) if f.endswith(".ogg"))
if not tracks:
    sys.exit("aucun fichier .ogg dans " + SRC)
dest = os.path.join(RES, "assets", NS, "sounds", "music")
os.makedirs(dest, exist_ok=True)
for name in tracks:
    if not name.replace("_", "").isalnum() or name != name.lower():
        sys.exit(f"nom invalide : {name} (minuscules, chiffres et _ seulement)")
    shutil.copyfile(os.path.join(SRC, name + ".ogg"), os.path.join(dest, name + ".ogg"))

sounds = [{"name": f"{NS}:music/{n}", "stream": True, "volume": VOLUME, "weight": WEIGHTS.get(n, 1)} for n in tracks]
data = {event: {"replace": False, "sounds": sounds} for event in EVENTS}
path = os.path.join(RES, "assets", "minecraft", "sounds.json")
os.makedirs(os.path.dirname(path), exist_ok=True)
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
    f.write("\n")
# un son par morceau, pour /playsound (et un jour, peut-être, autre chose)
own = {f"music.{n}": {"sounds": [{"name": f"{NS}:music/{n}", "stream": True, "volume": VOLUME}]} for n in tracks}
path = os.path.join(RES, "assets", NS, "sounds.json")
with open(path, "w", encoding="utf-8") as f:
    json.dump(own, f, ensure_ascii=False, indent=2)
    f.write("\n")
print(len(tracks), "morceaux dans", len(EVENTS), "listes")
