package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import fr.anger.echoes.fireforge.witherset.AshDoubleEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = EchoesFireForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FFEntities {

    private FFEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, EchoesFireForge.MOD_ID);

    /** Le Cendreux (plan, 12c) : 2,3 blocs, ne brûle pas. */
    public static final DeferredHolder<EntityType<?>, EntityType<CendreuxEntity>> CENDREUX = ENTITIES.register("cendreux",
            () -> EntityType.Builder.of(CendreuxEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.3F).eyeHeight(2.0F).fireImmune().clientTrackingRange(8)
                    .build(EchoesFireForge.id("cendreux").toString()));

    /** Le double, « Cendre d'<pseudo> » (plan, 13c) : la taille d'un wither squelette, ne brûle pas. */
    public static final DeferredHolder<EntityType<?>, EntityType<AshDoubleEntity>> ASH_DOUBLE = ENTITIES.register("ash_double",
            () -> EntityType.Builder.of(AshDoubleEntity::new, MobCategory.MONSTER)
                    .sized(0.72F, 2.4F).eyeHeight(2.1F).fireImmune().clientTrackingRange(8)
                    .build(EchoesFireForge.id("ash_double").toString()));

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(CENDREUX.get(), CendreuxEntity.createAttributes().build());
        event.put(ASH_DOUBLE.get(), AshDoubleEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(CENDREUX.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CendreuxEntity::checkCendreuxSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
