package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.simibubi.create.infrastructure.config.CTrains;

@Mixin(CTrains.class)
public class CTrainsMixin {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 128, ordinal = 0), remap = false)
    private int removeMaxTrackPlacementLengthLimit(int original) {
        return Integer.MAX_VALUE;
    }
}
