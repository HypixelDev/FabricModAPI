package net.hypixel.modapi.fabric.payload;

import net.hypixel.modapi.packet.HypixelPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ServerboundHypixelPayload implements CustomPayload {
    private final Id<ServerboundHypixelPayload> id;
    private final HypixelPacket packet;

    public ServerboundHypixelPayload(HypixelPacket packet) {
        this.id = new CustomPayload.Id<>(Identifier.of(packet.getIdentifier()));
        this.packet = packet;
    }

    private void write(PacketByteBuf buf) {
        PacketSerializer serializer = new PacketSerializer(buf);
        packet.write(serializer);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return id;
    }

    public static PacketCodec<PacketByteBuf, ServerboundHypixelPayload> buildCodec(Id<ServerboundHypixelPayload> id) {
        return CustomPayload.codecOf(ServerboundHypixelPayload::write, buf -> {
            throw new UnsupportedOperationException("Cannot read ServerboundHypixelPayload");
        });
    }
}
