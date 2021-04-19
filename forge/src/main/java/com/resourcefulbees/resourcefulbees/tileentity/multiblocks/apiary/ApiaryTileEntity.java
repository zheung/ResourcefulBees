package com.resourcefulbees.resourcefulbees.tileentity.multiblocks.apiary;


import com.resourcefulbees.resourcefulbees.api.ICustomBee;
import com.resourcefulbees.resourcefulbees.block.multiblocks.apiary.ApiaryBlock;
import com.resourcefulbees.resourcefulbees.block.multiblocks.apiary.ApiaryBreederBlock;
import com.resourcefulbees.resourcefulbees.block.multiblocks.apiary.ApiaryStorageBlock;
import com.resourcefulbees.resourcefulbees.config.Config;
import com.resourcefulbees.resourcefulbees.container.AutomationSensitiveItemStackHandler;
import com.resourcefulbees.resourcefulbees.container.UnvalidatedApiaryContainer;
import com.resourcefulbees.resourcefulbees.container.ValidatedApiaryContainer;
import com.resourcefulbees.resourcefulbees.item.BeeJar;
import com.resourcefulbees.resourcefulbees.lib.ApiaryTabs;
import com.resourcefulbees.resourcefulbees.lib.BeeConstants;
import com.resourcefulbees.resourcefulbees.lib.NBTConstants;
import com.resourcefulbees.resourcefulbees.network.NetPacketHandler;
import com.resourcefulbees.resourcefulbees.network.packets.UpdateClientApiaryMessage;
import com.resourcefulbees.resourcefulbees.registry.ModBlockEntityTypes;
import com.resourcefulbees.resourcefulbees.registry.ModBlocks;
import com.resourcefulbees.resourcefulbees.registry.ModItems;
import com.resourcefulbees.resourcefulbees.tileentity.multiblocks.MultiBlockHelper;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.resourcefulbees.resourcefulbees.lib.BeeConstants.MIN_HIVE_TIME;
import static com.resourcefulbees.resourcefulbees.lib.BeeConstants.RAINBOW_COLOR;

public class ApiaryTileEntity extends BlockEntity implements TickableBlockEntity, MenuProvider, IApiaryMultiblock {
    public static final int IMPORT = 0;
    public static final int EXPORT = 2;
    public static final int EMPTY_JAR = 1;
    public final Map<String, ApiaryBee> bees = new LinkedHashMap<>();
    private final List<BlockPos> structureBlocks = new ArrayList<>();
    protected int tier;
    private boolean isValidApiary;
    private boolean previewed;
    private final ApiaryTileEntity.TileStackHandler tileStackHandler = new ApiaryTileEntity.TileStackHandler(3);
    private final LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(this::getTileStackHandler);
    private int horizontalOffset = 0;
    private int verticalOffset = 0;
    private int numPlayersUsing;
    private int ticksSinceValidation;
    private int ticksSinceSync;
    private BlockPos storagePos;
    private BlockPos breederPos;
    private ApiaryStorageTileEntity apiaryStorage;
    private ApiaryBreederTileEntity apiaryBreeder;
    protected int ticksSinceBeesFlagged;


    public ApiaryTileEntity() {
        super(ModBlockEntityTypes.APIARY_TILE_ENTITY.get());
    }

