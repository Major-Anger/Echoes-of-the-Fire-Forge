package fr.anger.echoes.fireforge.cendreux;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Le Cendreux entretient encore les feux : quand il est calme, il va rallumer les feux de camp éteints. */
public class RekindleGoal extends Goal {

    private static final int SEARCH_RADIUS = 10, SEARCH_HEIGHT = 3, GIVE_UP_TICKS = 200;

    private final CendreuxEntity cendreux;
    @Nullable
    private BlockPos fire;
    private int cooldown, ticks;

    public RekindleGoal(CendreuxEntity cendreux) {
        this.cendreux = cendreux;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean isDeadFire(BlockState state) {
        return state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT)
                && !state.getValue(CampfireBlock.LIT) && !state.getValue(CampfireBlock.WATERLOGGED);
    }

    @Override
    public boolean canUse() {
        if (cendreux.getTarget() != null) return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        cooldown = 60 + cendreux.getRandom().nextInt(60);
        fire = BlockPos.findClosestMatch(cendreux.blockPosition(), SEARCH_RADIUS, SEARCH_HEIGHT,
                pos -> isDeadFire(cendreux.level().getBlockState(pos))).orElse(null);
        return fire != null;
    }

    @Override
    public void start() {
        ticks = 0;
        if (fire != null) cendreux.getNavigation().moveTo(fire.getX() + 0.5, fire.getY(), fire.getZ() + 0.5, 0.9);
    }

    @Override
    public boolean canContinueToUse() {
        return fire != null && cendreux.getTarget() == null && ticks < GIVE_UP_TICKS && isDeadFire(cendreux.level().getBlockState(fire));
    }

    @Override
    public void tick() {
        if (fire == null) return;
        ticks++;
        cendreux.getLookControl().setLookAt(fire.getX() + 0.5, fire.getY() + 0.3, fire.getZ() + 0.5);
        if (cendreux.distanceToSqr(fire.getX() + 0.5, fire.getY() + 0.5, fire.getZ() + 0.5) > 2.5 * 2.5) {
            if (cendreux.getNavigation().isDone()) cendreux.getNavigation().moveTo(fire.getX() + 0.5, fire.getY(), fire.getZ() + 0.5, 0.9);
            return;
        }
        BlockState state = cendreux.level().getBlockState(fire);
        cendreux.level().setBlock(fire, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
        cendreux.level().gameEvent(cendreux, GameEvent.BLOCK_CHANGE, fire);
        cendreux.level().playSound(null, fire, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 0.8F);
        cendreux.cough();
        fire = null;
    }

    @Override
    public void stop() {
        fire = null;
        cendreux.getNavigation().stop();
    }
}
