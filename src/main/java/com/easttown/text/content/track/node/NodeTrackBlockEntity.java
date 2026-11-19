package com.easttown.text.content.track.node;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 节点轨道的方块实体：把「节点轴向角度」从 blockstate 搬进 BE。
 *
 * <p><b>当前状态：节点系统整体禁用</b>（bug 太多）。本类保留是为了以后修好后直接恢复：
 * {@code AllBlocks.NODE_TRACK} / {@code AllItems.NODE_TRACK} 已不再注册，
 * 玩家拿不到也放不下节点方块；{@code TrackPlacementNodeMixin} 也已从
 * {@code text.mixins.json} 摘除。{@code AllBlockEntities.NODE_TRACK} 仍然注册着
 * （否则本类编译不过），但它指向的方块不存在，属于孤儿类型，不影响任何行为。
 *
 * <p><b>为什么当初需要自有 BE</b>：Create 原版轨道节点的轴向由 blockstate 的
 * {@code TrackShape}（16 向）决定，弯道角度只能取 22.5° 的整数倍且放下后无法再改。
 * 节点把轴向放进 BE，{@code getTrackAxes} 从 {@link #getAxis()} 读，于是能任意角度、放下后还能调。
 *
 * <p><b>自有 BE 的合法性</b>（已核对 6.0.10 源码）：Create 对轨道 BE 一律用
 * {@code instanceof TrackBlockEntity} 判断（19 处），只有 {@code TrackBlock} 自己引用
 * {@code AllBlockEntityTypes.TRACK}，没有写死的类型相等比较；ticker 由
 * {@code IBE#getTicker} 默认实现提供（{@code SmartBlockEntity} 子类 -> {@code SmartBlockEntityTicker}）。
 */
public class NodeTrackBlockEntity extends TrackBlockEntity {

    private float angle;

    /** 是否已固化：{@link NodeTrackBlock} 与渲染器都读它。 */
    public boolean isLocked;

    /**
     * 注册期回写的 BE 类型。
     *
     * <p>{@code BlockEntityType.Builder.of} 只接受两参工厂 {@code (pos, state)}，而
     * {@code TrackBlockEntity} 的构造器要求真实类型；注册那一刻类型对象还不存在，
     * 所以用静态缓存 + {@link #init} 回写（与 {@code Track43kgBogeyBlockEntity} 同一手法）。
     */
    private static BlockEntityType<?> cachedType;

    public NodeTrackBlockEntity(BlockPos pos, BlockState state) {
        super(cachedType, pos, state);
    }

    /** 由 {@code AllBlockEntities} 在注册 lambda 内调用，回写真实类型。 */
    public static void init(BlockEntityType<?> type) {
        cachedType = type;
    }

    /** 节点轴向单位向量（水平面内）：{@code (cos angle, 0, sin angle)}。 */
    public Vec3 getAxis() {
        double rad = Math.toRadians(angle);
        return new Vec3(Math.cos(rad), 0, Math.sin(rad)).normalize();
    }

    public float getAngle() {
        return angle;
    }

    /** 设置轴向角度；自动归一到 {@code [0, 360)}。 */
    public void setAngle(float value) {
        this.angle = ((value % 360.0F) + 360.0F) % 360.0F;
        setChanged();
        notifyUpdate();
    }

    /**
     * 连上曲线即锁定。
     *
     * <p>锁定后角度不可再调、方块模型彻底隐藏（{@code getRenderShape} 恒 {@code INVISIBLE}），
     * 只剩 {@code NodeTrackRenderer} 画的曲线。
     */
    @Override
    public void addConnection(BezierConnection connection) {
        super.addConnection(connection);
        if (isLocked)
            return;
        isLocked = true;
        setChanged();

        BlockState state = getBlockState();
        if (level != null && state.hasProperty(NodeTrackBlock.LOCKED)
                && !state.getValue(NodeTrackBlock.LOCKED))
            level.setBlockAndUpdate(worldPosition, state.setValue(NodeTrackBlock.LOCKED, true));
        notifyUpdate();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("TangentAngle", angle);
        tag.putBoolean("IsLocked", isLocked);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("TangentAngle"))
            angle = tag.getFloat("TangentAngle");
        if (tag.contains("IsLocked"))
            isLocked = tag.getBoolean("IsLocked");
    }
}