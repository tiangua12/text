package com.easttown.text.registry;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

/**
 * 43kg 轨距专用转向架方块。
 *
 * <p>外观/尺寸/渲染与 Create 普通转向架完全一致（测试期复用
 * {@link StandardBogeyBlock} 行为），唯一区别是 {@link #getTrackType} 返回 43kg
 * 轨距类型：
 * <ul>
 *   <li>在 43kg 轨道上：{@code isOnIncompatibleTrack} 判定兼容，可正常行驶；</li>
 *   <li>在标准轨道上：轨距不匹配 → 脱轨判定 / 寻路拒绝（真正不同轨距）。</li>
 * </ul>
 *
 * <p>BE 复用 Create 的 {@code AllBlockEntityTypes.BOGEY}（StandardBogeyBlockEntity）；
 * 转向架样式（BogeyStyle）只影响外观与扳手循环，轨距行为完全由本类的
 * {@code getTrackType} 决定，不受 BE 默认样式影响。
 */
public class Track43kgBogeyBlock extends StandardBogeyBlock {

    public Track43kgBogeyBlock(Properties props, BogeySizes.BogeySize size) {
        super(props, size);
    }

    @Override
    public TrackMaterial.TrackType getTrackType(BogeyStyle style) {
        return AllTrackTypes.TRACK_43KG;
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return AllTrackTypes.TRACK_43KG_STYLE;
    }
}
