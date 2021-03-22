
package com.resourcefulbees.resourcefulbees.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.config.Config;
import com.resourcefulbees.resourcefulbees.registry.ModRecipeSerializers;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import com.resourcefulbees.resourcefulbees.utils.RecipeUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class CentrifugeRecipe implements IRecipe<IInventory> {

    public static final Logger LOGGER = LogManager.getLogger();

    public static final IRecipeType<CentrifugeRecipe> CENTRIFUGE_RECIPE_TYPE = IRecipeType.register(ResourcefulBees.MOD_ID + ":centrifuge");
    public final ResourceLocation id;
    public final Ingredient ingredient;
    public final List<Pair<ItemStack, Float>> itemOutputs;
    public final List<Pair<FluidStack, Float>> fluidOutput;
    public final int time;
    public final int multiblockTime;
    public final boolean multiblock;
    public final boolean hasFluidOutput;
    public boolean noBottleInput;

    private static final String INGREDIENT_STRING = "ingredient";

    public CentrifugeRecipe(ResourceLocation id, Ingredient ingredient, List<Pair<ItemStack, Float>> itemOutputs, List<Pair<FluidStack, Float>> fluidOutput, int time, int multiblockTime, boolean multiblock, boolean hasFluidOutput) {
        this.id = id;
        this.ingredient = ingredient;
        this.itemOutputs = itemOutputs;
        this.fluidOutput = fluidOutput;
        this.time = time;
        this.multiblockTime = multiblockTime;
        this.multiblock = multiblock;
        this.hasFluidOutput = hasFluidOutput;
        this.noBottleInput = false;
    }

    public CentrifugeRecipe(ResourceLocation id, Ingredient ingredient, List<Pair<ItemStack, Float>> itemOutputs, List<Pair<FluidStack, Float>> fluidOutput, int time, int multiblockTime, boolean multiblock, boolean hasFluidOutput, boolean noBottleInput) {
        this.id = id;
        this.ingredient = ingredient;
        this.itemOutputs = itemOutputs;
        this.fluidOutput = fluidOutput;
        this.time = time;
        this.multiblockTime = multiblockTime;
        this.multiblock = multiblock;
        this.hasFluidOutput = hasFluidOutput;
        this.noBottleInput = noBottleInput;
    }

    @Override
    public boolean matches(IInventory inventory, @Nonnull World world) {
        ItemStack stack = inventory.getItem(0);
        ItemStack bottle = inventory.getItem(1);

        boolean noBottle = bottle.isEmpty() || bottle.getItem() == Items.AIR || bottle.getCount() == 0;

        // fail to get recipe if does not have enough bottles
        if (!noBottleInput && noBottle || (itemOutputs.size() > 2 && bottle.getCount() < itemOutputs.get(2).getLeft().getCount()))
            return false;
        // fail to get recipe if is honey fluid output and there are more than 0 bottles
        if (noBottleInput && bottle.getCount() > 0) return false;
        if (stack == ItemStack.EMPTY) return false;
        else {
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length == 0) return false;
            else {
                return Arrays.stream(matchingStacks).anyMatch(itemStack -> Container.consideredTheSameItem(stack, itemStack));
            }
        }
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull IInventory inventory) {
        return ItemStack.EMPTY;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    /**
     * Get the result of this recipe, usually for display purposes (e.g. recipe book). If your recipe has more than one
     * possible result (e.g. it's dynamic and depends on its inputs), then return an empty stack.
     */
    @Override
    public @NotNull ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ResourceLocation getId() {
        return id;
    }

    @NotNull
    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CENTRIFUGE_RECIPE.get();
    }

    @NotNull
    @Override
    public IRecipeType<?> getType() {
        return CENTRIFUGE_RECIPE_TYPE;
    }

    public static class Serializer<T extends CentrifugeRecipe> extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<T> {
        final IRecipeFactory<T> factory;

        public Serializer(Serializer.IRecipeFactory<T> factory) {
            this.factory = factory;
        }

        @Override
        public @NotNull T fromJson(@Nonnull ResourceLocation id, @NotNull JsonObject json) {
            Ingredient ingredient;
            if (JSONUtils.isArrayNode(json, INGREDIENT_STRING)) {
                ingredient = Ingredient.fromJson(JSONUtils.getAsJsonArray(json, INGREDIENT_STRING));
            } else {
                ingredient = Ingredient.fromJson(JSONUtils.getAsJsonObject(json, INGREDIENT_STRING));
            }

            JsonArray jsonArray = JSONUtils.getAsJsonArray(json, "results");
            List<Pair<ItemStack, Float>> outputs = new ArrayList<>();
            List<Pair<FluidStack, Float>> fluidOutput = new ArrayList<>();

            boolean hasFluidOutput = JSONUtils.getAsBoolean(json, "hasFluidOutput", false);
            if (!hasFluidOutput) {
                fluidOutput.add(Pair.of(FluidStack.EMPTY, 1f));
            }

            jsonArray.forEach(jsonElement -> {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.has("item")) {
                    // collect data
                    String registryName = JSONUtils.getAsString(jsonObject, "item");
                    int count = JSONUtils.getAsInt(jsonObject, "count", 1);
                    Float chance = JSONUtils.getAsFloat(jsonObject, "chance", 1);

                    // collect nbt
                    CompoundNBT nbt = new CompoundNBT();
                    if (jsonObject.has("nbtData")) {
                        JsonElement nbtData = JSONUtils.getAsJsonObject(jsonObject, "nbtData");
                        nbt = CompoundNBT.CODEC.parse(JsonOps.INSTANCE, nbtData).resultOrPartial(e -> LOGGER.warn(String.format("Could not deserialize NBT: [%s]", nbtData.toString()))).orElse(nbt);
                    }

                    // create item stack
                    ItemStack stack = new ItemStack(BeeInfoUtils.getItem(registryName), count);

                    // apply nbt
                    if (!nbt.isEmpty()) stack.setTag(nbt);

                    // collect and set potion
                    if (registryName.equals("minecraft:potion") && jsonObject.has("potion")) {
                        stack.getOrCreateTag()
                                .putString("Potion", JSONUtils.getAsString(jsonObject, "potion"));
                    }
                    // add outputs
                    outputs.add(Pair.of(stack, chance));
                } else if (jsonObject.has("fluid")) {
                    String fluid = JSONUtils.getAsString(jsonObject, "fluid");
                    int amount = JSONUtils.getAsInt(jsonObject, "amount", 1);
                    Float chance = JSONUtils.getAsFloat(jsonObject, "chance", 1);
                    FluidStack stack = new FluidStack(BeeInfoUtils.getFluid(fluid), amount);
                    fluidOutput.add(Pair.of(stack, chance));
                }
            });

            int time = JSONUtils.getAsInt(json, "time", Config.GLOBAL_CENTRIFUGE_RECIPE_TIME.get());
            int multiblockTime = JSONUtils.getAsInt(json, "multiblockTime", time - Config.MULTIBLOCK_RECIPE_TIME_REDUCTION.get());
            boolean multiblock = JSONUtils.getAsBoolean(json, "multiblock", false);
            boolean noBottle = JSONUtils.getAsBoolean(json, "noBottleInput", false);

            return this.factory.create(id, ingredient, outputs, fluidOutput, time, multiblockTime, multiblock, hasFluidOutput, noBottle);
        }

        public T fromNetwork(@Nonnull ResourceLocation id, @NotNull PacketBuffer buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            List<Pair<ItemStack, Float>> itemOutputs = new ArrayList<>();
            List<Pair<FluidStack, Float>> fluidOutput = new ArrayList<>();
            IntStream.range(0, buffer.readInt()).forEach(i -> itemOutputs.add(Pair.of(RecipeUtils.readItemStack(buffer), buffer.readFloat())));
            IntStream.range(0, buffer.readInt()).forEach(value -> fluidOutput.add(Pair.of(FluidStack.readFromPacket(buffer), buffer.readFloat())));
            int time = buffer.readInt();
            int multiblockTime = buffer.readInt();
            boolean multiblock = buffer.readBoolean();
            boolean hasFluidOutput = buffer.readBoolean();
            boolean noBottle = buffer.readBoolean();
            return this.factory.create(id, ingredient, itemOutputs, fluidOutput, time, multiblockTime, multiblock, hasFluidOutput, noBottle);
        }

        public void toNetwork(@NotNull PacketBuffer buffer, T recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeInt(recipe.itemOutputs.size());
            recipe.itemOutputs.forEach(itemStackFloatPair -> {
                ItemStack stack = itemStackFloatPair.getLeft();
                RecipeUtils.writeItemStack(stack, buffer);
                buffer.writeFloat(itemStackFloatPair.getRight());
            });
            buffer.writeInt(recipe.fluidOutput.size());
            recipe.fluidOutput.forEach(fluidStackFloatPair -> {
                FluidStack fluidStack = fluidStackFloatPair.getLeft();
                fluidStack.writeToPacket(buffer);
                buffer.writeFloat(fluidStackFloatPair.getRight());
            });
            buffer.writeInt(recipe.time);
            buffer.writeInt(recipe.multiblockTime);
            buffer.writeBoolean(recipe.multiblock);
            buffer.writeBoolean(recipe.hasFluidOutput);
            buffer.writeBoolean(recipe.noBottleInput);
        }

        public interface IRecipeFactory<T extends CentrifugeRecipe> {
            T create(ResourceLocation id, Ingredient input, List<Pair<ItemStack, Float>> itemOutputs, List<Pair<FluidStack, Float>> fluidOutputs, int time, int multiblockTime, boolean multiblock, boolean hasFluidOutput, boolean noBottleInput);
        }
    }
}
