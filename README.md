# Echoes : la série

Mods NeoForge pour Minecraft **1.21.1**. Chaque épisode raconte une partie de l'histoire et se joue seul ;
installés ensemble, ils se reconnaissent et débloquent du contenu croisé.

| Dossier | Mod | Identifiant | Rôle |
|---|---|---|---|
| `core/` | Echoes Core | `echoes_core` | Bibliothèque commune, toujours requise (comme YUNG's API). N'ajoute en jeu que la barre d'âmes et le livre guide. |
| `fireforge/` | Echoes of the Fire Forge | `echoes_fireforge` | Épisode. |
| *(dépôt à part)* | Echoes of the Silt | `echoes_silt` | Premier épisode. |

## Ce que le Core partage entre les épisodes

- **Les âmes** (`Souls`) : une seule jauge pour toute la série. Les bonus de taille sont rangés par source
  (`Souls.setBonus(joueur, id("eclats"), 10)`), donc deux épisodes ne s'écrasent jamais.
  Taille de base et bonus maximum réglables dans `config/echoes_core-common.toml`.
- **La barre d'âmes** (`SoulBar`) : dessinée par le Core ; un épisode peut la forcer à s'afficher
  (`SoulBar.showWhen`) ou dessiner autour (`SoulBar.decorate`).
- **Les Échos** (`Echoes`) : des succès qui laissent un bonus permanent. Chaque épisode enregistre les siens.
- **Le lore** (`Lore`) : ce que le joueur a lu, tous épisodes confondus (`echoes_silt:tablet/3`...).
- **Le livre guide** : un seul livre pour toute la série, donné à la première connexion. Chaque épisode y ajoute
  ses chapitres sans Java (voir plus bas). Chapitres cachés, pages « effacées » révélées par le lore, chapitres
  ou entrées qui n'apparaissent que si un autre épisode est installé.
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

## Le livre guide

Chaque épisode fournit ses fichiers dans `assets/<mod>/echoes_guide/` :

```
echoes_guide/book.json              les chapitres de l'épisode
echoes_guide/entries/<id>.json      une entrée = une suite de pages
```

`book.json` :
```json
{
  "order": 1,
  "intro": "guide.echoes_silt.intro",
  "categories": [
    { "id": "chronicles", "icon": "echoes_silt:watcher_tablet",
      "title": "guide.echoes_silt.cat.chronicles", "description": "guide.echoes_silt.cat.chronicles.desc",
      "hidden": true,
      "entries": ["chronicles/tablet_1", "chronicles/tablet_2"] }
  ]
}
```
- `order` : place de l'épisode dans le livre (1 = Silt, 2 = Forge...).
- `hidden` : chapitre caché tant que le joueur n'a lu aucune entrée de lore de cet épisode.
- `requires` (chapitre ou entrée) : `["echoes_silt"]` = n'apparaît que si ces mods sont installés.
- Un chapitre nommé avec l'identifiant complet d'un autre épisode (`"id": "echoes_silt:chronicles"`)
  ajoute ses entrées à ce chapitre.

Entrée :
```json
{ "title": "...", "icon": "echoes_silt:watcher_tablet",
  "lore": "echoes_silt:tablet/3", "label": "III",
  "pages": [ { "type": "text", "title": "...", "text": "..." } ] }
```
- `lore` : la page reste « effacée » tant que cette entrée de lore n'a pas été lue (`Lore.markRead`).
- `label` : affiché devant le titre (« III — Ce qui chante ») et sur la page effacée.
- Types de page fournis : `text`, `spotlight`, `crafting`, `smelting`, `entity`, `enchantment`, `image`.
  Un épisode ajoute les siens en Java avec `Guide.registerPageType` (ex. les runes du Chant),
  et peut transformer le texte avec `Guide.addTextFilter` (ex. la folie de Carcosa).
- `Guide.openLore(id)` ouvre le livre directement sur la page d'une tablette qu'on vient de lire.

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

Anger. Tous droits réservés.
