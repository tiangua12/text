package com.easttown.text.content.train;

/**
 * 列车站"装配竖直偏移"设定值的访问接口。
 *
 * <p>由 {@code mixin/StationBlockOffsetMixin} 用 {@code @Unique} 字段实现在
 * {@code StationBlockEntity} 上。车站自己是这个设定值的权威来源：装配列车时把
 * 它写进每一节 {@code Carriage}，列车在站时改它则立即覆盖在该列车上。
 *
 * <p>存车站而不是只存列车，是因为 {@code /data} 之外没有别的"装配参数"容器，
 * 而车站本来就是 Create 的装配入口（{@code StationBlockEntity#assemble}）。
 */
public interface StationOffsetAccess {

    float getCarriageYOffset();

    void setCarriageYOffset(float offset);
}
