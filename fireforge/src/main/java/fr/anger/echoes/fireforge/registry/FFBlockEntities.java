package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.FireForgeBlock;
import fr.anger.echoes.fireforge.forge.FireForgeBlockEntity;
import fr.anger.echoes.fireforge.forge.ForgeHopperAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FFBlockEntities {

    private FFBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EchoesFireForge.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireForgeBlockEntity>> FIRE_FORGE = BLOCK_ENTITIES.register("fire_forge",
            () -> BlockEntityType.Builder.of(FireForgeBlockEntity::new, FFBlocks.FIRE_FORGE.get()).build(null));

    /** Les entonnoirs fonctionnent sur les deux moitiés de la forge. */
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) ->
                level.getBlockEntity(FireForgeBlock.mainPos(pos, state)) instanceof FireForgeBlockEntity forge
                        ? ForgeHopperAccess.forSide(forge, side) : null, FFBlocks.FIRE_FORGE.get());
    }
}
