package com.namje.villagerdeed.networking.packet;

import com.namje.villagerdeed.VillagerDeed;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleSubscriptionPacketC2S(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ToggleSubscriptionPacketC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "toggle_subscription_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSubscriptionPacketC2S> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ToggleSubscriptionPacketC2S::pos,

                    ToggleSubscriptionPacketC2S::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
