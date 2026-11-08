package fr.anger.echoes.fireforge.machine;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFBlocks;
import fr.anger.echoes.fireforge.registry.FFSounds;
import fr.anger.echoes.fireforge.registry.FFStructures;
import fr.anger.echoes.fireforge.structure.GrandMachineStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Le choix au régulateur de la Grande Machine (plan, 15c, validé par Anger) et ses suites. Ni l'un ni l'autre n'est
 * « le bon » :
 * - baisser : la machine s'arrête et se tait, le cœur s'assombrit, la mer de lave de la caverne se fige peu à peu en une
 *   croûte de basalte, les fondations se vident et leur trappe s'ouvre ; mais ses Cendreux deviennent hostiles à tous ;
 * - pousser : tout va une fois et demie plus vite, le cœur brille plus fort, ses Cendreux laissent faire, et la bouche du
 *   cœur donne une Braise du cœur par jour à chaque joueur.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class MachineChoice {

    private MachineChoice() {}

    /** La Braise du cœur dure 8 reforges (validé par Anger). */
    public static final int EMBER_REFORGES = 8;

    /** La croûte : la caverne jusqu'à ses parois les plus lointaines (48 blocs, et leurs inégalités), du centre vers les
     *  parois, hors de dessous la coque ; quelques colonnes par tick. */
    public static final int CRUST_RADIUS = 53, CRUST_PER_TICK = 5, LAVA_TOP = 31;
    /** Les fondations se vident d'une couche toutes les 4 ticks, de haut en bas. */
    public static final int DRAIN_EVERY = 4;

    private static final List<int[]> CRUST = new ArrayList<>();

    static {
        for (int dx = -CRUST_RADIUS; dx <= CRUST_RADIUS; dx++) {
            for (int dz = -CRUST_RADIUS; dz <= CRUST_RADIUS; dz++) {
                if (dx * dx + dz * dz > CRUST_RADIUS * CRUST_RADIUS) continue;
                // sous la machine (sa coque et sa marge), c'est l'affaire des fondations
                if (dx >= -20 && dx <= 19 && dz >= -20 && dz <= 19) continue;
                CRUST.add(new int[]{dx, dz});
            }
        }
        CRUST.sort(Comparator.comparingInt(c -> c[0] * c[0] + c[1] * c[1]));
    }

    public static int crustColumns() {
        return CRUST.size();
    }

    /** Le coin de la coque, d'après la vanne tournée. */
    static BlockPos hullOf(BlockPos valve, RegulatorValveBlock.Kind kind) {
        BlockPos offset = kind == RegulatorValveBlock.Kind.LOWER ? GrandMachineStructure.LOWER_VALVE : GrandMachineStructure.RAISE_VALVE;
        return new BlockPos(valve.getX() - offset.getX(), GrandMachineStructure.BASE_Y, valve.getZ() - offset.getZ());
    }

    /** La roue tourne : le choix est fait, pour tout le monde et pour toujours. */
    public static void choose(ServerLevel level, BlockPos valve, RegulatorValveBlock.Kind kind) {
        GrandMachineState state = GrandMachineState.get(level);
        if (state.choice() != GrandMachineState.Choice.NONE) return;
        BlockPos hull = hullOf(valve, kind);
        boolean lower = kind == RegulatorValveBlock.Kind.LOWER;
        state.choose(lower ? GrandMachineState.Choice.LOWERED : GrandMachineState.Choice.RAISED, level.getGameTime(), hull);
        BlockState here = level.getBlockState(valve);
        if (here.is(FFBlocks.REGULATOR_VALVE.get())) level.setBlock(valve, here.setValue(RegulatorValveBlock.TURNED, true), 3);
        heart(level, hull, lower);
        BlockPos center = hull.offset(20, 98 - GrandMachineStructure.BASE_Y, 20);
        if (lower) {
            level.playSound(null, center, FFSounds.FORGE_MASTER_STEAM.get(), SoundSource.BLOCKS, 4.0F, 0.5F);
            level.playSound(null, center, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 4.0F, 0.4F);
        } else {
            level.playSound(null, center, FFSounds.FORGE_MASTER_GROUND_HIT.get(), SoundSource.BLOCKS, 4.0F, 0.6F);
            level.playSound(null, center, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 4.0F, 0.5F);
        }
        Component message = Component.translatable(lower ? "message.echoes_fireforge.machine.lowered" : "message.echoes_fireforge.machine.raised")
                .withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : level.players()) {
            if (MachineAmbienceEvents.inside(player)) player.displayClientMessage(message, false);
        }
        MachineAmbienceEvents.refresh(level);
    }

    /** Le cœur : baissé, sa braise s'assombrit ; poussé, elle s'avive. */
    static void heart(ServerLevel level, BlockPos hull, boolean lower) {
        double cx = hull.getX() + GrandMachineStructure.HEART_X, cy = GrandMachineStructure.HEART_Y, cz = hull.getZ() + GrandMachineStructure.HEART_Z;
        int r = (int) Math.ceil(GrandMachineStructure.HEART_CORE);
        for (BlockPos p : BlockPos.betweenClosed((int) cx - r - 1, (int) cy - r - 1, (int) cz - r - 1, (int) cx + r + 1, (int) cy + r + 1, (int) cz + r + 1)) {
            // mesuré comme dans le gabarit (depuis le coin des blocs) ; la coquille de cuivre, plus loin, n'est pas touchée
            double d = Math.sqrt(Math.pow(p.getX() - cx, 2) + Math.pow(p.getY() - cy, 2) + Math.pow(p.getZ() - cz, 2));
            if (d > GrandMachineStructure.HEART_CORE + 0.6) continue;
            BlockState s = level.getBlockState(p);
            if (lower) {
                if (s.is(Blocks.SHROOMLIGHT)) level.setBlock(p, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                else if (s.is(Blocks.MAGMA_BLOCK)) level.setBlock(p, Blocks.BLACKSTONE.defaultBlockState(), 3);
            } else if (s.is(Blocks.MAGMA_BLOCK)) {
                level.setBlock(p, Blocks.SHROOMLIGHT.defaultBlockState(), 3);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.NETHER) return;
        GrandMachineState state = GrandMachineState.get(level);
        if (state.choice() != GrandMachineState.Choice.LOWERED || state.hull() == null) return;
        crust(level, state);
        if (level.getGameTime() % DRAIN_EVERY == 0) drain(level, state);
    }

    /** Public pour les tests : quelques colonnes de plus de la mer de lave se figent. */
    public static void crust(ServerLevel level, GrandMachineState state) {
        BlockPos hull = state.hull();
        int cx = hull.getX() + 20, cz = hull.getZ() + 20;
        BlockState basalt = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        int done = state.crust();
        for (int n = 0; n < CRUST_PER_TICK && done < CRUST.size(); n++) {
            int[] c = CRUST.get(done);
            BlockPos p = new BlockPos(cx + c[0], LAVA_TOP, cz + c[1]);
            if (!loaded(level, p)) break;                                // on reprendra quand on sera là
            if (level.getFluidState(p).is(FluidTags.LAVA)) {
                level.setBlock(p, basalt, 2);
                if (done % 30 == 0) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 4, 0.4, 0.1, 0.4, 0.01);
                    level.playSound(null, p, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.7F);
                }
            }
            done++;
        }
        if (done != state.crust()) state.crust(done);
    }

    /** Public pour les tests : une couche de plus des fondations se vide ; vidées, leur trappe s'ouvre. */
    public static void drain(ServerLevel level, GrandMachineState state) {
        if (state.hatchOpen()) return;
        BlockPos hull = state.hull();
        int layers = GrandMachineStructure.FOUND_TOP - GrandMachineStructure.FOUND_BOTTOM + 1;
        int drained = state.drained();
        if (drained < layers) {
            int y = GrandMachineStructure.FOUND_TOP - drained;
            BlockPos a = hull.offset(GrandMachineStructure.FOUND_MIN, 0, GrandMachineStructure.FOUND_MIN).atY(y);
            BlockPos b = hull.offset(GrandMachineStructure.FOUND_MAX, 0, GrandMachineStructure.FOUND_MAX).atY(y);
            if (!loaded(level, a) || !loaded(level, b) || !loaded(level, new BlockPos(a.getX(), y, b.getZ()))
                    || !loaded(level, new BlockPos(b.getX(), y, a.getZ()))) return;
            for (BlockPos p : BlockPos.betweenClosed(a, b)) {
                if (level.getFluidState(p).is(FluidTags.LAVA)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
            BlockPos mid = hull.offset(20, 0, 20).atY(y);
            level.playSound(null, mid, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.5F, 0.5F);
            state.drained(drained + 1);
            return;
        }
        // la trappe : ses machines scellées s'en vont dans un jet de vapeur
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                BlockPos p = hull.offset(GrandMachineStructure.HATCH_X + x, 0, GrandMachineStructure.HATCH_Z + z).atY(GrandMachineStructure.HATCH_Y);
                if (level.getBlockState(p).is(FFBlocks.SEALED_MACHINE.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 6, 0.3, 0.4, 0.3, 0.05);
            }
        }
        BlockPos hatch = hull.offset(GrandMachineStructure.HATCH_X + 1, 0, GrandMachineStructure.HATCH_Z + 1).atY(GrandMachineStructure.HATCH_Y);
        level.playSound(null, hatch, FFSounds.FORGE_MASTER_STEAM.get(), SoundSource.BLOCKS, 2.0F, 0.8F);
        level.playSound(null, hatch, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 2.0F, 0.6F);
        Component message = Component.translatable("message.echoes_fireforge.machine.drained").withStyle(ChatFormatting.GRAY);
        for (ServerPlayer player : level.players()) {
            if (MachineAmbienceEvents.inside(player)) player.displayClientMessage(message, true);
        }
        state.openHatch();
    }

    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** L'humeur des Cendreux de la machine : hostiles à tous (baissée), ou ils laissent faire (poussée). */
    public enum Mood { USUAL, HOSTILE, PEACEFUL }

    public static Mood cendreuxMood(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.NETHER) return Mood.USUAL;
        GrandMachineState.Choice choice = GrandMachineState.get(level).choice();
        if (choice == GrandMachineState.Choice.NONE) return Mood.USUAL;
        if (!level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(FFStructures.GRAND_MACHINE_KEY)).isValid()) return Mood.USUAL;
        return choice == GrandMachineState.Choice.LOWERED ? Mood.HOSTILE : Mood.PEACEFUL;
    }
}
