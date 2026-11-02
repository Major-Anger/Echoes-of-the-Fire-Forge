package fr.anger.echoes.fireforge.cendreux;

import fr.anger.echoes.fireforge.registry.FFStructures;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.progress.ForgeEchoes;
import fr.anger.echoes.fireforge.registry.FFSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Le Cendreux (plan, 12c) : un Attiseur à mi-chemin de la maladie, presque wither squelette. Il garde ses lunettes et
 * son tablier, et entretient encore les feux éteints (il rallume les feux de camp morts).
 * <p>
 * Neutre tant qu'on ne touche pas aux machines des Attiseurs autour de lui (casser ou utiliser une forge, un établi,
 * une machine comprimée : tag {@link #ATTISEUR_MACHINES}) ; ensuite il lance des braises. Il tousse de la cendre.
 * On ne dit jamais d'où vient sa maladie.
 */
public class CendreuxEntity extends Monster implements RangedAttackMob {

    /** Les machines qu'un Cendreux défend. */
    public static final TagKey<Block> ATTISEUR_MACHINES = BlockTags.create(EchoesFireForge.id("attiseur_machines"));
    /** Distance à laquelle les Cendreux voient qu'on touche aux machines. */
    public static final int PROVOKE_RANGE = 16;
    /** Combien de temps ils restent hostiles (30 s), si on ne les frappe pas. */
    public static final int ANGER_TICKS = 600;
    /** Le succès « Les noyés de cendre » : vu à cette distance. */
    public static final int SIGHT_RANGE = 16;
    /** Événement d'entité : il tousse (bouffée de cendre, côté client). */
    public static final byte COUGH_EVENT = 70;

    @Nullable
    private UUID provokedBy;
    private int angerTicks;

    public CendreuxEntity(EntityType<? extends CendreuxEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
        // il ne brûle pas : la lave et le feu ne lui font pas peur
        setPathfindingMalus(PathType.LAVA, 8.0F);
        setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 60, 14.0F));
        goalSelector.addGoal(4, new RekindleGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new ProvokedTargetGoal(this));
    }

    // ------------------------------------------------------------------ la colère

    /** Ce joueur a touché aux machines : le Cendreux s'en prend à lui pendant 30 s. */
    public void provoke(Player player) {
        provokedBy = player.getUUID();
        angerTicks = ANGER_TICKS;
    }

    /** Tous les Cendreux proches de ce bloc s'en prennent au joueur (sauf en créatif ou spectateur). */
    public static void provokeNear(ServerLevel level, BlockPos pos, Player player) {
        if (player.isCreative() || player.isSpectator()) return;
        for (CendreuxEntity cendreux : level.getEntitiesOfClass(CendreuxEntity.class, new AABB(pos).inflate(PROVOKE_RANGE))) {
            cendreux.provoke(player);
        }
    }

    /** Le joueur à qui il en veut, s'il est encore là et que la colère dure. */
    @Nullable
    public Player provoker() {
        return provokedBy == null || angerTicks <= 0 ? null : level().getPlayerByUUID(provokedBy);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // la colère retombe : ProvokedTargetGoal lâche sa cible (s'il a été frappé, HurtByTargetGoal garde la sienne)
        if (angerTicks > 0 && --angerTicks == 0) provokedBy = null;
        setAggressive(getTarget() != null);
        if (tickCount % 40 == 0) {
            // « Les noyés de cendre » : les joueurs qui le voient
            for (Player player : level().players()) {
                if (player instanceof ServerPlayer seer && !seer.isSpectator() && seer.distanceToSqr(this) < SIGHT_RANGE * SIGHT_RANGE
                        && seer.hasLineOfSight(this)) ForgeEchoes.cendreuxSeen(seer);
            }
        }
    }

    // ------------------------------------------------------------------ les braises

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dy = target.getY(0.5) - getY(0.6);
        double spread = Math.sqrt(Math.sqrt(dx * dx + dz * dz)) * 0.25;
        Vec3 direction = new Vec3(getRandom().triangle(dx, spread), dy, getRandom().triangle(dz, spread));
        SmallFireball ember = new SmallFireball(level(), this, direction.normalize());
        ember.setPos(ember.getX(), getY(0.6), ember.getZ());
        level().addFreshEntity(ember);
        playSound(FFSounds.CENDREUX_EMBER.get(), 1.0F, 0.8F + getRandom().nextFloat() * 0.3F);
    }

    // ------------------------------------------------------------------ la toux, la cendre

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        level().broadcastEntityEvent(this, COUGH_EVENT);
    }

    /** Une bouffée de cendre (la toux), vers l'avant. */
    public void cough() {
        level().broadcastEntityEvent(this, COUGH_EVENT);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != COUGH_EVENT) {
            super.handleEntityEvent(id);
            return;
        }
        Vec3 look = getViewVector(1.0F);
        Vec3 mouth = getEyePosition().add(look.scale(0.4)).subtract(0, 0.2, 0);
        RandomSource random = getRandom();
        for (int i = 0; i < 8; i++) {
            level().addParticle(i % 2 == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH, mouth.x, mouth.y, mouth.z,
                    look.x * 0.08 + random.nextGaussian() * 0.02, 0.01, look.z * 0.08 + random.nextGaussian() * 0.02);
        }
        level().addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, look.x * 0.05, 0.02, look.z * 0.05);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // de la cendre se détache de lui
        if (level().isClientSide && getRandom().nextInt(6) == 0) {
            level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, -0.02, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FFSounds.CENDREUX_COUGH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FFSounds.CENDREUX_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FFSounds.CENDREUX_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 0.9F);
    }

    // ------------------------------------------------------------------ apparition

    /**
     * Rarement, en terres désolées et deltas de basalte, près de la lave (biome_modifier/cendreux.json) ; rarement dans
     * les forteresses du Nether (structure_modifier/cendreux_fortress.json) ; dans les chaufferies et les derniers foyers
     * (plan, 12b : leurs « spawn_overrides »). Dans ces lieux (le tag de structures cendreux_homes), la lave n'est pas
     * demandée.
     */
    public static boolean checkCendreuxSpawnRules(EntityType<CendreuxEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                                  BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        return atHome(level, pos) || lavaNear(level, pos);
    }

    /** Dans une forteresse, une chaufferie ou un dernier foyer (le tag de structures echoes_fireforge:cendreux_homes). */
    public static boolean atHome(ServerLevelAccessor level, BlockPos pos) {
        return level.getLevel().structureManager().getStructureWithPieceAt(pos, FFStructures.CENDREUX_HOMES).isValid();
    }

    /** De la lave à 6 blocs ou moins. */
    public static boolean lavaNear(ServerLevelAccessor level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-6, -3, -6), pos.offset(6, 2, 6))) {
            if (level.getFluidState(p).is(FluidTags.LAVA)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ sauvegarde

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (provokedBy != null && angerTicks > 0) {
            tag.putUUID("provoked_by", provokedBy);
            tag.putInt("anger", angerTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        provokedBy = tag.hasUUID("provoked_by") ? tag.getUUID("provoked_by") : null;
        angerTicks = tag.getInt("anger");
    }
}
