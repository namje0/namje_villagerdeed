package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SummonTenantPacketC2S(BlockPos pos) {
    public static void encode(SummonTenantPacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
    }

    public static SummonTenantPacketC2S decode(FriendlyByteBuf buf) {
        return new SummonTenantPacketC2S(buf.readBlockPos());
    }
}
