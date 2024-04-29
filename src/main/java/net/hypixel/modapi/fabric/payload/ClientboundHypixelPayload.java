package net.hypixel.modapi.fabric.payload;

import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.error.ErrorReason;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public class ClientboundHypixelPayload implements CustomPayload {
    private final Id<ClientboundHypixelPayload> id;

    private ClientboundHypixelPacket packet;
    private ErrorReason errorReason;

    private ClientboundHypixelPayload(Id<ClientboundHypixelPayload> id, PacketByteBuf buf) {
        this.id = id;

        PacketSerializer serializer = new PacketSerializer(buf);
        boolean success = serializer.readBoolean();
        if (!success) {
            this.errorReason = ErrorReason.getById(serializer.readVarInt());
            return;
        }

        this.packet = HypixelModAPI.getInstance().getRegistry().createClientboundPacket(id.id().toString(), serializer);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return id;
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

    public static PacketCodec<PacketByteBuf, ClientboundHypixelPayload> buildCodec(Id<ClientboundHypixelPayload> id) {
        return CustomPayload.codecOf((value, buf) -> {
            throw new UnsupportedOperationException("Cannot write ClientboundHypixelPayload");
        }, buf -> new ClientboundHypixelPayload(id, buf));
    }
}
