package fr.anger.echoes.fireforge.witherset;

import fr.anger.echoes.fireforge.registry.FFAttachments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Le set du Wither (plan, section 13) : l'armure que portaient les Attiseurs quand la maladie les a pris. Celui qui la
 * porte suit le même chemin, palier par palier ; rien ne dit d'où vient la maladie.
 * <p>
 * La progression (0 à 100 %) ne s'affiche pas : on la voit à l'armure qui change, à la cendre qui tombe, à la toux et
 * aux phrases à chaque palier (étape 13a), puis aux créatures (13b).
 */
public final class WitherSet {

    private WitherSet() {}

    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public static final int STAGES = 4;
    /** Un palier tous les 25 %. */
    public static final double PER_STAGE = 25.0;

    /** Par seconde : avec les 4 pièces, +1 % toutes les 2 minutes (le palier 4 en un peu plus de 3 heures). */
    public static final double RISE = 1.0 / 120;
    /** Par seconde, selon le nombre de pièces portées (0 à 3) : −2 %/min, −1 %/min, −1 % toutes les 2 min, toutes les 4 min. */
    public static final double[] FALL = {2.0 / 60, 1.0 / 60, 1.0 / 120, 1.0 / 240};

    public static int stage(double percent) {
        return Math.max(0, Math.min(STAGES, (int) Math.floor(percent / PER_STAGE)));
    }

    public static boolean isPiece(ItemStack stack) {
        return stack.getItem() instanceof WitherArmorItem;
    }

    /** Combien de pièces du set cette créature porte. */
    public static int piecesWorn(LivingEntity entity) {
        int count = 0;
        for (EquipmentSlot slot : SLOTS) if (isPiece(entity.getItemBySlot(slot))) count++;
        return count;
    }

    /** Le palier qu'on voit sur une créature : celui des pièces qu'elle porte (le plus haut). Côté client aussi. */
    public static int visibleStage(LivingEntity entity) {
        int stage = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (isPiece(stack)) stage = Math.max(stage, WitherArmorItem.stage(stack));
        }
        return stage;
    }

    /** Côté serveur : la progression du joueur. */
    public static WitherProgress progress(Player player) {
        return player.getData(FFAttachments.WITHER_PROGRESS);
    }

    /** Le palier du joueur (côté serveur). */
    public static int stage(Player player) {
        return progress(player).stage();
    }
}
