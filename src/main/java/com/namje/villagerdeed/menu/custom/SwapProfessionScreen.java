package com.namje.villagerdeed.menu.custom;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.networking.packet.EvictTenantPacketC2S;
import com.namje.villagerdeed.networking.packet.SwapTenantProfessionPacketC2S;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SwapProfessionScreen extends Screen {
    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "textures/gui/villagerdeed/swap_profession_gui.png");

    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller_disabled");

    private static final int VISIBLE_BUTTONS = 7;
    private static final int TRADE_BUTTON_HEIGHT = 20;
    private static final int TRADE_BUTTON_WIDTH = 88;
    private static final int SCROLLER_HEIGHT = 27;
    private static final int SCROLLER_WIDTH = 6;
    private static final int SCROLL_BAR_HEIGHT = 139;
    private static final int SCROLL_BAR_TOP_POS_Y = 18;
    private static final int SCROLL_BAR_START_X = 97;

    private final int imageWidth = 111;
    private final int imageHeight = 166;

    private final VillagerDeedBlockEntity blockEntity;
    private final List<VillagerProfession> professions;
    private final ProfessionButton[] professionButtons = new ProfessionButton[VISIBLE_BUTTONS];

    private int selectedIndex = -1;
    private int scrollOff;
    private boolean isDragging;

    public SwapProfessionScreen(Component title, VillagerDeedBlockEntity blockEntity) {
        super(title);
        this.blockEntity = blockEntity;
        this.professions = getProfessions();
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
    protected void init() {
        super.init();
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        int buttonY = yo + SCROLL_BAR_TOP_POS_Y;

        for (int i = 0; i < VISIBLE_BUTTONS; ++i) {
            this.professionButtons[i] = this.addRenderableWidget(
                    new ProfessionButton(xo + 8, buttonY, TRADE_BUTTON_WIDTH, TRADE_BUTTON_HEIGHT, i)
            );
            buttonY += TRADE_BUTTON_HEIGHT;
        }

        this.updateButtonStates();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.text(this.font, Component.translatable("gui.villagerdeed.profession"), x + 8, y + 7, 0xFF404040, false);
    }

    private void updateButtonStates() {
        for (int i = 0; i < VISIBLE_BUTTONS; i++) {
            int dataIndex = i + this.scrollOff;
            if (dataIndex < this.professions.size()) {
                this.professionButtons[i].visible = true;
                boolean isSelected = (dataIndex == this.selectedIndex);

                VillagerProfession profession = this.professions.get(dataIndex);
                Component rawLabel = getProfessionLabel(profession);

                if (isSelected) {
                    this.professionButtons[i].setMessage(
                            ((MutableComponent) rawLabel).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                    );
                } else {
                    this.professionButtons[i].setMessage(rawLabel);
                }

                this.professionButtons[i].active = !isSelected;
            } else {
                this.professionButtons[i].visible = false;
            }
        }
    }

    private Component getProfessionLabel(VillagerProfession profession) {
        Identifier key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);

        String namespace = key.getNamespace();
        String path = key.getPath();

        String moddedKey = "entity." + namespace + ".villager." + path;
        String vanillaKey = "entity.minecraft.villager." + path;

        Language language = Language.getInstance();

        if (language.has(moddedKey)) {
            return Component.translatable(moddedKey);
        }
        if (language.has(vanillaKey)) {
            return Component.translatable(vanillaKey);
        }

        // if no translation exists, format the key path into something nice
        String formattedFallback = Arrays.stream(path.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));

        return Component.literal(formattedFallback);
    }

    private boolean canScroll() {
        return this.professions.size() > VISIBLE_BUTTONS;
    }

    private void extractScroller(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY) {
        int maxScrollOff = this.professions.size() - VISIBLE_BUTTONS;

        if (maxScrollOff > 0) {
            int trackHeight = SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
            int scrollerYOff = (int) ((float) trackHeight * (float) this.scrollOff / (float) maxScrollOff);

            int scrollerX = xo + SCROLL_BAR_START_X;
            int scrollerY = yo + SCROLL_BAR_TOP_POS_Y + scrollerYOff;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, scrollerX, scrollerY, SCROLLER_WIDTH, SCROLLER_HEIGHT);

            if (mouseX >= scrollerX && mouseX < scrollerX + SCROLLER_WIDTH && mouseY >= scrollerY && mouseY <= scrollerY + SCROLLER_HEIGHT) {
                graphics.requestCursor(this.isDragging ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            }
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE,
                    xo + SCROLL_BAR_START_X, yo + SCROLL_BAR_TOP_POS_Y, SCROLLER_WIDTH, SCROLLER_HEIGHT);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth,
                this.imageHeight, 256, 256);

        this.extractScroller(graphics, xo, yo, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!super.mouseScrolled(x, y, scrollX, scrollY)) {
            if (this.canScroll()) {
                int maxScrollOff = this.professions.size() - VISIBLE_BUTTONS;
                this.scrollOff = Mth.clamp((int) ((double) this.scrollOff - scrollY), 0, maxScrollOff);
                this.updateButtonStates();
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        if (this.canScroll() && event.x() > (double) (xo + SCROLL_BAR_START_X)
                && event.x() < (double) (xo + SCROLL_BAR_START_X + SCROLLER_WIDTH)
                && event.y() > (double) (yo + SCROLL_BAR_TOP_POS_Y)
                && event.y() <= (double) (yo + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT + 1)) {
            this.isDragging = true;
            this.mouseDragged(event, 0, 0);
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.isDragging) {
            int numberOfItems = this.professions.size();
            int fullScrollTopPos = (this.height - this.imageHeight) / 2 + SCROLL_BAR_TOP_POS_Y;
            int fullScrollBottomPos = fullScrollTopPos + SCROLL_BAR_HEIGHT;
            int maxScrollOff = numberOfItems - VISIBLE_BUTTONS;
            float scrolling = ((float) event.y() - (float) fullScrollTopPos - 13.5F) / ((float) (fullScrollBottomPos - fullScrollTopPos) - 27.0F);
            scrolling = scrolling * (float) maxScrollOff + 0.5F;
            this.scrollOff = Mth.clamp((int) scrolling, 0, maxScrollOff);
            this.updateButtonStates();
            return true;
        } else {
            return super.mouseDragged(event, dx, dy);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDragging = false;
        return super.mouseReleased(event);
    }

    private class ProfessionButton extends Button {
        private final int slotIndex;

        public ProfessionButton(int x, int y, int width, int height, int slotIndex) {
            super(x, y, width, height, Component.empty(), button -> {
                int clickedDataIndex = slotIndex + SwapProfessionScreen.this.scrollOff;
                if (clickedDataIndex < SwapProfessionScreen.this.professions.size()) {
                    SwapProfessionScreen.this.selectedIndex = clickedDataIndex;
                    SwapProfessionScreen.this.updateButtonStates();
                }
            }, DEFAULT_NARRATION);
            this.slotIndex = slotIndex;
        }

        public int getSlotIndex() {
            return this.slotIndex;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            int clickedDataIndex = this.slotIndex + SwapProfessionScreen.this.scrollOff;
            if (clickedDataIndex < SwapProfessionScreen.this.professions.size()) {
                VillagerProfession profession = SwapProfessionScreen.this.professions.get(clickedDataIndex);

                ClientPacketDistributor.sendToServer(new SwapTenantProfessionPacketC2S(SwapProfessionScreen.this.blockEntity.getBlockPos(), profession));

                SwapProfessionScreen.this.onClose();
            }
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor guiGraphicsExtractor, int i, int i1, float v) {
            this.extractDefaultSprite(guiGraphicsExtractor);
            this.extractDefaultLabel(guiGraphicsExtractor.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
        }
    }

    private static List<VillagerProfession> getProfessions() {
        return BuiltInRegistries.VILLAGER_PROFESSION.stream()
                .filter(profession -> BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession)
                        .map(key -> !key.equals(VillagerProfession.NONE))
                        .orElse(true))
                .toList();
    }
}