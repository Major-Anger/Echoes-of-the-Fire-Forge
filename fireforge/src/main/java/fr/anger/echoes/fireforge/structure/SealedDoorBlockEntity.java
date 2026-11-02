package fr.anger.echoes.fireforge.structure;

import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La serrure de la porte scellée. Une fois par seconde, elle regarde la vieille chaudière autour d'elle : un chaudron
 * plein d'eau et un fourneau allumé. Alors l'eau part en vapeur, le manomètre s'allume, et la porte s'ouvre.
 */
public class SealedDoorBlockEntity extends BlockEntity {

    private static final int CHECK_TICKS = 20, REACH = 4, MAX_DOOR = 27;

    public SealedDoorBlockEntity(BlockPos pos, BlockState state) {
        super(FFBlockEntities.SEALED_DOOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SealedDoorBlockEntity lock) {
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), CHECK_TICKS) != 0) return;
        BlockPos water = findWater(level, pos);
        if (water != null && hasFire(level, pos)) open(level, pos, water);
    }

    /** Un chaudron plein d'eau, près de la porte. */
    @Nullable
    static BlockPos findWater(Level level, BlockPos lock) {
        for (BlockPos p : BlockPos.betweenClosed(lock.offset(-REACH, -1, -REACH), lock.offset(REACH, 1, REACH))) {
            BlockState state = level.getBlockState(p);
            if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL) {
                return p.immutable();
            }
        }
        return null;
    }

    /** Un fourneau allumé, près de la porte. */
    static boolean hasFire(Level level, BlockPos lock) {
        for (BlockPos p : BlockPos.betweenClosed(lock.offset(-REACH, -1, -REACH), lock.offset(REACH, 1, REACH))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof AbstractFurnaceBlock && state.getValue(AbstractFurnaceBlock.LIT)) return true;
        }
        return false;
    }

    /** L'eau du chaudron part en vapeur, le manomètre au-dessus de la porte s'allume, les machines de la porte s'en vont. */
    static void open(Level level, BlockPos lock, BlockPos water) {
        List<BlockPos> door = door(level, lock);
        level.setBlock(water, Blocks.CAULDRON.defaultBlockState(), 3);
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, water);
        for (BlockPos p : BlockPos.betweenClosed(lock.offset(-2, 0, -2), lock.offset(2, 5, 2))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof CopperBulbBlock && !state.getValue(CopperBulbBlock.LIT)) {
                level.setBlock(p, state.setValue(CopperBulbBlock.LIT, true), 3);
            }
        }
        for (BlockPos p : door) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            level.gameEvent(null, GameEvent.BLOCK_DESTROY, p);
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.03);
            for (BlockPos p : door) {
                server.sendParticles(ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.35, 0.35, 0.35, 0.02);
            }
        }
        level.playSound(null, lock, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.6F);
        level.playSound(null, lock, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /** Les blocs de la porte qui se touchent, à partir de la serrure. */
    static List<BlockPos> door(Level level, BlockPos lock) {
        List<BlockPos> door = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> next = new ArrayDeque<>();
        next.add(lock);
        seen.add(lock);
        while (!next.isEmpty() && door.size() < MAX_DOOR) {
            BlockPos p = next.poll();
            door.add(p);
            for (Direction direction : Direction.values()) {
                BlockPos q = p.relative(direction);
                if (seen.add(q) && level.getBlockState(q).is(FFBlocks.SEALED_MACHINE.get())) next.add(q);
            }
        }
        return door;
    }
}
