package fr.anger.echoes.fireforge.witherset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Où en est le porteur du set du Wither, gardé sur le joueur (jamais affiché) : de 0 à 100 %. Arrivé à 100 %
 * (le palier 4), plus de retour : {@code locked}.
 */
public record WitherProgress(double percent, boolean locked) {

    public static final WitherProgress NONE = new WitherProgress(0, false);

    public static final Codec<WitherProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("percent").forGetter(WitherProgress::percent),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(WitherProgress::locked)
    ).apply(i, WitherProgress::new));

    public int stage() {
        return WitherSet.stage(percent);
    }
}
