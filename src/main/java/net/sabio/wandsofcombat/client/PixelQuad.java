package net.sabio.wandsofcombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;

final class PixelQuad {
    private PixelQuad() {}

    static void draw(PoseStack pose, SubmitNodeCollector collector, Identifier texture, Quaternionf rotation, float size, float alpha) {
        float half = size * 0.5f;
        pose.pushPose();
        pose.last().pose().rotate(rotation);
        collector.submitCustomGeometry(pose, RenderTypes.eyes(texture), (p, vc) -> quad(p, vc, half, alpha));
        pose.popPose();
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, float h, float alpha) {
        vertex(p, vc, -h, -h, 0f, 1f, alpha);
        vertex(p, vc, h, -h, 0f, 0f, alpha);
        vertex(p, vc, h, h, 1f, 0f, alpha);
        vertex(p, vc, -h, h, 1f, 1f, alpha);
        vertex(p, vc, -h, h, 1f, 1f, alpha);
        vertex(p, vc, h, h, 1f, 0f, alpha);
        vertex(p, vc, h, -h, 0f, 0f, alpha);
        vertex(p, vc, -h, -h, 0f, 1f, alpha);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer vc, float x, float y, float u, float v, float alpha) {
        vc.addVertex(p, x, y, 0f)
                .setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(p, 0f, 1f, 0f);
    }
}