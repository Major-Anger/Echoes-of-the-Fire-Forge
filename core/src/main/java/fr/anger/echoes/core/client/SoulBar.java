package fr.anger.echoes.core.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/**
 * La barre d'âmes est dessinée par le Core ; les épisodes peuvent la compléter (côté client seulement) :
 * <pre>
 * SoulBar.showWhen(() -> estDansLeSilt());                 // l'afficher même à 0 âme
 * SoulBar.decorate((g, x, y, delta) -> dessinerLaGueule(g, x, y)); // dessiner autour
 * </pre>
 */
public final class SoulBar {

    /**
     * Dessin ajouté par un épisode. Appelé après la barre et sa bannière, dans le même repère :
     * (x, y) = coin haut gauche de la bannière (94 × 20), échelle déjà appliquée (réglable par le joueur :
     * soulBar.scale dans echoes_core-client.toml, 0,75 à 2). Dessiner en coordonnées relatives à (x, y).
     */
    @FunctionalInterface
    public interface Decoration {
        void render(GuiGraphics graphics, int x, int y, DeltaTracker delta);
    }

    private static final List<BooleanSupplier> SHOW_WHEN = new CopyOnWriteArrayList<>();
    private static final List<Decoration> DECORATIONS = new CopyOnWriteArrayList<>();

    private SoulBar() {}

    /** Par défaut, la barre n'apparaît que si le joueur a au moins une âme. */
    public static void showWhen(BooleanSupplier condition) {
        SHOW_WHEN.add(condition);
    }

    public static void decorate(Decoration decoration) {
        DECORATIONS.add(decoration);
    }

    static boolean forcedVisible() {
        for (BooleanSupplier condition : SHOW_WHEN) if (condition.getAsBoolean()) return true;
        return false;
    }

    static List<Decoration> decorations() {
        return DECORATIONS;
    }
}
