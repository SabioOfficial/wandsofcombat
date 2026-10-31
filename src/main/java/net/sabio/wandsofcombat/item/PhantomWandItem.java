package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;
import net.sabio.wandsofcombat.util.PhantomTargeting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class PhantomWandItem extends WandItem {
    private static final float ATTACK_DAMAGE_BONUS = 1.0f; // 5 total attack damage
    private static final float ATTACK_SPEED = -2.0f;
    public static final Set<UUID> pullingPlayers = new HashSet<>();

    public PhantomWandItem(Properties settings) {
        super(settings, ATTACK_DAMAGE_BONUS, ATTACK_SPEED);
    }

    @Override
    public int castCost(Player player) {
        return isUltimateInput(player) ? ManaCosts.PHANTOM_ULTIMATE : ManaCosts.PHANTOM_WAND;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (isUltimateInput(player)) return useUltimate(world, player);
        if (!canAfford(player, ManaCosts.PHANTOM_WAND)) return InteractionResult.FAIL;
        if (!world.isClientSide() && PhantomWandStalkHandler.isStalking(player)) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult useUltimate(Level world, Player player) {
        if (!ManaManager.hasEnough(player, ManaCosts.PHANTOM_ULTIMATE)) {
            ManaManager.playInsufficientManaSound(player);
            return InteractionResult.FAIL;
        }
        LivingEntity target = PhantomTargeting.findLookedAt(player, PhantomTargeting.ULTIMATE_RANGE);
        if (target == null || PhantomTargeting.isInvalidType(target) || !player.hasLineOfSight(target)) {
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (PhantomWandStalkHandler.isStalking(player)) return InteractionResult.FAIL;
            if (!PhantomWandUltimateHandler.tryCast(serverPlayer, target)) return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    protected void onWandHit(ServerLevel level, Player player, LivingEntity target) {
        PhantomWandComboHandler.onHit(player, target);
    }

    public static void addTooltip(Consumer<Component> tooltip) {
        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_PASSIVE))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Latching", ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Interaction range is increased by 2 blocks").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("and allows the user to \"latch\" onto enemies.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_COMBO))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Shadow Step", ChatFormatting.GOLD))
                .append(Component.literal(" \uD83D\uDDE16").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("You blink behind the target, and your next").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("hit within 2s does 1.5x damage.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ABILITY))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Dread", ChatFormatting.RED))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(String.valueOf(ManaCosts.PHANTOM_WAND)).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Hold for 1s on a target within 8 blocks to").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("freeze it for 4s (3s on players). It can't").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("move, jump or hurt anyone while spectral").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("bullets strike it for 1.5 damage each (1").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("on players), ignoring armor.").withStyle(ChatFormatting.GRAY));

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ULTIMATE))
                .append(Component.literal(" "))
                .append(Component.literal("Crouch").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.WHITE))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Hallucinizer", ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal(String.valueOf(ManaCosts.PHANTOM_ULTIMATE)).withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Launch 8 replicas of yourself in a ring").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("at a target within 12 blocks. Each lands").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("for 4 damage (1.5 on players), ignoring").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("armor. Weakens mobs for 4s and gives").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("players nausea for 5s.").withStyle(ChatFormatting.GRAY));
    }
}
