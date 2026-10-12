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
- Les Anciens Bâtisseurs se sont séparés en plusieurs familles : les Veilleurs
  (Deep Dark et Silt), les Oubliés (End), et les Attiseurs (Nether), qui ont bâti la Forge.
- Fire Forge : les machines des Attiseurs marchaient à la vapeur. Elles ont réchauffé
  le Nether et fait évaporer l'eau, qui a traversé le toit de bedrock.
- Une maladie a changé les Attiseurs en wither squelettes et les a chassés du Nether
  (les « noyés de cendre » de la tablette XIII du Silt). On ne dit jamais si elle vient
  de la pollution des machines ou d'autre chose.
- Les débris antiques sont des fossiles de machines, inutilisables. On ne les modifie
  pas ; le mod ajoute ses propres blocs de « machine comprimée ».

## Questions encore ouvertes (ne pas trancher sans Anger)
- D'où viennent les machines « qui jouaient faux » des hommes gris dans le Silt.
- Ce que devient l'eau évaporée, en jeu.
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
- Anger est le seul auteur de la série.

## Plan de conception
- Fire Forge : `docs/plan-fire-forge.md` (validé). Le relire avant de coder quoi que ce soit.

## État d'avancement (à tenir à jour)
- **Echoes Core** : fait (âmes et barre d'âmes, Échos, lore, livre guide, détection des épisodes).
- **Fire Forge**, étapes du plan :
  1. Découverte (machine comprimée, pièces, plans, génération, butin) : faite.
  2. La forge (bloc de 2, interface custom avec marteau, jauges, entonnoirs, enclume visible) : faite.
  3. Les raretés (composant `echoes_fireforge:forging`, chances affichées dans l'interface,
     attributs réels, résistance au feu, enchantement rare de l'offrande, infobulle) : faite.
  4. **Prochaine étape** : la fêlure (tags de tolérance par matériau, craquements pendant la
     reforge, réflexe en actionnant le soufflet, son/tremblement/étincelles, manqué = Fêlé).
     Voir la section 5 du plan. Ne pas commencer sans le feu vert d'Anger.
  5. Les modules (chaudière empilable, réservoir de lave, module des âmes avec le Silt, deux feux).
  6. Les effets des 8 Spéciaux (aujourd'hui tirés et affichés, sans effet), puis la liste « âme ».
  7. Le guide : reporté par Anger.
- Modèles et textures des modules déjà validés et présents (`models/block/module_*`),
  pas encore branchés dans le code.

## Points en attente (côté Anger)
- Les textes de lore des plans sont provisoires : Anger les écrira.
- Traduction anglaise « Stoker » pour Attiseur : à confirmer.
- Proposé, sans réponse : masquer les lignes d'attributs absolues de vanilla dans l'infobulle.
- Règle temporaire : ne pas toucher au guide du Core pour l'instant (sinon Anger devrait
  modifier son Silt tout de suite).

## Façon de travailler
- Écrire en français avec Anger. Branche de travail : `ccr-23832923-eezmsg`
  (`git push -u origin ccr-23832923-eezmsg`). Pas de PR sans demande.
- Commits signés `-c user.name="Anger" -c user.email="4nger.dud@gmail.com"`, un commit par étape.
- Textures : générées par les scripts Python/PIL de `tools/textures/`, présentées sur une
  planche d'aperçu (règles 4 et 5) avant intégration.
- Après chaque étape : une planche de captures envoyée à Anger.
- Le build doit rester sans avertissement (`-Xlint:deprecation`).
- **Tests** : copier le dépôt dans un dossier temporaire et y ajouter des classes de test
  (jamais dans le dépôt).
  - Serveur : une classe abonnée à `ServerStartedEvent` affiche `CHECK OK/FAIL` puis
    `server.halt(false)` ; lancer `./gradlew :fireforge:runServer`. FakePlayer pour le lore
    et les menus ; les progrès (Échos) demandent un vrai `ServerPlayer` avec une fausse connexion.
  - Client : `LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1280x800x24' ./gradlew :fireforge:runClient`
    avec `--quickPlaySingleplayer world` (monde créé par le serveur puis copié dans `run/saves/world`),
    `options.txt` avec `lang:fr_fr`, `guiScale:2`, `onboardAccessibility:false`. Un pilote sur
    `ClientTickEvent.Post` prend les captures (`Screenshot.grab`) ; la souris se déplace par
    réflexion sur les champs `xpos`/`ypos` de `MouseHandler`.
