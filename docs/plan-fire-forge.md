# Echoes of the Fire Forge : plan v1 (validé par Anger)

Référence de conception. Toute modification passe par Anger (voir CLAUDE.md, règle 5).

## Lore (rappel)
Les **Attiseurs**, une famille des Anciens Bâtisseurs, ont fait du Nether un monde-forge
à vapeur. La dimension s'est réchauffée et l'eau s'est évaporée à travers le toit de bedrock.
Une maladie les a changés en wither squelettes ; on ne dit jamais si elle vient des machines.
Les débris antiques sont des fossiles de leurs machines, inutilisables (on ne les modifie pas).

## 1. Découverte
- **Machine comprimée** : bloc rare, placé comme les débris antiques mais avec **un bloc de moins
  par filon** (grands filons : 2 au plus, entre Y 8 et 24 ; petits filons : 1, sur toute la hauteur
  du Nether), jamais au contact de l'air. Pioche en diamant au minimum ; toucher de soie : le bloc.
  Mesuré : environ 1,0 par chunk (débris : 1,7).
- En la minant : **1 pièce de machine** au hasard (engrenage, soupape, plaque gravée) et
  **15 % de chance d'un plan** : le Plan de la Forge (40 % des plans) ou un Plan d'Attiseur I à V.
- Les plans ne sont pas consommés quand on les lit (clic droit). Textes de lore provisoires :
  à écrire par Anger.
- **Plans** : lus, ils sont enregistrés dans le lore du Core (`echoes_fireforge:plan/…`),
  révèlent une page effacée du guide, et chaque plan lu **améliore les chances de rareté**
  (le joueur « imagine » la technique et la maîtrise peu à peu).
- **Recette de la forge** (étape 2) : seau vide, plaque gravée, cuivre / soupape, bloc de fer,
  engrenage / cuivre, Plan de la Forge (consommé), cuivre.

