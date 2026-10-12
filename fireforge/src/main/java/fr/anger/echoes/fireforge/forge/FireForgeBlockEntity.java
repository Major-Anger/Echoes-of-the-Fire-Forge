package fr.anger.echoes.fireforge.forge;

import fr.anger.echoes.fireforge.forging.Forging;
import fr.anger.echoes.fireforge.forging.ForgingData;
import fr.anger.echoes.fireforge.forging.Rarity;
import fr.anger.echoes.fireforge.plan.Plans;
import fr.anger.echoes.fireforge.registry.FFBlockEntities;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
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
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Le cœur de la forge (porté par la moitié gauche) : réserves d'eau et de lave, cases, reforge de 10 secondes.
 * À la fin, l'objet reçoit une rareté tirée selon la chaleur, l'offrande et les plans lus par le forgeron.
 * Trop chaud pour son matériau, l'objet fait craquer la forge : un coup de soufflet à temps évite la fêlure.
 */
public class FireForgeBlockEntity extends BlockEntity implements MenuProvider {

    /** Données envoyées à l'interface : eau, capacité, chaleur, progression, état. */
    public static final int DATA_COUNT = 5;
    public static final int SLOT_ITEM = 0, SLOT_OFFERING = 1, SLOT_FUEL = 2, SLOT_WATER = 3, SLOT_EMPTIES = 4, SLOTS = 5;

    /** Ce que l'interface affiche (et pourquoi la forge ne peut pas démarrer). */
    public enum Status {
        READY, WORKING, NO_ITEM, NO_OFFERING, NO_WATER, NO_FUEL, EMPTIES_FULL,
        /** Pendant la reforge : ça craque (il faut le soufflet), fêlure évitée, ou l'objet s'est fendu. */
        CRACKING, AVOIDED, CRACKED;

        public boolean isWorking() {
            return this == WORKING || this == CRACKING || this == AVOIDED || this == CRACKED;
        }
    }

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
    private int workPlans;
    @Nullable
    private UUID smith;
    // la fêlure : moments des craquements (en ticks de reforge), le prochain, la fenêtre ouverte, et le résultat
    private int[] crackTicks = new int[0];
    private int nextCrack;
    private int windowEnd = -1;
    private boolean cracked;
    /** Temps d'affichage de « Fêlure évitée ». */
    private int avoidedTicks;
    private int bellowsCooldown;

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
            return DATA_COUNT;
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
        if (working) {
            if (windowEnd >= 0) return Status.CRACKING;
            if (avoidedTicks > 0) return Status.AVOIDED;
            return cracked ? Status.CRACKED : Status.WORKING;
        }
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

    /** Ça craque en ce moment : il faut actionner le soufflet (le client s'en sert pour faire trembler l'objet). */
    public boolean isCracking() {
        return working && windowEnd >= 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireForgeBlockEntity forge) {
        forge.fillWater();
        if (forge.bellowsCooldown > 0) forge.bellowsCooldown--;
        if (forge.working) forge.workTick();
    }

    private void workTick() {
        progress++;
        if (avoidedTicks > 0) avoidedTicks--;
        if (windowEnd >= 0 && progress > windowEnd) missCrack();
        if (nextCrack < crackTicks.length && progress >= crackTicks[nextCrack]) {
            nextCrack++;
            if (!cracked) crack(); // déjà fendu : plus rien à rattraper
        }
        if (progress >= ForgeRules.REFORGE_TICKS) finish();
    }

    /** Les moments des craquements, au hasard, assez espacés pour que leurs fenêtres ne se chevauchent pas. */
    static int[] scheduleCracks(int count, RandomSource random) {
        int[] ticks = new int[count];
        int slack = ForgeRules.CRACK_LAST - ForgeRules.CRACK_FIRST - ForgeRules.CRACK_SPACING * (count - 1);
        for (int i = 0; i < count; i++) ticks[i] = random.nextInt(Math.max(0, slack) + 1);
        Arrays.sort(ticks);
        for (int i = 0; i < count; i++) ticks[i] += ForgeRules.CRACK_FIRST + i * ForgeRules.CRACK_SPACING;
        return ticks;
    }

