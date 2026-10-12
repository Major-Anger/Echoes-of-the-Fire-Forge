package fr.anger.echoes.fireforge.forging;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Les effets Spéciaux (liste validée, docs/plan-fire-forge.md). Chacun a sa contrepartie.
 * Étape 3 : ils sont tirés et affichés. Leurs effets en jeu arrivent à l'étape 6.
 */
public enum Special implements StringRepresentable {
    SOUPAPE("soupape", EquipmentSlot.MAINHAND),
    MARTEAU_PILON("marteau_pilon", EquipmentSlot.MAINHAND),
    MONTEE_EN_PRESSION("montee_en_pression", EquipmentSlot.MAINHAND),
    CHAUDIERE("chaudiere", EquipmentSlot.CHEST),
    PAS_DE_FONDEUR("pas_de_fondeur", EquipmentSlot.LEGS),
    SOUFFLET("soufflet", EquipmentSlot.FEET),
    LUNETTES("lunettes", EquipmentSlot.HEAD),
    LINCEUL_DE_CENDRE("linceul_de_cendre", null); // n'importe quelle pièce d'armure

    public static final Codec<Special> CODEC = StringRepresentable.fromEnum(Special::values);

    private final String key;
    private final EquipmentSlot slot;

    Special(String key, EquipmentSlot slot) {
        this.key = key;
        this.slot = slot;
    }

    /** Ce Spécial peut-il tomber sur un objet porté à cet emplacement ? */
    public boolean fits(EquipmentSlot itemSlot) {
        if (slot == null) return itemSlot.isArmor();
        return slot == itemSlot;
    }

    public Component displayName() {
        return Component.translatable("special.echoes_fireforge." + key).withStyle(ChatFormatting.LIGHT_PURPLE);
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
