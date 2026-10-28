package net.sabio.wandsofcombat.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public class StalkSigilEntity extends Projectile {
    public static final int FADE_IN = 8;
    public static final int FADE_OUT = 10;
    private static final float LARGE_WIDTH = 1.6f;
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(StalkSigilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LARGE = SynchedEntityData.defineId(StalkSigilEntity.class, EntityDataSerializers.BOOLEAN);
    private LivingEntity victim;

    public StalkSigilEntity(EntityType<? extends StalkSigilEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static boolean isLargeFor(LivingEntity victim) {
        return victim.getBbWidth() > LARGE_WIDTH;
    }

    public static float sizeFor(boolean large) {
        return large ? 3.0f : 2.0f;
    }

    public void setup(LivingEntity victim, int lifeTicks) {
        this.victim = victim;
        entityData.set(DATA_LIFE, lifeTicks);
        entityData.set(DATA_LARGE, isLargeFor(victim));
        setPos(victim.getX(), victim.getY(), victim.getZ());
    }

    public int getLife() {
        return entityData.get(DATA_LIFE);
    }

    public boolean isLarge() {
        return entityData.get(DATA_LARGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 0);
        builder.define(DATA_LARGE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (victim == null || victim.isRemoved() || !victim.isAlive() || tickCount >= getLife()) {
            discard();
            return;
        }
        setPos(victim.getX(), victim.getY(), victim.getZ());
    }
}