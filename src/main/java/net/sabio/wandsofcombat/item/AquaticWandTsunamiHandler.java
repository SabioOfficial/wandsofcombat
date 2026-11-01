package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.aqua.AquaHearts;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;
import net.sabio.wandsofcombat.util.HitUtil;
import net.sabio.wandsofcombat.util.TargetFilter;
import net.sabio.wandsofcombat.util.VelocitySync;

import java.util.*;

public class AquaticWandTsunamiHandler {
    // Config
    private static final double[] WIDTH = {8.0, 6.0, 4.0};
    private static final double[] LENGTH = {12.0, 8.0, 5.0};
    private static final int[] CAST_TICKS = {0, 20, 30};
    private static final float[] MOB_DAMAGE = {6.0f, 4.0f, 3.0f};
    private static final float[] PLAYER_DAMAGE = {2.0f, 1.5f, 1.0f};
    private static final float[] SOUND_VOLUME = {0.0f, 0.5f, 2.5f};
    private static final double[] CREST_HEIGHT = {4.5, 3.5, 2.5};

    private static final int LINGER_TICKS = 40;
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final double WAVE_SPEED = 0.75;
    private static final double WAVE_BOTTOM = -1.0;
    private static final double WAVE_TOP = 2.5;
    private static final double PUSH_ACCEL = 0.12;
    private static final double PUSH_MAX = 0.55;

    private record Charge(int tier, long startTick) {}

    private static class Wave {
        final ServerPlayer caster;
        final ServerLevel level;
        final Vec3 origin;
        final Vec3 dir;
        final Vec3 side;
        final int tier;
        final long start;
        final int travelTicks;
        final Map<UUID, Long> lastDamage = new HashMap<>();

        Wave(ServerPlayer caster, ServerLevel level, int tier, long start) {
            this.caster = caster;
            this.level = level;
            this.tier = tier;
            this.start = start;
            this.origin = caster.position();
            this.dir = Vec3.directionFromRotation(0.0f, caster.getYRot()).normalize();
            this.side = new Vec3(-dir.z, 0.0, dir.x);
            this.travelTicks = (int) Math.ceil(LENGTH[tier - 1] / WAVE_SPEED);
        }

        double width() { return WIDTH[tier - 1]; }
        double length() { return LENGTH[tier - 1]; }
        long endTick() { return start + travelTicks + LINGER_TICKS; }

        double front(long now) {
            return Math.min(length(), (now - start + 1) * WAVE_SPEED);
        }
    }

    private static final Map<UUID, Charge> charges = new HashMap<>();
    private static final List<Wave> waves = new ArrayList<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(AquaticWandTsunamiHandler::onTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(AquaticWandTsunamiHandler::onDamaged);
        ServerPlayConnectionEvents.DISCONNECT.register((listener, _) -> {
            UUID uuid = listener.player.getUUID();
            charges.remove(uuid);
            waves.removeIf(wave -> wave.caster.getUUID().equals(uuid));
        });
    }

    public static boolean isCharging(Player player) {
        return charges.containsKey(player.getUUID());
    }

    public static boolean tryBeginCast(ServerPlayer player) {
        if (charges.containsKey(player.getUUID())) return false;
        if (!AquaHearts.canUseAbilities(player)) {
            player.sendSystemMessage(Component.literal("Ability has not met use conditions yet!").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int tier = Math.clamp(AquaHearts.getTier(player), 1, 3);
        if (!ManaManager.hasEnough(player, ManaCosts.aquaticTsunami(tier))) {
            ManaManager.playInsufficientManaSound(player);
            return false;
        }

        long now = player.level().getGameTime();
        if (CAST_TICKS[tier - 1] <= 0) {
            release(player, tier, now);
            player.stopUsingItem();
            return true;
        }
        charges.put(player.getUUID(), new Charge(tier, now));
        float volume = SOUND_VOLUME[tier - 1];
        if (volume > 0.0f) cue(player.level(), player.position(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, volume * 0.5f, 1.2f);
        return true;
    }

    private static boolean isHoldingWand(ServerPlayer player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof AquaticWandItem;
    }

    private static void onDamaged(LivingEntity entity, DamageSource source,  float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof ServerPlayer player) || damageTaken <= 0) return;
        if (charges.remove(player.getUUID()) == null) return;
        player.stopUsingItem();
        player.level().sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, .4, 0.3, 0.05);
    }

