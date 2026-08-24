package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.easttown.text.util.TrackCurveCollisionHelper;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 弯道放置完成后（TrackPlacement.placeTracks 返回时），把曲线扫过的
 * 空气格子填充为不可见的 NONE 轨道方块，给弯道曲线区域补碰撞体积。
 *
 * <p>注意：handler 方法体会被 mixin 嵌入 TrackPlacement 类，因此方法体内
 * 只能引用普通包/目标类/第三方类，不能引用 mixin 包内的类。
 */
@Mixin(TrackPlacement.class)
public class TrackPlacementCurveMixin {

	@Inject(method = "placeTracks", at = @At("RETURN"))
	private static void text$fillCurveCollision(Level level, TrackPlacement.PlacementInfo info,
		BlockState state1, BlockState state2, BlockPos targetPos1, BlockPos targetPos2, boolean simulate,
		CallbackInfoReturnable<TrackPlacement.PlacementInfo> cir) {
		BezierConnection curve = TrackCurveCollisionHelper.getCurve(info);
		if (simulate || curve == null)
			return;
		TrackCurveCollisionHelper.fillCurveCollision(level, curve, info.trackMaterial);
	}
}
