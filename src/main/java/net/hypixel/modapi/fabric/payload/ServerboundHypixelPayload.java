package net.hypixel.modapi.fabric.payload;

import io.netty.buffer.ByteBuf;
import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class ServerboundHypixelPayload implements CustomPacketPayload {
    private final Type<ServerboundHypixelPayload> type;
    private final HypixelPacket packet;

    public ServerboundHypixelPayload(HypixelPacket packet) {
        this.type = new CustomPacketPayload.Type<>(Identifier.parse(packet.getIdentifier()));
        this.packet = packet;
    }

    private void write(ByteBuf buf) {
        PacketSerializer serializer = new PacketSerializer(buf);
        packet.write(serializer);
    }


    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public static StreamCodec<ByteBuf, ServerboundHypixelPayload> buildCodec() {
        return CustomPacketPayload.codec(ServerboundHypixelPayload::write, _ -> {
            throw new UnsupportedOperationException("Cannot read ServerboundHypixelPayload");
        });
    }
}
