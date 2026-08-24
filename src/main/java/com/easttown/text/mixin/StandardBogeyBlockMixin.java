package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.easttown.text.registry.AllTrackTypes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

/**
 * 让 Create 的普通转向架方块（small_bogey / large_bogey）在挂上 43kg 专用样式时
 * 返回 43kg 轨距。
 *
 * <p>Create 的转向架方块不区分轨距——轨距由转向架当前的 {@link BogeyStyle} 决定：
 * 同一个 small_bogey 方块，样式为 STANDARD 时只能在标准轨道行驶；用扳手切到
 * 43kg 样式后 {@code getTrackType} 返回 43kg → 只能在 43kg 轨道行驶（真正不同轨距）。
 */
@Mixin(StandardBogeyBlock.class)
public abstract class StandardBogeyBlockMixin {

    @Inject(method = "getTrackType", at = @At("RETURN"), cancellable = true)
    private void text$trackTypeByStyle(BogeyStyle style, CallbackInfoReturnable<TrackMaterial.TrackType> cir) {
        if (style == AllTrackTypes.TRACK_43KG_STYLE)
            cir.setReturnValue(AllTrackTypes.TRACK_43KG);
    }
}
