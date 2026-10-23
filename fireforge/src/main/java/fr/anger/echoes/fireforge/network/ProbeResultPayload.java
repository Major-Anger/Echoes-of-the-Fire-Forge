package fr.anger.echoes.fireforge.network;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.mechanism.ProbeMarks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Serveur -> client : les minerais que la Sonde de prospection a trouvés, à faire briller un instant. */
public record ProbeResultPayload(List<BlockPos> ores) implements CustomPacketPayload {

    public static final Type<ProbeResultPayload> TYPE = new Type<>(EchoesFireForge.id("probe_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProbeResultPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(512)), ProbeResultPayload::ores,
            ProbeResultPayload::new
    );

    @Override
    public Type<ProbeResultPayload> type() {
        return TYPE;
    }

    public static void handle(ProbeResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ProbeMarks.show(payload.ores()));
    }
}
