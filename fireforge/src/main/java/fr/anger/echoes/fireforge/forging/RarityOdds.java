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

    /**
     * Garantir (module des âmes) : ni Fêlé ni Brut au tirage. Leurs chances passent aux raretés possibles au-dessus,
     * en proportion. Si la chaleur plafonne à Brut, rien n'est possible au-dessus : on sort Brut.
     */
    public static double[] guarantee(double[] odds) {
        double[] out = odds.clone();
        double above = 0;
        for (int i = Rarity.TREMPE.ordinal(); i < out.length; i++) above += out[i];
        out[Rarity.FELE.ordinal()] = 0;
        if (above <= 0) {
            java.util.Arrays.fill(out, 0);
            out[Rarity.BRUT.ordinal()] = 100;
            return out;
        }
        out[Rarity.BRUT.ordinal()] = 0;
        for (int i = Rarity.TREMPE.ordinal(); i < out.length; i++) out[i] = out[i] * 100.0 / above;
        return out;
    }

    /**
     * Le Lit de cendre (étape 12c), avec une offrande : petite ou bonne, plus de Fêlé ; grosse (fragment de netherite),
     * ni Fêlé ni Brut. Sans offrande, rien ne change. La fêlure manquée au soufflet donne toujours Fêlé (hors tirage).
     */
    public static double[] ashBed(double[] odds, int offeringTier) {
        if (offeringTier <= 0) return odds;
        return floor(odds, offeringTier >= 3 ? Rarity.TREMPE : Rarity.BRUT);
    }

    /**
     * Rareté minimum : les chances des raretés en dessous passent à celles qui restent possibles, en proportion. Si la
     * chaleur plafonne sous le minimum, on sort la meilleure rareté que la chaleur permet.
     */
    public static double[] floor(double[] odds, Rarity minimum) {
        double[] out = odds.clone();
        double kept = 0;
        for (int i = minimum.ordinal(); i < out.length; i++) kept += out[i];
        if (kept <= 0) {
            int best = 0;
            for (int i = 0; i < out.length; i++) if (out[i] > 0) best = i;
            java.util.Arrays.fill(out, 0);
            out[best] = 100;
            return out;
        }
        for (int i = 0; i < minimum.ordinal(); i++) out[i] = 0;
        for (int i = minimum.ordinal(); i < out.length; i++) out[i] = out[i] * 100.0 / kept;
        return out;
    }
}
