package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record ToggleLockPacketC2S(BlockPos pos) {
    public static void encode(ToggleLockPacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
    }

    public static ToggleLockPacketC2S decode(FriendlyByteBuf buf) {
        return new ToggleLockPacketC2S(buf.readBlockPos());
    }
}
