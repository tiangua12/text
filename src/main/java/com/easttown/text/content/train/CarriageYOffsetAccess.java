package com.easttown.text.content.train;

/**
 * 车厢"装配后竖直偏移"的访问接口。
 *
 * <p>Create 的 {@code Carriage} 没有这个字段，由 {@code mixin/CarriageOffsetMixin}
 * 用 {@code @Unique} 字段实现本接口。之所以要接口而不是反射：mixin 的 handler 方法体
 * 里不能引用 {@code com.easttown.text.mixin} 包（见 AGENTS.md §4.2 R-JVM），
 * 而接口放在本包（非 mixin 包），双方都能安全引用。
 *
 * <p>偏移语义：<b>正值向上</b>，单位是格。它只作用于**已装配进列车的 contraption**
 * （方块、方块实体、碰撞），不作用于世界里单独摆放的转向架方块与轨道。
 */
public interface CarriageYOffsetAccess {

    float getCarriageYOffset();

    void setCarriageYOffset(float offset);
}
