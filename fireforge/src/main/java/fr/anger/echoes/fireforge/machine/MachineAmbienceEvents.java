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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Une fois par seconde : le joueur est-il dans la caverne de la Grande Machine ? Son client en tire l'ambiance (15b). */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class MachineAmbienceEvents {

    private MachineAmbienceEvents() {}

    private static final Set<UUID> INSIDE = new HashSet<>();

    /** Dans la caverne (ou sur la machine, son quai, son tunnel). */
    public static boolean inside(ServerPlayer player) {
        return player.serverLevel().structureManager().getStructureWithPieceAt(player.blockPosition(),
                holder -> holder.is(FFStructures.GRAND_MACHINE_KEY)).isValid();
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        boolean now = inside(player);
        if (now == INSIDE.contains(player.getUUID())) return;
        if (now) INSIDE.add(player.getUUID()); else INSIDE.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new MachineAmbiencePayload(now));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INSIDE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && INSIDE.remove(player.getUUID())) {
            PacketDistributor.sendToPlayer(player, new MachineAmbiencePayload(false));
        }
    }
}
