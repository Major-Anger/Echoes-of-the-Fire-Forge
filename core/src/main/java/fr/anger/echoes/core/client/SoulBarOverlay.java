package fr.anger.echoes.core.client;

import fr.anger.echoes.core.EchoesCore;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/** La barre d'âmes, à gauche de la barre d'outils (même rendu que dans Echoes of the Silt). */
public final class SoulBarOverlay implements LayeredDraw.Layer {

    // Premier plan : le cadre, avec un trou transparent pour la jauge
    private static final ResourceLocation BANNER_TEXTURE = EchoesCore.id("textures/gui/soul_banner.png");
    // Arrière-plan : la jauge bleue qui se remplit
    private static final ResourceLocation BAR_TEXTURE = EchoesCore.id("textures/gui/soul_bar.png");

    private static final int BANNER_WIDTH = 94;
    private static final int BANNER_HEIGHT = 20;
    private static final int BAR_WIDTH = 90;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_OFFSET_X = 2;
    private static final int BAR_OFFSET_Y = 12;
    private static final float SCALE = 1.5F;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        int souls = ClientSouls.get();
        if (souls <= 0 && !SoulBar.forcedVisible()) return;

        graphics.pose().pushPose();
        graphics.pose().scale(SCALE, SCALE, SCALE);

        int x = (int) ((graphics.guiWidth() / 2) / SCALE - 97 - 29 - 10 - BANNER_WIDTH + 59);
        int y = (int) ((graphics.guiHeight() / SCALE) - 25);

        int fill = (int) (((float) souls / ClientSouls.max()) * BAR_WIDTH);
        if (fill > 0) {
            graphics.blit(BAR_TEXTURE, x + BAR_OFFSET_X, y + BAR_OFFSET_Y, 0, 5, Math.min(fill, BAR_WIDTH), BAR_HEIGHT, 256, 256);
        }
        graphics.blit(BANNER_TEXTURE, x, y, 0, 0, BANNER_WIDTH, BANNER_HEIGHT, 256, 256);

        for (SoulBar.Decoration decoration : SoulBar.decorations()) decoration.render(graphics, x, y, delta);

        graphics.pose().popPose();
    }
}
