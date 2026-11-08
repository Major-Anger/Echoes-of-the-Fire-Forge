package fr.anger.echoes.fireforge.machine;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Le moteur d'un piston géant de la Grande Machine (plan, 15b) : caché dans le corps du piston, il fait monter et
 * retomber sa tête. Bloc technique (l'aspect d'une machine comprimée) : pas d'objet, incassable.
 */
public class MachinePistonBlock extends BaseEntityBlock {

    public static final MapCodec<MachinePistonBlock> CODEC = simpleCodec(MachinePistonBlock::new);

    public MachinePistonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachinePistonBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FFBlockEntities.MACHINE_PISTON.get(), MachinePistonBlockEntity::serverTick);
    }
}
