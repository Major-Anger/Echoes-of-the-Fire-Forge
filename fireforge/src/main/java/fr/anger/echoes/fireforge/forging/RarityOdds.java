package fr.anger.echoes.fireforge.forging;

import fr.anger.echoes.fireforge.forge.Heat;

/**
 * Les chances de chaque rareté, dans l'ordre validé : chances de départ → plans lus → offrande → plafond de chaleur.
 * (La fêlure par réflexe, étape 4, s'ajoute ensuite.) Le même calcul sert au tirage et à l'affichage.
 */
public final class RarityOdds {

    private RarityOdds() {}

    /** Chaque plan lu fait passer 2 % des raretés basses vers les hautes. */
    public static final double PER_PLAN = 2.0;
    /** Coup de pouce de l'offrande (petit, bon, gros). */
    private static final double[] OFFERING_SHIFT = {0, 4, 8, 14};
    /** Chance d'enchantement rare selon l'offrande, en %. */
    private static final double[] OFFERING_ENCHANT = {0, 3, 7, 15};

    /** Rareté maximum permise par la chaleur. */
    public static Rarity cap(Heat heat) {
        return switch (heat) {
            case NONE, TIEDE -> Rarity.BRUT;
            case CHAUD -> Rarity.TREMPE;
            case ARDENT -> Rarity.AFFINE;
            case BRULANT -> Rarity.ATTISE;
            case FOURNAISE -> Rarity.SPECIAL;
        };
    }

    public static double enchantChance(int offeringTier) {
        return OFFERING_ENCHANT[Math.max(0, Math.min(3, offeringTier))];
    }

    /** @return les chances en % (somme = 100), dans l'ordre de {@link Rarity#values()} */
    public static double[] compute(int plansRead, int offeringTier, Heat heat) {
        Rarity[] all = Rarity.values();
        double[] odds = new double[all.length];
        for (int i = 0; i < all.length; i++) odds[i] = all[i].baseChance();

        shift(odds, PER_PLAN * plansRead);
        shift(odds, OFFERING_SHIFT[Math.max(0, Math.min(3, offeringTier))]);

        // plafond de chaleur : ce qui dépasse redescend au plafond
        int cap = cap(heat).ordinal();
        for (int i = cap + 1; i < odds.length; i++) {
            odds[cap] += odds[i];
            odds[i] = 0;
        }
        return odds;
    }

    /**
     * Fait passer « amount » % de Fêlé (puis de Brut) vers Trempé, Affiné, Attisé et Spécial,
     * en proportion de leurs chances de départ : Spécial reste rare.
     */
    private static void shift(double[] odds, double amount) {
        double fromFele = Math.min(amount, odds[0]);
        odds[0] -= fromFele;
        double fromBrut = Math.min(amount - fromFele, odds[1]);
        odds[1] -= fromBrut;
        double moved = fromFele + fromBrut;
        double upperBase = 0;
        for (int i = 2; i < odds.length; i++) upperBase += Rarity.values()[i].baseChance();
        for (int i = 2; i < odds.length; i++) odds[i] += moved * Rarity.values()[i].baseChance() / upperBase;
    }
}
