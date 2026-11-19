package com.easttown.text.net;

import com.easttown.text.Text;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：切换"列车碰撞体显示"的某一层。
 *
 * <p>{@link #op} 是一个<b>位掩码</b>，客户端按 {@code flags ^= op} 处理；
 * {@link #OP_ALL_OFF}（-1）表示全关。三种层可以任意组合叠加。
 *
 * <p>状态只存在客户端（{@code ClientTrainBoxState}），服务端只下发"要切哪一层"，
 * 因为它并不知道这个玩家当前开了哪些层。多人下每个玩家各自独立。
 */
public record TrainBoxPayload(int op) implements CustomPacketPayload {

    /** 全关。用一个负值当哨兵，正常掩码只有低 3 位。 */
    public static final int OP_ALL_OFF = -1;

    /** 不规则碰撞体：逐方块的真实碰撞形状（{@code Contraption.simplifiedEntityColliders}）。 */
    public static final int FLAG_BOXES = 1;
    /** 车厢外框 OBB（{@code Contraption.bounds} 的 8 角）。 */
    public static final int FLAG_HULL = 1 << 1;
    /** 实体碰撞箱 AABB（{@code getBoundingBox()}）。 */
    public static final int FLAG_AABB = 1 << 2;

    public static final CustomPacketPayload.Type<TrainBoxPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "train_box"));

    /**
     * 一个字节即可：{@code writeByte(-1)} / {@code readByte()} 在有符号 byte 上能原样往返，
     * 所以 {@link #OP_ALL_OFF} 不会被破坏。不要在 codec 里写 payload id，NeoForge 会自动带。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, TrainBoxPayload> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, TrainBoxPayload payload) -> buf.writeByte(payload.op()),
            (RegistryFriendlyByteBuf buf) -> new TrainBoxPayload(buf.readByte()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
