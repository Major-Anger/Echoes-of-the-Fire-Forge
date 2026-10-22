"""Génère le guide de la Forge : book.json, entrées et textes (fr_fr / en_us).

Usage : python3 tools/guide/make_guide.py fireforge/src/main/resources
Les clés guide.echoes_fireforge.* des fichiers de langue sont réécrites à chaque fois.
Textes de lore des plans : provisoires (Anger les écrira).
"""
import json
import os
import sys

ROOT = sys.argv[1]  # fireforge/src/main/resources
NS = "echoes_fireforge"
GUIDE = os.path.join(ROOT, "assets", NS, "echoes_guide")
P = "guide." + NS

FR, EN = {}, {}


def t(key, fr, en):
    FR[key] = fr
    EN[key] = en
    return key


def entry(path, icon, title_fr, title_en, pages, lore=None, label=None, requires=None):
    base = f"{P}.{path.replace('/', '.')}"
    e = {"title": t(base + ".title", title_fr, title_en), "icon": icon}
    if lore:
        e["lore"] = lore
    if label:
        e["label"] = label
    if requires:
        e["requires"] = requires
    out = []
    for i, page in enumerate(pages, 1):
        page = dict(page)
        if "fr" in page:
            page["text"] = t(f"{base}.p{i}", page.pop("fr"), page.pop("en"))
        if "title_fr" in page:
            page["title"] = t(f"{base}.p{i}.title", page.pop("title_fr"), page.pop("title_en"))
        out.append(page)
    e["pages"] = out
    file = os.path.join(GUIDE, "entries", path + ".json")
    os.makedirs(os.path.dirname(file), exist_ok=True)
    with open(file, "w", encoding="utf-8") as f:
        json.dump(e, f, ensure_ascii=False, indent=2)
        f.write("\n")
    return path


def text(fr, en, **extra):
    return {"type": "text", "fr": fr, "en": en, **extra}


def spot(item, fr, en, **extra):
    p = {"type": "spotlight", "fr": fr, "en": en, **extra}
    if isinstance(item, list):
        p["items"] = item
    else:
        p["item"] = item
    return p


def craft(recipe):
    return {"type": "crafting", "recipe": recipe}


I = NS + ":"

