package fr.anger.echoes.fireforge.machine;

import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Le cycle d'une bouche de vapeur (validé par Anger) : 4 s. Elle siffle 1 s (petites bouffées), crache un jet de 5 blocs
 * pendant 1,5 s (1 cœur toutes les demi-secondes, et elle repousse vers le haut), puis 1,5 s de calme. Son décalage
 * (« phase ») vient du gabarit : les bouches de l'étage ne soufflent pas ensemble.
 */
public class SteamVentBlockEntity extends BlockEntity {

    public static final int CYCLE = 80, WARN = 20, JET = 30, HEIGHT = 5, HIT_EVERY = 10;
    public static final float DAMAGE = 2.0F;

    private int phase;

    public SteamVentBlockEntity(BlockPos pos, BlockState state) {
        super(FFBlockEntities.STEAM_VENT.get(), pos, state);
    }

    /** Où en est la bouche dans son cycle (0 à 79). */
    public int moment(ServerLevel level) {
        return (int) Math.floorMod(MachineClock.ticks(level) + phase, CYCLE);
    }

    public boolean blowing(ServerLevel level) {
        int u = moment(level);
        return u >= WARN && u < WARN + JET;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamVentBlockEntity vent) {
        if (level instanceof ServerLevel server) vent.tick(server);
    }

    private void tick(ServerLevel level) {
        int u = moment(level);
        BlockPos pos = getBlockPos();
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5, top = pos.getY() + 1.0;
        if (u < WARN) {
            if (u == 0) level.playSound(null, pos, FFSounds.FORGE_MASTER_STEAM.get(), SoundSource.BLOCKS, 0.35F, 1.7F);
            if (u % 4 == 0) level.sendParticles(ParticleTypes.CLOUD, cx, top + 0.1, cz, 2, 0.15, 0.05, 0.15, 0.01);
            return;
        }
        if (u >= WARN + JET) return;
        if (u == WARN) level.playSound(null, pos, FFSounds.FORGE_MASTER_STEAM.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (u % 2 == 0) level.sendParticles(ParticleTypes.CLOUD, cx, top + HEIGHT / 2.0, cz, 10, 0.12, HEIGHT / 4.0, 0.12, 0.03);
        if ((u - WARN) % HIT_EVERY != 0) return;
        AABB jet = new AABB(pos.getX() - 0.1, top, pos.getZ() - 0.1, pos.getX() + 1.1, top + HEIGHT, pos.getZ() + 1.1);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, jet, e -> e.isAlive() && !e.isSpectator())) {
            if (victim instanceof CendreuxEntity) continue;               // ceux qui gardent la machine la connaissent
            victim.hurt(MachineClock.damage(level, MachineClock.STEAM), DAMAGE);
            Vec3 v = victim.getDeltaMovement();
            victim.setDeltaMovement(v.x, Math.max(v.y, 0.55), v.z);
            victim.hurtMarked = true;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("phase", phase);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phase = tag.getInt("phase");
    }
}
