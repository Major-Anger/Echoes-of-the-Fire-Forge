package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.anger.echoes.fireforge.witherset.AshDoubleEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.FastColor;

/** Le double : le modèle du joueur, son skin noirci comme un wither squelette ; en colère, il tend les bras. */
public class AshDoubleModel extends PlayerModel<AshDoubleEntity> {

    /** Le skin, noirci (multiplié par ce gris très sombre). */
    private static final int ASH_TINT = 0xFF3D3734;

    public AshDoubleModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AshDoubleEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        AnimationUtils.animateZombieArms(leftArm, rightArm, entity.isAggressive(), attackTime, ageInTicks);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        super.renderToBuffer(pose, buffer, light, overlay, FastColor.ARGB32.multiply(color, ASH_TINT));
    }
}
