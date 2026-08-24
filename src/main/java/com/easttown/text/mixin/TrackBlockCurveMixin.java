package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.easttown.text.util.TrackCurveCollisionHelper;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 弯道节点方块被拆除时，按各 BezierConnection 的包围盒清理之前填充的
 * NONE 隐形碰撞方块，避免留下幽灵方块。
 */
@Mixin(TrackBlock.class)
public class TrackBlockCurveMixin {

	@Inject(method = "onRemove", at = @At("HEAD"))
	private static void text$clearCurveCollision(BlockState state, Level level, BlockPos pos, BlockState newState,
		boolean moved, CallbackInfo ci) {
		if (state.getValue(TrackBlock.HAS_BE) && !state.is(newState.getBlock())) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof TrackBlockEntity tbe)
				for (BezierConnection bc : tbe.getConnections()
					.values())
					TrackCurveCollisionHelper.clearCurveCollision(level, bc);
		}
	}
}
