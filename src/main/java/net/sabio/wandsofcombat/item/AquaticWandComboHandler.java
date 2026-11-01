package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.aqua.AquaHearts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AquaticWandComboHandler {
    // Config
    private static final int[] HITS_NEEDED = {4, 6, 8};
    private static final int[] WEAKNESS_AMPLIFIER = {1, 0, 0};
    private static final int[] WEAKNESS_TICKS = {80, 60, 40};
    private static final int RETRIGGER_DELAY_TICKS = 60;
    private static final int HEAL_COOLDOWN_TICKS = 20;
    private static final float HEAL_AMOUNT = 1.0f;

    private record Weakened(UUID owner, long until) {}

    private static final Map<UUID, Integer> hitCounters = new HashMap<>();
    private static final Map<UUID, Weakened> weakened = new HashMap<>();
    private static final Map<UUID, Long> retriggerAt = new HashMap<>();
    private static final Map<UUID, Long> lastHeal = new HashMap<>();

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(AquaticWandComboHandler::onDamaged);
        ServerTickEvents.END_SERVER_TICK.register(AquaticWandComboHandler::onTick);
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            UUID uuid = listener.player.getUUID();
            hitCounters.remove(uuid);
            lastHeal.remove(uuid);
        });
    }

    public static void onHit(Player attacker, LivingEntity target) {
        if (!(attacker instanceof ServerPlayer player) || target.isAlliedTo(player)) return;
        ServerLevel level = player.level();
        long now = level.getGameTime();
        UUID victim = target.getUUID();

        Long locked = retriggerAt.get(victim);
        if (locked != null && now < locked) return;

        UUID uuid = player.getUUID();
        int tier = AquaHearts.getComboTier(player);
        int hits = hitCounters.getOrDefault(uuid, 0) + 1;
        if (hits < HITS_NEEDED[tier - 1]) {
            hitCounters.put(uuid, hits);
            return;
        }
        hitCounters.put(uuid, 0);

        int ticks = WEAKNESS_TICKS[tier - 1];
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, WEAKNESS_AMPLIFIER[tier - 1], false, true, true));
        weakened.put(victim, new Weakened(uuid, now + ticks));
        retriggerAt.put(victim, now + ticks + RETRIGGER_DELAY_TICKS);
        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 24, 0.3, 0.4, 0.3, 0.1);
    }

    private static void onDamaged(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof ServerPlayer player) || damageTaken <= 0) return;
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        Weakened mark = weakened.get(attacker.getUUID());
        if (mark == null || !mark.owner().equals(player.getUUID())) return;

        long now = player.level().getGameTime();
        if (now >= mark.until()) return;
        Long last = lastHeal.get(player.getUUID());
        if (last != null && now - last < HEAL_COOLDOWN_TICKS) return;

        if (AquaHearts.healNormalHearts(player, HEAL_AMOUNT)) {
            lastHeal.put(player.getUUID(), now);
            player.level().sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + player.getBbHeight() * 0.8, player.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
        }
    }

    private static void onTick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 100 != 0) return;
        weakened.values().removeIf(mark -> now >= mark.until());
        retriggerAt.values().removeIf(until -> now >= until);
    }
}
