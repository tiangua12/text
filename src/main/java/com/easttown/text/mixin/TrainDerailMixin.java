package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.world.level.Level;

/**
 * 轨距不兼容 → 直接脱轨：
 *
 * <p>Create 的 {@code Train.tick} 对 {@code isOnIncompatibleTrack()} 只做
 * {@code blocked} 处理（speed=0 停一 tick，下 tick 手动输入又恢复 → "慢慢挪动"）。
 * 本 mixin 在 blocked 处理后追加：若任一车厢判定轨距不兼容（普通转向架在
 * 43kg 轨道上），直接置 {@code derailed = true} → 列车进入脱轨减速，无法继续行驶。
 *
 * <p>43kg 转向架列车（样式已由 CarriageBogeyGaugeMixin 持久化为 43kg）在
 * 43kg 轨道上不触发此判定，正常行驶；开到标准边时同样脱轨（两系统不互通）。
 */
@Mixin(Train.class)
public abstract class TrainDerailMixin {

    @Shadow
    @Final
    public List<Carriage> carriages;

    @Shadow
    public boolean derailed;

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void text$derailOnIncompatibleGauge(Level level, CallbackInfo ci) {
        if (derailed)
            return; // 已脱轨

        for (Carriage carriage : carriages) {
            if (carriage == null)
                continue;
            if (carriage.isOnIncompatibleTrack()) {
                derailed = true;
                return;
            }
        }
    }
}
