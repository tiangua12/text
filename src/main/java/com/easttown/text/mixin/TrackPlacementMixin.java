package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackPlacement;
import com.simibubi.create.content.trains.track.TrackPlacement.ConnectingFrom;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 放置时禁止跨轨距连接：
 *
 * <p>从已有轨道（起点）延伸放置新轨道时，若起点的轨距与新轨道的轨距不同
 * （例如从 43kg 轨道上接标准轨道），拒绝放置并提示——不同轨距物理上不能直接相接。
 *
 * <p>实现要点：不在 {@code tryConnect} HEAD 提前返回（那样会返回字段未初始化的
 * {@code PlacementInfo}，clientTick 渲染会 NPE），而是先用 {@link #text$rejectGauge}
 * 标记，让 {@code tryConnect} 完整执行并正常初始化字段，最后在 RETURN 把结果
 * 标记为无效（valid=false + gauge_mismatch 消息）。
 */
@Mixin(TrackPlacement.class)
public abstract class TrackPlacementMixin {

    @Unique
    private static boolean text$rejectGauge = false;

    @Inject(method = "tryConnect", at = @At("HEAD"))
    private static void text$detectGaugeMismatch(Level level, Player player, BlockPos pos2,
                                                 BlockState state2, ItemStack stack, boolean girder,
                                                 boolean maximiseTurn, CallbackInfoReturnable<PlacementInfo> cir) {
        text$rejectGauge = false;

        // 新轨道的轨距
        TrackMaterial newMaterial = TrackMaterial.fromItem(stack.getItem());
        if (newMaterial == null)
            return;

        // 起点轨道（玩家先选中的已有轨道）
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null)
            return;
        BlockState startState = level.getBlockState(from.pos());
        if (!(startState.getBlock() instanceof ITrackBlock startTrack))
            return;
        TrackMaterial startMaterial = startTrack.getMaterial();
        if (startMaterial == null)
            return;

        // 轨距不同 → 标记拒绝（不提前 return，保证 PlacementInfo 字段完整初始化）
        if (startMaterial.trackType != newMaterial.trackType)
            text$rejectGauge = true;
    }

    @Inject(method = "tryConnect", at = @At("RETURN"), cancellable = true)
    private static void text$rejectAtReturn(Level level, Player player, BlockPos pos2,
                                            BlockState state2, ItemStack stack, boolean girder,
                                            boolean maximiseTurn, CallbackInfoReturnable<PlacementInfo> cir) {
        if (!text$rejectGauge)
            return;
        text$rejectGauge = false;

        PlacementInfo info = cir.getReturnValue();
        if (info == null)
            return;
        ((PlacementInfoAccessor) (Object) info).text$setValid(false);
        info.withMessage("gauge_mismatch");
    }
}
