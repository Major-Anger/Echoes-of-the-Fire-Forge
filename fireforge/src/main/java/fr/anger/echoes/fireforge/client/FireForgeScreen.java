package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.FireForgeBlockEntity.Status;
import fr.anger.echoes.fireforge.forge.FireForgeMenu;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import fr.anger.echoes.fireforge.forge.Heat;
import fr.anger.echoes.fireforge.forge.Tolerance;
import fr.anger.echoes.fireforge.forging.Forging;
import fr.anger.echoes.fireforge.forging.ForgingData;
import fr.anger.echoes.fireforge.forging.Rarity;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import fr.anger.echoes.fireforge.forging.RarityOdds;
import fr.anger.echoes.fireforge.plan.Plans;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** L'interface de la forge : fer sombre et laiton, un marteau à la place d'un bouton, et le soufflet. */
public class FireForgeScreen extends AbstractContainerScreen<FireForgeMenu> {

    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/gui/fire_forge.png");
    private static final int BRASS = 0xE0B85A, LIGHT = 0xD8CFC0, FADED = 0x9A8F80, WARN = 0xE0805A, HOT = 0xFF8C3C, SAFE = 0x8CDC8C;
    private static final int HAMMER_X = 152, HAMMER_Y = 17, HAMMER_SIZE = 24;
    private static final int WATER_GAUGE_X = 9, HEAT_GAUGE_X = 47, GAUGE_Y = 19, GAUGE_H = 50;
    /** La jauge de lave, à droite du combustible, quand un réservoir est branché. */
    private static final int LAVA_X = 80, LAVA_Y = 17, LAVA_W = 10, LAVA_H = 40;
    /** Le soufflet, sous le combustible : sprites normal, survolé, écrasé, alerte (26 x 22, en y = 192). */
    private static final int BELLOWS_X = 62, BELLOWS_Y = 60, BELLOWS_W = 26, BELLOWS_H = 22, BELLOWS_V = 192;
    /** Le volet des chances, accroché à droite de la forge. */
    private static final int PANEL_W = 86, PANEL_H = 110;
    private static final int CRACKS_LINE_Y = PANEL_H - 26;

