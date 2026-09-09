package com.namje.villagerdeed.event;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.networking.ClientPayloadHandler;
import com.namje.villagerdeed.networking.packet.*;
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

        registrar.playToServer(SummonTenantPacketC2S.TYPE,
                SummonTenantPacketC2S.STREAM_CODEC, ClientPayloadHandler::handleSummonTenantPacket);

        registrar.playToServer(EvictTenantPacketC2S.TYPE,
                EvictTenantPacketC2S.STREAM_CODEC, ClientPayloadHandler::handleEvictTenantPacket);


        registrar.playToServer(ChangeDeedNamePacketC2S.TYPE,
                ChangeDeedNamePacketC2S.STREAM_CODEC, ClientPayloadHandler::handleChangeDeedNamePacket);

        registrar.playToServer(ChangeTenantNamePacketC2S.TYPE,
                ChangeTenantNamePacketC2S.STREAM_CODEC, ClientPayloadHandler::handleChangeTenantNamePacket);
    }
}
