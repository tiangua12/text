package com.easttown.text.content.track.node;

import java.util.List;
import java.util.Locale;

import com.easttown.text.content.track.Track43kgBlock;
import com.easttown.text.registry.AllBlockEntities;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 节点轨道方块：一个轴向角度可调、连上曲线后隐形的轨道锚点。
 *
 * <p><b>当前状态：节点系统整体禁用</b>（bug 太多）。本类保留源码以便以后修复，但：
 * <ul>
 *   <li>{@code AllBlocks.NODE_TRACK} / {@code AllItems.NODE_TRACK} 不再注册
 *       —— 玩家拿不到物品、世界里也不会有这个方块；</li>
 *   <li>{@code TrackPlacementNodeMixin} 已从 {@code text.mixins.json} 摘除
 *       —— 放角限制的注入不生效；</li>
 *   <li>资源（blockstate / 模型 / 贴图 / lang / loot）保留，避免恢复时重做。</li>
 * </ul>
 * 恢复步骤：把 {@code AllBlocks} / {@code AllItems} 的字段加回、{@code AllCreativeTabs} 加回展示项、
 * {@code TextClient} 加回渲染器注册、{@code text.mixins.json} 加回 mixin，
 * 并在 5 个 tag 里补上 {@code text:node_track}（缺了会被 Create 铺成普通轨道）。
 *
 * <p>它仍是 Create 的 {@code TrackBlock} 子类（经 {@link Track43kgBlock}，以便复用
 * {@code getBogeyAnchor} 返回本模组转向架），原设计只覆写三件事：
 * <ol>
 *   <li>{@link #getTrackAxes} 读 {@link NodeTrackBlockEntity#getAxis()}，让轴可以任意角度；</li>
 *   <li>外观：{@link #getRenderShape} 恒 {@code INVISIBLE}，由 {@code NodeTrackRenderer}
 *       画轴向箭头与曲线；</li>
 *   <li>{@link #onWrenched} 用扳手调角度（{@value #ANGLE_STEP}° 一档，Shift 反向）。</li>
 * </ol>
 *
 * <p>碰撞是有意做成空的：节点是隐形锚点，不该挡车挡人。但 {@link #getShape} 仍返回薄板，
 * 保证能选中、能被扳手点到（选择框与碰撞箱是两套 API）。
 */
public class NodeTrackBlock extends Track43kgBlock {

    /** 是否已连上曲线并固化。固化后角度不可调、方块隐身。 */
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    /** 选中框：2px 薄板。碰撞箱为空，这个只影响高亮与点击。 */
    private static final VoxelShape NODE_SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    /** 扳手单次调节步长（度）。 */
    public static final float ANGLE_STEP = 22.5F;

    public NodeTrackBlock(Properties properties, TrackMaterial material) {
        super(properties, material);
        registerDefaultState(defaultBlockState().setValue(LOCKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(LOCKED));
    }

    /**
     * 恒返回 INVISIBLE：节点方块完全不渲染。
     *
     * <p>节点是隐形锚点，玩家需要看的是它指向哪。未锁定时由 {@code NodeTrackRenderer}
     * 画白色双头箭头表示轴向，锁定后连箭头也消失，只剩曲线。
     *
     * <p>试过让方块模型跟着角度转，两条路都不值得：blockstate 的 variant 旋转是
     * {@code Rotation} 四态（只有 0/90/180/270），装不下 22.5° 一档；
     * 用 {@code ModelBlockRenderer#tesselateBlock} 自己转着画虽可行，但为纯装饰模型引入
     * 烘焙模型查询不划算。
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return NODE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * 整套机制的核心：轴向改从 BE 读。
     *
     * <p>BE 还没加载时退回原版行为（按 {@code TrackShape} 推导），避免世界刚加载那一瞬间
     * {@code TrackPlacement} / 渲染拿到 null 轴。
     */
    @Override
    public List<Vec3> getTrackAxes(BlockGetter world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof NodeTrackBlockEntity be)
            return List.of(be.getAxis());
        return super.getTrackAxes(world, pos, state);
    }

    /*
     * 刻意**不**覆写 getCurveStart，直接沿用 TrackBlock 的默认实现：
     *
     *     getCenterOf(pos)                                 // (x+0.5, y+0.5, z+0.5)
     *         .add(0, axis.y != 0 ? 0 : -0.5f, 0)          // 非垂直 -> y + 0.5 - 0.5 = y
     *         .add(axis.scale(0.5))                        // 沿轴向再推出 0.5 格
     *
     * 移植时曾照参考实现写成 `new Vec3(x+0.5, y-0.5, z+0.5)`，同时犯两个错：
     *   1. y 比正确值低 0.5 -> 弯道整体下沉一格；
     *   2. 少了最后的 axis*0.5 -> 起点落在节点正中心，曲线缩回方块内部，看着像没按角度转。
     * 默认实现已经把「中心 + 高度 + 沿轴偏移」都算对了，删掉覆写即可。
     */

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null)
            return null;
        return state.setValue(HAS_BE, true).setValue(LOCKED, false);
    }

    /**
     * 轨道 BE 类型改指向自己的。
     *
     * <p>不只是好看：{@code TrackTargetingBlockItem} 会把
     * {@code ((IBE<?>) block).getBlockEntityType()} 写进物品 NBT，返回 {@code create:track}
     * 会让放置后的 BE 反序列化失败。
     */
    @Override
    public BlockEntityType<? extends com.simibubi.create.content.trains.track.TrackBlockEntity> getBlockEntityType() {
        return AllBlockEntities.NODE_TRACK.get();
    }

    /**
     * 必须显式覆写，<b>不能只靠 IBE 的 default 实现</b>。
     *
     * <p>{@code TrackBlock} 自己覆写了 {@code newBlockEntity}（返回
     * {@code AllBlockEntityTypes.TRACK.create(...)}），而类的方法优先于接口 default 方法。
     * 不覆写的实测后果（2026-11-19 客户端崩溃）：
     * <pre>
     * IllegalStateException: Invalid block entity create:track state ...
     *     got Block{text:node_track}[locked=false,...]
     *     at BlockEntity.validateBlockState
     *     at TrackBlock.newBlockEntity(TrackBlock.java:546)
     * </pre>
     * 因为 {@code create:track} 的 {@code validBlocks} 里没有 {@code text:node_track}。
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return AllBlockEntities.NODE_TRACK.get().create(pos, state);
    }

    /**
     * 扳手 = 调轴向。已固化（{@code isLocked} 或 {@code LOCKED=true}）时拒绝：
     * 连好的曲线再改角度会扭曲。
     *
     * <p>参考实现还有 Ctrl 精调（5.625°），但那是客户端 {@code Screen#hasControlDown}，
     * 在专用服务端会 {@code NoClassDefFoundError}，所以这里不做。
     */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (!(level.getBlockEntity(pos) instanceof NodeTrackBlockEntity be))
            return InteractionResult.PASS;

        if (be.isLocked || state.getValue(LOCKED)) {
            if (!level.isClientSide && player != null)
                player.displayClientMessage(Component.literal("节点已连接固化，无法调节！"), true);
            return InteractionResult.FAIL;
        }

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        boolean reverse = player != null && player.isShiftKeyDown();
        be.setAngle(be.getAngle() + (reverse ? -ANGLE_STEP : ANGLE_STEP));

        if (player != null)
            player.displayClientMessage(Component.literal(
                    "节点轴向: " + String.format(Locale.ROOT, "%.1f", be.getAngle()) + "°"), true);
        return InteractionResult.SUCCESS;
    }

    /**
     * 只在方块被彻底替换（不是换成另一种轨道）时移除 BE。
     *
     * <p>换成别的 {@code TrackBlock} 时保留 BE，是让 Create 自己清理连接；
     * 手动 {@code removeBlockEntity} 会让连接数据无处可写。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()
                && !(newState.getBlock() instanceof com.simibubi.create.content.trains.track.TrackBlock))
            if (level.getBlockEntity(pos) instanceof NodeTrackBlockEntity)
                level.removeBlockEntity(pos);
        super.onRemove(state, level, pos, newState, isMoving);
    }
}