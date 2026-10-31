package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;
import net.sabio.wandsofcombat.util.HitUtil;
import net.sabio.wandsofcombat.util.TargetFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ElectricWandItem extends WandItem {
    private static final float ATTACK_DAMAGE_BONUS = 4.0f; // total atk damage: 8
    private static final float ATTACK_SPEED = -3f;
    private static final double ABILITY_RANGE = 12.0;
    private static final int LIGHTNING_COUNT = 3;
    private static final Map<UUID, Integer> hitCounters = new HashMap<>();

    public ElectricWandItem(Properties settings) {
        super(settings, ATTACK_DAMAGE_BONUS, ATTACK_SPEED);
    }

    @Override
    public int castCost(Player player) {
        return ManaCosts.ELECTRIC_WAND;
    }

    public static void strikeLightningOn(Entity target, ServerLevel world, Player attacker) {
        LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, world);
        lightning.snapTo(target.getX(), target.getY(), target.getZ());
        lightning.setVisualOnly(true);
        world.addFreshEntity(lightning);
        HitUtil.hurtThroughIFrames(world, target, HitUtil.wandMagic(world, attacker, attacker), 5.0f);
    }

    @Override
    protected void onWandHit(ServerLevel level, Player player, LivingEntity target) {
        UUID uuid = player.getUUID();
        int hits = hitCounters.getOrDefault(uuid, 0) + 1;
        if (hits >= 3) {
            hitCounters.put(uuid, 0);
            strikeLightningOn(target, level, player);
        } else {
            hitCounters.put(uuid, hits);
        }
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (!canAfford(player, ManaCosts.ELECTRIC_WAND)) return InteractionResult.FAIL;
        if (!world.isClientSide()) {
            ServerLevel ServerLevel = (ServerLevel) world;
            if (!ManaManager.tryConsume(player, ManaCosts.ELECTRIC_WAND)) return InteractionResult.FAIL;
            ServerLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.4f, ServerLevel.getRandom().nextLong());
            AABB box = player.getBoundingBox().inflate(ABILITY_RANGE);
            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && !entity.isRemoved() && TargetFilter.isHostileOrPlayer(entity));
            if (!targets.isEmpty()) {
                ElectricWandLightningHandler.scheduleAbility(ServerLevel, player, targets, LIGHTNING_COUNT);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void addTooltip(Consumer<Component> tooltip) {
        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_PASSIVE))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Electric Negation", ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Receive immunity to all electric damage.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_COMBO))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Zap", ChatFormatting.GOLD))
                .append(Component.literal(" \uD83D\uDDE13").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("Summon a lightning bolt that hits the enemy,").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("doing ❤2.5 to struck entities.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ABILITY))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Thunderstorm", ChatFormatting.RED))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(String.valueOf(ManaCosts.ELECTRIC_WAND)).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Strikes thrice on enemies in a " + (int) ABILITY_RANGE + " block").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("radius and stun them for 1.5s. If the enemy").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("has not been slain after the strikes, a").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.SKELETON_ICON))
                .append(Component.literal(" Skeleton with a stone sword will spawn").withStyle(ChatFormatting.GRAY)));
        tooltip.accept(Component.literal("to slay the enemy. Does not work on").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("friendly mobs.").withStyle(ChatFormatting.GRAY));
    }
}
