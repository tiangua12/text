package com.easttown.text.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.easttown.text.client.ScreenWidgetAccess;
import com.easttown.text.client.StationBlockEntityAccess;
import com.easttown.text.client.StationOffsetBox;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.station.StationScreen;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 往 Create 的列车站界面里插一个"装配竖直偏移"输入框。
 *
 * <p>位置选在 {@code STATION} 背景（200×127）的 y+90 那一行：
 * 上面是 {@code STATION_TEXTBOX_BOTTOM}（y+86..y+90）车厢信息框的收边，
 * 下面是 y+102 的开门方式控件，中间正好空着 10 像素——够放一个 10 高的输入框，
 * 又不会压到 Create 原有的任何控件或点击区。
 *
 * <p>只重写 {@code init}：输入框自己的回车、失焦、贴图都由 {@link StationOffsetBox} 处理，
 * Screen 的 {@code keyPressed} / {@code mouseClicked} / {@code removed} 一个都不用碰
 * （{@code super.mouseClicked} 会把事件分发给 {@code renderables}，{@code AbstractWidget}
 * 自己就能吃焦点）。
 *
 * <p><b>本 mixin 不 shadow 任何成员</b>——三个需要的东西全走别的路：
 * <ul>
 * <li>{@code blockEntity}（protected，声明在父类 {@code AbstractStationScreen}）→
 * {@link StationBlockEntityAccess}（{@code @Accessor} 混入声明类）；</li>
 * <li>{@code addRenderableWidget}（protected，声明在父类 {@code Screen}）→
 * {@link ScreenWidgetAccess}（{@code @Invoker} 混入声明类）；</li>
 * <li>窗口原点 → 直接用 {@code Screen.width/height}（public）减去背景尺寸算，
 * 与 catnip {@code AbstractSimiScreen.init} 里的算法一致，省掉 {@code guiLeft/guiTop}
 * 和 {@code @Local} 两种都对父类没辙的写法。</li>
 * </ul>
 * 教训：{@code @Shadow} 在无 refMap 的 dev 环境里**只认目标类自身声明的成员**，
 * 父类的字段和方法一律报 {@code was not located in the target class}，而且一崩就把
 * Create 一起带走。
 */
@Mixin(StationScreen.class)
public abstract class StationScreenOffsetMixin {

    @Inject(method = "init",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/station/StationScreen;tickTrainDisplay()V"))
    private void text$addOffsetBox(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        StationBlockEntity blockEntity = ((StationBlockEntityAccess) (Object) this).text$getStationBlockEntity();
        if (blockEntity == null)
            return;

        int x = (self.width - AllGuiTextures.STATION.getWidth()) / 2;
        int y = (self.height - AllGuiTextures.STATION.getHeight()) / 2;

        ((ScreenWidgetAccess) (Object) this)
                .text$addRenderableWidget(new StationOffsetBox(blockEntity, Minecraft.getInstance().font, x + 64,
                        y + 91, 60, 10));
    }
}
