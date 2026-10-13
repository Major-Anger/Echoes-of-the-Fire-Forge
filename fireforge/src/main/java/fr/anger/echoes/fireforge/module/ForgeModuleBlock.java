package fr.anger.echoes.fireforge.module;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.forge.FireForgeBlock;
import fr.anger.echoes.fireforge.forge.FireForgeBlockEntity;
import fr.anger.echoes.fireforge.forge.ForgeRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Un module empilable de la forge (chaudière, réservoir de lave). Il ne garde rien lui-même : il agrandit la
 * réserve de la forge contre laquelle il est posé. Il prend l'orientation de cette forge, pour que ses tuyaux
 * aillent vers elle.
 */
public class ForgeModuleBlock extends Block {

    public static final MapCodec<ForgeModuleBlock> CODEC = simpleCodec(ForgeModuleBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ModuleSection> SECTION = EnumProperty.create("section", ModuleSection.class);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public ForgeModuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(SECTION, ModuleSection.BOTTOM));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SECTION);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ------------------------------------------------------------------ pose et empilement

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState below = level.getBlockState(pos.below());
        if (below.is(this)) {
            facing = below.getValue(FACING);           // on continue la pile
        } else {
            FireForgeBlockEntity forge = findForge(level, pos);
            if (forge != null) facing = forge.getBlockState().getValue(FireForgeBlock.FACING);
        }
        return section(defaultBlockState().setValue(FACING, facing), level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis() == Direction.Axis.Y ? section(state, level, pos) : state;
    }

    private BlockState section(BlockState state, LevelAccessor level, BlockPos pos) {
        boolean below = level.getBlockState(pos.below()).is(this), above = level.getBlockState(pos.above()).is(this);
        ModuleSection section = !below ? ModuleSection.BOTTOM : above ? ModuleSection.MIDDLE : ModuleSection.TOP;
        return state.setValue(SECTION, section);
    }

    // ------------------------------------------------------------------ la forge reliée

    /** Le bas de la pile dont fait partie ce bloc. */
    public static BlockPos base(BlockGetter level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        BlockPos base = pos;
        while (level.getBlockState(base.below()).is(block)) base = base.below();
        return base;
    }

    /** La forge contre laquelle cette pile est posée, à la bonne place pour son côté (sinon null). */
    @Nullable
    public static FireForgeBlockEntity findForge(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ForgeModules.Type type = ForgeModules.get(state);
        BlockPos base = state.getBlock() instanceof ForgeModuleBlock || type != null ? base(level, pos) : pos;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos next = base.relative(dir);
            BlockState neighbor = level.getBlockState(next);
            if (!(neighbor.getBlock() instanceof FireForgeBlock)) continue;
            BlockPos main = FireForgeBlock.mainPos(next, neighbor);
            if (!(level.getBlockEntity(main) instanceof FireForgeBlockEntity forge)) continue;
            Direction facing = neighbor.getValue(FireForgeBlock.FACING);
            for (ModuleSide side : ModuleSide.values()) {
                if (type != null && type.side() != side) continue;
                if (ModuleLayout.bases(main, facing, side).contains(base)) return forge;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ remplissage au seau

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ForgeModules.Type type = ForgeModules.get(state);
        int water = type != null && type.waterPerBlock() > 0 ? ForgeRules.waterAmount(stack) : 0;
        boolean lava = type != null && type.lavaPerBlock() > 0 && stack.is(Items.LAVA_BUCKET);
        if (water == 0 && !lava) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        FireForgeBlockEntity forge = findForge(level, pos);
        if (forge == null) {
            player.displayClientMessage(Component.translatable("message.echoes_fireforge.module.unlinked").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.CONSUME;
        }
        int amount = lava ? ForgeRules.BUCKET : water;
        // un seau ne se vide pas à moitié : il faut la place pour tout
        if ((lava ? forge.fillLava(amount, true) : forge.fillWater(amount, true)) < amount) {
            player.displayClientMessage(Component.translatable("message.echoes_fireforge.module.full").withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.CONSUME;
        }
        if (lava) forge.fillLava(amount, false); else forge.fillWater(amount, false);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, ForgeRules.emptyContainer(stack)));
        level.playSound(null, pos, lava ? SoundEvents.BUCKET_EMPTY_LAVA : stack.is(Items.POTION) ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }
}
