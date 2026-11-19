package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.easttown.text.content.train.CarriageOffsets;
import com.easttown.text.content.train.StationOffsetAccess;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 给 {@code StationBlockEntity} 装"装配竖直偏移"设定值。
 *
 * <p>车站是 Create 唯一的装配入口（{@code StationBlockEntity#assemble}），所以把
 * 装配参数存在这里：先设值再组装时自动带上，组装完再改则通过 GUI 的网络包直接刷到
 * 在站列车上（见 {@code StationOffsetPayload}）。
 *
 * <p>三个注入点：
 * <ul>
 * <li>{@code write} / {@code read} —— 随方块实体存档；{@code clientPacket} 分支也写，
 * 客户端 GUI 才有值可显示（{@code SmartBlockEntity#sendData} 走的就是这条）。</li>
 * <li>{@code assemble} —— 在 {@code Create.RAILWAYS.addTrain(train)} 之前把值刷到
 * 新列车的每一节车厢。选这个调用点而不是方法末尾，是因为 {@code train} 这个局部变量
 * 的 debug 作用域在最后一次使用后就结束了，{@code @Local} 在 TAIL 处取不到。</li>
 * </ul>
 */
@Mixin(StationBlockEntity.class)
public abstract class StationBlockOffsetMixin implements StationOffsetAccess {

    @Unique
    private float text$carriageYOffset = CarriageOffsets.DEFAULT;

    /**
     * 构造函数里显式赋一次默认值。
     *
     * <p>只写字段初始化器不可靠：Mixin 把字段初始化器搬进目标类构造函数这一步，
     * 在目标类带显式构造器时可能被跳过（实测读出来是 0 而不是 -0.5，GUI 也就显示 0.00）。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void text$initYOffset(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
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
    private void text$writeYOffset(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        tag.putFloat("TextCarriageYOffset", text$carriageYOffset);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void text$readYOffset(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        // 老存档没有这个键，回落到默认值（-0.5）；GUI 也读这个值显示
        text$carriageYOffset = tag.contains("TextCarriageYOffset")
                ? CarriageOffsets.clamp(tag.getFloat("TextCarriageYOffset"))
                : CarriageOffsets.DEFAULT;
    }

    @Redirect(method = "assemble",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/GlobalRailwayManager;addTrain(Lcom/simibubi/create/content/trains/entity/Train;)V"))
    private void text$applyOffsetOnAssemble(GlobalRailwayManager manager, Train train) {
        CarriageOffsets.applyToTrain(train, text$carriageYOffset);
        manager.addTrain(train);
    }
}
