package fr.anger.echoes.core.client.guide;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Un élément de page du guide (titre, bloc de texte, recette, créature...).
 * Les épisodes peuvent créer leurs propres éléments avec {@link Guide#registerPageType}.
 */
public interface GuideElement {

    /** Hauteur occupée sur la page, en pixels. Doit être connue avant le rendu (sert à répartir les pages). */
    int height();

    /** @param x, y coin haut gauche de l'élément ; la largeur disponible est {@link GuideContext#PAGE_WIDTH} */
    void render(GuiGraphics graphics, int x, int y, int mouseX, int mouseY);
}
