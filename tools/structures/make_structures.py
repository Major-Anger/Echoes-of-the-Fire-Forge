"""Les structures de la Forge (plan, section 12b ; aperçu en attente de validation d'Anger) : des gabarits de structure
(.nbt) bâtis en blocs vanilla et blocs de la Forge, sans rien reprendre des structures de Mojang (règle 4).

Usage : python3 tools/structures/make_structures.py <dossier de sortie (data/echoes_fireforge/structure)>

- forge_ruin (terres désolées, rives de la mer de lave), forge_ruin_crimson (reprise par la forêt carmin),
  forge_ruin_soul (vallée des âmes) : une forge du quotidien, abandonnée ;
- fortress_annex : la forge d'une forteresse (les forteresses sont des ouvrages des Attiseurs), sur piliers ;
- boiler_hall : la chaufferie, creusée dans la roche, et sa salle scellée ; boiler_hall_gate : son entrée, posée là où
  son couloir débouche à l'air libre ;
- last_hearth : le dernier foyer ;
- ceiling_chimneys : les cheminées qui pendent du plafond de bedrock ;
- forge_arena : l'arène du maître de forge (14b) ;
- grand_machine, grand_machine_pier : la Grande Machine et son quai (section 15).
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
    # sa porte : des machines scellées (l'aspect des machines comprimées) dans un cadre de basalte, sous un linteau
    # de laiton et son manomètre ; elle ne s'ouvre qu'à la vapeur (structure/SealedDoorBlock)
    b.fill(W, 1, 8, W, 3, 10, S(NS + ":sealed_machine", lock=False))
    b.set(W, 1, 9, S(NS + ":sealed_machine", lock=True))           # la serrure : elle surveille la vieille chaudière
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


# ---------------------------------------------------------------------- l'arène du maître de forge (plan, 14b ; aperçu)
def forge_arena():
    """La grande forge du maître : une salle de 60 x 60, 30 de haut. Au centre, son estrade, son âtre et son enclume ;
    aux quatre coins, des foyers éteints (il les rallume en phase 2) ; autour, des gradins ; une seule entrée, au sud."""
    rnd = random.Random(61)
    N, TOP = 60, 29
    GRATE = S("iron_trapdoor", facing="north", half="bottom", open=False, powered=False, waterlogged=False)                # une grille de fer à plat, sur la lave
    b = Build(N, TOP + 1, N)
    c = N // 2
    # la coque : sol, murs à piliers et bandes de laiton, plafond à poutres
    for x in range(N):
        for z in range(N):
            ring = max(abs(x - c + 0.5), abs(z - c + 0.5))
            b.set(x, 0, z, S("smooth_basalt") if int(ring) % 6 == 0 else brick(rnd, 0.15))
            b.set(x, TOP, z, S("polished_basalt", axis="x") if z % 10 == 5 else S("smooth_basalt"))
            if x in (0, N - 1) or z in (0, N - 1):
                for y in range(1, TOP):
                    pillar = (x in (0, N - 1) and z % 6 == 0) or (z in (0, N - 1) and x % 6 == 0)
                    band = y in (9, 18)
                    b.set(x, y, z, PIPE if pillar else BRASS if band else brick(rnd, 0.12))
    b.carve(1, 1, 1, N - 2, TOP - 1, N - 2)
    # les gradins : trois marches de pierre noire le long des murs (sauf aux coins et devant l'entrée)
    for step in range(3):
        d = 1 + step * 2                                     # la marche la plus haute contre le mur
        h = 3 - step
        for i in range(8, N - 8):
            for side in ("n", "s", "w", "e"):
                if side == "s" and abs(i - c + 0.5) < 5:
                    continue                                 # l'entrée
                for dd in (d, d + 1):
                    x, z = {"n": (i, dd), "s": (i, N - 1 - dd), "w": (dd, i), "e": (N - 1 - dd, i)}[side]
                    for y in range(1, h + 1):
                        b.set(x, y, z, S("polished_blackstone_bricks") if y < h else S("polished_blackstone"))
    # au pied des gradins, une rigole de lave sous des grilles (la lumière de la forge)
    for i in range(8, N - 8):
        for side in ("n", "s", "w", "e"):
            if side == "s" and abs(i - c + 0.5) < 5:
                continue
            x, z = {"n": (i, 7), "s": (i, N - 8), "w": (7, i), "e": (N - 8, i)}[side]
            b.set(x, 0, z, S("lava", level=0))
            b.set(x, 1, z, GRATE)
    # les quatre foyers éteints, aux coins : un âtre sous une hotte de laiton, sa conduite jusqu'au plafond
    for fx, fz in ((10, 10), (N - 11, 10), (10, N - 11), (N - 11, N - 11)):
        for x in range(fx - 2, fx + 3):
            for z in range(fz - 2, fz + 3):
                edge = abs(x - fx) == 2 or abs(z - fz) == 2
                b.set(x, 1, z, S("polished_blackstone_bricks") if edge else S("smooth_basalt"))
        b.set(fx, 2, fz, campfire("south"))
        for x in range(fx - 1, fx + 2):
            for z in range(fz - 1, fz + 2):
                b.set(x, 6, z, BRASS)
        b.fill(fx, 7, fz, fx, TOP, fz, PIPE)
        for y in (12, 20):
            b.set(fx, y, fz, BRASS)
        for x, z in ((fx - 1, fz - 1), (fx + 1, fz - 1), (fx - 1, fz + 1), (fx + 1, fz + 1)):
            b.fill(x, 2, z, x, 5, z, S("chain", axis="y", waterlogged=False))
    # au centre : l'estrade du maître, cerclée d'une rigole de lave sous des grilles
    for x in range(c - 9, c + 9):
        for z in range(c - 9, c + 9):
            r = ((x - c + 0.5) ** 2 + (z - c + 0.5) ** 2) ** 0.5
            if 7.0 <= r < 8.0:
                b.set(x, 0, z, S("lava", level=0))
                b.set(x, 1, z, GRATE)
            elif r < 7.0:
                b.set(x, 1, z, S("polished_blackstone_bricks") if r >= 6 else S("smooth_basalt"))
    for x, z, facing in ((c - 1, c + 7, "north"), (c, c + 7, "north"), (c - 1, c - 8, "south"), (c, c - 8, "south"),
                         (c + 7, c - 1, "west"), (c + 7, c, "west"), (c - 8, c - 1, "east"), (c - 8, c, "east")):
        b.set(x, 1, z, S("polished_blackstone_brick_stairs", facing=facing, half="bottom", shape="straight", waterlogged=False))
    # son âtre : un grand foyer de braise sous une hotte, et la grande cheminée qui monte au plafond
    for x in range(c - 3, c + 3):
        for z in range(c - 5, c - 2):
            b.set(x, 2, z, S("polished_blackstone_bricks"))
    b.fill(c - 2, 2, c - 4, c + 1, 2, c - 4, S("magma_block"))
    b.fill(c - 2, 3, c - 4, c + 1, 3, c - 4, S("iron_bars"))
    for x in range(c - 3, c + 3):
        for z in range(c - 5, c - 2):
            b.set(x, 7, z, BRASS)
    for x in (c - 3, c + 2):
        for z in (c - 5, c - 3):
            b.fill(x, 3, z, x, 6, z, PIPE)
    for x in range(c - 1, c + 1):
        for z in range(c - 5, c - 3):
            b.fill(x, 8, z, x, TOP, z, PIPE)
    for y in (12, 18, 24):
        for x in range(c - 1, c + 1):
            for z in range(c - 5, c - 3):
                b.set(x, y, z, BRASS)
    # sa grande enclume, devant l'âtre
    b.fill(c - 2, 2, c + 1, c + 1, 2, c + 2, S("polished_blackstone"))
    b.fill(c - 1, 3, c + 1, c, 3, c + 2, PIPE)
    b.fill(c - 3, 4, c + 1, c + 2, 4, c + 2, S("smooth_basalt"))
    b.set(c - 1, 5, c + 1, S("damaged_anvil", facing="east"))
    # aux murs : de grandes conduites, des machines comprimées, des chaînes qui pendent
    for i in range(6, N - 6, 12):
        for wall in ("n", "w", "e"):
            x, z = {"n": (i, 1), "w": (1, i), "e": (N - 2, i)}[wall]
            b.fill(x, 4, z, x, TOP - 1, z, PIPE)
            b.set(x, 3, z, MACHINE)
    for x, z in ((c - 12, c - 12), (c + 12, c - 12), (c - 12, c + 12), (c + 12, c + 12), (c, c + 14), (c - 14, c), (c + 14, c)):
        b.fill(x, TOP - 8, z, x, TOP - 1, z, S("chain", axis="y", waterlogged=False))
        b.set(x, TOP - 9, z, S("lantern", hanging=True, waterlogged=False))
    # l'entrée, au sud : une porte de 5 de large, 5 de haut, sous un linteau de laiton (la porte se ferme en 14b)
    b.fill(c - 3, 1, N - 1, c + 2, 6, N - 1, AIR)
    for x in (c - 3, c + 2):
        b.fill(x, 1, N - 1, x, 6, N - 1, PIPE)
    b.fill(c - 3, 7, N - 1, c + 2, 7, N - 1, BRASS)
    b.chest(c + 3, 3, c + 1, "west", "forge_arena")
    # sous l'âtre, la forge du maître (forgemaster/MasterForgeBlockEntity) : elle le fait apparaître, endormi
    b.set(c - 1, 1, c - 4, S(NS + ":master_forge"), Compound({"id": String(NS + ":master_forge"), "fresh": Byte(1)}))
    return b


# ---------------------------------------------------------------------- la Grande Machine (plan, section 15)
GM_N, GM_BASE, GM_TOP = 40, 18, 131                 # 40 x 40 ; des pieds (y 18, dans la lave) au-dessus du toit (y 131)
GM_FLOORS = (36, 52, 70, 90, 106)                   # les planchers ; le dernier est le toit
GM_ENTRY = (18, 21)                                 # la porte, à l'ouest (z), au niveau du premier plancher


def _rot(k, x, z, n=GM_N):
    """Le quart de tour k (0 : le mur nord) d'une position, autour du centre de la machine."""
    for _ in range(k):
        x, z = n - 1 - z, x
    return x, z


