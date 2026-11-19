package com.easttown.text.client;

import com.easttown.text.content.track.node.NodeTrackBlockEntity;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 节点轨道的方块实体渲染器。只做两件事：
 *
 * <ol>
 *   <li>画已连接的曲线：{@code LOCKED=true} 后方块隐形，曲线没有任何方块在渲染，
 *       必须由这里调 {@link TrackRenderer#renderBezierTurn} 补上。</li>
 *   <li>画轴向指示箭头：未锁定时在节点中心画白色双头箭头。</li>
 * </ol>
 *
 * <p><b>当前状态：节点系统整体禁用</b>。方块已不注册，本渲染器没有 BE 会用到它；
 * 源码保留以便恢复。若恢复了方块注册，记得同时恢复 {@code TextClient} 里的
 * {@code registerBlockEntityRenderer} 那行。
 *
 * <p>方块模型本身刻意不渲染（{@code NodeTrackBlock#getRenderShape} 恒 INVISIBLE）：
 * 节点是隐形锚点，玩家需要看的是它指向哪，一个不会跟着角度转的方块模型反而误导。
 *
 * <p>1.21.1 的顶点 API 与 1.20.1 不同：{@code vertex(...).color(...).normal(...).endVertex()}
 * 已改成 {@code addVertex(pose, x, y, z).setColor(...).setNormal(pose, ...)}。
 */
@OnlyIn(Dist.CLIENT)
public class NodeTrackRenderer extends SafeBlockEntityRenderer<NodeTrackBlockEntity> {

    /** 箭头半长（格）。 */
    private static final double ARROW_LENGTH = 2.0;
    /** 箭头尖端分叉长度。 */
    private static final double ARROW_HEAD = 0.4;
    /** 箭头分叉横向张开量。 */
    private static final double ARROW_SPREAD = 0.2;
    /** 箭头抬升，避免与轨面 z-fighting。 */
    private static final double ARROW_LIFT = 0.15;

    public NodeTrackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(NodeTrackBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null)
            return;

        VertexConsumer solid = buffer.getBuffer(RenderType.cutoutMipped());
        for (BezierConnection connection : be.getConnections().values())
            TrackRenderer.renderBezierTurn(level, connection, ms, solid);

        if (be.isLocked)
            return;

        renderAxisArrow(be, ms, buffer);
    }

    /** 未锁定时的白色双头箭头，指示当前轴向。 */
    private static void renderAxisArrow(NodeTrackBlockEntity be, PoseStack ms, MultiBufferSource buffer) {
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        Vec3 center = new Vec3(0.5, 0.6, 0.5);
        Vec3 axis = be.getAxis();

        Vec3 positive = center.add(axis.scale(ARROW_LENGTH));
        Vec3 negative = center.subtract(axis.scale(ARROW_LENGTH));
        drawLine(ms, lines, negative, positive);

        Vec3 perpendicular = axis.cross(new Vec3(0, 1, 0));
        if (perpendicular.lengthSqr() < 1.0E-6)
            perpendicular = new Vec3(1, 0, 0);
        perpendicular = perpendicular.normalize().scale(ARROW_SPREAD);

        Vec3 lift = new Vec3(0, ARROW_LIFT, 0);
        Vec3 back = axis.scale(ARROW_HEAD);

        drawLine(ms, lines, positive, positive.subtract(back).add(perpendicular).add(lift));
        drawLine(ms, lines, positive, positive.subtract(back).subtract(perpendicular).add(lift));
        drawLine(ms, lines, negative, negative.add(back).add(perpendicular).add(lift));
        drawLine(ms, lines, negative, negative.add(back).subtract(perpendicular).add(lift));
    }

    private static void drawLine(PoseStack ms, VertexConsumer consumer, Vec3 from, Vec3 to) {
        PoseStack.Pose pose = ms.last();
        Vec3 normal = to.subtract(from).normalize();
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    @Override
    public boolean shouldRenderOffScreen(NodeTrackBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 192;
    }
}