package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.Wandsofcombat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PhantomWandComboHandler {
    // could be used in a potential new mechanic... 👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀👀
    // or config lol
    private static final int COMBO_HITS = 6;
    private static final int EMPOWER_TICKS = 40;
    private static final double DAMAGE_BONUS = 0.5;
    private static final double BEHIND_GAP = 0.6;
    private static final Map<UUID, Integer> hitCounters = new HashMap<>();
    private static final Map<UUID, Long> empoweredUntil = new HashMap<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomWandComboHandler::onTick);
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            hitCounters.remove(listener.player.getUUID());
            clearEmpower(listener.player);
        });
    }

    public static void onHit(Player attacker, LivingEntity target) {
        if (!(attacker instanceof ServerPlayer player)) return;
        ServerLevel level = player.level();
        UUID uuid = player.getUUID();

        Long until = empoweredUntil.get(uuid);
        if (until != null) {
            if (level.getGameTime() <= until) powerStrike(level, target);
            clearEmpower(player);
        }

        int hits = hitCounters.getOrDefault(uuid, 0) + 1;
        if (hits < COMBO_HITS) {
            hitCounters.put(uuid, hits);
            return;
        }
        hitCounters.put(uuid, 0);

        if (target.isAlive()) blinkBehind(level, player, target);
        empower(player, level.getGameTime());
    }

    private static void onTick(MinecraftServer server) {
        if (empoweredUntil.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Long until = empoweredUntil.get(player.getUUID());
            if (until == null) continue;
            if (now >= until || !player.isAlive() || !(player.getMainHandItem().getItem() instanceof PhantomWandItem)) {
                clearEmpower(player);
                continue;
            }
            if (now % 4 == 0) {
                player.level().sendParticles(new DustParticleOptions(0x9B59FF, 1.2f), player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.35, 0.5, 0.35, 0.0);
            }
        }
    }

    private static void empower(ServerPlayer player, long now) {
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage == null) return;
        damage.removeModifier(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "phantom_step_empower"));
        damage.addTransientModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "phantom_step_empower"), DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        empoweredUntil.put(player.getUUID(), now + EMPOWER_TICKS);
    }

    private static void clearEmpower(ServerPlayer player) {
        empoweredUntil.remove(player.getUUID());
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) damage.removeModifier(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "phantom_step_empower"));
    }

    private static void blinkBehind(ServerLevel level, ServerPlayer player, LivingEntity target) {
        double yawRad = Math.toRadians(target.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        double gap = target.getBbWidth() * 0.5 + player.getBbWidth() * 0.5 + BEHIND_GAP;

        Vec3 from = player.position();
        Vec3 dest = null;
        for (double scale : new double[]{1.0, 0.75, 0.5}) {
            Vec3 candidate = target.position().subtract(forward.scale(gap * scale));
            AABB moved = player.getBoundingBox().move(candidate.subtract(from));
            if (level.noCollision(player, moved)) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) return;

        double dx = target.getX() - dest.x;
        double dz = target.getZ() - dest.z;
        float yaw = (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI));

        blinkOut(level, from, player.getBbHeight());
        player.teleportTo(level, dest.x, dest.y, dest.z, Set.of(), yaw, 0.0f, false);
        blinkIn(level, from, dest, player.getBbHeight());
    }

    private static void blinkOut(ServerLevel level, Vec3 at, double height) {
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + height * 0.5, at.z, 30, 0.3, height * 0.4, 0.3, 0.4);
        level.sendParticles(new DustParticleOptions(0x9B59FF, 1.2f), at.x, at.y + height * 0.5, at.z, 12, 0.3, height * 0.4, .3, 0.0);
        cue(level, at, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.5f);
    }

    private static void blinkIn(ServerLevel level, Vec3 from, Vec3 to, double height) {
        Vec3 a = from.add(0, height * 0.5, 0);
        Vec3 b = to.add(0, height * 0.5, 0);
        int steps = Math.max(1, (int) (a.distanceTo(b) / 0.3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            level.sendParticles(new DustParticleOptions(0x9B59FF, 1.2f), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            if (i % 2 == 0) level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.01);
        }

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + height * 0.5, to.z, 30, 0.3, height * 0.4, 0.3, 0.15);
        int points = 20;
        for (int i = 0; i < points; i++) {
            double ang = i * (Math.PI * 2 / points);
            level.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 0.15, to.z, 0, Math.cos(ang), 0.0, Math.sin(ang), 0.15);
            level.sendParticles(new DustParticleOptions(0x9B59FF, 1.2f), to.x + Math.cos(ang), to.y + 0.1, to.z + Math.sin(ang), 1, 0, 0, 0, 0);
            level.sendParticles(new DustParticleOptions(0xD9B8FF, 0.8f), to.x + Math.cos(ang) * 0.55, to.y + 0.1, to.z + Math.sin(ang) * 0.55, 1, 0, 0, 0, 0);
        }

        cue(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.1f);
        cue(level, to, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, 1.5f);
    }

    private static void powerStrike(ServerLevel level, LivingEntity target) {
        double y = target.getY() + target.getBbHeight() * 0.5;
        level.sendParticles(ParticleTypes.CRIT, target.getX(), y, target.getZ(), 14, 0.3, target.getBbHeight() * 0.3, 0.3, 0.3);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), y, target.getZ(), 8, 0.2, target.getBbHeight() * 0.3, 0.2, 0.1);
        cue(level, target.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9f, 1.3f);
    }

    private static void cue(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
