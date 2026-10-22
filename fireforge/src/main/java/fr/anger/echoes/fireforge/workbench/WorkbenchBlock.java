package fr.anger.echoes.fireforge.workbench;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.forge.ForgeGeometry;
import fr.anger.echoes.fireforge.forge.ForgePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * L'Établi d'Attiseur : 2 blocs de long qui se posent d'un coup, comme la forge (et un lit). À gauche les tiroirs,
 * à droite le tableau à outils. On y greffe les mécanismes ; l'établi ne garde rien : ce qu'on y pose revient au
 * joueur quand il ferme l'interface, comme à une table de forgeron. Le bouilleur se pose à l'un de ses deux bouts.
 */
public class WorkbenchBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<WorkbenchBlock> CODEC = simpleCodec(WorkbenchBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ForgePart> PART = EnumProperty.create("part", ForgePart.class);
    private static final Component TITLE = Component.translatable("container.echoes_fireforge.workbench");

    public WorkbenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, ForgePart.LEFT));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite(); // l'avant face au joueur
        BlockPos other = ForgeGeometry.partner(context.getClickedPos(), facing, ForgePart.LEFT);
        Level level = context.getLevel();
        if (!level.getBlockState(other).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(other)) return null;
        return defaultBlockState().setValue(FACING, facing).setValue(PART, ForgePart.LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(ForgeGeometry.partner(pos, state.getValue(FACING), ForgePart.LEFT), state.setValue(PART, ForgePart.RIGHT), Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == ForgeGeometry.partnerDirection(state.getValue(FACING), state.getValue(PART))) {
            boolean partnerOk = neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART)
                    && neighbor.getValue(FACING) == state.getValue(FACING);
            return partnerOk ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Casser la moitié droite en survie : c'est la gauche (celle qui donne l'objet) qui tombe
        if (!level.isClientSide && !player.isCreative() && state.getValue(PART) == ForgePart.RIGHT) {
            BlockPos main = mainPos(pos, state);
            BlockState mainState = level.getBlockState(main);
            if (mainState.is(this)) level.destroyBlock(main, player.hasCorrectToolForDrops(mainState, level, main), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** La moitié gauche (les tiroirs). */
    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ForgePart.LEFT ? pos : ForgeGeometry.partner(pos, state.getValue(FACING), ForgePart.RIGHT);
    }

    /** La direction vers l'extérieur d'une moitié : c'est là, au bout de l'établi, que se pose le bouilleur. */
    public static Direction outward(BlockState state) {
        return ForgeGeometry.partnerDirection(state.getValue(FACING), state.getValue(PART)).getOpposite();
    }

    /** Le bouilleur branché à l'un des deux bouts de l'établi, ou null. */
    @Nullable
    public static SteamBoilerBlockEntity findBoiler(Level level, BlockPos main) {
        BlockState state = level.getBlockState(main);
        if (!(state.getBlock() instanceof WorkbenchBlock)) return null;
        for (BlockPos half : new BlockPos[]{main, ForgeGeometry.partner(main, state.getValue(FACING), ForgePart.LEFT)}) {
            BlockState halfState = level.getBlockState(half);
            if (!(halfState.getBlock() instanceof WorkbenchBlock)) continue;
            if (level.getBlockEntity(half.relative(outward(halfState))) instanceof SteamBoilerBlockEntity boiler) return boiler;
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) open(serverPlayer, level, mainPos(pos, state));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Ouvre l'établi, avec le panneau du bouilleur s'il y en a un de branché. */
    public static void open(ServerPlayer player, Level level, BlockPos main) {
        SteamBoilerBlockEntity boiler = findBoiler(level, main);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new WorkbenchMenu(id, inventory, ContainerLevelAccess.create(level, main), boiler), TITLE),
                buf -> {
                    buf.writeBoolean(boiler != null);
                    if (boiler != null) buf.writeBlockPos(boiler.getBlockPos());
                });
    }
}
