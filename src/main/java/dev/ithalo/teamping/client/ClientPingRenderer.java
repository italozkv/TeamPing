package dev.ithalo.teamping.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

import dev.ithalo.teamping.TeamPing;
import dev.ithalo.teamping.config.TeamPingConfig;
import dev.ithalo.teamping.ping.PingAckType;
import dev.ithalo.teamping.ping.PingAcknowledgement;
import dev.ithalo.teamping.ping.PingTargetKind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector4f;

public final class ClientPingRenderer {
    private static final int BADGE_BACKGROUND = 0xC0181C24;
    private static final int BADGE_TEXT = 0xFFFFFFFF;
    private static final int PING_ICON_WIDTH = 24;
    private static final int PING_ICON_HEIGHT = 30;
    private static final int ENTITY_ICON_SIZE = 20;
    private static final List<ScreenPing> SCREEN_PINGS = new ArrayList<>();
    private static final Map<String, ResourceLocation> SEGMENT_ICONS = new HashMap<>();
    private static final Map<PingTargetKind, ResourceLocation> ENTITY_ICONS = new HashMap<>();

    static {
        SEGMENT_ICONS.put("danger", icon("danger"));
        SEGMENT_ICONS.put("resource", icon("resource"));
        SEGMENT_ICONS.put("combat", icon("combat"));
        SEGMENT_ICONS.put("loot", icon("loot"));
        SEGMENT_ICONS.put("base", icon("base"));
        SEGMENT_ICONS.put("location", icon("location"));
        ENTITY_ICONS.put(PingTargetKind.HOSTILE_MOB, icon("enemy"));
        ENTITY_ICONS.put(PingTargetKind.PASSIVE_MOB, icon("animal"));
    }

    private ClientPingRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        ClientPingManager.removeExpired(minecraft.level.getGameTime());
        String currentDimension = minecraft.level.dimension().location().toString();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        SCREEN_PINGS.clear();

        for (ClientPing ping : ClientPingManager.pings()) {
            if (!ping.dimension().equals(currentDimension)) {
                continue;
            }

            addScreenPing(minecraft, event, ping);
        }

