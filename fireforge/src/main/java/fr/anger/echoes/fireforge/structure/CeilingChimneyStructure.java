package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Les cheminées du plafond : de grandes conduites qui pendent du plafond du Nether et s'y enfoncent (la vapeur est
 * passée par là). Il faut un vrai plafond au-dessus et un grand vide en dessous.
 */
public class CeilingChimneyStructure extends Structure {

    public static final MapCodec<CeilingChimneyStructure> CODEC = simpleCodec(CeilingChimneyStructure::new);

    public static final ResourceLocation CHIMNEYS = EchoesFireForge.id("ceiling_chimneys");
    /** Dans le gabarit : le milieu de la grande conduite (x, z), son haut (y), ce qui doit entrer dans la roche. */
    private static final int MIDDLE = 4, TOP = 19, EMBED = 4;
    /** Le plafond cherché (de haut en bas), le vide demandé dessous. */
    private static final int HIGHEST = 120, LOWEST = 80, EMPTY_BELOW = 22, ATTEMPTS = 4;

    public CeilingChimneyStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        Columns columns = new Columns(context);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
            int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);
            int under = columns.ceiling(x, z, HIGHEST, LOWEST);
            if (under == Columns.NONE || !columns.airBetween(x, z, under - EMPTY_BELOW, under)) continue;
            // le vide continue aux coins du gabarit (rien ne bute contre les conduites qui pendent)
            boolean clear = true;
            for (int dx = -MIDDLE; dx <= MIDDLE && clear; dx += MIDDLE) {
                for (int dz = -MIDDLE; dz <= MIDDLE && clear; dz += MIDDLE) {
                    clear = columns.airBetween(x + dx, z + dz, under - 14, under - 3);
                }
            }
            if (!clear) continue;
            Rotation rotation = Rotation.getRandom(random);
            BlockPos origin = new BlockPos(x - MIDDLE, under + EMBED - TOP, z - MIDDLE);
            BlockPos pivot = new BlockPos(MIDDLE, 0, MIDDLE);
            return Optional.of(new GenerationStub(new BlockPos(x, under - 8, z), builder -> builder.addPiece(new ForgeTemplatePiece(
                    context.structureTemplateManager(), CHIMNEYS, ForgeTemplatePiece.Kind.CHIMNEY, origin, rotation, pivot, null))));
        }
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return FFStructures.CEILING_CHIMNEY.get();
    }
}
