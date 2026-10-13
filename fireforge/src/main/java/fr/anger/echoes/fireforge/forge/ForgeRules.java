package fr.anger.echoes.fireforge.forge;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Ce que la forge accepte, et les quantités. Les listes d'objets sont des tags : un pack de mods peut les changer. */
public final class ForgeRules {

    private ForgeRules() {}

    /** Armes et armures qu'on peut reforger (épées, haches, armures ; les objets particuliers viendront au cas par cas). */
    public static final TagKey<Item> REFORGEABLE = tag("reforgeable");
    /** Offrandes, du petit au gros coup de pouce. */
    public static final TagKey<Item> OFFERING_SMALL = tag("offering/small");
    public static final TagKey<Item> OFFERING_GOOD = tag("offering/good");
    public static final TagKey<Item> OFFERING_LARGE = tag("offering/large");

    /** Eau : la forge seule garde 1 seau ; une reforge en consomme une fiole. */
    public static final int BASE_WATER_CAPACITY = 1000;
    public static final int BUCKET = 1000;
    public static final int BOTTLE = 250;
    public static final int WATER_PER_REFORGE = 250;
    /** Le foyer seul garde 1 seau de lave ; une reforge à la Fournaise en brûle un quart (4 reforges par seau). */
    public static final int BASE_LAVA_CAPACITY = 1000;
    public static final int LAVA_PER_REFORGE = 250;
    /** Modules (étape 5) : la chaudière ajoute 4 seaux d'eau par bloc (3 blocs), le réservoir 5 seaux de lave (2 blocs). */
    public static final int BOILER_WATER_PER_BLOCK = 4000, BOILER_MAX_HEIGHT = 3;
    public static final int LAVA_TANK_PER_BLOCK = 5000, LAVA_TANK_MAX_HEIGHT = 2;
    /** Durée d'une reforge : 10 secondes. */
    public static final int REFORGE_TICKS = 200;

    /** Fêlure (section 5 du plan) : chaque craquement laisse 1,5 s pour actionner le soufflet. */
    public static final int CRACK_WINDOW = 30;
    /** Les craquements tombent entre la 2e et la 8e seconde, au moins 2 s l'un après l'autre. */
    public static final int CRACK_FIRST = 40, CRACK_LAST = 160, CRACK_SPACING = 40;
    /** Le soufflet souffle de la vapeur : un coup qui ne rattrape rien coûte un peu d'eau. */
    public static final int BELLOWS_WATER = 50;
    /** Le temps de regonfler le soufflet entre deux coups. */
    public static final int BELLOWS_COOLDOWN = 6;

    /** Étape 5b, avec le module des âmes : prix en âmes du feu des âmes et des options du feu normal. */
    public static final int SOUL_FIRE_COST = 10, LOCK_COST = 15, GUARANTEE_COST = 20, REROLL_COST = 10;

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, EchoesFireForge.id(path));
    }

    public static boolean isReforgeable(ItemStack stack) {
        return stack.is(REFORGEABLE);
    }

    /** 0 = pas une offrande, 1 = petit, 2 = bon, 3 = gros coup de pouce. */
    public static int offeringTier(ItemStack stack) {
        if (stack.is(OFFERING_LARGE)) return 3;
        if (stack.is(OFFERING_GOOD)) return 2;
        if (stack.is(OFFERING_SMALL)) return 1;
        return 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return Heat.ofFuel(stack) != Heat.NONE;
    }

    /** Quantité d'eau apportée (0 si ce n'est pas de l'eau). */
    public static int waterAmount(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) return BUCKET;
        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) return BOTTLE;
        }
        return 0;
    }

    /** Le récipient vide rendu après avoir versé l'eau ou la lave. */
    public static ItemStack emptyContainer(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET)) return new ItemStack(Items.BUCKET);
        if (stack.is(Items.POTION)) return new ItemStack(Items.GLASS_BOTTLE);
        return ItemStack.EMPTY;
    }
}
