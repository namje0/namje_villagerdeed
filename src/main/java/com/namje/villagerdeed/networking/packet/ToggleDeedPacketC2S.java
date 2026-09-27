package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record ToggleDeedPacketC2S(BlockPos pos) {
    public static void encode(ToggleDeedPacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
    }

    public static ToggleDeedPacketC2S decode(FriendlyByteBuf buf) {
        return new ToggleDeedPacketC2S(buf.readBlockPos());
    }
}
