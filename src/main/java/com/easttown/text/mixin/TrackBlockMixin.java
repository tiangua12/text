package com.easttown.text.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 轨距物理隔离（双向）：
 * 任何轨道（包括 Create 标准轨道）的 {@code getConnected} 都不返回"端点落在
 * 异轨距轨道方块内"的连接。这样 TrackPropagator 构建/合并轨道图时，43kg 轨道
 * 永远不会与标准轨道图合并 → 43kg 图可独立持久化。
 *
 * <p>同轨距轨道之间不受影响（create 标准轨道 trackType=STANDARD，32kg 同）。
 */
@Mixin(TrackBlock.class)
public abstract class TrackBlockMixin {

    @Shadow
    protected TrackMaterial material;

    @Inject(method = "getConnected", at = @At("RETURN"), cancellable = true)
    private void text$filterCrossGaugeConnections(BlockGetter worldIn, BlockPos pos, BlockState state,
                                                  boolean linear, TrackNodeLocation connectedTo,
                                                  CallbackInfoReturnable<Collection<DiscoveredLocation>> cir) {
        Collection<DiscoveredLocation> list = cir.getReturnValue();
        if (list == null || list.isEmpty())
            return;

        list.removeIf(loc -> {
            Vec3 v = loc.getLocation();
            BlockState adjacent = worldIn.getBlockState(BlockPos.containing(v.x, v.y, v.z));
            if (!(adjacent.getBlock() instanceof ITrackBlock track))
                return false;
            TrackMaterial m = track.getMaterial();
            return m != null && m.trackType != material.trackType;
        });
    }
}
