package dev.ithalo.teamping.client;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.ithalo.teamping.ping.PingIconOption;
import dev.ithalo.teamping.ping.PingAckType;
import dev.ithalo.teamping.ping.PingSegment;
import dev.ithalo.teamping.TeamPing;
import dev.ithalo.teamping.team.PingTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;

public final class PingWheelRenderer {
    private static final float OVERLAY_RADIUS = 126.0F;
    private static final float OUTER_RADIUS = 126.0F;
    private static final float INNER_RADIUS = 38.0F;
    private static final float CENTER_RADIUS = 32.0F;
    private static final float ICON_RADIUS = 104.0F;
    private static final float TEAM_ROW_OFFSET_Y = 160.0F;
    private static final float TEAM_LABEL_OFFSET_X = -72.0F;
    private static final float TEAM_START_OFFSET_X = 12.0F;
    private static final float TEAM_SPACING = 28.0F;
    private static final int TEAM_SWATCH_RADIUS = 9;
    private static final float EXTRA_ROW_ONE_Y = 122.0F;
    private static final float EXTRA_ROW_TWO_Y = 145.0F;
    private static final float EXTRA_START_X = -69.0F;
    private static final float EXTRA_SPACING = 24.0F;
    private static final int EXTRA_ICON_RADIUS = 12;
    private static final int EXTRA_ICON_SIZE = 16;
    private static final int EXTRAS_BUTTON_SIZE = 26;
    private static final int EXTRAS_BUTTON_MARGIN = 10;
    private static final int EXTRAS_BUTTON_BACKGROUND = 0xAA0F1420;
    private static final int EXTRAS_BUTTON_HOVER_BACKGROUND = 0xCC1A2235;
    private static final int EXTRAS_BUTTON_BORDER = 0x66FFFFFF;
    private static final int EXTRAS_BUTTON_HOVER_BORDER = 0xAAFFFFFF;
    private static final int CUSTOM_PANEL_WIDTH = 360;
    private static final int CUSTOM_PANEL_HEIGHT = 190;
    private static final int CUSTOM_SLOT_SIZE = 28;
    private static final int CUSTOM_ICON_SIZE = 20;
    private static final int CUSTOM_SLOT_X = 18;
    private static final int CUSTOM_SLOT_Y = 44;
    private static final int CUSTOM_SLOT_GAP = 24;
    private static final int CUSTOM_ICON_GRID_X = 150;
    private static final int CUSTOM_ICON_GRID_Y = 44;
    private static final int CUSTOM_ICON_GRID_COLUMNS = 4;
    private static final int CUSTOM_ICON_GRID_GAP = 32;
    private static final int CUSTOM_BUTTON_Y = 156;
    private static final int CUSTOM_BUTTON_HEIGHT = 22;
    private static final float ACK_WHEEL_RADIUS = 78.0F;
    private static final float ACK_CENTER_RADIUS = 22.0F;
    private static final float ACK_OPTION_RADIUS = 58.0F;
    private static final float HIGHLIGHT_FILL_ALPHA = 0.22F;
    private static final float HIGHLIGHT_BORDER_ALPHA = 0.70F;
    private static final float HIGHLIGHT_POINT_ALPHA = 1.0F;
    private static final float LERP_SPEED = 0.18F;
    private static final int SUBDIVISIONS = 32;

    private static final int SCREEN_DIM_COLOR = 0x9A060A16;
    private static final int WHEEL_GLOW_RGB = 0x263866;
    private static final int WHEEL_SHADOW_RGB = 0x050914;
    private static final int RING_RGB = 0x08142D;
    private static final int CENTER_RGB = 0x283453;
    private static final int DIVIDER_RGB = 0xAAB4D6;
    private static final int BORDER_RGB = 0xB7C0DD;
    private static final int INSTRUCTION_COLOR = 0x40FFFFFF;
    private static final Map<PingSegment, VisualState> STATES = new EnumMap<>(PingSegment.class);
    private static final Map<PingSegment, ResourceLocation> ICONS = new EnumMap<>(PingSegment.class);

