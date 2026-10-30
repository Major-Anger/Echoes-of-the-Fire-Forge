package fr.anger.echoes.fireforge;

import com.mojang.logging.LogUtils;
import fr.anger.echoes.core.compat.EchoesMods;
import fr.anger.echoes.fireforge.compat.SiltCompat;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import fr.anger.echoes.fireforge.module.ForgeModules;
import fr.anger.echoes.fireforge.network.FFNetwork;
import fr.anger.echoes.fireforge.progress.ForgeEchoes;
import fr.anger.echoes.fireforge.module.ModuleSide;
import fr.anger.echoes.fireforge.registry.FFArmorMaterials;
import fr.anger.echoes.fireforge.registry.FFAttachments;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFBlocks;
import fr.anger.echoes.fireforge.registry.FFCreativeTab;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import fr.anger.echoes.fireforge.registry.FFEntities;
import fr.anger.echoes.fireforge.registry.FFItems;
import fr.anger.echoes.fireforge.registry.FFMenus;
import fr.anger.echoes.fireforge.registry.FFSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** Echoes of the Fire Forge : deuxième épisode de la série Echoes. Jouable seul (avec Echoes Core). */
@Mod(EchoesFireForge.MOD_ID)
public final class EchoesFireForge {

    public static final String MOD_ID = "echoes_fireforge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoesFireForge(IEventBus modBus) {
        FFDataComponents.COMPONENTS.register(modBus);
        FFBlocks.BLOCKS.register(modBus);
        FFItems.ITEMS.register(modBus);
        FFBlockEntities.BLOCK_ENTITIES.register(modBus);
        FFMenus.MENUS.register(modBus);
        FFEntities.ENTITIES.register(modBus);
        FFSounds.SOUNDS.register(modBus);
        FFArmorMaterials.MATERIALS.register(modBus);
        FFAttachments.ATTACHMENTS.register(modBus);
        FFCreativeTab.TABS.register(modBus);
        modBus.addListener(FFNetwork::register);
        ForgeEchoes.register(); // les Échos de la Forge (plan, section 12a)
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(EchoesFireForge::registerModules));

        // Contenu croisé : seulement si l'épisode est installé
        if (EchoesMods.isLoaded(EchoesMods.SILT)) SiltCompat.init(modBus);
    }

    /** Les modules de la forge (étape 5). D'autres mods peuvent ajouter les leurs de la même façon. */
    private static void registerModules() {
        ForgeModules.register(FFBlocks.BOILER.get(), new ForgeModules.Type(ModuleSide.LEFT,
                ForgeRules.BOILER_MAX_HEIGHT, ForgeRules.BOILER_WATER_PER_BLOCK, 0));
        ForgeModules.register(FFBlocks.LAVA_TANK.get(), new ForgeModules.Type(ModuleSide.BACK,
                ForgeRules.LAVA_TANK_MAX_HEIGHT, 0, ForgeRules.LAVA_TANK_PER_BLOCK));
        ForgeModules.register(FFBlocks.SOUL_MODULE.get(), new ForgeModules.Type(ModuleSide.RIGHT, 1, 0, 0, true));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
