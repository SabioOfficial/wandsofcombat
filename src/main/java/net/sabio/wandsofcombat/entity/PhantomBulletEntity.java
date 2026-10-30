package net.sabio.wandsofcombat.entity;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sabio.wandsofcombat.util.HitUtil;

public class PhantomBulletEntity extends Projectile {
    // Config
    private static final double START_SPEED = 0.35;
    private static final double END_SPEED = 1.5;
    private static final int RAMP_TICKS = 4;
    private static final int MAX_FLIGHT = 40;
    private static final int FINISHER_SLOT = 3;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(0xB07CFF, 0.8f);
    private static final DustParticleOptions TRAIL_CORE = new DustParticleOptions(0xF0E4FF, 0.5f);

    public Vec3 origin;

    private LivingEntity target;
    private float damage;
    private int chargeTicks;
    private boolean launched;
    private int slot;
    private Vec3 aimDir = Vec3.ZERO;
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(PhantomBulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE = SynchedEntityData.defineId(PhantomBulletEntity.class, EntityDataSerializers.INT);

    public int getTargetId() {
        return entityData.get(DATA_TARGET);
    }

    private static double flightSpeed(int k) {
        double t = Math.min(1.0, (k + 1) / (double) RAMP_TICKS);
        return START_SPEED + (END_SPEED - START_SPEED) * t * t;
    }

    public PhantomBulletEntity(EntityType<? extends PhantomBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(ServerPlayer owner, LivingEntity target, float damage, int chargeTicks, int slot) {
        setOwner(owner);
        this.slot = slot;
        this.target = target;
        this.damage = damage;
        this.chargeTicks = chargeTicks;
        entityData.set(DATA_TARGET, target.getId());
        entityData.set(DATA_CHARGE, chargeTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_CHARGE, 0);
    }

    public boolean isLaunched() {
        return getDeltaMovement().lengthSqr() > 1.0E-3;
    }

    @Override
    public void tick() {
        super.tick();
        if (origin == null) origin = position();

        if (level().isClientSide()) {
            if (isLaunched()) {
                Vec3 from = position();
                int k = Math.max(0, tickCount - entityData.get(DATA_CHARGE));
                Vec3 to = from.add(getDeltaMovement().normalize().scale(flightSpeed(k)));
                setPos(to.x, to.y, to.z);
                for (int i = 0; i < 4; i++) {
                    Vec3 p = from.lerp(to, i / 4.0);
                    level().addParticle(i % 2 == 0 ? TRAIL : TRAIL_CORE, p.x, p.y, p.z, 0, 0, 0);
                }
                if (random.nextInt(3) == 0) {
                    level().addParticle(ParticleTypes.END_ROD, to.x, to.y, to.z, (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.04);
                }
            } else if (tickCount % 3 == 0) {
                double ox = (random.nextDouble() - 0.5) * 0.9;
                double oy = (random.nextDouble() - 0.5) * 0.9;
                double oz = (random.nextDouble() - 0.5) * 0.9;
                level().addParticle(ParticleTypes.ENCHANT, getX() + ox, getY() + oy, getZ() + oz, -ox, -oy, -oz);
            }
            return;
        }

        if (target == null || !target.isAlive() || tickCount > chargeTicks + MAX_FLIGHT) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.03);
            }
            discard();
            return;
        }
        if (tickCount < chargeTicks) return;

        if (!launched) {
            launched = true;
            RandomSource r = random;
            Vec3 aim = target.getBoundingBox().getCenter().add(
                    (r.nextDouble() - 0.5) * 0.2,
                    (r.nextDouble() - 0.5) * 0.2,
                    (r.nextDouble() - 0.5) * 0.2);
            aimDir = aim.subtract(position()).normalize();
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.35f, 1.4f + r.nextFloat() * 0.4f);
        }

        setDeltaMovement(aimDir.scale(flightSpeed(tickCount - chargeTicks)));
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        AABB hitBox = target.getBoundingBox().inflate(0.3);
        if (hitBox.contains(from) || hitBox.clip(from, to).isPresent()) {
            impact();
            return;
        }
        setPos(to.x, to.y, to.z);
    }

    private void impact() {
        if (level() instanceof ServerLevel sl) {
            HitUtil.hurtThroughIFrames(sl, target, HitUtil.wandMagic(sl, this, getOwner()), damage);
            float pitch = 0.9f + slot * 0.12f + random.nextFloat() * 0.05f;
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, pitch);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 0.8f, pitch * 0.9f);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.4f, 0.6f + slot * 0.05f);
            if (slot >= FINISHER_SLOT) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.7f, 1.1f);
            }
            for (int i = 0; i < 8; i++) {
                double a = i * (Math.PI / 4);
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, Math.cos(a), 0.3, Math.sin(a), 0.12);
            }
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.2);
        }
        discard();
    }
}