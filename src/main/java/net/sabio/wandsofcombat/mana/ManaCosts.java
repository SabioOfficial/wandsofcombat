package net.sabio.wandsofcombat.mana;

import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.item.*;

public final class ManaCosts {
    private ManaCosts() {}

    public static final int MAGNET_WAND = 3;
    public static final int ICE_WAND_LITE = 4;
    public static final int ICE_WAND_FULL = 5;
    public static final int ELECTRIC_WAND = 4;
    public static final int PHANTOM_WAND = 5;
    public static final int PHANTOM_ULTIMATE = 8;
    public static final int MAGMA_ABILITY = 3;
    public static final int MAGMA_ULTIMATE = 8;
    private static final int[] AQUATIC_TSUNAMI = {3, 4, 5};

    public static int aquaticTsunami(int tier) {
        return AQUATIC_TSUNAMI[Math.clamp(tier, 1, 3) - 1];
    }

    public static int nextCastCost(Player player) {
        return player.getMainHandItem().getItem() instanceof WandItem wand ? wand.castCost(player) : -1;
    }
}
