package com.easttown.text.registry;

import com.easttown.text.Text;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.resources.ResourceLocation;

/**
 * 两种钢轨（32kg/m、43kg/m）的 TrackMaterial 注册。
 *
 * <p><b>轨距归属</b>：两者共用同一个轨距 {@code AllTrackTypes.GAUGE_1435}
 * （{@code text:gauge_1435}）。现实里 32kg/m 与 43kg/m 只是钢轨断面型号不同，
 * 轨距都是 1435mm，所以两条轨道可以互转、共用同一套转向架。
 * 与 Create 原版 {@code create:standard} 轨距仍是两个不同对象，互不兼容。
 *
 * <p>Skeleton (stage B). API signatures verified against
 * create-1.21.1-6.0.10-281-sources.jar and Registrate sources:
 * <ul>
 *   <li>{@code TrackMaterialFactory.make(RL).lang(...).block(...).particle(...)
 *       .standardModels().build()} — confirmed in TrackMaterialFactory.java
 *       (make L46, lang L50, block L55, particle L91, standardModels L101, build L126).</li>
 *   <li>{@code block(...)} takes {@code NonNullSupplier<NonNullSupplier<? extends TrackBlock>>}
 *       — confirmed in TrackMaterial.java L44/L61 and TrackMaterialFactory.java L24/L55.</li>
 *   <li>{@code NonNullSupplier} package is
 *       {@code com.tterrag.registrate.util.nullness.NonNullSupplier} — confirmed in
 *       TrackMaterial.java L19 import; static {@code lazy(Supplier)} at NonNullSupplier.java L30.</li>
 *   <li>{@code standardModels()} maps partial models to
 *       {@code block/track/<material>/{tie,segment_left,segment_right}} — TrackMaterialFactory.java L104-107.</li>
 * </ul>
 *
 * <p>Static-init ordering (R5): the block supplier is a <em>double</em> lazy —
 * the outer {@code NonNullSupplier.lazy} defers creating the inner lazy, and the
 * inner lazy defers {@code AllBlocks.TRACK_32KG.get()}. Neither
 * {@code AllBlocks} nor {@code AllTrackMaterials} dereferences the other during
 * class initialisation, so either class may load first safely.
 */
public final class AllTrackMaterials {

    /**
     * 32kg/m 钢轨。轨距 = {@code text:gauge_1435}（与 43kg 共用）。
     *
     * <p>必须显式 {@code .trackType(...)}：不写就走 {@code TrackMaterialFactory}
     * 的默认值 {@code TrackType.STANDARD}（{@code create:standard}），
     * 那样两条轨道会分裂成两个轨距。
     */
    public static final TrackMaterial TRACK_32KG = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_32kg"))
            .lang("32kg/m Rail")
            .block(NonNullSupplier.lazy(() ->
                    NonNullSupplier.lazy(() -> AllBlocks.TRACK_32KG.get())))
            .particle(ResourceLocation.fromNamespaceAndPath(Text.MODID,
                    "block/track/track_32kg/ascending"))
            .trackType(AllTrackTypes.GAUGE_1435)
            .standardModels()
            .build();

    /**
     * 43kg/m 钢轨：使用 43kg 素材（轨枕/钢轨贴图），形状模型沿用 Create 的
     * {@code standardModels()} 约定。轨距 = {@code text:gauge_1435}（与 32kg 共用）。
     */
    public static final TrackMaterial TRACK_43KG = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"))
            .lang("43kg/m Rail")
            .block(NonNullSupplier.lazy(() ->
                    NonNullSupplier.lazy(() -> AllBlocks.TRACK_43KG.get())))
            .particle(ResourceLocation.fromNamespaceAndPath(Text.MODID,
                    "block/track/track_43kg/segment_43kg"))
            .trackType(AllTrackTypes.GAUGE_1435)
            .standardModels()
            .build();

    private AllTrackMaterials() {
    }
}
