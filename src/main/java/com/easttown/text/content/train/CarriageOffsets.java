package com.easttown.text.content.train;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * 车厢竖直偏移的读写入口（双端共用，<b>不属于 mixin 包</b>，mixin handler 可以安全调用）。
 *
 * <p>数据只落在两个地方：
 * <ul>
 * <li>{@code Carriage} 上的 {@code @Unique float}（真实数据，随列车存档）；</li>
 * <li>{@code StationBlockEntity} 上的 {@code @Unique float}（装配参数，随方块实体存档）。</li>
 * </ul>
 * 这个类只做"从实体/车厢取偏移"和"把偏移刷到整列车"，不持有任何状态。
 *
 * <p>渲染与碰撞分别在两处消费它：
 * <ul>
 * <li>{@code ContraptionRenderOffsetMixin} —— {@code applyLocalTransforms} 里抬 PoseStack
 * （Flywheel 的 {@code ContraptionVisual} 与经典 {@code ContraptionEntityRenderer} 共用这条路）；</li>
 * <li>{@code ContraptionAnchorOffsetMixin} —— {@code getAnchorVec} 里抬锚点，
 * 于是 {@code ContraptionCollider} 的逐方块碰撞、实体 AABB、{@code toGlobalVector}
 * （点车厢方块、坐座位）全部跟着走。</li>
 * </ul>
 */
public final class CarriageOffsets {

    /** 允许的上下偏移范围（格）。超出范围会让车厢穿地或把连接处拉断。 */
    public static final float MIN = -1.5f;
    public static final float MAX = 1.5f;

    /**
     * 车厢的默认偏移（格）。负值 = 车体下沉。
     *
     * <p>之所以不是 0：本模组转向架的构架建模高度和 Create contraption 的锚点之间有固定落差，
     * 组装后车体会浮在转向架上方，需要往下压一点才贴合。{@code -0.5} 是目前目视对齐的值。
     * 老存档没有这个 NBT 键的，也走这个默认值。
     */
    public static final float DEFAULT = -0.5f;

    /** 单个车厢的偏移；{@code null} 或未实现接口时按 0 处理（兼容 Create 其他附属）。 */
    public static float of(Carriage carriage) {
        if (carriage instanceof CarriageYOffsetAccess access)
            return access.getCarriageYOffset();
        return 0;
    }

    /**
     * 取某个 contraption 实体对应的车厢偏移。
     *
     * <p>只有 {@link CarriageContraptionEntity} 有车厢；其它 contraption（矿车装配、
     * 轴承旋转、电梯）返回 0，本模组的偏移对它们没有意义。
     */
    public static float of(AbstractContraptionEntity entity) {
        if (entity instanceof CarriageContraptionEntity carriageEntity) {
            Carriage carriage = carriageEntity.getCarriage();
            if (carriage != null)
                return of(carriage);
        }
        return 0;
    }

    /** 把偏移写进整列车每一节车厢，返回是否有车厢的值真的变了。 */
    public static boolean applyToTrain(Train train, float offset) {
        float clamped = clamp(offset);
        boolean changed = false;
        for (Carriage carriage : train.carriages) {
            if (!(carriage instanceof CarriageYOffsetAccess access))
                continue;
            if (Mth.equal(access.getCarriageYOffset(), clamped))
                continue;
            access.setCarriageYOffset(clamped);
            changed = true;
        }
        return changed;
    }

    public static float clamp(float offset) {
        return Mth.clamp(offset, MIN, MAX);
    }

    /**
     * 让已经生成的 contraption 实体重算一次包围盒。
     *
     * <p>实体 AABB 只在 {@code AbstractContraptionEntity#setPos} 里刷新，而静止的列车
     * 不一定每 tick 都 {@code setPos}。改偏移时不显式刷一次，AABB 会停在旧高度上，
     * 表现是"车厢抬了但推不动/能穿过"。
     *
     * <p>用 {@code getDimensionalIfPresent} 而不是 {@code getDimensional}：后者会
     * {@code computeIfAbsent} 建一个空的维度槽，这里只是顺手刷一下，不该有副作用。
     */
    public static void refreshBounds(Carriage carriage, Level level) {
        if (level == null)
            return;
        DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(level.dimension());
        if (dimensional == null || dimensional.entity == null)
            return;
        CarriageContraptionEntity entity = dimensional.entity.get();
        if (entity == null)
            return;
        // 同坐标 setPos 的唯一作用是触发 setBoundingBox(contraption.bounds.move(getAnchorVec()))
        entity.setPos(entity.getX(), entity.getY(), entity.getZ());
    }

    private CarriageOffsets() {
    }
}
