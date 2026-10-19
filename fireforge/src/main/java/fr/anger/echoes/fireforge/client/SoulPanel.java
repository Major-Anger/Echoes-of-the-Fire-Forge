package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.core.client.ClientSouls;
import fr.anger.echoes.fireforge.forge.FireForgeMenu;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import fr.anger.echoes.fireforge.forging.Forging;
import fr.anger.echoes.fireforge.forging.ForgingData;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Le panneau des âmes (étape 5b), à gauche de la forge quand le module des âmes est branché :
 * le choix du feu, les options du feu normal (verrouiller, garantir, relancer) et leur prix.
 */
final class SoulPanel {

    static final int W = 100, H = 190;
    private static final int BRASS = 0xE0B85A, LIGHT = 0xD8CFC0, FADED = 0x7A7268, CYAN = 0x6EE6E6, RED = 0xE06050, ORANGE = 0xFFAA3C;
    private static final int FIRE_Y = 28, FIRE_SIZE = 20, NORMAL_X = 6, SOUL_X = 30;
    private static final int OPTIONS_Y = 56, ROW = 12, CONTENT_Y = 97, LINE = 11, COST_Y = 177;
    private static final int MAX_LOCK_LINES = 4, BONUS_W = W - 22;

    private final FireForgeMenu menu;
    private final Font font;
    private final ResourceLocation texture;

    SoulPanel(FireForgeMenu menu, Font font, ResourceLocation texture) {
        this.menu = menu;
        this.font = font;
        this.texture = texture;
    }

    /** Assez d'âmes pour la prochaine reforge (toujours en créatif, comme Souls.spend). */
    boolean affordable() {
        Minecraft mc = Minecraft.getInstance();
        return menu.soulCost() <= 0 || (mc.player != null && mc.player.isCreative()) || ClientSouls.get() >= menu.soulCost();
    }

    private List<ForgingData.StatRoll> bonuses() {
        return Forging.bonuses(menu.item());
    }

