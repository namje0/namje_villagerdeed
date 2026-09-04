package com.namje.villagerdeed.block.entity.custom;

import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.ModBlockEntities;
import com.namje.villagerdeed.menu.custom.VillagerDeedMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

import static com.namje.villagerdeed.block.custom.VillagerDeedBlock.ACTIVE;

public class VillagerDeedBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_MOVE_IN_TIME = 120;
    public static final double MAX_LEASH_DISTANCE = 16.0;
    public static final double HARD_TELEPORT_DISTANCE = 32.0;
    public static final int TENANT_UPD_TIME = 1200;

    /*
    0 = invalid, either due to no bed or other means; no functionality running
    1 = inactive, bound to a bed but no tenant bound
    2 = active, bed and tenant bound
    3 = waiting, tenant is dead/otherwise missing and waiting to respawn
     */
    private int state = 0;

    private int moveInTime = 0;
    private String roomName = "Room";
    private String tenantName = "";

    private @Nullable EntityReference<LivingEntity> tenant;
    private @Nullable CompoundTag tenantData;
    private final ContainerData data;

    public VillagerDeedBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.VILLAGERDEED_BE.get(), worldPosition, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int dataId) {
                return switch (dataId) {
                    case 0 -> VillagerDeedBlockEntity.this.moveInTime;
                    case 1 -> MAX_MOVE_IN_TIME;
                    default -> 0;
                };
            }

            @Override
            public void set(int dataId, int value) {
                if (dataId == 0) {
                    VillagerDeedBlockEntity.this.moveInTime = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public ProblemReporter.PathElement problemPath() {
        return new BlockEntityPathElement(this);
    }

    private static record BlockEntityPathElement(BlockEntity blockEntity) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return this.blockEntity.toString();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VillagerDeedBlockEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!state.getValue(ACTIVE)) {
            return;
        }

        EntityReference<LivingEntity> verifiedRef = validateTenant(entity.tenant, serverLevel);

        if (!Objects.equals(verifiedRef, entity.tenant)) {
            entity.tenant = verifiedRef;
            entity.setChanged();
            serverLevel.sendBlockUpdated(pos, state, state, 2);
        }

        Villager activeTenant = entity.getTenantEntity(serverLevel);

        if (activeTenant != null) {
            entity.moveInTime = 0;
            String currentName = activeTenant.getDisplayName().getString();
            if (!Objects.equals(entity.tenantName, currentName)) {
                entity.tenantName = currentName;
                entity.setChanged();
            }

            // ranch tenant to being near deed at all times (TODO: make into toggle)
            entity.restrictTenant(activeTenant, pos);

            // update tenant data every TENANT_UPD_TIME ticks, so we can build them from the data again
            if (serverLevel.getGameTime() % TENANT_UPD_TIME == 0) {
                entity.snapshotTenantData(activeTenant);
            }
        } else {
            if (!entity.tenantName.isEmpty()) {
                entity.tenantName = "";
                entity.setChanged();
            }

            entity.moveInTime++;
            if (entity.moveInTime >= MAX_MOVE_IN_TIME) {
                if (entity.spawnTenant(serverLevel, pos)) {
                    entity.moveInTime = 0;
                    entity.setChanged();
                }
            }
        }
    }

    // ranch tenants to a radius around the deed so we don't have to deal with unloaded tenants and loaded deeds if possible
    private void restrictTenant(Villager tenant, BlockPos deedPos) {
        double distSqr = tenant.distanceToSqr(deedPos.getX() + 0.5,
                deedPos.getY(), deedPos.getZ() + 0.5);

        if (distSqr > HARD_TELEPORT_DISTANCE * HARD_TELEPORT_DISTANCE) {
            VillagerDeed.LOGGER.info("tenant far from deed; teleport to it");
            tenant.teleportTo(deedPos.getX() + 0.5, deedPos.getY() + 1.0, deedPos.getZ() + 0.5);
            tenant.getNavigation().stop();
            return;
        }

        if (distSqr > MAX_LEASH_DISTANCE * MAX_LEASH_DISTANCE) {
            if (tenant.getNavigation().isDone()) {
                VillagerDeed.LOGGER.info("attempting to navigate tenant to deed");
                tenant.getNavigation().moveTo(deedPos.getX() + 0.5,
                        deedPos.getY() + 1.0, deedPos.getZ() + 0.5, 1);
            }
        }
    }

    private void snapshotTenantData(Villager tenant) {
        if (this.level == null) return;

        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), VillagerDeed.LOGGER)) {
            TagValueOutput entityData = TagValueOutput.createWithContext(reporter, this.level.registryAccess());
            tenant.saveWithoutId(entityData);
            this.tenantData = entityData.buildResult();
            this.setChanged();
        }
    }

    private static @Nullable EntityReference<LivingEntity> validateTenant(@Nullable EntityReference<LivingEntity> currentRef, ServerLevel level) {
        if (currentRef != null) {
            Villager villager = getTenantFromRef(currentRef, level);
            if (villager != null && villager.isAlive()) {
                return currentRef;
            }
        }
        return null;
    }

    private boolean spawnTenant(ServerLevel level, BlockPos pos) {
        Villager tenant = EntityTypes.VILLAGER.create(level, EntitySpawnReason.SPAWNER);
        if (tenant == null) {
            return false;
        }

        if (this.tenantData != null && !this.tenantData.isEmpty()) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), VillagerDeed.LOGGER)) {
                ValueInput input = TagValueInput.create(reporter, level.registryAccess(), this.tenantData);
                tenant.load(input);
                VillagerDeed.LOGGER.info("existing tenant data detected: attempting to load data");
            }
        }

        tenant.setUUID(UUID.randomUUID());
        tenant.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        tenant.setHealth(tenant.getMaxHealth());

        if (level.addFreshEntity(tenant)) {
            this.tenant = EntityReference.of(tenant);
            this.tenantName = tenant.getDisplayName().getString();

            snapshotTenantData(tenant);

            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
            VillagerDeed.LOGGER.info("tenant created/restored and bound to deed");
            return true;
        }

        return false;
    }

    @Nullable
    public Villager getTenantEntity(Level level) {
        return getTenantFromRef(this.tenant, level);
    }

    private static @Nullable Villager getTenantFromRef(@Nullable EntityReference<LivingEntity> ref, Level level) {
        if (ref == null) return null;
        return EntityReference.getLivingEntity(ref, level) instanceof Villager tenant ? tenant : null;
    }

    public void onCleanup(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            Villager tenant = this.getTenantEntity(serverLevel);
            if (tenant != null && tenant.isAlive()) {
                VillagerDeed.LOGGER.info("Tenant cleaned up");
                tenant.discard();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("MoveInTime", this.moveInTime);
        output.putString("RoomName", this.roomName);
        output.putString("tenantName", this.tenantName);
        EntityReference.store(this.tenant, output, "BoundTenant");

        if (this.tenantData != null) {
            output.store("TenantData", CompoundTag.CODEC, this.tenantData);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.moveInTime = input.getIntOr("MoveInTime", 0);
        this.roomName = input.getStringOr("RoomName", "Room");
        this.tenantName = input.getStringOr("tenantName", "");
        this.tenant = EntityReference.read(input, "BoundTenant");
        this.tenantData = input.read("TenantData", CompoundTag.CODEC).orElse(null);
    }

    /* sync */
    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        VillagerDeed.LOGGER.info("preRemoveSideEffects called, start cleanup");
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel serverLevel) {
            VillagerDeed.LOGGER.info("preRemoveSideEffects server check");
            onCleanup(serverLevel);
        }
    }

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