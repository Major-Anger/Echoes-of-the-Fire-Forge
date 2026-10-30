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
- **Reforgeable** : épées, haches, armures, bouclier, et depuis l'étape 6c arc, arbalète, trident, masse
  (tag `echoes_fireforge:reforgeable`).

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

### Suite de la liste âme : étapes 6c, 6d, 6e (plan validé par Anger, codé)
Lore vérifié : rien ne décrit le Sift, rien ne tranche.

**6c, nouvelles armes reforgeables** (prérequis) :
| Arme | Tolérance | Statistiques |
|---|---|---|
| Trident, masse | jusqu'à Brûlant | celles des armes (dégâts, vitesse, portée, recul) |
| Arc, arbalète | jusqu'à Chaud (bois et ficelle) | nouvelles : dégâts des flèches, vitesse de tension (arc) / de rechargement (arbalète), vitesse des flèches |

- Les Spéciaux deviennent liés à un **type d'arme** (épée, hache, arc, arbalète, trident, masse),
  plus seulement à « la main ». Les 3 Spéciaux de base (Soupape, Marteau-pilon, Montée en
  pression) restent pour épée et hache. Arc, arbalète, trident, masse n'ont pas de Spécial de
  base : au feu normal, un tirage Spécial devient Attisé (comme le bouclier).
- **Codé (étape 6c)** :
  - type d'arme reconnu par la classe de l'objet ou les tags communs `c:tools/bow`, `c:tools/crossbow`,
    `c:tools/spear` (trident), `c:tools/mace`, puis `minecraft:axes` ; toute autre arme de corps à corps
    compte comme une épée ; ces tags `c:` sont aussi dans `echoes_fireforge:reforgeable` ;
  - tolérances (tags) : arc et arbalète Chaud, trident et masse Brûlant ;
  - la Lame des Veilleurs (liste âme) est, comme les 3 Spéciaux de base, pour épée et hache ;
  - arc : dégâts des flèches, vitesse de tension, vitesse des flèches ; arbalète : dégâts des flèches,
    vitesse de rechargement, vitesse des flèches (3 statistiques, comme le bouclier : Attisé = 2 bonus et
    1 contrepartie) ;
  - dégâts des flèches : en % des dégâts de base de la flèche ; vitesse des flèches : en % de leur vitesse
    au départ, sans changer les dégâts (compensés), elle porte plus loin et plus droit ;
  - tension et rechargement : le temps d'utilisation avance plus vite ou plus lentement (à +5 %, un tick
    sur vingt compte double ; à −5 %, un tick sur vingt ne compte pas), à l'écran comme en jeu ;
  - dans le volet des chances, quand aucun Spécial n'est possible pour la pièce (avec le feu choisi),
    Spécial affiche « — » et sa chance est comptée dans Attisé (vaut aussi pour le bouclier) ;
  - panneau des âmes : une ligne de bonus trop longue finit par « … » et s'affiche entière en infobulle.

