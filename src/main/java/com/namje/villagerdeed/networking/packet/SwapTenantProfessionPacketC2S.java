package com.namje.villagerdeed.networking.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;

public record SwapTenantProfessionPacketC2S(BlockPos pos, VillagerProfession profession) {
    public static void encode(SwapTenantProfessionPacketC2S packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos());
        buf.writeResourceLocation(BuiltInRegistries.VILLAGER_PROFESSION.getKey(packet.profession()));
    }

    public static SwapTenantProfessionPacketC2S decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ResourceLocation profession = buf.readResourceLocation();
        return new SwapTenantProfessionPacketC2S(pos, BuiltInRegistries.VILLAGER_PROFESSION.get(profession));
    }
}
