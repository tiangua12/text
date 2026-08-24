package com.easttown.text.registry;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

/**
 * 43kg 轨距轨道方块。
 *
 * <p>轨道方块自身不做任何连接过滤——端点必须完整保留，轨道才可选中、可行驶、
 * 可被信号/车站绑定。轨距隔离由两处完成：
 * <ul>
 *   <li>{@code TrackPlacementMixin}：放置时拒绝异轨距轨道相接（不能从标准轨道上接 43kg）；
 *       43kg 轨道因此自成网络，轨道图天然与标准图分离，可独立持久化；</li>
 *   <li>Create 原生轨距校验：移动时 {@code isOnIncompatibleTrack}、寻路时
 *       {@code Navigation} validTypes 过滤——列车无法跨轨距行驶。</li>
 * </ul>
 */
public class Track43kgBlock extends TrackBlock {

    public Track43kgBlock(Properties properties, TrackMaterial material) {
        super(properties, material);
    }
}
