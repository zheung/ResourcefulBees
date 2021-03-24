package com.resourcefulbees.resourcefulbees.client.gui.screen.beepedia.pages;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.resourcefulbees.resourcefulbees.api.beedata.CustomBeeData;
import com.resourcefulbees.resourcefulbees.client.gui.screen.beepedia.BeepediaScreen;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import com.resourcefulbees.resourcefulbees.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class BeeInfoPage extends BeeDataPage {

    private Entity entityFlower = null;
    List<Block> flowers;
    int counter;
    int size;

    public BeeInfoPage(BeepediaScreen beepedia, CustomBeeData beeData, int xPos, int yPos, BeePage parent) {
        super(beepedia, beeData, xPos, yPos, parent);
        flowers = beeData.hasBlockFlowers() ? new ArrayList<>(beeData.getBlockFlowers()) : new ArrayList<>();
        counter = 0;
        size = flowers.size();
    }

    @Override
    public void renderBackground(MatrixStack matrix, float partialTick, int mouseX, int mouseY) {
        FontRenderer font = Minecraft.getInstance().font;
        TranslationTextComponent title = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info");
        TranslationTextComponent sizeName = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info.size");

        TranslationTextComponent healthName = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info.health");
        TranslationTextComponent damageName = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info.damage");
        TranslationTextComponent stingerName = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info.stinger");
        TranslationTextComponent passiveName = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info.passive");
        TranslationTextComponent poisonName = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info.poison");

        sizeName.append(BeeInfoUtils.getSizeName(beeData.getSizeModifier()));
        damageName.append(new StringTextComponent("" + (int) beeData.getCombatData().getAttackDamage()));
        healthName.append(new StringTextComponent("" + (int) beeData.getCombatData().getBaseHealth()));
        stingerName.append(BeeInfoUtils.getYesNo(beeData.getCombatData().removeStingerOnAttack()));
        passiveName.append(BeeInfoUtils.getYesNo(beeData.getCombatData().isPassive()));
        poisonName.append(BeeInfoUtils.getYesNo(beeData.getCombatData().inflictsPoison()));

        font.draw(matrix, title.withStyle(TextFormatting.WHITE), xPos, (float) yPos + 8, -1);
        font.draw(matrix, sizeName.withStyle(TextFormatting.GRAY), xPos, (float) yPos + 22, -1);
        font.draw(matrix, healthName.withStyle(TextFormatting.GRAY), xPos, (float) yPos + 34, -1);
        font.draw(matrix, damageName.withStyle(TextFormatting.GRAY), (float) xPos + 84, (float) yPos + 34, -1);
        font.draw(matrix, passiveName.withStyle(TextFormatting.GRAY), xPos, (float) yPos + 46, -1);
        font.draw(matrix, poisonName.withStyle(TextFormatting.GRAY), (float) xPos + 84, (float) yPos + 46, -1);
        font.draw(matrix, stingerName.withStyle(TextFormatting.GRAY), xPos, (float) yPos + 58, -1);
    }

    @Override
    public void renderForeground(MatrixStack matrix, int mouseX, int mouseY) {
        FontRenderer font = Minecraft.getInstance().font;
        TranslationTextComponent flowerName = new TranslationTextComponent("gui.resourcefulbees.beepedia.bee_subtab.info.flower");
        if (beeData.hasBlockFlowers()) {
            if (!flowers.isEmpty()) {
                font.draw(matrix, flowerName.withStyle(TextFormatting.GRAY), (float) xPos, (float) yPos + 75, -1);
                beepedia.drawSlot(matrix, flowers.get(counter), xPos + 36, yPos + 70);
            }
        } else if (beeData.hasEntityFlower()) {
            if (entityFlower == null) {
                EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(beeData.getEntityFlower());
                // makes sure the entity is valid
                if (entityType.equals(EntityType.PIG) && (!beeData.getEntityFlower().equals(new ResourceLocation("minecraft:pig"))))
                    return;
                entityFlower = entityType.create(beepedia.getMinecraft().level);
            }
            font.draw(matrix, flowerName.withStyle(TextFormatting.GRAY), (float) xPos, (float) yPos + 80, -1);
            RenderUtils.renderEntity(matrix, entityFlower, beepedia.getMinecraft().level, (float) xPos + 45, (float) yPos + 75, -45, 1.25f);
        }
    }

    @Override
    public String getSearch() {
        return String.format("%s %s %s %s %s",
                BeeInfoUtils.getSizeName(beeData.getSizeModifier()).getString(),
                beeData.getFlower(),
                beeData.getCombatData().isPassive() ? "passive" : "",
                beeData.getCombatData().inflictsPoison() ? "poison" : "",
                beeData.getCombatData().removeStingerOnAttack() ? "stinger" : "");
    }

    @Override
    public void tick(int ticksActive) {
        if (BeeInfoUtils.isShiftPressed()) return;
        if (ticksActive % 20 == 0) {
            counter++;
            if (counter >= size) {
                counter = 0;
            }
        }
    }
}