package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFEntities;
import fr.anger.echoes.fireforge.registry.FFMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FFClient {

    private FFClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FFMenus.FIRE_FORGE.get(), FireForgeScreen::new);
        event.register(FFMenus.WORKBENCH.get(), WorkbenchScreen::new);
        event.register(FFMenus.PIECE_STORAGE.get(), PieceStorageScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FFBlockEntities.FIRE_FORGE.get(), FireForgeRenderer::new);
        event.registerEntityRenderer(FFEntities.CENDREUX.get(), CendreuxRenderer::new);
        event.registerEntityRenderer(FFEntities.ASH_DOUBLE.get(), AshDoubleRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CendreuxModel.LAYER, CendreuxModel::createLayer);
    }
}
