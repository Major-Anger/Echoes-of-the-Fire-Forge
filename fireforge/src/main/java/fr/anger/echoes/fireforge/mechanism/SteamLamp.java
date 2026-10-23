package fr.anger.echoes.fireforge.mechanism;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFBlocks;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * La Lampe à vapeur (casque, touche C) : travailler au fond des galeries. Allumée, elle pose une lumière là où
 * regarde son porteur (jusqu'à 12 blocs). L'état allumé est gardé sur le casque (composant
 * {@code echoes_fireforge:lamp_on}) : tous les joueurs le voient, et le faisceau se dessine chez chacun.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class SteamLamp {

    private SteamLamp() {}

    public static final double RANGE = 12;
    public static final int LIGHT = 14;
    /** Où est la lumière de chaque porteur. */
    private static final Map<UUID, GlobalPos> LIGHTS = new HashMap<>();

    public static boolean isOn(Player player) {
        return MechanismEffects.worn(player, EquipmentSlot.HEAD) == Mechanism.LAMPE
                && player.getItemBySlot(EquipmentSlot.HEAD).getOrDefault(FFDataComponents.LAMP_ON, false);
    }

    /** Touche C : allumer ou éteindre. */
    public static void toggle(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        boolean on = !helmet.getOrDefault(FFDataComponents.LAMP_ON, false);
        helmet.set(FFDataComponents.LAMP_ON, on);
        player.serverLevel().playSound(null, player.blockPosition(), on ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.5F, on ? 1.2F : 1.6F);
        if (!on) clear(player);
    }

    /** Une lampe éclaire-t-elle encore ce bloc ? (lu par la lumière elle-même) */
    public static boolean lights(ServerLevel level, BlockPos pos) {
        GlobalPos here = GlobalPos.of(level.dimension(), pos);
        return LIGHTS.containsValue(here);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    /** Public pour les tests : la lumière suit le regard (vérifiée tous les 2 ticks). */
    public static void tick(ServerPlayer player) {
        if (!isOn(player) || player.isSpectator()) {
            if (LIGHTS.containsKey(player.getUUID())) clear(player);
            return;
        }
        if (player.tickCount % 2 != 0 && LIGHTS.containsKey(player.getUUID())) return;
        ServerLevel level = player.serverLevel();
        BlockPos target = target(player);
        GlobalPos old = LIGHTS.get(player.getUUID());
        if (old != null && old.dimension() == level.dimension() && old.pos().equals(target)) return;
        clear(player);
        if (target != null) {
            BlockState state = level.getBlockState(target);
            if (state.isAir()) level.setBlock(target, FFBlocks.LAMP_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL);
            LIGHTS.put(player.getUUID(), GlobalPos.of(level.dimension(), target));
        }
    }

    /** Le bloc d'air éclairé : juste devant ce que l'on regarde, ou au bout de la portée. */
    @Nullable
    public static BlockPos target(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        BlockPos pos = hit.getType() == HitResult.Type.MISS ? BlockPos.containing(end) : hit.getBlockPos().relative(hit.getDirection());
        // reculer vers le porteur jusqu'à trouver de l'air (ou une lumière de lampe)
        for (int i = 0; i < 4; i++) {
            BlockState state = player.level().getBlockState(pos);
            if (state.isAir() || state.is(FFBlocks.LAMP_LIGHT.get())) return pos;
            pos = BlockPos.containing(Vec3.atCenterOf(pos).subtract(look));
        }
        return null;
    }

    private static void clear(ServerPlayer player) {
        GlobalPos old = LIGHTS.remove(player.getUUID());
        if (old == null) return;
        ServerLevel level = player.server.getLevel(old.dimension());
        if (level != null && level.getBlockState(old.pos()).is(FFBlocks.LAMP_LIGHT.get()) && !LIGHTS.containsValue(old)) {
            level.setBlock(old.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
