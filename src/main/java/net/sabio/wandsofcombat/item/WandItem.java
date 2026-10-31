package net.sabio.wandsofcombat.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.sabio.wandsofcombat.mana.ManaManager;
import org.jspecify.annotations.NonNull;

public abstract class WandItem extends Item {
    protected WandItem(Properties settings, float attackDamageBonus, float attackSpeed) {
        super(ToolMaterial.DIAMOND.applyToolProperties(settings, BlockTags.SWORD_EFFICIENT, attackDamageBonus, attackSpeed, 0.0f));
    }

    public static boolean isUltimateInput(Player player) {
        return player.isCrouching();
    }

    public abstract int castCost(Player player);

    protected static boolean canAfford(Player player, int cost) {
        if (ManaManager.hasEnough(player, cost)) return true;
        ManaManager.playInsufficientManaSound(player);
        return false;
    }

    protected void onWandHit(ServerLevel level, Player attacker, LivingEntity target) {}

    @Override
    public final void postHurtEnemy(@NonNull ItemStack stack, @NonNull LivingEntity target, @NonNull LivingEntity attacker) {
        if (attacker instanceof Player player && attacker.level() instanceof ServerLevel level) {
            onWandHit(level, player, target);
        }
        super.postHurtEnemy(stack, target, attacker);
    }
}