# ---------------------------------------------------------------------- La Forge
forge = [
    entry("forge/compressed_machine", I + "compressed_machine", "La machine comprimée", "Compressed Machine", [
        spot(I + "compressed_machine",
             "Dans les profondeurs du Nether, près des débris antiques, dorment des blocs plus rares encore : des machines écrasées par le temps.\n\nIls ne touchent jamais l'air : il faut creuser pour les trouver. Les plus gros filons se cachent entre Y 8 et 24. Une pioche en diamant au minimum ; au toucher de soie, on garde le bloc.",
             "Deep in the Nether, near the ancient debris, lie even rarer blocks: machines crushed by time.\n\nThey never touch the air: you have to dig for them. The larger veins hide between Y 8 and 24. Needs at least a diamond pickaxe; with Silk Touch, you keep the block."),
        spot([I + "machine_gear", I + "machine_valve", I + "engraved_plate"],
             "Minée, elle donne une pièce au hasard : engrenage, soupape ou plaque gravée. La forge et ses modules en sont faits.",
             "Mined, it drops one random part: a gear, a valve or an engraved plate. The forge and its modules are made of them.",
             title_fr="Les pièces", title_en="The parts"),
        spot([I + "forge_plan", I + "attiseur_plan"],
             "Parfois, une machine garde aussi un plan gravé sur laiton. Le Plan de la Forge permet de la construire ; les Plans d'Attiseur, numérotés de I à V, racontent leur technique.\n\nClic droit pour déchiffrer un plan : il n'est pas consommé. Chaque plan lu améliore un peu les chances de la forge, et réveille une page des Carnets des Attiseurs.",
             "Sometimes a machine also holds a plan engraved on brass. The Forge Plan lets you build it; the Stoker's Plans, numbered I to V, describe their craft.\n\nRight-click to decipher a plan: it is not consumed. Each plan read improves the forge's odds a little, and wakes a page of the Stokers' Notebooks.",
             title_fr="Les plans", title_en="The plans"),
    ]),
    entry("forge/fire_forge", I + "fire_forge", "La Forge des Attiseurs", "The Stokers' Forge", [
        spot(I + "fire_forge",
             "Une forge de deux blocs de long, bâtie d'après le Plan de la Forge (consommé à la fabrication).\n\nÀ l'avant, le foyer et le soufflet ; sur le dessus, l'enclume où l'on voit l'objet en cours ; derrière, la cheminée. Clic droit pour ouvrir son interface.",
             "A forge two blocks long, built from the Forge Plan (consumed when crafted).\n\nAt the front, the hearth and the bellows; on top, the anvil showing the item being worked; behind, the chimney. Right-click to open it."),
        craft(I + "fire_forge"),
    ]),
    entry("forge/reforging", "minecraft:iron_sword", "Reforger", "Reforging", [
        text("Pour reforger, il faut quatre choses :\n\n- l'objet, posé à la main ;\n- une offrande, posée à la main, consommée à chaque reforge ;\n- un combustible, qui fixe la chaleur ;\n- de l'eau : une fiole par reforge.\n\nFrappe le marteau de l'interface. Une reforge dure environ 10 secondes et remplace la rareté précédente.",
             "To reforge, you need four things:\n\n- the item, placed by hand;\n- an offering, placed by hand, consumed by each reforge;\n- a fuel, which sets the heat;\n- water: one bottle per reforge.\n\nStrike the hammer in the interface. A reforge takes about 10 seconds and replaces the previous rarity."),
        text("Se reforgent : épées, haches, armures, bouclier, arcs, arbalètes, tridents et masses.\n\nLa forge seule tient un seau d'eau. L'eau et le combustible peuvent arriver par entonnoir (combustible par le dessus, eau par les côtés) ; les seaux et fioles vides sortent par le dessous.\n\nLa forge est fermée : elle marche aussi dans le Nether.",
             "Reforgeable: swords, axes, armor, shield, bows, crossbows, tridents and maces.\n\nThe forge alone holds one bucket of water. Water and fuel can come in through hoppers (fuel from the top, water from the sides); empty buckets and bottles leave through the bottom.\n\nThe forge is sealed: it also works in the Nether.",
             title_fr="Bon à savoir", title_en="Good to know"),
    ]),
    entry("forge/heat", "minecraft:blaze_rod", "La chaleur", "Heat", [
        spot(["minecraft:oak_planks", "minecraft:coal", "minecraft:coal_block", "minecraft:blaze_rod", "minecraft:lava_bucket"],
             "La chaleur vient du combustible en train de brûler. Elle fixe la rareté la plus haute possible :\n\n- bois, planches : Tiède, Brut au mieux ;\n- charbon : Chaud, Trempé ;\n- bloc de charbon : Ardent, Affiné ;\n- bâton de blaze : Brûlant, Attisé ;\n- lave : Fournaise, Spécial possible.\n\nUn tirage plus haut redescend au plafond. Un combustible par reforge ; un seau de lave en fait quatre.",
             "Heat comes from the fuel that is burning. It sets the highest possible rarity:\n\n- wood, planks: Warm, Raw at best;\n- coal: Hot, Tempered;\n- coal block: Blazing, Refined;\n- blaze rod: Searing, Stoked;\n- lava: Furnace, Special possible.\n\nA higher roll drops back to the cap. One fuel per reforge; a lava bucket lasts four."),
    ]),
    entry("forge/rarities", "minecraft:diamond_sword", "Les raretés", "Rarities", [
        text("Chaque reforge tire une rareté :\n\n- Fêlé : un malus (−2 à −6 %) ;\n- Brut : rien ;\n- Trempé : un bonus (+1 à +3 %) ;\n- Affiné : deux bonus (+2 à +6 %) ;\n- Attisé : deux ou trois bonus (+4 à +9 %) et une contrepartie ;\n- Spécial : un effet unique et une contrepartie.",
             "Each reforge rolls a rarity:\n\n- Cracked: one malus (−2 to −6%);\n- Raw: nothing;\n- Tempered: one bonus (+1 to +3%);\n- Refined: two bonuses (+2 to +6%);\n- Stoked: two or three bonuses (+4 to +9%) and one drawback;\n- Special: a unique effect and one drawback."),
        text("Les pourcentages portent sur les valeurs propres de l'objet : dégâts, vitesse d'attaque, portée, recul pour les armes ; armure, robustesse, résistance au recul, vitesse et résistance au feu pour les armures ; dégâts, tension ou rechargement et vitesse des flèches pour les arcs et arbalètes.\n\nLes chances s'affichent dans l'interface. La rareté reste avec les enchantements et au passage à la netherite.",
             "Percentages apply to the item's own values: damage, attack speed, reach, knockback for weapons; armor, toughness, knockback resistance, speed and fire resistance for armor; arrow damage, draw or reload speed and arrow speed for bows and crossbows.\n\nThe odds are shown in the interface. The rarity survives enchanting and the netherite upgrade.",
             title_fr="Les bonus", title_en="The bonuses"),
    ]),
    entry("forge/offerings", "minecraft:ghast_tear", "L'offrande", "Offerings", [
        spot(["minecraft:gold_nugget", "minecraft:quartz", "minecraft:magma_cream", "minecraft:ghast_tear", "minecraft:netherite_scrap"],
             "Ce qu'on donne au feu. L'offrande améliore les chances de rareté, et donne une petite chance d'enchantement rare, toujours compatible avec l'objet :\n\n- pépite d'or, quartz : petit coup de pouce ;\n- crème de magma, larme de ghast : bon ;\n- fragment de netherite : gros.\n\nPlus l'offrande est chère, plus la chance est haute.",
             "What you give to the fire. The offering improves the rarity odds, and gives a small chance of a rare enchantment, always compatible with the item:\n\n- gold nugget, quartz: small boost;\n- magma cream, ghast tear: good;\n- netherite scrap: large.\n\nThe costlier the offering, the higher the chance."),
    ]),
    entry("forge/cracks", "minecraft:iron_chestplate", "Trop chaud, ça fend", "Too Hot, It Cracks", [
        text("Chaque matériau a sa limite :\n\n- cuir, bois, or, arc, arbalète : Chaud ;\n- pierre, mailles, fer, bouclier : Ardent ;\n- diamant, trident, masse : Brûlant ;\n- netherite : tout.\n\nPour chaque palier au-delà, la forge craque une fois pendant la reforge : elle tremble, crache des étincelles, et le cadre rougit.",
             "Each material has its limit:\n\n- leather, wood, gold, bow, crossbow: Hot;\n- stone, chainmail, iron, shield: Blazing;\n- diamond, trident, mace: Searing;\n- netherite: anything.\n\nFor each step beyond it, the forge cracks once during the reforge: it shakes, throws sparks, and the frame turns red."),
        text("Quand ça craque, tu as une seconde et demie pour actionner le soufflet, dans l'interface ou d'un clic droit sur la forge. Réussi, le risque est écarté ; manqué, l'objet sortira Fêlé.\n\nLe soufflet souffle de la vapeur : un coup dans le vide coûte un peu d'eau.\n\nFêlé n'abîme pas l'objet : seulement un malus.",
             "When it cracks, you have a second and a half to work the bellows, in the interface or with a right-click on the forge. Success clears the risk; a miss means the item comes out Cracked.\n\nThe bellows blow steam: a puff at nothing costs a little water.\n\nCracked does not damage the item: just a malus.",
             title_fr="Le soufflet", title_en="The bellows"),
    ]),
    entry("forge/modules", I + "boiler", "Les modules", "Modules", [
        spot(I + "boiler",
             "Contre le côté gauche du foyer, la chaudière agrandit la réserve d'eau : 4 seaux par bloc, empilable sur 3 blocs.\n\nOn la remplit au seau ou à la fiole d'un clic droit, ou par des tuyaux. Fermée elle aussi, elle marche dans le Nether.",
             "Against the left side of the hearth, the boiler enlarges the water reserve: 4 buckets per block, stackable 3 blocks high.\n\nFill it with a bucket or bottle by right-clicking, or with pipes. Sealed as well, it works in the Nether."),
        craft(I + "boiler"),
        spot(I + "lava_tank",
             "Derrière la forge, le réservoir de lave nourrit la chaleur Fournaise : 5 seaux par bloc, sur 2 blocs de haut.\n\nIl ne sert que si la case combustible est vide : le combustible posé passe toujours en premier.\n\nUn module cassé rétrécit la réserve, et le surplus s'en va en vapeur ou en fumée.",
             "Behind the forge, the lava tank feeds Furnace heat: 5 buckets per block, 2 blocks high.\n\nIt is only used when the fuel slot is empty: placed fuel always comes first.\n\nBreaking a module shrinks the reserve, and the excess leaves as steam or smoke."),
        craft(I + "lava_tank"),
    ]),
    entry("forge/specials", "minecraft:netherite_sword", "Les Spéciaux", "Specials", [
        text("Un Spécial n'arrive qu'à la chaleur Fournaise. Chacun vient d'une machine des Attiseurs, et a sa contrepartie.\n\nArmes (épée, hache) : Soupape, Marteau-pilon, Montée en pression.\n\nArmures : Chaudière (plastron), Pas de fondeur (jambières), Soufflet (bottes), Lunettes d'Attiseur (casque), Linceul de cendre (n'importe quelle pièce).",
             "A Special only comes at Furnace heat. Each one comes from a Stoker machine, and has its drawback.\n\nWeapons (sword, axe): Valve, Steam Hammer, Building Pressure.\n\nArmor: Boiler (chestplate), Founder's Stride (leggings), Bellows (boots), Stoker's Goggles (helmet), Ash Shroud (any piece)."),
        text("L'effet et la contrepartie exacts s'affichent dans l'infobulle de l'objet.\n\nArcs, arbalètes, tridents, masses et bouclier n'ont pas de Spécial au feu normal : un tirage Spécial y devient Attisé.",
             "The exact effect and drawback are shown in the item's tooltip.\n\nBows, crossbows, tridents, maces and shields have no Special at a normal fire: a Special roll becomes Stoked."),
    ]),
    entry("forge/soul_module", I + "soul_module", "La Forge des âmes", "The Soul Forge", [
        spot(I + "soul_module",
             "Contre le côté droit de la forge, le module des âmes donne à chaque reforge le choix du feu.\n\nAu feu des âmes, le foyer devient bleu : 10 âmes par reforge, jamais de fêlure, mais tout ou rien. Les Spéciaux y viennent d'une liste à part, celle des Veilleurs.",
             "Against the right side of the forge, the soul module offers a choice of fire at each reforge.\n\nAt the soul fire, the hearth turns blue: 10 souls per reforge, never a crack, but all or nothing. Specials there come from a list of their own, the Watchers' list."),
        craft(I + "soul_module"),
        text("Au feu normal, les âmes paient des options :\n\n- verrouiller un bonus (15) : il reste quoi qu'il arrive ;\n- garantir (20) : ni Fêlé ni Brut au tirage ;\n- relancer (10) : garde la rareté, retire les valeurs, sans combustible ni offrande.\n\nLes âmes se paient au coup de marteau.",
             "At a normal fire, souls pay for options:\n\n- lock a bonus (15): it stays whatever happens;\n- guarantee (20): no Cracked or Raw roll;\n- reroll (10): keeps the rarity, rerolls the values, no fuel or offering.\n\nSouls are paid when the hammer strikes.",
             title_fr="Les options", title_en="The options"),
        text("Épée, hache : Lame des Veilleurs, Écho de lame ; hache seule : Fendeuse de racines. Arc : Corde chantante. Arbalète : Carreau muet. Trident : Harpon des rivières. Masse : Glas.\n\nCasque : Regard du Veilleur. Plastron : Urne close. Bottes : Pas feutré.\n\nCertains se paient en âmes à chaque usage.",
             "Sword, axe: Watchers' Blade, Blade Echo; axe only: Rootcleaver. Bow: Singing String. Crossbow: Silent Bolt. Trident: River Harpoon. Mace: Knell.\n\nHelmet: Watcher's Gaze. Chestplate: Sealed Urn. Boots: Muffled Step.\n\nSome cost souls each time they act.",
             title_fr="Les Spéciaux des âmes", title_en="Soul Specials"),
    ], requires=["echoes_silt"]),
]

