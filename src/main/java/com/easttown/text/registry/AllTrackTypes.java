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
 * 1435mm 标准轨距（TrackType）与专用转向架样式（BogeyStyle）注册。
 *
 * <p>轨距归属：{@code track_32kg} 与 {@code track_43kg} <b>共用同一个 TrackType</b>
 * （{@link #GAUGE_1435}）。现实里 32kg/m 与 43kg/m 只是钢轨型号不同，轨距都是
 * 1435mm；Create 的轨距判定只看 {@code TrackType} 引用是否相等（不做数值比较），
 * 所以两条轨道可以互转、共用同一套转向架。Create 原版 {@code create:standard}
 * 轨距与本轨距仍然是两个不同的对象，因此原版转向架在本模组轨道上依然不兼容。
 *
 * <p>Create 6.0.10 内置轨距框架：
 * <ul>
 * <li>{@link TrackMaterial.TrackType} —— 一种"轨距"；只有 {@code id} + {@code factory}
 * 两个字段，<b>没有轨距数值</b>。视觉上的轨距宽度在渲染层硬编码
 * （{@code BezierConnection} 的 ±0.965，已由 mixin 改为 1435mm 对应偏移）。</li>
 * <li>{@code AbstractBogeyBlock.isOnIncompatibleTrack} —— 转向架所在边的轨距
 * ≠ {@code getTrackType(style)} 时判定不兼容（脱轨）。</li>
 * <li>{@code Navigation.search} L706 —— 寻路只走转向架允许轨距的边。</li>
 * </ul>
 *
 * <p>专用转向架共三个样式，共用 cycle group {@code text:track_43kg}，扳手在组内循环切换
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

    /**
     * 1435mm 标准轨距类型，{@code track_32kg} 与 {@code track_43kg} 共用。
     *
     * <p>{@code id} 只用于日志与调试：Create 对 {@code TrackType} 只做引用比较
     * （{@code AbstractBogeyBlock.isOnIncompatibleTrack}、{@code Navigation.search}），
     * 从不把这个 id 写进存档，所以改名不影响老存档。
     *
     * <p>{@code factory} 保持 {@code TrackBlock::new}：它是
     * {@code TrackMaterial.createBlock} 在 {@code customFactory} 为空时的兜底，
     * Create 全项目只在注册 {@code create:track} 那一个方块时用到它；本模组的方块
     * 由 {@code AllBlocks} 直接实例化（这里是 {@code Track43kgBlock}），所以 factory
     * 不参与实际行为。刻意不写 {@code Track43kgBlock::new} 是为了避免
     * {@code AllTrackTypes → content.track → AllBlocks → AllTrackMaterials} 多绕一环。
     */
    public static final TrackMaterial.TrackType GAUGE_1435 = new TrackMaterial.TrackType(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "gauge_1435"), TrackBlock::new);

    /**
     * 原版小型转向架样式（仅 SMALL 档）：外观为 Create 标准小转向架
     * （StandardBogeyRenderer.Small / StandardBogeyVisual.Small），
     * 指向专用小号转向架方块 {@code track_43kg_bogey}。
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
     * 指向专用大号转向架方块 {@code track_43kg_large_bogey}。
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
     * SMALL/LARGE 两档共用同一模型，分别指向大小转向架方块。
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
