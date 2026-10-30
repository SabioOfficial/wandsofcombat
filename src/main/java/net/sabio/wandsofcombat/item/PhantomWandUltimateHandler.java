package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.entity.ModEntities;
import net.sabio.wandsofcombat.entity.PhantomReplicaEntity;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;
import net.sabio.wandsofcombat.util.PhantomTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhantomWandUltimateHandler {
    private static final int REPLICAS = 8;
    private static final int FORM_TICKS = 10;
    private static final int LAND_INTERVAL = 2;
    private static final double RING_RADIUS = 3.0;
    private static final float MOB_DAMAGE = 4.0f;
    private static final float PLAYER_DAMAGE = 1.5f;
    private static final int MOB_BLINDNESS_TICKS = 80;
    private static final int PLAYER_NAUSEA_TICKS = 100;

    private static class Cast {
        final ServerPlayer caster;
        final LivingEntity victim;
        final ServerLevel level;
        final long start;
        final double baseAngle;
        final ArrayList<PhantomReplicaEntity> replicas = new ArrayList<>();
        int launched = 0;

        Cast(ServerPlayer caster, LivingEntity victim, ServerLevel level, long start) {
            this.caster = caster;
            this.victim = victim;
            this.level = level;
            this.start = start;
            this.baseAngle = Math.atan2(caster.getZ() - victim.getZ(), caster.getX() - victim.getX()) + Math.PI / REPLICAS;
        }
    }

    private static final Map<UUID, Cast> casts = new HashMap<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomWandUltimateHandler::onTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Cast cast = casts.get(handler.player.getUUID());
            if (cast != null) abort(cast);
        });
    }

    public static boolean isCasting(Player player) {
        return casts.containsKey(player.getUUID());
    }

    public static boolean tryCast(ServerPlayer caster, LivingEntity victim) {
        if (casts.containsKey(caster.getUUID())) return false;
        if (PhantomTargeting.isInvalidType(victim) || !victim.isAlive()) return false;
        if (!ManaManager.tryConsume(caster, ManaCosts.PHANTOM_ULTIMATE)) {
            ManaManager.playInsufficientManaSound(caster);
            return false;
        }

        ServerLevel level = caster.level();
        casts.put(caster.getUUID(), new Cast(caster, victim, level, level.getGameTime()));

        if (victim instanceof Player) {
            victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, PLAYER_NAUSEA_TICKS, 0, false, true, true));
        } else {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MOB_BLINDNESS_TICKS, 0, false, true, true));
        }

        cue(level, victim.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 0.9f, 0.7f);
        cue(level, victim.position(), SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.9f);
        return true;
    }

    private static void onTick(MinecraftServer server) {
        if (casts.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Cast cast : new ArrayList<>(casts.values())) tick(cast, now);
    }

    private static void tick(Cast c, long now) {
        if (c.caster.isRemoved() || !c.caster.isAlive() || c.victim.isRemoved() || !c.victim.isAlive() || c.victim.level() != c.level) {
            abort(c);
            return;
        }

        long t = now - c.start;

        while (c.replicas.size() < REPLICAS && t >= spawnTick(c.replicas.size())) {
            spawn(c, c.replicas.size());
        }

        while (c.launched < c.replicas.size() && t >= FORM_TICKS + (long) c.launched * LAND_INTERVAL) {
            c.replicas.get(c.launched++).launch();
        }

        if (c.launched >= REPLICAS) casts.remove(c.caster.getUUID(), c);
    }

    private static int spawnTick(int i) {
        return i * FORM_TICKS / REPLICAS;
    }

    private static void spawn(Cast c, int i) {
        LivingEntity v = c.victim;
        double angle = c.baseAngle + i * (Math.PI * 2 / REPLICAS);
        double radius = v.getBbWidth() * 0.5 + RING_RADIUS;
        Vec3 offset = new Vec3(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);

        PhantomReplicaEntity replica = new PhantomReplicaEntity(ModEntities.PHANTOM_REPLICA, c.level);
        replica.setup(c.caster, v, offset, v instanceof Player ? PLAYER_DAMAGE : MOB_DAMAGE, i, i == REPLICAS - 1);
        c.level.addFreshEntity(replica);
        c.replicas.add(replica);

        c.level.sendParticles(ParticleTypes.REVERSE_PORTAL, replica.getX(), replica.getY() + 0.9, replica.getZ(), 14, 0.25, 0.7, 0.25, 0.12);
        c.level.sendParticles(ParticleTypes.END_ROD, replica.getX(), replica.getY() + 0.1, replica.getZ(), 4, 0.2, 0.05, 0.2, 0.02);
        for (int k = 0; k < 8; k++) {
            double a = k * (Math.PI / 4);
            c.level.sendParticles(new DustParticleOptions(0x9B59FF, 1.0f), replica.getX() + Math.cos(a) * 0.5, replica.getY() + 0.1, replica.getZ() + Math.sin(a) * 0.5, 1, 0, 0, 0, 0);
        }
        cue(c.level, replica.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4f, 0.7f + i * 0.1f);
    }

    private static void abort(Cast c) {
        for (PhantomReplicaEntity r : c.replicas) {
            if (!r.isRemoved()) r.discard();
        }
        casts.remove(c.caster.getUUID(), c);
    }

    private static void cue(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
