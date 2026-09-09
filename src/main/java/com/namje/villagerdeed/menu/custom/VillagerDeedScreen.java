package com.namje.villagerdeed.menu.custom;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.networking.packet.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Collections;
import java.util.List;

public class VillagerDeedScreen extends Screen {
    private static final int STATE_TEXT_MAX_WIDTH = 100;
    private static final int MAX_VISIBLE_LINES = 3;
    private List<FormattedCharSequence> cachedStateLines = Collections.emptyList();
    private int cachedDeedState = -1;

    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "textures/gui/villagerdeed/deed_gui.png");

    private static final Identifier BUTTON_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "widget/button_highlighted");
    private static final Identifier BUTTON_DISABLED_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "widget/button_disabled");
    private static final Identifier BUTTON_SELECTED_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "widget/button_selected");

    private static final Identifier TOGGLE_DEED_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/toggledeedbutton");
    private static final Identifier EVICT_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/evicttenantbutton");
    private static final Identifier SUMMON_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/summontenantbutton");
    private static final Identifier SHUFFLE_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/shuffletradesbutton");
    private static final Identifier EDIT_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/writebutton");

    private static final Component STAT_LABEL = Component.translatable("gui.villagerdeed.stat_label");

    private final int imageWidth = 176;
    private final int imageHeight = 147;

    private EditBox deedNameEdit;
    private EditBox tenantNameEdit;

    private ToggleActiveButton toggleActiveButton;
    private EvictButton evictButton;
    private SummonButton summonButton;
    private ShuffleTradesButton shuffleTradesButton;

    private ConfirmDeedNameButton confirmDeedNameButton;
    private ConfirmTenantNameButton confirmTenantNameButton;

    private CycleButton<String> professionButton;

    private final VillagerDeedBlockEntity blockEntity;

    public VillagerDeedScreen(Component title, VillagerDeedBlockEntity blockEntity) {
        super(title);
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.tenantNameEdit = new EditBox(this.font, x + 63, y + 25, 88, 16,
                Component.translatable("gui.villagerdeed.tenant_name"));
        this.tenantNameEdit.setMaxLength(50);
        String tenantName = this.blockEntity.getTenantName();
        this.tenantNameEdit.setValue(tenantName != null ? tenantName : "");
        this.addRenderableWidget(this.tenantNameEdit);

        this.toggleActiveButton = this.addRenderableWidget(new ToggleActiveButton(x + 12, y + 85));
        this.evictButton = this.addRenderableWidget(new EvictButton(x + 40, y + 85));
        this.summonButton = this.addRenderableWidget(new SummonButton(x + 58, y + 85));
        this.shuffleTradesButton = this.addRenderableWidget(new ShuffleTradesButton(x + 76, y + 85));
        this.confirmDeedNameButton = this.addRenderableWidget(new ConfirmDeedNameButton(x + 151, y + 9));
        this.confirmTenantNameButton = this.addRenderableWidget(new ConfirmTenantNameButton(x + 151, y + 25));

        this.deedNameEdit = new EditBox(this.font, x + 9, y + 9, 142, 16,
                Component.translatable("gui.villagerdeed.deed_name"));
        this.deedNameEdit.setMaxLength(20);
        String deedName = this.blockEntity.getDeedName();
        this.deedNameEdit.setValue(deedName != null ? deedName : "");
        this.addRenderableWidget(this.deedNameEdit);

        this.professionButton = this.addRenderableWidget(
                CycleButton.builder(
                                Component::literal,
                                "None"
                        )
                        .withValues(List.of("None"))
                        .displayOnlyValue()
                        .create(x + 9, y + 118, 77, 20, Component.translatable("gui.villagerdeed.button.profession"), (button, value) -> {
                            // Deferred profession specification
                        })
        );
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0, 0, this.imageWidth,
                this.imageHeight, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int currentState = this.blockEntity.getDeedState();

        if (currentState == 2) {
            Level level = this.minecraft.level;
            Villager tenant = (level != null) ? this.blockEntity.getTenantEntity(level) : null;

            Component stateComponent;
            if (tenant != null) {
                String health = String.format("%.0f/%.0f", tenant.getHealth(), tenant.getMaxHealth());

                stateComponent = Component.translatable("gui.villagerdeed.state.tenantData", health);
            } else {
                stateComponent = Component.translatable("gui.villagerdeed.state.invalid");
            }

            this.cachedStateLines = this.font.split(stateComponent, STATE_TEXT_MAX_WIDTH);
            this.cachedDeedState = currentState;

        } else if (this.cachedDeedState != currentState) {
            String messageKey = switch (currentState) {
                case 0 -> "no_bed";
                case 1 -> "waiting";
                case 3 -> "respawning";
                case 4 -> "disabled";
                default -> "invalid";
            };

            Component stateComponent = Component.translatable("gui.villagerdeed.state." + messageKey);
            this.cachedStateLines = this.font.split(stateComponent, STATE_TEXT_MAX_WIDTH);
            this.cachedDeedState = currentState;
        }

        int linesToDraw = Math.min(MAX_VISIBLE_LINES, this.cachedStateLines.size());
        for (int i = 0; i < linesToDraw; i++) {
            FormattedCharSequence line = this.cachedStateLines.get(i);
            int lineY = y + 44 + (i * this.font.lineHeight);
            graphics.text(this.font, line, x + 65, lineY, 0xFF404040, false);
        }

        graphics.text(this.font, Component.translatable("gui.villagerdeed.button.profession"), x + 10, y + 107, 0xFF404040, false);
    }

    private abstract static class DeedScreenButton extends AbstractButton {
        private boolean selected;

        protected DeedScreenButton(int x, int y, int width, int height, Component label) {
            super(x, y, width, height, label);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier sprite;
            if (!this.active) {
                sprite = BUTTON_DISABLED_SPRITE;
            } else if (this.selected) {
                sprite = BUTTON_SELECTED_SPRITE;
            } else if (this.isHoveredOrFocused()) {
                sprite = BUTTON_HIGHLIGHTED_SPRITE;
            } else {
                sprite = BUTTON_SPRITE;
            }

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
            this.extractIcon(graphics);
        }

        protected abstract void extractIcon(GuiGraphicsExtractor graphics);

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private abstract static class DeedSpriteScreenButton extends DeedScreenButton {
        private final Identifier sprite;

        protected DeedSpriteScreenButton(int x, int y, int width, int height, Identifier sprite, Component label) {
            super(x, y, width, height, label);
            this.setTooltip(Tooltip.create(label));
            this.sprite = sprite;
        }

        @Override
        protected void extractIcon(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.width, this.height);
        }
    }

    private class ToggleActiveButton extends DeedSpriteScreenButton {
        public ToggleActiveButton(int x, int y) {
            super(x, y, 26, 16, TOGGLE_DEED_TEXTURE, Component.translatable("gui.villagerdeed.button.toggle_active"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new ToggleDeedPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }
    }

    private class EvictButton extends DeedSpriteScreenButton {
        public EvictButton(int x, int y) {
            super(x, y, 16, 16, EVICT_TEXTURE, Component.translatable("gui.villagerdeed.button.evict"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new EvictTenantPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }
    }

    private class SummonButton extends DeedSpriteScreenButton {
        public SummonButton(int x, int y) {
            super(x, y, 16, 16, SUMMON_TEXTURE, Component.translatable("gui.villagerdeed.button.summon"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new SummonTenantPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }
    }

    private class ShuffleTradesButton extends DeedSpriteScreenButton {
        public ShuffleTradesButton(int x, int y) {
            super(x, y, 16, 16, SHUFFLE_TEXTURE, Component.translatable("gui.villagerdeed.button.shuffle"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            // Packet transmission: Evict tenant & flush entity reference
        }
    }

    private class ConfirmDeedNameButton extends DeedSpriteScreenButton {
        public ConfirmDeedNameButton(int x, int y) {
            super(x, y, 16, 16, EDIT_TEXTURE, Component.translatable("gui.villagerdeed.button.confirm_deed"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new ChangeDeedNamePacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos(), deedNameEdit.getValue()));
        }
    }

    private class ConfirmTenantNameButton extends DeedSpriteScreenButton {
        public ConfirmTenantNameButton(int x, int y) {
            super(x, y, 16, 16, EDIT_TEXTURE, Component.translatable("gui.villagerdeed.button.confirm_tenant"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new ChangeTenantNamePacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos(), tenantNameEdit.getValue()));
        }
    }
}