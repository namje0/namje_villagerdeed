package com.namje.villagerdeed.block.entity.custom;

import com.mojang.serialization.Codec;
import com.namje.villagerdeed.VillagerDeed;
import com.namje.villagerdeed.block.entity.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

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
            "Wendy", "Diana", "Kinzo", "Saya", "Madotsuki", "Kai", "TESTIFICATE"
    );

    public static final int MAX_MOVE_IN_TIME = 120;
    public static final double MAX_LEASH_DISTANCE = 16.0;
    public static final double HARD_TELEPORT_DISTANCE = 32.0;
    public static final int TENANT_UPD_TIME = 600;
    public static final int TENANT_LOGIC_TIME = 100;

    /*
    0 = invalid, either due to no bed or other means; no functionality running
    1 = inactive, bound to a bed but no tenant bound
    2 = active, bed and tenant bound
    3 = waiting, tenant is dead/otherwise missing and waiting to respawn
    4 = deed set to unavailable by player
     */
    private int deedState = 0;

    private int moveInTime = 0;
    private String deedName = "";
    private String tenantName = "";

    private boolean locked = false;
    private @Nullable UUID ownerUUID;
    private @Nullable BlockPos bedPos;
    private @Nullable EntityReference<LivingEntity> tenant;
    private @Nullable CompoundTag tenantData;

    private final Set<UUID> subscribedPlayers = new HashSet<>();
    private static final Codec<List<UUID>> UUID_LIST_CODEC = Codec.list(UUIDUtil.CODEC);

    private ResourceKey<VillagerProfession> tenantProfession = VillagerProfession.NONE;
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

            if (name.isBlank()) {
                if (activeTenant != null) {
                    activeTenant.setCustomName(null);
                    this.tenantName = activeTenant.getDisplayName().getString();
                } else {
                    this.tenantName = Component.translatable("entity.minecraft.villager").getString();
                }
            } else {
                this.tenantName = name;
                if (activeTenant != null) {
                    if (name.isBlank()) {
                        activeTenant.setCustomName(null);
                    } else {
                        activeTenant.setCustomName(Component.literal(name));
                    }
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
            VillagerDeed.LOGGER.info("subscribed player: " + playerUUID.toString());
            this.markUpdated();
        }
    }

    public void unsubscribePlayer(UUID playerUUID) {
        if (this.subscribedPlayers.remove(playerUUID)) {
            VillagerDeed.LOGGER.info("unsubscribed player: " + playerUUID.toString());
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
        VillagerDeed.LOGGER.info("logging msg to subscribers: " + message.toString());
        for (UUID uuid : this.subscribedPlayers) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }

    public ResourceKey<VillagerProfession> getTenantProfession() { return this.tenantProfession; }
    public void setTenantProfession(ResourceKey<VillagerProfession> profession) {
        this.tenantProfession = profession;
        if (this.level instanceof ServerLevel serverLevel) {
            Villager activeTenant = this.getTenantEntity(serverLevel);
            if (activeTenant != null) {
                applyProfessionToTenant(activeTenant, serverLevel, profession);
                this.snapshotTenantData(activeTenant);
            }
        }
        this.markUpdated();
    }

    private void applyProfessionToTenant(Villager tenant, ServerLevel level, ResourceKey<VillagerProfession> professionKey) {
        if (professionKey == VillagerProfession.NONE) {
            return;
        }
        Holder<VillagerProfession> profession = level.registryAccess()
                .lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .getOrThrow(professionKey);

        int currentXp = tenant.getVillagerXp();
        tenant.setVillagerData(tenant.getVillagerData().withProfession(profession));
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
            this.tenant = null;
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
                } else if (this.tenantData != null || this.tenant != null) {
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

        if (entity.deedState == 0 || entity.deedState == 4) {
            return;
        }

        EntityReference<LivingEntity> verifiedRef = validateTenant(entity.tenant, serverLevel);

        if (!Objects.equals(verifiedRef, entity.tenant)) {
            entity.tenant = verifiedRef;
            entity.setChanged();
            serverLevel.sendBlockUpdated(pos, state, state, 2);
        }

        Villager activeTenant = entity.getTenantEntity(serverLevel);

        if (entity.bedPos == null && activeTenant != null) {
            entity.cleanupTenant(serverLevel);
            entity.tenant = null;
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

            entity.moveInTime = 0;

            if (serverLevel.getGameTime() % TENANT_LOGIC_TIME == 0) {
                entity.hoverAroundDeed(activeTenant, entity.getBlockPos());
                entity.restrictTenant(activeTenant, pos);
            }

            if (serverLevel.getGameTime() % TENANT_UPD_TIME == 0) {
                entity.snapshotTenantData(activeTenant);
            }
        } else {
            int targetState = (entity.tenantData != null || entity.tenant != null) ? 3 : 1;
            if (entity.deedState != targetState) {
                entity.setDeedState(targetState);
            }

            entity.moveInTime++;
            if (entity.moveInTime >= MAX_MOVE_IN_TIME) {
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
            VillagerDeed.LOGGER.info("tenant far from deed; teleport to it");
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
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) return;

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

                notifySubscribers(level, Component.translatable("block.villagerdeed.namje_villagerdeed.respawned",
                        this.getTenantName(), this.getDeedName()));
            }
        } else {
            this.tenantName = TENANT_NAMES.get(level.getRandom().nextInt(TENANT_NAMES.size()));

            notifySubscribers(level, Component.translatable("block.villagerdeed.namje_villagerdeed.moved_in",
                    this.getTenantName(), this.getDeedName()));

            List<VillagerProfession> professions = getProfessions();
            if (!professions.isEmpty()) {
                VillagerProfession randomProfession = professions.get(level.getRandom().nextInt(professions.size()));
                this.tenantProfession = BuiltInRegistries.VILLAGER_PROFESSION
                        .getResourceKey(randomProfession)
                        .orElse(VillagerProfession.NONE);
            }
        }

        applyProfessionToTenant(tenant, level, this.tenantProfession);
        if (this.tenantName != null && !this.tenantName.isEmpty()) {
            tenant.setCustomName(Component.literal(this.tenantName));
        }
        tenant.setUUID(UUID.randomUUID());
        tenant.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        tenant.setHealth(tenant.getMaxHealth());

        // bind to bed
        BlockPos linkedBedPos = this.getBedPos();
        if (linkedBedPos != null) {
            PoiManager poiManager = level.getPoiManager();
            GlobalPos targetBedGlobalPos = GlobalPos.of(level.dimension(), bedPos);
            AABB searchBox = new AABB(bedPos).inflate(48.0);
            List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class, searchBox);

            for (Villager villager : nearbyVillagers) {
                villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(homePos -> {
                    if (homePos.equals(targetBedGlobalPos)) {
                        villager.getBrain().eraseMemory(MemoryModuleType.HOME);
                    }
                });
            }
            if (poiManager.exists(bedPos, holder -> holder.is(PoiTypes.HOME))) {
                poiManager.release(bedPos);
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
            this.tenant = EntityReference.of(tenant);

            snapshotTenantData(tenant);

            Position tenantPos = tenant.position();
            level.sendParticles(ParticleTypes.POOF, tenantPos.x(), tenantPos.y() + 0.5, tenantPos.z(), 20, 0.25, 0.25, 0.25, 0.05);

            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
            VillagerDeed.LOGGER.info("tenant created/restored and bound to deed");

            return true;
        }

        return false;
    }

    public @Nullable Player getOwnerPlayer(Level level) { return this.ownerUUID != null ? level.getPlayerByUUID(this.ownerUUID) : null; }

    @Nullable
    public Villager getTenantEntity(Level level) {
        return getTenantFromRef(this.tenant, level);
    }

    private static @Nullable Villager getTenantFromRef(@Nullable EntityReference<LivingEntity> ref, Level level) {
        if (ref == null) return null;
        return EntityReference.getLivingEntity(ref, level) instanceof Villager tenant ? tenant : null;
    }

    public void cleanupTenant(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            Villager tenant = this.getTenantEntity(serverLevel);
            if (tenant != null && tenant.isAlive()) {
                Position tenantPos = tenant.position();
                serverLevel.sendParticles(ParticleTypes.POOF, tenantPos.x(), tenantPos.y() + 0.5, tenantPos.z(), 20, 0.25, 0.25, 0.25, 0.05);

                // unlink bed
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
            if (this.tenant != null) {
                notifySubscribers(serverLevel, Component.translatable("block.villagerdeed.namje_villagerdeed.evicted",
                        this.getTenantName(), this.getDeedName()));
            }

            cleanupTenant(serverLevel);
            this.tenant = null;
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
                .filter(profession -> BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(profession)
                        .map(key -> !key.equals(VillagerProfession.NONE))
                        .orElse(true))
                .toList();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("DeedState", this.deedState);
        output.putInt("MoveInTime", this.moveInTime);
        output.putString("DeedName", this.deedName);
        output.putString("tenantName", this.tenantName);
        output.putBoolean("Locked", this.locked);
        EntityReference.store(this.tenant, output, "BoundTenant");

        if (this.bedPos != null) {
            output.store("BedPos", BlockPos.CODEC, this.bedPos);
        }

        if (this.tenantData != null) {
            output.store("TenantData", CompoundTag.CODEC, this.tenantData);
        }

        if (this.ownerUUID != null) {
            output.store("OwnerUUID", UUIDUtil.CODEC, this.ownerUUID);
        }

        if (!this.subscribedPlayers.isEmpty()) {
            output.store("SubscribedPlayers", UUID_LIST_CODEC, List.copyOf(this.subscribedPlayers));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.deedState = input.getIntOr("DeedState", 0);
        this.moveInTime = input.getIntOr("MoveInTime", 0);
        this.deedName = input.getStringOr("DeedName", "Room");
        this.tenantName = input.getStringOr("tenantName", "");
        this.tenant = EntityReference.read(input, "BoundTenant");
        this.bedPos = input.read("BedPos", BlockPos.CODEC).orElse(null);
        this.tenantData = input.read("TenantData", CompoundTag.CODEC).orElse(null);
        this.ownerUUID = input.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
        this.locked = input.getBooleanOr("Locked", false);
        this.subscribedPlayers.clear();
        input.read("SubscribedPlayers", UUID_LIST_CODEC).ifPresent(this.subscribedPlayers::addAll);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel serverLevel) {
            onCleanup(serverLevel);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public void markUpdated() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}