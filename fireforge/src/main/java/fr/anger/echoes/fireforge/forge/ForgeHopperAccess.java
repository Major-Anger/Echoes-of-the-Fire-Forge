package fr.anger.echoes.fireforge.forge;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Ce que les entonnoirs (et tuyaux d'autres mods) peuvent faire, selon le côté :
 *  - dessus : déposer du combustible ;
 *  - côtés : déposer de l'eau (seaux, fioles) ;
 *  - dessous : reprendre les récipients vides.
 * L'objet à reforger et l'offrande restent à la main uniquement.
 */
public final class ForgeHopperAccess implements IItemHandler {

    private final IItemHandler inventory;
    private final int slot;
    private final boolean canInsert;

    private ForgeHopperAccess(IItemHandler inventory, int slot, boolean canInsert) {
        this.inventory = inventory;
        this.slot = slot;
        this.canInsert = canInsert;
    }

    @Nullable
    public static IItemHandler forSide(FireForgeBlockEntity forge, @Nullable Direction side) {
        if (side == null) return null;
        return switch (side) {
            case UP -> new ForgeHopperAccess(forge.inventory(), FireForgeBlockEntity.SLOT_FUEL, true);
            case DOWN -> new ForgeHopperAccess(forge.inventory(), FireForgeBlockEntity.SLOT_EMPTIES, false);
            default -> new ForgeHopperAccess(forge.inventory(), FireForgeBlockEntity.SLOT_WATER, true);
        };
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        if (!canInsert || !inventory.isItemValid(slot, stack)) return stack;
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int index, int amount, boolean simulate) {
        return canInsert ? ItemStack.EMPTY : inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int index) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        return canInsert && inventory.isItemValid(slot, stack);
    }
}
