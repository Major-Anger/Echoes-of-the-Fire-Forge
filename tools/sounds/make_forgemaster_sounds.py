"""Convertit les sons du maître de forge d'Anger (plan, section 14) en .ogg mono pour le jeu.

Minecraft ne place dans l'espace que les sons mono : tout est remis en mono. Le silence de fin est coupé. Chaque
fichier de pas d'Anger en contient trois (à 0, 1 et 2 s) : on les découpe, un pas du jeu joue un seul pas. Les coups dans le vide (swing) sont mixés plus fort que les autres :
on les baisse un peu.

Usage : python3 tools/sounds/make_forgemaster_sounds.py fireforge/src/main/resources <dossier des sons d'Anger>
Les fichiers d'Anger restent hors du dépôt ; les sons convertis vont dans assets/echoes_fireforge/sounds/forge_master/.
Ensuite : python3 tools/data/make_forgemaster_data.py fireforge/src/main/resources (sounds.json et sous-titres).
"""
import os
import subprocess
import sys

RES, SRC = sys.argv[1], sys.argv[2]
DEST = os.path.join(RES, "assets", "echoes_fireforge", "sounds", "forge_master")

# nom dans le jeu -> fichier d'Anger, gain en dB
FILES = {
    "ground_hit1": ("Hammer-Ground-Hit.ogg", 0), "ground_hit2": ("Hammer-Ground-Hit2.ogg", 0),
    "ground_hit3": ("Hammer-Ground-Hit3.ogg", 0), "ground_hit4": ("Hammer-Ground-Hit4.ogg", 0),
    "hit1": ("Hammer-Mob-Hit.ogg", 0), "hit2": ("Hammer-Mob-Hit2.ogg", 0),
    "hit3": ("Hammer-Mob-Hit3.ogg", 0), "hit4": ("Hammer-Mob-Hit4.ogg", 0),
    "swing1": ("hammerSwing1.ogg", -5), "swing2": ("hammerSwing2.ogg", -5), "swing3": ("hammerSwing3.ogg", -5),
    "steam1": ("High_pressure.mp3", 0), "steam2": ("High_pressure2.mp3", 0),
    "death1": ("Maitre forge death.mp3", 0), "death2": ("Maitre forge death2.mp3", 0),
}

# les pas : fichier d'Anger -> début de chaque pas (s), chacun dure moins de 0,9 s
STEPS = [("Maitre forge footstep.mp3", (0.0, 1.0, 2.0)), ("Maitre forge footstep2.mp3", (0.0, 1.0, 1.95))]
TRIM = "areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,areverse"


def convert(src, name, gain=0, start=None):
    """Mono, gain, puis le silence de fin coupé (on retourne le son pour couper son début, et on le remet à l'endroit)."""
    cut = ["-ss", str(start), "-t", "0.9"] if start is not None else []
    subprocess.run(["ffmpeg", "-v", "error", "-y", *cut, "-i", os.path.join(SRC, src), "-ac", "1",
                    "-af", f"volume={gain}dB,{TRIM}", "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1",
                    os.path.join(DEST, name + ".ogg")], check=True)


os.makedirs(DEST, exist_ok=True)
for name, (src, gain) in FILES.items():
    convert(src, name, gain)
n = 0
for src, starts in STEPS:
    for start in starts:
        n += 1
        convert(src, f"step{n}", 0, start)
print(len(FILES) + n, "sons dans", DEST)
