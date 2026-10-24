package net.sabio.wandsofcombat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class FrozenIceRenderMixin<T extends net.minecraft.world.entity.LivingEntity, S extends LivingEntityRenderState, M extends net.minecraft.client.model.EntityModel<? super S>> {
    private static final Identifier ICE_SHELL_TEXTURE = Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "textures/entity/frozen_ice_shell.png");
    private static final float ALPHA = 0.5f;
    private static final float PADDING = 0.06f;
    private static final float SIZE_MARGIN = 1.1f;

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void wandsofcombat$renderFrozenIceShell(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (!state.isFullyFrozen) {
            return;
        }

        float width = state.boundingBoxWidth * SIZE_MARGIN + PADDING * 2f;
        float height = state.boundingBoxHeight * SIZE_MARGIN + PADDING * 2f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - state.bodyRot));
        poseStack.scale(width, height, width);
        poseStack.translate(-0.5, 0.0, -0.5);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(ICE_SHELL_TEXTURE), FrozenIceRenderMixin::wandsofcombat$buildShell);

        poseStack.popPose();
    }

    private static void wandsofcombat$buildShell(PoseStack.Pose pose, VertexConsumer buffer) {
        wandsofcombat$quad(pose, buffer, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1); // bottom
        wandsofcombat$quad(pose, buffer, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0); // top
        wandsofcombat$quad(pose, buffer, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1); // west
        wandsofcombat$quad(pose, buffer, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0); // east
        wandsofcombat$quad(pose, buffer, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0); // north
        wandsofcombat$quad(pose, buffer, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1); // south
    }

    private static void wandsofcombat$quad(PoseStack.Pose pose, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        int alpha = Math.round(ALPHA * 255f);
        int color = (alpha << 24) | (140 << 16) | (191 << 8) | 255;
        int overlay = OverlayTexture.NO_OVERLAY;
        buffer.addVertex(pose, x1, y1, z1).setColor(color).setUv(0f, 0f).setOverlay(overlay).setLight(15728880).setNormal(pose, 0f, 1f, 0f);
        buffer.addVertex(pose, x2, y2, z2).setColor(color).setUv(1f, 0f).setOverlay(overlay).setLight(15728880).setNormal(pose, 0f, 1f, 0f);
        buffer.addVertex(pose, x3, y3, z3).setColor(color).setUv(1f, 1f).setOverlay(overlay).setLight(15728880).setNormal(pose, 0f, 1f, 0f);
        buffer.addVertex(pose, x4, y4, z4).setColor(color).setUv(0f, 1f).setOverlay(overlay).setLight(15728880).setNormal(pose, 0f, 1f, 0f);
    }
}