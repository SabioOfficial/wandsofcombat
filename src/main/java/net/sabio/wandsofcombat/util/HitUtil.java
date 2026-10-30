package net.sabio.wandsofcombat.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class HitUtil {
    private HitUtil() {}

    private static final ResourceKey<DamageType> WAND_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("wandsofcombat", "wand_magic"));

    public static DamageSource wandMagic(ServerLevel level, Entity direct, Entity causing) {
        Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(WAND_MAGIC);
        return new DamageSource(type, direct, causing);
    }

    public static boolean hurtThroughIFrames(ServerLevel level, Entity target, DamageSource source, float amount) {
        if (target instanceof LivingEntity living) {
            //? if>=26.3 {
            living.setInvulnerableTime(0);
            //?} else {
            /*living.invulnerableTime = 0;
            *///?}
        }
        return target.hurtServer(level, source, amount);
    }
}
