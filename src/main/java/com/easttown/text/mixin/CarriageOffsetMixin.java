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
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * 给 {@code Carriage} 装一个"装配后竖直偏移"字段，并写进列车存档。
 *
 * <p>Create 的车厢 NBT 由 {@code Carriage#write(DimensionPalette, Provider)} 产出、
 * {@code Carriage#read(...)} 读回，两者是唯一入口，所以只需要在这两个方法上各挂一次。
 * 键名带 {@code Text} 前缀，避免将来 Create 自己加同名字段。
 *
 * <p>{@code read} 是静态工厂，拿不到 {@code this}，只能在 {@code RETURN} 处取返回值再回填；
 * 早于本 mixin 存在的存档没有这个键，回落到 {@link CarriageOffsets#DEFAULT}。
 *
 * <p><b>默认值三处兜底</b>：字段初始化器、构造函数 TAIL、{@code read} 缺键分支。
 * 原因见 {@code text$initYOffset} 的注释——只写字段初始化器会被 Mixin 跳过。
 */
@Mixin(Carriage.class)
public abstract class CarriageOffsetMixin implements CarriageYOffsetAccess {

    @Unique
    private float text$carriageYOffset = CarriageOffsets.DEFAULT;

    /**
     * 构造函数里显式赋一次默认值。
     *
     * <p>为什么不能只靠字段初始化器 {@code = CarriageOffsets.DEFAULT}：
     * Mixin 要把字段初始化器搬进目标类构造函数，而目标类带显式构造器时这一步并不保证发生
     * （实测字段读出来仍是 0）。{@code @Inject} 到 {@code <init>} 的 RETURN 是确定会执行的。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void text$initYOffset(CarriageBogey leadingBogey, CarriageBogey trailingBogey, int bogeySpacing,
            CallbackInfo ci) {
        text$carriageYOffset = CarriageOffsets.DEFAULT;
    }

    @Override
    public float getCarriageYOffset() {
        return text$carriageYOffset;
    }

    @Override
    public void setCarriageYOffset(float offset) {
        text$carriageYOffset = CarriageOffsets.clamp(offset);
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void text$writeYOffset(DimensionPalette dimensions, HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> cir) {
        // 默认值也写出去：这样存档里能直观看到"这节车的偏移是 -0.5"，
        // 而不是靠读取端再猜一次默认值。
        cir.getReturnValue().putFloat("TextCarriageYOffset", text$carriageYOffset);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void text$readYOffset(CompoundTag tag, HolderLookup.Provider registries, TrackGraph graph,
            DimensionPalette dimensions, CallbackInfoReturnable<Carriage> cir) {
        Carriage carriage = cir.getReturnValue();
        if (carriage == null)
            return;
        // 老存档没有这个键，回落到默认值（-0.5），不是 0
        ((CarriageYOffsetAccess) carriage).setCarriageYOffset(tag.contains("TextCarriageYOffset")
                ? tag.getFloat("TextCarriageYOffset")
                : CarriageOffsets.DEFAULT);
    }
}
