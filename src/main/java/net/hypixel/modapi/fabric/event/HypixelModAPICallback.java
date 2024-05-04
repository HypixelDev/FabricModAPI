package net.hypixel.modapi.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;

public interface HypixelModAPICallback {

    Event<HypixelModAPICallback> EVENT = EventFactory.createArrayBacked(HypixelModAPICallback.class, callbacks -> packet -> {
        for (HypixelModAPICallback callback : callbacks) {
            callback.onPacket(packet);
        }
    });

    void onPacket(ClientboundHypixelPacket packet);

}
