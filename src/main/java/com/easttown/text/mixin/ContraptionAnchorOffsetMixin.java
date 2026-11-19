package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.easttown.text.content.train.CarriageOffsets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.world.phys.Vec3;

/**
 * 把车厢竖直偏移抬进 contraption 的"锚点"，一次改动同时覆盖碰撞、交互与实体碰撞箱。
 *
 * <p>{@code getAnchorVec()} 是 Create 里"局部坐标 → 世界坐标"的唯一原点：
 * <ul>
 * <li>{@code AbstractContraptionEntity#setPos} 用它把 {@code contraption.bounds}
 * 搬到世界，得到实体 AABB（列车推挤、挤压判定、视锥剔除）；</li>
 * <li>{@code toGlobalVector} / {@code toLocalVector} 用它做双向换算——玩家点车厢方块、
 * 坐座位、传送门切片、{@code ContraptionCollider} 的射线，都走这里；</li>
 * <li>{@code ContraptionCollider#collideEntities} 拿它配合
 * {@code contraption.getSimplifiedEntityColliders()}（局部坐标的逐方块碰撞形状）
 * 做真正的实体 ↔ 车厢碰撞。</li>
 * </ul>
 * 因此"抬高锚点"等价于"抬高整节车厢的碰撞体"，而<b>实体本身的位置不动</b>——
 * 这一点很关键：转向架的世界位置由 {@code CarriageBogey} 的 {@code TravellingPoint}
 * 独立计算，只要不动实体坐标，轮对就仍然压在钢轨上。
 *
 * <p>{@code getPrevAnchorVec()} 必须同样处理：它是上一 tick 的锚点，
 * {@code getContactPointMotion} 用两者之差算车厢表面的位移，只抬一边会算出一个
 * 竖直方向的假速度。{@code position()} 在两端都是"当前实体位置"，取两次差值不受影响。
 */
@Mixin(AbstractContraptionEntity.class)
public abstract class ContraptionAnchorOffsetMixin {

    @Inject(method = "getAnchorVec", at = @At("HEAD"), cancellable = true)
    private void text$offsetAnchor(CallbackInfoReturnable<Vec3> cir) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        float offset = CarriageOffsets.of(self);
        if (offset != 0)
            cir.setReturnValue(self.position().add(0, offset, 0));
    }

    @Inject(method = "getPrevAnchorVec", at = @At("HEAD"), cancellable = true)
    private void text$offsetPrevAnchor(CallbackInfoReturnable<Vec3> cir) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        float offset = CarriageOffsets.of(self);
        if (offset != 0)
            cir.setReturnValue(self.getPrevPositionVec().add(0, offset, 0));
    }
}
