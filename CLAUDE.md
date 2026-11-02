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
- Les forteresses du Nether sont des ouvrages des Attiseurs ; les wither squelettes (les Attiseurs changés
  par la maladie) y sont restés (décision d'Anger, 12b).
- Les débris antiques sont des fossiles de machines, inutilisables. On ne les modifie
  pas ; le mod ajoute ses propres blocs de « machine comprimée ».

## Questions encore ouvertes (ne pas trancher sans Anger)
- D'où viennent les machines « qui jouaient faux » des hommes gris dans le Silt.
- Ce que devient l'eau évaporée, en jeu. Piste d'Anger (pas tranchée) : les monuments aquatiques et
  constructions sous l'eau étaient autrefois à la surface ; l'eau du Nether serait remontée dans l'Overworld ?
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
- NeoForge 1.21.1, textes du jeu en français et en anglais. Attiseur = « Stoker », Veilleur = « Watcher »,
  Cendreux = « Ashen » en anglais (validés).
- Anger est le seul auteur de la série.

## Plan de conception
- Fire Forge : `docs/plan-fire-forge.md` (validé). Le relire avant de coder quoi que ce soit.

## État d'avancement (à tenir à jour)
- **Echoes Core** (version 0.1.1) : fait (âmes et barre d'âmes, Échos, lore, livre guide, détection des
  épisodes). Config client `echoes_core-client.toml` : `soulBar.scale` (0,75 à 2, 1 par défaut) ; la
  bannière garde son bord droit à 116 px à gauche du milieu de l'écran et son bord bas à 8 px du bas
  (sa place d'avant, à l'échelle 1,5), et reste toujours entière à l'écran.
- **Fire Forge**, étapes du plan :
  1. Découverte (machine comprimée, pièces, plans, génération, butin) : faite.
  2. La forge (bloc de 2, interface custom avec marteau, jauges, entonnoirs, enclume visible) : faite.
  3. Les raretés (composant `echoes_fireforge:forging`, chances affichées dans l'interface,
     attributs réels, résistance au feu, enchantement rare de l'offrande, infobulle) : faite.
  4. La fêlure (tolérance par matériau, craquements, réflexe au soufflet dans l'interface ou
     dans le monde, vapeur qui coûte de l'eau à vide, bouclier reforgeable) : faite.
  5. Les modules, en deux temps :
     - 5a, chaudière empilable et réservoir de lave (réserves, seaux, tuyaux, jauge de lave,
       API `ForgeModules` pour les autres mods) : faite ;
     - 5b, le module des âmes avec le Silt (feu des âmes et foyer bleu, verrouiller, garantir,
       relancer, panneau des âmes) : faite.
  6. Les Spéciaux, en deux temps :
     - 6a, les effets des 8 Spéciaux de base (`forging/SpecialEffects`, brouillard des Lunettes
       dans `client/SpecialFog`) : faite ;
     - 6b, la liste « âme » : la mécanique est faite, avec un seul Spécial (Lame des Veilleurs) ;
     - 6c, 6d, 6e : la liste âme d'Anger, **plan validé** (plan, section « Suite de la liste
       âme ») ; 6c (arc, arbalète, trident, masse reforgeables ; Spéciaux par type d'arme,
       `forging/WeaponType`) : faite ; 6d (Écho de lame, Carreau muet, Harpon des rivières, Glas,
       Pas feutré, Regard du Veilleur, dans `forging/SoulSpecialEffects`) : faite ; 6e (Urne close,
       Fendeuse de racines, Corde chantante, dans `forging/SiltSpecialEffects` ; événements du Core
       `SoulChangeEvent` et `Noise`/`NoiseEvent`) : faite. L'étape 6 est finie.
       Ne pas inventer d'autres Spéciaux âme sans Anger.
  7. Le guide : fait (chapitres « La Forge » et « Carnets des Attiseurs », caché, une page effacée
     par plan ; fichiers de l'épisode seulement, guide du Core intact ; textes générés par
     `tools/guide/make_guide.py`, à relancer après chaque retouche). Lore des plans provisoire.
  8. L'Établi d'Attiseur et les mécanismes (idée d'Anger, plan section 11, validé), en cinq temps :
     8a (établi, Plan de l'Établi, greffe, gabarits, butin, duplication) : faite ; 8b (bouilleur,
     recharge, barre de vapeur) : faite ; 8c (les 12 mécanismes d'armure, touches C, B, N) : faite ;
     8d (mécanismes d'arme, clic droit, `mechanism/WeaponMechanisms`) : faite ; 8e (Lampe à vapeur,
     `mechanism/SteamLamp`, faisceau `client/LampBeamRender`, guide) : faite. L'étape 8 est finie.
     Shader d'Anger : pack Iris (zip, licence « tous droits réservés » : jamais dans le dépôt) ; le
     contrat (plan, 8e) : bloc `echoes_fireforge:lamp_light` dans son `block.properties`, faisceau rendu
     comme un faisceau de balise (`gbuffers_beaconbeam`). Anger ajoutera aussi la forge et le bouilleur.
  9. Après la v1 (plan, section 12) : ordre validé 12a, puis 12c, puis 12b.
     12a (succès et Échos, `progress/ForgeEchoes`, `tools/data/make_echoes_data.py`) : faite.
     12c (Cendreux, `cendreux/`, aussi rarement dans les forteresses ; leur butin : le Lit de cendre, état `ash`
     de la forge, rareté minimum selon l'offrande ; succès « Les noyés de cendre » ; `tools/data/make_cendreux_data.py`,
     `tools/textures/make_cendreux_textures.py`) : faite. 12b (structures : forge en ruine et ses lieux, dernier
     foyer, chaufferie et sa salle scellée, forge des forteresses, cheminées du plafond ; `structure/`,
     `registry/FFStructures`, `tools/structures/make_structures.py`, `tools/data/make_structures_data.py`) : faite.
     La porte scellée est un bloc technique (`sealed_machine`, sans objet) : la redstone vanilla ne sait pas ouvrir
     une porte de 3 sur 3.
  10. Le set du Wither (idée d'Anger, plan section 13) : **validé** (palier 2 : les animaux fuient ; pas de
      retour une fois au palier 4 ; textures validées ; double de 2,4 blocs). 13a (set, recette, progression
      cachée et ses signes, `witherset/`, `tools/data/make_wither_set_data.py`,
      `tools/textures/make_wither_set_textures.py`, commande d'essai `/witherset`) : faite.
      13b (défauts et bonus des paliers, `witherset/WitherStages`) : faite. 13c (Lien de cendre au palier 4,
      compte à rebours de 30 min, mort par la cendre, double `witherset/AshDoubleEntity`) : faite. Le set est fini,
      testé en jeu par Anger : tout fonctionne. La commande d'essai `/witherset` est encore là (à retirer avant la
      sortie ? à demander à Anger).

## Points en attente (côté Anger)
- Les textes de lore des plans sont provisoires : Anger les écrira.
- Dépôt du Silt : sources présentes depuis le commit `77e1de5` (vérifié en lecture seule) :
  `echoes_silt:soul_crystal` et `echoes_silt:resonant_crystal` existent (recette du module des
  âmes bonne), tag `echoes_silt:biomass` présent, le Silt utilise les âmes du Core
  (`fr.anger.echoes.core`, dépendance Maven locale `fr.anger.echoes:echoes_core:0.1.0`, tâche
  `corepublish`), point d'entrée de la Gueule `SiltRules.noise(Player, float)`. La 6e est donc
  débloquée. Testé en jeu par Anger avec les deux épisodes chargés : tout fonctionne.
- Branchement du Silt sur les événements de la 6e (fait par Anger, voir le plan, « À faire côté
  Silt ») : `Souls.lose` pour les pertes passives, `Souls.spend` pour les armes, bruit par `Noise.make`
  et Gueule remplie depuis `NoiseEvent.Post`. En attendant, l'Urne close ne réduit aucune perte du Silt.
- Core 0.1.1 : le Silt dépend encore de `core_version=0.1.0` (son `gradle.properties`) ; Anger (ou
  Claude dans Cowork) doit le passer à 0.1.1 et relancer corepublish sur sa machine.
- Le Silt (`origin/main`, mod_id `echoes_silt`) range ses tablettes dans le lore du Core
  (`echoes_silt:tablet/1` à `/15`). Attention : le clone local `/home/user/echoes-of-the-silt` est resté sur
  un ancien historique (`876ca42`, encore `smalladditions`) ; lire le Silt avec `git show origin/main:...`
  après `git fetch` (lecture seule, sans toucher au clone).
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
