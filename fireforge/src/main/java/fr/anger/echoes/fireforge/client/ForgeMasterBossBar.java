package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * La barre du maître de forge, à la place de celle du jeu (aperçu validé) : un tube de laiton riveté, sa vie en métal
 * en fusion, un rouage qui tourne (plus vite en phase 2), des fêlures et de la vapeur sous la moitié de sa vie.
 * Textures : tools/textures/make_forgemaster_bar.py.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class ForgeMasterBossBar {

    public static final String NAME_KEY = "entity.echoes_fireforge.forge_master";
    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/gui/forge_master_bar.png");
    private static final int W = 200, H = 15, FILL_FROM = 12, FILL_TO = 192, TEX_W = 256, TEX_H = 64, GEAR = 15;

    private ForgeMasterBossBar() {}

    @SubscribeEvent
    public static void onBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents name) || !NAME_KEY.equals(name.getKey())) return;
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        int x = g.guiWidth() / 2 - W / 2, y = event.getY();
        float progress = boss.getProgress();
        boolean phaseTwo = progress < 0.5F;
        g.blit(TEXTURE, x, y, 0, 0, W, H, TEX_W, TEX_H);
        int fill = FILL_FROM + Math.round(progress * (FILL_TO - FILL_FROM + 1));
        if (fill > FILL_FROM) g.blit(TEXTURE, x, y, 0, 16, fill, H, TEX_W, TEX_H);
        if (phaseTwo) g.blit(TEXTURE, x, y, 0, 32, W, H, TEX_W, TEX_H);
        long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        int frame = (int) ((time / (phaseTwo ? 2 : 5)) % 4);
        g.blit(TEXTURE, x, y, frame * 16, 48, GEAR, GEAR, TEX_W, TEX_H);
        var font = Minecraft.getInstance().font;
        g.drawString(font, boss.getName(), g.guiWidth() / 2 - font.width(boss.getName()) / 2, y - 9, 0xFFC46E, true);
        event.setIncrement(H + 11);
    }
}