    private void crack() {
        windowEnd = progress + ForgeRules.CRACK_WINDOW;
        level.playSound(null, worldPosition, SoundEvents.BASALT_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F);
        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35F, 1.7F);
        if (level instanceof ServerLevel server) {
            Direction facing = getBlockState().getValue(FireForgeBlock.FACING);
            Vec3 hearth = ForgeGeometry.point(worldPosition, facing, 16, 4, 15.5);
            Vec3 anvil = ForgeGeometry.point(ForgeGeometry.partner(worldPosition, facing, ForgePart.LEFT), facing, 8, 18.5, 9.5);
            server.sendParticles(ParticleTypes.LAVA, hearth.x, hearth.y, hearth.z, 5, 0.3, 0.1, 0.1, 0);
            server.sendParticles(ParticleTypes.LAVA, anvil.x, anvil.y, anvil.z, 3, 0.1, 0.05, 0.1, 0);
            server.sendParticles(ParticleTypes.SMALL_FLAME, anvil.x, anvil.y, anvil.z, 10, 0.15, 0.05, 0.15, 0.03);
        }
        tellSmith(Component.translatable("message.echoes_fireforge.crack").withStyle(ChatFormatting.GOLD));
        setChanged();
        sync();
    }

    private void missCrack() {
        windowEnd = -1;
        cracked = true;
        level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        if (level instanceof ServerLevel server) {
            Direction facing = getBlockState().getValue(FireForgeBlock.FACING);
            Vec3 anvil = ForgeGeometry.point(ForgeGeometry.partner(worldPosition, facing, ForgePart.LEFT), facing, 8, 18.5, 9.5);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, anvil.x, anvil.y, anvil.z, 6, 0.1, 0.05, 0.1, 0.01);
        }
        tellSmith(Component.translatable("message.echoes_fireforge.cracked").withStyle(ChatFormatting.RED));
        setChanged();
        sync();
    }

    /**
     * Un coup de soufflet : un jet de vapeur sur le métal. Pendant un craquement, il évite la fêlure ;
     * sinon, il ne rattrape rien et coûte un peu d'eau. Sans assez d'eau, pas de vapeur.
     * @return true si un craquement a été rattrapé
     */
    public boolean blowBellows(Player player) {
        if (level == null || bellowsCooldown > 0) return false;
        bellowsCooldown = ForgeRules.BELLOWS_COOLDOWN;
        if (water < ForgeRules.BELLOWS_WATER) {
            level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 0.6F);
            return false;
        }
        boolean saved = working && windowEnd >= 0;
        if (saved) {
            windowEnd = -1;
            avoidedTicks = ForgeRules.CRACK_WINDOW;
            player.displayClientMessage(Component.translatable("message.echoes_fireforge.avoided").withStyle(ChatFormatting.GREEN), true);
            sync();
        } else {
            water -= ForgeRules.BELLOWS_WATER;
        }
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, saved ? 0.9F : 0.5F, 1.3F);
        steamJet(saved ? 8 : 4);
        setChanged();
        return saved;
    }

    /** La vapeur sort de la buse du soufflet, vers le foyer. */
    private void steamJet(int puffs) {
        if (!(level instanceof ServerLevel server)) return;
        Direction facing = getBlockState().getValue(FireForgeBlock.FACING);
        Vec3 nozzle = ForgeGeometry.point(worldPosition, facing, 9.5, 5, 17);
        Vec3 dir = ForgeGeometry.point(worldPosition, facing, 10.5, 5, 17).subtract(nozzle).normalize();
        for (int i = 0; i < puffs; i++) {
            double spread = (level.random.nextDouble() - 0.5) * 0.06;
            server.sendParticles(ParticleTypes.CLOUD, nozzle.x, nozzle.y, nozzle.z, 0, dir.x + spread, 0.05 + spread, dir.z - spread, 0.12 + i * 0.015);
        }
    }

    private void tellSmith(Component message) {
        Player player = smith != null ? level.getPlayerByUUID(smith) : null;
        if (player != null && player.distanceToSqr(worldPosition.getCenter()) < 32 * 32) player.displayClientMessage(message, true);
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

    /** Appelé par le marteau de l'interface : c'est le savoir (les plans lus) de ce joueur qui compte. */
    public boolean tryStart(Player player) {
        if (status() != Status.READY || level == null) return false;
        workPlans = Plans.readCount(player);
        smith = player.getUUID();
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
        crackTicks = scheduleCracks(Tolerance.cracks(inventory.getStackInSlot(SLOT_ITEM), workHeat), level.getRandom());
        nextCrack = 0;
        windowEnd = -1;
        cracked = false;
        avoidedTicks = 0;
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
        Rarity rarity = null;
        Optional<Holder<Enchantment>> enchant = Optional.empty();
        ItemStack stack = inventory.getStackInSlot(SLOT_ITEM);
        if (level != null && !stack.isEmpty()) {
            ItemStack forged = stack.copy();
            // un craquement manqué : l'objet sort Fêlé, quel que soit le tirage
            ForgingData data = cracked
                    ? Forging.rollFor(forged, Rarity.FELE, level.getRandom())
                    : Forging.roll(forged, workHeat, workOffering, workPlans, level.getRandom());
            forged.set(FFDataComponents.FORGING, data);
            enchant = Forging.maybeEnchant(forged, workOffering, level.getRandom(), level.registryAccess());
            inventory.setStackInSlot(SLOT_ITEM, forged); // reforger remplace la rareté précédente
            rarity = data.rarity();
        }
        working = false;
        progress = 0;
        workHeat = Heat.NONE;
        workOffering = 0;
        crackTicks = new int[0];
        nextCrack = 0;
        windowEnd = -1;
        cracked = false;
        avoidedTicks = 0;
        setLit(false);
        if (level != null && rarity != null) announce(rarity, enchant);
        smith = null;
        setChanged();
        sync();
    }

    /** Le son et le message dépendent de ce qui sort de la forge. */
    private void announce(Rarity rarity, Optional<Holder<Enchantment>> enchant) {
        switch (rarity) {
            case FELE -> level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.6F);
            case ATTISE -> level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.7F);
            case SPECIAL -> level.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 0.8F);
            default -> {}
        }
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
        Component message = Component.translatable("message.echoes_fireforge.forged", rarity.displayName()).withStyle(ChatFormatting.GRAY);
        if (enchant.isPresent()) {
            message = message.copy().append(Component.translatable("message.echoes_fireforge.enchanted",
                    enchant.get().value().description()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tellSmith(message);
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
        tag.putInt("plans", workPlans);
        if (smith != null) tag.putUUID("smith", smith);
        tag.putIntArray("cracks", crackTicks);
        tag.putInt("next_crack", nextCrack);
        tag.putInt("window_end", windowEnd);
        tag.putBoolean("cracked", cracked);
        tag.putInt("avoided", avoidedTicks);
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
        workPlans = tag.getInt("plans");
        smith = tag.hasUUID("smith") ? tag.getUUID("smith") : null;
        crackTicks = tag.getIntArray("cracks");
        nextCrack = tag.getInt("next_crack");
        windowEnd = tag.contains("window_end") ? tag.getInt("window_end") : -1;
        cracked = tag.getBoolean("cracked");
        avoidedTicks = tag.getInt("avoided");
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
