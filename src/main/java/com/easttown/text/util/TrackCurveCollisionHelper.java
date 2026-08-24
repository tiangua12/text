package com.easttown.text.util;

import java.lang.reflect.Field;

import com.easttown.text.Config;
import com.easttown.text.registry.AllTrackMaterials;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackPlacement;
import com.simibubi.create.content.trains.track.TrackShape;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 给 32kg 轨道弯道的曲线区域补碰撞体积的工具类（普通包，非 mixin 包——
 * mixin 包的类不能从注入后的目标类直接引用）。
 *
 * <p>Create 放置弯道时只在两端节点放轨道方块（TrackPlacement.placeTracks），
 * 贝塞尔曲线扫过的中间格子保持空气，因此弯道内部没有碰撞。本工具把曲线
 * 扫过的格子填充为 {@code SHAPE=NONE} 的轨道方块——该状态在 blockstate 里
 * 映射为 air 模型（不可见），但 getCollisionShape 仍返回 16×16×2 的
 * TRACK_COLLISION（与直道相同的矮碰撞），且不创建方块实体、零开销。
 *
 * <p>约束（见配置 {@link Config#CURVE_COLLISION_ENABLED}）：
 * <ul>
 *   <li>默认关闭；开启后只对放置之后的弯道生效；</li>
 *   <li>只处理 32kg 轨道（text:track_32kg）的弯道；</li>
 *   <li>只处理平面弯道（两端无高度差），坡度弯道不生成。</li>
 * </ul>
 *
 * <p>注意：不要在方法体内引用任何 mixin 包（com.easttown.text.mixin）的类，
 * 因为 handler 方法体会被 mixin 嵌入目标类（TrackPlacement / TrackBlock）。
 */
public final class TrackCurveCollisionHelper {

	/** 格子中心到曲线的最远填充距离（格），约等于弯道道床视觉宽度的一半。 */
	private static final double FILL_DISTANCE = 1.3;

	private TrackCurveCollisionHelper() {
	}

	/**
	 * 读取 PlacementInfo 的包私有字段 curve（贝塞尔连接）。
	 *
	 * <p>用反射而非 @Accessor：本工具类位于普通包，handler 方法体被 mixin 嵌入
	 * 目标类（TrackPlacement）后仍可正常引用；@Accessor 若放 mixin 包则会被
	 * 目标类引用触发 IllegalClassLoadError。
	 */
	public static BezierConnection getCurve(TrackPlacement.PlacementInfo info) {
		try {
			Field f = TrackPlacement.PlacementInfo.class.getDeclaredField("curve");
			f.setAccessible(true);
			return (BezierConnection) f.get(info);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	/** 该方块状态是否为本功能生成的隐形碰撞填充块。 */
	public static boolean isFillBlock(BlockState state) {
		return state.getBlock() instanceof TrackBlock
			&& state.getValue(TrackBlock.SHAPE) == TrackShape.NONE
			&& !state.getValue(TrackBlock.HAS_BE);
	}

	/** 弯道创建后调用：把曲线扫过的空气格子填充为 NONE 轨道方块（仅服务端、平面、32kg、配置开启时）。 */
	public static void fillCurveCollision(Level level, BezierConnection bc, TrackMaterial material) {
		if (level.isClientSide)
			return;
		if (!Config.CURVE_COLLISION_ENABLED.get())
			return;
		if (material != AllTrackMaterials.TRACK_32KG)
			return;
		// 坡度弯道（两端有高度差）不生成，避免坡上填充块错位
		if (Math.abs(bc.starts.getFirst().y - bc.starts.getSecond().y) > 0.001)
			return;

		java.util.List<Vec3> samples = new java.util.ArrayList<>();
		int segments = Math.max(1, bc.getSegmentCount());
		for (int i = 0; i <= segments; i++)
			samples.add(bc.getPosition(i / (float) segments));

		AABB bounds = bc.getBounds();
		int minX = (int) Math.floor(bounds.minX - 1);
		int maxX = (int) Math.ceil(bounds.maxX + 1);
		int minY = (int) Math.floor(bounds.minY - 1);
		int maxY = (int) Math.ceil(bounds.maxY + 1);
		int minZ = (int) Math.floor(bounds.minZ - 1);
		int maxZ = (int) Math.ceil(bounds.maxZ + 1);

		BlockState noneState = material.getBlock()
			.defaultBlockState()
			.setValue(TrackBlock.SHAPE, TrackShape.NONE)
			.setValue(TrackBlock.HAS_BE, false);

		double sq = FILL_DISTANCE * FILL_DISTANCE;
		for (int x = minX; x <= maxX; x++)
			for (int y = minY; y <= maxY; y++)
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (!state.canBeReplaced() && !state.is(BlockTags.FLOWERS))
						continue;
					Vec3 center = Vec3.atCenterOf(pos);
					boolean hit = false;
					for (Vec3 s : samples) {
						if (center.distanceToSqr(s) < sq) {
							hit = true;
							break;
						}
					}
					if (hit)
						level.setBlock(pos, noneState, 3);
				}
	}

	/** 弯道拆除时调用：清除该连接包围盒内所有 NONE 轨道方块（仅服务端）。 */
	public static void clearCurveCollision(Level level, BezierConnection bc) {
		if (level.isClientSide)
			return;

		AABB bounds = bc.getBounds();
		int minX = (int) Math.floor(bounds.minX - 1);
		int maxX = (int) Math.ceil(bounds.maxX + 1);
		int minY = (int) Math.floor(bounds.minY - 1);
		int maxY = (int) Math.ceil(bounds.maxY + 1);
		int minZ = (int) Math.floor(bounds.minZ - 1);
		int maxZ = (int) Math.ceil(bounds.maxZ + 1);

		for (int x = minX; x <= maxX; x++)
			for (int y = minY; y <= maxY; y++)
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (isFillBlock(level.getBlockState(pos)))
						level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
	}
}
