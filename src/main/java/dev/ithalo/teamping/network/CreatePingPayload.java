package dev.ithalo.teamping.network;

import dev.ithalo.teamping.TeamPing;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CreatePingPayload(
        double x,
        double y,
        double z,
        String segmentId,
        String targetKind,
        String targetLabel,
        String targetIconId,
        int targetEntityId
) implements CustomPacketPayload {
    public static final Type<CreatePingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "create_ping"));

    public static final StreamCodec<ByteBuf, CreatePingPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.DOUBLE.encode(buffer, payload.x());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.y());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.z());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.segmentId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.targetKind());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.targetLabel());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.targetIconId());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.targetEntityId());
            },
            buffer -> new CreatePingPayload(
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
