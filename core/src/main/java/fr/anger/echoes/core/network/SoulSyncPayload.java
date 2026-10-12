package fr.anger.echoes.core.network;

import fr.anger.echoes.core.EchoesCore;
import fr.anger.echoes.core.client.ClientSouls;
import fr.anger.echoes.core.soul.SoulData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Serveur -> client : la jauge d'âmes. */
public record SoulSyncPayload(int souls, int max) implements CustomPacketPayload {

    public static final Type<SoulSyncPayload> TYPE = new Type<>(EchoesCore.id("soul_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SoulSyncPayload::souls,
            ByteBufCodecs.VAR_INT, SoulSyncPayload::max,
            SoulSyncPayload::new
    );

    public static SoulSyncPayload of(SoulData data) {
        return new SoulSyncPayload(data.getSouls(), data.getMax());
    }

    @Override
    public Type<SoulSyncPayload> type() {
        return TYPE;
    }

    public static void handle(SoulSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSouls.set(payload.souls(), payload.max()));
    }
}
