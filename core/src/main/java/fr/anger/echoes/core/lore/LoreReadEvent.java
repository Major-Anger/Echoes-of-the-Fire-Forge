package fr.anger.echoes.core.lore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Posté sur NeoForge.EVENT_BUS quand un joueur lit une entrée de lore pour la première fois.
 * C'est le point d'accroche des liens d'histoire entre épisodes : la Forge peut réagir à une tablette
 * du Silt en comparant simplement son identifiant, sans dépendre du code du Silt.
 */
public class LoreReadEvent extends Event {

    private final ServerPlayer player;
    private final ResourceLocation entry;

    public LoreReadEvent(ServerPlayer player, ResourceLocation entry) {
        this.player = player;
        this.entry = entry;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getEntry() {
        return entry;
    }
}
