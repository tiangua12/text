package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.graph.TrackEdge;

/**
 * 轨距边界校验（列车转向时）：
 *
 * <p>Create 的 {@code TrackEdge.canTravelTo} 只检查方向对齐，不检查轨距——因此
 * 只要轨道图连通（不同轨距轨道相接），列车就能从标准边直接转向开进 43kg 边。
 *
 * <p>本 mixin 在方向检查基础上追加<b>轨距匹配</b>：相邻两条边的
 * {@code getTrackMaterial().trackType} 不同则禁止互转。这样：
 * <ul>
 *   <li>列车在标准边上行驶到节点时，43kg 边被排除 → 选不上 → 停车（不会开过去）；</li>
 *   <li>列车（含 43kg 转向架列车）在 43kg 边上行驶时，标准边同样被排除；</li>
 *   <li>同轨距轨道之间不受影响（正常转弯/直行）。</li>
 * </ul>
 */
@Mixin(TrackEdge.class)
public abstract class TrackEdgeGaugeMixin {

    @Inject(method = "canTravelTo", at = @At("RETURN"), cancellable = true)
    private void text$rejectCrossGaugeTurn(TrackEdge other, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ())
            return; // 原本就不可转向，无需处理

        TrackEdge self = (TrackEdge) (Object) this;
        var selfType = self.getTrackMaterial() == null ? null : self.getTrackMaterial().trackType;
        var otherType = other.getTrackMaterial() == null ? null : other.getTrackMaterial().trackType;

        // 轨距不同 → 禁止转向（异轨距轨道不能互转）
        if (selfType != null && otherType != null && selfType != otherType)
            cir.setReturnValue(false);
    }
}
