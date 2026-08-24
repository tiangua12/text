package com.easttown.text.registry;

import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 43kg 转向架方块实体：默认样式为 43kg 专用样式（TRACK_43KG_STYLE）。
 *
 * <p>Create 装配列车时从转向架方块 BE 读取样式——本 BE 默认 43kg 样式，
 * 因此用 43kg 转向架方块装配的列车转向架就是 43kg 样式（真正分开的转向架，
 * 而非运行时动态更换）。
 */
public class Track43kgBogeyBlockEntity extends StandardBogeyBlockEntity {

    /** 供 2 参数工厂使用的类型缓存（注册时由 AllTracks 设置）。 */
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
        return AllTrackTypes.TRACK_43KG_STYLE;
    }
}
