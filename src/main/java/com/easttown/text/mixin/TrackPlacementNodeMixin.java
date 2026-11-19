package com.easttown.text.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.easttown.text.content.track.node.NodeTrackBlock;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.TrackPlacement;

import net.createmod.catnip.math.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 节点轨道专用的放置放宽。**当前已禁用：本类未在 {@code text.mixins.json} 登记，不会生效。**
 *
 * <p>之所以整类保留、只用「不登记」来禁用（而不是删掉），是因为这是节点系统里唯一
 * 改动 Create 内部行为的部分：一旦它挂上，{@code create:track} 原版轨道的放置路径
 * 也会经过这些 handler（虽然内部判了是否节点，但注入本身生效）。禁用节点系统时
 * 必须把它一起摘掉，否则等于给原版轨道保留了一条隐形的改动通道。
 *
 * <p>恢复方法：把 {@code "TrackPlacementNodeMixin"} 加回 {@code text.mixins.json} 的
 * {@code mixins} 数组，并恢复 {@code AllBlocks.NODE_TRACK} / {@code AllItems.NODE_TRACK} 注册。
 *
 * <hr>
 *
 * <p><b>它原本要解决的问题</b>：节点把轴向搬进 BE 后两节点可以任意角度相对，而
 * Create 6.0.10 的 {@code TrackPlacement.tryConnect} 用「两条轴向延长线的交点」推算弯道尺寸，
 * 角度一顺手就撞两个拒绝条件（玩家看到 {@code track.too_sharp} /「转弯过急」）：
 * <ul>
 *   <li>{@code intersect[0] < 0 || intersect[1] < 0}；</li>
 *   <li>{@code turnSize = min(dist1, dist2)} 小于 2（爬升分支是 3.25）。</li>
 * </ul>
 * 参考实现（Quality-Railway / Create 5.x）靠 {@code @ModifyConstant} 改 60f、300f 两个角度常量，
 * 但 6.x 已经没有那两个常量。
 *
 * <p><b>注入点的选择是关键</b>：{@code tryConnect} 里 {@code VecHelper.intersect} 被调用 4 次
 * （{@code javap} 核对过 ordinal 与偏移）：
 * <ul>
 *   <li>ordinal 0 —— 主轴，它的负值会驱动「翻转 axis2」的自动修正，<b>抹掉负号会让曲线反向</b>，所以不动；</li>
 *   <li>ordinal 1 —— S 曲线 sTest，那条路报的是 {@code perpendicular}，与本问题无关，不动；</li>
 *   <li>ordinal 2、3 —— 俯仰 / 爬升分支，正是 {@code too_sharp} 的判定处，只 clamp 这两个。</li>
 * </ul>
 * 另外<b>没有</b>照搬参考实现的「交点返回 null 伪装平行」：6.x 的调用点
 * {@code intersect[0] / axis1.length()} 不判 null，那样会直接 NPE。
 *
 * <p>{@code Math.min(DD)} 在 {@code tryConnect} 内恰好只有两处（都是 {@code turnSize}），
 * 所以这一条不写 ordinal，两处都抬，不会误伤别处的 {@code Math.min(int,int)}。
 */
@Mixin(TrackPlacement.class)
public abstract class TrackPlacementNodeMixin {

    /** 交点为负时抬到的最小正值。 */
    @Unique
    private static final double TEXT$MIN_INTERSECT = 0.05D;

    /** 抬升后的最小弯道尺寸：必须大于原版爬升分支的 3.25。 */
    @Unique
    private static final double TEXT$MIN_TURN_SIZE = 4.0D;

    private static final String TEXT$INTERSECT_TARGET =
            "Lnet/createmod/catnip/math/VecHelper;intersect(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/Direction$Axis;)[D";

    /** 俯仰分支（intersect ordinal = 2）。 */
    @Redirect(method = "tryConnect", require = 1, at = @At(value = "INVOKE",
            target = TEXT$INTERSECT_TARGET, ordinal = 2))
    private static double[] text$clampSlopeIntersect(Vec3 p1, Vec3 p2, Vec3 d1, Vec3 d2, Axis axis,
                                                     Level level, Player player, BlockPos pos2, BlockState state2,
                                                     ItemStack stack, boolean girder, boolean maximiseTurn) {
        return text$clampForNode(VecHelper.intersect(p1, p2, d1, d2, axis), level, pos2, state2, stack);
    }

    /** 爬升分支（intersect ordinal = 3）。 */
    @Redirect(method = "tryConnect", require = 1, at = @At(value = "INVOKE",
            target = TEXT$INTERSECT_TARGET, ordinal = 3))
    private static double[] text$clampAscendIntersect(Vec3 p1, Vec3 p2, Vec3 d1, Vec3 d2, Axis axis,
                                                      Level level, Player player, BlockPos pos2, BlockState state2,
                                                      ItemStack stack, boolean girder, boolean maximiseTurn) {
        return text$clampForNode(VecHelper.intersect(p1, p2, d1, d2, axis), level, pos2, state2, stack);
    }

    /** 节点参与时把弯道尺寸抬到 {@link #TEXT$MIN_TURN_SIZE}。 */
    @Redirect(method = "tryConnect", require = 1,
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
    private static double text$raiseNodeTurnSize(double a, double b,
                                                 Level level, Player player, BlockPos pos2, BlockState state2,
                                                 ItemStack stack, boolean girder, boolean maximiseTurn) {
        double result = Math.min(a, b);
        if (!text$involvesNode(level, pos2, state2, stack))
            return result;
        return Math.max(result, TEXT$MIN_TURN_SIZE);
    }

    @Unique
    private static double[] text$clampForNode(double[] result, Level level, BlockPos pos2, BlockState state2,
                                              ItemStack stack) {
        if (result == null || result.length < 2)
            return result;
        if (!text$involvesNode(level, pos2, state2, stack))
            return result;
        return new double[] {
                Math.max(result[0], TEXT$MIN_INTERSECT),
                Math.max(result[1], TEXT$MIN_INTERSECT)
        };
    }

    /**
     * 这次连线是否涉及节点：目标方块是节点，或手上物品记着的起点是节点。
     *
     * <p>起点从 6.x 的 DataComponent {@code AllDataComponents.TRACK_CONNECTING_FROM} 读
     * ——1.20.1 是物品 NBT 的 {@code "ConnectingFrom"}，1.21 已改成组件。
     */
    @Unique
    private static boolean text$involvesNode(Level level, BlockPos pos2, BlockState state2, ItemStack stack) {
        if (state2 != null && state2.getBlock() instanceof NodeTrackBlock)
            return true;
        if (level == null || stack == null || stack.isEmpty())
            return false;

        TrackPlacement.ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null || from.pos() == null)
            return false;
        return level.getBlockState(from.pos()).getBlock() instanceof NodeTrackBlock;
    }
}