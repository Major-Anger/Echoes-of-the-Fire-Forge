package fr.anger.echoes.fireforge.module;

import net.minecraft.util.StringRepresentable;

/** La place d'un bloc dans sa pile : le bas porte le manomètre, le haut la soupape. */
public enum ModuleSection implements StringRepresentable {
    BOTTOM("bottom"), MIDDLE("middle"), TOP("top");

    private final String name;

    ModuleSection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
