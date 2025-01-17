package com.teamresourceful.resourcefulbees.registry;

import com.google.gson.JsonObject;
import com.teamresourceful.resourcefulbees.api.IHoneyRegistry;
import com.teamresourceful.resourcefulbees.api.honeydata.HoneyBottleData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HoneyRegistry implements IHoneyRegistry {

    private final Map<String, JsonObject> rawHoneyData = new LinkedHashMap<>(); //MOVE THIS TO HONEY REGISTRY - DOES NOT BELONG HERE
    private final Map<String, HoneyBottleData> honeyInfo = new LinkedHashMap<>(); //MOVE THIS TO HONEY REGISTRY - DOES NOT BELONG HERE

    private static final HoneyRegistry INSTANCE = new HoneyRegistry();

    /**
     * Return the instance of this class. This is useful for calling methods to the mod from a static or threaded context.
     *
     * @return Instance of this class
     */
    public static HoneyRegistry getRegistry() {
        return INSTANCE;
    }

    /**
     * Returns a HoneyBottleData object for the given honey type.
     *
     * @param honey Honey type for which HoneyData is requested.
     * @return Returns a HoneyBottleData object for the given bee type.
     */
    public HoneyBottleData getHoneyData(String honey) {
        return honeyInfo.get(honey);
    }

    public void cacheRawHoneyData(String name, JsonObject jsonObject) {
        rawHoneyData.computeIfAbsent(name, s -> jsonObject);
    }

    /**
     * Returns an unmodifiable copy of the Honey Registry.
     * This is useful for iterating over all honey without worry of changing data
     *
     * @return Returns unmodifiable copy of honey registry.
     */
    public Map<String, HoneyBottleData> getHoneyBottles() {
        return Collections.unmodifiableMap(honeyInfo);
    }

    /**
     * Registers the supplied Honey Type and associated data to the mod.
     * If the bee already exists in the registry the method will return false.
     *
     * @param honeyType Honey Type of the honey being registered.
     * @param honeyData HoneyData of the honey being registered
     * @return Returns false if bee already exists in the registry.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean registerHoney(String honeyType, HoneyBottleData honeyData) {
        honeyInfo.putIfAbsent(honeyType, honeyData);
        return true;
    }

    public Map<String, JsonObject> getRawHoney() {
        return rawHoneyData;
    }
}
