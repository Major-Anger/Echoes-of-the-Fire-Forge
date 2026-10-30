package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Le modèle du Cendreux (validé sur la planche de la 12c) : membres d'os de 2 px, voûté, 37 px de haut.
 * Les boîtes et leurs coins de texture viennent de tools/textures/make_cendreux_textures.py (CENDREUX_PARTS),
 * passées dans le repère des modèles (y vers le bas, l'avant vers -z, les pieds à y = 24).
 */
public class CendreuxModel extends HierarchicalModel<CendreuxEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EchoesFireForge.id("cendreux"), "main");

    private static final float DEG = Mth.DEG_TO_RAD;
    // les poses de repos (le dos voûté, les bras qui pendent vers l'avant)
    private static final float BODY_X = 14 * DEG, HEAD_X = -8 * DEG, HEAD_Z = -6 * DEG;
    private static final float ARM_R_X = -20 * DEG, ARM_R_Z = 5 * DEG, ARM_L_X = -10 * DEG, ARM_L_Z = -4 * DEG;
    private static final float LEG_R_X = 6 * DEG, LEG_L_X = -6 * DEG;

    private final ModelPart root, body, head, armRight, armLeft, legRight, legLeft;

    public CendreuxModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.armRight = body.getChild("arm_r");
        this.armLeft = body.getChild("arm_l");
        this.legRight = root.getChild("leg_r");
        this.legLeft = root.getChild("leg_l");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition body = parts.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, -12, -2, 8, 12, 4), PartPose.offsetAndRotation(0, 7, 0, BODY_X, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.offsetAndRotation(0, -12, 0, HEAD_X, 0, HEAD_Z));
        // la sangle des lunettes, et les lunettes relevées sur le front
        head.addOrReplaceChild("strap", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4, -6, -4, 8, 2, 8, new CubeDeformation(0.4F)), PartPose.ZERO);
        head.addOrReplaceChild("lens_r", CubeListBuilder.create().texOffs(32, 10)
                .addBox(-3.5F, -7.2F, -5.2F, 3, 3, 1), PartPose.ZERO);
        head.addOrReplaceChild("lens_l", CubeListBuilder.create().texOffs(40, 10)
                .addBox(0.5F, -7.2F, -5.2F, 3, 3, 1), PartPose.ZERO);
        body.addOrReplaceChild("apron", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3, -3, -3, 6, 8, 1), PartPose.rotation(-12 * DEG, 0, 0));
        body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(-5, -11, 0, ARM_R_X, 0, ARM_R_Z));
        body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(0, 36)
                .addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(5, -11, 0, ARM_L_X, 0, ARM_L_Z));
        parts.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(16, 36)
                .addBox(-1, 0, -1, 2, 17, 2), PartPose.offsetAndRotation(-2, 7, 0, LEG_R_X, 0, 0));
        parts.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(24, 36)
                .addBox(-1, 0, -1, 2, 17, 2), PartPose.offsetAndRotation(2, 7, 0, LEG_L_X, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(CendreuxEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // la tête suit le regard, sans quitter sa pose penchée
        head.yRot = netHeadYaw * DEG;
        head.xRot = HEAD_X + headPitch * DEG * 0.6F;
        head.zRot = HEAD_Z;
        // la marche : des pas raides, les bras qui ballottent
        float swing = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        legRight.xRot = LEG_R_X + swing * 1.0F;
        legLeft.xRot = LEG_L_X - swing * 1.0F;
        float sway = Mth.sin(ageInTicks * 0.06F) * 0.05F;
        armRight.xRot = ARM_R_X - swing * 0.5F;
        armLeft.xRot = ARM_L_X + swing * 0.5F;
        armRight.zRot = ARM_R_Z + sway;
        armLeft.zRot = ARM_L_Z - sway;
        // en colère : le bras droit se lève pour lancer ses braises
        if (entity.isAggressive()) {
            armRight.xRot = -100 * DEG + Mth.sin(ageInTicks * 0.3F) * 0.1F;
            armRight.zRot = ARM_R_Z;
        }
        body.xRot = BODY_X + Mth.sin(ageInTicks * 0.05F) * 0.015F; // il respire mal
    }
}
