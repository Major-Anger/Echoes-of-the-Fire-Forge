package fr.anger.echoes.fireforge;

import com.mojang.logging.LogUtils;
import fr.anger.echoes.core.compat.EchoesMods;
import fr.anger.echoes.fireforge.compat.SiltCompat;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFBlocks;
import fr.anger.echoes.fireforge.registry.FFCreativeTab;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import fr.anger.echoes.fireforge.registry.FFItems;
import fr.anger.echoes.fireforge.registry.FFMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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
        FFCreativeTab.TABS.register(modBus);

        // Contenu croisé : seulement si l'épisode est installé
        if (EchoesMods.isLoaded(EchoesMods.SILT)) SiltCompat.init(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
