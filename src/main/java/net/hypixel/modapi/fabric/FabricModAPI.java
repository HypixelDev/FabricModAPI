package net.hypixel.modapi.fabric;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.fabric.event.HypixelModAPICallback;
import net.hypixel.modapi.fabric.payload.ClientboundHypixelPayload;
import net.hypixel.modapi.fabric.payload.ServerboundHypixelPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class FabricModAPI implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        registerPayloads();
        registerPacketSender();
    }

    private void registerPayloads() {
        for (String identifier : HypixelModAPI.getInstance().getRegistry().getIdentifiers()) {
            registerClientbound(identifier);
            registerServerbound(identifier);
        }
    }

    private void registerPacketSender() {
        HypixelModAPI.getInstance().setPacketSender((packet) -> {
            ServerboundHypixelPayload payload = new ServerboundHypixelPayload(packet);
            ClientPlayNetworking.send(payload);
        });
    }

    public void registerClientbound(String identifier) {
        Validate.isTrue(HypixelModAPI.getInstance().getRegistry().isRegistered(identifier), "Identifier %s is not registered", identifier);

        CustomPayload.Id<ClientboundHypixelPayload> clientboundId = CustomPayload.id(identifier);
        PacketCodec<PacketByteBuf, ClientboundHypixelPayload> codec = ClientboundHypixelPayload.buildCodec(clientboundId);
        PayloadTypeRegistry.playS2C().register(clientboundId, codec);

        // Also register the global receiver for handling incoming packets
        ClientPlayNetworking.registerGlobalReceiver(clientboundId, (payload, context) -> {
            if (!payload.isSuccess()) {
                LOGGER.warn("Received an error response for packet {}: {}", identifier, payload.getErrorReason());
                return;
            }

            try {
                HypixelModAPI.getInstance().handle(payload.getPacket());
            } catch (Exception e) {
                LOGGER.error("An error occurred while handling packet {}", identifier, e);
            }

            try {
                HypixelModAPICallback.EVENT.invoker().onPacketReceived(payload.getPacket());
            } catch (Exception e) {
                LOGGER.error("An error occurred while handling packet {}", identifier, e);
            }
        });
    }

    public void registerServerbound(String identifier) {
        Validate.isTrue(HypixelModAPI.getInstance().getRegistry().isRegistered(identifier), "Identifier %s is not registered", identifier);

        CustomPayload.Id<ServerboundHypixelPayload> serverboundId = CustomPayload.id(identifier);
        PacketCodec<PacketByteBuf, ServerboundHypixelPayload> codec = ServerboundHypixelPayload.buildCodec(serverboundId);
        PayloadTypeRegistry.playC2S().register(serverboundId, codec);
    }
}
