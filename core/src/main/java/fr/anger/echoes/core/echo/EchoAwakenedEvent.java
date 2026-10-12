package fr.anger.echoes.core.echo;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Posté sur NeoForge.EVENT_BUS quand un joueur éveille un Écho, quel que soit l'épisode qui l'a créé.
 * Un épisode peut y réagir sans dépendre de l'épisode d'origine : il suffit de comparer l'identifiant.
 */
public class EchoAwakenedEvent extends Event {

    private final ServerPlayer player;
    private final Echoes.Echo echo;

    public EchoAwakenedEvent(ServerPlayer player, Echoes.Echo echo) {
        this.player = player;
        this.echo = echo;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Echoes.Echo getEcho() {
        return echo;
    }
}
