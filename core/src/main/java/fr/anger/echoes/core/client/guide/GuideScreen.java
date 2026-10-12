package fr.anger.echoes.core.client.guide;

import com.google.gson.JsonObject;
import fr.anger.echoes.core.client.ClientLore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.fml.ModList;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static fr.anger.echoes.core.client.guide.GuideContext.inside;

/**
 * L'écran du livre guide : une double page façon parchemin.
 * Accueil (chapitres de tous les épisodes) -> Chapitre (liste des entrées) -> Entrée (pages).
 * Navigation : flèches sous le livre, molette, « « Retour » (ou Retour arrière, ou clic droit), Échap pour fermer.
 */
public class GuideScreen extends Screen implements GuideContext {

    private static final ResourceLocation BOOK = BOOK_TEXTURE;
    private static final int TEX_W = 512, TEX_H = 256;
    private static final int BOOK_W = 292, BOOK_H = 180;
    private static final int PAGE_W = PAGE_WIDTH;
    private static final int PAGE_TOP = 16;         // marge haute
    private static final int PAGE_HEIGHT = 134;     // hauteur utile : on garde de l'air au-dessus du pied de page
    private static final int LINE_H = 10;           // un peu d'interligne, plus lisible
    private static final int PARAGRAPH_GAP = 5;     // espace entre deux paragraphes (« \n\n » dans les textes)
    private static final int LEFT_X = 22, RIGHT_X = 156;

    private static final int LINK_HOVER = 0x33B4884C;
    private static final int ERASED = 0xB5A385;

    // Position mémorisée entre deux ouvertures du livre
    private static int lastCategory = -1, lastEntry = -1, lastSpread = 0;

    private final GuideBook book;
    private final List<GuideBook.Category> categories;
    private int category = -1, entry = -1, spread = 0;
    private List<Page> pages = new ArrayList<>();

    private int bx, by;
    private PageButton nextButton, prevButton;

    // Survol / clics, recalculés à chaque image
    private final List<Hotspot> hotspots = new ArrayList<>();
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private List<Component> hoveredText = null;

    private static final Map<ResourceLocation, LivingEntity> ENTITY_CACHE = new HashMap<>();

    public GuideScreen() {
        super(Component.translatable("item.echoes_core.guide_book"));
        this.book = GuideBook.load(Minecraft.getInstance().getResourceManager());
        this.categories = this.book.categories();
        ENTITY_CACHE.clear();
        this.category = lastCategory < this.categories.size() ? lastCategory : -1;
        this.entry = (this.category >= 0 && lastEntry < this.categories.get(this.category).entries().size()) ? lastEntry : -1;
        this.spread = lastSpread;
    }

