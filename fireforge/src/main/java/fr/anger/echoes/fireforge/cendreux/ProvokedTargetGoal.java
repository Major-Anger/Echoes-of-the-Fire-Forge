package fr.anger.echoes.fireforge.cendreux;

import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Le Cendreux s'en prend au joueur qui a touché aux machines (voir {@link CendreuxEntity#provoke}). */
public class ProvokedTargetGoal extends TargetGoal {

    private final CendreuxEntity cendreux;

    public ProvokedTargetGoal(CendreuxEntity cendreux) {
        super(cendreux, false);
        this.cendreux = cendreux;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        Player player = cendreux.provoker();
        if (player == null || !canAttack(player, TargetingConditions.DEFAULT)) return false;
        targetMob = player;
        return true;
    }

    /** La colère retombée (30 s), le but s'arrête et lâche sa cible (sauf si un autre but la reprend, s'il a été frappé). */
    @Override
    public boolean canContinueToUse() {
        return cendreux.provoker() != null && super.canContinueToUse();
    }

    @Override
    public void start() {
        mob.setTarget(targetMob);
        super.start();
    }
}
