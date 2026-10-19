package fr.anger.echoes.fireforge.forging;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * Le type d'une arme tenue en main (étape 6c) : les Spéciaux d'arme sont liés à un type, et l'arc et l'arbalète
 * ont leurs propres statistiques. Les armes d'autres mods sont reconnues par les tags communs ({@code c:tools/…}).
 */
public enum WeaponType {
    SWORD, AXE, BOW, CROSSBOW, TRIDENT, MACE;

    /** Le type de cette arme, ou null si l'objet ne se tient pas en main (armure, bouclier). */
    @Nullable
    public static WeaponType of(ItemStack stack) {
        if (Forging.slotOf(stack) != EquipmentSlot.MAINHAND) return null;
        if (stack.getItem() instanceof CrossbowItem || stack.is(Tags.Items.TOOLS_CROSSBOW)) return CROSSBOW;
        if (stack.getItem() instanceof BowItem || stack.is(Tags.Items.TOOLS_BOW)) return BOW;
        if (stack.getItem() instanceof TridentItem || stack.is(Tags.Items.TOOLS_SPEAR)) return TRIDENT;
        if (stack.getItem() instanceof MaceItem || stack.is(Tags.Items.TOOLS_MACE)) return MACE;
        if (stack.is(ItemTags.AXES)) return AXE;
        return SWORD; // les autres armes de corps à corps se forgent comme des épées
    }
}
