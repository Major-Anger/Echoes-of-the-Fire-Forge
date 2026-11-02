package fr.anger.echoes.fireforge.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFStructures;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La forge d'une forteresse (les forteresses sont des ouvrages des Attiseurs) : posée à côté d'un pont d'une vraie
 * forteresse, à la hauteur du tablier, sur ses piliers, son bout de pont rompu tourné vers lui. Rien n'est glissé dans
 * la forteresse elle-même.
 * <p>
 * La structure est examinée à chaque tronçon, mais ne va plus loin que là où Minecraft place une forteresse : on
 * rejoue son choix (forteresse ou bastion) et on reprend les pièces de la forteresse pour trouver un pont.
 */
public class FortressAnnexStructure extends Structure {

    public static final MapCodec<FortressAnnexStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F / 3.0F).forGetter(s -> s.chance)
    ).apply(instance, FortressAnnexStructure::new));

    public static final ResourceLocation ANNEX = EchoesFireForge.id("fortress_annex");
    /** Dans le gabarit : le bout du pont rompu, au niveau du plateau. */
    private static final BlockPos STUB = new BlockPos(5, 4, 0);
    /** Pas plus loin du tronçon de départ (les pièces d'une structure restent à 8 tronçons au plus). */
    private static final int MAX_CHUNKS = 7;

    private final float chance;

    public FortressAnnexStructure(StructureSettings settings, float chance) {
        super(settings);
        this.chance = chance;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        if (random.nextFloat() >= chance) return Optional.empty();
        StructureStart fortress = fortressAt(context);
        if (fortress == null) return Optional.empty();
        StructureTemplate template = context.structureTemplateManager().getOrCreate(ANNEX);
        Columns columns = new Columns(context);
        List<StructurePiece> bridges = new ArrayList<>();
        for (StructurePiece piece : fortress.getPieces()) {
            if (piece instanceof NetherFortressPieces.BridgeStraight && piece.getOrientation() != null) bridges.add(piece);
        }
        Util.shuffle(bridges, random);
        for (StructurePiece bridge : bridges) {
            BoundingBox box = bridge.getBoundingBox();
            boolean alongZ = bridge.getOrientation().getAxis() == Direction.Axis.Z;
            int deck = box.minY() + 4;                               // le dessus du tablier
            int first = random.nextBoolean() ? 1 : -1;
            for (int side : new int[]{first, -first}) {
                int along = 4 + random.nextInt(11), gap = 2 + random.nextInt(3);
                BlockPos target;
                Rotation rotation;
                if (alongZ) {
                    target = new BlockPos(side > 0 ? box.maxX() + 1 + gap : box.minX() - 1 - gap, deck, box.minZ() + along);
                    rotation = side > 0 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
                } else {
                    target = new BlockPos(box.minX() + along, deck, side > 0 ? box.maxZ() + 1 + gap : box.minZ() - 1 - gap);
                    rotation = side > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
                }
                BlockPos origin = target.subtract(StructureTemplate.transform(STUB, Mirror.NONE, rotation, BlockPos.ZERO));
                BoundingBox annex = template.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), origin);
                if (!fits(annex, fortress, context.chunkPos()) || !open(columns, annex, deck)) continue;
                BlockPos center = annex.getCenter().atY(deck);
                return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new ForgeTemplatePiece(
                        context.structureTemplateManager(), ANNEX, ForgeTemplatePiece.Kind.ANNEX, origin, rotation, BlockPos.ZERO, null))));
            }
        }
        return Optional.empty();
    }

    /** Loin de toutes les pièces de la forteresse, et pas trop loin du tronçon de départ. */
    private static boolean fits(BoundingBox annex, StructureStart fortress, ChunkPos start) {
        BoundingBox around = annex.inflatedBy(1);
        for (StructurePiece piece : fortress.getPieces()) {
            if (piece.getBoundingBox().intersects(around)) return false;
        }
        ChunkPos chunk = new ChunkPos(annex.getCenter());
        return Math.abs(chunk.x - start.x) <= MAX_CHUNKS && Math.abs(chunk.z - start.z) <= MAX_CHUNKS;
    }

    /** L'annexe donne surtout sur le vide (pas enfouie dans la roche). */
    private static boolean open(Columns columns, BoundingBox annex, int deck) {
        int air = 0, total = 0;
        for (int x : new int[]{annex.minX(), annex.getCenter().getX(), annex.maxX()}) {
            for (int z : new int[]{annex.minZ(), annex.getCenter().getZ(), annex.maxZ()}) {
                for (int y = deck + 1; y <= deck + 5; y += 2) {
                    total++;
                    if (columns.air(x, y, z)) air++;
                }
            }
        }
        return air >= total * 0.6F;
    }

    /**
     * La forteresse que Minecraft place dans ce tronçon, s'il y en a une : on rejoue son tirage (le même que dans
     * ChunkGenerator.createStructures) entre les structures de « nether_complexes ». Null sinon (pas de forteresse,
     * un bastion, ou un ensemble changé par un autre mod).
     */
    @Nullable
    private static StructureStart fortressAt(GenerationContext context) {
        Optional<Holder.Reference<StructureSet>> complexes = context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .getHolder(BuiltinStructureSets.NETHER_COMPLEXES);
        if (complexes.isEmpty() || !(complexes.get().value().placement() instanceof RandomSpreadStructurePlacement spread)) return null;
        ChunkPos here = context.chunkPos();
        if (!spread.getPotentialStructureChunk(context.seed(), here.x, here.z).equals(here)) return null;
        List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>(complexes.get().value().structures());
        WorldgenRandom pick = new WorldgenRandom(new LegacyRandomSource(0L));
        pick.setLargeFeatureSeed(context.seed(), here.x, here.z);
        int total = 0;
        for (StructureSet.StructureSelectionEntry entry : entries) total += entry.weight();
        while (!entries.isEmpty()) {
            int roll = pick.nextInt(total), index = 0;
            for (StructureSet.StructureSelectionEntry entry : entries) {
                roll -= entry.weight();
                if (roll < 0) break;
                index++;
            }
            StructureSet.StructureSelectionEntry entry = entries.get(index);
            Structure structure = entry.structure().value();
            StructureStart start = structure.generate(context.registryAccess(), context.chunkGenerator(), context.biomeSource(),
                    context.randomState(), context.structureTemplateManager(), context.seed(), here, 0, context.heightAccessor(),
                    structure.biomes()::contains);
            if (start.isValid()) return entry.structure().is(BuiltinStructures.FORTRESS) ? start : null;
            entries.remove(index);
            total -= entry.weight();
        }
        return null;
    }

    @Override
    public StructureType<?> type() {
        return FFStructures.FORTRESS_ANNEX.get();
    }
}
