package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.mechanism.PieceStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Le Sac dorsal et les Poches : une rangée de cases en laiton au-dessus de l'inventaire (dessinée, sans texture). */
public class PieceStorageScreen extends AbstractContainerScreen<PieceStorageMenu> {

    private static final int SOOT = 0xFF1F1B1C, IRON_DARK = 0xFF2F2A2A, IRON = 0xFF3D3735, IRON_LIGHT = 0xFF6B625C;
    private static final int BRASS_DARK = 0xFF7A5A24, BRASS = 0xFFA5822F, BRASS_LIGHT = 0xFFCFA64A, SLOT = 0xFF282424;
    private static final int TITLE = 0xE0B85A, LIGHT = 0xD8CFC0;

    public PieceStorageScreen(PieceStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = PieceStorageMenu.INV_Y + 58 + 18 + 7;
        inventoryLabelY = PieceStorageMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, BRASS_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BRASS_LIGHT);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, BRASS);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, IRON);
        // la rangée portée dans la pièce : des cases cerclées de laiton
        int left = (w - menu.size() * 18) / 2;
        g.fill(x + left - 2, y + PieceStorageMenu.ROW_Y - 3, x + left + menu.size() * 18 + 2, y + PieceStorageMenu.ROW_Y + 19, BRASS_DARK);
        for (int i = 0; i < menu.size(); i++) slot(g, x + left + i * 18 + 1, y + PieceStorageMenu.ROW_Y);
        g.fill(x + 3, y + PieceStorageMenu.INV_Y - 14, x + w - 3, y + PieceStorageMenu.INV_Y - 13, SOOT);
        g.fill(x + 3, y + PieceStorageMenu.INV_Y - 13, x + w - 3, y + PieceStorageMenu.INV_Y - 12, IRON_LIGHT);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + PieceStorageMenu.INV_Y + row * 18);
        for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + PieceStorageMenu.INV_Y + 58);
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SOOT);
        g.fill(x, y, x + 17, y + 17, IRON_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT);
        g.fill(x, y, x + 16, y + 1, IRON_DARK);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TITLE, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LIGHT, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
