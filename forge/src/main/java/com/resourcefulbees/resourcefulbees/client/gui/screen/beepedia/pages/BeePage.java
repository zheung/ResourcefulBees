package com.resourcefulbees.resourcefulbees.client.gui.screen.beepedia.pages;

import com.mojang.blaze3d.vertex.PoseStack;
import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.api.beedata.CustomBeeData;
import com.resourcefulbees.resourcefulbees.api.beedata.mutation.EntityMutation;
import com.resourcefulbees.resourcefulbees.api.beedata.mutation.ItemMutation;
import com.resourcefulbees.resourcefulbees.client.gui.screen.beepedia.BeepediaPage;
import com.resourcefulbees.resourcefulbees.client.gui.screen.beepedia.BeepediaScreen;
import com.resourcefulbees.resourcefulbees.client.gui.widget.TabImageButton;
import com.resourcefulbees.resourcefulbees.config.Config;
import com.resourcefulbees.resourcefulbees.item.BeeJar;
import com.resourcefulbees.resourcefulbees.registry.BeeRegistry;
import com.resourcefulbees.resourcefulbees.registry.ModItems;
import com.resourcefulbees.resourcefulbees.utils.RenderUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class BeePage extends BeepediaPage {

    public CustomBeeData beeData;

    private Entity bee = null;
    protected Pair<TabImageButton, BeeDataPage> subPage;
    Pair<TabImageButton, BeeDataPage> beeInfoPage;
    Pair<TabImageButton, BeeDataPage> mutations;
    Pair<TabImageButton, BeeDataPage> traitListPage;
    Pair<TabImageButton, BeeDataPage> centrifugePage;
    Pair<TabImageButton, BeeDataPage> spawningPage;
    Pair<TabImageButton, BeeDataPage> breedingPage;
    List<Pair<TabImageButton, BeeDataPage>> tabs = new ArrayList<>();
    ResourceLocation buttonImage = new ResourceLocation(ResourcefulBees.MOD_ID, "textures/gui/beepedia/button.png");

    private int tabCounter;
    MutableComponent label;
    public boolean beeUnlocked;
    private String search = null;

    public BeePage(BeepediaScreen beepedia, CustomBeeData beeData, String id, int xPos, int yPos) {
        super(beepedia, xPos, yPos, id);
        this.beeData = beeData;
        int subX = this.xPos + 1;
        int subY = this.yPos + 50;
        beeUnlocked = beepedia.itemBees.contains(id) || beepedia.complete;

        tabCounter = 0;
        beeInfoPage = Pair.of(
                getTabButton(new ItemStack(Items.BOOK), onPress -> setSubPage(SubPageType.INFO),
                        new TranslatableComponent("gui.resourcefulbees.beepedia.bee_subtab.info")),
                new BeeInfoPage(beepedia, beeData, subX, subY, this)
        );
        subPage = beeInfoPage;
        tabs.add(beeInfoPage);
        if (beeData.getMutationData().testMutations() && (!Config.BEEPEDIA_HIDE_LOCKED.get() || beeUnlocked)) {
            mutations = Pair.of(
                    getTabButton(new ItemStack(Items.FERMENTED_SPIDER_EYE), onPress -> setSubPage(SubPageType.MUTATIONS),
                            new TranslatableComponent("gui.resourcefulbees.beepedia.bee_subtab.mutations")),
                    new MutationListPage(beepedia, beeData, subX, subY, this)
            );
            tabs.add(mutations);
        }
        if (beeData.getTraitData().hasTraits() && beeData.hasTraitNames() && (!Config.BEEPEDIA_HIDE_LOCKED.get() || beeUnlocked)) {
            traitListPage = Pair.of(
                    getTabButton(new ItemStack(Items.BLAZE_POWDER), onPress -> setSubPage(SubPageType.TRAIT_LIST),
                            new TranslatableComponent("gui.resourcefulbees.beepedia.bee_subtab.traits")),
                    new TraitListPage(beepedia, beeData, subX, subY, this)
            );
            tabs.add(traitListPage);
        }
        if (beeData.hasHoneycomb() && (!Config.BEEPEDIA_HIDE_LOCKED.get() || beeUnlocked) && beeData.getCombRegistryObject().isPresent()) {
            centrifugePage = Pair.of(
                    getTabButton(new ItemStack(Items.HONEYCOMB), onPress -> setSubPage(SubPageType.HONEYCOMB),
                            new TranslatableComponent("gui.resourcefulbees.beepedia.bee_subtab.honeycombs")),
                    new HoneycombPage(beepedia, beeData, subX, subY, this)
            );
            tabs.add(centrifugePage);
        }
        if (beeData.getSpawnData().canSpawnInWorld()) {
            spawningPage = Pair.of(
                    getTabButton(new ItemStack(Items.SPAWNER), onPress -> setSubPage(SubPageType.SPAWNING),
                            new TranslatableComponent("gui.resourcefulbees.beepedia.bee_subtab.spawning")),
                    new SpawningPage(beepedia, beeData, subX, subY, this)
            );
            tabs.add(spawningPage);
        }
        List<EntityMutation> breedMutations = BeeRegistry.getRegistry().getMutationsContaining(beeData);
        List<ItemMutation> itemBreedMutation = BeeRegistry.getRegistry().getItemMutationsContaining(beeData);
        if (beeData.getBreedData().isBreedable() || !breedMutations.isEmpty() || !itemBreedMutation.isEmpty()) {
            breedingPage = Pair.of(
                    getTabButton(new ItemStack(ModItems.GOLD_FLOWER_ITEM.get()), onPress -> setSubPage(SubPageType.BREEDING),
                            new TranslatableComponent("gui.resourcefulbees.beepedia.bee_subtab.breeding")),
                    new BreedingPage(beepedia, beeData, subX, subY, breedMutations, itemBreedMutation, this)
            );
            tabs.add(breedingPage);
        }

        ItemStack beeJar = new ItemStack(ModItems.BEE_JAR.get());
        BeeJar.fillJar(beeJar, beeData);
        MutableComponent star = new TextComponent(beeUnlocked ? ChatFormatting.GREEN + "✦ " + ChatFormatting.RESET : "✧ ");
        star.append(beeData.getTranslation());
        label = star;
        newListButton(beeJar, label);
    }

    public TabImageButton getTabButton(ItemStack stack, Button.OnPress pressable, Component tooltip) {
        TabImageButton button = new TabImageButton(this.xPos + 40 + tabCounter * 21, this.yPos + 27,
                20, 20, 0, 0, 20, buttonImage, stack, 2, 2, pressable, beepedia.getTooltipProvider(tooltip));
        beepedia.addButton(button);
        button.visible = false;
        tabCounter++;
        return button;
    }

    @Override
    public void renderBackground(PoseStack matrix, float partialTick, int mouseX, int mouseY) {
        beepedia.getMinecraft().textureManager.bind(splitterImage);
        GuiComponent.blit(matrix, xPos, yPos, 0, 0, 165, 100, 165, 100);
        Minecraft.getInstance().font.draw(matrix, label.withStyle(ChatFormatting.WHITE), (float) xPos + 40, (float) yPos + 10, -1);
        subPage.getRight().renderBackground(matrix, partialTick, mouseX, mouseY);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        double scale = beepedia.getMinecraft().getWindow().getGuiScale();
        int scissorY = (int) (beepedia.getMinecraft().getWindow().getHeight() - (yPos + 9 + 38) * scale);
        GL11.glScissor((int) (xPos * scale), scissorY, (int) (38 * scale), (int) (38 * scale));
        RenderUtils.renderEntity(matrix, getBee(), Minecraft.getInstance().level, (float) xPos + 10, (float) yPos + 2, -45, 2);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

    }

    @Override
    public void openPage() {
        super.openPage();
        if (BeepediaScreen.currScreenState.getPageID() != null) {
            setSubPage(BeepediaScreen.currScreenState.getBeeSubPage());
        } else {
            setSubPage(SubPageType.INFO);
        }
        tabs.forEach(p -> p.getLeft().visible = true);
    }

    @Override
    public void closePage() {
        super.closePage();
        if (subPage != null) this.subPage.getRight().closePage();
        tabs.forEach(p -> p.getLeft().visible = false);
    }

    @Override
    public void renderForeground(PoseStack matrix, int mouseX, int mouseY) {
        subPage.getRight().renderForeground(matrix, mouseX, mouseY);
    }

    @Override
    public String getSearch() {
        if (search == null) {
            search = beeData.getTranslation().getString();
            for (Pair<TabImageButton, BeeDataPage> tab : tabs) {
                search = String.format("%s %s", search, tab.getRight().getSearch());
            }
        }
        return search;
    }

    @Override
    public void tick(int ticksActive) {
        subPage.getRight().tick(ticksActive);
    }

    @Override
    public void drawTooltips(PoseStack matrixStack, int mouseX, int mouseY) {
        subPage.getRight().drawTooltips(matrixStack, mouseX, mouseY);
        tabs.forEach(p -> p.getLeft().renderToolTip(matrixStack, mouseX, mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        return subPage.getRight().mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return subPage.getRight().mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void setSubPage(SubPageType beeSubPage) {
        Pair<TabImageButton, BeeDataPage> page;
        switch (beeSubPage) {
            case BREEDING:
                page = breedingPage;
                break;
            case INFO:
                page = beeInfoPage;
                break;
            case SPAWNING:
                page = spawningPage;
                break;
            case MUTATIONS:
                page = mutations;
                break;
            case HONEYCOMB:
                page = centrifugePage;
                break;
            case TRAIT_LIST:
                page = traitListPage;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + beeSubPage);
        }
        if (subPage != null) {
            this.subPage.getRight().closePage();
            this.subPage.getLeft().active = true;
        }
        this.subPage = page == null ? beeInfoPage : page;
        if (subPage != null) {
            if (!(subPage.getRight() instanceof SpawningPage)) {
                BeepediaScreen.currScreenState.setBiomesOpen(false);
                BeepediaScreen.currScreenState.setSpawningScroll(0);
            }
            if (!(subPage.getRight() instanceof TraitListPage)) {
                BeepediaScreen.currScreenState.setTraitsScroll(0);
            }
            this.subPage.getLeft().active = false;
            this.subPage.getRight().openPage();
        }

        BeepediaScreen.currScreenState.setBeeSubPage(beeSubPage);
    }

    public Entity getBee() {
        if (bee == null) bee = beepedia.initEntity(beeData.getEntityTypeRegistryID());
        return bee;
    }

    public enum SubPageType {
        INFO,
        SPAWNING,
        BREEDING,
        MUTATIONS,
        HONEYCOMB,
        TRAIT_LIST
    }
}