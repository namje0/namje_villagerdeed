package com.namje.villagerdeed.menu.custom;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.event.ModEvents;
import com.namje.villagerdeed.networking.packet.SwapTenantProfessionPacketC2S;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SwapProfessionScreen extends Screen {
    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation(VillagerDeed.MODID, "textures/gui/villagerdeed/swap_profession_gui.png");

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
        ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);

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

    private void renderScroller(GuiGraphics graphics, int xo, int yo, int mouseX, int mouseY) {
        int maxScrollOff = this.professions.size() - VISIBLE_BUTTONS;

        int scrollerX = xo + SCROLL_BAR_START_X;
        int scrollerY;
        int color;

        if (maxScrollOff > 0) {
            int trackHeight = SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
            int scrollerYOff = (int) ((float) trackHeight * (float) this.scrollOff / (float) maxScrollOff);

            scrollerY = yo + SCROLL_BAR_TOP_POS_Y + scrollerYOff;
            color = 0xFFC6C6C6;
        } else {
            scrollerY = yo + SCROLL_BAR_TOP_POS_Y;
            color = 0xFF8B8B8B;
        }

        graphics.fill(scrollerX, scrollerY, scrollerX + SCROLLER_WIDTH, scrollerY + SCROLLER_HEIGHT, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        this.renderBackground(graphics);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;

        graphics.blit(GUI_TEXTURE, xo, yo, 0, 0, this.imageWidth, this.imageHeight);

        this.renderScroller(graphics, xo, yo, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, a);

        graphics.drawString(this.font, Component.translatable("gui.villagerdeed.profession"), xo + 8, yo + 7, 0xFF404040, false);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (!super.mouseScrolled(x, y, delta)) {
            if (this.canScroll()) {
                int maxScrollOff = this.professions.size() - VISIBLE_BUTTONS;
                this.scrollOff = Mth.clamp((int) ((double) this.scrollOff - delta), 0, maxScrollOff);
                this.updateButtonStates();
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        if (this.canScroll() && mouseX > (double) (xo + SCROLL_BAR_START_X)
                && mouseX < (double) (xo + SCROLL_BAR_START_X + SCROLLER_WIDTH)
                && mouseY > (double) (yo + SCROLL_BAR_TOP_POS_Y)
                && mouseY <= (double) (yo + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT + 1)) {
            this.isDragging = true;
            this.mouseDragged(mouseX, mouseY, button, 0, 0);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.isDragging) {
            int numberOfItems = this.professions.size();
            int fullScrollTopPos = (this.height - this.imageHeight) / 2 + SCROLL_BAR_TOP_POS_Y;
            int fullScrollBottomPos = fullScrollTopPos + SCROLL_BAR_HEIGHT;
            int maxScrollOff = numberOfItems - VISIBLE_BUTTONS;
            float scrolling = ((float) mouseY - (float) fullScrollTopPos - 13.5F) / ((float) (fullScrollBottomPos - fullScrollTopPos) - 27.0F);
            scrolling = scrolling * (float) maxScrollOff + 0.5F;
            this.scrollOff = Mth.clamp((int) scrolling, 0, maxScrollOff);
            this.updateButtonStates();
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
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
        public void onPress() {
            int clickedDataIndex = this.slotIndex + SwapProfessionScreen.this.scrollOff;
            if (clickedDataIndex < SwapProfessionScreen.this.professions.size()) {
                VillagerProfession profession = SwapProfessionScreen.this.professions.get(clickedDataIndex);

                ModEvents.CHANNEL.sendToServer(new SwapTenantProfessionPacketC2S(SwapProfessionScreen.this.blockEntity.getBlockPos(), profession));

                SwapProfessionScreen.this.onClose();
            }
        }
    }

    private static List<VillagerProfession> getProfessions() {
        return BuiltInRegistries.VILLAGER_PROFESSION.stream()
                .filter(profession -> profession != VillagerProfession.NONE)
                .toList();
    }
}
