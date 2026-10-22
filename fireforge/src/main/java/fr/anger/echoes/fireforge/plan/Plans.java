package fr.anger.echoes.fireforge.plan;

import fr.anger.echoes.core.client.ClientLore;
import fr.anger.echoes.core.lore.Lore;
import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Les plans des Attiseurs. Lire un plan l'enregistre dans le lore du Core (echoes_fireforge:plan/...) :
 * le joueur « imagine » peu à peu leur technique, et chaque plan lu améliorera les chances de la forge.
 */
public final class Plans {

    private Plans() {}

    /** Nombre de Plans d'Attiseur (numérotés I à V). */
    public static final int COUNT = 5;

    /** Le Plan de la Forge : il sert à fabriquer la forge. */
    public static final ResourceLocation FORGE = EchoesFireForge.id("plan/forge");

    /** Le Plan de l'Établi : il sert à fabriquer l'Établi d'Attiseur. */
    public static final ResourceLocation WORKBENCH = EchoesFireForge.id("plan/workbench");

    public static ResourceLocation attiseur(int number) {
        return EchoesFireForge.id("plan/" + number);
    }

    /** Nombre de plans différents lus par le joueur (le Plan de la Forge compris). Côté serveur. */
    public static int readCount(Player player) {
        int count = 0;
        for (ResourceLocation entry : Lore.read(player)) {
            if (entry.getNamespace().equals(EchoesFireForge.MOD_ID) && entry.getPath().startsWith("plan/")) count++;
        }
        return count;
    }

    /** Nombre de plans lus, côté client (pour afficher les chances dans la forge). */
    public static int clientReadCount() {
        int count = (ClientLore.hasRead(FORGE) ? 1 : 0) + (ClientLore.hasRead(WORKBENCH) ? 1 : 0);
        for (int n = 1; n <= COUNT; n++) if (ClientLore.hasRead(attiseur(n))) count++;
        return count;
    }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static String roman(int number) {
        return number > 0 && number < ROMAN.length ? ROMAN[number] : String.valueOf(number);
    }
}
