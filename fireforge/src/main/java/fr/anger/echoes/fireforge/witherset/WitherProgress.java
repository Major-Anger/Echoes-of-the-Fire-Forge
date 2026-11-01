package fr.anger.echoes.fireforge.witherset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Où en est le porteur du set du Wither, gardé sur le joueur (jamais affiché) : de 0 à 100 %. Arrivé à 100 %
 * (le palier 4), plus de retour : {@code locked} ; {@code doom} compte alors le temps de jeu écoulé (en ticks) avant
 * la fin (étape 13c).
 */
public record WitherProgress(double percent, boolean locked, int doom) {

    public static final WitherProgress NONE = new WitherProgress(0, false, 0);

    public static final Codec<WitherProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("percent").forGetter(WitherProgress::percent),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(WitherProgress::locked),
            Codec.INT.optionalFieldOf("doom", 0).forGetter(WitherProgress::doom)
    ).apply(i, WitherProgress::new));

    public WitherProgress(double percent, boolean locked) {
        this(percent, locked, 0);
    }

    public int stage() {
        return WitherSet.stage(percent);
    }

    /** Les minutes écoulées depuis le palier 4 (0 à 30). */
    public int doomMinutes() {
        return Math.min(WitherSet.DOOM_MINUTES, doom / 1200);
    }
}
