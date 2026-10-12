package fr.anger.echoes.fireforge.compat;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.neoforged.bus.api.IEventBus;

/**
 * Tout ce que la Forge ajoute quand Echoes of the Silt est installé.
 * Appelée seulement si le Silt est présent (voir EchoesFireForge) : sans lui, cette classe n'est jamais chargée.
 *
 * Pour réagir au Silt, passer par le Core plutôt que par le code du Silt :
 *  - NeoForge.EVENT_BUS.addListener(LoreReadEvent.class, ...) : le joueur a lu une tablette du Silt ;
 *  - NeoForge.EVENT_BUS.addListener(EchoAwakenedEvent.class, ...) : il a éveillé un Écho du Silt ;
 *  - les âmes sont déjà communes (Souls).
 * Les objets du Silt se désignent par leur identifiant ou par des tags, pas par ses classes Java.
 */
public final class SiltCompat {

    private SiltCompat() {}

    public static void init(IEventBus modBus) {
        EchoesFireForge.LOGGER.info("Echoes of the Silt détecté : contenu croisé activé.");
    }
}
