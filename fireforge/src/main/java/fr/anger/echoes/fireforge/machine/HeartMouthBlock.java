package fr.anger.echoes.fireforge.machine;

import com.mojang.serialization.MapCodec;
import fr.anger.echoes.fireforge.registry.FFItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * La bouche du cœur de la Grande Machine (plan, 15c), sous la boule. La pression poussée, le cœur y donne une Braise du
 * cœur par jour à chaque joueur. Bloc technique : pas d'objet, incassable.
 */
public class HeartMouthBlock extends Block {

    public static final MapCodec<HeartMouthBlock> CODEC = simpleCodec(HeartMouthBlock::new);
    /** Un jour du jeu, compté au temps du monde (le Nether n'a pas de jour à lui). */
    public static final long DAY = 24000;

    public HeartMouthBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        take(server, pos, serverPlayer);
        return InteractionResult.CONSUME;
    }

    /** Public pour les tests. @return true si le joueur a reçu une braise */
    public static boolean take(ServerLevel level, BlockPos pos, ServerPlayer player) {
        GrandMachineState state = GrandMachineState.get(level);
        String key = switch (state.choice()) {
            case NONE -> "message.echoes_fireforge.heart.nothing";
            case LOWERED -> "message.echoes_fireforge.heart.cold";
            case RAISED -> state.takeEmber(player.getUUID(), level.getGameTime() / DAY) ? null : "message.echoes_fireforge.heart.tomorrow";
        };
        if (key != null) {
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ItemStack ember = new ItemStack(FFItems.HEART_EMBER.get());
        if (!player.getInventory().add(ember)) player.drop(ember, false);
        player.displayClientMessage(Component.translatable("message.echoes_fireforge.heart.ember").withStyle(ChatFormatting.GOLD), true);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.02);
        return true;
    }
}
