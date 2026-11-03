package fr.anger.echoes.fireforge.forgemaster;

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
 * La forge du maître (plan, 14b) : un bloc technique caché sous l'âtre de l'arène (l'aspect d'une machine comprimée),
 * qu'on ne peut pas briser. Pas d'objet. C'est son entité de bloc qui mène l'arène.
 */
public class MasterForgeBlock extends BaseEntityBlock {

    public static final MapCodec<MasterForgeBlock> CODEC = simpleCodec(MasterForgeBlock::new);

    public MasterForgeBlock(Properties properties) {
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
        return new MasterForgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FFBlockEntities.MASTER_FORGE.get(), MasterForgeBlockEntity::serverTick);
    }
}
