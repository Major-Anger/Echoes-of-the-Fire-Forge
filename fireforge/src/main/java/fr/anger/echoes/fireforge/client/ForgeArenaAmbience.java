package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forgemaster.ForgeMasterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * L'ambiance de l'arène du maître de forge (plan, 14b) : près de lui, un brouillard couleur de braise et des cendres qui
 * flottent. Elle vient doucement, et s'en va de même quand on s'éloigne.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class ForgeArenaAmbience {

    private static final double RANGE = 40;
    private static float strength, previous;

    private ForgeArenaAmbience() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        previous = strength;
        if (player == null || mc.level == null || mc.isPaused()) return;
        boolean near = !mc.level.getEntitiesOfClass(ForgeMasterEntity.class, player.getBoundingBox().inflate(RANGE)).isEmpty();
        strength = Mth.clamp(strength + (near ? 0.02F : -0.02F), 0, 1);
        if (strength <= 0) return;
        for (int i = 0; i < 3 * strength; i++) {
            mc.level.addParticle(ParticleTypes.WHITE_ASH, player.getX() + (mc.level.random.nextDouble() - 0.5) * 24,
                    player.getY() + mc.level.random.nextDouble() * 10 - 2, player.getZ() + (mc.level.random.nextDouble() - 0.5) * 24, 0, -0.01, 0);
        }
    }

    private static float strength(double partialTick) {
        return (float) Mth.lerp(partialTick, previous, strength);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float s = strength(event.getPartialTick()) * 0.55F;
        if (s <= 0) return;
        event.setRed(Mth.lerp(s, event.getRed(), 0.32F));
        event.setGreen(Mth.lerp(s, event.getGreen(), 0.11F));
        event.setBlue(Mth.lerp(s, event.getBlue(), 0.05F));
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        float s = strength(event.getPartialTick());
        if (s <= 0) return;
        event.setNearPlaneDistance(Mth.lerp(s, event.getNearPlaneDistance(), 4));
        event.setFarPlaneDistance(Mth.lerp(s, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), 72)));
        event.setCanceled(true);
    }
}
