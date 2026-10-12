package fr.anger.echoes.fireforge.forging;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.registry.FFDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Applique les statistiques de la forge aux objets portés. */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class ForgingEvents {

    private ForgingEvents() {}

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
            event.addModifier(attribute, new AttributeModifier(
                    EchoesFireForge.id("forging/" + roll.stat().getSerializedName()), amount, roll.stat().operation()), group);
        }
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
