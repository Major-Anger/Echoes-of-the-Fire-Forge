package fr.anger.echoes.fireforge.forge;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * La Forge des Attiseurs : un bloc de 2 de long qui se pose d'un coup, comme un lit.
 * La moitié gauche (le foyer et la cheminée) porte la logique ; la droite (l'enclume) renvoie vers elle.
 */
public class FireForgeBlock extends BaseEntityBlock {

    public static final MapCodec<FireForgeBlock> CODEC = simpleCodec(FireForgeBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ForgePart> PART = EnumProperty.create("part", ForgePart.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Le feu des âmes est choisi : le foyer passe au bleu (étape 5b). */
    public static final BooleanProperty SOUL = BooleanProperty.create("soul");

    public FireForgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, ForgePart.LEFT).setValue(LIT, false).setValue(SOUL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, LIT, SOUL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ------------------------------------------------------------------ pose et casse (comme un lit)

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite(); // l'avant de la forge face au joueur
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
            return partnerOk ? state.setValue(LIT, neighbor.getValue(LIT)).setValue(SOUL, neighbor.getValue(SOUL)) : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Casser l'enclume en survie : c'est le foyer (qui porte l'objet forge et le contenu) qui tombe
        if (!level.isClientSide && !player.isCreative() && state.getValue(PART) == ForgePart.RIGHT) {
            BlockPos main = ForgeGeometry.partner(pos, state.getValue(FACING), ForgePart.RIGHT);
            BlockState mainState = level.getBlockState(main);
            if (mainState.is(this)) level.destroyBlock(main, player.hasCorrectToolForDrops(mainState, level, main), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FireForgeBlockEntity forge) {
            for (int slot = 0; slot < forge.inventory().getSlots(); slot++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), forge.inventory().getStackInSlot(slot));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Position de la moitié qui porte la logique (le foyer). */
    public static BlockPos mainPos(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ForgePart.LEFT ? pos : ForgeGeometry.partner(pos, state.getValue(FACING), ForgePart.RIGHT);
    }

    // ------------------------------------------------------------------ interaction

    /** Clic droit : sur le soufflet, on l'actionne ; ailleurs, on ouvre la forge. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(mainPos(pos, state)) instanceof FireForgeBlockEntity forge) {
            if (isOnBellows(state, pos, hit.getLocation())) forge.blowBellows(player);
            else if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(forge, buf -> buf.writeBlockPos(forge.getBlockPos()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Le soufflet : sur la face avant de la moitié gauche, à gauche du foyer (x 1 à 7, y 2 à 8 dans le modèle). */
    public static boolean isOnBellows(BlockState state, BlockPos pos, Vec3 hit) {
        if (state.getValue(PART) != ForgePart.LEFT) return false;
        double[] m = ForgeGeometry.toModel(pos, state.getValue(FACING), hit);
        return m[0] >= 0.5 && m[0] <= 7.5 && m[1] >= 1.5 && m[1] <= 8.5 && m[2] >= 15.9;
    }

    // ------------------------------------------------------------------ logique

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ForgePart.LEFT ? new FireForgeBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(PART) != ForgePart.LEFT) return null;
        return createTickerHelper(type, FFBlockEntities.FIRE_FORGE.get(), FireForgeBlockEntity::serverTick);
    }

    // ------------------------------------------------------------------ forme

    /** Forme visée : le soufflet, qui dépasse sur l'avant, se vise et s'actionne. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape body = body(state);
        return state.getValue(PART) == ForgePart.LEFT
                ? Shapes.or(body, ForgeGeometry.box(state.getValue(FACING), 1, 2, 16, 7, 8, 21))  // soufflet
                : body;
    }

    /** Collisions : sans le soufflet, qui dépasse dans le bloc de devant. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return body(state);
    }

    private static VoxelShape body(BlockState state) {
        Direction facing = state.getValue(FACING);
        VoxelShape base = ForgeGeometry.box(facing, 0, 0, 0, 16, 12, 16);
        return state.getValue(PART) == ForgePart.LEFT
                ? Shapes.or(base, ForgeGeometry.box(facing, 3, 12, 2, 9, 16, 8))     // départ de la cheminée
                : Shapes.or(base, ForgeGeometry.box(facing, 3, 12, 6, 13, 16, 13));  // enclume
    }

    // ------------------------------------------------------------------ ambiance

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(PART) != ForgePart.LEFT) return;
        Direction facing = state.getValue(FACING);
        // fumée en haut de la cheminée
        Vec3 top = ForgeGeometry.point(pos, facing, 6, 30, 5);
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, top.x, top.y, top.z, 0, 0.05, 0);
        // flammes dans le foyer, centré sur les deux blocs (x = 16 dans le modèle de gauche)
        boolean soul = state.getValue(SOUL);
        for (int i = 0; i < 2; i++) {
            Vec3 fire = ForgeGeometry.point(pos, facing, 16 + (random.nextDouble() - 0.5) * 10, 2 + random.nextDouble() * 3, 15.5);
            var flame = soul ? ParticleTypes.SOUL_FIRE_FLAME : random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME;
            level.addParticle(flame, fire.x, fire.y, fire.z, 0, 0.01, 0);
        }
        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 0.9F, false);
        }
    }
}
