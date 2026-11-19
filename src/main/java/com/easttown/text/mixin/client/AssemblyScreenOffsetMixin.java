package com.easttown.text.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.easttown.text.client.ScreenWidgetAccess;
import com.easttown.text.client.StationBlockEntityAccess;
import com.easttown.text.client.StationOffsetBox;
import com.simibubi.create.content.trains.station.AssemblyScreen;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 在**装配列车界面**（{@code AssemblyScreen}，背景 {@code STATION_ASSEMBLING} 200×178）
 * 里放"装配高度偏移"输入框。
 *
 * <p>这个界面才是设装配参数的地方：它只在车站处于装配模式、列车还没组装时出现，
 * 值随车站存档，点"组装"时由 {@code StationBlockOffsetMixin} 拦
 * {@code Create.RAILWAYS.addTrain(train)} 写进新列车每一节车厢。
 *
 * <p>位置 y+32..y+45：上面是 {@code iconTypeScroll}（y+17..y+31，占满 x+4..x+166），
 * 下面是 y+47 的 "N bogeys" 状态文字，中间这条 14 高的空带不压任何控件。
 *
 * <p>走 {@link StationBlockEntityAccess}（{@code @Accessor} 混入父类
 * {@code AbstractStationScreen}）和 {@link ScreenWidgetAccess}（{@code @Invoker} 混入
 * {@code Screen}），不用 {@code @Shadow}：父类成员 shadow 在无 refMap 环境下必崩，
 * 详见 AGENTS.md §5.2。
 */
@Mixin(AssemblyScreen.class)
public abstract class AssemblyScreenOffsetMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void text$addOffsetBox(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        StationBlockEntity blockEntity = ((StationBlockEntityAccess) (Object) this).text$getStationBlockEntity();
        if (blockEntity == null)
            return;

        int x = (self.width - AllGuiTextures.STATION_ASSEMBLING.getWidth()) / 2;
        int y = (self.height - AllGuiTextures.STATION_ASSEMBLING.getHeight()) / 2;

        ((ScreenWidgetAccess) (Object) this)
                .text$addRenderableWidget(new StationOffsetBox(blockEntity, Minecraft.getInstance().font, x + 64,
                        y + 34, 60, 10));
    }
}
