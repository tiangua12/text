package com.easttown.text.network;

import com.easttown.text.Text;
import com.easttown.text.content.train.CarriageOffsets;
import com.easttown.text.content.train.StationOffsetAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

/**
 * 客户端 → 服务端：设置某个列车站的"装配竖直偏移"。
 *
 * <p>只有一个方向。车站 GUI 里的输入框改动时发一次（回车 / 失焦 / 关界面），服务端做三件事：
 * <ol>
 * <li>把值写进车站方块实体（以后在这站组装的列车自动带上）；</li>
 * <li>如果站上停着列车，立刻刷到它的每一节车厢（不用拆了重装）；</li>
 * <li>{@code sendData()} 把值同步回客户端，GUI 显示与存档一致。</li>
 * </ol>
 *
 * <p>服务端对来源做距离校验：车站不是随便一个方块，不能让远处的玩家改别人站上的装配参数。
 */
public record StationOffsetPayload(BlockPos pos, float offset) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StationOffsetPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Text.MODID, "station_offset"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationOffsetPayload> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, StationOffsetPayload payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeFloat(payload.offset());
            },
            (RegistryFriendlyByteBuf buf) -> new StationOffsetPayload(buf.readBlockPos(), buf.readFloat()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端处理。校验失败时静默丢弃，不踢人也不回包。 */
    public static void apply(StationOffsetPayload payload, ServerPlayer player) {
        if (player.distanceToSqr(payload.pos().getX() + .5, payload.pos().getY() + .5, payload.pos().getZ() + .5) > 64 * 64)
            return;

        BlockEntity be = player.level().getBlockEntity(payload.pos());
        if (!(be instanceof StationBlockEntity stationBlockEntity))
            return;
        if (!(stationBlockEntity instanceof StationOffsetAccess access))
            return;

        float offset = CarriageOffsets.clamp(payload.offset());
        access.setCarriageYOffset(offset);

        GlobalStation station = stationBlockEntity.getStation();
        if (station != null) {
            Train train = station.getPresentTrain();
            if (train != null && CarriageOffsets.applyToTrain(train, offset))
                for (Carriage carriage : train.carriages)
                    CarriageOffsets.refreshBounds(carriage, player.level());
        }

        stationBlockEntity.sendData();
    }
}
