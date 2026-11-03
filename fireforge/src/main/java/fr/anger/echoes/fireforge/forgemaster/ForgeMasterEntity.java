package fr.anger.echoes.fireforge.forgemaster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import fr.anger.echoes.fireforge.registry.FFSounds;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Le maître de forge (plan, section 14) : un Attiseur changé par la maladie, resté bien plus grand et plus fort, qui
 * tient encore son marteau-pilon. Phase 1 : la frappe, l'onde de choc (qu'on saute), la grosse frappe annoncée par le
 * rouage qui s'emballe. Phase 2, sous la moitié de sa vie : il frappe plus vite et le marteau crache de la vapeur.
 */
public class ForgeMasterEntity extends Monster {

    /** Ce qu'il est en train de faire (synchronisé : le modèle l'anime). */
    public enum Action { NONE, SLAM_WINDUP, SLAM, CHARGE, BIG_STRIKE, STEAM }

    public static final int SLAM_WINDUP_TICKS = 16, SLAM_TICKS = 14, CHARGE_TICKS = 30, BIG_STRIKE_TICKS = 12, STEAM_TICKS = 40;
    public static final float SHOCKWAVE_DAMAGE = 10, BIG_STRIKE_DAMAGE = 22, STEAM_DAMAGE = 6;
    public static final double SHOCKWAVE_RADIUS = 6, BIG_STRIKE_REACH = 5.5, STEAM_REACH = 4.5;
    private static final int SLAM_COOLDOWN = 120, BIG_COOLDOWN = 200, STEAM_COOLDOWN = 80;

    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(ForgeMasterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PHASE_TWO = SynchedEntityData.defineId(ForgeMasterEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private int actionTicks, slamCooldown = 60, bigCooldown = 140, steamCooldown = 40;
    /** Côté client : le moment où l'action a commencé (pour l'animer). */
    private int actionStart;
    private double waveRadius = -1;
    private final Set<UUID> waveHit = new HashSet<>();
    /** Sa forge (l'arène), s'il en a une. */
    @org.jetbrains.annotations.Nullable
    private net.minecraft.core.BlockPos arena;

    public ForgeMasterEntity(EntityType<? extends ForgeMasterEntity> type, Level level) {
        super(type, level);
        xpReward = 50;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, 0);
        builder.define(PHASE_TWO, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new HammerGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, CendreuxEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Le corps à corps, plus vif en phase 2 ; rien pendant ses grands coups. */
    private static final class HammerGoal extends MeleeAttackGoal {
        private final ForgeMasterEntity master;

        HammerGoal(ForgeMasterEntity master) {
            super(master, 1.0, true);
            this.master = master;
        }

        @Override
        public boolean canUse() {
            return master.action() == Action.NONE && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return master.action() == Action.NONE && super.canContinueToUse();
        }

        @Override
        protected int getAttackInterval() {
            return adjustedTickDelay(master.isPhaseTwo() ? 17 : 28);
        }
    }

    // ------------------------------------------------------------------ état

    public Action action() {
        int i = entityData.get(ACTION);
        return i >= 0 && i < Action.values().length ? Action.values()[i] : Action.NONE;
    }

    public boolean isPhaseTwo() {
        return entityData.get(PHASE_TWO);
    }

    /** Côté client : depuis combien de ticks dure l'action en cours ({@code ageInTicks} : tickCount + la fraction). */
    public float actionAge(float ageInTicks) {
        return ageInTicks - actionStart;
    }

    private void setAction(Action action) {
        entityData.set(ACTION, action.ordinal());
        actionTicks = 0;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ACTION.equals(key)) actionStart = tickCount;
    }

    // ------------------------------------------------------------------ le combat

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isPhaseTwo() && getHealth() < getMaxHealth() / 2) enterPhaseTwo();
        slamCooldown--;
        bigCooldown--;
        steamCooldown--;
        tickWave();
        LivingEntity target = getTarget();
        Action action = action();
        if (action != Action.NONE) {
            getNavigation().stop();
            if (target != null) getLookControl().setLookAt(target, 30, 30);
            actionTicks++;
            tickAction(action, target);
            return;
        }
        if (target == null || !target.isAlive()) return;
        double distance = distanceTo(target);
        boolean two = isPhaseTwo();
        if (bigCooldown <= 0 && distance < 6) {
            setAction(Action.CHARGE);
            bigCooldown = two ? 140 : BIG_COOLDOWN;
        } else if (slamCooldown <= 0 && distance < 10) {
            setAction(Action.SLAM_WINDUP);
            slamCooldown = two ? 80 : SLAM_COOLDOWN;
        } else if (two && steamCooldown <= 0 && distance < 5) {
            setAction(Action.STEAM);
            steamCooldown = STEAM_COOLDOWN;
            waveHit.clear();
            playSound(FFSounds.FORGE_MASTER_STEAM.get(), 2.0F, 1.0F);
        }
    }

