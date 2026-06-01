package dev.ithalo.teamping.server;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.ithalo.teamping.config.TeamPingConfig;
import dev.ithalo.teamping.network.C2SPingAckPayload;
import dev.ithalo.teamping.network.CreatePingPayload;
import dev.ithalo.teamping.network.S2CPingAckUpdatePayload;
import dev.ithalo.teamping.network.ShowPingPayload;
import dev.ithalo.teamping.ping.PingAcknowledgement;
import dev.ithalo.teamping.ping.PingIconOption;
import dev.ithalo.teamping.ping.PingSegment;
import dev.ithalo.teamping.ping.PingTargetKind;
import dev.ithalo.teamping.team.PingTeam;
import dev.ithalo.teamping.team.PlayerTeamData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPingManager {
    private static final Map<UUID, Long> LAST_PING_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACK_MS = new HashMap<>();
    private static final Map<UUID, ActivePing> ACTIVE_PINGS = new HashMap<>();
    private static final Map<UUID, ArrayDeque<Long>> ACTIVE_PING_EXPIRATIONS = new HashMap<>();

    private ServerPingManager() {
    }

    public static void handleCreatePing(CreatePingPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        Optional<PingTeam> team = PlayerTeamData.getTeam(player);
        if (team.isEmpty()) {
            return;
        }

        PingTeam pingTeam = team.get();
        if (!isOnSameTeam(player, pingTeam)) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        UUID playerId = player.getUUID();
        int cooldownTicks = TeamPingConfig.PING_COOLDOWN_SECONDS.get() * 20;
        long lastPing = LAST_PING_TICK.getOrDefault(playerId, Long.MIN_VALUE / 4);
        if (gameTime - lastPing < cooldownTicks) {
            return;
        }

        PingSegment segment = PingSegment.byId(payload.segmentId()).orElse(PingSegment.LOCATION);
        TargetInfo targetInfo = resolveTargetInfo(player, payload, segment);
        Vec3 target = targetInfo.position();
        double maxDistance = TeamPingConfig.MAX_PING_DISTANCE.get();
        if (player.getEyePosition(1.0F).distanceToSqr(target) > maxDistance * maxDistance) {
            return;
        }

        int durationTicks = TeamPingConfig.PING_DURATION_SECONDS.get() * 20;
        ArrayDeque<Long> expirations = ACTIVE_PING_EXPIRATIONS.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        removeExpired(expirations, gameTime);
        if (expirations.size() >= TeamPingConfig.MAX_ACTIVE_PINGS_PER_PLAYER.get()) {
            return;
        }

        LAST_PING_TICK.put(playerId, gameTime);
        expirations.addLast(gameTime + durationTicks);

        String dimension = player.level().dimension().location().toString();
        UUID pingId = UUID.randomUUID();
        ShowPingPayload response = new ShowPingPayload(
                pingId.toString(),
                player.getGameProfile().getName(),
                pingTeam.id(),
                segment.id(),
                targetInfo.kind().id(),
                targetInfo.label(),
                targetInfo.iconId(),
                targetInfo.entityId(),
                dimension,
                target.x(),
                target.y(),
                target.z(),
                pingTeam.color(),
                durationTicks
        );
        ACTIVE_PINGS.put(pingId, new ActivePing(
                pingId,
                playerId,
                player.getGameProfile().getName(),
                pingTeam,
                dimension,
                gameTime + durationTicks,
                new HashMap<>()
        ));

        for (ServerPlayer targetPlayer : player.server.getPlayerList().getPlayers()) {
            if (shouldReceivePing(targetPlayer, pingTeam, dimension)) {
                PacketDistributor.sendToPlayer(targetPlayer, response);
            }
        }
    }

    public static void handlePingAck(C2SPingAckPayload payload, IPayloadContext context) {
        if (!TeamPingConfig.ENABLE_ACKNOWLEDGEMENTS.get() || !(context.player() instanceof ServerPlayer player)) {
            return;
        }

        ActivePing activePing = ACTIVE_PINGS.get(payload.pingId());
        if (activePing == null || activePing.isExpired(player.serverLevel().getGameTime())) {
            return;
        }

        Optional<PingTeam> playerTeam = PlayerTeamData.getTeam(player);
        if (playerTeam.isEmpty() || playerTeam.get() != activePing.team()) {
            return;
        }

        if (!shouldReceivePing(player, activePing.team(), activePing.dimension())) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        long lastAckMs = LAST_ACK_MS.getOrDefault(player.getUUID(), Long.MIN_VALUE / 4);
        if (nowMs - lastAckMs < TeamPingConfig.ACK_RESPONSE_COOLDOWN_MS.get()) {
            return;
        }

        LAST_ACK_MS.put(player.getUUID(), nowMs);
        PingAcknowledgement acknowledgement = new PingAcknowledgement(
                activePing.id(),
                player.getUUID(),
                player.getGameProfile().getName(),
                payload.ackType(),
                nowMs
        );
        activePing.acknowledgements().put(player.getUUID(), acknowledgement);

        S2CPingAckUpdatePayload response = new S2CPingAckUpdatePayload(
                acknowledgement.pingId(),
                acknowledgement.playerId(),
                acknowledgement.playerName(),
                acknowledgement.type(),
                acknowledgement.timestamp()
        );

        for (ServerPlayer targetPlayer : player.server.getPlayerList().getPlayers()) {
            if (shouldReceivePing(targetPlayer, activePing.team(), activePing.dimension())) {
                PacketDistributor.sendToPlayer(targetPlayer, response);
            }
        }
    }

    public static void cleanup(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        for (Iterator<Map.Entry<UUID, ArrayDeque<Long>>> iterator = ACTIVE_PING_EXPIRATIONS.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<UUID, ArrayDeque<Long>> entry = iterator.next();
            removeExpired(entry.getValue(), gameTime);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
                LAST_PING_TICK.remove(entry.getKey());
            }
        }

        ACTIVE_PINGS.values().removeIf(ping -> ping.isExpired(gameTime));
    }

    private static boolean shouldReceivePing(ServerPlayer targetPlayer, PingTeam pingTeam, String dimension) {
        if (!isOnSameTeam(targetPlayer, pingTeam)) {
            return false;
        }

        return TeamPingConfig.ALLOW_CROSS_DIMENSION_PINGS.get()
                || targetPlayer.level().dimension().location().toString().equals(dimension);
    }

    private static void removeExpired(ArrayDeque<Long> expirations, long gameTime) {
        Iterator<Long> iterator = expirations.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() <= gameTime) {
                iterator.remove();
            }
        }
    }

    private static boolean isOnSameTeam(ServerPlayer player, PingTeam pingTeam) {
        Optional<PingTeam> targetTeam = PlayerTeamData.getTeam(player);
        return targetTeam.isPresent() && targetTeam.get() == pingTeam;
    }

    private static String sanitizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String sanitizeExtraIcon(String value) {
        return PingIconOption.byId(value).map(PingIconOption::id).orElse("");
    }

    private static TargetInfo resolveTargetInfo(ServerPlayer player, CreatePingPayload payload, PingSegment segment) {
        if (payload.targetEntityId() >= 0) {
            Entity entity = player.serverLevel().getEntity(payload.targetEntityId());
            if (isValidEntityTarget(player, entity)) {
                return targetInfoFromEntity(entity);
            }
        }

        return new TargetInfo(
                PingTargetKind.BLOCK,
                "",
                sanitizeExtraIcon(payload.targetIconId()),
                -1,
                new Vec3(payload.x(), payload.y(), payload.z())
        );
    }

    private static boolean isValidEntityTarget(ServerPlayer player, Entity entity) {
        return entity != null
                && entity.isAlive()
                && !(entity instanceof Player)
                && entity.level() == player.level()
                && (entity instanceof ItemEntity || entity instanceof LivingEntity);
    }

    private static TargetInfo targetInfoFromEntity(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            String itemId = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString();
            return new TargetInfo(
                    PingTargetKind.ITEM,
                    sanitizeText(itemEntity.getItem().getHoverName().getString(), 48),
                    sanitizeText(itemId, 96),
                    entity.getId(),
                    entityPingPosition(entity)
            );
        }

        if (entity instanceof Monster) {
            return new TargetInfo(
                    PingTargetKind.HOSTILE_MOB,
                    sanitizeText(entity.getDisplayName().getString(), 48),
                    "",
                    entity.getId(),
                    entityPingPosition(entity)
            );
        }

        return new TargetInfo(
                PingTargetKind.PASSIVE_MOB,
                sanitizeText(entity.getDisplayName().getString(), 48),
                "",
                entity.getId(),
                entityPingPosition(entity)
        );
    }

    private static Vec3 entityPingPosition(Entity entity) {
        return new Vec3(
                (entity.getBoundingBox().minX + entity.getBoundingBox().maxX) * 0.5D,
                entity.getBoundingBox().maxY + 0.25D,
                (entity.getBoundingBox().minZ + entity.getBoundingBox().maxZ) * 0.5D
        );
    }

    private record TargetInfo(PingTargetKind kind, String label, String iconId, int entityId, Vec3 position) {
    }

    private record ActivePing(
            UUID id,
            UUID creatorId,
            String creatorName,
            PingTeam team,
            String dimension,
            long expiresAtGameTime,
            Map<UUID, PingAcknowledgement> acknowledgements
    ) {
        private boolean isExpired(long gameTime) {
            return gameTime >= expiresAtGameTime;
        }
    }
}
