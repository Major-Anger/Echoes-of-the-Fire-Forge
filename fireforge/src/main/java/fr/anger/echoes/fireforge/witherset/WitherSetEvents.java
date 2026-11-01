package fr.anger.echoes.fireforge.witherset;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFAttachments;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import fr.anger.echoes.fireforge.registry.FFSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Locale;

/**
 * La progression du set du Wither, chaque seconde, côté serveur : elle monte avec les 4 pièces, redescend d'autant
 * plus vite qu'il en manque ; une fois au palier 4, plus de retour. Au passage d'un palier vers le haut, le porteur
 * tousse de la cendre et une courte phrase passe dans la barre d'action, sans rien expliquer.
 * <p>
 * Au palier 4 (étape 13c), le set colle à la peau (Lien de cendre), la cendre autour du porteur s'épaissit, et au bout
 * de 30 minutes de jeu, la cendre l'emporte. À sa mort, quelle qu'en soit la cause, son double se lève avec le set.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class WitherSetEvents {

    private WitherSetEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0 || !player.isAlive()) return;
        WitherProgress before = WitherSet.progress(player);
        if (player.isCreative() || player.isSpectator()) {
            show(player, before); // le temps ne compte pas, mais l'armure garde son aspect
            return;
        }
        WitherProgress after = advance(before, WitherSet.piecesWorn(player));
        if (!after.equals(before)) player.setData(FFAttachments.WITHER_PROGRESS, after);
        if (after.stage() > before.stage()) stageReached(player, after.stage());
        show(player, after);
        if (after.locked() && after.doom() >= WitherSet.DOOM_TICKS) takenByAsh(player);
    }

    /** Une seconde de plus avec ce nombre de pièces portées. */
    public static WitherProgress advance(WitherProgress progress, int pieces) {
        if (progress.locked()) return new WitherProgress(100, true, progress.doom() + 20); // le palier 4 : plus de retour
        double percent = pieces >= WitherSet.SLOTS.length
                ? progress.percent() + WitherSet.RISE
                : progress.percent() - WitherSet.FALL[pieces];
        percent = Math.max(0, Math.min(100, percent));
        return new WitherProgress(percent, percent >= 100);
    }

    /**
     * Ce qui se voit sur les pièces portées : l'aspect du palier ; au palier 4, le Lien de cendre (elles ne s'enlèvent
     * plus) et les minutes écoulées (la cendre s'épaissit autour du porteur, côté client).
     */
    public static void show(ServerPlayer player, WitherProgress progress) {
        int stage = progress.stage();
        for (EquipmentSlot slot : WitherSet.SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!WitherSet.isPiece(stack)) continue;
            if (WitherArmorItem.stage(stack) != stage) stack.set(FFDataComponents.WITHER_STAGE, stage);
            WitherSet.setBound(player.level(), stack, progress.locked());
            if (progress.locked()) {
                if (stack.getOrDefault(FFDataComponents.WITHER_DOOM, -1) != progress.doomMinutes()) stack.set(FFDataComponents.WITHER_DOOM, progress.doomMinutes());
            } else if (stack.has(FFDataComponents.WITHER_DOOM)) {
                stack.remove(FFDataComponents.WITHER_DOOM);
            }
        }
    }

    /** Un palier atteint : une toux de cendre, une phrase. */
    public static void stageReached(ServerPlayer player, int stage) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), FFSounds.CENDREUX_COUGH.get(), SoundSource.PLAYERS, 0.9F, 1.1F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 mouth = player.getEyePosition().add(look.scale(0.4)).subtract(0, 0.15, 0);
        player.serverLevel().sendParticles(ParticleTypes.WHITE_ASH, mouth.x, mouth.y, mouth.z, 12, 0.1, 0.05, 0.1, 0.02);
        player.serverLevel().sendParticles(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 3, 0.05, 0.05, 0.05, 0.01);
        player.displayClientMessage(Component.translatable("message.echoes_fireforge.wither_stage." + stage)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    /** Les 30 minutes sont passées : la cendre l'emporte. Ni l'armure, ni les effets, ni un totem n'y changent rien. */
    public static void takenByAsh(ServerPlayer player) {
        var type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(WitherSet.ASH);
        player.hurt(new DamageSource(type), Float.MAX_VALUE);
    }

    /**
     * La mort au palier 4, de la cendre ou d'autre chose (Anger) : la transformation s'achève. Le double se lève avec le
     * set, et la progression du joueur repart de zéro. En dernier, pour qu'un autre mod puisse encore empêcher la mort.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !WitherSet.progress(player).locked()) return;
        AshDoubleEntity.rise(player);
        player.setData(FFAttachments.WITHER_PROGRESS, WitherProgress.NONE);
    }

    /** Pour les essais (opérateurs) : /witherset get|set <joueur> [pour cent] ; /witherset doom <joueur> <minutes>. */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("witherset").requires(source -> source.hasPermission(2))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    WitherProgress progress = WitherSet.progress(player);
                    context.getSource().sendSuccess(() -> Component.translatable("commands.echoes_fireforge.witherset.get", player.getDisplayName(),
                            String.format(Locale.ROOT, "%.1f", progress.percent()), progress.stage(),
                            String.format(Locale.ROOT, "%.1f", progress.doom() / 1200.0)), false);
                    return progress.stage();
                })))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 100)).executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            double percent = DoubleArgumentType.getDouble(context, "percent");
                            int before = WitherSet.stage(player);
                            WitherProgress progress = new WitherProgress(percent, percent >= 100);
                            player.setData(FFAttachments.WITHER_PROGRESS, progress);
                            if (progress.stage() > before) stageReached(player, progress.stage());
                            show(player, progress);
                            context.getSource().sendSuccess(() -> Component.translatable("commands.echoes_fireforge.witherset.set", player.getDisplayName(),
                                    String.format(Locale.ROOT, "%.1f", percent), progress.stage()), true);
                            return progress.stage();
                        }))))
                .then(Commands.literal("doom").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("minutes", DoubleArgumentType.doubleArg(0, WitherSet.DOOM_MINUTES)).executes(context -> {
                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            double minutes = DoubleArgumentType.getDouble(context, "minutes");
                            int before = WitherSet.stage(player);
                            WitherProgress progress = new WitherProgress(100, true, (int) Math.round(minutes * 1200));
                            player.setData(FFAttachments.WITHER_PROGRESS, progress);
                            if (progress.stage() > before) stageReached(player, progress.stage());
                            show(player, progress);
                            context.getSource().sendSuccess(() -> Component.translatable("commands.echoes_fireforge.witherset.doom", player.getDisplayName(),
                                    String.format(Locale.ROOT, "%.1f", minutes)), true);
                            return progress.doomMinutes();
                        })))));
    }
}
