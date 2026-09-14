package com.namje.villagerdeed.networking.packet;

import com.namje.villagerdeed.VillagerDeed;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleDeedPacketC2S(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ToggleDeedPacketC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "namje_toggle_deed_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleDeedPacketC2S> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ToggleDeedPacketC2S::pos,

                    ToggleDeedPacketC2S::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
