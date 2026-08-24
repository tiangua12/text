package com.easttown.text.registry;

import com.easttown.text.Text;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block / Item / CreativeTab registrations for the "32kg/m Rail" track.
 *
 * <p>
 * Skeleton (stage B). API signatures verified against
 * create-1.21.1-6.0.10-281-sources.jar and neoforge-21.1.235-sources.jar:
 * <ul>
 * <li>{@code DeferredRegister.createBlocks / createItems} return the
 * {@code Blocks}/{@code Items} subclasses whose
 * {@code register(String, Supplier)}
 * yield {@code DeferredBlock<B>} / {@code DeferredItem<I>} (both extend
 * {@code DeferredHolder}) — confirmed in NeoForge DeferredRegister.java.</li>
 * <li>{@code TrackBlock(BlockBehaviour.Properties, TrackMaterial)} — public
 * ctor,
 * confirmed in TrackBlock.java L115.</li>
 * <li>{@code TrackBlockItem(Block, Item.Properties)} — public ctor,
 * confirmed in TrackBlockItem.java L42.</li>
 * <li>Block properties mirror create {@code AllBlocks.TRACK} (AllBlocks.java
 * L1612-1616).</li>
 * </ul>
 */
public final class AllTracks {

        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Text.MODID);
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Text.MODID);
        @SuppressWarnings("null")
        public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister
                        .create(Registries.CREATIVE_MODE_TAB, Text.MODID);

        @SuppressWarnings("null")
        public static final DeferredHolder<Block, TrackBlock> TRACK_32KG_BLOCK = BLOCKS.register(
                        "track_32kg", () -> new TrackBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.METAL)
                                                        .strength(0.8f)
                                                        .sound(SoundType.METAL)
                                                        .noOcclusion()
                                                        .forceSolidOn(),
                                        AllTrackMaterials.TRACK_32KG));

        public static final DeferredHolder<Item, TrackBlockItem> TRACK_32KG_ITEM = ITEMS.register(
                        "track_32kg", () -> new TrackBlockItem(TRACK_32KG_BLOCK.get(), new Item.Properties()));

        /** 铁轨43kg/米：方块属性与 32kg 一致（参照 create AllBlocks.TRACK）。 */
        @SuppressWarnings("null")
        public static final DeferredHolder<Block, Track43kgBlock> TRACK_43KG_BLOCK = BLOCKS.register(
                        "track_43kg", () -> new Track43kgBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.METAL)
                                                        .strength(0.8f)
                                                        .sound(SoundType.METAL)
                                                        .noOcclusion()
                                                        .forceSolidOn(),
                                        AllTrackMaterials.TRACK_43KG));

        public static final DeferredHolder<Item, TrackBlockItem> TRACK_43KG_ITEM = ITEMS.register(
                        "track_43kg", () -> new TrackBlockItem(TRACK_43KG_BLOCK.get(), new Item.Properties()));

        @SuppressWarnings("null")
        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TEXT_TAB = TABS.register(
                        "text", () -> CreativeModeTab.builder()
                                        .title(Component.translatable("itemGroup.text"))
                                        .icon(() -> new ItemStack(TRACK_43KG_ITEM.get()))
                                        .displayItems((params, output) -> {
                                                output.accept(TRACK_32KG_ITEM.get());
                                                output.accept(TRACK_43KG_ITEM.get());
                                        })
                                        .build());

        // TODO(C): PLAN ownership table lists a TRACK_SHAPE_MODEL_IDS constant
        // (TrackShape -> model file id map used by the hand-written blockstate,
        // §4.1/§6). It is not required by any Java API; add it here only if C
        // decides the blockstate generation benefits from a shared constant.

        private AllTracks() {
        }
}
