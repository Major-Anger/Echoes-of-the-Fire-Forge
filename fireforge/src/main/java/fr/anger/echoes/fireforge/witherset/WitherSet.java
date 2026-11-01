package fr.anger.echoes.fireforge.witherset;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFAttachments;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Le set du Wither (plan, section 13) : l'armure que portaient les Attiseurs quand la maladie les a pris. Celui qui la
 * porte suit le même chemin, palier par palier ; rien ne dit d'où vient la maladie.
 * <p>
 * La progression (0 à 100 %) ne s'affiche pas : on la voit à l'armure qui change, à la cendre qui tombe, à la toux et
 * aux phrases à chaque palier (étape 13a), puis aux créatures (13b). Au palier 4, le set colle à la peau, la cendre
 * s'épaissit, et au bout de 30 minutes, la cendre emporte le porteur : son double se lève avec le set (13c).
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

    /** Le compte à rebours du palier 4 : 30 minutes de jeu, puis la mort (étape 13c). */
    public static final int DOOM_MINUTES = 30, DOOM_TICKS = DOOM_MINUTES * 60 * 20;
    /** Au palier 4, le set colle à la peau : une malédiction, comme celle du lien. */
    public static final ResourceKey<Enchantment> ASH_BOND = ResourceKey.create(Registries.ENCHANTMENT, EchoesFireForge.id("ash_bond"));
    /** La mort par la cendre (« La cendre a emporté … ») : rien ne l'arrête, pas même un totem. */
    public static final ResourceKey<DamageType> ASH = ResourceKey.create(Registries.DAMAGE_TYPE, EchoesFireForge.id("ash"));

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

    /** Les minutes écoulées depuis le palier 4, lues sur les pièces portées (-1 avant). Côté client aussi. */
    public static int visibleDoom(LivingEntity entity) {
        int doom = -1;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (isPiece(stack)) doom = Math.max(doom, stack.getOrDefault(FFDataComponents.WITHER_DOOM, -1));
        }
        return doom;
    }

    /** Côté serveur : la progression du joueur. */
    public static WitherProgress progress(Player player) {
        return player.getData(FFAttachments.WITHER_PROGRESS);
    }

    /** Le palier du joueur (côté serveur). */
    public static int stage(Player player) {
        return progress(player).stage();
    }

    // ------------------------------------------------------------------ le Lien de cendre (palier 4)

    public static Optional<Holder.Reference<Enchantment>> bond(Level level) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(ASH_BOND);
    }

    public static boolean isBound(ItemStack stack) {
        for (Holder<Enchantment> enchantment : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).keySet()) if (enchantment.is(ASH_BOND)) return true;
        return false;
    }

    /** Le set colle (ou ne colle plus) à la peau. */
    public static void setBound(Level level, ItemStack stack, boolean bound) {
        if (bound == isBound(stack)) return;
        if (bound) bond(level).ifPresent(enchantment -> stack.enchant(enchantment, 1));
        else EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.removeIf(e -> e.is(ASH_BOND)));
    }
}
