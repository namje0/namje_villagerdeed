package com.namje.villagerdeed.menu.custom;

import com.namje.villagerdeed.VillagerDeed;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class VillagerDeedScreen extends AbstractContainerScreen<VillagerDeedMenu> {
    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "textures/gui/villagerdeed/deed_gui.png");

    public VillagerDeedScreen(VillagerDeedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0, 0, imageWidth,
                imageHeight, 256, 256);
    }
}
