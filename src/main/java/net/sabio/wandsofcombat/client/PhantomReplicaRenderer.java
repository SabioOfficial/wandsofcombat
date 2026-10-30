package net.sabio.wandsofcombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.sabio.wandsofcombat.entity.PhantomReplicaEntity;

public class PhantomReplicaRenderer extends EntityRenderer<PhantomReplicaEntity, PhantomReplicaRenderer.State> {
    public static class State extends EntityRenderState {
        public EntityRenderer<?, ?> ownerRenderer;
        public EntityRenderState ownerState;
    }

    public PhantomReplicaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void extractRenderState(PhantomReplicaEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ownerRenderer = null;
        state.ownerState = null;

        Entity owner = entity.level().getEntity(entity.getOwnerEntityId());
        if (!(owner instanceof AbstractClientPlayer player)) return;

        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        EntityRenderState ownerState = renderer.createRenderState();
        renderer.extractRenderState(player, ownerState, partialTick);

        ownerState.nameTag = null;
        ownerState.lightCoords = state.lightCoords;
        if (ownerState instanceof LivingEntityRenderState living) {
            living.bodyRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            living.yRot = 0.0f;
            living.xRot = 0.0f;
            living.walkAnimationPos = 0.0f;
            living.walkAnimationSpeed = 0.0f;
            living.deathTime = 0.0f;
            living.hasRedOverlay = false;
            living.pose = Pose.STANDING;
        }
        if (ownerState instanceof HumanoidRenderState humanoid) {
            humanoid.isCrouching = false;
        }

        state.ownerRenderer = renderer;
        state.ownerState = ownerState;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.ownerRenderer == null || state.ownerState == null) return;
        ((EntityRenderer) state.ownerRenderer).submit(state.ownerState, pose, collector, camera);
    }
}