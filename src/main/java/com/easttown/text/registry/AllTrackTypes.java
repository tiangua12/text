package com.easttown.text.registry;

import com.easttown.text.Text;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyRenderer;
import com.simibubi.create.content.trains.bogey.StandardBogeyVisual;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 43kg 轨距（TrackType）与 43kg 专用转向架样式（BogeyStyle）注册。
 *
 * <p>Create 6.0.10 内置轨距框架：
 * <ul>
 *   <li>{@link TrackMaterial.TrackType} —— 一种"轨距"；{@code TrackBlock} 持有所属材料，
 *       轨道边（{@code TrackEdge.getTrackMaterial().trackType}）记录轨距。</li>
 *   <li>{@code AbstractBogeyBlock.isOnIncompatibleTrack} —— 转向架所在边的轨距
 *       ≠ {@code getTrackType(style)} 时判定不兼容（脱轨）。</li>
 *   <li>{@code Navigation.search} L706 —— 寻路只走转向架允许轨距的边。</li>
 * </ul>
 */
public final class AllTrackTypes {

    /** 43kg 轨距类型：与 Create 标准轨距（STANDARD）物理不兼容。 */
    public static final TrackMaterial.TrackType TRACK_43KG = new TrackMaterial.TrackType(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"), TrackBlock::new);

    /**
     * 43kg 专用转向架样式。测试阶段：外观与 Create 普通转向架一致
     * （复用 StandardBogeyRenderer / StandardBogeyVisual），且**复用 Create 已有的
     * 转向架方块**（small_bogey / large_bogey，与 AllBogeyStyles.STANDARD 相同的方块），
     * 不单独注册新方块——由 {@code StandardBogeyBlockMixin} 让
     * {@code getTrackType(style)} 按样式返回 43kg 轨距。
     *
     * <p>cycle group 独立于 STANDARD（不加入 create:standard），因此用扳手不能在
     * 普通转向架与 43kg 转向架之间互相切换——不同轨距的转向架不通用。
     */
    public static final BogeyStyle TRACK_43KG_STYLE = new BogeyStyle.Builder(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"),
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"))
            .displayName(Component.translatable("bogey.style.text.track_43kg"))
            .size(BogeySizes.SMALL,
                    AllBlocks.SMALL_BOGEY,
                    () -> () -> new BogeyStyle.SizeRenderer(
                            new StandardBogeyRenderer.Small(), StandardBogeyVisual.Small::new))
            .size(BogeySizes.LARGE,
                    AllBlocks.LARGE_BOGEY,
                    () -> () -> new BogeyStyle.SizeRenderer(
                            new StandardBogeyRenderer.Large(), StandardBogeyVisual.Large::new))
            .build();

    private AllTrackTypes() {
    }
}
