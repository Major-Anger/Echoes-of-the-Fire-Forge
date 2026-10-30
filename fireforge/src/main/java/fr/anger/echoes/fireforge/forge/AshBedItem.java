package fr.anger.echoes.fireforge.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Le Lit de cendre (étape 12c) : les Attiseurs garnissaient leurs foyers de cendre pour tenir une chaleur égale.
 * Posé dans la forge d'un clic droit (voir {@link FireForgeBlock#useItemOn}), il y reste pour toujours, même quand
 * on la casse, et fixe une rareté minimum selon l'offrande ({@link fr.anger.echoes.fireforge.forging.RarityOdds#ashBed}).
 */
public class AshBedItem extends Item {

    public AshBedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.ash_bed").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.ash_bed.offering").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.ash_bed.scrap").withStyle(ChatFormatting.DARK_GRAY));
    }
}