    /** Ouvre directement la première entrée qui correspond (sinon, le livre s'ouvre là où on l'avait laissé). */
    void focus(Predicate<GuideBook.Entry> target) {
        for (int c = 0; c < this.categories.size(); c++) {
            List<GuideBook.Entry> entries = this.categories.get(c).entries();
            for (int e = 0; e < entries.size(); e++) {
                if (target.test(entries.get(e))) {
                    this.category = c;
                    this.entry = e;
                    this.spread = 0;
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Mise en place
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        this.bx = (this.width - BOOK_W) / 2;
        this.by = Math.max(2, (this.height - BOOK_H - 18) / 2);
        this.prevButton = this.addRenderableWidget(new PageButton(this.bx + 12, this.by + BOOK_H + 3, false, b -> turn(-1), true));
        this.nextButton = this.addRenderableWidget(new PageButton(this.bx + BOOK_W - 35, this.by + BOOK_H + 3, true, b -> turn(1), true));
        rebuild();
    }

    private void rebuild() {
        if (this.category < 0) {
            this.pages = buildHome();
        } else if (this.entry < 0) {
            this.pages = buildCategory(this.categories.get(this.category));
        } else {
            this.pages = buildEntry(this.categories.get(this.category).entries().get(this.entry));
        }
        int maxSpread = Math.max(0, (this.pages.size() - 1) / 2);
        this.spread = Math.max(0, Math.min(this.spread, maxSpread));
        updateButtons();
        remember();
    }

    private void updateButtons() {
        int maxSpread = Math.max(0, (this.pages.size() - 1) / 2);
        this.prevButton.visible = this.spread > 0;
        this.nextButton.visible = this.spread < maxSpread;
    }

    private void remember() {
        lastCategory = this.category;
        lastEntry = this.entry;
        lastSpread = this.spread;
    }

    private void turn(int delta) {
        int maxSpread = Math.max(0, (this.pages.size() - 1) / 2);
        int target = Math.max(0, Math.min(maxSpread, this.spread + delta));
        if (target != this.spread) {
            this.spread = target;
            updateButtons();
            remember();
        }
    }

    private void open(int newCategory, int newEntry) {
        this.category = newCategory;
        this.entry = newEntry;
        this.spread = 0;
        rebuild();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    private void goBack() {
        if (this.entry >= 0) open(this.category, -1);
        else if (this.category >= 0) open(-1, -1);
    }

    // ------------------------------------------------------------------
    // Construction des pages
    // ------------------------------------------------------------------

    private static boolean isVisible(GuideBook.Category cat) {
        // chapitre secret : caché tant qu'aucune entrée de lore de son mod n'a été lue
        return !cat.hidden() || ClientLore.hasAny(cat.id().getNamespace());
    }

    private List<Page> buildHome() {
        List<Page> result = new ArrayList<>();
        Page left = new Page();
        left.add(new BigTitle(Component.translatable("guide.echoes_core.title")));
        left.add(new CenteredLine(Component.translatable("guide.echoes_core.subtitle"), FADED));
        left.add(new Spacer(6));
        result.add(left);
        for (String intro : this.book.introKeys()) addText(result, result.get(result.size() - 1), Component.translatable(intro));

        // Les chapitres, groupés par épisode (avec le nom de l'épisode s'il y en a plusieurs)
        List<Integer> visible = new ArrayList<>();
        for (int i = 0; i < this.categories.size(); i++) if (isVisible(this.categories.get(i))) visible.add(i);
        boolean severalEpisodes = visible.stream().map(i -> this.categories.get(i).id().getNamespace()).distinct().count() > 1;

        List<GuideElement> links = new ArrayList<>();
        String currentNamespace = null;
        for (int index : visible) {
            GuideBook.Category cat = this.categories.get(index);
            String namespace = cat.id().getNamespace();
            if (severalEpisodes && !namespace.equals(currentNamespace)) {
                if (currentNamespace != null) links.add(new Spacer(4));
                links.add(new SubHeading(Component.literal(modName(namespace))));
                currentNamespace = namespace;
            }
            links.add(new Link(cat.icon(), Component.translatable(cat.titleKey()), () -> open(index, -1)));
        }
        // Les chapitres commencent sur une nouvelle page (celle de droite si l'intro tient sur une page)
        Page chapters = new Page();
        chapters.add(new Heading(Component.translatable("guide.echoes_core.chapters")));
        result.add(chapters);
        flow(result, links);
        return result;
    }

    private static String modName(String namespace) {
        return ModList.get().getModContainerById(namespace).map(c -> c.getModInfo().getDisplayName()).orElse(namespace);
    }

    private List<Page> buildCategory(GuideBook.Category cat) {
        List<Page> result = new ArrayList<>();
        Page left = new Page();
        left.add(new Heading(Component.translatable(cat.titleKey())));
        result.add(left);
        if (!cat.descKey().isEmpty()) addText(result, left, Component.translatable(cat.descKey()));

        List<GuideElement> links = new ArrayList<>();
        for (int i = 0; i < cat.entries().size(); i++) {
            GuideBook.Entry e = cat.entries().get(i);
            final int index = i;
            Component title = Component.translatable(e.titleKey());
            Component label = e.label().isEmpty() ? title : Component.literal(e.label() + " — ").append(title);
            if (e.lore() != null && !ClientLore.hasRead(e.lore())) {
                links.add(new ErasedLink(e.label())); // page « effacée » : pas encore trouvée
            } else {
                links.add(new Link(e.icon(), label, () -> open(this.category, index)));
            }
        }
        Page list = new Page();
        list.add(new Heading(Component.translatable("guide.echoes_core.entries")));
        result.add(list);
        flow(result, links);
        return result;
    }

    private void flow(List<Page> result, List<GuideElement> elements) {
        Page current = result.get(result.size() - 1);
        for (GuideElement element : elements) {
            if (current.used + element.height() > PAGE_HEIGHT) {
                current = new Page();
                result.add(current);
            }
            current.add(element);
        }
    }

    private List<Page> buildEntry(GuideBook.Entry e) {
        List<Page> result = new ArrayList<>();
        boolean first = true;
        for (JsonObject json : e.pages()) {
            Page page = new Page();
            result.add(page);
            if (first) {
                page.add(new Heading(Component.translatable(e.titleKey())));
                first = false;
            }
            if (json.has("title")) page.add(new SubHeading(Component.translatable(GsonHelper.getAsString(json, "title"))));
            GuideElement header = header(json, e.id().getNamespace());
            if (header != null) {
                if (page.used + header.height() > PAGE_HEIGHT) {
                    page = new Page();
                    result.add(page);
                }
                page.add(header);
            }
            if (json.has("text")) addText(result, page, Component.translatable(GsonHelper.getAsString(json, "text")));
        }
        if (result.isEmpty()) result.add(new Page());
        return result;
    }

    /** Remplit la page avec le texte, et crée des pages de suite si ça déborde. Les paragraphes (« \n\n ») sont espacés. */
    private void addText(List<Page> result, Page page, Component text) {
        String[] paragraphs = text.getString().split("\n\\s*\n");
        Page current = page;
        boolean firstParagraph = true;
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) continue;
            Component part = Guide.filterText(Component.literal(paragraph.strip()));
            List<FormattedCharSequence> lines = this.font.split(part, PAGE_W);
            // un écart avant le paragraphe, sauf en haut d'une page
            if (!firstParagraph && current.used > 0 && current.used + PARAGRAPH_GAP + LINE_H <= PAGE_HEIGHT) current.add(new Spacer(PARAGRAPH_GAP));
            firstParagraph = false;
            List<FormattedCharSequence> chunk = new ArrayList<>();
            for (FormattedCharSequence line : lines) {
                if (current.used + (chunk.size() + 1) * LINE_H > PAGE_HEIGHT) {
                    if (!chunk.isEmpty()) current.add(new TextBlock(chunk));
                    chunk = new ArrayList<>();
                    current = new Page();
                    result.add(current);
                }
                chunk.add(line);
            }
            if (!chunk.isEmpty()) current.add(new TextBlock(chunk));
        }
    }

    /** L'élément « illustré » d'une page, selon son type. */
    private GuideElement header(JsonObject json, String namespace) {
        String type = GsonHelper.getAsString(json, "type", "text");
        return switch (type) {
            case "text" -> null;
            case "spotlight" -> {
                List<ItemStack> stacks = new ArrayList<>();
                if (json.has("items")) {
                    json.getAsJsonArray("items").forEach(el -> stacks.add(GuideBook.stack(el.getAsString())));
                } else {
                    stacks.add(GuideBook.stack(GsonHelper.getAsString(json, "item")));
                }
                yield new Spotlight(stacks, json.has("name") ? Component.translatable(GsonHelper.getAsString(json, "name")) : null);
            }
            case "crafting" -> new CraftingRecipeEl(ResourceLocation.parse(GsonHelper.getAsString(json, "recipe")));
            case "smelting" -> new SmeltingRecipeEl(ResourceLocation.parse(GsonHelper.getAsString(json, "recipe")));
            case "entity" -> new EntityEl(ResourceLocation.parse(GsonHelper.getAsString(json, "entity")),
                    GsonHelper.getAsInt(json, "height", 72), GsonHelper.getAsFloat(json, "scale", 0F));
            case "enchantment" -> new EnchantEl(ResourceLocation.parse(GsonHelper.getAsString(json, "enchantment")));
            case "image" -> new ImageEl(ResourceLocation.parse(GsonHelper.getAsString(json, "texture")),
                    GsonHelper.getAsInt(json, "width"), GsonHelper.getAsInt(json, "height"),
                    GsonHelper.getAsInt(json, "texture_width", GsonHelper.getAsInt(json, "width")),
                    GsonHelper.getAsInt(json, "texture_height", GsonHelper.getAsInt(json, "height")));
            default -> {
                // type ajouté par un épisode : « chant_table » = <mod de l'entrée>:chant_table
                ResourceLocation id = type.indexOf(':') >= 0 ? ResourceLocation.tryParse(type) : ResourceLocation.tryBuild(namespace, type);
                Guide.PageFactory factory = id == null ? null : Guide.pageType(id);
                yield factory == null ? new UnknownEl(type) : factory.create(json, this);
            }
        };
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick); // fond + boutons
        this.hotspots.clear();
        this.hoveredStack = ItemStack.EMPTY;
        this.hoveredText = null;

        g.blit(BOOK, this.bx, this.by, 0, 0, BOOK_W, BOOK_H, TEX_W, TEX_H);

        int leftIndex = this.spread * 2;
        if (leftIndex < this.pages.size()) drawPage(g, this.pages.get(leftIndex), this.bx + LEFT_X, mouseX, mouseY);
        if (leftIndex + 1 < this.pages.size()) drawPage(g, this.pages.get(leftIndex + 1), this.bx + RIGHT_X, mouseX, mouseY);

        // Pied de page : retour + numéro
        int footerY = this.by + BOOK_H - 16;
        if (this.category >= 0) {
            Component back = Component.translatable("guide.echoes_core.back");
            int w = this.font.width(back);
            boolean hover = inside(mouseX, mouseY, this.bx + LEFT_X, footerY - 1, w, 10);
            g.drawString(this.font, back, this.bx + LEFT_X, footerY, hover ? TITLE : FADED, false);
            this.hotspots.add(new Hotspot(this.bx + LEFT_X, footerY - 1, w, 10, this::goBack));
        }
        if (this.pages.size() > 2) {
            String number = (this.spread + 1) + " / " + ((this.pages.size() + 1) / 2);
            g.drawString(this.font, number, this.bx + RIGHT_X + PAGE_W - this.font.width(number), footerY, FADED, false);
        }

        if (!this.hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, this.hoveredStack, mouseX, mouseY);
        } else if (this.hoveredText != null) {
            g.renderComponentTooltip(this.font, this.hoveredText, mouseX, mouseY);
        }
    }

