package fr.anger.echoes.fireforge.plan;

import fr.anger.echoes.core.client.ClientLore;
import fr.anger.echoes.core.lore.Lore;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Un plan des Attiseurs, gravé sur une fine feuille de laiton (le papier n'aurait pas survécu au Nether).
 * Clic droit : le joueur le déchiffre, ce qui l'enregistre dans son lore. Le plan n'est pas consommé.
 */
public class PlanItem extends Item {

    /** Le plan d'un bloc (la Forge, l'Établi) ; null : un Plan d'Attiseur numéroté. */
    @Nullable
    private final ResourceLocation lore;

    public PlanItem(@Nullable ResourceLocation lore, Properties properties) {
        super(properties);
        this.lore = lore;
    }

    /** L'entrée de lore que ce plan enregistre. */
    public ResourceLocation loreEntry(ItemStack stack) {
        return lore != null ? lore : Plans.attiseur(number(stack));
    }

    private static int number(ItemStack stack) {
        return stack.getOrDefault(FFDataComponents.PLAN_NUMBER, 1);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (lore != null) return super.getName(stack);
        return Component.translatable("item.echoes_fireforge.attiseur_plan.numbered", Plans.roman(number(stack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean firstTime = Lore.markRead(serverPlayer, loreEntry(stack));
            serverPlayer.displayClientMessage(Component.translatable(
                    firstTime ? "message.echoes_fireforge.plan.read" : "message.echoes_fireforge.plan.known", getName(stack))
                    .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, player.blockPosition(), firstTime ? SoundEvents.BOOK_PAGE_TURN : SoundEvents.UI_LOOM_TAKE_RESULT,
                    SoundSource.PLAYERS, 1.0F, firstTime ? 0.8F : 1.2F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.plan").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        // ClientLore ne dépend d'aucune classe client : sans danger côté serveur (il y restera simplement vide)
        boolean read = ClientLore.hasRead(loreEntry(stack));
        tooltip.add(Component.translatable(read ? "tooltip.echoes_fireforge.plan.read" : "tooltip.echoes_fireforge.plan.unread")
                .withStyle(read ? ChatFormatting.DARK_GREEN : ChatFormatting.GOLD));
    }
}
