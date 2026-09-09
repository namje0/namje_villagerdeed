package com.namje.villagerdeed.networking.packet;

import com.namje.villagerdeed.VillagerDeed;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChangeDeedNamePacketC2S(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<ChangeDeedNamePacketC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "change_deed_name_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeDeedNamePacketC2S> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ChangeDeedNamePacketC2S::pos,

                    ByteBufCodecs.STRING_UTF8,
                    ChangeDeedNamePacketC2S::name,

                    ChangeDeedNamePacketC2S::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
