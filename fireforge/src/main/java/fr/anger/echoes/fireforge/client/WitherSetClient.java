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

    /** De la cendre se détache du porteur, d'autant plus souvent que le palier est haut (on le lit sur ses pièces). */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        RandomSource random = mc.level.getRandom();
        for (AbstractClientPlayer player : mc.level.players()) {
            int stage = WitherSet.visibleStage(player);
            if (stage <= 0 || player.isInvisible() || random.nextFloat() >= stage * 0.07F) continue;
            mc.level.addParticle(stage >= 3 && random.nextInt(3) == 0 ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH,
                    player.getRandomX(0.5), player.getRandomY(), player.getRandomZ(0.5), 0, -0.02, 0);
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
