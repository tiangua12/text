package com.easttown.text.content.bogey;

import com.easttown.text.registry.AllBlockEntities;
import com.easttown.text.registry.AllBlocks;
import com.easttown.text.registry.AllTrackTypes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 43kg 转向架方块实体：默认样式为原版 43kg 样式（{@link AllTrackTypes#TRACK_43KG_STYLE}）。
 *
 * <p>Create 装配列车时从转向架方块 BE 读取样式——本 BE 默认原版样式，
 * 因此用 43kg 转向架方块装配的列车转向架默认就是原版外观
 * （可用扳手切到 HXD3C 样式）。
 */
public class Track43kgBogeyBlockEntity extends StandardBogeyBlockEntity {

    /** 供 2 参数工厂使用的类型缓存（注册时由 {@link AllBlockEntities} 设置）。 */
    private static BlockEntityType<?> cachedType;

    public Track43kgBogeyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 2 参数构造器（BlockEntityType.Builder.of 使用）：从缓存获取真实类型。 */
    public Track43kgBogeyBlockEntity(BlockPos pos, BlockState state) {
        this(cachedType, pos, state);
    }

    /** 注册时调用：把真实 BE 类型写入缓存，供 2 参数工厂使用。 */
    public static void init(BlockEntityType<?> type) {
        cachedType = type;
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        // 大号方块默认原版大型样式，小号方块默认原版小型样式
        if (getBlockState().getBlock() == AllBlocks.TRACK_43KG_LARGE_BOGEY.get())
            return AllTrackTypes.TRACK_43KG_LARGE_STYLE;
        return AllTrackTypes.TRACK_43KG_STYLE;
    }

    /**
     * HXD3C 转向架模型为真实比例（全长约 7 格），超出 Create 默认的 ±2 格
     * 渲染包围盒，放置状态下模型两端会被视锥裁剪。扩大到 ±4 格。
     */
    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(4);
    }
}
