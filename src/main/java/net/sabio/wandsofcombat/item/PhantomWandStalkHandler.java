package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;

import java.util.*;

public class PhantomWandStalkHandler {
    // Configuration
    private static final double RANGE = 8.0;
    private static final int CHANNEL_TICKS = 20; // This is the duration it takes to "charge up" the ability
    private static final int GRACE_TICKS = 10; // This is the invulnerability period you have until enemies can attack you when you start stalking
    private static final int MOB_DURATION = 140; // This is the ability duration when the victim is a mob.
    private static final int PLAYER_DURATION = 100; // This is the ability duration when the victim is a player.
    private static final float MOB_DAMAGE_MULT = 2.0f; // This is the damage multiplier during stalking when the victim is a mob.
    private static final float PLAYER_DAMAGE_MULT = 1.5f; // This is the damage multiplier during stalking when the victim is a player.
    private static final int VULNERABLE_TICKS = 60; // This is the amount of time after being forcibly ejected that you receive extra damage towards yourself.
    private static final float VULNERABLE_MULT = 3.0f; // This is the multiplier of incoming damage after being forcibly ejected.
    private static final float VULNERABLE_EXTRA_CAP = 10.0f; // This is the cap of extra damage to the incoming damage multiplier.
    private static final double BEHIND_GAP = 0.3; // Gap between the two hitboxes.
    private static final int SAFE_RADIUS = 6; // How far to try and find for a safe block to eject onto.

    private static class Stalk {
        final ServerPlayer stalker;
        final LivingEntity victim;
        final ServerLevel level;
        final Vec3 startPos;
        final long startTick;
        final long endTick;
        Stalk(ServerPlayer stalker, LivingEntity victim, ServerLevel level, long startTick, long endTick) {
            this.stalker = stalker;
            this.victim = victim;
            this.level = level;
            this.startPos = stalker.position();
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }

    private static class Channel {
        final LivingEntity target;
        int ticks = 0;
        Channel(LivingEntity target) { this.target = target; }
    }

    private static final Map<UUID, Stalk> byStalker = new HashMap<>();
    private static final Map<UUID, Stalk> byVictim = new HashMap<>();
    private static final Map<UUID, Channel> channels = new HashMap<>();
    private static final Map<UUID, Long> vulnerableUntil = new HashMap<>();
    private static final Set<UUID> rescaling = new HashSet<>();
    private static final List<Stalk> pendingEjections = new ArrayList<>();

    public static boolean isStalking(Player player) {
        return byStalker.containsKey(player.getUUID());
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomWandStalkHandler::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(PhantomWandStalkHandler::onDamage);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> vulnerableUntil.remove(newPlayer.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            Stalk asStalker = byStalker.get(uuid);
            if (asStalker != null) eject(asStalker);
            Stalk asVictim = byVictim.get(uuid);
            if (asVictim != null) eject(asVictim);
            channels.remove(uuid);
        });
    }

