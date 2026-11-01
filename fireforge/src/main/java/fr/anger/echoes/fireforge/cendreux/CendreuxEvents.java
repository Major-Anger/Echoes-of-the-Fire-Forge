package fr.anger.echoes.fireforge.cendreux;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.witherset.AshDoubleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Toucher aux machines des Attiseurs (les casser, s'en servir) près d'un Cendreux le met en colère. Et le wither ne le touche pas. */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class CendreuxEvents {

    private CendreuxEvents() {}

    /** Comme les wither squelettes, le wither ne touche ni le Cendreux, ni le double du set du Wither. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if ((event.getEntity() instanceof CendreuxEntity || event.getEntity() instanceof AshDoubleEntity) && event.getEffectInstance().is(MobEffects.WITHER)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getState().is(CendreuxEntity.ATTISEUR_MACHINES)) {
            CendreuxEntity.provokeNear(level, event.getPos(), event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && level.getBlockState(event.getPos()).is(CendreuxEntity.ATTISEUR_MACHINES)) {
            CendreuxEntity.provokeNear(level, event.getPos(), event.getEntity());
        }
    }
}
