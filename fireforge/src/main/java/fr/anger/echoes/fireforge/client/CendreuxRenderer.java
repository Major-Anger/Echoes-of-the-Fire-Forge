package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Le Cendreux, et sa couche de braises (yeux, poitrine, une veine du bras) qui luit dans le noir. */
public class CendreuxRenderer extends MobRenderer<CendreuxEntity, CendreuxModel> {

    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/entity/cendreux.png");
    private static final ResourceLocation GLOW = EchoesFireForge.id("textures/entity/cendreux_glow.png");

    public CendreuxRenderer(EntityRendererProvider.Context context) {
        super(context, new CendreuxModel(context.bakeLayer(CendreuxModel.LAYER)), 0.5F);
        addLayer(new Embers(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CendreuxEntity entity) {
        return TEXTURE;
    }

    /** Les braises : comme les yeux des araignées, à pleine lumière ; elles palpitent un peu. */
    private static final class Embers extends RenderLayer<CendreuxEntity, CendreuxModel> {

        Embers(RenderLayerParent<CendreuxEntity, CendreuxModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CendreuxEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer buffer = buffers.getBuffer(RenderType.eyes(GLOW));
            float pulse = 0.8F + 0.2F * (float) Math.sin((ageInTicks + entity.getId() * 7) * 0.15F);
            int alpha = Math.round(pulse * 255);
            getParentModel().renderToBuffer(pose, buffer, 0xF00000, OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF);
        }
    }
}
