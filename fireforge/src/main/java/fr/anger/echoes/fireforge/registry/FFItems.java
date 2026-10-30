package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.AshBedItem;
import fr.anger.echoes.fireforge.mechanism.Mechanism;
import fr.anger.echoes.fireforge.mechanism.MechanismTemplateItem;
import fr.anger.echoes.fireforge.plan.PlanItem;
import fr.anger.echoes.fireforge.plan.Plans;
import fr.anger.echoes.fireforge.witherset.WitherArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
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

    // Le set du Wither (plan, section 13) : forgé à la table de forgeron sur la netherite
    public static final DeferredItem<WitherArmorItem> WITHER_HELMET = witherPiece("wither_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<WitherArmorItem> WITHER_CHESTPLATE = witherPiece("wither_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<WitherArmorItem> WITHER_LEGGINGS = witherPiece("wither_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<WitherArmorItem> WITHER_BOOTS = witherPiece("wither_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<SmithingTemplateItem> WITHER_TEMPLATE = ITEMS.register("wither_template", () -> new SmithingTemplateItem(
            Component.translatable("item.echoes_fireforge.wither_template.applies_to").withStyle(ChatFormatting.BLUE),
            Component.translatable("item.echoes_fireforge.wither_template.ingredients").withStyle(ChatFormatting.BLUE),
            Component.translatable("item.echoes_fireforge.wither_template.upgrade").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.echoes_fireforge.wither_template.base_slot"),
            Component.translatable("item.echoes_fireforge.wither_template.additions_slot"),
            List.of(ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"), ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
                    ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"), ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")),
            List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"))));

    private static DeferredItem<WitherArmorItem> witherPiece(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new WitherArmorItem(FFArmorMaterials.WITHER, type,
                new Item.Properties().fireResistant().rarity(Rarity.EPIC).durability(type.getDurability(40))));
    }

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