    public ApiaryTileEntity(BlockEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    //region PLAYER SYNCING
    public static int calculatePlayersUsingSync(Level world, ApiaryTileEntity apiaryTileEntity, int ticksSinceSync, int posX, int posY, int posZ, int numPlayersUsing) {
        if (!world.isClientSide && numPlayersUsing != 0 && (ticksSinceSync + posX + posY + posZ) % 200 == 0) {
            numPlayersUsing = calculatePlayersUsing(world, apiaryTileEntity, posX, posY, posZ);
        }

        return numPlayersUsing;
    }

    public static int calculatePlayersUsing(Level world, ApiaryTileEntity apiaryTileEntity, int posX, int posY, int posZ) {
        int i = 0;
        float f = 5.0F;

        for (Player playerentity : world.getEntitiesOfClass(Player.class, new AABB(posX - f, posY - f, posZ - f, (posX + 1) + f, (posY + 1) + f, (posZ + 1) + f))) {
            if (playerentity.containerMenu instanceof ValidatedApiaryContainer) {
                ApiaryTileEntity apiaryTileEntity1 = ((ValidatedApiaryContainer) playerentity.containerMenu).getApiaryTileEntity();
                if (apiaryTileEntity1 == apiaryTileEntity) {
                    ++i;
                }
            }
        }

        return i;
    }

    public static void syncApiaryToPlayersUsing(Level world, BlockPos pos, CompoundTag data) {
        NetPacketHandler.sendToAllLoaded(new UpdateClientApiaryMessage(pos, data), world, pos);
    }
    //endregion

    public boolean isValidApiary(boolean runValidation) {
        if (runValidation) {
            runStructureValidation(null);
        }
        return isValidApiary;
    }

    public int getTier() {
        return tier;
    }

    public int getMaxBees() {
        return Config.APIARY_MAX_BEES.get();
    }

    public int getBeeCount() {
        return this.bees.size();
    }

    public ApiaryStorageTileEntity getApiaryStorage() {
        if (level != null && getStoragePos() != null) {
            BlockEntity tile = level.getBlockEntity(getStoragePos());
            if (tile instanceof ApiaryStorageTileEntity) {
                return (ApiaryStorageTileEntity) tile;
            }
        }
        setStoragePos(null);
        return null;
    }

    public ApiaryBreederTileEntity getApiaryBreeder() {
        if (level != null && getBreederPos() != null) {
            BlockEntity tile = level.getBlockEntity(getBreederPos());
            if (tile instanceof ApiaryBreederTileEntity) {
                return (ApiaryBreederTileEntity) tile;
            }
        }
        setBreederPos(null);
        return null;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    //region BEE HANDLING
    public boolean releaseBee(@NotNull BlockState state, ApiaryBee apiaryBee, boolean exportBee) {
        BlockPos blockPos = this.getBlockPos();
        Direction direction = state.getValue(BeehiveBlock.FACING);
        BlockPos blockPos1 = blockPos.relative(direction);
        CompoundTag nbt = apiaryBee.entityData;

        if (level != null && this.level.getBlockState(blockPos1).getCollisionShape(this.level, blockPos1).isEmpty()) {
            nbt.remove("Passengers");
            nbt.remove("Leash");
            nbt.remove("UUID");
            Entity entity = EntityType.loadEntityRecursive(nbt, this.level, entity1 -> entity1);
            if (entity == null) return true;
            BeeInfoUtils.setEntityLocationAndAngle(blockPos, direction, entity);

            if (entity instanceof Bee) {
                Bee vanillaBeeEntity = (Bee) entity;

                if (nbt.getBoolean("HasNectar")) {
                    vanillaBeeEntity.dropOffNectar();

                    if (!exportBee && isValidApiary(true)) {
                        getApiaryStorage().deliverHoneycomb(((Bee) entity), getTier());
                    }
                }

                this.ageBee(apiaryBee.getTicksInHive(), vanillaBeeEntity);

                if (exportBee) {
                    exportBee(vanillaBeeEntity);
                } else {
                    BlockPos hivePos = this.getBlockPos();
                    this.level.playSound(null, hivePos.getX(), hivePos.getY(), hivePos.getZ(), SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.level.addFreshEntity(entity);
                }
            }
            return true;
        }
        return false;
    }

    private void ageBee(int ticksInHive, Bee beeEntity) {
        BeeInfoUtils.ageBee(ticksInHive, beeEntity);
    }

    public boolean tryEnterHive(Entity bee, boolean hasNectar, boolean imported) {
        if (isValidApiary(true))
            return this.tryEnterHive(bee, hasNectar, 0, imported);
        return false;
    }

    public boolean tryEnterHive(@NotNull Entity bee, boolean hasNectar, int ticksInHive, boolean imported) {
        if (this.level != null && bee instanceof Bee) {
            String type = BeeConstants.VANILLA_BEE_TYPE;
            String beeColor = BeeConstants.VANILLA_BEE_COLOR;

            if (bee instanceof ICustomBee) {
                type = ((ICustomBee) bee).getBeeType();
            }

            if (!this.bees.containsKey(type) && this.bees.size() < getMaxBees()) {
                bee.ejectPassengers();
                CompoundTag nbt = new CompoundTag();
                bee.save(nbt);

                int maxTimeInHive = getMaxTimeInHive(BeeConstants.MAX_TIME_IN_HIVE);
                if (bee instanceof ICustomBee) {
                    ICustomBee iCustomBee = (ICustomBee) bee;
                    maxTimeInHive = getMaxTimeInHive(iCustomBee.getBeeData().getMaxTimeInHive());

                    if (iCustomBee.getBeeData().getColorData().hasPrimaryColor()) {
                        beeColor = iCustomBee.getBeeData().getColorData().getPrimaryColor();
                    } else if (iCustomBee.getBeeData().getColorData().isRainbowBee()) {
                        beeColor = RAINBOW_COLOR;
                    } else if (iCustomBee.getBeeData().getColorData().hasHoneycombColor()) {
                        beeColor = iCustomBee.getBeeData().getColorData().getHoneycombColor();
                    }
                }

                int finalMaxTimeInHive = maxTimeInHive;
                String finalType = type;

                String finalBeeColor = beeColor;

                Component displayName = bee.getName();

                this.bees.computeIfAbsent(finalType, k -> new ApiaryBee(nbt, ticksInHive, hasNectar ? finalMaxTimeInHive : MIN_HIVE_TIME, finalType, finalBeeColor, displayName));
                BlockPos pos = this.getBlockPos();
                this.level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);

                if (imported) {
                    this.bees.get(type).setLocked(true);
                }

                if (this.getNumPlayersUsing() > 0)
                    syncApiaryToPlayersUsing(this.level, this.getBlockPos(), this.saveToNBT(new CompoundTag()));

                bee.remove();
                return true;
            }
        }
        return false;
    }

    private int getMaxTimeInHive(int timeInput) {
        if (this.tier != 1) {
            if (this.tier == 0) {
                return (int) (timeInput * 1.05);
            } else {
                return (int) (timeInput * (1 - getTier() * .05));
            }
        }
        return timeInput;
    }

    @Override
    public void tick() {
        if (level != null) {
            BlockPos blockpos = this.getBlockPos();
            int x = blockpos.getX();
            int y = blockpos.getY();
            int z = blockpos.getZ();
            ++ticksSinceSync;
            this.setNumPlayersUsing(calculatePlayersUsingSync(this.level, this, this.ticksSinceSync, x, y, z, this.getNumPlayersUsing()));

            this.tickBees();

            if (!level.isClientSide && isValidApiary) {
                if (ticksSinceValidation >= 20) runStructureValidation(null);
                else ticksSinceValidation++;

                if (this.bees.size() > 0 && this.level.getRandom().nextDouble() < 0.005D) {
                    double d0 = blockpos.getX() + 0.5D;
                    double d1 = blockpos.getY();
                    double d2 = blockpos.getZ() + 0.5D;
                    this.level.playSound(null, d0, d1, d2, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                ticksSinceBeesFlagged++;
                if (ticksSinceBeesFlagged == 80) {
                    BeeInfoUtils.flagBeesInRange(worldPosition, level);
                    ticksSinceBeesFlagged = 0;
                }
            }
        }
    }

    private void tickBees() {
        if (this.level != null) {
            Iterator<Map.Entry<String, ApiaryBee>> iterator = this.bees.entrySet().iterator();
            BlockState blockstate = this.getBlockState();

            while (iterator.hasNext()) {
                Map.Entry<String, ApiaryBee> element = iterator.next();
                ApiaryBee apiaryBee = element.getValue();
                if (!apiaryBee.isLocked() && apiaryBee.getTicksInHive() > apiaryBee.minOccupationTicks && !level.isClientSide) {


                    if (this.releaseBee(blockstate, apiaryBee, false)) {
                        iterator.remove();
                        if (this.getNumPlayersUsing() > 0 && !this.level.isClientSide)
                            syncApiaryToPlayersUsing(this.level, this.getBlockPos(), this.saveToNBT(new CompoundTag()));
                    }
                } else {
                    apiaryBee.setTicksInHive(apiaryBee.getTicksInHive() + 1);
                    apiaryBee.setTicksInHive(Math.min(apiaryBee.getTicksInHive(), Integer.MAX_VALUE - 1));
                }
            }
        }
    }

    public boolean isFullOfBees() {
        return this.bees.size() >= getMaxBees();
    }

    public boolean isAllowedBee() {
        Block hive = getBlockState().getBlock();
        return isValidApiary(false) && hive instanceof ApiaryBlock;
    }

    public void lockOrUnlockBee(String beeType) {
        this.bees.get(beeType).setLocked(!this.bees.get(beeType).isLocked());
        syncApiaryToPlayersUsing(this.level, this.getBlockPos(), this.saveToNBT(new CompoundTag()));
    }
    //endregion

    //region NBT HANDLING

    @Nonnull
    public ListTag writeBees() {
        ListTag listnbt = new ListTag();

        this.bees.forEach((key, apiaryBee) -> {
            apiaryBee.entityData.remove("UUID");
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.put("EntityData", apiaryBee.entityData);
            compoundnbt.putInt("TicksInHive", apiaryBee.getTicksInHive());
            compoundnbt.putInt("MinOccupationTicks", apiaryBee.minOccupationTicks);
            compoundnbt.putBoolean(NBTConstants.NBT_LOCKED, apiaryBee.isLocked());
            compoundnbt.putString(NBTConstants.NBT_BEE_TYPE, apiaryBee.beeType);
            compoundnbt.putString(NBTConstants.NBT_COLOR, apiaryBee.beeColor);
            compoundnbt.putString(NBTConstants.NBT_BEE_NAME, Component.Serializer.toJson(apiaryBee.displayName));
            listnbt.add(compoundnbt);
        });

        return listnbt;
    }

    public void loadBees(CompoundTag nbt) {
        ListTag listnbt = nbt.getList(NBTConstants.NBT_BEES, 10);

        if (!listnbt.isEmpty()) {
            for (int i = 0; i < listnbt.size(); ++i) {
                CompoundTag data = listnbt.getCompound(i);

                String beeType = data.getString(NBTConstants.NBT_BEE_TYPE);
                String beeColor = data.contains(NBTConstants.NBT_COLOR) ? data.getString(NBTConstants.NBT_COLOR) : BeeConstants.VANILLA_BEE_COLOR;
                Component displayName = data.contains(NBTConstants.NBT_BEE_NAME) ? Component.Serializer.fromJson(data.getString(NBTConstants.NBT_BEE_NAME)) : new TextComponent("Temp Bee Name");

                this.bees.computeIfAbsent(data.getString(NBTConstants.NBT_BEE_TYPE), k -> new ApiaryBee(
                        data.getCompound("EntityData"),
                        data.getInt("TicksInHive"),
                        data.getInt("MinOccupationTicks"),
                        beeType,
                        beeColor,
                        displayName));

                this.bees.get(beeType).setLocked(data.getBoolean(NBTConstants.NBT_LOCKED));
            }
        }
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundTag nbt) {
        super.load(state, nbt);
        this.loadFromNBT(nbt);
    }

    @NotNull
    @Override
    public CompoundTag save(@Nonnull CompoundTag nbt) {
        super.save(nbt);
        return this.saveToNBT(nbt);
    }

    public void loadFromNBT(CompoundTag nbt) {
        loadBees(nbt);

        if (nbt.contains(NBTConstants.NBT_VALID_APIARY))
            this.isValidApiary = nbt.getBoolean(NBTConstants.NBT_VALID_APIARY);
        if (nbt.contains(NBTConstants.NBT_VERT_OFFSET))
            this.setVerticalOffset(nbt.getInt(NBTConstants.NBT_VERT_OFFSET));
        if (nbt.contains(NBTConstants.NBT_HOR_OFFSET))
            this.setHorizontalOffset(nbt.getInt(NBTConstants.NBT_HOR_OFFSET));
        CompoundTag invTag = nbt.getCompound(NBTConstants.NBT_INVENTORY);
        getTileStackHandler().deserializeNBT(invTag);

        if (nbt.contains(NBTConstants.NBT_STORAGE_POS))
            setStoragePos(NbtUtils.readBlockPos(nbt.getCompound(NBTConstants.NBT_STORAGE_POS)));
        if (nbt.contains(NBTConstants.NBT_BREEDER_POS))
            setBreederPos(NbtUtils.readBlockPos(nbt.getCompound(NBTConstants.NBT_BREEDER_POS)));
        if (nbt.contains(NBTConstants.NBT_TIER)) {
            setTier(nbt.getInt(NBTConstants.NBT_TIER));
        }
        validateBees();
    }

    private void validateBees() {
        if (this.level == null) return;
        bees.forEach((s, apiaryBee) -> {
            String id = apiaryBee.entityData.getString("id");
            EntityType<?> type = BeeInfoUtils.getEntityType(id);
            if (type == EntityType.PIG) bees.remove(s);
        });
    }

    public CompoundTag saveToNBT(CompoundTag nbt) {
        CompoundTag inv = this.getTileStackHandler().serializeNBT();
        nbt.put(NBTConstants.NBT_INVENTORY, inv);
        nbt.put(NBTConstants.NBT_BEES, this.writeBees());
        nbt.putBoolean(NBTConstants.NBT_VALID_APIARY, isValidApiary);
        nbt.putInt(NBTConstants.NBT_VERT_OFFSET, getVerticalOffset());
        nbt.putInt(NBTConstants.NBT_HOR_OFFSET, getHorizontalOffset());
        if (getStoragePos() != null)
            nbt.put(NBTConstants.NBT_STORAGE_POS, NbtUtils.writeBlockPos(getStoragePos()));
        if (getBreederPos() != null)
            nbt.put(NBTConstants.NBT_BREEDER_POS, NbtUtils.writeBlockPos(getBreederPos()));
        nbt.putInt(NBTConstants.NBT_TIER, getTier());
        return nbt;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return super.getUpdatePacket();
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(@NotNull BlockState state, CompoundTag tag) {
        this.load(state, tag);
    }
    //endregion

    //region IMPORT/EXPORT
    public void importBee(ServerPlayer player) {
        Level world = this.level;
        boolean imported = false;

        if (world != null && !this.getTileStackHandler().getStackInSlot(IMPORT).isEmpty() && this.getTileStackHandler().getStackInSlot(EMPTY_JAR).getCount() < 16) {
            ItemStack filledJar = this.getTileStackHandler().getStackInSlot(IMPORT);
            ItemStack emptyJar = this.getTileStackHandler().getStackInSlot(EMPTY_JAR);

            if (filledJar.getItem() instanceof BeeJar) {
                BeeJar jarItem = (BeeJar) filledJar.getItem();
                Entity entity = jarItem.getEntityFromStack(filledJar, world, true);

                if (entity instanceof Bee) {
                    Bee beeEntity = (Bee) entity;
                    imported = tryEnterHive(beeEntity, beeEntity.hasNectar(), true);

                    if (imported) {
                        filledJar.shrink(1);
                        if (emptyJar.isEmpty()) {
                            this.getTileStackHandler().setStackInSlot(EMPTY_JAR, new ItemStack(jarItem));
                        } else {
                            emptyJar.grow(1);
                        }
                    }
                }
            }
        }

        player.displayClientMessage(new TranslatableComponent("gui.resourcefulbees.apiary.import." + imported), true);
    }

    public void exportBee(ServerPlayer player, String beeType) {
        boolean exported = false;
        ApiaryBee bee = this.bees.get(beeType);

        if (bee.isLocked() && getTileStackHandler().getStackInSlot(EXPORT).isEmpty() && !getTileStackHandler().getStackInSlot(EMPTY_JAR).isEmpty()) {
            exported = releaseBee(this.getBlockState(), bee, true);
        }
        if (exported) {
            this.bees.remove(beeType);
            this.getTileStackHandler().getStackInSlot(EMPTY_JAR).shrink(1);
            if (this.getNumPlayersUsing() > 0 && this.level != null && !this.level.isClientSide)
                syncApiaryToPlayersUsing(this.level, this.getBlockPos(), this.saveToNBT(new CompoundTag()));
        }

        player.displayClientMessage(new TranslatableComponent("gui.resourcefulbees.apiary.export." + exported), true);
    }

    public void exportBee(Bee beeEntity) {
        ItemStack beeJar = new ItemStack(ModItems.BEE_JAR.get());
        beeJar.setTag(BeeInfoUtils.createJarBeeTag(beeEntity, NBTConstants.NBT_ENTITY));
        BeeJar.renameJar(beeJar, beeEntity);
        this.getTileStackHandler().setStackInSlot(EXPORT, beeJar);
    }
    //endregion

    //region STRUCTURE VALIDATION
    public void runStructureValidation(@Nullable ServerPlayer validatingPlayer) {
        if (this.level != null && !this.level.isClientSide()) {
            if (!this.isValidApiary || structureBlocks.isEmpty())
                buildStructureBlockList();
            this.isValidApiary = validateStructure(this.level, validatingPlayer);
            this.level.setBlockAndUpdate(this.getBlockPos(), getBlockState().setValue(ApiaryBlock.VALIDATED, this.isValidApiary));
            if (validatingPlayer != null && this.isValidApiary) {
                NetworkHooks.openGui(validatingPlayer, this, this.getBlockPos());
            }
            this.ticksSinceValidation = 0;
        }
    }

    public boolean validateStructure(Level worldIn, @Nullable ServerPlayer validatingPlayer) {
        AtomicBoolean isStructureValid = new AtomicBoolean(true);
        this.apiaryStorage = getApiaryStorage();
        this.apiaryBreeder = getApiaryBreeder();
        validateLinks();
        isStructureValid.set(validateBlocks(isStructureValid, worldIn, validatingPlayer));

        if (apiaryStorage == null) {
            isStructureValid.set(false);
            if (validatingPlayer != null) {
                validatingPlayer.displayClientMessage(new TextComponent("Missing Apiary Storage Block!"), false);
            }
        }

        if (validatingPlayer != null) {
            validatingPlayer.displayClientMessage(new TranslatableComponent("gui.resourcefulbees.apiary.validated." + isStructureValid.get()), true);
        }
        return isStructureValid.get();
    }

    private boolean validateBlocks(AtomicBoolean isStructureValid, Level worldIn, @Nullable ServerPlayer validatingPlayer) {
        structureBlocks.forEach(pos -> {
            Block block = worldIn.getBlockState(pos).getBlock();
            if (block.is(BeeInfoUtils.getValidApiaryTag()) || block instanceof ApiaryBreederBlock || block instanceof ApiaryStorageBlock || block instanceof ApiaryBlock) {
                BlockEntity tile = worldIn.getBlockEntity(pos);
                linkStorageAndBreeder(tile);
            } else {
                isStructureValid.set(false);
                if (validatingPlayer != null)
                    validatingPlayer.displayClientMessage(new TextComponent(String.format("Block at position (X: %1$s Y: %2$s Z: %3$s) is invalid!", pos.getX(), pos.getY(), pos.getZ())), false);
            }
        });

        return isStructureValid.get();
    }


    public BoundingBox buildStructureBounds(int horizontalOffset, int verticalOffset) {
        return MultiBlockHelper.buildStructureBounds(this.getBlockPos(), 7, 6, 7, -horizontalOffset - 3, -verticalOffset - 2, 0, this.getBlockState().getValue(ApiaryBlock.FACING));
    }

    private void buildStructureBlockList() {
        if (this.level != null) {
            BoundingBox box = buildStructureBounds(this.getHorizontalOffset(), this.getVerticalOffset());
            structureBlocks.clear();
            BlockPos.betweenClosedStream(box).forEach((blockPos -> {
                if (blockPos.getX() == box.x0 || blockPos.getX() == box.x1 ||
                        blockPos.getY() == box.y0 || blockPos.getY() == box.y1 ||
                        blockPos.getZ() == box.z0 || blockPos.getZ() == box.z1) {
                    BlockPos savedPos = new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    structureBlocks.add(savedPos);
                }
            }));
        }
    }

    public void runCreativeBuild(ServerPlayer player) {
        if (this.level != null) {
            buildStructureBlockList();
            boolean addedStorage = false;
            for (BlockPos pos : structureBlocks) {
                Block block = this.level.getBlockState(pos).getBlock();
                if (!(block instanceof ApiaryBlock)) {
                    if (addedStorage) {
                        this.level.setBlockAndUpdate(pos, Blocks.GLASS.defaultBlockState());
                    } else {
                        this.level.setBlockAndUpdate(pos, ModBlocks.APIARY_STORAGE_BLOCK.get().defaultBlockState());
                        addedStorage = true;
                    }
                }
            }
            runStructureValidation(player);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean linkStorageAndBreeder(BlockEntity tile) {
        if (tile instanceof ApiaryStorageTileEntity && apiaryStorage == null && ((ApiaryStorageTileEntity) tile).getApiaryPos() == null) {
            apiaryStorage = (ApiaryStorageTileEntity) tile;
            setStoragePos(apiaryStorage.getBlockPos());
            apiaryStorage.setApiaryPos(this.worldPosition);
            if (level != null) {
                level.sendBlockUpdated(getStoragePos(), apiaryStorage.getBlockState(), apiaryStorage.getBlockState(), 2);
            }
            return true;
        }

        if (tile instanceof ApiaryBreederTileEntity && apiaryBreeder == null && ((ApiaryBreederTileEntity) tile).getApiaryPos() == null) {
            apiaryBreeder = (ApiaryBreederTileEntity) tile;
            setBreederPos(apiaryBreeder.getBlockPos());
            apiaryBreeder.setApiaryPos(this.worldPosition);
            if (level != null) {
                level.sendBlockUpdated(getBreederPos(), apiaryBreeder.getBlockState(), apiaryBreeder.getBlockState(), 2);
            }
            return true;
        }

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean validateLinks() {
        boolean linksValid = true;
        if (apiaryStorage != null && (apiaryStorage.getApiaryPos() == null || positionsMismatch(apiaryStorage.getApiaryPos()))) {
            apiaryStorage = null;
            setStoragePos(null);
            linksValid = false;
        }
        if (apiaryBreeder != null && (apiaryBreeder.getApiaryPos() == null || positionsMismatch(apiaryBreeder.getApiaryPos()))) {
            apiaryBreeder = null;
            setBreederPos(null);
            linksValid = false;
        }
        if (!linksValid) {
            setChanged();
        }
        return linksValid;
    }

    private boolean positionsMismatch(BlockPos pos) {
        return pos.compareTo(this.worldPosition) != 0;
    }
    //endregion

    //region SCREEN HANDLING
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
        if (level != null) {
            setNumPlayersUsing(getNumPlayersUsing() + 1);
            if (isValidApiary(true)) {
                return new ValidatedApiaryContainer(i, level, worldPosition, playerInventory);
            }
            return new UnvalidatedApiaryContainer(i, level, worldPosition, playerInventory);
        }
        return null;
    }

    @Override
    public void switchTab(ServerPlayer player, ApiaryTabs tab) {
        if (level != null) {
            if (tab == ApiaryTabs.STORAGE) {
                NetworkHooks.openGui(player, getApiaryStorage(), getStoragePos());
            }
            if (tab == ApiaryTabs.BREED) {
                NetworkHooks.openGui(player, getApiaryBreeder(), getBreederPos());
            }
        }
    }

    //endregion

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? getLazyOptional().cast() :
                super.getCapability(cap, side);
    }

    public AutomationSensitiveItemStackHandler.IAcceptor getAcceptor() {
        return (slot, stack, automation) -> !automation || slot == 0 || slot == 1;
    }

    public AutomationSensitiveItemStackHandler.IRemover getRemover() {
        return (slot, automation) -> !automation || slot == 1 || slot == 2;
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("gui.resourcefulbees.apiary");
    }

    public boolean isPreviewed() {
        return previewed;
    }

    public void setPreviewed(boolean previewed) {
        this.previewed = previewed;
    }

    public @NotNull TileStackHandler getTileStackHandler() {
        return tileStackHandler;
    }

    public LazyOptional<IItemHandler> getLazyOptional() {
        return lazyOptional;
    }

    public int getHorizontalOffset() {
        return horizontalOffset;
    }

    public void setHorizontalOffset(int horizontalOffset) {
        this.horizontalOffset = horizontalOffset;
    }

    public int getVerticalOffset() {
        return verticalOffset;
    }

    public void setVerticalOffset(int verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

    public int getNumPlayersUsing() {
        return numPlayersUsing;
    }

    public void setNumPlayersUsing(int numPlayersUsing) {
        this.numPlayersUsing = numPlayersUsing;
    }

    public BlockPos getStoragePos() {
        return storagePos;
    }

    public void setStoragePos(BlockPos storagePos) {
        this.storagePos = storagePos;
    }

    public BlockPos getBreederPos() {
        return breederPos;
    }

    public void setBreederPos(BlockPos breederPos) {
        this.breederPos = breederPos;
    }

    public enum State {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY
    }

    public static class ApiaryBee {
        public final CompoundTag entityData;
        public final int minOccupationTicks;
        public final String beeType;
        private int ticksInHive;
        private boolean isLocked = false;
        public final String beeColor;
        public final Component displayName;

        public ApiaryBee(CompoundTag nbt, int ticksInHive, int minOccupationTicks, String beeType, String beeColor, Component displayName) {
            nbt.remove("UUID");
            this.entityData = nbt;
            this.setTicksInHive(ticksInHive);
            this.minOccupationTicks = minOccupationTicks;
            this.beeType = beeType;
            this.beeColor = beeColor;
            this.displayName = displayName;
        }

        public int getTicksInHive() {
            return ticksInHive;
        }

        public void setTicksInHive(int ticksInHive) {
            this.ticksInHive = ticksInHive;
        }

        public boolean isLocked() {
            return isLocked;
        }

        public void setLocked(boolean locked) {
            isLocked = locked;
        }
    }

    public class TileStackHandler extends AutomationSensitiveItemStackHandler {

        protected TileStackHandler(int slots) {
            super(slots);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == IMPORT) {
                return stack.getItem() instanceof BeeJar && BeeJar.isFilled(stack);
            } else if (slot == EMPTY_JAR) {
                return stack.getItem() instanceof BeeJar && !BeeJar.isFilled(stack);
            } else {
                return false;
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == IMPORT) {
                return 1;
            }
            return super.getSlotLimit(slot);
        }

        @Override
        public AutomationSensitiveItemStackHandler.IAcceptor getAcceptor() {
            return ApiaryTileEntity.this.getAcceptor();
        }

        @Override
        public AutomationSensitiveItemStackHandler.IRemover getRemover() {
            return ApiaryTileEntity.this.getRemover();
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }
}
