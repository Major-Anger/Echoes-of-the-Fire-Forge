package fr.anger.echoes.fireforge.mechanism;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Un gabarit de mécanisme, comme un modèle d'armure : une plaque de laiton (ou d'acier bleui, pour les rares)
 * gravée du mécanisme. Consommé à la greffe ; il se duplique à l'établi de fabrication avec des engrenages et du cuivre.
 */
public class MechanismTemplateItem extends Item {

    private final Mechanism mechanism;

    public MechanismTemplateItem(Mechanism mechanism, Properties properties) {
        super(properties);
        this.mechanism = mechanism;
    }

    public Mechanism mechanism() {
        return mechanism;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.template", mechanism.displayName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" ").append(mechanism.description()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.template.target", mechanism.target()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
