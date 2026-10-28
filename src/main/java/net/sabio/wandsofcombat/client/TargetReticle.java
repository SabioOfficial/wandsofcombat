package net.sabio.wandsofcombat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.Wandsofcombat;
import org.joml.Quaternionf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Optional;

public final class TargetReticle {
    private static final float PX = 1.0f / 16.0f;
    private static final float SCALE = 1.0f; // TODO: should be configurable :)
    private static boolean sizeLoaded;
    private static int texW = 16;
    private static int texH = 16;

    private TargetReticle() {}

    public static void submit(double x, double y, double z, float boxWidth, float boxHeight, PoseStack pose, SubmitNodeCollector collector) {
        LivingEntity target = PhantomTargetClient.displayTarget();
        Minecraft mc = Minecraft.getInstance();
        if (target == null || mc.player == null) return;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (Math.abs(Mth.lerp(pt, target.xo, target.getX()) - x) > 0.35 || Math.abs(Mth.lerp(pt, target.yo, target.getY()) - y) > 0.35 || Math.abs(Mth.lerp(pt, target.zo, target.getZ()) - z) > 0.35) {
            return;
        }
        if (!sizeLoaded) loadSize();

        Vec3 center = new Vec3(x, y + boxHeight * 0.5, z);
        Vec3 toEye = mc.player.getEyePosition(pt).subtract(center);
        if (toEye.lengthSqr() < 1.0E-4) return;
        Vec3 dir = toEye.normalize();
        double push = boxWidth * 0.5 + 0.25;

        boolean locked = PhantomTargetClient.isLocked();
        float alpha = locked ? 1.0f : 0.65f + 0.15f * Mth.sin((target.tickCount + pt) * 0.3f);
        int argb = (Math.round(alpha * 255f) << 24) | (locked ? 0xFFFFFF : 0xFFFFFF); // TODO: Add option to customize this?
        float hw = texW * PX * 0.25f * SCALE;
        float hh = texH * PX * 0.25f * SCALE;

        pose.pushPose();
        pose.translate(dir.x * push, boxHeight * 0.5 + dir.y * push, dir.z * push);
        pose.last().pose().rotate(new Quaternionf().rotationY((float) Math.atan2(dir.x, dir.z)));
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "textures/entity/target.png")), (p, vc) -> quad(p, vc, hw, hh, argb));
        pose.popPose();
    }

    private static void loadSize() {
        sizeLoaded = true;
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "textures/entity/target.png"));
            if (resource.isEmpty()) return;
            try (InputStream in = resource.get().open()) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    texW = img.getWidth();
                    texH = img.getHeight();
                }
            }
        } catch (Exception ignored) {

        }
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, float hw, float hh, int argb) {
        vertex(p, vc, -hw, -hh, 0f, 1f, argb);
        vertex(p, vc, hw, -hh, 1f, 1f, argb);
        vertex(p, vc, hw, hh, 1f, 0f, argb);
        vertex(p, vc, -hw, hh, 0f, 0f, argb);
        vertex(p, vc, -hw, hh, 0f, 0f, argb);
        vertex(p, vc, hw, hh, 1f, 0f, argb);
        vertex(p, vc, hw, -hh, 1f, 1f, argb);
        vertex(p, vc, -hw, -hh, 0f, 1f, argb);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer vc, float x, float y, float u, float v, int argb) {
        vc.addVertex(p, x, y, 0f).setColor(argb).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(p, 0f, 0f, 1f);
    }
}
