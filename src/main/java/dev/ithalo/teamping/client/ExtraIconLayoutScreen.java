package dev.ithalo.teamping.client;

import java.util.List;

import dev.ithalo.teamping.TeamPing;
import dev.ithalo.teamping.ping.PingIconOption;
import dev.ithalo.teamping.ping.PingSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ExtraIconLayoutScreen extends Screen {
    private static final int PANEL_WIDTH = 332;
    private static final int PANEL_HEIGHT = 226;
    private static final int SLOT_SIZE = 26;
    private static final int ICON_SIZE = 20;
    private static final int SLOT_X = -126;
    private static final int SLOT_Y = -54;
    private static final int SLOT_GAP = 31;
    private static final int ICON_GRID_START_X = 20;
    private static final int ICON_GRID_START_Y = -44;
    private static final int ICON_GRID_GAP = 30;

    private final List<PingIconOption> icons = PingIconOption.displayOrder();
    private PingSegment selectedSegment = PingSegment.DANGER;
    private PingIconOption selectedIcon = PingIconOption.QUESTION_MARK;

    public ExtraIconLayoutScreen() {
        super(Component.literal("TeamPing Extras"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        int centerY = height / 2;
        int left = centerX - PANEL_WIDTH / 2;
        int top = centerY - PANEL_HEIGHT / 2;

        graphics.fill(0, 0, width, height, 0x99080D18);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE121824);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 1, 0x66FFFFFF);
        graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0x33000000);

        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, "Customize radial icons", left + 14, top + 12, 0xF4F7FF, false);
        graphics.drawString(font, "Radial slots", left + 18, top + 40, 0x90FFFFFF, false);
        graphics.drawString(font, "Extras", left + 164, top + 40, 0x90FFFFFF, false);
        graphics.drawString(font, "Selected", left + 18, top + PANEL_HEIGHT - 46, 0x90FFFFFF, false);
        graphics.drawString(font, selectedSegment.id().toUpperCase(), left + 18, top + PANEL_HEIGHT - 32, 0xF4F7FF, false);
        graphics.drawString(font, selectedIcon.label(), left + 110, top + PANEL_HEIGHT - 32, 0xF4F7FF, false);

        drawSlotButton(graphics, PingSegment.DANGER, left + 16, top + 58, mouseX, mouseY);
        drawSlotButton(graphics, PingSegment.RESOURCE, left + 16, top + 58 + SLOT_GAP, mouseX, mouseY);
        drawSlotButton(graphics, PingSegment.COMBAT, left + 16, top + 58 + SLOT_GAP * 2, mouseX, mouseY);
        drawSlotButton(graphics, PingSegment.LOOT, left + 16, top + 58 + SLOT_GAP * 3, mouseX, mouseY);
        drawSlotButton(graphics, PingSegment.BASE, left + 16, top + 58 + SLOT_GAP * 4, mouseX, mouseY);
        drawSlotButton(graphics, PingSegment.LOCATION, left + 16, top + 58 + SLOT_GAP * 5, mouseX, mouseY);

        drawExtraGrid(graphics, left + 160, top + 58, mouseX, mouseY);
        drawActionButton(graphics, left + 18, top + PANEL_HEIGHT - 72, 70, 22, "Clear", 0xAA2A1E28);
        drawActionButton(graphics, left + 96, top + PANEL_HEIGHT - 72, 92, 22, "Replace", 0xCC263A63);
        drawActionButton(graphics, left + 196, top + PANEL_HEIGHT - 72, 72, 22, "Save", 0xCC2C5A3A);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        int centerY = height / 2;
        int left = centerX - PANEL_WIDTH / 2;
        int top = centerY - PANEL_HEIGHT / 2;

        PingSegment clickedSegment = segmentAt(mouseX, mouseY, left + 16, top + 58);
        if (clickedSegment != null) {
            selectedSegment = clickedSegment;
            selectedIcon = PingIconOption.byId(ClientExtraIconStore.resolveIconId(selectedSegment))
                    .orElse(PingIconOption.QUESTION_MARK);
            if (button == 1) {
                ClientExtraIconStore.clearIcon(selectedSegment);
                selectedIcon = PingIconOption.QUESTION_MARK;
            }
            return true;
        }

        PingIconOption clickedIcon = iconAt(mouseX, mouseY, left + 160, top + 58);
        if (clickedIcon != null) {
            selectedIcon = clickedIcon;
            return true;
        }

        if (isInside(mouseX, mouseY, left + 18, top + PANEL_HEIGHT - 72, 70, 22)) {
            ClientExtraIconStore.clearIcon(selectedSegment);
            selectedIcon = PingIconOption.QUESTION_MARK;
            return true;
        }

        if (isInside(mouseX, mouseY, left + 96, top + PANEL_HEIGHT - 72, 92, 22)) {
            ClientExtraIconStore.assignIcon(selectedSegment, selectedIcon.id());
            return true;
        }

        if (isInside(mouseX, mouseY, left + 196, top + PANEL_HEIGHT - 72, 72, 22)) {
            ClientExtraIconStore.assignIcon(selectedSegment, selectedIcon.id());
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void drawSlotButton(GuiGraphics graphics, PingSegment segment, int x, int y, int mouseX, int mouseY) {
        ResourceLocation icon = ClientExtraIconStore.resolveIconTexture(segment, defaultIcon(segment));
        boolean selected = segment == selectedSegment;
        boolean hover = isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);

        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, selected ? 0xCC2B3550 : 0xAA0D111B);
        if (hover || selected) {
            graphics.fill(x, y, x + SLOT_SIZE, y + 1, selected ? 0x99FFFFFF : 0x44FFFFFF);
            graphics.fill(x, y, x + 1, y + SLOT_SIZE, selected ? 0x99FFFFFF : 0x44FFFFFF);
            graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, selected ? 0x99FFFFFF : 0x44FFFFFF);
            graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, selected ? 0x99FFFFFF : 0x44FFFFFF);
        }
        graphics.blit(icon, x + 3, y + 3, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, 204, 256, 204, 256);
    }

    private void drawActionButton(GuiGraphics graphics, int x, int y, int width, int height, String label, int color) {
        graphics.fill(x, y, x + width, y + height, color);
        graphics.fill(x, y, x + width, y + 1, 0x88FFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0x44000000);
        graphics.fill(x, y, x + 1, y + height, 0x88FFFFFF);
        graphics.fill(x + width - 1, y, x + width, y + height, 0x88FFFFFF);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 7, 0xF4F7FF, false);
    }

    private void drawExtraGrid(GuiGraphics graphics, int startX, int startY, int mouseX, int mouseY) {
        for (int i = 0; i < icons.size(); i++) {
            PingIconOption option = icons.get(i);
            int column = i % 6;
            int row = i / 6;
            int x = startX + column * ICON_GRID_GAP;
            int y = startY + row * ICON_GRID_GAP;
            boolean hover = isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);

            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hover ? 0xD8222837 : 0xAA0D111B);
            if (hover) {
                graphics.fill(x, y, x + SLOT_SIZE, y + 1, 0x55FFFFFF);
                graphics.fill(x, y, x + 1, y + SLOT_SIZE, 0x55FFFFFF);
                graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x55FFFFFF);
                graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, 0x55FFFFFF);
            }
        graphics.blit(option.texture(), x + 3, y + 3, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, 204, 256, 204, 256);
    }
    }

    private PingSegment segmentAt(double mouseX, double mouseY, int startX, int startY) {
        PingSegment[] segments = {
                PingSegment.DANGER,
                PingSegment.RESOURCE,
                PingSegment.COMBAT,
                PingSegment.LOOT,
                PingSegment.BASE,
                PingSegment.LOCATION
        };

        for (int i = 0; i < segments.length; i++) {
            int y = startY + i * SLOT_GAP;
            if (isInside(mouseX, mouseY, startX, y, SLOT_SIZE, SLOT_SIZE)) {
                return segments[i];
            }
        }
        return null;
    }

    private PingIconOption iconAt(double mouseX, double mouseY, int startX, int startY) {
        for (int i = 0; i < icons.size(); i++) {
            int column = i % 6;
            int row = i / 6;
            int x = startX + column * ICON_GRID_GAP;
            int y = startY + row * ICON_GRID_GAP;
            if (isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
                return icons.get(i);
            }
        }
        return null;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private ResourceLocation defaultIcon(PingSegment segment) {
        return switch (segment) {
            case DANGER -> ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/danger.png");
            case RESOURCE -> ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/resource.png");
            case COMBAT -> ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/combat.png");
            case LOOT -> ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/loot.png");
            case BASE -> ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/base.png");
            case LOCATION -> ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/location.png");
        };
    }
}
