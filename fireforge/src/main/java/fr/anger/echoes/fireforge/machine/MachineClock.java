package fr.anger.echoes.fireforge.machine;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Le rythme de la Grande Machine (plan, 15b) : ses pistons et ses bouches de vapeur battent tous sur la même horloge.
 * Le régulateur (15c) la change : baissée, la machine s'arrête ; poussée, elle va une fois et demie plus vite.
 */
public final class MachineClock {

    private MachineClock() {}

    /** Les dégâts de la machine : la vapeur des bouches, les pistons qui retombent. */
    public static final ResourceKey<DamageType> STEAM = ResourceKey.create(Registries.DAMAGE_TYPE, EchoesFireForge.id("steam"));
    public static final ResourceKey<DamageType> PISTON = ResourceKey.create(Registries.DAMAGE_TYPE, EchoesFireForge.id("piston"));

    /** Pousser la pression : tout va une fois et demie plus vite (validé par Anger). */
    public static final int RAISED_NUM = 3, RAISED_DEN = 2;

    /** Le temps de la machine, en ticks : celui du monde, une fois et demie plus vite depuis qu'on a poussé la pression. */
    public static long ticks(ServerLevel level) {
        long now = level.getGameTime();
        GrandMachineState state = GrandMachineState.get(level);
        if (state.choice() != GrandMachineState.Choice.RAISED) return now;
        long since = Math.max(0, now - state.choiceTick());
        return state.choiceTick() + since * RAISED_NUM / RAISED_DEN;
    }

    /** La pression baissée : la machine s'est tue (pistons en bas, bouches muettes, le cœur sombre). */
    public static boolean stopped(ServerLevel level) {
        return GrandMachineState.get(level).choice() == GrandMachineState.Choice.LOWERED;
    }

    public static DamageSource damage(ServerLevel level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }
}
