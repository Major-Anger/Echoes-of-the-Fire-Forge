package fr.anger.echoes.fireforge.machine;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Le rythme de la Grande Machine (plan, 15b) : ses pistons et ses bouches de vapeur battent tous sur la même horloge.
 * Au 15c, le régulateur (baisser ou pousser la pression) changera cette horloge.
 */
public final class MachineClock {

    private MachineClock() {}

    /** Les dégâts de la machine : la vapeur des bouches, les pistons qui retombent. */
    public static final ResourceKey<DamageType> STEAM = ResourceKey.create(Registries.DAMAGE_TYPE, EchoesFireForge.id("steam"));
    public static final ResourceKey<DamageType> PISTON = ResourceKey.create(Registries.DAMAGE_TYPE, EchoesFireForge.id("piston"));

    /** Le temps de la machine, en ticks. */
    public static long ticks(ServerLevel level) {
        return level.getGameTime();
    }

    public static DamageSource damage(ServerLevel level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }
}