    private void tickAction(Action action, LivingEntity target) {
        switch (action) {
            case SLAM_WINDUP -> {
                if (actionTicks == 1) playSound(SoundEvents.PISTON_EXTEND, 1.5F, 0.5F);
                if (actionTicks == SLAM_WINDUP_TICKS - 4) playSound(FFSounds.FORGE_MASTER_SWING.get(), 1.8F, 0.9F);
                if (actionTicks >= SLAM_WINDUP_TICKS) {
                    setAction(Action.SLAM);
                    startWave();
                }
            }
            case SLAM -> {
                if (actionTicks >= SLAM_TICKS) setAction(Action.NONE);
            }
            case CHARGE -> {
                // le rouage s'emballe : un bruit qui monte, le temps de s'écarter
                if (actionTicks % 5 == 1) playSound(SoundEvents.GRINDSTONE_USE, 1.5F, 0.6F + actionTicks / (float) CHARGE_TICKS);
                if (actionTicks == 1) playSound(FFSounds.FORGE_MASTER_STEAM.get(), 1.2F, 1.2F);          // la vapeur fuit
                if (actionTicks == CHARGE_TICKS - 4) playSound(FFSounds.FORGE_MASTER_SWING.get(), 2.0F, 0.8F);
                if (actionTicks >= CHARGE_TICKS) {
                    setAction(Action.BIG_STRIKE);
                    bigStrike();
                }
            }
            case BIG_STRIKE -> {
                if (actionTicks >= BIG_STRIKE_TICKS) setAction(Action.NONE);
            }
            case STEAM -> {
                steamJet();
                if (actionTicks >= STEAM_TICKS) setAction(Action.NONE);
            }
            default -> setAction(Action.NONE);
        }
    }

    /** Il abat le marteau : une onde court au sol jusqu'à 6 blocs. */
    private void startWave() {
        waveRadius = 0.5;
        waveHit.clear();
        playSound(FFSounds.FORGE_MASTER_GROUND_HIT.get(), 2.5F, 1.0F);
    }

