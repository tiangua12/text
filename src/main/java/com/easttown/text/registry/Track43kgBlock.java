package com.easttown.text.registry;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 43kg 轨距轨道方块。
 *
 * <p>轨道方块自身不做任何连接过滤——端点必须完整保留，轨道才可选中、可行驶、
 * 可被信号/车站绑定。轨距隔离由下列机制完成：
 * <ul>
 *   <li>{@code TrackEdgeGaugeMixin}：轨道边转向（canTravelTo）时禁止跨轨距；
 *       43kg 轨道因此自成网络，轨道图天然与标准图分离，可独立持久化；</li>
 *   <li>Create 原生轨距校验：移动时 {@code isOnIncompatibleTrack}、寻路时
 *       {@code Navigation} validTypes 过滤——列车无法跨轨距行驶；</li>
 *   <li>{@link #getBogeyAnchor}：装配/右击轨道生成的转向架必须是 43kg 专用转向架
 *       （Create 原生实现硬编码标准转向架，会跨轨距）；</li>
 *   <li>{@code TrainRelocatorGaugeMixin}：扳手搬迁列车到目标轨道时，
 *       目标轨道轨距必须与列车转向架一致，否则拒绝搬迁。</li>
 * </ul>
 */
public class Track43kgBlock extends TrackBlock {

    public Track43kgBlock(Properties properties, TrackMaterial material) {
        super(properties, material);
    }

    /**
     * Create 原生实现硬编码 {@code AllBlocks.SMALL_BOGEY}（标准转向架），
     * 导致在 43kg 轨道上右击轨道装配时生成标准转向架——跨轨距。
     * 覆写为 43kg 专用转向架（小号），轴方向跟随轨道形状。
     */
    @Override
    public BlockState getBogeyAnchor(BlockGetter world, BlockPos pos, BlockState state) {
        return AllTracks.TRACK_43KG_BOGEY_BLOCK.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_AXIS,
                        state.getValue(SHAPE) == TrackShape.XO ? Direction.Axis.X : Direction.Axis.Z);
    }
}
