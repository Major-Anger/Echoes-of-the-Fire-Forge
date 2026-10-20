package fr.anger.echoes.fireforge.forging;

import fr.anger.echoes.core.noise.Noise;
import fr.anger.echoes.core.noise.NoiseEvent;
import fr.anger.echoes.core.soul.SoulChangeEvent;
import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Les Spéciaux âme liés au Silt (étape 6e) : Urne close, Fendeuse de racines, Corde chantante, et la part de Pas feutré
 * qui passe par le bruit. Tout passe par les événements du Core ({@link SoulChangeEvent}, {@link NoiseEvent}) et par le tag
 * {@code echoes_silt:biomass} : la Forge n'appelle jamais le code du Silt, et tout reste jouable avec le Core seul.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class SiltSpecialEffects {

    private SiltSpecialEffects() {}

    /** Urne close : les pertes passives réduites de moitié, les gains de 25 %. */
    public static final double URN_LOSS = 0.5, URN_GAIN = 0.75;
    /** Fendeuse de racines : +40 % contre la biomasse, −10 % contre le reste. */
    public static final float ROOT_BONUS = 1.4F, ROOT_MALUS = 0.9F;
    /** Corde chantante : 3 touches d'affilée sur la même cible, puis une flèche qui transperce avec +50 % de dégâts. */
    public static final int SONG_HITS = 3;
    public static final float SONG_DAMAGE = 1.5F, SHOT_NOISE = 3.0F;
    /** Pas feutré : le bruit de course réduit de moitié. */
    public static final float MUFFLED_RUN = 0.5F;

    public static final TagKey<EntityType<?>> BIOMASS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("echoes_silt", "biomass"));

    private static final String SINGING = EchoesFireForge.MOD_ID + ":singing";
    private static final String SUNG = EchoesFireForge.MOD_ID + ":sung";

    /** Les fractions d'âme gardées d'une fois sur l'autre (la jauge compte en âmes entières). */
    private static final Map<UUID, double[]> URN_CARRY = new HashMap<>();
    private record Streak(UUID target, int hits) {}
    private static final Map<UUID, Streak> STREAKS = new HashMap<>();

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        URN_CARRY.remove(event.getEntity().getUUID());
        STREAKS.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ Urne close

    /** Les Veilleurs gardaient leurs morts au chaud dans des urnes. */
    @SubscribeEvent
    public static void onSouls(SoulChangeEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!SpecialEffects.wears(player, Special.URNE_CLOSE)) return;
        int slot;
        double factor;
        switch (event.getCause()) {
            case LOSS -> { slot = 0; factor = URN_LOSS; }
            case GAIN -> { slot = 1; factor = URN_GAIN; }
            default -> { return; } // une dépense ou une correction n'est pas une âme perdue
        }
        double[] carry = URN_CARRY.computeIfAbsent(player.getUUID(), id -> new double[2]);
        double exact = event.getAmount() * factor + carry[slot];
        int whole = (int) exact; // vers zéro : on ne perd et ne gagne que des âmes entières
        carry[slot] = exact - whole;
        event.setAmount(whole);
    }

    // ------------------------------------------------------------------ Fendeuse de racines

    /** Les Veilleurs forgeaient leurs armes contre la biomasse. */
    @SubscribeEvent
    public static void onAxe(LivingIncomingDamageEvent event) {
        Player attacker = SpecialEffects.meleeAttacker(event.getSource());
        if (attacker == null || !SpecialEffects.wields(attacker, Special.FENDEUSE_DE_RACINES)) return;
        event.setAmount(event.getAmount() * (event.getEntity().getType().is(BIOMASS) ? ROOT_BONUS : ROOT_MALUS));
    }

    // ------------------------------------------------------------------ Corde chantante

    /** La musique des Bâtisseurs : chaque tir sonne une note, et fait du bruit. */
    @SubscribeEvent
    public static void onShot(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractArrow arrow) || arrow.getWeaponItem() == null
                || SpecialEffects.specialOf(arrow.getWeaponItem()) != Special.CORDE_CHANTANTE
                || !(arrow.getOwner() instanceof ServerPlayer owner) || arrow.getPersistentData().contains(SINGING)) return;
        Streak streak = STREAKS.get(owner.getUUID());
        int hits = streak != null ? streak.hits() : 0;
        boolean sung = hits >= SONG_HITS;
        arrow.getPersistentData().putBoolean(SINGING, true);
        if (sung) {
            arrow.getPersistentData().putBoolean(SUNG, true);
            arrow.setPierceLevel((byte) (arrow.getPierceLevel() + 1));
            STREAKS.remove(owner.getUUID());
        }
        // la note monte à chaque touche ; la flèche chantée sonne la plus haute
        float pitch = sung ? 2.0F : 0.8F + 0.3F * hits;
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, pitch);
        if (sung) owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
        Noise.make(owner, SHOT_NOISE, Noise.SHOT);
    }

    /** La flèche chantée frappe plus fort. */
    @SubscribeEvent
    public static void onSungHit(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getPersistentData().getBoolean(SUNG)) {
            event.setAmount(event.getAmount() * SONG_DAMAGE);
        }
    }

    /** Les touches d'affilée sur la même cible. */
    @SubscribeEvent
    public static void onArrowHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !arrow.getPersistentData().getBoolean(SINGING)
                || arrow.getPersistentData().getBoolean(SUNG) || !(arrow.getOwner() instanceof ServerPlayer owner)) return;
        UUID target = event.getEntity().getUUID();
        Streak streak = STREAKS.get(owner.getUUID());
        STREAKS.put(owner.getUUID(), new Streak(target, streak != null && streak.target().equals(target) ? streak.hits() + 1 : 1));
    }

    /** Une flèche à côté casse la série. */
    @SubscribeEvent
    public static void onMiss(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK && event.getProjectile() instanceof AbstractArrow arrow
                && arrow.getPersistentData().getBoolean(SINGING) && !arrow.getPersistentData().getBoolean(SUNG)
                && arrow.getOwner() instanceof ServerPlayer owner) {
            STREAKS.remove(owner.getUUID());
        }
    }

    /** Public pour les tests : les touches d'affilée du tireur. */
    public static int streak(Player player) {
        Streak streak = STREAKS.get(player.getUUID());
        return streak != null ? streak.hits() : 0;
    }

    // ------------------------------------------------------------------ Pas feutré (le bruit)

    /** Les Veilleurs vivaient dans le silence : la course fait moitié moins de bruit. */
    @SubscribeEvent
    public static void onNoise(NoiseEvent.Pre event) {
        if (event.getCause().equals(Noise.RUN) && SpecialEffects.wears(event.getPlayer(), Special.PAS_FEUTRE)) {
            event.setAmount(event.getAmount() * MUFFLED_RUN);
        }
    }
}
