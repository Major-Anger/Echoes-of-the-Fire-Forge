package fr.anger.echoes.core.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.anger.echoes.core.CoreConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * La jauge d'âmes d'un joueur.
 * Les bonus de taille sont rangés par source (un identifiant par épisode / mécanique, par exemple
 * echoes_silt:ancient_shards), pour que deux épisodes ne s'écrasent jamais l'un l'autre.
 * Passe par {@link Souls} plutôt que de modifier cet objet directement : Souls renvoie la jauge au client.
 */
public final class SoulData {

    public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("souls", 0).forGetter(SoulData::getSouls),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("bonuses", Map.of()).forGetter(data -> data.bonuses)
    ).apply(instance, SoulData::new));

    private int souls;
    private final Map<ResourceLocation, Integer> bonuses;

    public SoulData() {
        this(0, Map.of());
    }

    private SoulData(int souls, Map<ResourceLocation, Integer> bonuses) {
        this.souls = Math.max(0, souls);
        this.bonuses = new HashMap<>(bonuses);
    }

    public int getSouls() {
        return souls;
    }

    /** Taille de la jauge : base + bonus (plafonnés, voir config/echoes_core-common.toml). */
    public int getMax() {
        return CoreConfig.baseSouls() + Math.min(getTotalBonus(), CoreConfig.maxSoulBonus());
    }

    public int getTotalBonus() {
        int total = 0;
        for (int bonus : bonuses.values()) total += bonus;
        return total;
    }

    public int getBonus(ResourceLocation source) {
        return bonuses.getOrDefault(source, 0);
    }

    void setSouls(int amount) {
        this.souls = Math.max(0, Math.min(amount, getMax()));
    }

    void setBonus(ResourceLocation source, int bonus) {
        if (bonus <= 0) bonuses.remove(source);
        else bonuses.put(source, bonus);
        setSouls(souls);
    }

    /** À la mort : la nouvelle jauge garde les bonus de l'ancienne, pas les âmes. */
    void copyBonusesFrom(SoulData other) {
        bonuses.clear();
        bonuses.putAll(other.bonuses);
    }
}
