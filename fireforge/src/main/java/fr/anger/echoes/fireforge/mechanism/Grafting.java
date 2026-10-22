package fr.anger.echoes.fireforge.mechanism;

import fr.anger.echoes.fireforge.registry.FFDataComponents;
import fr.anger.echoes.fireforge.registry.FFItems;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Nullable;

/** La greffe d'un mécanisme à l'établi : une pièce, un gabarit (consommé) et un engrenage (consommé). */
public final class Grafting {

    private Grafting() {}

    /** Ce qui manque ou ce qui bloque, affiché dans l'interface. */
    public enum Status {
        NO_PIECE, NO_TEMPLATE, NO_GEAR, WRONG_PIECE, ENCHANTED_TRIDENT, ALREADY, OUTPUT_FULL, READY;

        public String key() {
            return "gui.echoes_fireforge.workbench.status." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    @Nullable
    public static Mechanism mechanismOf(ItemStack template) {
        return template.getItem() instanceof MechanismTemplateItem item ? item.mechanism() : null;
    }

    public static Status status(ItemStack piece, ItemStack template, ItemStack gear, ItemStack output) {
        if (!output.isEmpty()) return Status.OUTPUT_FULL; // d'abord reprendre la pièce greffée
        if (piece.isEmpty()) return Status.NO_PIECE;
        Mechanism mechanism = mechanismOf(template);
        if (mechanism == null) return Status.NO_TEMPLATE;
        if (!mechanism.fits(piece)) return Status.WRONG_PIECE;
        if (mechanism == Mechanism.TRIDENT_DE_TREMPE && throwEnchanted(piece)) return Status.ENCHANTED_TRIDENT;
        if (piece.get(FFDataComponents.MECHANISM) == mechanism) return Status.ALREADY;
        if (!gear.is(FFItems.MACHINE_GEAR.get())) return Status.NO_GEAR;
        return Status.READY;
    }

    /** La pièce greffée : une nouvelle greffe remplace l'ancienne ; rareté, Spécial et enchantements restent. */
    public static ItemStack graft(ItemStack piece, Mechanism mechanism) {
        ItemStack result = piece.copyWithCount(1);
        result.set(FFDataComponents.MECHANISM, mechanism);
        return result;
    }

    /** Le Trident de trempe ne se lance plus : Loyauté et Impulsion n'y serviraient à rien (validé : greffe refusée). */
    private static boolean throwEnchanted(ItemStack trident) {
        for (Holder<Enchantment> enchantment : trident.getTagEnchantments().keySet()) {
            if (enchantment.is(Enchantments.LOYALTY) || enchantment.is(Enchantments.RIPTIDE)) return true;
        }
        return false;
    }
}
