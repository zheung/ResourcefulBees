package com.resourcefulbees.resourcefulbees.config;

import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.data.BeeData;
import com.resourcefulbees.resourcefulbees.entity.passive.CustomBeeEntity;
import com.resourcefulbees.resourcefulbees.lib.BeeConstants;
import com.resourcefulbees.resourcefulbees.registry.RegistryHandler;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import com.resourcefulbees.resourcefulbees.utils.BeeValidator;
import com.resourcefulbees.resourcefulbees.utils.RandomCollection;
import com.google.gson.Gson;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.placement.ChanceConfig;
import net.minecraft.world.gen.placement.HeightWithChanceConfig;
import net.minecraft.world.gen.placement.Placement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.resourcefulbees.resourcefulbees.ResourcefulBees.LOGGER;
import static com.resourcefulbees.resourcefulbees.config.BeeInfo.BEE_INFO;
import static com.resourcefulbees.resourcefulbees.config.Config.GENERATE_DEFAULTS;

public class BeeBuilder {

    private static final String ASSETS_DIR = "/assets/resourcefulbees/default_bees/";

    private static final String[] DEFAULT_BEES = new String[]{
            "Diamond.json",
            "Emerald.json",
            "Gold.json",
            "Iron.json",
            "Coal.json",
            "Redstone.json",
            "Nether_Quartz.json",
            "Lapis_Lazuli.json",
            "Ender.json",
            "Creeper.json",
            "Pigman.json",
            "Skeleton.json",
            "Wither.json",
            "Zombie.json"
    };

    public static Path BEE_PATH;
    public static Path RESOURCE_PATH;

    public static void setupBees() {
        if (GENERATE_DEFAULTS.get()) setupDefaultBees();
        addBees();
        setupBeeSpawns();
    }

    private static void parseBee(File file) throws IOException {
        String name = file.getName();
        name = name.substring(0, name.indexOf('.'));

        Gson gson = new Gson();
        Reader r = new FileReader(file);
        BeeData bee = gson.fromJson(r, BeeData.class);
        bee.setName(name);
        if (BeeValidator.validate(bee)) {
            BeeInfo.BEE_INFO.put(name.toLowerCase(), bee);
            if (bee.canSpawnInWorld())
                BeeInfoUtils.parseBiomes(bee);
            if (bee.isBreedable())
                BeeInfoUtils.buildFamilyTree(bee);
            if (Config.SHOW_DEBUG_INFO.get())
                LOGGER.info(name + " bee passed validation check!!");
        }
    }

