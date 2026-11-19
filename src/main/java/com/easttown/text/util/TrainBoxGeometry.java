package com.easttown.text.util;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.collision.CollisionList;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 列车车厢碰撞体的几何计算（服务端 / 客户端共用）。
 *
 * <p><b>先澄清 Create 里"列车碰撞"到底是什么</b>（全部核对过 6.0.10-281 源码）：
 *
 * <ol>
 *   <li><b>车厢 ↔ 方块世界 / 车厢 ↔ 车厢（装置那一侧）</b>：真正的碰撞体是
 *       <b>逐方块的碰撞形状</b>，完全不是盒子。{@code Contraption#invalidateColliders()}
 *       遍历 {@code blocks}，对每个方块取
 *       {@code info.state().getCollisionShape(collisionLevel, localPos, CollisionContext.empty())}，
 *       再 {@code forAllBoxes(populate)} 把形状拆成若干 AABB，连同方块偏移一起塞进
 *       {@link CollisionList}（扁平 double 数组：centerX/Y/Z + extentsX/Y/Z）。
 *       楼梯、台阶、栅栏这种不规则形状因此被如实保留——这就是"不规则碰撞体"。</li>
 *   <li><b>列车 ↔ 列车</b>：{@code Train#collideWithOtherTrains} 反而很粗，用每节车厢
 *       leading/trailing 两点拼成的<b>线段</b>做 {@code VecHelper.intersect}，
 *       退化时把对向车厢当半径 {@code 0.125} 的球。这一层没有盒子。</li>
 *   <li><b>实体碰撞箱 / 渲染包围盒</b>：{@code getBoundingBox()} 与
 *       {@code createRenderBoundingBox().inflate(2)}，只用于实体碰撞与视锥裁剪。</li>
 * </ol>
 *
 * <p>本类提供前两者的可视化数据。所有世界坐标一律经
 * {@link AbstractContraptionEntity#toGlobalVector(Vec3, float)} 变换——那是 Create 自己摆放
 * 车厢用的同一套变换（anchor + applyRotation），比按 yaw 手搓旋转矩阵可靠。
 */
public final class TrainBoxGeometry {

    /** 一条线段的两个世界端点。 */
    public record EdgeSegment(Vec3 from, Vec3 to) {
    }

    /** 车厢外壳：实体 AABB + 由 contraption 整体 bounds 推出的 OBB。 */
    public record CarriageHull(AABB aabb, Vec3 obbCenter, Vec3 obbExtents, Vec3[] corners) {
    }

    /**
     * 车厢的<b>不规则碰撞体</b>：逐方块碰撞形状拆出的每个 AABB，返回其 8 个世界角点。
     *
     * <p>为什么返回角点而不是 {@link AABB}：车厢可能是斜的（弯道、上下坡），
     * 变换后的小盒不再是轴对齐的。用 8 个角点连线画出来才是真实形状；
     * 转成世界 AABB 会把盒子撑大，转角处会失真。
     *
     * <p>角点顺序：{@code index = (xBit << 2) | (yBit << 1) | zBit}，
     * 与 {@link #edges(Vec3[])} 的连线表对应。
     *
     * <p>数据源 {@link Contraption#getSimplifiedEntityColliders()} 由 Create 在装配时
     * 算好并缓存，这里只做坐标变换，不重算碰撞形状（重算需要 contraption 自己的假世界，
     * 外部拿不到）。
     */
    public static List<Vec3[]> collisionBoxes(AbstractContraptionEntity entity, float partialTicks) {
        Contraption contraption = entity.getContraption();
        if (contraption == null)
            return List.of();
        CollisionList colliders = contraption.getSimplifiedEntityColliders();
        if (colliders == null || colliders.size == 0)
            return List.of();

        List<Vec3[]> boxes = new ArrayList<>(colliders.size);
        for (int i = 0; i < colliders.size; i++) {
            double cx = colliders.centerX[i];
            double cy = colliders.centerY[i];
            double cz = colliders.centerZ[i];
            double ex = colliders.extentsX[i];
            double ey = colliders.extentsY[i];
            double ez = colliders.extentsZ[i];
            boxes.add(corners(entity, partialTicks,
                    cx - ex, cy - ey, cz - ez,
                    cx + ex, cy + ey, cz + ez));
        }
        return boxes;
    }

    /** 车厢外壳（AABB + 整体 OBB）；拿不到 contraption 时返回 {@code null}。 */
    public static CarriageHull hull(AbstractContraptionEntity entity, float partialTicks) {
        Contraption contraption = entity.getContraption();
        if (contraption == null)
            return null;
        AABB local = contraption.bounds;
        if (local == null)
            return null;

        Vec3[] corners = corners(entity, partialTicks,
                local.minX, local.minY, local.minZ,
                local.maxX, local.maxY, local.maxZ);

        Vec3 sum = Vec3.ZERO;
        for (Vec3 corner : corners)
            sum = sum.add(corner);

        Vec3 extents = new Vec3(local.getXsize() / 2, local.getYsize() / 2, local.getZsize() / 2);
        return new CarriageHull(entity.getBoundingBox(), sum.scale(1.0 / 8.0), extents, corners);
    }

    /**
     * 把一个轴对齐的局部盒子变换成 8 个世界角点。
     * 顺序：{@code x{min,max}} → {@code y{min,max}} → {@code z{min,max}}，
     * 即 {@code index = (xBit << 2) | (yBit << 1) | zBit}。
     */
    private static Vec3[] corners(AbstractContraptionEntity entity, float partialTicks,
                                  double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        Vec3[] corners = new Vec3[8];
        int i = 0;
        for (double x : new double[] { minX, maxX })
            for (double y : new double[] { minY, maxY })
                for (double z : new double[] { minZ, maxZ })
                    corners[i++] = entity.toGlobalVector(new Vec3(x, y, z), partialTicks);
        return corners;
    }

    /**
     * 8 角点的 12 条棱，索引约定同 {@link #corners}：
     * <pre>
     *   沿 Z: 0-1 2-3 4-5 6-7
     *   沿 Y: 0-2 1-3 4-6 5-7
     *   沿 X: 0-4 1-5 2-6 3-7
     * </pre>
     */
    public static EdgeSegment[] edges(Vec3[] corners) {
        int[][] pairs = {
                { 0, 1 }, { 2, 3 }, { 4, 5 }, { 6, 7 },
                { 0, 2 }, { 1, 3 }, { 4, 6 }, { 5, 7 },
                { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 },
        };
        EdgeSegment[] out = new EdgeSegment[pairs.length];
        for (int i = 0; i < pairs.length; i++)
            out[i] = new EdgeSegment(corners[pairs[i][0]], corners[pairs[i][1]]);
        return out;
    }

    private TrainBoxGeometry() {
    }
}
