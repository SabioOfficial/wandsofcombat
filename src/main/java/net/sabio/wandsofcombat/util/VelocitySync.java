package net.sabio.wandsofcombat.util;

import net.minecraft.world.entity.Entity;

public final class VelocitySync {
    private VelocitySync() {}

    public static void mark(Entity entity) {
        //? if >=26.3 {
        entity.syncVelocity = true;
        //?} else {
        /*entity.hurtMarked = true;
        *///?}
    }
}
