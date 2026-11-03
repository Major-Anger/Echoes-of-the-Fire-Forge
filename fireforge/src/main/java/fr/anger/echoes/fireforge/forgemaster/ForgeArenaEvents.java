package fr.anger.echoes.fireforge.forgemaster;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Rallumer la forge froide du maître : 8 Cendres d'Attiseur sur son âtre (clic droit). */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class ForgeArenaEvents {

    private ForgeArenaEvents() {}

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(FFItems.STOKER_ASH.get()) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos clicked = event.getPos();
        // la forge du maître est cachée juste sous son âtre
        for (int dx = -2; dx <= 2; dx++) {
            if (level.getBlockEntity(clicked.offset(dx, -1, 0)) instanceof MasterForgeBlockEntity forge && forge.isHearth(clicked)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (forge.isLit()) return;
                if (stack.getCount() < MasterForgeBlockEntity.RELIGHT_ASH && !event.getEntity().isCreative()) {
                    event.getEntity().displayClientMessage(Component.translatable("message.echoes_fireforge.master_forge.ash",
                            MasterForgeBlockEntity.RELIGHT_ASH), true);
                    return;
                }
                if (forge.relight(level) && !event.getEntity().isCreative()) stack.shrink(MasterForgeBlockEntity.RELIGHT_ASH);
                return;
            }
        }
    }
}
