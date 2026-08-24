package com.easttown.text.registry;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

/**
 * 43kg 轨距轨道方块。
 *
 * <p>轨道方块自身不做任何连接过滤——端点必须完整保留，轨道才可选中、可行驶、
 * 可被信号/车站绑定。轨距隔离由两处完成：
 * <ul>
 *   <li>{@code TrackPlacementMixin}：放置时拒绝异轨距轨道相接（不能从标准轨道上接 43kg）；</li>
 *   <li>{@code TrackGaugeIsolationMixin}：建图搜索时跳过异轨距轨道，43kg 图与标准图永不合并。</li>
 * </ul>
 */
public class Track43kgBlock extends TrackBlock {

    public Track43kgBlock(Properties properties, TrackMaterial material) {
        super(properties, material);
    }
}
