package fr.anger.echoes.fireforge.mechanism;

import fr.anger.echoes.fireforge.registry.FFDataComponents;
import fr.anger.echoes.fireforge.registry.FFMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Le Sac dorsal (9 cases, touche B) et les Poches (6 cases, touche N) : les objets restent dans la pièce
 * (composant {@code minecraft:container}) ; si on perd la pièce, on perd ce qu'elle porte. Ni shulker, ni sac,
 * ni rien qui contienne déjà des objets à l'intérieur.
 */
public class PieceStorageMenu extends AbstractContainerMenu {

    public static final int BACKPACK_SIZE = 9, POCKETS_SIZE = 6;
    public static final int ROW_Y = 20, INV_Y = 52;

    private final EquipmentSlot slot;
    private final int size;
    private final Player player;
    /** La pièce ouverte : si elle change (retirée, échangée), l'interface se ferme. */
    private final ItemStack piece;
    private final SimpleContainer storage;

    /** Côté client. */
    public PieceStorageMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, EquipmentSlot.values()[buf.readVarInt()], buf.readVarInt());
    }

    public PieceStorageMenu(int containerId, Inventory inventory, EquipmentSlot slot, int size) {
        super(FFMenus.PIECE_STORAGE.get(), containerId);
        this.slot = slot;
        this.size = size;
        this.player = inventory.player;
        this.piece = player.getItemBySlot(slot);
        this.storage = new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                save();
            }
        };
        if (!player.level().isClientSide) {
            NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
            piece.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < size; i++) storage.setItem(i, items.get(i));
        }
        int left = (176 - size * 18) / 2;
        for (int i = 0; i < size; i++) {
            addSlot(new Slot(storage, i, left + i * 18 + 1, ROW_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return allowed(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
    }

    public int size() {
        return size;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public static void open(ServerPlayer player, EquipmentSlot slot, int size) {
        Component title = Component.translatable(slot == EquipmentSlot.CHEST ? "container.echoes_fireforge.backpack" : "container.echoes_fireforge.pockets");
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PieceStorageMenu(id, inventory, slot, size), title), buf -> {
            buf.writeVarInt(slot.ordinal());
            buf.writeVarInt(size);
        });
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    /** Ni shulker, ni sac, ni poches, ni rien qui contienne déjà des objets. */
    public static boolean allowed(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock) return false;
        if (stack.has(DataComponents.CONTAINER) || stack.has(DataComponents.BUNDLE_CONTENTS)) return false;
        Mechanism mechanism = stack.get(FFDataComponents.MECHANISM);
        return mechanism != Mechanism.SAC && mechanism != Mechanism.POCHES;
    }

    /** Le contenu retourne dans la pièce portée, à chaque changement (côté serveur). */
    private void save() {
        if (player.level().isClientSide || player.getItemBySlot(slot) != piece) return;
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) items.set(i, storage.getItem(i));
        piece.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true; // le serveur décide (côté client, la pièce est recopiée à chaque mise à jour)
        return player.getItemBySlot(slot) == piece && !piece.isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot from = slots.get(index);
        if (!from.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = from.getItem();
        ItemStack copy = stack.copy();
        if (index < size) {
            if (!moveItemStackTo(stack, size, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!allowed(stack) || !moveItemStackTo(stack, 0, size, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) from.setByPlayer(ItemStack.EMPTY);
        else from.setChanged();
        return copy;
    }
}
