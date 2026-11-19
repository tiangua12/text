package com.easttown.text.client;

import java.util.Locale;

import com.easttown.text.content.train.CarriageOffsets;
import com.easttown.text.content.train.StationOffsetAccess;
import com.easttown.text.network.StationOffsetPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 车站 / 装配界面里那个"装配竖直偏移"输入框。
 *
 * <p>做成 {@link EditBox} 的子类，让背景、标签、回车提交都自包含——Screen 侧只要
 * {@code addRenderableWidget(new StationOffsetBox(...))} 一句。
 *
 * <h2>为什么这么写</h2>
 * catnip 的 {@code AbstractSimiScreen} 覆写了 {@code getFocused()}：
 * <pre>{@code
 * GuiEventListener focused = super.getFocused();
 * if (focused instanceof AbstractWidget && !focused.isFocused())
 *     focused = null;
 * setFocused(focused);      // 反手把 widget.setFocused(false) 打回去
 * return focused;
 * }</pre>
 * 而 {@code Screen#keyPressed} 每次按键都调它，于是焦点会在"以为自己在编辑"和
 * "其实已经被清掉"之间反复横跳。这带来两个必须自己兜住的后果：
 *
 * <ul>
 * <li><b>字符能输入、退格/回车失效</b>：{@code charTyped} 不看焦点一致性，
 * {@code keyPressed} 看。所以退格自己处理，不走 {@code EditBox} 那套
 * （它还额外依赖 {@code cursorPosition > 0}）。</li>
 * <li><b>输入到一半自己变回原值</b>：只要"非编辑态就同步真实值"的判断被反复触发，
 * 文本就会被 {@code current()} 覆盖掉。所以引入 {@link #editing} 状态：
 * 编辑期间不同步、并每帧把焦点抢回来。</li>
 * </ul>
 *
 * <p>{@link #editing} 的退出条件刻意区分了两种"失焦"：
 * 被**别的控件**抢走（{@code screen.getFocused() != null}）才算退出编辑；
 * 被 catnip 的自愈逻辑清成 {@code null} 不算——那种情况下一帧会自己抢回来。
 * 这样站名框之类的其它输入控件依然能被正常点到。
 */
public class StationOffsetBox extends EditBox {

    private static final int LABEL_COLOR = 0x7A7A7A;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int BACKGROUND_COLOR = 0xFF141414;

    private final StationBlockEntity blockEntity;
    private final Font font;

    private float lastSyncedValue;
    /** 正在编辑：不同步真实值、每帧抢焦点。 */
    private boolean editing;

    public StationOffsetBox(StationBlockEntity blockEntity, Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, Component.translatable("text.gui.carriage_offset"));
        this.blockEntity = blockEntity;
        this.font = font;

        setBordered(false);
        setTextColor(0xFFFFFF);
        setMaxLength(8);
        setFilter(text -> text.isEmpty() || text.matches("-?\\d{0,2}(\\.\\d{0,3})?"));
        lastSyncedValue = current();
        setValue(format(lastSyncedValue));
        setTooltip(Tooltip.create(Component.translatable("text.gui.carriage_offset.tooltip")));
    }

    private float current() {
        return blockEntity instanceof StationOffsetAccess access ? access.getCarriageYOffset() : 0;
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (editing) {
            // 焦点若被 catnip 的自愈清掉，这里一帧内拿回来
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null && screen.getFocused() != this)
                screen.setFocused(this);
        } else {
            // 非编辑态才跟随真实值；只在值真的变了才重设，避免每帧复位光标
            float currentValue = current();
            if (Float.compare(currentValue, lastSyncedValue) != 0) {
                lastSyncedValue = currentValue;
                setValue(format(currentValue));
            }
        }

        graphics.drawString(font, Component.translatable("text.gui.carriage_offset"), getX() - 41, getY() + 2,
                LABEL_COLOR, false);
        graphics.fill(getX() - 2, getY() - 1, getX() + getWidth() + 2, getY() + getHeight() + 1, BORDER_COLOR);
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), BACKGROUND_COLOR);

        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            commit();
            editing = false;
            setFocused(false);
            return true;
        }

        // 退格自己处理：EditBox 的实现依赖 cursorPosition > 0，
        // 光标在开头（点空白处、程序化 setValue 之后都会回到 0）时按了等于没按。
        if (keyCode == InputConstants.KEY_BACKSPACE) {
            String value = getValue();
            if (!value.isEmpty())
                setValue(value.substring(0, value.length() - 1));
            setCursorPosition(getValue().length());
            setHighlightPos(getCursorPosition());
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!isInside(mouseX, mouseY))
            return handled;

        // 只要点在框内就进编辑态：不依赖 super 的返回值，也不依赖 isFocused()
        // —— catnip 的焦点自愈可能已经把后者清成 false 了。
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null)
            screen.setFocused(this);
        super.setFocused(true);
        editing = true;

        String value = getValue();
        setCursorPosition(value.length());
        setHighlightPos(value.length());
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        boolean wasFocused = isFocused();
        super.setFocused(focused);

        if (!wasFocused && focused) {
            String value = getValue();
            setCursorPosition(value.length());
            setHighlightPos(getCursorPosition());
            return;
        }

        if (wasFocused && !focused) {
            // 只有"被别的控件抢走焦点"才算退出编辑；
            // catnip 自愈会把它清成 null，那种情况下一帧要抢回来。
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null && screen.getFocused() != null)
                editing = false;
        }
    }

    /** 解析输入并发给服务端；解析失败时退回当前值，不留半截脏数据。 */
    private void commit() {
        float value;
        try {
            value = CarriageOffsets.clamp(Float.parseFloat(getValue()));
        } catch (NumberFormatException e) {
            value = current();
        }

        if (blockEntity instanceof StationOffsetAccess access)
            access.setCarriageYOffset(value);
        lastSyncedValue = value;
        setValue(format(value));

        CatnipServices.NETWORK.sendToServer(new StationOffsetPayload(blockEntity.getBlockPos(), value));
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
