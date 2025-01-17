package com.teamresourceful.resourcefulbees.entity.passive;

import com.teamresourceful.resourcefulbees.api.honeydata.HoneyBottleData;
import com.teamresourceful.resourcefulbees.api.honeydata.HoneyEffect;
import com.teamresourceful.resourcefulbees.lib.ModConstants;
import com.teamresourceful.resourcefulbees.registry.ModBlocks;
import com.teamresourceful.resourcefulbees.registry.ModFluids;
import com.teamresourceful.resourcefulbees.registry.ModItems;
import com.teamresourceful.resourcefulbees.utils.color.Color;
import net.minecraft.world.effect.MobEffects;

import java.util.LinkedList;
import java.util.List;

public class KittenBee {

    private KittenBee() {
        throw new IllegalStateException(ModConstants.UTILITY_CLASS);
    }

    private static HoneyBottleData honeyBottleData = null;

    public static HoneyBottleData getHoneyBottleData() {
        if (honeyBottleData == null) {
            List<HoneyEffect> effects = new LinkedList<>();
            effects.add(new HoneyEffect(MobEffects.MOVEMENT_SPEED, 2400, 2, 1));
            effects.add(new HoneyEffect(MobEffects.JUMP, 2400, 1, 1));
            effects.add(new HoneyEffect(MobEffects.NIGHT_VISION, 2400, 0, 1));
            honeyBottleData = new HoneyBottleData(8, 0.9f, Color.parse("#BD5331"), false, false, false, effects).setName("cantnip");
            honeyBottleData.setHoneyBlockRegistryObject(ModBlocks.CATNIP_HONEY_BLOCK);
            honeyBottleData.setHoneyStillFluidRegistryObject(ModFluids.CATNIP_HONEY_STILL);
            honeyBottleData.setHoneyFlowingFluidRegistryObject(ModFluids.CATNIP_HONEY_FLOWING);
            honeyBottleData.setHoneyBlockItemRegistryObject(ModItems.CATNIP_HONEY_BLOCK_ITEM);
            honeyBottleData.setHoneyBucketItemRegistryObject(ModItems.CATNIP_HONEY_FLUID_BUCKET);
            honeyBottleData.setHoneyBottleRegistryObject(ModItems.CATNIP_HONEY_BOTTLE);
        }
        return honeyBottleData;
    }
}
