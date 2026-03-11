package net.hypixel.modapi.fabric.payload;

import io.netty.buffer.ByteBuf;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.error.ErrorReason;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public class ClientboundHypixelPayload implements CustomPacketPayload {
    private final Type<ClientboundHypixelPayload> type;

    private ClientboundHypixelPacket packet;
    private ErrorReason errorReason;

    private ClientboundHypixelPayload(Type<ClientboundHypixelPayload> type, ByteBuf buf) {
        this.type = type;

        PacketSerializer serializer = new PacketSerializer(buf);
        boolean success = serializer.readBoolean();
        if (!success) {
            this.errorReason = ErrorReason.getById(serializer.readVarInt());
            return;
        }

        this.packet = HypixelModAPI.getInstance().getRegistry().createClientboundPacket(type.id().toString(), serializer);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public boolean isSuccess() {
        return packet != null;
    }

    public ClientboundHypixelPacket getPacket() {
        return packet;
    }

    public ErrorReason getErrorReason() {
        return errorReason;
    }

    public static StreamCodec<ByteBuf, ClientboundHypixelPayload> buildCodec(Type<ClientboundHypixelPayload> id) {
        return CustomPacketPayload.codec((value, buf) -> {
            throw new UnsupportedOperationException("Cannot write ClientboundHypixelPayload");
        }, buf -> new ClientboundHypixelPayload(id, buf));
    }
}
