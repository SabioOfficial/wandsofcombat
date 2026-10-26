package net.sabio.wandsofcombat.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class TargetFilter {
    private TargetFilter() {}

    public static boolean isHostileOrPlayer(LivingEntity entity) {
        return entity instanceof Enemy || entity instanceof Player;
    }
}