**6d, Spéciaux âme sans dépendance au Silt** :
| Spécial | Pièce | Effet | Contrepartie | Raison |
|---|---|---|---|---|
| Écho de lame | épée et hache | un coup sur 5 répète 50 % de ses dégâts 1 s plus tard | 1 âme par écho (sans âme : pas d'écho) | le chant laisse ses ondes dans la pierre, comme la Roche d'écho |
| Carreau muet | arbalète | carreaux sans son ni vibration ; la cible ne prévient pas les autres | rechargement 20 % plus lent | la vapeur d'écho de la Résonance étouffe les sons |
| Harpon des rivières | trident | lancé, il ramène la créature touchée | 1 âme par lancer | les Veilleurs pêchaient le Poisson-lie dans les rivières d'âme du Récif |
| Glas | masse | un coup en tombant sonne comme un gong et fige 1 s les monstres à 4 blocs | 3 âmes par coup de Glas | le gong des Instruments des Bâtisseurs |
| Pas feutré | bottes | en courant, un pas sur deux sans vibration (sculk, Warden) ; le bruit de course envoyé au Silt réduit de moitié (via l'événement « bruit » du Core, voir 6e) | vitesse −5 % | les Veilleurs vivaient dans le silence |
| Regard du Veilleur | casque | après avoir pris des dégâts, les monstres à 16 blocs ont Brillance 5 s (visible de tous : accepté, la Lanterne des âmes du Silt fait pareil) | la Vision nocturne dure moitié moins | ils veillaient dans le noir total |

- **Codé (étape 6d)**, dans `forging/SoulSpecialEffects` :
  - Écho de lame : le 5e coup au corps à corps (compté par joueur) programme une onde magique de 50 %
    des dégâts subis, 1 s plus tard ; l'âme se paie au moment de l'écho (sans âme, rien) ; l'onde
    ne relance pas les autres Spéciaux ;
  - Carreau muet : le carreau est silencieux, le son du tir est coupé, et les vibrations du tir,
    de l'impact et du cri de la cible (ce tick) sont annulées ; rechargement −20 % ;
  - Harpon des rivières : l'âme se paie au lancer (sans âme, lancer normal) ; la première créature
    touchée est tirée vers le lanceur, et le recul du coup est annulé pour elle ;
  - Glas : coup de masse « en tombant » (règle vanilla du coup écrasant) ; cloche, onde, et les monstres
    à 4 blocs reçoivent Lenteur X 1 s et s'arrêtent net ;
  - Pas feutré : en courant, un pas sur deux n'émet pas de vibration ; vitesse −5 % (modificateur
    d'attribut) ; la moitié du bruit envoyé au Silt viendra avec l'événement « bruit » (6e) ;
  - Regard du Veilleur : Brillance 5 s aux monstres à 16 blocs après chaque dégât reçu ; toute Vision
    nocturne reçue voit sa durée divisée par deux (une seule fois par effet).

**6e, Spéciaux âme liés au Silt** :
| Spécial | Pièce | Effet | Contrepartie | Raison |
|---|---|---|---|---|
| Urne close (ex-« Linceul d'urne ») | plastron | les âmes perdues sans être dépensées (usure du Silt, vol du Moissonneur déchu, Lanterne) sont réduites de moitié | les âmes gagnées −25 % | les Veilleurs gardaient leurs morts au chaud dans des urnes |
| Fendeuse de racines | hache | +40 % de dégâts contre le tag `echoes_silt:biomass` | −10 % contre le reste | les Veilleurs forgeaient leurs armes contre la biomasse |
| Corde chantante | arc | chaque tir sonne une note ; 3 touches d'affilée sur la même cible : la flèche suivante transperce avec +50 % de dégâts | chaque tir fait du bruit (dans le Silt, il remplit la Gueule) | la musique des Bâtisseurs |

Faits du Silt (donnés par Anger, vérifiés dans son code) :
- Les âmes ne se perdent pas à la mort. Tous les gains passent par `Souls.add` (ou la façade
  `SoulHelper.add`) ; les pertes passives (usure −1 toutes les 30 s dans le Silt, 45 s avec
  l'Écho « La chute » ; vol du Moissonneur déchu) passent par `Souls.add` négatif ; les vraies
  dépenses (artefacts, Puits, Grimoire) par `Souls.spend`.
- La Gueule n'écoute pas les vibrations : son appétit monte quand on court, frappe ou est
  frappé, casse un bloc, ou qu'une explosion a lieu à moins de 32 blocs. Point d'entrée du
  Silt : `SiltRules.noise(joueur, quantité)`.
- Tag `echoes_silt:biomass` : contaminated_zombie, contaminated_skeleton, contaminated_spider,
  rampeur, noeud_mere, grand_noeud_mere, digested, silt_tracker, dragged, carapace,
  fallen_harvester (le Premier Veilleur n'y est pas, exprès : c'est le boss).

Ajouts au **Core** (code, pas le guide) pour 6e :
- un événement sur `Souls.add` (gains et pertes, montant modifiable) : l'Urne close réduit les
  gains de 25 % et les pertes de moitié ; il ne touche pas à `Souls.spend` ;
- un événement « bruit » (joueur, quantité, modifiable) : la Forge le déclenche (Corde
  chantante) et le réduit (Pas feutré) ; le Silt l'écoute pour remplir la Gueule (Anger fera le
  branchement côté Silt). Les épisodes restent indépendants.

- **Codé (étape 6e)** (choix d'Anger : tout passe par les événements du Core, la Forge n'appelle
  jamais le code du Silt) :
  - Core, `soul/SoulChangeEvent` : posté par `Souls.add` (cause `GAIN` si positif, `CORRECTION` si
    négatif), par la nouvelle `Souls.lose(joueur, n)` (cause `LOSS`, les pertes passives) et par
    `Souls.spend` (cause `SPEND`) ; la quantité est modifiable. `set` et `fill` ne le postent pas ;
  - Core, `noise/Noise.make(joueur, force, cause)` : poste `NoiseEvent.Pre` (modifiable, annulable)
    puis `NoiseEvent.Post` (le bruit final, à écouter) ; causes communes `echoes_core:run`, `attack`,
    `hurt`, `break`, `explosion`, `shot`, `other` ;
  - Urne close : seules les causes `LOSS` (moitié) et `GAIN` (−25 %) ; les dépenses et les corrections
    ne sont pas touchées (sinon les armes du Silt payées en `Souls.add` négatif coûteraient moitié
    moins, et la malédiction de `RelicEffects` laisserait passer des âmes). Les fractions d'âme sont
    gardées d'une fois sur l'autre (4 pertes d'une âme en coûtent 2, 4 gains en rapportent 3) ;
  - Fendeuse de racines : coups de hache au corps à corps seulement ;
  - Corde chantante : note de flûte qui monte à chaque touche, carillon pour la flèche chantée ; une
    autre cible relance la série à 1, un tir dans un bloc la casse ; la flèche chantée gagne un niveau
    de perçage (access transformer sur `AbstractArrow.setPierceLevel`) ; bruit de tir : 3 (cause `shot`,
    une frappe en vaut 2,5 dans le Silt) ;
  - Pas feutré : le bruit de cause `run` réduit de moitié.
- **À faire côté Silt (Anger, quand il veut)** : remplacer `Souls.add(p, -n)` par `Souls.lose(p, n)`
  pour l'usure, le Moissonneur déchu et la Lanterne ; passer les armes (Claymore, Dagues jumelles,
  Lame tourbillon) sur `Souls.spend` (gratuit en créatif) ; faire passer le bruit de course et les
  appels à `SiltRules.noise` par `Noise.make` (avec la cause qui va bien), et remplir la Gueule depuis
  `NoiseEvent.Post`. Republier le Core (`gradlew :core:publishToMavenLocal`) avant.

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
- **Codé (étape 7)** : livre en place 2 (après le Silt), un texte d'accueil ;
  - « La Forge » : machine comprimée (pièces, plans), la forge, reforger, la chaleur, les raretés,
    l'offrande, la fêlure et le soufflet, les modules (chaudière, réservoir), les Spéciaux, et
    « La Forge des âmes » (feu des âmes, options, liste âme), visible seulement avec le Silt ;
  - « Carnets des Attiseurs » : chapitre caché tant qu'aucun plan n'est lu ; une entrée par plan
    (Plan de la Forge, puis I à V), effacée tant que le plan n'est pas déchiffré ;
  - textes de lore des plans **provisoires** (Anger les écrira) : clés `guide.echoes_fireforge.plans.*` ;
  - les textes sont générés depuis un seul script (`tools/guide/make_guide.py`) qui écrit les entrées
    et les deux langues ; espaces insécables en français devant « ; : % ! ? » et dans les guillemets.

## 10. Hors v1
Créatures (les Cendreux : Attiseurs à mi-chemin de la maladie), structures, Échos,
objets particuliers au cas par cas, liste des Spéciaux version âme, ce que devient l'eau.

## 11. L'Établi d'Attiseur et les mécanismes (validé par Anger)
Idée d'Anger. Les Attiseurs ne faisaient pas que forger : ils équipaient leurs armes et leurs
armures de mécanismes pour vivre et travailler dans le Nether.

### L'établi
- **Établi d'Attiseur** (« Stoker's Workbench ») : bloc de 2 de long, comme la forge.
- Fabriqué avec le **Plan de l'Établi** (consommé), un nouveau plan qui sort des machines
  comprimées. Répartition proposée des plans (15 % de chance par machine, inchangé) : Plan de la
  Forge 35 %, Plan de l'Établi 25 %, Plans d'Attiseur I à V 40 %.
- Interface façon table de forgeron : la pièce, un gabarit, des pièces de machine, le résultat.
- **Un mécanisme par pièce**, en plus de la rareté et du Spécial ; une nouvelle greffe remplace
  l'ancienne ; le mécanisme reste quand on reforge. Composant `echoes_fireforge:mechanism`, une
  ligne dans l'infobulle.

### Les gabarits (comme les modèles d'armure)
- Un **gabarit** par mécanisme. On le trouve dans les coffres de bastion et de forteresse, et plus
  rarement dans les machines comprimées.
- **Rares** (validé) : Sonde de prospection, Planeur à vapeur, Pistons de genoux, Ressorts.
  Les autres sont communs. Chances proposées : commun 8 %, rare 2 % par coffre ; machine comprimée 2 %.
- **Duplication** (validé) : gabarit + 3 engrenages + 1 bloc de cuivre → 2 gabarits ; un rare
  demande en plus une plaque gravée.

### La vapeur (armures seulement ; validé : pas pour tout)
- Les mécanismes « à vapeur » (Planeur, Jet de vapeur, Pistons de genoux) ont une **barre de vapeur** :
  un trait blanc continu dans le bord bas de la barre d'objets (validé : rien ne bouge dans le HUD),
  seulement quand on en porte un. Les autres n'en utilisent pas.
- **20 charges par pièce** (validé : « une vingtaine de fois ») : Jet et Pistons, 1 charge par usage ;
  Planeur, 1 charge par seconde de vol (20 s en tout).
- **Recharge au bouilleur de l'établi** (idée d'Anger) : un module qui se pose contre l'établi, comme
  ceux de la forge. Il ajoute un panneau sur le côté de l'interface (comme le panneau des âmes de la
  forge) : une case combustible (charbon et autres, comme la forge), une jauge d'eau (seau, fiole,
  entonnoir, tuyaux), et une case où l'on pose la pièce à recharger. Proposé : une recharge complète
  (20 charges) coûte 1 charbon et 1 seau d'eau, en 10 secondes. Sans bouilleur, l'établi greffe
  mais ne recharge pas.
