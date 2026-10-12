package fr.anger.echoes.core.lore;

import fr.anger.echoes.core.CoreAttachments;
import fr.anger.echoes.core.EchoesCore;
import fr.anger.echoes.core.network.LoreSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ce que le joueur a lu (tablettes, pages, inscriptions...), tous épisodes confondus.
 * Chaque entrée est un identifiant libre choisi par l'épisode, par exemple echoes_silt:tablet/3.
 * Côté serveur ; le client lit {@link fr.anger.echoes.core.client.ClientLore}.
 */
@EventBusSubscriber(modid = EchoesCore.MOD_ID)
public final class Lore {

    private Lore() {}

    public static Set<ResourceLocation> read(Player player) {
        return player.getData(CoreAttachments.LORE);
    }

    public static boolean hasRead(Player player, ResourceLocation entry) {
        return read(player).contains(entry);
    }

    /** Nombre d'entrées lues dans un épisode (son namespace). */
    public static int count(Player player, String namespace) {
        int count = 0;
        for (ResourceLocation entry : read(player)) if (entry.getNamespace().equals(namespace)) count++;
        return count;
    }

    /**
     * Marque une entrée comme lue, la renvoie au client et poste un {@link LoreReadEvent}.
     * @return true si elle n'avait pas encore été lue
     */
    public static boolean markRead(ServerPlayer player, ResourceLocation entry) {
        Set<ResourceLocation> read = read(player);
        if (read.contains(entry)) return false;
        Set<ResourceLocation> updated = new HashSet<>(read);
        updated.add(entry);
        player.setData(CoreAttachments.LORE, Set.copyOf(updated));
        sync(player);
        NeoForge.EVENT_BUS.post(new LoreReadEvent(player, entry));
        return true;
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new LoreSyncPayload(List.copyOf(read(player))));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }
}
