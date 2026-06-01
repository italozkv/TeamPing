package dev.ithalo.teamping.network;

import java.util.UUID;

import dev.ithalo.teamping.TeamPing;
import dev.ithalo.teamping.ping.PingAckType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CPingAckUpdatePayload(
        UUID pingId,
        UUID playerId,
        String playerName,
        PingAckType ackType,
        long timestamp
) implements CustomPacketPayload {
    public static final Type<S2CPingAckUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "ping_ack_update"));

    public static final StreamCodec<ByteBuf, S2CPingAckUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.pingId().toString());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.playerId().toString());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.playerName());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.ackType().id());
                ByteBufCodecs.STRING_UTF8.encode(buffer, Long.toString(payload.timestamp()));
            },
            buffer -> new S2CPingAckUpdatePayload(
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    PingAckType.byId(ByteBufCodecs.STRING_UTF8.decode(buffer)).orElseThrow(),
                    Long.parseLong(ByteBufCodecs.STRING_UTF8.decode(buffer))
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
