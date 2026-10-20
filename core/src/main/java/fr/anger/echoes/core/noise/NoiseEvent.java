package fr.anger.echoes.core.noise;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Postés sur NeoForge.EVENT_BUS par {@link Noise#make}. */
public abstract class NoiseEvent extends Event {

    private final ServerPlayer player;
    private final ResourceLocation cause;
    protected float amount;

    protected NoiseEvent(ServerPlayer player, ResourceLocation cause, float amount) {
        this.player = player;
        this.cause = cause;
        this.amount = amount;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    /** Ce qui a fait le bruit (voir les constantes de {@link Noise}). */
    public ResourceLocation getCause() {
        return cause;
    }

    public float getAmount() {
        return amount;
    }

    /** Avant : la force peut être changée, ou le bruit annulé (étouffé). */
    public static class Pre extends NoiseEvent implements ICancellableEvent {
        public Pre(ServerPlayer player, ResourceLocation cause, float amount) {
            super(player, cause, amount);
        }

        public void setAmount(float amount) {
            this.amount = Math.max(0, amount);
        }
    }

    /** Après : le bruit tel qu'il a été fait, à écouter (par exemple la Gueule du Silt). */
    public static class Post extends NoiseEvent {
        public Post(ServerPlayer player, ResourceLocation cause, float amount) {
            super(player, cause, amount);
        }
    }
}
