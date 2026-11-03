package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forgemaster.ForgeMasterEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Le maître de forge : la géométrie vient du fichier Blockbench d'Anger (ForgeMasterGeometry, généré) ; ici, les
 * animations, réglées sur la pose d'Anger (le marteau levé devant lui) : il le lève plus haut, l'abat jusqu'au sol ;
 * le rouage tourne, s'emballe avant la grosse frappe.
 */
public class ForgeMasterModel extends HierarchicalModel<ForgeMasterEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EchoesFireForge.id("forge_master"), "main");

    private final ModelPart root, body, head, armRight, armLeft, legRight, legLeft, gear;

    public ForgeMasterModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.armRight = body.getChild("arm_r");
        this.armLeft = body.getChild("arm_l");
        this.legRight = root.getChild("leg_r");
        this.legLeft = root.getChild("leg_l");
        this.gear = armRight.getChild("hammer").getChild("gear");
    }

    public static LayerDefinition createLayer() {
        return ForgeMasterGeometry.createLayer();
    }

    @Override
    public ModelPart root() {
        return root;
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override
    public void setupAnim(ForgeMasterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.7F;
        // le pas, lourd et lent
        float walk = Mth.cos(limbSwing * 0.45F) * 0.9F * limbSwingAmount;
        legRight.xRot += walk;
        legLeft.xRot -= walk;
        armLeft.xRot -= walk * 0.6F;

        if (entity.isNoAi()) {                          // endormi sur son estrade : la tête basse
            head.xRot += 0.75F;
            body.xRot += 0.12F;
        }
        float t = entity.actionAge(ageInTicks);
        switch (entity.action()) {
            case SLAM_WINDUP -> {                       // il lève le marteau au-dessus de sa tête
                float f = ease(t / ForgeMasterEntity.SLAM_WINDUP_TICKS);
                armRight.xRot -= 2.7F * f;
                body.xRot -= 0.15F * f;
            }
            case SLAM -> {                              // et l'abat
                float down = ease(t / 3), back = ease((t - 6) / 8);
                armRight.xRot += -2.7F * (1 - down) + 1.5F * down * (1 - back);   // la tête du marteau jusqu'au sol, devant lui
                body.xRot += 0.35F * down * (1 - back);
            }
            case CHARGE -> {                            // le rouage s'emballe, il arme le coup
                float f = ease(t / 8);
                armRight.xRot -= 1.1F * f;
                armRight.zRot -= 0.22F * f;                  // un peu vers l'extérieur : le marteau frôle l'épaule, pas la tête
                armRight.xRot += Mth.sin(ageInTicks * 2.5F) * 0.04F * f;
            }
            case BIG_STRIKE -> {
                float hit = ease(t / 3), back = ease((t - 5) / 7);
                armRight.xRot += (-1.1F + 2.5F * hit) * (1 - back);
                body.xRot += 0.25F * hit * (1 - back);
                armRight.zRot -= 0.22F * (1 - hit);
                body.yRot -= 0.35F * hit * (1 - back);
            }
            case STEAM -> armRight.zRot += 0.25F * Mth.sin(t * 0.3F);
            default -> { }
        }
        // le coup au corps à corps
        if (attackTime > 0) {
            float swing = Mth.sin(attackTime * Mth.PI);
            armRight.xRot += 1.3F * swing;                                          // il abat le marteau devant lui
            body.yRot -= 0.25F * swing;
        }
        float spin = entity.action() == ForgeMasterEntity.Action.CHARGE ? 0.9F : entity.isPhaseTwo() ? 0.35F : 0.12F;
        gear.zRot = ageInTicks * spin;
    }
}
