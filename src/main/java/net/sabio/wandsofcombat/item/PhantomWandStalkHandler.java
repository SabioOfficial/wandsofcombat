package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.entity.ModEntities;
import net.sabio.wandsofcombat.entity.PhantomBulletEntity;
import net.sabio.wandsofcombat.entity.StalkSigilEntity;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;
import net.sabio.wandsofcombat.util.PhantomTargeting;

import java.util.*;

public class PhantomWandStalkHandler {
    // Config
    private static final double RANGE = PhantomTargeting.RANGE;
    private static final int CHANNEL_TICKS = 20;
    private static final int MOB_STUN = 80;
    private static final int PLAYER_STUN = 60;
    private static final int BULLET_INTERVAL = 12;
    private static final int VOLLEY_SIZE = 4;
    private static final int HIT_SPACING = 3;
    private static final int APPEAR_GAP = 2;
    private static final double RING_RADIUS = 1.4;
    private static final double SLOT_RISE = 0.18;
    private static final double VOLLEY_TURN = Math.PI / VOLLEY_SIZE;
    private static final int BASE_CHARGE = 12;
    private static final int BULLET_TAIL = 30;
    private static final float MOB_BULLET_DAMAGE = 1.5f;
    private static final float PLAYER_BULLET_DAMAGE = 1.0f;
    private static final DustParticleOptions RING_DUST = new DustParticleOptions(0x9B59FF, 1.0f);
    private static final DustParticleOptions RING_DUST_INNER = new DustParticleOptions(0xD9B8FF, 0.8f);

    private static class Stalk {
        final ServerPlayer stalker;
        final LivingEntity victim;
        final ServerLevel level;
        final long startTick;
        final long endTick;
        final boolean wasNoAi;
        StalkSigilEntity sigil;

        Stalk(ServerPlayer stalker, LivingEntity victim, ServerLevel level, long startTick, long endTick) {
            this.stalker = stalker;
            this.victim = victim;
            this.level = level;
            this.startTick = startTick;
            this.endTick = endTick;
            this.wasNoAi = victim instanceof Mob m && m.isNoAi();
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

    public static boolean isStalking(Player player) {
        return byStalker.containsKey(player.getUUID());
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomWandStalkHandler::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(PhantomWandStalkHandler::onDamage);
        ServerPlayConnectionEvents.DISCONNECT.register((listener, _) -> {
            UUID uuid = listener.player.getUUID();
            Stalk asStalker = byStalker.get(uuid);
            if (asStalker != null) endStalk(asStalker);
            Stalk asVictim = byVictim.get(uuid);
            if (asVictim != null) endStalk(asVictim);
            channels.remove(uuid);
        });
    }

    private static void onTick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
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
        LivingEntity nearest = PhantomTargeting.findLookedAt(player);
        return nearest != null && isValidTarget(nearest) ? nearest : null;
    }

    private static boolean isValidTarget(LivingEntity target) {
        if (PhantomTargeting.isInvalidType(target)) return false;
        return !byVictim.containsKey(target.getUUID()) && !byStalker.containsKey(target.getUUID());
    }

    private static void begin(ServerPlayer player, LivingEntity victim, long now) {
        ServerLevel level = player.level();
        int duration = victim instanceof Player ? PLAYER_STUN : MOB_STUN;
        Stalk stalk = new Stalk(player, victim, level, now, now + duration);
        byStalker.put(player.getUUID(), stalk);
        byVictim.put(victim.getUUID(), stalk);

        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 9, false, true, true));
        disableJump(victim);
        if (victim instanceof Mob m) m.setNoAi(true);

        StalkSigilEntity sigil = new StalkSigilEntity(ModEntities.STALK_SIGIL, level);
        sigil.setup(victim, duration);
        level.addFreshEntity(sigil);
        stalk.sigil = sigil;