        bufferSource.endBatch();
    }

    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        ClientPingManager.removeExpired(minecraft.level.getGameTime());
        String currentDimension = minecraft.level.dimension().location().toString();
        GuiGraphics graphics = event.getGuiGraphics();
        ClientPing focusedPing = closestPingToCenter(minecraft, TeamPingConfig.ACK_HUD_DETAILS_DISTANCE.get()).orElse(null);
        for (ScreenPing screenPing : SCREEN_PINGS) {
            if (screenPing.ping().dimension().equals(currentDimension)) {
                renderHudPing(minecraft, graphics, screenPing, screenPing.ping() == focusedPing);
            }
        }

        renderAckNotification(minecraft, graphics);
    }

    private static void addScreenPing(Minecraft minecraft, RenderLevelStageEvent event, ClientPing ping) {
        Vec3 position = pingDisplayPosition(minecraft, ping);
        Vector4f clip = new Vector4f(
                (float)(position.x() - event.getCamera().getPosition().x),
                (float)(position.y() - event.getCamera().getPosition().y),
                (float)(position.z() - event.getCamera().getPosition().z),
                1.0F
        );
        clip.mul(event.getModelViewMatrix());
        clip.mul(event.getProjectionMatrix());

        if (clip.w() <= 0.0F) {
            return;
        }

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        if (ndcX < -1.25F || ndcX > 1.25F || ndcY < -1.25F || ndcY > 1.25F) {
            return;
        }

        int screenX = Math.round((ndcX * 0.5F + 0.5F) * minecraft.getWindow().getGuiScaledWidth());
        int screenY = Math.round((0.5F - ndcY * 0.5F) * minecraft.getWindow().getGuiScaledHeight());
        SCREEN_PINGS.add(new ScreenPing(ping, screenX, screenY));
    }

    public static Optional<ClientPing> closestPingToCenter(Minecraft minecraft, double maxDistance) {
        if (minecraft.player == null) {
            return Optional.empty();
        }

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
        ClientPing bestPing = null;
        double bestScreenDistance = Double.MAX_VALUE;
        double maxDistanceSqr = maxDistance * maxDistance;

        for (ScreenPing screenPing : SCREEN_PINGS) {
            ClientPing ping = screenPing.ping();
            if (minecraft.player.position().distanceToSqr(pingBasePosition(minecraft, ping)) > maxDistanceSqr) {
                continue;
            }

            double dx = screenPing.x() - centerX;
            double dy = screenPing.y() - centerY;
            double screenDistance = dx * dx + dy * dy;
            if (screenDistance < bestScreenDistance) {
                bestPing = ping;
                bestScreenDistance = screenDistance;
            }
        }

        return Optional.ofNullable(bestPing);
    }

    private static void renderHudPing(Minecraft minecraft, GuiGraphics graphics, ScreenPing screenPing, boolean focused) {
        ClientPing ping = screenPing.ping();
        String text = formatDistance(minecraft, ping);
        int width = minecraft.font.width(text) + 18;
        int markerX = screenPing.x();
        int markerY = screenPing.y();
        int boxX = Math.max(8, Math.min(graphics.guiWidth() - width - 8, markerX - width / 2));
        int boxY = Math.max(8, markerY - 31);
        int teamColor = 0xFF000000 | ping.color();

        graphics.fill(boxX, boxY, boxX + width, boxY + 16, BADGE_BACKGROUND);
        graphics.fill(boxX, boxY, boxX + 3, boxY + 16, teamColor);
        graphics.drawString(minecraft.font, text, boxX + 9, boxY + 4, BADGE_TEXT, true);
        renderAckSummary(minecraft, graphics, ping, boxX, boxY + 18);
        drawPingIcon(graphics, ping, markerX, boxY + 31);
        if (focused && minecraft.player.position().distanceToSqr(pingBasePosition(minecraft, ping)) <= TeamPingConfig.ACK_HUD_DETAILS_DISTANCE.get() * TeamPingConfig.ACK_HUD_DETAILS_DISTANCE.get()) {
            renderAckDetails(minecraft, graphics, ping, markerX + 18, boxY + 28);
        }
    }

    private static String formatDistance(Minecraft minecraft, ClientPing ping) {
        if (minecraft.player == null) {
            return "";
        }

        int blocks = (int)Math.round(minecraft.player.position().distanceTo(pingBasePosition(minecraft, ping)));
        return blocks == 1 ? "1 bloco" : blocks + " blocos";
    }

    private static Vec3 pingBasePosition(Minecraft minecraft, ClientPing ping) {
        Entity entity = trackedEntity(minecraft, ping);
        if (entity != null) {
            return entity.position();
        }
        return new Vec3(ping.x(), ping.y(), ping.z());
    }

    private static Vec3 pingDisplayPosition(Minecraft minecraft, ClientPing ping) {
        Entity entity = trackedEntity(minecraft, ping);
        if (entity == null) {
            return new Vec3(ping.x(), ping.y() + 1.35D, ping.z());
        }

        double x = (entity.getBoundingBox().minX + entity.getBoundingBox().maxX) * 0.5D;
        double y = entity instanceof ItemEntity ? entity.getBoundingBox().maxY + 0.45D : entity.getBoundingBox().maxY + 0.35D;
        double z = (entity.getBoundingBox().minZ + entity.getBoundingBox().maxZ) * 0.5D;
        return new Vec3(x, y, z);
    }

    private static Entity trackedEntity(Minecraft minecraft, ClientPing ping) {
        if (minecraft.level == null || ping.targetEntityId() < 0) {
            return null;
        }

        Entity entity = minecraft.level.getEntity(ping.targetEntityId());
        if (entity == null || entity.isRemoved()) {
            return null;
        }
        return entity;
    }

    private static void drawPingIcon(GuiGraphics graphics, ClientPing ping, int centerX, int centerY) {
        PingTargetKind targetKind = PingTargetKind.byId(ping.targetKind()).orElse(PingTargetKind.BLOCK);
        if (targetKind == PingTargetKind.ITEM) {
            drawItemIcon(graphics, ping, centerX, centerY);
            return;
        }

        ResourceLocation entityIcon = ENTITY_ICONS.get(targetKind);
        if (entityIcon != null) {
            drawStaticIcon(graphics, entityIcon, centerX, centerY, ENTITY_ICON_SIZE, ENTITY_ICON_SIZE);
            return;
        }

        dev.ithalo.teamping.ping.PingIconOption.byId(ping.targetIconId()).ifPresentOrElse(
                option -> drawStaticIcon(graphics, option.texture(), centerX, centerY, ENTITY_ICON_SIZE, ENTITY_ICON_SIZE),
                () -> {
                    ResourceLocation icon = SEGMENT_ICONS.getOrDefault(
                            ping.segmentId().toLowerCase(Locale.ROOT),
                            SEGMENT_ICONS.get("location")
                    );
                    drawStaticIcon(graphics, icon, centerX, centerY, PING_ICON_WIDTH, PING_ICON_HEIGHT);
                }
        );
    }

    private static void drawItemIcon(GuiGraphics graphics, ClientPing ping, int centerX, int centerY) {
        ResourceLocation itemId = ResourceLocation.tryParse(ping.targetIconId());
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            drawStaticIcon(graphics, SEGMENT_ICONS.get("loot"), centerX, centerY, ENTITY_ICON_SIZE, ENTITY_ICON_SIZE + 5);
            return;
        }

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
        int x = centerX - 8;
        int y = centerY - 8;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    private static void renderAckSummary(Minecraft minecraft, GuiGraphics graphics, ClientPing ping, int x, int y) {
        if (ping.acknowledgements().isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (PingAckType type : PingAckType.values()) {
            long count = ping.acknowledgements().values().stream().filter(ack -> ack.type() == type).count();
            if (count <= 0) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("  ");
            }
            builder.append(type.shortLabel()).append(' ').append(count);
        }

        if (builder.isEmpty()) {
            return;
        }

        String text = builder.toString();
        int width = minecraft.font.width(text) + 10;
        graphics.fill(x, y, x + width, y + 14, 0xA8181C24);
        graphics.drawString(minecraft.font, text, x + 5, y + 3, 0xFFFFFFFF, true);
    }

    private static void renderAckDetails(Minecraft minecraft, GuiGraphics graphics, ClientPing ping, int x, int y) {
        if (ping.acknowledgements().isEmpty()) {
            return;
        }

        List<AckGroup> groups = ackGroups(ping);
        int width = 164;
        int height = 30 + groups.size() * 16;
        graphics.fill(x, y, x + width, y + height, 0xC0181C24);
        graphics.fill(x, y, x + 3, y + height, 0xFF000000 | ping.color());
        graphics.drawString(minecraft.font, ping.segmentId().toUpperCase(Locale.ROOT), x + 6, y + 5, 0xFFFFFFFF, true);
        graphics.drawString(minecraft.font, "By " + ping.playerName(), x + 6, y + 15, 0xFFBFC7D5, false);

        int lineY = y + 31;
        for (AckGroup group : groups) {
            int typeColor = 0xFF000000 | group.type().color();
            String count = group.type().shortLabel() + " " + group.names().size();
            String names = compactNames(group.names());
            graphics.fill(x + 6, lineY - 2, x + 31, lineY + 11, 0xAA0D111B);
            graphics.drawString(minecraft.font, count, x + 10, lineY + 1, typeColor, true);
            graphics.drawString(minecraft.font, trimToWidth(minecraft, names, width - 44), x + 38, lineY + 1, 0xFFE6EBF5, false);
            lineY += 16;
        }
    }

    private static List<AckGroup> ackGroups(ClientPing ping) {
        List<AckGroup> groups = new ArrayList<>();
        for (PingAckType type : PingAckType.values()) {
            List<String> names = new ArrayList<>();
            for (PingAcknowledgement acknowledgement : ping.acknowledgements().values()) {
                if (acknowledgement.type() == type) {
                    names.add(acknowledgement.playerName());
                }
            }
            if (!names.isEmpty()) {
                groups.add(new AckGroup(type, names));
            }
        }
        return groups;
    }

    private static String compactNames(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        if (names.size() == 2) {
            return names.get(0) + ", " + names.get(1);
        }
        return names.get(0) + ", " + names.get(1) + " +" + (names.size() - 2);
    }

    private static String trimToWidth(Minecraft minecraft, String text, int maxWidth) {
        if (minecraft.font.width(text) <= maxWidth) {
            return text;
        }
        return minecraft.font.plainSubstrByWidth(text, Math.max(0, maxWidth - minecraft.font.width("..."))) + "...";
    }

    private static void renderAckNotification(Minecraft minecraft, GuiGraphics graphics) {
        String notification = ClientPingManager.ackNotification();
        if (notification.isBlank()) {
            return;
        }

        int width = minecraft.font.width(notification) + 18;
        int x = (graphics.guiWidth() - width) / 2;
        int y = 26;
        graphics.fill(x, y, x + width, y + 16, 0xC0181C24);
        graphics.drawString(minecraft.font, notification, x + 9, y + 4, 0xFFFFFFFF, true);
    }

    private static void drawStaticIcon(GuiGraphics graphics, ResourceLocation icon, int centerX, int centerY, int width, int height) {
        int x = centerX - width / 2;
        int y = centerY - height / 2;
        graphics.fill(centerX - 3, y + height - 2, centerX + 4, y + height + 1, 0x66000000);
        graphics.blit(icon, x, y, width, height, 0.0F, 0.0F, 204, 256, 204, 256);
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "textures/gui/pings/" + name + ".png");
    }

    private record ScreenPing(ClientPing ping, int x, int y) {
    }

    private record AckGroup(PingAckType type, List<String> names) {
    }
}
