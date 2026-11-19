package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.easttown.text.content.train.CarriageOffsets;
import com.easttown.text.content.train.CarriageYOffsetAccess;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSyncData;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 把车厢竖直偏移塞进每节车厢已有的同步通道。
 *
 * <p>{@code CarriageSyncData} 是 {@code CarriageContraptionEntity} 的
 * {@code SynchedEntityData}，每 {@code updateInterval} 由服务端重算一次
 * （{@code update}）并发给客户端（{@code read}），客户端再 {@code apply} 回
 * {@code Carriage}。偏移只需要跟着这条既有链路走，既不用新加同步包，
 * 也不需要客户端自己去猜服务端设定。
 *
 * <p>读写是对称的：{@code write}/{@code read} 的每个 {@code RETURN} 前都会插一次
 * （这两个方法在"有 fallback 位置"和"只有车轮位置"两种分支下都会提前 return），
 * 所以两端读到的字节序始终一致。
 *
 * <p>{@code copy()} 也要带上，否则 {@code EntityDataSerializer#copy} 会把偏移丢掉。
 */
@Mixin(CarriageSyncData.class)
public abstract class CarriageSyncOffsetMixin {

    @Unique
    private float text$yOffset;

    @Inject(method = "write", at = @At("RETURN"))
    private void text$writeYOffset(FriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeFloat(text$yOffset);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void text$readYOffset(FriendlyByteBuf buffer, CallbackInfo ci) {
        text$yOffset = buffer.readFloat();
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void text$captureYOffset(CarriageContraptionEntity entity, Carriage carriage, CallbackInfo ci) {
        text$yOffset = CarriageOffsets.of(carriage);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void text$applyYOffset(CarriageContraptionEntity entity, Carriage carriage, CallbackInfo ci) {
        if (!(carriage instanceof CarriageYOffsetAccess access))
            return;
        if (Float.compare(access.getCarriageYOffset(), text$yOffset) == 0)
            return;
        access.setCarriageYOffset(text$yOffset);
        // AABB 只在 setPos 里刷新，静止列车不会自己重算，这里补一次
        CarriageOffsets.refreshBounds(carriage, entity.level());
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void text$copyYOffset(CallbackInfoReturnable<CarriageSyncData> cir) {
        CarriageSyncData copy = cir.getReturnValue();
        if (copy != null)
            ((CarriageSyncOffsetMixin) (Object) copy).text$yOffset = text$yOffset;
    }
}
