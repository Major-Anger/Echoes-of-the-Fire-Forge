package fr.anger.echoes.fireforge.workbench;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Le bouilleur : le module de l'établi, posé contre l'un de ses deux bouts. Il recharge en vapeur les pièces à
 * mécanisme ; son panneau s'ouvre sur le côté de l'interface de l'établi. Fermé, il marche aussi dans le Nether.
 */
public class SteamBoilerBlock extends BaseEntityBlock {

    public static final MapCodec<SteamBoilerBlock> CODEC = simpleCodec(SteamBoilerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SteamBoilerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** L'établi auquel ce bouilleur est branché (sa moitié gauche), ou null. */
    @Nullable
    public static BlockPos findWorkbench(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            BlockState state = level.getBlockState(next);
            if (state.getBlock() instanceof WorkbenchBlock && WorkbenchBlock.outward(state) == direction.getOpposite()) {
                return WorkbenchBlock.mainPos(next, state);
            }
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int water = ForgeRules.waterAmount(stack);
        if (water == 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SteamBoilerBlockEntity boiler)) return ItemInteractionResult.CONSUME;
        // un seau ne se vide pas à moitié : il faut la place pour tout
        if (boiler.fillWater(water, true) < water) {
            player.displayClientMessage(Component.translatable("message.echoes_fireforge.module.full").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.CONSUME;
        }
        boiler.fillWater(water, false);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, ForgeRules.emptyContainer(stack)));
        level.playSound(null, pos, stack.is(Items.POTION) ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }

    /** Sans rien en main : on ouvre l'établi (le panneau du bouilleur est sur le côté). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos bench = findWorkbench(level, pos);
        if (bench != null && player instanceof ServerPlayer serverPlayer) {
            WorkbenchBlock.open(serverPlayer, level, bench);
        } else {
            player.displayClientMessage(Component.translatable("message.echoes_fireforge.steam_boiler.unlinked").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SteamBoilerBlockEntity boiler) {
            for (int slot = 0; slot < boiler.inventory().getSlots(); slot++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), boiler.inventory().getStackInSlot(slot));
            }
            if (boiler.water() > 0) level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F); // l'eau s'en va en vapeur
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FFBlockEntities.STEAM_BOILER.get(), SteamBoilerBlockEntity::serverTick);
    }

    /** Il chauffe : un filet de vapeur au sifflet, le foyer qui crépite. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.04, 0);
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.1F, false);
        }
    }
}
