package fr.anger.echoes.fireforge.forging;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Les effets Spéciaux (liste validée, docs/plan-fire-forge.md). Chacun a sa contrepartie.
 * Leurs effets en jeu sont dans {@link SpecialEffects} (étape 6). Les Spéciaux « âme » ne sortent que du feu
 * des âmes (module des âmes, avec le Silt) ; la liste âme est provisoire, Anger la complétera.
 */
public enum Special implements StringRepresentable {
    SOUPAPE("soupape", EquipmentSlot.MAINHAND),
    MARTEAU_PILON("marteau_pilon", EquipmentSlot.MAINHAND),
    MONTEE_EN_PRESSION("montee_en_pression", EquipmentSlot.MAINHAND),
    CHAUDIERE("chaudiere", EquipmentSlot.CHEST),
    PAS_DE_FONDEUR("pas_de_fondeur", EquipmentSlot.LEGS),
    SOUFFLET("soufflet", EquipmentSlot.FEET),
    LUNETTES("lunettes", EquipmentSlot.HEAD),
    LINCEUL_DE_CENDRE("linceul_de_cendre", null), // n'importe quelle pièce d'armure
    // liste âme (feu des âmes) : un seul pour valider la mécanique
    LAME_DES_VEILLEURS("lame_des_veilleurs", EquipmentSlot.MAINHAND, true);

    public static final Codec<Special> CODEC = StringRepresentable.fromEnum(Special::values);

    private final String key;
    private final EquipmentSlot slot;
    private final boolean soul;

    Special(String key, EquipmentSlot slot) {
        this(key, slot, false);
    }

    Special(String key, EquipmentSlot slot, boolean soul) {
        this.key = key;
        this.slot = slot;
        this.soul = soul;
    }

    /** Un Spécial de la liste âme (il ne sort que du feu des âmes). */
    public boolean soul() {
        return soul;
    }

    /** Ce Spécial peut-il tomber sur un objet porté à cet emplacement ? */
    public boolean fits(EquipmentSlot itemSlot) {
        if (slot == null) return itemSlot.isArmor();
        return slot == itemSlot;
    }

    public Component displayName() {
        return Component.translatable("special.echoes_fireforge." + key).withStyle(soul ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE);
    }

    public Component effect() {
        return Component.translatable("special.echoes_fireforge." + key + ".effect");
    }

    public Component drawback() {
        return Component.translatable("special.echoes_fireforge." + key + ".drawback");
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