    private void drawPage(GuiGraphics g, Page page, int x, int mouseX, int mouseY) {
        int y = this.by + PAGE_TOP;
        for (GuideElement element : page.elements) {
            element.render(g, x, y, mouseX, mouseY);
            y += element.height();
        }
    }

    // ------------------------------------------------------------------
    // GuideContext : outils pour les éléments de page (y compris ceux des épisodes)
    // ------------------------------------------------------------------

    @Override
    public Font font() {
        return this.font;
    }

    @Override
    public void slot(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        g.blit(BOOK, x - 1, y - 1, 0, 184, 18, 18, TEX_W, TEX_H);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x, y);
            g.renderItemDecorations(this.font, stack, x, y);
            if (inside(mouseX, mouseY, x, y, 16, 16)) this.hoveredStack = stack;
        }
    }

    @Override
    public void blitBook(GuiGraphics g, int x, int y, int u, int v, int width, int height) {
        g.blit(BOOK, x, y, u, v, width, height, TEX_W, TEX_H);
    }

    @Override
    public void hoverStack(ItemStack stack) {
        this.hoveredStack = stack;
    }

    @Override
    public void hoverText(List<Component> lines) {
        this.hoveredText = lines;
    }

    @Override
    public boolean isHoveringStack() {
        return !this.hoveredStack.isEmpty();
    }

    @Override
    public Optional<Recipe<?>> recipe(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Optional.empty();
        return mc.level.getRecipeManager().byKey(id).map(h -> (Recipe<?>) h.value());
    }

    @Override
    public ItemStack result(Recipe<?> recipe) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? ItemStack.EMPTY : recipe.getResultItem(mc.level.registryAccess());
    }

    @Override
    public void missing(GuiGraphics g, int x, int y) {
        g.drawString(this.font, Component.translatable("guide.echoes_core.missing_recipe"), x, y + 4, 0xAA3322, false);
    }

    // ------------------------------------------------------------------
    // Entrées clavier / souris
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Hotspot h : this.hotspots) {
                if (inside(mouseX, mouseY, h.x, h.y, h.w, h.h)) {
                    h.action.run();
                    return true;
                }
            }
        } else if (button == 1 && this.category >= 0) {
            goBack(); // clic droit = retour
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            turn(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            goBack();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        for (LivingEntity living : ENTITY_CACHE.values()) living.tickCount++; // anime les créatures affichées
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------
    // Éléments de page
    // ------------------------------------------------------------------

    private record Hotspot(int x, int y, int w, int h, Runnable action) {}

    private static class Page {
        final List<GuideElement> elements = new ArrayList<>();
        int used = 0;

        void add(GuideElement element) {
            this.elements.add(element);
            this.used += element.height();
        }
    }

    private record Spacer(int height) implements GuideElement {
        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {}
    }

    private class BigTitle implements GuideElement {
        private final Component text;

        BigTitle(Component text) { this.text = text; }

        public int height() { return 20; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            List<FormattedCharSequence> lines = font.split(text, (int) (PAGE_W / 1.3F));
            g.pose().pushPose();
            g.pose().translate(x + PAGE_W / 2F, y + 2, 0);
            g.pose().scale(1.3F, 1.3F, 1F);
            FormattedCharSequence line = lines.get(0);
            g.drawString(font, line, -font.width(line) / 2, 0, TITLE, false);
            g.pose().popPose();
        }
    }

    private class CenteredLine implements GuideElement {
        private final int color;
        private final List<FormattedCharSequence> lines;

        CenteredLine(Component text, int color) { this.color = color; this.lines = font.split(text, PAGE_W); }

        public int height() { return lines.size() * 10 + 1; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            for (int i = 0; i < lines.size(); i++)
                g.drawString(font, lines.get(i), x + (PAGE_W - font.width(lines.get(i))) / 2, y + i * 10, color, false);
        }
    }

    private class Heading implements GuideElement {
        private final List<FormattedCharSequence> lines;

        Heading(Component text) {
            this.lines = font.split(text.copy().withStyle(ChatFormatting.BOLD), PAGE_W); // un titre trop long passe sur plusieurs lignes
        }

        public int height() { return lines.size() * 10 + 6; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            for (int i = 0; i < lines.size(); i++)
                g.drawString(font, lines.get(i), x + (PAGE_W - font.width(lines.get(i))) / 2, y + i * 10, TITLE, false);
            int ly = y + lines.size() * 10 + 1;
            g.fill(x + 10, ly, x + PAGE_W - 10, ly + 1, 0x66805A36);
        }
    }

    private class SubHeading implements GuideElement {
        private final List<FormattedCharSequence> lines;

        SubHeading(Component text) {
            this.lines = font.split(text.copy().withStyle(ChatFormatting.UNDERLINE), PAGE_W);
        }

        public int height() { return lines.size() * 10 + 2; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x, y + i * 10, TITLE, false);
        }
    }

    private class TextBlock implements GuideElement {
        private final List<FormattedCharSequence> lines;

        TextBlock(List<FormattedCharSequence> lines) { this.lines = lines; }

        public int height() { return lines.size() * LINE_H; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x, y + i * LINE_H, TEXT, false);
        }
    }

    private class Link implements GuideElement {
        private final ItemStack icon;
        private final Runnable action;
        private final List<FormattedCharSequence> lines;

        Link(ItemStack icon, Component label, Runnable action) {
            this.icon = icon; this.action = action;
            List<FormattedCharSequence> l = font.split(label, PAGE_W - 20);
            this.lines = l.size() > 2 ? l.subList(0, 2) : l;          // un nom long tient sur deux lignes
        }

        public int height() { return lines.size() > 1 ? 22 : 18; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            int h = this.height() - 1;
            boolean hover = inside(mouseX, mouseY, x, y, PAGE_W, h);
            if (hover) g.fill(x - 2, y - 1, x + PAGE_W + 2, y + h, LINK_HOVER);
            g.renderItem(icon, x, y + (h - 17) / 2);
            int ty = lines.size() > 1 ? y + 1 : y + 4;
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 20, ty + i * 10, hover ? TITLE : TEXT, false);
            hotspots.add(new Hotspot(x - 2, y - 1, PAGE_W + 4, h + 1, action));
        }
    }

    /** Une page pas encore trouvée (son entrée de lore n'a pas été lue). */
    private class ErasedLink implements GuideElement {
        private final FormattedCharSequence line;

        ErasedLink(String label) {
            Component erased = Component.translatable("guide.echoes_core.erased");
            Component text = (label.isEmpty() ? erased.copy() : Component.literal(label + " — ").append(erased)).withStyle(ChatFormatting.ITALIC);
            this.line = font.split(text, PAGE_W - 20).get(0);
        }

        public int height() { return 18; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            g.drawString(font, line, x + 20, y + 4, ERASED, false);
        }
    }

    /** Un ou plusieurs objets en grand, avec leur nom. */
    private class Spotlight implements GuideElement {
        private final List<ItemStack> stacks;
        private final List<FormattedCharSequence> nameLines;

        Spotlight(List<ItemStack> stacks, Component name) {
            this.stacks = stacks;
            Component shown = name != null ? name : (stacks.size() == 1 ? stacks.get(0).getHoverName() : null);
            this.nameLines = shown != null ? font.split(shown.copy().withStyle(ChatFormatting.ITALIC), PAGE_W) : List.of();
        }

        /** Taille d'une case : 32 px d'habitude, plus petite s'il y a trop d'objets pour la largeur de la page. */
        private int cell() {
            int n = Math.max(1, stacks.size());
            return Math.max(16, Math.min(32, (PAGE_W + 4) / n - 4));
        }

        /** Objets par rangée : tous sur une ligne si possible, sinon plusieurs rangées de cases de 16 px. */
        private int perRow() {
            return Math.max(1, Math.min(stacks.size(), (PAGE_W + 4) / (cell() + 4)));
        }

        private int rows() {
            return Math.max(1, (stacks.size() + perRow() - 1) / perRow());
        }

        public int height() { return rows() * (cell() + 4) + nameLines.size() * 10; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            int c = cell(), perRow = perRow();
            for (int i = 0; i < stacks.size(); i++) {
                int row = i / perRow, col = i % perRow;
                int inRow = Math.min(perRow, stacks.size() - row * perRow);
                int sx = x + (PAGE_W - (inRow * (c + 4) - 4)) / 2 + col * (c + 4);
                int sy = y + row * (c + 4);
                ItemStack stack = stacks.get(i);
                if (c == 32) g.blit(BOOK, sx - 1, sy - 1, 60, 184, 34, 34, TEX_W, TEX_H);
                else g.blit(BOOK, sx - 1, sy - 1, c + 2, c + 2, 60, 184, 34, 34, TEX_W, TEX_H);
                g.pose().pushPose();
                g.pose().translate(sx + 1, sy + 1, 0);
                g.pose().scale(c / 16F, c / 16F, 1F);
                g.renderItem(stack, 0, 0);
                g.pose().popPose();
                if (inside(mouseX, mouseY, sx, sy, c, c)) hoveredStack = stack;
            }
            int ny = y + rows() * (c + 4);
            for (int i = 0; i < nameLines.size(); i++)
                g.drawString(font, nameLines.get(i), x + (PAGE_W - font.width(nameLines.get(i))) / 2, ny + i * 10, TITLE, false);
        }
    }

    private class CraftingRecipeEl implements GuideElement {
        private final ResourceLocation id;

        CraftingRecipeEl(ResourceLocation id) { this.id = id; }

        public int height() { return 60; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            Optional<Recipe<?>> opt = recipe(id);
            if (opt.isEmpty()) { missing(g, x, y); return; }
            Recipe<?> r = opt.get();
            int gx = x + 8, gy = y + 2;
            int width = 3;
            if (r instanceof ShapedRecipe shaped) width = shaped.getWidth();
            List<Ingredient> ingredients = r.getIngredients();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int index = row * width + col;
                    ItemStack stack = (col < width && index < ingredients.size()) ? GuideContext.cycle(ingredients.get(index)) : ItemStack.EMPTY;
                    slot(g, stack, gx + col * 18, gy + row * 18, mouseX, mouseY);
                }
            }
            g.blit(BOOK, gx + 58, gy + 19, 20, 184, 22, 15, TEX_W, TEX_H);
            slot(g, result(r), gx + 86, gy + 18, mouseX, mouseY);
        }
    }

    private class SmeltingRecipeEl implements GuideElement {
        private final ResourceLocation id;

        SmeltingRecipeEl(ResourceLocation id) { this.id = id; }

        public int height() { return 30; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            Optional<Recipe<?>> opt = recipe(id);
            if (opt.isEmpty() || !(opt.get() instanceof AbstractCookingRecipe cooking)) { missing(g, x, y); return; }
            int sx = x + 22, sy = y + 4;
            slot(g, GuideContext.cycle(cooking.getIngredients().get(0)), sx, sy, mouseX, mouseY);
            g.blit(BOOK, sx + 22, sy + 1, 44, 184, 14, 14, TEX_W, TEX_H);
            g.blit(BOOK, sx + 38, sy + 1, 20, 184, 22, 15, TEX_W, TEX_H);
            slot(g, result(cooking), sx + 66, sy, mouseX, mouseY);
        }
    }

    /** Une créature, qui tourne lentement sur elle-même. */
    private class EntityEl implements GuideElement {
        private final ResourceLocation id;
        private final int height;
        private final float scale;

        EntityEl(ResourceLocation id, int height, float scale) { this.id = id; this.height = Math.min(72, height); this.scale = scale; }

        public int height() { return height + 4; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            LivingEntity living = entity();
            g.blit(BOOK, x, y, 100, 184, PAGE_W, height, TEX_W, TEX_H); // cadre
            if (living == null) return;
            float size = Math.max(living.getBbHeight(), living.getBbWidth());
            float s = scale > 0 ? scale : Math.min(34F, (height - 12) / size);
            float yaw = (System.currentTimeMillis() / 30L) % 360L;
            float bodyRot = living.yBodyRot, yRot = living.getYRot(), xRot = living.getXRot();
            float headO = living.yHeadRotO, head = living.yHeadRot;
            living.yBodyRot = yaw;
            living.setYRot(yaw);
            living.setXRot(0);
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
            g.enableScissor(x, y, x + PAGE_W, y + height);
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateX(-0.25F);
            InventoryScreen.renderEntityInInventory(g, x + PAGE_W / 2F, y + height - 8, s,
                    new Vector3f(0, 0, 0), pose, null, living);
            g.disableScissor();
            living.yBodyRot = bodyRot;
            living.setYRot(yRot);
            living.setXRot(xRot);
            living.yHeadRotO = headO;
            living.yHeadRot = head;
            if (inside(mouseX, mouseY, x, y, PAGE_W, height)) hoveredText = List.of(living.getType().getDescription());
        }

        private LivingEntity entity() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
            return ENTITY_CACHE.computeIfAbsent(id, key -> {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
                Entity created = type.create(mc.level);
                return created instanceof LivingEntity l ? l : null;
            });
        }
    }

    /** Un livre enchanté (niveau max) avec le nom de l'enchantement et ses niveaux. */
    private class EnchantEl implements GuideElement {
        private final Holder<Enchantment> holder;
        private final List<FormattedCharSequence> label;

        EnchantEl(ResourceLocation id) {
            Minecraft mc = Minecraft.getInstance();
            this.holder = mc.level == null ? null : mc.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
            if (this.holder == null) {
                this.label = List.of();
            } else {
                int max = this.holder.value().getMaxLevel();
                Component description = this.holder.value().description();
                Component text = (max > 1 ? Component.translatable("guide.echoes_core.enchant_levels", description, max) : description.copy())
                        .copy().withStyle(ChatFormatting.ITALIC);
                this.label = font.split(text, PAGE_W);                 // un nom long passe à la ligne
            }
        }

        public int height() { return 36 + Math.max(1, label.size()) * 10; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            if (holder == null) { missing(g, x, y); return; }
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, holder.value().getMaxLevel()));
            int sx = x + (PAGE_W - 32) / 2;
            g.blit(BOOK, sx - 1, y - 1, 60, 184, 34, 34, TEX_W, TEX_H);
            g.pose().pushPose();
            g.pose().translate(sx + 1, y + 1, 0);
            g.pose().scale(2F, 2F, 1F);
            g.renderItem(book, 0, 0);
            g.pose().popPose();
            if (inside(mouseX, mouseY, sx, y, 32, 32)) hoveredStack = book;
            for (int i = 0; i < label.size(); i++)
                g.drawString(font, label.get(i), x + (PAGE_W - font.width(label.get(i))) / 2, y + 36 + i * 10, TITLE, false);
        }
    }

    private class ImageEl implements GuideElement {
        private final ResourceLocation texture;
        private final int width, height, texWidth, texHeight;

        ImageEl(ResourceLocation texture, int width, int height, int texWidth, int texHeight) {
            this.texture = texture;
            this.width = Math.min(width, PAGE_W);
            this.height = height;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
        }

        public int height() { return height + 4; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            g.blit(texture, x + (PAGE_W - width) / 2, y, width, height, 0, 0, texWidth, texHeight, texWidth, texHeight);
        }
    }

    /** Type de page inconnu (épisode absent ou faute de frappe) : on le signale au lieu de planter. */
    private class UnknownEl implements GuideElement {
        private final String type;

        UnknownEl(String type) { this.type = type; }

        public int height() { return 12; }

        public void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            g.drawString(font, font.plainSubstrByWidth("? " + type, PAGE_W), x, y + 2, 0xAA3322, false);
        }
    }
}