    static {
        for (PingSegment segment : PingSegment.values()) {
            STATES.put(segment, new VisualState());
        }
        ICONS.put(PingSegment.DANGER, ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/danger.png"));
        ICONS.put(PingSegment.RESOURCE, ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/resource.png"));
        ICONS.put(PingSegment.COMBAT, ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/combat.png"));
        ICONS.put(PingSegment.LOOT, ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/loot.png"));
        ICONS.put(PingSegment.BASE, ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/base.png"));
        ICONS.put(PingSegment.LOCATION, ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/location.png"));
    }

    private PingWheelRenderer() {
    }

    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        PingWheelInputHandler inputHandler = PingWheelInputHandler.getInstance();
        if (minecraft.player == null || minecraft.level == null || inputHandler == null) {
            return;
        }

        if (inputHandler.isAckWheelOpen()) {
            renderAckWheel(event, minecraft, inputHandler);
            return;
        }

        if (!inputHandler.isWheelOpen()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        float centerX = graphics.guiWidth() / 2.0F;
        float centerY = graphics.guiHeight() / 2.0F;
        int teamColor = inputHandler.getSelectedTeam().color();
        inputHandler.refreshActiveSegment(minecraft);
        inputHandler.confirmIfKeyReleased(minecraft);
        if (!inputHandler.isWheelOpen()) {
            return;
        }
        PingSegment activeSegment = inputHandler.getActiveSegment();

        updateState(activeSegment);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = graphics.pose().last().pose();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), SCREEN_DIM_COLOR);
        drawFilledCircle(matrix, centerX, centerY, OVERLAY_RADIUS + 18.0F, withAlpha(WHEEL_GLOW_RGB, 0.28F), SUBDIVISIONS);
        drawFilledCircle(matrix, centerX, centerY, OVERLAY_RADIUS + 3.0F, withAlpha(WHEEL_SHADOW_RGB, 0.28F), SUBDIVISIONS);
        drawWheelBase(matrix, centerX, centerY);
        drawHighlights(matrix, centerX, centerY, activeSegment, teamColor);
        drawDividers(matrix, centerX, centerY);
        drawBorders(matrix, centerX, centerY);
        drawSelector(matrix, centerX, centerY, inputHandler, teamColor);
        drawSegmentLabels(graphics, font, centerX, centerY);
        drawTeamPicker(graphics, font, centerX, centerY, inputHandler);
        drawInstruction(graphics, font, centerX, centerY);
        drawExtrasButton(graphics, inputHandler);
        if (inputHandler.isExtrasMenuOpen()) {
            drawExtrasCustomizationMenu(graphics, font, inputHandler);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderAckWheel(RenderGuiEvent.Post event, Minecraft minecraft, PingWheelInputHandler inputHandler) {
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        float centerX = graphics.guiWidth() / 2.0F;
        float centerY = graphics.guiHeight() / 2.0F;

        inputHandler.refreshAckWheelSelection(minecraft);
        inputHandler.confirmAckWheelIfKeyReleased(minecraft);
        if (!inputHandler.isAckWheelOpen()) {
            return;
        }
        PingAckType activeType = inputHandler.getActiveAckType();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = graphics.pose().last().pose();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x78060A16);
        drawFilledCircle(matrix, centerX, centerY, ACK_WHEEL_RADIUS + 14.0F, withAlpha(WHEEL_GLOW_RGB, 0.22F), SUBDIVISIONS);
        drawFilledCircle(matrix, centerX, centerY, ACK_WHEEL_RADIUS, withAlpha(RING_RGB, 0.82F), SUBDIVISIONS);
        drawFilledCircle(matrix, centerX, centerY, ACK_CENTER_RADIUS, withAlpha(CENTER_RGB, 0.86F), SUBDIVISIONS);
        drawRadialLine(matrix, centerX, centerY, 45.0F, ACK_CENTER_RADIUS + 5.0F, ACK_WHEEL_RADIUS - 4.0F, 1.2F, withAlpha(DIVIDER_RGB, 0.16F));
        drawRadialLine(matrix, centerX, centerY, 135.0F, ACK_CENTER_RADIUS + 5.0F, ACK_WHEEL_RADIUS - 4.0F, 1.2F, withAlpha(DIVIDER_RGB, 0.16F));
        drawRadialLine(matrix, centerX, centerY, 225.0F, ACK_CENTER_RADIUS + 5.0F, ACK_WHEEL_RADIUS - 4.0F, 1.2F, withAlpha(DIVIDER_RGB, 0.16F));
        drawRadialLine(matrix, centerX, centerY, 315.0F, ACK_CENTER_RADIUS + 5.0F, ACK_WHEEL_RADIUS - 4.0F, 1.2F, withAlpha(DIVIDER_RGB, 0.16F));
        drawAckOption(graphics, font, matrix, PingAckType.UNDERSTOOD, activeType, centerX, centerY, 270.0F, "OK");
        drawAckOption(graphics, font, matrix, PingAckType.ON_MY_WAY, activeType, centerX, centerY, 180.0F, "GO");
        drawAckOption(graphics, font, matrix, PingAckType.NEGATIVE, activeType, centerX, centerY, 0.0F, "NO");
        drawAckOption(graphics, font, matrix, PingAckType.CHECKING, activeType, centerX, centerY, 90.0F, "CHK");

        drawCenteredText(graphics, font, "Responder", centerX, centerY + ACK_WHEEL_RADIUS + 14.0F, 0xB8FFFFFF);
        drawCenteredText(graphics, font, "Solte R", centerX, centerY + ACK_WHEEL_RADIUS + 27.0F, 0x66FFFFFF);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawAckOption(GuiGraphics graphics, Font font, Matrix4f matrix, PingAckType type, PingAckType activeType, float centerX, float centerY, float angleDegrees, String label) {
        float radians = angleDegrees * Mth.DEG_TO_RAD;
        float x = centerX + Mth.cos(radians) * ACK_OPTION_RADIUS;
        float y = centerY + Mth.sin(radians) * ACK_OPTION_RADIUS;
        boolean active = type == activeType;
        int color = type.color();

        if (active) {
            drawFilledCircle(matrix, x, y, 19.0F, withAlpha(color, 0.26F), 20);
            drawFilledCircle(matrix, x, y, 13.0F, withAlpha(color, 0.72F), 20);
        } else {
            drawFilledCircle(matrix, x, y, 12.0F, 0xAA101624, 20);
        }

        drawCenteredText(graphics, font, label, x, y - 4.0F, active ? 0xFFFFFFFF : 0xB8FFFFFF);
    }

    public static boolean isExtrasButtonAt(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        int left = guiWidth - EXTRAS_BUTTON_MARGIN - EXTRAS_BUTTON_SIZE;
        int top = guiHeight - EXTRAS_BUTTON_MARGIN - EXTRAS_BUTTON_SIZE;
        return mouseX >= left && mouseX <= left + EXTRAS_BUTTON_SIZE && mouseY >= top && mouseY <= top + EXTRAS_BUTTON_SIZE;
    }

    public static PingSegment customizationSegmentAt(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        int left = customPanelLeft(guiWidth);
        int top = customPanelTop(guiHeight);
        PingSegment[] segments = customizationSegments();
        for (int i = 0; i < segments.length; i++) {
            int x = left + CUSTOM_SLOT_X;
            int y = top + CUSTOM_SLOT_Y + i * CUSTOM_SLOT_GAP;
            if (isInside(mouseX, mouseY, x, y, CUSTOM_SLOT_SIZE, CUSTOM_SLOT_SIZE)) {
                return segments[i];
            }
        }
        return null;
    }

    public static PingIconOption customizationIconAt(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        int left = customPanelLeft(guiWidth);
        int top = customPanelTop(guiHeight);
        for (int i = 0; i < PingIconOption.displayOrder().size(); i++) {
            int column = i % CUSTOM_ICON_GRID_COLUMNS;
            int row = i / CUSTOM_ICON_GRID_COLUMNS;
            int x = left + CUSTOM_ICON_GRID_X + column * CUSTOM_ICON_GRID_GAP;
            int y = top + CUSTOM_ICON_GRID_Y + row * CUSTOM_ICON_GRID_GAP;
            if (isInside(mouseX, mouseY, x, y, CUSTOM_SLOT_SIZE, CUSTOM_SLOT_SIZE)) {
                return PingIconOption.displayOrder().get(i);
            }
        }
        return null;
    }

    public static boolean isCustomizationReplaceButtonAt(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        int left = customPanelLeft(guiWidth);
        int top = customPanelTop(guiHeight);
        return isInside(mouseX, mouseY, left + 150, top + CUSTOM_BUTTON_Y, 86, CUSTOM_BUTTON_HEIGHT);
    }

    public static boolean isCustomizationSaveButtonAt(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        int left = customPanelLeft(guiWidth);
        int top = customPanelTop(guiHeight);
        return isInside(mouseX, mouseY, left + 244, top + CUSTOM_BUTTON_Y, 58, CUSTOM_BUTTON_HEIGHT);
    }

    public static boolean isCustomizationClearButtonAt(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        int left = customPanelLeft(guiWidth);
        int top = customPanelTop(guiHeight);
        return isInside(mouseX, mouseY, left + 18, top + CUSTOM_BUTTON_Y, 58, CUSTOM_BUTTON_HEIGHT);
    }

    public static boolean isExtrasCloseButtonAt(double mouseX, double mouseY, int guiWidth, int guiHeight) {
        int left = customPanelLeft(guiWidth);
        int top = customPanelTop(guiHeight);
        return isInside(mouseX, mouseY, left + CUSTOM_PANEL_WIDTH - 24, top + 8, 16, 16);
    }

    private static void updateState(PingSegment activeSegment) {
        for (PingSegment segment : PingSegment.values()) {
            VisualState state = STATES.get(segment);
            boolean active = segment == activeSegment;

            state.fillAlpha = Mth.lerp(LERP_SPEED, state.fillAlpha, active ? HIGHLIGHT_FILL_ALPHA : 0.0F);
            state.borderAlpha = Mth.lerp(LERP_SPEED, state.borderAlpha, active ? HIGHLIGHT_BORDER_ALPHA : 0.0F);
            state.pointAlpha = Mth.lerp(LERP_SPEED, state.pointAlpha, active ? HIGHLIGHT_POINT_ALPHA : 0.0F);
            state.iconScale = Mth.lerp(LERP_SPEED, state.iconScale, active ? 1.5F : 1.0F);
            state.labelAlpha = Mth.lerp(LERP_SPEED, state.labelAlpha, active ? 0.95F : 0.55F);
        }
    }

    private static void drawWheelBase(Matrix4f matrix, float centerX, float centerY) {
        drawFilledCircle(matrix, centerX, centerY, OUTER_RADIUS, withAlpha(RING_RGB, 0.82F), SUBDIVISIONS);

        drawFilledCircle(matrix, centerX, centerY, CENTER_RADIUS + 8.0F, withAlpha(CENTER_RGB, 0.24F), SUBDIVISIONS);
        drawFilledCircle(matrix, centerX, centerY, CENTER_RADIUS, withAlpha(CENTER_RGB, 0.82F), SUBDIVISIONS);
    }

    private static void drawDividers(Matrix4f matrix, float centerX, float centerY) {
        int dividerColor = withAlpha(DIVIDER_RGB, 0.17F);
        drawRadialLine(matrix, centerX, centerY, 22.5F, CENTER_RADIUS + 6.0F, OUTER_RADIUS - 3.0F, 1.5F, dividerColor);
        drawRadialLine(matrix, centerX, centerY, 90.0F, CENTER_RADIUS + 6.0F, OUTER_RADIUS - 3.0F, 1.5F, dividerColor);
        drawRadialLine(matrix, centerX, centerY, 157.5F, CENTER_RADIUS + 6.0F, OUTER_RADIUS - 3.0F, 1.5F, dividerColor);
        drawRadialLine(matrix, centerX, centerY, 225.0F, CENTER_RADIUS + 6.0F, OUTER_RADIUS - 3.0F, 1.5F, dividerColor);
        drawRadialLine(matrix, centerX, centerY, 315.0F, CENTER_RADIUS + 6.0F, OUTER_RADIUS - 3.0F, 1.5F, dividerColor);
    }

    private static void drawBorders(Matrix4f matrix, float centerX, float centerY) {
        int borderColor = withAlpha(BORDER_RGB, 0.13F);
        drawAnnularSector(matrix, centerX, centerY, INNER_RADIUS - 1.0F, INNER_RADIUS, 0.0F, 360.0F, borderColor, SUBDIVISIONS);
        drawAnnularSector(matrix, centerX, centerY, OUTER_RADIUS - 1.0F, OUTER_RADIUS, 0.0F, 360.0F, borderColor, SUBDIVISIONS);
        drawAnnularSector(matrix, centerX, centerY, CENTER_RADIUS, CENTER_RADIUS + 1.5F, 0.0F, 360.0F, withAlpha(BORDER_RGB, 0.24F), SUBDIVISIONS);
    }

    private static void drawHighlights(Matrix4f matrix, float centerX, float centerY, PingSegment activeSegment, int teamColor) {
        for (PingSegment segment : PingSegment.values()) {
            VisualState state = STATES.get(segment);
            if (state.fillAlpha <= 0.001F && state.borderAlpha <= 0.001F && state.pointAlpha <= 0.001F) {
                continue;
            }

            if (segment == PingSegment.LOCATION) {
                if (state.fillAlpha > 0.001F) {
                    drawFilledCircle(matrix, centerX, centerY, CENTER_RADIUS, withAlpha(teamColor, state.fillAlpha), SUBDIVISIONS);
                }
                if (state.borderAlpha > 0.001F) {
                    drawFilledCircle(matrix, centerX, centerY, CENTER_RADIUS + 6.0F, withAlpha(teamColor, state.borderAlpha), SUBDIVISIONS);
                }
                if (state.pointAlpha > 0.001F) {
                    drawGlowPoint(matrix, centerX, centerY, teamColor, state.pointAlpha);
                }
                continue;
            }

            float[] range = segmentRange(segment);
            float start = range[0];
            float end = range[1];

            if (state.fillAlpha > 0.001F) {
                drawAnnularSector(matrix, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, start, end, withAlpha(teamColor, state.fillAlpha), SUBDIVISIONS);
            }
            if (state.borderAlpha > 0.001F) {
                drawAnnularSector(matrix, centerX, centerY, INNER_RADIUS - 2.0F, OUTER_RADIUS + 6.0F, start, end, withAlpha(teamColor, state.borderAlpha), SUBDIVISIONS);
            }
            if (state.pointAlpha > 0.001F) {
                float midpoint = midpointAngle(start, end);
                float pointRadius = (INNER_RADIUS + OUTER_RADIUS) * 0.5F + 4.0F;
                float radians = midpoint * Mth.DEG_TO_RAD;
                float pointX = centerX + Mth.cos(radians) * pointRadius;
                float pointY = centerY + Mth.sin(radians) * pointRadius;
                drawGlowPoint(matrix, pointX, pointY, teamColor, state.pointAlpha);
            }
        }
    }

    private static void drawSelector(Matrix4f matrix, float centerX, float centerY, PingWheelInputHandler inputHandler, int teamColor) {
        float selectorX = centerX + (float)inputHandler.getSelectorX();
        float selectorY = centerY + (float)inputHandler.getSelectorY();
        drawFilledCircle(matrix, selectorX, selectorY, 5.0F, withAlpha(teamColor, 0.90F), 16);
        drawFilledCircle(matrix, selectorX, selectorY, 9.0F, withAlpha(teamColor, 0.22F), 16);
    }

    private static void drawSegmentLabels(GuiGraphics graphics, Font font, float centerX, float centerY) {
        drawSegmentLabel(graphics, font, PingSegment.DANGER, "DANGER", 270.0F, centerX, centerY);
        drawSegmentLabel(graphics, font, PingSegment.RESOURCE, "RESOURCE", 180.0F, centerX, centerY);
        drawSegmentLabel(graphics, font, PingSegment.COMBAT, "COMBAT", 0.0F, centerX, centerY);
        drawSegmentLabel(graphics, font, PingSegment.LOOT, "LOOT", 135.0F, centerX, centerY);
        drawSegmentLabel(graphics, font, PingSegment.BASE, "BASE", 45.0F, centerX, centerY);
        drawCenterLabel(graphics, font, centerX, centerY);
    }

    private static void drawSegmentLabel(GuiGraphics graphics, Font font, PingSegment segment, String label, float angleDegrees, float centerX, float centerY) {
        VisualState state = STATES.get(segment);
        float radians = angleDegrees * Mth.DEG_TO_RAD;
        float iconX = centerX + Mth.cos(radians) * ICON_RADIUS;
        float iconY = centerY + Mth.sin(radians) * ICON_RADIUS;

        drawIcon(graphics, segment, iconX, iconY - 9.0F, state.iconScale);
        drawCenteredText(graphics, font, label, iconX, iconY + 14.0F, withAlpha(0xFFFFFF, state.labelAlpha));
    }

    private static void drawCenterLabel(GuiGraphics graphics, Font font, float centerX, float centerY) {
        VisualState state = STATES.get(PingSegment.LOCATION);
        drawIcon(graphics, PingSegment.LOCATION, centerX, centerY - 8.0F, state.iconScale);
        drawCenteredText(graphics, font, "LOCATION", centerX, centerY + 26.0F, withAlpha(0xFFFFFF, state.labelAlpha));
    }

    private static void drawInstruction(GuiGraphics graphics, Font font, float centerX, float centerY) {
        drawCenteredText(graphics, font, "Hold G - Release to ping", centerX, centerY + TEAM_ROW_OFFSET_Y + 24.0F, INSTRUCTION_COLOR);
    }

    private static void drawExtrasCustomizationMenu(GuiGraphics graphics, Font font, PingWheelInputHandler inputHandler) {
        int left = customPanelLeft(graphics.guiWidth());
        int top = customPanelTop(graphics.guiHeight());
        PingSegment selectedSegment = inputHandler.getCustomizationSegment();
        PingIconOption selectedIcon = inputHandler.getCustomizationIcon();

        graphics.fill(left, top, left + CUSTOM_PANEL_WIDTH, top + CUSTOM_PANEL_HEIGHT, 0xF0121826);
        drawRectBorder(graphics, left, top, CUSTOM_PANEL_WIDTH, CUSTOM_PANEL_HEIGHT, 0x55FFFFFF);
        graphics.fill(left, top, left + CUSTOM_PANEL_WIDTH, top + 28, 0x661B2742);
        graphics.drawString(font, "Extras do radial", left + 12, top + 10, 0xF4F7FF, false);
        graphics.drawString(font, "X", left + CUSTOM_PANEL_WIDTH - 20, top + 10, 0xCCFFFFFF, false);

        graphics.drawString(font, "Slot", left + 18, top + 32, 0x90FFFFFF, false);
        graphics.drawString(font, "Icones extras", left + CUSTOM_ICON_GRID_X, top + 32, 0x90FFFFFF, false);

        PingSegment[] segments = customizationSegments();
        for (int i = 0; i < segments.length; i++) {
            PingSegment segment = segments[i];
            int x = left + CUSTOM_SLOT_X;
            int y = top + CUSTOM_SLOT_Y + i * CUSTOM_SLOT_GAP;
            boolean selected = segment == selectedSegment;
            ResourceLocation icon = ClientExtraIconStore.resolveIconTexture(segment, ICONS.get(segment));
            graphics.fill(x, y, x + CUSTOM_SLOT_SIZE, y + CUSTOM_SLOT_SIZE, selected ? 0xCC27385A : 0xAA0D111B);
            drawRectBorder(graphics, x, y, CUSTOM_SLOT_SIZE, CUSTOM_SLOT_SIZE, selected ? 0xAAFFFFFF : 0x44FFFFFF);
            graphics.blit(icon, x + 4, y + 2, CUSTOM_ICON_SIZE, CUSTOM_ICON_SIZE + 5, 0.0F, 0.0F, 204, 256, 204, 256);
            graphics.drawString(font, segment.id().toUpperCase(), x + 36, y + 10, selected ? 0xFFFFFFFF : 0xA8FFFFFF, false);
        }

        for (int i = 0; i < PingIconOption.displayOrder().size(); i++) {
            PingIconOption option = PingIconOption.displayOrder().get(i);
            int column = i % CUSTOM_ICON_GRID_COLUMNS;
            int row = i / CUSTOM_ICON_GRID_COLUMNS;
            int x = left + CUSTOM_ICON_GRID_X + column * CUSTOM_ICON_GRID_GAP;
            int y = top + CUSTOM_ICON_GRID_Y + row * CUSTOM_ICON_GRID_GAP;
            boolean selected = option == selectedIcon;
            graphics.fill(x, y, x + CUSTOM_SLOT_SIZE, y + CUSTOM_SLOT_SIZE, selected ? 0xCC2E3B60 : 0xAA0D111B);
            drawRectBorder(graphics, x, y, CUSTOM_SLOT_SIZE, CUSTOM_SLOT_SIZE, selected ? 0xAAFFFFFF : 0x44FFFFFF);
            graphics.blit(option.texture(), x + 4, y + 4, CUSTOM_ICON_SIZE, CUSTOM_ICON_SIZE, 0.0F, 0.0F, 204, 256, 204, 256);
        }

        drawButton(graphics, font, left + 18, top + CUSTOM_BUTTON_Y, 58, CUSTOM_BUTTON_HEIGHT, "Limpar", 0xAA151B28);
        drawButton(graphics, font, left + 150, top + CUSTOM_BUTTON_Y, 86, CUSTOM_BUTTON_HEIGHT, "Substituir", 0xCC263A63);
        drawButton(graphics, font, left + 244, top + CUSTOM_BUTTON_Y, 58, CUSTOM_BUTTON_HEIGHT, "Salvar", 0xCC2C5A3A);
    }

    private static void drawExtrasButton(GuiGraphics graphics, PingWheelInputHandler inputHandler) {
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        int left = guiWidth - EXTRAS_BUTTON_MARGIN - EXTRAS_BUTTON_SIZE;
        int top = guiHeight - EXTRAS_BUTTON_MARGIN - EXTRAS_BUTTON_SIZE;
        double mouseX = guiWidth / 2.0D + inputHandler.getSelectorX();
        double mouseY = guiHeight / 2.0D + inputHandler.getSelectorY();
        boolean hovered = isExtrasButtonAt(mouseX, mouseY, guiWidth, guiHeight);
        int border = hovered ? EXTRAS_BUTTON_HOVER_BORDER : EXTRAS_BUTTON_BORDER;

        graphics.fill(left, top, left + EXTRAS_BUTTON_SIZE, top + EXTRAS_BUTTON_SIZE, hovered ? EXTRAS_BUTTON_HOVER_BACKGROUND : EXTRAS_BUTTON_BACKGROUND);
        graphics.fill(left, top, left + EXTRAS_BUTTON_SIZE, top + 1, border);
        graphics.fill(left, top + EXTRAS_BUTTON_SIZE - 1, left + EXTRAS_BUTTON_SIZE, top + EXTRAS_BUTTON_SIZE, border);
        graphics.fill(left, top, left + 1, top + EXTRAS_BUTTON_SIZE, border);
        graphics.fill(left + EXTRAS_BUTTON_SIZE - 1, top, left + EXTRAS_BUTTON_SIZE, top + EXTRAS_BUTTON_SIZE, border);
        graphics.blit(PingIconOption.USERS_GROUP.texture(), left + 5, top + 5, 16, 16, 0.0F, 0.0F, 204, 256, 204, 256);
    }

    private static void drawButton(GuiGraphics graphics, Font font, int x, int y, int width, int height, String label, int color) {
        graphics.fill(x, y, x + width, y + height, color);
        drawRectBorder(graphics, x, y, width, height, 0x66FFFFFF);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 7, 0xF4F7FF, false);
    }

    private static void drawTeamPicker(GuiGraphics graphics, Font font, float centerX, float centerY, PingWheelInputHandler inputHandler) {
        PingTeam[] teams = {PingTeam.BLUE, PingTeam.RED, PingTeam.GREEN, PingTeam.YELLOW};
        float rowY = centerY + TEAM_ROW_OFFSET_Y;
        float startX = centerX + TEAM_START_OFFSET_X;
        drawCenteredText(graphics, font, "Team color:", centerX + TEAM_LABEL_OFFSET_X, rowY - 4.0F, 0x90FFFFFF);

        for (int i = 0; i < teams.length; i++) {
            PingTeam team = teams[i];
            float swatchX = startX + TEAM_SPACING * i;
            boolean hovered = team == inputHandler.getHoveredTeam();
            boolean selected = team == inputHandler.getSelectedTeam();
            int radius = hovered ? TEAM_SWATCH_RADIUS + 3 : TEAM_SWATCH_RADIUS;

            if (selected) {
                drawGuiCircle(graphics, swatchX, rowY, radius + 5, 0x70FFFFFF);
            }
            drawGuiCircle(graphics, swatchX, rowY, radius + 2, 0xD8F4F7FF);
            drawGuiCircle(graphics, swatchX, rowY, radius, 0xFF000000 | team.color());
        }
    }

    private static void drawGlowPoint(Matrix4f matrix, float centerX, float centerY, int rgbColor, float alpha) {
        drawFilledCircle(matrix, centerX, centerY, 7.0F, withAlpha(rgbColor, alpha * 0.35F), SUBDIVISIONS);
        drawFilledCircle(matrix, centerX, centerY, 4.0F, withAlpha(rgbColor, alpha), SUBDIVISIONS);
    }

    private static void drawScaledText(GuiGraphics graphics, Font font, String text, float centerX, float centerY, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, color, false);
        graphics.pose().popPose();
    }

    private static void drawIcon(GuiGraphics graphics, PingSegment segment, float centerX, float centerY, float scale) {
        int width = Math.round(20.0F * scale);
        int height = Math.round(25.0F * scale);
        int x = Math.round(centerX - width / 2.0F);
        int y = Math.round(centerY - height / 2.0F);
        ResourceLocation icon = ClientExtraIconStore.resolveIconTexture(segment, ICONS.get(segment));
        graphics.blit(icon, x, y, width, height, 0.0F, 0.0F, 204, 256, 204, 256);
    }

    private static void drawIcon(GuiGraphics graphics, ResourceLocation icon, float centerX, float centerY, int width, int height) {
        int x = Math.round(centerX - width / 2.0F);
        int y = Math.round(centerY - height / 2.0F);
        graphics.blit(icon, x, y, width, height, 0.0F, 0.0F, 204, 256, 204, 256);
    }

    private static void drawCenteredText(GuiGraphics graphics, Font font, String text, float centerX, float centerY, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2.0F, centerY, color, false);
    }

    private static void drawRectBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static int customPanelLeft(int guiWidth) {
        return guiWidth / 2 - CUSTOM_PANEL_WIDTH / 2;
    }

    private static int customPanelTop(int guiHeight) {
        return guiHeight / 2 - CUSTOM_PANEL_HEIGHT / 2;
    }

    private static PingSegment[] customizationSegments() {
        return new PingSegment[] {
                PingSegment.DANGER,
                PingSegment.RESOURCE,
                PingSegment.COMBAT,
                PingSegment.LOOT,
                PingSegment.BASE,
                PingSegment.LOCATION
        };
    }

    private static void drawGuiCircle(GuiGraphics graphics, float centerX, float centerY, int radius, int argbColor) {
        int roundedCenterX = Math.round(centerX);
        int roundedCenterY = Math.round(centerY);
        for (int y = -radius; y <= radius; y++) {
            int width = Mth.floor(Math.sqrt(radius * radius - y * y));
            graphics.fill(roundedCenterX - width, roundedCenterY + y, roundedCenterX + width + 1, roundedCenterY + y + 1, argbColor);
        }
    }

    private static void drawAnnularSector(Matrix4f matrix, float centerX, float centerY, float innerRadius, float outerRadius, float startDegrees, float endDegrees, int argbColor, int subdivisions) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float start = startDegrees * Mth.DEG_TO_RAD;
        float end = endDegrees * Mth.DEG_TO_RAD;
        float sweep = angularSweep(start, end);

        for (int i = 0; i < subdivisions; i++) {
            float progress0 = i / (float)subdivisions;
            float progress1 = (i + 1) / (float)subdivisions;
            float angle0 = start + sweep * progress0;
            float angle1 = start + sweep * progress1;

            float outerX0 = centerX + Mth.cos(angle0) * outerRadius;
            float outerY0 = centerY + Mth.sin(angle0) * outerRadius;
            float outerX1 = centerX + Mth.cos(angle1) * outerRadius;
            float outerY1 = centerY + Mth.sin(angle1) * outerRadius;
            float innerX0 = centerX + Mth.cos(angle0) * innerRadius;
            float innerY0 = centerY + Mth.sin(angle0) * innerRadius;
            float innerX1 = centerX + Mth.cos(angle1) * innerRadius;
            float innerY1 = centerY + Mth.sin(angle1) * innerRadius;

            addTriangle(builder, matrix, outerX0, outerY0, outerX1, outerY1, innerX1, innerY1, argbColor);
            addTriangle(builder, matrix, outerX0, outerY0, innerX1, innerY1, innerX0, innerY0, argbColor);
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void drawFilledCircle(Matrix4f matrix, float centerX, float centerY, float radius, int argbColor, int subdivisions) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float step = Mth.TWO_PI / subdivisions;

        for (int i = 0; i < subdivisions; i++) {
            float angle0 = i * step;
            float angle1 = (i + 1) * step;
            float x0 = centerX + Mth.cos(angle0) * radius;
            float y0 = centerY + Mth.sin(angle0) * radius;
            float x1 = centerX + Mth.cos(angle1) * radius;
            float y1 = centerY + Mth.sin(angle1) * radius;
            addTriangle(builder, matrix, centerX, centerY, x0, y0, x1, y1, argbColor);
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void drawRadialLine(Matrix4f matrix, float centerX, float centerY, float angleDegrees, float innerRadius, float outerRadius, float thickness, int argbColor) {
        float radians = angleDegrees * Mth.DEG_TO_RAD;
        float dirX = Mth.cos(radians);
        float dirY = Mth.sin(radians);
        float normalX = -dirY * thickness * 0.5F;
        float normalY = dirX * thickness * 0.5F;
        float innerX = centerX + dirX * innerRadius;
        float innerY = centerY + dirY * innerRadius;
        float outerX = centerX + dirX * outerRadius;
        float outerY = centerY + dirY * outerRadius;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        addTriangle(builder, matrix, innerX - normalX, innerY - normalY, outerX - normalX, outerY - normalY, outerX + normalX, outerY + normalY, argbColor);
        addTriangle(builder, matrix, innerX - normalX, innerY - normalY, outerX + normalX, outerY + normalY, innerX + normalX, innerY + normalY, argbColor);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void addTriangle(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2, float x3, float y3, int argbColor) {
        builder.addVertex(matrix, x1, y1, 0.0F).setColor(argbColor);
        builder.addVertex(matrix, x2, y2, 0.0F).setColor(argbColor);
        builder.addVertex(matrix, x3, y3, 0.0F).setColor(argbColor);
    }

    private static float[] segmentRange(PingSegment segment) {
        return switch (segment) {
            case COMBAT -> new float[] {315.0F, 22.5F};
            case BASE -> new float[] {22.5F, 90.0F};
            case LOOT -> new float[] {90.0F, 157.5F};
            case RESOURCE -> new float[] {157.5F, 225.0F};
            case DANGER -> new float[] {225.0F, 315.0F};
            default -> new float[] {0.0F, 0.0F};
        };
    }

    private static PingSegment[] outerSegments() {
        return new PingSegment[] {
                PingSegment.DANGER,
                PingSegment.RESOURCE,
                PingSegment.COMBAT,
                PingSegment.LOOT,
                PingSegment.BASE
        };
    }

    private static float midpointAngle(float start, float end) {
        float normalizedStart = normalizeDegrees(start);
        float normalizedEnd = normalizeDegrees(end);
        float sweep = normalizedEnd - normalizedStart;
        if (sweep <= 0.0F) {
            sweep += 360.0F;
        }
        return normalizeDegrees(normalizedStart + sweep * 0.5F);
    }

    private static float angularSweep(float startRadians, float endRadians) {
        float sweep = endRadians - startRadians;
        if (sweep <= 0.0F) {
            sweep += Mth.TWO_PI;
        }
        return sweep;
    }

    private static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }

    private static int withAlpha(int rgbColor, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (a << 24) | (rgbColor & 0x00FFFFFF);
    }

    private static final class VisualState {
        private float fillAlpha;
        private float borderAlpha;
        private float pointAlpha;
        private float iconScale = 1.0F;
        private float labelAlpha = 0.55F;
    }
}
