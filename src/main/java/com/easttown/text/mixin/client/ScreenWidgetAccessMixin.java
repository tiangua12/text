package com.easttown.text.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.easttown.text.client.ScreenWidgetAccess;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

/**
 * 给 {@code Screen} 装一个 {@code addRenderableWidget} 的访问器。
 *
 * <p>用 class mixin + {@code abstract @Invoker}（而不是 interface mixin）：接口
 * {@link ScreenWidgetAccess} 必须放在非 mixin 包，否则 {@code ScreenWidgetAccess} 的
 * 调用方（同样在 mixin 包里）内联后会引用 mixin 包类型，触发 R-JVM。
 * class mixin 的 abstract 方法由 Mixin 生成实现，效果与 interface mixin 等价。
 *
 * <p>泛型边界必须原样照抄 {@code Screen#addRenderableWidget} 的三段约束，
 * 擦除后的描述符才会是 {@code (LGuiEventListener;)LGuiEventListener;}，否则 {@code @Invoker}
 * 依旧找不到目标方法。
 */
@Mixin(Screen.class)
public abstract class ScreenWidgetAccessMixin implements ScreenWidgetAccess {

    @Invoker("addRenderableWidget")
    @Override
    public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T text$addRenderableWidget(
            T widget);
}
