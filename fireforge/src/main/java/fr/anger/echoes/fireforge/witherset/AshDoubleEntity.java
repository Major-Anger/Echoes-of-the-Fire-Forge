package fr.anger.echoes.fireforge.witherset;

import fr.anger.echoes.fireforge.registry.FFEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Le double, « Cendre d'<pseudo> » (plan, section 13, étape 13c) : ce que devient le porteur du set du Wither à sa mort,
 * une fois le palier 4 atteint. Il a la taille d'un wither squelette (2,4 blocs), le skin du joueur noirci, et se bat
 * comme un wither squelette : à mains nues, ses coups infligent Wither 10 s. Il ne garde que le set, et ne le rend qu'à
 * qui le tue. Il ne brûle pas, et ne disparaît jamais (même en paisible) : le set ne se perd pas.
 */
public class AshDoubleEntity extends Monster {

    /** Le joueur dont il est le double : son skin (côté client). */
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(AshDoubleEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** Comme les coups d'un wither squelette : 10 s de Wither. */
    public static final int WITHER_TICKS = 200;
    /** Rendu sans faute : plus de 1, il tombe toujours, et sans usure. */
    private static final float SURE_DROP = 2.0F;

    public AshDoubleEntity(EntityType<? extends AshDoubleEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractPiglin.class, true));
    }

    // ------------------------------------------------------------------ la naissance

    /**
     * Le joueur meurt au palier 4 (de la cendre, ou d'autre chose) : son double se lève là où il est tombé, avec les
     * pièces du set qu'il portait (elles ne s'enlevaient plus), et elles quittent le joueur, même avec keepInventory.
     */
    public static AshDoubleEntity rise(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AshDoubleEntity ash = FFEntities.ASH_DOUBLE.get().create(level);
        Vec3 at = safeSpot(level, player.position());
        ash.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
        ash.setYHeadRot(player.getYRot());
        ash.entityData.set(OWNER, Optional.of(player.getUUID()));
        ash.setCustomName(nameOf(player.getGameProfile().getName()));
        ash.setCustomNameVisible(true);
        for (EquipmentSlot slot : WitherSet.SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!WitherSet.isPiece(piece)) continue;
            ash.setItemSlot(slot, piece.copy());
            ash.setDropChance(slot, SURE_DROP);
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        level.addFreshEntity(ash);
        level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y + 1.2, at.z, 80, 0.5, 1.0, 0.5, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.2, at.z, 12, 0.4, 0.8, 0.4, 0.01);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 1.2F, 0.6F);
        return ash;
    }

    /** « Cendre d'Anger », « Cendre de Bob » (élision devant une voyelle ou un h) ; « Ash of … » en anglais. */
    public static Component nameOf(String player) {
        String first = player.isEmpty() ? "" : player.substring(0, 1).toLowerCase(Locale.ROOT);
        boolean elided = !first.isEmpty() && "aeiouyhàâäéèêëîïôöùûü".contains(first);
        return Component.translatable("entity.echoes_fireforge.ash_double." + (elided ? "of_elided" : "of"), player);
    }

    /** Tombé dans le vide, il se relève sur le sol le plus haut au-dessus (ou au point d'apparition du monde). */
    private static Vec3 safeSpot(ServerLevel level, Vec3 position) {
        if (position.y >= level.getMinBuildHeight()) return position;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(position));
        if (top.getY() > level.getMinBuildHeight()) return Vec3.atBottomCenterOf(top);
        return Vec3.atBottomCenterOf(level.getSharedSpawnPos());
    }

    @Nullable
    public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    // ------------------------------------------------------------------ comme un wither squelette

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS), this);
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 1.0F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && getRandom().nextInt(3) == 0) {
            level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, -0.02, 0);
        }
    }

    // ------------------------------------------------------------------ il ne disparaît jamais

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** Tué, il rend le set entier (rareté, enchantements, mécanisme), sans le Lien de cendre. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        for (EquipmentSlot slot : WitherSet.SLOTS) WitherSet.setBound(level, getItemBySlot(slot), false);
        super.dropCustomDeathLoot(level, source, recentlyHit);
    }

    // ------------------------------------------------------------------ sauvegarde

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = owner();
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("owner")) entityData.set(OWNER, Optional.of(tag.getUUID("owner")));
    }
}
