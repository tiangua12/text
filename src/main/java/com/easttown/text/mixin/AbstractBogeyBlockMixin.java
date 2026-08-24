package com.easttown.text.mixin;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.easttown.text.registry.AllTrackTypes;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.track.TrackMaterial;

/**
 * 测试放行：普通（STANDARD 样式）转向架允许在 43kg 轨道上行驶。
 *
 * <p>正式行为：不同轨距不兼容（43kg 轨道只能跑 43kg 转向架）。测试阶段用户要求
 * 普通转向架样式也可用于 43kg 轨道，故：
 * <ul>
 *   <li>{@code isOnIncompatibleTrack}：当转向架是普通 STANDARD 样式、所在边为
 *       43kg 轨道时返回兼容；</li>
 *   <li>{@code getValidPathfindingTypes}：STANDARD 样式额外放行 43kg 轨距寻路。</li>
 * </ul>
 * 43kg 专用转向架（{@code Track43kgBogeyBlock}）不受影响，仍只能在 43kg 轨道行驶。
 */
@Mixin(AbstractBogeyBlock.class)
public abstract class AbstractBogeyBlockMixin {

    @Inject(method = "isOnIncompatibleTrack", at = @At("RETURN"), cancellable = true)
    private void text$allowStandardBogeyOn43kg(Carriage carriage, boolean leading,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ())
            return; // 原本就兼容，无需放行

        CarriageBogey bogey = leading ? carriage.leadingBogey() : carriage.trailingBogey();
        if (bogey.getStyle() != AllBogeyStyles.STANDARD)
            return; // 只放行普通转向架样式

        TravellingPoint point = leading ? carriage.getLeadingPoint() : carriage.getTrailingPoint();
        TrackEdge edge = point.edge;
        if (edge != null && edge.getTrackMaterial() != null
                && edge.getTrackMaterial().trackType == AllTrackTypes.TRACK_43KG)
            cir.setReturnValue(false); // 普通转向架在 43kg 轨道 → 测试期放行
    }

    @Inject(method = "getValidPathfindingTypes", at = @At("RETURN"), cancellable = true)
    private void text$allowStandardBogeyPathfindOn43kg(BogeyStyle style,
                                                       CallbackInfoReturnable<Set<TrackMaterial.TrackType>> cir) {
        if (style != AllBogeyStyles.STANDARD)
            return;

        Set<TrackMaterial.TrackType> types = new HashSet<>(cir.getReturnValue());
        types.add(AllTrackTypes.TRACK_43KG);
        cir.setReturnValue(types);
    }
}
