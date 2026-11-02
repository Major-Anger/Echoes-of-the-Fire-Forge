package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * La porte scellée de la chaufferie : des machines comprimées (même aspect) qui ne s'ouvrent qu'à la vapeur, quand on
 * rend vie à la vieille chaudière d'à côté (de l'eau dans son chaudron, du feu dans son fourneau). Un seul bloc de la
 * porte, la serrure ({@link #LOCK}), surveille la chaudière. Pas d'objet : on ne la trouve que dans les chaufferies,
 * et brisée, elle ne donne rien.
 */
public class SealedDoorBlock extends BaseEntityBlock {

    public static final MapCodec<SealedDoorBlock> CODEC = simpleCodec(SealedDoorBlock::new);
    public static final BooleanProperty LOCK = BooleanProperty.create("lock");

    public SealedDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LOCK, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOCK);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(LOCK) ? new SealedDoorBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(LOCK) ? null
                : createTickerHelper(type, FFBlockEntities.SEALED_DOOR.get(), SealedDoorBlockEntity::serverTick);
    }
}
