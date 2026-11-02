package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * La chaufferie, creusée dans la roche qui est déjà là. On part d'un sol de grotte (le dehors, hors des forêts), on va
 * vers une paroi toute proche : l'entrée se pose sur la paroi, un couloir s'enfonce tout droit, et la salle (avec sa
 * salle scellée) doit tenir dans la roche derrière. Deux puits montent de son plafond jusqu'au premier vide.
 */
public class BoilerHallStructure extends Structure {

    public static final MapCodec<BoilerHallStructure> CODEC = simpleCodec(BoilerHallStructure::new);

    public static final ResourceLocation HALL = EchoesFireForge.id("boiler_hall");
    public static final ResourceLocation GATE = EchoesFireForge.id("boiler_hall_gate");

    /** Dans le gabarit de la salle : le milieu de sa porte (au sud), les puits (x, z), la hauteur des puits. */
    static final int DOOR_X = 9, DOOR_Z = 18, VENT_Y = 14;
    static final int[][] VENTS = {{4, 12}, {14, 12}};
    /** La paroi cherchée à 8 blocs au plus du sol de départ ; les longueurs de couloir essayées ; la roche demandée. */
    private static final int MAX_FACE = 8, ATTEMPTS = 8;
    private static final int[] CORRIDORS = {0, 2, 4, 6};
    private static final float BURIED = 0.85F;
    /** Les biomes où l'entrée ne se pose pas (les forêts du Nether). */
    public static final TagKey<Biome> OVERGROWN = TagKey.create(Registries.BIOME, EchoesFireForge.id("overgrown"));

    public BoilerHallStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        StructureTemplateManager manager = context.structureTemplateManager();
        int sea = context.chunkGenerator().getSeaLevel();
        Columns columns = new Columns(context);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
            int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);
            int floor = columns.floor(x, z, sea + 6 + random.nextInt(60), sea + 2);
            if (floor == Columns.NONE || !columns.airBetween(x, z, floor + 1, floor + 3)) continue;
            // l'entrée donne sur un sol nu : dans les forêts, les champignons géants pousseraient devant
            if (context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(floor + 1), QuartPos.fromBlock(z),
                    context.randomState().sampler()).is(OVERGROWN)) continue;
            int first = random.nextInt(4);
            for (int i = 0; i < 4; i++) {
                Direction in = Direction.from2DDataValue((first + i) % 4);      // du dehors vers la roche
                int face = face(columns, x, floor, z, in);
                if (face < 0) continue;
                Direction out = in.getOpposite();                             // de la salle vers le dehors
                Rotation rotation = out == Direction.SOUTH ? Rotation.NONE : out == Direction.WEST ? Rotation.CLOCKWISE_90
                        : out == Direction.NORTH ? Rotation.CLOCKWISE_180 : Rotation.COUNTERCLOCKWISE_90;
                for (int corridor : CORRIDORS) {
                    if (!tunnel(columns, x, floor, z, in, face, corridor)) break;
                    // la porte de la salle, au bout du couloir ; d'elle, le coin du gabarit
                    BlockPos door = new BlockPos(x, floor, z).relative(in, face + 2 + corridor);
                    BlockPos origin = door.subtract(rotate(DOOR_X, 0, DOOR_Z, rotation));
                    if (!buried(columns, origin, rotation)) continue;
                    BlockPos center = origin.offset(rotate(9, 1, 9, rotation));
                    return Optional.of(new GenerationStub(center, builder -> {
                        builder.addPiece(new ForgeTemplatePiece(manager, HALL, ForgeTemplatePiece.Kind.HALL, origin, rotation, BlockPos.ZERO, null));
                        if (corridor > 0) builder.addPiece(new HallCorridorPiece(door.relative(out), out, corridor));
                        builder.addPiece(new ForgeTemplatePiece(manager, GATE, ForgeTemplatePiece.Kind.GATE,
                                origin.offset(rotate(DOOR_X - 3, 0, DOOR_Z + 1 + corridor, rotation)), rotation, BlockPos.ZERO, null));
                        for (int[] vent : VENTS) builder.addPiece(new HallVentPiece(origin.offset(rotate(vent[0], VENT_Y, vent[1], rotation))));
                    }));
                }
            }
        }
        return Optional.empty();
    }

    static BlockPos rotate(int x, int y, int z, Rotation rotation) {
        return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, rotation, BlockPos.ZERO);
    }

    /**
     * En allant vers {@code in} sur un sol de plain-pied (de l'air sur 3 de haut, du sol dessous), la distance de la
     * première case de roche (la paroi) ; -1 s'il y a un trou, une marche ou rien à 8 blocs.
     */
    private static int face(Columns columns, int x, int floor, int z, Direction in) {
        for (int d = 1; d <= MAX_FACE; d++) {
            int cx = x + in.getStepX() * d, cz = z + in.getStepZ() * d;
            if (columns.solidBetween(cx, cz, floor + 1, floor + 3)) return d;
            if (!columns.solid(cx, floor, cz) || !columns.airBetween(cx, cz, floor + 1, floor + 3)) return -1;
        }
        return -1;
    }

    /** L'entrée (2 cases) et le couloir sont pris dans la roche, à partir de la paroi. */
    private static boolean tunnel(Columns columns, int x, int floor, int z, Direction in, int face, int corridor) {
        for (int d = face; d <= face + 1 + corridor; d++) {
            if (!columns.solidBetween(x + in.getStepX() * d, z + in.getStepZ() * d, floor + 1, floor + 4)) return false;
        }
        return true;
    }

    /** La salle et sa salle scellée tiennent dans la roche (à quelques poches près), sans lave. */
    private static boolean buried(Columns columns, BlockPos origin, Rotation rotation) {
        int solid = 0, total = 0;
        for (int x : new int[]{0, 9, 18, 24}) {
            for (int z : new int[]{0, 9, 17}) {
                BlockPos column = origin.offset(rotate(x, 0, z, rotation));
                for (int y : new int[]{0, 3, 6, 9, 12, 13}) {
                    total++;
                    if (columns.solid(column.getX(), origin.getY() + y, column.getZ())) solid++;
                    else if (columns.lava(column.getX(), origin.getY() + y, column.getZ())) return false;
                }
            }
        }
        return solid >= total * BURIED;
    }

    @Override
    public StructureType<?> type() {
        return FFStructures.BOILER_HALL.get();
    }
}
