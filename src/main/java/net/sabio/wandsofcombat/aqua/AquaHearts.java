package net.sabio.wandsofcombat.aqua;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.item.AquaticWandItem;
import net.sabio.wandsofcombat.util.TargetFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AquaHearts {
    // Config
    public static final int MAX_AQUA_HP = 6;
    private static final double NORMAL_HP = 10.0;
    private static final int REGEN_INTERVAL_TICKS = 600;
    private static final float UNLOCK_HEALTH = 8.0f;
    private static final int UNLOCK_WINDOW_TICKS = 200;

    private static final Map<UUID, Integer> aquaHp = new HashMap<>();
    private static final Map<UUID, Integer> regenProgress = new HashMap<>();
    private static final Map<UUID, Long> lastEnemyHit = new HashMap<>();

    private AquaHearts() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(AquaHearts::onTick);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(AquaHearts::onDamaged);
        ServerPlayConnectionEvents.JOIN.register((listener, _, server) -> {
            UUID uuid = listener.player.getUUID();
            aquaHp.put(uuid, AquaState.get(server).getPoints(uuid));
            regenProgress.put(uuid, 0);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((_, newPlayer, alive) -> {
            if (alive) return;
            UUID uuid = newPlayer.getUUID();
            aquaHp.put(uuid, MAX_AQUA_HP);
            regenProgress.put(uuid, 0);
            lastEnemyHit.remove(uuid);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            UUID uuid = listener.player.getUUID();
            AquaState.get(server).setPoints(uuid, aquaHp.getOrDefault(uuid, MAX_AQUA_HP));
            aquaHp.remove(uuid);
            regenProgress.remove(uuid);
            lastEnemyHit.remove(uuid);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            AquaState state = AquaState.get(server);
            aquaHp.forEach(state::setPoints);
        });
    }

    public static boolean isHoldingWand(Player player) {
        return player.getMainHandItem().getItem() instanceof AquaticWandItem;
    }

    public static int getAquaHp(Player player) {
        return aquaHp.getOrDefault(player.getUUID(), MAX_AQUA_HP);
    }

    public static int getTier(Player player) {
        return (getAquaHp(player) + 1) / 2;
    }

    public static boolean canUseAbilities(Player player) {
        if (!isHoldingWand(player) || getTier(player) < 1) return false;
        if (player.getHealth() > UNLOCK_HEALTH) return false;
        Long last = lastEnemyHit.get(player.getUUID());
        return last != null && player.level().getGameTime() - last <= UNLOCK_WINDOW_TICKS;
    }

    public static void restoreHearts(ServerPlayer player, int hearts) {
        int before = getAquaHp(player);
        int after = Math.min(MAX_AQUA_HP, before + hearts * 2);
        aquaHp.put(player.getUUID(), after);
        boolean holding = isHoldingWand(player);
        syncMaxHealth(player, holding, after);
        if (holding) player.setHealth(Math.min(player.getHealth() + (after - before), player.getMaxHealth()));
    }

    private static void onDamaged(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof ServerPlayer player) || damageTaken <= 0) return;
        aquaHp.put(player.getUUID(), Math.min(getAquaHp(player), Mth.ceil(player.getHealth())));
        if (source.getEntity() instanceof LivingEntity attacker && attacker != player && TargetFilter.isHostileOrPlayer(attacker) && !attacker.isAlliedTo(player)) {
            lastEnemyHit.put(player.getUUID(), player.level().getGameTime());
        }
    }

    private static void onTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) continue;
            UUID uuid = player.getUUID();
            boolean holding = isHoldingWand(player);

            int aqua = Math.min(getAquaHp(player), Mth.ceil(player.getHealth()));
            boolean returned = false;
            if (aqua < MAX_AQUA_HP) {
                int progress = regenProgress.getOrDefault(uuid, 0) + 1;
                if (progress >= REGEN_INTERVAL_TICKS) {
                    aqua = Math.min(MAX_AQUA_HP, aqua + 2);
                    progress = 0;
                    returned = true;
                }
                regenProgress.put(uuid, progress);
            } else {
                regenProgress.put(uuid, 0);
            }
            aquaHp.put(uuid, aqua);

            syncMaxHealth(player, holding, aqua);
            if (returned && holding) {
                player.setHealth(Math.min(player.getHealth() + 2, player.getMaxHealth()));
            }
        }
    }

    private static void syncMaxHealth(ServerPlayer player, boolean holding, int aqua) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        AttributeModifier existing = maxHealth.getModifier(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "aqua_hearts"));
        if (!holding) {
            if (existing != null) maxHealth.removeModifier(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "aqua_hearts"));
            return;
        }
        double amount = NORMAL_HP + aqua - maxHealth.getBaseValue();
        if (existing != null && existing.amount() == amount) return;
        maxHealth.removeModifier(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "aqua_hearts"));
        maxHealth.addTransientModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "aqua_hearts"), amount, AttributeModifier.Operation.ADD_VALUE));
    }
}
