package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.core.compat.EchoesMods;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.plan.Plans;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FFCreativeTab {

    private FFCreativeTab() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EchoesFireForge.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.echoes_fireforge"))
            .icon(() -> new ItemStack(FFItems.MACHINE_GEAR.get()))
            .displayItems((params, output) -> {
                output.accept(FFItems.FIRE_FORGE.get());
                output.accept(FFItems.BOILER.get());
                output.accept(FFItems.LAVA_TANK.get());
                if (EchoesMods.isLoaded(EchoesMods.SILT)) output.accept(FFItems.SOUL_MODULE.get());
                output.accept(FFItems.COMPRESSED_MACHINE.get());
                output.accept(FFItems.MACHINE_GEAR.get());
                output.accept(FFItems.MACHINE_VALVE.get());
                output.accept(FFItems.ENGRAVED_PLATE.get());
                output.accept(FFItems.FORGE_PLAN.get());
                for (int n = 1; n <= Plans.COUNT; n++) {
                    ItemStack plan = new ItemStack(FFItems.ATTISEUR_PLAN.get());
                    plan.set(FFDataComponents.PLAN_NUMBER, n);
                    output.accept(plan);
                }
            })
            .build());
}
