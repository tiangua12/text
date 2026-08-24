package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackMaterial;

/**
 * 暴露 DiscoveredLocation 的 package-private 材料字段（materialA/materialB），
 * 供轨距隔离逻辑读取端点轨距。
 */
@Mixin(TrackNodeLocation.DiscoveredLocation.class)
public interface DiscoveredLocationAccessor {

    @Accessor("materialA")
    TrackMaterial text$getMaterialA();

    @Accessor("materialB")
    TrackMaterial text$getMaterialB();
}
