package fr.anger.echoes.core.client.guide;

import com.google.gson.JsonObject;
import fr.anger.echoes.core.client.ClientLore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

/**
 * Le livre guide commun à la série (côté client).
 * Le contenu vient des fichiers JSON des épisodes (voir {@link GuideBook}) ; ce qui demande du Java passe par ici :
 * <pre>
 * // un type de page propre à l'épisode, utilisable dans ses JSON avec "type": "chant_table"
 * Guide.registerPageType(id("chant_table"), (json, ctx) -> new ChantTableElement(ctx));
 * // modifier le texte affiché (ex. : illisible pendant la folie de Carcosa)
 * Guide.addTextFilter(text -> CarcosaClient.isMad() ? text.copy().withStyle(...) : text);
 * </pre>
 * À appeler pendant FMLClientSetupEvent (ou plus tôt, côté client).
 */
public final class Guide {

    @FunctionalInterface
    public interface PageFactory {
        GuideElement create(JsonObject json, GuideContext context);
    }

    private static final Map<ResourceLocation, PageFactory> PAGE_TYPES = new ConcurrentHashMap<>();
    private static final List<UnaryOperator<Component>> TEXT_FILTERS = new CopyOnWriteArrayList<>();

    private Guide() {}

    /**
     * Ajoute un type de page. Dans les JSON, "type": "chant_table" cherche d'abord les types du Core,
     * puis &lt;mod de l'entrée&gt;:chant_table ; "type": "echoes_silt:chant_table" désigne le type exact.
     */
    public static void registerPageType(ResourceLocation type, PageFactory factory) {
        if (PAGE_TYPES.putIfAbsent(type, factory) != null) throw new IllegalStateException("Type de page déjà enregistré : " + type);
    }

    public static void addTextFilter(UnaryOperator<Component> filter) {
        TEXT_FILTERS.add(filter);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }

    /** Ouvre le livre sur l'entrée liée à cette entrée de lore (après la lecture d'une tablette, par exemple). */
    public static void openLore(ResourceLocation lore) {
        ClientLore.markRead(lore); // lecture optimiste : la page s'affiche sans attendre le serveur
        GuideScreen screen = new GuideScreen();
        screen.focus(entry -> lore.equals(entry.lore()));
        Minecraft.getInstance().setScreen(screen);
    }

    /** Ouvre le livre sur une entrée précise (ex. echoes_silt:creatures/sediment). */
    public static void openEntry(ResourceLocation entryId) {
        GuideScreen screen = new GuideScreen();
        screen.focus(entry -> entryId.equals(entry.id()));
        Minecraft.getInstance().setScreen(screen);
    }

    static PageFactory pageType(ResourceLocation type) {
        return PAGE_TYPES.get(type);
    }

    static Component filterText(Component text) {
        for (UnaryOperator<Component> filter : TEXT_FILTERS) text = filter.apply(text);
        return text;
    }
}
