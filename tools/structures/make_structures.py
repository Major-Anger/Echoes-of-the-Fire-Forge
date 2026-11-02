"""Les structures de la Forge (plan, section 12b ; aperçu en attente de validation d'Anger) : des gabarits de structure
(.nbt) bâtis en blocs vanilla et blocs de la Forge, sans rien reprendre des structures de Mojang (règle 4).

Usage : python3 tools/structures/make_structures.py <dossier de sortie (data/echoes_fireforge/structure)>

- forge_ruin (terres désolées, rives de la mer de lave), forge_ruin_crimson (reprise par la forêt carmin),
  forge_ruin_soul (vallée des âmes) : une forge du quotidien, abandonnée ;
- fortress_annex : la forge d'une forteresse (les forteresses sont des ouvrages des Attiseurs), sur piliers ;
- boiler_hall : la chaufferie, creusée dans la roche, et sa salle scellée ; boiler_hall_gate : son entrée, posée là où
  son couloir débouche à l'air libre ;
- last_hearth : le dernier foyer ;
- ceiling_chimneys : les cheminées qui pendent du plafond de bedrock.
Les blocs non décrits ne sont pas posés : le terrain reste. L'air décrit creuse la place.
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402
from nbt import Byte, Compound, Double, Float, Int, List, String  # noqa: E402

OUT = sys.argv[1]
NS = "echoes_fireforge"
DATA_VERSION = 3955  # Minecraft 1.21.1


def S(name, **props):
    """Un état de bloc : S("chest", facing="south")."""
    if ":" not in name:
        name = "minecraft:" + name
    return name, tuple(sorted((k, str(v).lower()) for k, v in props.items()))


AIR = S("air")
MACHINE = S(NS + ":compressed_machine")


class Build:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.block_nbt = {}
        self.entities = []

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, state, block_nbt=None):
        if not self.inside(x, y, z):
            return
        self.blocks[(x, y, z)] = state
        if block_nbt is not None:
            self.block_nbt[(x, y, z)] = block_nbt
        else:
            self.block_nbt.pop((x, y, z), None)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, state):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, state)

    def carve(self, x0, y0, z0, x1, y1, z1):
        """De l'air là où rien n'est posé : la roche ne rentre pas dans la place."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, AIR)

    def chest(self, x, y, z, facing, loot):
        self.set(x, y, z, S("chest", facing=facing, type="single", waterlogged=False),
                 Compound({"id": String("minecraft:chest"), "LootTable": String(NS + ":chests/" + loot)}))

    def armor_stand(self, x, y, z, yaw, head=None, stage=4):
        empty = Compound()
        armor = [empty, empty, empty, empty]
        if head:
            armor[3] = Compound({"id": String(NS + ":" + head), "count": Int(1),
                                 "components": Compound({NS + ":wither_stage": Int(stage)})})
        pos = [x + 0.5, float(y), z + 0.5]
        self.entities.append(Compound({
            "pos": List(Double, [Double(v) for v in pos]),
            "blockPos": List(Int, [Int(x), Int(y), Int(z)]),
            "nbt": Compound({
                "id": String("minecraft:armor_stand"),
                "Pos": List(Double, [Double(v) for v in pos]),
                "Rotation": List(Float, [Float(float(yaw)), Float(0.0)]),
                "ArmorItems": List(Compound, armor),
                "HandItems": List(Compound, [empty, empty]),
                "ShowArms": Byte(1),
            }),
        }))

    def save(self, name):
        palette, index = [], {}
        blocks = []
        for pos in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            state = self.blocks[pos]
            if state not in index:
                index[state] = len(palette)
                entry = {"Name": String(state[0])}
                if state[1]:
                    entry["Properties"] = Compound({k: String(v) for k, v in state[1]})
                palette.append(Compound(entry))
            block = {"pos": List(Int, [Int(v) for v in pos]), "state": Int(index[state])}
            if pos in self.block_nbt:
                block["nbt"] = self.block_nbt[pos]
            blocks.append(Compound(block))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List(Int, [Int(v) for v in self.size]),
            "palette": List(Compound, palette),
            "blocks": List(Compound, blocks),
            "entities": List(Compound, self.entities),
        })
        os.makedirs(OUT, exist_ok=True)
        nbt.write(os.path.join(OUT, name + ".nbt"), root)
        print(f"{name}: {self.size}, {len(blocks)} blocs, {len(palette)} états, {len(self.entities)} entités")


