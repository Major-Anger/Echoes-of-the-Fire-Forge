package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.witherset.AshDoubleEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Le double, « Cendre d'<pseudo> » : le modèle du joueur (large ou fin, selon son skin), à la taille d'un wither
 * squelette (×1,2, 2,4 blocs), le skin noirci, le set du Wither et sa braise, deux yeux de braise.
 */
public class AshDoubleRenderer extends HumanoidMobRenderer<AshDoubleEntity, AshDoubleModel> {

    private static final ResourceLocation EYES = EchoesFireForge.id("textures/entity/ash_double_eyes.png");
    private static final float SCALE = 1.2F;

    private final AshDoubleModel wide, slim;

    public AshDoubleRenderer(EntityRendererProvider.Context context) {
        super(context, new AshDoubleModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F * SCALE);
        this.wide = model;
        this.slim = new AshDoubleModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new WitherGlowLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
        addLayer(new Eyes(this));
    }

    @Override
    public void render(AshDoubleEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = DoubleSkins.of(entity).model() == PlayerSkin.Model.SLIM ? slim : wide;
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    protected void scale(AshDoubleEntity entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(AshDoubleEntity entity) {
        return DoubleSkins.of(entity).texture();
    }

    /** Deux braises à la place des yeux, à pleine lumière. */
    private static final class Eyes extends RenderLayer<AshDoubleEntity, AshDoubleModel> {

        Eyes(RenderLayerParent<AshDoubleEntity, AshDoubleModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AshDoubleEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(EYES)), 0xF00000, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
    }
}