# ---------------------------------------------------------------------- Carnets des Attiseurs (textes provisoires : Anger les écrira)
plans = [
    entry("plans/forge", I + "forge_plan", "Le Plan de la Forge", "The Forge Plan", [
        text("Deux foyers sous une seule cheminée. Le trait est sûr, appuyé, gravé par quelqu'un qui en avait bâti cent.\n\nDans la marge, une ligne plus petite : « L'eau d'abord. Le feu ensuite. Jamais l'inverse. »",
             "Two hearths under a single chimney. The line is sure and firm, engraved by someone who had built a hundred.\n\nIn the margin, a smaller line: \"Water first. Fire second. Never the other way.\""),
    ], lore=I + "plan/forge"),
    entry("plans/1", I + "attiseur_plan", "Les feux", "The Fires", [
        text("Cinq feux, du plus doux au plus fort. Le bois est noté d'un trait, la lave de cinq.\n\n« Le métal ne prend que ce que le feu lui donne. Qui veut plus doit chauffer plus. »",
             "Five fires, from the gentlest to the strongest. Wood is marked with one stroke, lava with five.\n\n\"Metal takes only what the fire gives it. Whoever wants more must burn hotter.\""),
    ], lore=I + "plan/1", label="I"),
    entry("plans/2", I + "attiseur_plan", "La vapeur", "Steam", [
        text("Une chaudière, en coupe. Des flèches montent du foyer jusqu'au plafond, et le traversent.\n\nEn dessous, gratté puis regravé : « Il en faut toujours plus. L'eau part plus vite qu'on ne la verse. »",
             "A boiler, in cross-section. Arrows rise from the hearth to the ceiling, and pass through it.\n\nBelow, scratched out then engraved again: \"We always need more. The water leaves faster than we pour it.\""),
    ], lore=I + "plan/2", label="II"),
    entry("plans/3", I + "attiseur_plan", "Le soufflet", "The Bellows", [
        text("Des métaux rangés en colonne, chacun avec sa marque de chaleur. Au-delà, une fêlure dessinée d'un seul trait.\n\n« Quand le métal crie, souffle. La vapeur le calme. Celui qui hésite garde la cicatrice. »",
             "Metals in a column, each with its heat mark. Beyond it, a crack drawn in a single stroke.\n\n\"When the metal cries out, blow. The steam calms it. Whoever hesitates keeps the scar.\""),
    ], lore=I + "plan/3", label="III"),
    entry("plans/4", I + "attiseur_plan", "L'offrande", "The Offering", [
        text("Une main tendue au-dessus d'un foyer. Dans la paume, quelque chose de petit et de précieux.\n\n« Le feu rend ce qu'on lui donne. Parfois, il rend davantage. »",
             "An outstretched hand above a hearth. In the palm, something small and precious.\n\n\"The fire returns what it is given. Sometimes, it returns more.\""),
    ], lore=I + "plan/4", label="IV"),
    entry("plans/5", I + "attiseur_plan", "Les dernières lignes", "The Last Lines", [
        text("Le trait tremble, le plan s'arrête là.\n\n« Les anciens toussent de la cendre. Certains accusent la fumée des machines ; d'autres disent que c'était là avant nous. »",
             "The line trembles; the plan stops there.\n\n\"The elders cough ash. Some blame the smoke of the machines; others say it was here before us.\""),
    ], lore=I + "plan/5", label="V"),
]

