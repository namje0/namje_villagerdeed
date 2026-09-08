package com.namje.villagerdeed.networking;

import com.namje.villagerdeed.networking.packet.ToggleDeedPacketC2S;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    // server
    public static void handleToggleDeedPacket(ToggleDeedPacketC2S testPacketC2S, IPayloadContext context) {
        EntityTypes.COW.spawn(((ServerLevel) context.player().level()),
                context.player().getOnPos(), EntitySpawnReason.TRIGGERED);
    }
}