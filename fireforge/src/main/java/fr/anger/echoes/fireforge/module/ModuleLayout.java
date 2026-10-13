package fr.anger.echoes.fireforge.module;

import fr.anger.echoes.fireforge.forge.ForgeGeometry;
import fr.anger.echoes.fireforge.forge.ForgePart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

/** Où se posent les modules autour de la forge. {@code main} est la moitié du foyer (gauche, vue de face). */
public final class ModuleLayout {

    private ModuleLayout() {}

    /** Les positions où peut se poser le bas d'une pile de modules, pour ce côté. */
    public static List<BlockPos> bases(BlockPos main, Direction facing, ModuleSide side) {
        BlockPos right = ForgeGeometry.partner(main, facing, ForgePart.LEFT);
        return switch (side) {
            case LEFT -> List.of(main.relative(facing.getClockWise()));
            case BACK -> List.of(main.relative(facing.getOpposite()), right.relative(facing.getOpposite()));
            case RIGHT -> List.of(right.relative(facing.getCounterClockWise()));
        };
    }
}
