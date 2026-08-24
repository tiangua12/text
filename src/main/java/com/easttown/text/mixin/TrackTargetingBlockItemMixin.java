package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.easttown.text.Config;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;

@Mixin(TrackTargetingBlockItem.class)
public class TrackTargetingBlockItemMixin {

    @ModifyConstant(method = "useOn", constant = @Constant(intValue = 16), remap = false)
    private int modifyBaseDistanceInt(int original) {
        return Config.TRACK_TARGETING_MAX_DISTANCE.getAsInt();
    }

    @ModifyConstant(method = "useOn", constant = @Constant(doubleValue = 16.0), remap = false)
    private double modifyBaseDistanceDouble(double original) {
        return Config.TRACK_TARGETING_MAX_DISTANCE.getAsInt();
    }
}
