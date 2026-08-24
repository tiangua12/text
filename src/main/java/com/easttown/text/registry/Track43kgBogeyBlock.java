package com.easttown.text.registry;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 43kg 轨距专用转向架方块（小号/大号共用此类，由 size 区分）。
 *
 * <p>与 Create 普通转向架方块（small_bogey / large_bogey）真正分开：
 * <ul>
 *   <li>{@link #getTrackType} 返回 43kg 轨距 → 只在 43kg 轨道上兼容，普通轨道上脱轨；</li>
 *   <li>{@link #getDefaultStyle} 返回 43kg 专用样式 → 装配列车时转向架即 43kg 样式；</li>
 *   <li>BE 类型为独立的 {@code text:track_43kg_bogey}（validBlocks 只含本方块，
 *       生成 {@link Track43kgBogeyBlockEntity} 实例），不复用 create:bogey
 *       （避免 validBlocks 校验崩溃）。</li>
 * </ul>
 */
public class Track43kgBogeyBlock extends StandardBogeyBlock {

    public Track43kgBogeyBlock(Properties props, BogeySizes.BogeySize size) {
        super(props, size);
    }

    @Override
    public TrackMaterial.TrackType getTrackType(BogeyStyle style) {
        return AllTrackTypes.TRACK_43KG;
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return AllTrackTypes.TRACK_43KG_STYLE;
    }

    @Override
    public BlockEntityType<? extends StandardBogeyBlockEntity> getBlockEntityType() {
        return AllTracks.TRACK_43KG_BOGEY_BE.get();
    }

    /**
     * Create 的 {@code AbstractBogeyBlock} 没有覆写 {@code getStateForPlacement}，
     * 手动放置转向架时 AXIS 恒为默认值 X（轴不跟随玩家朝向）。
     * 覆写后：轴方向 = 玩家放置时的水平朝向，且保持含水状态一致。
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        return ProperWaterloggedBlock.withWater(context.getLevel(), state, context.getClickedPos());
    }
}
