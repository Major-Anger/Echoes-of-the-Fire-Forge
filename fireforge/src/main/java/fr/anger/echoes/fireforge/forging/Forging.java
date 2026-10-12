package fr.anger.echoes.fireforge.forging;

import fr.anger.echoes.fireforge.forge.Heat;
import fr.anger.echoes.fireforge.forging.ForgingData.StatRoll;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Le tirage de la forge : rareté, statistiques et Spécial, puis l'éventuel enchantement rare de l'offrande. */
public final class Forging {

    private Forging() {}

    /** Emplacement où l'objet se porte : armure (tête, torse…), bouclier (main secondaire) ou arme (main). */
    public static EquipmentSlot slotOf(ItemStack stack) {
        Equipable equipable = Equipable.get(stack);
        return equipable != null ? equipable.getEquipmentSlot() : EquipmentSlot.MAINHAND;
    }

    public static Rarity pick(double[] odds, RandomSource random) {
        double roll = random.nextDouble() * 100.0, total = 0;
        Rarity[] all = Rarity.values();
        for (int i = 0; i < all.length; i++) {
            total += odds[i];
            if (roll < total) return all[i];
        }
        // arrondis : on prend la plus haute rareté encore possible
        for (int i = all.length - 1; i >= 0; i--) if (odds[i] > 0) return all[i];
        return Rarity.BRUT;
    }

    /** Tire la rareté et ce qui va avec, pour cet objet. */
    public static ForgingData roll(ItemStack stack, Heat heat, int offeringTier, int plansRead, RandomSource random) {
        return rollFor(stack, pick(RarityOdds.compute(plansRead, offeringTier, heat), random), random);
    }

    public static ForgingData rollFor(ItemStack stack, Rarity rarity, RandomSource random) {
        EquipmentSlot slot = slotOf(stack);
        List<ForgeStat> pool = new ArrayList<>(Arrays.asList(ForgeStat.poolFor(slot)));
        List<StatRoll> stats = new ArrayList<>();
        Optional<Special> special = Optional.empty();
        switch (rarity) {
            case FELE -> stats.add(take(pool, random, -2, -6));
            case TREMPE -> stats.add(take(pool, random, 1, 3));
            case AFFINE -> {
                stats.add(take(pool, random, 2, 6));
                stats.add(take(pool, random, 2, 6));
            }
            case ATTISE -> {
                int bonuses = Math.min(random.nextBoolean() ? 2 : 3, pool.size() - 1); // le bouclier n'a que 3 statistiques
                for (int i = 0; i < bonuses; i++) stats.add(take(pool, random, 4, 9));
                stats.add(take(pool, random, -3, -6)); // la contrepartie
            }
            case SPECIAL -> {
                List<Special> fitting = Arrays.stream(Special.values()).filter(s -> s.fits(slot)).toList();
                if (fitting.isEmpty()) return rollFor(stack, Rarity.ATTISE, random);
                special = Optional.of(fitting.get(random.nextInt(fitting.size())));
            }
            default -> {}
        }
        return new ForgingData(rarity, List.copyOf(stats), special);
    }

    /** Une statistique pas encore tirée, avec un pourcentage entre a et b (arrondi au dixième). */
    private static StatRoll take(List<ForgeStat> pool, RandomSource random, float a, float b) {
        ForgeStat stat = pool.remove(random.nextInt(pool.size()));
        float low = Math.min(a, b), high = Math.max(a, b);
        float percent = Math.round((low + random.nextFloat() * (high - low)) * 10f) / 10f;
        return new StatRoll(stat, percent);
    }

    /**
     * L'offrande peut ajouter un enchantement rare : compatible avec l'objet et ses enchantements,
     * jamais un enchantement « trésor » ni une malédiction.
     * @return l'enchantement ajouté, ou vide
     */
    public static Optional<Holder<Enchantment>> maybeEnchant(ItemStack stack, int offeringTier, RandomSource random, RegistryAccess registries) {
        if (random.nextDouble() * 100.0 >= RarityOdds.enchantChance(offeringTier)) return Optional.empty();
        List<Holder<Enchantment>> candidates = new ArrayList<>();
        registries.registryOrThrow(Registries.ENCHANTMENT).holders().forEach(holder -> {
            Enchantment enchantment = holder.value();
            if (holder.is(EnchantmentTags.TREASURE) || holder.is(EnchantmentTags.CURSE)) return;
            if (enchantment.getWeight() > 2) return;                       // seulement les rares
            if (!stack.supportsEnchantment(holder)) return;
            if (stack.getTagEnchantments().getLevel(holder) > 0) return;
            if (!EnchantmentHelper.isEnchantmentCompatible(stack.getTagEnchantments().keySet(), holder)) return;
            candidates.add(holder);
        });
        if (candidates.isEmpty()) return Optional.empty();
        Holder<Enchantment> chosen = candidates.get(random.nextInt(candidates.size()));
        stack.enchant(chosen, 1 + random.nextInt(chosen.value().getMaxLevel()));
        return Optional.of(chosen);
    }
}
