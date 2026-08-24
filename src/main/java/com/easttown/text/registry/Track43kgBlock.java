package com.easttown.text.registry;

import java.util.Collection;

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
 * 43kg 轨距轨道方块。
 *
 * <p>与 Create 标准轨距（STANDARD）物理隔离：覆写 {@link #getConnected}，
 * 过滤掉端点处为异轨距轨道的连接。这样 TrackPropagator 在构建/合并轨道图时
 * 永远走不到标准轨道图 → 43kg 轨道自成一图，可与标准轨道图分开持久化。
 */
public class Track43kgBlock extends TrackBlock {

    public Track43kgBlock(Properties properties, TrackMaterial material) {
        super(properties, material);
    }

    @Override
    public Collection<DiscoveredLocation> getConnected(BlockGetter worldIn, BlockPos pos, BlockState state,
                                                       boolean linear, TrackNodeLocation connectedTo) {
        Collection<DiscoveredLocation> list = super.getConnected(worldIn, pos, state, linear, connectedTo);
        if (list == null || list.isEmpty())
            return list;

        list.removeIf(loc -> {
            Vec3 v = loc.getLocation();
            BlockState adjacent = worldIn.getBlockState(BlockPos.containing(v.x, v.y, v.z));
            if (!(adjacent.getBlock() instanceof ITrackBlock track))
                return false;
            TrackMaterial material = track.getMaterial();
            // 端点落在异轨距轨道上 → 断开该连接（不同轨距物理上不能相接）
            return material != null && material.trackType != AllTrackTypes.TRACK_43KG;
        });
        return list;
    }
}
