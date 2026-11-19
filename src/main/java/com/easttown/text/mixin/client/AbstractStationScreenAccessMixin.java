package com.easttown.text.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.easttown.text.client.StationBlockEntityAccess;
import com.simibubi.create.content.trains.station.AbstractStationScreen;
import com.simibubi.create.content.trains.station.StationBlockEntity;

/**
 * 给 {@code AbstractStationScreen} 的 protected 字段 {@code blockEntity} 开一个访问器。
 *
 * <p>目标类必须是**字段的声明类**（{@code AbstractStationScreen}），不能是子类
 * {@code StationScreen}——{@code @Shadow} / {@code @Accessor} 都不跨父类查找。
 * 这一条踩过两次：先是 {@code @Shadow addRenderableWidget}（父类 {@code Screen} 的方法），
 * 再是 {@code @Shadow blockEntity}（父类 {@code AbstractStationScreen} 的字段），
 * 两次都在 apply 阶段把整个 Create 带崩。
 */
@Mixin(AbstractStationScreen.class)
public abstract class AbstractStationScreenAccessMixin implements StationBlockEntityAccess {

    @Accessor("blockEntity")
    @Override
    public abstract StationBlockEntity text$getStationBlockEntity();
}
