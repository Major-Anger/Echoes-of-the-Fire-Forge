package fr.anger.echoes.core.soul;

import fr.anger.echoes.core.CoreAttachments;
import fr.anger.echoes.core.EchoesCore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = EchoesCore.MOD_ID)
public final class SoulEvents {

    private SoulEvents() {}

    /** À la mort, les âmes sont perdues… mais pas la taille de la jauge. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        event.getEntity().getData(CoreAttachments.SOULS).copyBonusesFrom(event.getOriginal().getData(CoreAttachments.SOULS));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Souls.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Souls.sync(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Souls.sync(player);
    }
}
