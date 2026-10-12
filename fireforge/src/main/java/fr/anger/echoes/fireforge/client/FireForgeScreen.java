package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.FireForgeBlockEntity.Status;
import fr.anger.echoes.fireforge.forge.FireForgeMenu;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import fr.anger.echoes.fireforge.forge.Heat;
import fr.anger.echoes.fireforge.forging.Rarity;
import fr.anger.echoes.fireforge.forging.RarityOdds;
import fr.anger.echoes.fireforge.plan.Plans;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

/** L'interface de la forge : fer sombre et laiton, et un marteau à la place d'un bouton. */
public class FireForgeScreen extends AbstractContainerScreen<FireForgeMenu> {

    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/gui/fire_forge.png");
    private static final int BRASS = 0xE0B85A, LIGHT = 0xD8CFC0, FADED = 0x9A8F80, WARN = 0xE0805A;
    private static final int HAMMER_X = 152, HAMMER_Y = 17, HAMMER_SIZE = 24;
    private static final int WATER_GAUGE_X = 9, HEAT_GAUGE_X = 47, GAUGE_Y = 19, GAUGE_H = 50;
    /** Le volet des chances, accroché à droite de la forge. */
    private static final int PANEL_W = 86, PANEL_H = 98;

    public FireForgeScreen(FireForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 200;
        imageHeight = 190;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 20;
        inventoryLabelY = 98;
    }

    @Override
    protected void init() {
        super.init();
        leftPos -= (PANEL_W + 2) / 2; // centre la forge et son volet ensemble
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        // jauge d'eau (se remplit par le bas)
        int water = menu.water() * GAUGE_H / menu.waterCapacity();
        if (water > 0) g.blit(TEXTURE, x + WATER_GAUGE_X, y + GAUGE_Y + GAUGE_H - water, 200, 48 + GAUGE_H - water, 8, water, 256, 256);
        // jauge de chaleur (un cran par niveau)
        int heat = menu.heat().level() * GAUGE_H / Heat.FOURNAISE.level();
        if (heat > 0) g.blit(TEXTURE, x + HEAT_GAUGE_X, y + GAUGE_Y + GAUGE_H - heat, 210, 48 + GAUGE_H - heat, 8, heat, 256, 256);
        // progression de la reforge
        int done = menu.progress() * 56 / ForgeRules.REFORGE_TICKS;
        if (done > 0) g.blit(TEXTURE, x + 126, y + 46, 200, 100, done, 5, 256, 256);

        // le marteau : frappe pendant la reforge, s'éclaire au survol quand on peut forger
        int u, v;
        if (menu.isWorking()) {
            boolean down = (minecraft.level.getGameTime() / 6) % 2 == 0;
            u = down ? 224 : 200;
            v = down ? 24 : 0;
        } else if (menu.status() == Status.READY) {
            u = overHammer(mouseX, mouseY) ? 224 : 200;
            v = 0;
        } else {
            u = 200;
            v = 24;
        }
        g.blit(TEXTURE, x + HAMMER_X, y + HAMMER_Y, u, v, HAMMER_SIZE, HAMMER_SIZE, 256, 256);
        renderOdds(g, x + imageWidth + 2, y);
    }

    /** Les chances de chaque rareté, avec la chaleur, l'offrande posée et les plans lus par le joueur. */
    private void renderOdds(GuiGraphics g, int px, int py) {
        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF7A5A24);
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + PANEL_H - 1, 0xFFA5822F);
        g.fill(px + 2, py + 2, px + PANEL_W - 2, py + PANEL_H - 2, 0xFF2F2A2A);
        g.drawString(font, Component.translatable("gui.echoes_fireforge.odds"), px + 6, py + 6, BRASS, false);
        int offering = ForgeRules.offeringTier(menu.offering());
        double[] odds = RarityOdds.compute(Plans.clientReadCount(), offering, menu.heat());
        Rarity[] all = Rarity.values();
        for (int i = 0; i < all.length; i++) {
            int ly = py + 20 + i * 11;
            boolean possible = odds[i] > 0.0001;
            g.drawString(font, all[i].displayName(), px + 6, ly, possible ? all[i].color().getColor() : 0x55504A, false);
            String value = possible ? formatOdds(odds[i]) : "—";
            g.drawString(font, value, px + PANEL_W - 6 - font.width(value), ly, possible ? LIGHT : 0x55504A, false);
        }
        if (offering > 0) {
            String enchant = formatOdds(RarityOdds.enchantChance(offering));
            g.drawString(font, Component.translatable("gui.echoes_fireforge.enchant_odds", enchant), px + 6, py + PANEL_H - 13, 0xC58CE0, false);
        }
    }

    private static String formatOdds(double value) {
        return value >= 9.95 ? Math.round(value) + " %" : String.format(Locale.ROOT, "%.1f %%", value).replace('.', ',');
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, BRASS, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LIGHT, false);

        // panneau d'état : ce qui se passe, ou ce qui manque
        Status status = menu.status();
        int color = status == Status.READY || status == Status.WORKING ? LIGHT : WARN;
        List<FormattedCharSequence> lines = font.split(Component.translatable("status.echoes_fireforge." + status.name().toLowerCase()), 62);
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), 128, 60 + i * 10, color, false);
        Heat heat = menu.heat();
        g.drawString(font, heat == Heat.NONE ? Component.translatable("heat.echoes_fireforge.cold") : heat.displayName(), 128, 79, heat == Heat.NONE ? FADED : BRASS, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        if (menu.getCarried().isEmpty()) {
            if (inside(mouseX, mouseY, WATER_GAUGE_X - 1, GAUGE_Y - 1, 10, GAUGE_H + 2)) {
                g.renderTooltip(font, Component.translatable("tooltip.echoes_fireforge.water", menu.water(), menu.waterCapacity()), mouseX, mouseY);
            } else if (inside(mouseX, mouseY, HEAT_GAUGE_X - 1, GAUGE_Y - 1, 10, GAUGE_H + 2)) {
                Heat heat = menu.heat();
                g.renderTooltip(font, heat == Heat.NONE ? Component.translatable("heat.echoes_fireforge.cold") : heat.displayName(), mouseX, mouseY);
            } else if (overHammer(mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable("tooltip.echoes_fireforge.hammer"), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overHammer(mouseX, mouseY) && menu.status() == Status.READY) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FireForgeMenu.BUTTON_FORGE);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_PLACE, 1.4F, 0.4F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    private boolean overHammer(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, HAMMER_X, HAMMER_Y, HAMMER_SIZE, HAMMER_SIZE);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        double mx = mouseX - leftPos, my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
