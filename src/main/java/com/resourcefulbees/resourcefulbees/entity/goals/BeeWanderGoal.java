package com.resourcefulbees.resourcefulbees.entity.goals;

import com.resourcefulbees.resourcefulbees.entity.passive.ModBeeEntity;
import com.resourcefulbees.resourcefulbees.utils.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;

public class BeeWanderGoal extends Goal {
    private final ModBeeEntity modBeeEntity;

    public BeeWanderGoal(ModBeeEntity modBeeEntity) {
        this.modBeeEntity = modBeeEntity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    public boolean canUse() {
        return modBeeEntity.getNavigation().isDone() && modBeeEntity.getRandom().nextInt(10) == 0;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        return modBeeEntity.getNavigation().isInProgress();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start() {
        Vector3d vector3d = this.getRandomLocation();
        if (vector3d != null) {
            modBeeEntity.getNavigation().moveTo(modBeeEntity.getNavigation().createPath(new BlockPos(vector3d), 1), 1.0D);
        }

    }

    @Nullable
    private Vector3d getRandomLocation() {
        Vector3d vector3d;
        if (modBeeEntity.isHiveValid() && !modBeeEntity.checkIsWithinDistance(Objects.requireNonNull(modBeeEntity.hivePos), 22)) {
            Vector3d vector3d1 = Vector3d.atCenterOf(modBeeEntity.hivePos);
            vector3d = vector3d1.subtract(modBeeEntity.position()).normalize();
        } else {
            vector3d = modBeeEntity.getViewVector(0.0F);
        }

        int randHorz = modBeeEntity.getRandom().nextInt(8) + 8;

        Vector3d vector3d2 = RandomPositionGenerator.findAirTarget(modBeeEntity, randHorz, 7, vector3d);
        return vector3d2 != null ? vector3d2 : RandomPositionGenerator.findGroundTarget(modBeeEntity, randHorz, 6, -4, vector3d);
    }
}
