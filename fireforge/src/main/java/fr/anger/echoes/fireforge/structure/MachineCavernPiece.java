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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * La caverne de la Grande Machine : une grande salle ronde, de la mer de lave (remplie jusqu'à y = 31) jusque sous le toit
 * de bedrock, plus large au milieu (48 blocs de rayon), aux parois inégales. La roche des parois est cuite par la chaleur
 * (basalte, pierre noire). La machine et son quai sont posés par-dessus.
 */
public class MachineCavernPiece extends StructurePiece {

    public static final int RADIUS = 48, BOTTOM = 14, LAVA_TOP = 31, TOP = 122, SCORCH = 2;
    private static final int WIGGLE = 4;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState(), LAVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState[] SCORCHED = {
            Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y),
            Blocks.BLACKSTONE.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState()};

    private final BlockPos center;

    public MachineCavernPiece(BlockPos center) {
        super(FFStructures.CAVERN_PIECE.get(), 0, new BoundingBox(center.getX() - RADIUS - WIGGLE - SCORCH, BOTTOM, center.getZ() - RADIUS - WIGGLE - SCORCH,
                center.getX() + RADIUS + WIGGLE + SCORCH, TOP, center.getZ() + RADIUS + WIGGLE + SCORCH));
        this.center = center;
    }

    public MachineCavernPiece(CompoundTag tag) {
        super(FFStructures.CAVERN_PIECE.get(), tag);
        this.center = new BlockPos(tag.getInt("CX"), 0, tag.getInt("CZ"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", center.getX());
        tag.putInt("CZ", center.getZ());
    }

    /** Le rayon de la caverne à cette hauteur (sans les inégalités) : 48 au milieu, plus étroite en haut et au fond. */
    public static double radius(int y) {
        return RADIUS - Math.max(0, y - 100) * 0.9 - Math.max(0, 40 - y) * 0.4;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // le même bruit d'un tronçon à l'autre : il vient de la position de la machine
        SimplexNoise noise = new SimplexNoise(new LegacyRandomSource(center.asLong()));
        BoundingBox area = getBoundingBox();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(area.minX(), box.minX()); x <= Math.min(area.maxX(), box.maxX()); x++) {
            for (int z = Math.max(area.minZ(), box.minZ()); z <= Math.min(area.maxZ(), box.maxZ()); z++) {
                double dx = x - center.getX(), dz = z - center.getZ();
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RADIUS + WIGGLE + SCORCH) continue;
                for (int y = BOTTOM; y <= TOP; y++) {
                    double r = radius(y) + noise.getValue(x / 14.0, y / 10.0, z / 14.0) * WIGGLE;
                    if (d > r + SCORCH) continue;
                    p.set(x, y, z);
                    BlockState here = level.getBlockState(p);
                    if (here.is(Blocks.BEDROCK)) continue;
                    if (d <= r) {
                        level.setBlock(p, y <= LAVA_TOP ? LAVA : AIR, 2);
                    } else if (Columns.solid(here) && y > LAVA_TOP) {
                        // la paroi, cuite par la chaleur de la machine
                        int pick = Math.floorMod((int) (BlockPos.asLong(x, y, z) * 0x9E3779B97F4A7C15L >>> 40), 5);
                        if (pick < SCORCHED.length) level.setBlock(p, SCORCHED[pick], 2);
                    }
                }
            }
        }
    }
}
