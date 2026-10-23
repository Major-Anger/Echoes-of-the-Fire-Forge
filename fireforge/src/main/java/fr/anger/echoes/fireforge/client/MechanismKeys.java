package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.mechanism.Mechanism;
import fr.anger.echoes.fireforge.mechanism.MechanismEffects;
import fr.anger.echoes.fireforge.network.MechanismKeyPayload;
import fr.anger.echoes.fireforge.workbench.Steam;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Les touches des mécanismes d'armure (validé : C casque, B plastron, N jambières ; modifiables dans les Commandes).
 * Elles sont envoyées au serveur, qui décide. Deux choses restent côté client, parce que c'est lui qui calcule
 * la vue et le mouvement du joueur : le zoom de la Longue-vue et le vol du Planeur.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class MechanismKeys {

    private MechanismKeys() {}

    private static final String CATEGORY = "key.categories.echoes_fireforge";
    public static final KeyMapping HELMET = new KeyMapping("key.echoes_fireforge.helmet", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping CHEST = new KeyMapping("key.echoes_fireforge.chest", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping LEGS = new KeyMapping("key.echoes_fireforge.legs", GLFW.GLFW_KEY_N, CATEGORY);
    private static final KeyMapping[] KEYS = {HELMET, CHEST, LEGS};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS};
    private static final boolean[] WAS_DOWN = new boolean[3];

    /** Longue-vue : la vue est 4 fois plus serrée. */
    public static final double ZOOM = 0.25;
    /** Planeur : on ne tombe pas plus vite que ça ; une petite poussée vers l'avant à chaque tick, une bouffée par seconde. */
    public static final double GLIDE_FALL = -0.12, GLIDE_PUSH = 0.02, GLIDE_MAX = 0.45, GLIDE_PUFF = 0.22;
    private static int glideTicks;

    @EventBusSubscriber(modid = EchoesFireForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            for (KeyMapping key : KEYS) event.register(key);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        for (int i = 0; i < KEYS.length; i++) {
            while (KEYS[i].consumeClick()) {
                // les appuis sont suivis par l'état de la touche, ci-dessous
            }
            boolean down = KEYS[i].isDown() && mc.screen == null;
            if (down != WAS_DOWN[i]) {
                WAS_DOWN[i] = down;
                if (MechanismEffects.worn(player, SLOTS[i]) != null || !down) PacketDistributor.sendToServer(new MechanismKeyPayload(SLOTS[i], down));
            }
        }
        glide(player);
    }

    /** Planeur : chute ralentie et petites poussées, tant que B est tenue en l'air et qu'il reste de la vapeur. */
    private static void glide(LocalPlayer player) {
        boolean airborne = !player.onGround() && !player.getAbilities().flying && !player.isInWater() && !player.isFallFlying();
        if (!WAS_DOWN[1] || !airborne || MechanismEffects.worn(player, EquipmentSlot.CHEST) != Mechanism.PLANEUR
                || Steam.charges(player.getItemBySlot(EquipmentSlot.CHEST)) == 0) {
            glideTicks = 0;
            return;
        }
        Vec3 v = player.getDeltaMovement();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        double vx = v.x, vz = v.z, vy = Math.max(v.y, GLIDE_FALL);
        if (look.lengthSqr() > 1.0E-4) {
            look = look.normalize();
            vx += look.x * GLIDE_PUSH;
            vz += look.z * GLIDE_PUSH;
        }
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        if (horizontal > GLIDE_MAX) {
            vx *= GLIDE_MAX / horizontal;
            vz *= GLIDE_MAX / horizontal;
        }
        if (glideTicks++ % MechanismEffects.GLIDE_TICKS_PER_CHARGE == 0) vy = Math.max(vy, GLIDE_PUFF); // la bouffée qui coûte la charge
        player.setDeltaMovement(vx, vy, vz);
        player.fallDistance = 0;
    }

    /** Longue-vue : tant que C est tenue. */
    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        // la vue du monde seulement : la main garde son champ de vision
        if (event.usedConfiguredFov() && mc.player != null && WAS_DOWN[0] && MechanismEffects.worn(mc.player, EquipmentSlot.HEAD) == Mechanism.LONGUE_VUE) {
            event.setFOV(event.getFOV() * ZOOM);
        }
    }
}
