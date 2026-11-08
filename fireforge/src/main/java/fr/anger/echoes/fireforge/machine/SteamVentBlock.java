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
 * La bouche de vapeur de la Grande Machine (plan, 15b) : dans le sol de l'étage des pistons. Elle siffle, puis crache un
 * jet de vapeur, à son heure. Bloc technique : pas d'objet, incassable.
 */
public class SteamVentBlock extends BaseEntityBlock {

    public static final MapCodec<SteamVentBlock> CODEC = simpleCodec(SteamVentBlock::new);

    public SteamVentBlock(Properties properties) {
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
        return new SteamVentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FFBlockEntities.STEAM_VENT.get(), SteamVentBlockEntity::serverTick);
    }
}
