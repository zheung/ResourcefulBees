package com.resourcefulbees.resourcefulbees.container;

import com.resourcefulbees.resourcefulbees.registry.ModContainers;
import com.resourcefulbees.resourcefulbees.tileentity.MechanicalCentrifugeTileEntity;
import com.resourcefulbees.resourcefulbees.utils.FunctionalIntReferenceHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class MechanicalCentrifugeContainer extends Container {

    private final MechanicalCentrifugeTileEntity centrifugeTileEntity;
    private final PlayerEntity player;

    public MechanicalCentrifugeContainer(int id, World world, BlockPos pos, PlayerInventory inv) {
        super(ModContainers.MECHANICAL_CENTRIFUGE_CONTAINER.get(), id);

        this.player = inv.player;

        centrifugeTileEntity = (MechanicalCentrifugeTileEntity) world.getBlockEntity(pos);

        this.addDataSlot(new FunctionalIntReferenceHolder(() -> getCentrifugeTileEntity().getClicks(), v -> getCentrifugeTileEntity().setClicks(v)));

        this.addSlot(new SlotItemHandlerUnconditioned(getCentrifugeTileEntity().getItemStackHandler(), MechanicalCentrifugeTileEntity.HONEYCOMB_SLOT, 30, 20){

            @Override
            public boolean mayPlace(ItemStack stack){
                return !stack.getItem().equals(Items.GLASS_BOTTLE);
            }
        });
        this.addSlot(new SlotItemHandlerUnconditioned(getCentrifugeTileEntity().getItemStackHandler(), MechanicalCentrifugeTileEntity.BOTTLE_SLOT, 30, 38){

            @Override
            public boolean mayPlace(ItemStack stack){
                return stack.getItem().equals(Items.GLASS_BOTTLE);
            }
        });
        this.addSlot(new OutputSlot(getCentrifugeTileEntity().getItemStackHandler(), MechanicalCentrifugeTileEntity.HONEY_BOTTLE, 80, 59));
        this.addSlot(new OutputSlot(getCentrifugeTileEntity().getItemStackHandler(), MechanicalCentrifugeTileEntity.OUTPUT1, 129, 20));
        this.addSlot(new OutputSlot(getCentrifugeTileEntity().getItemStackHandler(), MechanicalCentrifugeTileEntity.OUTPUT2, 129, 38));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
        }
    }

    /**
     * Determines whether supplied player can use this container
     *
     * @param player the player
     */
    @Override
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return true;
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index <= 4) {
                if (!this.moveItemStackTo(itemstack1, 5, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    public MechanicalCentrifugeTileEntity getCentrifugeTileEntity() {
        return centrifugeTileEntity;
    }

    public PlayerEntity getPlayer() {
        return player;
    }
}