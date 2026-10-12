package fr.anger.echoes.core.compat;

import fr.anger.echoes.core.EchoesCore;
import net.neoforged.fml.ModList;

/**
 * Les épisodes de la série et leur détection.
 *
 * Règle de la série : le contenu croisé vit dans l'épisode le plus récent. La Forge connaît le Silt,
 * le Silt n'a pas besoin de connaître la Forge.
 *
 * Code Java qui touche à un autre épisode : on le range dans une classe à part, appelée seulement
 * si l'épisode est là, pour ne jamais charger ses classes quand il est absent :
 * <pre>
 * if (EchoesMods.isLoaded(EchoesMods.SILT)) SiltCompat.init(modBus);
 * </pre>
 * Recettes, loot, etc. : pas besoin de Java, une condition suffit dans le JSON :
 * <pre>
 * "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "echoes_silt" } ]
 * </pre>
 */
public final class EchoesMods {

    public static final String CORE = EchoesCore.MOD_ID;
    /** À confirmer quand le nouvel identifiant du Silt sera choisi. */
    public static final String SILT = "echoes_silt";
    public static final String FIRE_FORGE = "echoes_fireforge";

    private EchoesMods() {}

    /** Utilisable dès le constructeur des mods. */
    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