- **Armes** : pas de vapeur, un léger temps de recharge (validé).

### Les touches (configurables)
- **Arme** : clic droit (validé). **Plastron** : B (validé). Proposé : **casque** C, **jambières** N.
  Les bottes sont passives : pas de touche.

### Les mécanismes d'armure (3 par emplacement)
| Pièce | Mécanisme | Type | Effet proposé | Raison |
|---|---|---|---|---|
| Casque | Longue-vue | touche C tenue | zoom ×4 | surveiller les coulées de loin |
| | Sonde de prospection (rare) | touche C | les minerais à 8 blocs brillent 5 s ; recharge 20 s | ils cherchaient le métal dans la roche |
| | Lampe à vapeur | touche C | une lumière suit le joueur (allumée / éteinte), avec un faisceau dans le regard (validé : rendu fait par Anger avec son shader) | travailler au fond des galeries |
| Plastron | Sac dorsal | touche B | 9 cases en plus | porter les outils de forge |
| | Planeur à vapeur (rare) | touche B tenue en l'air | chute ralentie et petites poussées ; vapeur : environ 8 s de vol par seau | traverser les lacs de lave |
| | Jet de vapeur | touche B | une bouffée repousse les créatures à 4 blocs ; 200 mB, recharge 1 s | les soupapes de décharge |
| Jambières | Poches | touche N | 6 cases en plus | les poches à outils |
| | Pistons de genoux (rare) | touche N | un élan bref vers l'avant ; 150 mB | les pistons des machines |
| | Ancre | passif | accroupi : aucun recul | tenir debout devant le souffle des fourneaux |
| Bottes | Patins | passif | +20 % de vitesse sur pierre, basalte, pierre noire, briques, métal | les quais des forges |
| | Ressorts (rare) | passif | saut +50 %, chute amortie de moitié | les amortisseurs des machines |
| | Crampons | passif | monter un bloc sans sauter ; le sable des âmes ne ralentit plus | marcher dans le Nether |

