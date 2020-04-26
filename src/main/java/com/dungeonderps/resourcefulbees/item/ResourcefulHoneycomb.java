package com.dungeonderps.resourcefulbees.item;

import com.dungeonderps.resourcefulbees.ResourcefulBees;
import com.dungeonderps.resourcefulbees.utils.Color;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public class ResourcefulHoneycomb extends Item {

    public ResourcefulHoneycomb() {
        super(new Properties().group(ItemGroup.MATERIALS));
    }

    //TODO Consider using two separate textures for honeycomb
    // -One texture for outline
    // -Another texture to be colored.
    public static int getColor(ItemStack stack, int tintIndex){

        CompoundNBT honeycombNBT = stack.getChildTag("ResourcefulBees");
        return honeycombNBT != null && honeycombNBT.contains("Color") ? Color.parseInt(honeycombNBT.getString("Color")) : 0x000000;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        CompoundNBT beeType = stack.getChildTag("ResourcefulBees");
        String name;
        if ((beeType != null && beeType.contains("BeeType"))) {
            name = "item" + '.' + ResourcefulBees.MOD_ID + '.' + beeType.getString("BeeType").toLowerCase() + "_honeycomb";
        } else {
            name = "item" + '.' + ResourcefulBees.MOD_ID + '.' + "resourceful_honeycomb";
        }
        return name;
    }
}