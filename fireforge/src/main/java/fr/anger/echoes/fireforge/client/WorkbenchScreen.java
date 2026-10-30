package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import fr.anger.echoes.fireforge.mechanism.Grafting;
import fr.anger.echoes.fireforge.workbench.Steam;
import fr.anger.echoes.fireforge.workbench.WorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** L'interface de l'établi : la même famille que la forge (fer sombre, laiton), une clé à la place du marteau. */
public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {

    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/gui/workbench.png");
    private static final int BRASS = 0xE0B85A, COPPER = 0xE2966A, LIGHT = 0xD8CFC0, FADED = 0x9A8F80, WARN = 0xE0805A;
    private static final int KEY_X = 98, KEY_Y = 31, KEY_SIZE = 24;
    /** Le panneau du bouilleur (texture en 176, 48) et ses jauges, relatifs au panneau. */
    private static final int PANEL_W = 76, PANEL_H = 120, GAUGE_X = 9, GAUGE_Y = 49, GAUGE_W = 10, GAUGE_H = 62;
    private static final int FUEL_BAR_X = 31, FUEL_BAR_Y = 32, FUEL_BAR_W = 36, CHARGE_BAR_X = 27, CHARGE_BAR_Y = 101, CHARGE_BAR_W = 44;
    /** La clé reste tournée quelques ticks après le coup. */
    private int turning;

    public WorkbenchScreen(WorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        // le panneau du bouilleur est accroché à droite : on centre l'ensemble
        if (menu.hasBoiler()) leftPos = (width - (imageWidth + 2 + PANEL_W)) / 2;
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
        if (menu.hasBoiler()) renderBoiler(g, x + WorkbenchMenu.PANEL_X, y);
    }

    /** Le panneau du bouilleur : combustible, eau, et les charges de la pièce posée. */
    private void renderBoiler(GuiGraphics g, int px, int py) {
        g.blit(TEXTURE, px, py, 176, 48, PANEL_W, PANEL_H, 256, 256);
        int water = menu.boilerWater() * GAUGE_H / Steam.TANK;
        if (water > 0) g.blit(TEXTURE, px + GAUGE_X, py + GAUGE_Y + GAUGE_H - water, 0, 168 + GAUGE_H - water, GAUGE_W, water, 256, 256);
        int fuel = Math.min(FUEL_BAR_W, menu.boilerFuel() * FUEL_BAR_W / (Steam.FUEL_PER_CHARGE * Steam.CHARGES));
        if (fuel > 0) g.blit(TEXTURE, px + FUEL_BAR_X, py + FUEL_BAR_Y, 12, 168, fuel, 4, 256, 256);
        ItemStack piece = menu.getSlot(WorkbenchMenu.SLOT_BOILER_PIECE).getItem();
        if (Steam.usesSteam(piece)) {
            // les charges, plus la charge en cours qui se remplit
            int charges = Steam.charges(piece);
            int capacity = Steam.capacity(piece);
            int filled = Math.min(CHARGE_BAR_W, charges * CHARGE_BAR_W / capacity
                    + (charges < capacity ? menu.boilerProgress() * CHARGE_BAR_W / capacity / Steam.TICKS_PER_CHARGE : 0));
            if (filled > 0) g.blit(TEXTURE, px + CHARGE_BAR_X, py + CHARGE_BAR_Y, 12, 174, filled, 4, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (menu.hasBoiler()) g.drawString(font, Component.translatable("gui.echoes_fireforge.steam_boiler"), WorkbenchMenu.PANEL_X + 6, 6, COPPER, false);
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
        if (menu.hasBoiler() && menu.getCarried().isEmpty()) {
            int px = leftPos + WorkbenchMenu.PANEL_X, py = topPos;
            if (inside(mouseX, mouseY, px + GAUGE_X - 1, py + GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2)) {
                g.renderComponentTooltip(font, List.of(Component.translatable("gui.echoes_fireforge.steam_boiler.water", menu.boilerWater(), Steam.TANK),
                        Component.translatable("gui.echoes_fireforge.steam_boiler.water.hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            } else if (inside(mouseX, mouseY, px + FUEL_BAR_X - 1, py + FUEL_BAR_Y - 1, FUEL_BAR_W + 2, 6)) {
                g.renderTooltip(font, Component.translatable("gui.echoes_fireforge.steam_boiler.fuel", menu.boilerFuel() / Steam.FUEL_PER_CHARGE), mouseX, mouseY);
            } else if (inside(mouseX, mouseY, px + CHARGE_BAR_X - 1, py + CHARGE_BAR_Y - 1, CHARGE_BAR_W + 2, 6)) {
                ItemStack piece = menu.getSlot(WorkbenchMenu.SLOT_BOILER_PIECE).getItem();
                g.renderTooltip(font, Component.translatable("tooltip.echoes_fireforge.steam", Steam.charges(piece), Steam.capacity(piece)), mouseX, mouseY);
            }
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
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
        // un seau d'eau à la souris sur la jauge : on le verse
        if (button == 0 && menu.hasBoiler() && ForgeRules.waterAmount(menu.getCarried()) > 0
                && inside(mouseX, mouseY, leftPos + WorkbenchMenu.PANEL_X + GAUGE_X - 1, topPos + GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkbenchMenu.BUTTON_POUR);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Le panneau du bouilleur compte dans l'interface (un clic dessus n'y jette pas l'objet tenu). */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (menu.hasBoiler() && inside(mouseX, mouseY, left + WorkbenchMenu.PANEL_X, top, PANEL_W, PANEL_H)) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }
}
