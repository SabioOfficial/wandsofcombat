package net.sabio.wandsofcombat.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class PhantomTargeting {
    public static final double RANGE = 8.0;
    public static final double ULTIMATE_RANGE = 12.0;

    private PhantomTargeting() {}

    public static LivingEntity findLookedAt(Player player) {
        return findLookedAt(player, RANGE);
    }

    public static LivingEntity findLookedAt(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getLookAngle().scale(range);
        Vec3 end = eye.add(reach);
        AABB sweep = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity e : player.level().getEntities(player, sweep, ent -> ent instanceof LivingEntity && ent.isAlive() && !ent.isSpectator())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(e.getPickRadius()).clip(eye, end);
            if (hit.isEmpty()) continue;
            double dist = eye.distanceToSqr(hit.get());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = (LivingEntity) e;
            }
        }
        return nearest;
    }

    public static boolean isInvalidType(LivingEntity target) {
        EntityType<?> type = target.getType();
        if (type == EntityTypes.WITHER || type == EntityTypes.ENDER_DRAGON || type == EntityTypes.WARDEN) return true;
        return target instanceof Player p && p.isSpectator();
    }

    public static double distanceToBox(Vec3 point, AABB box) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
