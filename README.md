# Echoes : la série

Mods NeoForge pour Minecraft **1.21.1**. Chaque épisode raconte une partie de l'histoire et se joue seul ;
installés ensemble, ils se reconnaissent et débloquent du contenu croisé.

| Dossier | Mod | Identifiant | Rôle |
|---|---|---|---|
| `core/` | Echoes Core | `echoes_core` | Bibliothèque commune, toujours requise (comme YUNG's API). N'ajoute rien de visible à part la barre d'âmes. |
| `fireforge/` | Echoes of the Fire Forge | `echoes_fireforge` | Épisode. |
| *(dépôt à part)* | Echoes of the Silt | `echoes_silt` *(à confirmer)* | Premier épisode. |

## Ce que le Core partage entre les épisodes

- **Les âmes** (`Souls`) : une seule jauge pour toute la série. Les bonus de taille sont rangés par source
  (`Souls.setBonus(joueur, id("eclats"), 10)`), donc deux épisodes ne s'écrasent jamais.
  Taille de base et bonus maximum réglables dans `config/echoes_core-common.toml`.
- **La barre d'âmes** (`SoulBar`) : dessinée par le Core ; un épisode peut la forcer à s'afficher
  (`SoulBar.showWhen`) ou dessiner autour (`SoulBar.decorate`).
- **Les Échos** (`Echoes`) : des succès qui laissent un bonus permanent. Chaque épisode enregistre les siens.
- **Le lore** (`Lore`) : ce que le joueur a lu, tous épisodes confondus (`echoes_silt:tablet/3`...).
- **Des événements** (`LoreReadEvent`, `EchoAwakenedEvent`) : un épisode réagit à ce qui se passe dans un autre
  sans dépendre de son code.
- **La détection des épisodes** (`EchoesMods.isLoaded`).

## Règles de la série

1. Chaque épisode fonctionne avec seulement le Core.
2. Le contenu croisé vit dans l'épisode le plus récent : la Forge connaît le Silt, pas l'inverse.
3. Le code qui touche à un autre épisode est isolé (ex. `fireforge/.../compat/SiltCompat.java`) et n'est appelé
   que si l'épisode est installé.
4. Recettes, loot, etc. croisés : une condition dans le JSON, sans Java :
   ```json
   "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "echoes_silt" } ]
   ```

## Compiler et lancer

Il faut un JDK 21.

```
gradlew build                    # les .jar sortent dans core/build/libs et fireforge/build/libs
gradlew :fireforge:runClient     # lance le jeu avec le Core + la Forge
gradlew :core:publishToMavenLocal  # rend le Core disponible pour un autre projet (le Silt)
```

Dans un autre projet (le Silt par exemple), après `publishToMavenLocal` :

```gradle
repositories { mavenLocal() }
dependencies { implementation "fr.anger.echoes:echoes_core:0.1.0" }
```

## Auteurs

Anger, SpeedY, Helnyxie. Tous droits réservés.
