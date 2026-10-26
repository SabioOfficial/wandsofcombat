package net.sabio.wandsofcombat.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.Wandsofcombat;

import java.util.*;

public class PhantomModeHandler {
    private static final Map<UUID, Long> pullImmunityEndTimes = new HashMap<>();
    private static final double PULL_RANGE = 5.0;
    private static final double MELEE_RANGE = 3.0;
    private static final double PULL_SPEED = 1.2;
    private static final int PULL_IMMUNITY_DURATION = 10; // immune for 10 ticks after pull
    private static final Identifier REACH_MODIFIER_ID = Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "phantom_wand_reach");
    private static class PullProgress {
        final ServerPlayer attacker;
        final LivingEntity target;
        final Vec3 startPos;
        final Vec3 endPos;
        int totalTicks;
        int ticksElapsed;
        PullProgress(ServerPlayer attacker, LivingEntity target, Vec3 startPos, Vec3 endPos, int totalTicks) {
            this.attacker = attacker;
            this.target = target;
            this.startPos = startPos;
            this.endPos = endPos;
            this.totalTicks = totalTicks;
            this.ticksElapsed = 0;
        }
    }
    private static final Map<UUID, PullProgress> activePulls = new HashMap<>();
    private static void manageReachAttribute(Player player, boolean holding) {
        var reachAttribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reachAttribute == null) return;
        reachAttribute.removeModifier(REACH_MODIFIER_ID);
        if (holding) {
            reachAttribute.addTransientModifier(new AttributeModifier(
                    REACH_MODIFIER_ID,
                    2.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }
    private static void onTick(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            long currentTick = world.getGameTime();
            for (Player player : world.players()) {
                UUID uuid = player.getUUID();
                boolean holdingPhantomWand = player.getMainHandItem().getItem() instanceof PhantomWandItem;
                manageReachAttribute(player, holdingPhantomWand);
            }
            List<UUID> finishedPulls = new ArrayList<>();
            for (Map.Entry<UUID, PullProgress> entry : activePulls.entrySet()) {
                PullProgress pull = entry.getValue();
                pull.ticksElapsed++;
                if (pull.attacker.isRemoved() || pull.target.isRemoved()) {
                    finishedPulls.add(entry.getKey());
                    continue;
                }
                float t = (float) pull.ticksElapsed / pull.totalTicks;
                t = Math.min(t, 1.0f);
                double x = pull.startPos.x + (pull.endPos.x - pull.startPos.x) * t;
                double y = pull.startPos.y + (pull.endPos.y - pull.startPos.y) * t;
                double z = pull.startPos.z + (pull.endPos.z - pull.startPos.z) * t;
                pull.attacker.teleportTo(
                        pull.attacker.level(),
                        x,
                        y,
                        z,
                        java.util.Set.of(),
                        pull.attacker.getYRot(),
                        pull.attacker.getXRot(),
                        false
                );
                if (pull.ticksElapsed >= pull.totalTicks) {
                    pull.attacker.attack(pull.target);
                    PhantomWandItem.pullingPlayers.remove(entry.getKey());
                    finishedPulls.add(entry.getKey());
                }
            }
            finishedPulls.forEach(activePulls::remove);
        }
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(PhantomModeHandler::onTick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof Player player) {
                Long immunityEnd = pullImmunityEndTimes.get(player.getUUID());
                if (immunityEnd != null && entity.level().getGameTime() <= immunityEnd) {
                    return false;
                }
            }
            return true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            PhantomWandItem.pullingPlayers.remove(uuid);
            pullImmunityEndTimes.remove(uuid);
            activePulls.remove(uuid);
            var reachAttribute = handler.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (reachAttribute != null) {
                reachAttribute.removeModifier(REACH_MODIFIER_ID);
            }
        });
    }
    public static boolean tryPullAttack(ServerPlayer attacker, LivingEntity target) {
        if (!(attacker.getMainHandItem().getItem() instanceof PhantomWandItem)) {
            return false;
        }
        if (PhantomWandStalkHandler.isStalking(attacker)) return false;
        double distance = attacker.distanceTo(target);
        if (distance <= MELEE_RANGE || distance > PULL_RANGE) {
            return false;
        }
        long immunityEnd = (attacker.level()).getGameTime() + PULL_IMMUNITY_DURATION + 5;
        pullImmunityEndTimes.put(attacker.getUUID(), immunityEnd);
        PhantomWandItem.pullingPlayers.add(attacker.getUUID());
        attacker.level().playSeededSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 1.0f, 1.2f, attacker.level().getRandom().nextLong());
        Vec3 direction = target.position().subtract(attacker.position()).normalize();
        double pullDistance = distance - MELEE_RANGE + 0.5;
        Vec3 destination = attacker.position().add(direction.scale(pullDistance));

        activePulls.put(attacker.getUUID(), new PullProgress(
                attacker,
                target,
                attacker.position(),
                destination,
                8
        ));
        return true;
    }
}
