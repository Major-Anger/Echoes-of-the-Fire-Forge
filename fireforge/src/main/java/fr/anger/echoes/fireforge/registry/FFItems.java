package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.AshBedItem;
import fr.anger.echoes.fireforge.mechanism.Mechanism;
import fr.anger.echoes.fireforge.mechanism.MechanismTemplateItem;
import fr.anger.echoes.fireforge.plan.PlanItem;
import fr.anger.echoes.fireforge.plan.Plans;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** Tout ce qui vient des machines des Attiseurs a survécu à la chaleur : rien ne brûle dans la lave. */
public final class FFItems {

    private FFItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EchoesFireForge.MOD_ID);

    public static final DeferredItem<BlockItem> COMPRESSED_MACHINE =
            ITEMS.registerSimpleBlockItem(FFBlocks.COMPRESSED_MACHINE, new Item.Properties().fireResistant());

    public static final DeferredItem<BlockItem> FIRE_FORGE =
            ITEMS.registerSimpleBlockItem(FFBlocks.FIRE_FORGE, new Item.Properties().fireResistant());

    public static final DeferredItem<BlockItem> WORKBENCH =
            ITEMS.registerSimpleBlockItem(FFBlocks.WORKBENCH, new Item.Properties().fireResistant());

    public static final DeferredItem<BlockItem> STEAM_BOILER =
            ITEMS.registerSimpleBlockItem(FFBlocks.STEAM_BOILER, new Item.Properties().fireResistant());

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
            () -> new PlanItem(Plans.FORGE, new Item.Properties().fireResistant().stacksTo(16)));
    public static final DeferredItem<PlanItem> WORKBENCH_PLAN = ITEMS.register("workbench_plan",
            () -> new PlanItem(Plans.WORKBENCH, new Item.Properties().fireResistant().stacksTo(16)));
    public static final DeferredItem<PlanItem> ATTISEUR_PLAN = ITEMS.register("attiseur_plan",
            () -> new PlanItem(null, new Item.Properties().fireResistant().stacksTo(16).component(FFDataComponents.PLAN_NUMBER, 1)));

    // Les Cendreux (étape 12c) : leur cendre, et le Lit de cendre qu'on en tire pour la forge
    public static final DeferredItem<Item> STOKER_ASH = ITEMS.registerSimpleItem("stoker_ash", new Item.Properties().fireResistant());
    public static final DeferredItem<AshBedItem> ASH_BED = ITEMS.register("ash_bed",
            () -> new AshBedItem(new Item.Properties().fireResistant().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> CENDREUX_SPAWN_EGG = ITEMS.register("cendreux_spawn_egg",
            () -> new DeferredSpawnEggItem(FFEntities.CENDREUX, 0x2A2523, 0xE8762A, new Item.Properties()));

    // Les gabarits de mécanisme (un par mécanisme ; les rares en acier bleui)
    public static final Map<Mechanism, DeferredItem<MechanismTemplateItem>> TEMPLATES = templates();

    private static Map<Mechanism, DeferredItem<MechanismTemplateItem>> templates() {
        Map<Mechanism, DeferredItem<MechanismTemplateItem>> map = new EnumMap<>(Mechanism.class);
        for (Mechanism mechanism : Mechanism.values()) {
            map.put(mechanism, ITEMS.register("template_" + mechanism.key(), () -> new MechanismTemplateItem(mechanism,
                    new Item.Properties().fireResistant().rarity(mechanism.rare() ? Rarity.RARE : Rarity.UNCOMMON))));
        }
        return map;
    }
}