Sac et poches : pas de shulker ni de sac à l'intérieur ; le contenu suit la pièce si on la perd.

### Les mécanismes d'arme
| Mécanisme | Armes | Effet proposé (clic droit) | Recharge |
|---|---|---|---|
| Frappe-sol (zone) | épée, hache | onde de choc à 3 blocs : 60 % des dégâts et recul | 5 s |
| Treuil (lancer) | épée, hache | l'arme part au bout d'une chaîne (10 blocs), frappe à 100 % et revient | 4 s |
| Piston (perçant) | épée (validé) | estocade en ligne sur 4 blocs, 100 % des dégâts | 3 s |
| Sillon (perçant, version hache) | hache (validé) | une onde court au sol en ligne droite sur 6 blocs, 80 % des dégâts à tout ce qu'elle traverse, avec des éclats de roche | 4 s |
| Trident de trempe | trident | ne se lance plus. Clic droit : **surchauffe** 8 s (+30 % de dégâts, met le feu), puis 6 s de refroidissement ; **dans l'eau ou sous la pluie**, la surchauffe fait de la vapeur : +50 % et pas de feu | 6 s |
| — | masse | rien (validé) | — |

Raison du trident : le métal chauffé qu'on plonge dans l'eau, c'est la trempe.

- **Bouclier** (validé) : avec un bouclier en main gauche, le clic droit lève le bouclier ; le
  mécanisme de l'arme se déclenche alors accroupi + clic droit.