    private static void onTick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (!charges.isEmpty()) tickCharges(server, now);
        if (!waves.isEmpty()) {
            for (Wave wave : new ArrayList<>(waves)) tickWave(wave, now);
        }
    }

    private static void tickCharges(MinecraftServer server, long now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Charge charge = charges.get(player.getUUID());
            if (charge == null) continue;
            if (!player.isAlive() || !isHoldingWand(player)) {
                charges.remove(player.getUUID());
                continue;
            }
            long elapsed = now - charge.startTick();
            if (elapsed < CAST_TICKS[charge.tier() - 1]) {
                player.level().sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.5, 0.6, 0.5, 0.02);
                continue;
            }
            charges.remove(player.getUUID());
            player.stopUsingItem();
            release(player, charge.tier(), now);
        }
    }

    private static void release(ServerPlayer player, int tier, long now) {
        if (!AquaHearts.canUseAbilities(player)) return;
        if (!ManaManager.tryConsume(player, ManaCosts.aquaticTsunami(tier))) {
            ManaManager.playInsufficientManaSound(player);
            return;
        }
        ServerLevel level = player.level();
        Wave wave = new Wave(player, level, tier, now);
        waves.add(wave);

        float volume = SOUND_VOLUME[tier - 1];
        if (volume > .0f) {
            cue(level, player.position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, volume, 0.9f);
            cue(level, player.position(), SoundEvents.GENERIC_SPLASH, volume, 0.8f);
        }
    }

    private static void tickWave(Wave wave, long now) {
        if (now >= wave.endTick() || wave.caster.isRemoved() || wave.caster.level() != wave.level) {
            waves.remove(wave);
            return;
        }

        double front = wave.front(now);
        double halfWidth = wave.width() / 2.0;
        double yMin = wave.origin.y + WAVE_BOTTOM;
        double yMax = wave.origin.y + WAVE_TOP;

        spawnWave(wave, now, front, halfWidth);
        if (now == wave.start + wave.travelTicks) crash(wave, front, halfWidth);
        if (SOUND_VOLUME[wave.tier - 1] >= 1.0f && (now - wave.start) % 20 == 0) {
            cue(wave.level, wave.origin.add(wave.dir.scale(front * 0.5)), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SOUND_VOLUME[wave.tier - 1] * 0.6f, 1.0f);
        }

        Vec3 farLeft = wave.origin.add(wave.dir.scale(front)).add(wave.side.scale(halfWidth));
        Vec3 farRight = wave.origin.add(wave.dir.scale(front)).subtract(wave.side.scale(halfWidth));
        Vec3 nearLeft = wave.origin.add(wave.side.scale(halfWidth));
        Vec3 nearRight = wave.origin.subtract(wave.side.scale(halfWidth));
        AABB box = new AABB(
                Math.min(Math.min(farLeft.x, farRight.x), Math.min(nearLeft.x, nearRight.x)), yMin,
                Math.min(Math.min(farLeft.z, farRight.z), Math.min(nearLeft.z, nearRight.z)),
                Math.max(Math.max(farLeft.x, farRight.x), Math.max(nearLeft.x, nearRight.x)), yMax,
                Math.max(Math.max(farLeft.z, farRight.z), Math.max(nearLeft.z, nearRight.z))
        ).inflate(1.0, 0.0, 1.0);

        List<LivingEntity> candidates = wave.level.getEntitiesOfClass(LivingEntity.class, box, entity -> isValidTarget(wave.caster, entity));
        for (LivingEntity target : candidates) {
            AABB bb = target.getBoundingBox();
            if (bb.maxY < yMin || bb.minY > yMax) continue;
            Vec3 rel = bb.getCenter().subtract(wave.origin);
            double margin = target.getBbWidth() / 2.0;
            double along = rel.x * wave.dir.x + rel.z * wave.dir.z;
            double lateral = rel.x * wave.side.x + rel.z * wave.side.z;
            if (along < -margin || along > front + margin || Math.abs(lateral) > halfWidth + margin) continue;

            push(target, wave.dir);

            Long last = wave.lastDamage.get(target.getUUID());
            if (last != null && now - last < DAMAGE_INTERVAL_TICKS) continue;
            wave.lastDamage.put(target.getUUID(), now);
            float amount = target instanceof Player ? PLAYER_DAMAGE[wave.tier - 1] : MOB_DAMAGE[wave.tier - 1];
            HitUtil.hurtThroughIFrames(wave.level, target, HitUtil.wandMagic(wave.level, wave.caster, wave.caster), amount);
        }
    }

    private static boolean isValidTarget(ServerPlayer caster, LivingEntity entity) {
        return entity != caster && entity.isAlive() && !entity.isSpectator() && TargetFilter.isHostileOrPlayer(entity) && !entity.isAlliedTo(caster);
    }

    private static void push(LivingEntity target, Vec3 dir) {
        Vec3 velocity = target.getDeltaMovement();
        double along = velocity.x * dir.x + velocity.z * dir.z;
        if (along >= PUSH_MAX) return;
        double add = Math.min(PUSH_ACCEL, PUSH_MAX - along);
        target.setDeltaMovement(velocity.add(dir.x * add, 0.0, dir.z * add));
        VelocitySync.mark(target);
    }

    private static void emit(Wave wave, ParticleOptions type, double along, double lateral, double y) {
        Vec3 p = wave.origin.add(wave.dir.scale(along)).add(wave.side.scale(lateral));
        wave.level.sendParticles(type, p.x, p.y + y, p.z, 1, 0.08, 0.08, 0.08, 0.01);
    }

    private static void spawnWave(Wave wave, long now, double front, double halfWidth) {
        var rng = wave.level.getRandom();
        long age = now - wave.start;

        double rise = Math.min(1.0, (age + 1) / 8.0);
        double fall = age < wave.travelTicks ? 1.0 : Math.max(0.0, 1.0 - (age - wave.travelTicks) / (double) LINGER_TICKS);
        double crest = CREST_HEIGHT[wave.tier - 1] * rise * fall;
        if (crest < 0.3) return;

        for (double lateral = -halfWidth; lateral <= halfWidth; lateral += 0.5) {
            double edge = Math.cos(lateral / halfWidth * Math.PI / 2.0);
            double ripple = 1.0 + 0.12 * Math.sin(lateral * 1.7 + age * 0.5);
            double h = crest * (0.35 + 0.65 * edge) * ripple;
            double back = h * 1.6;
            double r = h * 0.28;

            for (int i = 0; i < 2; i++) {
                double a = rng.nextDouble() * back;
                emit(wave, ParticleTypes.SPLASH, front - a, lateral, h * Math.pow(1.0 - a / back, 1.5));
            }

            if (rng.nextInt(3) == 0) {
                double a = rng.nextDouble() * back;
                double surface = h * Math.pow(1.0 - a / back, 1.5);
                emit(wave, ParticleTypes.BUBBLE, front - a, lateral, rng.nextDouble() * surface);
            }

            for (int i = 0; i < 2; i++) {
                double phi = Math.toRadians(90.0 - rng.nextDouble() * 125.0);
                double along = front + r * Math.cos(phi);
                double y = h - r + r * Math.sin(phi);
                emit(wave, rng.nextInt(4) == 0 ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, along, lateral, y);
            }

            double lipPhi = Math.toRadians(-35.0);
            Vec3 lip = wave.origin
                    .add(wave.dir.scale(front + r * Math.cos(lipPhi)))
                    .add(wave.side.scale(lateral));
            wave.level.sendParticles(ParticleTypes.FALLING_WATER, lip.x, lip.y + h - r + r * Math.sin(lipPhi), lip.z, 1, 0.1, 0.0, 0.1, 0.0);
        }
    }

    private static void crash(Wave wave, double front, double halfWidth) {
        for (double lateral = -halfWidth; lateral <= halfWidth; lateral += 0.5) {
            Vec3 p = wave.origin.add(wave.dir.scale(front)).add(wave.side.scale(lateral));
            wave.level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.5, p.z, 8, 0.3, 0.6, 0.3, 0.25);
        }
        cue(wave.level, wave.origin.add(wave.dir.scale(front)), SoundEvents.GENERIC_SPLASH, SOUND_VOLUME[wave.tier - 1], 0.7f);
    }

    private static void cue(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
