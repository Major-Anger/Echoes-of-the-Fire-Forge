package fr.anger.echoes.fireforge.mechanism;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Côté client : les minerais repérés par la Sonde, et jusqu'à quand ils brillent (aucune classe client ici). */
public final class ProbeMarks {

    private ProbeMarks() {}

    /** Combien de temps ils brillent : 5 s. */
    public static final long DURATION_MS = 5000;
    private static volatile List<BlockPos> ores = List.of();
    private static volatile long until;

    public static void show(List<BlockPos> found) {
        ores = List.copyOf(found);
        until = System.currentTimeMillis() + DURATION_MS;
    }

    public static List<BlockPos> ores() {
        return System.currentTimeMillis() < until ? ores : List.of();
    }

    /** De 1 à 0 au fil des 5 s (pour faire pâlir la lueur). */
    public static float fade() {
        long left = until - System.currentTimeMillis();
        return left <= 0 ? 0 : Math.min(1F, left / (float) DURATION_MS);
    }
}
