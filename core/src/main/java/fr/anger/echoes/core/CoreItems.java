package fr.anger.echoes.core;

import fr.anger.echoes.core.guide.GuideBookItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoreItems {

    private CoreItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EchoesCore.MOD_ID);

    /** Le livre guide commun à tous les épisodes. */
    public static final DeferredItem<GuideBookItem> GUIDE_BOOK = ITEMS.register("guide_book",
            () -> new GuideBookItem(new Item.Properties().stacksTo(1)));
}
