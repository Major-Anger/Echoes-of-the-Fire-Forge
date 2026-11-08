package fr.anger.echoes.fireforge.network;

import fr.anger.echoes.fireforge.EchoesFireForge;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Serveur -> client : le joueur entre dans la caverne de la Grande Machine, ou en sort (son ambiance, plan 15b), et ce
 * qu'on a fait de la machine (15c : 0 rien, 1 la pression baissée, 2 poussée).
 */
public record MachineAmbiencePayload(boolean inside, byte choice) implements CustomPacketPayload {

    public static final Type<MachineAmbiencePayload> TYPE = new Type<>(EchoesFireForge.id("machine_ambience"));

    public static final StreamCodec<ByteBuf, MachineAmbiencePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MachineAmbiencePayload::inside,
            ByteBufCodecs.BYTE, MachineAmbiencePayload::choice,
            MachineAmbiencePayload::new);

    @Override
    public Type<MachineAmbiencePayload> type() {
        return TYPE;
    }

    public static void handle(MachineAmbiencePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> fr.anger.echoes.fireforge.client.MachineAmbience.set(payload.inside(), payload.choice()));
    }
}
