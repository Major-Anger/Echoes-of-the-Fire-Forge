package fr.anger.echoes.fireforge.machine;

import fr.anger.echoes.fireforge.structure.GrandMachineStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Le Manomètre d'Attiseur (plan, 15d) : toutes les jauges des Attiseurs étaient reliées à la Grande Machine. Dans le
 * Nether, son aiguille la montre, comme une boussole son aimant ; ailleurs, elle tremble et tourne sans s'arrêter.
 * La cible est gardée sur l'objet (le composant de suivi des boussoles, sans aimant à vérifier) ; le client la lit.
 */
public class StokerGaugeItem extends Item {

    public StokerGaugeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || level.dimension() != Level.NETHER || level.getGameTime() % 40 != 0) return;
        if (stack.has(DataComponents.LODESTONE_TRACKER)) return;
        GrandMachineStructure.locate(server).ifPresent(at ->
                stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.NETHER, at)), false)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.echoes_fireforge.stoker_gauge").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
