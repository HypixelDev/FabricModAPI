package net.hypixel.modapi.fabric;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.fabric.event.HypixelModAPICallback;
import net.hypixel.modapi.fabric.event.HypixelModAPIErrorCallback;
import net.hypixel.modapi.fabric.payload.ClientboundHypixelPayload;
import net.hypixel.modapi.fabric.payload.ServerboundHypixelPayload;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.slf4j.Logger;

public class FabricModAPI implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_MODE = FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("net.hypixel.modapi.debug");

    @Override
    public void onInitializeClient() {
        reloadRegistrations();
        registerPacketSender();

        if (DEBUG_MODE) {
            LOGGER.info("Debug mode is enabled!");
            registerDebug();
        }
    }

    /**
     * Reloads the identifiers that are registered in the Hypixel Mod API and makes sure that the packets are registered.
     * <p>
     * This method is available for internal use by Hypixel to add new packets externally, and is not intended for use by other developers.
     */
    public static void reloadRegistrations() {
        for (String identifier : HypixelModAPI.getInstance().getRegistry().getClientboundIdentifiers()) {
            try {
                registerClientbound(identifier);
                LOGGER.info("Registered clientbound packet with identifier '{}'", identifier);
            } catch (Exception e) {
                LOGGER.error("Failed to register clientbound packet with identifier '{}'", identifier, e);
            }
        }

        for (String identifier : HypixelModAPI.getInstance().getRegistry().getServerboundIdentifiers()) {
            try {
                registerServerbound(identifier);
                LOGGER.info("Registered serverbound packet with identifier '{}'", identifier);
            } catch (Exception e) {
                LOGGER.error("Failed to register serverbound packet with identifier '{}'", identifier, e);
            }
        }
    }

    private static void registerPacketSender() {
        HypixelModAPI.getInstance().setPacketSender((packet) -> {
            ServerboundHypixelPayload hypixelPayload = new ServerboundHypixelPayload(packet);

            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                ClientPlayNetworking.send(hypixelPayload);
                return true;
            }

            try {
                ClientConfigurationNetworking.send(hypixelPayload);
                return true;
            } catch (IllegalStateException ignored) {
                LOGGER.warn("Failed to send a packet as the client is not connected to a server '{}'", packet);
                return false;
            }
        });
    }

    private static void registerClientbound(String identifier) {
        try {
            CustomPayload.Id<ClientboundHypixelPayload> clientboundId = CustomPayload.id(identifier);
            PacketCodec<PacketByteBuf, ClientboundHypixelPayload> codec = ClientboundHypixelPayload.buildCodec(clientboundId);
            PayloadTypeRegistry.playS2C().register(clientboundId, codec);
            PayloadTypeRegistry.configurationS2C().register(clientboundId, codec);

            // Also register the global receiver for handling incoming packets during PLAY and CONFIGURATION
            ClientPlayNetworking.registerGlobalReceiver(clientboundId, (payload, context) -> {
                LOGGER.debug("Received packet with identifier '{}', during PLAY", identifier);
                handleIncomingPayload(identifier, payload);

            });
            ClientConfigurationNetworking.registerGlobalReceiver(clientboundId, (payload, context) -> {
                LOGGER.debug("Received packet with identifier '{}', during CONFIGURATION", identifier);
                handleIncomingPayload(identifier, payload);
            });
        } catch (IllegalArgumentException ignored) {
            // Ignored as this is fired when we reload the registrations and the packet is already registered
        }
    }

    private static void handleIncomingPayload(String identifier, ClientboundHypixelPayload payload) {
        if (!payload.isSuccess()) {
            LOGGER.warn("Received an error response for packet {}: {}", identifier, payload.getErrorReason());
            try {
                HypixelModAPI.getInstance().handleError(identifier, payload.getErrorReason());
            } catch (Exception e) {
                LOGGER.error("An error occurred while handling error response for packet {}", identifier, e);
            }

            try {
                HypixelModAPIErrorCallback.EVENT.invoker().onError(identifier, payload.getErrorReason());
            } catch (Exception e) {
                LOGGER.error("An error occurred while handling error response for packet {}", identifier, e);
            }
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
    }

    private static void registerServerbound(String identifier) {
        try {
            CustomPayload.Id<ServerboundHypixelPayload> serverboundId = CustomPayload.id(identifier);
            PacketCodec<PacketByteBuf, ServerboundHypixelPayload> codec = ServerboundHypixelPayload.buildCodec(serverboundId);
            PayloadTypeRegistry.playC2S().register(serverboundId, codec);
            PayloadTypeRegistry.configurationC2S().register(serverboundId, codec);
        } catch (IllegalArgumentException ignored) {
            // Ignored as this is fired when we reload the registrations and the packet is already registered
        }
    }

    private static void registerDebug() {
        // Register events
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);

        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, packet -> LOGGER.info("Received location packet {}", packet))
                .onError(error -> LOGGER.error("Received error response for location packet: {}", error));

        HypixelModAPICallback.EVENT.register(packet -> LOGGER.info("Received packet {}", packet));
        HypixelModAPIErrorCallback.EVENT.register((identifier, error) -> LOGGER.error("Received error response for packet {}: {}", identifier, error));
    }
}
