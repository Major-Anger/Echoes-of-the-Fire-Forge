package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.Codec;
import fr.anger.echoes.fireforge.cendreux.CendreuxEntity;
import fr.anger.echoes.fireforge.registry.FFEntities;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Un gabarit de la Forge (tools/structures/make_structures.py), posé avec un brin d'usure au hasard, puis fini selon
 * son genre : des fondations sous les ruines, les piliers de l'annexe jusqu'au sol, les cheminées jusqu'au plafond,
 * les Cendreux qui gardent la chaufferie et le dernier foyer. Tout se fait tronçon par tronçon.
 */
public class ForgeTemplatePiece extends TemplateStructurePiece {

    public enum Kind implements StringRepresentable {
        RUIN, HEARTH, HALL, GATE, ANNEX, CHIMNEY;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Les piliers de l'annexe (x, z du gabarit), prolongés jusqu'au sol sous le bas du gabarit. */
    private static final int[][] ANNEX_PILLARS = {{1, 3}, {8, 3}, {13, 3}, {1, 11}, {8, 11}, {13, 11}};
    /** Les conduites (x, z du gabarit) prolongées jusqu'au plafond au-dessus du haut du gabarit ; la dernière est une chaîne. */
    private static final int[][] CHIMNEY_COLUMNS = {{3, 3}, {4, 3}, {5, 3}, {3, 4}, {5, 4}, {3, 5}, {4, 5}, {5, 5}, {7, 6}, {1, 1}, {6, 2}};
    private static final int CHIMNEY_TOP = 20, CHIMNEY_REACH = 10;
    /** Où se tiennent les gardiens (x, z du gabarit, posés sur le sol du gabarit). */
    private static final double[][] HALL_GUARDS = {{9.5, 8.5}, {5.5, 13.5}};
    private static final double[][] HEARTH_GUARDS = {{12.5, 9.5}, {2.5, 9.5}};

    private final Kind kind;
    @Nullable
    private final BlockState foundation;

    public ForgeTemplatePiece(StructureTemplateManager manager, ResourceLocation template, Kind kind, BlockPos position,
                              Rotation rotation, BlockPos pivot, @Nullable BlockState foundation) {
        super(FFStructures.TEMPLATE_PIECE.get(), 0, manager, template, template.toString(), settings(kind, rotation, pivot), position);
        this.kind = kind;
        this.foundation = foundation;
    }

    public ForgeTemplatePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(FFStructures.TEMPLATE_PIECE.get(), tag, manager, id -> settings(kindOf(tag), Rotation.valueOf(tag.getString("Rot")),
                new BlockPos(tag.getInt("PvX"), tag.getInt("PvY"), tag.getInt("PvZ"))));
        this.kind = kindOf(tag);
        this.foundation = tag.contains("Foundation")
                ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Foundation")) : null;
    }

    /** Un gabarit centré (en x, z) sur {@code center}, son sol à la hauteur de {@code center}, tourné autour de son milieu. */
    public static ForgeTemplatePiece centered(StructureTemplateManager manager, ResourceLocation template, Kind kind, BlockPos center,
                                              Rotation rotation, @Nullable BlockState foundation) {
        var size = manager.getOrCreate(template).getSize();
        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        return new ForgeTemplatePiece(manager, template, kind, center.offset(-pivot.getX(), 0, -pivot.getZ()), rotation, pivot, foundation);
    }

    private static Kind kindOf(CompoundTag tag) {
        return Kind.valueOf(tag.getString("Kind"));
    }

