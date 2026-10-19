package fr.anger.echoes.fireforge.forging;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Les statistiques que la forge peut modifier, en pourcentage de la valeur propre de l'objet.
 * reference = valeur de base du joueur ajoutée à celle de l'objet (dégâts : 1, vitesse d'attaque : 4),
 * ou valeur de référence quand l'objet n'en a pas (portée : 3 blocs, recul : 1).
 */
public enum ForgeStat implements StringRepresentable {
    // armes
    DAMAGE("damage", Attributes.ATTACK_DAMAGE, 1.0, 0.0, true),
    ATTACK_SPEED("attack_speed", Attributes.ATTACK_SPEED, 4.0, 0.0, true),
    REACH("reach", Attributes.ENTITY_INTERACTION_RANGE, 3.0, 0.0, false),
    KNOCKBACK("knockback", Attributes.ATTACK_KNOCKBACK, 0.0, 1.0, true),
    // armures
    ARMOR("armor", Attributes.ARMOR, 0.0, 0.0, true),
    TOUGHNESS("toughness", Attributes.ARMOR_TOUGHNESS, 0.0, 1.0, true),
    KNOCKBACK_RESISTANCE("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE, 0.0, 1.0, true),
    MOVE_SPEED("move_speed", Attributes.MOVEMENT_SPEED, 0.0, 0.0, false),
    FIRE_RESISTANCE("fire_resistance", null, 0.0, 0.0, false), // pas d'attribut : appliqué aux dégâts de feu et de lave
    // arc et arbalète (étape 6c) : pas d'attribut, appliqués aux flèches tirées et au temps de tension (ForgingEvents)
    ARROW_DAMAGE("arrow_damage", null, 0.0, 0.0, false),
    DRAW_SPEED("draw_speed", null, 0.0, 0.0, false),
    RELOAD_SPEED("reload_speed", null, 0.0, 0.0, false),
    ARROW_SPEED("arrow_speed", null, 0.0, 0.0, false);

    public static final Codec<ForgeStat> CODEC = StringRepresentable.fromEnum(ForgeStat::values);
    public static final ForgeStat[] WEAPON = {DAMAGE, ATTACK_SPEED, REACH, KNOCKBACK};
    public static final ForgeStat[] ARMOR_STATS = {ARMOR, TOUGHNESS, KNOCKBACK_RESISTANCE, MOVE_SPEED, FIRE_RESISTANCE};
    /** Bouclier, actif dans la main secondaire. */
    public static final ForgeStat[] SHIELD = {TOUGHNESS, KNOCKBACK_RESISTANCE, MOVE_SPEED};
    public static final ForgeStat[] BOW = {ARROW_DAMAGE, DRAW_SPEED, ARROW_SPEED};
    public static final ForgeStat[] CROSSBOW = {ARROW_DAMAGE, RELOAD_SPEED, ARROW_SPEED};

    /** Les statistiques possibles pour cet objet : armure, bouclier, arc, arbalète ou arme de corps à corps (trident et masse compris). */
    public static ForgeStat[] poolFor(ItemStack stack) {
        EquipmentSlot slot = Forging.slotOf(stack);
        if (slot.isArmor()) return ARMOR_STATS;
        if (slot == EquipmentSlot.OFFHAND) return SHIELD;
        WeaponType type = WeaponType.of(stack);
        return type == WeaponType.BOW ? BOW : type == WeaponType.CROSSBOW ? CROSSBOW : WEAPON;
    }

    private final String key;
    @Nullable
    private final Holder<Attribute> attribute;
    /** Valeur de base du joueur, ajoutée à celle de l'objet (ex. : dégâts à mains nues). */
    private final double playerBase;
    /** Valeur minimale prise comme référence quand l'objet en a peu ou pas (ex. : recul). */
    private final double minimumReference;
    /** true : on ajoute un pourcentage de la valeur de l'objet ; false : traité à part (portée, vitesse, feu). */
    private final boolean fromItemValue;

    ForgeStat(String key, @Nullable Holder<Attribute> attribute, double playerBase, double minimumReference, boolean fromItemValue) {
        this.key = key;
        this.attribute = attribute;
        this.playerBase = playerBase;
        this.minimumReference = minimumReference;
        this.fromItemValue = fromItemValue;
    }

    @Nullable
    public Holder<Attribute> attribute() {
        return attribute;
    }

    /** L'opération de l'attribut ajouté : la vitesse de déplacement se multiplie, le reste s'additionne. */
    public AttributeModifier.Operation operation() {
        return this == MOVE_SPEED ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE;
    }

    /**
     * Montant de l'attribut ajouté pour un pourcentage donné.
     * @param itemValue la valeur que l'objet donne déjà à cet attribut (somme de ses modificateurs)
     */
    public double amount(double percent, double itemValue) {
        double fraction = percent / 100.0;
        if (this == MOVE_SPEED) return fraction;                    // déjà un pourcentage de la vitesse de base
        if (this == REACH) return playerBase * fraction;             // portée de base du joueur : 3 blocs
        double reference = fromItemValue ? Math.max(playerBase + itemValue, minimumReference) : playerBase;
        return reference * fraction;
    }

    public Component displayName() {
        return Component.translatable("stat.echoes_fireforge." + key);
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
