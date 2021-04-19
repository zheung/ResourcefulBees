package com.resourcefulbees.resourcefulbees.item;

import com.resourcefulbees.resourcefulbees.api.beedata.ColorData;
import com.resourcefulbees.resourcefulbees.config.Config;
import com.resourcefulbees.resourcefulbees.utils.color.RainbowColor;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class HoneycombItem extends Item {

    private final ColorData colorData;
    private final String beeType;

    public HoneycombItem(String beeType, ColorData colorData, Item.Properties properties) {
        super(properties);
        this.colorData = colorData;
        this.beeType = beeType;
    }

    @SuppressWarnings("unusedParameter")
    public static int getColor(ItemStack stack, int tintIndex) {
        HoneycombItem honeycombItem = (HoneycombItem) stack.getItem();
        return honeycombItem.colorData.isRainbowBee() ? RainbowColor.getRGB() : honeycombItem.getHoneycombColor();
    }

    public int getHoneycombColor() { return colorData.getHoneycombColorInt(); }

    public String getBeeType() { return beeType; }

    @Override
    public boolean isEdible() {
        return true;
    }

    @Nullable
    @Override
    public FoodProperties getFoodProperties() {
        return new FoodProperties.Builder()
                .nutrition(Config.HONEYCOMB_HUNGER.get())
                .saturationMod(Config.HONEYCOMB_SATURATION.get().floatValue())
                .fast()
                .build();
    }
}
