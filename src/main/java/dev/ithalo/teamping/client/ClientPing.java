package dev.ithalo.teamping.client;

import java.util.Map;
import java.util.UUID;

import dev.ithalo.teamping.ping.PingAcknowledgement;

public record ClientPing(
        String id,
        String playerName,
        String teamId,
        String segmentId,
        String targetKind,
        String targetLabel,
        String targetIconId,
        int targetEntityId,
        String dimension,
        double x,
        double y,
        double z,
        int color,
        long expiresAtGameTime,
        Map<UUID, PingAcknowledgement> acknowledgements
) {
}
