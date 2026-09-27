package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record ToggleSubscriptionPacketC2S(BlockPos pos) {
    public static void encode(ToggleSubscriptionPacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
    }

    public static ToggleSubscriptionPacketC2S decode(FriendlyByteBuf buf) {
        return new ToggleSubscriptionPacketC2S(buf.readBlockPos());
    }
}
