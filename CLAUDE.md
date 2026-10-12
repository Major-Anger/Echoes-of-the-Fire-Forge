# Série Echoes : règles pour Claude

## Règles de la série (à respecter dans tout le code, les textes et les designs)
1. On ne décrit jamais le Sift. On en parle par ses effets, jamais par ce qu'il est.
2. On ne tranche jamais entre deux hypothèses de lore. Le joueur doit pouvoir hésiter.
3. Chaque ajout a une raison dans l'histoire. Pas de contenu « juste pour le fun ».
4. Designs et textures 100 % originaux (Mojang autorisé seulement dans les aperçus).
5. Toujours un aperçu ou un plan avant de coder, puis attendre la validation d'Anger.

## Décisions de lore
- A et B sont les deux dimensions : le Sift (annoncé par Mojang) et le Silt.
  On ne dit jamais laquelle est la « bonne ». Le Chant ouvre la bonne, le Contre-chant
  la mauvaise, sans jamais dire laquelle est laquelle. Rien ne doit faire pencher le joueur.
- Le lore de toute la série s'inspire de la théorie des Anciens Bâtisseurs :
  ils expliquent les constructions anciennes et la perte des connaissances.
- Les hommes gris sont les Illagers.

## Questions encore ouvertes (ne pas trancher sans Anger)
- Qui a construit les machines de la Forge : un autre peuple, les hommes gris,
  ou une origine commune qui s'est séparée.
- Le branchement sur le Sift de Mojang : prévu, mais il dépendra de leur mécanique
  d'ouverture. Garder la destination du Chant et du Contre-chant facile à changer.

## Organisation technique
- Echoes Core (`echoes_core`) : bibliothèque commune, toujours requise
  (âmes, Échos, lore, livre guide, détection des épisodes).
- Épisodes : Echoes of the Silt (`echoes_silt`, dépôt à part), Echoes of the Fire Forge
  (`echoes_fireforge`). Chacun doit être jouable avec seulement le Core.
- Le contenu croisé vit dans l'épisode le plus récent (la Forge connaît le Silt, pas l'inverse).
- Le Core sera sorti dans son propre dépôt avant la sortie publique.
- Ne jamais modifier le dépôt Echoes of the Silt sans demande explicite (lecture seule).
- NeoForge 1.21.1, textes du jeu en français et en anglais.
