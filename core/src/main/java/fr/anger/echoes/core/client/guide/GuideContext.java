package fr.anger.echoes.core.client.guide;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Optional;

/** Ce que le livre met à disposition des éléments de page (y compris ceux des épisodes). */
public interface GuideContext {

    /** Largeur utile d'une page. */
    int PAGE_WIDTH = 114;

    // Couleurs du parchemin
    int TEXT = 0x3B2A1A;
    int TITLE = 0x1F4F55;
    int FADED = 0x8A7456;

    /** Texture du livre (512 × 256) : cases, flèches et cadres à partir de v = 184. */
    ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath("echoes_core", "textures/gui/guide_book.png");

    Font font();

    /** Dessine une case d'inventaire avec un objet (et son infobulle au survol). */
    void slot(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY);

    /** Dessine un morceau de la texture du livre (flèche de recette, flamme, cadre...). */
    void blitBook(GuiGraphics graphics, int x, int y, int u, int v, int width, int height);

    /** Infobulle d'objet à afficher pour cette image. */
    void hoverStack(ItemStack stack);

    /** Infobulle de texte à afficher pour cette image. */
    void hoverText(List<Component> lines);

    /** Un objet est-il déjà survolé (son infobulle a priorité) ? */
    boolean isHoveringStack();

    Optional<Recipe<?>> recipe(ResourceLocation id);

    ItemStack result(Recipe<?> recipe);

    /** Message « recette introuvable ». */
    void missing(GuiGraphics graphics, int x, int y);

    /** Fait défiler les objets possibles d'un ingrédient (un par seconde). */
    static ItemStack cycle(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        return items[(int) ((System.currentTimeMillis() / 1000L) % items.length)];
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
