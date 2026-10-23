package fr.anger.echoes.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/echoes_core-client.toml : les réglages d'affichage, propres à chaque joueur. */
public final class CoreClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue SOUL_BAR_SCALE = BUILDER
            .comment("Taille de la barre d'âmes à l'écran (1.0 = taille normale).")
            .defineInRange("soulBar.scale", 1.0, 0.75, 2.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    private CoreClientConfig() {}

    public static float soulBarScale() {
        double scale = SPEC.isLoaded() ? SOUL_BAR_SCALE.get() : SOUL_BAR_SCALE.getDefault();
        return (float) scale;
    }
}
