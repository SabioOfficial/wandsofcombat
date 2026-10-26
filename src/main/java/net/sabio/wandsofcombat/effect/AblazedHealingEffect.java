package net.sabio.wandsofcombat.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class AblazedHealingEffect extends MobEffect {
    public AblazedHealingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xED541C);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.isOnFire() && entity.getRemainingFireTicks() > 0) {
            entity.heal(0.1f);
            entity.setRemainingFireTicks(0);
            if (entity.tickCount % 5 == 0) {
                level.sendParticles(new DustParticleOptions(0xED541C, 1.5f), entity.getX(), entity.getY() + 1.0, entity.getZ(), 6, 0.3, 0.4, 0.3, 0);
                level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.5, entity.getZ(), 4, 0.2, 0.3, 0.2, 0.02);
            }
        }
        return true;
    }
}