# ---------------------------------------------------------------------- matériaux
def brick(rnd, crack=0.3):
    r = rnd.random()
    if r < crack:
        return S("cracked_polished_blackstone_bricks")
    if r < crack + 0.1:
        return S("polished_blackstone")
    return S("polished_blackstone_bricks")


def nether_brick(rnd, crack=0.15):
    return S("cracked_nether_bricks") if rnd.random() < crack else S("nether_bricks")


BRASS = S("waxed_exposed_cut_copper")          # le laiton terni
BRASS_SLAB = S("waxed_exposed_cut_copper_slab", type="bottom", waterlogged=False)
PIPE = S("polished_basalt", axis="y")
COPPER = S("waxed_exposed_copper")


def copper_stairs(facing, half="bottom"):
    return S("waxed_exposed_cut_copper_stairs", facing=facing, half=half, shape="straight", waterlogged=False)


def campfire(facing="south", soul=False, lit=False):
    return S("soul_campfire" if soul else "campfire", facing=facing, lit=lit, signal_fire=False, waterlogged=False)


# ---------------------------------------------------------------------- la forge en ruine
def forge_ruin(variant):
    rnd = random.Random({"base": 11, "crimson": 12, "soul": 13}[variant])
    soul = variant == "soul"
    b = Build(13, 8, 11)
    # le sol, grignoté sur les bords
    for x in range(13):
        for z in range(11):
            if x in (0, 12) and z in (0, 10):
                continue
            if (x in (0, 12) or z in (0, 10)) and rnd.random() < 0.35:
                continue
            b.set(x, 0, z, brick(rnd))
    b.carve(1, 1, 1, 11, 6, 9)
    # le mur du fond, effondré par endroits
    tops = [3, 4, 5, 6, 6, 6, 6, 6, 6, 6, 5, 3, 2]
    for x in range(1, 12):
        top = max(1, tops[x] - rnd.choice((0, 0, 1, 2)))
        for y in range(1, top + 1):
            b.set(x, y, 1, brick(rnd))
    for x, y in ((2, 3), (10, 3)):
        if b.get(x, y, 1):
            b.set(x, y, 1, S("iron_bars"))
    # l'âtre : deux jambages, un linteau de laiton, la hotte ; dedans un feu éteint sur un lit de cendre
    for z in (2, 3):
        for y in (1, 2, 3):
            b.set(4, y, z, S("polished_blackstone"))
            b.set(8, y, z, S("polished_blackstone"))
    b.fill(4, 4, 2, 8, 4, 3, BRASS)
    b.fill(5, 5, 2, 7, 5, 2, S("polished_blackstone_brick_stairs", facing="north", half="bottom", shape="straight", waterlogged=False))
    b.fill(5, 0, 2, 7, 0, 3, S("smooth_basalt"))
    b.fill(5, 1, 2, 7, 3, 3, AIR)
    b.set(6, 1, 2, campfire("south", soul))
    # la cheminée : un moignon debout, le reste couché au sol
    b.fill(5, 6, 1, 7, 6, 2, PIPE)
    b.set(6, 6, 1, BRASS)
    for x in range(6, 11):
        b.set(x, 1, 8, S("polished_basalt", axis="x"))
    for x in (7, 8, 9):
        b.set(x, 1, 7, S("polished_basalt", axis="x"))
    b.set(8, 2, 8, BRASS_SLAB)
    # les murs de côté, bas
    for z in range(2, 8):
        for y in range(1, 1 + rnd.choice((1, 1, 2, 3))):
            b.set(1, y, z, brick(rnd))
    for z in range(2, 6):
        for y in range(1, 1 + rnd.choice((1, 2))):
            b.set(11, y, z, brick(rnd))
    # des machines comprimées prises dans les murs
    for pos in ((1, 2, 1), (11, 2, 1), (1, 1, 5)):
        if b.get(*pos):
            b.set(*pos, MACHINE)
    # l'enclume ébréchée, le coffre, des gravats
    b.set(3, 1, 5, S("damaged_anvil", facing="east"))
    b.chest(10, 1, 3, "west", "forge_ruin")
    for _ in range(6):
        x, z = rnd.randint(2, 10), rnd.randint(4, 9)
        if (x, 1, z) not in b.blocks or b.get(x, 1, z) == AIR:
            b.set(x, 1, z, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
    # la forêt carmin a repris la forge ; la vallée des âmes l'a ensevelie
    if variant == "crimson":
        for (x, y, z), state in list(b.blocks.items()):
            if y == 0 and rnd.random() < 0.4:
                b.set(x, y, z, S("crimson_nylium"))
                if b.get(x, 1, z) in (None, AIR) and rnd.random() < 0.35:
                    b.set(x, 1, z, S("crimson_roots") if rnd.random() < 0.7 else S("crimson_fungus"))
        for x in (5, 7):
            b.set(x, 3, 3, S("weeping_vines_plant"))
            b.set(x, 2, 3, S("weeping_vines", age=rnd.randint(10, 20)))
        b.set(3, 5, 1, S("shroomlight"))
    elif soul:
        for (x, y, z), state in list(b.blocks.items()):
            if y == 0 and rnd.random() < 0.4:
                b.set(x, y, z, S("soul_soil"))
        b.set(9, 1, 6, S("bone_block", axis="x"))
        b.set(2, 1, 8, S("bone_block", axis="z"))
    return b


# ---------------------------------------------------------------------- la forge d'une forteresse
def fortress_annex():
    rnd = random.Random(21)
    b = Build(15, 15, 13)
    plat = 4
    for x, z in ((1, 3), (8, 3), (13, 3), (1, 11), (8, 11), (13, 11)):   # les piliers (prolongés jusqu'au sol à la génération)
        b.fill(x, 0, z, x, plat - 1, z, S("nether_bricks"))
    for x in range(15):
        for z in range(2, 13):
            b.set(x, plat, z, nether_brick(rnd))
    for x in range(4, 7):                                    # le pont rompu, vers la forteresse
        for z in range(0, 2):
            if not (z == 0 and x == 6) and rnd.random() < 0.85:
                b.set(x, plat, z, nether_brick(rnd))
    for x in range(15):                                       # la rambarde, comme celle des ponts
        for z in range(2, 13):
            edge = x in (0, 14) or z == 12 or (z == 2 and not 4 <= x <= 6)
            if edge and rnd.random() < 0.85:
                b.set(x, plat + 1, z, S("nether_brick_fence"))
    b.carve(1, plat + 1, 3, 13, plat + 10, 11)
    # la forge : murs, porte vers le pont, fenêtres à barreaux de briques, toit
    for x in range(2, 9):
        for z in range(4, 11):
            if x in (2, 8) or z in (4, 10):
                for y in range(plat + 1, plat + 6):
                    b.set(x, y, z, nether_brick(rnd))
    b.fill(5, plat + 1, 4, 5, plat + 2, 4, AIR)
    for x, z in ((2, 6), (2, 8), (3, 4), (7, 4)):
        b.set(x, plat + 3, z, S("nether_brick_fence"))
    for x in range(2, 9):
        for z in range(4, 11):
            if rnd.random() < 0.9:
                b.set(x, plat + 6, z, S("nether_brick_slab", type="bottom", waterlogged=False))
    b.fill(3, plat + 1, 5, 7, plat + 5, 9, AIR)
    # l'âtre contre le mur du fond, et sa cheminée qui perce le toit
    for y in range(plat + 1, plat + 4):
        b.set(4, y, 9, S("polished_blackstone"))
        b.set(6, y, 9, S("polished_blackstone"))
    b.fill(4, plat + 4, 9, 6, plat + 4, 9, BRASS)
    b.set(5, plat + 1, 9, campfire("north"))
    b.fill(5, plat + 5, 10, 5, plat + 10, 10, PIPE)
    for y in (plat + 7, plat + 9):
        b.set(5, y, 10, BRASS)
    # dehors, contre le mur est : la cuve qui donnait la vapeur, dans son bâti de basalte, et sa conduite vers la forge
    for x in range(10, 13):
        for z in range(6, 9):
            post = x != 11 and z != 7
            for y in range(plat + 1, plat + 5):
                if post:
                    b.set(x, y, z, PIPE)
                else:
                    b.set(x, y, z, S("polished_blackstone") if y == plat + 1 else BRASS if y == plat + 3 else COPPER)
            b.set(x, plat + 5, z, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False) if post else BRASS_SLAB)
    b.set(11, plat + 5, 7, BRASS)
    b.fill(9, plat + 5, 7, 10, plat + 5, 7, S("polished_basalt", axis="x"))
    b.set(11, plat + 2, 8, S("waxed_exposed_copper_bulb", lit=False, powered=False))
    # dedans : la machine (machines comprimées et chaînes), l'enclume, le coffre
    b.fill(3, plat + 1, 5, 3, plat + 2, 6, MACHINE)
    b.fill(3, plat + 3, 5, 3, plat + 5, 5, S("chain", axis="y", waterlogged=False))
    b.set(5, plat + 1, 7, S("damaged_anvil", facing="west"))
    b.chest(7, plat + 1, 5, "west", "fortress_annex")
    return b


