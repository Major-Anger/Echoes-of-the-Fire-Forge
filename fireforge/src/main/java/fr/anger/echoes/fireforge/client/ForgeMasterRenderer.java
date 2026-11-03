package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forgemaster.ForgeMasterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Le maître de forge (texture d'Anger), et ce qui luit dans le noir : les yeux, la braise de la poitrine, les verres. */
public class ForgeMasterRenderer extends MobRenderer<ForgeMasterEntity, ForgeMasterModel> {

    private static final ResourceLocation TEXTURE = EchoesFireForge.id("textures/entity/forge_master.png");
    private static final ResourceLocation GLOW = EchoesFireForge.id("textures/entity/forge_master_glow.png");

    public ForgeMasterRenderer(EntityRendererProvider.Context context) {
        super(context, new ForgeMasterModel(context.bakeLayer(ForgeMasterModel.LAYER)), 1.1F);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ForgeMasterEntity entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<ForgeMasterEntity, ForgeMasterModel> {

        Glow(RenderLayerParent<ForgeMasterEntity, ForgeMasterModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ForgeMasterEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float pulse = (entity.isPhaseTwo() ? 0.85F : 0.75F) + 0.15F * (float) Math.sin(ageInTicks * (entity.isPhaseTwo() ? 0.35F : 0.12F));
            int alpha = Math.round(Math.min(1, pulse) * 255);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF00000, OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF);
        }
    }
}
