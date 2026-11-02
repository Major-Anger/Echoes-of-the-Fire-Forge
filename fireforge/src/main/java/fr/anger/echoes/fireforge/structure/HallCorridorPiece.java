package fr.anger.echoes.fireforge.structure;

import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Le couloir de la chaufferie, de sa porte jusqu'à son entrée : 3 de large, 3 de haut, creusé dans la roche, un sol de
 * pierre noire et, tous les 4 blocs, des montants de basalte sous un linteau de laiton. Là où la roche manque (une
 * poche d'air, de la lave), on ferme avec de la pierre noire.
 */
public class HallCorridorPiece extends StructurePiece {

    private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState POST = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    private static final BlockState BRASS = Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState();

    private final BlockPos start;
    private final Direction direction;
    private final int length;

    /** {@code start} : le sol de la première case du couloir, au milieu ; le couloir part dans {@code direction}. */
    public HallCorridorPiece(BlockPos start, Direction direction, int length) {
        super(FFStructures.CORRIDOR_PIECE.get(), 0, box(start, direction, length));
        this.start = start;
        this.direction = direction;
        this.length = length;
    }

    public HallCorridorPiece(CompoundTag tag) {
        super(FFStructures.CORRIDOR_PIECE.get(), tag);
        this.start = new BlockPos(tag.getInt("SX"), tag.getInt("SY"), tag.getInt("SZ"));
        this.direction = Direction.from2DDataValue(tag.getInt("Dir"));
        this.length = tag.getInt("Len");
    }

    private static BoundingBox box(BlockPos start, Direction direction, int length) {
        Direction side = direction.getClockWise();
        BlockPos a = start.relative(side, -2);
        BlockPos b = start.relative(direction, Math.max(0, length - 1)).relative(side, 2).above(4);
        return BoundingBox.fromCorners(a, b);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("SX", start.getX());
        tag.putInt("SY", start.getY());
        tag.putInt("SZ", start.getZ());
        tag.putInt("Dir", direction.get2DDataValue());
        tag.putInt("Len", length);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        Direction side = direction.getClockWise();
        for (int i = 0; i < length; i++) {
            boolean rib = i % 4 == 0;
            for (int w = -2; w <= 2; w++) {
                for (int h = 0; h <= 4; h++) {
                    BlockPos p = start.relative(direction, i).relative(side, w).above(h);
                    if (!box.isInside(p)) continue;
                    boolean inside = Math.abs(w) <= 1;
                    BlockState state = level.getBlockState(p);
                    if (h == 0) {
                        if (inside || ForgeTemplatePiece.fillable(state)) level.setBlock(p, brick(p), 2);
                    } else if (h <= 3) {
                        if (inside) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        else if (rib) level.setBlock(p, POST, 2);
                        else if (ForgeTemplatePiece.fillable(state)) level.setBlock(p, brick(p), 2);
                    } else if (rib) {
                        level.setBlock(p, BRASS, 2);
                    } else if (ForgeTemplatePiece.fillable(state)) {
                        level.setBlock(p, brick(p), 2);
                    }
                }
            }
        }
    }

    /** Une brique sur sept est fendue (le hasard vient de la position : le même d'un tronçon à l'autre). */
    private static BlockState brick(BlockPos p) {
        return RandomSource.create(p.asLong()).nextInt(7) == 0 ? CRACKED : BRICKS;
    }
}
