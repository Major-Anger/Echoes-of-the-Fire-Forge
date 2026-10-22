package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.workbench.Steam;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

/**
 * La barre de vapeur (validé : B) : un trait blanc dans le bord bas de la barre d'objets, seulement quand on porte
 * une pièce à vapeur. Deux pièces à vapeur (plastron et jambières) : deux moitiés. Rien d'autre ne bouge dans le HUD.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SteamBar {

    private SteamBar() {}

    private static final int FULL = 0xFFF2F2F2, EMPTY = 0xFF34343A;

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, EchoesFireForge.id("steam_bar"), SteamBar::render);
    }

    /** Les pièces à vapeur portées, du haut vers le bas. */
    public static List<ItemStack> steamPieces(Player player) {
        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (Steam.usesSteam(stack)) pieces.add(stack);
        }
        return pieces;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        List<ItemStack> pieces = steamPieces(mc.player);
        if (pieces.isEmpty()) return;
        int left = g.guiWidth() / 2 - 91 + 2, y = g.guiHeight() - 2, width = 178, gap = 2;
        int each = (width - gap * (pieces.size() - 1)) / pieces.size();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200); // devant le cadre de la case choisie
        for (int i = 0; i < pieces.size(); i++) {
            int x = left + i * (each + gap);
            int filled = Steam.charges(pieces.get(i)) * each / Steam.CHARGES;
            g.fill(x, y, x + each, y + 1, EMPTY);
            if (filled > 0) g.fill(x, y, x + filled, y + 1, FULL);
        }
        g.pose().popPose();
    }
}
