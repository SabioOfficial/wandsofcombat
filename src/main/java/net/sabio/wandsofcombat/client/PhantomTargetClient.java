package net.sabio.wandsofcombat.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.item.PhantomWandItem;
import net.sabio.wandsofcombat.util.PhantomTargeting;

public final class PhantomTargetClient {
    private static LivingEntity hovered;
    private static LivingEntity locked;
    private static boolean armed = true;
    private static boolean havePrev;
    private static float prevYaw;
    private static float prevPitch;

    private PhantomTargetClient() {}

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(PhantomTargetClient::tick);
    }

    public static LivingEntity displayTarget() {
        return locked != null ? locked : hovered;
    }

    public static boolean isLocked() {
        return locked != null;
    }

    private static boolean holdsWand(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof PhantomWandItem || player.getOffhandItem().getItem() instanceof PhantomWandItem;
    }

    private static void clear() {
        hovered = null;
        locked = null;
        armed = true;
        havePrev = false;
    }

    private static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gui.screen() != null || !holdsWand(player)) {
            clear();
            return;
        }

        boolean using = player.isUsingItem() && player.getUseItem().getItem() instanceof PhantomWandItem;
        if (!using) armed = true;

        double range = !using && player.isCrouching() ? PhantomTargeting.ULTIMATE_RANGE : PhantomTargeting.RANGE;
        LivingEntity looked = PhantomTargeting.findLookedAt(player, range);
        if (looked != null && (PhantomTargeting.isInvalidType(looked) || !player.hasLineOfSight(looked))) looked = null;

        if (locked != null && (!using || !canKeep(player, locked))) {
            if (using) armed = false;
            locked = null;
            havePrev = false;
        }
        if (locked == null && using && armed && looked != null) {
            locked = looked;
            havePrev = false;
        }

        if (locked != null) follow(player, locked);
        hovered = looked;
    }

    private static boolean canKeep(LocalPlayer player, LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && PhantomTargeting.distanceToBox(player.getEyePosition(), target.getBoundingBox()) <= PhantomTargeting.RANGE && player.hasLineOfSight(target);
    }

    private static void follow(LocalPlayer player, LivingEntity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 c = target.getBoundingBox().getCenter();
        double dx = c.x - eye.x, dy = c.y - eye.y, dz = c.z - eye.z;
        float yaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);

        if (havePrev) {
            player.setYRot(player.getYRot() + Mth.wrapDegrees(yaw - prevYaw));
            player.setXRot(Mth.clamp(player.getXRot() + (pitch - prevPitch), -90.0f, 90.0f));
        }
        prevYaw = yaw;
        prevPitch = pitch;
        havePrev = true;
    }
}