    void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        g.fill(x, y, x + W, y + H, 0xFF7A5A24);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, 0xFFA5822F);
        g.fill(x + 2, y + 2, x + W - 2, y + H - 2, 0xFF2F2A2A);

        g.drawString(font, Component.translatable("gui.echoes_fireforge.souls"), x + 6, y + 6, BRASS, false);
        String souls = ClientSouls.get() + " / " + ClientSouls.max();
        g.drawString(font, souls, x + W - 6 - font.width(souls), y + 6, CYAN, false);

        // le feu
        boolean soul = menu.soulFire();
        g.drawString(font, Component.translatable("gui.echoes_fireforge.fire"), x + 6, y + 18, FADED, false);
        fireButton(g, x + NORMAL_X, y + FIRE_Y, false, !soul, inside(mouseX, mouseY, x + NORMAL_X, y + FIRE_Y, FIRE_SIZE, FIRE_SIZE));
        fireButton(g, x + SOUL_X, y + FIRE_Y, true, soul, inside(mouseX, mouseY, x + SOUL_X, y + FIRE_Y, FIRE_SIZE, FIRE_SIZE));
        g.drawString(font, Component.translatable(soul ? "gui.echoes_fireforge.fire.soul" : "gui.echoes_fireforge.fire.normal"),
                x + 54, y + 34, soul ? CYAN : ORANGE, false);

        // les options du feu normal (grisées au feu des âmes)
        option(g, x, y + OPTIONS_Y, "lock", ForgeRules.LOCK_COST, menu.lockIndex() >= 0, !soul);
        option(g, x, y + OPTIONS_Y + ROW, "guarantee", ForgeRules.GUARANTEE_COST, menu.guarantee() && !menu.reroll(), !soul && !menu.reroll());
        option(g, x, y + OPTIONS_Y + 2 * ROW, "reroll", ForgeRules.REROLL_COST, menu.reroll(), !soul);
        g.fill(x + 6, y + CONTENT_Y - 5, x + W - 6, y + CONTENT_Y - 4, 0xFF46403C);

        // ce qui se passe, selon le feu et les options
        int cy = y + CONTENT_Y;
        if (soul) {
            cy = text(g, x, cy, Component.translatable("gui.echoes_fireforge.soul.cold"), CYAN);
            cy = text(g, x, cy, Component.translatable("gui.echoes_fireforge.soul.never"), LIGHT);
            cy += 4;
            cy = text(g, x, cy, Component.translatable("gui.echoes_fireforge.soul.all"), CYAN);
            text(g, x, cy, Component.translatable("gui.echoes_fireforge.soul.noopt"), LIGHT);
        } else {
            List<ForgingData.StatRoll> bonuses = bonuses();
            if (!bonuses.isEmpty()) {
                cy = text(g, x, cy, Component.translatable("gui.echoes_fireforge.lock.title"), FADED);
                for (int i = 0; i < Math.min(MAX_LOCK_LINES, bonuses.size()); i++) {
                    boolean locked = i == menu.lockIndex();
                    if (locked) g.fill(x + 4, cy - 2, x + W - 4, cy + 9, 0xFF1E3C3E);
                    else if (inside(mouseX, mouseY, x + 4, cy - 2, W - 8, LINE)) g.fill(x + 4, cy - 2, x + W - 4, cy + 9, 0xFF3A3430);
                    if (locked) g.blit(texture, x + 6, cy, 134, 192, 5, 6, 256, 256);
                    String line = bonusLine(bonuses.get(i));
                    if (font.width(line) > BONUS_W) line = font.plainSubstrByWidth(line, BONUS_W - font.width("…")) + "…"; // en entier dans l'infobulle
                    g.drawString(font, line, x + 14, cy, 0x55FF55, false);
                    cy += LINE;
                }
                cy += 3;
            }
            Component help = menu.reroll() ? Component.translatable("gui.echoes_fireforge.reroll.help", currentRarity())
                    : menu.guarantee() ? Component.translatable("gui.echoes_fireforge.guarantee.help") : null;
            if (help != null) {
                // l'aide en entier ou pas du tout (elle reste dans l'infobulle de l'option)
                List<FormattedCharSequence> lines = font.split(help, W - 12);
                if (cy + lines.size() * 10 <= y + COST_Y - 8) {
                    for (FormattedCharSequence l : lines) {
                        g.drawString(font, l, x + 6, cy, LIGHT, false);
                        cy += 10;
                    }
                }
            }
        }

        // le prix
        g.fill(x + 6, y + COST_Y - 6, x + W - 6, y + COST_Y - 5, 0xFF46403C);
        int cost = menu.soulCost();
        g.drawString(font, Component.translatable("gui.echoes_fireforge.cost", cost), x + 6, y + COST_Y, affordable() ? CYAN : RED, false);
    }

    private static String bonusLine(ForgingData.StatRoll roll) {
        return ForgingTooltip.percent(roll.percent()) + " " + roll.stat().displayName().getString();
    }

    /** La ligne de bonus visée (sous le titre « verrouiller »), ou -1. */
    private int bonusAt(double mouseX, double mouseY, int x, int y) {
        int lines = Math.min(MAX_LOCK_LINES, bonuses().size());
        for (int i = 0; i < lines; i++) {
            // les lignes des bonus suivent le titre (10 px), comme dans render
            if (inside(mouseX, mouseY, x + 4, y + CONTENT_Y + 10 + LINE * i - 2, W - 8, LINE)) return i;
        }
        return -1;
    }

    private Component currentRarity() {
        ForgingData data = menu.item().get(FFDataComponents.FORGING);
        return data != null ? data.rarity().displayName() : Component.literal("—");
    }

    private void fireButton(GuiGraphics g, int x, int y, boolean soul, boolean selected, boolean hover) {
        g.fill(x, y, x + FIRE_SIZE, y + FIRE_SIZE, selected ? 0xFFE0B85A : hover ? 0xFF6A6058 : 0xFF46403C);
        g.fill(x + 1, y + 1, x + FIRE_SIZE - 1, y + FIRE_SIZE - 1, 0xFF161212);
        g.blit(texture, x + 5, y + 5, soul ? 124 : 114, 192, 9, 9, 256, 256);
    }

    private void option(GuiGraphics g, int x, int y, String key, int cost, boolean checked, boolean enabled) {
        g.fill(x + 6, y, x + 13, y + 7, enabled ? 0xFFA5822F : 0xFF46403C);
        g.fill(x + 7, y + 1, x + 12, y + 6, 0xFF161212);
        if (checked) g.fill(x + 8, y + 2, x + 11, y + 5, enabled ? 0xFF6EE6E6 : 0xFF7A7268);
        g.drawString(font, Component.translatable("gui.echoes_fireforge.option." + key), x + 16, y, enabled ? LIGHT : FADED, false);
        String price = String.valueOf(cost);
        g.drawString(font, price, x + W - 6 - font.width(price), y, enabled ? CYAN : FADED, false);
    }

    private int text(GuiGraphics g, int x, int cy, Component text, int color) {
        g.drawString(font, text, x + 6, cy, color, false);
        return cy + 10;
    }

    /** Le bouton du menu visé par ce clic, ou -1. */
    int click(double mouseX, double mouseY, int x, int y) {
        if (inside(mouseX, mouseY, x + NORMAL_X, y + FIRE_Y, FIRE_SIZE, FIRE_SIZE)) return FireForgeMenu.BUTTON_FIRE_NORMAL;
        if (inside(mouseX, mouseY, x + SOUL_X, y + FIRE_Y, FIRE_SIZE, FIRE_SIZE)) return FireForgeMenu.BUTTON_FIRE_SOUL;
        if (menu.soulFire()) return -1;
        if (inside(mouseX, mouseY, x + 4, y + OPTIONS_Y - 2, W - 8, ROW)) return FireForgeMenu.BUTTON_LOCK;
        if (inside(mouseX, mouseY, x + 4, y + OPTIONS_Y + ROW - 2, W - 8, ROW)) return FireForgeMenu.BUTTON_GUARANTEE;
        if (inside(mouseX, mouseY, x + 4, y + OPTIONS_Y + 2 * ROW - 2, W - 8, ROW)) return FireForgeMenu.BUTTON_REROLL;
        int bonus = bonusAt(mouseX, mouseY, x, y);
        return bonus >= 0 ? FireForgeMenu.BUTTON_LOCK_STAT + bonus : -1;
    }

    /** Les infobulles du panneau. @return true si une infobulle a été dessinée */
    boolean tooltip(GuiGraphics g, int mouseX, int mouseY, int x, int y) {
        if (inside(mouseX, mouseY, x + NORMAL_X, y + FIRE_Y, FIRE_SIZE, FIRE_SIZE)) {
            g.renderTooltip(font, Component.translatable("tooltip.echoes_fireforge.fire.normal").withStyle(ChatFormatting.GOLD), mouseX, mouseY);
            return true;
        }
        if (inside(mouseX, mouseY, x + SOUL_X, y + FIRE_Y, FIRE_SIZE, FIRE_SIZE)) {
            g.renderTooltip(font, Component.translatable("tooltip.echoes_fireforge.fire.soul", ForgeRules.SOUL_FIRE_COST).withStyle(ChatFormatting.AQUA), mouseX, mouseY);
            return true;
        }
        if (!menu.soulFire() && inside(mouseX, mouseY, x + 4, y + OPTIONS_Y + ROW - 2, W - 8, ROW)) {
            g.renderTooltip(font, font.split(Component.translatable("gui.echoes_fireforge.guarantee.help"), 160), mouseX, mouseY);
            return true;
        }
        if (!menu.soulFire() && inside(mouseX, mouseY, x + 4, y + OPTIONS_Y + 2 * ROW - 2, W - 8, ROW)) {
            g.renderTooltip(font, font.split(Component.translatable("gui.echoes_fireforge.reroll.help", currentRarity()), 160), mouseX, mouseY);
            return true;
        }
        int bonus = menu.soulFire() ? -1 : bonusAt(mouseX, mouseY, x, y);
        if (bonus >= 0 && font.width(bonusLine(bonuses().get(bonus))) > BONUS_W) {
            // une ligne coupée (« +5.0% Knockb… ») : la ligne entière
            g.renderTooltip(font, Component.literal(bonusLine(bonuses().get(bonus))).withStyle(ChatFormatting.GREEN), mouseX, mouseY);
            return true;
        }
        return false;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