# ---------------------------------------------------------------------- la chaufferie et sa salle scellée
def boiler(b, cx, cz, top, front="south"):
    """Une chaudière debout : une cuve ronde de cuivre (5 sur 5, coins rognés) cerclée de pierre noire, son foyer
    éteint et sa grille, un manomètre (une ampoule de cuivre éteinte), un dôme, et sa conduite jusqu'au plafond."""
    cells = [(x, z) for x in range(cx - 2, cx + 3) for z in range(cz - 2, cz + 3) if abs(x - cx) + abs(z - cz) < 4]
    for x, z in cells:
        b.set(x, 1, z, S("polished_blackstone"))
        b.set(x, 2, z, S("polished_blackstone_bricks"))
        for y in (3, 4, 6):
            b.set(x, y, z, COPPER)
        b.set(x, 5, z, BRASS)
        b.set(x, 7, z, S("polished_blackstone_bricks"))
        if abs(x - cx) == 2 or abs(z - cz) == 2:                      # le dôme, en marches vers le centre
            facing = "south" if z == cz - 2 else "north" if z == cz + 2 else "east" if x == cx - 2 else "west"
            b.set(x, 8, z, copper_stairs(facing))
        else:
            b.set(x, 8, z, BRASS)
            if (x, z) != (cx, cz):
                if x == cx or z == cz:
                    facing = "south" if z == cz - 1 else "north" if z == cz + 1 else "east" if x == cx - 1 else "west"
                    b.set(x, 9, z, copper_stairs(facing))
                else:
                    b.set(x, 9, z, BRASS_SLAB)
    fx, fz = {"south": (cx, cz + 2), "north": (cx, cz - 2), "east": (cx + 2, cz), "west": (cx - 2, cz)}[front]
    b.set(fx, 1, fz, campfire(front))                                 # le foyer, éteint
    b.set(fx, 2, fz, S("iron_bars"))                                  # sa grille
    b.set(fx, 4, fz, S("waxed_exposed_copper_bulb", lit=False, powered=False))   # le manomètre
    b.fill(cx, 9, cz, cx, top + 1, cz, PIPE)


