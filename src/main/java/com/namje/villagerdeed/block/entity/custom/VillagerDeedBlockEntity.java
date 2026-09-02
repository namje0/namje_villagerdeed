package com.namje.villagerdeed.block.entity.custom;

import com.namje.villagerdeed.block.entity.ModBlockEntities;
import com.namje.villagerdeed.menu.custom.VillagerDeedMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public class VillagerDeedBlockEntity extends BlockEntity implements MenuProvider {
    private static final int MAX_MOVE_IN_TIME = 60;
    private int moveInTime = 0;
    private String roomName = "Room";
    private String villagerName = "";
    private int villagerId = 0; // nothing now, if we want diff villager models this might be good for tracking?


    private final ContainerData data;

    public VillagerDeedBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.VILLAGERDEED_BE.get(), worldPosition, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int dataId) {
                return switch (dataId) {
                    case 0 -> VillagerDeedBlockEntity.this.moveInTime;
                    default -> 0;
                };
            }

            @Override
            public void set(int dataId, int value) {
                switch (dataId) {
                    case 0: VillagerDeedBlockEntity.this.moveInTime = value;
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    }

    /* sync */
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    };

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.villagerdeed.namje_villagerdeed");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VillagerDeedMenu(id, inventory, this);
    }
}
