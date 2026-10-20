package fr.anger.echoes.core.noise;

import fr.anger.echoes.core.EchoesCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Le bruit que fait un joueur, partagé par tous les épisodes : un épisode le fait, un autre l'atténue,
 * un troisième l'écoute, sans qu'aucun ne dépende du code des autres.
 * {@link #make} poste {@link NoiseEvent.Pre} (modifiable, annulable) puis {@link NoiseEvent.Post} (le bruit final).
 * La force n'a pas d'unité commune : chaque épisode qui écoute l'interprète à sa façon.
 */
public final class Noise {

    private Noise() {}

    /** Courir (un bruit répété tant que le joueur court). */
    public static final ResourceLocation RUN = EchoesCore.id("run");
    /** Frapper. */
    public static final ResourceLocation ATTACK = EchoesCore.id("attack");
    /** Être frappé. */
    public static final ResourceLocation HURT = EchoesCore.id("hurt");
    /** Casser un bloc. */
    public static final ResourceLocation BREAK = EchoesCore.id("break");
    /** Une explosion proche. */
    public static final ResourceLocation EXPLOSION = EchoesCore.id("explosion");
    /** Tirer (arc, arbalète…). */
    public static final ResourceLocation SHOT = EchoesCore.id("shot");
    /** Un bruit sans catégorie commune ; un épisode peut aussi utiliser ses propres identifiants. */
    public static final ResourceLocation OTHER = EchoesCore.id("other");

    /** @return la force finale du bruit (0 s'il a été étouffé) */
    public static float make(ServerPlayer player, float amount, ResourceLocation cause) {
        if (amount <= 0) return 0;
        NoiseEvent.Pre pre = NeoForge.EVENT_BUS.post(new NoiseEvent.Pre(player, cause, amount));
        if (pre.isCanceled() || pre.getAmount() <= 0) return 0;
        NeoForge.EVENT_BUS.post(new NoiseEvent.Post(player, cause, pre.getAmount()));
        return pre.getAmount();
    }
}
