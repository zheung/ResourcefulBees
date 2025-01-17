package com.teamresourceful.resourcefulbees.entity.passive;

import com.google.gson.JsonObject;
import com.teamresourceful.resourcefulbees.api.ICustomBee;
import com.teamresourceful.resourcefulbees.api.beedata.*;
import com.teamresourceful.resourcefulbees.config.Config;
import com.teamresourceful.resourcefulbees.lib.NBTConstants;
import com.teamresourceful.resourcefulbees.mixin.AnimalEntityAccessor;
import com.teamresourceful.resourcefulbees.registry.BeeRegistry;
import com.teamresourceful.resourcefulbees.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;

public class CustomBeeEntity extends ModBeeEntity implements ICustomBee {

    private static final EntityDataAccessor<Integer> FEED_COUNT = SynchedEntityData.defineId(CustomBeeEntity.class, EntityDataSerializers.INT);

    protected final JsonObject rawBeeData;
    protected final CustomBeeData customBeeData;
    protected final String beeType;
    protected int timeWithoutHive;
    protected int flowerID;
    private BlockPos lastFlower;
    private boolean hasHiveInRange;
    private int disruptorInRange;

    public CustomBeeEntity(EntityType<? extends Bee> type, Level world, String beeType, JsonObject beeData) {
        super(type, world);
        this.rawBeeData = beeData;
        this.beeType = beeType;
        this.customBeeData = BeeRegistry.getRegistry().getBeeData(beeType);
    }

