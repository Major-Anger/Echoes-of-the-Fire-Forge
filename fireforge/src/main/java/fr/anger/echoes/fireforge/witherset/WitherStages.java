package fr.anger.echoes.fireforge.witherset;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Field;

/**
 * Les paliers du set du Wither (étape 13b) : à chacun un défaut, et en contrepartie un bonus. Ils s'ajoutent : au
 * palier 3, on a les défauts et les bonus des paliers 1, 2 et 3. Redescendre sous un palier retire les deux.
 * <ul>
 * <li>1 : les loups sauvages t'attaquent, comme un squelette ; le feu et la lave font moitié moins mal ;</li>
 * <li>2 : les animaux d'élevage te fuient ; l'effet Wither ne te touche plus ;</li>
 * <li>3 : les villageois te fuient de temps en temps, et leurs prix montent ; tes coups infligent Wither (3 s) ;</li>
 * <li>4 : les golems de fer t'attaquent ; les wither squelettes et les Cendreux te prennent pour l'un des leurs.</li>
 * </ul>
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class WitherStages {

    private WitherStages() {}

    public static final int WOLVES = 1, FIRE = 1, ANIMALS = 2, WITHER_IMMUNE = 2, VILLAGERS = 3, WITHER_HITS = 3, GOLEMS = 4, KIN = 4;
    /** Les animaux d'élevage qui fuient le porteur (ceux qui marchent aux « buts » : vache, mouton, cochon, poule, lapin). */
    public static final TagKey<EntityType<?>> FLEES = TagKey.create(Registries.ENTITY_TYPE, EchoesFireForge.id("flees_wither"));
    /** Wither infligé par les coups au palier 3 : 3 s. */
    public static final int WITHER_TICKS = 60;
    /** Les villageois : à 10 blocs, toutes les 5 s, une chance sur trois de prendre peur ; les prix montent de moitié. */
    public static final double VILLAGER_RANGE = 10, VILLAGER_CHANCE = 1 / 3.0, PRICE_RISE = 0.5;
    public static final int VILLAGER_CHECK_TICKS = 100, PANIC_TICKS = 100;

    /** Le palier d'un joueur, côté serveur (0 pour tout le reste, et pour le client). */
    public static int stage(LivingEntity entity) {
        return entity instanceof ServerPlayer player && !player.isSpectator() ? WitherSet.stage(player) : 0;
    }

    // ------------------------------------------------------------------ les créatures (leurs buts, à leur arrivée)

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Wolf wolf) {
            // 1 : un loup sauvage le prend pour un squelette
            wolf.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(wolf, Player.class, 10, true, false,
                    target -> !wolf.isTame() && stage(target) >= WOLVES));
        } else if (event.getEntity() instanceof IronGolem golem) {
            // 4 : les golems du village l'attaquent (pas ceux qu'un joueur a bâtis : ils n'attaquent jamais un joueur)
            golem.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(golem, Player.class, 10, true, false,
                    target -> stage(target) >= GOLEMS));
        } else if (event.getEntity() instanceof PathfinderMob animal && animal.getType().is(FLEES)) {
            // 2 : les animaux d'élevage le fuient
            animal.goalSelector.addGoal(1, new AvoidEntityGoal<>(animal, Player.class, 10.0F, 1.0, 1.3,
                    target -> stage(target) >= ANIMALS));
        }
    }

    // ------------------------------------------------------------------ 3 : les villageois

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        int stage = stage(player);
        // 2 : un Wither qui le touchait déjà s'éteint
        if (stage >= WITHER_IMMUNE && player.hasEffect(MobEffects.WITHER)) player.removeEffect(MobEffects.WITHER);
        if (player.tickCount % VILLAGER_CHECK_TICKS == 0) scareVillagers(player, VILLAGER_CHANCE);
    }

    /** Les villageois qui le voient prennent peur (de temps en temps) : ils paniquent et le fuient, comme devant un zombie. */
    public static void scareVillagers(ServerPlayer player, double chance) {
        if (stage(player) < VILLAGERS) return;
        for (Villager villager : player.level().getEntitiesOfClass(Villager.class, new AABB(player.blockPosition()).inflate(VILLAGER_RANGE))) {
            if (villager.isSleeping() || villager.getTradingPlayer() == player || player.getRandom().nextDouble() >= chance
                    || !villager.hasLineOfSight(player)) continue;
            DamageSource fright = player.damageSources().playerAttack(player);
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY, fright, PANIC_TICKS);
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY_ENTITY, player, PANIC_TICKS);
        }
    }

    private static final Field TRADER = traderField();

    private static Field traderField() {
        try {
            Field field = MerchantMenu.class.getDeclaredField("trader");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            EchoesFireForge.LOGGER.warn("Set du Wither : le marchand d'une interface d'échange est introuvable, les prix ne monteront pas", e);
            return null;
        }
    }

    /**
     * Leurs prix montent : à l'ouverture de l'échange (après les prix du jour, avant l'envoi des offres), chaque offre
     * coûte moitié plus. Les villageois remettent leurs prix à zéro à la fin de l'échange.
     */
    @SubscribeEvent
    public static void onTrade(PlayerContainerEvent.Open event) {
        if (!(event.getContainer() instanceof MerchantMenu menu) || stage(event.getEntity()) < VILLAGERS || TRADER == null) return;
        Merchant trader;
        try {
            trader = (Merchant) TRADER.get(menu);
        } catch (IllegalAccessException e) {
            return;
        }
        if (!(trader instanceof Villager)) return; // le marchand ambulant ne remet pas ses prix à zéro
        for (MerchantOffer offer : menu.getOffers()) {
            offer.addToSpecialPriceDiff(Math.max(1, (int) Math.ceil(offer.getBaseCostA().getCount() * PRICE_RISE)));
        }
    }

    // ------------------------------------------------------------------ les bonus

    /** 1 : le feu et la lave font moitié moins mal. */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && stage(event.getEntity()) >= FIRE) event.setAmount(event.getAmount() * 0.5F);
    }

    /** 3 : ses coups infligent Wither. */
    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (event.getNewDamage() > 0 && source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof ServerPlayer attacker
                && stage(attacker) >= WITHER_HITS && event.getEntity() != attacker) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS), attacker);
        }
    }

    /** 2 : l'effet Wither ne le touche plus. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.WITHER) && stage(event.getEntity()) >= WITHER_IMMUNE) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** 4 : les wither squelettes et les Cendreux le prennent pour l'un des leurs, sauf s'il les frappe. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity(), target = event.getNewAboutToBeSetTarget();
        if ((mob instanceof WitherSkeleton || mob instanceof CendreuxEntity) && target != null && stage(target) >= KIN
                && mob.getLastHurtByMob() != target) {
            event.setCanceled(true);
        }
    }
}