    /** Le soufflet reste écrasé quelques ticks après un clic. */
    private int bellowsPressed;
    /** Le panneau des âmes, à gauche, quand le module des âmes est branché. */
    private SoulPanel souls;

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
        souls = new SoulPanel(menu, font, TEXTURE);
        layout();
    }

    /** Centre la forge et ses volets : les chances à droite, les âmes à gauche (si le module est branché). */
    private void layout() {
        int soulWidth = menu.hasSoulModule() ? SoulPanel.W + 2 : 0;
        int total = soulWidth + imageWidth + 2 + PANEL_W;
        leftPos = (width - total) / 2 + soulWidth;
    }

    /** Le marteau peut frapper : la forge est prête et le forgeron a de quoi payer. */
    private boolean canForge() {
        return menu.status() == Status.READY && souls.affordable();
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
        int heatU = menu.hasSoulModule() && menu.soulFire() ? 230 : 210; // flamme froide : la jauge passe au cyan
        if (heat > 0) g.blit(TEXTURE, x + HEAT_GAUGE_X, y + GAUGE_Y + GAUGE_H - heat, heatU, 48 + GAUGE_H - heat, 8, heat, 256, 256);
        // lave du réservoir
        if (menu.showsLava()) {
            g.blit(TEXTURE, x + LAVA_X, y + LAVA_Y, 104, 192, LAVA_W, LAVA_H, 256, 256);
            int lava = Math.min(LAVA_H - 2, menu.lava() * (LAVA_H - 2) / menu.lavaCapacity());
            if (lava > 0) g.blit(TEXTURE, x + LAVA_X + 1, y + LAVA_Y + LAVA_H - 1 - lava, 220, 48 + LAVA_H - 2 - lava, 8, lava, 256, 256);
        }
        // progression de la reforge
        int done = menu.progress() * 56 / ForgeRules.REFORGE_TICKS;
        if (done > 0) g.blit(TEXTURE, x + 126, y + 46, 200, 100, done, 5, 256, 256);

        // le marteau : frappe pendant la reforge, s'éclaire au survol quand on peut forger
        int u, v;
        if (menu.isWorking()) {
            boolean down = (minecraft.level.getGameTime() / 6) % 2 == 0;
            u = down ? 224 : 200;
            v = down ? 24 : 0;
        } else if (canForge()) {
            u = overHammer(mouseX, mouseY) ? 224 : 200;
            v = 0;
        } else {
            u = 200;
            v = 24;
        }
        g.blit(TEXTURE, x + HAMMER_X, y + HAMMER_Y, u, v, HAMMER_SIZE, HAMMER_SIZE, 256, 256);

        // le soufflet
        boolean cracking = menu.status() == Status.CRACKING;
        int sprite = bellowsPressed > 0 ? 2 : cracking ? 3 : overBellows(mouseX, mouseY) ? 1 : 0;
        g.blit(TEXTURE, x + BELLOWS_X, y + BELLOWS_Y, sprite * BELLOWS_W, BELLOWS_V, BELLOWS_W, BELLOWS_H, 256, 256);
        if (cracking) renderCrack(g, x, y);
        renderOdds(g, x + imageWidth + 2, y);
        if (menu.hasSoulModule()) souls.render(g, x - 2 - SoulPanel.W, y, mouseX, mouseY);
    }

    /** Ça craque : le cadre rougit par à-coups, des étincelles jaillissent autour de l'objet. */
    private void renderCrack(GuiGraphics g, int x, int y) {
        long time = minecraft.level.getGameTime();
        if ((time / 3) % 2 == 0) {
            int c = 0xFFFF6E28;
            g.fill(x, y, x + imageWidth, y + 2, c);
            g.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, c);
            g.fill(x, y, x + 2, y + imageHeight, c);
            g.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, c);
        }
        RandomSource random = RandomSource.create(time);
        int[] colors = {0xFFFFE678, 0xFFFFAA3C, 0xFFFF7828};
        for (int i = 0; i < 9; i++) {
            int sx = x + 94 + random.nextInt(30), sy = y + 18 + random.nextInt(32);
            if (sx > x + 98 && sx < x + 120 && sy > y + 24 && sy < y + 46) continue; // pas sur l'objet lui-même
            g.fill(sx, sy, sx + 2, sy + 2, colors[random.nextInt(colors.length)]);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (bellowsPressed > 0) bellowsPressed--;
        layout(); // le module des âmes peut être posé ou retiré pendant que l'interface est ouverte
    }

    /** Combien de fois l'objet posé craquera à cette chaleur (0 si aucun risque). */
    private int cracks() {
        if (menu.hasSoulModule() && (menu.soulFire() || menu.reroll())) return 0; // flamme froide, ou rien à chauffer
        return Tolerance.cracks(menu.item(), menu.heat());
    }

    /** Les chances de chaque rareté, avec la chaleur, l'offrande posée et les plans lus par le joueur. */
    private void renderOdds(GuiGraphics g, int px, int py) {
        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF7A5A24);
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + PANEL_H - 1, 0xFFA5822F);
        g.fill(px + 2, py + 2, px + PANEL_W - 2, py + PANEL_H - 2, 0xFF2F2A2A);
        g.drawString(font, Component.translatable("gui.echoes_fireforge.odds"), px + 6, py + 6, BRASS, false);
        int offering = ForgeRules.offeringTier(menu.offering());
        double[] odds = RarityOdds.compute(Plans.clientReadCount(), offering, menu.heat());
        ForgingData current = menu.item().get(FFDataComponents.FORGING);
        if (menu.hasSoulModule() && menu.reroll() && current != null) {
            // relancer : la rareté est gardée
            odds = new double[Rarity.values().length];
            odds[current.rarity().ordinal()] = 100;
            offering = 0;
        } else if (menu.hasSoulModule() && menu.guarantee() && !menu.soulFire()) {
            odds = RarityOdds.guarantee(odds);
        }
        // pas de Spécial pour cette pièce (bouclier, arc, arbalète, trident, masse au feu normal) : il devient Attisé
        if (Forging.specials(menu.item(), menu.hasSoulModule() && menu.soulFire()).isEmpty()) {
            odds[Rarity.ATTISE.ordinal()] += odds[Rarity.SPECIAL.ordinal()];
            odds[Rarity.SPECIAL.ordinal()] = 0;
        }
        Rarity[] all = Rarity.values();
        for (int i = 0; i < all.length; i++) {
            int ly = py + 20 + i * 11;
            boolean possible = odds[i] > 0.0001;
            g.drawString(font, all[i].displayName(), px + 6, ly, possible ? all[i].color().getColor() : 0x55504A, false);
            String value = possible ? formatOdds(odds[i]) : "—";
            g.drawString(font, value, px + PANEL_W - 6 - font.width(value), ly, possible ? LIGHT : 0x55504A, false);
        }
        int cracks = cracks();
        if (cracks > 0) {
            g.drawString(font, Component.translatable("gui.echoes_fireforge.cracks", cracks), px + 6, py + CRACKS_LINE_Y, WARN, false);
        }
        if (offering > 0) {
            // l'Écho « Carnets complets » : quelques points de plus avec une offrande
            double bonus = offering > 0 && fr.anger.echoes.core.client.ClientLore.hasRead(fr.anger.echoes.fireforge.progress.ForgeEchoes.NOTEBOOKS_MARK) ? fr.anger.echoes.fireforge.progress.ForgeEchoes.NOTEBOOKS_ENCHANT : 0;
            String enchant = formatOdds(RarityOdds.enchantChance(offering) + bonus);
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
        int color = switch (status) {
            case READY, WORKING -> LIGHT;
            case CRACKING -> HOT;
            case AVOIDED -> SAFE;
            default -> WARN;
        };
        String key = status.name().toLowerCase();
        if (status == Status.READY && !souls.affordable()) {
            key = "no_souls";
            color = WARN;
        } else if (status == Status.READY && menu.hasSoulModule() && menu.reroll()) {
            key = "reroll";
        }
        List<FormattedCharSequence> lines = font.split(Component.translatable("status.echoes_fireforge." + key), 62);
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), 128, 60 + i * 10, color, false);
        if (menu.hasSoulModule() && menu.reroll()) return; // relancer : pas de feu
        Heat heat = menu.heat();
        g.drawString(font, heat == Heat.NONE ? Component.translatable("heat.echoes_fireforge.cold") : heat.displayName(), 128, 79, heat == Heat.NONE ? FADED : BRASS, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        if (menu.getCarried().isEmpty()) {
            if (menu.hasSoulModule() && souls.tooltip(g, mouseX, mouseY, leftPos - 2 - SoulPanel.W, topPos)) return;
            if (inside(mouseX, mouseY, WATER_GAUGE_X - 1, GAUGE_Y - 1, 10, GAUGE_H + 2)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("tooltip.echoes_fireforge.water", menu.water(), menu.waterCapacity()));
                int extra = menu.waterCapacity() - ForgeRules.BASE_WATER_CAPACITY;
                if (extra > 0) lines.add(Component.translatable("tooltip.echoes_fireforge.water.boiler", extra).withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            } else if (menu.showsLava() && inside(mouseX, mouseY, LAVA_X, LAVA_Y, LAVA_W, LAVA_H)) {
                g.renderComponentTooltip(font, List.of(
                        Component.translatable("tooltip.echoes_fireforge.lava", menu.lava(), menu.lavaCapacity()).withStyle(ChatFormatting.GOLD),
                        Component.translatable("tooltip.echoes_fireforge.lava.reforges", menu.lava() / ForgeRules.LAVA_PER_REFORGE).withStyle(ChatFormatting.GRAY),
                        Component.translatable("tooltip.echoes_fireforge.lava.rule").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            } else if (inside(mouseX, mouseY, HEAT_GAUGE_X - 1, GAUGE_Y - 1, 10, GAUGE_H + 2)) {
                Heat heat = menu.heat();
                g.renderTooltip(font, heat == Heat.NONE ? Component.translatable("heat.echoes_fireforge.cold") : heat.displayName(), mouseX, mouseY);
            } else if (overHammer(mouseX, mouseY)) {
                g.renderTooltip(font, Component.translatable("tooltip.echoes_fireforge.hammer"), mouseX, mouseY);
            } else if (overBellows(mouseX, mouseY)) {
                List<Component> lines = List.of(
                        Component.translatable("tooltip.echoes_fireforge.bellows").withStyle(ChatFormatting.GOLD),
                        Component.translatable("tooltip.echoes_fireforge.bellows.help").withStyle(ChatFormatting.GRAY),
                        Component.translatable("tooltip.echoes_fireforge.bellows.cost", ForgeRules.BELLOWS_WATER).withStyle(ChatFormatting.DARK_AQUA));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            } else if (cracks() > 0 && inside(mouseX, mouseY, imageWidth + 2, CRACKS_LINE_Y - 2, PANEL_W, 11)) {
                g.renderTooltip(font, font.split(Component.translatable("tooltip.echoes_fireforge.cracks", cracks()), 170), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.hasSoulModule() && !menu.isWorking()) {
            int id = souls.click(mouseX, mouseY, leftPos - 2 - SoulPanel.W, topPos);
            if (id >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F));
                return true;
            }
        }
        if (button == 0 && overHammer(mouseX, mouseY) && canForge()) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FireForgeMenu.BUTTON_FORGE);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_PLACE, 1.4F, 0.4F));
            return true;
        }
        if (button == 0 && overBellows(mouseX, mouseY) && bellowsPressed == 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FireForgeMenu.BUTTON_BELLOWS);
            bellowsPressed = ForgeRules.BELLOWS_COOLDOWN;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    /** Un clic sur un volet n'est pas un clic « dehors » (sinon l'objet tenu tomberait par terre). */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        boolean odds = mouseX >= guiLeft + imageWidth && mouseX < guiLeft + imageWidth + 2 + PANEL_W && mouseY >= guiTop && mouseY < guiTop + PANEL_H;
        boolean soul = menu.hasSoulModule() && mouseX >= guiLeft - 2 - SoulPanel.W && mouseX < guiLeft && mouseY >= guiTop && mouseY < guiTop + SoulPanel.H;
        return !odds && !soul && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    private boolean overBellows(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, BELLOWS_X, BELLOWS_Y, BELLOWS_W, BELLOWS_H);
    }

    private boolean overHammer(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, HAMMER_X, HAMMER_Y, HAMMER_SIZE, HAMMER_SIZE);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        double mx = mouseX - leftPos, my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
