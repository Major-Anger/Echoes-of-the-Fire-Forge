package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forging.ForgingData;
import fr.anger.echoes.fireforge.forging.ForgingEvents;
import fr.anger.echoes.fireforge.mechanism.Mechanism;
import fr.anger.echoes.fireforge.workbench.Steam;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Sous le nom de l'objet : sa rareté, ses statistiques en pourcentage, son Spécial, et son mécanisme. */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class ForgingTooltip {

    private ForgingTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ForgingData data = event.getItemStack().get(FFDataComponents.FORGING);
        Mechanism mechanism = event.getItemStack().get(FFDataComponents.MECHANISM);
        if (data == null && mechanism == null) return;
        List<Component> lines = new ArrayList<>();
        if (data != null) addForging(data, lines);
        // le mécanisme greffé à l'établi
        if (mechanism != null) {
            lines.add(Component.translatable("tooltip.echoes_fireforge.mechanism", mechanism.displayName()).withStyle(ChatFormatting.GOLD));
            lines.add(Component.literal("  ").append(mechanism.description()).withStyle(ChatFormatting.GRAY));
            if (mechanism == Mechanism.LAMPE) {
                boolean on = event.getItemStack().getOrDefault(FFDataComponents.LAMP_ON, false);
                lines.add(Component.literal("  ").append(Component.translatable(on ? "tooltip.echoes_fireforge.lamp.on" : "tooltip.echoes_fireforge.lamp.off"))
                        .withStyle(on ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
            }
            if (mechanism.steam()) {
                lines.add(Component.literal("  ").append(Component.translatable("tooltip.echoes_fireforge.steam",
                        Steam.charges(event.getItemStack()), Steam.CHARGES)).withStyle(ChatFormatting.WHITE));
            }
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    private static void addForging(ForgingData data, List<Component> lines) {
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
    }

    /** Les modificateurs de la forge s'affichent déjà en pourcentage : on retire leurs lignes vanilla en valeur absolue. */
    @SubscribeEvent
    public static void onSkippedAttributes(GatherSkippedAttributeTooltipsEvent event) {
        ForgingData data = event.getStack().get(FFDataComponents.FORGING);
        if (data == null) return;
        for (ForgingData.StatRoll roll : data.stats()) {
            event.skipId(ForgingEvents.modifierId(roll.stat()));
        }
    }

    /** +4,2 % en français, +4.2% en anglais. */
    static String percent(float value) {
        boolean french = Minecraft.getInstance().getLanguageManager().getSelected().startsWith("fr");
        String number = String.format(french ? Locale.FRANCE : Locale.ROOT, "%+.1f", value);
        return french ? number + " %" : number + "%";
    }
}
