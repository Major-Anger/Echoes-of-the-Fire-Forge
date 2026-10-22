package fr.anger.echoes.fireforge.mechanism;

import com.mojang.serialization.Codec;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import fr.anger.echoes.fireforge.forging.Forging;
import fr.anger.echoes.fireforge.forging.WeaponType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Les mécanismes des Attiseurs (plan, section 11) : greffés à l'Établi d'Attiseur sur une arme ou une armure,
 * un seul par pièce, en plus de la rareté et du Spécial. Les Attiseurs équipaient leurs outils de rouages et de
 * vapeur pour vivre et travailler dans le Nether. Effets : étapes 8c (armures) et 8d (armes).
 */
public enum Mechanism implements StringRepresentable {
    // casque
    LONGUE_VUE("longue_vue", EquipmentSlot.HEAD, false, false),
    SONDE("sonde", EquipmentSlot.HEAD, true, false),
    LAMPE("lampe", EquipmentSlot.HEAD, false, false),
    // plastron
    SAC("sac", EquipmentSlot.CHEST, false, false),
    PLANEUR("planeur", EquipmentSlot.CHEST, true, true),
    JET("jet", EquipmentSlot.CHEST, false, true),
    // jambières
    POCHES("poches", EquipmentSlot.LEGS, false, false),
    PISTONS("pistons", EquipmentSlot.LEGS, true, true),
    ANCRE("ancre", EquipmentSlot.LEGS, false, false),
    // bottes
    PATINS("patins", EquipmentSlot.FEET, false, false),
    RESSORTS("ressorts", EquipmentSlot.FEET, true, false),
    CRAMPONS("crampons", EquipmentSlot.FEET, false, false),
    // armes
    PISTON("piston", WeaponType.SWORD),
    SILLON("sillon", WeaponType.AXE),
    FRAPPE_SOL("frappe_sol", WeaponType.SWORD, WeaponType.AXE),
    TREUIL("treuil", WeaponType.SWORD, WeaponType.AXE),
    TRIDENT_DE_TREMPE("trident_de_trempe", WeaponType.TRIDENT);

    public static final Codec<Mechanism> CODEC = StringRepresentable.fromEnum(Mechanism::values);
    public static final StreamCodec<ByteBuf, Mechanism> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Mechanism::ordinal);

    private final String key;
    private final EquipmentSlot slot;
    private final boolean rare;
    /** Il se paie en charges de vapeur (étape 8b) ; les armes n'en utilisent jamais. */
    private final boolean steam;
    private final Set<WeaponType> weapons;

    Mechanism(String key, EquipmentSlot slot, boolean rare, boolean steam) {
        this.key = key;
        this.slot = slot;
        this.rare = rare;
        this.steam = steam;
        this.weapons = Set.of();
    }

    Mechanism(String key, WeaponType... weapons) {
        this.key = key;
        this.slot = EquipmentSlot.MAINHAND;
        this.rare = false;
        this.steam = false;
        this.weapons = Set.of(weapons);
    }

    public String key() {
        return key;
    }

    /** Son gabarit est rare (acier bleui) : Sonde, Planeur, Pistons de genoux, Ressorts (validé). */
    public boolean rare() {
        return rare;
    }

    public boolean steam() {
        return steam;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    /** Ce mécanisme se greffe-t-il sur cet objet ? Une pièce reforgeable : une armure à sa place, une arme selon son type. */
    public boolean fits(ItemStack stack) {
        if (!ForgeRules.isReforgeable(stack)) return false;
        EquipmentSlot itemSlot = Forging.slotOf(stack);
        if (itemSlot != slot) return false;
        return slot != EquipmentSlot.MAINHAND || weapons.contains(WeaponType.of(stack));
    }

    public Component displayName() {
        return Component.translatable("mechanism.echoes_fireforge." + key);
    }

    public Component description() {
        return Component.translatable("mechanism.echoes_fireforge." + key + ".desc");
    }

    /** « casque », « épée ou hache »… : où il se greffe. */
    public Component target() {
        return Component.translatable("mechanism.echoes_fireforge.target." + (slot == EquipmentSlot.MAINHAND
                ? String.join("_", weapons.stream().map(w -> w.name().toLowerCase(java.util.Locale.ROOT)).sorted().toList())
                : slot.getName()));
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
