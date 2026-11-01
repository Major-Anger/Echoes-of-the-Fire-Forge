package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.witherset.WitherSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Côté client, les signes du set du Wither : la cendre qui tombe du porteur, et la braise qui luit. */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class WitherSetClient {

    private WitherSetClient() {}

    /**
     * De la cendre se détache du porteur, d'autant plus souvent que le palier est haut (on le lit sur ses pièces). Au
     * palier 4, pendant les 30 dernières minutes, elle s'épaissit en nuage autour de lui (étape 13c).
     */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        RandomSource random = mc.level.getRandom();
        for (AbstractClientPlayer player : mc.level.players()) {
            int stage = WitherSet.visibleStage(player);
            if (stage <= 0 || player.isInvisible()) continue;
            int doom = WitherSet.visibleDoom(player);
            // jusqu'à 7 ou 8 grains de cendre par tick dans la dernière minute, en nuage d'un mètre et demi autour de lui
            float rate = stage * 0.07F + (doom >= 0 ? doom * 0.25F : 0);
            double spread = 0.5 + (doom >= 0 ? doom / (double) WitherSet.DOOM_MINUTES * 1.0 : 0);
            int count = (int) rate + (random.nextFloat() < rate - (int) rate ? 1 : 0);
            for (int i = 0; i < count; i++) {
                mc.level.addParticle(stage >= 3 && random.nextInt(3) == 0 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH,
                        player.getX() + (random.nextDouble() * 2 - 1) * spread, player.getY() + random.nextDouble() * (player.getBbHeight() + 0.4),
                        player.getZ() + (random.nextDouble() * 2 - 1) * spread, (random.nextDouble() - 0.5) * 0.02, -0.02, (random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    @EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Layers {

        private Layers() {}

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new WitherGlowLayer<>(renderer,
                            new HumanoidArmorModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                            new HumanoidArmorModel<>(event.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
                }
            }
        }
    }
}
