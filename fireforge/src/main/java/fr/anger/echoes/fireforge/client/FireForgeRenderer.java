package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.anger.echoes.fireforge.forge.FireForgeBlock;
import fr.anger.echoes.fireforge.forge.FireForgeBlockEntity;
import fr.anger.echoes.fireforge.forge.ForgeGeometry;
import fr.anger.echoes.fireforge.forge.ForgePart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** L'objet à reforger, posé à plat sur l'enclume (la moitié droite de la forge). */
public class FireForgeRenderer implements BlockEntityRenderer<FireForgeBlockEntity> {

    private final BlockEntityRendererProvider.Context context;

    public FireForgeRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(FireForgeBlockEntity forge, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = forge.inventory().getStackInSlot(FireForgeBlockEntity.SLOT_ITEM);
        if (stack.isEmpty() || forge.getLevel() == null) return;
        Direction facing = forge.getBlockState().getValue(FireForgeBlock.FACING);
        BlockPos anvilPos = ForgeGeometry.partner(forge.getBlockPos(), facing, ForgePart.LEFT);
        // dessus de l'enclume : (8, 18, 9.5) dans le modèle de la moitié droite
        Vec3 spot = ForgeGeometry.point(anvilPos, facing, 8, 18.1, 9.5).subtract(Vec3.atLowerCornerOf(forge.getBlockPos()));

        pose.pushPose();
        pose.translate(spot.x, spot.y, spot.z);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.5F, 0.5F, 0.5F);
        int anvilLight = LevelRenderer.getLightColor(forge.getLevel(), anvilPos.above());
        context.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, anvilLight, overlay, pose, buffers, forge.getLevel(), (int) forge.getBlockPos().asLong());
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FireForgeBlockEntity forge) {
        BlockPos pos = forge.getBlockPos();
        return new AABB(pos).inflate(1.0, 1.0, 1.0);
    }
}
