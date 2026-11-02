package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Un gabarit posé sur un sol plat du Nether (la forge en ruine et ses variantes, le dernier foyer) : le sol du gabarit
 * remplace le dessus du terrain, les petits creux sont comblés. Au bord de la mer de lave ({@code shore}), la ruine
 * se pose sur la rive, l'âtre tourné vers la lave.
 */
public class FloorRuinStructure extends Structure {

    public static final MapCodec<FloorRuinStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            ForgeTemplatePiece.Kind.CODEC.optionalFieldOf("kind", ForgeTemplatePiece.Kind.RUIN).forGetter(s -> s.kind),
            Codec.BOOL.optionalFieldOf("shore", false).forGetter(s -> s.shore),
            BlockState.CODEC.optionalFieldOf("foundation", Blocks.NETHERRACK.defaultBlockState()).forGetter(s -> s.foundation),
            Codec.intRange(0, 256).optionalFieldOf("max_y", 100).forGetter(s -> s.maxY)
    ).apply(instance, FloorRuinStructure::new));

    private static final int ATTEMPTS = 4, SLOPE = 2;

    private final ResourceLocation template;
    private final ForgeTemplatePiece.Kind kind;
    private final boolean shore;
    private final BlockState foundation;
    private final int maxY;

    public FloorRuinStructure(StructureSettings settings, ResourceLocation template, ForgeTemplatePiece.Kind kind, boolean shore,
                              BlockState foundation, int maxY) {
        super(settings);
        this.template = template;
        this.kind = kind;
        this.shore = shore;
        this.foundation = foundation;
        this.maxY = maxY;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        Vec3i size = context.structureTemplateManager().getOrCreate(template).getSize();
        int sea = context.chunkGenerator().getSeaLevel();
        Columns columns = new Columns(context);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
            int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);
            int from = shore ? sea + 4 : sea + 4 + random.nextInt(Math.max(1, maxY - sea - 4));
            int floor = columns.floor(x, z, from, shore ? sea : sea + 1);
            if (floor == Columns.NONE || !columns.airBetween(x, z, floor + 1, floor + size.getY())) continue;
            Rotation rotation = shore ? facingLava(columns, x, floor, z, size.getZ() / 2, sea, random) : Rotation.getRandom(random);
            if (rotation == null) continue;
            boolean turned = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
            int hx = (turned ? size.getZ() : size.getX()) / 2, hz = (turned ? size.getX() : size.getZ()) / 2;
            if (!flat(columns, x, z, floor, hx, hz, shore ? front(rotation) : null, sea)) continue;
            BlockPos center = new BlockPos(x, floor, z);
            return Optional.of(new GenerationStub(center, builder -> builder.addPiece(ForgeTemplatePiece.centered(
                    context.structureTemplateManager(), template, kind, center, rotation, foundation))));
        }
        return Optional.empty();
    }

    /**
     * Le sol aux coins et au milieu des bords reste à 2 blocs près de celui du centre. Sur la rive, le bord tourné vers
     * la lave ({@code front}) peut déjà donner sur la mer de lave : les fondations y font un petit quai.
     */
    private static boolean flat(Columns columns, int x, int z, int floor, int hx, int hz, @Nullable Direction front, int sea) {
        for (int dx = -hx; dx <= hx; dx += hx) {
            for (int dz = -hz; dz <= hz; dz += hz) {
                if (dx == 0 && dz == 0) continue;
                if (columns.floor(x + dx, z + dz, floor + SLOPE, floor - SLOPE) != Columns.NONE) continue;
                boolean onFront = front != null && (front.getStepX() != 0 ? Integer.signum(dx) == front.getStepX() : Integer.signum(dz) == front.getStepZ());
                if (!onFront || !columns.lava(x + dx, sea - 1, z + dz)) return false;
            }
        }
        return true;
    }

    /** L'avant du gabarit (son sud), une fois tourné. */
    private static Direction front(Rotation rotation) {
        return rotation.rotate(Direction.SOUTH);
    }

    /** La rotation qui tourne l'avant du gabarit (son sud) vers la mer de lave toute proche ; null s'il n'y en a pas. */
    @Nullable
    private static Rotation facingLava(Columns columns, int x, int floor, int z, int halfDepth, int sea, WorldgenRandom random) {
        if (floor > sea + 3) return null;
        int first = random.nextInt(4);
        for (int i = 0; i < 4; i++) {
            Direction direction = Direction.from2DDataValue((first + i) % 4);
            int lava = 0;
            for (int d = halfDepth + 2; d <= halfDepth + 8; d += 2) {
                int cx = x + direction.getStepX() * d, cz = z + direction.getStepZ() * d;
                if (columns.lava(cx, sea - 1, cz) && columns.air(cx, sea, cz)) lava++;
            }
            if (lava >= 3) return switch (direction) {
                case SOUTH -> Rotation.NONE;
                case WEST -> Rotation.CLOCKWISE_90;
                case NORTH -> Rotation.CLOCKWISE_180;
                default -> Rotation.COUNTERCLOCKWISE_90;
            };
        }
        return null;
    }

    @Override
    public StructureType<?> type() {
        return FFStructures.FLOOR_RUIN.get();
    }
}
