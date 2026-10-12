package fr.anger.echoes.fireforge.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Les modèles de la forge sont dessinés face au sud (en pixels, 0 à 16). Ces outils tournent un point ou une
 * forme selon l'orientation réelle de la forge, comme le fait la rotation « y » des blockstates.
 */
public final class ForgeGeometry {

    private ForgeGeometry() {}

    /** Nombre de quarts de tour (dans le sens horaire, vu du dessus) depuis le sud. */
    private static int quarterTurns(Direction facing) {
        return switch (facing) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    /** (x, z) en pixels, tourné d'un quart de tour : (x, z) -> (16 - z, x). */
    private static double[] rotate(double x, double z, int turns) {
        for (int i = 0; i < turns; i++) {
            double nx = 16 - z;
            z = x;
            x = nx;
        }
        return new double[]{x, z};
    }

    /** Un point du modèle (en pixels) dans le monde, pour le bloc à cette position. */
    public static Vec3 point(BlockPos pos, Direction facing, double x, double y, double z) {
        double[] r = rotate(x, z, quarterTurns(facing));
        return new Vec3(pos.getX() + r[0] / 16.0, pos.getY() + y / 16.0, pos.getZ() + r[1] / 16.0);
    }

    /** Une boîte du modèle (en pixels), tournée selon l'orientation. */
    public static VoxelShape box(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        int turns = quarterTurns(facing);
        double[] a = rotate(x1, z1, turns), b = rotate(x2, z2, turns);
        return Shapes.box(Math.min(a[0], b[0]) / 16, y1 / 16, Math.min(a[1], b[1]) / 16,
                Math.max(a[0], b[0]) / 16, y2 / 16, Math.max(a[1], b[1]) / 16);
    }

    /** Position de l'autre moitié de la forge. */
    public static BlockPos partner(BlockPos pos, Direction facing, ForgePart part) {
        return pos.relative(partnerDirection(facing, part));
    }

    /** Direction de l'autre moitié. Exemple : forge tournée vers le sud, la moitié droite (vue de face) est à l'est. */
    public static Direction partnerDirection(Direction facing, ForgePart part) {
        return part == ForgePart.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
    }
}
