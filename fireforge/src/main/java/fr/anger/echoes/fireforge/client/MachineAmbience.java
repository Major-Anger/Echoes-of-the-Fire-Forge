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
    /** Ce qu'on a fait de la machine : 0 rien, 1 la pression baissée (silence, brouillard moins chaud), 2 poussée. */
    private static byte choice;
    private static float strength, previous;
    private static int hum;

    private MachineAmbience() {}

    public static void set(boolean in, byte madeChoice) {
        inside = in;
        choice = madeChoice;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        previous = strength;
        if (player == null || mc.level == null) {
            inside = false;
            choice = 0;
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
        // le ronronnement : éteint quand la pression est baissée, plus fort et plus serré quand elle est poussée
        if (inside && choice != 1 && ++hum >= (choice == 2 ? 60 : 100)) {
            hum = 0;
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT,
                    (choice == 2 ? 0.75F : 0.45F) * strength, choice == 2 ? 0.6F : 0.5F, false);
        }
    }

    private static float strength(double partialTick) {
        return (float) Mth.lerp(partialTick, previous, strength);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float s = strength(event.getPartialTick()) * (choice == 2 ? 0.7F : 0.5F);
        if (s <= 0) return;
        // poussée : plus rouge ; baissée : la chaleur retombe, un gris de cendre
        float r = choice == 2 ? 0.42F : choice == 1 ? 0.20F : 0.30F;
        float g = choice == 2 ? 0.10F : choice == 1 ? 0.15F : 0.13F;
        float b = choice == 2 ? 0.04F : choice == 1 ? 0.12F : 0.06F;
        event.setRed(Mth.lerp(s, event.getRed(), r));
        event.setGreen(Mth.lerp(s, event.getGreen(), g));
        event.setBlue(Mth.lerp(s, event.getBlue(), b));
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
