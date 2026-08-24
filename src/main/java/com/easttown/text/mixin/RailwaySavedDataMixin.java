package com.easttown.text.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.easttown.text.data.TextTrackSavedData;
import com.simibubi.create.content.trains.RailwaySavedData;
import com.simibubi.create.content.trains.graph.TrackGraph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

/**
 * 43kg 轨距轨道图存档分离：
 * 保存 {@code create_tracks} 时剔除 43kg 图（它们由 {@link TextTrackSavedData}
 * 单独存到 {@code text_track_43kg} 文件）。
 */
@Mixin(RailwaySavedData.class)
public abstract class RailwaySavedDataMixin {

    /**
     * 把写入 create_tracks 的图列表过滤为"不含 43kg 图"。
     * save() 里第一处 writeCompoundList 即 RailGraphs。
     */
    @ModifyArg(method = "save", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/nbt/NBTHelper;writeCompoundList(Ljava/lang/Iterable;Ljava/util/function/Function;)Lnet/minecraft/nbt/ListTag;",
            ordinal = 0), index = 0)
    private Iterable<TrackGraph> text$exclude43kgFromCreateTracks(Iterable<TrackGraph> graphs) {
        java.util.List<TrackGraph> filtered = new java.util.ArrayList<>();
        for (TrackGraph g : graphs)
            if (!TextTrackSavedData.is43kgGraph(g))
                filtered.add(g);
        return filtered;
    }

    /** 确保 43kg 独立文件在本轮保存时也被写盘。 */
    @Inject(method = "save", at = @At("HEAD"))
    private void text$markTextTrackSavedDataDirty(CompoundTag nbt, HolderLookup.Provider registries,
                                                  CallbackInfoReturnable<CompoundTag> cir) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null)
            TextTrackSavedData.load(server).setDirty();
    }
}
