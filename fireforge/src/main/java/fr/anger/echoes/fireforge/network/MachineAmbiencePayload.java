package fr.anger.echoes.fireforge.network;

import fr.anger.echoes.fireforge.EchoesFireForge;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> client : le joueur entre dans la caverne de la Grande Machine, ou en sort (son ambiance, plan 15b). */
public record MachineAmbiencePayload(boolean inside) implements CustomPacketPayload {

    public static final Type<MachineAmbiencePayload> TYPE = new Type<>(EchoesFireForge.id("machine_ambience"));

    public static final StreamCodec<ByteBuf, MachineAmbiencePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(MachineAmbiencePayload::new, MachineAmbiencePayload::inside);

    @Override
    public Type<MachineAmbiencePayload> type() {
        return TYPE;
    }

    public static void handle(MachineAmbiencePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.anger.echoes.fireforge.client.MachineAmbience.set(payload.inside()));
    }
}
