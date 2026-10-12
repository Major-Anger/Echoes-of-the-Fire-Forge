package fr.anger.echoes.fireforge.forging;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;

/**
 * Ce que la forge a laissé sur un objet : sa rareté, ses statistiques (en %) et, s'il est Spécial, son effet.
 * Stocké dans un composant de l'objet : il survit aux enchantements et au passage à la netherite.
 */
public record ForgingData(Rarity rarity, List<StatRoll> stats, Optional<Special> special) {

    /** Une statistique modifiée : +4,2 % de dégâts, −3 % de vitesse… */
    public record StatRoll(ForgeStat stat, float percent) {
        public static final Codec<StatRoll> CODEC = RecordCodecBuilder.create(i -> i.group(
                ForgeStat.CODEC.fieldOf("stat").forGetter(StatRoll::stat),
                Codec.FLOAT.fieldOf("percent").forGetter(StatRoll::percent)
        ).apply(i, StatRoll::new));
    }

    public static final Codec<ForgingData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Rarity.CODEC.fieldOf("rarity").forGetter(ForgingData::rarity),
            StatRoll.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(ForgingData::stats),
            Special.CODEC.optionalFieldOf("special").forGetter(ForgingData::special)
    ).apply(i, ForgingData::new));

    public static final StreamCodec<ByteBuf, ForgingData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Pourcentage total pour une statistique (0 si l'objet n'en a pas). */
    public float percent(ForgeStat stat) {
        float total = 0;
        for (StatRoll roll : stats) if (roll.stat() == stat) total += roll.percent();
        return total;
    }
}