def boiler_hall():
    rnd = random.Random(31)
    W = 18                       # le mur est, partagé avec la salle scellée
    b = Build(W + 7, 14, 19)
    top = 12
    # la coque : sol, plafond de basalte lisse, murs et piliers
    for x in range(0, W + 1):
        for z in range(0, 19):
            b.set(x, 0, z, brick(rnd, 0.25))
            b.set(x, top, z, S("smooth_basalt"))
            if x in (0, W) or z in (0, 18):
                for y in range(1, top):
                    pillar = (x in (0, W) and z % 4 == 3) or (z in (0, 18) and x % 4 == 3)
                    b.set(x, y, z, PIPE if pillar else brick(rnd, 0.2))
    b.carve(1, 1, 1, W - 1, top - 1, 17)
    # l'entrée, au sud : le couloir part de là (prolongé jusqu'au dehors à la génération)
    b.fill(8, 1, 18, 10, 3, 18, AIR)
    # trois chaudières contre le mur nord ; un collecteur relie leurs conduites, qui se perdent dans le plafond
    for cx in (3, 9, 15):
        boiler(b, cx, 3, top)
    b.fill(1, 10, 3, W - 1, 10, 3, S("polished_basalt", axis="x"))
    for cx in (3, 9, 15):
        b.set(cx, 10, 3, BRASS)
    # deux puits d'aération au-dessus de feux de camp : la fumée monte jusqu'au dehors
    for vx in (4, 14):
        b.fill(vx, top, 12, vx, top + 1, 12, AIR)
        b.set(vx, 1, 12, campfire("south"))
    # des piliers ; l'établi en ruine contre le mur ouest, son palan qui tient encore une machine ; deux coffres
    for x, z in ((6, 9), (12, 9), (6, 15), (12, 15)):
        b.fill(x, 1, z, x, top - 1, z, PIPE)
    b.fill(1, 1, 10, 1, 1, 13, S("polished_blackstone_brick_slab", type="top", waterlogged=False))
    b.set(1, 1, 14, S("damaged_anvil", facing="south"))
    b.set(1, 1, 9, S("grindstone", face="floor", facing="east"))
    b.fill(2, 3, 12, 2, top - 1, 12, S("chain", axis="y", waterlogged=False))
    b.set(2, 2, 12, MACHINE)
    b.chest(1, 1, 16, "east", "boiler_hall")
    b.chest(W - 1, 1, 16, "west", "boiler_hall")
    # la salle scellée, derrière le mur est
    for x in range(W + 1, W + 7):
        for z in range(5, 14):
            for y in range(0, 7):
                wall = x == W + 6 or z in (5, 13) or y in (0, 6)
                b.set(x, y, z, brick(rnd, 0.15) if wall else AIR)
    # sa porte : des machines comprimées dans un cadre de basalte, sous un linteau de laiton et son manomètre
    b.fill(W, 1, 8, W, 3, 10, MACHINE)
    for z in (7, 11):
        b.fill(W, 1, z, W, 3, z, PIPE)
    b.fill(W, 4, 6, W, 4, 12, BRASS)
    b.set(W, 5, 9, S("waxed_exposed_copper_bulb", lit=False, powered=False))
    # la vieille chaudière qui l'ouvre : son chaudron (l'eau) et son foyer (le feu), reliés au linteau
    b.set(W - 1, 1, 6, S("cauldron"))
    b.set(W - 1, 1, 12, S("furnace", facing="west", lit=False))
    for z in (6, 12):
        b.fill(W - 1, 2, z, W - 1, 4, z, S("lightning_rod", facing="up", powered=False, waterlogged=False))
    # derrière : le meilleur coffre, et des machines rangées là
    b.chest(W + 5, 1, 9, "west", "boiler_hall_sealed")
    b.fill(W + 5, 1, 6, W + 5, 2, 6, MACHINE)
    b.fill(W + 5, 1, 12, W + 5, 1, 11, MACHINE)
    b.set(W + 1, 1, 12, MACHINE)
    b.fill(W + 3, 3, 9, W + 3, 5, 9, S("chain", axis="y", waterlogged=False))
    return b


