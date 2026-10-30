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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Locale;

/**
 * La progression du set du Wither, chaque seconde, côté serveur : elle monte avec les 4 pièces, redescend d'autant
 * plus vite qu'il en manque ; une fois au palier 4, plus de retour. Au passage d'un palier vers le haut, le porteur
 * tousse de la cendre et une courte phrase passe dans la barre d'action, sans rien expliquer.
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class WitherSetEvents {

    private WitherSetEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0 || !player.isAlive()) return;
        if (player.isCreative() || player.isSpectator()) {
            showStage(player, WitherSet.stage(player)); // le temps ne compte pas, mais l'armure garde son aspect
            return;
        }
        WitherProgress before = WitherSet.progress(player);
        int pieces = WitherSet.piecesWorn(player);
        WitherProgress after = advance(before, pieces);
        if (!after.equals(before)) player.setData(FFAttachments.WITHER_PROGRESS, after);
        if (after.stage() > before.stage()) stageReached(player, after.stage());
        showStage(player, after.stage());
    }

    /** Une seconde de plus avec ce nombre de pièces portées. */
    public static WitherProgress advance(WitherProgress progress, int pieces) {
        if (progress.locked()) return progress; // le palier 4 : plus de retour
        double percent = pieces >= WitherSet.SLOTS.length
                ? progress.percent() + WitherSet.RISE
                : progress.percent() - WitherSet.FALL[pieces];
        percent = Math.max(0, Math.min(100, percent));
        return new WitherProgress(percent, percent >= 100);
    }

    /** Les pièces portées prennent l'aspect du palier du porteur (les autres joueurs le voient). */
    public static void showStage(ServerPlayer player, int stage) {
        for (EquipmentSlot slot : WitherSet.SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (WitherSet.isPiece(stack) && WitherArmorItem.stage(stack) != stage) stack.set(FFDataComponents.WITHER_STAGE, stage);
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

    /** Pour les essais (opérateurs) : /witherset get|set <joueur> [pour cent]. */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("witherset").requires(source -> source.hasPermission(2))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    WitherProgress progress = WitherSet.progress(player);
                    context.getSource().sendSuccess(() -> Component.translatable("commands.echoes_fireforge.witherset.get", player.getDisplayName(),
                            String.format(Locale.ROOT, "%.1f", progress.percent()), progress.stage()), false);
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
                            showStage(player, progress.stage());
                            context.getSource().sendSuccess(() -> Component.translatable("commands.echoes_fireforge.witherset.set", player.getDisplayName(),
                                    String.format(Locale.ROOT, "%.1f", percent), progress.stage()), true);
                            return progress.stage();
                        })))));
    }
}
