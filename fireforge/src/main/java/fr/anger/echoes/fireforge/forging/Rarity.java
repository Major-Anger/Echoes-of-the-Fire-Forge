package fr.anger.echoes.fireforge.forging;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Les raretés de la forge, de la pire à la meilleure (voir docs/plan-fire-forge.md, section 7). */
public enum Rarity implements StringRepresentable {
    FELE("fele", 20, ChatFormatting.GRAY),
    BRUT("brut", 35, ChatFormatting.WHITE),
    TREMPE("trempe", 25, ChatFormatting.GREEN),
    AFFINE("affine", 14, ChatFormatting.AQUA),
    ATTISE("attise", 5, ChatFormatting.GOLD),
    SPECIAL("special", 1, ChatFormatting.LIGHT_PURPLE);

    public static final Codec<Rarity> CODEC = StringRepresentable.fromEnum(Rarity::values);

    private final String key;
    /** Chance de départ, en pour cent (avant plans, offrande et chaleur). */
    private final int baseChance;
    private final ChatFormatting color;

    Rarity(String key, int baseChance, ChatFormatting color) {
        this.key = key;
        this.baseChance = baseChance;
        this.color = color;
    }

    public int baseChance() {
        return baseChance;
    }

    public ChatFormatting color() {
        return color;
    }

    public Component displayName() {
        return Component.translatable("rarity.echoes_fireforge." + key).withStyle(color);
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
