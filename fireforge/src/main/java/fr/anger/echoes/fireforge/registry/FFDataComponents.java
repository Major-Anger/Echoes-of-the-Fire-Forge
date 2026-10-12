package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forging.ForgingData;
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
}