- **Trident de trempe** (validé) : greffe refusée sur un trident avec Loyauté ou Impulsion.
- **Lampe à vapeur** (validé) : sans vapeur. La Forge s'occupe de la lumière et de l'état allumé /
  éteint (synchronisé pour tous les joueurs) et donne au client de quoi dessiner le faisceau
  (porteur, position, direction) ; un faisceau simple par défaut, que le shader d'Anger remplace.

- **Recharge** (validé) : 1 charbon et 1 seau d'eau pour 20 charges, en 10 secondes.
- **Shader d'Anger** : pas encore de dépôt (un zip). Il vivra dans la Forge (seul usage pour
  l'instant), derrière un point d'accroche client : sans lui, le faisceau simple par défaut.
- **Textures** : `tools/textures/make_workbench_textures.py` (établi 2 blocs, bouilleur, 17 gabarits,
  Plan de l'Établi ; gabarits rares en acier bleui par la trempe). Planche d'aperçu envoyée,
  en attente de validation.

### Découpage proposé
- **8a, l'établi** : Plan de l'Établi (butin), bloc de 2, interface (pièce, gabarit, engrenage, la
  clé), composant `echoes_fireforge:mechanism`, infobulle, gabarits (butin des coffres, duplication),
  greffe gardée à la reforge. Aucun effet encore.
  **Codée** : `mechanism/` (Mechanism, gabarits, Grafting) et `workbench/` (bloc, interface) ; la pièce
  doit être reforgeable (tag `echoes_fireforge:reforgeable`) ; l'établi ne garde rien (tout revient au
  joueur à la fermeture) ; gabarits : 2 % par machine comprimée (rare 1 pour 3 communs), coffres de
  bastion et de forteresse par un modificateur de butin (`neoforge:add_table`) ; données générées par
  `tools/data/make_workbench_data.py`, textures par `tools/textures/make_workbench_textures.py` et
  `make_workbench_gui.py`. L'infobulle décrit déjà l'effet de chaque mécanisme (effets en 8c et 8d).