def boiler_hall_gate():
    """L'entrée de la chaufferie, posée là où son couloir débouche à l'air libre (z = 1 : la face dehors)."""
    rnd = random.Random(32)
    b = Build(7, 6, 2)
    for x in range(1, 6):
        for y in range(0, 5):
            b.set(x, y, 0, brick(rnd, 0.2))
    b.fill(2, 0, 1, 4, 0, 1, S("polished_blackstone_bricks"))         # le seuil
    for x in (1, 5):
        b.fill(x, 0, 1, x, 3, 1, PIPE)                                # les montants
    b.fill(1, 4, 1, 5, 4, 1, BRASS)                                   # le linteau
    b.fill(2, 5, 1, 4, 5, 1, S("polished_blackstone_bricks"))
    for x in (1, 5):
        b.set(x, 5, 1, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
    b.fill(2, 1, 0, 4, 3, 1, AIR)                                     # le passage
    b.fill(2, 3, 1, 4, 3, 1, S("iron_bars"))                          # la herse, restée levée
    return b


# ---------------------------------------------------------------------- le dernier foyer
def last_hearth():
    rnd = random.Random(41)
    b = Build(15, 5, 15)
    c = 7
    for x in range(15):
        for z in range(15):
            d = ((x - c) ** 2 + (z - c) ** 2) ** 0.5
            if d <= 7.2:
                b.set(x, 0, z, S("smooth_basalt") if rnd.random() < 0.15 else brick(rnd, 0.3))
    b.carve(0, 1, 0, 14, 3, 14)
    for x in range(15):                                          # ce qui n'est pas sur le disque reste en place
        for z in range(15):
            if ((x - c) ** 2 + (z - c) ** 2) ** 0.5 > 7.2:
                for y in range(1, 4):
                    b.blocks.pop((x, y, z), None)
    # le grand âtre : une braise qui couve encore, cerclée de pierre
    for x in range(5, 10):
        for z in range(5, 10):
            if x in (5, 9) or z in (5, 9):
                b.set(x, 1, z, S("polished_blackstone_bricks"))
    b.fill(6, 0, 6, 8, 0, 8, S("magma_block"))
    # les feux de camp autour, les bancs
    for dx, dz in ((4, 0), (-4, 0), (0, 4), (3, 3), (-3, 3), (3, -3), (-3, -3)):
        b.set(c + dx, 1, c + dz, campfire())
    for dx, dz, facing in ((6, 0, "west"), (-6, 0, "east"), (0, 6, "north"), (5, -4, "west"), (-5, -4, "east")):
        b.set(c + dx, 1, c + dz, S("polished_blackstone_brick_stairs", facing=facing, half="bottom", shape="straight", waterlogged=False))
    # un pan de mur au nord, le porte-armure et son casque, le coffre
    for x in range(3, 12):
        for y in range(1, 1 + rnd.choice((1, 2, 2, 3))):
            b.set(x, y, 1, brick(rnd))
    b.armor_stand(7, 1, 3, 0, head="wither_helmet")
    b.chest(9, 1, 2, "south", "last_hearth")
    return b


# ---------------------------------------------------------------------- les cheminées du plafond
def ceiling_chimneys():
    rnd = random.Random(51)
    b = Build(9, 20, 9)
    # la grande conduite, creuse, cerclée de laiton, qui s'enfonce dans le plafond
    for y in range(4, 20):
        for x in range(3, 6):
            for z in range(3, 6):
                if (x, z) == (4, 4):
                    b.set(x, y, z, AIR)
                else:
                    b.set(x, y, z, BRASS if y in (7, 11, 15) else PIPE)
    for x in range(3, 6):
        for z in range(3, 6):
            if (x, z) != (4, 4):
                b.set(x, 3, z, S("iron_bars"))
    # une plus fine, rompue, et des chaînes
    b.fill(7, 10, 6, 7, 19, 6, PIPE)
    b.set(7, 13, 6, BRASS)
    b.fill(7, 7, 6, 7, 9, 6, S("chain", axis="y", waterlogged=False))
    b.fill(1, 13, 1, 1, 19, 1, PIPE)
    b.set(1, 16, 1, BRASS)
    b.fill(6, 14, 2, 6, 19, 2, S("chain", axis="y", waterlogged=False))
    return b


forge_ruin("base").save("forge_ruin")
forge_ruin("crimson").save("forge_ruin_crimson")
forge_ruin("soul").save("forge_ruin_soul")
fortress_annex().save("fortress_annex")
boiler_hall().save("boiler_hall")
boiler_hall_gate().save("boiler_hall_gate")
last_hearth().save("last_hearth")
ceiling_chimneys().save("ceiling_chimneys")