    /** L'usure, différente à chaque endroit mais la même d'un tronçon à l'autre (le hasard vient de la position). */
    private static StructurePlaceSettings settings(Kind kind, Rotation rotation, BlockPos pivot) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setRotationPivot(pivot).setMirror(Mirror.NONE);
        switch (kind) {
            case RUIN -> {
                // des briques tombées, d'autres fendues
                settings.addProcessor(new BlockRotProcessor(HolderSet.direct(BuiltInRegistries.BLOCK::wrapAsHolder,
                        Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS), 0.9F));
                settings.addProcessor(cracks(0.2F));
            }
            case HEARTH, HALL, GATE, ANNEX -> settings.addProcessor(cracks(0.08F));
            case CHIMNEY -> { }
        }
        return settings;
    }

    private static RuleProcessor cracks(float chance) {
        return new RuleProcessor(List.of(
                new ProcessorRule(new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, chance), AlwaysTrueTest.INSTANCE,
                        Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()),
                new ProcessorRule(new RandomBlockMatchTest(Blocks.NETHER_BRICKS, chance), AlwaysTrueTest.INSTANCE,
                        Blocks.CRACKED_NETHER_BRICKS.defaultBlockState())));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Kind", kind.name());
        tag.putString("Rot", placeSettings.getRotation().name());
        BlockPos pivot = placeSettings.getRotationPivot();
        tag.putInt("PvX", pivot.getX());
        tag.putInt("PvY", pivot.getY());
        tag.putInt("PvZ", pivot.getZ());
        if (foundation != null) tag.put("Foundation", NbtUtils.writeBlockState(foundation));
    }

    public Kind kind() {
        return kind;
    }

    /** Un point du gabarit (x, y, z), dans le monde. */
    public BlockPos world(int x, int y, int z) {
        return StructureTemplate.calculateRelativePosition(placeSettings, new BlockPos(x, y, z)).offset(templatePosition);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);
        if (foundation != null) foundations(level, box);
        switch (kind) {
            case ANNEX -> pillars(level, box);
            case CHIMNEY -> reachCeiling(level, box);
            case HALL -> guardians(level, box, random, HALL_GUARDS);
            case HEARTH -> guardians(level, box, random, HEARTH_GUARDS);
            default -> { }
        }
    }

    /** Rien ne flotte : sous le sol du gabarit, on comble les creux (6 blocs au plus). */
    private void foundations(WorldGenLevel level, BoundingBox box) {
        BoundingBox area = getBoundingBox();
        int floor = templatePosition.getY();
        for (int x = Math.max(area.minX(), box.minX()); x <= Math.min(area.maxX(), box.maxX()); x++) {
            for (int z = Math.max(area.minZ(), box.minZ()); z <= Math.min(area.maxZ(), box.maxZ()); z++) {
                if (level.getBlockState(new BlockPos(x, floor, z)).isAir()) continue;
                for (int y = floor - 1; y >= floor - 6; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!fillable(level.getBlockState(p))) break;
                    level.setBlock(p, foundation, 2);
                }
            }
        }
    }

    /** Les piliers de l'annexe descendent jusqu'au sol, à travers l'air et la lave, comme ceux des forteresses. */
    private void pillars(WorldGenLevel level, BoundingBox box) {
        BlockState bricks = Blocks.NETHER_BRICKS.defaultBlockState();
        for (int[] pillar : ANNEX_PILLARS) {
            BlockPos p = world(pillar[0], -1, pillar[1]);
            if (!box.isInside(p.getX(), box.minY(), p.getZ())) continue;
            while (p.getY() > level.getMinBuildHeight() + 1 && fillable(level.getBlockState(p))) {
                level.setBlock(p, bricks, 2);
                p = p.below();
            }
        }
    }

    /** Les conduites montent jusqu'à la roche du plafond, s'il est plus haut que le gabarit. */
    private void reachCeiling(WorldGenLevel level, BoundingBox box) {
        BlockState pipe = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        BlockState chain = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        for (int i = 0; i < CHIMNEY_COLUMNS.length; i++) {
            int[] column = CHIMNEY_COLUMNS[i];
            BlockState state = i == CHIMNEY_COLUMNS.length - 1 ? chain : pipe;
            BlockPos p = world(column[0], CHIMNEY_TOP, column[1]);
            if (!box.isInside(p.getX(), box.minY(), p.getZ())) continue;
            for (int n = 0; n < CHIMNEY_REACH && p.getY() < level.getMaxBuildHeight() && level.getBlockState(p).isAir(); n++) {
                level.setBlock(p, state, 2);
                p = p.above();
            }
        }
    }

    /** Quelques Cendreux gardent encore les lieux (ils ne partent pas). */
    private void guardians(WorldGenLevel level, BoundingBox box, RandomSource random, double[][] spots) {
        for (double[] spot : spots) {
            BlockPos p = world((int) Math.floor(spot[0]), 1, (int) Math.floor(spot[1]));
            if (!box.isInside(p)) continue;
            CendreuxEntity cendreux = FFEntities.CENDREUX.get().create(level.getLevel());
            if (cendreux == null) continue;
            cendreux.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            EventHooks.finalizeMobSpawn(cendreux, level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
            cendreux.setPersistenceRequired();
            level.addFreshEntityWithPassengers(cendreux);
        }
    }

    static boolean fillable(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
    }
}