- **8b, le bouilleur et la vapeur** : module de l'établi, panneau sur le côté, charges des pièces,
  barre de vapeur.
  **Codée** : `workbench/Steam`, `SteamBoilerBlock`, `SteamBoilerBlockEntity`, `client/SteamBar` ; le
  bouilleur se pose à l'un des deux bouts de l'établi (pas devant ni derrière) ; ballon de 4 seaux
  (seau, fiole, tuyaux, ou un seau versé sur la jauge de l'interface) ; combustible : tout ce qui
  brûle dans un fourneau (entonnoir accepté), compté en ticks de fourneau ; une charge = 50 mB d'eau
  et 80 ticks de combustible, en une demi-seconde ; composant `echoes_fireforge:steam` (0 à 20) ; une
  pièce greffée part à 0 charge ; infobulle « Vapeur : 13 / 20 » ; maj-clic range combustible et pièce
  à vapeur dans le bouilleur ; barre : deux moitiés si deux pièces à vapeur sont portées.
- **8c, les armures** : les 12 mécanismes, touches C, B, N ; sac et poches.
  **Codée** : `mechanism/MechanismEffects` (serveur), `PieceStorageMenu` (sac et poches, composant
  `minecraft:container`), `client/MechanismKeys` (touches, zoom, vol du Planeur), `client/ProbeRender`,
  réseau `network/` (touches vers le serveur, résultat de la Sonde vers le client). Chiffres : zoom ×4 ;
  Sonde : minerais (`#c:ores`), débris antiques et machines comprimées à 8 blocs, lueur de laiton à
  travers la roche 5 s, recharge 20 s (celle de l'objet) ; Jet : 4 blocs, recharge 1 s ; Pistons : élan
  de 1,3 bloc/tick, recharge 1 s ; Planeur : chute limitée, petite poussée vers l'avant, une bouffée par
  seconde (la charge), pas de dégâts de chute ; Patins : +20 % (tag `echoes_fireforge:skate_surfaces`) ;
  Ressorts : saut +50 %, dégâts de chute ×0,5 ; Crampons : hauteur de pas 1 bloc, efficacité de
  mouvement 1 (le sable des âmes ne ralentit plus) ; Ancre : accroupi, recul annulé. La Lampe à vapeur
  (touche C) arrive en 8e. Sans vapeur : « Plus de vapeur : recharge au bouilleur ».
- **8d, les armes** : Piston, Sillon, Frappe-sol, Treuil, Trident de trempe ; clic droit et bouclier.
  **Codée** : `mechanism/WeaponMechanisms` ; recharge = celle de l'objet (visible sur la barre), 1 point
  d'usure par usage ; dégâts = l'attaque de l'arme × la part du mécanisme ; les coups de mécanisme ne
  sont pas des coups d'épée (source « attaque de créature » du joueur) : ils ne relancent pas les
  Spéciaux (Écho de lame, Soupape…) ; Treuil : un coup instantané sur la première créature visée à
  10 blocs (pas à travers un mur), une chaîne en particules, sans entité qui vole ; Trident de trempe :
  le clic droit ne le lance plus, il surchauffe (recharge de 14 s sur l'objet : 8 de surchauffe et 6 de
  refroidissement) ; « mouillé » = dans l'eau, sous la pluie ou dans les bulles.
- **8e, la Lampe à vapeur et le guide** : lumière, faisceau par défaut, point d'accroche pour le
  shader ; entrées du guide (chapitre « La Forge » et un plan de plus dans les Carnets).
  **Codée** : `mechanism/SteamLamp` et `LampLightBlock`, `client/LampBeamRender`. Touche C : allumée ou
  éteinte, gardé sur le casque (composant `echoes_fireforge:lamp_on`, l'infobulle le dit). La lumière
  (niveau 14) se pose dans l'air juste devant ce que l'on regarde, jusqu'à 12 blocs, et suit le regard ;
  elle se retire d'elle-même si plus aucune lampe ne l'éclaire (vérifié chaque seconde). Le faisceau :
  un cône blanc chaud de 10 blocs qui pâlit au loin, visible par tous les autres joueurs et par soi en
  vue de dos ; pas de cône pour soi en vue à la première personne (on le verrait par le bout, la lumière
  sur les murs suffit). Guide : l'établi, les gabarits, le bouilleur, les mécanismes d'armure et d'arme,
  et le Plan de l'Établi dans les Carnets (texte provisoire).
  **Pour le pack de shaders d'Anger** (Iris) : la lumière est le bloc `echoes_fireforge:lamp_light`
  (à ajouter dans `block.properties` pour sa couleur). Il a un petit cube entièrement transparent, en
  découpe (cutout) : invisible, mais il envoie des sommets ; sans eux, la passe d'ombre du pack (qui
  repère les sources de lumière colorée dans la géométrie du terrain) ne le voyait pas ; le faisceau est rendu comme un faisceau de
  balise (`RenderType.beaconBeam`, translucide, texture `echoes_fireforge:textures/entity/lamp_beam.png`),
  il passe donc par `gbuffers_beaconbeam`. Aussi proposé à Anger : la forge allumée
  (`echoes_fireforge:fire_forge:lit=true`, et `soul=true` en cyan) et le bouilleur allumé
  (`echoes_fireforge:steam_boiler:lit=true`).

## 12. Après la v1 (12a et 12c validés ; 12b attend)

### 12a. Les Échos et les succès de la Forge (Core, déjà prêt)
Un arbre de succès « Echoes of the Fire Forge ». Certains laissent un **Écho** : un petit bonus permanent
lié aux systèmes de la Forge (jamais aux dégâts ni à la vie, comme dans le Silt).
| Succès | Comment | Écho proposé |
|---|---|---|
| Le feu reprend | reforger un premier objet | — |
| Main d'Attiseur | sortir un Spécial | compte comme un plan lu de plus (les chances montent un peu) |
| Le souffle juste | rattraper 10 fêlures au soufflet | fenêtre de réaction +0,25 s |
| Carnets complets | lire tous les plans | +2 % de chance d'enchantement rare à l'offrande |
| Vapeur domptée | recharger 200 charges au bouilleur | 22 charges par pièce au lieu de 20 |
| L'établi | greffer un premier mécanisme | — |
| Les noyés de cendre | voir un Cendreux (12c) ; avec le Silt, avoir lu la tablette XIII | — (le lien d'histoire, sans trancher la cause) |

### 12b. Les structures : les forges mortes des Attiseurs
- **Forge en ruine** (petite, assez commune, terres désolées et deltas de basalte) : une forge effondrée
  en briques du Nether et basalte, des machines comprimées dans les murs, un coffre (plans, gabarits,
  pièces de machine).
- **Chaufferie** (grande, rare, à demi enfouie) : une salle des chaudières, des conduites de vapeur
  fossiles, le Plan de l'Établi plus souvent, un établi en ruine (décor), des Cendreux (12c).
- Construites en blocs vanilla et blocs de la Forge (règle 4 : aucune structure de Mojang copiée).

### 12c. Les Cendreux : des Attiseurs à mi-chemin de la maladie
- Ni tout à fait vivants, ni encore des wither squelettes : la peau en cendre, des braises dans la poitrine.
- Ils hantent les chaufferies et entretiennent encore les feux éteints (ils rallument les foyers morts).
- Neutres tant qu'on ne touche pas aux machines autour d'eux ; ensuite ils lancent des braises.
- Ils toussent de la cendre. On ne dit jamais d'où vient la maladie (fumée des machines ou autre chose).
- Butin proposé : de la cendre d'Attiseur (une offrande « bonne » de plus ?), parfois une pièce de machine.
- Modèle et texture originaux, présentés sur une planche avant intégration.

### Décisions d'Anger
- **Ordre validé : 12a, puis 12c.** Les structures (12b) attendent.
- Cendreux : l'apaisement, « on verra ».
- **Butin des Cendreux : de quoi améliorer la forge**, une amélioration qui fixe une **rareté minimum selon
  l'offrande** (idée d'Anger : avec un fragment de netherite, plus de Brut ni en dessous).
  Proposition :
  - la **cendre d'Attiseur** tombe des Cendreux ; avec elle on fabrique un **Lit de cendre** (avec des
    pièces de machine), que l'on pose dans la forge d'un clic droit : une amélioration permanente,
    gardée quand on casse la forge ;
  - raison : les Attiseurs garnissaient leurs foyers de cendre pour tenir une chaleur égale ;
  - avec le Lit de cendre : petite offrande (pépite d'or, quartz) → pas de Fêlé au tirage ; bonne offrande
    (crème de magma, larme de ghast) → pas de Fêlé au tirage ; grosse offrande (fragment de netherite) →
    ni Fêlé ni Brut (Trempé au minimum) ; les chances retirées passent aux raretés au-dessus, comme
    « Garantir » du module des âmes ;
  - une fêlure ratée au soufflet donne toujours Fêlé (le réflexe reste) ;
  - « Garantir » (20 âmes) reste utile avec une petite ou une bonne offrande (il retire aussi Brut).
- Eau évaporée : pas encore décidé. Piste d'Anger (« lore caché ») : les monuments aquatiques et les
  constructions sous l'eau étaient autrefois à la surface ; l'eau du Nether serait-elle remontée dans
  l'Overworld ? À ne pas trancher : au mieux, des indices qui laissent hésiter.

### Décisions d'Anger (suite)
- **12a validée telle quelle** (le tableau ci-dessus). Faite : `progress/ForgeEchoes`, succès dans
  `data/echoes_fireforge/advancement/echoes/` et textes, générés par `tools/data/make_echoes_data.py`.
  - Le souffle juste : le compteur des fêlures rattrapées est gardé par joueur (et à la mort).
  - Vapeur domptée : on compte les charges rechargées au bouilleur pour le dernier joueur qui a ouvert
    l'établi ; avec l'Écho, la pièce rechargée garde ses 22 charges (composant `echoes_fireforge:steam_capacity`),
    même rechargée plus tard par un autre.
  - Main d'Attiseur et Carnets complets se voient dans les chances de l'interface : à l'éveil, une marque de
    lore (espace `echoes_fireforge_echo`, qui ne révèle pas les Carnets) le dit au client.
  - Les noyés de cendre : avec la 12c.
- **12c** : apparition validée (rarement, terres désolées et deltas de basalte près de la lave) **et aussi,
  rarement, dans les forteresses du Nether**. Hostilité : neutres tant qu'on ne touche pas aux machines.
  Apaisement : « on verra ».
- **Lit de cendre** : les minimums proposés sont validés. **L'amélioration doit se voir sur la forge**
  (un lit de cendre visible dans le foyer, à présenter sur une planche avant de coder).

### 12c, telle que validée sur les planches et faite
- **Le Cendreux** (version 2 de la planche, bras raccourcis à 12 px) : un Attiseur presque wither squelette,
  2,3 blocs, voûté, membres d'os de 2 px, os calcinés noirs, la cage ouverte où couve la braise, la moitié du
  visage déjà en crâne ; restent les lunettes relevées, le tablier roussi, les bretelles, le cuir, un lambeau de
  peau. Couche de braises lumineuse (yeux, poitrine, une veine du bras). Modèle : `client/CendreuxModel`, boîtes
  et texture générées par `tools/textures/make_cendreux_textures.py` (CENDREUX_PARTS).
- **Comportement** (`cendreux/CendreuxEntity`) : 26 PV, ne brûle pas, le wither ne le touche pas. Neutre ;
  casser ou utiliser une machine des Attiseurs (tag `echoes_fireforge:attiseur_machines` : forge, établi,
  bouilleur, machine comprimée, modules) à 16 blocs ou moins le met en colère 30 s ; frappé, il riposte. Il lance
  des braises (boules de feu de blaze), tousse de la cendre, perd de la cendre en marchant, et rallume les feux de
  camp éteints quand il est calme. Créatif et spectateur ne le fâchent pas.
- **Apparition** : poids 2 en terres désolées et deltas de basalte, seulement à 6 blocs ou moins de la lave ;
  poids 1 dans les forteresses du Nether (sans condition de lave). Œuf d'apparition.
- **Butin** : 1 à 2 Cendres d'Attiseur (+ pillage), 8 % d'engrenage (tué par un joueur).
- **Lit de cendre** : 4 Cendres d'Attiseur aux coins, 4 Engrenages, 1 Plaque gravée. Clic droit sur la forge
  (n'importe quelle moitié) : état `ash` du bloc (foyer de cendre pâle et plateau de laiton qui dépasse, aussi au
  feu des âmes). Cassée, la forge garde son Lit (composant `echoes_fireforge:ash_bed` sur l'objet). Tirage :
  `RarityOdds.ashBed` (petite ou bonne offrande : pas de Fêlé ; fragment de netherite : Trempé au minimum ;
  sans offrande, rien), avant « Garantir » ; affiché dans les chances de l'interface.
- **Les noyés de cendre** : voir un Cendreux (à 16 blocs, sans obstacle) ; avec le Silt, avoir aussi lu la
  Tablette XIII, que le Silt range dans le lore du Core (`echoes_silt:tablet/13`) : dans un ordre ou dans l'autre.
- Sons : les fichiers du wither squelette et du blaze (jeu de base), nos sous-titres. Nom anglais : « Ashen »
  (validé). Guide : pages « Les Cendreux » et « Le Lit de cendre ».
- Données générées par `tools/data/make_cendreux_data.py`.

## Textures et modèles
Premières versions originales dessinées par Claude, présentées sur une planche d'aperçu
avant toute intégration (règles 4 et 5).
