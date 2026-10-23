package fr.anger.echoes.fireforge.mechanism;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forging.Forging;
import fr.anger.echoes.fireforge.network.ProbeResultPayload;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import fr.anger.echoes.fireforge.workbench.Steam;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Les mécanismes d'armure (étape 8c), côté serveur. Les touches arrivent par {@link fr.anger.echoes.fireforge.network.MechanismKeyPayload} :
 * C pour le casque, B pour le plastron, N pour les jambières ; les bottes sont passives. Les mécanismes à vapeur
 * (Planeur, Jet, Pistons) se paient en charges (étape 8b). La Longue-vue est un zoom, côté client ; la Lampe arrive en 8e.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class MechanismEffects {

    private MechanismEffects() {}

    /** Sonde : les minerais à 8 blocs brillent 5 s (côté client) ; recharge 20 s. */
    public static final int PROBE_RADIUS = 8, PROBE_COOLDOWN = 400;
    /** Jet de vapeur : repousse les créatures à 4 blocs ; recharge 1 s. */
    public static final double JET_RADIUS = 4, JET_PUSH = 1.2;
    public static final int JET_COOLDOWN = 20;
    /** Pistons de genoux : un élan vers l'avant ; recharge 1 s. */
    public static final double DASH_SPEED = 1.3;
    public static final int DASH_COOLDOWN = 20;
    /** Planeur : 1 charge par seconde de vol. */
    public static final int GLIDE_TICKS_PER_CHARGE = 20;
    /** Patins : +20 % de vitesse sur la pierre ; Ressorts : saut +50 %, chute amortie de moitié ; Crampons : un bloc sans sauter. */
    public static final double SKATE_SPEED = 0.2, SPRING_JUMP = 0.5, SPRING_FALL = -0.5, CRAMPON_STEP = 0.4;

    private static final ResourceLocation SKATE_ID = EchoesFireForge.id("mechanism/patins");
    private static final ResourceLocation SPRING_JUMP_ID = EchoesFireForge.id("mechanism/ressorts_saut");
    private static final ResourceLocation SPRING_FALL_ID = EchoesFireForge.id("mechanism/ressorts_chute");
    private static final ResourceLocation CRAMPON_STEP_ID = EchoesFireForge.id("mechanism/crampons_pas");
    private static final ResourceLocation CRAMPON_GRIP_ID = EchoesFireForge.id("mechanism/crampons_prise");

    /** Les sols où roulent les Patins : pierre, basalte, pierre noire, briques, métal (les quais des forges). */
    public static final TagKey<Block> SKATE_SURFACES = TagKey.create(Registries.BLOCK, EchoesFireForge.id("skate_surfaces"));
    /** Ce que la Sonde cherche : les minerais, les débris antiques, les machines comprimées. */
    public static final TagKey<Block> PROBE_TARGETS = TagKey.create(Registries.BLOCK, EchoesFireForge.id("probe_targets"));

    private static final Set<UUID> GLIDING = new HashSet<>();
    private static final Map<UUID, Integer> GLIDE_TICKS = new HashMap<>();
    /** Fin des recharges : [jet, pistons], en temps de jeu. */
    private static final Map<UUID, long[]> READY_AT = new HashMap<>();

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        GLIDING.remove(id);
        GLIDE_TICKS.remove(id);
        READY_AT.remove(id);
    }

    /** La pièce portée à cet emplacement, si elle porte un mécanisme (et qu'elle est bien à sa place). */
    @Nullable
    public static Mechanism worn(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        Mechanism mechanism = stack.get(FFDataComponents.MECHANISM);
        return mechanism != null && mechanism.slot() == slot && Forging.slotOf(stack) == slot ? mechanism : null;
    }

    // ------------------------------------------------------------------ les touches

    public static void key(ServerPlayer player, EquipmentSlot slot, boolean down) {
        if (!down) {
            if (slot == EquipmentSlot.CHEST) GLIDING.remove(player.getUUID());
            return;
        }
        Mechanism mechanism = worn(player, slot);
        if (mechanism == null || player.isSpectator()) return;
        switch (mechanism) {
            case SONDE -> probe(player);
            case SAC -> PieceStorageMenu.open(player, EquipmentSlot.CHEST, PieceStorageMenu.BACKPACK_SIZE);
            case POCHES -> PieceStorageMenu.open(player, EquipmentSlot.LEGS, PieceStorageMenu.POCKETS_SIZE);
            case PLANEUR -> GLIDING.add(player.getUUID());
            case JET -> jet(player);
            case PISTONS -> dash(player);
            default -> {} // Longue-vue : un zoom côté client ; Lampe à vapeur : étape 8e
        }
    }

    /** Une charge de vapeur, ou un message (« plus de vapeur »). */
    private static boolean steam(ServerPlayer player, EquipmentSlot slot) {
        if (Steam.spend(player.getItemBySlot(slot))) return true;
        player.displayClientMessage(Component.translatable("message.echoes_fireforge.no_steam").withStyle(ChatFormatting.GRAY), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.8F);
        return false;
    }

    private static boolean ready(ServerPlayer player, int index, int cooldown) {
        long[] ready = READY_AT.computeIfAbsent(player.getUUID(), id -> new long[2]);
        long now = player.serverLevel().getGameTime();
        if (now < ready[index]) return false;
        ready[index] = now + cooldown;
        return true;
    }

    /** Sonde de prospection : les Attiseurs cherchaient le métal dans la roche. */
    private static void probe(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (player.getCooldowns().isOnCooldown(helmet.getItem())) return;
        List<BlockPos> found = scan(player.serverLevel(), player.blockPosition(), PROBE_RADIUS);
        PacketDistributor.sendToPlayer(player, new ProbeResultPayload(found));
        player.getCooldowns().addCooldown(helmet.getItem(), PROBE_COOLDOWN);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /** Public pour les tests : les cibles de la Sonde dans ce rayon, les plus proches d'abord (512 au plus). */
    public static List<BlockPos> scan(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) <= radius * radius && level.getBlockState(pos).is(PROBE_TARGETS)) found.add(pos.immutable());
        }
        found.sort((a, b) -> Double.compare(a.distSqr(center), b.distSqr(center)));
        return found.size() > 512 ? found.subList(0, 512) : found;
    }

    /** Jet de vapeur : les soupapes de décharge. */
    private static void jet(ServerPlayer player) {
        if (!ready(player, 0, JET_COOLDOWN) || !steam(player, EquipmentSlot.CHEST)) return;
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(JET_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !e.isSpectator()
                && e.distanceToSqr(player) <= JET_RADIUS * JET_RADIUS)) {
            Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) away = player.getLookAngle().multiply(1, 0, 1);
            away = away.normalize();
            target.knockback(JET_PUSH, -away.x, -away.z);
            target.hurtMarked = true;
        }
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 0, Math.cos(angle), 0.05, Math.sin(angle), 0.35);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /** Pistons de genoux : les pistons des machines. */
    private static void dash(ServerPlayer player) {
        if (!ready(player, 1, DASH_COOLDOWN) || !steam(player, EquipmentSlot.LEGS)) return;
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0E-4) return;
        look = look.normalize().scale(DASH_SPEED);
        player.setDeltaMovement(look.x, Math.max(player.getDeltaMovement().y, 0.2), look.z);
        player.hurtMarked = true; // le mouvement du joueur est calculé par son client : on le lui envoie
        player.fallDistance = 0;
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.4, player.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    /** Public pour les tests : le Planeur est-il tenu ? */
    public static boolean gliding(Player player) {
        return GLIDING.contains(player.getUUID());
    }

    // ------------------------------------------------------------------ chaque tick

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    /** Public pour les tests : le travail d'un tick pour ce joueur. */
    public static void tick(ServerPlayer player) {
        // Planeur : tant que la touche est tenue en l'air (le vol lui-même est calculé par le client, voir MechanismKeys)
        UUID id = player.getUUID();
        if (GLIDING.contains(id)) {
            boolean airborne = !player.onGround() && !player.getAbilities().flying && !player.isInWater() && !player.isFallFlying();
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (worn(player, EquipmentSlot.CHEST) != Mechanism.PLANEUR || Steam.charges(chest) == 0) {
                GLIDING.remove(id);
            } else if (airborne) {
                player.fallDistance = 0;
                int ticks = GLIDE_TICKS.merge(id, 1, Integer::sum);
                if (ticks % GLIDE_TICKS_PER_CHARGE == 0) Steam.spend(chest);
                if (ticks % 4 == 0) player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 1, 0.15, 0, 0.15, 0.01);
            }
        }

        // Patins : plus vite sur la pierre
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean skating = worn(player, EquipmentSlot.FEET) == Mechanism.PATINS && player.onGround()
                    && player.level().getBlockState(player.getBlockPosBelowThatAffectsMyMovement()).is(SKATE_SURFACES);
            if (skating && !speed.hasModifier(SKATE_ID)) {
                speed.addTransientModifier(new AttributeModifier(SKATE_ID, SKATE_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!skating && speed.hasModifier(SKATE_ID)) {
                speed.removeModifier(SKATE_ID);
            }
        }
    }

    // ------------------------------------------------------------------ les bottes et l'Ancre

    /** Ressorts et Crampons : de vrais attributs de la pièce (ils apparaissent dans son infobulle). */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Mechanism mechanism = stack.get(FFDataComponents.MECHANISM);
        if (mechanism == null || Forging.slotOf(stack) != EquipmentSlot.FEET) return;
        if (mechanism == Mechanism.RESSORTS) {
            event.addModifier(Attributes.JUMP_STRENGTH, new AttributeModifier(SPRING_JUMP_ID, SPRING_JUMP, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.FEET);
            event.addModifier(Attributes.FALL_DAMAGE_MULTIPLIER, new AttributeModifier(SPRING_FALL_ID, SPRING_FALL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.FEET);
        } else if (mechanism == Mechanism.CRAMPONS) {
            event.addModifier(Attributes.STEP_HEIGHT, new AttributeModifier(CRAMPON_STEP_ID, CRAMPON_STEP, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
            event.addModifier(Attributes.MOVEMENT_EFFICIENCY, new AttributeModifier(CRAMPON_GRIP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
        }
    }

    /** Ancre : accroupi, aucun recul (tenir debout devant le souffle des fourneaux). */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.isCrouching() && worn(entity, EquipmentSlot.LEGS) == Mechanism.ANCRE) event.setCanceled(true);
    }
}
