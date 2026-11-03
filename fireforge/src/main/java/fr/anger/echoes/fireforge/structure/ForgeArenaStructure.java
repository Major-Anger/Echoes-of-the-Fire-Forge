package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * L'arène du maître de forge (plan, 14b), creusée dans la roche comme la chaufferie : on part d'un sol de grotte, on va
 * vers le nord jusqu'à une paroi ; l'entrée s'y pose, un couloir s'enfonce, et la salle de 60 x 60 x 30 doit tenir dans
 * la roche derrière. Elle ne tourne jamais (son entrée est au sud) : la forge du maître s'y repère toute seule.
 */
public class ForgeArenaStructure extends Structure {

    public static final MapCodec<ForgeArenaStructure> CODEC = simpleCodec(ForgeArenaStructure::new);

    public static final ResourceLocation ARENA = EchoesFireForge.id("forge_arena");
    /** Dans le gabarit : le milieu de l'entrée (le mur sud). */
    private static final int DOOR_X = 30, DOOR_Z = 59, MAX_FACE = 10, ATTEMPTS = 12;
    private static final int[] CORRIDORS = {0, 2, 4, 6, 8};
    private static final float BURIED = 0.85F;

    public ForgeArenaStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        StructureTemplateManager manager = context.structureTemplateManager();
        int sea = context.chunkGenerator().getSeaLevel();
        Columns columns = new Columns(context);
        Direction in = Direction.NORTH;                             // du dehors vers la roche : l'arène est au nord de son entrée
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
            int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);
            int floor = columns.floor(x, z, sea + 6 + random.nextInt(45), sea + 2);
            if (floor == Columns.NONE || !columns.airBetween(x, z, floor + 1, floor + 3)) continue;
            if (context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(floor + 1), QuartPos.fromBlock(z),
                    context.randomState().sampler()).is(BoilerHallStructure.OVERGROWN)) continue;
            int face = face(columns, x, floor, z, in);
            if (face < 0) continue;
            for (int corridor : CORRIDORS) {
                if (!tunnel(columns, x, floor, z, in, face, corridor)) break;
                BlockPos door = new BlockPos(x, floor, z).relative(in, face + 2 + corridor);
                BlockPos origin = door.offset(-DOOR_X, 0, -DOOR_Z);
                if (!buried(columns, origin)) continue;
                BlockPos center = origin.offset(30, 2, 30);
                Direction out = Direction.SOUTH;
                return Optional.of(new GenerationStub(center, builder -> {
                    builder.addPiece(new ForgeTemplatePiece(manager, ARENA, ForgeTemplatePiece.Kind.ARENA, origin, Rotation.NONE, BlockPos.ZERO, null));
                    if (corridor > 0) builder.addPiece(new HallCorridorPiece(door.relative(out), out, corridor));
                    builder.addPiece(new ForgeTemplatePiece(manager, BoilerHallStructure.GATE, ForgeTemplatePiece.Kind.GATE,
                            origin.offset(DOOR_X - 3, 0, DOOR_Z + 1 + corridor), Rotation.NONE, BlockPos.ZERO, null));
                }));
            }
        }
        return Optional.empty();
    }

    private static int face(Columns columns, int x, int floor, int z, Direction in) {
        for (int d = 1; d <= MAX_FACE; d++) {
            int cx = x + in.getStepX() * d, cz = z + in.getStepZ() * d;
            if (columns.solidBetween(cx, cz, floor + 1, floor + 3)) return d;
            if (!columns.solid(cx, floor, cz) || !columns.airBetween(cx, cz, floor + 1, floor + 3)) return -1;
        }
        return -1;
    }

    private static boolean tunnel(Columns columns, int x, int floor, int z, Direction in, int face, int corridor) {
        for (int d = face; d <= face + 1 + corridor; d++) {
            if (!columns.solidBetween(x + in.getStepX() * d, z + in.getStepZ() * d, floor + 1, floor + 4)) return false;
        }
        return true;
    }

    /** La salle tient dans la roche (à quelques poches près), sans lave. */
    private static boolean buried(Columns columns, BlockPos origin) {
        int solid = 0, total = 0;
        for (int x = 0; x <= 59; x += 59 / 4) {
            for (int z = 0; z <= 59; z += 59 / 4) {
                for (int y = 0; y <= 29; y += 7) {
                    total++;
                    if (columns.solid(origin.getX() + x, origin.getY() + y, origin.getZ() + z)) solid++;
                    else if (columns.lava(origin.getX() + x, origin.getY() + y, origin.getZ() + z)) return false;
                }
            }
        }
        return solid >= total * BURIED;
    }

    @Override
    public StructureType<?> type() {
        return FFStructures.FORGE_ARENA.get();
    }
}