## 2. La forge et ses modules
- **Forge** : 2 blocs de long, une petite jauge d'eau. L'avant porte l'interface.
  - le **foyer** (l'ouverture du feu) est **centré sur toute la face avant**, à cheval
    sur les deux blocs ;
  - la **cheminée** (environ 1,9 bloc de haut, validée telle quelle, même si la
    chaudière pleine la dépasse) ;
  - le **soufflet** sur la face avant, à gauche du foyer : on l'actionne pour le réflexe
    de la fêlure (section 5) ;
  - l'**enclume** sur le dessus, où l'objet en cours de reforge est visible.
- **Trois côtés à modules**, un module par côté :
  - **gauche** : la **chaudière** (agrandit la réserve d'eau), empilable jusqu'à
    **3 blocs de haut** au total ; **4 seaux par bloc** (12 au maximum) ;
  - **arrière** : le **réservoir de lave** (alimente la chaleur Fournaise), jusqu'à
    **2 blocs de haut** ; **5 seaux par bloc** (10 au maximum) ;
  - **droite** : le **module d'amélioration** (Forge des âmes, avec le Silt).
- Chaque bloc ajouté à une réserve ajoute sa propre capacité.
- Chaque côté accepte un module parmi plusieurs : d'autres mods pourront ajouter leurs
  propres modules sans modifier la forge.
- **Codé (étape 5a)** :
  - la chaudière se pose contre le côté gauche de la moitié du foyer, le réservoir derrière l'une
    ou l'autre moitié (un seul compte) ; un module ailleurs ne fait rien ; il prend l'orientation
    de la forge, et la pile choisit ses modèles (bas avec manomètre, milieu, haut avec soupape) ;
  - réserves : forge seule 1 seau d'eau et 1 seau de lave ; chaudière +4 seaux d'eau par bloc
    (13 au plus) ; réservoir +5 seaux de lave par bloc (11 au plus avec le foyer) ;
  - remplissage : clic droit avec un seau ou une fiole sur le module (un seau ne se vide pas à
    moitié), seau de lave posé en combustible versé dans la réserve, tuyaux d'autres mods
    (capacité de fluide NeoForge, remplissage seulement) ;
  - **le combustible posé passe avant la lave** : la réserve de lave ne sert que si la case
    combustible est vide ;
  - un module cassé : la réserve rétrécit, le surplus est perdu (vapeur, ou fumée pour la lave) ;
  - recettes : chaudière = cuivre partout, soupape au centre, seau en bas au milieu ;
    réservoir = fer, verre de chaque côté, soupape au centre, plaque gravée en bas au milieu ;
  - pour les autres mods : `ForgeModules.register(bloc, new ForgeModules.Type(côté, hauteur,
    eau par bloc, lave par bloc))`.

## 3. Fonctionnement
- **Objet à reforger** : à la main uniquement (pas d'entonnoir).
- **Offrande** : à la main uniquement, consommée à chaque reforge.
- **Combustible** et **eau** (seau ou fiole) : à la main ou par entonnoir.
  La chaudière est fermée : elle fonctionne dans le Nether.
- **Récipients vides** (seaux, fioles) : case de sortie dédiée, vidable par un entonnoir
  en dessous.
- **Consommation** : 1 combustible par reforge, 1 seau de lave pour 4 reforges,
  environ 10 secondes par reforge.
- La chaleur est celle du combustible **en train de brûler**, affichée par une jauge.
- Reforger **remplace** la rareté précédente.
- **Interface** (étape 2, codée) : fer sombre et laiton ; un **marteau** à la place d'un bouton
  lance la reforge ; jauges d'eau et de chaleur, panneau d'état (ce qui manque) ; l'objet en cours
  est posé sur l'enclume. Entonnoirs : combustible par le dessus, eau par les côtés, récipients
  vides par le dessous.
- **Réserve d'eau de la forge seule : 1 seau** ; une reforge consomme une fiole (¼ de seau).
- **Reforgeable pour l'instant** : épées, haches, armures, bouclier (tag `echoes_fireforge:reforgeable`).

## 4. La chaleur (fixe la rareté maximum)
| Combustible | Chaleur | Rareté max |
|---|---|---|
| Bois, planches | Tiède | Brut |
| Charbon, charbon de bois | Chaud | Trempé |
| Bloc de charbon | Ardent | Affiné |
| Bâton de blaze | Brûlant | Attisé |
| Lave | Fournaise | Spécial possible |

Un tirage au-dessus du plafond **redescend au plafond**.

## 5. Trop chaud, ça fend
| Matériau | Supporte jusqu'à |
|---|---|
| Cuir, bois, or | Chaud |
| Pierre, mailles, fer, bouclier | Ardent |
| Diamant | Brûlant |
| Netherite | tout |

- **Mécanique de réflexe** : pour chaque palier de chaleur au-delà de ce que supporte
  le matériau, la forge **craque une fois** pendant la reforge, à un moment aléatoire
  (son de craquement, la forge tremble, étincelles). Le joueur a une courte fenêtre pour
  **actionner le soufflet**. Réussi : ce risque est écarté. Manqué : l'objet sort **Fêlé**.
- Le Fêlé « de base » (mauvais tirage, 20 %) reste possible indépendamment.
- **Fêlé n'abîme pas l'objet** : seulement un malus de statistique, pas de perte de durabilité.
- **Objets d'autres mods** : tolérance rangée dans des tags ; à défaut, déduite de
  l'ingrédient de réparation ; sinon « jusqu'à Ardent ».
- **Armes et armures particulières** (arcs, arbalètes, trident, masse, élytres,
  carapace de tortue…) : au cas par cas. Le bouclier est traité comme du fer.
- **Chiffres codés (étape 4)** :
  - chaque palier au-dessus de la tolérance = 1 craquement (3 au plus), à un moment au hasard
    entre la 2e et la 8e seconde, au moins 2 s entre deux ; **1,5 s** pour réagir ;
  - le soufflet s'actionne dans l'interface (sous le combustible) ou d'un clic droit sur le
    soufflet de la forge ; il souffle de la **vapeur** : un coup qui ne rattrape rien coûte
    **50 mB d'eau**, sans 50 mB d'eau il ne souffle pas ; 0,3 s pour se regonfler ;
  - un seul raté suffit : l'objet sortira Fêlé, et les craquements suivants n'ont plus lieu ;
  - tags `echoes_fireforge:tolerance/chaud|ardent|brulant|fournaise`, puis l'ingrédient de
    réparation (cuir, planches, or : Chaud ; pierre, fer : Ardent ; diamant : Brûlant ;
    netherite : tout), sinon Ardent ;
  - le volet des chances annonce « Fêlures : n » ; quand ça craque, le cadre de l'interface
    rougit, des étincelles jaillissent, l'objet tremble sur l'enclume, et la vapeur sort de
    la buse du soufflet vers le foyer.

## 6. L'offrande
| Offrande | Coup de pouce |
|---|---|
| Pépite d'or, quartz | petit |
| Crème de magma, larme de ghast | bon |
| Fragment de netherite | gros |

Elle améliore les chances de rareté et donne une petite chance d'**enchantement rare**
(compatible avec l'objet, sans enchantement « trésor » ni malédiction). Plus l'offrande est
chère, plus la chance est haute.

## 7. Les raretés
| Rareté | Bonus | Malus / contrepartie | Chance de départ |
|---|---|---|---|
| Fêlé | — | 1 malus de −2 à −6 % | 20 % |
| Brut | — | — | 35 % |
| Trempé | 1 bonus de +1 à +3 % | — | 25 % |
| Affiné | 2 bonus de +2 à +6 % | — | 14 % |
| Attisé | 2 ou 3 bonus de +4 à +9 % | 1 contrepartie de −3 à −6 % | 5 % |
| Spécial | 1 effet unique | 1 contrepartie | 1 % |

- Valeurs tirées au hasard dans leur fourchette, **en pourcentage de la valeur propre de
  l'objet** (+6 % de dégâts sur une épée de 7 = +0,42). L'infobulle affiche les pourcentages.
- **Armes** : dégâts, vitesse d'attaque, portée, recul.
- **Armures** : armure, robustesse, résistance au recul, vitesse, résistance au feu et à la lave.
- **Bouclier** (main secondaire) : robustesse, résistance au recul, vitesse. Aucun Spécial ne
  lui va pour l'instant : un tirage Spécial devient Attisé.
- **Ordre des calculs** : chances de départ → plans lus → offrande → plafond de chaleur →
  risque de fêlure (réflexe). Les chances s'affichent dans l'interface de la forge.
- Compatible avec les enchantements, conservée lors du passage à la netherite.
- **Chiffres codés (étape 3)** : chaque plan lu fait passer **2 %** des raretés basses (Fêlé, puis
  Brut) vers les hautes, en proportion de leurs chances de départ ; l'offrande fait passer
  **4 / 8 / 14 %** (petit / bon / gros coup de pouce) et donne **3 / 7 / 15 %** de chance d'enchantement
  rare (poids 1 ou 2 dans le jeu, compatible, ni trésor ni malédiction). Les statistiques
  s'appliquent comme de vrais attributs ; la résistance au feu et à la lave réduit ces dégâts.
  Les Spéciaux sont tirés et affichés ; leurs effets arrivent à l'étape 6.

### Spéciaux (liste de base)
| Nom | Pièce | Effet | Contrepartie | Raison dans l'histoire |
|---|---|---|---|---|
| Soupape | arme | chaque coup lâche un jet de vapeur qui aveugle 1 s | après plusieurs coups rapides, brûle son porteur | les soupapes de sécurité des chaudières |
| Marteau-pilon | arme | un coup sur trois frappe deux fois plus fort, avec recul | vitesse d'attaque réduite | les marteaux à vapeur des forges |
| Montée en pression | arme | chaque coup sur la même cible augmente la vitesse d'attaque (plafonnée) | changer de cible ou s'arrêter fait tout retomber, avec un court ralentissement | une machine à vapeur monte en pression avant sa pleine force |
| Chaudière | plastron | dégâts de feu et de lave fortement réduits | s'éteint dans l'eau ou sous la pluie : lenteur | les Attiseurs travaillaient au cœur du feu |
| Pas de fondeur | jambières | aucun dégât des blocs de magma, on avance mieux dans la lave | plus lent sur la terre ferme | ils marchaient au bord des bassins de fonte |
| Soufflet | bottes | accroupi + saut = poussée de vapeur vers le haut | les bottes s'usent vite | les soufflets de forge |
| Lunettes d'Attiseur | casque | on voit clair dans la lave | vision réduite en plein jour, hors du Nether | leurs lunettes de forge |
| Linceul de cendre | toute armure | immunité à l'effet Wither | régénération naturelle ralentie | protection contre les « noyés de cendre » (sans dire d'où vient la maladie) |

### Effets codés (étape 6a)
| Spécial | Chiffres |
|---|---|
| Soupape | joueur touché : Cécité 1 s ; monstre : perd sa cible, Lenteur II et Cécité 1 s ; 4 coups en moins de 3 s : feu 2 s pour le porteur |
| Marteau-pilon | un coup sur trois ×2 avec recul ; vitesse d'attaque −10 % (dans les attributs de l'arme) |
| Montée en pression | +5 % de vitesse d'attaque par coup sur la même cible, 25 % au plus ; autre cible, 3 s sans frapper ou arme rangée : tout retombe, Lenteur 1 s |
| Chaudière | feu et lave −50 % (avant l'armure) ; mouillée : Lenteur I, et encore 5 s après |
| Pas de fondeur | aucun dégât des blocs de magma ; ~40 % plus vite dans la lave ; −6 % de vitesse hors de la lave |
| Soufflet | accroupi + saut : environ 3 blocs de haut ; 2 s de recharge ; 4 points d'usure |
| Lunettes d'Attiseur | lave : 12 blocs de vue ; plein jour à ciel ouvert hors du Nether : brouillard −40 % |
| Linceul de cendre | immunité au Wither ; la faim soigne deux fois moins (Régénération et Soin instantané inchangés) |

### Liste âme (étape 6b, provisoire)
Le feu des âmes tire d'abord dans la liste âme, s'il y a un Spécial âme pour la pièce, sinon dans
la liste de base. Relancer un Spécial âme reste dans la liste âme. Anger complétera la liste ; pour
l'instant, un seul Spécial valide la mécanique :

| Nom | Pièce | Effet | Contrepartie |
|---|---|---|---|
| Lame des Veilleurs | arme | chaque ennemi tué rend 1 âme | −10 % de dégâts en plein jour (les Veilleurs vivent dans le noir) |

## 8. Avec le Silt : la Forge des âmes résonnantes
- **Étape 5b (codée)** : panneau des âmes à gauche de la forge ; le choix du feu reste d'une
  reforge à l'autre, les options se décochent après chaque reforge ; Verrouiller et Garantir se
  cumulent, Relancer se cumule avec Verrouiller (Garantir grisé) ; on ne verrouille qu'un bonus,
  gardé même si le tirage sort Brut ou Fêlé ; les âmes se paient au coup de marteau (sinon
  « Pas assez d'âmes ») ; au feu des âmes, les Spéciaux viennent de la liste de base jusqu'à
  l'étape 6 ; l'Âmacier du Silt tient jusqu'à Brûlant (tag), les autres armes du Silt suivent
  leur matériau. Détail du plan validé : feu des âmes = 10 âmes par reforge, foyer
  bleu ; au feu normal, options payées en âmes : verrouiller un bonus (15), garantir (pas de Fêlé
  ni de Brut au tirage, une fêlure ratée fend quand même : 20), relancer (garde la rareté, retire
  les valeurs, sans combustible ni offrande : 10). Recette du module : cristal résonnant en haut,
  cristal des âmes / catalyseur de sculk / cristal des âmes au milieu, cuivre / plaque gravée /
  cuivre en bas ; le bloc existe toujours, la recette seulement avec le Silt.
- **Module de droite**, fabriqué avec `echoes_silt:soul_crystal`, `echoes_silt:resonant_crystal`
  et `minecraft:sculk_catalyst`.
- **À chaque reforge, le joueur choisit son feu :**

| | Feu normal | Feu des âmes |
|---|---|---|
| Rareté max | fixée par la chaleur | la même |
| Fêlure si trop chaud | oui (réflexe) | jamais (flamme froide) |
| Verrouillage d'un bonus (en âmes) | oui | non : tout ou rien |
| Spéciaux | liste de base | liste à part, version âme (à définir) |
| Coût en âmes | seulement pour verrouiller, relancer ou garantir un minimum | à chaque reforge |

## 9. Guide
- Chapitre « La Forge » : fonctionnement, chaleur, raretés, offrandes.
- Chapitre caché « Carnets des Attiseurs » : les plans, en pages effacées.
- Fichiers de l'épisode uniquement (`assets/echoes_fireforge/echoes_guide/`),
  sans toucher au guide du Core.

## 10. Hors v1
Créatures (les Cendreux : Attiseurs à mi-chemin de la maladie), structures, Échos,
objets particuliers au cas par cas, liste des Spéciaux version âme, ce que devient l'eau.

## Textures et modèles
Premières versions originales dessinées par Claude, présentées sur une planche d'aperçu
avant toute intégration (règles 4 et 5).
