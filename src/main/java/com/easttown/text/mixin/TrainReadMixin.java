package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

import com.easttown.text.data.TextTrackSavedData;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 列车反序列化前先合并 43kg 轨道图：
 *
 * <p>43kg 图独立存于 {@code text_track_43kg}，{@code create_tracks} 里没有它们。
 * Create 的 {@code RailwaySavedData.load} 先读图、再读列车——列车读图引用时
 * （{@code Train.read} → {@code trackNetworks.get(graphId)}）43kg 图若尚未合并，
 * 返回 null → 列车 graph 为 null → 客户端判定脱轨（进存档所有车脱轨）。
 *
 * <p>在 {@code Train.read} 入口把 43kg 图合并进传入的图集合（{@code putIfAbsent}，
 * 幂等），保证图引用解析成功。运行时合并（{@code GlobalRailwayManagerMixin}）保留。
 */
@Mixin(Train.class)
public abstract class TrainReadMixin {

    @Inject(method = "read", at = @At("HEAD"))
    private static void text$merge43kgGraphsBeforeTrainRead(CompoundTag tag, HolderLookup.Provider registries,
                                                            Map<UUID, TrackGraph> trackNetworks,
                                                            DimensionPalette dimensions,
                                                            CallbackInfoReturnable<Train> cir) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        TextTrackSavedData extra = TextTrackSavedData.load(server);
        for (TrackGraph graph : extra.getTrackNetworks().values())
            trackNetworks.putIfAbsent(graph.id, graph);
    }
}
