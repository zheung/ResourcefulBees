package com.resourcefulbees.resourcefulbees.config;

import com.resourcefulbees.resourcefulbees.data.BeeData;
import com.resourcefulbees.resourcefulbees.lib.BeeConstants;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import com.resourcefulbees.resourcefulbees.utils.MathUtils;
import com.resourcefulbees.resourcefulbees.utils.RandomCollection;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class BeeInfo {

    public static final LinkedHashMap<String, BeeData> BEE_INFO = new LinkedHashMap<>();
    public static final HashMap<Biome, RandomCollection<String>> SPAWNABLE_BIOMES = new HashMap<>();
    public static final HashMap<Pair<String, String>, RandomCollection<String>> FAMILY_TREE = new HashMap<>();

    public static float[] getBeeColorAsFloat(String color){
        java.awt.Color tempColor = java.awt.Color.decode(color);
        return tempColor.getComponents(null);
    }

    /**
     * Returns a random bee from the BEE_INFO hashmap.
     * This is used for selecting a bee from all possible bees.
     *
     *  @return Returns random bee type as a string.
     */
    public static String getRandomBee(){
        int randInt = MathUtils.nextInt(BEE_INFO.size() - 1) +1;
        Object[] dataArray = BEE_INFO.keySet().toArray();
        Object key = dataArray[randInt];
        return BEE_INFO.get(key.toString()).getName();
    }

    /**
     * Returns a random bee from the SPAWNABLE_BIOMES hashmap.
     * This is used for in-world spawning based on biome.
     *
     *  @return Returns random bee type as a string.
     */
    public static String getRandomBee(Biome biome){
        if (SPAWNABLE_BIOMES.get(biome) != null) {
            return SPAWNABLE_BIOMES.get(biome).next();
        }
        return BeeConstants.DEFAULT_BEE_TYPE;
    }

    public static BeeData getInfo(String bee){
        BeeData info = BEE_INFO.get(bee);
        return info != null ? info : BEE_INFO.get(BeeConstants.DEFAULT_BEE_TYPE);
    }

    public static boolean canParentsBreed(String parent1, String parent2){
        return FAMILY_TREE.containsKey(BeeInfoUtils.sortParents(parent1, parent2));
    }

    public static String getWeightedChild(String parent1, String parent2){
        return FAMILY_TREE.get(BeeInfoUtils.sortParents(parent1, parent2)).next();
    }

    public static double getAdjustedWeightForChild(BeeData child){
        return FAMILY_TREE.get(BeeInfoUtils.sortParents(child.getParent1(), child.getParent2())).getAdjustedWeight(child.getBreedWeight());
    }
}