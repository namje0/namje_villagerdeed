package com.namje.villagerdeed.event;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.networking.ClientPayloadHandler;
import com.namje.villagerdeed.networking.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = VillagerDeed.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(VillagerDeed.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    @SubscribeEvent
    public static void registerPayloads(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int id = 0;

            CHANNEL.messageBuilder(ToggleDeedPacketC2S.class, id++)
                    .encoder(ToggleDeedPacketC2S::encode)
                    .decoder(ToggleDeedPacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleToggleDeedPacket)
                    .add();

            CHANNEL.messageBuilder(SummonTenantPacketC2S.class, id++)
                    .encoder(SummonTenantPacketC2S::encode)
                    .decoder(SummonTenantPacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleSummonTenantPacket)
                    .add();

            CHANNEL.messageBuilder(EvictTenantPacketC2S.class, id++)
                    .encoder(EvictTenantPacketC2S::encode)
                    .decoder(EvictTenantPacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleEvictTenantPacket)
                    .add();

            CHANNEL.messageBuilder(ChangeDeedNamePacketC2S.class, id++)
                    .encoder(ChangeDeedNamePacketC2S::encode)
                    .decoder(ChangeDeedNamePacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleChangeDeedNamePacket)
                    .add();

            CHANNEL.messageBuilder(ChangeTenantNamePacketC2S.class, id++)
                    .encoder(ChangeTenantNamePacketC2S::encode)
                    .decoder(ChangeTenantNamePacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleChangeTenantNamePacket)
                    .add();

            CHANNEL.messageBuilder(SwapTenantProfessionPacketC2S.class, id++)
                    .encoder(SwapTenantProfessionPacketC2S::encode)
                    .decoder(SwapTenantProfessionPacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleSwapTenantProfessionPacket)
                    .add();

            CHANNEL.messageBuilder(ToggleLockPacketC2S.class, id++)
                    .encoder(ToggleLockPacketC2S::encode)
                    .decoder(ToggleLockPacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleToggleLockPacket)
                    .add();

            CHANNEL.messageBuilder(ToggleSubscriptionPacketC2S.class, id++)
                    .encoder(ToggleSubscriptionPacketC2S::encode)
                    .decoder(ToggleSubscriptionPacketC2S::decode)
                    .consumerMainThread(ClientPayloadHandler::handleToggleSubscriptionPacket)
                    .add();
        });
    }
}
