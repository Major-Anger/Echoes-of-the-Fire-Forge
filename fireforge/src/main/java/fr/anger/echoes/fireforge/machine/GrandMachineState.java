package fr.anger.echoes.fireforge.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ce qui a été fait de la Grande Machine (plan, 15c), gardé dans le monde du Nether : une seule machine, un seul choix,
 * pour tout le monde et pour toujours. Et ce qui s'ensuit : la croûte de la caverne, les fondations, les braises données.
 */
public class GrandMachineState extends SavedData {

    public enum Choice implements StringRepresentable {
        NONE("none"), LOWERED("lowered"), RAISED("raised");

        private final String key;

        Choice(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }

        static Choice byKey(String key) {
            for (Choice c : values()) if (c.key.equals(key)) return c;
            return NONE;
        }
    }

    private static final String NAME = "echoes_fireforge_grand_machine";

    private Choice choice = Choice.NONE;
    private long choiceTick;
    @Nullable
    private BlockPos hull;
    /** Baisser : jusqu'où la croûte a pris (colonnes de la caverne, du centre vers les parois). */
    private int crust;
    /** Baisser : les couches des fondations déjà vidées (du haut vers le bas) ; la trappe ouverte. */
    private int drained;
    private boolean hatchOpen;
    /** Pousser : le dernier jour où chaque joueur a reçu sa Braise du cœur. */
    private final Map<UUID, Long> emberDay = new HashMap<>();

    public static GrandMachineState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(GrandMachineState::new, GrandMachineState::load, null), NAME);
    }

    public Choice choice() {
        return choice;
    }

    public long choiceTick() {
        return choiceTick;
    }

    @Nullable
    public BlockPos hull() {
        return hull;
    }

    void choose(Choice choice, long tick, BlockPos hull) {
        this.choice = choice;
        this.choiceTick = tick;
        this.hull = hull;
        setDirty();
    }

    int crust() {
        return crust;
    }

    void crust(int value) {
        crust = value;
        setDirty();
    }

    int drained() {
        return drained;
    }

    void drained(int value) {
        drained = value;
        setDirty();
    }

    boolean hatchOpen() {
        return hatchOpen;
    }

    void openHatch() {
        hatchOpen = true;
        setDirty();
    }

    /** La Braise du jour : true si ce joueur peut en recevoir une aujourd'hui (et la lui compte). */
    boolean takeEmber(UUID player, long day) {
        Long last = emberDay.get(player);
        if (last != null && last >= day) return false;
        emberDay.put(player, day);
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("choice", choice.getSerializedName());
        tag.putLong("choice_tick", choiceTick);
        if (hull != null) tag.put("hull", NbtUtils.writeBlockPos(hull));
        tag.putInt("crust", crust);
        tag.putInt("drained", drained);
        tag.putBoolean("hatch", hatchOpen);
        CompoundTag embers = new CompoundTag();
        emberDay.forEach((id, day) -> embers.putLong(id.toString(), day));
        tag.put("embers", embers);
        return tag;
    }

    private static GrandMachineState load(CompoundTag tag, HolderLookup.Provider registries) {
        GrandMachineState state = new GrandMachineState();
        state.choice = Choice.byKey(tag.getString("choice"));
        state.choiceTick = tag.getLong("choice_tick");
        state.hull = NbtUtils.readBlockPos(tag, "hull").orElse(null);
        state.crust = tag.getInt("crust");
        state.drained = tag.getInt("drained");
        state.hatchOpen = tag.getBoolean("hatch");
        CompoundTag embers = tag.getCompound("embers");
        for (String key : embers.getAllKeys()) state.emberDay.put(UUID.fromString(key), embers.getLong(key));
        return state;
    }
}
