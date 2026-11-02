package fr.anger.echoes.fireforge.structure;

import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Un puits d'aération de la chaufferie : il monte du plafond de la salle jusqu'au premier vide (24 blocs au plus). S'il
 * n'en trouve pas, ou s'il tombe d'abord sur de la lave, on ne creuse pas : il reste la bouche du gabarit, fermée par
 * le haut (la lave n'entre pas dans la salle).
 */
public class HallVentPiece extends StructurePiece {

    public static final int REACH = 24;

    private final BlockPos bottom;

    /** {@code bottom} : le premier bloc à creuser au-dessus de l'ouverture du gabarit. */
    public HallVentPiece(BlockPos bottom) {
        super(FFStructures.VENT_PIECE.get(), 0, new BoundingBox(bottom.getX(), bottom.getY(), bottom.getZ(),
                bottom.getX(), bottom.getY() + REACH - 1, bottom.getZ()));
        this.bottom = bottom;
    }

    public HallVentPiece(CompoundTag tag) {
        super(FFStructures.VENT_PIECE.get(), tag);
        this.bottom = new BlockPos(tag.getInt("BX"), tag.getInt("BY"), tag.getInt("BZ"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("BX", bottom.getX());
        tag.putInt("BY", bottom.getY());
        tag.putInt("BZ", bottom.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        if (!box.isInside(bottom.getX(), box.minY(), bottom.getZ())) return;
        int open = -1;
        for (int n = 0; n < REACH && bottom.getY() + n < level.getMaxBuildHeight(); n++) {
            BlockState state = level.getBlockState(bottom.above(n));
            if (state.isAir()) {
                open = n;                                                // le dehors (ou une grotte) : le puits respire
                break;
            }
            if (state.is(Blocks.BEDROCK)) break;                         // le toit du monde
            if (!state.getFluidState().isEmpty()) {
                if (n == 0) level.setBlock(bottom.below(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
                break;                                                   // de la lave : on ne creuse pas jusqu'à elle
            }
        }
        for (int n = 0; n < open; n++) level.setBlock(bottom.above(n), Blocks.AIR.defaultBlockState(), 2);
    }
}
