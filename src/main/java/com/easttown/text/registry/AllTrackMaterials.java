package com.easttown.text.registry;

import com.easttown.text.Text;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.resources.ResourceLocation;

/**
 * TrackMaterial registrations for the "32kg/m Rail" track.
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
 *       {@code block/track/track_32kg/{tie,segment_left,segment_right}} — TrackMaterialFactory.java L104-107.</li>
 * </ul>
 *
 * <p>Static-init ordering (R5): the block supplier is a <em>double</em> lazy —
 * the outer {@code NonNullSupplier.lazy} defers creating the inner lazy, and the
 * inner lazy defers {@code AllBlocks.TRACK_32KG.get()}. Neither
 * {@code AllBlocks} nor {@code AllTrackMaterials} dereferences the other during
 * class initialisation, so either class may load first safely.
 */
public final class AllTrackMaterials {

    public static final TrackMaterial TRACK_32KG = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_32kg"))
            .lang("32kg/m Rail")
            .block(NonNullSupplier.lazy(() ->
                    NonNullSupplier.lazy(() -> AllBlocks.TRACK_32KG.get())))
            .particle(ResourceLocation.fromNamespaceAndPath(Text.MODID,
                    "block/track/track_32kg/ascending"))
            .standardModels()
            .build();

    /** 铁轨43kg/米：使用 资源\43 素材（43kg 轨枕/钢轨贴图）。轨距类型为独立的 43kg（TRACK_43KG）。 */
    public static final TrackMaterial TRACK_43KG = TrackMaterialFactory
            .make(ResourceLocation.fromNamespaceAndPath(Text.MODID, "track_43kg"))
            .lang("43kg/m Rail")
            .block(NonNullSupplier.lazy(() ->
                    NonNullSupplier.lazy(() -> AllBlocks.TRACK_43KG.get())))
            .particle(ResourceLocation.fromNamespaceAndPath(Text.MODID,
                    "block/track/track_43kg/segment_43kg"))
            .trackType(AllTrackTypes.TRACK_43KG)
            .standardModels()
            .build();

    private AllTrackMaterials() {
    }
}
