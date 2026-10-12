package fr.anger.echoes.fireforge.forge;

import net.minecraft.util.StringRepresentable;

/** Les deux moitiés de la forge, vues de face : à gauche le foyer et la cheminée (le cœur), à droite l'enclume. */
public enum ForgePart implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    ForgePart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