    private static void onTick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Stalk stalk : new ArrayList<>(pendingEjections)) teleportOut(stalk);
        pendingEjections.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (byStalker.containsKey(player.getUUID())) {
                if (isHoldingWand(player)) player.stopUsingItem();
            } else {
                tickChannel(player, now);
            }
        }
        for (Stalk stalk : new ArrayList<>(byStalker.values())) tickStalk(stalk, now);
    }

    private static boolean isHoldingWand(ServerPlayer player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof PhantomWandItem;
    }

    private static void tickChannel(ServerPlayer player, long now) {
        UUID uuid = player.getUUID();
        if (!isHoldingWand(player)) {
            channels.remove(uuid);
            return;
        }
        LivingEntity looked = findLookedAt(player);
        Channel channel = channels.get(uuid);
        if (looked == null || !player.hasLineOfSight(looked) || (channel != null && channel.target != looked)) {
            channels.remove(uuid);
            player.stopUsingItem();
            return;
        }
        if (channel == null) {
            channel = new Channel(looked);
            channels.put(uuid, channel);
        }
        if (++channel.ticks < CHANNEL_TICKS) return;
        channels.remove(uuid);
        player.stopUsingItem();
        if (!ManaManager.tryConsume(player, ManaCosts.PHANTOM_WAND)) {
            ManaManager.playInsufficientManaSound(player);
            return;
        }
        begin(player, looked, now);
    }

    private static LivingEntity findLookedAt(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getLookAngle().scale(RANGE);
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
        return nearest != null && isValidTarget(nearest) ? nearest : null;
    }

    private static boolean isValidTarget(LivingEntity target) {
        EntityType<?> type = target.getType();
        if (type == EntityTypes.WITHER || type == EntityTypes.ENDER_DRAGON || type == EntityTypes.WARDEN) return false;
        if (target instanceof Player p && p.isSpectator()) return false;
        return !byVictim.containsKey(target.getUUID()) && !byStalker.containsKey(target.getUUID());
    }

    private static void begin(ServerPlayer player, LivingEntity victim, long now) {
        ServerLevel level = player.level();
        int duration = victim instanceof Player ? PLAYER_DURATION : MOB_DURATION;
        Stalk stalk = new Stalk(player, victim, level, now, now + duration);
        byStalker.put(player.getUUID(), stalk);
        byVictim.put(victim.getUUID(), stalk);
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, true, true));
        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 254, false, true, true));
        blink(level, player.position());
        moveBehind(stalk);
    }

    private static void tickStalk(Stalk s, long now) {
        ServerPlayer p = s.stalker;
        if (p.isRemoved() || !p.isAlive()) {
            endStalk(s);
            return;
        }
        if (s.victim.isRemoved() || !s.victim.isAlive() || s.victim.level() != s.level || p.level() != s.level || now >= s.endTick) {
            eject(s);
            return;
        }
        moveBehind(s);
    }

    private static void moveBehind(Stalk s) {
        LivingEntity v = s.victim;
        ServerPlayer p = s.stalker;
        double yaw = Math.toRadians(v.getYRot());
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));

        double dist = (v.getBoundingBox().getXsize() + p.getBoundingBox().getXsize()) / 2.0 + BEHIND_GAP;

        Vec3 dest = null;
        for (double deg: new double[]{0, 35, -35, 70, -70}) {
            Vec3 dir = back.yRot((float) Math.toRadians(deg));
            Vec3 spot = v.position().add(dir.scale(dist));
            AABB box = p.getBoundingBox().move(spot.x - p.getX(), spot.y - p.getY(), spot.z - p.getZ());
            if (s.level.noCollision(p, box)) {
                dest = spot;
                break;
            }
        }
        if (dest == null) return;
        if (p.position().distanceToSqr(dest) > 0.0025) snapTo(p, s.level, dest);
    }

    private static void snapTo(ServerPlayer p, ServerLevel level, Vec3 pos) {
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        p.teleportTo(level, pos.x, pos.y, pos.z, Set.of(Relative.X_ROT, Relative.Y_ROT), 0f, 0f, false);
    }

    private static void blink(ServerLevel level, Vec3 at) {
        level.playSeededSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f, level.getRandom().nextLong());
    }

    private static void endStalk(Stalk s) {
        byStalker.remove(s.stalker.getUUID(), s);
        byVictim.remove(s.victim.getUUID(), s);
        s.victim.removeEffect(MobEffects.WEAKNESS);
        s.victim.removeEffect(MobEffects.BLINDNESS);
        s.victim.removeEffect(MobEffects.SLOWNESS);
    }

    private static void eject(Stalk s) {
        endStalk(s);
        teleportOut(s);
    }

    private static void teleportOut(Stalk s) {
        ServerPlayer p = s.stalker;
        if (p.isRemoved() || !p.isAlive() || p.level() != s.level) return;
        Vec3 safe = findSafeSpot(s.level, p);
        Vec3 dest = safe != null ? safe : s.startPos;
        snapTo(p, s.level, dest);
        blink(s.level, dest);
    }

    private static Vec3 findSafeSpot(ServerLevel level, ServerPlayer p) {
        BlockPos origin = p.blockPosition();
        Vec3 from = p.position();
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -SAFE_RADIUS; dx <= SAFE_RADIUS; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -SAFE_RADIUS; dz <= SAFE_RADIUS; dz++) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    Vec3 spot = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                    double dist = spot.distanceToSqr(from);
                    if (dist >= bestDist || !isSafeStand(level, p, feet, spot)) continue;
                    best = spot;
                    bestDist = dist;
                }
            }
        }
        return best;
    }

    private static boolean isSafeStand(ServerLevel level, ServerPlayer p, BlockPos feet, Vec3 spot) {
        BlockPos floorPos = feet.below();
        BlockState floor = level.getBlockState(floorPos);
        if (!floor.isFaceSturdy(level, floorPos, Direction.UP)) return false;
        if (floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS) || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)) return false;
        for (int i = 0; i < 2; i++) {
            BlockPos at = feet.above(i);
            BlockState state = level.getBlockState(at);
            if (!level.getFluidState(at).isEmpty() || state.is(BlockTags.FIRE) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.COBWEB)) return false;
        }
        AABB box = p.getBoundingBox().move(spot.x - p.getX(), spot.y - p.getY(), spot.z - p.getZ());
        return level.noCollision(p, box);
    }

    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        long now = entity.level().getGameTime();

        if (attacker != null && byVictim.containsKey(attacker.getUUID())) return false;

        if (attacker instanceof ServerPlayer sp && attacker != entity && byStalker.containsKey(sp.getUUID()) && !rescaling.contains(sp.getUUID())) {
            float mult = entity instanceof Player ? PLAYER_DAMAGE_MULT : MOB_DAMAGE_MULT;
            return rescale(entity, source, amount * mult, sp.getUUID());
        }

        if (entity instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            Stalk stalk = byStalker.get(uuid);
            if (stalk != null) {
                if (now - stalk.startTick < GRACE_TICKS) return false;
                if (attacker != null && attacker != player) {
                    endStalk(stalk);
                    pendingEjections.add(stalk);
                    vulnerableUntil.put(uuid, now + VULNERABLE_TICKS);
                }
                return true;
            }
            Long until = vulnerableUntil.get(uuid);
            if (until != null && now < until && !rescaling.contains(uuid)) {
                float extra = Math.min(amount * (VULNERABLE_MULT - 1.0f), VULNERABLE_EXTRA_CAP);
                return rescale(player, source, amount + extra, uuid);
            }
        }
        return true;
    }

    private static boolean rescale(LivingEntity target, DamageSource source, float newAmount, UUID guard) {
        rescaling.add(guard);
        try {
            if (target.level() instanceof ServerLevel level) target.hurtServer(level, source, newAmount);
        } finally {
            rescaling.remove(guard);
        }
        return false;
    }
}
