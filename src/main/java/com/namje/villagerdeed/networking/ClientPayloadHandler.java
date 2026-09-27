package com.namje.villagerdeed.networking;

import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.networking.packet.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientPayloadHandler {
    //TODO: sanity check player UUID and if locked + not owner uuid, return early

    // server
    public static void handleToggleDeedPacket(ToggleDeedPacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            deedBlockEntity.toggleDeedAvailability();
            deedBlockEntity.markUpdated();
        }
    }

    public static void handleToggleLockPacket(ToggleLockPacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            UUID playerUUID = player.getUUID();
            UUID ownerUUID = deedBlockEntity.getOwnerUUID();

            if (ownerUUID != null && ownerUUID.equals(playerUUID)) {
                deedBlockEntity.setLocked(!deedBlockEntity.getLocked());
                deedBlockEntity.markUpdated();
            }
        }
    }

    public static void handleToggleSubscriptionPacket(ToggleSubscriptionPacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            deedBlockEntity.toggleSubscription(player.getUUID());
        }
    }

    public static void handleSummonTenantPacket(SummonTenantPacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            deedBlockEntity.summonTenant(deedBlockEntity.getTenantEntity(level), pos);
        }
    }

    public static void handleEvictTenantPacket(EvictTenantPacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            deedBlockEntity.evictTenant(level);
        }
    }

    public static void handleChangeDeedNamePacket(ChangeDeedNamePacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();
        String name = packet.name();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            deedBlockEntity.setDeedName(name);
            deedBlockEntity.markUpdated();
        }
    }

    public static void handleChangeTenantNamePacket(ChangeTenantNamePacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();
        String name = packet.name();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            deedBlockEntity.setTenantName(name);
            deedBlockEntity.markUpdated();
        }
    }

    public static void handleSwapTenantProfessionPacket(SwapTenantProfessionPacketC2S packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = packet.pos();
        VillagerProfession profession = packet.profession();

        // TODO: sanity check position
        if (level.getBlockEntity(pos) instanceof VillagerDeedBlockEntity deedBlockEntity) {
            deedBlockEntity.setTenantProfession(profession);
            deedBlockEntity.markUpdated();
        }
    }
}
