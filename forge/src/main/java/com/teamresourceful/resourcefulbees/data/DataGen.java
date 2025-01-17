package com.teamresourceful.resourcefulbees.data;

import com.teamresourceful.resourcefulbees.ResourcefulBees;
import com.teamresourceful.resourcefulbees.api.IBeeRegistry;
import com.teamresourceful.resourcefulbees.api.beedata.CustomBeeData;
import com.teamresourceful.resourcefulbees.api.honeydata.HoneyBottleData;
import com.teamresourceful.resourcefulbees.config.Config;
import com.teamresourceful.resourcefulbees.init.BeeSetup;
import com.teamresourceful.resourcefulbees.lib.HoneycombTypes;
import com.teamresourceful.resourcefulbees.lib.ModConstants;
import com.teamresourceful.resourcefulbees.registry.BeeRegistry;
import com.teamresourceful.resourcefulbees.registry.HoneyRegistry;
import com.teamresourceful.resourcefulbees.registry.ModEntities;
import com.teamresourceful.resourcefulbees.registry.TraitRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.fml.RegistryObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.teamresourceful.resourcefulbees.ResourcefulBees.LOGGER;

@SuppressWarnings("deprecation")
public class DataGen {

    private DataGen() {
        throw new IllegalStateException(ModConstants.UTILITY_CLASS);
    }

    private static final IBeeRegistry BEE_REGISTRY = BeeRegistry.getRegistry();

    private static final String ITEM_RESOURCEFULBEES = "item.resourcefulbees.";

    private static final Map<ResourceLocation, Set<ResourceLocation>> TAGS = new HashMap<>();

