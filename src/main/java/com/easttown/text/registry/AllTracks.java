package com.easttown.text.registry;

import com.easttown.text.Text;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block / Item / CreativeTab / BlockEntityType registrations.
 */
@SuppressWarnings("null")
public final class AllTracks {

        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Text.MODID);
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Text.MODID);
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
                        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Text.MODID);
        public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister
                        .create(Registries.CREATIVE_MODE_TAB, Text.MODID);

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

        /** 43kg 轨距专用转向架（小号）：真正独立的转向架方块，只在 43kg 轨道上兼容。 */
        public static final DeferredHolder<Block, Track43kgBogeyBlock> TRACK_43KG_BOGEY_BLOCK = BLOCKS.register(
                        "track_43kg_bogey", () -> new Track43kgBogeyBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.METAL)
                                                        .strength(0.8f)
                                                        .sound(SoundType.NETHERITE_BLOCK)
                                                        .noOcclusion(),
                                                        // 无碰撞箱：玩家/实体可穿过；getShape 仍是完整方块，
                                                        // 射线检测可命中 → 能正常点击破坏、扳手交互
                                                      //  .noCollission(),
                                        BogeySizes.SMALL));

        public static final DeferredHolder<Item, Item> TRACK_43KG_BOGEY_ITEM = ITEMS.register(
                        "track_43kg_bogey", () -> new BlockItem(
                                        TRACK_43KG_BOGEY_BLOCK.get(), new Item.Properties()));

        /** 43kg 轨距专用转向架（大号）。 */
        public static final DeferredHolder<Block, Track43kgBogeyBlock> TRACK_43KG_LARGE_BOGEY_BLOCK = BLOCKS.register(
                        "track_43kg_large_bogey", () -> new Track43kgBogeyBlock(
                                        BlockBehaviour.Properties.of()
                                                        .mapColor(MapColor.METAL)
                                                        .strength(0.8f)
                                                        .sound(SoundType.NETHERITE_BLOCK)
                                                        .noOcclusion(),
                                                      //  .noCollission(),
                                        BogeySizes.LARGE));

        public static final DeferredHolder<Item, Item> TRACK_43KG_LARGE_BOGEY_ITEM = ITEMS.register(
                        "track_43kg_large_bogey", () -> new net.minecraft.world.item.BlockItem(
                                        TRACK_43KG_LARGE_BOGEY_BLOCK.get(), new Item.Properties()));

        /** 43kg 转向架专用 BE 类型：validBlocks 只含 43kg 转向架方块。 */
        public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Track43kgBogeyBlockEntity>> TRACK_43KG_BOGEY_BE =
                        BLOCK_ENTITY_TYPES.register("track_43kg_bogey", () -> {
                                BlockEntityType<Track43kgBogeyBlockEntity> type = BlockEntityType.Builder
                                                .of(Track43kgBogeyBlockEntity::new,
                                                                TRACK_43KG_BOGEY_BLOCK.get(),
                                                                TRACK_43KG_LARGE_BOGEY_BLOCK.get())
                                                .build(null);
                                Track43kgBogeyBlockEntity.init(type);
                                return type;
                        });

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TEXT_TAB = TABS.register(
                        "text", () -> CreativeModeTab.builder()
                                        .title(Component.translatable("itemGroup.text"))
                                        .icon(() -> new ItemStack(TRACK_43KG_ITEM.get()))
                                        .displayItems((params, output) -> {
                                                output.accept(TRACK_32KG_ITEM.get());
                                                output.accept(TRACK_43KG_ITEM.get());
                                                output.accept(TRACK_43KG_BOGEY_ITEM.get());
                                                output.accept(TRACK_43KG_LARGE_BOGEY_ITEM.get());
                                        })
                                        .build());

        private AllTracks() {
        }
}
