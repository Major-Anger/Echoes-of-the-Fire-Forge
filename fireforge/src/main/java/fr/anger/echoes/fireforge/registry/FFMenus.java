package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.forge.FireForgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FFMenus {

    private FFMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EchoesFireForge.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FireForgeMenu>> FIRE_FORGE =
            MENUS.register("fire_forge", () -> IMenuTypeExtension.create(FireForgeMenu::new));
}
