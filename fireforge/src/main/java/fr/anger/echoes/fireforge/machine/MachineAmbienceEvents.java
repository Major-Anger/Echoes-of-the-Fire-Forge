package fr.anger.echoes.fireforge.machine;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.network.MachineAmbiencePayload;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Une fois par seconde : le joueur est-il dans la caverne de la Grande Machine ? Son client en tire l'ambiance (15b). */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class MachineAmbienceEvents {

    private MachineAmbienceEvents() {}

    /** Ce que chaque joueur a reçu en dernier : 0 dehors ; dedans : 1 + le choix (1 rien, 2 baissée, 3 poussée). */
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    /** Dans la caverne (ou sur la machine, son quai, son tunnel). */
    public static boolean inside(ServerPlayer player) {
        return player.serverLevel().structureManager().getStructureWithPieceAt(player.blockPosition(),
                holder -> holder.is(FFStructures.GRAND_MACHINE_KEY)).isValid();
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        update(player);
    }

    private static void update(ServerPlayer player) {
        int now = inside(player) ? 1 + GrandMachineState.get(player.serverLevel()).choice().ordinal() : 0;
        Integer before = SENT.get(player.getUUID());
        if (before != null && before == now || before == null && now == 0) return;
        SENT.put(player.getUUID(), now);
        PacketDistributor.sendToPlayer(player, new MachineAmbiencePayload(now > 0, (byte) Math.max(0, now - 1)));
    }

    /** Le choix vient d'être fait : ceux qui sont dans la caverne l'entendent tout de suite. */
    public static void refresh(net.minecraft.server.level.ServerLevel level) {
        for (ServerPlayer player : level.players()) update(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SENT.remove(player.getUUID()) != null) {
            PacketDistributor.sendToPlayer(player, new MachineAmbiencePayload(false, (byte) 0));
        }
    }
}
