package fr.anger.echoes.core.client;

import fr.anger.echoes.core.CoreClientConfig;
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
    /**
     * Où s'accroche la bannière, en pixels d'interface (quelle que soit l'échelle) : son bord droit à 116 px à
     * gauche du milieu de l'écran (à gauche de la barre d'outils), son bord bas à 8 px du bas. C'est la place
     * qu'elle avait à l'échelle 1,5 ; plus grande ou plus petite, elle grandit vers la gauche et vers le haut.
     */
    private static final float RIGHT_FROM_CENTER = 116, BOTTOM_MARGIN = 8, SCREEN_MARGIN = 2;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        int souls = ClientSouls.get();
        if (souls <= 0 && !SoulBar.forcedVisible()) return;

        float scale = CoreClientConfig.soulBarScale();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, scale);

        // dans le repère mis à l'échelle ; la bannière reste toujours entière à l'écran
        float width = graphics.guiWidth() / scale, height = graphics.guiHeight() / scale;
        int x = Math.round((graphics.guiWidth() / 2F - RIGHT_FROM_CENTER) / scale) - BANNER_WIDTH;
        int y = Math.round((graphics.guiHeight() - BOTTOM_MARGIN) / scale) - BANNER_HEIGHT;
        x = clamp(x, SCREEN_MARGIN, width - SCREEN_MARGIN - BANNER_WIDTH);
        y = clamp(y, SCREEN_MARGIN, height - SCREEN_MARGIN - BANNER_HEIGHT);

        int fill = (int) (((float) souls / ClientSouls.max()) * BAR_WIDTH);
        if (fill > 0) {
            graphics.blit(BAR_TEXTURE, x + BAR_OFFSET_X, y + BAR_OFFSET_Y, 0, 5, Math.min(fill, BAR_WIDTH), BAR_HEIGHT, 256, 256);
        }
        graphics.blit(BANNER_TEXTURE, x, y, 0, 0, BANNER_WIDTH, BANNER_HEIGHT, 256, 256);

        // les décorations des épisodes : le coin haut gauche de la bannière, dans ce même repère mis à l'échelle
        for (SoulBar.Decoration decoration : SoulBar.decorations()) decoration.render(graphics, x, y, delta);

        graphics.pose().popPose();
    }

    /** Entre min et max ; si l'écran est trop petit pour la bannière, on garde le bord gauche (ou haut). */
    private static int clamp(int value, float min, float max) {
        return (int) Math.max(min, Math.min(value, Math.max(min, max)));
    }
}
