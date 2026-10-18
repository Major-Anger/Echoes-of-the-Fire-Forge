package fr.anger.echoes.fireforge.forging;

import fr.anger.echoes.core.soul.Souls;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Les effets des Spéciaux (étape 6, chiffres validés par Anger) et leurs contreparties.
 * Un Spécial n'agit que là où sa pièce se porte : l'arme en main, l'armure sur soi.
 * Les Lunettes d'Attiseur agissent sur le brouillard, côté client (voir client.SpecialFog).
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class SpecialEffects {

    private SpecialEffects() {}

    // ------------------------------------------------------------------ chiffres
    /** Soupape : la vapeur aveugle 1 s ; 4 coups en moins de 3 s brûlent le porteur 2 s. */
    public static final int STEAM_TICKS = 20, OVERHEAT_HITS = 4, OVERHEAT_WINDOW = 60, OVERHEAT_FIRE_SECONDS = 2;
    /** Marteau-pilon : un coup sur trois frappe deux fois plus fort ; vitesse d'attaque −10 %. */
    public static final int HAMMER_EVERY = 3;
    public static final float HAMMER_MULTIPLIER = 2.0F;
    public static final double HAMMER_SPEED = -0.10;
    /** Montée en pression : +5 % de vitesse d'attaque par coup sur la même cible, jusqu'à 5 coups ; 3 s pour retomber. */
    public static final double PRESSURE_STEP = 0.05;
    public static final int PRESSURE_MAX = 5, PRESSURE_TIMEOUT = 60, PRESSURE_SLOWNESS = 20;
    /** Chaudière : feu et lave −50 % ; mouillée, elle s'éteint : lenteur, et encore 5 s après. */
    public static final float BOILER_FIRE_FACTOR = 0.5F;
    public static final int BOILER_WET_TICKS = 100;
    /** Pas de fondeur : ~40 % plus vite dans la lave, −6 % sur la terre ferme. */
    public static final double LAVA_PUSH = 1.29, GROUND_SPEED = -0.06;
    /** Soufflet : une poussée d'environ 3 blocs, 2 s de recharge, 4 points d'usure. */
    public static final double BELLOWS_PUSH = 0.28;
    public static final int BELLOWS_COOLDOWN = 40, BELLOWS_WEAR = 4;
    /** Lame des Veilleurs (âme) : 1 âme par ennemi tué ; −10 % de dégâts en plein jour. */
    public static final int WATCHER_SOULS = 1;
    public static final float WATCHER_DAYLIGHT = 0.9F;

    private static final ResourceLocation HAMMER_ID = EchoesFireForge.id("special/marteau_pilon");
    private static final ResourceLocation PRESSURE_ID = EchoesFireForge.id("special/montee_en_pression");
    private static final ResourceLocation STRIDE_ID = EchoesFireForge.id("special/pas_de_fondeur");

    /** L'état passager de chaque joueur (côté serveur) : coups récents, pression, chaudière mouillée. */
    private static final class State {
        /** Les moments des derniers coups de Soupape (NO_HIT : pas de coup). */
        final long[] hits = newHits();
        int hitIndex;
        int hammerCount;
        @Nullable UUID pressureTarget;
        int pressure;
        long lastPressureHit;
        int wet;
        boolean regenSkip;
    }

    private static final long NO_HIT = Long.MIN_VALUE;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static long[] newHits() {
        long[] hits = new long[OVERHEAT_HITS];
        java.util.Arrays.fill(hits, NO_HIT);
        return hits;
    }

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ qui porte quoi

    @Nullable
    public static Special specialOf(ItemStack stack) {
        ForgingData data = stack.get(FFDataComponents.FORGING);
        return data != null ? data.special().orElse(null) : null;
    }

    /** L'arme en main porte ce Spécial. */
    public static boolean wields(LivingEntity entity, Special special) {
        return specialOf(entity.getMainHandItem()) == special;
    }

    /** Une pièce d'armure portée (à sa place) porte ce Spécial. */
    public static boolean wears(LivingEntity entity, Special special) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (specialOf(stack) == special && Forging.slotOf(stack) == slot) return true;
        }
        return false;
    }

    /** Un coup d'arme au corps à corps, porté par un joueur. */
    @Nullable
    private static Player meleeAttacker(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof Player player && source.getDirectEntity() == player ? player : null;
    }

    /** En plein jour, à ciel ouvert, hors du Nether (Lunettes, Lame des Veilleurs). */
    public static boolean inDaylight(Entity entity) {
        Level level = entity.level();
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() && level.isDay()
                && level.canSeeSky(entity.blockPosition().above());
    }

    // ------------------------------------------------------------------ armes : avant le coup

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();

        // Chaudière : les Attiseurs travaillaient au cœur du feu
        if (source.is(DamageTypeTags.IS_FIRE) && wears(target, Special.CHAUDIERE)) event.setAmount(event.getAmount() * BOILER_FIRE_FACTOR);
        // Pas de fondeur : ils marchaient au bord des bassins de fonte
        if (source.is(DamageTypes.HOT_FLOOR) && wears(target, Special.PAS_DE_FONDEUR)) {
            event.setCanceled(true);
            return;
        }

        Player attacker = meleeAttacker(source);
        if (attacker == null || attacker.level().isClientSide) return;
        // Marteau-pilon : un coup sur trois tombe de tout son poids
        if (wields(attacker, Special.MARTEAU_PILON) && ++state(attacker).hammerCount % HAMMER_EVERY == 0) {
            event.setAmount(event.getAmount() * HAMMER_MULTIPLIER);
            Vec3 push = target.position().subtract(attacker.position()).normalize();
            target.knockback(0.8, -push.x, -push.z);
            attacker.level().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 0.6F);
        }
        // Lame des Veilleurs : les Veilleurs vivent dans le noir
        if (wields(attacker, Special.LAME_DES_VEILLEURS) && inDaylight(attacker)) event.setAmount(event.getAmount() * WATCHER_DAYLIGHT);
    }

    // ------------------------------------------------------------------ armes : après le coup

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        Player attacker = meleeAttacker(event.getSource());
        if (attacker == null || attacker.level().isClientSide) return;
        LivingEntity target = event.getEntity();
        long now = attacker.level().getGameTime();
        State state = state(attacker);

        // Soupape : un jet de vapeur à chaque coup ; trop de coups d'affilée, et elle brûle son porteur
        if (wields(attacker, Special.SOUPAPE)) {
            if (target instanceof Player) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STEAM_TICKS + 10, 0));
            } else {
                // la cécité ne gêne guère les monstres : ils perdent leur cible et ralentissent
                if (target instanceof Mob mob) mob.setTarget(null);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STEAM_TICKS, 1));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STEAM_TICKS, 0));
            }
            if (attacker.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getEyeY(), target.getZ(), 6, 0.2, 0.15, 0.2, 0.02);
            }
            attacker.level().playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.5F);
            state.hits[state.hitIndex] = now;
            state.hitIndex = (state.hitIndex + 1) % OVERHEAT_HITS;
            long oldest = state.hits[state.hitIndex]; // le 4e coup avant celui-ci
            if (oldest != NO_HIT && now - oldest < OVERHEAT_WINDOW) {
                attacker.igniteForSeconds(OVERHEAT_FIRE_SECONDS);
                java.util.Arrays.fill(state.hits, NO_HIT);
            }
        }

        // Montée en pression : la machine monte en pression avant sa pleine force
        if (wields(attacker, Special.MONTEE_EN_PRESSION)) {
            if (state.pressureTarget != null && !state.pressureTarget.equals(target.getUUID()) && state.pressure > 0) {
                releasePressure(attacker, state); // changer de cible : tout retombe
            }
            state.pressureTarget = target.getUUID();
            state.pressure = Math.min(PRESSURE_MAX, state.pressure + 1);
            state.lastPressureHit = now;
            setModifier(attacker.getAttribute(Attributes.ATTACK_SPEED), PRESSURE_ID, PRESSURE_STEP * state.pressure, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    private static void releasePressure(Player player, State state) {
        state.pressure = 0;
        state.pressureTarget = null;
        removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), PRESSURE_ID);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PRESSURE_SLOWNESS, 0));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        // Lame des Veilleurs : l'âme de ce qui tombe revient au porteur
        if (event.getSource().getEntity() instanceof ServerPlayer player && meleeAttacker(event.getSource()) == player
                && wields(player, Special.LAME_DES_VEILLEURS) && event.getEntity() != player) {
            Souls.add(player, WATCHER_SOULS);
            player.serverLevel().sendParticles(ParticleTypes.SOUL, event.getEntity().getX(), event.getEntity().getEyeY(), event.getEntity().getZ(), 5, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** Marteau-pilon : la vitesse d'attaque réduite fait partie de l'arme (elle apparaît dans son infobulle). */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        if (specialOf(event.getItemStack()) == Special.MARTEAU_PILON && Forging.slotOf(event.getItemStack()) == EquipmentSlot.MAINHAND) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(HAMMER_ID, HAMMER_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    EquipmentSlotGroup.MAINHAND);
        }
    }

    // ------------------------------------------------------------------ chaque tick

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        tick(event.getEntity());
    }

    /** Public pour les tests : le travail d'un tick pour ce joueur. */
    public static void tick(Player player) {
        // Pas de fondeur dans la lave : le mouvement est calculé par le client du joueur
        if (player.isLocalPlayer() && player.isInLava() && wears(player, Special.PAS_DE_FONDEUR)) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * LAVA_PUSH, v.y, v.z * LAVA_PUSH);
        }
        if (player.level().isClientSide) return;
        State state = state(player);
        long now = player.level().getGameTime();

        // Montée en pression : arrêter de frapper, ou ranger l'arme, fait tout retomber
        if (state.pressure > 0 && (now - state.lastPressureHit > PRESSURE_TIMEOUT || !wields(player, Special.MONTEE_EN_PRESSION))) {
            releasePressure(player, state);
        }

        // Pas de fondeur : plus lent sur la terre ferme
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (wears(player, Special.PAS_DE_FONDEUR) && !player.isInLava()) setModifier(speed, STRIDE_ID, GROUND_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        else removeModifier(speed, STRIDE_ID);

        // Chaudière : mouillée, elle s'éteint (lenteur, et encore 5 s après)
        if (wears(player, Special.CHAUDIERE)) {
            if (player.isInWaterRainOrBubble()) {
                if (state.wet == 0) player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 0.8F);
                state.wet = BOILER_WET_TICKS;
            }
            if (state.wet > 0) {
                state.wet--;
                if (now % 10 == 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, true, false, true));
            }
        } else {
            state.wet = 0;
        }
    }

    /** Public pour les tests : la Chaudière est-elle éteinte (mouillée) en ce moment ? */
    public static boolean boilerWet(Player player) {
        return state(player).wet > 0;
    }

    public static int pressure(Player player) {
        return state(player).pressure;
    }

    // ------------------------------------------------------------------ Soufflet

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.isShiftKeyDown() || !wears(player, Special.SOUFFLET)) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (player.getCooldowns().isOnCooldown(boots.getItem())) return;
        if (player.isLocalPlayer()) {
            // la poussée : c'est le client du joueur qui calcule son mouvement
            player.setDeltaMovement(player.getDeltaMovement().add(0, BELLOWS_PUSH, 0));
        }
        if (player instanceof ServerPlayer server) {
            player.getCooldowns().addCooldown(boots.getItem(), BELLOWS_COOLDOWN);
            boots.hurtAndBreak(BELLOWS_WEAR, server, EquipmentSlot.FEET);
            server.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.25, 0.05, 0.25, 0.04);
            server.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
    }

    // ------------------------------------------------------------------ Linceul de cendre

    /** Immunité à l'effet Wither : une protection contre les « noyés de cendre » (on ne dit pas d'où vient la maladie). */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.WITHER) && wears(event.getEntity(), Special.LINCEUL_DE_CENDRE)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * La régénération par la faim est deux fois plus lente : elle soigne par petites touches (1 point au plus),
     * hors effet Régénération. Les potions et les pommes dorées (Régénération, Soin instantané) ne changent pas.
     */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (event.getAmount() > 1.0F || player.hasEffect(MobEffects.REGENERATION) || !wears(player, Special.LINCEUL_DE_CENDRE)) return;
        State state = state(player);
        state.regenSkip = !state.regenSkip;
        if (state.regenSkip) event.setCanceled(true);
    }

    // ------------------------------------------------------------------ outils

    private static void setModifier(@Nullable AttributeInstance attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(id);
        if (current != null && current.amount() == amount) return;
        attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static void removeModifier(@Nullable AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null && attribute.getModifier(id) != null) attribute.removeModifier(id);
    }
}
