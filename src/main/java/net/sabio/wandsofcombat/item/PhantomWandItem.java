package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.sabio.wandsofcombat.mana.ManaCosts;
import net.sabio.wandsofcombat.mana.ManaManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class PhantomWandItem extends Item {
    private static final float ATTACK_DAMAGE_BONUS = 1.0f; // 5 total attack damage
    private static final float ATTACK_SPEED = -2.0f;
    public static final Set<UUID> pullingPlayers = new HashSet<>();
    public PhantomWandItem(Properties settings) {
        super(ToolMaterial.DIAMOND.applyToolProperties(
                settings,
                BlockTags.SWORD_EFFICIENT,
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED,
                0.0f
        ));
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (!ManaManager.hasEnough(player, ManaCosts.PHANTOM_WAND)) {
            ManaManager.playInsufficientManaSound(player);
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide() && PhantomWandStalkHandler.isStalking(player)) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, displayComponent, tooltip, flag);

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_PASSIVE))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Latching", ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Interaction range is increased by 2 blocks").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("and allows the user to \"latch\" onto enemies.").withStyle(ChatFormatting.GRAY));
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
    }
}
