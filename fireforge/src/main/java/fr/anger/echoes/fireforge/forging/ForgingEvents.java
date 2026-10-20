package fr.anger.echoes.fireforge.forging;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** Applique les statistiques de la forge aux objets portés. */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class ForgingEvents {

    private ForgingEvents() {}

    /** Marque d'une flèche déjà modifiée par son arc (rechargée du disque ou passée par un portail : rien à refaire). */
    private static final String FORGED_ARROW = EchoesFireForge.MOD_ID + ":forged_arrow";
    /** Le reste de tick de tension gagné ou perdu, par tireur ; une table par côté (client, serveur), chacune sur son fil. */
    private static final Map<LivingEntity, Float> CLIENT_DRAW = new WeakHashMap<>(), SERVER_DRAW = new WeakHashMap<>();

    /** Les bonus et malus deviennent de vrais modificateurs d'attributs, actifs quand l'objet est tenu ou porté. */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        ForgingData data = stack.get(FFDataComponents.FORGING);
        if (data == null || data.stats().isEmpty()) return;
        EquipmentSlot slot = Forging.slotOf(stack);
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
        for (ForgingData.StatRoll roll : data.stats()) {
            Holder<Attribute> attribute = roll.stat().attribute();
            if (attribute == null) continue; // résistance au feu : voir onDamage
            double amount = roll.stat().amount(roll.percent(), itemValue(event.getDefaultModifiers(), attribute));
            event.addModifier(attribute, new AttributeModifier(modifierId(roll.stat()), amount, roll.stat().operation()), group);
        }
    }

    /** L'identifiant du modificateur d'attribut posé par la forge pour cette statistique. */
    public static ResourceLocation modifierId(ForgeStat stat) {
        return EchoesFireForge.id("forging/" + stat.getSerializedName());
    }

    /** Ce que l'objet donne déjà à cet attribut (ses modificateurs d'origine, armures comprises), base du pourcentage. */
    private static double itemValue(ItemAttributeModifiers defaults, Holder<Attribute> attribute) {
        double total = 0;
        for (ItemAttributeModifiers.Entry entry : defaults.modifiers()) {
            if (entry.attribute().equals(attribute) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                total += entry.modifier().amount();
            }
        }
        return total;
    }

    /**
     * Arc et arbalète : les flèches tirées par l'arme prennent ses dégâts et sa vitesse. Les dégâts d'une flèche suivent
     * aussi sa vitesse : on les compense, pour que la vitesse des flèches porte plus loin et plus droit sans frapper plus fort.
     */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AbstractArrow arrow) || arrow.getWeaponItem() == null) return;
        ForgingData data = arrow.getWeaponItem().get(FFDataComponents.FORGING);
        if (data == null) return;
        float damage = data.percent(ForgeStat.ARROW_DAMAGE), speed = data.percent(ForgeStat.ARROW_SPEED);
        if (damage == 0 && speed == 0) return;
        CompoundTag persistent = arrow.getPersistentData();
        if (persistent.getBoolean(FORGED_ARROW)) return;
        persistent.putBoolean(FORGED_ARROW, true);
        double speedFactor = 1 + speed / 100.0;
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(speedFactor));
        arrow.setBaseDamage(arrow.getBaseDamage() * (1 + damage / 100.0) / speedFactor);
    }

    /**
     * Vitesse de tension (arc) et de rechargement (arbalète) : le temps d'utilisation avance plus vite ou plus lentement.
     * À +5 %, un tick sur vingt compte double ; à −5 %, un tick sur vingt ne compte pas. Le client du tireur fait le même
     * calcul que le serveur, pour que l'arc se tende à l'écran comme en jeu.
     */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        ForgingData data = event.getItem().get(FFDataComponents.FORGING);
        if (data == null) return;
        float percent = data.percent(ForgeStat.DRAW_SPEED) + data.percent(ForgeStat.RELOAD_SPEED)
                + SoulSpecialEffects.reloadPenalty(event.getItem()); // Carreau muet : rechargement plus lent
        if (percent == 0) return;
        LivingEntity user = event.getEntity();
        Map<LivingEntity, Float> carry = user.level().isClientSide ? CLIENT_DRAW : SERVER_DRAW;
        float total = carry.getOrDefault(user, 0F) + Math.abs(percent) / 100F;
        if (total >= 1) {
            total -= 1;
            event.setDuration(percent > 0 ? Math.max(1, event.getDuration() - 1) : event.getDuration() + 1);
        }
        carry.put(user, total);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        LivingEntity user = event.getEntity();
        (user.level().isClientSide ? CLIENT_DRAW : SERVER_DRAW).remove(user);
    }

    /** Résistance au feu et à la lave des armures : réduit (ou augmente, si c'est un malus) ces dégâts. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) return;
        float percent = 0;
        for (ItemStack armor : event.getEntity().getArmorSlots()) {
            ForgingData data = armor.get(FFDataComponents.FORGING);
            if (data != null) percent += data.percent(ForgeStat.FIRE_RESISTANCE);
        }
        if (percent != 0) event.setAmount(event.getAmount() * (1 - Math.min(percent, 80f) / 100f));
    }
}
