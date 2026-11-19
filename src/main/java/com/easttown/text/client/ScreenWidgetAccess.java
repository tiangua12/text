package com.easttown.text.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

/**
 * 对 {@code Screen#addRenderableWidget} 的调用入口。
 *
 * <p>为什么需要这一层：那个方法是 {@code Screen} 上的 <b>protected 泛型方法</b>，
 * {@code StationScreen} 只是调用它、并没有覆写它。Mixin 的 {@code @Shadow}
 * 只在目标类自身的方法表里查找，找不到就往父类找——字段支持这种继承查找，方法不支持，
 * 于是 {@code @Shadow addRenderableWidget} 会在 apply 阶段直接抛
 * {@code InvalidMixinException: ... was not located in the target class}，
 * 把整个 Create 一起带崩（060.10 里 StationScreen 一加载就完蛋）。
 *
 * <p>改用 {@code @Invoker}：{@code ScreenWidgetAccessMixin} 把它实现到 {@code Screen} 上，
 * 任何 {@code Screen} 子类都能 cast 到本接口。
 *
 * <p>本接口<b>必须留在非 mixin 包</b>——mixin handler 的方法体被内联进目标类，
 * 里面出现的类型不能来自 {@code com.easttown.text.mixin}（AGENTS.md §4.2 R-JVM）。
 */
public interface ScreenWidgetAccess {

    <T extends GuiEventListener & Renderable & NarratableEntry> T text$addRenderableWidget(T widget);
}
