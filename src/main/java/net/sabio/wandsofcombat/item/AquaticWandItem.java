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
import net.sabio.wandsofcombat.aqua.AquaHearts;
import net.sabio.wandsofcombat.mana.ManaCosts;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class AquaticWandItem extends WandItem {
    private static final float ATTACK_DAMAGE_BONUS = 4.0f;
    private static final float ATTACK_SPEED = -2.6f;

    public AquaticWandItem(Properties settings) {
        super(settings, ATTACK_DAMAGE_BONUS, ATTACK_SPEED);
    }

    @Override
    public int castCost(Player player) {
        boolean client = player.level().isClientSide();
        boolean unlocked = client ? AquaHearts.isClientUnlocked() : AquaHearts.canUseAbilities(player);
        if (!unlocked) return -1;
        int tier = client ? AquaHearts.getClientTier() : AquaHearts.getTier(player);
        return ManaCosts.aquaticTsunami(tier);
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level world, @NonNull Player player, @NonNull InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof AquaticWandItem) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        if (!AquaticWandTsunamiHandler.tryBeginCast(serverPlayer)) return InteractionResult.FAIL;
        if (AquaticWandTsunamiHandler.isCharging(serverPlayer)) serverPlayer.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(@NonNull ItemStack stack, @NonNull LivingEntity user) {
        return 72000;
    }

    @Override
    protected void onWandHit(ServerLevel level, Player player, LivingEntity target) {
        AquaticWandComboHandler.onHit(player, target);
    }

    public static void addTooltip(Consumer<Component> tooltip) {
        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_PASSIVE))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Aqua-fy", ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("While held, max health is ❤8: 5 normal hearts").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("and 3 Aqua Hearts that are always last. Aqua").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("Hearts return after 30s each, which cannot be").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("regenerated naturally. Abilities unlock at ❤4").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("or less after enemy damage and strengthen with").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("lower health.").withStyle(ChatFormatting.GRAY));


        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_COMBO))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Water Hands", ChatFormatting.GOLD))
                .append(Component.literal(" \uD83D\uDDE18/6/4").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("Every 8/6/4 hits (3/2/1 Aqua Hearts) weakens").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("the victim for 2/3/4s, and weakens strongest").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("at 1 heart. Each hit the victim lands on you").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("heals ❤0.5 once per second, but will never heal").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("Aqua Hearts. It can't be weakened again until 3s").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("after it ends.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ABILITY))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("Tsunami", ChatFormatting.RED))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal("5/4/3").withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("Launch a wave (4x5 / 6x8 / 8x12) that lingers").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("for 2s and pushes enemies forward. Deals ❤1.5/").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("2/3 per second (❤0.5/0.75/1 on players) ignoring").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("armor. Casts in 1.5s / 1s / instantly, but taking").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("damage while casting cancels the spell. Gets").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("quieter as Aqua Hearts are lost.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.empty());

        tooltip.accept(Component.empty()
                .append(TooltipIcons.icon(TooltipIcons.BADGE_ULTIMATE))
                .append(Component.literal(" "))
                .append(Component.literal("Crouch").withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE, ChatFormatting.WHITE))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.RIGHT_CLICK_ICON))
                .append(Component.literal(" "))
                .append(TooltipIcons.title("???", ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" "))
                .append(TooltipIcons.icon(TooltipIcons.MANA_ICON))
                .append(Component.literal("?").withStyle(ChatFormatting.AQUA)));
        tooltip.accept(Component.literal("?????????????????????????????????????").withStyle(ChatFormatting.GRAY));
    }
}
