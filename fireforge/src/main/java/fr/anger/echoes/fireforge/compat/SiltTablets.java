package fr.anger.echoes.fireforge.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Les Tablettes des Veilleurs lues dans le Silt, vues depuis la Forge sans charger le code du Silt : le Silt les garde
 * dans une donnée de joueur (un entier, un bit par tablette), que l'on retrouve par son identifiant. Lecture seule.
 * <p>
 * Le Silt s'appelle encore « smalladditions » dans son code ; il deviendra « echoes_silt » : on cherche les deux.
 */
public final class SiltTablets {

    private SiltTablets() {}

    private static final ResourceLocation[] IDS = {
            ResourceLocation.fromNamespaceAndPath("echoes_silt", "lore_tablets"),
            ResourceLocation.fromNamespaceAndPath("smalladditions", "lore_tablets"),
    };

    @Nullable
    private static AttachmentType<?> type() {
        for (ResourceLocation id : IDS) {
            AttachmentType<?> type = NeoForgeRegistries.ATTACHMENT_TYPES.get(id);
            if (type != null) return type;
        }
        return null;
    }

    /** Le Silt est-il là (sa donnée des tablettes existe) ? */
    public static boolean installed() {
        return type() != null;
    }

    /** Le joueur a-t-il lu cette tablette (1 à 15) ? Faux si le Silt n'est pas là. */
    public static boolean hasRead(Player player, int tablet) {
        AttachmentType<?> type = type();
        if (type == null || !player.hasData(type)) return false;
        return player.getData(type) instanceof Integer mask && (mask & (1 << tablet)) != 0;
    }
}
