package com.teamresourceful.resourcefulbees.api.beedata;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamresourceful.resourcefulbees.ResourcefulBees;
import com.teamresourceful.resourcefulbees.registry.BeeRegistry;
import com.teamresourceful.resourcefulbees.utils.BeeInfoUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.function.Consumer;


public class CustomBeeData {

    public static Codec<CustomBeeData> codec(String name) {
        return RecordCodecBuilder.create(instance -> instance.group(
                CoreData.codec(name).fieldOf("CoreData").orElseGet((Consumer<String>) s -> ResourcefulBees.LOGGER.error("CoreData is REQUIRED!"), null).forGetter(CustomBeeData::getCoreData),
                HoneycombData.CODEC.fieldOf("HoneycombData").orElse(HoneycombData.DEFAULT).forGetter(CustomBeeData::getHoneycombData),
                RenderData.CODEC.fieldOf("RenderData").orElseGet((Consumer<String>) s -> ResourcefulBees.LOGGER.error("RenderData is REQUIRED!"), null).forGetter(CustomBeeData::getRenderData),
                BreedData.CODEC.fieldOf("BreedData").orElse(BreedData.createDefault()).forGetter(CustomBeeData::getBreedData),
                CentrifugeData.CODEC.fieldOf("CentrifugeData").orElse(CentrifugeData.createDefault()).forGetter(CustomBeeData::getCentrifugeData),
                CombatData.CODEC.fieldOf("CombatData").orElse(CombatData.createDefault()).forGetter(CustomBeeData::getCombatData),
                MutationData.CODEC.fieldOf("MutationData").orElse(MutationData.createDefault()).forGetter(CustomBeeData::getMutationData),
                SpawnData.CODEC.fieldOf("SpawnData").orElse(SpawnData.createDefault()).forGetter(CustomBeeData::getSpawnData),
                TraitData.codec(name).fieldOf("TraitData").orElse(TraitData.createDefault()).forGetter(CustomBeeData::getTraitData)
        ).apply(instance, CustomBeeData::new));
    }

    private final CoreData coreData;
    private final HoneycombData honeycombData;
    private final RenderData renderData;
    private final BreedData breedData;
    private final CentrifugeData centrifugeData;
    private final CombatData combatData;
    private final MutationData mutationData;
    private final SpawnData spawnData;
    private final TraitData traitData;
    private final ResourceLocation registryID;

    public CustomBeeData(CoreData coreData, HoneycombData honeycombData, RenderData renderData, BreedData breedData, CentrifugeData centrifugeData, CombatData combatData, MutationData mutationData, SpawnData spawnData, TraitData traitData) {
        this.coreData = coreData;
        this.honeycombData = honeycombData;
        this.renderData = renderData;
        this.breedData = breedData;
        this.centrifugeData = centrifugeData;
        this.combatData = combatData;
        this.mutationData = mutationData;
        this.spawnData = spawnData;
        this.traitData = traitData;
        this.registryID = new ResourceLocation(ResourcefulBees.MOD_ID + ":" + coreData.getName() + "_bee");
    }

    public CoreData getCoreData() {
        return coreData;
    }

    public HoneycombData getHoneycombData() {
        return honeycombData;
    }

    public RenderData getRenderData() {
        return renderData;
    }

    public BreedData getBreedData() {
        return breedData;
    }

    public CentrifugeData getCentrifugeData() {
        return centrifugeData;
    }

    public CombatData getCombatData() {
        return combatData;
    }

    public MutationData getMutationData() {
        return mutationData;
    }

    public SpawnData getSpawnData() {
        return spawnData;
    }

    public TraitData getTraitData() {
        return traitData;
    }

    public EntityType<?> getEntityType() {
        return BeeInfoUtils.getEntityType(registryID);
    }

    public ResourceLocation getRegistryID() {
        return registryID;
    }

    public JsonObject getRawData() {
        return BeeRegistry.getRegistry().getRawBeeData(coreData.getName());
    }

    public TranslatableComponent getTranslation() {
        return new TranslatableComponent("entity.resourcefulbees." + coreData.getName() + "_bee");
    }
}
