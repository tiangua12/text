package com.easttown.text;

import java.lang.reflect.Method;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.easttown.text.util.TrackCurveCollisionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class Config {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TRACK_TARGETING_MAX_DISTANCE = BUILDER
            .comment("Maximum distance (in blocks) for placing track-targeting items (signals, stations, etc.) from the selected track position")
            .defineInRange("trackTargetingMaxDistance", 64, 1, 256);

    public static final ModConfigSpec.BooleanValue CURVE_COLLISION_ENABLED = BUILDER
            .comment("Generate invisible collision blocks under curved tracks (only for the 32kg/m rail, flat curves only).",
                    "Disabled by default. When toggled off at runtime, all previously generated collision blocks are removed.",
                    "Enabling it only affects tracks placed afterwards.")
            .define("curveCollisionEnabled", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    /** 由 Text 构造器注册到 mod bus；Loading（含默认关闭的首次加载）与 Reloading 都会触发。 */
    @SuppressWarnings("null")
    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() != Config.SPEC)
            return;
        if (Config.CURVE_COLLISION_ENABLED.get())
            return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            ServerChunkCache cache = level.getChunkSource();
            Iterable<?> holders = getChunkHolders(cache);
            if (holders == null)
                continue;
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight();
            for (Object holderObj : holders) {
                ChunkHolder holder = (ChunkHolder) holderObj;
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk == null)
                    continue;
                int baseX = chunk.getPos()
                    .getMinBlockX();
                int baseZ = chunk.getPos()
                    .getMinBlockZ();
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        for (int y = minY; y < maxY; y++) {
                            BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                            BlockState state = chunk.getBlockState(pos);
                            if (TrackCurveCollisionHelper.isFillBlock(state)) {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                removed++;
                            }
                        }
            }
        }
        if (removed > 0)
            LOGGER.info("Curve collision disabled: removed {} invisible collision block(s)", removed);
    }

    /** ChunkMap.getChunks() 是 protected，用反射取已加载区块（仅开发/运行 classpath 可用，NeoForge 保证）。 */
    private static Iterable<?> getChunkHolders(ServerChunkCache cache) {
        try {
            Method m = Class.forName("net.minecraft.server.level.ChunkMap")
                .getDeclaredMethod("getChunks");
            m.setAccessible(true);
            return (Iterable<?>) m.invoke(cache.chunkMap);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("Failed to iterate loaded chunks for curve-collision cleanup", e);
            return null;
        }
    }
}
