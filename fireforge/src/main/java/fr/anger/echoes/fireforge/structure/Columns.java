package fr.anger.echoes.fireforge.structure;

import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Le terrain de base du Nether (avant les décorations), colonne par colonne, gardé le temps d'une recherche de place.
 * Dans le Nether, il n'y a que de la roche, de l'air et la mer de lave.
 */
final class Columns {

    static final int NONE = Integer.MIN_VALUE;

    private final Structure.GenerationContext context;
    private final Map<Long, NoiseColumn> cache = new HashMap<>();

    Columns(Structure.GenerationContext context) {
        this.context = context;
    }

    BlockState get(int x, int y, int z) {
        return cache.computeIfAbsent(((long) x << 32) | (z & 0xFFFFFFFFL),
                key -> context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState())).getBlock(y);
    }

    static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    boolean solid(int x, int y, int z) {
        return solid(get(x, y, z));
    }

    boolean air(int x, int y, int z) {
        return get(x, y, z).isAir();
    }

    boolean lava(int x, int y, int z) {
        return !get(x, y, z).getFluidState().isEmpty();
    }

    /** Le premier sol (un bloc plein sous de l'air) en descendant de {@code fromY} à {@code toY} ; {@link #NONE} sinon. */
    int floor(int x, int z, int fromY, int toY) {
        for (int y = fromY; y >= toY; y--) {
            if (solid(x, y, z) && air(x, y + 1, z)) return y;
        }
        return NONE;
    }

    /** Le dessous du plafond (de l'air sous un bloc plein) en descendant de {@code fromY} à {@code toY} ; {@link #NONE} sinon. */
    int ceiling(int x, int z, int fromY, int toY) {
        for (int y = fromY; y >= toY; y--) {
            if (air(x, y, z) && solid(x, y + 1, z)) return y;
        }
        return NONE;
    }

    /** De l'air seulement (ni roche ni lave) de {@code fromY} à {@code toY}. */
    boolean airBetween(int x, int z, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            if (!air(x, y, z)) return false;
        }
        return true;
    }

    /** De la roche seulement de {@code fromY} à {@code toY}. */
    boolean solidBetween(int x, int z, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            if (!solid(x, y, z)) return false;
        }
        return true;
    }
}
