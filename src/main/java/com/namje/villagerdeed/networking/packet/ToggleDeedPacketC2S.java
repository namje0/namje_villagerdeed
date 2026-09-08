package com.namje.villagerdeed.networking.packet;

import com.namje.villagerdeed.VillagerDeed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleDeedPacketC2S(String name, int value) implements CustomPacketPayload {
    public static final Type<ToggleDeedPacketC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "toggle_deed_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleDeedPacketC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ToggleDeedPacketC2S::name,

                    ByteBufCodecs.VAR_INT,
                    ToggleDeedPacketC2S::value,

                    ToggleDeedPacketC2S::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
