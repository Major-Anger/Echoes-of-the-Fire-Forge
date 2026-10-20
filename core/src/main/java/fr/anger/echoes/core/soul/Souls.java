package fr.anger.echoes.core.soul;

import fr.anger.echoes.core.CoreAttachments;
import fr.anger.echoes.core.network.SoulSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Les âmes, partagées par tous les épisodes : celles récoltées dans le Silt se dépensent à la Forge.
 * Toutes les méthodes qui modifient la jauge la renvoient au client.
 * {@link #add}, {@link #lose} et {@link #spend} postent un {@link SoulChangeEvent} que les autres mods peuvent modifier.
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

    /**
     * Ajoute des âmes gagnées, dans les limites de la jauge. Négatif, c'est une correction
     * (remettre la jauge où on la veut) : pour une perte passive, préférer {@link #lose}, pour un coût, {@link #spend}.
     */
    public static void add(ServerPlayer player, int amount) {
        change(player, amount >= 0 ? SoulChangeEvent.Cause.GAIN : SoulChangeEvent.Cause.CORRECTION, amount);
    }

    /** Retire des âmes perdues sans être dépensées (usure, vol…), dans les limites de la jauge. */
    public static void lose(ServerPlayer player, int amount) {
        if (amount > 0) change(player, SoulChangeEvent.Cause.LOSS, -amount);
    }

    private static void change(ServerPlayer player, SoulChangeEvent.Cause cause, int amount) {
        if (amount == 0) return;
        amount = NeoForge.EVENT_BUS.post(new SoulChangeEvent(player, cause, amount)).getAmount();
        if (amount == 0) return;
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
        amount = -NeoForge.EVENT_BUS.post(new SoulChangeEvent(player, SoulChangeEvent.Cause.SPEND, -amount)).getAmount();
        if (amount == 0) return true;
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
