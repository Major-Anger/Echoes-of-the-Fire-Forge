package fr.anger.echoes.core.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le contenu du livre guide, assemblé à partir de tous les épisodes installés.
 *
 * Chaque mod fournit (sans Java) :
 *  - assets/&lt;mod&gt;/echoes_guide/book.json          : ses chapitres et leurs entrées ;
 *  - assets/&lt;mod&gt;/echoes_guide/entries/&lt;id&gt;.json : une entrée = une suite de pages.
 * Les textes sont des clés de traduction.
 *
 * book.json :
 * <pre>
 * {
 *   "order": 1,                      // place de l'épisode dans le livre (1 = Silt, 2 = Forge...)
 *   "intro": "guide.echoes_silt.intro",   // texte d'accueil de l'épisode (facultatif)
 *   "categories": [
 *     { "id": "chronicles", "icon": "echoes_silt:watcher_tablet",
 *       "title": "...", "description": "...",
 *       "hidden": true,                  // caché tant qu'aucune entrée de lore de ce mod n'a été lue
 *       "requires": ["echoes_silt"],     // seulement si ces mods sont installés
 *       "entries": ["chronicles/tablet_1", "echoes_fireforge:silt/forge_tablet"] }
 *   ]
 * }
 * </pre>
 * Un chapitre déclaré avec l'identifiant complet d'un chapitre d'un autre mod (« echoes_silt:chronicles »)
 * ajoute ses entrées à ce chapitre : c'est ainsi qu'un épisode complète le livre d'un autre.
 *
 * Entrée :
 * <pre>
 * { "title": "...", "icon": "...",
 *   "lore": "echoes_silt:tablet/3",  // page « effacée » tant que cette entrée de lore n'a pas été lue
 *   "label": "III",                   // affiché devant le titre (et sur la page effacée)
 *   "requires": ["echoes_silt"],
 *   "pages": [ { "type": "text", "title": "...", "text": "..." }, ... ] }
 * </pre>
 */
public final class GuideBook {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "echoes_guide/";

    public record Category(ResourceLocation id, ItemStack icon, String titleKey, String descKey, boolean hidden, List<Entry> entries) {}

    public record Entry(ResourceLocation id, ItemStack icon, String titleKey, @Nullable ResourceLocation lore, String label, List<JsonObject> pages) {}

    /** Un book.json : l'apport d'un mod au livre. */
    private record Part(String namespace, int order, String introKey, JsonObject json) {}

    private final List<String> introKeys;
    private final List<Category> categories;

    private GuideBook(List<String> introKeys, List<Category> categories) {
        this.introKeys = introKeys;
        this.categories = categories;
    }

    /** Textes d'accueil des épisodes, dans l'ordre du livre. */
    public List<String> introKeys() {
        return introKeys;
    }

    public List<Category> categories() {
        return categories;
    }

    public static GuideBook load(ResourceManager resources) {
        List<Part> parts = new ArrayList<>();
        for (String namespace : resources.getNamespaces()) {
            JsonObject json = read(resources, ResourceLocation.fromNamespaceAndPath(namespace, ROOT + "book.json"));
            if (json == null) continue;
            parts.add(new Part(namespace, GsonHelper.getAsInt(json, "order", 1000), GsonHelper.getAsString(json, "intro", ""), json));
        }
        parts.sort(Comparator.comparingInt(Part::order).thenComparing(Part::namespace));

        List<String> intros = new ArrayList<>();
        Map<ResourceLocation, Category> categories = new LinkedHashMap<>();
        for (Part part : parts) {
            if (!part.introKey().isEmpty()) intros.add(part.introKey());
            for (JsonElement element : GsonHelper.getAsJsonArray(part.json(), "categories", new JsonArray())) {
                JsonObject cat = element.getAsJsonObject();
                if (!requirementsMet(cat)) continue;
                ResourceLocation id = id(part.namespace(), GsonHelper.getAsString(cat, "id"));
                Category category = categories.get(id);
                // Le mod qui déclare le chapitre (il a un titre) fixe son apparence, même s'il est lu après
                // un mod qui le complète.
                if (category == null || (category.titleKey().isEmpty() && cat.has("title"))) {
                    category = new Category(id, stack(GsonHelper.getAsString(cat, "icon", "minecraft:book")),
                            GsonHelper.getAsString(cat, "title", ""), GsonHelper.getAsString(cat, "description", ""),
                            GsonHelper.getAsBoolean(cat, "hidden", false),
                            category == null ? new ArrayList<>() : category.entries());
                    categories.put(id, category);
                }
                for (JsonElement entryId : GsonHelper.getAsJsonArray(cat, "entries", new JsonArray())) {
                    Entry entry = loadEntry(resources, id(part.namespace(), entryId.getAsString()));
                    if (entry != null) category.entries().add(entry);
                }
            }
        }
        List<Category> result = new ArrayList<>();
        for (Category category : categories.values()) {
            if (category.titleKey().isEmpty()) {
                LOGGER.warn("[Guide] Chapitre {} complété par un autre mod mais jamais déclaré : ignoré", category.id());
                continue;
            }
            result.add(new Category(category.id(), category.icon(), category.titleKey(), category.descKey(), category.hidden(),
                    List.copyOf(category.entries())));
        }
        return new GuideBook(List.copyOf(intros), List.copyOf(result));
    }

    @Nullable
    private static Entry loadEntry(ResourceManager resources, ResourceLocation id) {
        JsonObject entry = read(resources, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), ROOT + "entries/" + id.getPath() + ".json"));
        if (entry == null) {
            LOGGER.warn("[Guide] Entrée introuvable : {}", id);
            return null;
        }
        if (!requirementsMet(entry)) return null;
        List<JsonObject> pages = new ArrayList<>();
        for (JsonElement page : GsonHelper.getAsJsonArray(entry, "pages", new JsonArray())) pages.add(page.getAsJsonObject());
        String lore = GsonHelper.getAsString(entry, "lore", "");
        return new Entry(id, stack(GsonHelper.getAsString(entry, "icon", "minecraft:book")), GsonHelper.getAsString(entry, "title"),
                lore.isEmpty() ? null : ResourceLocation.parse(lore), GsonHelper.getAsString(entry, "label", ""), pages);
    }

    private static boolean requirementsMet(JsonObject json) {
        for (JsonElement mod : GsonHelper.getAsJsonArray(json, "requires", new JsonArray())) {
            if (!ModList.get().isLoaded(mod.getAsString())) return false;
        }
        return true;
    }

    /** « chronicles » -> &lt;namespace&gt;:chronicles ; « echoes_silt:chronicles » reste tel quel. */
    private static ResourceLocation id(String namespace, String id) {
        return id.indexOf(':') >= 0 ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath(namespace, id);
    }

    public static ItemStack stack(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return new ItemStack(Items.BARRIER);
        return new ItemStack(BuiltInRegistries.ITEM.get(location));
    }

    @Nullable
    private static JsonObject read(ResourceManager resources, ResourceLocation location) {
        Optional<Resource> resource = resources.getResource(location);
        if (resource.isEmpty()) return null;
        try (Reader reader = resource.get().openAsReader()) {
            return GsonHelper.parse(reader);
        } catch (Exception e) {
            LOGGER.error("[Guide] Impossible de lire {} : {}", location, e.getMessage());
            return null;
        }
    }
}
