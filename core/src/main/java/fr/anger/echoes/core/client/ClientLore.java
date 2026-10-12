package fr.anger.echoes.core.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Côté client : les entrées de lore lues par le joueur (reçues du serveur). */
public final class ClientLore {

    private static final Set<ResourceLocation> READ = new HashSet<>();

    private ClientLore() {}

    public static void set(Collection<ResourceLocation> read) {
        READ.clear();
        READ.addAll(read);
    }

    /** Lecture optimiste (avant la réponse du serveur), pour afficher la page tout de suite. */
    public static void markRead(ResourceLocation entry) {
        READ.add(entry);
    }

    public static boolean hasRead(ResourceLocation entry) {
        return READ.contains(entry);
    }

    public static boolean hasAny(String namespace) {
        for (ResourceLocation entry : READ) if (entry.getNamespace().equals(namespace)) return true;
        return false;
    }

    public static int count(String namespace) {
        int count = 0;
        for (ResourceLocation entry : READ) if (entry.getNamespace().equals(namespace)) count++;
        return count;
    }
}
