package fr.anger.echoes.core.network;

import fr.anger.echoes.core.EchoesCore;
import fr.anger.echoes.core.client.ClientLore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Serveur -> client : toutes les entrées de lore lues par le joueur. */
public record LoreSyncPayload(List<ResourceLocation> read) implements CustomPacketPayload {

    public static final Type<LoreSyncPayload> TYPE = new Type<>(EchoesCore.id("lore_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoreSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), LoreSyncPayload::read,
            LoreSyncPayload::new
    );

    @Override
    public Type<LoreSyncPayload> type() {
        return TYPE;
    }

    public static void handle(LoreSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientLore.set(payload.read()));
    }
}
