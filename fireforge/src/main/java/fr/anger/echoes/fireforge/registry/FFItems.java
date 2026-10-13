package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.plan.PlanItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tout ce qui vient des machines des Attiseurs a survécu à la chaleur : rien ne brûle dans la lave. */
public final class FFItems {

    private FFItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EchoesFireForge.MOD_ID);

    public static final DeferredItem<BlockItem> COMPRESSED_MACHINE =
            ITEMS.registerSimpleBlockItem(FFBlocks.COMPRESSED_MACHINE, new Item.Properties().fireResistant());

    public static final DeferredItem<BlockItem> FIRE_FORGE =
            ITEMS.registerSimpleBlockItem(FFBlocks.FIRE_FORGE, new Item.Properties().fireResistant());

    public static final DeferredItem<BlockItem> BOILER =
            ITEMS.registerSimpleBlockItem(FFBlocks.BOILER, new Item.Properties().fireResistant());

    public static final DeferredItem<BlockItem> LAVA_TANK =
            ITEMS.registerSimpleBlockItem(FFBlocks.LAVA_TANK, new Item.Properties().fireResistant());

    public static final DeferredItem<BlockItem> SOUL_MODULE =
            ITEMS.registerSimpleBlockItem(FFBlocks.SOUL_MODULE, new Item.Properties().fireResistant());

    // Les pièces de machine
    public static final DeferredItem<Item> MACHINE_GEAR = ITEMS.registerSimpleItem("machine_gear", new Item.Properties().fireResistant());
    public static final DeferredItem<Item> MACHINE_VALVE = ITEMS.registerSimpleItem("machine_valve", new Item.Properties().fireResistant());
    public static final DeferredItem<Item> ENGRAVED_PLATE = ITEMS.registerSimpleItem("engraved_plate", new Item.Properties().fireResistant());

    // Les plans (de fines feuilles de laiton gravées)
    public static final DeferredItem<PlanItem> FORGE_PLAN = ITEMS.register("forge_plan",
            () -> new PlanItem(true, new Item.Properties().fireResistant().stacksTo(16)));
    public static final DeferredItem<PlanItem> ATTISEUR_PLAN = ITEMS.register("attiseur_plan",
            () -> new PlanItem(false, new Item.Properties().fireResistant().stacksTo(16).component(FFDataComponents.PLAN_NUMBER, 1)));
}
