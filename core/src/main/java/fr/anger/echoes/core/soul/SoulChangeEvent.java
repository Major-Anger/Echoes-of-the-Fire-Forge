package fr.anger.echoes.core.soul;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Posté sur NeoForge.EVENT_BUS juste avant que la jauge d'un joueur change, par {@link Souls}.
 * La quantité peut être modifiée (0 = rien ne change) : c'est ainsi qu'un épisode réduit les gains
 * ou les pertes d'un autre sans dépendre de son code. {@link Souls#set} et {@link Souls#fill} ne le postent pas.
 */
public class SoulChangeEvent extends Event {

    /** D'où vient le changement. */
    public enum Cause {
        /** Des âmes gagnées ({@link Souls#add} positif). */
        GAIN,
        /** Des âmes perdues sans être dépensées : usure, vol… ({@link Souls#lose}). */
        LOSS,
        /** Une vraie dépense ({@link Souls#spend}) ; la quantité ne doit pas dépasser la jauge. */
        SPEND,
        /** Une correction ({@link Souls#add} négatif) : un mod remet la jauge où il la veut. */
        CORRECTION
    }

    private final ServerPlayer player;
    private final Cause cause;
    private final int originalAmount;
    private int amount;

    public SoulChangeEvent(ServerPlayer player, Cause cause, int amount) {
        this.player = player;
        this.cause = cause;
        this.originalAmount = amount;
        this.amount = amount;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Cause getCause() {
        return cause;
    }

    /** La quantité demandée, avant tout changement (négative pour une perte ou une dépense). */
    public int getOriginalAmount() {
        return originalAmount;
    }

    /** La quantité qui sera appliquée (négative pour une perte ou une dépense). */
    public int getAmount() {
        return amount;
    }

    /** Fixe la quantité ; elle garde le signe de la quantité d'origine (sinon 0). */
    public void setAmount(int amount) {
        this.amount = originalAmount >= 0 ? Math.max(0, amount) : Math.min(0, amount);
    }
}
