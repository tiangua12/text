package com.easttown.text.registry;

import com.easttown.text.Text;
import com.easttown.text.content.bogey.Track43kgBogeyBlock;
import com.easttown.text.content.track.Track43kgBlock;
import com.simibubi.create.content.trains.bogey.BogeySizes;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册表：本类只做 Block 一件事。
 *
 * <p>拆分原则（与 {@link AllItems} / {@link AllBlockEntities} / {@link AllCreativeTabs} 配套）：
 * 每个注册表一个类，字段名不带 {@code _BLOCK} / {@code _ITEM} 后缀——
 * 类名本身就是命名空间（{@code AllBlocks.TRACK_32KG} / {@code AllItems.TRACK_32KG}）。
 *
 * <p>依赖方向（单向，无环）：{@code AllBlocks → AllTrackMaterials → AllTrackTypes}。
 * 方块构造器需要 {@link AllTrackMaterials} 的实例，因此材料类必须先能加载；
 * 反过来材料类对方块的引用一律走 {@code NonNullSupplier.lazy}（见 AllTrackMaterials javadoc）。
 *
 * <p>两条轨道都必须用 {@link Track43kgBlock}（而不是 Create 的 {@code TrackBlock}）：
 * 它覆写 {@code getBogeyAnchor} 返回本模组的转向架。Create 原生实现硬编码
 * {@code AllBlocks.SMALL_BOGEY}（标准转向架），而本模组两条轨道的轨距是
 * {@code text:gauge_1435}，与标准转向架的 {@code create:standard} 不兼容——
 * 用 {@code TrackBlock} 会导致在这两条轨道上装配出注定脱轨的标准转向架。
 *
 * <p>不启用 {@code noCollission()} 是刻意的：转向架与轨道必须可点击（扳手/右键交互），
 * 无碰撞会让玩家直接穿过方块并破坏它。
 */
@SuppressWarnings("null")
public final class AllBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Text.MODID);

    /** 32kg/m 轨道。轨距与 43kg 共用 {@code text:gauge_1435}，故同样走 {@link Track43kgBlock}。 */
    public static final DeferredHolder<Block, Track43kgBlock> TRACK_32KG = BLOCKS.register(
            "track_32kg", () -> new Track43kgBlock(
                    trackProperties(),
                    AllTrackMaterials.TRACK_32KG));

    /** 43kg/m 轨道。 */
    public static final DeferredHolder<Block, Track43kgBlock> TRACK_43KG = BLOCKS.register(
            "track_43kg", () -> new Track43kgBlock(
                    trackProperties(),
                    AllTrackMaterials.TRACK_43KG));

    /** 专用转向架（小号）：只在 {@code text:gauge_1435} 轨距的轨道上兼容。 */
    public static final DeferredHolder<Block, Track43kgBogeyBlock> TRACK_43KG_BOGEY = BLOCKS.register(
            "track_43kg_bogey", () -> new Track43kgBogeyBlock(
                    bogeyProperties(),
                    BogeySizes.SMALL));

    /** 专用转向架（大号）。 */
    public static final DeferredHolder<Block, Track43kgBogeyBlock> TRACK_43KG_LARGE_BOGEY = BLOCKS.register(
            "track_43kg_large_bogey", () -> new Track43kgBogeyBlock(
                    bogeyProperties(),
                    BogeySizes.LARGE));

    /** 轨道方块属性，与 Create 的 {@code AllBlocks.TRACK} 保持一致。 */
    private static BlockBehaviour.Properties trackProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.8f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .forceSolidOn();
    }

    /** 转向架方块属性（Create 转向架用 NETHERITE_BLOCK 音效，无 forceSolidOn）。 */
    private static BlockBehaviour.Properties bogeyProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.8f)
                .sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion();
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private AllBlocks() {
    }
}
