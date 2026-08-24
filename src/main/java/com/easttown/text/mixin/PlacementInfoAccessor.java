package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.track.TrackPlacement;

/**
 * 暴露 PlacementInfo 的 package-private {@code valid} 字段，
 * 供跨轨距放置拒绝逻辑修改（clientTick 依赖 PlacementInfo 完整初始化，
 * 只能在方法完整执行后把 valid 置 false）。
 */
@Mixin(TrackPlacement.PlacementInfo.class)
public interface PlacementInfoAccessor {

    @Accessor("valid")
    void text$setValid(boolean valid);
}
