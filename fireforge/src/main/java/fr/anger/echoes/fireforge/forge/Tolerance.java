package fr.anger.echoes.fireforge.forge;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Jusqu'à quelle chaleur un objet tient sans se fendre (docs/plan-fire-forge.md, section 5).
 * Chaque palier au-dessus fait craquer la forge une fois pendant la reforge.
 * <p>
 * Ordre de recherche : les tags {@code echoes_fireforge:tolerance/<chaleur>}, puis l'ingrédient de réparation
 * (pour les objets d'autres mods), sinon « jusqu'à Ardent ».
 */
public final class Tolerance {

    private Tolerance() {}

    /** Au-delà de ce nombre, les craquements ne tiendraient plus dans une reforge. */
    public static final int MAX_CRACKS = 3;

    private static final List<Heat> TAGGED = List.of(Heat.FOURNAISE, Heat.BRULANT, Heat.ARDENT, Heat.CHAUD);

    /** Les ingrédients de réparation connus, du plus résistant au plus fragile. */
    private static final List<Repair> REPAIRS = List.of(
            new Repair(Items.NETHERITE_INGOT, Heat.FOURNAISE),
            new Repair(Items.DIAMOND, Heat.BRULANT),
            new Repair(Items.IRON_INGOT, Heat.ARDENT),
            new Repair(Items.COBBLESTONE, Heat.ARDENT),
            new Repair(Items.GOLD_INGOT, Heat.CHAUD),
            new Repair(Items.LEATHER, Heat.CHAUD),
            new Repair(Items.OAK_PLANKS, Heat.CHAUD));

    private record Repair(Item item, Heat tolerance) {}

    public static TagKey<Item> tag(Heat heat) {
        return TagKey.create(Registries.ITEM, EchoesFireForge.id("tolerance/" + heat.getSerializedName()));
    }

    /** La chaleur la plus forte que cet objet supporte sans craquer. */
    public static Heat of(ItemStack stack) {
        for (Heat heat : TAGGED) {
            if (stack.is(tag(heat))) return heat;
        }
        for (Repair repair : REPAIRS) {
            if (stack.getItem().isValidRepairItem(stack, new ItemStack(repair.item()))) return repair.tolerance();
        }
        return Heat.ARDENT;
    }

    /** Combien de fois la forge craquera en reforgeant cet objet à cette chaleur. */
    public static int cracks(ItemStack stack, Heat heat) {
        if (stack.isEmpty() || heat == Heat.NONE) return 0;
        return Math.min(MAX_CRACKS, Math.max(0, heat.level() - of(stack).level()));
    }
}
