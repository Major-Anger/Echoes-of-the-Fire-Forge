package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forging.ForgingData;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Sous le nom de l'objet : sa rareté, ses statistiques en pourcentage, et son Spécial. */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class ForgingTooltip {

    private ForgingTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ForgingData data = event.getItemStack().get(FFDataComponents.FORGING);
        if (data == null) return;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.echoes_fireforge.forged", data.rarity().displayName()).withStyle(ChatFormatting.GRAY));
        for (ForgingData.StatRoll roll : data.stats()) {
            boolean bonus = roll.percent() >= 0;
            lines.add(Component.literal(" " + percent(roll.percent()) + " ").append(roll.stat().displayName())
                    .withStyle(bonus ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
        }
        data.special().ifPresent(special -> {
            lines.add(Component.literal(" ").append(special.displayName()));
            lines.add(Component.literal("  ").append(special.effect()).withStyle(ChatFormatting.DARK_GREEN));
            lines.add(Component.literal("  ").append(special.drawback()).withStyle(ChatFormatting.RED));
        });
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    /** +4,2 % en français, +4.2% en anglais. */
    private static String percent(float value) {
        boolean french = Minecraft.getInstance().getLanguageManager().getSelected().startsWith("fr");
        String number = String.format(french ? Locale.FRANCE : Locale.ROOT, "%+.1f", value);
        return french ? number + " %" : number + "%";
    }
}
