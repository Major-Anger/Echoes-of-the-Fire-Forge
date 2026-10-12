package fr.anger.echoes.fireforge;

import com.mojang.logging.LogUtils;
import fr.anger.echoes.core.compat.EchoesMods;
import fr.anger.echoes.fireforge.compat.SiltCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/** Echoes of the Fire Forge : deuxième épisode de la série Echoes. Jouable seul (avec Echoes Core). */
@Mod(EchoesFireForge.MOD_ID)
public final class EchoesFireForge {

    public static final String MOD_ID = "echoes_fireforge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public EchoesFireForge(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);

        // Contenu croisé : seulement si l'épisode est installé
        if (EchoesMods.isLoaded(EchoesMods.SILT)) SiltCompat.init(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
