package fr.anger.echoes.fireforge.network;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.mechanism.MechanismEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> serveur : la touche d'un mécanisme d'armure (C casque, B plastron, N jambières) enfoncée ou relâchée. */
public record MechanismKeyPayload(EquipmentSlot slot, boolean down) implements CustomPacketPayload {

    public static final Type<MechanismKeyPayload> TYPE = new Type<>(EchoesFireForge.id("mechanism_key"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MechanismKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> EquipmentSlot.values()[i], EquipmentSlot::ordinal), MechanismKeyPayload::slot,
            ByteBufCodecs.BOOL, MechanismKeyPayload::down,
            MechanismKeyPayload::new
    );

    @Override
    public Type<MechanismKeyPayload> type() {
        return TYPE;
    }

    public static void handle(MechanismKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && payload.slot().isArmor()) MechanismEffects.key(player, payload.slot(), payload.down());
        });
    }
}
