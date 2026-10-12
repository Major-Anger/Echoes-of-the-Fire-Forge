package fr.anger.echoes.core.echo;

import fr.anger.echoes.core.EchoesCore;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Les Échos : certains succès laissent un écho dans l'âme du joueur, un petit bonus permanent.
 * Chaque épisode enregistre les siens au démarrage (dans le constructeur de son mod) :
 * <pre>
 * Echoes.register(id("echoes/reef"));                                      // bonus vérifié ailleurs avec Echoes.has(...)
 * Echoes.register(id("echoes/chant"), p -> Souls.addBonus(p, id("chant"), 5)); // bonus appliqué une fois, à l'éveil
 * </pre>
 * Le succès lui-même reste un fichier JSON normal (data/&lt;mod&gt;/advancement/...). Il doit avoir un bloc
 * « display » : NeoForge ne signale pas l'obtention des succès sans display, l'Écho ne s'éveillerait jamais.
 * Texte affiché à l'éveil : la clé de traduction « echo.&lt;mod&gt;.&lt;chemin&gt; » (les / deviennent des points).
 */
@EventBusSubscriber(modid = EchoesCore.MOD_ID)
public final class Echoes {

    /** @param onAwaken appelé une seule fois, quand le joueur obtient le succès */
    public record Echo(ResourceLocation advancement, String descriptionKey, Consumer<ServerPlayer> onAwaken) {}

    private static final Map<ResourceLocation, Echo> ECHOES = new ConcurrentHashMap<>();

    private Echoes() {}

    public static Echo register(ResourceLocation advancement) {
        return register(advancement, player -> {});
    }

    public static Echo register(ResourceLocation advancement, Consumer<ServerPlayer> onAwaken) {
        return register(advancement, Util.makeDescriptionId("echo", advancement), onAwaken);
    }

    public static Echo register(ResourceLocation advancement, String descriptionKey, Consumer<ServerPlayer> onAwaken) {
        Echo echo = new Echo(advancement, descriptionKey, onAwaken);
        if (ECHOES.putIfAbsent(advancement, echo) != null) {
            throw new IllegalStateException("Écho déjà enregistré : " + advancement);
        }
        return echo;
    }

    public static boolean isEcho(ResourceLocation advancement) {
        return ECHOES.containsKey(advancement);
    }

    /** Tous les Échos de tous les épisodes installés. */
    public static Collection<Echo> all() {
        return Collections.unmodifiableCollection(ECHOES.values());
    }

    /** Accorde un succès (toutes ses conditions), Écho ou non. Sans effet s'il est déjà obtenu ou s'il n'existe pas. */
    public static void grant(ServerPlayer player, ResourceLocation advancement) {
        AdvancementHolder holder = player.server.getAdvancements().get(advancement);
        if (holder == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(holder, criterion);
    }

    /** Le joueur a-t-il ce succès (donc son Écho) ? Côté serveur seulement : toujours false côté client. */
    public static boolean has(Player player, ResourceLocation advancement) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        AdvancementHolder holder = serverPlayer.server.getAdvancements().get(advancement);
        return holder != null && serverPlayer.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Nombre d'Échos éveillés par le joueur, tous épisodes confondus. */
    public static int count(ServerPlayer player) {
        int count = 0;
        for (ResourceLocation advancement : ECHOES.keySet()) if (has(player, advancement)) count++;
        return count;
    }

    @SubscribeEvent
    public static void onEarn(AdvancementEvent.AdvancementEarnEvent event) {
        Echo echo = ECHOES.get(event.getAdvancement().id());
        if (echo == null || !(event.getEntity() instanceof ServerPlayer player)) return;

        echo.onAwaken().accept(player);
        player.displayClientMessage(Component.translatable("message.echoes_core.echo", Component.translatable(echo.descriptionKey()))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.6F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.4F);
        NeoForge.EVENT_BUS.post(new EchoAwakenedEvent(player, echo));
    }
}
