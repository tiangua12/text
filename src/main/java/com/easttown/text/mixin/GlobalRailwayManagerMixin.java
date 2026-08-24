package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

import com.easttown.text.data.TextTrackSavedData;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.graph.TrackGraph;

import net.minecraft.server.MinecraftServer;

/**
 * 43kg 轨距轨道图加载合并 + 存档脏标记：
 * <ul>
 *   <li>{@code loadTrackData}：把 {@code text_track_43kg} 文件里的 43kg 图合并回
 *       {@code Create.RAILWAYS.trackNetworks}（运行时列车/信号/车站均从全局网络查询）；</li>
 *   <li>{@code markTracksDirty}：43kg 图变更时同时标记独立文件为脏。</li>
 * </ul>
 */
@Mixin(GlobalRailwayManager.class)
public abstract class GlobalRailwayManagerMixin {

    @Shadow
    public Map<UUID, TrackGraph> trackNetworks;

    /**
     * 加载存档后合并 43kg 图。
     */
    @Inject(method = "loadTrackData", at = @At("TAIL"))
    private void text$merge43kgGraphs(MinecraftServer server, CallbackInfo ci) {
        TextTrackSavedData extra = TextTrackSavedData.load(server);
        for (TrackGraph graph : extra.getTrackNetworks().values()) {
            trackNetworks.putIfAbsent(graph.id, graph);
        }
    }

    /**
     * 轨道图变更时，43kg 独立文件也标记为需要保存。
     */
    @Inject(method = "markTracksDirty", at = @At("TAIL"))
    private void text$mark43kgSavedDataDirty(CallbackInfo ci) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null)
            TextTrackSavedData.load(server).setDirty();
    }
}
