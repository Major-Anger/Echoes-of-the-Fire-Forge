package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.structure.BoilerHallStructure;
import fr.anger.echoes.fireforge.structure.CeilingChimneyStructure;
import fr.anger.echoes.fireforge.structure.FloorRuinStructure;
import fr.anger.echoes.fireforge.structure.ForgeTemplatePiece;
import fr.anger.echoes.fireforge.structure.FortressAnnexStructure;
import fr.anger.echoes.fireforge.structure.HallCorridorPiece;
import fr.anger.echoes.fireforge.structure.HallVentPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Les structures de la Forge (plan, 12b) : les types (le code) ; les structures elles-mêmes sont des données (worldgen/structure). */
public final class FFStructures {

    private FFStructures() {}

    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, EchoesFireForge.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, EchoesFireForge.MOD_ID);

    /** Posée sur un sol plat : la forge en ruine (et ses variantes), le dernier foyer. */
    public static final DeferredHolder<StructureType<?>, StructureType<FloorRuinStructure>> FLOOR_RUIN =
            TYPES.register("floor_ruin", () -> () -> FloorRuinStructure.CODEC);
    /** Creusée dans la roche : la chaufferie, son couloir, son entrée, ses puits. */
    public static final DeferredHolder<StructureType<?>, StructureType<BoilerHallStructure>> BOILER_HALL =
            TYPES.register("boiler_hall", () -> () -> BoilerHallStructure.CODEC);
    /** À côté d'un pont d'une vraie forteresse : sa forge. */
    public static final DeferredHolder<StructureType<?>, StructureType<FortressAnnexStructure>> FORTRESS_ANNEX =
            TYPES.register("fortress_annex", () -> () -> FortressAnnexStructure.CODEC);
    /** Accrochée au plafond : les cheminées. */
    public static final DeferredHolder<StructureType<?>, StructureType<CeilingChimneyStructure>> CEILING_CHIMNEY =
            TYPES.register("ceiling_chimney", () -> () -> CeilingChimneyStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TEMPLATE_PIECE =
            PIECES.register("template", () -> (StructurePieceType.StructureTemplateType) ForgeTemplatePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CORRIDOR_PIECE =
            PIECES.register("hall_corridor", () -> (StructurePieceType.ContextlessType) HallCorridorPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> VENT_PIECE =
            PIECES.register("hall_vent", () -> (StructurePieceType.ContextlessType) HallVentPiece::new);

    // Les structures (données), pour le code et les essais
    public static final ResourceKey<Structure> FORGE_RUIN = key("forge_ruin");
    public static final ResourceKey<Structure> FORGE_RUIN_SHORE = key("forge_ruin_shore");
    public static final ResourceKey<Structure> FORGE_RUIN_CRIMSON = key("forge_ruin_crimson");
    public static final ResourceKey<Structure> FORGE_RUIN_SOUL = key("forge_ruin_soul");
    public static final ResourceKey<Structure> FORTRESS_ANNEX_KEY = key("fortress_annex");
    public static final ResourceKey<Structure> BOILER_HALL_KEY = key("boiler_hall");
    public static final ResourceKey<Structure> LAST_HEARTH = key("last_hearth");
    public static final ResourceKey<Structure> CEILING_CHIMNEY_KEY = key("ceiling_chimney");

    /** Là où vivent les Cendreux, en plus des abords de la lave : forteresses, chaufferies, derniers foyers. */
    public static final TagKey<Structure> CENDREUX_HOMES = TagKey.create(Registries.STRUCTURE, EchoesFireForge.id("cendreux_homes"));

    private static ResourceKey<Structure> key(String name) {
        return ResourceKey.create(Registries.STRUCTURE, EchoesFireForge.id(name));
    }
}
