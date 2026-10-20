package fr.anger.echoes.fireforge.forging;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Les effets Spéciaux (liste validée, docs/plan-fire-forge.md). Chacun a sa contrepartie.
 * Leurs effets en jeu sont dans {@link SpecialEffects} (étape 6). Les Spéciaux d'arme sont liés à des types d'arme
 * (étape 6c) : arc, arbalète, trident et masse n'ont pas de Spécial de base. Les Spéciaux « âme » ne sortent que du feu
 * des âmes (module des âmes, avec le Silt) ; la liste âme est celle d'Anger (étapes 6b, 6d, 6e).
 */
public enum Special implements StringRepresentable {
    SOUPAPE("soupape", false, WeaponType.SWORD, WeaponType.AXE),
    MARTEAU_PILON("marteau_pilon", false, WeaponType.SWORD, WeaponType.AXE),
    MONTEE_EN_PRESSION("montee_en_pression", false, WeaponType.SWORD, WeaponType.AXE),
    CHAUDIERE("chaudiere", EquipmentSlot.CHEST),
    PAS_DE_FONDEUR("pas_de_fondeur", EquipmentSlot.LEGS),
    SOUFFLET("soufflet", EquipmentSlot.FEET),
    LUNETTES("lunettes", EquipmentSlot.HEAD),
    LINCEUL_DE_CENDRE("linceul_de_cendre", null), // n'importe quelle pièce d'armure
    // liste âme (feu des âmes), d'Anger : effets dans SoulSpecialEffects (étape 6d)
    LAME_DES_VEILLEURS("lame_des_veilleurs", true, WeaponType.SWORD, WeaponType.AXE),
    ECHO_DE_LAME("echo_de_lame", true, WeaponType.SWORD, WeaponType.AXE),
    CARREAU_MUET("carreau_muet", true, WeaponType.CROSSBOW),
    HARPON_DES_RIVIERES("harpon_des_rivieres", true, WeaponType.TRIDENT),
    GLAS("glas", true, WeaponType.MACE),
    PAS_FEUTRE("pas_feutre", EquipmentSlot.FEET, true),
    REGARD_DU_VEILLEUR("regard_du_veilleur", EquipmentSlot.HEAD, true),
    URNE_CLOSE("urne_close", EquipmentSlot.CHEST, true),
    FENDEUSE_DE_RACINES("fendeuse_de_racines", true, WeaponType.AXE),
    CORDE_CHANTANTE("corde_chantante", true, WeaponType.BOW);

    public static final Codec<Special> CODEC = StringRepresentable.fromEnum(Special::values);

    private final String key;
    @Nullable
    private final EquipmentSlot slot;
    private final boolean soul;
    /** Pour un Spécial d'arme : les types d'arme qui peuvent le porter (étape 6c). */
    private final Set<WeaponType> weapons;

    /** Un Spécial d'armure (slot null : n'importe quelle pièce). */
    Special(String key, @Nullable EquipmentSlot slot) {
        this(key, slot, false);
    }

    /** Un Spécial d'armure, de la liste de base ou de la liste âme. */
    Special(String key, @Nullable EquipmentSlot slot, boolean soul) {
        this.key = key;
        this.slot = slot;
        this.soul = soul;
        this.weapons = Set.of();
    }

    /** Un Spécial d'arme, lié à ces types d'arme. */
    Special(String key, boolean soul, WeaponType... weapons) {
        this.key = key;
        this.slot = EquipmentSlot.MAINHAND;
        this.soul = soul;
        this.weapons = Set.of(weapons);
    }

    /** Un Spécial de la liste âme (il ne sort que du feu des âmes). */
    public boolean soul() {
        return soul;
    }

    /** Ce Spécial peut-il tomber sur cet objet ? Une armure à sa place, une arme selon son type. */
    public boolean fits(ItemStack stack) {
        EquipmentSlot itemSlot = Forging.slotOf(stack);
        if (slot == null) return itemSlot.isArmor();
        if (slot != itemSlot) return false;
        return slot != EquipmentSlot.MAINHAND || weapons.contains(WeaponType.of(stack));
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
