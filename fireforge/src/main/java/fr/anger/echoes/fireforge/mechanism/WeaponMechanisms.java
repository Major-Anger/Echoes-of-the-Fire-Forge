package fr.anger.echoes.fireforge.mechanism;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Les mécanismes d'arme (étape 8d) : un clic droit avec l'arme (accroupi + clic droit si un bouclier est en main
 * gauche : le bouclier passe avant). Pas de vapeur, une recharge (celle de l'objet, visible sur la barre).
 * Les coups des mécanismes ne sont pas des coups d'épée : ils ne relancent pas les Spéciaux.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class WeaponMechanisms {

    private WeaponMechanisms() {}

    /** Piston (épée) : une estocade en ligne sur 4 blocs, 100 % des dégâts ; recharge 3 s. */
    public static final double PISTON_RANGE = 4, PISTON_PART = 1.0;
    public static final int PISTON_COOLDOWN = 60;
    /** Sillon (hache) : une onde court au sol sur 6 blocs, 80 % des dégâts ; recharge 4 s. */
    public static final int FURROW_LENGTH = 6, FURROW_COOLDOWN = 80;
    public static final double FURROW_PART = 0.8;
    /** Frappe-sol : une onde de choc à 3 blocs, 60 % des dégâts et du recul ; recharge 5 s. */
    public static final double SLAM_RADIUS = 3, SLAM_PART = 0.6, SLAM_PUSH = 0.8;
    public static final int SLAM_COOLDOWN = 100;
    /** Treuil : l'arme part au bout d'une chaîne (10 blocs), frappe à 100 % et revient ; recharge 4 s. */
    public static final double WINCH_RANGE = 10, WINCH_PART = 1.0;
    public static final int WINCH_COOLDOWN = 80;
    /** Trident de trempe : surchauffe 8 s (+30 %, met le feu 3 s), puis 6 s de refroidissement ; mouillé, +50 % et pas de feu. */
    public static final int OVERHEAT_TICKS = 160, COOLING_TICKS = 120, BURN_SECONDS = 3;
    public static final float OVERHEAT_BONUS = 1.3F, QUENCH_BONUS = 1.5F;

    /** Les ondes du Sillon en route : une case par tick. */
    private record Furrow(ServerPlayer player, Vec3 from, Vec3 step, float damage, Set<UUID> hit, int[] done) {}
    private static final List<Furrow> FURROWS = new ArrayList<>();
    /** Fin de la surchauffe du Trident de trempe, par joueur (temps de jeu). */
    private static final Map<UUID, Long> OVERHEAT_UNTIL = new HashMap<>();

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        OVERHEAT_UNTIL.remove(event.getEntity().getUUID());
        FURROWS.removeIf(f -> f.player() == event.getEntity());
    }

    /** Le mécanisme d'arme de cet objet, ou null. */
    @Nullable
    public static Mechanism of(ItemStack stack) {
        Mechanism mechanism = stack.get(FFDataComponents.MECHANISM);
        return mechanism != null && mechanism.slot() == net.minecraft.world.entity.EquipmentSlot.MAINHAND && mechanism.fits(stack) ? mechanism : null;
    }

    /** Le clic droit déclenche-t-il le mécanisme ? Avec un bouclier en main gauche, seulement accroupi (validé). */
    public static boolean triggers(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || of(player.getMainHandItem()) == null) return false;
        return !(player.getOffhandItem().getItem() instanceof ShieldItem) || player.isShiftKeyDown();
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!triggers(player, event.getHand())) return;
        // le même choix des deux côtés : le trident ne se lance plus, le bouclier ne se lève pas
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (player instanceof ServerPlayer server) activate(server);
    }

    /** Public pour les tests : le mécanisme de l'arme en main agit (s'il n'est pas en recharge). */
    public static boolean activate(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        Mechanism mechanism = of(weapon);
        if (mechanism == null || player.getCooldowns().isOnCooldown(weapon.getItem())) return false;
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (mechanism) {
            case PISTON -> {
                thrust(player, damage * (float) PISTON_PART);
                player.getCooldowns().addCooldown(weapon.getItem(), PISTON_COOLDOWN);
            }
            case SILLON -> {
                furrow(player, damage * (float) FURROW_PART);
                player.getCooldowns().addCooldown(weapon.getItem(), FURROW_COOLDOWN);
            }
            case FRAPPE_SOL -> {
                slam(player, damage * (float) SLAM_PART);
                player.getCooldowns().addCooldown(weapon.getItem(), SLAM_COOLDOWN);
            }
            case TREUIL -> {
                winch(player, damage * (float) WINCH_PART);
                player.getCooldowns().addCooldown(weapon.getItem(), WINCH_COOLDOWN);
            }
            case TRIDENT_DE_TREMPE -> {
                overheat(player);
                player.getCooldowns().addCooldown(weapon.getItem(), OVERHEAT_TICKS + COOLING_TICKS);
            }
            default -> {
                return false;
            }
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        weapon.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return true;
    }

    /** Un coup de mécanisme : la source est le joueur, mais ce n'est pas un coup d'épée (pas de Spécial relancé). */
    private static boolean strike(ServerPlayer player, LivingEntity target, float damage) {
        if (target == player || !target.isAlive() || target.isSpectator()) return false;
        DamageSource source = player.damageSources().mobAttack(player);
        target.invulnerableTime = 0;
        return target.hurt(source, damage);
    }

    private static List<LivingEntity> targetsAlong(ServerPlayer player, Vec3 from, Vec3 to, double inflate) {
        AABB box = new AABB(from, to).inflate(inflate + 1);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !e.isSpectator())) {
            AABB body = e.getBoundingBox().inflate(inflate);
            if (body.contains(from) || body.clip(from, to).isPresent()) found.add(e);
        }
        found.sort((a, b) -> Double.compare(a.distanceToSqr(from), b.distanceToSqr(from)));
        return found;
    }

    /** Jusqu'où l'on voit dans cette direction avant un bloc (au plus range). */
    private static Vec3 reach(ServerPlayer player, Vec3 from, Vec3 direction, double range) {
        Vec3 to = from.add(direction.scale(range));
        var hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** Piston : les pistons des machines. */
    private static void thrust(ServerPlayer player, float damage) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 end = reach(player, eye, look, PISTON_RANGE);
        for (LivingEntity target : targetsAlong(player, eye, end, 0.3)) strike(player, target, damage);
        ServerLevel level = player.serverLevel();
        for (double d = 0.5; d <= eye.distanceTo(end); d += 0.4) {
            Vec3 p = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y - 0.2, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.9F, 1.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 0.9F);
    }

    /** Sillon : la hache fend la roche, l'onde court devant elle. */
    private static void furrow(ServerPlayer player, float damage) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0E-4) look = Vec3.directionFromRotation(0, player.getYRot());
        FURROWS.add(new Furrow(player, player.position(), look.normalize(), damage, new HashSet<>(), new int[]{0}));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        runFurrows();
    }

    /** Public pour les tests : une case de plus pour chaque onde en route. */
    public static void runFurrows() {
        Iterator<Furrow> it = FURROWS.iterator();
        while (it.hasNext()) {
            Furrow f = it.next();
            int step = ++f.done()[0];
            if (step > FURROW_LENGTH || !f.player().isAlive()) {
                it.remove();
                continue;
            }
            ServerLevel level = f.player().serverLevel();
            Vec3 at = f.from().add(f.step().scale(step));
            BlockPos ground = BlockPos.containing(at.x, at.y - 0.5, at.z);
            BlockState below = level.getBlockState(ground);
            if (!below.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), at.x, at.y + 0.1, at.z, 12, 0.3, 0.1, 0.3, 0.15);
            }
            AABB zone = new AABB(at.x - 0.8, at.y - 0.5, at.z - 0.8, at.x + 0.8, at.y + 1.6, at.z + 0.8);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, zone, e -> e != f.player() && e.isAlive())) {
                if (f.hit().add(target.getUUID())) strike(f.player(), target, f.damage());
            }
        }
    }

    /** Frappe-sol : le gong sourd d'un marteau à vapeur qui tombe. */
    private static void slam(ServerPlayer player, float damage) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SLAM_RADIUS),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= SLAM_RADIUS * SLAM_RADIUS)) {
            if (!strike(player, target, damage)) continue;
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) continue;
            away = away.normalize();
            target.knockback(SLAM_PUSH, -away.x, -away.z);
            target.hurtMarked = true;
        }
        BlockState below = level.getBlockState(player.getBlockPosBelowThatAffectsMyMovement());
        for (int i = 0; i < 20; i++) {
            double angle = i * Math.PI * 2 / 20;
            double x = player.getX() + Math.cos(angle) * 1.5, z = player.getZ() + Math.sin(angle) * 1.5;
            if (!below.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), x, player.getY() + 0.1, z, 3, 0.1, 0.05, 0.1, 0.1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Treuil : les treuils qui remontaient les charges des machines. */
    private static void winch(ServerPlayer player, float damage) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 end = reach(player, eye, look, WINCH_RANGE);
        List<LivingEntity> along = targetsAlong(player, eye, end, 0.3);
        if (!along.isEmpty()) {
            LivingEntity first = along.get(0);
            strike(player, first, damage);
            end = first.getBoundingBox().getCenter();
        }
        ServerLevel level = player.serverLevel();
        ItemParticleOption chain = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(net.minecraft.world.item.Items.CHAIN));
        for (double d = 0.8; d <= eye.distanceTo(end); d += 0.5) {
            Vec3 p = eye.add(look.scale(d));
            level.sendParticles(chain, p.x, p.y - 0.25, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, player.getMainHandItem()), end.x, end.y, end.z, 6, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, BlockPos.containing(end), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Trident de trempe : le métal chauffé ; plongé dans l'eau, c'est la trempe. */
    private static void overheat(ServerPlayer player) {
        OVERHEAT_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + OVERHEAT_TICKS);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 0.6F);
        player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
    }

    public static boolean overheated(Player player) {
        Long until = OVERHEAT_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until && of(player.getMainHandItem()) == Mechanism.TRIDENT_DE_TREMPE;
    }

    /** La surchauffe : les coups de trident frappent plus fort et brûlent ; mouillé, la vapeur frappe encore plus fort, sans feu. */
    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player || !overheated(player)) return;
        boolean quenched = player.isInWaterRainOrBubble();
        event.setAmount(event.getAmount() * (quenched ? QUENCH_BONUS : OVERHEAT_BONUS));
        LivingEntity target = event.getEntity();
        ServerLevel level = player.serverLevel();
        if (quenched) {
            level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.03);
            level.playSound(null, target.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
        } else {
            target.igniteForSeconds(BURN_SECONDS);
        }
    }
}
