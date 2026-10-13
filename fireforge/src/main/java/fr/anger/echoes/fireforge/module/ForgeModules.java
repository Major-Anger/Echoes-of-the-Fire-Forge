package fr.anger.echoes.fireforge.module;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Les modules de la forge. Un autre mod peut ajouter les siens sans toucher à la forge : il enregistre son bloc
 * (pendant {@code FMLCommonSetupEvent}) avec le côté qu'il occupe et ce que chaque bloc de la pile apporte.
 * <pre>{@code
 * ForgeModules.register(MY_TANK.get(), new ForgeModules.Type(ModuleSide.LEFT, 2, 8000, 0));
 * }</pre>
 * La forge lit ensuite la pile posée contre ce côté (voir {@link ModuleLayout}).
 */
public final class ForgeModules {

    private ForgeModules() {}

    /**
     * @param side          le côté de la forge où le module se pose
     * @param maxHeight     le nombre de blocs empilés pris en compte
     * @param waterPerBlock l'eau (en mB) que chaque bloc ajoute à la réserve
     * @param lavaPerBlock  la lave (en mB) que chaque bloc ajoute à la réserve
     */
    public record Type(ModuleSide side, int maxHeight, int waterPerBlock, int lavaPerBlock) {}

    private static final Map<Block, Type> TYPES = new ConcurrentHashMap<>();

    public static void register(Block block, Type type) {
        TYPES.put(block, type);
    }

    @Nullable
    public static Type get(BlockState state) {
        return TYPES.get(state.getBlock());
    }
}