    private static void addBees() {
        //BeeInfo.BEE_INFO.clear();
        BeeInfoUtils.genDefaultBee();
        try {
            Files.walk(BEE_PATH)
                    .filter(f -> f.getFileName().toString().endsWith(".zip"))
                    .forEach(BeeBuilder::openZipFile);
            Files.walk(BEE_PATH)
                    .filter(f -> f.getFileName().toString().endsWith(".json"))
                    .forEach(BeeBuilder::addBee);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addBee(Path file) {
        File f = file.toFile();
        try {
            parseBee(f);
        } catch (IOException e) {
            LOGGER.error("File not found when parsing bees");
        }
    }

    private static void openZipFile(Path file){
        try {
            ZipFile zf = new ZipFile(file.toString());
            zf.stream().forEach(zipEntry -> {
                if(zipEntry.isDirectory()) {
                    extractDirectoryFromZip(zipEntry, zf);
                } else if (zipEntry.getName().endsWith(".json")) {
                    extractJSONFromZip(zipEntry, zf);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not read ZipFile! ZipFile: " + file.getFileName());
        }
    }

    public static void extractDirectoryFromZip(ZipEntry zipEntry, ZipFile zf) {
        Path dirToCreate = BEE_PATH.resolve(zipEntry.getName());
        try {
            Files.createDirectory(dirToCreate);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            LOGGER.warn("Could not create directory from ZipFile! ZipFile: " + zf.getName() + " Directory: " + zipEntry.getName());
        }
    }

    public static void extractJSONFromZip(ZipEntry zipEntry, ZipFile zf) {
        try {
            Path fileToCreate = BEE_PATH.resolve(zipEntry.getName());
            Files.copy(zf.getInputStream(zipEntry), fileToCreate, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            LOGGER.warn("Could not create file from ZipFile! ZipFile: " + zf.getName() + " File: " + zipEntry.getName());
        }
    }

    private static void setupDefaultBees() {
        for (String bee : DEFAULT_BEES) {
            String path = ASSETS_DIR + bee;
            try (InputStream inputStream = ResourcefulBees.class.getResourceAsStream(path)) {
                Path newPath = Paths.get(BEE_PATH.toString() + "/" + bee);
                File targetFile = new File(String.valueOf(newPath));
                Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void setupBeeSpawns() {
        for (Map.Entry<Biome, RandomCollection<String>> element : BeeInfo.SPAWNABLE_BIOMES.entrySet()) {
            Biome biome = element.getKey();
            if (Config.GENERATE_BEE_NESTS.get()) {
                addNestFeature(biome);
            }
            int divisor = Config.GENERATE_BEE_NESTS.get() ? 2 : 1;
            biome.getSpawns(EntityClassification.CREATURE).add(new Biome.SpawnListEntry(RegistryHandler.CUSTOM_BEE.get(),
                    Config.SPAWN_WEIGHT.get() / divisor,
                    Config.SPAWN_MIN_GROUP.get() / divisor,
                    Config.SPAWN_MAX_GROUP.get() / divisor));
        }

        EntitySpawnPlacementRegistry.register(RegistryHandler.CUSTOM_BEE.get(),
                EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                CustomBeeEntity::canBeeSpawn);
    }

    private static void addNestFeature(Biome biome) {
        Biome.Category category = biome.getCategory();
        if (category == Biome.Category.NETHER)
            biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION,
                    RegistryHandler.BEE_NEST_FEATURE.get()
                            .withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG)
                            .withPlacement(Placement.COUNT_CHANCE_HEIGHTMAP_DOUBLE
                                    .configure(new HeightWithChanceConfig(3, .125f))));
        else
            biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION,
                    RegistryHandler.BEE_NEST_FEATURE.get()
                            .withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG)
                            .withPlacement(Placement.CHANCE_TOP_SOLID_HEIGHTMAP
                                    .configure(new ChanceConfig(16))));
    }

    public static void GenerateEnglishLang() {
        if (Config.GENERATE_ENGLISH_LANG.get()) {
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            for (Map.Entry<String, BeeData> bee : BEE_INFO.entrySet()) {
                if (!bee.getKey().equals(BeeConstants.DEFAULT_BEE_TYPE)) {
                    String displayName = StringUtils.replace(bee.getKey(), "_", " ");
                    displayName = WordUtils.capitalizeFully(displayName);

                    //block
                    builder.append("\"block.resourcefulbees.");
                    builder.append(bee.getKey());
                    builder.append("_honeycomb_block\" : \"");
                    builder.append(displayName);
                    builder.append(" Honeycomb Block\",\n");
                    //comb
                    builder.append("\"item.resourcefulbees.");
                    builder.append(bee.getKey());
                    builder.append("_honeycomb\" : \"");
                    builder.append(displayName);
                    builder.append(" Honeycomb\",\n");
                    //spawn egg
                    builder.append("\"item.resourcefulbees.");
                    builder.append(bee.getKey());
                    builder.append("_spawn_egg\" : \"");
                    builder.append(displayName);
                    builder.append(" Bee Spawn Egg\",\n");
                    //entity
                    builder.append("\"entity.resourcefulbees.");
                    builder.append(bee.getKey());
                    builder.append("_bee\" : \"");
                    builder.append(displayName);
                    builder.append(" Bee\",\n");
                }
            }

            builder.deleteCharAt(builder.lastIndexOf(","));
            builder.append("}");

            String langPath = RESOURCE_PATH.toString() + "/assets/resourcefulbees/lang/";
            String langFile = "en_us.json";
            try {
                Files.createDirectories(Paths.get(langPath));
                FileWriter writer = new FileWriter(Paths.get(langPath, langFile).toFile());
                writer.write(builder.toString());
                writer.close();
                LOGGER.info("Language File Generated!");
            } catch (IOException e) {
                LOGGER.error("Could not generate language file!");
                e.printStackTrace();
            }
        }
    }
}