package fr.anger.echoes.fireforge.forgemaster;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFItems;
import fr.anger.echoes.fireforge.registry.FFSounds;
import fr.anger.echoes.fireforge.workbench.Steam;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Le marteau-pilon du maître de forge (plan, 14c), le modèle d'Anger : une arme lourde (10 de dégâts, 0,8 coup par
 * seconde), reforgeable à la Forge comme une masse. Clic droit : le coup à la vapeur, une onde de choc au sol autour
 * de soi, comme celle du maître ; il coûte 6 charges de vapeur (rechargées au bouilleur de l'établi), 8 s d'attente.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public class SteamHammerItem extends Item {

    public static final float DAMAGE = 10, SPEED = 0.8F;
    public static final int DURABILITY = 1000;
    /** Le coup à la vapeur : 6 charges, 8 s ; l'onde court au sol jusqu'à 5 blocs, 80 % des dégâts de l'arme. */
    public static final int STEAM_COST = 6, COOLDOWN = 160;
    public static final double WAVE_RADIUS = 5, WAVE_STEP = 0.5;
    public static final float WAVE_PART = 0.8F, WAVE_PUSH = 0.6F;

    /** Les ondes en route : un demi-bloc par tick. */
    private record Wave(ServerPlayer player, Vec3 center, float damage, Set<UUID> hit, double[] radius) {}
    private static final List<Wave> WAVES = new ArrayList<>();

    public SteamHammerItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, DAMAGE - 1, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, SPEED - 4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    /** On le répare avec de la Cendre d'Attiseur, comme le set du Wither. */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(FFItems.STOKER_ASH.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (Steam.charges(stack) < STEAM_COST) {
            if (player instanceof ServerPlayer server) {
                server.displayClientMessage(Component.translatable("message.echoes_fireforge.steam_hammer.empty").withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.4F);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer server) steamStrike(server, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Public pour les tests : le coup à la vapeur (la vapeur doit suffire, l'attente est posée ici). */
    public static void steamStrike(ServerPlayer player, ItemStack stack) {
        for (int i = 0; i < STEAM_COST; i++) Steam.spend(stack, player);       // une à une (l'Écho de la Grande Machine compte)
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN);
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * WAVE_PART;
        WAVES.add(new Wave(player, player.position(), damage, new HashSet<>(), new double[]{0}));
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), FFSounds.FORGE_MASTER_GROUND_HIT.get(), SoundSource.PLAYERS, 1.6F, 1.1F);
        level.playSound(null, player.blockPosition(), FFSounds.FORGE_MASTER_STEAM.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3, player.getZ(), 16, 0.4, 0.1, 0.4, 0.08);
        stack.hurtAndBreak(2, player, EquipmentSlot.MAINHAND);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        runWaves();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WAVES.removeIf(w -> w.player() == event.getEntity());
    }

    /** Public pour les tests : chaque onde avance d'un demi-bloc. */
    public static void runWaves() {
        Iterator<Wave> it = WAVES.iterator();
        while (it.hasNext()) {
            Wave w = it.next();
            double r = w.radius()[0] += WAVE_STEP;
            if (r > WAVE_RADIUS || !w.player().isAlive()) {
                it.remove();
                continue;
            }
            ServerLevel level = w.player().serverLevel();
            Vec3 c = w.center();
            BlockState below = level.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
            int points = (int) (r * 5);
            for (int i = 0; i < points; i++) {
                double a = i * Mth.TWO_PI / points;
                double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
                level.sendParticles(ParticleTypes.CLOUD, x, c.y + 0.1, z, 1, 0.05, 0.02, 0.05, 0.01);
                if (!below.isAir() && i % 3 == 0) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), x, c.y + 0.1, z, 1, 0.1, 0.05, 0.1, 0.1);
            }
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, w.player().getBoundingBox().move(c.subtract(w.player().position()))
                    .inflate(WAVE_RADIUS + 1, 2, WAVE_RADIUS + 1))) {
                if (victim == w.player() || !victim.isAlive() || victim.isSpectator() || w.hit().contains(victim.getUUID())) continue;
                if (victim instanceof OwnableEntity pet && w.player().getUUID().equals(pet.getOwnerUUID())) continue;
                double d = Math.sqrt(Mth.square(victim.getX() - c.x) + Mth.square(victim.getZ() - c.z));
                // comme l'onde du maître : au sol seulement, qui saute au bon moment n'est pas touché
                if (Math.abs(d - r) > 0.75 || Math.abs(victim.getY() - c.y) > 1.5 || !victim.onGround()) continue;
                w.hit().add(victim.getUUID());
                // un coup de vapeur, pas un coup d'arme : il ne relance pas les Spéciaux
                DamageSource source = w.player().damageSources().mobAttack(w.player());
                if (victim.hurt(source, w.damage())) {
                    Vec3 away = new Vec3(victim.getX() - c.x, 0, victim.getZ() - c.z);
                    if (away.lengthSqr() > 1.0E-4) {
                        away = away.normalize();
                        victim.knockback(WAVE_PUSH, -away.x, -away.z);
                        victim.push(0, 0.3, 0);
                        victim.hurtMarked = true;
                    }
                    victim.playSound(FFSounds.FORGE_MASTER_HIT.get(), 0.8F, 1.1F);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.steam_hammer").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.echoes_fireforge.steam",
                Steam.charges(stack), Steam.capacity(stack))).withStyle(ChatFormatting.WHITE));
    }
}
