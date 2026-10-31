package net.sabio.wandsofcombat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
    }
}
