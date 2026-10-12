package fr.anger.echoes.fireforge.forge;

import fr.anger.echoes.fireforge.registry.FFMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static fr.anger.echoes.fireforge.forge.FireForgeBlockEntity.*;

public class FireForgeMenu extends AbstractContainerMenu {

    /** Bouton du marteau (envoyé au serveur par le clic). */
    public static final int BUTTON_FORGE = 0;
    /** Bouton du soufflet (rattrape une fêlure). */
    public static final int BUTTON_BELLOWS = 1;

    // Positions des cases dans l'interface (coin haut gauche de l'objet)
    public static final int WATER_X = 23, WATER_Y = 19, EMPTIES_X = 23, EMPTIES_Y = 53, FUEL_X = 61, FUEL_Y = 36;
    public static final int ITEM_X = 101, ITEM_Y = 27, OFFERING_X = 101, OFFERING_Y = 58;
    public static final int INV_X = 20, INV_Y = 109;

    @Nullable
    private final FireForgeBlockEntity forge;
    private final ContainerData data;

    /** Côté client : les cases et les données arrivent du serveur. */
    public FireForgeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, clientForge(playerInventory, buf.readBlockPos()), new SimpleContainerData(DATA_COUNT));
    }

    @Nullable
    private static FireForgeBlockEntity clientForge(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof FireForgeBlockEntity forge ? forge : null;
    }

    public FireForgeMenu(int containerId, Inventory playerInventory, @Nullable FireForgeBlockEntity forge, ContainerData data) {
        super(FFMenus.FIRE_FORGE.get(), containerId);
        this.forge = forge;
        this.data = data;
        IItemHandler items = forge != null ? forge.inventory() : new ItemStackHandler(SLOTS);

        // L'objet : bloqué pendant la reforge, une seule pièce
        addSlot(new Rule(items, SLOT_ITEM, ITEM_X, ITEM_Y, ForgeRules::isReforgeable) {
            @Override
            public boolean mayPickup(Player player) {
                return !isWorking();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Rule(items, SLOT_OFFERING, OFFERING_X, OFFERING_Y, s -> ForgeRules.offeringTier(s) > 0));
        addSlot(new Rule(items, SLOT_FUEL, FUEL_X, FUEL_Y, ForgeRules::isFuel));
        addSlot(new Rule(items, SLOT_WATER, WATER_X, WATER_Y, s -> ForgeRules.waterAmount(s) > 0));
        addSlot(new Rule(items, SLOT_EMPTIES, EMPTIES_X, EMPTIES_Y, s -> false)); // on ne fait qu'y reprendre

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, INV_X + col * 18, INV_Y + 58));

        addDataSlots(data);
    }

    private static class Rule extends SlotItemHandler {
        private final Predicate<ItemStack> valid;

        Rule(IItemHandler handler, int index, int x, int y, Predicate<ItemStack> valid) {
            super(handler, index, x, y);
            this.valid = valid;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return valid.test(stack);
        }
    }

    // ------------------------------------------------------------------ données pour l'écran

    public int water() { return data.get(0); }
    public int waterCapacity() { return Math.max(1, data.get(1)); }
    public Heat heat() { return Heat.byLevel(data.get(2)); }
    public int progress() { return data.get(3); }
    public Status status() { return Status.values()[Math.max(0, Math.min(Status.values().length - 1, data.get(4)))]; }
    public boolean isWorking() { return status().isWorking(); }

    /** L'offrande posée (pour afficher les chances côté client). */
    public ItemStack offering() { return getSlot(SLOT_OFFERING).getItem(); }

    /** L'objet sur l'enclume (pour annoncer le risque de fêlure côté client). */
    public ItemStack item() { return getSlot(SLOT_ITEM).getItem(); }

    // ------------------------------------------------------------------ actions

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (forge == null) return false;
        if (id == BUTTON_FORGE) return forge.tryStart(player);
        if (id == BUTTON_BELLOWS) {
            forge.blowBellows(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return forge != null && !forge.isRemoved()
                && player.distanceToSqr(forge.getBlockPos().getCenter()) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        net.minecraft.world.inventory.Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS) {
            // de la forge vers l'inventaire
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // de l'inventaire vers la bonne case de la forge
            int target = ForgeRules.isReforgeable(stack) ? SLOT_ITEM
                    : ForgeRules.offeringTier(stack) > 0 ? SLOT_OFFERING
                    : ForgeRules.isFuel(stack) ? SLOT_FUEL
                    : ForgeRules.waterAmount(stack) > 0 ? SLOT_WATER : -1;
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
