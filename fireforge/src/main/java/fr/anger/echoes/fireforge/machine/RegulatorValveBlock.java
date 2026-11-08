package fr.anger.echoes.fireforge.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Une vanne du régulateur de la Grande Machine (plan, 15c) : baisser (−) ou pousser (+) la pression. Les deux sont
 * identiques, à leur plaque près : rien ne dit laquelle est la bonne. Le premier clic prévient, le second (dans les
 * 10 secondes) tourne la roue : le choix vaut pour tout le monde, et pour toujours. Bloc technique : pas d'objet.
 */
public class RegulatorValveBlock extends Block {

    public enum Kind implements StringRepresentable {
        LOWER("lower"), RAISE("raise");

        private final String key;

        Kind(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public static final MapCodec<RegulatorValveBlock> CODEC = simpleCodec(RegulatorValveBlock::new);
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final BooleanProperty TURNED = BooleanProperty.create("turned");
    public static final int CONFIRM_TICKS = 200;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 12, 13), Block.box(0, 12, 0, 16, 14, 16));

    private record Pending(BlockPos pos, long tick) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public RegulatorValveBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.LOWER).setValue(TURNED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, TURNED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        turn(server, pos, state, serverPlayer);
        return InteractionResult.CONSUME;
    }

    /** Public pour les tests : un clic sur la vanne. */
    public static void turn(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (GrandMachineState.get(level).choice() != GrandMachineState.Choice.NONE) {
            player.displayClientMessage(Component.translatable("message.echoes_fireforge.valve.locked").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long now = level.getGameTime();
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.pos().equals(pos) || now - pending.tick() > CONFIRM_TICKS) {
            PENDING.put(player.getUUID(), new Pending(pos.immutable(), now));
            player.displayClientMessage(Component.translatable("message.echoes_fireforge.valve.confirm").withStyle(ChatFormatting.GOLD), false);
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1.0F, 0.6F);
            return;
        }
        PENDING.remove(player.getUUID());
        MachineChoice.choose(level, pos, state.getValue(KIND));
    }
}
