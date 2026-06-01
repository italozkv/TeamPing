package dev.ithalo.teamping.network;

import java.util.UUID;

import dev.ithalo.teamping.TeamPing;
import dev.ithalo.teamping.ping.PingAckType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SPingAckPayload(UUID pingId, PingAckType ackType) implements CustomPacketPayload {
    public static final Type<C2SPingAckPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "ping_ack"));

    public static final StreamCodec<ByteBuf, C2SPingAckPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.pingId().toString());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.ackType().id());
            },
            buffer -> new C2SPingAckPayload(
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    PingAckType.byId(ByteBufCodecs.STRING_UTF8.decode(buffer)).orElseThrow()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
