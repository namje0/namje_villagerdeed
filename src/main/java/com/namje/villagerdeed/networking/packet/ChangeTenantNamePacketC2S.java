package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record ChangeTenantNamePacketC2S(BlockPos pos, String name) {
    public static void encode(ChangeTenantNamePacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
        buf.writeUtf(packet.name());
    }

    public static ChangeTenantNamePacketC2S decode(FriendlyByteBuf buf) {
        return new ChangeTenantNamePacketC2S(buf.readBlockPos(), buf.readUtf());
    }
}
