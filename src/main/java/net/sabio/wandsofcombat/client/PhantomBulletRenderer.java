package net.sabio.wandsofcombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.entity.PhantomBulletEntity;
import org.joml.Quaternionf;

public class PhantomBulletRenderer extends EntityRenderer<PhantomBulletEntity, PhantomBulletRenderer.State> {
    private static final float PX = 1.0f / 16.0f;

    public static class State extends EntityRenderState {
        public float age;
        public boolean launched;
        public Vec3 originOffset = Vec3.ZERO;
        public Vec3 faceDir = Vec3.ZERO;
    }

    public PhantomBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PhantomBulletEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.launched = entity.isLaunched();
        state.originOffset = entity.origin == null ? Vec3.ZERO : entity.origin.subtract(entity.getPosition(partialTick));
        state.faceDir = Vec3.ZERO;
        if (entity.origin != null) {
            Entity t = entity.level().getEntity(entity.getTargetId());
            if (t != null) {
                Vec3 center = t.getPosition(partialTick).add(0, t.getBbHeight() * 0.5, 0);
                state.faceDir = center.subtract(entity.origin);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float grow = Math.min(1.0f, state.age / 8.0f);
        float pulse = 0.8f + 0.2f * Mth.sin(state.age / 0.5f);
        float flicker = 0.85f + 0.15f * Mth.sin(state.age * 1.7f);

        double away = state.originOffset.length();
        float holoFade = state.launched ? Math.max(0f, 1.0f - (float) away / 3.0f) : 1.0f;
        if (holoFade > 0.01f) {
            Quaternionf rot = new Quaternionf(camera.orientation);
            if (state.faceDir.lengthSqr() > 1.0E-6) {
                Vec3 d = state.faceDir.normalize();
                rot.rotationTo(0f, 0f, 1f, (float) d.x, (float) d.y, (float) d.z);
            }

            pose.pushPose();
            pose.translate(state.originOffset.x, state.originOffset.y, state.originOffset.z);
            drawQuad(pose, collector, Identifier.fromNamespaceAndPath("wandsofcombat", "textures/entity/stalk_halo.png"), rot, 32 * PX, 0.45f * holoFade * flicker * grow);
            drawQuad(pose, collector, Identifier.fromNamespaceAndPath("wandsofcombat", "textures/item/stalk_hologram.png"), rot, 21 * PX, 0.45f * holoFade * flicker * grow);
            pose.popPose();
        }

        pose.pushPose();
        drawCross(pose, collector, Identifier.fromNamespaceAndPath("wandsofcombat", "textures/entity/stalk_orb_glow.png"), state.faceDir, 16 * PX, 0.5f * (state.launched ? 0.9f : 0.6f) * pulse * grow);
        drawCross(pose, collector, Identifier.fromNamespaceAndPath("wandsofcombat", "textures/entity/stalk_bullet.png"), state.faceDir, 8 * PX, grow);
        pose.popPose();

        super.submit(state, pose, collector, camera);
    }

    private void drawCross(PoseStack pose, SubmitNodeCollector collector, Identifier texture, Vec3 faceDir, float size, float alpha) {
        float quarter = (float) (Math.PI / 2.0);
        Quaternionf base = new Quaternionf();
        if (faceDir.lengthSqr() > 1.0E-6) {
            Vec3 d = faceDir.normalize();
            base.rotationTo(0f, 0f, 1f, (float) d.x, (float) d.y, (float) d.z);
        }
        drawQuad(pose, collector, texture, new Quaternionf(base).rotateX(quarter), size, alpha);
        drawQuad(pose, collector, texture, new Quaternionf(base).rotateZ(quarter).rotateX(quarter), size, alpha);
        drawQuad(pose, collector, texture, base, size, alpha);
    }

    private void drawQuad(PoseStack pose, SubmitNodeCollector collector, Identifier texture, Quaternionf rotation, float size, float alpha) {
        PixelQuad.draw(pose, collector, texture, rotation, size, alpha);
    }
}