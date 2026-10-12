package fr.anger.echoes.core;

import com.mojang.logging.LogUtils;
import fr.anger.echoes.core.network.CoreNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Echoes Core : la bibliothèque commune à tous les épisodes de la série Echoes.
 * Elle n'ajoute aucun objet en jeu ; elle porte ce que les épisodes partagent :
 *  - les âmes ({@link fr.anger.echoes.core.soul.Souls}) : une seule jauge pour toute la série ;
 *  - les Échos ({@link fr.anger.echoes.core.echo.Echoes}) : des succès qui laissent un bonus permanent ;
 *  - le lore ({@link fr.anger.echoes.core.lore.Lore}) : ce que le joueur a lu, tous épisodes confondus ;
 *  - la détection des autres épisodes ({@link fr.anger.echoes.core.compat.EchoesMods}).
 */
@Mod(EchoesCore.MOD_ID)
public final class EchoesCore {

    public static final String MOD_ID = "echoes_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoesCore(IEventBus modBus, ModContainer container) {
        CoreAttachments.ATTACHMENT_TYPES.register(modBus);
        modBus.addListener(CoreNetwork::register);
        container.registerConfig(ModConfig.Type.COMMON, CoreConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