        blink(level, victim.position());
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 30, 0.4, victim.getBbHeight() * 0.4, 0.4, 0.1);
        cue(level, victim.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 0.9f, 0.8f);
        castShockwave(level, victim);
    }

    private static void castShockwave(ServerLevel level, LivingEntity victim) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double a = i * (Math.PI * 2 / points);
            level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + 0.15, victim.getZ(), 0, Math.cos(a), 0.0, Math.sin(a), 0.18);
        }
    }

    private static void tickStalk(Stalk s, long now) {
        if (s.stalker.isRemoved() || !s.stalker.isAlive() || s.victim.isRemoved() || !s.victim.isAlive() || s.victim.level() != s.level || now >= s.endTick) {
            endStalk(s);
            return;
        }

        long t = now - s.startTick;
        long phase = t % BULLET_INTERVAL;
        if (now < s.endTick - BULLET_TAIL && phase % APPEAR_GAP == 0 && phase / APPEAR_GAP < VOLLEY_SIZE) {
            int slot = (int) (phase / APPEAR_GAP);
            spawnBullet(s, slot, t / BULLET_INTERVAL);
            cue(s.level, s.victim.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.35f, 0.7f + slot * 0.15f);
        }
        if (t % 2 == 0) sigilSparks(s, now);
    }

    private static void spawnBullet(Stalk s, int slot, long volley) {
        LivingEntity v = s.victim;

        double angle = volley * VOLLEY_TURN + slot * (Math.PI * 2 / VOLLEY_SIZE);
        double radius = v.getBbWidth() * 0.5 + RING_RADIUS;
        double rise = (slot - (VOLLEY_SIZE - 1) / 2.0) * SLOT_RISE;
        Vec3 pos = v.position().add(Math.cos(angle) * radius, v.getBbHeight() * 0.5 + rise, Math.sin(angle) * radius);

        int charge = BASE_CHARGE + slot * (HIT_SPACING - APPEAR_GAP);

        PhantomBulletEntity bullet = new PhantomBulletEntity(ModEntities.PHANTOM_BULLET, s.level);
        bullet.setPos(pos.x, pos.y, pos.z);
        bullet.setup(s.stalker, v, v instanceof Player ? PLAYER_BULLET_DAMAGE : MOB_BULLET_DAMAGE, charge, slot);
        s.level.addFreshEntity(bullet);
    }

    private static void sigilSparks(Stalk s, long now) {
        long age = now - s.startTick;
        long left = s.endTick - now;
        if (age < StalkSigilEntity.FADE_IN || left < StalkSigilEntity.FADE_OUT || age % 4 != 0) return;

        LivingEntity v = s.victim;
        double r = StalkSigilEntity.sizeFor(StalkSigilEntity.isLargeFor(v)) * 0.5 * 0.85;
        double spin = age * 0.15;
        for (int i = 0; i < 2; i++) {
            double a = spin + i * Math.PI;
            s.level.sendParticles(ParticleTypes.END_ROD, v.getX() + Math.cos(a) * r, v.getY() + 0.1, v.getZ() + Math.sin(a) * r, 0, 0.0, 1.0, 0.0, 0.05);
        }
    }

    private static void endStalk(Stalk s) {
        if (s.sigil != null) s.sigil.discard();
        if (!s.victim.isRemoved()) {
            if (!s.victim.isAlive() && s.stalker.isAlive()) finale(s);
            else fizzle(s);
        }
        byStalker.remove(s.stalker.getUUID(), s);
        byVictim.remove(s.victim.getUUID(), s);
        s.victim.removeEffect(MobEffects.SLOWNESS);
        enableJump(s.victim);
        if (s.victim instanceof Mob m && !s.wasNoAi) m.setNoAi(false);
    }

    private static void fizzle(Stalk s) {
        LivingEntity v = s.victim;
        cue(s.level, v.position(), SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.6f);
        s.level.sendParticles(ParticleTypes.END_ROD, v.getX(), v.getY() + v.getBbHeight() * 0.5, v.getZ(), 12, 0.3, v.getBbHeight() * 0.3, 0.3, 0.06);
    }

    private static void finale(Stalk s) {
        ServerLevel level = s.level;
        LivingEntity v = s.victim;
        double cx = v.getX(), cy = v.getY() + v.getBbHeight() * 0.5, cz = v.getZ();

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, cx, cy, cz, 40, 0.5, v.getBbHeight() * 0.4, 0.5, 0.15);
        castShockwave(level, v);
        level.sendParticles(ParticleTypes.END_ROD, cx, v.getY(), cz, 24, 0.15, 0.05, 0.15, 0.12);

        double r = v.getBbWidth() * 0.5 + 1.2;
        for (int i = 0; i < 24; i++) {
            double a = i * (Math.PI * 2 / 24);
            level.sendParticles(RING_DUST, cx + Math.cos(a) * r, v.getY() + 0.1, cz + Math.sin(a) * r, 1, 0, 0, 0, 0);
            level.sendParticles(RING_DUST_INNER, cx + Math.cos(a) * r * 0.55, v.getY() + 0.1, cz + Math.sin(a) * r * 0.55, 1, 0, 0, 0, 0);
        }

        cue(level, v.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, .8f);
        cue(level, v.position(), SoundEvents.ENDER_EYE_DEATH, 0.8f, 1.3f);
        cue(level, v.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7f, 1.1f);
    }

    private static void disableJump(LivingEntity v) {
        AttributeInstance jump = v.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null && !jump.hasModifier(Identifier.fromNamespaceAndPath("wandsofcombat", "stalk_no_jump"))) {
            jump.addTransientModifier(new AttributeModifier(Identifier.fromNamespaceAndPath("wandsofcombat", "stalk_no_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void enableJump(LivingEntity v) {
        AttributeInstance jump = v.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(Identifier.fromNamespaceAndPath("wandsofcombat", "stalk_no_jump"));
    }

    private static void cue(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void blink(ServerLevel level, Vec3 at) {
        level.playSeededSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f, level.getRandom().nextLong());
    }

    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        return attacker == null || !byVictim.containsKey(attacker.getUUID());
    }
}