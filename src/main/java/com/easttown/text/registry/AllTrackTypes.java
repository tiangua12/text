package com.easttown.text.registry;

import com.easttown.text.Text;
import com.easttown.text.client.Hxd3cBogeyRenderer;
import com.easttown.text.client.Hxd3cBogeyVisual;
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
 * <li>{@link TrackMaterial.TrackType} —— 一种"轨距"；{@code TrackBlock} 持有所属材料，
 * 轨道边（{@code TrackEdge.getTrackMaterial().trackType}）记录轨距。</li>
 * <li>{@code AbstractBogeyBlock.isOnIncompatibleTrack} —— 转向架所在边的轨距
 * ≠ {@code getTrackType(style)} 时判定不兼容（脱轨）。</li>
 * <li>{@code Navigation.search} L706 —— 寻路只走转向架允许轨距的边。</li>
 * </ul>
 *
 * <p>43kg 转向架共三个样式，共用 cycle group {@code text:track_43kg}，扳手在组内循环切换
 * （小 → 大 → HXD3C → 小）：
 * <ol>
 * <li>{@link #TRACK_43KG_STYLE} —— 原版小型转向架（Create 标准小转向架外观），仅 SMALL 档；</li>
 * <li>{@link #TRACK_43KG_LARGE_STYLE} —— 原版大型转向架（Create 标准大转向架外观），仅 LARGE 档；</li>
 * <li>{@link #HXD3C_STYLE} —— HXD3C 电力集成转向架（自定义模型），SMALL/LARGE 两档同外观。</li>
 * </ol>
 * 三个样式必须各自独立（不能合并成"一个样式两档"）：Create 扳手切换样式时，如果当前
 * 尺寸不在目标样式的 validSizes 里会把尺寸调到目标样式支持的档位并换对应方块——
 * 若"原版样式"同时含大小两档而 HXD3C 只有一档，尺寸会被卡在 HXD3C 支持的档位，
 * 永远切不回另一档（例如大号切到 HXD3C 后变小，再也回不到大号）。
 */
public final class AllTrackTypes {

    /** 43kg 轨距类型：与 Create 标准轨距（STANDARD）物理不兼容。 */
    public static final TrackMaterial.TrackType TRACK_43KG = new TrackMaterial.TrackType(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"), TrackBlock::new);

    /**
     * 原版小型转向架样式（仅 SMALL 档）：外观为 Create 标准小转向架
     * （StandardBogeyRenderer.Small / StandardBogeyVisual.Small），
     * 指向 43kg 小转向架方块 {@code track_43kg_bogey}。
     */
    public static final BogeyStyle TRACK_43KG_STYLE = new BogeyStyle.Builder(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"),
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"))
            .displayName(Component.translatable("bogey.style.text.track_43kg"))
            .size(BogeySizes.SMALL,
                    () -> AllBlocks.TRACK_43KG_BOGEY.get(),
                    () -> () -> new BogeyStyle.SizeRenderer(
                            new StandardBogeyRenderer.Small(), StandardBogeyVisual.Small::new))
            .build();

    /**
     * 原版大型转向架样式（仅 LARGE 档）：外观为 Create 标准大转向架
     * （StandardBogeyRenderer.Large / StandardBogeyVisual.Large），
     * 指向 43kg 大转向架方块 {@code track_43kg_large_bogey}。
     */
    public static final BogeyStyle TRACK_43KG_LARGE_STYLE = new BogeyStyle.Builder(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg_large"),
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"))
            .displayName(Component.translatable("bogey.style.text.track_43kg_large"))
            .size(BogeySizes.LARGE,
                    () -> AllBlocks.TRACK_43KG_LARGE_BOGEY.get(),
                    () -> () -> new BogeyStyle.SizeRenderer(
                            new StandardBogeyRenderer.Large(), StandardBogeyVisual.Large::new))
            .build();

    /**
     * HXD3C 电力集成转向架样式：外观为 HXD3C 机车转向架
     * （Hxd3cBogeyRenderer / Hxd3cBogeyVisual，Co-Co 三轴轮对），
     * SMALL/LARGE 两档共用同一模型，分别指向大小 43kg 转向架方块。
     */
    public static final BogeyStyle HXD3C_STYLE = new BogeyStyle.Builder(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "hxd3c"),
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"))
            .displayName(Component.translatable("bogey.style.text.hxd3c"))
            .size(BogeySizes.SMALL,
                    () -> AllBlocks.TRACK_43KG_BOGEY.get(),
                    () -> () -> new BogeyStyle.SizeRenderer(
                            new Hxd3cBogeyRenderer(), Hxd3cBogeyVisual::new))
            .size(BogeySizes.LARGE,
                    () -> AllBlocks.TRACK_43KG_LARGE_BOGEY.get(),
                    () -> () -> new BogeyStyle.SizeRenderer(
                            new Hxd3cBogeyRenderer(), Hxd3cBogeyVisual::new))
            .build();

    private AllTrackTypes() {
    }
}
