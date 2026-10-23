package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forging.ForgingData;
import fr.anger.echoes.fireforge.mechanism.Mechanism;
import fr.anger.echoes.fireforge.plan.Plans;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FFDataComponents {

    private FFDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EchoesFireForge.MOD_ID);

    /** Le numéro d'un Plan d'Attiseur (I à V). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PLAN_NUMBER =
            COMPONENTS.registerComponentType("plan_number", builder -> builder
                    .persistent(ExtraCodecs.intRange(1, Plans.COUNT))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Ce que la forge a laissé sur une arme ou une armure : rareté, statistiques, Spécial. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ForgingData>> FORGING =
            COMPONENTS.registerComponentType("forging", builder -> builder
                    .persistent(ForgingData.CODEC)
                    .networkSynchronized(ForgingData.STREAM_CODEC));

    /** Le mécanisme greffé à l'Établi d'Attiseur (un seul par pièce ; il reste quand on reforge). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Mechanism>> MECHANISM =
            COMPONENTS.registerComponentType("mechanism", builder -> builder
                    .persistent(Mechanism.CODEC)
                    .networkSynchronized(Mechanism.STREAM_CODEC));

    /** La Lampe à vapeur est allumée (gardé sur le casque : tous les joueurs voient le faisceau). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LAMP_ON =
            COMPONENTS.registerComponentType("lamp_on", builder -> builder
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /** Les charges de vapeur d'une pièce à mécanisme à vapeur (0 à 20), rechargées au bouilleur. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STEAM =
            COMPONENTS.registerComponentType("steam", builder -> builder
                    .persistent(ExtraCodecs.intRange(0, fr.anger.echoes.fireforge.workbench.Steam.CHARGES))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));
}
