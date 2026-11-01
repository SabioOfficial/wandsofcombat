package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public class AquaticWandItem extends WandItem {
    private static final float ATTACK_DAMAGE_BONUS = 4.0f;
    private static final float ATTACK_SPEED = -2.6f;

    public AquaticWandItem(Properties settings) {
        super(settings, ATTACK_DAMAGE_BONUS, ATTACK_SPEED);
    }

    @Override
    public int castCost(Player player) {
        return -1;
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
        tooltip.accept(Component.empty());

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
    }
}
