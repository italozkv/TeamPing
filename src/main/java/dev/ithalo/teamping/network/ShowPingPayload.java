package dev.ithalo.teamping.network;

import dev.ithalo.teamping.TeamPing;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ShowPingPayload(
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
        int durationTicks
) implements CustomPacketPayload {
    public static final Type<ShowPingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamPing.MOD_ID, "show_ping"));

    public static final StreamCodec<ByteBuf, ShowPingPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.id());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.playerName());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.teamId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.segmentId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.targetKind());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.targetLabel());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.targetIconId());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.targetEntityId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.dimension());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.x());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.y());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.z());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.color());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.durationTicks());
            },
            buffer -> new ShowPingPayload(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
