package fr.anger.echoes.fireforge.forging;

import fr.anger.echoes.core.soul.Souls;
import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Les Spéciaux âme d'Anger qui ne dépendent pas du Silt (étape 6d, plan validé). Ils ne sortent que du feu des âmes.
 * Écho de lame, Harpon des rivières et Glas se paient en âmes (Core) : sans âmes, l'effet n'a pas lieu.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class SoulSpecialEffects {

    private SoulSpecialEffects() {}

    // ------------------------------------------------------------------ chiffres
    /** Écho de lame : un coup sur 5 se répète à 50 % une seconde plus tard, pour 1 âme. */
    public static final int ECHO_EVERY = 5, ECHO_DELAY = 20, ECHO_COST = 1;
    public static final float ECHO_PART = 0.5F;
    /** Carreau muet : rechargement 20 % plus lent (ajouté à la statistique de rechargement). */
    public static final float SILENT_RELOAD = -20F;
    /** Harpon des rivières : 1 âme par lancer ; la créature touchée est ramenée vers le lanceur. */
    public static final int HARPOON_COST = 1;
    public static final double HARPOON_PULL = 1.1;
    /** Glas : 3 âmes ; les monstres à 4 blocs sont figés 1 s. */
    public static final int KNELL_COST = 3, KNELL_TICKS = 20;
    public static final double KNELL_RADIUS = 4.0;
    /** Pas feutré : en courant, un pas sur deux sans vibration ; −5 % de vitesse. */
    public static final double MUFFLED_SPEED = -0.05;
    /** Regard du Veilleur : après un coup reçu, les monstres à 16 blocs brillent 5 s ; Vision nocturne à moitié. */
    public static final double GAZE_RADIUS = 16.0;
    public static final int GAZE_TICKS = 100;

    private static final ResourceLocation MUFFLED_ID = EchoesFireForge.id("special/pas_feutre");
    private static final String SILENT_BOLT = EchoesFireForge.MOD_ID + ":silent_bolt";
    private static final String HARPOON = EchoesFireForge.MOD_ID + ":harpoon";

    /** Un écho en attente : il frappera la cible à ce tick (côté serveur). */
    private record PendingEcho(ServerPlayer attacker, LivingEntity target, float amount, long due) {}

    private static final List<PendingEcho> ECHOES = new ArrayList<>();
    private static final Map<UUID, Integer> ECHO_COUNT = new HashMap<>();
    private static final Map<UUID, Boolean> STEP_SKIP = new HashMap<>();
    /** Les créatures touchées par un carreau muet, ce tick : leur cri de douleur ne fait pas de vibration. */
    private static final Map<UUID, Long> HUSHED = new HashMap<>();
    /** Les créatures harponnées, ce tick : le recul du coup ne doit pas annuler la prise. */
    private static final Map<UUID, Long> HARPOONED = new HashMap<>();
    /** Joueurs dont la Vision nocturne vient d'arriver : on la raccourcit au tick suivant. */
    private static final Set<UUID> NIGHT_VISION_TO_HALVE = new HashSet<>();
    private static final Set<UUID> NIGHT_VISION_HALVED = new HashSet<>();

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        ECHO_COUNT.remove(id);
        STEP_SKIP.remove(id);
        NIGHT_VISION_TO_HALVE.remove(id);
        NIGHT_VISION_HALVED.remove(id);
    }

    private static boolean pay(Player player, int cost) {
        return player instanceof ServerPlayer server && Souls.spend(server, cost);
    }

    // ------------------------------------------------------------------ Écho de lame, Glas, Regard (coups au corps à corps)

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        // Regard du Veilleur : ils veillaient dans le noir total
        if (target instanceof Player hurt && SpecialEffects.wears(hurt, Special.REGARD_DU_VEILLEUR)) {
            AABB area = hurt.getBoundingBox().inflate(GAZE_RADIUS);
            for (LivingEntity mob : hurt.level().getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof Enemy && e.isAlive())) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, GAZE_TICKS, 0, false, false));
            }
        }

        // Harpon des rivières : ramène la créature touchée par le trident lancé
        if (event.getSource().getDirectEntity() instanceof ThrownTrident trident && trident.getPersistentData().getBoolean(HARPOON)
                && trident.getOwner() instanceof Player owner) {
            Vec3 pull = owner.position().subtract(target.position());
            if (pull.lengthSqr() > 1) {
                Vec3 dir = pull.normalize().scale(HARPOON_PULL).add(0, 0.35, 0);
                target.setDeltaMovement(dir);
                target.hurtMarked = true;
                HARPOONED.put(target.getUUID(), target.level().getGameTime());
            }
            trident.getPersistentData().putBoolean(HARPOON, false); // une seule prise par lancer
        }

        Player attacker = SpecialEffects.meleeAttacker(event.getSource());
        if (!(attacker instanceof ServerPlayer server)) return;

        // Écho de lame : le chant laisse ses ondes dans la pierre
        if (SpecialEffects.wields(server, Special.ECHO_DE_LAME)) {
            int count = ECHO_COUNT.merge(server.getUUID(), 1, Integer::sum);
            if (count % ECHO_EVERY == 0 && event.getNewDamage() > 0) {
                ECHOES.add(new PendingEcho(server, target, event.getNewDamage() * ECHO_PART, server.level().getGameTime() + ECHO_DELAY));
            }
        }

        // Glas : un coup en tombant sonne comme le gong des Instruments des Bâtisseurs
        if (SpecialEffects.wields(server, Special.GLAS) && MaceItem.canSmashAttack(server) && pay(server, KNELL_COST)) {
            ServerLevel level = server.serverLevel();
            level.playSound(null, target.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.5F);
            level.playSound(null, target.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            level.sendParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
            AABB area = target.getBoundingBox().inflate(KNELL_RADIUS);
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof Enemy && e.isAlive() && e != server)) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, KNELL_TICKS, 9, false, true));
                mob.setDeltaMovement(Vec3.ZERO);
                if (mob instanceof Mob m) m.getNavigation().stop();
            }
        }

    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (HARPOONED.getOrDefault(event.getEntity().getUUID(), Long.MIN_VALUE) == event.getEntity().level().getGameTime()) event.setCanceled(true);
    }

    /** Public pour les tests : le nombre d'échos en attente. */
    public static int pendingEchoes() {
        return ECHOES.size();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // ces marques ne valent que pour le tick où elles ont été posées
        HUSHED.clear();
        HARPOONED.clear();
        if (ECHOES.isEmpty()) return;
        Iterator<PendingEcho> it = ECHOES.iterator();
        while (it.hasNext()) {
            PendingEcho echo = it.next();
            if (echo.attacker().level().getGameTime() < echo.due()) continue;
            it.remove();
            runEcho(echo);
        }
    }

    /** Public pour les tests : l'écho frappe (s'il y a une âme pour le payer). */
    public static void runPendingEchoesNow() {
        List<PendingEcho> all = new ArrayList<>(ECHOES);
        ECHOES.clear();
        all.forEach(SoulSpecialEffects::runEcho);
    }

    private static void runEcho(PendingEcho echo) {
        LivingEntity target = echo.target();
        ServerPlayer attacker = echo.attacker();
        if (!target.isAlive() || target.level() != attacker.level() || !pay(attacker, ECHO_COST)) return;
        target.invulnerableTime = 0;
        // une onde, pas un coup d'épée : elle ne relance pas les autres Spéciaux
        target.hurt(target.damageSources().indirectMagic(attacker, attacker), echo.amount());
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getEyeY(), target.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
            level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    // ------------------------------------------------------------------ Carreau muet, Harpon (projectiles)

    @SubscribeEvent
    public static void onProjectile(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractArrow arrow) || arrow.getWeaponItem() == null) return;
        Special special = SpecialEffects.specialOf(arrow.getWeaponItem());
        if (special == Special.CARREAU_MUET) {
            // la vapeur d'écho de la Résonance étouffe les sons
            arrow.setSilent(true);
            arrow.getPersistentData().putBoolean(SILENT_BOLT, true);
        } else if (special == Special.HARPON_DES_RIVIERES && arrow instanceof ThrownTrident trident
                && !trident.getPersistentData().contains(HARPOON) && trident.getOwner() instanceof Player owner) {
            // les Veilleurs pêchaient le Poisson-lie dans les rivières d'âme du Récif
            trident.getPersistentData().putBoolean(HARPOON, pay(owner, HARPOON_COST));
        }
    }

    /** Le tir de l'arbalète muette ne fait pas de bruit. */
    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null || !event.getSound().is(SoundEvents.CROSSBOW_SHOOT.getLocation()) || event.getLevel().isClientSide) return;
        Vec3 at = event.getPosition();
        for (Player player : event.getLevel().players()) {
            if (player.distanceToSqr(at) < 2.0 && holdsSilentCrossbow(player)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static boolean holdsSilentCrossbow(Player player) {
        return SpecialEffects.specialOf(player.getMainHandItem()) == Special.CARREAU_MUET
                || SpecialEffects.specialOf(player.getOffhandItem()) == Special.CARREAU_MUET;
    }

    /** Rechargement plus lent de l'arbalète muette (lu par ForgingEvents.onUseTick). */
    public static float reloadPenalty(ItemStack stack) {
        return SpecialEffects.specialOf(stack) == Special.CARREAU_MUET ? SILENT_RELOAD : 0F;
    }

    @SubscribeEvent
    public static void onSilentHit(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getPersistentData().getBoolean(SILENT_BOLT)) {
            HUSHED.put(event.getEntity().getUUID(), event.getEntity().level().getGameTime());
        }
    }

    /**
     * Les vibrations étouffées : le tir et l'impact d'un carreau muet, le cri de sa cible (elle ne prévient pas les autres),
     * et un pas sur deux d'un porteur de Pas feutré qui court.
     */
    @SubscribeEvent
    public static void onVibration(VanillaGameEvent event) {
        if (event.getLevel().isClientSide) return;
        var type = event.getVanillaEvent();
        Entity cause = event.getCause();
        if (type.is(GameEvent.PROJECTILE_SHOOT.key()) && cause instanceof Player shooter && holdsSilentCrossbow(shooter)) {
            event.setCanceled(true);
        } else if (type.is(GameEvent.PROJECTILE_LAND.key()) && cause instanceof AbstractArrow arrow && arrow.getPersistentData().getBoolean(SILENT_BOLT)) {
            event.setCanceled(true);
        } else if (type.is(GameEvent.PROJECTILE_LAND.key()) && isNearSilentBolt(event)) {
            event.setCanceled(true);
        } else if (type.is(GameEvent.ENTITY_DAMAGE.key()) && cause != null && HUSHED.getOrDefault(cause.getUUID(), Long.MIN_VALUE) == event.getLevel().getGameTime()) {
            event.setCanceled(true);
        } else if (type.is(GameEvent.STEP.key()) && cause instanceof Player runner && runner.isSprinting() && SpecialEffects.wears(runner, Special.PAS_FEUTRE)) {
            // les Veilleurs vivaient dans le silence
            boolean skip = !STEP_SKIP.getOrDefault(runner.getUUID(), false);
            STEP_SKIP.put(runner.getUUID(), skip);
            if (skip) event.setCanceled(true);
        }
    }

    private static boolean isNearSilentBolt(VanillaGameEvent event) {
        Vec3 at = event.getEventPosition();
        return !event.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(at, at).inflate(1.0),
                a -> a.getPersistentData().getBoolean(SILENT_BOLT)).isEmpty();
    }

    // ------------------------------------------------------------------ Pas feutré, Regard du Veilleur (chaque tick)

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        tick(event.getEntity());
    }

    /** Public pour les tests : le travail d'un tick pour ce joueur. */
    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (SpecialEffects.wears(player, Special.PAS_FEUTRE)) {
                if (speed.getModifier(MUFFLED_ID) == null) speed.addTransientModifier(new AttributeModifier(MUFFLED_ID, MUFFLED_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (speed.getModifier(MUFFLED_ID) != null) {
                speed.removeModifier(MUFFLED_ID);
            }
        }
        // Regard du Veilleur : la Vision nocturne dure moitié moins (une fois par effet reçu)
        if (NIGHT_VISION_TO_HALVE.remove(player.getUUID())) {
            MobEffectInstance vision = player.getEffect(MobEffects.NIGHT_VISION);
            if (vision != null && !vision.isInfiniteDuration() && SpecialEffects.wears(player, Special.REGARD_DU_VEILLEUR)) {
                NIGHT_VISION_HALVED.add(player.getUUID());
                player.removeEffect(MobEffects.NIGHT_VISION);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.max(1, vision.getDuration() / 2),
                        vision.getAmplifier(), vision.isAmbient(), vision.isVisible(), vision.showIcon()));
            }
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (!event.getEffectInstance().is(MobEffects.NIGHT_VISION) || !SpecialEffects.wears(player, Special.REGARD_DU_VEILLEUR)) return;
        // l'effet raccourci par nous-mêmes ne se raccourcit pas une deuxième fois
        if (NIGHT_VISION_HALVED.remove(player.getUUID())) return;
        NIGHT_VISION_TO_HALVE.add(player.getUUID());
    }

    /** Public pour les tests. */
    @Nullable
    public static Boolean harpoonPaid(ThrownTrident trident) {
        return trident.getPersistentData().contains(HARPOON) ? trident.getPersistentData().getBoolean(HARPOON) : null;
    }
}
