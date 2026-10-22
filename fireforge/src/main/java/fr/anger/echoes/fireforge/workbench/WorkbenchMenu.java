package fr.anger.echoes.fireforge.workbench;

import fr.anger.echoes.fireforge.mechanism.Grafting;
import fr.anger.echoes.fireforge.mechanism.Mechanism;
import fr.anger.echoes.fireforge.mechanism.MechanismTemplateItem;
import fr.anger.echoes.fireforge.registry.FFBlocks;
import fr.anger.echoes.fireforge.registry.FFItems;
import fr.anger.echoes.fireforge.registry.FFMenus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * L'interface de l'établi : la pièce, le gabarit, un engrenage, la clé, et le résultat. Un coup de clé greffe
 * le mécanisme (le gabarit et l'engrenage sont consommés) et pose la pièce dans la case du résultat.
 */
public class WorkbenchMenu extends AbstractContainerMenu {

    /** Bouton de la clé (envoyé au serveur par le clic). */
    public static final int BUTTON_GRAFT = 0;

    public static final int SLOT_PIECE = 0, SLOT_TEMPLATE = 1, SLOT_GEAR = 2, SLOT_RESULT = 3;
    // Positions des cases (coin haut gauche de l'objet), en accord avec la texture
    public static final int PIECE_X = 27, TEMPLATE_X = 51, GEAR_X = 75, RESULT_X = 133, ROW_Y = 35;
    public static final int INV_X = 8, INV_Y = 84;

    private final ContainerLevelAccess access;
    private final Container inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            WorkbenchMenu.this.slotsChanged(this);
        }
    };
    private final Container output = new SimpleContainer(1);

    /** Côté client. */
    public WorkbenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public WorkbenchMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(FFMenus.WORKBENCH.get(), containerId);
        this.access = access;
        addSlot(new Slot(inputs, SLOT_PIECE, PIECE_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isPiece(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(inputs, SLOT_TEMPLATE, TEMPLATE_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof MechanismTemplateItem;
            }
        });
        addSlot(new Slot(inputs, SLOT_GEAR, GEAR_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(FFItems.MACHINE_GEAR.get());
            }
        });
        addSlot(new Slot(output, 0, RESULT_X, ROW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // on ne fait qu'y reprendre
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, INV_X + col * 18, INV_Y + 58));
    }

    /** Une arme ou une armure à laquelle au moins un mécanisme peut aller. */
    public static boolean isPiece(ItemStack stack) {
        for (Mechanism mechanism : Mechanism.values()) if (mechanism.fits(stack)) return true;
        return false;
    }

    public Grafting.Status status() {
        return Grafting.status(inputs.getItem(SLOT_PIECE), inputs.getItem(SLOT_TEMPLATE), inputs.getItem(SLOT_GEAR), output.getItem(0));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_GRAFT || status() != Grafting.Status.READY) return false;
        Mechanism mechanism = Grafting.mechanismOf(inputs.getItem(SLOT_TEMPLATE));
        output.setItem(0, Grafting.graft(inputs.getItem(SLOT_PIECE), mechanism));
        inputs.setItem(SLOT_PIECE, ItemStack.EMPTY);
        inputs.removeItem(SLOT_TEMPLATE, 1);
        inputs.removeItem(SLOT_GEAR, 1);
        access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.01);
            }
        });
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = SLOT_RESULT + 1, playerEnd = slots.size();
        if (index <= SLOT_RESULT) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof MechanismTemplateItem) {
            if (!moveItemStackTo(stack, SLOT_TEMPLATE, SLOT_TEMPLATE + 1, false)) return ItemStack.EMPTY;
        } else if (stack.is(FFItems.MACHINE_GEAR.get())) {
            if (!moveItemStackTo(stack, SLOT_GEAR, SLOT_GEAR + 1, false)) return ItemStack.EMPTY;
        } else if (isPiece(stack)) {
            if (!moveItemStackTo(stack, SLOT_PIECE, SLOT_PIECE + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, FFBlocks.WORKBENCH.get());
    }

    /** Rien ne reste dans l'établi : tout revient au joueur à la fermeture. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> {
            clearContainer(player, inputs);
            clearContainer(player, output);
        });
    }
}
