package com.namje.villagerdeed.event;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.networking.ClientPayloadHandler;
import com.namje.villagerdeed.networking.packet.ToggleDeedPacketC2S;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = VillagerDeed.MODID)
public class ModEvents {
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").executesOn(HandlerThread.MAIN);

        registrar.playToServer(ToggleDeedPacketC2S.TYPE,
                ToggleDeedPacketC2S.STREAM_CODEC, ClientPayloadHandler::handleToggleDeedPacket);
    }
}
