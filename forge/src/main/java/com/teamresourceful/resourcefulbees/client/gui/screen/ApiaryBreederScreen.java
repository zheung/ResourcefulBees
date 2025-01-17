package com.teamresourceful.resourcefulbees.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teamresourceful.resourcefulbees.ResourcefulBees;
import com.teamresourceful.resourcefulbees.client.gui.widget.TabImageButton;
import com.teamresourceful.resourcefulbees.container.ApiaryBreederContainer;
import com.teamresourceful.resourcefulbees.lib.ApiaryTabs;
import com.teamresourceful.resourcefulbees.network.NetPacketHandler;
import com.teamresourceful.resourcefulbees.network.packets.ApiaryTabMessage;
import com.teamresourceful.resourcefulbees.registry.ModItems;
import com.teamresourceful.resourcefulbees.tileentity.multiblocks.apiary.ApiaryBreederTileEntity;
import com.teamresourceful.resourcefulbees.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class ApiaryBreederScreen extends AbstractContainerScreen<ApiaryBreederContainer> {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(ResourcefulBees.MOD_ID, "textures/gui/apiary/apiary_breeder_gui.png");
    private static final ResourceLocation TABS_BG = new ResourceLocation(ResourcefulBees.MOD_ID, "textures/gui/apiary/apiary_gui_tabs.png");

    private final ApiaryBreederTileEntity apiaryBreederTileEntity;

    private TabImageButton mainTabButton;
    private TabImageButton storageTabButton;


    public ApiaryBreederScreen(ApiaryBreederContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        apiaryBreederTileEntity = this.menu.getApiaryBreederTileEntity();
        preInit();
    }

    protected void preInit(){
        this.imageWidth = 226;
        this.imageHeight = 110 + this.menu.getNumberOfBreeders() * 20;
    }

    @Override
    protected void init() {
        super.init();
        this.buttons.clear();

        int i = this.leftPos;
        int j = this.topPos;
        int t = i + this.imageWidth - 24;

        mainTabButton = this.addButton(new TabImageButton(t+1, j+17, 18, 18, 110, 0, 18, TABS_BG, new ItemStack(ModItems.BEE_JAR.get()), 1, 1,
                onPress -> this.changeScreen(ApiaryTabs.MAIN), 128, 128) {

            @Override
            public void renderToolTip(@NotNull PoseStack matrix, int mouseX, int mouseY) {
                TranslatableComponent s = new TranslatableComponent("gui.resourcefulbees.apiary.button.main_screen");
                ApiaryBreederScreen.this.renderTooltip(matrix, s, mouseX, mouseY);
            }
        });

        storageTabButton = this.addButton(new TabImageButton(t + 1, j + 37, 18, 18, 110, 0, 18, TABS_BG, new ItemStack(Items.HONEYCOMB), 2, 1,
                onPress -> this.changeScreen(ApiaryTabs.STORAGE), 128, 128) {

            @Override
            public void renderToolTip(@NotNull PoseStack matrix,int mouseX, int mouseY) {
                TranslatableComponent s = new TranslatableComponent("gui.resourcefulbees.apiary.button.storage_screen");
                ApiaryBreederScreen.this.renderTooltip(matrix, s, mouseX, mouseY);
            }
        });

        this.addButton(new TabImageButton(t + 1, j + 57, 18, 18, 110, 0, 18, TABS_BG, new ItemStack(ModItems.GOLD_FLOWER_ITEM.get()), 1, 1,
                onPress -> this.changeScreen(ApiaryTabs.BREED), 128, 128) {

            @Override
            public void renderToolTip(@NotNull PoseStack matrix, int mouseX, int mouseY) {
                TranslatableComponent s = new TranslatableComponent("gui.resourcefulbees.apiary.button.breed_screen");
                ApiaryBreederScreen.this.renderTooltip(matrix, s, mouseX, mouseY);
            }
        }).active = false;
    }

    private void changeScreen(ApiaryTabs tab) {
        switch (tab) {
            case BREED:
                break;
            case STORAGE:
                if (storageTabButton.active && getApiaryBreederTileEntity() != null)
                    NetPacketHandler.sendToServer(new ApiaryTabMessage(getApiaryBreederTileEntity().getBlockPos(), ApiaryTabs.STORAGE));
                break;
            case MAIN:
                if (mainTabButton.active && getApiaryBreederTileEntity() != null)
                    NetPacketHandler.sendToServer(new ApiaryTabMessage(getApiaryBreederTileEntity().getBlockPos(), ApiaryTabs.MAIN));
        }
    }

    @Override
    public void render(@NotNull PoseStack matrix,int mouseX, int mouseY, float partialTicks) {
        if (getApiaryBreederTileEntity() != null) {
            this.renderBackground(matrix);
            super.render(matrix, mouseX, mouseY, partialTicks);
            this.renderTooltip(matrix, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull PoseStack matrix, float partialTicks, int mouseX, int mouseY) {
        if (this.menu.isRebuild()) {
            preInit();
            init();
            this.menu.setRebuild(false);
        }

        mainTabButton.active = getApiaryBreederTileEntity().getApiary() != null;
        storageTabButton.active = getApiaryBreederTileEntity().getApiary() != null && getApiaryBreederTileEntity().getApiary().getStoragePos() != null;


        Minecraft client = this.minecraft;
        if (client != null) {
            client.getTextureManager().bind(BACKGROUND);
            int i = this.leftPos;
            int j = this.topPos;
            //upgrade slots
            blit(matrix, i, j+16, 0, 16, 25, 82);
            //Top of screen
            blit(matrix, i+25, j, 25, 0, 176, 15);
            //slots
            int scaledprogress;
            for (int z = 0; z < this.menu.getNumberOfBreeders(); z++){
                blit(matrix, i+25, j+ 15 + (z*20), 25, 15, 176, 20);
            }

            for (int k = 0; k < this.menu.getNumberOfBreeders(); k++) {
                scaledprogress = MathUtils.clamp(118 * this.menu.times.get(k) / this.menu.getApiaryBreederTileEntity().getTotalTime(), 0, this.menu.getApiaryBreederTileEntity().getTotalTime());
                blit(matrix, i+54, j + 21 + (k*20), 0, 246, scaledprogress, 10);
            }

            blit(matrix, i+25, j+15 + (20 * this.menu.getNumberOfBreeders()), 25, 95, 176, 95);

            int t = i + this.imageWidth - 24;
            this.minecraft.getTextureManager().bind(TABS_BG);
            blit(matrix, t -1, j + 12, 0,0, 25, 68, 128, 128);
        }
    }

    @Override
    protected void renderLabels(@NotNull PoseStack matrix, int mouseX, int mouseY) {
        for (AbstractWidget widget : this.buttons) {
            if (widget.isHovered()) {
                widget.renderToolTip(matrix, mouseX - this.leftPos, mouseY - this.topPos);
                break;
            }
        }
    }

    public ApiaryBreederTileEntity getApiaryBreederTileEntity() {
        return apiaryBreederTileEntity;
    }
}
