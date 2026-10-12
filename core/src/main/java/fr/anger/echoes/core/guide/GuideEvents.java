package fr.anger.echoes.core.guide;

import fr.anger.echoes.core.CoreItems;
import fr.anger.echoes.core.EchoesCore;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Donne le livre guide une seule fois, à la première connexion du joueur (quel que soit le nombre d'épisodes). */
@EventBusSubscriber(modid = EchoesCore.MOD_ID)
public final class GuideEvents {

    private static final String RECEIVED = "echoes_core.received_guide";

    private GuideEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.getPersistentData().getBoolean(RECEIVED)) return;
        player.getPersistentData().putBoolean(RECEIVED, true);
        ItemStack book = new ItemStack(CoreItems.GUIDE_BOOK.get());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }

    // On garde l'information après une mort (le joueur est recréé)
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(RECEIVED)) {
            event.getEntity().getPersistentData().putBoolean(RECEIVED, true);
        }
    }

    @EventBusSubscriber(modid = EchoesCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Tabs {
        private Tabs() {}

        /** Le Core n'a pas d'onglet : le livre va dans « Outils et utilitaires ». Les épisodes peuvent aussi l'ajouter au leur. */
        @SubscribeEvent
        public static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(CoreItems.GUIDE_BOOK.get());
        }
    }
}
