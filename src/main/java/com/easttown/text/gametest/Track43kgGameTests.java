package com.easttown.text.gametest;

import com.easttown.text.registry.AllTrackTypes;
import com.easttown.text.registry.AllTracks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * 43kg 轨距功能 GameTest：
 * <ol>
 *   <li>43kg 轨道方块/物品/转向架方块已注册；</li>
 *   <li>43kg TrackType 已注册且 43kg 轨道材料绑定该轨距；</li>
 *   <li>43kg 转向架样式已注册进 AllBogeyStyles；</li>
 *   <li>放置 43kg 轨道后能进入轨道图（TrackGraph），且与标准轨道互不相连。</li>
 * </ol>
 */
@GameTestHolder("text")
public class Track43kgGameTests {

    @GameTest(template = "empty", templateNamespace = "text")
    public void test43kgRegistries(GameTestHelper helper) {
        // 1. 方块注册
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("text", "track_43kg"));
        if (block != AllTracks.TRACK_43KG_BLOCK.get())
            helper.fail("text:track_43kg block not registered");
        if (BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("text", "track_43kg")) == null)
            helper.fail("text:track_43kg item not registered");
        if (!(AllTracks.TRACK_43KG_BOGEY_BLOCK.get() instanceof AbstractBogeyBlock<?>))
            helper.fail("43kg bogey block not registered as bogey");

        // 2. TrackType / TrackMaterial
        TrackMaterial mat43 = com.easttown.text.registry.AllTrackMaterials.TRACK_43KG;
        if (mat43.trackType != AllTrackTypes.TRACK_43KG)
            helper.fail("43kg material does not use 43kg track type");
        if (TrackMaterial.ALL.get(ResourceLocation.fromNamespaceAndPath("text", "track_43kg")) != mat43)
            helper.fail("43kg material not in TrackMaterial.ALL");

        // 3. BogeyStyle
        if (!AllBogeyStyles.BOGEY_STYLES.containsKey(ResourceLocation.fromNamespaceAndPath("text", "track_43kg")))
            helper.fail("43kg bogey style not registered");

        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "text", timeoutTicks = 200)
    public void test43kgTrackGraphIsolation(GameTestHelper helper) {
        GlobalRailwayManager railways = Create.RAILWAYS;

        // 放置两条标准轨道（互连）
        BlockPos standardA = new BlockPos(0, 1, 0);
        BlockPos standardB = new BlockPos(1, 1, 0);
        BlockState standardState = AllBlocks.TRACK.getDefaultState();
        helper.setBlock(standardA, standardState);
        helper.setBlock(standardB, standardState);

        // 放置两条 43kg 轨道（互连），与标准轨道相邻但不连接
        BlockPos kg43A = new BlockPos(3, 1, 0);
        BlockPos kg43B = new BlockPos(4, 1, 0);
        BlockState kg43State = AllTracks.TRACK_43KG_BLOCK.get().defaultBlockState();
        helper.setBlock(kg43A, kg43State);
        helper.setBlock(kg43B, kg43State);

        // 等待轨道图生成（tick 调度）
        helper.runAfterDelay(40, () -> {
            int graphCount = railways.trackNetworks.size();
            // 标准轨道与 43kg 轨道不共享图 → 至少有两张图
            if (graphCount < 2)
                helper.fail("expected >=2 track graphs (standard + 43kg isolated), got " + graphCount);
            helper.succeed();
        });
    }
}
