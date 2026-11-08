package fr.anger.echoes.fireforge.network;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Les messages de la Forge : les touches des mécanismes (client -> serveur), la Sonde et l'ambiance de la Grande Machine (serveur -> client). */
public final class FFNetwork {

    private FFNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(EchoesFireForge.MOD_ID).versioned("1");
        registrar.playToServer(MechanismKeyPayload.TYPE, MechanismKeyPayload.STREAM_CODEC, MechanismKeyPayload::handle);
        registrar.playToClient(ProbeResultPayload.TYPE, ProbeResultPayload.STREAM_CODEC, ProbeResultPayload::handle);
        registrar.playToClient(MachineAmbiencePayload.TYPE, MachineAmbiencePayload.STREAM_CODEC, MachineAmbiencePayload::handle);
    }
}
