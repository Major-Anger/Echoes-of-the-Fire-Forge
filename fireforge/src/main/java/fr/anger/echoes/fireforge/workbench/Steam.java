package fr.anger.echoes.fireforge.workbench;

import fr.anger.echoes.fireforge.mechanism.Mechanism;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * La vapeur des mécanismes d'armure (plan, section 11) : 20 charges par pièce, rechargées au bouilleur de l'établi.
 * Une recharge complète coûte 1 charbon et 1 seau d'eau, en 10 secondes (validé).
 */
public final class Steam {

    private Steam() {}

    public static final int CHARGES = 20;
    /** Une pièce rechargée par un forgeron qui a l'Écho « Vapeur domptée » tient 22 charges (plan, 12a). */
    public static final int TEMPERED_CHARGES = 22;
    /** Par charge : 1/20 de seau d'eau et 1/20 de charbon (un charbon brûle 1600 ticks dans un fourneau). */
    public static final int WATER_PER_CHARGE = 50, FUEL_PER_CHARGE = 80, TICKS_PER_CHARGE = 10;
    /** La réserve d'eau du bouilleur : 4 seaux. */
    public static final int TANK = 4000;

    /** La pièce porte un mécanisme à vapeur. */
    public static boolean usesSteam(ItemStack stack) {
        Mechanism mechanism = stack.get(FFDataComponents.MECHANISM);
        return mechanism != null && mechanism.steam();
    }

    public static int charges(ItemStack stack) {
        return usesSteam(stack) ? stack.getOrDefault(FFDataComponents.STEAM, 0) : 0;
    }

    /** Combien de charges cette pièce peut tenir (20, ou 22 avec l'Écho « Vapeur domptée »). */
    public static int capacity(ItemStack stack) {
        return stack.getOrDefault(FFDataComponents.STEAM_CAPACITY, CHARGES);
    }

    public static void setCharges(ItemStack stack, int charges) {
        stack.set(FFDataComponents.STEAM, Math.max(0, Math.min(capacity(stack), charges)));
    }

    /** Dépense une charge (étape 8c). @return true si la pièce en avait une */
    public static boolean spend(ItemStack stack) {
        int charges = charges(stack);
        if (charges <= 0) return false;
        setCharges(stack, charges - 1);
        return true;
    }
}
