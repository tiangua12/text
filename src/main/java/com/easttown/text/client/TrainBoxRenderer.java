package com.easttown.text.client;

import java.util.List;

import com.easttown.text.net.TrainBoxPayload;
import com.easttown.text.util.TrainBoxGeometry;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 把列车车厢的碰撞体画成线框。三种层独立开关，可叠加：
 *
 * <ul>
 *   <li><b>青</b>（{@code FLAG_BOXES}）：<b>不规则碰撞体</b>——逐方块碰撞形状拆出的每个盒子。
 *       这是 Create 里真正拿来做碰撞的那套几何，楼梯/台阶/栅栏都是它们本来的形状；</li>
 *   <li><b>黄</b>（{@code FLAG_HULL}）：车厢整体外框 OBB，只用来定位，不参与碰撞；</li>
 *   <li><b>绿</b>（{@code FLAG_AABB}）：实体碰撞箱 {@code getBoundingBox()}。</li>
 * </ul>
 *
 * <p>为什么用 {@code toGlobalVector} 而不是自己按 yaw 造旋转矩阵：那是 Create 摆放车厢
 * 用的同一套变换（anchor + applyRotation），自驾矩阵在初始朝向 / 传送门 / 上下坡上会对不齐。
 *
 * <p>不规则碰撞体可能上百个盒子，所以只在镜头 {@value #SCAN_RADIUS} 格内绘制；
 * 缩放/距离都不做剔除，调试就要看全。
 */
@OnlyIn(Dist.CLIENT)
public final class TrainBoxRenderer {

    /** 搜索半径（格）：只画镜头附近的车厢，避免整图遍历。 */
    private static final double SCAN_RADIUS = 128;

    private static final float[] COLOR_BOXES = { 0.25F, 0.95F, 1.0F, 1.0F };
    private static final float[] COLOR_HULL = { 1.0F, 0.85F, 0.2F, 1.0F };
    private static final float[] COLOR_AABB = { 0.25F, 1.0F, 0.35F, 1.0F };

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;

        int flags = ClientTrainBoxState.flags();
        if (flags == 0)
            return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null)
            return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        // stage 里的坐标系仍带着相机旋转，顶点要用世界坐标 ⇒ 先平移回世界原点。
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        boolean drawBoxes = (flags & TrainBoxPayload.FLAG_BOXES) != 0;
        boolean drawHull = (flags & TrainBoxPayload.FLAG_HULL) != 0;
        boolean drawAabb = (flags & TrainBoxPayload.FLAG_AABB) != 0;

        AABB scan = AABB.ofSize(cameraPos, SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS);
        for (CarriageContraptionEntity carriage : level.getEntitiesOfClass(CarriageContraptionEntity.class, scan)) {
            if (drawAabb)
                LevelRenderer.renderLineBox(poseStack, lines, carriage.getBoundingBox(),
                        COLOR_AABB[0], COLOR_AABB[1], COLOR_AABB[2], COLOR_AABB[3]);

            if (drawHull) {
                TrainBoxGeometry.CarriageHull hull = TrainBoxGeometry.hull(carriage, partialTicks);
                if (hull != null)
                    drawCorners(poseStack, lines, hull.corners(), COLOR_HULL);
            }

            if (drawBoxes) {
                List<Vec3[]> boxes = TrainBoxGeometry.collisionBoxes(carriage, partialTicks);
                for (Vec3[] box : boxes)
                    drawCorners(poseStack, lines, box, COLOR_BOXES);
            }
        }

        poseStack.popPose();
        // 手动 flush：线的 buffer 不会被后面的 entity 渲染顺带清掉。
        buffers.endBatch(RenderType.lines());
    }

    private static void drawCorners(PoseStack poseStack, VertexConsumer consumer, Vec3[] corners, float[] color) {
        for (TrainBoxGeometry.EdgeSegment edge : TrainBoxGeometry.edges(corners))
            drawLine(poseStack, consumer, edge.from(), edge.to(), color);
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to, float[] color) {
        PoseStack.Pose pose = poseStack.last();
        Vec3 normal = to.subtract(from).normalize();
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(color[0], color[1], color[2], color[3])
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(color[0], color[1], color[2], color[3])
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private TrainBoxRenderer() {
    }
}