    public static void generateClientData() {
        if (Config.GENERATE_ENGLISH_LANG.get().equals(Boolean.TRUE)) generateEnglishLang();
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> getTags() {
        return Collections.unmodifiableMap(TAGS);
    }

    public static void generateCommonData() {
        generateBeeTags();
        generateCombBlockItemTags();
        generateCombBlockTags();
        generateCombItemTags();

        //custom honey data
        generateHoneyBottleTags();
        if (Config.HONEY_GENERATE_BLOCKS.get().equals(Boolean.TRUE)) {
            generateHoneyBlockTags();
            generateHoneyBlockItemTags();
        }
        if (Config.HONEY_GENERATE_FLUIDS.get().equals(Boolean.TRUE)) {
            generateHoneyTags();
        }
    }

    private static void writeFile(String path, String file, String data) throws IOException {
        Files.createDirectories(Paths.get(path));
        try (FileWriter writer = new FileWriter(Paths.get(path, file).toFile())) {
            writer.write(data);
        } catch (IOException e) {
            LOGGER.error("context", e);
        }
    }

    private static void generateEnglishLang() {
        LOGGER.info("Generating English Lang...");
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        BEE_REGISTRY.getBees().forEach(((name, customBeeData) -> {
            String displayName = StringUtils.replace(name, "_", " ");
            displayName = WordUtils.capitalizeFully(displayName);

            //block
            generateLangEntry(builder, "block.resourcefulbees.", name, "_honeycomb_block", displayName, "Honeycomb Block");

            //comb
            generateLangEntry(builder, ITEM_RESOURCEFULBEES, name, "_honeycomb", displayName, "Honeycomb");

            //spawn egg
            generateLangEntry(builder, ITEM_RESOURCEFULBEES, name, "_bee_spawn_egg", displayName, "Bee Spawn Egg");

            //entity
            generateLangEntry(builder, "entity.resourcefulbees.", name, "_bee", displayName, "Bee");

        }));
        HoneyRegistry.getRegistry().getHoneyBottles().forEach((name, honeyData) -> {
            String displayName = StringUtils.replace(name, "_", " ");
            displayName = WordUtils.capitalizeFully(displayName);

            //honey bottle
            generateLangEntry(builder, ITEM_RESOURCEFULBEES, name, "_honey_bottle", displayName, "Honey Bottle");

            //honey block
            if (Boolean.TRUE.equals(Config.HONEY_GENERATE_BLOCKS.get()) && honeyData.doGenerateHoneyBlock()) {
                generateLangEntry(builder, "block.resourcefulbees.", name, "_honey_block", displayName, "Honey Block");
            }

            if (Boolean.TRUE.equals(Config.HONEY_GENERATE_FLUIDS.get()) && honeyData.doGenerateHoneyFluid()) {
                //honey bucket
                generateLangEntry(builder, ITEM_RESOURCEFULBEES, name, "_honey_fluid_bucket", displayName, "Honey Bucket");

                //honey fluid
                generateLangEntry(builder, "fluid.resourcefulbees.", name, "_honey", displayName, "Honey");
            }
        });
        TraitRegistry.getRegistry().getTraits().forEach((name, trait) -> {
            String displayName = StringUtils.replace(name, "_", " ");
            displayName = WordUtils.capitalizeFully(displayName);
            builder.append(String.format("\"%s\" : \"%s\",%n", trait.getTranslationKey(), displayName));
        });
        builder.deleteCharAt(builder.lastIndexOf(","));
        builder.append("}");

        String langPath = BeeSetup.getResourcePath().toString() + "/assets/resourcefulbees/lang/";
        String langFile = "en_us.json";
        try {
            writeFile(langPath, langFile, builder.toString());
            LOGGER.info("Language File Generated!");
        } catch (IOException e) {
            LOGGER.error("Could not generate language file!");
        }
    }

    private static void generateLangEntry(StringBuilder builder, String prefix, String name, String suffix, String displayName, String displaySuffix){
        builder.append("\"");
        builder.append(prefix);
        builder.append(name);
        builder.append(suffix);
        builder.append("\": \"");
        builder.append(displayName);
        builder.append(" ");
        builder.append(displaySuffix);
        builder.append("\",\n");
    }


    private static void generateCombItemTags() {
        TAGS.put(new ResourceLocation(ResourcefulBees.MOD_ID, "tags/items/resourceful_honeycomb.json"),
                BEE_REGISTRY.getBees().values().stream()
                        .map(CustomBeeData::getHoneycombData)
                        .filter(honeycombDataCodec -> honeycombDataCodec.getHoneycombType() == HoneycombTypes.DEFAULT)
                        .map(honeycombDataCodec -> honeycombDataCodec.getHoneycomb().getRegistryName()).collect(Collectors.toSet()));
    }

    private static void generateCombBlockItemTags() {
        TAGS.put(new ResourceLocation(ResourcefulBees.MOD_ID, "tags/items/resourceful_honeycomb_block.json"),
                BEE_REGISTRY.getBees().values().stream()
                        .map(CustomBeeData::getHoneycombData)
                        .filter(honeycombDataCodec -> honeycombDataCodec.getHoneycombType() == HoneycombTypes.DEFAULT)
                        .map(honeycombDataCodec -> honeycombDataCodec.getHoneycombBlock().getRegistryName()).collect(Collectors.toSet()));
    }

    private static void generateCombBlockTags() {
        TAGS.put(new ResourceLocation(ResourcefulBees.MOD_ID, "tags/blocks/resourceful_honeycomb_block.json"),
                BEE_REGISTRY.getBees().values().stream()
                        .map(CustomBeeData::getHoneycombData)
                        .filter(honeycombDataCodec -> honeycombDataCodec.getHoneycombType() == HoneycombTypes.DEFAULT)
                        .filter(honeycombDataCodec -> honeycombDataCodec.getHoneycombBlock() instanceof BlockItem)
                        .map(honeycombDataCodec -> ((BlockItem) honeycombDataCodec.getHoneycombBlock()).getBlock().getRegistryName()).collect(Collectors.toSet()));
    }

    private static void generateHoneyBottleTags() {
        TAGS.put(new ResourceLocation("forge", "tags/items/honey_bottle.json"),
                HoneyRegistry.getRegistry().getHoneyBottles().values().stream()
                        .filter(HoneyBottleData::doGenerateHoneyBlock)
                        .map(honey -> honey.getHoneyBottleRegistryObject().getId()).collect(Collectors.toSet()));
    }

    private static void generateHoneyBlockTags() {
        TAGS.put(new ResourceLocation(ResourcefulBees.MOD_ID, "tags/blocks/resourceful_honey_block.json"),
                HoneyRegistry.getRegistry().getHoneyBottles().values().stream()
                        .filter(HoneyBottleData::doGenerateHoneyBlock)
                        .map(honey -> honey.getHoneyBlockRegistryObject().getId()).collect(Collectors.toSet()));
    }

    private static void generateHoneyBlockItemTags() {
        TAGS.put(new ResourceLocation(ResourcefulBees.MOD_ID, "tags/items/resourceful_honey_block.json"),
                HoneyRegistry.getRegistry().getHoneyBottles().values().stream()
                        .filter(HoneyBottleData::doGenerateHoneyBlock)
                        .map(honey -> honey.getHoneyBlockItemRegistryObject().getId()).collect(Collectors.toSet()));
    }

    private static void generateBeeTags() {
        TAGS.put(new ResourceLocation("minecraft", "tags/entity_types/beehive_inhabitors.json"),
                ModEntities.getModBees().values().stream()
                        .map(RegistryObject::getId).collect(Collectors.toSet()));
    }

    private static void generateHoneyTags() {
        TAGS.put(new ResourceLocation("forge", "tags/fluids/honey.json"),
                HoneyRegistry.getRegistry().getHoneyBottles().values().stream()
                        .filter(HoneyBottleData::doGenerateHoneyFluid)
                        .flatMap(hbd -> Stream.of(hbd.getHoneyFlowingFluidRegistryObject().getId(), hbd.getHoneyStillFluidRegistryObject().getId()))
                        .collect(Collectors.toSet()));
    }

}
