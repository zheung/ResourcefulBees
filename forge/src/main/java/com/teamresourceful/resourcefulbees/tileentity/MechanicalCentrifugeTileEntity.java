package com.teamresourceful.resourcefulbees.tileentity;

import com.google.gson.JsonElement;
import com.teamresourceful.resourcefulbees.api.beedata.outputs.ItemOutput;
import com.teamresourceful.resourcefulbees.block.MechanicalCentrifugeBlock;
import com.teamresourceful.resourcefulbees.container.AutomationSensitiveItemStackHandler;
import com.teamresourceful.resourcefulbees.container.MechanicalCentrifugeContainer;
import com.teamresourceful.resourcefulbees.lib.BeeConstants;
import com.teamresourceful.resourcefulbees.recipe.CentrifugeRecipe;
import com.teamresourceful.resourcefulbees.registry.ModBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MechanicalCentrifugeTileEntity extends BlockEntity implements TickableBlockEntity, MenuProvider {

    public static final int HONEYCOMB_SLOT = 0;
    public static final int BOTTLE_SLOT = 1;

    public static final int HONEY_BOTTLE = 2;
    public static final int OUTPUT1 = 3;
    public static final int OUTPUT2 = 4;

    private final AutomationSensitiveItemStackHandler itemStackHandler = new MechanicalCentrifugeTileEntity.TileStackHandler(5, getAcceptor(), getRemover());
    private final LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(this::getItemStackHandler);
    private final LazyOptional<IItemHandler> automationHandler = LazyOptional.of(() -> new AutomationSensitiveItemStackHandler(5, AutomationSensitiveItemStackHandler.ACCEPT_FALSE, AutomationSensitiveItemStackHandler.REMOVE_FALSE));
    private int clicks;
    private CentrifugeRecipe recipe;
    private ItemStack failedMatch = ItemStack.EMPTY;

    public MechanicalCentrifugeTileEntity() {
        super(ModBlockEntityTypes.MECHANICAL_CENTRIFUGE_ENTITY.get());
    }


    @Override
    public void tick() {
        if (level != null && !level.isClientSide) {
            boolean dirty = false;
            CentrifugeRecipe irecipe = getRecipe();
            if (!getItemStackHandler().getStackInSlot(HONEYCOMB_SLOT).isEmpty() && !getItemStackHandler().getStackInSlot(BOTTLE_SLOT).isEmpty()) {
                if (this.canProcess(irecipe)) {
                    if (this.getClicks() > 0)
                        level.setBlockAndUpdate(worldPosition,getBlockState().setValue(MechanicalCentrifugeBlock.PROPERTY_ON,true));
                    if (this.getClicks() >= 8) {
                        this.setClicks(0);
                        this.processItem(irecipe);
                        dirty = true;
                        level.setBlockAndUpdate(worldPosition,getBlockState().setValue(MechanicalCentrifugeBlock.PROPERTY_ON,false));
                    }
                }
            } else {
                setClicks(0);
                level.setBlockAndUpdate(worldPosition,getBlockState().setValue(MechanicalCentrifugeBlock.PROPERTY_ON,false));
            }
            if (dirty) {
                this.setChanged();
            }
        }
    }

    public boolean canProcess(@Nullable CentrifugeRecipe recipe) {
        if (recipe != null && !recipe.isMultiblock()) {
            List<ItemOutput> outputs = recipe.getItemOutputs();
            ItemStack glassBottle = getItemStackHandler().getStackInSlot(BOTTLE_SLOT);
            ItemStack combs = getItemStackHandler().getStackInSlot(HONEYCOMB_SLOT);
            JsonElement count = recipe.getIngredient().toJson().getAsJsonObject().get(BeeConstants.INGREDIENT_COUNT);
            int inputAmount = count !=null ? count.getAsInt() : 1;
            List<ItemStack> outputSlots = new ArrayList<>(
                    Arrays.asList(
                            getItemStackHandler().getStackInSlot(OUTPUT1),
                            getItemStackHandler().getStackInSlot(OUTPUT2),
                            getItemStackHandler().getStackInSlot(HONEY_BOTTLE)
                    )
            );
            int processScore = 0;
            if (outputSlots.get(0).isEmpty() && outputSlots.get(1).isEmpty() && outputSlots.get(2).isEmpty() && glassBottle.getItem() == Items.GLASS_BOTTLE && combs.getCount() >= inputAmount) return true;
            else {
                for(int i=0;i<3;i++) {
                    if (outputSlots.get(i).isEmpty() || (outputSlots.get(i).getItem() == outputs.get(i).getItem()
                            && outputSlots.get(i).getCount() + outputs.get(i).getCount() <= outputSlots.get(i).getMaxStackSize())) {
                        processScore++;
                    }
                }
                if (combs.getCount() >= inputAmount) {
                    processScore++;
                }
                if (processScore == 4 && glassBottle.getItem() == Items.GLASS_BOTTLE) {
                    return true;
                } else {
                    if (level != null) {
                        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(MechanicalCentrifugeBlock.PROPERTY_ON, false));
                    }
                    return false;
                }
            }
        }
        return false;
    }

    private void processItem(@Nullable CentrifugeRecipe recipe) {
        if (recipe != null && this.canProcess(recipe)) {
            JsonElement count = recipe.getIngredient().toJson().getAsJsonObject().get(BeeConstants.INGREDIENT_COUNT);
            int inputAmount = count !=null ? count.getAsInt() : 1;
            ItemStack comb = getItemStackHandler().getStackInSlot(HONEYCOMB_SLOT);
            ItemStack glassBottle = getItemStackHandler().getStackInSlot(BOTTLE_SLOT);
            List<Pair<ItemStack, Integer>> slots = new ArrayList<>(
                    Arrays.asList(
                            Pair.of(getItemStackHandler().getStackInSlot(OUTPUT1),OUTPUT1),
                            Pair.of(getItemStackHandler().getStackInSlot(OUTPUT2),OUTPUT2),
                            Pair.of(getItemStackHandler().getStackInSlot(HONEY_BOTTLE),HONEY_BOTTLE)
                    )
            );
            if (level != null)
                for(int i = 0; i < 3; i++){
                    ItemOutput output = recipe.getItemOutputs().get(i);
                    if (output.getChance() >= level.random.nextDouble()) {
                        if (slots.get(i).getLeft().isEmpty()) {
                            this.getItemStackHandler().setStackInSlot(slots.get(i).getRight(), output.getItemStack());
                        } else if (slots.get(i).getLeft().getItem() == output.getItem()) {
                            slots.get(i).getLeft().grow(output.getCount());
                        }
                        if (slots.get(i).getRight().equals(HONEY_BOTTLE)) glassBottle.shrink(1);
                    }
                }
            comb.shrink(inputAmount);
        }
        setClicks(0);
    }

    public CentrifugeRecipe getRecipe() {
        ItemStack input = getItemStackHandler().getStackInSlot(HONEYCOMB_SLOT);
        if (input.isEmpty() || input == getFailedMatch() || level == null) {
            return null;
        }
        if (recipe == null || !recipe.matches(new RecipeWrapper(getItemStackHandler()), level)) {
            CentrifugeRecipe rec = level.getRecipeManager().getRecipeFor(CentrifugeRecipe.CENTRIFUGE_RECIPE_TYPE, new RecipeWrapper(getItemStackHandler()), this.level).orElse(null);
            setFailedMatch(rec == null ? input : ItemStack.EMPTY);
            recipe = rec;
        }
        return recipe;
    }

    @Override
    public void load(@NotNull BlockState state, CompoundTag tag) {
        CompoundTag invTag = tag.getCompound("inv");
        getItemStackHandler().deserializeNBT(invTag);
        setClicks(tag.getInt("clicks"));
        super.load(state, tag);
    }

    @NotNull
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag inv = this.getItemStackHandler().serializeNBT();
        tag.put("inv", inv);
        tag.putInt("clicks", getClicks());
        return super.save(tag);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbtTagCompound = new CompoundTag();
        save(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(@NotNull BlockState state, CompoundTag tag) { this.load(state, tag); }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap.equals(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) return getAutomationHandler().cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        this.lazyOptional.invalidate();
    }

    public AutomationSensitiveItemStackHandler.IAcceptor getAcceptor() {
        return (slot, stack, automation) -> !automation && (slot == 0 || slot == 1);
    }

    public AutomationSensitiveItemStackHandler.IRemover getRemover() {
        return (slot, automation) -> !automation || slot == 2 || slot == 3 || slot == 4;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
        //noinspection ConstantConditions
        return new MechanicalCentrifugeContainer(id, level, worldPosition, playerInventory);
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("gui.resourcefulbees.mechanical_centrifuge");
    }

    public @NotNull AutomationSensitiveItemStackHandler getItemStackHandler() {
        return itemStackHandler;
    }

    public LazyOptional<IItemHandler> getAutomationHandler() {
        return automationHandler;
    }

    public int getClicks() {
        return clicks;
    }

    public void setClicks(int clicks) {
        this.clicks = clicks;
    }

    public void setRecipe(CentrifugeRecipe recipe) {
        this.recipe = recipe;
    }

    public ItemStack getFailedMatch() {
        return failedMatch;
    }

    public void setFailedMatch(ItemStack failedMatch) {
        this.failedMatch = failedMatch;
    }

    protected class TileStackHandler extends AutomationSensitiveItemStackHandler {
        protected TileStackHandler(int slots, IAcceptor acceptor, IRemover remover) {
            super(slots,acceptor,remover);
        }
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }
}
