package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.mechanism.SteamLamp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Le faisceau de la Lampe à vapeur : un cône de lumière blanche et chaude qui pâlit au loin, dessiné pour chaque
 * porteur (tous les joueurs le voient). Il est rendu comme un faisceau de balise : avec Iris, il passe par le
 * programme {@code gbuffers_beaconbeam} du pack de shaders, qui peut lui donner son style (la brume, la diffusion).
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class LampBeamRender {

    private LampBeamRender() {}

    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/entity/lamp_beam.png");
    public static final float LENGTH = 10F, RADIUS_START = 0.12F, RADIUS_END = 1.7F, ALPHA = 0.45F;
    private static final int SIDES = 12, SEGMENTS = 6;

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.beaconBeam(TEXTURE, true);
        boolean any = false;
        for (AbstractClientPlayer player : mc.level.players()) {
            if (!SteamLamp.isOn(player) || player.isInvisible()) continue;
            // pour soi, en vue à la première personne, pas de cône : on le verrait par le bout. La lumière sur les murs
            // suffit (et un pack de shaders peut y ajouter la brume)
            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            Vec3 look = player.getViewVector(partial);
            Vec3 from = player.getEyePosition(partial).add(look.scale(0.25));
            float scroll = (mc.level.getGameTime() + partial) * 0.01F;
            cone(pose, buffers.getBuffer(type), from, look, ALPHA, scroll);
            any = true;
        }
        if (!any) return;
        // les sommets sont déjà dans le repère de la caméra
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();
        buffers.endBatch(type);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void cone(PoseStack pose, VertexConsumer buffer, Vec3 from, Vec3 dir, float alpha, float scroll) {
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(up).normalize(), v = dir.cross(u).normalize();
        PoseStack.Pose last = pose.last();
        for (int s = 0; s < SEGMENTS; s++) {
            float t0 = (float) s / SEGMENTS, t1 = (float) (s + 1) / SEGMENTS;
            float r0 = RADIUS_START + (RADIUS_END - RADIUS_START) * t0, r1 = RADIUS_START + (RADIUS_END - RADIUS_START) * t1;
            float a0 = alpha * (float) Math.pow(1 - t0, 1.6), a1 = alpha * (float) Math.pow(1 - t1, 1.6);
            Vec3 c0 = from.add(dir.scale(LENGTH * t0)), c1 = from.add(dir.scale(LENGTH * t1));
            for (int i = 0; i < SIDES; i++) {
                double ang0 = Math.PI * 2 * i / SIDES, ang1 = Math.PI * 2 * (i + 1) / SIDES;
                Vec3 n0 = u.scale(Math.cos(ang0)).add(v.scale(Math.sin(ang0))), n1 = u.scale(Math.cos(ang1)).add(v.scale(Math.sin(ang1)));
                Vec3 p00 = c0.add(n0.scale(r0)), p01 = c0.add(n1.scale(r0)), p10 = c1.add(n0.scale(r1)), p11 = c1.add(n1.scale(r1));
                float uu0 = (float) i / SIDES, uu1 = (float) (i + 1) / SIDES, vv0 = t0 + scroll, vv1 = t1 + scroll;
                // les deux faces : le faisceau se voit de dehors comme de dedans
                quad(buffer, last, p00, p10, p11, p01, uu0, uu1, vv0, vv1, a0, a1, n0);
                quad(buffer, last, p01, p11, p10, p00, uu1, uu0, vv0, vv1, a0, a1, n0.scale(-1));
            }
        }
    }

    private static void quad(VertexConsumer b, PoseStack.Pose pose, Vec3 a, Vec3 bb, Vec3 c, Vec3 d,
                             float u0, float u1, float v0, float v1, float alpha0, float alpha1, Vec3 n) {
        vertex(b, pose, a, u0, v0, alpha0, n);
        vertex(b, pose, bb, u0, v1, alpha1, n);
        vertex(b, pose, c, u1, v1, alpha1, n);
        vertex(b, pose, d, u1, v0, alpha0, n);
    }

    private static void vertex(VertexConsumer b, PoseStack.Pose pose, Vec3 p, float u, float v, float alpha, Vec3 n) {
        b.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(1.0F, 0.94F, 0.8F, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }
}