    public static AttributeSupplier.Builder createBeeAttributes(String key) {
        CombatData combatData = BeeRegistry.getRegistry().getBeeData(key).getCombatData();
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, combatData.getBaseHealth())
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, combatData.getAttackDamage())
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, combatData.getArmor())
                .add(Attributes.ARMOR_TOUGHNESS, combatData.getArmorToughness())
                .add(Attributes.ATTACK_KNOCKBACK, combatData.getKnockback());
    }

    //region BEE INFO RELATED METHODS BELOW

    public String getBeeType() {
        return beeType;
    }

    public BlockPos getLastFlower() {
        return lastFlower;
    }

    public void setLastFlower(BlockPos lastFlower) {
        this.lastFlower = lastFlower;
    }

    public JsonObject getRawBeeData() {
        return rawBeeData;
    }

    public CoreData getCoreData() {
        return customBeeData.getCoreData();
    }

    public HoneycombData getHoneycombData() {
        return customBeeData.getHoneycombData();
    }

    public RenderData getRenderData() {
        return customBeeData.getRenderData();
    }

    public BreedData getBreedData() {
        return customBeeData.getBreedData();
    }

    public CentrifugeData getCentrifugeData() {
        return customBeeData.getCentrifugeData();
    }

    public CombatData getCombatData() {
        return customBeeData.getCombatData();
    }

    public MutationData getMutationData() {
        return customBeeData.getMutationData();
    }

    public SpawnData getSpawnData() {
        return customBeeData.getSpawnData();
    }

    public TraitData getTraitData() {
        return customBeeData.getTraitData();
    }

    public int getFlowerEntityID() {
        return flowerID;
    }

    public void setFlowerEntityID(int id) {
        flowerID = id;
    }

    @Override
    public int getFeedCount() {
        return this.entityData.get(FEED_COUNT);
    }

    @Override
    public void resetFeedCount() {
        this.entityData.set(FEED_COUNT, 0);
    }

    @Override
    public void addFeedCount() {
        this.entityData.set(FEED_COUNT, this.getFeedCount() + 1);
    }
    //endregion

    //region CUSTOM BEE RELATED METHODS BELOW

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        TraitData info = getTraitData();
        if (hasEffect(MobEffects.WATER_BREATHING) && source == DamageSource.DROWN) {
            return true;
        }
        if (source.equals(DamageSource.SWEET_BERRY_BUSH)) {
            return true;
        }
        if (info.hasTraits() && info.hasDamageImmunities()) {
            return info.getDamageImmunities().stream().anyMatch(source.msgId::equalsIgnoreCase);
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance effectInstance) {
        TraitData info = getTraitData();
        if (info.hasTraits() && info.hasPotionImmunities()) {
            MobEffect potionEffect = effectInstance.getEffect();
            return info.getPotionImmunities().stream().noneMatch(potionEffect::equals);
        }
        return super.canBeAffected(effectInstance);
    }

    @Override
    public void aiStep() {
        if (this.level.isClientSide) {
            if (this.tickCount % 40 == 0) {
                TraitData info = getTraitData();
                if (info.hasTraits() && info.hasParticleEffects()) {
                    info.getParticleEffects().forEach(basicParticleType -> {
                        for (int i = 0; i < 10; ++i) {
                            this.level.addParticle((ParticleOptions) basicParticleType, this.getRandomX(0.5D),
                                    this.getRandomY() - 0.25D, this.getRandomZ(0.5D),
                                    (this.random.nextDouble() - 0.5D) * 2.0D, -this.random.nextDouble(),
                                    (this.random.nextDouble() - 0.5D) * 2.0D);
                        }
                    });
                }
            }
        } else {
            if (Config.BEES_DIE_IN_VOID.get() && this.position().y <= 0) {
                this.remove();
            }
            if (!hasCustomName() && this.tickCount % 100 == 0) {
                if (hasHiveInRange() || hasSavedFlowerPos() || isPassenger() || isPersistenceRequired() || isLeashed() || hasNectar() || disruptorInRange > 0 || isBaby()) {
                    timeWithoutHive = 0;
                } else {
                    timeWithoutHive += 100;
                    if (timeWithoutHive >= 12000) this.remove();
                }
                hasHiveInRange = false;
            }
            if (this.tickCount % 100 == 0) {
                disruptorInRange--;
                if (disruptorInRange < 0) disruptorInRange = 0;
            }
        }
        super.aiStep();
    }

    public boolean hasHiveInRange() {
        return hasHiveInRange;
    }

    public void setHasHiveInRange(boolean hasHiveInRange) {
        this.hasHiveInRange = hasHiveInRange;
    }

    public static boolean canBeeSpawn(EntityType<? extends AgableMob> typeIn, LevelReader worldIn, MobSpawnType reason, BlockPos pos, Random randomIn) {
        String namespaceID = EntityType.getKey(typeIn).toString();
        String beeType = namespaceID.substring(namespaceID.lastIndexOf(":") + 1, namespaceID.length() - 4);
        SpawnData spawnData = BeeRegistry.getRegistry().getBeeData(beeType).getSpawnData();

        switch (reason) {
            case NATURAL:
            case CHUNK_GENERATION:
                if (spawnData.canSpawnInWorld()) {
                    if (!MathUtils.inRangeInclusive(pos.getY(), spawnData.getMinYLevel(), spawnData.getMaxYLevel())) {
                        return false;
                    }
                    switch (spawnData.getLightLevel()) {
                        case DAY:
                            return worldIn.getMaxLocalRawBrightness(pos) >= 8;
                        case NIGHT:
                            return worldIn.getMaxLocalRawBrightness(pos) <= 7;
                        case ANY:
                        default:
                            return true;
                    }
                }
                break;
            default:
                return true;
        }

        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FEED_COUNT, 0);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(FEED_COUNT, compound.getInt(NBTConstants.NBT_FEED_COUNT));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString(NBTConstants.NBT_BEE_TYPE, this.getBeeType());
        compound.putInt(NBTConstants.NBT_FEED_COUNT, this.getFeedCount());
    }

    public AgableMob createSelectedChild(CustomBeeData customBeeData) {
        EntityType<?> entityType = Objects.requireNonNull(customBeeData.getEntityType());
        Entity entity = entityType.create(level);
        return (AgableMob) entity;
    }

    //This is because we don't want IF being able to breed our animals
    @Override
    public void setInLove(@Nullable Player player) {
        if (player != null && !(player instanceof FakePlayer))
            super.setInLove(player);
    }

    @Override
    public void setInLoveTime(int time) {
        //This is overridden bc Botania breeds animals regardless of breeding rules
        // See the method below ( setLove() ) as alternative
        // super.setInLove(time);
    }

    public void setLoveTime(int time) {
        ((AnimalEntityAccessor) this).setLove(time);
    }

    @Override
    public void resetLove() {
        super.resetLove();
        resetFeedCount();
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return this.getBreedData().getFeedItems().contains(stack.getItem());
    }

    @NotNull
    @Override
    public InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (item instanceof NameTagItem) {
            super.mobInteract(player, hand);
        }
        if (this.isFood(itemstack)) {
            if (!this.level.isClientSide && this.getAge() == 0 && !this.isInLove()) {
                this.usePlayerItem(player, itemstack);
                player.addItem(new ItemStack(getBreedData().getFeedReturnItem().orElse(Items.AIR)));
                this.addFeedCount();
                if (this.getFeedCount() >= getBreedData().getFeedAmount()) {
                    this.setInLove(player);
                }
                player.swing(hand, true);
                return InteractionResult.PASS;
            }

            if (this.isBaby()) {
                this.usePlayerItem(player, itemstack);
                this.ageUp((int) ((-this.getAge() / 20D) * 0.1F), true);
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby()) {
            BlockPos pos = this.blockPosition();
            this.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public void setHasDisruptorInRange() {
        disruptorInRange += 2;
        if (disruptorInRange > 10) disruptorInRange = 10;
    }

    public boolean getDisruptorInRange() {
        return disruptorInRange > 0;
    }
    //endregion
}