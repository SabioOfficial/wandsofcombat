package net.sabio.wandsofcombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.sabio.wandsofcombat.entity.StalkSigilEntity;
import org.joml.Quaternionf;

public class StalkSigilRenderer extends EntityRenderer<StalkSigilEntity, StalkSigilRenderer.State> {
    private static final boolean SMOOTH_SPIN = true;

    public static class State extends EntityRenderState {
        public float age;
        public int life;
        public boolean large;
    }

    public StalkSigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("wandsofcombat", path);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StalkSigilEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.life = entity.getLife();
        state.large = entity.isLarge();
    }

    private static float spinAngle(float age, int life) {
        float t = Math.min(age, (float) life);
        float l = Math.max(1, life);
        float extra = t * 0.5f - l / (4.0f * (float) Math.PI) * Mth.sin(2.0f * (float) Math.PI * t / l);
        return 0.03f * t + (0.5f - 0.03f) * extra;
    }

    private static float snap(float angle) {
        if (SMOOTH_SPIN) return angle;
        float quarter = (float) (Math.PI / 2.0);
        return Math.round(angle / quarter) * quarter;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float fadeIn = Math.min(1.0f, state.age / StalkSigilEntity.FADE_IN);
        float fadeOut = Math.clamp((state.life - state.age) / StalkSigilEntity.FADE_OUT, 0f, 1.0f);
        float alpha = fadeIn * fadeOut;
        if (alpha < 0.01f) return;
        float pulse = 0.8f + 0.2f * Mth.sin(state.age * 0.35f);

        float outerPx = state.large ? 48 : 32;
        float innerPx = state.large ? 30 : 20;

        Quaternionf flat = new Quaternionf().rotationX((float) (-Math.PI / 2.0));
        float spin = spinAngle(state.age, state.life);
        Quaternionf wheel = new Quaternionf(flat).rotateZ(snap(spin));
        Quaternionf ring = new Quaternionf(flat).rotateZ(snap(spin * -0.5f));

        pose.pushPose();
        pose.translate(0.0, 0.02f, 0.0);
        PixelQuad.draw(pose, collector, state.large ? id("textures/entity/stalk_sigil_large.png") : id("textures/entity/stalk_sigil_small.png"), ring, outerPx * (1.0f / 16.0f), alpha * pulse);
        pose.translate(0.0, 0.02f, 0.0);
        PixelQuad.draw(pose, collector, state.large ? id("textures/entity/stalk_sigil_inner_large.png") : id("textures/entity/stalk_sigil_inner_small.png"), wheel, innerPx * (1.0f / 16.0f), alpha);
        pose.popPose();

        super.submit(state, pose, collector, camera);
    }
}