book = {
    "order": 2,
    "intro": t(P + ".intro",
               "Ces pages rassemblent ce qu'ajoute Echoes of the Fire Forge : la machine comprimée, la forge et ses modules, les raretés et les Spéciaux.\n\nLes Carnets des Attiseurs se révèlent à mesure que tu déchiffres leurs plans.",
               "These pages gather what Echoes of the Fire Forge adds: the compressed machine, the forge and its modules, rarities and Specials.\n\nThe Stokers' Notebooks reveal themselves as you decipher their plans."),
    "categories": [
        {"id": "forge", "icon": I + "fire_forge",
         "title": t(P + ".cat.forge", "La Forge", "The Forge"),
         "description": t(P + ".cat.forge.desc",
                          "Les Attiseurs ont fait du Nether un monde-forge. Leurs machines dorment encore sous la roche, et leur forge peut revivre.",
                          "The Stokers turned the Nether into a forge-world. Their machines still sleep under the rock, and their forge can live again."),
         "entries": forge},
        {"id": "plans", "icon": I + "attiseur_plan", "hidden": True,
         "title": t(P + ".cat.plans", "Carnets des Attiseurs", "Stokers' Notebooks"),
         "description": t(P + ".cat.plans.desc",
                          "Les plans des Attiseurs, gravés sur laiton et retrouvés dans les machines comprimées.\n\nLes pages effacées attendent d'être découvertes.",
                          "The Stokers' plans, engraved on brass and found in compressed machines.\n\nThe erased pages are waiting to be found."),
         "entries": plans},
    ],
}
with open(os.path.join(GUIDE, "book.json"), "w", encoding="utf-8") as f:
    json.dump(book, f, ensure_ascii=False, indent=2)
    f.write("\n")

import re
for k, v in FR.items():
    v = re.sub(r" ([;:%!?»])", "\u00a0\\1", v)
    FR[k] = re.sub(r"« ", "«\u00a0", v)

for lang, data in (("fr_fr", FR), ("en_us", EN)):
    path = os.path.join(ROOT, "assets", NS, "lang", lang + ".json")
    with open(path, encoding="utf-8") as f:
        current = json.load(f)
    for k in [k for k in current if k.startswith(P + ".")]:
        del current[k]
    current.update(data)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(current, f, ensure_ascii=False, indent=2)
        f.write("\n")
print(len(FR), "clés")
