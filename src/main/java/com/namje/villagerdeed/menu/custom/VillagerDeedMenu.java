package com.namje.villagerdeed.menu.custom;

import com.namje.villagerdeed.block.ModBlocks;
import com.namje.villagerdeed.block.entity.custom.VillagerDeedBlockEntity;
import com.namje.villagerdeed.menu.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

public class VillagerDeedMenu extends AbstractContainerMenu {
    public final VillagerDeedBlockEntity blockEntity;
    private final Level level;

    public VillagerDeedMenu(int containerId, Inventory inv, FriendlyByteBuf data) {
        this(containerId, inv, inv.player.level().getBlockEntity(data.readBlockPos()));
    }

    public VillagerDeedMenu(int containerId, @Nullable Inventory inv, BlockEntity blockEntity) {
        super(ModMenuTypes.VILLAGER_MENU.get(), containerId);
        this.blockEntity = (VillagerDeedBlockEntity) blockEntity;
        this.level = inv.player.level();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.VILLAGERDEED_BLOCK.get());
    }
}
