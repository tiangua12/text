package com.easttown.text.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 轨道图轨距隔离（图层面）：
 *
 * <p>Create 的轨道图（TrackGraph）按节点位置合并——不同轨距轨道物理相邻时会
 * 共享节点、并入同一张图（"看似拆开、实则连通"）。本 mixin 在
 * {@code ITrackBlock.walkConnectedTracks}（图构建/连接搜索的唯一入口）过滤掉
 * 异轨距的连接端点：从 43kg 轨道端点出发永远走不到标准轨道（反之亦然），
 * 因此 43kg 轨道图与标准轨道图<b>真正分离</b>，可独立持久化。
 *
 * <p>轨道方块自身的端点（getConnected）保持完整，因此信号/车站绑定
 * （TrackGraphHelper）、轨道选中、列车行驶均不受影响。
 */
@Mixin(ITrackBlock.class)
public interface TrackGaugeIsolationMixin {

    @Inject(method = "walkConnectedTracks", at = @At("RETURN"), cancellable = true)
    private static void text$filterCrossGauge(BlockGetter worldIn, TrackNodeLocation location, boolean linear,
                                              CallbackInfoReturnable<Collection<DiscoveredLocation>> cir) {
        Collection<DiscoveredLocation> result = cir.getReturnValue();
        if (result == null || result.isEmpty() || !(location instanceof DiscoveredLocation current))
            return;

        TrackMaterial source = ((DiscoveredLocationAccessor) (Object) current).text$getMaterialA();
        if (source == null || source.trackType == null)
            return;

        BlockGetter world = location != null && worldIn instanceof ServerLevel sl ? sl.getServer()
            .getLevel(location.dimension) : worldIn;

        List<DiscoveredLocation> filtered = new ArrayList<>();
        for (DiscoveredLocation loc : result) {
            TrackMaterial target = ((DiscoveredLocationAccessor) (Object) loc).text$getMaterialA();
            // 未知材料（默认 andesite 兜底）不阻断；同轨距保留；异轨距剔除
            if (target != null && target.trackType != null && target.trackType != source.trackType) {
                // 双保险：确认目标端点的方块确实是异轨距轨道再剔除
                BlockPos bp = BlockPos.containing(loc.getLocation());
                BlockState bs = world.getBlockState(bp);
                if (bs.getBlock() instanceof ITrackBlock track && track.getMaterial() != null
                        && track.getMaterial().trackType != source.trackType)
                    continue;
            }
            filtered.add(loc);
        }
        cir.setReturnValue(filtered);
    }
}
