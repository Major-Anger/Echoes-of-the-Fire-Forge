package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * La Grande Machine (plan, section 15) : une seule par monde (anneaux concentriques, un seul point). Elle se pose
 * toujours : elle creuse sa caverne de la mer de lave jusqu'au toit ({@link MachineCavernPiece}), la machine s'y dresse,
 * son quai part de la paroi ouest, et un tunnel continue derrière l'arche du quai jusqu'au premier vide, s'il y en a un.
 */
public class GrandMachineStructure extends Structure {

    public static final MapCodec<GrandMachineStructure> CODEC = simpleCodec(GrandMachineStructure::new);

    public static final ResourceLocation MACHINE = EchoesFireForge.id("grand_machine");
    public static final ResourceLocation PIER = EchoesFireForge.id("grand_machine_pier");
    /** La coque fait 40 de large ; le gabarit, 42 (une marge d'un bloc). Il commence à y = 18, les pieds dans la lave. */
    public static final int HULL = 40, BASE_Y = 18;
    /** Le premier plancher (on marche à y = 37), la porte à l'ouest entre z = 18 et 21 de la coque. */
    public static final int FLOOR_Y = 36, DOOR_Z = 18;
    /** Le quai : 28 de long, de la paroi de la caverne à la porte ; son gabarit commence à y = 25 (ses poteaux). */
    public static final int PIER_LENGTH = 28, PIER_Y = 25, PIER_Z = 17;
    public static final int TUNNEL_MAX = 64;

    // le cœur et son régulateur (15c), par rapport au coin de la coque (y : la hauteur du monde)
    public static final BlockPos LOWER_VALVE = new BlockPos(14, 91, 29), RAISE_VALVE = new BlockPos(25, 91, 29);
    public static final BlockPos HEART_MOUTH = new BlockPos(19, 92, 23);
    public static final double HEART_X = 19.5, HEART_Y = 98.5, HEART_Z = 19.5, HEART_CORE = 3.4;
    /** Les fondations, sous le premier plancher : la salle (pleine de lave tant qu'on n'a pas baissé) et sa trappe. */
    public static final int FOUND_MIN = 9, FOUND_MAX = 30, FOUND_BOTTOM = 23, FOUND_TOP = 35;
    public static final int HATCH_X = 10, HATCH_Z = 14, HATCH_Y = 36;

    public GrandMachineStructure(StructureSettings settings) {
        super(settings);
    }

    /** Le coin nord-ouest de la coque, pour une machine centrée sur ce tronçon. */
    public static BlockPos hullCorner(ChunkPos chunk) {
        return new BlockPos(chunk.getMiddleBlockX() - HULL / 2, BASE_Y, chunk.getMiddleBlockZ() - HULL / 2);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        StructureTemplateManager manager = context.structureTemplateManager();
        ChunkPos chunk = context.chunkPos();
        BlockPos center = new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
        BlockPos hull = hullCorner(chunk);
        BlockPos template = hull.offset(-1, 0, -1);
        BlockPos pier = hull.offset(-PIER_LENGTH, PIER_Y - BASE_Y, PIER_Z);
        // derrière l'arche du quai, vers l'ouest : jusqu'au premier vide du terrain (sinon, l'arche reste une niche)
        Columns columns = new Columns(context);
        int z = hull.getZ() + DOOR_Z + 1;
        int tunnel = 0;
        for (int d = 1; d <= TUNNEL_MAX; d++) {
            if (columns.airBetween(pier.getX() - d, z, FLOOR_Y + 1, FLOOR_Y + 3)) {
                tunnel = d - 1;
                break;
            }
        }
        int length = tunnel;
        return Optional.of(new GenerationStub(center.atY(60), builder -> {
            builder.addPiece(new MachineCavernPiece(center));
            builder.addPiece(new ForgeTemplatePiece(manager, MACHINE, ForgeTemplatePiece.Kind.MACHINE, template, Rotation.NONE, BlockPos.ZERO, null));
            builder.addPiece(new ForgeTemplatePiece(manager, PIER, ForgeTemplatePiece.Kind.MACHINE, pier, Rotation.NONE, BlockPos.ZERO, null));
            if (length > 0) builder.addPiece(new HallCorridorPiece(new BlockPos(pier.getX() - 1, FLOOR_Y, z), Direction.WEST, length));
        }));
    }

    /** Le centre de la Grande Machine de ce monde (au niveau du cœur), ou rien si ce Nether n'en a pas. */
    public static java.util.Optional<BlockPos> locate(net.minecraft.server.level.ServerLevel level) {
        var sets = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        var set = sets.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE_SET,
                EchoesFireForge.id("grand_machine")));
        if (set.isEmpty() || !(set.get().value().placement() instanceof net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement rings)) {
            return java.util.Optional.empty();
        }
        java.util.List<ChunkPos> places = level.getChunkSource().getGeneratorState().getRingPositionsFor(rings);
        if (places == null || places.isEmpty()) return java.util.Optional.empty();
        ChunkPos at = places.get(0);
        return java.util.Optional.of(new BlockPos(at.getMiddleBlockX(), 98, at.getMiddleBlockZ()));
    }

    @Override
    public StructureType<?> type() {
        return FFStructures.GRAND_MACHINE.get();
    }
}
