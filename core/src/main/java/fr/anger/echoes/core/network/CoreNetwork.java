package fr.anger.echoes.core.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CoreNetwork {

    private CoreNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(fr.anger.echoes.core.EchoesCore.MOD_ID).versioned("1");
        registrar.playToClient(SoulSyncPayload.TYPE, SoulSyncPayload.STREAM_CODEC, SoulSyncPayload::handle);
        registrar.playToClient(LoreSyncPayload.TYPE, LoreSyncPayload.STREAM_CODEC, LoreSyncPayload::handle);
    }
}
