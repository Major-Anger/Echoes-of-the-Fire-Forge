package fr.anger.echoes.fireforge.forgemaster;

import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFBlocks;
import fr.anger.echoes.fireforge.registry.FFEntities;
import fr.anger.echoes.fireforge.structure.SealedDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * La forge du maître (plan, 14b) : cachée sous l'âtre de l'arène, elle mène tout. Le maître y dort ; quand un joueur
 * entre, il se réveille et la porte se ferme. S'il n'y a plus personne pendant 30 s, il se rendort, guéri, et la porte
 * se rouvre. En phase 2, ses quatre foyers se rallument et des Cendreux en sortent. À sa mort, la forge est froide (la
 * braise de l'âtre devient basalte) ; on la rallume avec 8 Cendres d'Attiseur, et il revient.
 * <p>
 * L'arène ne tourne jamais (son entrée est toujours au sud) : tout se repère depuis ce bloc, avec les positions du
 * gabarit (tools/structures/make_structures.py, forge_arena).
 */
public class MasterForgeBlockEntity extends BlockEntity {

    /** Ce bloc, dans le gabarit de l'arène. */
    private static final int AT_X = 29, AT_Y = 1, AT_Z = 26, SIZE = 60, HEIGHT = 30;
    private static final String CALLED = "echoes_fireforge.called_by_forge";
    private static final int[][] FOYERS = {{10, 2, 10}, {49, 2, 10}, {10, 2, 49}, {49, 2, 49}};
    private static final double DAIS_X = 30.0, DAIS_Y = 2, DAIS_Z = 29.5;
    private static final int GATE_X0 = 28, GATE_X1 = 31, GATE_Y0 = 1, GATE_Y1 = 6, GATE_Z = 59;
    private static final int HEARTH_X0 = 28, HEARTH_X1 = 31, HEARTH_Y = 2, HEARTH_Z = 26;
    public static final int RELIGHT_ASH = 8;
    private static final int CHECK = 10, SLEEP_AFTER = 600, CENDREUX_EVERY = 400, CENDREUX_MAX = 4;

    private boolean lit = true, fresh, awake, foyersLit;
    @Nullable
    private UUID master;
    private int empty, cendreuxTimer;

    public MasterForgeBlockEntity(BlockPos pos, BlockState state) {
        super(FFBlockEntities.MASTER_FORGE.get(), pos, state);
    }

    /** Un point du gabarit de l'arène, dans le monde. */
    private BlockPos at(int x, int y, int z) {
        return worldPosition.offset(x - AT_X, y - AT_Y, z - AT_Z);
    }

    private Vec3 dais() {
        BlockPos o = at(0, 0, 0);
        return new Vec3(o.getX() + DAIS_X, o.getY() + DAIS_Y, o.getZ() + DAIS_Z);
    }

    public AABB arena() {
        BlockPos o = at(0, 0, 0);
        return new AABB(o.getX() + 1, o.getY(), o.getZ() + 1, o.getX() + SIZE - 1, o.getY() + HEIGHT, o.getZ() + SIZE - 1);
    }

    public boolean isLit() {
        return lit;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MasterForgeBlockEntity forge) {
        if (!(level instanceof ServerLevel server) || Math.floorMod(level.getGameTime() + pos.hashCode(), CHECK) != 0) return;
        forge.tick(server);
    }

    private void tick(ServerLevel level) {
        if (fresh) {                                     // l'arène vient d'être générée : il est là, endormi
            fresh = false;
            spawnMaster(level);
            setChanged();
            return;
        }
        ForgeMasterEntity boss = master(level);
        if (boss == null) {
            // il n'est plus là (et ses tronçons sont bien chargés) : la forge refroidit
            if (lit && master != null && level.areEntitiesLoaded(ChunkPos.asLong(BlockPos.containing(dais())))) goCold(level);
            return;
        }
        List<Player> inside = level.getEntitiesOfClass(Player.class, arena(), p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
        if (!awake) {
            if (!inside.isEmpty()) wake(level, boss);
            return;
        }
        if (inside.isEmpty()) {
            empty += CHECK;
            if (empty >= SLEEP_AFTER) sleep(level, boss);
            return;
        }
        empty = 0;
        if (boss.isPhaseTwo()) {
            if (!foyersLit) setFoyers(level, true);
            cendreuxTimer += CHECK;
            if (cendreuxTimer >= CENDREUX_EVERY) {
                cendreuxTimer = 0;
                releaseCendreux(level);
            }
        }
    }

    @Nullable
    private ForgeMasterEntity master(ServerLevel level) {
        if (master == null) return null;
        Entity e = level.getEntity(master);
        return e instanceof ForgeMasterEntity boss && boss.isAlive() ? boss : null;
    }

    private void spawnMaster(ServerLevel level) {
        ForgeMasterEntity boss = FFEntities.FORGE_MASTER.get().create(level);
        if (boss == null) return;
        Vec3 d = dais();
        boss.moveTo(d.x, d.y, d.z, 0, 0);
        boss.setYHeadRot(0);
        boss.setYBodyRot(0);
        EventHooks.finalizeMobSpawn(boss, level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.STRUCTURE, null);
        boss.setArena(worldPosition);
        boss.setNoAi(true);
        level.addFreshEntity(boss);
        master = boss.getUUID();
        awake = false;
        setChanged();
    }

    /** Un joueur entre (ou le frappe) : il se lève, la porte tombe. */
    void wake(ServerLevel level, ForgeMasterEntity boss) {
        boss.setNoAi(false);
        awake = true;
        empty = 0;
        setGate(level, true);
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 2.5F, 0.4F);
        setChanged();
    }

    /** Plus personne : il retourne sur son estrade, guéri, et se rendort ; la porte se relève. */
    private void sleep(ServerLevel level, ForgeMasterEntity boss) {
        Vec3 d = dais();
        boss.restAt(d);
        awake = false;
        empty = 0;
        setGate(level, false);
        recallCendreux(level);
        setFoyers(level, false);
        setChanged();
    }

    /** Les Cendreux que la forge a appelés rentrent dans les foyers (sinon ils les rallumeraient). */
    private void recallCendreux(ServerLevel level) {
        for (CendreuxEntity c : level.getEntitiesOfClass(CendreuxEntity.class, arena().inflate(16), c -> c.getPersistentData().getBoolean(CALLED))) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, c.getX(), c.getY() + 1, c.getZ(), 12, 0.4, 0.8, 0.4, 0.02);
            c.discard();
        }
    }

    /** Sa mort : la forge refroidit. */
    void onMasterDeath(ServerLevel level) {
        goCold(level);
    }

    private void goCold(ServerLevel level) {
        lit = false;
        awake = false;
        master = null;
        setGate(level, false);
        setFoyers(level, false);
        setHearth(level, false);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2.0F, 0.5F);
        setChanged();
    }

    /** On rallume la forge avec des Cendres d'Attiseur : le maître revient, endormi sur son estrade. */
    public boolean relight(ServerLevel level) {
        if (lit) return false;
        lit = true;
        setHearth(level, true);
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5, 12, 1, 0.3, 0.3, 0);
        spawnMaster(level);
        return true;
    }

    /** La porte : une herse de machines qui tombe (ou se relève) dans un jet de vapeur. */
    private void setGate(ServerLevel level, boolean closed) {
        BlockState gate = FFBlocks.SEALED_MACHINE.get().defaultBlockState().setValue(SealedDoorBlock.LOCK, false);
        for (int x = GATE_X0; x <= GATE_X1; x++) {
            for (int y = GATE_Y0; y <= GATE_Y1; y++) {
                BlockPos p = at(x, y, GATE_Z);
                BlockState s = level.getBlockState(p);
                if (closed && s.isAir()) level.setBlock(p, gate, 3);
                else if (!closed && s.is(FFBlocks.SEALED_MACHINE.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
        BlockPos mid = at((GATE_X0 + GATE_X1) / 2, 3, GATE_Z);
        level.playSound(null, mid, closed ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 2.0F, 0.5F);
        level.playSound(null, mid, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.5F, 0.7F);
    }

    private void setFoyers(ServerLevel level, boolean on) {
        foyersLit = on;
        cendreuxTimer = on ? CENDREUX_EVERY - 40 : 0;
        for (int[] f : FOYERS) {
            BlockPos p = at(f[0], f[1], f[2]);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT) != on) {
                level.setBlock(p, s.setValue(CampfireBlock.LIT, on), 3);
                if (on) level.playSound(null, p, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.5F, 0.8F);
            }
        }
    }

    /** Des Cendreux sortent des foyers allumés (on peut les éteindre à la pelle), quatre au plus dans l'arène. */
    private void releaseCendreux(ServerLevel level) {
        int count = level.getEntitiesOfClass(CendreuxEntity.class, arena()).size();
        for (int[] f : FOYERS) {
            if (count >= CENDREUX_MAX) return;
            BlockPos p = at(f[0], f[1], f[2]);
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof CampfireBlock) || !s.getValue(CampfireBlock.LIT)) continue;
            CendreuxEntity c = FFEntities.CENDREUX.get().create(level);
            if (c == null) continue;
            c.moveTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 2.5, level.random.nextFloat() * 360, 0);
            c.getPersistentData().putBoolean(CALLED, true);
            EventHooks.finalizeMobSpawn(c, level, level.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(c);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 10, 0.4, 0.6, 0.4, 0.02);
            count++;
        }
    }

    /** L'âtre de braise (allumé) ou de basalte (froid). */
    private void setHearth(ServerLevel level, boolean on) {
        BlockState state = on ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.SMOOTH_BASALT.defaultBlockState();
        for (int x = HEARTH_X0; x <= HEARTH_X1; x++) level.setBlock(at(x, HEARTH_Y, HEARTH_Z), state, 3);
    }

    /** Ce bloc mène-t-il l'âtre en {@code pos} ? */
    public boolean isHearth(BlockPos pos) {
        for (int x = HEARTH_X0; x <= HEARTH_X1; x++) if (at(x, HEARTH_Y, HEARTH_Z).equals(pos)) return true;
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("lit", lit);
        tag.putBoolean("fresh", fresh);
        tag.putBoolean("awake", awake);
        tag.putBoolean("foyers", foyersLit);
        if (master != null) tag.putUUID("master", master);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lit = !tag.contains("lit") || tag.getBoolean("lit");
        fresh = tag.getBoolean("fresh");
        awake = tag.getBoolean("awake");
        foyersLit = tag.getBoolean("foyers");
        master = tag.hasUUID("master") ? tag.getUUID("master") : null;
    }
}
