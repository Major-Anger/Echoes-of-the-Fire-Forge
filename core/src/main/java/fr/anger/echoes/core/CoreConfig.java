package fr.anger.echoes.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/echoes_core-common.toml */
public final class CoreConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue BASE_SOULS = BUILDER
            .comment("Taille de base de la jauge d'âmes.")
            .defineInRange("souls.base", 50, 1, 10_000);

    private static final ModConfigSpec.IntValue MAX_SOUL_BONUS = BUILDER
            .comment("Bonus total maximum que les épisodes peuvent ajouter à la jauge (Éclats, Échos...).")
            .defineInRange("souls.maxBonus", 50, 0, 10_000);

    static final ModConfigSpec SPEC = BUILDER.build();

    private CoreConfig() {}

    public static int baseSouls() {
        return SPEC.isLoaded() ? BASE_SOULS.get() : BASE_SOULS.getDefault();
    }

    public static int maxSoulBonus() {
        return SPEC.isLoaded() ? MAX_SOUL_BONUS.get() : MAX_SOUL_BONUS.getDefault();
    }
}
