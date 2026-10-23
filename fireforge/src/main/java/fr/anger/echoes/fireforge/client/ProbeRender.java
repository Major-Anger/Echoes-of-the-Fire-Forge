package fr.anger.echoes.fireforge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.mechanism.ProbeMarks;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/** La Sonde de prospection : les minerais trouvés brillent à travers la roche, d'une lueur de laiton qui pâlit en 5 s. */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, value = Dist.CLIENT)
public final class ProbeRender {

    private ProbeRender() {}

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        List<BlockPos> ores = ProbeMarks.ores();
        if (ores.isEmpty()) return;
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f matrix = new Matrix4f(event.getModelViewMatrix()).translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
        float alpha = 0.25F + 0.75F * ProbeMarks.fade();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (BlockPos pos : ores) box(buffer, matrix, pos, alpha);
        MeshData mesh = buffer.build();
        if (mesh != null) {
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.applyModelViewMatrix();
            BufferUploader.drawWithShader(mesh);
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void box(BufferBuilder buffer, Matrix4f m, BlockPos pos, float alpha) {
        float x0 = pos.getX() + 0.02F, y0 = pos.getY() + 0.02F, z0 = pos.getZ() + 0.02F, x1 = x0 + 0.96F, y1 = y0 + 0.96F, z1 = z0 + 0.96F;
        float[][] corners = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}, {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            for (int c : e) buffer.addVertex(m, corners[c][0], corners[c][1], corners[c][2]).setColor(1.0F, 0.78F, 0.32F, alpha);
        }
    }
}
