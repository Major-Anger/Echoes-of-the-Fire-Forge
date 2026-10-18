package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forging.Special;
import fr.anger.echoes.fireforge.forging.SpecialEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Lunettes d'Attiseur : on voit clair dans la lave, mais le plein jour éblouit (hors du Nether). */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class SpecialFog {

    /** Dans la lave : environ 12 blocs au lieu de 1. */
    private static final float LAVA_SIGHT = 12.0F;
    /** En plein jour : le brouillard se rapproche d'environ 40 %. */
    private static final float DAYLIGHT_FACTOR = 0.6F;

    private SpecialFog() {}

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !SpecialEffects.wears(player, Special.LUNETTES)) return;
        if (event.getType() == FogType.LAVA) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(LAVA_SIGHT);
            event.setCanceled(true);
        } else if (event.getType() == FogType.NONE && SpecialEffects.inDaylight(player)) {
            event.scaleNearPlaneDistance(DAYLIGHT_FACTOR);
            event.scaleFarPlaneDistance(DAYLIGHT_FACTOR);
            event.setCanceled(true);
        }
    }
}
