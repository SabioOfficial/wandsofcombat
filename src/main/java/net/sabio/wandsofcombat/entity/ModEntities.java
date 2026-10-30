package net.sabio.wandsofcombat.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    private static final ResourceKey<EntityType<?>> PHANTOM_BULLET_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("wandsofcombat", "phantom_bullet"));

    public static final EntityType<PhantomBulletEntity> PHANTOM_BULLET = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            PHANTOM_BULLET_KEY,
            EntityType.Builder.<PhantomBulletEntity>of(PhantomBulletEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(PHANTOM_BULLET_KEY));

    private static final ResourceKey<EntityType<?>> STALK_SIGIL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("wandsofcombat", "stalk_sigil"));

    public static final EntityType<StalkSigilEntity> STALK_SIGIL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            STALK_SIGIL_KEY,
            EntityType.Builder.<StalkSigilEntity>of(StalkSigilEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(STALK_SIGIL_KEY));

    private static final ResourceKey<EntityType<?>> PHANTOM_REPLICA_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("wandsofcombat", "phantom_replica"));

    public static final EntityType<PhantomReplicaEntity> PHANTOM_REPLICA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            PHANTOM_REPLICA_KEY,
            EntityType.Builder.<PhantomReplicaEntity>of(PhantomReplicaEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build(PHANTOM_REPLICA_KEY));

    public static void init() {}
}