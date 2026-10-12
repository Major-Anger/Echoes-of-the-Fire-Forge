package fr.anger.echoes.core.soul;

import fr.anger.echoes.core.CoreAttachments;
import fr.anger.echoes.core.network.SoulSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Les âmes, partagées par tous les épisodes : celles récoltées dans le Silt se dépensent à la Forge.
 * Toutes les méthodes qui modifient la jauge la renvoient au client.
 */
public final class Souls {

    private Souls() {}

    public static SoulData data(Player player) {
        return player.getData(CoreAttachments.SOULS);
    }

    public static int get(Player player) {
        return data(player).getSouls();
    }

    public static int max(Player player) {
        return data(player).getMax();
    }

    /** Ajoute (ou retire, si négatif) des âmes, dans les limites de la jauge. */
    public static void add(ServerPlayer player, int amount) {
        SoulData data = data(player);
        data.setSouls(data.getSouls() + amount);
        sync(player);
    }

    public static void set(ServerPlayer player, int amount) {
        data(player).setSouls(amount);
        sync(player);
    }

    public static void fill(ServerPlayer player) {
        SoulData data = data(player);
        data.setSouls(data.getMax());
        sync(player);
    }

    /** Retire des âmes si le joueur en a assez (gratuit en créatif). @return true si la dépense a eu lieu */
    public static boolean spend(ServerPlayer player, int amount) {
        if (player.isCreative() || amount <= 0) return true;
        SoulData data = data(player);
        if (data.getSouls() < amount) return false;
        data.setSouls(data.getSouls() - amount);
        sync(player);
        return true;
    }

    public static int bonus(Player player, ResourceLocation source) {
        return data(player).getBonus(source);
    }

    /** Fixe le bonus de taille donné par une source (0 = le retirer). Conservé à la mort. */
    public static void setBonus(ServerPlayer player, ResourceLocation source, int bonus) {
        data(player).setBonus(source, bonus);
        sync(player);
    }

    public static void addBonus(ServerPlayer player, ResourceLocation source, int amount) {
        setBonus(player, source, bonus(player, source) + amount);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SoulSyncPayload.of(data(player)));
    }
}
