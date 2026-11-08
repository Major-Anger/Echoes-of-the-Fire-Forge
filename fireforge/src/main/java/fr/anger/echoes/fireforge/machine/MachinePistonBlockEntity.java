package fr.anger.echoes.fireforge.machine;

import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Un piston géant (validé par Anger) : un cycle de 6 s. 2 s en bas, il monte d'un bloc toutes les 4 ticks (2,2 s), 1,3 s
 * en haut, puis il retombe d'un coup (0,5 s). Il porte ce qui est sur sa tête ; qui reste sur son corps quand la tête
 * retombe est écrasé (3 cœurs) et repoussé. Son décalage (« phase ») vient du gabarit : les six font une vague.
 *
 * Le moteur est dans le corps, à (0, 0, 0) ; la tige (2 x 2) au-dessus de lui ; la tête (6 x 6, une plaque de laiton et
 * une dalle de pierre noire) autour. La tête va de 4 blocs (posée sur le corps) à 15 blocs au-dessus du moteur.
 */
public class MachinePistonBlockEntity extends BlockEntity {

    public static final int CYCLE = 120, LOW = 40, RISE_STEP = 4, STROKE = 11, HIGH = 26, FALL = 10;
    public static final int BOTTOM = 4;
    public static final float CRUSH = 6.0F;
    private static final BlockState ROD = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    private static final BlockState PLATE = Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState();
    private static final BlockState CAP = Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);

    private int phase;
    /** La hauteur de la tête, au-dessus du moteur. */
    private int head = BOTTOM;

    public MachinePistonBlockEntity(BlockPos pos, BlockState state) {
        super(FFBlockEntities.MACHINE_PISTON.get(), pos, state);
    }

    /** La hauteur de la tête voulue à ce moment du cycle (au-dessus du moteur). */
    public static int heightAt(int u) {
        if (u < LOW) return BOTTOM;
        if (u < LOW + RISE_STEP * STROKE) return BOTTOM + (u - LOW) / RISE_STEP;
        if (u < LOW + RISE_STEP * STROKE + HIGH) return BOTTOM + STROKE;
        int falling = u - (LOW + RISE_STEP * STROKE + HIGH);
        return BOTTOM + STROKE - (int) Math.ceil((falling + 1) * STROKE / (double) FALL);
    }

    public int moment(ServerLevel level) {
        return (int) Math.floorMod(MachineClock.ticks(level) + phase, CYCLE);
    }

    public int head() {
        return head;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachinePistonBlockEntity piston) {
        if (level instanceof ServerLevel server) piston.tick(server);
    }

    private void tick(ServerLevel level) {
        int u = moment(level);
        int target = Math.max(BOTTOM, heightAt(u));
        if (target != head) move(level, target);
        if (u == LOW) level.playSound(null, getBlockPos().above(BOTTOM), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8F, 0.6F);
    }

    private void move(ServerLevel level, int target) {
        BlockPos o = getBlockPos();
        int from = head;
        boolean up = target > from;
        AABB plate = new AABB(o.getX() - 2, o.getY() + from, o.getZ() - 2, o.getX() + 4, o.getY() + from + 1.5, o.getZ() + 4);
        // ce qui est posé sur la tête monte avec elle
        if (up) {
            AABB riders = new AABB(plate.minX, plate.maxY - 0.01, plate.minZ, plate.maxX, plate.maxY + 2.5, plate.maxZ);
            for (Entity rider : level.getEntitiesOfClass(Entity.class, riders, e -> !e.isSpectator())) {
                rider.teleportTo(rider.getX(), rider.getY() + (target - from), rider.getZ());
            }
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // l'ancienne tête s'en va, la tige s'allonge ou se raccourcit, la nouvelle tête se pose
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 3; dz++) {
                for (int y = from; y <= from + 1; y++) set(level, p.set(o.getX() + dx, o.getY() + y, o.getZ() + dz), Blocks.AIR.defaultBlockState());
            }
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int y = BOTTOM; y < target; y++) set(level, p.set(o.getX() + dx, o.getY() + y, o.getZ() + dz), ROD);
                for (int y = target + 2; y < from; y++) set(level, p.set(o.getX() + dx, o.getY() + y, o.getZ() + dz), Blocks.AIR.defaultBlockState());
            }
        }
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 3; dz++) {
                set(level, p.set(o.getX() + dx, o.getY() + target, o.getZ() + dz), PLATE);
                set(level, p.set(o.getX() + dx, o.getY() + target + 1, o.getZ() + dz), CAP);
            }
        }
        head = target;
        setChanged();
        if (up) return;
        // en retombant : qui se trouve sous la tête est écrasé, et chassé hors de dessous
        AABB under = new AABB(o.getX() - 2, o.getY() + target, o.getZ() - 2, o.getX() + 4, o.getY() + target + 1.5, o.getZ() + 4);
        Vec3 center = new Vec3(o.getX() + 1, o.getY() + BOTTOM, o.getZ() + 1);
        for (Entity e : level.getEntitiesOfClass(Entity.class, under, e -> !e.isSpectator())) {
            if (e instanceof LivingEntity living) living.hurt(MachineClock.damage(level, MachineClock.PISTON), CRUSH);
            Vec3 away = new Vec3(e.getX() - center.x, 0, e.getZ() - center.z);
            if (away.lengthSqr() < 1.0E-3) away = new Vec3(1, 0, 0);
            away = away.normalize();
            // juste au-delà du bord de la tête (un carré de 3 blocs autour du centre), sur le sol de l'étage
            double reach = (3.0 + e.getBbWidth() / 2 + 0.3) / Math.max(Math.abs(away.x), Math.abs(away.z));
            e.teleportTo(center.x + away.x * reach, o.getY(), center.z + away.z * reach);
            e.setDeltaMovement(away.x * 0.4, 0.3, away.z * 0.4);
            e.hurtMarked = true;
        }
        if (target == BOTTOM) {
            level.playSound(null, o.above(BOTTOM), FFSounds.FORGE_MASTER_GROUND_HIT.get(), SoundSource.BLOCKS, 1.4F, 0.75F);
            level.sendParticles(ParticleTypes.CLOUD, center.x, o.getY() + BOTTOM + 0.3, center.z, 24, 2.2, 0.1, 2.2, 0.06);
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).is(Blocks.BEDROCK)) return;
        level.setBlock(pos, state, 2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("phase", phase);
        tag.putInt("head", head);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phase = tag.getInt("phase");
        head = tag.contains("head") ? tag.getInt("head") : BOTTOM;
    }
}
