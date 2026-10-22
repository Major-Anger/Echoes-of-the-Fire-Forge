package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.mechanism.Grafting;
import fr.anger.echoes.fireforge.workbench.WorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/** L'interface de l'établi : la même famille que la forge (fer sombre, laiton), une clé à la place du marteau. */
public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {

    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/gui/workbench.png");
    private static final int BRASS = 0xE0B85A, LIGHT = 0xD8CFC0, FADED = 0x9A8F80, WARN = 0xE0805A;
    private static final int KEY_X = 98, KEY_Y = 31, KEY_SIZE = 24;
    /** La clé reste tournée quelques ticks après le coup. */
    private int turning;

    public WorkbenchScreen(WorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (turning > 0) turning--;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        boolean ready = menu.status() == Grafting.Status.READY;
        // sprites : normale (176, 0), survolée (200, 0), éteinte (176, 24), qui serre (200, 24)
        int u, v;
        if (turning > 0) {
            u = 200;
            v = 24;
        } else if (ready) {
            u = overKey(mouseX, mouseY) ? 200 : 176;
            v = 0;
        } else {
            u = 176;
            v = 24;
        }
        g.blit(TEXTURE, x + KEY_X, y + KEY_Y, u, v, KEY_SIZE, KEY_SIZE, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, BRASS, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LIGHT, false);
        Grafting.Status status = menu.status();
        Component text = Component.translatable(status.key());
        int color = status == Grafting.Status.READY ? LIGHT
                : status == Grafting.Status.WRONG_PIECE || status == Grafting.Status.ENCHANTED_TRIDENT ? WARN : FADED;
        g.drawString(font, text, (imageWidth - font.width(text)) / 2, 59, color, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (overKey(mouseX, mouseY) && menu.getCarried().isEmpty()) {
            g.renderTooltip(font, Component.translatable("gui.echoes_fireforge.workbench.key"), mouseX, mouseY);
        }
    }

    private boolean overKey(double mouseX, double mouseY) {
        return mouseX >= leftPos + KEY_X && mouseX < leftPos + KEY_X + KEY_SIZE && mouseY >= topPos + KEY_Y && mouseY < topPos + KEY_Y + KEY_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overKey(mouseX, mouseY) && menu.status() == Grafting.Status.READY) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.BUTTON_GRAFT);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 1.2F));
            turning = 8;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
