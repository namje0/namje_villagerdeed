package com.namje.villagerdeed.networking.packet;

import com.namje.villagerdeed.VillagerDeed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public record SwapTenantProfessionPacketC2S(BlockPos pos, VillagerProfession profession) implements CustomPacketPayload {
    public static final Type<SwapTenantProfessionPacketC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerDeed.MODID, "swap_tenant_profession_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapTenantProfessionPacketC2S> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SwapTenantProfessionPacketC2S::pos,

                    ByteBufCodecs.registry(Registries.VILLAGER_PROFESSION),
                    SwapTenantProfessionPacketC2S::profession,

                    SwapTenantProfessionPacketC2S::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
