package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.FireForgeBlock;
import fr.anger.echoes.fireforge.module.ForgeModuleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FFBlocks {

    private FFBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EchoesFireForge.MOD_ID);

    /**
     * Une machine des Attiseurs écrasée par la chaleur et la pression, moins abîmée que les débris antiques :
     * on peut encore en tirer des pièces. Aussi dure que les débris, il faut une pioche en diamant.
     */
    public static final DeferredBlock<Block> COMPRESSED_MACHINE = BLOCKS.register("compressed_machine",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(30.0F, 1200.0F)
                    .sound(SoundType.ANCIENT_DEBRIS)));

    /** La Forge des Attiseurs (2 blocs). Le foyer éclaire quand elle travaille. */
    public static final DeferredBlock<FireForgeBlock> FIRE_FORGE = BLOCKS.register("fire_forge",
            () -> new FireForgeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(FireForgeBlock.LIT) ? 13 : 0)));

    /** Module de gauche : la chaudière, fermée (elle marche aussi dans le Nether). Empilable sur 3 blocs. */
    public static final DeferredBlock<ForgeModuleBlock> BOILER = BLOCKS.register("boiler",
            () -> new ForgeModuleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.COPPER)
                    .noOcclusion()));

    /** Module de l'arrière : le réservoir de lave, qui nourrit la chaleur Fournaise. Empilable sur 2 blocs. */
    public static final DeferredBlock<ForgeModuleBlock> LAVA_TANK = BLOCKS.register("lava_tank",
            () -> new ForgeModuleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .lightLevel(state -> 6)));
}
