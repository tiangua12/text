package com.easttown.text.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.easttown.text.registry.AllTrackTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 43kg 轨距轨道图的独立存档文件（key: {@code text_track_43kg}）。
 *
 * <p>Create 原版把所有轨道的轨道图（TrackGraph）统一存在 overworld 的
 * {@code create_tracks}（RailwaySavedData）里。本类为 43kg 轨距提供独立文件：
 * <ul>
 *   <li><b>运行时</b>：43kg 图仍在 {@code Create.RAILWAYS.trackNetworks}（列车、
 *       信号、车站、传感器都从这里查询，保证兼容）；</li>
 *   <li><b>持久化</b>：{@link RailwaySavedDataMixin} 在保存 {@code create_tracks}
 *       时剔除含 43kg 边的图，改由本类单独序列化；加载时再合并回全局网络。</li>
 * </ul>
 */
public class TextTrackSavedData extends SavedData {

    public static final String NAME = "text_track_43kg";
    public static final String KEY_GRAPHS = "TrackGraphs43";

    private final Map<UUID, TrackGraph> trackNetworks = new HashMap<>();

    public static SavedData.Factory<TextTrackSavedData> factory() {
        return new SavedData.Factory<>(TextTrackSavedData::new, TextTrackSavedData::load);
    }

    @SuppressWarnings("null")
    @Override
    public CompoundTag save( CompoundTag nbt,  HolderLookup.Provider registries) {
        // 43kg 图始终从全局网络提取（运行时图与标准轨道图物理隔离，见 Track43kgBlock）
        DimensionPalette dimensions = new DimensionPalette();
        nbt.put(KEY_GRAPHS, NBTHelper.writeCompoundList(
                extract43kgGraphs(Create.RAILWAYS.trackNetworks.values()), tg -> tg.write(registries, dimensions)));
        dimensions.write(nbt);
        return nbt;
    }

    @SuppressWarnings("null")
    private static TextTrackSavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
        TextTrackSavedData sd = new TextTrackSavedData();
        DimensionPalette dimensions = DimensionPalette.read(nbt);
        NBTHelper.iterateCompoundList(nbt.getList(KEY_GRAPHS, Tag.TAG_COMPOUND), c -> {
            TrackGraph graph = TrackGraph.read(c, registries, dimensions);
            sd.trackNetworks.put(graph.id, graph);
        });
        return sd;
    }

    public Map<UUID, TrackGraph> getTrackNetworks() {
        return trackNetworks;
    }

    @SuppressWarnings("null")
    public static TextTrackSavedData load(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), NAME);
    }

    /** 判断一张图是否属于 43kg 轨距：存在任意一条 43kg 材料边。 */
    public static boolean is43kgGraph(TrackGraph graph) {
        for (TrackNodeLocation loc : graph.getNodes()) {
            TrackNode node = graph.locateNode(loc);
            if (node == null)
                continue;
            for (TrackEdge edge : graph.getConnectionsFrom(node).values())
                if (edge.getTrackMaterial() != null && edge.getTrackMaterial().trackType == AllTrackTypes.TRACK_43KG)
                    return true;
        }
        return false;
    }

    private static Iterable<TrackGraph> extract43kgGraphs(Iterable<TrackGraph> all) {
        java.util.List<TrackGraph> out = new java.util.ArrayList<>();
        for (TrackGraph g : all)
            if (is43kgGraph(g))
                out.add(g);
        return out;
    }
}
