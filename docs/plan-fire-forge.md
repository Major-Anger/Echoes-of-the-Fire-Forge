# Echoes of the Fire Forge : plan v1 (validé par Anger)

Référence de conception. Toute modification passe par Anger (voir CLAUDE.md, règle 5).

## Lore (rappel)
Les **Attiseurs**, une famille des Anciens Bâtisseurs, ont fait du Nether un monde-forge
à vapeur. La dimension s'est réchauffée et l'eau s'est évaporée à travers le toit de bedrock.
Une maladie les a changés en wither squelettes ; on ne dit jamais si elle vient des machines.
Les débris antiques sont des fossiles de leurs machines, inutilisables (on ne les modifie pas).

## 1. Découverte
- **Machine comprimée** : bloc rare, à la profondeur des débris antiques, à miner au minimum
  avec une pioche en diamant.
- En la minant : des **pièces de machine** (engrenage, soupape, plaque gravée…) et parfois
  un **plan**.
- **Plans** : lus, ils sont enregistrés dans le lore du Core (`echoes_fireforge:plan/…`),
  révèlent une page effacée du guide, et chaque plan lu **améliore les chances de rareté**
  (le joueur « imagine » la technique et la maîtrise peu à peu).
- Avec des pièces et le bon plan, on construit la forge.

## 2. La forge et ses modules
- **Forge** : 2 blocs de long, une petite jauge d'eau. L'avant porte l'interface.
  - moitié gauche : le foyer et la cheminée (environ 1,9 bloc de haut, validé) ;
    le **soufflet** sur la face avant, contre le foyer : on l'actionne pour le réflexe
    de la fêlure (section 5) ;
  - moitié droite : l'enclume, où l'objet en cours de reforge est visible.
- **Trois côtés à modules**, un module par côté :
  - **gauche** : la **chaudière** (agrandit la réserve d'eau), empilable jusqu'à
    **3 blocs de haut** au total ;
  - **arrière** : le **réservoir de lave** (alimente la chaleur Fournaise), jusqu'à
    **2 blocs de haut** et 5 seaux au total (répartition à confirmer) ;
  - **droite** : le **module d'amélioration** (Forge des âmes, avec le Silt).
- Chaque côté accepte un module parmi plusieurs : d'autres mods pourront ajouter leurs
  propres modules sans modifier la forge.

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
| Pierre, mailles, fer | Ardent |
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
- **Armes et armures particulières** (arcs, arbalètes, trident, masse, bouclier, élytres,
  carapace de tortue…) : au cas par cas.

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
- **Ordre des calculs** : chances de départ → plans lus → offrande → plafond de chaleur →
  risque de fêlure (réflexe). Les chances s'affichent dans l'interface de la forge.
- Compatible avec les enchantements, conservée lors du passage à la netherite.

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

## 8. Avec le Silt : la Forge des âmes résonnantes
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
