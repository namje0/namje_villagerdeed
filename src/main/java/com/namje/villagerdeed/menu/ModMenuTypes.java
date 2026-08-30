package com.namje.villagerdeed.menu;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.menu.custom.VillagerDeedMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, VillagerDeed.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VillagerDeedMenu>> VILLAGER_MENU =
            registerMenuType("deed_menu", VillagerDeedMenu::new);

    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>,
            MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
