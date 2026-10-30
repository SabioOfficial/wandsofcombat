package net.sabio.wandsofcombat.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PhantomReplicaEntity extends Projectile {
    // config / new mechanic soon that will change this
    private static final double LAUNCH_SPEED = 1.6;
    private static final int MAX_FLIGHT = 20;
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(PhantomReplicaEntity.class, EntityDataSerializers.INT);

    private LivingEntity target;
    private Vec3 offset = Vec3.ZERO;
    private float damage;
    private int slot;
    private boolean finisher;
    private boolean launched;
    private int flightTicks;

    public PhantomReplicaEntity(EntityType<? extends PhantomReplicaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(ServerPlayer owner, LivingEntity target, Vec3 offset, float damage, int slot, boolean finisher) {
        setOwner(owner);
        this.target = target;
        this.offset = offset;
        this.damage = damage;
        this.slot = slot;
        this.finisher = finisher;
        entityData.set(DATA_OWNER, owner.getId());
        setPos(target.getX() + offset.x, target.getY() + offset.y, target.getZ() + offset.z);
        setYRot(yawToTarget());
    }

    public int getOwnerEntityId() {
        return entityData.get(DATA_OWNER);
    }

    public void launch() {
        if (launched) return;
        launched = true;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.35f, 1.5f + slot * 0.04f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
    }

    private float yawToTarget() {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        return (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (!(level() instanceof ServerLevel sl)) return;

        if (target == null || !target.isAlive() || target.isRemoved() || target.level() != level() || flightTicks > MAX_FLIGHT) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.9, getZ(), 6, 0.2, 0.4, 0.2, 0.03);
            discard();
            return;
        }

        if (!launched) {
            setPos(target.getX() + offset.x, target.getY() + offset.y, target.getZ() + offset.z);
            setYRot(yawToTarget());
            if (tickCount % 4 == 0) {
                sl.sendParticles(new DustParticleOptions(0xB07CFF, 0.9f), getX(), getY() + 0.9, getZ(), 2, 0.25, 0.6, 0.25, 0.0);
            }
            return;
        }

        flightTicks++;
        Vec3 center = position().add(0, getBbHeight() * 0.5, 0);
        Vec3 aim = target.getBoundingBox().getCenter();
        Vec3 toTarget = aim.subtract(center);
        double dist = toTarget.length();
        if (dist <= LAUNCH_SPEED) {
            impact(sl);
            return;
        }

        Vec3 step = toTarget.scale(LAUNCH_SPEED / dist);
        Vec3 next = position().add(step);
        for (int i = 0; i < 4; i++) {
            Vec3 p = center.add(step.scale(i / 4.0));
            sl.sendParticles(i % 2 == 0 ? new DustParticleOptions(0xB07CFF, 0.9f) : new DustParticleOptions(0xF0E4FF, 0.6f), p.x, p.y, p.z, 1, 0.05, 0.1, 0.05, 0.0);
        }
        setPos(next.x, next.y, next.z);
        setYRot(yawToTarget());
    }

    private void impact(ServerLevel sl) {
        //? if>=26.3 {
        target.setInvulnerableTime(0);
        //?} else {
        /*target.invulnerableTime = 0;
        *///?}
        target.hurtServer(sl, damageSources().indirectMagic(this, getOwner()), damage);

        double cy = target.getY() + target.getBbHeight() * 0.5;
        float pitch = 0.9f + slot * 0.1f;
        sl.playSound(null, target.getX(), cy, target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, pitch);
        sl.playSound(null, target.getX(), cy, target.getZ(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, .8f, pitch * 0.9f);
        sl.playSound(null, target.getX(), cy, target.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.35f, 1.7f);
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI / 4);
            sl.sendParticles(ParticleTypes.END_ROD, target.getX(), cy, target.getZ(), 0, Math.cos(a), 0.3, Math.sin(a), 0.14);
        }
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), cy, target.getZ(), 10, 0.25, target.getBbHeight() * 0.3, .25, 0.15);
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), cy, target.getZ(), 6, 0.2, 0.2, 0.2, 0.2);

        if (finisher) {
            sl.playSound(null, target.getX(), cy, target.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, .8f);
            sl.playSound(null, target.getX(), cy, target.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7f, 1.1f);
            for (int i = 0; i < 24; i++) {
                double a = i * (Math.PI * 2 / 24);
                sl.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.15, target.getZ(), 0, Math.cos(a), 0.0, Math.sin(a), 0.2);
            }
        }
        discard();
    }
}
