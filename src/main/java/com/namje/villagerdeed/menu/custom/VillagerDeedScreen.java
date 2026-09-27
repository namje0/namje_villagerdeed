package com.namje.villagerdeed.menu.custom;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.event.ModEvents;
import com.namje.villagerdeed.networking.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

import java.util.*;

public class VillagerDeedScreen extends Screen {
    private static final int STATE_TEXT_MAX_WIDTH = 100;
    private static final int MAX_VISIBLE_LINES = 3;
    private List<FormattedCharSequence> cachedStateLines = Collections.emptyList();
    private int cachedDeedState = -1;
    private String cachedDeedName = null;
    private String cachedTenantName = null;

    private static final int VILLAGER_SCALE = 24;

    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/villagerdeed/deed_gui.png");

    private static final ResourceLocation BUTTON_SPRITE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/button.png");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/button_highlighted.png");
    private static final ResourceLocation BUTTON_DISABLED_SPRITE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/button_disabled.png");

    private static final ResourceLocation EVICT_ICON_SPRITE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/evicttenantbutton.png");
    private static final ResourceLocation SUMMON_ICON_SPRITE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/summontenantbutton.png");
    private static final ResourceLocation SWAP_PROFESSION_ICON_SPRITE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/swapprofessionsbutton.png");
    private static final ResourceLocation EDIT_ICON_SPRITE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/writebutton.png");

    private static final ResourceLocation TOGGLE_DEED_TEXTURE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/toggledeedbutton.png");
    private static final ResourceLocation TOGGLE_DEED_TEXTURE_HIGHLIGHT =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/toggledeedbutton_highlighted.png");
    private static final ResourceLocation TOGGLE_DEED_TEXTURE_OFF =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/toggledeedbutton_off.png");
    private static final ResourceLocation TOGGLE_DEED_TEXTURE_OFF_HIGHLIGHT =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/toggledeedbutton_off_highlighted.png");
    private static final ResourceLocation TOGGLE_DEED_DISABLED =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/toggledeedbutton_disabled.png");
    private static final ResourceLocation LOCK_TEXTURE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/lockbutton.png");
    private static final ResourceLocation LOCK_TEXTURE_HIGHLIGHT =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/lockbutton_highlighted.png");
    private static final ResourceLocation LOCK_TEXTURE_OFF =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/unlockbutton.png");
    private static final ResourceLocation LOCK_TEXTURE_OFF_HIGHLIGHT =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/unlockbutton_highlighted.png");
    private static final ResourceLocation LOCK_TEXTURE_DISABLED =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/lockbutton_disabled.png");
    private static final ResourceLocation LOCK_TEXTURE_OFF_DISABLED =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/unlockbutton_disabled.png");
    private static final ResourceLocation SUBSCRIBE_TEXTURE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/subscribebutton_enabled.png");
    private static final ResourceLocation SUBSCRIBE_TEXTURE_OFF =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/subscribebutton.png");
    private static final ResourceLocation SUBSCRIBE_TEXTURE_HIGHLIGHT =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/sprites/villagerdeed/subscribebutton_highlighted.png");

    private final int imageWidth = 176;
    private final int imageHeight = 113;

    private EditBox deedNameEdit;
    private EditBox tenantNameEdit;

    private ToggleActiveButton toggleActiveButton;
    private ToggleLockButton toggleLockButton;
    private ToggleSubscriptionButton toggleSubscriptionButton;

    private EvictButton evictButton;
    private SummonButton summonButton;
    private SwapProfessionButton swapProfessionButton;

    private ConfirmDeedNameButton confirmDeedNameButton;
    private ConfirmTenantNameButton confirmTenantNameButton;

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
        this.toggleLockButton = this.addRenderableWidget(new ToggleLockButton(x + 128, y + 83));
        this.toggleSubscriptionButton = this.addRenderableWidget(new ToggleSubscriptionButton(x + 146, y + 83));

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

        UUID playerUUID = (this.minecraft != null && this.minecraft.player != null) ? this.minecraft.player.getUUID() : null;
        UUID ownerUUID = this.blockEntity.getOwnerUUID();
        boolean isLocked = this.blockEntity.getLocked();
        boolean isUnauthorized = isLocked && (playerUUID == null || !playerUUID.equals(ownerUUID));

        if (isUnauthorized) {
            if (this.deedNameEdit != null) {
                this.deedNameEdit.active = false;
                this.deedNameEdit.setEditable(false);
            }
            if (this.tenantNameEdit != null) {
                this.tenantNameEdit.active = false;
                this.tenantNameEdit.setEditable(false);
            }
            if (this.toggleActiveButton != null) this.toggleActiveButton.active = false;
            if (this.toggleLockButton != null) this.toggleLockButton.active = false;
            if (this.evictButton != null) this.evictButton.active = false;
            if (this.summonButton != null) this.summonButton.active = false;
            if (this.swapProfessionButton != null) this.swapProfessionButton.active = false;
            if (this.confirmDeedNameButton != null) this.confirmDeedNameButton.active = false;
            if (this.confirmTenantNameButton != null) this.confirmTenantNameButton.active = false;

            if (this.toggleSubscriptionButton != null) {
                this.toggleSubscriptionButton.active = true;
            }
            return;
        }

        int state = this.blockEntity.getDeedState();

        if (this.deedNameEdit != null) {
            this.deedNameEdit.active = true;
            this.deedNameEdit.setEditable(true);
        }
        if (this.confirmDeedNameButton != null) {
            this.confirmDeedNameButton.active = true;
        }
        if (this.toggleLockButton != null) {
            this.toggleLockButton.active = playerUUID != null && playerUUID.equals(ownerUUID);
        }
        if (this.toggleSubscriptionButton != null) {
            this.toggleSubscriptionButton.active = true;
        }

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

    private void renderVillagerPreview(GuiGraphics graphics, Villager villager, int mouseX, int mouseY, int guiLeft, int guiTop) {
        int x0 = guiLeft + 8;
        int y0 = guiTop + 24;
        int x1 = guiLeft + 59;
        int y1 = guiTop + 75;

        float entityCenterX = guiLeft + 30.0F;
        float entityCenterY = guiTop + 35.0F;

        float deltaX = entityCenterX - mouseX;
        float deltaY = entityCenterY - mouseY;

        float yawOffset = (float) Math.atan(deltaX / 40.0F) * 20.0F;
        float pitchOffset = (float) Math.atan(deltaY / 40.0F) * 20.0F;

        float yRot = Mth.clamp(yawOffset, -45.0F, 45.0F);
        float xRot = Mth.clamp(-pitchOffset, -30.0F, 30.0F);

        float bodyRotO = villager.yBodyRot;
        float yRotO = villager.getYRot();
        float xRotO = villager.getXRot();
        float headRotO = villager.yHeadRotO;
        float headRot = villager.yHeadRot;

        villager.yBodyRot = 200.0F;
        villager.setYRot(yRot);
        villager.setXRot(xRot);
        villager.yHeadRot = villager.getYRot();
        villager.yHeadRotO = villager.getYRot();

        Quaternionf pose = new Quaternionf().rotationXYZ(0.2F, 0.0F, (float) Math.PI);

        graphics.enableScissor(x0, y0, x1, y1);
        InventoryScreen.renderEntityInInventory(graphics, guiLeft + 33, y1, VILLAGER_SCALE, pose, null, villager);
        graphics.disableScissor();

        villager.yBodyRot = bodyRotO;
        villager.setYRot(yRotO);
        villager.setXRot(xRotO);
        villager.yHeadRotO = headRotO;
        villager.yHeadRot = headRot;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        this.renderBackground(graphics);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (this.blockEntity.getDeedState() == 2) {
            Level level = this.minecraft.level;
            Villager tenant = (level != null) ? this.blockEntity.getTenantEntity(level) : null;
            if (tenant != null) {
                this.renderVillagerPreview(graphics, tenant, mouseX, mouseY, x, y);
            }
        }

        super.render(graphics, mouseX, mouseY, a);

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
            graphics.drawString(this.font, line, x + 63, lineY, 0xFF404040, false);
        }
    }

    private abstract static class DeedIconButton extends AbstractButton {
        private final ResourceLocation iconSprite;

        protected DeedIconButton(int x, int y, int width, int height, ResourceLocation iconSprite, Component label) {
            super(x, y, width, height, label);
            this.setTooltip(Tooltip.create(label));
            this.iconSprite = iconSprite;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
            ResourceLocation sprite;
            if (!this.active) {
                sprite = BUTTON_DISABLED_SPRITE;
            } else if (this.isHovered()) {
                sprite = BUTTON_HIGHLIGHTED_SPRITE;
            } else {
                sprite = BUTTON_SPRITE;
            }

            graphics.blit(sprite, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
            this.renderIcon(graphics);
        }

        protected void renderIcon(GuiGraphics graphics) {
            graphics.blit(this.iconSprite, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
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

        private ResourceLocation getSprite() {
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
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
            graphics.blit(this.getSprite(), this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        public void onPress() {
            ModEvents.CHANNEL.sendToServer(new ToggleDeedPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class ToggleLockButton extends AbstractButton {
        public ToggleLockButton(int x, int y) {
            super(x, y, 16, 16, Component.translatable("gui.villagerdeed.button.toggle_lock"));
            this.setTooltip(Tooltip.create(this.getMessage()));
        }

        private ResourceLocation getSprite() {
            boolean isLocked = VillagerDeedScreen.this.blockEntity.getLocked();
            boolean hovered = this.active && this.isHovered();

            if (!this.active) {
                return isLocked ? LOCK_TEXTURE_DISABLED : LOCK_TEXTURE_OFF_DISABLED;
            }
            if (!isLocked) {
                return hovered ? LOCK_TEXTURE_OFF_HIGHLIGHT : LOCK_TEXTURE_OFF;
            }
            return hovered ? LOCK_TEXTURE_HIGHLIGHT : LOCK_TEXTURE;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
            graphics.blit(this.getSprite(), this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        public void onPress() {
            ModEvents.CHANNEL.sendToServer(new ToggleLockPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class ToggleSubscriptionButton extends AbstractButton {
        public ToggleSubscriptionButton(int x, int y) {
            super(x, y, 16, 16, Component.translatable("gui.villagerdeed.button.subscribe"));
            this.setTooltip(Tooltip.create(this.getMessage()));
        }

        private ResourceLocation getSprite() {
            UUID playerUUID = (Minecraft.getInstance().player != null) ? Minecraft.getInstance().player.getUUID() : null;
            boolean isSubscribed = playerUUID != null && VillagerDeedScreen.this.blockEntity.playerIsSubscribed(playerUUID);
            boolean hovered = this.active && this.isHovered();

            if (isSubscribed) {
                return hovered ? SUBSCRIBE_TEXTURE_HIGHLIGHT : SUBSCRIBE_TEXTURE;
            }
            return hovered ? SUBSCRIBE_TEXTURE_HIGHLIGHT : SUBSCRIBE_TEXTURE_OFF;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
            graphics.blit(this.getSprite(), this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        public void onPress() {
            ModEvents.CHANNEL.sendToServer(new ToggleSubscriptionPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
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
        public void onPress() {
            ModEvents.CHANNEL.sendToServer(new EvictTenantPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }
    }

    private class SummonButton extends DeedIconButton {
        public SummonButton(int x, int y) {
            super(x, y, 16, 16, SUMMON_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.summon"));
        }

        @Override
        public void onPress() {
            ModEvents.CHANNEL.sendToServer(new SummonTenantPacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos()));
        }
    }

    private class SwapProfessionButton extends DeedIconButton {
        public SwapProfessionButton(int x, int y) {
            super(x, y, 16, 16, SWAP_PROFESSION_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.swap_profession"));
        }

        @Override
        public void onPress() {
            Minecraft.getInstance().setScreen(new SwapProfessionScreen(Component.translatable("block.villagerdeed.namje_villagerdeed"), VillagerDeedScreen.this.blockEntity));
        }
    }

    private class ConfirmDeedNameButton extends DeedIconButton {
        public ConfirmDeedNameButton(int x, int y) {
            super(x, y, 16, 16, EDIT_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.confirm_deed"));
        }

        @Override
        public void onPress() {
            ModEvents.CHANNEL.sendToServer(new ChangeDeedNamePacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos(), deedNameEdit.getValue()));
        }
    }

    private class ConfirmTenantNameButton extends DeedIconButton {
        public ConfirmTenantNameButton(int x, int y) {
            super(x, y, 16, 16, EDIT_ICON_SPRITE, Component.translatable("gui.villagerdeed.button.confirm_tenant"));
        }

        @Override
        public void onPress() {
            ModEvents.CHANNEL.sendToServer(new ChangeTenantNamePacketC2S(VillagerDeedScreen.this.blockEntity.getBlockPos(), tenantNameEdit.getValue()));
        }
    }
}
