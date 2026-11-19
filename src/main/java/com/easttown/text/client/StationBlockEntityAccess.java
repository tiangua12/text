package com.easttown.text.client;

import com.simibubi.create.content.trains.station.StationBlockEntity;

/**
 * 读取列车站界面持有的 {@code StationBlockEntity}。
 *
 * <p>{@code AbstractStationScreen#blockEntity} 是 protected，而且声明在**父类**上。
 * Mixin 的 {@code @Shadow} 只认目标类自身声明的成员——在 dev 环境（无 refMap）下
 * 连字段都不做父类查找，会直接抛
 * {@code InvalidMixinException: @Shadow field blockEntity was not located in the target class}。
 * 所以走 {@code @Accessor} 混入 {@code AbstractStationScreen}（字段真正的声明处）。
 *
 * <p>必须在非 mixin 包：调用方在 mixin 包里，类型若来自 {@code com.easttown.text.mixin}
 * 会触发 R-JVM（AGENTS.md §4.2）。
 */
public interface StationBlockEntityAccess {

    StationBlockEntity text$getStationBlockEntity();
}
