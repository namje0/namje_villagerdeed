package com.namje.villagerdeed.menu.custom;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.networking.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

public class VillagerDeedScreen extends Screen {
    private static final int STATE_TEXT_MAX_WIDTH = 100;
    private static final int MAX_VISIBLE_LINES = 3;
    private List<FormattedCharSequence> cachedStateLines = Collections.emptyList();
    private int cachedDeedState = -1;
    private String cachedDeedName = null;
    private String cachedTenantName = null;

    private static final Vector3fc VILLAGER_TRANSLATION = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Quaternionfc VILLAGER_ANGLE = new Quaternionf().rotationXYZ(0.2F, 0.0F, (float) Math.PI);
    private static final float VILLAGER_SCALE = 24.0F;

    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "textures/gui/villagerdeed/deed_gui.png");

    private static final Identifier BUTTON_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/button");
    private static final Identifier BUTTON_HIGHLIGHTED_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/button_highlighted");
    private static final Identifier BUTTON_DISABLED_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/button_disabled");

    private static final Identifier EVICT_ICON_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/evicttenantbutton");
    private static final Identifier SUMMON_ICON_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/summontenantbutton");
    private static final Identifier SWAP_PROFESSION_ICON_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/swapprofessionsbutton");
    private static final Identifier EDIT_ICON_SPRITE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/writebutton");

    private static final Identifier TOGGLE_DEED_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/toggledeedbutton");
    private static final Identifier TOGGLE_DEED_TEXTURE_HIGHLIGHT =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/toggledeedbutton_highlighted");
    private static final Identifier TOGGLE_DEED_TEXTURE_OFF =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/toggledeedbutton_off");
    private static final Identifier TOGGLE_DEED_TEXTURE_OFF_HIGHLIGHT =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/toggledeedbutton_off_highlighted");
    private static final Identifier TOGGLE_DEED_DISABLED =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "villagerdeed/toggledeedbutton_disabled");


    private final int imageWidth = 176;
    private final int imageHeight = 113;

    private EditBox deedNameEdit;
    private EditBox tenantNameEdit;

    private ToggleActiveButton toggleActiveButton;
    private EvictButton evictButton;
    private SummonButton summonButton;
    private SwapProfessionButton swapProfessionButton;

    private ConfirmDeedNameButton confirmDeedNameButton;
    private ConfirmTenantNameButton confirmTenantNameButton;

    private CycleButton<ResourceKey<VillagerProfession>> professionButton;

    private final VillagerDeedBlockEntity blockEntity;
    private final VillagerRenderState villagerRenderState = new VillagerRenderState();

    public VillagerDeedScreen(Component title, VillagerDeedBlockEntity blockEntity) {
        super(title);
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.cachedDeedName = this.blockEntity.getDeedName();
        this.cachedTenantName = this.blockEntity.getTenantName();

        this.tenantNameEdit = new EditBox(this.font, x + 61, y + 23, 88, 16,
                Component.translatable("gui.villagerdeed.tenant_name"));
        this.tenantNameEdit.setMaxLength(50);
        String tenantName = this.blockEntity.getTenantName();
        this.tenantNameEdit.setValue(tenantName != null ? tenantName : "");
        this.tenantNameEdit.setHint(Component.translatable("gui.villagerdeed.tenant_name"));
        this.addRenderableWidget(this.tenantNameEdit);

        this.toggleActiveButton = this.addRenderableWidget(new ToggleActiveButton(x + 10, y + 83));
        this.evictButton = this.addRenderableWidget(new EvictButton(x + 38, y + 83));
        this.summonButton = this.addRenderableWidget(new SummonButton(x + 56, y + 83));
        this.swapProfessionButton = this.addRenderableWidget(new SwapProfessionButton(x + 74, y + 83));
        this.confirmDeedNameButton = this.addRenderableWidget(new ConfirmDeedNameButton(x + 149, y + 7));
        this.confirmTenantNameButton = this.addRenderableWidget(new ConfirmTenantNameButton(x + 149, y + 23));

        this.deedNameEdit = new EditBox(this.font, x + 7, y + 7, 142, 16,
                Component.translatable("gui.villagerdeed.deed_name"));
        this.deedNameEdit.setMaxLength(20);
        String deedName = this.blockEntity.getDeedName();
        this.deedNameEdit.setValue(deedName != null ? deedName : "");
        this.deedNameEdit.setHint(Component.translatable("gui.villagerdeed.deed_name"));
        this.addRenderableWidget(this.deedNameEdit);

        this.updateWidgetStates();
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
    public void tick() {
        super.tick();
        this.updateWidgetStates();
    }

    private void updateWidgetStates() {
        String currentDeedName = this.blockEntity.getDeedName();
        if (!Objects.equals(currentDeedName, this.cachedDeedName)) {
            this.cachedDeedName = currentDeedName;
            if (this.deedNameEdit != null && !this.deedNameEdit.isFocused()) {
                this.deedNameEdit.setValue(currentDeedName != null ? currentDeedName : "");
            }
        }

        String currentTenantName = this.blockEntity.getTenantName();
        if (!Objects.equals(currentTenantName, this.cachedTenantName)) {
            this.cachedTenantName = currentTenantName;
            if (this.tenantNameEdit != null && !this.tenantNameEdit.isFocused()) {
                this.tenantNameEdit.setValue(currentTenantName != null ? currentTenantName : "");
            }
        }

        int state = this.blockEntity.getDeedState();

        if (this.toggleActiveButton != null) {
            this.toggleActiveButton.active = (state == 0 || state == 1 || state == 4);
        }

        boolean isState2 = (state == 2);
        if (this.summonButton != null) {
            this.summonButton.active = isState2;
        }
        if (this.swapProfessionButton != null) {
            this.swapProfessionButton.active = isState2;
        }

        boolean isState2Or3 = (state == 2 || state == 3);
        if (this.evictButton != null) {
            this.evictButton.active = isState2Or3;
        }
        if (this.confirmTenantNameButton != null) {
            this.confirmTenantNameButton.active = isState2Or3;
        }
        if (this.tenantNameEdit != null) {
            this.tenantNameEdit.active = isState2Or3;
            this.tenantNameEdit.setEditable(isState2Or3);
        }
    }

    private void updateVillagerStateFromEntity(Villager villager, float partialTick, int mouseX, int mouseY, int guiLeft, int guiTop) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (dispatcher.getRenderer(villager) instanceof VillagerRenderer renderer) {
            renderer.extractRenderState(villager, this.villagerRenderState, partialTick);

            float entityCenterX = guiLeft + 30.0F;
            float entityCenterY = guiTop + 35.0F;

            float deltaX = entityCenterX - mouseX;
            float deltaY = entityCenterY - mouseY;

            float yawOffset = (float) Math.atan(deltaX / 40.0F) * 20.0F;
            float pitchOffset = (float) Math.atan(deltaY / 40.0F) * 20.0F;

            this.villagerRenderState.bodyRot = 200.0F;

            this.villagerRenderState.yRot = Math.clamp(yawOffset, -45.0F, 45.0F);
            this.villagerRenderState.xRot = Math.clamp(-pitchOffset, -30.0F, 30.0F);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0, 0, this.imageWidth,
                this.imageHeight, 256, 256);

        if (this.blockEntity.getDeedState() == 2) {
            Level level = this.minecraft.level;
            Villager tenant = (level != null) ? this.blockEntity.getTenantEntity(level) : null;
            if (tenant != null) {
                this.updateVillagerStateFromEntity(tenant, a, mouseX, mouseY, x, y);

                int x0 = x + 8;
                int y0 = y + 24;
                int x1 = x + 59;
                int y1 = y + 75;

                graphics.entity(
                        this.villagerRenderState,
                        VILLAGER_SCALE,
                        VILLAGER_TRANSLATION,
                        VILLAGER_ANGLE,
                        null,
                        x0, y0, x1, y1
                );
            }
        }
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
            int lineY = y + 42 + (i * this.font.lineHeight);
            graphics.text(this.font, line, x + 63, lineY, 0xFF404040, false);
        }
    }

    private abstract static class DeedIconButton extends AbstractButton {
        private final Identifier iconSprite;

        protected DeedIconButton(int x, int y, int width, int height, Identifier iconSprite, Component label) {
            super(x, y, width, height, label);
            this.setTooltip(Tooltip.create(label));
            this.iconSprite = iconSprite;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier sprite;
            if (!this.active) {
                sprite = BUTTON_DISABLED_SPRITE;
            } else if (this.isHovered()) {
                sprite = BUTTON_HIGHLIGHTED_SPRITE;
            } else {
                sprite = BUTTON_SPRITE;
            }

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
            this.extractIcon(graphics);
        }

        protected void extractIcon(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.iconSprite, this.getX(), this.getY(), this.width, this.height);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class ToggleActiveButton extends AbstractButton {
        public ToggleActiveButton(int x, int y) {
            super(x, y, 26, 16, Component.translatable("gui.villagerdeed.button.toggle_active"));
            this.setTooltip(Tooltip.create(this.getMessage()));
        }

        private Identifier getSprite() {
            boolean isDisabled = VillagerDeedScreen.this.blockEntity.getDeedState() == 4;
            boolean hovered = this.active && this.isHovered();

            if (!this.active) {
                return TOGGLE_DEED_DISABLED;
            }
            if (isDisabled) {
                return hovered ? TOGGLE_DEED_TEXTURE_OFF_HIGHLIGHT : TOGGLE_DEED_TEXTURE_OFF;
            }
            return hovered ? TOGGLE_DEED_TEXTURE_HIGHLIGHT : TOGGLE_DEED_TEXTURE;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(), this.getX(), this.getY(), this.width, this.height);
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new ToggleDeedPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class EvictButton extends DeedIconButton {
        public EvictButton(int x, int y) {
            super(x, y, 16, 16, EVICT_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.evict"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new EvictTenantPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }
    }

    private class SummonButton extends DeedIconButton {
        public SummonButton(int x, int y) {
            super(x, y, 16, 16, SUMMON_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.summon"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new SummonTenantPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }
    }

    private class SwapProfessionButton extends DeedIconButton {
        public SwapProfessionButton(int x, int y) {
            super(x, y, 16, 16, SWAP_PROFESSION_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.swap_profession"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            Minecraft.getInstance().setScreenAndShow(new SwapProfessionScreen(Component.translatable("block.villagerdeed.namje_villagerdeed"), VillagerDeedScreen.this.blockEntity));
        }
    }

    private class ConfirmDeedNameButton extends DeedIconButton {
        public ConfirmDeedNameButton(int x, int y) {
            super(x, y, 16, 16, EDIT_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.confirm_deed"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new ChangeDeedNamePacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos(), deedNameEdit.getValue()));
        }
    }

    private class ConfirmTenantNameButton extends DeedIconButton {
        public ConfirmTenantNameButton(int x, int y) {
            super(x, y, 16, 16, EDIT_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.confirm_tenant"));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientPacketDistributor.sendToServer(new ChangeTenantNamePacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos(), tenantNameEdit.getValue()));
        }
    }
}