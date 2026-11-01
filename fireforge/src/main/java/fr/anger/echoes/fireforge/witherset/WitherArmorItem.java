package fr.anger.echoes.fireforge.witherset;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Une pièce du set du Wither. Elle garde le palier de son dernier porteur (composant {@code wither_stage}), et son
 * aspect suit : neuve, fêlée, la braise dans les fêlures, l'acier qui s'écaille, l'os noirci.
 */
public class WitherArmorItem extends ArmorItem {

    public WitherArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static int stage(ItemStack stack) {
        return stack.getOrDefault(FFDataComponents.WITHER_STAGE, 0);
    }

    /** La texture du palier : wither_layer_<1 ou 2>_<palier>.png. */
    public static ResourceLocation texture(int stage, boolean inner, boolean glow) {
        return EchoesFireForge.id("textures/models/armor/wither_layer_" + (inner ? 2 : 1) + "_" + stage + (glow ? "_glow" : "") + ".png");
    }

    /** Le Lien de cendre seul ne fait pas briller la pièce : l'armure garde son aspect de cendre. */
    @Override
    public boolean isFoil(ItemStack stack) {
        for (Holder<Enchantment> enchantment : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet()) if (!enchantment.is(WitherSet.ASH_BOND)) return true;
        return false;
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return texture(stage(stack), innerModel, false);
    }
}
