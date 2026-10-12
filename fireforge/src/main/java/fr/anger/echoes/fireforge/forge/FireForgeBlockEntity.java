package fr.anger.echoes.fireforge.forge;

import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Le cœur de la forge (porté par la moitié gauche) : réserves d'eau et de lave, cases, reforge de 10 secondes.
 * À l'étape 2, la reforge consomme tout mais ne tire pas encore de rareté (étape 3).
 */
public class FireForgeBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_ITEM = 0, SLOT_OFFERING = 1, SLOT_FUEL = 2, SLOT_WATER = 3, SLOT_EMPTIES = 4, SLOTS = 5;

    /** Ce que l'interface affiche (et pourquoi la forge ne peut pas démarrer). */
    public enum Status { READY, WORKING, NO_ITEM, NO_OFFERING, NO_WATER, NO_FUEL, EMPTIES_FULL }

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_ITEM -> ForgeRules.isReforgeable(stack);
                case SLOT_OFFERING -> ForgeRules.offeringTier(stack) > 0;
                case SLOT_FUEL -> ForgeRules.isFuel(stack);
                case SLOT_WATER -> ForgeRules.waterAmount(stack) > 0;
                default -> true; // les récipients vides : seule la forge y dépose (voir le menu)
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_ITEM ? 1 : super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == SLOT_ITEM) sync();
        }
    };

    private int water;
    private int lavaCharges;
    private boolean working;
    private int progress;
    private Heat workHeat = Heat.NONE;
    private int workOffering;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> water;
                case 1 -> waterCapacity();
                case 2 -> (working ? workHeat : nextHeat()).level();
                case 3 -> progress;
                case 4 -> status().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 5;
        }
    };

    public FireForgeBlockEntity(BlockPos pos, BlockState state) {
        super(FFBlockEntities.FIRE_FORGE.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public boolean isWorking() {
        return working;
    }

    /** Capacité d'eau. La chaudière l'agrandira (étape 5). */
    public int waterCapacity() {
        return ForgeRules.BASE_WATER_CAPACITY;
    }

    /** La chaleur que donnera la prochaine reforge. */
    public Heat nextHeat() {
        return lavaCharges > 0 ? Heat.FOURNAISE : Heat.ofFuel(inventory.getStackInSlot(SLOT_FUEL));
    }

    public Status status() {
        if (working) return Status.WORKING;
        if (inventory.getStackInSlot(SLOT_ITEM).isEmpty()) return Status.NO_ITEM;
        if (inventory.getStackInSlot(SLOT_OFFERING).isEmpty()) return Status.NO_OFFERING;
        if (water < ForgeRules.WATER_PER_REFORGE) return Status.NO_WATER;
        if (nextHeat() == Heat.NONE) return Status.NO_FUEL;
        // un seau de lave rend un seau vide : il faut de la place pour lui
        ItemStack empty = lavaCharges == 0 ? ForgeRules.emptyContainer(inventory.getStackInSlot(SLOT_FUEL)) : ItemStack.EMPTY;
        if (!empty.isEmpty() && !canStoreEmpty(empty)) return Status.EMPTIES_FULL;
        return Status.READY;
    }

    private boolean canStoreEmpty(ItemStack empty) {
        return inventory.insertItem(SLOT_EMPTIES, empty, true).isEmpty();
    }

    // ------------------------------------------------------------------ fonctionnement

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireForgeBlockEntity forge) {
        forge.fillWater();
        if (forge.working && ++forge.progress >= ForgeRules.REFORGE_TICKS) forge.finish();
    }

    /** Verse l'eau des seaux et fioles dans la chaudière, et range le récipient vide. */
    private void fillWater() {
        ItemStack stack = inventory.getStackInSlot(SLOT_WATER);
        int amount = ForgeRules.waterAmount(stack);
        if (amount == 0 || water + amount > waterCapacity()) return;
        ItemStack empty = ForgeRules.emptyContainer(stack);
        if (!canStoreEmpty(empty)) return;
        inventory.extractItem(SLOT_WATER, 1, false);
        inventory.insertItem(SLOT_EMPTIES, empty, false);
        water += amount;
        setChanged();
    }

    /** Appelé par le marteau de l'interface. */
    public boolean tryStart() {
        if (status() != Status.READY || level == null) return false;
        workOffering = ForgeRules.offeringTier(inventory.getStackInSlot(SLOT_OFFERING));
        inventory.extractItem(SLOT_OFFERING, 1, false);
        if (lavaCharges == 0) {
            ItemStack fuel = inventory.getStackInSlot(SLOT_FUEL);
            Heat heat = Heat.ofFuel(fuel);
            ItemStack empty = ForgeRules.emptyContainer(fuel);
            inventory.extractItem(SLOT_FUEL, 1, false);
            if (heat == Heat.FOURNAISE) lavaCharges = ForgeRules.REFORGES_PER_LAVA_BUCKET;
            if (!empty.isEmpty()) inventory.insertItem(SLOT_EMPTIES, empty, false);
            workHeat = heat;
        } else {
            workHeat = Heat.FOURNAISE;
        }
        if (workHeat == Heat.FOURNAISE) lavaCharges--;
        water -= ForgeRules.WATER_PER_REFORGE;
        working = true;
        progress = 0;
        setLit(true);
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7F, 0.8F);
        setChanged();
        sync();
        return true;
    }

    private void finish() {
        working = false;
        progress = 0;
        // Étape 3 : c'est ici que la rareté sera tirée (chaleur workHeat, offrande workOffering, plans lus).
        workHeat = Heat.NONE;
        workOffering = 0;
        setLit(false);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
        setChanged();
        sync();
    }

    private void setLit(boolean lit) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.getValue(FireForgeBlock.LIT) != lit) level.setBlock(worldPosition, state.setValue(FireForgeBlock.LIT, lit), Block.UPDATE_ALL);
    }

    private void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ sauvegarde et synchro

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("water", water);
        tag.putInt("lava", lavaCharges);
        tag.putBoolean("working", working);
        tag.putInt("progress", progress);
        tag.putInt("heat", workHeat.level());
        tag.putInt("offering", workOffering);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        water = tag.getInt("water");
        lavaCharges = tag.getInt("lava");
        working = tag.getBoolean("working");
        progress = tag.getInt("progress");
        workHeat = Heat.byLevel(tag.getInt("heat"));
        workOffering = tag.getInt("offering");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------ interface

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.echoes_fireforge.fire_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FireForgeMenu(containerId, playerInventory, this, data);
    }
}
