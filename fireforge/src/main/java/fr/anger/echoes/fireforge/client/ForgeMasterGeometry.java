package fr.anger.echoes.fireforge.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Généré par tools/models/make_forgemaster_model.py depuis le fichier Blockbench d'Anger
 * (art/forgemaster/maitre_de_forge.bbmodel) : ne pas retoucher à la main, relancer le script.
 */
@SuppressWarnings("unused")
final class ForgeMasterGeometry {

    private static final float DEG = Mth.DEG_TO_RAD;

    private ForgeMasterGeometry() {}

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(106, 78).addBox(-1.5F, -8F, -1.5F, 3F, 8F, 3F),
                PartPose.offsetAndRotation(0F, -4F, 0F, 0, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 57).addBox(-8F, -22F, -3.5F, 16F, 14F, 7F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition core = body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(82, 78).addBox(-4F, -18F, -1.5F, 8F, 8F, 4F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition pelvis = body.addOrReplaceChild("pelvis", CubeListBuilder.create().texOffs(0, 93).addBox(-6F, -2F, -3F, 12F, 5F, 6F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition belt = body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(76, 93).addBox(-6.5F, -2.2F, -3.5F, 13F, 2F, 7F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition buckle = body.addOrReplaceChild("buckle", CubeListBuilder.create().texOffs(96, 104).addBox(-1.5F, -2.5F, -4.5F, 3F, 3F, 1F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition pouch = body.addOrReplaceChild("pouch", CubeListBuilder.create().texOffs(62, 104).addBox(3F, 0F, -5.5F, 3F, 4F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 33).addBox(-6F, -12F, -6F, 12F, 12F, 12F),
                PartPose.offsetAndRotation(0F, -22F, 0F, 0, 0, 0));
        PartDefinition goggles = head.addOrReplaceChild("goggles", CubeListBuilder.create().texOffs(46, 57).addBox(-6F, -10F, -6F, 12F, 3F, 12F, new CubeDeformation(0.45F)),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition pauldron_r = body.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(94, 57).addBox(-13F, -24F, -4.5F, 7F, 6F, 9F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition pauldron_l = body.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(0, 78).addBox(6F, -24F, -4.5F, 7F, 6F, 9F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition arm_r = body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(12, 0).addBox(-1.5F, 0F, -1.5F, 3F, 28F, 3F),
                PartPose.offsetAndRotation(-11F, -20F, 0F, -6F * DEG, 0, 4F * DEG));
        PartDefinition hammer = arm_r.addOrReplaceChild("hammer", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -22F, -1.5F, 3F, 30F, 3F),
                PartPose.offsetAndRotation(0F, 23F, 0F, 6F * DEG, 0, -4F * DEG));
        PartDefinition counterweight = hammer.addOrReplaceChild("counterweight", CubeListBuilder.create().texOffs(36, 93).addBox(-2.5F, -27F, -2.5F, 5F, 5F, 5F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition counterweight_cap = hammer.addOrReplaceChild("counterweight_cap", CubeListBuilder.create().texOffs(72, 104).addBox(-1.5F, -29F, -1.5F, 3F, 2F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition hammer_head = hammer.addOrReplaceChild("hammer_head", CubeListBuilder.create().texOffs(48, 33).addBox(-9F, 7F, -6F, 16F, 12F, 12F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition strike_plate = hammer.addOrReplaceChild("strike_plate", CubeListBuilder.create().texOffs(68, 0).addBox(-11F, 6F, -7F, 2F, 14F, 14F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition mass = hammer.addOrReplaceChild("mass", CubeListBuilder.create().texOffs(32, 78).addBox(7F, 10F, -4.5F, 5F, 6F, 9F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition nozzle = hammer.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(56, 93).addBox(12F, 10.5F, -2.5F, 5F, 5F, 5F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition gear = hammer.addOrReplaceChild("gear", CubeListBuilder.create().texOffs(48, 104).addBox(-3F, -3F, -0.5F, 6F, 6F, 1F),
                PartPose.offsetAndRotation(3F, 12F, -6.5F, 0, 0, 0));
        PartDefinition bolt = hammer.addOrReplaceChild("bolt", CubeListBuilder.create().texOffs(0, 104).addBox(-3F, 4F, -3F, 6F, 3F, 6F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition bolt_low = hammer.addOrReplaceChild("bolt_low", CubeListBuilder.create().texOffs(24, 104).addBox(-3F, 19F, -3F, 6F, 3F, 6F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition bolt_cap = hammer.addOrReplaceChild("bolt_cap", CubeListBuilder.create().texOffs(84, 104).addBox(-1.5F, 22F, -1.5F, 3F, 2F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0, 0, 0));
        PartDefinition arm_l = body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-1.5F, 0F, -1.5F, 3F, 28F, 3F).mirror(false),
                PartPose.offsetAndRotation(11F, -20F, 0F, 4F * DEG, 0, -5F * DEG));
        PartDefinition leg_r = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(36, 0).addBox(-2F, 0F, -2F, 4F, 26F, 4F),
                PartPose.offsetAndRotation(-4F, -2F, 0F, 0, 0, 0));
        PartDefinition leg_l = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-2F, 0F, -2F, 4F, 26F, 4F).mirror(false),
                PartPose.offsetAndRotation(4F, -2F, 0F, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