_FACING = ["north", "east", "south", "west"]


def _turn(facing, k):
    return _FACING[(_FACING.index(facing) + k) % 4]


def _flight(b, rnd, k, floor, upper, sealed):
    """Un escalier contre un mur (k : 0 nord, 1 est, 2 sud, 3 ouest) qui monte du plancher `floor` au plancher `upper`,
    et en haut le sas : une petite pièce fermée, sa porte scellée (un chaudron plein d'eau et un fourneau allumé
    l'ouvrent : la machine manque d'eau, c'est le joueur qui l'apporte)."""
    Y = lambda y: y - GM_BASE
    n = upper - floor
    def put(x, y, z, state, nbt_=None):
        wx, wz = _rot(k, x, z)
        b.set(wx, Y(y), wz, state, nbt_)
    for step in range(1, n + 1):
        x = 4 + step
        for z in (1, 2, 3):
            put(x, floor + step, z, S("polished_blackstone_brick_stairs", facing=_turn("east", k), half="bottom", shape="straight", waterlogged=False))
            for y in range(floor + 1, floor + step):
                put(x, y, z, brick(rnd, 0.1))
        for y in range(floor + step + 1, floor + step + 4):     # la place pour la tête
            for z in (1, 2, 3):
                put(x, y, z, AIR)
    for x in range(4 + n - 5, 4 + n):                             # le passage dans le plancher du dessus
        for z in (1, 2, 3):
            put(x, upper, z, AIR)
    if not sealed:
        return
    x0, x1 = 4 + n - 6, 4 + n + 5
    for x in range(x0, x1 + 1):
        for z in range(0, 7):
            for y in range(upper + 1, upper + 6):
                wall = x in (x0, x1) or z == 6 or y == upper + 5
                if wall:
                    put(x, y, z, S("polished_basalt", axis="y") if (x in (x0, x1) and z == 6) else brick(rnd, 0.1))
                elif not (z in (1, 2, 3) and 4 + n - 5 <= x < 4 + n):
                    put(x, y, z, AIR)
    dx = 4 + n + 1
    for x in range(dx, dx + 3):
        for y in range(upper + 1, upper + 4):
            put(x, y, 6, S(NS + ":sealed_machine", lock=False))
    put(dx + 1, upper + 1, 6, S(NS + ":sealed_machine", lock=True))
    for x in range(dx - 1, dx + 4):
        put(x, upper + 4, 6, BRASS)
    put(dx + 3, upper + 1, 4, S("cauldron"))
    put(dx + 3, upper + 1, 2, S("furnace", facing=_turn("west", k), lit=False))
    for z in (2, 4):
        put(dx + 3, upper + 2, z, S("lightning_rod", facing="up", powered=False, waterlogged=False))
        put(dx + 3, upper + 3, z, S("lightning_rod", facing="up", powered=False, waterlogged=False))
    put(dx + 1, upper + 4, 5, S("waxed_exposed_copper_bulb", lit=False, powered=False))   # le manomètre, éteint


