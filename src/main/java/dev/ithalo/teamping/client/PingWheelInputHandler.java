package dev.ithalo.teamping.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import java.util.Optional;

import dev.ithalo.teamping.config.TeamPingConfig;
import dev.ithalo.teamping.network.C2SPingAckPayload;
import dev.ithalo.teamping.network.CreatePingPayload;
import dev.ithalo.teamping.network.SetPingTeamPayload;
import dev.ithalo.teamping.ping.PingAckType;
import dev.ithalo.teamping.ping.PingIconOption;
import dev.ithalo.teamping.ping.PingSegment;
import dev.ithalo.teamping.ping.PingTargetKind;
import dev.ithalo.teamping.team.PingTeam;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class PingWheelInputHandler {
    private static PingWheelInputHandler instance;
    private static final long HOLD_TO_OPEN_MS = 150L;

    private final KeyMapping pingKey = new KeyMapping(
            "key.team.ping",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.teamping"
    );
    private final KeyMapping ackWheelKey = new KeyMapping(
            "key.teamping.ack_wheel",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "key.categories.teamping"
    );

    private boolean wheelOpen;
    private boolean ackWheelOpen;
    private boolean keyHeld;
    private boolean pingHandledByTick;
    private long keyPressedAtMs;
    private PingSegment activeSegment;
    private PingAckType activeAckType;
    private ClientPing ackWheelTarget;
    private PingTeam hoveredTeam;
    private PingTeam selectedTeam = PingTeam.BLUE;
    private double selectorX;
    private double selectorY;
    private boolean extrasButtonMouseDown;
    private boolean extrasMenuOpen;
    private PingSegment customizationSegment = PingSegment.LOCATION;
    private PingIconOption customizationIcon = PingIconOption.QUESTION_MARK;
    private long lastAckSentAtMs;

    public PingWheelInputHandler() {
        instance = this;
    }

    public static PingWheelInputHandler getInstance() {
        return instance;
    }

    public void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(pingKey);
        event.register(ackWheelKey);
    }

    public void onKeyInput(net.neoforged.neoforge.client.event.InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        if (ackWheelKey.matches(event.getKey(), event.getScanCode())) {
            if (event.getAction() == InputConstants.PRESS) {
                openAckWheel(minecraft);
            } else if (event.getAction() == InputConstants.RELEASE) {
                confirmAckWheel(minecraft);
            }
            return;
        }

        if (!ackWheelOpen && event.getAction() == InputConstants.PRESS && handleAckShortcut(minecraft, event.getKey())) {
            return;
        }

        if (!pingKey.matches(event.getKey(), event.getScanCode())) {
            return;
        }

        if (event.getAction() == InputConstants.PRESS) {
            pingHandledByTick = false;
            keyHeld = true;
            keyPressedAtMs = Util.getMillis();
        } else if (event.getAction() == InputConstants.RELEASE) {
            if (pingHandledByTick) {
                pingHandledByTick = false;
                return;
            }
            boolean wasWheelOpen = wheelOpen;
            keyHeld = false;
            if (!wasWheelOpen) {
                sendQuickPing(minecraft);
                return;
            }
            if (extrasMenuOpen) {
                closeWheel();
                return;
            }
            refreshActiveSegment(minecraft);
            if (hoveredTeam != null) {
                selectTeam(hoveredTeam);
                PacketDistributor.sendToServer(new SetPingTeamPayload(hoveredTeam.id()));
                playConfirmSound(minecraft);
            } else if (activeSegment != null) {
                sendSelectedPing(minecraft, activeSegment);
                playConfirmSound(minecraft);
            }
            closeWheel();
        }
    }

    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        if (ackWheelOpen) {
            refreshAckWheelSelection(minecraft);
            confirmAckWheelIfKeyReleased(minecraft);
            return;
        }

        handlePingKeyPolling(minecraft);
        if (!keyHeld) {
            return;
        }

        if (wheelOpen) {
            checkWheelMouseClick(minecraft);
            return;
        }

        if (Util.getMillis() - keyPressedAtMs >= HOLD_TO_OPEN_MS) {
            openWheel(minecraft);
        }
    }

    private void handlePingKeyPolling(Minecraft minecraft) {
        boolean pingDown = pingKey.isDown()
                || InputConstants.isKeyDown(minecraft.getWindow().getWindow(), InputConstants.KEY_G);

        if (pingDown) {
            if (!keyHeld) {
                pingHandledByTick = false;
                keyHeld = true;
                keyPressedAtMs = Util.getMillis();
            }
            return;
        }

        if (!keyHeld) {
            return;
        }

        pingHandledByTick = true;
        boolean wasWheelOpen = wheelOpen;
        keyHeld = false;
        if (!wasWheelOpen) {
            sendQuickPing(minecraft);
            return;
        }
        if (extrasMenuOpen) {
            closeWheel();
            return;
        }
        refreshActiveSegment(minecraft);
        if (hoveredTeam != null) {
            selectTeam(hoveredTeam);
            PacketDistributor.sendToServer(new SetPingTeamPayload(hoveredTeam.id()));
            playConfirmSound(minecraft);
        } else if (activeSegment != null) {
            sendSelectedPing(minecraft, activeSegment);
            playConfirmSound(minecraft);
        }
        closeWheel();
    }

    public void onMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        if (event.getButton() != InputConstants.MOUSE_BUTTON_LEFT || event.getAction() != InputConstants.PRESS) {
            return;
        }

        if (!wheelOpen) {
            return;
        }

        double mouseX = mouseGuiX(minecraft);
        double mouseY = mouseGuiY(minecraft);
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (handleWheelClick(minecraft, mouseX, mouseY, guiWidth, guiHeight)) {
            extrasButtonMouseDown = true;
            return;
        }

        refreshActiveSegment(minecraft);
        if (hoveredTeam != null) {
            selectTeam(hoveredTeam);
            PacketDistributor.sendToServer(new SetPingTeamPayload(hoveredTeam.id()));
            playConfirmSound(minecraft);
        }
    }

    public void refreshActiveSegment(Minecraft minecraft) {
        if (!wheelOpen) {
            return;
        }

        if (minecraft.player == null || minecraft.level == null) {
            activeSegment = null;
            return;
        }

        Window window = minecraft.getWindow();
        if (window.getScreenWidth() <= 0 || window.getScreenHeight() <= 0) {
            activeSegment = null;
            return;
        }

        double scaledMouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / (double)window.getScreenWidth();
        double scaledMouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / (double)window.getScreenHeight();
        selectorX = scaledMouseX - window.getGuiScaledWidth() / 2.0D;
        selectorY = scaledMouseY - window.getGuiScaledHeight() / 2.0D;
        if (extrasMenuOpen) {
            hoveredTeam = null;
            activeSegment = null;
            return;
        }

        if (PingWheelRenderer.isExtrasButtonAt(scaledMouseX, scaledMouseY, window.getGuiScaledWidth(), window.getGuiScaledHeight())) {
            hoveredTeam = null;
            activeSegment = null;
            return;
        }

        hoveredTeam = teamAt(selectorX, selectorY);
        if (hoveredTeam != null) {
            activeSegment = null;
            return;
        }

        double distance = Math.sqrt(selectorX * selectorX + selectorY * selectorY);
        double angle = Math.toDegrees(Math.atan2(selectorY, selectorX));
        if (angle < 0.0D) {
            angle += 360.0D;
        }

        activeSegment = PingSegment.fromWheelSelection(angle, distance, 32.0D);
    }

    public void confirmIfKeyReleased(Minecraft minecraft) {
        if (!wheelOpen || minecraft.player == null || minecraft.level == null) {
            return;
        }

        boolean keyDown = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), InputConstants.KEY_G);
        if (keyDown) {
            return;
        }

        if (extrasMenuOpen) {
            closeWheel();
            return;
        }

        refreshActiveSegment(minecraft);
        if (hoveredTeam != null) {
            selectTeam(hoveredTeam);
            PacketDistributor.sendToServer(new SetPingTeamPayload(hoveredTeam.id()));
            playConfirmSound(minecraft);
        } else if (activeSegment != null) {
            sendSelectedPing(minecraft, activeSegment);
            playConfirmSound(minecraft);
        }
        closeWheel();
    }

    private void sendSelectedPing(Minecraft minecraft, PingSegment segment) {
        SelectedTarget target = findSelectedTarget(minecraft, segment);
        PingSegment pingType = mapSegmentToPingType(segment);
        String iconId = ClientExtraIconStore.resolveIconId(segment);
        PacketDistributor.sendToServer(new CreatePingPayload(
                target.position().x(),
                target.position().y(),
                target.position().z(),
                pingType.id(),
                target.kind().id(),
                target.label(),
                iconId,
                target.entityId()
        ));
    }

    private void sendQuickPing(Minecraft minecraft) {
        SelectedTarget target = findQuickTarget(minecraft);
        String iconId = ClientExtraIconStore.resolveIconId(PingSegment.LOCATION);
        PacketDistributor.sendToServer(new CreatePingPayload(
                target.position().x(),
                target.position().y(),
                target.position().z(),
                PingSegment.LOCATION.id(),
                target.kind().id(),
                target.label(),
                iconId,
                target.entityId()
        ));
        playConfirmSound(minecraft);
    }

    private boolean handleAckShortcut(Minecraft minecraft, int key) {
        PingAckType ackType = switch (key) {
            case InputConstants.KEY_Z -> PingAckType.UNDERSTOOD;
            case InputConstants.KEY_X -> PingAckType.ON_MY_WAY;
            case InputConstants.KEY_C -> PingAckType.NEGATIVE;
            case InputConstants.KEY_V -> PingAckType.CHECKING;
            default -> null;
        };

        if (ackType == null || !TeamPingConfig.ENABLE_ACKNOWLEDGEMENTS.get()) {
            return false;
        }

        closestAckTarget(minecraft).ifPresent(ping -> sendAck(minecraft, ping, ackType));
        return true;
    }

    private Optional<ClientPing> closestAckTarget(Minecraft minecraft) {
        if (!TeamPingConfig.ENABLE_ACKNOWLEDGEMENTS.get()) {
            return Optional.empty();
        }
        return ClientPingRenderer.closestPingToCenter(minecraft, TeamPingConfig.ACK_INTERACTION_DISTANCE.get());
    }

    private void sendAck(Minecraft minecraft, ClientPing ping, PingAckType ackType) {
        long now = Util.getMillis();
        if (now - lastAckSentAtMs < TeamPingConfig.ACK_RESPONSE_COOLDOWN_MS.get()) {
            return;
        }

        lastAckSentAtMs = now;
        PacketDistributor.sendToServer(new C2SPingAckPayload(java.util.UUID.fromString(ping.id()), ackType));
        playConfirmSound(minecraft);
    }

    private SelectedTarget findSelectedTarget(Minecraft minecraft, PingSegment segment) {
        double maxDistance = Math.max(1.0D, TeamPingConfig.MAX_PING_DISTANCE.get());
        Vec3 eyePosition = minecraft.player.getEyePosition(1.0F);
        Vec3 viewVector = minecraft.player.getViewVector(1.0F);
        Vec3 fallback = eyePosition.add(viewVector.scale(maxDistance));

        SelectedTarget entityTarget = findEntityTarget(minecraft, eyePosition, fallback);
        if (entityTarget != null) {
            return entityTarget;
        }

        HitResult hitResult = minecraft.level.clip(new ClipContext(
                eyePosition,
                fallback,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));

        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
            return new SelectedTarget(hitResult.getLocation(), PingTargetKind.BLOCK, "", segment.id(), -1);
        }
        return new SelectedTarget(fallback, PingTargetKind.BLOCK, "", segment.id(), -1);
    }

    private SelectedTarget findQuickTarget(Minecraft minecraft) {
        double maxDistance = Math.max(1.0D, TeamPingConfig.MAX_PING_DISTANCE.get());
        Vec3 eyePosition = minecraft.player.getEyePosition(1.0F);
        Vec3 viewVector = minecraft.player.getViewVector(1.0F);
        Vec3 fallback = eyePosition.add(viewVector.scale(maxDistance));
        SelectedTarget entityTarget = findEntityTarget(minecraft, eyePosition, fallback);
        if (entityTarget != null) {
            return entityTarget;
        }

        HitResult hitResult = minecraft.level.clip(new ClipContext(
                eyePosition,
                fallback,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));

        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
            return new SelectedTarget(hitResult.getLocation(), PingTargetKind.BLOCK, "", PingSegment.LOCATION.id(), -1);
        }
        return new SelectedTarget(fallback, PingTargetKind.BLOCK, "", PingSegment.LOCATION.id(), -1);
    }

    private void playConfirmSound(Minecraft minecraft) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private PingSegment mapSegmentToPingType(PingSegment segment) {
        return switch (segment) {
            case DANGER -> PingSegment.DANGER;
            case RESOURCE -> PingSegment.RESOURCE;
            case COMBAT -> PingSegment.COMBAT;
            case LOOT -> PingSegment.LOOT;
            case BASE -> PingSegment.BASE;
            case LOCATION -> PingSegment.LOCATION;
        };
    }

    private SelectedTarget findEntityTarget(Minecraft minecraft, Vec3 eyePosition, Vec3 fallback) {
        Vec3 ray = fallback.subtract(eyePosition);
        AABB searchBox = minecraft.player.getBoundingBox().expandTowards(ray).inflate(2.0D);
        Entity bestEntity = null;
        Vec3 bestHit = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity candidate : minecraft.level.getEntities(minecraft.player, searchBox, this::isPingEntityTarget)) {
            AABB hitBox = candidate.getBoundingBox().inflate(hitInflation(candidate));
            Optional<Vec3> hit = hitBox.clip(eyePosition, fallback);
            if (hit.isEmpty()) {
                continue;
            }

            double distance = eyePosition.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                bestEntity = candidate;
                bestHit = hit.get();
                bestDistance = distance;
            }
        }

        if (bestEntity == null || bestHit == null) {
            return null;
        }

        if (bestEntity instanceof ItemEntity itemEntity) {
            String itemId = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString();
            return new SelectedTarget(
                    bestHit,
                    PingTargetKind.ITEM,
                    itemEntity.getItem().getHoverName().getString(),
                    itemId,
                    bestEntity.getId()
            );
        }

        if (bestEntity instanceof Monster) {
            return new SelectedTarget(
                    bestHit,
                    PingTargetKind.HOSTILE_MOB,
                    bestEntity.getDisplayName().getString(),
                    "",
                    bestEntity.getId()
            );
        }

        return new SelectedTarget(
                bestHit,
                PingTargetKind.PASSIVE_MOB,
                bestEntity.getDisplayName().getString(),
                "",
                bestEntity.getId()
        );
    }

    private boolean isPingEntityTarget(Entity entity) {
        return entity != Minecraft.getInstance().player
                && !(entity instanceof Player)
                && entity.isAlive()
                && (entity instanceof ItemEntity || entity instanceof LivingEntity);
    }

    private double hitInflation(Entity entity) {
        if (entity instanceof ItemEntity) {
            return 0.65D;
        }
        return Math.max(0.35D, entity.getPickRadius() + 0.35D);
    }

    public boolean isWheelOpen() {
        return wheelOpen;
    }

    public boolean isAckWheelOpen() {
        return ackWheelOpen;
    }

    public PingSegment getActiveSegment() {
        return activeSegment;
    }

    public PingAckType getActiveAckType() {
        return activeAckType;
    }

    public ClientPing getAckWheelTarget() {
        return ackWheelTarget;
    }

    public PingTeam getHoveredTeam() {
        return hoveredTeam;
    }

    public PingTeam getSelectedTeam() {
        return selectedTeam;
    }

    public double getSelectorX() {
        return selectorX;
    }

    public double getSelectorY() {
        return selectorY;
    }

    public boolean isExtrasMenuOpen() {
        return extrasMenuOpen;
    }

    public PingSegment getCustomizationSegment() {
        return customizationSegment;
    }

    public PingIconOption getCustomizationIcon() {
        return customizationIcon;
    }

    private void openWheel(Minecraft minecraft) {
        if (ackWheelOpen) {
            return;
        }
        wheelOpen = true;
        activeSegment = PingSegment.LOCATION;
        hoveredTeam = null;
        selectorX = 0.0D;
        selectorY = 0.0D;
        minecraft.mouseHandler.releaseMouse();
    }

    private void closeWheel() {
        wheelOpen = false;
        keyHeld = false;
        activeSegment = null;
        hoveredTeam = null;
        selectorX = 0.0D;
        selectorY = 0.0D;
        extrasButtonMouseDown = false;
        extrasMenuOpen = false;
        Minecraft.getInstance().mouseHandler.grabMouse();
    }

    private void openAckWheel(Minecraft minecraft) {
        if (wheelOpen || ackWheelOpen) {
            return;
        }

        Optional<ClientPing> target = closestAckTarget(minecraft);
        if (target.isEmpty()) {
            return;
        }

        ackWheelOpen = true;
        ackWheelTarget = target.get();
        activeAckType = PingAckType.UNDERSTOOD;
        selectorX = 0.0D;
        selectorY = 0.0D;
        minecraft.mouseHandler.releaseMouse();
    }

    private void closeAckWheel() {
        ackWheelOpen = false;
        activeAckType = null;
        ackWheelTarget = null;
        selectorX = 0.0D;
        selectorY = 0.0D;
        Minecraft.getInstance().mouseHandler.grabMouse();
    }

    private void confirmAckWheel(Minecraft minecraft) {
        if (!ackWheelOpen) {
            return;
        }

        refreshAckWheelSelection(minecraft);
        if (ackWheelTarget != null && activeAckType != null) {
            sendAck(minecraft, ackWheelTarget, activeAckType);
        }
        closeAckWheel();
    }

    public void confirmAckWheelIfKeyReleased(Minecraft minecraft) {
        if (!ackWheelOpen || minecraft.player == null || minecraft.level == null) {
            return;
        }

        boolean keyDown = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), InputConstants.KEY_R);
        if (!keyDown) {
            confirmAckWheel(minecraft);
        }
    }

    public void refreshAckWheelSelection(Minecraft minecraft) {
        if (!ackWheelOpen) {
            return;
        }

        Window window = minecraft.getWindow();
        if (window.getScreenWidth() <= 0 || window.getScreenHeight() <= 0) {
            activeAckType = PingAckType.UNDERSTOOD;
            return;
        }

        double scaledMouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / (double)window.getScreenWidth();
        double scaledMouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / (double)window.getScreenHeight();
        selectorX = scaledMouseX - window.getGuiScaledWidth() / 2.0D;
        selectorY = scaledMouseY - window.getGuiScaledHeight() / 2.0D;
        activeAckType = ackTypeAt(selectorX, selectorY);
    }

    private PingAckType ackTypeAt(double x, double y) {
        double distance = Math.sqrt(x * x + y * y);
        if (distance < 18.0D) {
            return PingAckType.UNDERSTOOD;
        }

        double angle = Math.toDegrees(Math.atan2(y, x));
        if (angle < 0.0D) {
            angle += 360.0D;
        }

        if (angle >= 225.0D && angle < 315.0D) {
            return PingAckType.UNDERSTOOD;
        }
        if (angle >= 135.0D && angle < 225.0D) {
            return PingAckType.ON_MY_WAY;
        }
        if (angle >= 45.0D && angle < 135.0D) {
            return PingAckType.CHECKING;
        }
        return PingAckType.NEGATIVE;
    }

    private void openExtrasMenu(Minecraft minecraft) {
        activeSegment = null;
        hoveredTeam = null;
        selectorX = 0.0D;
        selectorY = 0.0D;
        extrasButtonMouseDown = false;
        wheelOpen = true;
        extrasMenuOpen = true;
        customizationSegment = PingSegment.LOCATION;
        customizationIcon = PingIconOption.byId(ClientExtraIconStore.resolveIconId(customizationSegment))
                .orElse(PingIconOption.QUESTION_MARK);
        minecraft.mouseHandler.releaseMouse();
    }

    private void checkWheelMouseClick(Minecraft minecraft) {
        long windowHandle = minecraft.getWindow().getWindow();
        boolean mouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!mouseDown) {
            extrasButtonMouseDown = false;
            return;
        }

        if (extrasButtonMouseDown) {
            return;
        }

        if (handleWheelClick(
                minecraft,
                mouseGuiX(minecraft),
                mouseGuiY(minecraft),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight())) {
            extrasButtonMouseDown = true;
        }
    }

    private boolean handleWheelClick(Minecraft minecraft, double mouseX, double mouseY, int guiWidth, int guiHeight) {
        if (extrasMenuOpen) {
            if (PingWheelRenderer.isExtrasCloseButtonAt(mouseX, mouseY, guiWidth, guiHeight)) {
                extrasMenuOpen = false;
                playConfirmSound(minecraft);
                return true;
            }

            PingSegment segment = PingWheelRenderer.customizationSegmentAt(mouseX, mouseY, guiWidth, guiHeight);
            if (segment != null) {
                customizationSegment = segment;
                customizationIcon = PingIconOption.byId(ClientExtraIconStore.resolveIconId(segment))
                        .orElse(PingIconOption.QUESTION_MARK);
                playConfirmSound(minecraft);
                return true;
            }

            PingIconOption icon = PingWheelRenderer.customizationIconAt(mouseX, mouseY, guiWidth, guiHeight);
            if (icon != null) {
                customizationIcon = icon;
                playConfirmSound(minecraft);
                return true;
            }

            if (PingWheelRenderer.isCustomizationReplaceButtonAt(mouseX, mouseY, guiWidth, guiHeight)
                    || PingWheelRenderer.isCustomizationSaveButtonAt(mouseX, mouseY, guiWidth, guiHeight)) {
                ClientExtraIconStore.assignIcon(customizationSegment, customizationIcon.id());
                playConfirmSound(minecraft);
                return true;
            }

            if (PingWheelRenderer.isCustomizationClearButtonAt(mouseX, mouseY, guiWidth, guiHeight)) {
                ClientExtraIconStore.clearIcon(customizationSegment);
                customizationIcon = PingIconOption.QUESTION_MARK;
                playConfirmSound(minecraft);
                return true;
            }

            return true;
        }

        if (PingWheelRenderer.isExtrasButtonAt(mouseX, mouseY, guiWidth, guiHeight)) {
            openExtrasMenu(minecraft);
            playConfirmSound(minecraft);
            return true;
        }

        return false;
    }

    private void selectTeam(PingTeam team) {
        selectedTeam = team;
    }

    private record SelectedTarget(Vec3 position, PingTargetKind kind, String label, String iconId, int entityId) {
    }

    private PingTeam teamAt(double x, double y) {
        double rowY = 160.0D;
        double startX = 12.0D;
        double spacing = 28.0D;
        double radius = 12.0D;
        PingTeam[] teams = {PingTeam.BLUE, PingTeam.RED, PingTeam.GREEN, PingTeam.YELLOW};

        for (int i = 0; i < teams.length; i++) {
            double dx = x - (startX + spacing * i);
            double dy = y - rowY;
            if (dx * dx + dy * dy <= radius * radius) {
                return teams[i];
            }
        }
        return null;
    }

    private double mouseGuiX(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        return minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / (double)window.getScreenWidth();
    }

    private double mouseGuiY(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        return minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / (double)window.getScreenHeight();
    }
}
