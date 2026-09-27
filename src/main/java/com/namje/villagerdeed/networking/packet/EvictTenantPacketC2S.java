package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record EvictTenantPacketC2S(BlockPos pos) {
    public static void encode(EvictTenantPacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
    }

    public static EvictTenantPacketC2S decode(FriendlyByteBuf buf) {
        return new EvictTenantPacketC2S(buf.readBlockPos());
    }
}