class _Shifted:
    """Une vue décalée d'un gabarit : on y écrit en coordonnées de la coque, le gabarit garde sa marge."""

    def __init__(self, build, d):
        self.build, self.d = build, d

    def set(self, x, y, z, state, block_nbt=None):
        self.build.set(x + self.d, y, z + self.d, state, block_nbt)

    def get(self, x, y, z):
        return self.build.get(x + self.d, y, z + self.d)

    def fill(self, x0, y0, z0, x1, y1, z1, state):
        self.build.fill(x0 + self.d, y0, z0 + self.d, x1 + self.d, y1, z1 + self.d, state)

    def carve(self, x0, y0, z0, x1, y1, z1):
        self.build.carve(x0 + self.d, y0, z0 + self.d, x1 + self.d, y1, z1 + self.d)

    def chest(self, x, y, z, facing, loot):
        self.build.chest(x + self.d, y, z + self.d, facing, loot)


def grand_machine():
    """La Grande Machine : 40 x 40 (le gabarit en fait 42 : une marge d'un bloc pour les conduites du dehors ; la coque
    commence donc en (1, 1) du gabarit), une centaine de blocs de haut. Les pieds dans la mer de lave ; on entre à l'ouest,
    au premier plancher (y 36), et on monte : 1. les foyers (encore allumés), 2. les cuves (vides, les marques de l'eau),
    3. les pistons, 4. le cœur et son régulateur, puis le toit, et les conduites qui percent le bedrock et ressortent
    dessus. Les blocs non décrits ne sont pas posés : sous la coque, la lave de la caverne reste."""
    rnd = random.Random(71)
    N = GM_N
    raw = Build(N + 2, GM_TOP - GM_BASE + 1, N + 2)     # une marge d'un bloc autour de la coque, pour ce qui dépasse
    b = _Shifted(raw, 1)                               # dans ce qui suit, (0, 0) est le coin de la coque
    Y = lambda y: y - GM_BASE
    GRATE = S("iron_trapdoor", facing="north", half="bottom", open=False, powered=False, waterlogged=False)
    CHAIN = S("chain", axis="y", waterlogged=False)

    # les pieds, plantés dans la lave, et leurs traverses
    legs = ((3, 3), (33, 3), (3, 33), (33, 33), (18, 18))
    for lx, lz in legs:
        for x in range(lx, lx + 4):
            for z in range(lz, lz + 4):
                for y in range(GM_BASE, 36):
                    b.set(x, Y(y), z, BRASS if y in (25, 31) else PIPE)
    for i in range(7, 33):
        for a in (4, 5):
            for y in (34, 35):
                b.set(i, Y(y), a, S("polished_basalt", axis="x"))
                b.set(i, Y(y), N - 1 - a, S("polished_basalt", axis="x"))
                b.set(a, Y(y), i, S("polished_basalt", axis="z"))
                b.set(N - 1 - a, Y(y), i, S("polished_basalt", axis="z"))

    # les fondations (15c), sous le premier plancher : une salle entre les pieds, noyée de lave (elle se vide quand on
    # baisse la pression : machine/MachineChoice). De vieilles chaudières, des machines, deux coffres ; un escalier
    # descend de la trappe (machines scellées, dans le premier plancher), qui s'ouvre une fois la salle vidée.
    F0, F1, FB, FT = 8, 31, 22, 35
    for x in range(F0, F1 + 1):
        for z in range(F0, F1 + 1):
            wall = x in (F0, F1) or z in (F0, F1)
            for y in range(FB, FT + 1):
                if y == FB:
                    b.set(x, Y(y), z, S("smooth_basalt"))
                elif wall:
                    b.set(x, Y(y), z, brick(rnd, 0.2))
                elif not (18 <= x <= 21 and 18 <= z <= 21):        # le pied du milieu traverse la salle
                    b.set(x, Y(y), z, S("lava", level=0))
    for bx, bz in ((13, 25), (25, 25), (25, 12)):                    # de vieilles chaudières de cuivre
        for x in range(bx - 1, bx + 2):
            for z in range(bz - 1, bz + 2):
                for y in range(23, 29):
                    b.set(x, Y(y), z, BRASS if y in (23, 28) else COPPER)
        b.fill(bx, Y(29), bz, bx, Y(35), bz, PIPE)
    for x, z in ((10, 28), (29, 18), (29, 29), (16, 10)):
        b.fill(x, Y(23), z, x, Y(24), z, MACHINE)
    b.chest(12, Y(23), 21, "east", "grand_machine_foundations")
    b.chest(27, Y(23), 16, "west", "grand_machine_foundations")
    for x, z in ((15, 25), (25, 18), (15, 12)):
        b.set(x, Y(35), z, S("chain", axis="y", waterlogged=False))
        b.set(x, Y(34), z, S("lantern", hanging=True, waterlogged=False))
    # l'escalier, sous la trappe : il descend vers l'est
    for k in range(0, 13):
        for z in range(14, 17):
            b.set(10 + k, Y(35 - k), z, S("polished_blackstone_brick_stairs", facing="west", half="bottom", shape="straight", waterlogged=False))

    # la coque : murs à piliers, bandes de laiton, fenêtres à barreaux à chaque étage
    for y in range(36, 107):
        for x in range(N):
            for z in range(N):
                if not (x in (0, N - 1) or z in (0, N - 1)):
                    continue
                along = z if x in (0, N - 1) else x
                pillar = along % 8 == 3 or along in (0, N - 1)
                band = y in GM_FLOORS or y in (44, 61, 80, 98)
                b.set(x, Y(y), z, PIPE if pillar else BRASS if band else brick(rnd, 0.15))
    for fy, top in zip(GM_FLOORS[:4], GM_FLOORS[1:]):
        for along in range(N):
            if along % 8 not in (6, 7, 0) or along in (0, N - 1):
                continue
            for y in range(fy + 4, min(fy + 9, top - 2)):
                for x, z in ((along, 0), (along, N - 1), (0, along), (N - 1, along)):
                    b.set(x, Y(y), z, S("iron_bars", waterlogged=False))
    # les planchers (pierre noire, des lignes de basalte dans l'axe des piliers) ; le toit, et son parapet
    for fy in GM_FLOORS:
        for x in range(1, N - 1):
            for z in range(1, N - 1):
                line = x % 8 == 3 or z % 8 == 3
                if fy == 106:
                    b.set(x, Y(fy), z, S("smooth_basalt"))
                else:
                    b.set(x, Y(fy), z, S("smooth_basalt") if line else brick(rnd, 0.08))
    # la trappe des fondations, dans le premier plancher
    b.fill(10, Y(36), 14, 12, Y(36), 16, S(NS + ":sealed_machine", lock=False))
    for along in range(N):
        for x, z in ((along, 0), (along, N - 1), (0, along), (N - 1, along)):
            b.set(x, Y(107), z, S("polished_blackstone_brick_wall", east="none", north="none", south="none", west="none", up=True, waterlogged=False)
                  if along % 8 == 3 else S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))

    # l'entrée, à l'ouest : 4 de large, 4 de haut, sous un linteau de laiton
    z0, z1 = GM_ENTRY
    b.fill(0, Y(37), z0, 0, Y(40), z1, AIR)
    b.fill(0, Y(37), z0 - 1, 0, Y(41), z0 - 1, PIPE)
    b.fill(0, Y(37), z1 + 1, 0, Y(41), z1 + 1, PIPE)
    b.fill(0, Y(41), z0, 0, Y(41), z1, BRASS)

    # 1. les foyers (y 37 à 51) : quatre grands foyers de lave sous des grilles, leurs hottes et leurs conduites
    hearths = ((9, 9), (30, 9), (9, 30), (30, 30))
    for hx, hz in hearths:
        for x in range(hx - 3, hx + 4):
            for z in range(hz - 3, hz + 4):
                edge = abs(x - hx) == 3 or abs(z - hz) == 3
                if edge:
                    b.fill(x, Y(37), z, x, Y(38), z, S("polished_blackstone_bricks"))
                    b.set(x, Y(39), z, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
                else:
                    b.set(x, Y(37), z, S("lava", level=0))
                    b.set(x, Y(38), z, GRATE)
        for x in range(hx - 2, hx + 3):
            for z in range(hz - 2, hz + 3):
                b.set(x, Y(44), z, BRASS)
        for x in range(hx - 1, hx + 2):
            for z in range(hz - 1, hz + 2):
                b.set(x, Y(45), z, BRASS_SLAB)
        for x, z in ((hx - 2, hz - 2), (hx + 2, hz - 2), (hx - 2, hz + 2), (hx + 2, hz + 2)):
            b.fill(x, Y(40), z, x, Y(43), z, CHAIN)
        b.fill(hx, Y(46), hz, hx, Y(51), hz, PIPE)
        b.set(hx, Y(49), hz, BRASS)
    # du charbon en tas, des machines comprimées le long des murs, des chaînes au plafond
    for x, z in ((15, 6), (24, 33), (5, 24), (34, 15)):
        b.fill(x, Y(37), z, x + 1, Y(37), z + 1, S("coal_block"))
        b.set(x, Y(38), z, S("coal_block"))
    for x, z in ((1, 12), (1, 27), (38, 24), (12, 38), (27, 38)):
        b.fill(x, Y(37), z, x, Y(38), z, MACHINE)
    for x, z in ((14, 14), (25, 14), (14, 25), (25, 25), (20, 20)):
        b.fill(x, Y(46), z, x, Y(51), z, CHAIN)
        b.set(x, Y(45), z, S("lantern", hanging=True, waterlogged=False))

    # 2. les cuves (y 53 à 69) : deux cuves de cuivre, vides ; le vert-de-gris marque où l'eau est descendue
    tanks = ((12, 23), (27, 23))
    for tx, tz in tanks:
        for x in range(tx - 6, tx + 7):
            for z in range(tz - 6, tz + 7):
                r = ((x - tx) ** 2 + (z - tz) ** 2) ** 0.5
                if r > 6.4:
                    continue
                wall = r > 5.3
                for y in range(53, 68):
                    if wall:
                        mark = y in (56, 59, 63)
                        ring = y in (54, 61, 66)
                        b.set(x, Y(y), z, BRASS if ring else S("waxed_oxidized_copper") if mark else COPPER)
                    else:
                        b.set(x, Y(y), z, AIR)
                b.set(x, Y(68), z, BRASS if wall else COPPER)
        for x, z, h in ((tx - 2, tz + 1, 3), (tx + 1, tz - 2, 2), (tx + 2, tz + 2, 4), (tx - 1, tz - 3, 2)):
            b.set(x, Y(67), z, S("dripstone_block"))
            length = h - 1                                         # des stalactites : l'eau a coulé là longtemps
            for i in range(1, h):
                shape = "tip" if i == length else "frustum" if i == length - 1 else "base" if i == 1 else "middle"
                b.set(x, Y(67 - i), z, S("pointed_dripstone", thickness=shape, vertical_direction="down", waterlogged=False))
        b.fill(tx, Y(69), tz, tx, Y(70), tz, PIPE)                 # vers les pistons
    # une brèche dans chaque cuve, pour y entrer
    b.fill(11, Y(53), 17, 13, Y(56), 17, AIR)
    b.fill(26, Y(53), 29, 28, Y(55), 29, AIR)
    for x in (11, 13):
        b.set(x, Y(57), 17, S("waxed_exposed_cut_copper_stairs", facing="south", half="top", shape="straight", waterlogged=False))
    # les conduites des foyers montent jusqu'aux cuves
    for hx, hz in hearths:
        b.fill(hx, Y(53), hz, hx, Y(58), hz, PIPE)
        tz = 23
        step = 1 if hz < tz else -1
        z = hz + step
        tx = 12 if hx < 20 else 27
        while ((hx - tx) ** 2 + (z - tz) ** 2) ** 0.5 > 6.4:
            b.set(hx, Y(58), z, S("polished_basalt", axis="z"))
            z += step
        b.set(hx, Y(58), hz, BRASS)
    b.chest(20, Y(53), 30, "north", "grand_machine")

    # 3. les pistons (y 71 à 89) : six pistons géants ; leur moteur (machine/MachinePistonBlockEntity), caché dans le
    #    corps, fait monter et retomber leur tête, en vague (décalés d'une seconde) ; ils sont posés tête en bas.
    #    Dans le sol, six bouches de vapeur (machine/SteamVentBlockEntity), décalées elles aussi.
    for i, (px_, pz) in enumerate(((10, 11), (20, 11), (29, 11), (29, 26), (20, 26), (10, 26))):
        for x in range(px_ - 2, px_ + 2):
            for z in range(pz - 2, pz + 2):
                for y in range(71, 75):
                    b.set(x, Y(y), z, BRASS if y == 74 else S("polished_blackstone_bricks"))
        b.set(px_ - 1, Y(71), pz - 1, S(NS + ":machine_piston"),
              Compound({"id": String(NS + ":machine_piston"), "phase": Int(i * 20), "head": Int(4)}))
        for x in range(px_ - 3, px_ + 3):
            for z in range(pz - 3, pz + 3):
                b.set(x, Y(75), z, BRASS)
                b.set(x, Y(76), z, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
    for (x, z), phase in zip(((15, 18), (24, 18), (6, 19), (33, 33), (15, 31), (25, 6)), (0, 40, 10, 50, 20, 60)):
        b.set(x, Y(70), z, S(NS + ":steam_vent"), Compound({"id": String(NS + ":steam_vent"), "phase": Int(phase)}))
    b.fill(12, Y(70), 23, 12, Y(71), 23, PIPE)
    b.fill(27, Y(70), 23, 27, Y(71), 23, PIPE)

    # 4. le cœur (y 91 à 105) : une boule de cuivre ajouré où brûle la braise, sur son socle ; ses conduites vers le toit ;
    #    devant, le régulateur et ses deux vannes (baisser, pousser : les vraies vannes viendront en 15c)
    cx, cy, cz = 19.5, 98.5, 19.5
    for x in range(12, 28):
        for z in range(12, 28):
            for y in range(92, 106):
                r = ((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) ** 0.5
                if 5.0 < r <= 6.1:
                    rib = x in (19, 20) or z in (19, 20) or y == 98
                    b.set(x, Y(y), z, BRASS if rib else S("waxed_exposed_copper_grate", waterlogged=False))
                elif r <= 3.4:
                    b.set(x, Y(y), z, S("shroomlight") if (x + y + z) % 3 else S("magma_block"))
    for x in range(16, 24):
        for z in range(16, 24):
            b.set(x, Y(91), z, S("polished_blackstone_bricks"))
            if 17 <= x <= 22 and 17 <= z <= 22:
                b.set(x, Y(92), z, BRASS)
    pipes = ((10, 10), (29, 10), (10, 29), (29, 29))
    for px_, pz in pipes:
        dx = 1 if px_ > 20 else -1
        dz = 1 if pz > 20 else -1
        x = 25 if dx > 0 else 14
        while x != px_:
            b.set(x, Y(100), 20 if dz > 0 else 19, S("polished_basalt", axis="x"))
            x += dx
        z = 20 if dz > 0 else 19
        while z != pz:
            b.set(px_, Y(100), z, S("polished_basalt", axis="z"))
            z += dz
        b.set(px_, Y(100), pz, BRASS)
    # le régulateur (15c) : deux vannes identiques, à leur plaque près (− baisser, + pousser) ; la bouche du cœur, sous la boule
    b.set(14, Y(91), 29, S(NS + ":regulator_valve", kind="lower", turned=False))
    b.set(25, Y(91), 29, S(NS + ":regulator_valve", kind="raise", turned=False))
    b.fill(15, Y(91), 29, 24, Y(91), 29, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
    b.set(19, Y(92), 23, S(NS + ":heart_mouth"))

    # les conduites : de l'étage du cœur, à travers le toit, jusqu'au bedrock, qu'elles percent ; elles ressortent dessus
    for px_, pz in pipes:
        for x in range(px_ - 1, px_ + 2):
            for z in range(pz - 1, pz + 2):
                for y in range(91, GM_TOP + 1):
                    if y == GM_TOP and (x, z) == (px_, pz):
                        continue
                    if y > 106 or (x, z) == (px_, pz):
                        ring = y in (115, 122, 128)
                        b.set(x, Y(y), z, BRASS if ring else PIPE)
        b.set(px_, Y(GM_TOP), pz, S("campfire", facing="north", lit=True, signal_fire=True, waterlogged=False))   # la vapeur qui sort

    # des lanternes pendues aux plafonds des étages du haut (la lumière vient sinon des fenêtres et des foyers)
    for lo, hi, spots in ((52, 70, ((20, 12), (5, 34), (34, 34), (20, 35))),
                          (70, 90, ((15, 18), (25, 18), (15, 33), (5, 5), (34, 5), (5, 25))),
                          (90, 106, ((6, 6), (33, 6), (6, 33), (33, 26), (26, 33)))):
        for x, z in spots:
            b.fill(x, Y(hi - 4), z, x, Y(hi - 1), z, CHAIN)
            b.set(x, Y(hi - 5), z, S("lantern", hanging=True, waterlogged=False))

    # les escaliers, d'un étage à l'autre : nord, est, sud scellés ; le dernier, à l'ouest, monte au toit
    for k, (lo, hi) in enumerate(zip(GM_FLOORS[:4], GM_FLOORS[1:])):
        _flight(b, rnd, k, lo, hi, sealed=k < 3)

    # l'air des étages (là où rien n'est posé)
    for lo, hi in zip(GM_FLOORS[:4], GM_FLOORS[1:]):
        b.carve(1, Y(lo + 1), 1, N - 2, Y(hi - 1), N - 2)

    # dehors, contre la coque : les grandes conduites montent des foyers jusqu'au-dessus du toit, cerclées de laiton
    # tous les 12 blocs, avec leurs soupapes à mi-hauteur (côté ouest, la porte reste libre)
    for along in (7, 31):
        for ox, oz in ((along, -1), (along, N), (-1, along), (N, along)):
            for y in range(37, 111):
                b.set(ox, Y(y), oz, BRASS if y % 12 == 0 else PIPE)
            b.set(ox, Y(111), oz, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
            b.set(ox, Y(36), oz, BRASS)
            for y in (58, 82):
                b.set(ox, Y(y), oz, S("waxed_exposed_copper_bulb", lit=False, powered=False))
    return raw


def grand_machine_pier():
    """Le quai qui mène à la porte de la machine, depuis la paroi de la caverne : 28 de long, 6 de large, posé à l'ouest
    (x = 27 touche la machine). Ses poteaux plongent dans la lave ; une travée s'est effondrée."""
    rnd = random.Random(72)
    L, W, BASE = 28, 6, 25
    b = Build(L, 16, W)
    Y = lambda y: y - BASE
    for x in range(L):
        gap = 9 <= x <= 11
        for z in range(W):
            edge = z in (0, W - 1)
            if gap:
                continue
            b.set(x, Y(36), z, BRASS if edge else brick(rnd, 0.25))
        if x % 6 == 1:
            for z in (0, W - 1):
                b.fill(x, Y(25), z, x, Y(35), z, PIPE)
                if not gap:
                    b.fill(x, Y(37), z, x, Y(38), z, S("polished_blackstone_brick_wall", east="none", north="none", south="none",
                                                        west="none", up=True, waterlogged=False))
                    b.set(x, Y(39), z, S("lantern", hanging=False, waterlogged=False))
    # la travée effondrée : ce qu'il en reste, à moitié dans la lave
    for x, z in ((9, 1), (10, 3), (11, 2), (10, 4)):
        b.set(x, Y(31 + rnd.choice((0, 1))), z, S("cracked_polished_blackstone_bricks"))
    b.set(8, Y(36), 2, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
    b.set(12, Y(36), 3, S("polished_blackstone_brick_slab", type="bottom", waterlogged=False))
    # à la paroi : une arche, qui continue dans la roche (le tunnel est creusé à la génération)
    b.fill(0, Y(37), 1, 0, Y(40), 4, AIR)
    b.fill(0, Y(37), 0, 0, Y(41), 0, PIPE)
    b.fill(0, Y(37), W - 1, 0, Y(41), W - 1, PIPE)
    b.fill(0, Y(41), 0, 0, Y(41), W - 1, BRASS)
    return b


forge_ruin("base").save("forge_ruin")
forge_ruin("crimson").save("forge_ruin_crimson")
forge_ruin("soul").save("forge_ruin_soul")
fortress_annex().save("fortress_annex")
boiler_hall().save("boiler_hall")
boiler_hall_gate().save("boiler_hall_gate")
last_hearth().save("last_hearth")
ceiling_chimneys().save("ceiling_chimneys")
forge_arena().save("forge_arena")
grand_machine().save("grand_machine")
grand_machine_pier().save("grand_machine_pier")
