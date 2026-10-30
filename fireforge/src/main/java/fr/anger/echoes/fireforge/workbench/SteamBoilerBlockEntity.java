package fr.anger.echoes.fireforge.workbench;

import fr.anger.echoes.fireforge.progress.ForgeEchoes;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Le bouilleur de l'établi : un foyer et un ballon d'eau. Une pièce à vapeur posée dans sa case se recharge,
 * une charge toutes les demi-secondes, en prenant l'eau et le combustible au fur et à mesure.
 */
public class SteamBoilerBlockEntity extends BlockEntity {

    public static final int SLOT_FUEL = 0, SLOT_PIECE = 1, SLOTS = 2;
    public static final int DATA_COUNT = 4;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_FUEL ? isFuel(stack) : Steam.usesSteam(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_PIECE ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int water;
    /** Ce qui reste du combustible déjà brûlé, en ticks de fourneau. */
    private int fuel;
    private int progress;
    /** Le dernier joueur à avoir ouvert l'établi : c'est lui qui pose les pièces (succès « Vapeur domptée »). */
    @Nullable
    private UUID user;

    /** Pour l'interface : eau, combustible en réserve, progression, travaille-t-il (0/1). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> water;
                case 1 -> fuel;
                case 2 -> progress;
                default -> working() ? 1 : 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SteamBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(FFBlockEntities.STEAM_BOILER.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    public int water() {
        return water;
    }

    public void setUser(UUID user) {
        this.user = user;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    /** @return la quantité d'eau prise (ou qui serait prise, en simulation) */
    public int fillWater(int amount, boolean simulate) {
        int taken = Math.max(0, Math.min(amount, Steam.TANK - water));
        if (!simulate && taken > 0) {
            water += taken;
            setChanged();
        }
        return taken;
    }

    private boolean needsCharge() {
        ItemStack piece = inventory.getStackInSlot(SLOT_PIECE);
        return Steam.usesSteam(piece) && Steam.charges(piece) < Steam.capacity(piece);
    }

    /** Il a de quoi faire une charge de plus. */
    private boolean working() {
        return needsCharge() && water >= Steam.WATER_PER_CHARGE && (fuel >= Steam.FUEL_PER_CHARGE || isFuel(inventory.getStackInSlot(SLOT_FUEL)));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamBoilerBlockEntity boiler) {
        boolean working = boiler.working();
        if (working) {
            if (boiler.fuel < Steam.FUEL_PER_CHARGE) boiler.burnOne();
            if (++boiler.progress >= Steam.TICKS_PER_CHARGE) {
                boiler.progress = 0;
                ItemStack piece = boiler.inventory.getStackInSlot(SLOT_PIECE).copy();
                ServerPlayer user = boiler.user != null && level.getServer() != null ? level.getServer().getPlayerList().getPlayer(boiler.user) : null;
                // l'Écho « Vapeur domptée » : la pièce tiendra 22 charges
                if (user != null && ForgeEchoes.has(user, ForgeEchoes.STEAM)) piece.set(FFDataComponents.STEAM_CAPACITY, Steam.TEMPERED_CHARGES);
                Steam.setCharges(piece, Steam.charges(piece) + 1);
                if (user != null) ForgeEchoes.steamCharged(user);
                boiler.inventory.setStackInSlot(SLOT_PIECE, piece);
                boiler.water -= Steam.WATER_PER_CHARGE;
                boiler.fuel -= Steam.FUEL_PER_CHARGE;
            }
            boiler.setChanged();
        } else if (boiler.progress != 0) {
            boiler.progress = 0;
            boiler.setChanged();
        }
        if (state.getValue(SteamBoilerBlock.LIT) != working) level.setBlock(pos, state.setValue(SteamBoilerBlock.LIT, working), 3);
    }

    /** Un combustible de plus au foyer (un seau de lave rend son seau). */
    private void burnOne() {
        ItemStack stack = inventory.getStackInSlot(SLOT_FUEL);
        if (!isFuel(stack)) return;
        fuel += stack.getBurnTime(RecipeType.SMELTING);
        ItemStack remainder = stack.getCraftingRemainingItem();
        ItemStack rest = stack.copy();
        rest.shrink(1);
        inventory.setStackInSlot(SLOT_FUEL, rest.isEmpty() ? remainder : rest);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("water", water);
        tag.putInt("fuel", fuel);
        tag.putInt("progress", progress);
        if (user != null) tag.putUUID("user", user);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        water = tag.getInt("water");
        fuel = tag.getInt("fuel");
        progress = tag.getInt("progress");
        user = tag.hasUUID("user") ? tag.getUUID("user") : null;
    }
}
