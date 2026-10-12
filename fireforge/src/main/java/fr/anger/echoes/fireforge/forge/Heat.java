package fr.anger.echoes.fireforge.forge;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** La chaleur de la forge, fixée par le combustible : elle plafonne la rareté (voir docs/plan-fire-forge.md). */
public enum Heat {
    NONE("none"),
    TIEDE("tiede"),        // bois, planches          -> max Brut
    CHAUD("chaud"),        // charbon, charbon de bois -> max Trempé
    ARDENT("ardent"),      // bloc de charbon          -> max Affiné
    BRULANT("brulant"),    // bâton de blaze           -> max Attisé
    FOURNAISE("fournaise"); // lave                    -> Spécial possible

    private final String key;
    private final TagKey<Item> tag;

    Heat(String key) {
        this.key = key;
        this.tag = TagKey.create(Registries.ITEM, EchoesFireForge.id("heat/" + key));
    }

    public int level() {
        return ordinal();
    }

    /** Le nom utilisé dans les tags et les traductions (tiede, chaud…). */
    public String getSerializedName() {
        return key;
    }

    public Component displayName() {
        return Component.translatable("heat.echoes_fireforge." + key);
    }

    public static Heat byLevel(int level) {
        Heat[] values = values();
        return level >= 0 && level < values.length ? values[level] : NONE;
    }

    /** Chaleur donnée par un combustible (NONE si ce n'est pas un combustible de la forge). */
    public static Heat ofFuel(ItemStack stack) {
        if (stack.isEmpty()) return NONE;
        if (stack.is(Items.LAVA_BUCKET)) return FOURNAISE;
        for (int i = values().length - 1; i > 0; i--) {
            if (stack.is(values()[i].tag)) return values()[i];
        }
        return NONE;
    }
}
