package com.namje.villagerdeed.block.entity.custom;

import com.namje.villagerdeed.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class VillagerDeedBlockEntity extends BlockEntity {
    private static final List<String> TENANT_NAMES = List.of(
            "Ramon", "Cedric", "Hunter", "Richard", "Clemence", "Ollie", "Fennel", "Percy",
            "Beatrice", "Camille", "Jasmine", "Eleanor", "Minerva", "Ignis", "Elena", "Laura",
            "Bonnabelle", "Meazar", "Jordell", "Daejon", "Sofia", "Charmvir", "Jeff", "Isabelle",
            "Poppy", "Freya", "Julia", "Claudia", "Clementine", "Ngoc", "Miyabi", "Billy",
            "Mina", "Alice", "Zhao", "Soup", "Kazu", "Reimu", "Jane", "Bea",
            "Willow", "Dora", "Olivia", "Tom", "Clyde", "Bonnie", "Coach", "Ellis",
            "Rachael", "Nick", "Vivian", "Maddie", "Sydney", "Claude", "Olive", "Doc",
            "Amelia", "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael",
            "Linda", "William", "Elizabeth", "David", "Barbara", "Susan", "Joseph", "Jessica",
            "Thomas", "Sarah", "Charles", "Karen", "Stanford", "Stanley", "Mabel", "Dipper",
            "Carmen", "Angela", "Roland", "Gebura", "Hod", "Elijah", "Garion", "Xiao",
            "Yuuri", "Chito", "Irina", "Rin", "Nadeshiko", "Mono", "Tich", "Soos",
            "Wendy", "Diana", "Kinzo", "Saya", "Madotsuki", "Kai", "TESTIFICATE", "Silence",
            "Kristen", "Saria", "Mumu", "Dorothy", "Ifrit", "Magallan", "Ling", "Jie",
            "Shu", "Nian", "Lee", "Aak", "Fu", "Fliss", "Ina"
    );

    public static final int MIN_MOVE_IN_TIME = 600;
    public static final int MAX_MOVE_IN_TIME = 2400;
    public static final int RESPAWN_TIME = 120;
    public static final double MAX_LEASH_DISTANCE = 16.0;
    public static final double HARD_TELEPORT_DISTANCE = 32.0;
    public static final int TENANT_UPD_TIME = 600;
    public static final int TENANT_LOGIC_TIME = 100;

    private int deedState = 0;

    private int nextMoveInTime = 0;
    private int moveInTime = 0;
    private String deedName = "";
    private String tenantName = "";

    private boolean locked = false;
    private @Nullable UUID ownerUUID;
    private @Nullable BlockPos bedPos;
    private @Nullable UUID tenantUUID;
    private @Nullable CompoundTag tenantData;

    private final Set<UUID> subscribedPlayers = new HashSet<>();

    private VillagerProfession tenantProfession = VillagerProfession.NONE;
    private final ContainerData data;

    public VillagerDeedBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.VILLAGERDEED_BE.get(), worldPosition, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int dataId) {
                return switch (dataId) {
                    case 0 -> VillagerDeedBlockEntity.this.moveInTime;
                    case 1 -> MAX_MOVE_IN_TIME;
                    case 2 -> VillagerDeedBlockEntity.this.deedState;
                    default -> 0;
                };
            }

            @Override
            public void set(int dataId, int value) {
                switch (dataId) {
                    case 0 -> VillagerDeedBlockEntity.this.moveInTime = value;
                    case 2 -> VillagerDeedBlockEntity.this.deedState = value;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    public ContainerData getData() {
        return this.data;
    }

    public boolean getLocked() { return this.locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getDeedName() {
        if (this.deedName.isBlank()) {
            return "Room";
        }
        return this.deedName;
    }
    public void setDeedName(String name) { this.deedName = name; }
    public String getTenantName() { return this.tenantName; }
    public void setTenantName(String name) {
        if (this.level instanceof ServerLevel serverLevel) {
            Villager activeTenant = this.getTenantEntity(serverLevel);

            if (name == null || name.isBlank()) {
                if (activeTenant != null) {
                    activeTenant.setCustomName(null);
                    this.tenantName = activeTenant.getDisplayName().getString();
                } else {
                    this.tenantName = Component.translatable("entity.minecraft.villager").getString();
                }
            } else {
                this.tenantName = name;
                if (activeTenant != null) {
                    activeTenant.setCustomName(Component.literal(name));
                }
            }

            if (activeTenant != null) {
                this.snapshotTenantData(activeTenant);
            }
        } else {
            this.tenantName = (name != null) ? name : Component.translatable("entity.minecraft.villager").getString();
        }

        this.setChanged();
    }

    public @Nullable UUID getOwnerUUID() { return this.ownerUUID; }
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
        this.setChanged();
    }

    public int getDeedState() {
        return this.deedState;
    }
    public void setDeedState(int state) {
        if (this.deedState != state) {
            this.deedState = state;
            this.setChanged();
        }
    }

    public boolean playerIsSubscribed(UUID playerUUID) {
        return this.subscribedPlayers.contains(playerUUID);
    }

    public void subscribePlayer(UUID playerUUID) {
        if (this.subscribedPlayers.add(playerUUID)) {
            this.markUpdated();
        }
    }

    public void unsubscribePlayer(UUID playerUUID) {
        if (this.subscribedPlayers.remove(playerUUID)) {
            this.markUpdated();
        }
    }

    public void toggleSubscription(UUID playerUUID) {
        if (this.playerIsSubscribed(playerUUID)) {
            this.unsubscribePlayer(playerUUID);
        } else {
            this.subscribePlayer(playerUUID);
        }
    }

    private void notifySubscribers(ServerLevel level, Component message) {
        if (this.subscribedPlayers.isEmpty()) {
            return;
        }
        for (UUID uuid : this.subscribedPlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }

    public VillagerProfession getTenantProfession() {
        return this.tenantProfession;
    }

    public void setTenantProfession(@Nullable VillagerProfession profession) {
        this.tenantProfession = (profession != null) ? profession : VillagerProfession.NONE;
        if (this.level instanceof ServerLevel serverLevel) {
            Villager activeTenant = this.getTenantEntity(serverLevel);
            if (activeTenant != null) {
                applyProfessionToTenant(activeTenant, this.tenantProfession);
                this.snapshotTenantData(activeTenant);
            }
        }
        this.markUpdated();
    }

    private void applyProfessionToTenant(Villager tenant, VillagerProfession profession) {
        if (profession == null || profession == VillagerProfession.NONE) {
            return;
        }
        int currentXp = tenant.getVillagerXp();
        tenant.setVillagerData(tenant.getVillagerData().setProfession(profession));
        tenant.setVillagerXp(Math.max(currentXp, 1));
    }

    public void toggleDeedAvailability() {
        if (this.deedState != 4) {
            setDeedState(4);
        } else {
            setDeedState(0);
            this.updateBedPresence(this.findAdjacentBedHead(level, this.getBlockPos()));
        }
    }

    public @Nullable BlockPos getBedPos() {
        return this.bedPos;
    }

    @Nullable
    public BlockPos findAdjacentBedHead(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof BedBlock && neighborState.hasProperty(BedBlock.PART)) {
                BedPart part = neighborState.getValue(BedBlock.PART);
                if (part == BedPart.HEAD) {
                    return neighborPos;
                } else if (part == BedPart.FOOT && neighborState.hasProperty(BedBlock.FACING)) {
                    Direction facing = neighborState.getValue(BedBlock.FACING);
                    BlockPos headPos = neighborPos.relative(facing);
                    BlockState headState = level.getBlockState(headPos);
                    if (headState.getBlock() instanceof BedBlock
                            && headState.hasProperty(BedBlock.PART)
                            && headState.getValue(BedBlock.PART) == BedPart.HEAD) {
                        return headPos;
                    }
                }
            }
        }
        return null;
    }

    private boolean findExistingDeed(Level level, BlockPos bedPos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = bedPos.relative(dir);
            if (!neighbor.equals(this.worldPosition) && level.getBlockEntity(neighbor) instanceof VillagerDeedBlockEntity) {
                return true;
            }
        }
        return false;
    }

    public void updateBedPresence(@Nullable BlockPos foundBedPos) {
        if (foundBedPos != null && this.level != null && !this.level.isClientSide()) {
            if (findExistingDeed(this.level, foundBedPos)) {
                this.level.destroyBlock(this.worldPosition, true);
                return;
            }
        }

        this.bedPos = foundBedPos;
        boolean hasBed = (foundBedPos != null);

        if (!hasBed) {
            cleanupTenant(this.level);
            this.tenantUUID = null;
            if (this.deedState != 0) {
                setDeedState(0);
                if (this.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        } else {
            if (this.deedState == 0) {
                Villager activeTenant = getTenantEntity(this.level);
                if (activeTenant != null) {
                    setDeedState(2);
                } else if (this.tenantData != null || this.tenantUUID != null) {
                    setDeedState(3);
                } else {
                    setDeedState(1);
                }
                if (this.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VillagerDeedBlockEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity.deedState == 0 || entity.deedState == 4) {
            return;
        }

        UUID verifiedUUID = validateTenant(entity.tenantUUID, serverLevel);

        if (!Objects.equals(verifiedUUID, entity.tenantUUID)) {
            entity.tenantUUID = verifiedUUID;
            entity.setChanged();
            serverLevel.sendBlockUpdated(pos, state, state, 2);
        }

        Villager activeTenant = entity.getTenantEntity(serverLevel);

        if (entity.bedPos == null && activeTenant != null) {
            entity.cleanupTenant(serverLevel);
            entity.tenantUUID = null;
            entity.setDeedState(0);
            return;
        }

        if (activeTenant != null) {
            if (entity.deedState != 2) {
                entity.setDeedState(2);
            }

            String currentDisplayName = activeTenant.getDisplayName().getString();
            if (entity.tenantName != null && !entity.tenantName.isEmpty() && !entity.tenantName.equals(currentDisplayName)) {
                activeTenant.setCustomName(Component.literal(entity.getTenantName()));
            }

            entity.nextMoveInTime = 0;
            entity.moveInTime = 0;

            if (serverLevel.getGameTime() % TENANT_LOGIC_TIME == 0) {
                entity.hoverAroundDeed(activeTenant, entity.getBlockPos());
                entity.restrictTenant(activeTenant, pos);
            }

            if (serverLevel.getGameTime() % TENANT_UPD_TIME == 0) {
                entity.snapshotTenantData(activeTenant);
            }
        } else {
            int targetState = (entity.tenantData != null || entity.tenantUUID != null) ? 3 : 1;
            if (entity.deedState != targetState) {
                entity.setDeedState(targetState);
            }

            if (entity.nextMoveInTime == 0 && entity.tenantData == null) {
                entity.nextMoveInTime = level.getRandom().nextIntBetweenInclusive(MIN_MOVE_IN_TIME, MAX_MOVE_IN_TIME);
            }

            entity.moveInTime++;

            int nextSpawnTime = (entity.tenantData == null) ? entity.nextMoveInTime : RESPAWN_TIME;
            if (entity.moveInTime >= nextSpawnTime) {
                if (entity.spawnTenant(serverLevel, pos)) {
                    entity.moveInTime = 0;
                    entity.setDeedState(2);
                }
            }
        }
    }

    private void restrictTenant(Villager tenant, BlockPos deedPos) {
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) return;
        if (this.getTenantEntity(level) == null) {
            return;
        }

        double distSqr = tenant.distanceToSqr(deedPos.getX() + 0.5,
                deedPos.getY(), deedPos.getZ() + 0.5);

        if (distSqr > HARD_TELEPORT_DISTANCE * HARD_TELEPORT_DISTANCE) {
            tenant.teleportTo(deedPos.getX() + 0.5, deedPos.getY() + 1.0, deedPos.getZ() + 0.5);
            tenant.getNavigation().stop();
            return;
        }

        if (distSqr > MAX_LEASH_DISTANCE * MAX_LEASH_DISTANCE) {
            if (tenant.getNavigation().isDone()) {
                Brain<Villager> brain = tenant.getBrain();
                stopVillagerMemory(tenant, serverLevel, brain);

                WalkTarget walkTarget = new WalkTarget(deedPos, 0.6f, 6);
                brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
            }
        }
    }

    public void summonTenant(Villager tenant, BlockPos deedPos) {
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) return;
        if (this.getTenantEntity(level) == null) {
            return;
        }

        Player owner = this.getOwnerPlayer(level);
        if (owner != null) {
            serverLevel.playSound(null, this.getBlockPos(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2f, 1f);
        }

        Brain<Villager> brain = tenant.getBrain();
        stopVillagerMemory(tenant, serverLevel, brain);

        WalkTarget walkTarget = new WalkTarget(deedPos, 1f, 3);
        brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    private void hoverAroundDeed(Villager tenant, BlockPos deedPos) {
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) return;
        Brain<Villager> brain = tenant.getBrain();

        if (!brain.isActive(Activity.WORK) && !brain.isActive(Activity.IDLE)) {
            return;
        }
        stopVillagerMemory(tenant, serverLevel, brain);

        WalkTarget walkTarget = new WalkTarget(deedPos, 0.6f, 10);
        brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    private static void stopVillagerMemory(Villager tenant, ServerLevel serverLevel, Brain<Villager> brain) {
        brain.stopAll(serverLevel, tenant);

        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.PATH);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        brain.eraseMemory(MemoryModuleType.BREED_TARGET);
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }

    private void snapshotTenantData(Villager tenant) {
        if (this.level == null || !(this.level instanceof ServerLevel)) return;

        CompoundTag tag = new CompoundTag();
        tenant.saveWithoutId(tag);
        this.tenantData = tag;
        this.setChanged();
    }

    private static @Nullable UUID validateTenant(@Nullable UUID tenantUUID, ServerLevel level) {
        if (tenantUUID != null) {
            Entity entity = level.getEntity(tenantUUID);
            if (entity instanceof Villager villager && villager.isAlive()) {
                return tenantUUID;
            }
        }
        return null;
    }

    private boolean spawnTenant(ServerLevel level, BlockPos pos) {
        Villager tenant = EntityType.VILLAGER.create(level);
        if (tenant == null) {
            return false;
        }

        if (this.tenantData != null && !this.tenantData.isEmpty()) {
            tenant.load(this.tenantData);
            notifySubscribers(level, Component.translatable("block.villagerdeed.namje_villagerdeed.respawned",
                    this.getTenantName(), this.getDeedName()));
        } else {
            this.tenantName = TENANT_NAMES.get(level.getRandom().nextInt(TENANT_NAMES.size()));

            notifySubscribers(level, Component.translatable("block.villagerdeed.namje_villagerdeed.moved_in",
                    this.getTenantName(), this.getDeedName()));

            List<VillagerProfession> professions = getProfessions();
            if (!professions.isEmpty()) {
                this.tenantProfession = professions.get(level.getRandom().nextInt(professions.size()));
            } else {
                this.tenantProfession = VillagerProfession.NONE;
            }
        }

        applyProfessionToTenant(tenant, this.tenantProfession);
        if (this.tenantName != null && !this.tenantName.isEmpty()) {
            tenant.setCustomName(Component.literal(this.tenantName));
        }
        tenant.setUUID(UUID.randomUUID());
        tenant.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        tenant.setHealth(tenant.getMaxHealth());

        BlockPos linkedBedPos = this.getBedPos();
        if (linkedBedPos != null) {
            PoiManager poiManager = level.getPoiManager();
            GlobalPos targetBedGlobalPos = GlobalPos.of(level.dimension(), linkedBedPos);
            AABB searchBox = new AABB(linkedBedPos).inflate(48.0);
            List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class, searchBox);

            for (Villager villager : nearbyVillagers) {
                villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(homePos -> {
                    if (homePos.equals(targetBedGlobalPos)) {
                        villager.getBrain().eraseMemory(MemoryModuleType.HOME);
                    }
                });
            }
            if (poiManager.exists(linkedBedPos, holder -> holder.is(PoiTypes.HOME))) {
                poiManager.release(linkedBedPos);
            }

            if (poiManager.exists(linkedBedPos, holder -> holder.is(PoiTypes.HOME))) {
                poiManager.take(
                        holder -> holder.is(PoiTypes.HOME),
                        (type, p) -> p.equals(linkedBedPos),
                        linkedBedPos,
                        1
                );
            }
            tenant.getBrain().setMemory(MemoryModuleType.HOME, targetBedGlobalPos);
            tenant.getBrain().setMemory(MemoryModuleType.LAST_SLEPT, level.getGameTime());
            tenant.startSleeping(linkedBedPos);
        }

        if (level.addFreshEntity(tenant)) {
            this.tenantUUID = tenant.getUUID();

            snapshotTenantData(tenant);

            Vec3 tenantPos = tenant.position();
            level.sendParticles(ParticleTypes.POOF, tenantPos.x, tenantPos.y + 0.5, tenantPos.z, 20, 0.25, 0.25, 0.25, 0.05);

            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);

            return true;
        }

        return false;
    }

    public @Nullable Player getOwnerPlayer(Level level) { return this.ownerUUID != null ? level.getPlayerByUUID(this.ownerUUID) : null; }

    @Nullable
    public Villager getTenantEntity(Level level) {
        if (this.tenantUUID == null) return null;
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.tenantUUID);
            if (entity instanceof Villager villager && villager.isAlive()) {
                return villager;
            }
        }
        return null;
    }

    public void cleanupTenant(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            Villager tenant = this.getTenantEntity(serverLevel);
            if (tenant != null && tenant.isAlive()) {
                Vec3 tenantPos = tenant.position();
                serverLevel.sendParticles(ParticleTypes.POOF, tenantPos.x, tenantPos.y + 0.5, tenantPos.z, 20, 0.25, 0.25, 0.25, 0.05);

                BlockPos linkedBedPos = this.getBedPos();
                if (linkedBedPos != null) {
                    serverLevel.getPoiManager().release(linkedBedPos);
                }
                tenant.discard();
            }
        }
    }

    public void evictTenant(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            if (this.tenantUUID != null) {
                notifySubscribers(serverLevel, Component.translatable("block.villagerdeed.namje_villagerdeed.evicted",
                        this.getTenantName(), this.getDeedName()));
            }

            cleanupTenant(serverLevel);
            this.tenantUUID = null;
            this.tenantData = null;
            this.tenantName = "";
            this.tenantProfession = VillagerProfession.NONE;
            this.moveInTime = 0;
            setDeedState(1);
            markUpdated();
        }
    }

    public void onCleanup(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            if (this.tenantData != null) {
                notifySubscribers(serverLevel, Component.translatable("block.villagerdeed.namje_villagerdeed.destroyed",
                        this.getDeedName(), this.getTenantName()));
            }

            cleanupTenant(level);

            if (this.bedPos != null) {
                if (serverLevel.getBlockState(this.bedPos).getBlock() instanceof BedBlock) {
                    serverLevel.destroyBlock(this.bedPos, true);
                }
                this.bedPos = null;
            }
        }
    }

    private static List<VillagerProfession> getProfessions() {
        return BuiltInRegistries.VILLAGER_PROFESSION.stream()
                .filter(profession -> profession != VillagerProfession.NONE)
                .toList();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DeedState", this.deedState);
        tag.putInt("MoveInTime", this.moveInTime);
        tag.putString("DeedName", this.deedName);
        tag.putString("tenantName", this.tenantName);
        tag.putBoolean("Locked", this.locked);

        if (this.tenantUUID != null) {
            tag.putUUID("BoundTenant", this.tenantUUID);
        }

        if (this.bedPos != null) {
            tag.put("BedPos", NbtUtils.writeBlockPos(this.bedPos));
        }

        if (this.tenantData != null) {
            tag.put("TenantData", this.tenantData);
        }

        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }

        if (!this.subscribedPlayers.isEmpty()) {
            ListTag listTag = new ListTag();
            for (UUID uuid : this.subscribedPlayers) {
                listTag.add(NbtUtils.createUUID(uuid));
            }
            tag.put("SubscribedPlayers", listTag);
        }

        if (this.tenantProfession != null && this.tenantProfession != VillagerProfession.NONE) {
            ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(this.tenantProfession);
            if (key != null) {
                tag.putString("TenantProfession", key.toString());
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.deedState = tag.getInt("DeedState");
        this.moveInTime = tag.getInt("MoveInTime");
        this.deedName = tag.getString("DeedName");
        this.tenantName = tag.getString("tenantName");
        this.locked = tag.getBoolean("Locked");

        if (tag.hasUUID("BoundTenant")) {
            this.tenantUUID = tag.getUUID("BoundTenant");
        } else {
            this.tenantUUID = null;
        }

        if (tag.contains("BedPos", Tag.TAG_COMPOUND)) {
            this.bedPos = NbtUtils.readBlockPos(tag.getCompound("BedPos"));
        } else {
            this.bedPos = null;
        }

        if (tag.contains("TenantData", Tag.TAG_COMPOUND)) {
            this.tenantData = tag.getCompound("TenantData");
        } else {
            this.tenantData = null;
        }

        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        } else {
            this.ownerUUID = null;
        }

        this.subscribedPlayers.clear();
        if (tag.contains("SubscribedPlayers", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("SubscribedPlayers", Tag.TAG_INT_ARRAY);
            for (int i = 0; i < listTag.size(); i++) {
                this.subscribedPlayers.add(NbtUtils.loadUUID(listTag.get(i)));
            }
        }

        if (tag.contains("TenantProfession", Tag.TAG_STRING)) {
            ResourceLocation loc = ResourceLocation.tryParse(tag.getString("TenantProfession"));
            if (loc != null) {
                this.tenantProfession = BuiltInRegistries.VILLAGER_PROFESSION.get(loc);
            } else {
                this.tenantProfession = VillagerProfession.NONE;
            }
        } else {
            this.tenantProfession = VillagerProfession.NONE;
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public void markUpdated() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}