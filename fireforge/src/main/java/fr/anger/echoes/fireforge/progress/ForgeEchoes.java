package fr.anger.echoes.fireforge.progress;

import fr.anger.echoes.core.echo.Echoes;
import fr.anger.echoes.core.lore.Lore;
import fr.anger.echoes.core.lore.LoreReadEvent;
import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.compat.SiltTablets;
import fr.anger.echoes.fireforge.plan.Plans;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Les succès de la Forge et ses Échos (plan, section 12a). Certains succès laissent un Écho : un petit bonus
 * permanent lié aux systèmes de la Forge, jamais aux dégâts ni à la vie (comme dans le Silt). Le mécanisme
 * (message, sons) est dans Echoes Core ; ici, les noms, les compteurs et le moment où chaque succès est accordé.
 * <p>
 * Deux Échos se voient dans l'interface de la forge (les chances) : à leur éveil, ils posent aussi une marque
 * dans le lore du joueur, que le client connaît ({@link #HAND_MARK}, {@link #NOTEBOOKS_MARK}).
 */
@EventBusSubscriber(modid = EchoesFireForge.MOD_ID)
public final class ForgeEchoes {

    private ForgeEchoes() {}

    // ---- les succès accordés par le code
    public static final String FIRST_REFORGE = "first_reforge", WORKBENCH = "workbench", DROWNED_IN_ASH = "drowned_in_ash";
    // ---- ceux qui laissent un Écho
    public static final String HAND = "hand", BREATH = "breath", NOTEBOOKS = "notebooks", STEAM = "steam";
    private static final List<String> WITH_ECHO = List.of(HAND, BREATH, NOTEBOOKS, STEAM);

    /** Le souffle juste : 10 fêlures rattrapées au soufflet. Vapeur domptée : 200 charges rechargées. */
    public static final int BREATH_SAVES = 10, STEAM_CHARGES = 200;
    /** Main d'Attiseur : un plan de plus ; Le souffle juste : +5 ticks pour réagir ; Carnets complets : +2 % d'enchantement rare. */
    public static final int HAND_PLANS = 1, BREATH_WINDOW = 5;
    public static final double NOTEBOOKS_ENCHANT = 2.0;

    /** Les marques de lore des Échos visibles côté client (hors de l'espace « echoes_fireforge » : elles ne révèlent pas les Carnets). */
    public static final ResourceLocation HAND_MARK = ResourceLocation.fromNamespaceAndPath("echoes_fireforge_echo", "hand");
    public static final ResourceLocation NOTEBOOKS_MARK = ResourceLocation.fromNamespaceAndPath("echoes_fireforge_echo", "notebooks");

    private static final String SAVES_KEY = EchoesFireForge.MOD_ID + ".bellows_saves";
    private static final String CHARGES_KEY = EchoesFireForge.MOD_ID + ".steam_charges";
    private static final String SEEN_CENDREUX_KEY = EchoesFireForge.MOD_ID + ".seen_cendreux";
    /** Les noyés de cendre : avec le Silt, il faut aussi avoir lu la Tablette XIII des Veilleurs. */
    public static final int ASH_TABLET = 13;

    public static ResourceLocation id(String name) {
        return EchoesFireForge.id("echoes/" + name);
    }

    /** Enregistre les Échos de la Forge dans le Core (au démarrage du mod). Texte : « echo.echoes_fireforge.<nom> ». */
    public static void register() {
        for (String name : WITH_ECHO) {
            Echoes.register(id(name), "echo." + EchoesFireForge.MOD_ID + "." + name, switch (name) {
                case HAND -> player -> Lore.markRead(player, HAND_MARK);
                case NOTEBOOKS -> player -> Lore.markRead(player, NOTEBOOKS_MARK);
                default -> player -> {};
            });
        }
    }

    public static void grant(ServerPlayer player, String name) {
        Echoes.grant(player, id(name));
    }

    /** Côté serveur seulement. */
    public static boolean has(Player player, String name) {
        return Echoes.has(player, id(name));
    }

    // ------------------------------------------------------------------ les moments

    /** Une reforge vient de finir pour ce forgeron. */
    public static void reforged(ServerPlayer smith, boolean special) {
        grant(smith, FIRST_REFORGE);
        if (special) grant(smith, HAND);
    }

    /** Une fêlure rattrapée au soufflet. */
    public static void bellowsSaved(ServerPlayer player) {
        int saves = player.getPersistentData().getInt(SAVES_KEY) + 1;
        player.getPersistentData().putInt(SAVES_KEY, saves);
        if (saves >= BREATH_SAVES) grant(player, BREATH);
    }

    /** Une charge de vapeur rechargée au bouilleur, pour ce joueur. */
    public static void steamCharged(ServerPlayer player) {
        int charges = player.getPersistentData().getInt(CHARGES_KEY) + 1;
        player.getPersistentData().putInt(CHARGES_KEY, charges);
        if (charges >= STEAM_CHARGES) grant(player, STEAM);
    }

    /** Le joueur a vu un Cendreux (étape 12c). */
    public static void cendreuxSeen(ServerPlayer player) {
        if (has(player, DROWNED_IN_ASH)) return;
        player.getPersistentData().putBoolean(SEEN_CENDREUX_KEY, true);
        tryDrownedInAsh(player);
    }

    /** Les noyés de cendre : avoir vu un Cendreux ; avec le Silt, avoir aussi lu la Tablette XIII. */
    private static void tryDrownedInAsh(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(SEEN_CENDREUX_KEY)) return;
        if (SiltTablets.installed() && !SiltTablets.hasRead(player, ASH_TABLET)) return;
        grant(player, DROWNED_IN_ASH);
    }

    /** La tablette peut être lue après avoir vu le Cendreux : on regarde de temps en temps. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 100 == 0
                && player.getPersistentData().getBoolean(SEEN_CENDREUX_KEY) && !has(player, DROWNED_IN_ASH)) {
            tryDrownedInAsh(player);
        }
    }

    /** Carnets complets : tous les plans lus (la Forge, l'Établi, et les Plans d'Attiseur I à V). */
    @SubscribeEvent
    public static void onLoreRead(LoreReadEvent event) {
        if (!event.getEntry().getNamespace().equals(EchoesFireForge.MOD_ID)) return;
        ServerPlayer player = event.getPlayer();
        if (!Lore.hasRead(player, Plans.FORGE) || !Lore.hasRead(player, Plans.WORKBENCH)) return;
        for (int n = 1; n <= Plans.COUNT; n++) if (!Lore.hasRead(player, Plans.attiseur(n))) return;
        grant(player, NOTEBOOKS);
    }

    /** Les compteurs (et « a vu un Cendreux ») survivent à la mort. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        for (String key : new String[]{SAVES_KEY, CHARGES_KEY}) {
            int value = event.getOriginal().getPersistentData().getInt(key);
            if (value > 0) event.getEntity().getPersistentData().putInt(key, value);
        }
        if (event.getOriginal().getPersistentData().getBoolean(SEEN_CENDREUX_KEY)) event.getEntity().getPersistentData().putBoolean(SEEN_CENDREUX_KEY, true);
    }
}
