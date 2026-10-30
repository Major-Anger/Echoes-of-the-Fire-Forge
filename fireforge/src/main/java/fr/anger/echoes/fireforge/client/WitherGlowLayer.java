package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.anger.echoes.fireforge.witherset.WitherArmorItem;
import fr.anger.echoes.fireforge.witherset.WitherSet;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * La braise du set du Wither (dès le palier 2) luit dans le noir, comme celle du Cendreux : les mêmes modèles que
 * l'armure, avec la couche de braises, à pleine lumière.
 */
public class WitherGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private final HumanoidArmorModel<T> inner, outer;

    public WitherGlowLayer(RenderLayerParent<T, M> parent, HumanoidArmorModel<T> inner, HumanoidArmorModel<T> outer) {
        super(parent);
        this.inner = inner;
        this.outer = outer;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        float pulse = 0.75F + 0.25F * (float) Math.sin((ageInTicks + entity.getId() * 11) * 0.12F);
        int color = (Math.round(pulse * 255) << 24) | 0xFFFFFF;
        for (EquipmentSlot slot : WitherSet.SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!WitherSet.isPiece(stack) || WitherArmorItem.stage(stack) < 2) continue;
            boolean legs = slot == EquipmentSlot.LEGS;
            HumanoidArmorModel<T> model = legs ? inner : outer;
            getParentModel().copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (slot) {
                case HEAD -> { model.head.visible = true; model.hat.visible = true; }
                case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
                case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
                default -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
            }
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(WitherArmorItem.texture(WitherArmorItem.stage(stack), legs, true))),
                    0xF00000, OverlayTexture.NO_OVERLAY, color);
        }
    }
}
