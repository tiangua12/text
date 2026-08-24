package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.easttown.text.registry.AllTrackTypes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * 43kg 轨道上装配/行驶的列车自动使用 43kg 专用转向架样式：
 *
 * <p>Create 装配列车时从世界里的转向架方块 BE 读取样式（默认 STANDARD）。
 * 若转向架所在的行驶边轨距为 43kg，本 mixin 将 43kg 样式<b>持久化写入
 * bogeyData</b>（而不仅是动态返回）：
 * <ul>
 *   <li>装配在 43kg 轨道上 → 转向架样式固定为 43kg → 能在 43kg 轨道正常行驶；</li>
 *   <li>43kg 列车开到标准边 → 样式仍是 43kg → {@code isOnIncompatibleTrack} 判定
 *       不兼容 → 脱轨（两系统不互通）；</li>
 *   <li>普通轨道上的列车不受影响（边轨距为标准 → 保持原样式）。</li>
 * </ul>
 */
@Mixin(CarriageBogey.class)
public abstract class CarriageBogeyGaugeMixin {

    @Inject(method = "getStyle", at = @At("RETURN"), cancellable = true)
    private void text$auto43kgStyleOn43kgTrack(CallbackInfoReturnable<BogeyStyle> cir) {
        BogeyStyle style = cir.getReturnValue();
        if (style != null && style.id.equals(AllTrackTypes.TRACK_43KG_STYLE.id))
            return; // 已是 43kg 样式

        CarriageBogey bogey = (CarriageBogey) (Object) this;
        TravellingPoint point = bogey.leading();
        if (point == null)
            return;
        TrackEdge edge = point.edge;
        if (edge == null || edge.getTrackMaterial() == null)
            return;
        if (edge.getTrackMaterial().trackType != AllTrackTypes.TRACK_43KG)
            return;

        // 持久化 43kg 样式到 bogeyData（public 字段），保证跨边/存档后仍为 43kg 样式
        CompoundTag data = bogey.bogeyData;
        if (data == null)
            data = new CompoundTag();
        NBTHelper.writeResourceLocation(data, "BogeyStyle", AllTrackTypes.TRACK_43KG_STYLE.id);
        bogey.bogeyData = data;
        cir.setReturnValue(AllTrackTypes.TRACK_43KG_STYLE);
    }
}
