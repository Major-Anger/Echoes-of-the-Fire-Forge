package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * L'ambiance de la caverne de la Grande Machine (plan, 15b) : un brouillard plus clair et plus chaud, pour voir la
 * machine d'un bout à l'autre de sa caverne, des cendres qui flottent, et de temps en temps le ronronnement sourd de la
 * machine. Le serveur dit quand on y entre (MachineAmbiencePayload).
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class MachineAmbience {

    private static boolean inside;
    private static float strength, previous;
    private static int hum;

    private MachineAmbience() {}

    public static void set(boolean in) {
        inside = in;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        previous = strength;
        if (player == null || mc.level == null) {
            inside = false;
            strength = previous = 0;
            return;
        }
        if (mc.isPaused()) return;
        strength = Mth.clamp(strength + (inside ? 0.02F : -0.02F), 0, 1);
        if (strength <= 0) return;
        for (int i = 0; i < 2 * strength; i++) {
            mc.level.addParticle(ParticleTypes.WHITE_ASH, player.getX() + (mc.level.random.nextDouble() - 0.5) * 24,
                    player.getY() + mc.level.random.nextDouble() * 10 - 2, player.getZ() + (mc.level.random.nextDouble() - 0.5) * 24, 0, -0.01, 0);
        }
        if (inside && ++hum >= 100) {
            hum = 0;
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT,
                    0.45F * strength, 0.5F, false);
        }
    }

    private static float strength(double partialTick) {
        return (float) Mth.lerp(partialTick, previous, strength);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float s = strength(event.getPartialTick()) * 0.5F;
        if (s <= 0) return;
        event.setRed(Mth.lerp(s, event.getRed(), 0.30F));
        event.setGreen(Mth.lerp(s, event.getGreen(), 0.13F));
        event.setBlue(Mth.lerp(s, event.getBlue(), 0.06F));
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        float s = strength(event.getPartialTick());
        if (s <= 0) return;
        event.setNearPlaneDistance(Mth.lerp(s, event.getNearPlaneDistance(), 8));
        event.setFarPlaneDistance(Mth.lerp(s, event.getFarPlaneDistance(), Math.max(event.getFarPlaneDistance(), 120)));
        event.setCanceled(true);
    }
}
