package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record ChangeDeedNamePacketC2S(BlockPos pos, String name) {
    public static void encode(ChangeDeedNamePacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
        buf.writeUtf(packet.name());
    }

    public static ChangeDeedNamePacketC2S decode(FriendlyByteBuf buf) {
        return new ChangeDeedNamePacketC2S(buf.readBlockPos(), buf.readUtf());
    }
}
