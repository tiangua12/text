package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 扳手搬迁列车时禁止跨轨距：
 *
 * <p>Create 手持扳手右键列车车厢 → 进入搬迁模式 → 再右键目标轨道，
 * {@code TrainRelocator.relocate} 会把列车整体迁移到目标轨道上，
 * 但原实现不检查目标轨道轨距——43kg 列车可以被搬到标准轨道上（或反之）。
 *
 * <p>本 Mixin 在 {@code relocate} 入口校验：目标轨道
 * （{@code ITrackBlock.getMaterial().trackType}）必须与列车转向架轨距
 * （{@code AbstractBogeyBlock.getTrackType(getStyle())}）一致；
 * 不一致直接返回 false——客户端预览显示红色"无效"，服务端拒绝搬迁，
 * 列车原地不动。搬迁本身（扳手移动列车）保留。
 */
@Mixin(TrainRelocator.class)
public abstract class TrainRelocatorGaugeMixin {

    @SuppressWarnings("null")
    @Inject(method = "relocate", at = @At("HEAD"), cancellable = true)
    private static void text$rejectWrongGaugeRelocation(Train train, Level level, BlockPos pos,
                                                        BezierTrackPointLocation bezier, boolean bezierDirection,
                                                        Vec3 lookAngle, boolean simulate,
                                                        CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ITrackBlock track))
            return;
        TrackMaterial material = track.getMaterial();
        if (material == null)
            return;
        TrackMaterial.TrackType targetType = material.trackType;
        for (Carriage carriage : train.carriages) {
            if (bogeyMismatch(carriage.leadingBogey(), targetType)
                    || bogeyMismatch(carriage.trailingBogey(), targetType)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    private static boolean bogeyMismatch(CarriageBogey bogey, TrackMaterial.TrackType targetType) {
        if (bogey == null)
            return false;
        BogeyStyle style = bogey.getStyle();
        // CarriageBogey.type 是包私有字段，这里通过样式→尺寸→方块的映射取转向架方块
        if (style == null || !style.validSizes().contains(bogey.getSize()))
            return false;
        AbstractBogeyBlock<?> bogeyBlock = style.getBlockForSize(bogey.getSize());
        return bogeyBlock.getTrackType(style) != targetType;
    }
}
