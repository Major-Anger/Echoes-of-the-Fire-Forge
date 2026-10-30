package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class FFArmorMaterials {

    private FFArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, EchoesFireForge.MOD_ID);

    /**
     * Le set du Wither (plan, section 13) : un peu au-dessus de la netherite (protection et robustesse), sans son bonus
     * de recul. Il se répare avec de la Cendre d'Attiseur. Les textures changent avec le palier du porteur
     * (voir WitherArmorItem#getArmorTexture) ; celles du matériau sont l'armure neuve.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WITHER = MATERIALS.register("wither", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 7);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 11);
            }),
            15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(FFItems.STOKER_ASH.get()),
            List.of(new ArmorMaterial.Layer(EchoesFireForge.id("wither"))), 3.5F, 0.0F));
}
