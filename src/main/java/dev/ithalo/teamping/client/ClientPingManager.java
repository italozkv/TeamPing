package dev.ithalo.teamping.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.ithalo.teamping.config.TeamPingConfig;
import dev.ithalo.teamping.network.S2CPingAckUpdatePayload;
import dev.ithalo.teamping.network.ShowPingPayload;
import dev.ithalo.teamping.ping.PingAcknowledgement;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public final class ClientPingManager {
    private static final Map<String, ClientPing> PINGS = new LinkedHashMap<>();
    private static String ackNotification = "";
    private static long ackNotificationExpiresAtMs;

    private ClientPingManager() {
    }

    public static void addPing(ShowPingPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long expiresAt = minecraft.level.getGameTime() + payload.durationTicks();
        PINGS.put(payload.id(), new ClientPing(
                payload.id(),
                payload.playerName(),
                payload.teamId(),
                payload.segmentId(),
                payload.targetKind(),
                payload.targetLabel(),
                payload.targetIconId(),
                payload.targetEntityId(),
                payload.dimension(),
                payload.x(),
                payload.y(),
                payload.z(),
                payload.color(),
                expiresAt,
                new LinkedHashMap<>()
        ));
    }

    public static void updateAcknowledgement(S2CPingAckUpdatePayload payload) {
        ClientPing ping = PINGS.get(payload.pingId().toString());
        if (ping == null) {
            return;
        }

        PingAcknowledgement acknowledgement = new PingAcknowledgement(
                payload.pingId(),
                payload.playerId(),
                payload.playerName(),
                payload.ackType(),
                payload.timestamp()
        );
        ping.acknowledgements().put(payload.playerId(), acknowledgement);

        if (TeamPingConfig.SHOW_ACK_NOTIFICATIONS.get()) {
            ackNotification = payload.playerName() + ": " + payload.ackType().id();
            ackNotificationExpiresAtMs = System.currentTimeMillis() + 1800L;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (TeamPingConfig.PLAY_ACK_SOUND.get() && minecraft.level != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.25F));
        }
    }

    public static Optional<ClientPing> getPing(UUID pingId) {
        return Optional.ofNullable(PINGS.get(pingId.toString()));
    }

    public static Collection<ClientPing> pings() {
        return PINGS.values();
    }

    public static String ackNotification() {
        return System.currentTimeMillis() <= ackNotificationExpiresAtMs ? ackNotification : "";
    }

    public static void removeExpired(long gameTime) {
        PINGS.values().removeIf(ping -> ping.expiresAtGameTime() <= gameTime);
    }
}