    private void tickWave() {
        if (waveRadius < 0) return;
        waveRadius += 0.5;
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 20; i++) {
                double a = i * Mth.TWO_PI / 20;
                server.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * waveRadius, getY() + 0.1, getZ() + Math.sin(a) * waveRadius,
                        1, 0.05, 0.02, 0.05, 0.01);
            }
        }
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SHOCKWAVE_RADIUS + 1, 2, SHOCKWAVE_RADIUS + 1))) {
            if (victim == this || victim instanceof CendreuxEntity || waveHit.contains(victim.getUUID())) continue;
            double d = Math.sqrt(Mth.square(victim.getX() - getX()) + Mth.square(victim.getZ() - getZ()));
            if (Math.abs(d - waveRadius) > 0.75 || Math.abs(victim.getY() - getY()) > 1.5) continue;
            // qui est en l'air au passage de l'onde n'est pas touché
            if (!victim.onGround()) continue;
            waveHit.add(victim.getUUID());
            if (victim.hurt(damageSources().mobAttack(this), SHOCKWAVE_DAMAGE)) {
                Vec3 away = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ()).normalize().scale(0.7);
                victim.push(away.x, 0.55, away.z);
                victim.hurtMarked = true;
            }
        }
        if (waveRadius >= SHOCKWAVE_RADIUS) waveRadius = -1;
    }

    /** Le coup annoncé : devant lui, il traverse les boucliers. */
    private void bigStrike() {
        playSound(FFSounds.FORGE_MASTER_GROUND_HIT.get(), 2.5F, 0.85F);
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BIG_STRIKE_REACH, 2, BIG_STRIKE_REACH))) {
            if (victim == this || victim instanceof CendreuxEntity) continue;
            Vec3 to = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ());
            if (to.length() > BIG_STRIKE_REACH || (to.length() > 1 && to.normalize().dot(look) < 0.5)) continue;
            if (victim instanceof Player player && player.isBlocking()) player.disableShield();
            if (victim.hurt(damageSources().mobAttack(this), BIG_STRIKE_DAMAGE)) {
                victim.playSound(FFSounds.FORGE_MASTER_HIT.get(), 1.5F, 0.9F);
                Vec3 push = to.length() > 0.01 ? to.normalize().scale(1.1) : look.scale(1.1);
                victim.push(push.x, 0.4, push.z);
                victim.hurtMarked = true;
            }
        }
        if (level() instanceof ServerLevel server) {
            Vec3 at = position().add(look.scale(2.5));
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, getY() + 0.3, at.z, 2, 0.5, 0.1, 0.5, 0);
        }
    }

    /** La vapeur par l'arrière du marteau : elle brûle et repousse ce qui est derrière lui et sur ses flancs. */
    private void steamJet() {
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        if (level() instanceof ServerLevel server && actionTicks % 2 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(getYRot() + 90 + random.nextGaussian() * 50);
                double r = 1 + random.nextDouble() * STEAM_REACH;
                server.sendParticles(ParticleTypes.CLOUD, getX() - Math.sin(a) * r, getY() + 0.6 + random.nextDouble(), getZ() + Math.cos(a) * r,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(STEAM_REACH, 2, STEAM_REACH))) {
            if (victim == this || victim instanceof CendreuxEntity) continue;
            Vec3 to = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ());
            if (to.length() > STEAM_REACH || to.length() < 0.01 || to.normalize().dot(look) > 0.3) continue;
            if (waveHit.add(victim.getUUID())) victim.hurt(damageSources().mobAttack(this), STEAM_DAMAGE);
            Vec3 push = to.normalize().scale(0.25);
            victim.push(push.x, 0.05, push.z);
            victim.hurtMarked = true;
        }
    }

    private void enterPhaseTwo() {
        entityData.set(PHASE_TWO, true);
        playSound(FFSounds.FORGE_MASTER_STEAM.get(), 2.5F, 0.8F);
        playSound(FFSounds.FORGE_MASTER_AMBIENT.get(), 2.0F, 0.8F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2, getZ(), 40, 0.8, 1.2, 0.8, 0.05);
            server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 2.5, getZ(), 12, 0.5, 0.5, 0.5, 0);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            bossEvent.setProgress(getHealth() / getMaxHealth());
            bossEvent.setVisible(!isNoAi());                 // endormi sur son estrade : pas de barre
        }
        if (level().isClientSide && (action() == Action.CHARGE || isPhaseTwo() && tickCount % 4 == 0)) {
            // la vapeur s'échappe de la bouche du marteau, à sa droite
            Vec3 right = Vec3.directionFromRotation(0, yBodyRot + 90);
            level().addParticle(ParticleTypes.CLOUD, getX() - right.x * 0.9, getY() + 0.7, getZ() - right.z * 0.9, 0, 0.04, 0);
        }
    }

    // ------------------------------------------------------------------ son arène (14b)

    public void setArena(net.minecraft.core.BlockPos forge) {
        this.arena = forge;
    }

    /** Il retourne sur son estrade, guéri, et se rendort (plus personne dans l'arène). */
    void restAt(Vec3 dais) {
        setHealth(getMaxHealth());
        entityData.set(PHASE_TWO, false);
        setAction(Action.NONE);
        waveRadius = -1;
        setTarget(null);
        getNavigation().stop();
        moveTo(dais.x, dais.y, dais.z, 0, 0);
        setYHeadRot(0);
        setYBodyRot(0);
        setNoAi(true);
    }

    @org.jetbrains.annotations.Nullable
    private MasterForgeBlockEntity forge() {
        return arena != null && level().getBlockEntity(arena) instanceof MasterForgeBlockEntity forge ? forge : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // on le frappe pendant qu'il dort : il se réveille
        if (!level().isClientSide && isNoAi() && forge() != null && level() instanceof ServerLevel server) forge().wake(server, this);
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        MasterForgeBlockEntity forge = forge();
        if (forge != null && level() instanceof ServerLevel server) forge.onMasterDeath(server);
    }

    // ------------------------------------------------------------------ la barre de boss

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    // ------------------------------------------------------------------ le reste

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FFSounds.FORGE_MASTER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FFSounds.FORGE_MASTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FFSounds.FORGE_MASTER_DEATH.get();
    }

    /** Ses pas : ceux d'Anger, lourds, à la place de ceux du bloc. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(FFSounds.FORGE_MASTER_STEP.get(), 1.0F, 1.0F);
    }

    /** Le coup simple : le marteau fend l'air, puis frappe. */
    @Override
    public boolean doHurtTarget(Entity target) {
        playSound(FFSounds.FORGE_MASTER_SWING.get(), 1.5F, 1.0F);
        boolean hit = super.doHurtTarget(target);
        if (hit) target.playSound(FFSounds.FORGE_MASTER_HIT.get(), 1.5F, 1.0F);
        return hit;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("phase_two", isPhaseTwo());
        if (arena != null) tag.putLong("arena", arena.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE_TWO, tag.getBoolean("phase_two"));
        arena = tag.contains("arena") ? net.minecraft.core.BlockPos.of(tag.getLong("arena")) : null;
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }
}
