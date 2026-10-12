package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
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
}
